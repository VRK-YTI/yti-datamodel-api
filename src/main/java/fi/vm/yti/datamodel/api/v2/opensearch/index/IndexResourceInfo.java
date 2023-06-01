package fi.vm.yti.datamodel.api.v2.opensearch.index;

import fi.vm.yti.datamodel.api.v2.dto.ResourceType;

import java.util.Map;

public class IndexResourceInfo extends IndexBase {
    private Map<String, String> note;
    private ResourceType resourceType;
    private String namespace;
    private String identifier;
    private ConceptInfo conceptInfo;
    private DatamodelInfo dataModelInfo;

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public ConceptInfo getConceptInfo() {
        return conceptInfo;
    }

    public void setConceptInfo(ConceptInfo conceptInfo) {
        this.conceptInfo = conceptInfo;
    }

    public DatamodelInfo getDataModelInfo() {
        return dataModelInfo;
    }

    public void setDataModelInfo(DatamodelInfo dataModelInfo) {
        this.dataModelInfo = dataModelInfo;
    }
}
