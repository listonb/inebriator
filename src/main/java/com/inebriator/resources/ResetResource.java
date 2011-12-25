package com.inebriator.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.inebriator.InebriatorWrapperListener;

@Path("/inebriator/reset")
public class ResetResource {

	@GET
	public Response reset() {
		InebriatorWrapperListener.inebriator.reset();
		return Response.noContent().build();
	}
}
