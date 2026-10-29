package com.github.kvr000.exp.java.quarkus.securefield.ticket;

import com.github.kvr000.exp.java.quarkus.securefield.security.Roles;
import com.github.kvr000.exp.java.quarkus.securefield.security.SecureWriteField;
import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Ticket entity.
 *
 * Read access is enforced by Quarkus REST {@link SecureField} during serialization, write access by
 * {@link com.github.kvr000.exp.java.quarkus.securefield.security.SecureFieldWriter} based on {@link SecureWriteField}
 * (found by Jackson introspection on the field backing the getter / setter).
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection
public class Ticket
{
	/** Server assigned. */
	@SecureWriteField
	private Long id;

	@SecureField(rolesAllowed = { Roles.SUPER, Roles.ADMIN })
	@SecureWriteField(rolesAllowed = { Roles.SUPER, Roles.ADMIN })
	private String system;

	private String title;

	/** View counter, incremented by GET /api/ticket/{id}; readable by admin, writable by super only. */
	@SecureField(rolesAllowed = { Roles.SUPER, Roles.ADMIN })
	@SecureWriteField(rolesAllowed = { Roles.SUPER })
	private int views;

	@SecureField(rolesAllowed = { Roles.SUPER, Roles.ADMIN, Roles.USER })
	@SecureWriteField(rolesAllowed = { Roles.SUPER, Roles.ADMIN, Roles.USER })
	private String description;

	public Ticket copy()
	{
		return toBuilder().build();
	}
}
