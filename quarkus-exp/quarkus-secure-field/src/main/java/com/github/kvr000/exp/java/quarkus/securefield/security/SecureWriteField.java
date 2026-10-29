package com.github.kvr000.exp.java.quarkus.securefield.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Write counterpart of {@link io.quarkus.resteasy.reactive.jackson.SecureField}: restricts which roles can set the
 * field via {@link SecureFieldWriter}.
 *
 * Fields without this annotation are writable by anyone who can access the endpoint.  Empty {@link #rolesAllowed()}
 * means the field is read-only for everyone (server managed).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SecureWriteField
{
	String[] rolesAllowed() default {};
}
