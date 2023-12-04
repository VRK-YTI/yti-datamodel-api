package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.dto.visualization.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.stream.Collectors;

public class V1DataMapper {

    private V1DataMapper() {}

    private static final Property DOCUMENTATION =
            ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#documentation");

    private static final Property POINT_XY =
            ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#pointXY");

    public static DataModelDTO getLibraryMetadata(String modelURI, Model oldData, Model serviceCategories, String defaultOrganization) {
        var modelResource = oldData.getResource(modelURI);
        var dto = new DataModelDTO();
        dto.setPrefix(MapperUtils.getLiteral(modelResource, DCAP.preferredXMLNamespacePrefix, String.class));
        dto.setLabel(MapperUtils.localizedPropertyToMap(modelResource, RDFS.label));
        dto.setDescription(MapperUtils.localizedPropertyToMap(modelResource, RDFS.comment));

        var groups = new HashSet<String>();
        MapperUtils.arrayPropertyToSet(modelResource, DCTerms.isPartOf)
                .forEach(g -> groups.add(MapperUtils.propertyToString(serviceCategories.getResource(g), SKOS.notation)));
        dto.setGroups(groups);

        if (defaultOrganization == null || defaultOrganization.isBlank()) {
            var org = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor).stream()
                .map(o -> UUID.fromString(o.replace("urn:uuid:", "")))
                .collect(Collectors.toSet());
            dto.setOrganizations(org);
        } else {
            dto.setOrganizations(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));
        }

        var languages = MapperUtils.getList(oldData, modelResource, DCTerms.language)
                .asJavaList().stream()
                .map(l -> l.asLiteral().getString())
                .collect(Collectors.toSet());
        dto.setLanguages(languages);

        dto.setTerminologies(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.references));

        var internalNamespaces = new HashSet<String>();
        var externalNamespaces = new HashSet<ExternalNamespaceDTO>();

        MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires)
                .forEach(ns -> {
                    if (!ns.contains("uri.suomi.fi")) {
                        var extResource = oldData.getResource(ns);
                        var ext = new ExternalNamespaceDTO();
                        ext.setNamespace(ns);
                        ext.setName(MapperUtils.localizedPropertyToMap(extResource, RDFS.label));
                        ext.setPrefix(MapperUtils.propertyToString(extResource, DCAP.preferredXMLNamespacePrefix));
                        externalNamespaces.add(ext);
                    }
                });

        dto.setInternalNamespaces(internalNamespaces);
        dto.setExternalNamespaces(externalNamespaces);

        if (modelResource.hasProperty(DCTerms.relation)) {
            var links = MapperUtils.getList(oldData, modelResource, DCTerms.relation).asJavaList().stream().map(link -> {
                var linkDTO = new LinkDTO();
                linkDTO.setUri(MapperUtils.propertyToString(link.asResource(), FOAF.homepage));
                linkDTO.setName(MapperUtils.localizedPropertyToMap(link.asResource(), DCTerms.title));
                linkDTO.setDescription(MapperUtils.localizedPropertyToMap(link.asResource(), DCTerms.description));
                return linkDTO;
            }).collect(Collectors.toSet());
            dto.setLinks(links);
        }

        dto.setDocumentation(MapperUtils.localizedPropertyToMap(modelResource, DOCUMENTATION));

        return dto;
    }

    public static ClassDTO mapLibraryClass(Resource classResource) {
        var dto = new ClassDTO();
        mapCommon(dto, classResource);
        dto.setLabel(MapperUtils.localizedPropertyToMap(classResource, SH.name));
        dto.setNote(MapperUtils.localizedPropertyToMap(classResource, SH.description));
        var subClasses = MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf).stream()
                .map(V1DataMapper::fixNamespace)
                .collect(Collectors.toSet());
        dto.setSubClassOf(subClasses);
        return dto;
    }

    public static ResourceDTO mapLibraryResource(Resource resource) {
        var dto = new ResourceDTO();
        mapCommon(dto, resource);
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
        var range = fixNamespace(MapperUtils.propertyToString(resource, RDFS.range));

        if (MapperUtils.hasType(resource, OWL.DatatypeProperty) && range != null) {
            range = range.replace(ModelConstants.PREFIXES.get("rdf"), "rdf:");
            range = range.replace(ModelConstants.PREFIXES.get("rdfs"), "rdfs:");
            range = range.replace(ModelConstants.PREFIXES.get("owl"), "owl:");
        }
        dto.setRange(range);
        return dto;
    }

    public static List<Resource> findResourcesByType(Model oldData, Resource type) {
            return oldData.listSubjectsWithProperty(RDF.type, type)
                    .mapWith(s -> oldData.getResource(s.getURI()))
                    .toList();
    }

    public static List<PositionDataDTO> mapPositions(String modelURI, Model oldVisualization) {
        var positions = new ArrayList<PositionDataDTO>();

        oldVisualization.listSubjectsWithProperty(RDF.type, RDFS.Class).forEach(subj -> {
            var dto = new PositionDataDTO();
            var uri = NodeFactory.createURI(subj.getURI());

            if (uri.toString().startsWith(modelURI)) {
                dto.setIdentifier(uri.getLocalName());
            } else {
                var prefix = MapperUtils.getModelIdFromNamespace(uri.getNameSpace().replace("#", ""));
                dto.setIdentifier(prefix + ":" + uri.getLocalName());
            }
            var coordinates = MapperUtils.propertyToString(subj, POINT_XY);
            if (coordinates == null) {
                return;
            }
            var xy = coordinates.split(",");
            dto.setX(Double.parseDouble(xy[0]));
            dto.setY(Double.parseDouble(xy[1]));

            positions.add(dto);
        });

        return positions;
    }

    private static void mapCommon(BaseDTO dto, Resource resource) {
        dto.setIdentifier(resource.getLocalName());
        dto.setEditorialNote(MapperUtils.propertyToString(resource, SKOS.editorialNote));
        dto.setSubject(MapperUtils.propertyToString(resource, DCTerms.subject));
    }

    private static String fixNamespace(String uri) {
        if (uri != null && uri.contains("uri.suomi.fi")) {
            return uri.replace("#", ModelConstants.RESOURCE_SEPARATOR);
        } else if (uri != null) {
            return uri.replace("http://www.w3.org/2001/XMLSchema#", "xsd:");
        }
        return uri;
    }
}