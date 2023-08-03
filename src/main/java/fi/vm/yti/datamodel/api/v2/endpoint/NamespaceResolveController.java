package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.service.NamespaceResolver;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Hidden
    @Operation(summary = "Resolve a namespace")
    @PutMapping
    public Boolean resolveNamespace(@RequestParam @Parameter(description = "Namespace URI") String namespace,
                                    @RequestParam(required = false, defaultValue = "false") @Parameter(description = "Should resolution be forced") boolean force){
        return namespaceResolver.resolve(namespace, force);
    }

    @Operation(summary = "Check if namespace is already resolved")
    @GetMapping("/resolved")
    public boolean isNamespaceAlreadyResolved(@RequestParam @Parameter(description = "Namespace URI") String namespace){
        return namespaceResolver.namespaceAlreadyResolved(namespace);
    }
}
