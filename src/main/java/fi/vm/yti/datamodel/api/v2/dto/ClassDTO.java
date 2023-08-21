package fi.vm.yti.datamodel.api.v2.dto;

import java.util.HashSet;
import java.util.Set;

public class ClassDTO extends BaseDTO {

    private Set<String> equivalentClass = new HashSet<>();
    private Set<String> subClassOf = new HashSet<>();
    private Set<String> disjointWith = new HashSet<>();

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

    public Set<String> getDisjointWith() {
        return disjointWith;
    }

    public void setDisjointWith(Set<String> disjointWith) {
        this.disjointWith = disjointWith;
    }
}
