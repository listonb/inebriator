package com.inebriator.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gson.GsonBuilder;
import com.inebriator.InebriatorWrapperListener;

@Path("/inebriator/ingredients")
public class IngredientsResource {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public String getIngredients() {
		return new GsonBuilder().setPrettyPrinting().create().toJson(InebriatorWrapperListener.inebriator.getSolenoidsByName()) + "\n";
	}
}
