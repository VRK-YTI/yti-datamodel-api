package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.BaseDTO;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.annotation.Annotation;

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


    public void checkLabel(ConstraintValidatorContext context, BaseDTO dto, boolean updateClass){
        var labels = dto.getLabel();
        if(!updateClass && (labels == null || labels.isEmpty() || labels.values().stream().allMatch(String::isBlank))){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "label");
        }else if(labels != null){
            labels.forEach((lang, value) -> {
                if(value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
                    addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT + ValidationConstants.TEXT_FIELD_MAX_LENGTH, "label");
                }
            });
        }
    }

    public void checkEditorialNote(ConstraintValidatorContext context, BaseDTO dto){
        var editorialNote = dto.getEditorialNote();
        if(editorialNote != null && editorialNote.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT + ValidationConstants.TEXT_AREA_MAX_LENGTH, "editorialNote");
        }
    }

    public void checkNote(ConstraintValidatorContext context, BaseDTO dto) {
        var notes = dto.getNote();
        if(notes != null){
            notes.forEach((lang, value) -> {
                if(value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
                    addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT + ValidationConstants.TEXT_FIELD_MAX_LENGTH, "note");
                }
            });
        }
    }

    public void checkStatus(ConstraintValidatorContext context, BaseDTO dto, boolean updateClass){
        var status = dto.getStatus();
        //Status has to be defined when creating
        if(!updateClass && status == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "status");
        }
    }

    public void checkSubject(ConstraintValidatorContext context, BaseDTO dto){
        var subject = dto.getSubject();
        //TODO check if subject is found in one of the terminologies added to datamodel
    }

    public void checkIdentifier(ConstraintValidatorContext context, BaseDTO dto, boolean updateClass){
        var identifier = dto.getIdentifier();
        if(!updateClass && (identifier == null || identifier.isBlank())){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "identifier");
        }else if(updateClass && identifier != null){
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "identifier");
        }
    }

    public void checkCommonTextField(ConstraintValidatorContext context, String value, String property) {
        if(value != null && value.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT
                    + ValidationConstants.TEXT_AREA_MAX_LENGTH, property);
        }
    }

}
