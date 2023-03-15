package fi.vm.yti.datamodel.api.v2.endpoint.error;

public class OpenSearchException extends RuntimeException{

    private final String index;

    public OpenSearchException(String message, String index){
        super(message);
        this.index = index;
    }

    public String getIndex() {
        return index;
    }
}
