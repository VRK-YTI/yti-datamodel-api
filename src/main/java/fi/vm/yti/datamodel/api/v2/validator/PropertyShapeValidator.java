package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PropertyShapeValidator extends BaseValidator implements ConstraintValidator<ValidPropertyShape, PropertyShapeDTO> {

    boolean updateProperty;

    ResourceType resourceType;

    @Autowired
    private ResourceService resourceService;


    @Override
    public void initialize(ValidPropertyShape constraintAnnotation) {
        this.updateProperty = constraintAnnotation.updateProperty();
        this.resourceType = constraintAnnotation.resourceType();
    }

    @Override
    public boolean isValid(PropertyShapeDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, value);
        checkEditorialNote(context, value);
        checkNote(context, value);
        checkIdentifier(context, value.getIdentifier(), updateProperty);
        checkPath(context, value);
        if(resourceType.equals(ResourceType.ASSOCIATION)){
            checkClassType(context, (AssociationRestriction) value);
        }else {
            var dto = (AttributeRestriction) value;
            checkDataType(context, dto);
            checkCodeList(context, dto);
            checkCommonTextArea(context, dto.getDefaultValue(), "defaultValue");
            checkCommonTextArea(context, dto.getHasValue(), "hasValue");
            checkCommonTextField(context, dto.getPattern(), "pattern");
            checkLanguageTags(context, dto.getLanguageIn(), "languageIn");
            if (dto.getAllowedValues() != null) {
                dto.getAllowedValues().forEach(v -> checkCommonTextArea(context, v, "allowedValues"));
            }
        }

        return !isConstraintViolationAdded();
    }

    private void checkPath(ConstraintValidatorContext context, PropertyShapeDTO dto) {
        var path = dto.getPath();

        if (path != null && !path.isBlank()
                && !resourceService.checkIfResourceIsOneOfTypes(path, List.of(OWL.ObjectProperty, OWL.DatatypeProperty, RDF.Property))) {
            addConstraintViolation(context, "not-property-or-doesnt-exist", "path");
        }
    }

    private void checkClassType(ConstraintValidatorContext context, AssociationRestriction dto) {
        var classType = dto.getClassType();
        if (classType != null && !resourceService.checkIfResourceIsOneOfTypes(classType, List.of(RDFS.Class, OWL.Class))) {
            addConstraintViolation(context, "not-class-or-doesnt-exist", "path");
        }
    }

    private void checkDataType(ConstraintValidatorContext context, AttributeRestriction dto) {
        if (dto.getDataType() != null && !ModelConstants.SUPPORTED_DATA_TYPES.contains(dto.getDataType())) {
            addConstraintViolation(context, "unsupported-datatype", "dataType");
        }
    }

    private void checkCodeList(ConstraintValidatorContext context, AttributeRestriction dto) {
        if(dto.getCodeLists().stream().anyMatch(codeList -> !codeList.startsWith(ModelConstants.CODELIST_NAMESPACE))){
            addConstraintViolation(context, "invalid-codelist-uri", "codelists");
        }
    }
}
