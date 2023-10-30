package fi.vm.yti.datamodel.api.v2.dto.visualization;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PositionDTO {
    private Double x;
    private Double y;

    public PositionDTO(Double x, Double y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
