package fi.vm.yti.datamodel.api.v2.dto;

import java.util.LinkedList;
import java.util.List;

public class VisualizationAssociationDTO extends VisualizationItemDTO {

    private List<String> route = new LinkedList<>();

    public List<String> getRoute() {
        return route;
    }

    public void setRoute(List<String> route) {
        this.route = route;
    }

}
