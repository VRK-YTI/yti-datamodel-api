package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.graph.NodeFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class ClassValidator extends BaseValidator implements
        ConstraintValidator<ValidClass, ClassDTO> {

    @Autowired
    private JenaService jenaService;

    boolean updateClass;

    @Override
    public void initialize(ValidClass constraintAnnotation) {
        updateClass = constraintAnnotation.updateClass();
    }

    @Override
    public boolean isValid(ClassDTO classDTO, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, classDTO);
        checkEditorialNote(context, classDTO);
        checkNote(context, classDTO);
        checkStatus(context, classDTO);
        checkEquivalentClass(context, classDTO);
        checkSubClassOf(context, classDTO);
        checkSubject(context, classDTO);
        checkIdentifier(context, classDTO);

        return !isConstraintViolationAdded();
    }

    private void checkLabel(ConstraintValidatorContext context, ClassDTO classDTO){
        var labels = classDTO.getLabel();
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

    private void checkEditorialNote(ConstraintValidatorContext context, ClassDTO classDTO){
        var editorialNote = classDTO.getEditorialNote();
        if(editorialNote != null && editorialNote.length() > ValidationConstants.TEXT_AREA_MAX_LENGTH){
            addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT + ValidationConstants.TEXT_AREA_MAX_LENGTH, "editorialNote");
        }
    }

    private void checkNote(ConstraintValidatorContext context, ClassDTO classDTO) {
        var notes = classDTO.getNote();
        if(notes != null){
            notes.forEach((lang, value) -> {
                if(value.length() > ValidationConstants.TEXT_FIELD_MAX_LENGTH){
                    addConstraintViolation(context, ValidationConstants.MSG_OVER_CHARACTER_LIMIT + ValidationConstants.TEXT_FIELD_MAX_LENGTH, "note");
                }
            });
        }
    }

    private void checkStatus(ConstraintValidatorContext context, ClassDTO classDTO){
        var status = classDTO.getStatus();
        //Status has to be defined when creating
        if(!updateClass && status == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "status");
        }
    }

    private void checkEquivalentClass(ConstraintValidatorContext context, ClassDTO classDTO){
        var equivalentClass = classDTO.getEquivalentClass();
        if(equivalentClass != null){
            equivalentClass.forEach(eqClass -> {
                var asUri = NodeFactory.createURI(eqClass);
                //if namespace is resolvable make sure class can be found in resolved namespace
                if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
                        && jenaService.doesResourceExistInImportedNamespace(asUri.getNameSpace(), asUri.getURI())){
                    addConstraintViolation(context, "class-not-found-in-resolved-namespace", "eqClass");
                }
            });
        }
    }

    private void checkSubClassOf(ConstraintValidatorContext context, ClassDTO classDTO){
        var subClassOf = classDTO.getSubClassOf();
        if(subClassOf != null) {
            subClassOf.forEach(subClass -> {
                var asUri = NodeFactory.createURI(subClass);
                //if namespace is resolvable make sure class can be found in resolved namespace
                if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
                        && jenaService.doesResourceExistInImportedNamespace(asUri.getNameSpace(), asUri.getURI())){
                    addConstraintViolation(context, "class-not-found-in-resolved-namespace", "subClassOf");
                }
            });
        }
    }

    private void checkSubject(ConstraintValidatorContext context, ClassDTO classDTO){
        var subject = classDTO.getSubject();
        //TODO check if subject is found in one of the terminologies added to datamodel
    }

    private void checkIdentifier(ConstraintValidatorContext context, ClassDTO classDTO){
        var identifier = classDTO.getIdentifier();
        if(!updateClass && (identifier == null || identifier.isBlank())){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "identifier");
        }else if(updateClass && identifier != null){
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "identifier");
        }
    }
}
