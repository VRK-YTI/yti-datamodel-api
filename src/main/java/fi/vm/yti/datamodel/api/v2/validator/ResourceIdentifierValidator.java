package fi.vm.yti.datamodel.api.v2.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ResourceIdentifierValidator extends BaseValidator implements
        ConstraintValidator<ValidResourceIdentifier, String> {

    @Override
    public boolean isValid(String identifier, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkIdentifier(context, identifier, false);
        return !isConstraintViolationAdded();
    }
}
