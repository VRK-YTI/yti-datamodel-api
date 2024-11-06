package fi.vm.yti.datamodel.api.v2.security;

import fi.vm.yti.common.security.BaseAuthorizationManagerImpl;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import org.apache.jena.rdf.model.Model;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!junit")
public class DataModelAuthorizationManager extends BaseAuthorizationManagerImpl {

    public DataModelAuthorizationManager(AuthenticatedUserProvider userProvider) {
        super(userProvider);
    }

    public boolean hasRightToModel(String prefix, Model model) {
        var graphURI = DataModelURI.createModelURI(prefix).getGraphURI();
        return hasRightToModel(graphURI, model, Role.DATA_MODEL_EDITOR);
    }

    public boolean hasRightToDoMigration() {
        return getUser().isSuperuser();
    }

    public boolean hasRightToDropDatabase() {
        return getUser().isSuperuser();
    }
}
