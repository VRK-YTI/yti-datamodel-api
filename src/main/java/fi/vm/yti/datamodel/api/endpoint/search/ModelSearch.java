package fi.vm.yti.datamodel.api.endpoint.search;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.ModelQueryFactory;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import fi.vm.yti.datamodel.api.index.model.ModelSearchRequest;
import fi.vm.yti.datamodel.api.index.model.ModelSearchResponse;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

@Component
@Path("/searchModels")
@Api(tags = { "Index" })
public class ModelSearch {

    private static final Logger logger = LoggerFactory.getLogger(ModelSearch.class.getName());
    private SearchIndexManager searchIndexManager;
    private JerseyResponseManager jerseyResponseManager;
    private ObjectMapper objectMapper;

    @Autowired
    public ModelSearch(SearchIndexManager searchIndexManager,
                       JerseyResponseManager jerseyResponseManager,
                       ObjectMapper objectMapper) {
        this.searchIndexManager = searchIndexManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.objectMapper = objectMapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response searchModels(ModelSearchRequest request) {
        ModelSearchResponse response = searchIndexManager.searchModels(request);
        return jerseyResponseManager.ok(objectMapper.valueToTree(response));
    }

}
