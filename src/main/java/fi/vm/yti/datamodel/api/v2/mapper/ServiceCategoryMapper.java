package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceCategoryMapper {

    private ServiceCategoryMapper(){
        //static class
    }

    public static Set<ServiceCategoryDTO> mapServiceCategoriesToDTO(Set<String> ids, Model serviceCategories) {
        return ids.stream().map(org -> {
            var res = serviceCategories.getResource(org);
            var labels = MapperUtils.localizedPropertyToMap(res, RDFS.label);
            var identifier = MapperUtils.propertyToString(res, SKOS.notation);
            if(identifier == null || labels.isEmpty()){
                throw new MappingError("Could not map Service category");
            }
            return new ServiceCategoryDTO(res.getURI(), labels, identifier);
        }).collect(Collectors.toSet());
    }

    public static List<ServiceCategoryDTO> mapToListServiceCategoryDTO(Model serviceCategoryModel) {
        var iterator = serviceCategoryModel.listResourcesWithProperty(RDF.type, FOAF.Group);
        List<ServiceCategoryDTO> result = new ArrayList<>();

        while (iterator.hasNext()) {
            var resource = iterator.next().asResource();
            var labels = MapperUtils.localizedPropertyToMap(resource, RDFS.label);
            var identifier = resource.getProperty(SKOS.notation).getObject().toString();
            result.add(new ServiceCategoryDTO(resource.getURI(), labels, identifier));
        }
        return result;
    }

}
