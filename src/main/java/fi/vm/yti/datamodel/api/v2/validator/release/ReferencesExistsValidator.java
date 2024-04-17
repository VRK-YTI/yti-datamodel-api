package fi.vm.yti.datamodel.api.v2.validator.release;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Service
public class ReferencesExistsValidator extends ReleaseValidator {

    private final CoreRepository coreRepository;

    public ReferencesExistsValidator(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public Set<ResourceReferenceDTO> validate(Model model) {
        var graph = model.listSubjectsWithProperty(DCAP.preferredXMLNamespace)
                .filterKeep(s -> s.getNameSpace().startsWith(ModelConstants.SUOMI_FI_NAMESPACE))
                .next().getURI();

        var dependencies = DataModelUtils.getInternalReferenceModels(model.getResource(graph));

        // check that all references inside the model exists
        var notExistCurrent = getInternalReferences(model)
                .filterKeep(s -> s.getObject().toString().startsWith(graph))
                .filterKeep(s -> !model.contains(s.getObject().asResource(), null))
                .mapWith(s -> {
                    var ref = new ResourceReferenceDTO();
                    ref.setProperty(MapperUtils.getCurie(s.getPredicate().asResource(), model));
                    ref.setTarget(MapperUtils.getCurie(s.getObject().asResource(), model));

                    var subj = DataModelUtils.findSubjectForObject(s, model);
                    ref.setResourceURI(MapperUtils.uriToURIDTO(subj.getURI(), model));
                    return ref;
                })
                .toSet();

        Set<ResourceReferenceDTO> missingObjects = new HashSet<>(notExistCurrent);

        // check that all external references (to other models in Interoperability platform) exists
        var otherReferences = getInternalReferences(model)
                .filterKeep(r -> !r.getObject().toString().startsWith(graph))
                .mapWith(r -> DataModelURI.fromURI(r.getObject().toString()).getResourceURI())
                .mapWith(ResourceFactory::createResource)
                .toList();

        if (!dependencies.isEmpty() && !otherReferences.isEmpty()) {
            var select = new SelectBuilder();
            var exprFactory = select.getExprFactory();

            // finds all objects which doesn't exist in the database
            var missingExternalReferences = new ArrayList<Resource>();
            select
                    .addVar("?ext_subj")
                    .addVar(exprFactory.exists(
                                    new WhereBuilder().addWhere("?ext_subj", "?ext_pred", "?ext_obj")),
                            "?exists")
                    .addWhereValueVar("?ext_subj", otherReferences.toArray());

            coreRepository.querySelect(select.build(), (var row) -> {
                if (!row.get("exists").asLiteral().getBoolean()) {
                    var r = row.get("ext_subj").asResource();
                    dependencies.stream()
                            .filter(d -> r.getURI().startsWith(d))
                            .findFirst()
                            .ifPresent(d -> {
                                var dmURI = DataModelURI.fromURI(d);
                                var resURI = DataModelURI.createResourceURI(dmURI.getModelId(), r.getLocalName(), dmURI.getVersion());
                                missingExternalReferences.add(ResourceFactory.createResource(resURI.getResourceVersionURI()));
                            });
                }
            });

            missingExternalReferences.forEach(m -> model.listStatements(null, null, m).forEach(stmt -> {
                var ref = new ResourceReferenceDTO();
                var subj = DataModelUtils.findSubjectForObject(stmt, model);
                ref.setResourceURI(MapperUtils.uriToURIDTO(subj.getURI(), model));
                ref.setProperty(MapperUtils.getCurie(stmt.getPredicate(), model));
                ref.setTarget(stmt.getObject().asResource().getURI());
                missingObjects.add(ref);
            }));
        }

        return missingObjects;
    }

    @Override
    public String getErrorKey() {
        return "references-not-exist";
    }

    private static ExtendedIterator<Statement> getInternalReferences(Model model) {
        return model.listStatements()
                .filterDrop(s -> s.getPredicate().equals(OWL.imports)
                                 || s.getPredicate().equals(OWL.priorVersion))
                .filterKeep(s -> s.getObject().isResource()
                                 && s.getObject().toString().startsWith(ModelConstants.SUOMI_FI_NAMESPACE));
    }
}
