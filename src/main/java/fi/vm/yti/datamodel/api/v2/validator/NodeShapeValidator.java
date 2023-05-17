package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.NodeShapeDTO;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
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
    private JenaService jenaService;

    boolean updateNodeShape;

    @Override
    public void initialize(ValidNodeShape constraintAnnotation) {
        updateNodeShape = constraintAnnotation.updateNodeShape();
    }

    @Override
    public boolean isValid(NodeShapeDTO nodeShapeDTO, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, nodeShapeDTO, updateNodeShape);
        checkEditorialNote(context, nodeShapeDTO);
        checkNote(context, nodeShapeDTO);
        checkStatus(context, nodeShapeDTO, updateNodeShape);
        checkSubject(context, nodeShapeDTO);
        checkIdentifier(context, nodeShapeDTO, updateNodeShape);
        checkTargetClass(context, nodeShapeDTO);
        checkTargetNode(context, nodeShapeDTO);

        return !isConstraintViolationAdded();
    }

    private void checkTargetClass(ConstraintValidatorContext context, NodeShapeDTO nodeShapeDTO){
        var targetClass = nodeShapeDTO.getTargetClass();
        if(targetClass != null && !targetClass.isBlank()){
            var checkImports = !targetClass.startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
            if(!jenaService.checkIfResourceIsOneOfTypes(targetClass, List.of(RDFS.Class, OWL.Class), checkImports)){
                addConstraintViolation(context, "not-class-or-doesnt-exist", "targetClass");
            }
        }
    }

    private void checkTargetNode(ConstraintValidatorContext context, NodeShapeDTO nodeShapeDTO){
        var targetNode = nodeShapeDTO.getTargetNode();
        if(targetNode != null && !targetNode.isBlank()){
            var checkImports = !targetNode.startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
            if(!jenaService.checkIfResourceIsOneOfTypes(targetNode, List.of(SH.NodeShape), checkImports)){
                addConstraintViolation(context, "not-node-shape-or-doesnt-exist", "targetNode");
            }
        }
    }
}
