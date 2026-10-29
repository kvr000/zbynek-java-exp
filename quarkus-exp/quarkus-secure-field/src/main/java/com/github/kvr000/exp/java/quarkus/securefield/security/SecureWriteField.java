package com.github.kvr000.exp.java.quarkus.securefield.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Write counterpart of {@link io.quarkus.resteasy.reactive.jackson.SecureField}: restricts which roles can set the
 * property via {@link SecureFieldWriter}.
 *
 * Can be placed on the setter, the field or the getter of a property (looked up in this order, similarly to
 * Jackson annotations).  Properties without this annotation are writable by anyone who can access the endpoint.
 * Empty {@link #rolesAllowed()} means the property is read-only for everyone (server managed).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface SecureWriteField
{
	String[] rolesAllowed() default {};
}
