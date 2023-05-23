package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.vocabulary.OWL;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PropertyShapeValidator extends BaseValidator implements ConstraintValidator<ValidResource, PropertyShapeDTO> {

    boolean updateProperty;

    @Autowired
    private JenaService jenaService;

    @Override
    public void initialize(ValidResource constraintAnnotation) {
        this.updateProperty = constraintAnnotation.updateProperty();
    }

    @Override
    public boolean isValid(PropertyShapeDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, value, updateProperty);
        checkEditorialNote(context, value);
        checkStatus(context, value, updateProperty);
        checkNote(context, value);
        checkIdentifier(context, value, updateProperty);
        checkType(context, value);
        checkPath(context, value);

        return !isConstraintViolationAdded();
    }

    private void checkType(ConstraintValidatorContext context, PropertyShapeDTO resourceDTO){
        if(!updateProperty && resourceDTO.getType() == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "type");
        }else if (updateProperty && resourceDTO.getType() != null){
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "type");
        }
    }

    private void checkPath(ConstraintValidatorContext context, PropertyShapeDTO dto) {
        var path = dto.getPath();
        if(path != null && !path.isBlank()){
            var checkImports = !path.startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
            var allowedType = dto.getType().equals(ResourceType.ASSOCIATION)
                    ? OWL.ObjectProperty
                    : OWL.DatatypeProperty;
            if(!jenaService.checkIfResourceIsOneOfTypes(path, List.of(allowedType), checkImports)){
                addConstraintViolation(context, "not-property-or-doesnt-exist", "path");
            }
        }
    }
}
