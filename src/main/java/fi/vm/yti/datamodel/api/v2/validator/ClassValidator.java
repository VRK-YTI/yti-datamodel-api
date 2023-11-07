package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.graph.NodeFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class ClassValidator extends BaseValidator implements
        ConstraintValidator<ValidClass, ClassDTO> {

    @Autowired
    private ImportsRepository importsRepository;

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
        checkStatus(context, classDTO.getStatus());
        checkClassResolvable(context, classDTO.getEquivalentClass(), "equivalentClass");
        checkClassResolvable(context, classDTO.getSubClassOf(), "subClassOf");
        checkClassResolvable(context, classDTO.getDisjointWith(), "disjointWith");
        checkSubject(context, classDTO);
        checkIdentifier(context, classDTO.getIdentifier(), updateClass);
        checkReservedIdentifier(context, classDTO);

        return !isConstraintViolationAdded();
    }

    private void checkClassResolvable(ConstraintValidatorContext context, Set<String> classes, String property) {
        if(classes != null){
            classes.forEach(cls -> {
                var asUri = NodeFactory.createURI(cls);
                //if namespace is resolvable make sure class can be found in resolved namespace
                if(importsRepository.graphExists(asUri.getNameSpace())
                && !importsRepository.resourceExistsInGraph(asUri.getNameSpace(), asUri.getURI())) {
                    addConstraintViolation(context, "class-not-found-in-resolved-namespace", property);
                }
            });
        }
    }

}
