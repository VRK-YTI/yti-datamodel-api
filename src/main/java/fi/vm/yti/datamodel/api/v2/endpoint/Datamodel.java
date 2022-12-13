package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;

@Component
@Path("v2/model")
@Tag(name = "Model" )
public class Datamodel {

    private static final Logger logger = LoggerFactory.getLogger(Datamodel.class);

    private final JenaService jenaService;
    private final ModelMapper mapper;

    public Datamodel(JenaService jenaService) {
        this.jenaService = jenaService;
        this.mapper = new ModelMapper();
    }

    @PUT
    public void createModel(@RequestBody DataModelDTO modelDTO) {
        logger.info(modelDTO.toString());

        Model jenaModel = mapper.mapToJenaModel(modelDTO);

        jenaService.createDataModel(modelDTO.getId(), jenaModel);
    }

}
