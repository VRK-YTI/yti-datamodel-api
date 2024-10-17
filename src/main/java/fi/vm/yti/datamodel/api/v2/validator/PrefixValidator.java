package fi.vm.yti.datamodel.api.v2.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PrefixValidator extends BaseValidator implements
        ConstraintValidator<ValidPrefix, String> {

    @Override
    public boolean isValid(String prefix, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        checkPrefix(context, prefix, "prefix", false);
        return !isConstraintViolationAdded();
    }
}
