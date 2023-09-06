package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.CodeListDTO;
import fi.vm.yti.datamodel.api.v2.mapper.CodeListMapper;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.Collection;

@Service
public class CodeListService {

    private static final Logger LOG = LoggerFactory.getLogger(CodeListService.class);

    private final SchemesRepository schemesRepository;

    /**
     * Control which environment is used for resolving terminology uris.
     * Possible values: awsdev, awstest and awslocal. Resolve from prod if empty
     */
    @Value("${env:}")
    private String awsEnv;
    private final WebClient client;

    public CodeListService(
            SchemesRepository schemesRepository,
            @Qualifier("uriResolveClient") WebClient webClient) {
        this.schemesRepository = schemesRepository;
        this.client = webClient;
    }

    /**
     * Fetch CodeList information and persist to Fuseki
     * @param codeLists set of uris
     */
    public void resolveCodelistScheme(Collection<String> codeLists) {
        codeLists.forEach(codeList -> {
            var uri = URI.create(codeList);
            LOG.debug("Fetching codelist {} from env {}", uri, awsEnv);
            try {
                var result = client.get().uri(builder -> builder
                                .path(uri.getPath())
                                .queryParam("env", awsEnv)
                                .build())
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<CodeListDTO>() {})
                        .block();

                if (result != null) {
                    var model = CodeListMapper.mapToJenaModel(codeList, result);
                    schemesRepository.put(codeList, model);
                }
            } catch (Exception e) {
                LOG.warn("Could not resolve codelist scheme {} from env {}, {}", uri, awsEnv, e.getMessage());
            }
        });
    }

}
