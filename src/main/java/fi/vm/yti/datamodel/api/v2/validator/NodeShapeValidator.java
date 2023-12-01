package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.NodeShapeDTO;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;

public class NodeShapeValidator extends BaseValidator implements
        ConstraintValidator<ValidNodeShape, NodeShapeDTO> {

    @Autowired
    private ResourceService resourceService;

    boolean updateNodeShape;

    @Override
    public void initialize(ValidNodeShape constraintAnnotation) {
        updateNodeShape = constraintAnnotation.updateNodeShape();
    }

    @Override
    public boolean isValid(NodeShapeDTO nodeShapeDTO, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, nodeShapeDTO);
        checkEditorialNote(context, nodeShapeDTO);
        checkNote(context, nodeShapeDTO);
        checkSubject(context, nodeShapeDTO);
        checkIdentifier(context, nodeShapeDTO.getIdentifier(), updateNodeShape);
        checkReservedIdentifier(context, nodeShapeDTO);
        checkTargetClass(context, nodeShapeDTO);
        checkTargetNode(context, nodeShapeDTO);

        return !isConstraintViolationAdded();
    }

    private void checkTargetClass(ConstraintValidatorContext context, NodeShapeDTO nodeShapeDTO){
        var targetClass = nodeShapeDTO.getTargetClass();
        if(targetClass != null && !targetClass.isBlank()
                && !resourceService.checkIfResourceIsOneOfTypes(targetClass, List.of(RDFS.Class, OWL.Class))) {
            addConstraintViolation(context, "not-class-or-doesnt-exist", "targetClass");
        }
    }

    private void checkTargetNode(ConstraintValidatorContext context, NodeShapeDTO nodeShapeDTO){
        var targetNode = nodeShapeDTO.getTargetNode();
        if(targetNode != null && !targetNode.isBlank()
                && !resourceService.checkIfResourceIsOneOfTypes(targetNode, List.of(SH.NodeShape))) {
            addConstraintViolation(context, "not-node-shape-or-doesnt-exist", "targetNode");
        }
    }
}
