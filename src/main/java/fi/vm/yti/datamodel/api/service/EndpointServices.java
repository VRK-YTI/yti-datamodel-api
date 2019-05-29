package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;

import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.springframework.stereotype.Service;

@Service
public final class EndpointServices {

    private String endpoint;
    private String scheme;

    public EndpointServices(ApplicationProperties properties) {
        this.endpoint = properties.getEndpoint();
        this.scheme = properties.getDefaultScheme();
    }

    public RDFConnection getCoreConnection() {
        return RDFConnectionRemote.create().destination(endpoint + "/core/").build();
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public String getCoreReadAddress() {
        return endpoint + "/core/get";
    }

    public String getCoreReadWriteAddress() {
        return endpoint + "/core/data";
    }

    public String getCoreSparqlAddress() {
        return endpoint + "/core/sparql";
    }

    public String getSparqlAddress(String service) {
        return endpoint + "/" + service + "/sparql";
    }

    public String getSchemesReadAddress() {
        return endpoint + "/scheme/get";
    }

    public String getSchemesReadWriteAddress() {
        return endpoint + "/scheme/data";
    }

    public String getSchemesSparqlAddress() {
        return endpoint + "/scheme/sparql";
    }

    public String getLocalhostCoreSparqlAddress() {
        return "http://localhost:3030/core/sparql";
    }

    public String getCoreSparqlUpdateAddress() {
        return endpoint + "/core/update";
    }

    public String getSparqlUpdateAddress(String service) {
        return endpoint + "/" + service + "/update";
    }

    public String getTempConceptReadWriteAddress() {
        return endpoint + "/concept/data";
    }

    public String getTempConceptReadSparqlAddress() {
        return endpoint + "/concept/sparql";
    }

    public String getTempConceptSparqlUpdateAddress() {
        return endpoint + "/concept/update";
    }

    public String getProvReadWriteAddress() {
        return endpoint + "/prov/data";
    }

    public String getProvReadSparqlAddress() {
        return endpoint + "/prov/sparql";
    }

    public String getProvSparqlUpdateAddress() {
        return endpoint + "/prov/update";
    }

    public String getVocabExportAPI(String vocab) {
        return scheme + "rest/v1/" + vocab + "/data";
    }

    public String getImportsReadWriteAddress() {
        return endpoint + "/imports/data";
    }

    public String getImportsReadAddress() {
        return endpoint + "/imports/get";
    }

    public String getImportsSparqlAddress() {
        return endpoint + "/imports/sparql";
    }
}
