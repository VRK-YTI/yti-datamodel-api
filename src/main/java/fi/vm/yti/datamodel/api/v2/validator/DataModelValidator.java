package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.beans.factory.annotation.Autowired;

public class DataModelValidator extends BaseValidator implements
        ConstraintValidator<ValidDatamodel, DataModelDTO> {

    @Autowired
    private CoreRepository coreRepository;

    boolean updateModel;
    GraphType modelType;

    @Override
    public void initialize(ValidDatamodel constraintAnnotation) {
        updateModel = constraintAnnotation.updateModel();
        modelType = constraintAnnotation.modelType();
    }

    @Override
    public boolean isValid(DataModelDTO dataModel, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkModelPrefix(context, dataModel);
        checkLanguages(context, dataModel);
        checkLabels(context, dataModel);
        checkDescription(context, dataModel);
        checkOrganizations(context, dataModel);
        checkGroups(context, dataModel);
        checkContact(context, dataModel);
        checkDocumentation(context, dataModel);
        checkLinks(context, dataModel);

        checkInternalNamespaces(context, dataModel);
        checkExternalNamespaces(context, dataModel);
        checkStatus(context, dataModel);

        checkTerminologies(context, dataModel);
        checkCodeLists(context, dataModel, modelType);
        return !isConstraintViolationAdded();
    }

    /**
     * Check if prefix is valid, if updating prefix cannot be set
     * @param context Constraint validator context
     * @param dataModel DataModel
     */
    private void checkModelPrefix(ConstraintValidatorContext context, DataModelDTO dataModel){
        final var prefixPropertyLabel = "prefix";
        var prefix = dataModel.getPrefix();

        checkPrefix(context, prefix, prefixPropertyLabel, updateModel);
        // Check prefix text content
        checkPrefixContent(context, prefix, prefixPropertyLabel, false);

        if (coreRepository.graphExists(DataModelURI.createModelURI(prefix).getGraphURI())) {
            // Checking if in use is different for data models and its resources so it is not in the above function
            addConstraintViolation(context, "prefix-in-use", prefixPropertyLabel);
        }
    }

    /**
     * Check if languages are valid
     * @param context Constraint validator context
     * @param dataModel Data model
     */
    private void checkLanguages(ConstraintValidatorContext context, DataModelDTO dataModel){
        var languages = dataModel.getLanguages();

        if (languages.isEmpty()) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "languages");
            return;
        }

        checkLanguageTags(context, languages, "languages");
    }

    /**
     * Check if labels are valid
     * @param context Constraint validator context
     * @param dataModel Data Model
     */
    private void checkLabels(ConstraintValidatorContext context, DataModelDTO dataModel){
        final var labelPropertyLabel = "label";
        var labels = dataModel.getLabel();
        var languages = dataModel.getLanguages();

        if (labels == null || labels.isEmpty() || labels.values().stream().anyMatch(label -> label == null || label.isBlank())) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "label");
        } else if(labels.size() != languages.size()){
            addConstraintViolation(context, "label-language-count-mismatch", labelPropertyLabel);
        } else {
            labels.forEach((key, value) -> {
                if (!languages.contains(key)) {
                    addConstraintViolation(context, "language-not-in-language-list." + key, labelPropertyLabel);
                }
                checkCommonTextField(context, value, labelPropertyLabel);
            });
        }
    }

    /**
     * Check if descriptions are valid
     * @param context Constraint validator context
     * @param dataModel Data model
     */
    private void checkDescription(ConstraintValidatorContext context, DataModelDTO dataModel){
        var description = dataModel.getDescription();
        var languages = dataModel.getLanguages();
        description.forEach((key, value) -> {
            if (!languages.contains(key)) {
                addConstraintViolation(context, "language-not-in-language-list." + key, "description");
            }
            checkCommonTextArea(context, value, "description");
        });
    }

    /**
     * Check if organizations are valid
     * @param context Constraint validator context
     * @param dataModel DataModel
     */
    private void checkOrganizations(ConstraintValidatorContext context, DataModelDTO dataModel){
        var organizations = dataModel.getOrganizations();
        if(organizations.isEmpty()){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "organization");
            return;
        }
        var existingOrgs = coreRepository.getOrganizations();
        organizations.forEach(org -> {
            var queryRes = ResourceFactory.createResource(Constants.URN_UUID + org.toString());
            if(!existingOrgs.containsResource(queryRes)){
                addConstraintViolation(context, "does-not-exist." + org, "organizations");
            }
        });
    }

    /**
     * Check if groups are valid
     * @param context Constraint validator context
     * @param dataModel DataModel
    */
    private void checkGroups(ConstraintValidatorContext context, DataModelDTO dataModel){
        var groups = dataModel.getGroups();
        if(groups.isEmpty()){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "groups");
            return;
        }
        var existingGroups = coreRepository.getServiceCategories();
        groups.forEach(group -> {
            var resources = existingGroups.listResourcesWithProperty(SKOS.notation, group);
            if (!resources.hasNext()) {
                addConstraintViolation(context, "does-not-exist." + group, "groups");
            }
        });
    }

    /**
     * Check contact,
     * @param context Constraint validator context
     * @param dataModel DataModel
     */
    private void checkContact(ConstraintValidatorContext context, DataModelDTO dataModel){
        var contact = dataModel.getContact();
        if(contact != null && contact.length() > ValidationConstants.EMAIL_FIELD_MAX_LENGTH ) {
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT, "contact");
        }
    }


    /**
     * Check if internal namespaces are valid
     * @param context Constrain validator context
     * @param dataModel Data model
     */
    private void checkInternalNamespaces(ConstraintValidatorContext context, DataModelDTO dataModel){
        var namespaces = dataModel.getInternalNamespaces();
        if(namespaces.stream().anyMatch(ns -> !ns.startsWith(Constants.DATA_MODEL_NAMESPACE))){
            addConstraintViolation(context, "namespace-not-internal", "internalNamespaces");
        }
    }

    /**
     * Check if external namespaces are valid
     * NOTE: Due to the nature of how updating works we cannot check the profile type here
     * @param context Constraint validator context
     * @param dataModel Data model
     */
    private void checkExternalNamespaces(ConstraintValidatorContext context, DataModelDTO dataModel){
        var namespaces = dataModel.getExternalNamespaces();
        var externalNamespace = "externalNamespaces";
        namespaces.forEach(namespace -> {
            checkRequiredLocalizedValue(context, namespace.getName(), "namespace.name");
            if(namespace.getPrefix() == null || namespace.getNamespace() == null){
                addConstraintViolation(context, "namespace-missing-value", externalNamespace);
            }else {
                checkPrefix(context, namespace.getPrefix(), externalNamespace, false);
                if(namespace.getNamespace().startsWith(Constants.DATA_MODEL_NAMESPACE)){
                    addConstraintViolation(context, "namespace-not-external", externalNamespace);
                }
                checkPrefixContent(context, namespace.getPrefix(), externalNamespace, true);
            }
        });
    }

    /**
     * Helper function for checking prefix contents
     * @param context Constraint validator context
     * @param prefix Prefix
     * @param property Property name if violation happens
     * @param externalNamespace if true, allow using reserved prefixes, e.g. 'owl'
     */
    private void checkPrefixContent(ConstraintValidatorContext context, String prefix, String property, boolean externalNamespace){
        if(prefix == null){
            return;
        }
        //Checking for reserved words and reserved namespaces. This error won't distinguish which one it was
        if(ValidationConstants.RESERVED_WORDS.contains(prefix)
                || (!externalNamespace && ValidationConstants.RESERVED_NAMESPACES.containsKey(prefix))) {
            addConstraintViolation(context, "prefix-is-reserved", property);
        }
    }

    private void checkTerminologies(ConstraintValidatorContext context, DataModelDTO dataModel) {
        if (!dataModel.getTerminologies().stream().allMatch(uri -> uri.matches("^https?://iri.suomi.fi/terminology/(.*)"))) {
            addConstraintViolation(context, "invalid-terminology-uri", "terminologies");
        }
    }

    private void checkCodeLists(ConstraintValidatorContext context, DataModelDTO dataModel, GraphType modelType) {
        if(modelType.equals(GraphType.LIBRARY) && !dataModel.getCodeLists().isEmpty()){
            addConstraintViolation(context, "library-not-supported", "codeLists");
        }

        if (!dataModel.getCodeLists().stream().allMatch(uri -> uri.matches("^https?://uri.suomi.fi/codelist/(.*)"))) {
            addConstraintViolation(context, "invalid-codelist-uri", "codeLists");
        }
    }

    private void checkLinks(ConstraintValidatorContext context, DataModelDTO dataModel) {
        dataModel.getLinks().forEach(linkDTO -> {
            checkRequiredLocalizedValue(context, linkDTO.getName(), "links.name");
            checkNotNull(context, linkDTO.getUri(), "links.uri");
            linkDTO.getDescription().forEach((description, value) -> checkCommonTextArea(context, value, "links.description"));
        });
    }

    private void checkStatus(ConstraintValidatorContext context, DataModelDTO dataModel) {
        // don't allow other statuses than draft
        if (dataModel.getStatus() != null && !dataModel.getStatus().equals(Status.DRAFT)) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_INVALID, "status");
        }
    }
}
