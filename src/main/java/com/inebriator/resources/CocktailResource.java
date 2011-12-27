package com.inebriator.resources;

import java.net.URISyntaxException;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inebriator.Cocktail;
import com.inebriator.InebriatorWrapperListener;

@Path("/inebriator/cocktail/{name}")
public class CocktailResource {

	private static final Logger LOG = LoggerFactory.getLogger(CocktailResource.class);

  @Context
  UriInfo uriInfo;

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addCocktail(@PathParam("name") String name, byte[] data) throws URISyntaxException {
		String cocktailJson = new String(data);
		LOG.info("Received definition for cocktail {}: {}", name, cocktailJson);
		Cocktail cocktail = new Gson().fromJson(new String(data), Cocktail.class);
		LOG.info("Created Cocktail {}", cocktail);
		InebriatorWrapperListener.inebriator.addCocktailDefinition(name, cocktail);
		return Response.created(uriInfo.getRequestUri()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getCocktail(@PathParam("name") String name) {
		Cocktail cocktail = InebriatorWrapperListener.inebriator.getCocktailDefinition(name);
		return new GsonBuilder().setPrettyPrinting().create().toJson(cocktail) + "\n";
	}

	@DELETE
	public void deleteCocktail(@PathParam("name") String name) {
		LOG.info("Deleting definition for cocktail {}", name);
		InebriatorWrapperListener.inebriator.deleteCocktailDefinition(name);
	}
}
