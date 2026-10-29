package com.github.kvr000.exp.java.quarkus.securefield.security;

import java.util.List;


/**
 * Roles known to the application.
 */
public final class Roles
{
	public static final String SUPER = "super";
	public static final String ADMIN = "admin";
	public static final String USER = "user";
	public static final String GUEST = "guest";

	public static final List<String> ALL = List.of(SUPER, ADMIN, USER, GUEST);

	private Roles()
	{
	}
}
