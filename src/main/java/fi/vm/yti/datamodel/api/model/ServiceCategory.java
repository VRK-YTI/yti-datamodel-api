package fi.vm.yti.datamodel.api.model;

import java.util.List;

/**
 * Created by malonen on 14.11.2017.
 */
public enum ServiceCategory {

    AGRI,
    ECON,
    EDUC,
    ENER,
    ENVI,
    GOVE,
    HEAL,
    INTR,
    JUST,
    REGI,
    SOCI,
    TECH,
    TRAN;

    public static boolean contains(String serviceCategoryString) {
        for (ServiceCategory category : values()) {
            if (category.name().equals(serviceCategoryString)) {
                return true;
            }
        }

        return false;
    }

    public static boolean containsAll(List<String> services) {
        for(String serviceString : services) {
            if(!contains(serviceString)) {
                return false;
            }
        }
        return true;
    }

}
