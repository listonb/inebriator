package com.inebriator.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.inebriator.InebriatorWrapperListener;

@Path("/inebriator/pour/cocktail/{name}")
public class PourCocktailResource {
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response pour(@PathParam("name") String name) {
		try {
			InebriatorWrapperListener.inebriator.pourCocktail(name);
		} catch (Exception e) {
			return Response.serverError().entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
		}

		return Response.ok().entity("One " + name + " has been served").type(MediaType.TEXT_PLAIN).build();
	}
}
