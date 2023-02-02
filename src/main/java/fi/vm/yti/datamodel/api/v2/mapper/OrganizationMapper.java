package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.GroupManagementOrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.Iow;

import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static fi.vm.yti.datamodel.api.v2.dto.ModelConstants.*;
import static fi.vm.yti.datamodel.api.v2.mapper.MapperUtils.getUUID;
import static fi.vm.yti.datamodel.api.v2.mapper.MapperUtils.localizedPropertyToMap;

public class OrganizationMapper {

    public static Model mapGroupManagementOrganizationToModel(List<GroupManagementOrganizationDTO> organizations) {
        var orgModel = ModelFactory.createDefaultModel();

        for (var organization : organizations) {
            var resource = orgModel.createResource(URN_UUID + organization.getUuid());
            resource.addProperty(RDF.type, FOAF.Organization);

            USED_LANGUAGES.forEach(lang -> {
                var label = organization.getPrefLabel().get(lang);
                var description = organization.getDescription().get(lang);

                if (StringUtils.isNotBlank(label)) {
                    resource.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(label, lang));
                }
                if (StringUtils.isNotBlank(description)) {
                    resource.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description, lang));
                }
            });

            if (StringUtils.isNotBlank(organization.getParentId())) {
                resource.addProperty(Iow.parentOrganization, URN_UUID + organization.getParentId());
            }
            if (StringUtils.isNotBlank(organization.getUrl())) {
                resource.addLiteral(FOAF.homepage, organization.getUrl());
            }
        }
        return orgModel;
    }

    public static List<OrganizationDTO> mapToListOrganizationDTO(Model organizationModel) {
        var iterator = organizationModel.listResourcesWithProperty(RDF.type, FOAF.Organization);
        List<OrganizationDTO> result = new ArrayList<>();

        while (iterator.hasNext()) {
            var resource = iterator.next().asResource();

            var labels = localizedPropertyToMap(resource, SKOS.prefLabel);
            var id = getUUID(resource.getURI());

            Statement stmt = resource.getProperty(Iow.parentOrganization);
            UUID parentId = stmt != null ? getUUID(stmt.getObject().toString()) : null;

            result.add(new OrganizationDTO(id.toString(), labels, parentId));
        }
        return result;
    }

}
