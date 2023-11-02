package fi.vm.yti.datamodel.api.v2.dto.visualization;

import java.util.Set;

public class VisualizationResultDTO {
    Set<VisualizationNodeDTO> nodes;
    Set<VisualizationHiddenNodeDTO> hiddenNodes;

    public Set<VisualizationNodeDTO> getNodes() {
        return nodes;
    }

    public void setNodes(Set<VisualizationNodeDTO> nodes) {
        this.nodes = nodes;
    }

    public Set<VisualizationHiddenNodeDTO> getHiddenNodes() {
        return hiddenNodes;
    }

    public void setHiddenNodes(Set<VisualizationHiddenNodeDTO> hiddenNodes) {
        this.hiddenNodes = hiddenNodes;
    }
}
