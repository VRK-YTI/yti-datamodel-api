package fi.vm.yti.datamodel.api.v2.dto.visualization;

import java.util.Set;

public class VisualizationPropertyShapeAttributeDTO extends VisualizationAttributeDTO {

    private Integer minCount;
    private Integer maxCount;
    private Set<String> codeLists;

    public Integer getMinCount() {
        return minCount;
    }

    public void setMinCount(Integer minCount) {
        this.minCount = minCount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public Set<String> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(Set<String> codeLists) {
        this.codeLists = codeLists;
    }
}
