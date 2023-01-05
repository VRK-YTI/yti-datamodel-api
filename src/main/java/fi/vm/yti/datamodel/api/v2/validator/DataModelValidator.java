package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class DataModelValidator extends BaseValidator implements
        ConstraintValidator<ValidDatamodel, DataModelDTO> {

    @Autowired
    private JenaService jenaService;

    @Override
    public boolean isValid(DataModelDTO dataModel, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkPrefix(context, dataModel);
        checkLanguages(context, dataModel);
        checkLabels(context, dataModel);
        checkDescription(context, dataModel);
        checkOrganizations(context, dataModel);
        checkGroups(context, dataModel);

        return !isConstraintViolationAdded();
    }

    /**
     * Check if prefix is valid
     * @param context Constraint validator context
     * @param dataModel Datamodel
     */
    private void checkPrefix(ConstraintValidatorContext context, DataModelDTO dataModel){
        var prefix = dataModel.getPrefix();
        if(prefix.length() < 3 || prefix.length() > 10){
            addConstraintViolation(context, "character-count-mismatch", "prefix");
        }
        if(!dataModel.getPrefix().matches("^[a-z][a-z0-9-_]{2,}")){
            addConstraintViolation(context, "not-matching-pattern", "prefix");
        }
    }

    /**
     * Check if languages are valid
     * @param context Constraint validator context
     * @param dataModel Datamodel
     */
    private void checkLanguages(ConstraintValidatorContext context, DataModelDTO dataModel){
        var languages = dataModel.getLanguages();
        languages.forEach(language -> {
            //Matches RFC-4646
            if(!language.matches("^[a-z]{2,3}(?:-[A-Z]{2,3}(?:-[a-zA-Z]{4})?)?$")){
                addConstraintViolation(context, "does-not-match-rfc-4646", "languages");
            }
        });
    }

    /**
     * Check if labels are valid
     * @param context Constraint validator context
     * @param dataModel Datamodel
     */
    private void checkLabels(ConstraintValidatorContext context, DataModelDTO dataModel){
        var labels = dataModel.getLabel();
        var languages = dataModel.getLanguages();
        labels.forEach((key, value) -> {
            if (!languages.contains(key)) {
                addConstraintViolation(context, "language-not-in-language-list." + key, "label");
            }
            if(value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
                addConstraintViolation(context, "value-over-character-limit." + ValidationConstants.TEXT_FIELD_MAX_LENGTH, "label");
            }
        });
    }

    /**
     * Check if descriptions are valid
     * @param context Constraint validator context
     * @param dataModel Datamodel
     */
    private void checkDescription(ConstraintValidatorContext context, DataModelDTO dataModel){
        var description = dataModel.getDescription();
        var languages = dataModel.getLanguages();
        if(description == null){
            return;
        }
        description.forEach((key, value) -> {
            if (!languages.contains(key)) {
                addConstraintViolation(context, "language-not-in-language-list." + key, "description");
            }
            if(value.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
                addConstraintViolation(context, "value-over-character-limit." + ValidationConstants.TEXT_AREA_MAX_LENGTH, "description");
            }
        });
    }

    /**
     * Check if organizations are valid
     * @param context Constraint validator context
     * @param dataModel DataModel
     */
    private void checkOrganizations(ConstraintValidatorContext context, DataModelDTO dataModel){
        var organizations = dataModel.getOrganizations();
        var existingOrgs = jenaService.getOrganizations();
        if(organizations == null || organizations.isEmpty()){
            addConstraintViolation(context, "should-have-value", "organization");
            return;
        }
        organizations.forEach(org -> {
            var queryRes = ResourceFactory.createResource("urn:uuid:" + org.toString());
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
        var existingGroups = jenaService.getServiceCategories();
        if(groups == null || groups.isEmpty()){
            addConstraintViolation(context, "should-have-value", "groups");
            return;
        }
        groups.forEach(group -> {
            var resources = existingGroups.listResourcesWithProperty(SKOS.notation, group);
            if (!resources.hasNext()) {
                addConstraintViolation(context, "does-not-exist." + group, "groups");
            }
        });
    }
}
