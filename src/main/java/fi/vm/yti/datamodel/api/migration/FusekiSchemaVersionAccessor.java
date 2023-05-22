package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.migration.InitializationException;
import fi.vm.yti.migration.SchemaVersionAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FusekiSchemaVersionAccessor implements SchemaVersionAccessor {

    private static final Logger logger = LoggerFactory.getLogger(FusekiSchemaVersionAccessor.class.getName());
    private final JenaService jenaService;

    public FusekiSchemaVersionAccessor(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    @Override
    public boolean isInitialized() throws InitializationException {
        return jenaService.isVersionGraphInitialized();
    }

    @Override
    public void initialize() {
        logger.debug("Setting metamodel version to 0");
        setSchemaVersion(0);
    }

    @Override
    public int getSchemaVersion() {
        return jenaService.getVersionNumber();
    }

    @Override
    public void setSchemaVersion(int version) {
        jenaService.setVersionNumber(version);
    }
}
