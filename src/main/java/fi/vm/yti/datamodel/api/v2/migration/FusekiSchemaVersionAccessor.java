package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.common.service.VersionService;
import fi.vm.yti.migration.InitializationException;
import fi.vm.yti.migration.SchemaVersionAccessor;
import org.springframework.stereotype.Service;

@Service
public class FusekiSchemaVersionAccessor implements SchemaVersionAccessor {

    private final VersionService versionService;

    public FusekiSchemaVersionAccessor(VersionService versionService) {
        this.versionService = versionService;
    }

    @Override
    public boolean isInitialized() throws InitializationException {
        return versionService.isVersionGraphInitialized();
    }

    @Override
    public void initialize() {
        setSchemaVersion(0);
    }

    @Override
    public int getSchemaVersion() {
        return versionService.getVersionNumber();
    }

    @Override
    public void setSchemaVersion(int version) {
        versionService.setVersionNumber(version);
    }
}
