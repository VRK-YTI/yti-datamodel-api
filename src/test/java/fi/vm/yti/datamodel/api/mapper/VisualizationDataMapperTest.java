package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.visualization.*;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import fi.vm.yti.datamodel.api.v2.properties.Iow;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class VisualizationDataMapperTest {

    private final Map<String, String> libraryNamespaces = Map.of(
            ModelConstants.SUOMI_FI_NAMESPACE + "yti-model/", "yti-model",
            "https://www.example.com/ns/external/", "ext");

    private final Map<String, String> profileNamespaces = Map.of(
            ModelConstants.SUOMI_FI_NAMESPACE + "personprof/", "personprof",
            ModelConstants.SUOMI_FI_NAMESPACE + "ytm/", "ytm"
    );

    @Test
    void mapClassWithParent() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");

        var uri = DataModelURI.createResourceURI("visu", "natural-person");
        var classDTO = VisualizationMapper.mapClass(uri.getResourceURI(), model, libraryNamespaces);

        assertEquals("Luonnollinen henkilö", classDTO.getLabel().get("fi"));

        var parentClass = classDTO.getReferences().stream()
                .filter(r -> r.getReferenceType().equals(VisualizationReferenceType.PARENT_CLASS))
                .findFirst();

        assertTrue(parentClass.isPresent());

        assertEquals("person", parentClass.get().getIdentifier());
        assertEquals(VisualizationReferenceType.PARENT_CLASS, parentClass.get().getReferenceType());

        assertEquals(VisualizationNodeType.CLASS, classDTO.getType());
        assertEquals("natural-person", classDTO.getIdentifier());
    }

    @Test
    void mapLibraryAttributesAndAssociations() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");
        var classResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "visu" + ModelConstants.RESOURCE_SEPARATOR + "person");
        var externalResources = new HashSet<Resource>();
        var classDTO = new VisualizationClassDTO();
        VisualizationMapper.mapLibraryClassResources(classDTO, model, classResource, externalResources, libraryNamespaces);

        var attributes = classDTO.getAttributes();
        var associations = classDTO.getAssociations();

        assertEquals(1, attributes.size());
        assertEquals(2, associations.size());

        var attributeAge = attributes.get(0);
        var associationAddress = associations.stream()
                .filter(a -> a.getIdentifier().equals("is-address"))
                .findFirst();

        assertTrue(associationAddress.isPresent());
        assertEquals("age", attributeAge.getIdentifier());
        assertEquals("Ikä", attributeAge.getLabel().get("fi"));
        assertEquals("xsd:integer", attributeAge.getDataType());

        assertEquals("is-address", associationAddress.get().getIdentifier());
        assertEquals(VisualizationReferenceType.ASSOCIATION, associationAddress.get().getReferenceType());
        assertEquals("onOsoite", associationAddress.get().getLabel().get("fi"));
        assertEquals("address", associationAddress.get().getReferenceTarget());
    }

    @Test
    void mapAttributesWithDomain() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");

        var attributes = VisualizationMapper.mapAttributesWithDomain(model);

        assertEquals(1, attributes.size());

        var attribute = attributes.get(0);

        assertEquals("name", attribute.getIdentifier());
        assertEquals("Nimi", attribute.getLabel().get("fi"));
        assertEquals(VisualizationNodeType.ATTRIBUTE, attribute.getType());
        assertEquals(1, attribute.getReferences().size());
        assertEquals("xsd:string", attribute.getDataType());

        var reference = attribute.getReferences().iterator().next();
        assertEquals("person", reference.getReferenceTarget());
        assertNull(reference.getLabel());
    }

    @Test
    void mapAssociationsWithDomain() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");
        var classResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + "visu" + ModelConstants.RESOURCE_SEPARATOR + "address");
        var classDTO = new VisualizationClassDTO();
        VisualizationMapper.mapAssociationsWithDomain(classDTO, model, classResource, libraryNamespaces);

        assertEquals(1, classDTO.getReferences().size());

        var reference = classDTO.getReferences().iterator().next();

        assertEquals("city", reference.getReferenceTarget());
        assertEquals("onKaupunki", reference.getLabel().get("fi"));
        assertEquals(VisualizationReferenceType.ASSOCIATION, reference.getReferenceType());
    }

    @Test
    void mapLibraryClassWithExternalResources() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");
        var classURI = ModelConstants.SUOMI_FI_NAMESPACE + "visu" + ModelConstants.RESOURCE_SEPARATOR + "person";
        var externalResources = new HashSet<Resource>();

        var classDTO = VisualizationMapper.mapClass(classURI, model, libraryNamespaces);
        VisualizationMapper.mapLibraryClassResources(classDTO, model, model.getResource(classURI), externalResources, libraryNamespaces);

        // should have one parent class reference to external class
        assertEquals(1, classDTO.getReferences().size());
        // should have one association restriction
        assertEquals(2, classDTO.getAssociations().size());
        // should have one reference to external association (will be mapped later)
        assertEquals("https://www.example.com/ns/external/someTarget",
                MapperUtils.propertyToString(externalResources.iterator().next().asResource(), OWL.someValuesFrom));

        var parentRef = classDTO.getReferences().stream()
                .filter(ref -> ref.getReferenceType().equals(VisualizationReferenceType.PARENT_CLASS))
                .findFirst();
        var associationRef = classDTO.getAssociations().stream()
                .filter(ref -> ref.getReferenceTarget().equals("ext:address"))
                .findFirst();

        assertTrue(parentRef.isPresent());
        assertTrue(associationRef.isPresent());

        assertEquals("ext:Person", parentRef.get().getReferenceTarget());
        assertNull(parentRef.get().getLabel());
    }

    @Test
    void mapExternalClass() {
        String identifier = "ext:TestClass";

        var external = VisualizationMapper.mapExternalClass(identifier, Set.of("fi", "en"));

        assertEquals(identifier, external.getIdentifier());
        assertEquals(Map.of("fi", identifier, "en", identifier), external.getLabel());
    }

    @Test
    void mapApplicationProfileClass() {
        var model = MapperTestUtils.getModelFromFile("/models/test_application_profile_visualization.ttl");
        var classURI = DataModelURI.createResourceURI("visuprof", "person").getResourceURI();
        var externalResources = new HashSet<Resource>();

        var classDTO = (VisualizationNodeShapeDTO) VisualizationMapper
                .mapClass(classURI, model, profileNamespaces);
        VisualizationMapper.mapNodeShapeResources(classDTO, model.getResource(classURI), model, externalResources, profileNamespaces);

        assertEquals("person", classDTO.getIdentifier());
        assertEquals("Henkilö", classDTO.getLabel().get("fi"));
        assertEquals("ytm:person", classDTO.getTargetClass());

        assertEquals(2, classDTO.getAttributes().size());
        assertEquals(1, classDTO.getAssociations().size());
        assertEquals(ModelConstants.SUOMI_FI_NAMESPACE + "personprof/ext-attr", externalResources.iterator().next().getURI());

        var mappedAttribute = findItem(classDTO.getAttributes(), "age", VisualizationPropertyShapeAttributeDTO.class);
        var mappedAssociation = findItem(classDTO.getAssociations(), "is-address", VisualizationPropertyShapeAssociationDTO.class);

        assertNotNull(mappedAttribute);
        assertNotNull(mappedAssociation);

        assertEquals("Ikä", mappedAttribute.getLabel().get("fi"));
        assertEquals("age", mappedAttribute.getIdentifier());
        assertEquals("xsd:integer", mappedAttribute.getDataType());
        // assertEquals("ytm:age", mappedAttribute.getPath());
        assertEquals(1, mappedAttribute.getMinCount());
        assertEquals(1, mappedAttribute.getMaxCount());

        assertEquals("onOsoite", mappedAssociation.getLabel().get("fi"));
        assertEquals("is-address", mappedAssociation.getIdentifier());
        assertEquals(1, mappedAssociation.getMinCount());
        assertEquals(2, mappedAssociation.getMaxCount());
        assertEquals("address", mappedAssociation.getReferenceTarget());
    }

    @Test
    void mapPositionDataToGraph() {
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model" + ModelConstants.RESOURCE_SEPARATOR;

        var position = new PositionDataDTO();
        position.setIdentifier("class-1");
        position.setX(3.0);
        position.setY(5.0);
        position.setReferenceTargets(Set.of("class-2"));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI, List.of(position));

        var resource = positionModel.getResource(positionGraphURI + position.getIdentifier());

        assertEquals("class-1", resource.getProperty(DCTerms.identifier).getString());
        assertEquals(3.0, resource.getProperty(Iow.posX).getLiteral().getDouble());
        assertEquals(5.0, resource.getProperty(Iow.posY).getLiteral().getDouble());
        assertEquals("class-2", resource.listProperties(Iow.referenceTarget).toList().get(0).getString());
    }

    @Test
    void mapPositionDataWithParentClasses() {
        var modelPrefix = "test-model";
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix + ModelConstants.RESOURCE_SEPARATOR;

        var node1 = new PositionDataDTO();
        node1.setIdentifier("class-1");
        node1.setX(1.0);
        node1.setY(2.0);
        node1.setReferenceTargets(Set.of("class-2"));

        var node2 = new PositionDataDTO();
        node2.setIdentifier("class-2");
        node2.setX(0.0);
        node2.setY(10.0);

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI,
                List.of(node1, node2));

        var classDTO1 = new VisualizationClassDTO();
        classDTO1.setIdentifier("class-1");
        classDTO1.setReferences(Set.of(getReference("class-2", VisualizationReferenceType.PARENT_CLASS)));

        var classDTO2 = new VisualizationClassDTO();
        classDTO2.setIdentifier("class-2");

        var classes = new HashSet<VisualizationNodeDTO>();
        classes.add(classDTO1);
        classes.add(classDTO2);

        var hiddenNodes = VisualizationMapper.mapPositionsDataToDTOsAndCreateHiddenNodes(positionModel, modelPrefix, classes);
        assertEquals(0, hiddenNodes.size());

        var c1 = findItem(classes, "class-1", VisualizationClassDTO.class);
        assertEquals(1.0, c1.getPosition().getX());
        assertEquals(2.0, c1.getPosition().getY());

        assertEquals(2, classes.size());
        assertEquals("class-2", c1.getReferences().iterator().next().getIdentifier());
    }

    @Test
    void mapPositionDataWithParentClassesAndHiddenNode() {
        var modelPrefix = "test-model";
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix + ModelConstants.RESOURCE_SEPARATOR;

        var node1 = new PositionDataDTO();
        node1.setIdentifier("class-1");
        node1.setX(1.0);
        node1.setY(2.0);
        node1.setReferenceTargets(Set.of("corner-1"));

        var node2 = new PositionDataDTO();
        node2.setIdentifier("class-2");
        node2.setX(0.0);
        node2.setY(10.0);

        var corner1 = new PositionDataDTO();
        corner1.setIdentifier("corner-1");
        corner1.setX(0.0);
        corner1.setY(0.0);
        corner1.setReferenceTargets(Set.of("class-2"));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI,
                List.of(node1, node2, corner1));

        var classDTO1 = new VisualizationClassDTO();
        classDTO1.setIdentifier("class-1");
        classDTO1.setReferences(Set.of(getReference("class-2", VisualizationReferenceType.PARENT_CLASS)));

        var classDTO2 = new VisualizationClassDTO();
        classDTO2.setIdentifier("class-2");

        var classes = new HashSet<VisualizationNodeDTO>();
        classes.add(classDTO1);
        classes.add(classDTO2);

        var hiddenNodes = VisualizationMapper.mapPositionsDataToDTOsAndCreateHiddenNodes(positionModel, modelPrefix, classes);

        var c1 = findItem(classes, "class-1", VisualizationClassDTO.class);
        var hidden = findHiddenNode(hiddenNodes, "corner-1");
        assertNotNull(hidden);

        assertEquals(2, classes.size());
        assertEquals(1, hiddenNodes.size());
        assertEquals("corner-1", c1.getReferences().iterator().next().getReferenceTarget());
        assertEquals("class-2", hidden.getReferenceTarget());
    }

    @Test
    void mapPositionDataWithParentClassesAndAssociationsAndHiddenNodes() {
        var modelPrefix = "test-model";
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix + ModelConstants.RESOURCE_SEPARATOR;

        // class-1 has parent class reference to class-2 via corner-1 and corner-2
        // class-1 has association to class-3 via corner-3
        // class-2 has parent class reference to class-3 directly
        var node1 = new PositionDataDTO();
        node1.setIdentifier("class-1");
        node1.setX(1.0);
        node1.setY(2.0);
        node1.setReferenceTargets(Set.of("corner-1", "corner-3"));

        var node2 = new PositionDataDTO();
        node2.setIdentifier("class-2");
        node2.setX(0.0);
        node2.setY(10.0);
        node2.setReferenceTargets(Set.of("class-3"));

        var node3 = new PositionDataDTO();
        node3.setIdentifier("class-3");
        node3.setX(20.0);
        node3.setY(15.0);

        var corner1 = new PositionDataDTO();
        corner1.setX(6.0);
        corner1.setY(7.0);
        corner1.setIdentifier("corner-1");
        corner1.setReferenceTargets(Set.of("corner-2"));

        var corner2 = new PositionDataDTO();
        corner2.setX(3.0);
        corner2.setY(6.0);
        corner2.setIdentifier("corner-2");
        corner2.setReferenceTargets(Set.of("class-2"));

        var corner3 = new PositionDataDTO();
        corner3.setX(3.0);
        corner3.setY(6.0);
        corner3.setIdentifier("corner-3");
        corner3.setReferenceTargets(Set.of("class-3"));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI,
                List.of(node1, node2, node3, corner1, corner2, corner3));

        var class1AssociationRef = getReference("class-3", VisualizationReferenceType.ASSOCIATION);
        var class1ParentRef = getReference("class-2", VisualizationReferenceType.PARENT_CLASS);

        var classDTO1 = new VisualizationClassDTO();
        classDTO1.setIdentifier("class-1");
        classDTO1.setReferences(Set.of(class1ParentRef, class1AssociationRef));

        var class2ParentRef = getReference("class-3", VisualizationReferenceType.PARENT_CLASS);
        var classDTO2 = new VisualizationClassDTO();
        classDTO2.setIdentifier("class-2");
        classDTO2.setReferences(Set.of(class2ParentRef));

        var classDTO3 = new VisualizationClassDTO();
        classDTO3.setIdentifier("class-3");

        var classes = new HashSet<VisualizationNodeDTO>();
        classes.add(classDTO1);
        classes.add(classDTO2);
        classes.add(classDTO3);

        var hiddenNodes = VisualizationMapper.mapPositionsDataToDTOsAndCreateHiddenNodes(positionModel, modelPrefix, classes);

        // should contain three classes and three hidden nodes
        assertEquals(3, classes.size());
        assertEquals(3, hiddenNodes.size());

        var identifiers = hiddenNodes.stream().map(VisualizationHiddenNodeDTO::getIdentifier).toList();
        assertTrue(identifiers.containsAll(List.of("corner-1", "corner-2", "corner-3")));

        var classItem = findItem(classes, "class-1", VisualizationNodeDTO.class);
        var classItem2 =  findItem(classes, "class-2", VisualizationNodeDTO.class);
        assertEquals(2, classItem.getReferences().size());
        assertEquals(1, classItem2.getReferences().size());

        var ref1 = findItem(classItem.getReferences(), "class-3", VisualizationReferenceDTO.class);
        var ref2 = findItem(classItem.getReferences(), "class-2", VisualizationReferenceDTO.class);
        var ref3 = findItem(classItem2.getReferences(), "class-3", VisualizationReferenceDTO.class);

        // class1 -> corner-3
        assertEquals("corner-3", ref1.getReferenceTarget());
        // class1 -> corner-1
        assertEquals("corner-1", ref2.getReferenceTarget());
        // class-2 -> class-3
        assertEquals("class-3", ref3.getReferenceTarget());

        // assertEquals("corner-1", classItem.getReferences().iterator().next().getReferenceTarget());
        assertEquals(1.0, classItem.getPosition().getX());
        assertEquals(2.0, classItem.getPosition().getY());

        var cornerItem1 = findHiddenNode(hiddenNodes, "corner-1");
        assertNotNull(cornerItem1);
        assertEquals(6.0, cornerItem1.getPosition().getX());
        assertEquals(7.0, cornerItem1.getPosition().getY());
        assertEquals("corner-2", cornerItem1.getReferenceTarget());

        var cornerItem2 = findHiddenNode(hiddenNodes, "corner-2");
        assertNotNull(cornerItem2);
        assertEquals("class-2", cornerItem2.getReferenceTarget());

        var cornerItem3 = findHiddenNode(hiddenNodes, "corner-3");
        assertNotNull(cornerItem3);
        assertEquals("class-3", cornerItem3.getReferenceTarget());
    }

    @Test
    void mapPositionDataAssociationRecursive() {
        var modelPrefix = "test-model";
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix + ModelConstants.RESOURCE_SEPARATOR;

        var association = new VisualizationReferenceDTO();
        association.setReferenceTarget("class-1");
        association.setIdentifier("assoc-1");

        var node1 = new PositionDataDTO();
        node1.setIdentifier("class-1");
        node1.setX(1.0);
        node1.setY(2.0);
        node1.setReferenceTargets(Set.of("corner-1"));

        var corner1 = new PositionDataDTO();
        corner1.setX(6.0);
        corner1.setY(7.0);
        corner1.setIdentifier("corner-1");
        corner1.setReferenceTargets(Set.of("corner-2"));

        var corner2 = new PositionDataDTO();
        corner2.setX(6.0);
        corner2.setY(7.0);
        corner2.setIdentifier("corner-2");
        corner2.setReferenceTargets(Set.of("class-1"));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI,
                List.of(node1, corner1, corner2));

        var classDTO1 = new VisualizationClassDTO();
        classDTO1.setIdentifier("class-1");
        classDTO1.setAssociations(List.of(association));

        var classes = new HashSet<VisualizationNodeDTO>();
        classes.add(classDTO1);

        var hiddenNodes = VisualizationMapper.mapPositionsDataToDTOsAndCreateHiddenNodes(positionModel, modelPrefix, classes);

        assertEquals(1, classes.size());
        assertEquals(2, hiddenNodes.size());
    }

    private static VisualizationReferenceDTO getReference(String target, VisualizationReferenceType type) {
        var ref = new VisualizationReferenceDTO();
        ref.setIdentifier(target);
        ref.setReferenceTarget(target);
        ref.setReferenceType(type);

        return ref;
    }

    private static VisualizationHiddenNodeDTO findHiddenNode(Set<VisualizationHiddenNodeDTO> hiddenNodes, String identifier) {
        return hiddenNodes.stream().filter(c -> c.getIdentifier().equals(identifier)).findFirst().orElse(null);
    }

    private static <T extends VisualizationItemDTO> T findItem(Collection<? extends VisualizationItemDTO> references,
                                                               String identifier,
                                                               Class<T> type) {
        var ref = references.stream().filter(c -> c.getIdentifier().equals(identifier)).findFirst();
        assertTrue(ref.isPresent(), "Item not present: " + identifier);
        return type.cast(ref.get());
    }
}

