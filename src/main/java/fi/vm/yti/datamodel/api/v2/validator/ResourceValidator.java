package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceDTO;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ResourceValidator extends BaseValidator implements ConstraintValidator<ValidResource, ResourceDTO> {

    private boolean updateProperty;

    @Autowired
    private JenaService jenaService;
    @Override
    public void initialize(ValidResource constraintAnnotation) {
        this.updateProperty = constraintAnnotation.updateProperty();
    }

    @Override
    public boolean isValid(ResourceDTO value, ConstraintValidatorContext context) {
        setConstraintViolationAdded(false);

        checkLabel(context, value, updateProperty);
        checkEditorialNote(context, value);
        checkStatus(context, value, updateProperty);
        checkNote(context, value);
        checkEquivalentProperty(context, value);
        checkSubPropertyOf(context, value);
        checkIdentifier(context, value, updateProperty);
        checkType(context, value);
        checkDomain(context, value);
        checkRange(context, value);

        return !isConstraintViolationAdded();
    }

    private void checkType(ConstraintValidatorContext context, ResourceDTO resourceDTO){
        if(!updateProperty && resourceDTO.getType() == null){
            addConstraintViolation(context, ValidationConstants.MSG_VALUE_MISSING, "type");
        }else if (updateProperty && resourceDTO.getType() != null){
            addConstraintViolation(context, ValidationConstants.MSG_NOT_ALLOWED_UPDATE, "type");
        }
    }

    private void checkEquivalentProperty(ConstraintValidatorContext context, ResourceDTO resourceDTO){
        var equivalentResource = resourceDTO.getEquivalentResource();
        if(equivalentResource != null){
            equivalentResource.forEach(eq -> {
                var asUri = NodeFactory.createURI(eq);
                //if namespace is resolvable make sure class can be found in resolved namespace
                if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
                        && !jenaService.doesResourceExistInImportedNamespace(asUri.getNameSpace(), asUri.getURI())){
                    addConstraintViolation(context, "resource-not-found-in-resolved-namespace", "eqClass");
                }
            });
        }
    }

    private void checkSubPropertyOf(ConstraintValidatorContext context, ResourceDTO resourceDTO){
        var getSubResourceOf = resourceDTO.getSubResourceOf();
        if(getSubResourceOf != null) {
            getSubResourceOf.forEach(sub -> {
                var asUri = NodeFactory.createURI(sub);
                //if namespace is resolvable make sure class can be found in resolved namespace
                if(jenaService.doesResolvedNamespaceExist(asUri.getNameSpace())
                        && !jenaService.doesResourceExistInImportedNamespace(asUri.getNameSpace(), asUri.getURI())){
                    addConstraintViolation(context, "resource-not-found-in-resolved-namespace", "subClassOf");
                }
            });
        }
    }


    private void checkDomain(ConstraintValidatorContext context, ResourceDTO resourceDTO){
        var domain = resourceDTO.getDomain();
        if(domain != null && !domain.isBlank()){
            var checkImports = !domain.startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
            if(!jenaService.checkIfResourceIsOneOfTypes(domain, List.of(RDFS.Class, OWL.Class), checkImports)){
                addConstraintViolation(context, "not-class-or-doesnt-exist", "domain");
            }
        }
    }

    private void checkRange(ConstraintValidatorContext context, ResourceDTO resourceDTO){
        var range = resourceDTO.getRange();
        if(range != null && !range.isBlank()){
            var checkImports = !range.startsWith(ModelConstants.SUOMI_FI_NAMESPACE);
            if(!jenaService.checkIfResourceIsOneOfTypes(range, List.of(RDFS.Class, OWL.Class), checkImports)){
                addConstraintViolation(context, "not-class-or-doesnt-exist", "range");
            }
        }
    }
}
