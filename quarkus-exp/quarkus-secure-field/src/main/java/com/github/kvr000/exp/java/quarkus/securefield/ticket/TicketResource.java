package com.github.kvr000.exp.java.quarkus.securefield.ticket;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kvr000.exp.java.quarkus.securefield.security.Roles;
import com.github.kvr000.exp.java.quarkus.securefield.security.SecureFieldWriter;
import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;


/**
 * Ticket REST API.  Any role can read, guest cannot write.  Field visibility is filtered by
 * {@link io.quarkus.resteasy.reactive.jackson.SecureField} on {@link Ticket}, so the methods must return
 * {@code Ticket} or {@code List<Ticket>} directly (not {@code Response}) for the filtering to be applied.
 */
@Path("/api/ticket")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({ Roles.SUPER, Roles.ADMIN, Roles.USER, Roles.GUEST })
@RequiredArgsConstructor
public class TicketResource
{
	public static final String MERGE_PATCH_JSON = "application/merge-patch+json";

	private final TicketRepository repository;

	private final SecureFieldWriter writer;

	/** Request scoped (proxied); getIdentity() returns the real immutable identity of the current request. */
	private final CurrentIdentityAssociation identityAssociation;

	@GET
	public List<Ticket> list()
	{
		return repository.list();
	}

	@GET
	@Path("{id}")
	public Ticket get(@PathParam("id") long id)
	{
		return repository.view(id)
			.orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
	}

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ Roles.SUPER, Roles.ADMIN, Roles.USER })
	@ResponseStatus(201)
	public Ticket create(ObjectNode body)
	{
		Ticket ticket = writer.apply(new Ticket(), requireBody(body), SecureFieldWriter.Mode.CREATE, identityAssociation.getIdentity());
		validate(ticket);
		return repository.create(ticket);
	}

	@PUT
	@Path("{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@RolesAllowed({ Roles.SUPER, Roles.ADMIN, Roles.USER })
	public Ticket replace(@PathParam("id") long id, ObjectNode body)
	{
		return modify(id, body, SecureFieldWriter.Mode.REPLACE);
	}

	@PATCH
	@Path("{id}")
	@Consumes({ MediaType.APPLICATION_JSON, MERGE_PATCH_JSON })
	@RolesAllowed({ Roles.SUPER, Roles.ADMIN, Roles.USER })
	public Ticket patch(@PathParam("id") long id, ObjectNode body)
	{
		return modify(id, body, SecureFieldWriter.Mode.MERGE);
	}

	private Ticket modify(long id, ObjectNode body, SecureFieldWriter.Mode mode)
	{
		ObjectNode input = requireBody(body);
		SecurityIdentity identity = identityAssociation.getIdentity();
		return repository.update(id, old -> {
				Ticket ticket = writer.apply(old, input, mode, identity);
				validate(ticket);
				return ticket;
			})
			.orElseThrow(() -> new NotFoundException("Ticket not found: " + id));
	}

	private static ObjectNode requireBody(ObjectNode body)
	{
		if (body == null) {
			throw SecureFieldWriter.error(Response.Status.BAD_REQUEST, "Missing JSON object body");
		}
		return body;
	}

	private static void validate(Ticket ticket)
	{
		if (ticket.getTitle() == null || ticket.getTitle().isBlank()) {
			throw SecureFieldWriter.error(Response.Status.BAD_REQUEST, "title is required");
		}
	}
}
