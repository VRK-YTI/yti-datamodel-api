package fi.vm.yti.datamodel.api.v2.validator.release;

import fi.vm.yti.common.Constants;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class ReferencesExistsValidator extends ReleaseValidator {

    private final CoreRepository coreRepository;

    private static final List<Resource> SKIP_PREDICATES = List.of(
            OWL.imports,
            OWL.priorVersion,
            DCTerms.requires,
            RDF.type
    );

    public ReferencesExistsValidator(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public Set<ResourceReferenceDTO> validate(Model model) {
        var graph = model.listSubjectsWithProperty(DCAP.preferredXMLNamespace)
                .filterKeep(s -> s.getNameSpace().startsWith(Constants.DATA_MODEL_NAMESPACE))
                .next().getURI();

        DataModelUtils.addPrefixesToModel(graph, model);
        var dependencies = DataModelUtils.getInternalReferenceModels(model.getResource(graph)).stream()
                .map(DataModelURI.Factory::fromURI)
                .toList();

        // check that all references inside the model exists
        var notExistCurrent = getInternalReferences(model)
                .filterKeep(s -> s.getObject().toString().startsWith(graph))
                .filterKeep(s -> !model.contains(s.getObject().asResource(), null))
                .mapWith(s -> mapResourceReference(model, s))
                .toSet();

        Set<ResourceReferenceDTO> missingObjects = new HashSet<>(notExistCurrent);

        // check that all external references (to other models in Interoperability platform) exists
        // for checking, version must be removed from the resource URI
        var otherReferences = getInternalReferences(model)
                .filterKeep(r -> !r.getObject().toString().startsWith(graph))
                .mapWith(r -> DataModelURI.Factory.fromURI(r.getObject().toString()).getResourceURI())
                .filterKeep(Objects::nonNull)
                .mapWith(ResourceFactory::createResource)
                .toSet();

        if (!dependencies.isEmpty() && !otherReferences.isEmpty()) {
            var missingExternalReferences = new HashSet<Resource>();

            // query for finding all objects which don't exist in the database
            var query = getExistsQuery(dependencies, otherReferences);
            coreRepository.querySelect(query, (var row) -> {
                if (!row.get("exists").asLiteral().getBoolean()) {
                    var r = row.get("ext_subj").asResource();
                    dependencies.stream()
                            .filter(d -> r.getNameSpace().equals(d.getNamespace()))
                            .findFirst()
                            .ifPresent(d -> {
                                var resURI = DataModelURI.Factory.createResourceURI(d.getModelId(), r.getLocalName(), d.getVersion());
                                missingExternalReferences.add(ResourceFactory.createResource(resURI.getResourceVersionURI()));
                            });
                }
            });

            for (var missingRef : missingExternalReferences) {
                model.listStatements(null, null, missingRef)
                        .forEach(stmt -> missingObjects.add(mapResourceReference(model, stmt)));
            }
        }
        return missingObjects;
    }

    @Override
    public String getErrorKey() {
        return "references-not-exist";
    }

    private static ResourceReferenceDTO mapResourceReference(Model model, Statement s) {
        var ref = new ResourceReferenceDTO();
        ref.setProperty(DataModelMapperUtils.getCurie(s.getPredicate().asResource(), model));
        ref.setTarget(DataModelMapperUtils.getCurie(s.getObject().asResource(), model));

        var subj = DataModelUtils.findSubjectForObject(s, model);
        ref.setResourceURI(DataModelMapperUtils.uriToURIDTO(subj.getURI(), model));
        return ref;
    }

    private static Query getExistsQuery(List<DataModelURI> dependencies, Set<Resource> otherReferences) {
        var select = new SelectBuilder();
        var exprFactory = select.getExprFactory();
        var extVar = "?ext_subj";

        return select
                .addVar(extVar)
                .addVar(exprFactory.exists(
                                new WhereBuilder().addWhere(extVar, "?ext_pred", "?ext_obj")),
                        "?exists")
                .from(dependencies.stream().map(DataModelURI::getGraphURI).toList())
                .addWhereValueVar(extVar, otherReferences.toArray())
                .build();
    }

    private static ExtendedIterator<Statement> getInternalReferences(Model model) {
        return model.listStatements()
                .filterDrop(s -> SKIP_PREDICATES.contains(s.getPredicate()))
                .filterKeep(s -> s.getObject().isResource()
                                 && s.getObject().toString().startsWith(Constants.DATA_MODEL_NAMESPACE));
    }
}
