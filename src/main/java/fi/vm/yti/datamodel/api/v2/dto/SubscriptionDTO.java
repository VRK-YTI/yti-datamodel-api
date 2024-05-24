package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class SubscriptionDTO {

    private String modelURI;
    private Map<String, String> label;
    private String subscriptionARN;

    public String getModelURI() {
        return modelURI;
    }

    public void setModelURI(String modelURI) {
        this.modelURI = modelURI;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getSubscriptionARN() {
        return subscriptionARN;
    }

    public void setSubscriptionARN(String subscriptionARN) {
        this.subscriptionARN = subscriptionARN;
    }
}
