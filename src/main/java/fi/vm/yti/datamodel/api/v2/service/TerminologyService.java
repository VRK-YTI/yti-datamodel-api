package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import fi.vm.yti.datamodel.api.v2.mapper.TerminologyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Service
public class TerminologyService {
    private static final Logger LOG = LoggerFactory.getLogger(TerminologyService.class);

    @Value("${awsEnv:}")
    private String awsEnv;

    private final WebClient.Builder webClientBuilder;
    private final JenaService jenaService;

    public TerminologyService(WebClient.Builder webClientBuilder, JenaService jenaService) {
        this.webClientBuilder = webClientBuilder;
        this.jenaService = jenaService;
    }

    /**
     * Fetch terminology information and persist to Fuseki
     * @param terminologyUris set of uris
     */
    public void resolveTerminology(Set<String> terminologyUris) {

        for (String u : terminologyUris) {
            var uri = URI.create(u);
            var baseUrl = uri.getScheme().concat("://").concat(uri.getHost());
            var client = webClientBuilder.clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().followRedirect(true)
            )).baseUrl(baseUrl).build();

            try {
                var result = client.get().uri(builder -> builder
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
