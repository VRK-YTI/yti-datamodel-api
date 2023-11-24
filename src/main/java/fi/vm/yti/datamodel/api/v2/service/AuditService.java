package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.security.YtiUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuditService {

    private final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final String entityType;


    public AuditService(String entity) {
        this.entityType = entity;
    }

    public void log(ActionType type, String uri, YtiUser user) {
        if(user.isAnonymous()) {
            return;
        }
        logger.info("{} {}: <{}>, User[email={}, id={}]", entityType, type, uri, user.getEmail(), user.getId());
    }

    public enum ActionType {
        CREATE,
        UPDATE,
        DELETE,
        SAVE,
    }

}
