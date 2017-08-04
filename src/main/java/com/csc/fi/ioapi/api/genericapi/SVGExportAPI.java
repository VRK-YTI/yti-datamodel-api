/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.genericapi;

import de.bripkens.svgexport.Format;
import de.bripkens.svgexport.SVGExport;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author malonen
 */
@Path("svg")
@Api(tags = {"Deprecated"}, description = "SVG to ... ")
public class SVGExportAPI {
    
    /* TODO: not working? */
   
  private static final Logger logger = Logger.getLogger(SVGExportAPI.class.getName());

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK"),
            @ApiResponse(code = 400, message = "Err")
        })
	public Response svgToFile(
		@FormDataParam("file") InputStream uploadedInputStream,
		@FormDataParam("file") FormDataContentDisposition fileDetail,
                @ApiParam(value = "export-type", required = true, allowableValues = "jpg,png,pdf") @QueryParam("export-type") String type) {
            
                if(type==null) return Response.serverError().build();
                
                Format f = null;
                
                if(type.equals("jpg")) f = Format.JPEG;
                if(type.equals("png")) f = Format.PNG;
                if(type.equals("pdf")) f = Format.PDF;
                    
                String filename = "export."+type;
                
                ByteArrayOutputStream bOutput = new ByteArrayOutputStream();
                
                new SVGExport().setInput(uploadedInputStream)
                .setOutput(bOutput)
                .setTranscoder(f)
                .transcode();
                
                logger.info(filename);
                logger.info(""+bOutput.size());
                        
                ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                 .fileName(filename).creationDate(new Date()).build();
                
		return Response.ok(bOutput.toByteArray())
                .header("Content-Disposition",contentDisposition).build();

	}

    
}
