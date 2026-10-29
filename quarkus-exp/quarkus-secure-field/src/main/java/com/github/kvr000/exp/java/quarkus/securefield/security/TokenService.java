package com.github.kvr000.exp.java.quarkus.securefield.security;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.Set;


/**
 * Issues signed JWT tokens and the session cookie carrying them.
 *
 * The "session" is just the JWT stored in an HttpOnly cookie; the same token can be used as
 * {@code Authorization: Bearer} header (see smallrye.jwt.always-check-authorization).
 */
@ApplicationScoped
public class TokenService
{
	public static final Duration VALIDITY = Duration.ofHours(8);

	@ConfigProperty(name = "mp.jwt.verify.issuer")
	String issuer;

	@ConfigProperty(name = "mp.jwt.token.cookie")
	String cookieName;

	public String issue(String username, Set<String> roles)
	{
		return Jwt.issuer(issuer)
			.upn(username)
			.subject(username)
			.groups(roles)
			.expiresIn(VALIDITY.toSeconds())
			.sign();
	}

	public NewCookie sessionCookie(String token)
	{
		return new NewCookie.Builder(cookieName)
			.value(token)
			.path("/")
			.httpOnly(true)
			.sameSite(NewCookie.SameSite.LAX)
			.maxAge((int) VALIDITY.toSeconds())
			.build();
	}

	public NewCookie clearCookie()
	{
		return new NewCookie.Builder(cookieName)
			.value("")
			.path("/")
			.httpOnly(true)
			.sameSite(NewCookie.SameSite.LAX)
			.maxAge(0)
			.build();
	}
}
