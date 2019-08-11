package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.TestContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.TestRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.ControllerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.NotFoundControllerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.response.UnsupportedRequestControllerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.test.HomeController;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util.MapLookupFunction;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;


/**
 * {@link JaxRsCaptureControllerRouter} tests.
 */
public class JaxRsCaptureControllerRouterTest
{
	private final JaxRsCaptureControllerRouter router = JaxRsCaptureControllerRouter.fromClasspathJaxRsPaths(JaxRsCaptureControllerRouterTest.class, "../test/JaxRsMetadata.xml");

	@Test
	public void testMethodGet()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("", Map.of());
		ControllerResponse response = router.call(exchange, new TestContainerContext(new MapLookupFunction<>(Map.of(HomeController.class, homeController))));
		Assert.assertEquals(response.contentType(), MediaType.TEXT_PLAIN_TYPE);
	}

	@Test
	public void testNotFound()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("/non-existent", Map.of());
		ControllerResponse response = router.call(exchange, new TestContainerContext(new MapLookupFunction<>(Map.of(HomeController.class, homeController))));
		MatcherAssert.assertThat(response, Matchers.instanceOf(NotFoundControllerResponse.class));
	}

	@Test
	public void testUnsupportedMediaType()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("", Map.of(HttpHeaders.ACCEPT, List.of("wrong/unknown")));
		ControllerResponse response = router.call(exchange, new TestContainerContext(new MapLookupFunction<>(Map.of(HomeController.class, homeController))));
		MatcherAssert.assertThat(response, Matchers.instanceOf(UnsupportedRequestControllerResponse.class));
	}

	@Test
	public void testUnsupportedMethod()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = new TestRequestExchange("OPTIONS", "", Map.of("accept", List.of("text/plain")), null);
		ControllerResponse response = router.call(exchange, new TestContainerContext(new MapLookupFunction<>(Map.of(HomeController.class, homeController))));
		MatcherAssert.assertThat(response, Matchers.instanceOf(UnsupportedRequestControllerResponse.class));
	}
}
