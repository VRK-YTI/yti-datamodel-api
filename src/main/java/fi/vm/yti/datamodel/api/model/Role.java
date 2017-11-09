package fi.vm.yti.datamodel.api.model;

public enum Role {

    ADMIN,
    DATA_MODEL_EDITOR,
    TERMINOLOGY_EDITOR,
    CODE_LIST_EDITOR;

    public static boolean contains(String roleString) {
        for (Role role : values()) {
            if (role.name().equals(roleString)) {
                return true;
            }
        }

        return false;
    }
}
