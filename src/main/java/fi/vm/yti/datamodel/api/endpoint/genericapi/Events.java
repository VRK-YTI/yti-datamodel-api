/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;
import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.SseBroadcaster;
import org.glassfish.jersey.media.sse.SseFeature;
import org.springframework.stereotype.Component;

@Deprecated
@Component
@Singleton
@Path("events")
@Api(tags = {"Deprecated"}, description = "Event API not working?")
public class Events {

    private static final Logger logger = LoggerFactory.getLogger(Events.class.getName());

    private final SseBroadcaster broadcaster = new SseBroadcaster();

    @GET
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput listenToBroadcast() {
        final EventOutput eventOutput = new EventOutput();
        this.broadcaster.add(eventOutput);
        return eventOutput;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void post(String message) {

        OutboundEvent event = new OutboundEvent.Builder().name("message")
                .data(String.class, message)
                .mediaType(MediaType.APPLICATION_JSON_TYPE).build();

        broadcaster.broadcast(event);
    }
}