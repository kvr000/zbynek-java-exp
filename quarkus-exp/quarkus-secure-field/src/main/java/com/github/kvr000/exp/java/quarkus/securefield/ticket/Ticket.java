package com.github.kvr000.exp.java.quarkus.securefield.ticket;

import com.github.kvr000.exp.java.quarkus.securefield.security.Roles;
import com.github.kvr000.exp.java.quarkus.securefield.security.SecureWriteField;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;


/**
 * Ticket entity.
 *
 * Read access is enforced by Quarkus REST {@link SecureField} during serialization, write access by
 * {@link com.github.kvr000.exp.java.quarkus.securefield.security.SecureFieldWriter} based on {@link SecureWriteField}.
 */
@RegisterForReflection
@Data
public class Ticket
{
	/** Server assigned. */
	@SecureWriteField
	public Long id;

	@SecureField(rolesAllowed = { Roles.SUPER })
	@SecureWriteField(rolesAllowed = { Roles.SUPER, Roles.ADMIN })
	public String system;

	public String title;

	/** View counter, incremented by GET /api/ticket/{id}; readable by admin, writable by super only. */
	@SecureField(rolesAllowed = { Roles.SUPER, Roles.ADMIN })
	@SecureWriteField(rolesAllowed = { Roles.SUPER })
	public int views;

	@SecureField(rolesAllowed = { Roles.SUPER, Roles.ADMIN, Roles.USER })
	@SecureWriteField(rolesAllowed = { Roles.SUPER, Roles.ADMIN, Roles.USER })
	public String description;

	public Ticket()
	{
	}

	public Ticket(Long id, String system, String title, int views, String description)
	{
		this.id = id;
		this.system = system;
		this.title = title;
		this.views = views;
		this.description = description;
	}

	public Ticket copy()
	{
		return new Ticket(id, system, title, views, description);
	}
}
