package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.validator.release.LibraryReferenceValidator;
import fi.vm.yti.datamodel.api.v2.validator.release.ReferencesExistsValidator;
import fi.vm.yti.datamodel.api.v2.validator.release.ReleaseValidator;
import org.springframework.stereotype.Service;

import java.util.*;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ReleaseValidationService {
    private final CoreRepository coreRepository;
    private final AuthorizationManager authorizationManager;
    private final List<ReleaseValidator> validators = new ArrayList<>();

    public ReleaseValidationService(CoreRepository coreRepository,
                                    AuthorizationManager authorizationManager,
                                    ReferencesExistsValidator referencesExistsValidator) {
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        validators
                .addAll(List.of(
                        referencesExistsValidator,
                        new LibraryReferenceValidator()));
    }

    public Map<String, Set<ResourceReferenceDTO>> validateRelease(String prefix) {
        var model = coreRepository.fetch(DataModelURI.createModelURI(prefix).getGraphURI());
        var result = new HashMap<String, Set<ResourceReferenceDTO>>();

        check(authorizationManager.hasRightToModel(prefix, model));

        validators.forEach(validator -> {
            var validateResult = validator.validate(model);
            if (!validateResult.isEmpty()) {
                result.put(validator.getErrorKey(), validateResult);
            }
        });

        return result;
    }
}
