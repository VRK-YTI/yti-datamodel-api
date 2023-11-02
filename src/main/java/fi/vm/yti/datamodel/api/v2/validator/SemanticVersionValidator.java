package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.utils.SemVer;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SemanticVersionValidator extends BaseValidator implements
        ConstraintValidator<ValidSemanticVersion, String> {

    @Override
    public boolean isValid(String version, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);
        if(version != null && !version.matches(SemVer.VALID_REGEX)) {
            addConstraintViolation(context, "Invalid semantic version", "version");
        }
        return !isConstraintViolationAdded();
    }
}
