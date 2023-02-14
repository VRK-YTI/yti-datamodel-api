package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.endpoint.error.ResolvingException;
import fi.vm.yti.datamodel.api.v2.service.NamespaceResolver;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v2/namespace")
@Tag(name = "Namespace" )
public class NamespaceResolveController {


    private final NamespaceResolver namespaceResolver;

    public NamespaceResolveController(NamespaceResolver namespaceResolver) {
        this.namespaceResolver = namespaceResolver;
    }

    @PutMapping
    public boolean resolveNamespace(@RequestParam String namespace, @RequestParam(required = false, defaultValue = "false") boolean force){
        if(!force && namespaceResolver.namespaceAlreadyResolved(namespace)){
            throw new ResolvingException("Already resolved", "Use force parameter to force resolving");
        }
        if(ValidationConstants.RESERVED_NAMESPACES.containsValue(namespace)){
            throw new ResolvingException("Reserved namespace", "This namespace is reserved and cannot be resolved");
        }
        return namespaceResolver.resolveNamespace(namespace);
    }

    @GetMapping("/resolved")
    public boolean isNamespaceAlreadyResolved(@RequestParam String namespace){
        return namespaceResolver.namespaceAlreadyResolved(namespace);
    }
}
