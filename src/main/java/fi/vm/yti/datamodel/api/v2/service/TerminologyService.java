package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ConceptDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceInfoBaseDTO;
import fi.vm.yti.datamodel.api.v2.dto.SimpleResourceDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.TerminologyMapper;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.TerminologyRepository;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Consumer;

@Service
public class TerminologyService {

    private final TerminologyRepository terminologyRepository;

    public TerminologyService(TerminologyRepository terminologyRepository) {
        this.terminologyRepository = terminologyRepository;
    }

    public void resolveConcept(String uri) {
        if (uri == null) {
            return;
        }
        var exists = terminologyRepository.queryAsk(
                new AskBuilder().addGraph("?g",
                            new WhereBuilder().addWhere(NodeFactory.createURI(uri), "?p", "?o"))
                        .build());
        if (!exists) {
            throw new ResourceNotFoundException(uri);
        }
    }

    public void resolveTerminology(Set<String> uris) {
        for (var uri : uris) {
            var exists = terminologyRepository.graphExists(uri);
            if (!exists) {
                throw new ResourceNotFoundException(uri);
            }
        }
    }

    public Consumer<ResourceInfoBaseDTO> mapConcept() {
        return (var dto) -> dto.setSubject(getMappedConceptDTO(dto.getSubject()));
    }

    public Consumer<SimpleResourceDTO> mapConceptToResource() {
        return (var dto) -> dto.setConcept(getMappedConceptDTO(dto.getConcept()));
    }

    private ConceptDTO getMappedConceptDTO(ConceptDTO dto) {
        if (dto == null || dto.getConceptURI() == null || dto.getConceptURI().isEmpty()) {
            return null;
        }
        var conceptModel = getConcept(dto.getConceptURI());
        return TerminologyMapper.mapToConceptDTO(conceptModel, dto.getConceptURI());
    }

    public Model getTerminology(String uri) {
        if (uri != null && !uri.endsWith(ModelConstants.RESOURCE_SEPARATOR)) {
            uri = uri + ModelConstants.RESOURCE_SEPARATOR;
        }
        var builder = new ConstructBuilder()
                .addConstruct("?s", SKOS.prefLabel, "?label")
                .addGraph(NodeFactory.createURI(uri), new WhereBuilder()
                        .addWhere("?s", RDF.type, SKOS.ConceptScheme)
                        .addWhere("?s", SKOS.prefLabel, "?label"));

        return terminologyRepository.queryConstruct(builder.build());
    }

    public Model getConcept(String uri) {
        var concept = NodeFactory.createURI(uri);
        var builder = new ConstructBuilder()
                .addConstruct(concept, SKOS.definition, "?definition")
                .addConstruct(concept, SuomiMeta.publicationStatus, "?status")
                .addConstruct(concept, SKOS.prefLabel, "?label")
                .addConstruct("?g", SKOS.prefLabel, "?terminology")
                .addGraph("?g", new WhereBuilder()
                        .addOptional(concept, SKOS.definition, "?definition")
                        .addWhere(concept, SuomiMeta.publicationStatus, "?status")
                        .addWhere(concept, SKOS.prefLabel, "?recommendedTerm")
                        .addWhere("?recommendedTerm", SKOSXL.literalForm, "?label")
                        .addWhere("?g", SKOS.prefLabel, "?terminology"));

        return terminologyRepository.queryConstruct(builder.build());
    }

    public Model getConcepts(Set<String> conceptURIs) {
        if (conceptURIs.isEmpty()) {
            return ModelFactory.createDefaultModel();
        }
        var resources = conceptURIs.stream()
                .map(NodeFactory::createURI)
                .toList()
                .toArray();

        var builder = new ConstructBuilder()
                .addConstruct("?concept", SKOS.prefLabel, "?conceptLabel")
                .addConstruct("?g", SKOS.prefLabel, "?terminologyLabel")
                .addGraph("?g", new WhereBuilder()
                        .addWhere("?concept", SKOS.prefLabel, "?prefLabel")
                        .addWhere("?prefLabel", SKOSXL.literalForm, "?conceptLabel")
                        .addWhere("?g", SKOS.prefLabel, "?terminologyLabel"))
                .addValueVar("concept", resources);

        return terminologyRepository.queryConstruct(builder.build());
    }
}
