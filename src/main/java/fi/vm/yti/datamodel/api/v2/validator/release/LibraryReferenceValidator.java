package fi.vm.yti.datamodel.api.v2.validator.release;

import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.vocabulary.SH;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LibraryReferenceValidator extends ReleaseValidator {

    @Override
    public Set<ResourceReferenceDTO> validate(Model model) {
        Set<ResourceReferenceDTO> invalidResources = new HashSet<>();
        var typeStmt = model.listStatements(null, RDF.type, SuomiMeta.ApplicationProfile);

        if (typeStmt.hasNext()) {
            invalidResources.addAll(getInvalidResources(SH.targetClass, OWL.Thing, model));
            invalidResources.addAll(getInvalidResources(SH.path, OWL2.topDataProperty, model));
            invalidResources.addAll(getInvalidResources(SH.path, OWL2.topObjectProperty, model));
        }
        return invalidResources;
    }

    @Override
    public String getErrorKey() {
        return "missing-reference-to-library";
    }

    private List<ResourceReferenceDTO> getInvalidResources(Property property, Resource object, Model model) {
        return model.listSubjectsWithProperty(property, object)
                .mapWith(resource -> {
                    var dto = new ResourceReferenceDTO();
                    dto.setResourceURI(DataModelMapperUtils.uriToURIDTO(resource.getURI(), model));
                    dto.setProperty(DataModelMapperUtils.getCurie(property, model));
                    dto.setTarget(DataModelMapperUtils.getCurie(object, model));
                    return dto;
                }).toList();
    }
}
