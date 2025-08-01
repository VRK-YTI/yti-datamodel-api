package fi.vm.yti.datamodel.api.v2.migration.task;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.SemVer;
import fi.vm.yti.migration.MigrationTask;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@SuppressWarnings("java:S101")
@Component
public class V7_VersionDraft implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V7_VersionDraft.class);

    private final CoreRepository coreRepository;

    private final ModelMapper modelMapper;

    public V7_VersionDraft(CoreRepository coreRepository,
                           ModelMapper modelMapper) {
        this.coreRepository = coreRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public void migrate() {
        LOG.info("Creating draft versions for all graphs");
        var selectBuilder = new SelectBuilder();
        selectBuilder.addPrefixes(ModelConstants.PREFIXES);
        var exprFactory = selectBuilder.getExprFactory();
        var expr = exprFactory.strstarts(exprFactory.str("?g"), Constants.DATA_MODEL_NAMESPACE);
        selectBuilder.addFilter(expr);
        selectBuilder.addGraph("?g", new WhereBuilder());

        var graphs = new ArrayList<String>();
        coreRepository.querySelect(selectBuilder.build(), res -> graphs.add(res.get("g").toString()));


        graphs.stream().filter(graph -> {
            var lastPath = graph.substring(graph.lastIndexOf("/") +1);
            //keep only graphs that do are not versioned (i.e. do not end with a valid semver)
            return !lastPath.matches(SemVer.VALID_REGEX);
        }).forEach(graph -> {
            var model = coreRepository.fetch(graph);
            var modelRes = model.getResource(graph);
            var prefix = MapperUtils.getLiteral(modelRes, DCTerms.identifier, String.class);
            var status = Status.valueOf(MapperUtils.propertyToString(modelRes, OWL.versionInfo));
            var previousVersion = MapperUtils.propertyToString(modelRes, OWL.priorVersion);
            if(status.equals(Status.SUGGESTED)){
                coreRepository.put(graph, model);
                var newDraft = ModelFactory.createDefaultModel().add(model);


                var newVersion = "0.1.0";
                if(previousVersion != null) {
                    var previousNumber = new SemVer(previousVersion.substring(previousVersion.lastIndexOf("/") + 1));
                    newVersion = previousNumber.getMajor() + "." + previousNumber.getMinor() + "." + (previousNumber.getPatch() + 1);
                }

                var versionUri = DataModelURI.Factory.createModelURI(prefix, newVersion);
                modelMapper.mapReleaseProperties(model, versionUri, Status.SUGGESTED);
                //Map new newest release to draft model
                modelMapper.mapPriorVersion(newDraft, graph, versionUri.getGraphURI());
                //unversioned graphs should not be suggested
                var newDraftRes = newDraft.getResource(graph);
                newDraftRes.removeAll(OWL.versionInfo);
                newDraftRes.addProperty(OWL.versionInfo, Status.DRAFT.name());

                coreRepository.put(graph, newDraft);
                coreRepository.put(versionUri.getGraphURI(), model);

                //this migration does not need to index as migration happens before startup indexing
            }
        }
        );

    }
}
