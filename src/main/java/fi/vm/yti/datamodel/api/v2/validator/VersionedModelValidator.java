package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.VersionedModelDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class VersionedModelValidator extends BaseValidator implements ConstraintValidator<ValidVersionedDatamodel, VersionedModelDTO> {

    @Override
    public boolean isValid(VersionedModelDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkStatus(context, value.getStatus());
        checkDocumentation(context, value);

        return !isConstraintViolationAdded();
    }
}
