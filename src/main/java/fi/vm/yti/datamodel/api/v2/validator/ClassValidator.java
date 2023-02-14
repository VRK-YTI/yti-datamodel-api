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
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "status");
        }
    }

    private void checkEquivalentClass(ConstraintValidatorContext context, ClassDTO classDTO){
        var equivalentClass = classDTO.getEquivalentClass();
        equivalentClass.forEach(eqClass -> {
            var asUri = NodeFactory.createURI(eqClass);
            //if namespace is resolvable make sure class can be found in resolved namespace
            if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
                    && jenaService.doesClassExistInNamespace(asUri.getNameSpace(), asUri.getURI())){
                addConstraintViolation(context, "class-not-found-in-resolved-namespace", "eqClass");
            }
        });
    }

    private void checkSubClassOf(ConstraintValidatorContext context, ClassDTO classDTO){
        var subClassOf = classDTO.getSubClassOf();
        subClassOf.forEach(subClass -> {
            var asUri = NodeFactory.createURI(subClass);
            //if namespace is resolvable make sure class can be found in resolved namespace
            if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
            && jenaService.doesClassExistInNamespace(asUri.getNameSpace(), asUri.getURI())){
                    addConstraintViolation(context, "class-not-found-in-resolved-namespace", "subClassOf");
            }
        });
    }

    private void checkSubject(ConstraintValidatorContext context, ClassDTO classDTO){
        var subject = classDTO.getSubject();
        //TODO check if subject is found in one of the terminologies added to datamodel
    }

    private void checkIdentifier(ConstraintValidatorContext context, ClassDTO classDTO){
        var identifier = classDTO.getIdentifier();
        if(identifier == null || identifier.isBlank()){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "identifier");
        }
    }
}
