package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.BaseDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

public abstract class BaseValidator implements Annotation{

    private boolean constraintViolationAdded;

    @Override
    public Class<? extends Annotation> annotationType() {
        return BaseValidator.class;
    }


    /**
     * Add constraint violation to the constraint validator context
     * @param context Constraint validator context
     * @param message Message
     * @param property Property
     */
    void addConstraintViolation(ConstraintValidatorContext context, String message, String property) {
        if (!this.constraintViolationAdded) {
            context.disableDefaultConstraintViolation();
            this.constraintViolationAdded = true;
        }

        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(property)
                .addConstraintViolation();
    }

    public boolean isConstraintViolationAdded() {
        return constraintViolationAdded;
    }

    public void setConstraintViolationAdded(boolean constraintViolationAdded) {
        this.constraintViolationAdded = constraintViolationAdded;
    }

    public void checkLabel(ConstraintValidatorContext context, BaseDTO dto){
        checkRequiredLocalizedValue(context, dto.getLabel(), "label");
    }

    public void checkEditorialNote(ConstraintValidatorContext context, BaseDTO dto){
        var editorialNote = dto.getEditorialNote();
        checkCommonTextArea(context, editorialNote, "editorialNote");
    }

    public void checkNote(ConstraintValidatorContext context, BaseDTO dto) {
        var notes = dto.getNote();
        if(notes != null){
            notes.forEach((lang, value) -> checkCommonTextArea(context, value, "note"));
        }
    }

    public void checkStatus(ConstraintValidatorContext context, Status status){
        //Status has to be defined when creating
        if(status == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "status");
        }
    }

    public void checkSubject(ConstraintValidatorContext context, BaseDTO dto){
        var subject = dto.getSubject();
        if (subject != null && !subject.matches("^https?://uri.suomi.fi/terminology/(.*)")) {
            addConstraintViolation(context, "invalid-terminology-uri", "subject");
        }
    }

    public void checkIdentifier(ConstraintValidatorContext context, final String value, boolean update) {
        checkPrefixOrIdentifier(context, value, "identifier", ValidationConstants.RESOURCE_IDENTIFIER_REGEX, update);
    }

    public void checkPrefix(ConstraintValidatorContext context, final String value, String propertyName, boolean update) {
        checkPrefixOrIdentifier(context, value, propertyName, ValidationConstants.PREFIX_REGEX, update);
    }

    public void checkReservedIdentifier(ConstraintValidatorContext context, BaseDTO dto) {
        if (dto.getIdentifier() != null && dto.getIdentifier().startsWith(ModelConstants.CORNER_PREFIX)) {
            addConstraintViolation(context, "reserved-identifier", "identifier");
        }
    }

    public void checkCommonTextArea(ConstraintValidatorContext context, String value, String property) {
        if(value != null && value.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT
                    + ValidationConstants.TEXT_AREA_MAX_LENGTH, property);
        }
    }

    public void checkCommonTextField(ConstraintValidatorContext context, String value, String property) {
        if(value != null && value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT
                    + ValidationConstants.TEXT_FIELD_MAX_LENGTH, property);
        }
    }

    public void checkNotNull(ConstraintValidatorContext context, String value, String property) {
        if(value == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, property);
        }
    }

    public void checkShouldBeNull(ConstraintValidatorContext context, Object value, String property) {
        if(value != null) {
            addConstraintViolation(context, ValidationConstants.MSQ_NOT_ALLOWED, property);
        }
    }

    public void checkLanguageTags(ConstraintValidatorContext context, Collection<String> languages, String property) {
        languages.forEach(language -> {
            //Matches RFC-4646
            if(!language.matches("^[a-z]{2,3}(?:-[A-Z]{2,3}(?:-[a-zA-Z]{4})?)?$")){
                addConstraintViolation(context, "does-not-match-rfc-4646", property);
            }
        });
    }

    public void checkRequiredLocalizedValue(ConstraintValidatorContext context, Map<String, String> value, String property) {
        if (value == null || value.isEmpty() || value.values().stream().anyMatch(v -> v == null || v.isBlank())) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, property);
        } else {
            value.forEach((lang, v) -> checkCommonTextField(context, v, property));
        }
    }

    private void checkPrefixOrIdentifier(ConstraintValidatorContext context, String value, String propertyName, String regexp, boolean update) {
        if (update && value != null) {
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, propertyName);
        } else if (!update && (value == null || value.isBlank())) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, propertyName);
        } else if (value != null && !value.matches(regexp)) {
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_INVALID, propertyName);
        } else if (value != null && (
                value.length() < ValidationConstants.PREFIX_MIN_LENGTH
                        || value.length() > ValidationConstants.PREFIX_MAX_LENGTH)) {
            addConstraintViolation(context, propertyName + "-character-count-mismatch", propertyName);
        }
    }
}

