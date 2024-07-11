package com.mkyong.resource;


import com.mkyong.service.MessageService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("graphs/graph/vertices")
public class PathParamAPI {

    // DI via HK2
    @Inject
    private MessageService messageService;

    // output text
    @Path("{id}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String PathParam() {
        return "Jersey hello Path Param!";
    }
}
