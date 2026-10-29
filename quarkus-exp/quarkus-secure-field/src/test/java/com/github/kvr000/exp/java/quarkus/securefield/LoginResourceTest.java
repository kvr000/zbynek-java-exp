package com.github.kvr000.exp.java.quarkus.securefield;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;


@QuarkusTest
public class LoginResourceTest
{
	private static final String COOKIE = "qsf-token";

	@Test
	public void index_available()
	{
		given().get("/").then().statusCode(200).body(containsString("/login/roles"));
	}

	@Test
	public void login_wrongPassword_unauthorized()
	{
		given().redirects().follow(false)
			.formParam("username", "super").formParam("password", "wrong")
			.post("/login").then().statusCode(401).body(containsString("Invalid username or password"));
	}

	@Test
	public void roles_anonymous_redirectsToLogin()
	{
		given().redirects().follow(false).get("/login/roles").then()
			.statusCode(303).header("Location", endsWith("/login"));
	}

	@Test
	public void loginAndSwitchRoles()
	{
		String superToken = given().redirects().follow(false)
			.formParam("username", "super").formParam("password", "super")
			.post("/login").then().statusCode(303)
			.header("Location", endsWith("/login/roles"))
			.extract().cookie(COOKIE);

		given().cookie(COOKIE, superToken).get("/login/roles").then()
			.statusCode(200)
			.body(containsString(superToken))
			.body(containsString("value=\"super\" checked"));
		given().cookie(COOKIE, superToken).get("/api/ticket/1").then()
			.statusCode(200).body(containsString("\"system\""));

		String guestToken = given().redirects().follow(false).cookie(COOKIE, superToken)
			.formParam("roles", "guest")
			.post("/login/roles").then().statusCode(303)
			.extract().cookie(COOKIE);

		given().cookie(COOKIE, guestToken).get("/api/ticket/1").then()
			.statusCode(200).body(not(containsString("\"system\""))).body(not(containsString("\"description\"")));
		// the same token works as Bearer
		given().header("Authorization", "Bearer " + guestToken).get("/api/ticket/1").then()
			.statusCode(200).body(not(containsString("\"system\"")));
	}
}
