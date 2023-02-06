package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ClassValidator extends BaseValidator implements
        ConstraintValidator<ValidClass, ClassDTO> {

    private static final String MSG_VALUE_MISSING = "should-have-value";

    boolean updateClass;

    @Override
    public void initialize(ValidClass constraintAnnotation) {
        updateClass = constraintAnnotation.updateClass();
    }

    @Override
    public boolean isValid(ClassDTO classDTO, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, classDTO);
        checkComment(context, classDTO);
        checkStatus(context, classDTO);
        checkEquivalentClass(context, classDTO);
        checkSubClassOf(context, classDTO);
        checkSubject(context, classDTO);
        checkIdentifier(context, classDTO);

        return !isConstraintViolationAdded();
    }

    private void checkLabel(ConstraintValidatorContext context, ClassDTO classDTO){
        var labels = classDTO.getLabel();
        labels.forEach((lang, value) -> {
            if(value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
                addConstraintViolation(context, "value-over-character-limit." + ValidationConstants.TEXT_FIELD_MAX_LENGTH, "label");
            }
        });
    }

    private void checkComment(ConstraintValidatorContext context, ClassDTO classDTO){
        var comment = classDTO.getComment();
        if(comment.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
            addConstraintViolation(context, "value-over-character-limit." + ValidationConstants.TEXT_AREA_MAX_LENGTH, "label");
        }
    }

    private void checkStatus(ConstraintValidatorContext context, ClassDTO classDTO){
        var status = classDTO.getStatus();
        //Status has to be defined when creating
        if(!updateClass && status == null){
            addConstraintViolation(context, MSG_VALUE_MISSING, "status");
        }
    }

    private void checkEquivalentClass(ConstraintValidatorContext context, ClassDTO classDTO){
        var equivalentClass = classDTO.getEquivalentClass();
        equivalentClass.forEach(eqClass -> {
            //TODO check if equivalentClass is found in the resolved namespace
        });
    }

    private void checkSubClassOf(ConstraintValidatorContext context, ClassDTO classDTO){
        var subClassOf = classDTO.getSubClassOf();
        subClassOf.forEach(subClass -> {
            //TODO check if subClassOf is found in the resolved namespaces
        });
    }

    private void checkSubject(ConstraintValidatorContext context, ClassDTO classDTO){
        var subject = classDTO.getSubject();
        //TODO check if subject is found in one of the terminologies added to datamodel
    }

    private void checkIdentifier(ConstraintValidatorContext context, ClassDTO classDTO){
        var identifier = classDTO.getIdentifier();
        if(identifier == null || identifier.isBlank()){
            addConstraintViolation(context, MSG_VALUE_MISSING, "identifier");
        }
    }
}
