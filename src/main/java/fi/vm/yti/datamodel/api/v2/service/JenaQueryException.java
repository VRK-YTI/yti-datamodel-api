package fi.vm.yti.datamodel.api.v2.service;

public class JenaQueryException extends RuntimeException{

    private static final String DEFAULT_MESSAGE = "Error querying graph";

    public JenaQueryException() {
        super(DEFAULT_MESSAGE);
    }

    public JenaQueryException(String message){
        super(message);
    }
}
