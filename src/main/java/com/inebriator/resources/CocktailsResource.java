package com.inebriator.resources;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gson.GsonBuilder;
import com.inebriator.InebriatorWrapperListener;

@Path("/inebriator/cocktails")
public class CocktailsResource {

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getCocktail(@PathParam("name") String name) {
		Set<String> names = InebriatorWrapperListener.inebriator.getAvailableCocktailNames();
		return new GsonBuilder().setPrettyPrinting().create().toJson(names) + "\n";
	}
}
