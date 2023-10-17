package fi.vm.yti.datamodel.api.v2.dto;

public class VersionedModelDTO extends ModelMetaData {
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
