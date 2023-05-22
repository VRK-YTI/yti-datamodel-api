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

        checkLabel(context, classDTO, updateClass);
        checkEditorialNote(context, classDTO);
        checkNote(context, classDTO);
        checkStatus(context, classDTO, updateClass);
        checkEquivalentClass(context, classDTO);
        checkSubClassOf(context, classDTO);
        checkSubject(context, classDTO);
        checkIdentifier(context, classDTO, updateClass);

        return !isConstraintViolationAdded();
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

}
