package fi.vm.yti.datamodel.api.v2.validator;

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
}
