package com.github.kvr000.exp.java.quarkus.securefield;

import com.github.kvr000.exp.java.quarkus.securefield.security.TokenService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;


@QuarkusTest
public class TicketResourceTest
{
	@Inject
	TokenService tokenService;

	private RequestSpecification as(String... roles)
	{
		return given().header("Authorization", "Bearer " + tokenService.issue("test", Set.of(roles)));
	}

	@Test
	public void list_anonymous_unauthorized()
	{
		given().get("/api/ticket").then().statusCode(401);
	}

	@Test
	public void list_guest_onlyPublicFields()
	{
		as("guest").get("/api/ticket").then()
			.statusCode(200)
			.body("title", everyItem(notNullValue()))
			.body("$", everyItem(not(hasKey("system"))))
			.body("$", everyItem(not(hasKey("views"))))
			.body("$", everyItem(not(hasKey("description"))));
	}

	@Test
	public void get_user_seesDescription()
	{
		as("user").get("/api/ticket/2").then()
			.statusCode(200)
			.body("$", hasKey("description"))
			.body("$", not(hasKey("system")))
			.body("$", not(hasKey("views")));
	}

	@Test
	public void get_super_seesAll()
	{
		as("super").get("/api/ticket/2").then()
			.statusCode(200)
			.body("$", hasKey("system"))
			.body("$", hasKey("views"))
			.body("$", hasKey("description"));
	}

	@Test
	public void get_unknown_notFound()
	{
		as("super").get("/api/ticket/999999").then().statusCode(404);
	}

	@Test
	public void post_guest_forbidden()
	{
		as("guest").contentType(ContentType.JSON).body("{\"title\":\"x\"}")
			.post("/api/ticket").then().statusCode(403);
	}

	@Test
	public void post_userWithSystem_forbidden()
	{
		as("user").contentType(ContentType.JSON).body("{\"title\":\"x\",\"system\":\"billing\"}")
			.post("/api/ticket").then().statusCode(403);
	}

	@Test
	public void post_user_created()
	{
		as("user").contentType(ContentType.JSON).body("{\"title\":\"by user\",\"description\":\"desc\"}")
			.post("/api/ticket").then()
			.statusCode(201)
			.body("id", notNullValue())
			.body("title", equalTo("by user"))
			.body("description", equalTo("desc"))
			.body("$", not(hasKey("views")));
	}

	@Test
	public void post_lenientJson_created()
	{
		as("user").contentType(ContentType.JSON).body("{title: \"lenient\", description: \"no quotes\",}")
			.post("/api/ticket").then()
			.statusCode(201)
			.body("title", equalTo("lenient"))
			.body("description", equalTo("no quotes"));
	}

	@Test
	public void post_unknownField_badRequest()
	{
		as("super").contentType(ContentType.JSON).body("{\"title\":\"x\",\"foo\":1}")
			.post("/api/ticket").then().statusCode(400);
	}

	@Test
	public void patch_views_adminForbidden_superAllowed()
	{
		int id = as("super").contentType(ContentType.JSON).body("{\"title\":\"views test\",\"views\":5}")
			.post("/api/ticket").then().statusCode(201).body("views", equalTo(5))
			.extract().path("id");

		as("admin").contentType(ContentType.JSON).body("{\"views\":100}")
			.patch("/api/ticket/" + id).then().statusCode(403);
		// echoing unchanged value is fine
		as("admin").contentType(ContentType.JSON).body("{\"views\":5,\"system\":\"crm\"}")
			.patch("/api/ticket/" + id).then().statusCode(200)
			.body("views", equalTo(5));
		as("super").contentType(ContentType.JSON).body("{\"views\":100}")
			.patch("/api/ticket/" + id).then().statusCode(200)
			.body("views", equalTo(100))
			.body("title", equalTo("views test"));
	}

	@Test
	public void put_user_preservesRestrictedFields()
	{
		int id = as("admin").contentType(ContentType.JSON).body("{\"title\":\"put test\",\"system\":\"backup\",\"description\":\"d\"}")
			.post("/api/ticket").then().statusCode(201)
			.extract().path("id");

		as("user").contentType(ContentType.JSON).body("{\"title\":\"put test 2\"}")
			.put("/api/ticket/" + id).then().statusCode(200)
			.body("title", equalTo("put test 2"))
			.body("$", hasKey("description"))
			.body("description", equalTo(null));
		as("super").get("/api/ticket/" + id).then().statusCode(200)
			.body("system", equalTo("backup"))
			.body("views", equalTo(1));
	}
}
