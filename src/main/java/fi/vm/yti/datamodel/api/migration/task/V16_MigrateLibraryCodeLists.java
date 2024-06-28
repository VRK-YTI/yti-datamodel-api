package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.migration.V1DataMapper;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.migration.MigrationTask;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.OWL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Arrays;

@Component
@SuppressWarnings("java:S101")
public class V16_MigrateLibraryCodeLists implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V16_MigrateLibraryCodeLists.class);

    private static final Property CODE_LIST =
            ResourceFactory.createProperty("http://purl.org/dc/dcam/memberOf");

    @Value("${datamodel.v1.migration.url:https://tietomallit.dev.yti.cloud.dvv.fi}")
    String serviceURL;

    @Value("${datamodel.v1.migration.librariesWithCodeLists:}")
    String modelIds;

    private final CoreRepository coreRepository;

    public V16_MigrateLibraryCodeLists(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public void migrate() {
        if (modelIds.isBlank()) {
            return;
        }

        Arrays.asList(modelIds.split(",")).forEach(prefix -> {
            var oldData = ModelFactory.createDefaultModel();
            var modelURI = "http://uri.suomi.fi/datamodel/ns/" + prefix;
            RDFParser.create()
                    .source(serviceURL + "/datamodel-api/api/v1/exportModel?graph=" + DataModelUtils.encode(modelURI))
                    .lang(Lang.JSONLD)
                    .acceptHeader("application/ld+json")
                    .parse(oldData);
            addCodeList(prefix, oldData, DataModelURI.createModelURI(prefix));
        });
    }

    private void addCodeList(String prefix, Model oldData, DataModelURI newModelURI) {
        LOG.info("Add code list to model {}", newModelURI.getGraphURI());
        Model model;
        try {
            model = coreRepository.fetch(newModelURI.getGraphURI());
        } catch (ResourceNotFoundException e) {
            // model not migrated
            LOG.warn("Model {} not found", prefix);
            return;
        }

        oldData.listSubjectsWithProperty(CODE_LIST).forEach(attr -> {
            var path = V1DataMapper.fixNamespace(MapperUtils.propertyToString(attr, SH.path));
            var stmt = oldData.listStatements(null, SH.property, attr);

            if (!stmt.hasNext() || path == null) {
                LOG.warn("Missing data, could not add code list for attribute {} in graph {}", attr.getURI(), newModelURI.getGraphURI());
                return;
            }
            var clazz = stmt.next().getSubject();

            var resourceURI = DataModelURI.createResourceURI(prefix, clazz.getLocalName());
            var classResource = model.getResource(resourceURI.getResourceURI());

            if (classResource.listProperties().hasNext()) {
                var restrictionResource = ClassMapper.getClassRestrictionList(model, classResource).stream()
                        .filter(r -> {
                            // remove trailing _ (renamed in migration -> not exist in original data)
                            var onProperty = DataModelURI
                                    .fromURI(MapperUtils.propertyToString(r, OWL.onProperty))
                                    .getResourceURI()
                                    .replaceAll("_$", "");
                            return path.equals(onProperty);
                        })
                        .findFirst();

                restrictionResource.ifPresentOrElse(
                        r -> MapperUtils.arrayPropertyToSet(attr, CODE_LIST)
                                .forEach(codeList -> r.addProperty(SuomiMeta.codeList, codeList)),
                        () -> LOG.warn("Restriction for path {} not added to class {}", path, classResource.getURI()));
            }
        });

        coreRepository.put(newModelURI.getGraphURI(), model);

        // add code lists to versions
        var priorVersion = MapperUtils.propertyToString(model.getResource(newModelURI.getModelURI()), OWL.priorVersion);

        if (priorVersion != null) {
            addCodeList(prefix, oldData, DataModelURI.fromURI(priorVersion));
        }
    }
}
