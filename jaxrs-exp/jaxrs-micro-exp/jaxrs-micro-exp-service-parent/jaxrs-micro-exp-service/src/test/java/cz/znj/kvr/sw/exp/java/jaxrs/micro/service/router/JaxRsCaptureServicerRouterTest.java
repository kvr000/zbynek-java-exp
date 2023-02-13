package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router;

import com.google.inject.Key;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.TestContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.TestRequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.ServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.NotFoundServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.UnsupportedRequestServicerResponse;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.test.HomeController;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;


/**
 * {@link JaxRsCaptureServicerRouter} tests.
 */
public class JaxRsCaptureServicerRouterTest
{
	private final JaxRsCaptureServicerRouter router = JaxRsCaptureServicerRouter.fromClasspathJaxRsPaths(JaxRsCaptureServicerRouterTest.class, "../test/JaxRsMetadata.xml");

	@Test
	public void testMethodGet()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("", Map.of());
		ServicerResponse response = router.call(exchange, new TestContainerContext(Map.of(Key.get(HomeController.class), homeController))).join();
		Assert.assertEquals(response.contentType(), MediaType.TEXT_PLAIN_TYPE);
	}

	@Test
	public void testNotFound()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("/non-existent", Map.of());
		ServicerResponse response = router.call(exchange, new TestContainerContext(Map.of(Key.get(HomeController.class), homeController))).join();
		MatcherAssert.assertThat(response, Matchers.instanceOf(NotFoundServicerResponse.class));
	}

	@Test
	public void testUnsupportedMediaType()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = TestRequestExchange.fromGet("", Map.of(HttpHeaders.ACCEPT, List.of("wrong/unknown")));
		ServicerResponse response = router.call(exchange, new TestContainerContext(Map.of(Key.get(HomeController.class), homeController))).join();
		MatcherAssert.assertThat(response, Matchers.instanceOf(UnsupportedRequestServicerResponse.class));
	}

	@Test
	public void testUnsupportedMethod()
	{
		HomeController homeController = new HomeController();
		TestRequestExchange exchange = new TestRequestExchange("OPTIONS", "", Map.of("accept", List.of("text/plain")), null);
		ServicerResponse response = router.call(exchange, new TestContainerContext(Map.of(Key.get(HomeController.class), homeController))).join();
		MatcherAssert.assertThat(response, Matchers.instanceOf(UnsupportedRequestServicerResponse.class));
	}
}
