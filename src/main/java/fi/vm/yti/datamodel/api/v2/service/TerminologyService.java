package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import fi.vm.yti.datamodel.api.v2.mapper.TerminologyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Service
public class TerminologyService {
    private static final Logger LOG = LoggerFactory.getLogger(TerminologyService.class);

    /**
     * Control which environment is used for resolving terminology uris.
     * Possible values: awsdev, awstest and awslocal. Resolve from prod if empty
     */
    @Value("${env:}")
    private String awsEnv;
    private final WebClient client;
    private final JenaService jenaService;

    public TerminologyService(
            @Qualifier("uriResolveClient") WebClient webClient,
            JenaService jenaService) {
        this.jenaService = jenaService;
        this.client = webClient;
    }

    /**
     * Fetch terminology information and persist to Fuseki
     * @param terminologyUris set of uris
     */
    public void resolveTerminology(Set<String> terminologyUris) {

        for (String u : terminologyUris) {
            var uri = URI.create(u);
            LOG.debug("Fetching terminology {}", uri);
            try {
                var result = client.get().uri(uriBuilder -> uriBuilder
                                .path(uri.getPath())
                                .queryParam("env", awsEnv)
                                .build()
                        )
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<List<TerminologyNodeDTO>>() {
                        })
                        .block();

                if (result == null) {
                    continue;
                }
                var node = result.stream()
                        .filter(n -> n.getType().getId().equals("TerminologicalVocabulary"))
                        .findFirst();

                if (node.isPresent()) {
                    jenaService.putTerminologyToConcepts(uri.toString(), TerminologyMapper.mapToJenaModel(u, node.get()));
                } else {
                    LOG.warn("Could not find node with type TerminologicalVocabulary from {}", uri);
                }
            } catch (Exception e) {
                LOG.warn("Could not resolve terminology {}, {}", uri, e.getMessage());
            }
        }
    }

}
