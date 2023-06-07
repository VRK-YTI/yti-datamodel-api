package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Set;

public class ClassDTO extends BaseDTO {

    private Set<String> equivalentClass;
    private Set<String> subClassOf;

    public Set<String> getEquivalentClass() {
        return equivalentClass;
    }

    public void setEquivalentClass(Set<String> equivalentClass) {
        this.equivalentClass = equivalentClass;
    }

    public Set<String> getSubClassOf() {
        return subClassOf;
    }

    public void setSubClassOf(Set<String> subClassOf) {
        this.subClassOf = subClassOf;
    }

}
