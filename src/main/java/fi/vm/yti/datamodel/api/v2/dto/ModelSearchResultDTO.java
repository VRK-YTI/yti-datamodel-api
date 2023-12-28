package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;

import java.util.List;

public class ModelSearchResultDTO extends IndexModel {

    public ModelSearchResultDTO() {
        this.setMatchingResources(new java.util.ArrayList<>());
    }

    private List<IndexResource> matchingResources;

    public List<IndexResource> getMatchingResources() {
        return matchingResources;
    }

    public void setMatchingResources(List<IndexResource> matchingResources) {
        this.matchingResources = matchingResources;
    }

    public void addMatchingResource(IndexResource resource) {
        this.matchingResources.add(resource);
    }
}
