package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.DataModelInfoDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class DataModelService {

    private final CoreRepository coreRepository;
    private final AuthorizationManager authorizationManager;
    private final GroupManagementService groupManagementService;
    private final ModelMapper mapper;
    private final TerminologyService terminologyService;
    private final CodeListService codeListService;
    private final OpenSearchIndexer openSearchIndexer;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public DataModelService(CoreRepository coreRepository,
                            AuthorizationManager authorizationManager,
                            GroupManagementService groupManagementService,
                            ModelMapper modelMapper,
                            TerminologyService terminologyService,
                            CodeListService codeListService,
                            OpenSearchIndexer openSearchIndexer,
                            AuthenticatedUserProvider userProvider) {
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        this.groupManagementService = groupManagementService;
        this.mapper = modelMapper;
        this.terminologyService = terminologyService;
        this.codeListService = codeListService;
        this.openSearchIndexer = openSearchIndexer;
        this.userProvider = userProvider;
    }

    public DataModelInfoDTO get(String prefix) {
        var model = coreRepository.fetch(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        var hasRightsToModel = authorizationManager.hasRightToModel(prefix, model);

        var userMapper = hasRightsToModel ? groupManagementService.mapUser() : null;
        return mapper.mapToDataModelDTO(prefix, model, userMapper);
    }

    public URI create(DataModelDTO dto, ModelType modelType) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + dto.getPrefix();

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToJenaModel(dto, modelType, userProvider.getUser());

        coreRepository.put(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(dto.getPrefix(), jenaModel);
        openSearchIndexer.createModelToIndex(indexModel);
        return new URI(graphUri);
    }

    public void update(String prefix, DataModelDTO dto) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var oldModel = coreRepository.fetch(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        check(authorizationManager.hasRightToModel(graphUri, oldModel));

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToUpdateJenaModel(prefix, dto, oldModel, userProvider.getUser());

        coreRepository.put(ModelConstants.SUOMI_FI_NAMESPACE + prefix, jenaModel);

        var indexModel = mapper.mapToIndexModel(prefix, jenaModel);
        openSearchIndexer.updateModelToIndex(indexModel);
    }

    public void delete(String prefix) {
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!coreRepository.graphExists(modelUri)){
            throw new ResourceNotFoundException(modelUri);
        }
        var model = coreRepository.fetch(modelUri);
        check(authorizationManager.hasRightToModel(prefix, model));

        coreRepository.delete(modelUri);
        openSearchIndexer.deleteModelFromIndex(modelUri);
    }

    public boolean exists(String prefix) {
        if (ValidationConstants.RESERVED_WORDS.contains(prefix)) {
            return true;
        }
        return coreRepository.graphExists(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
    }

}
