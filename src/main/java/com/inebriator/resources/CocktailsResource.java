package com.inebriator.resources;

import java.net.URISyntaxException;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.GET;
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

@Path("/inebriator/cocktails")
public class CocktailsResource {

	private static final Logger LOG = LoggerFactory.getLogger(CocktailResource.class);

  @Context
  UriInfo uriInfo;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getCocktail() {
		Set<String> names = InebriatorWrapperListener.inebriator.getAvailableCocktailNames();
		return new GsonBuilder().setPrettyPrinting().create().toJson(names) + "\n";
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addCocktail(byte[] data) throws URISyntaxException {
		String cocktailJson = new String(data);
		LOG.info("Received definition for cocktail {}: {}", cocktailJson);
		Cocktail cocktail = new Gson().fromJson(new String(data), Cocktail.class);
		LOG.info("Created Cocktail {}", cocktail);
		InebriatorWrapperListener.inebriator.addCocktailDefinition(cocktail.getName(), cocktail);
		return Response.created(uriInfo.getRequestUri()).build();
	}
}
