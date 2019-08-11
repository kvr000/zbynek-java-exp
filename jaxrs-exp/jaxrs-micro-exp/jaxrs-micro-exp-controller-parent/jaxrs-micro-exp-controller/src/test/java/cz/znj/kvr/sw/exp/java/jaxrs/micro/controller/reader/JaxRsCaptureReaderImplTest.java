package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader;

import org.testng.Assert;
import org.testng.annotations.Test;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;


/**
 * {@link JaxRsCaptureReaderImpl} tests.
 */
public class JaxRsCaptureReaderImplTest
{
	@Test
	public void testReader() throws IOException
	{
		try (
				InputStream inputStream = Objects.requireNonNull(getClass().getResourceAsStream("JaxRsMetadata.xml"), "Cannot open JaxRsMetadata.xml");
				JaxRsCaptureReader reader = new JaxRsCaptureReaderImpl(inputStream)
		) {
			{
				ControllerMeta controller = reader.next();
				Assert.assertNotNull(controller, "controller");
				Assert.assertEquals(controller.getClassName(), "test.HomeController");
				Assert.assertEquals(controller.getPath(), "/");
				Assert.assertEquals(controller.getMethods().size(), 1);
				{
					MethodMeta method = controller.getMethods().get(0);
					Assert.assertEquals(method.getPath(), "/");
					Assert.assertEquals(method.getFunction(), "homePage()");
					Assert.assertEquals(method.getMethods(), Arrays.asList("GET"));
				}
			}
			{
				ControllerMeta controller = reader.next();
				Assert.assertNotNull(controller, "controller");
				Assert.assertEquals(controller.getClassName(), "test.sub.RestController");
				Assert.assertEquals(controller.getPath(), "/sub");
				Assert.assertEquals(controller.getMethods().size(), 5);
				{
					MethodMeta method = controller.getMethods().get(4);
					Assert.assertEquals(method.getPath(), "/both");
					Assert.assertEquals(method.getFunction(), "pageMulti()");
					Assert.assertEquals(method.getMethods(), Arrays.asList("GET", "POST"));
				}
			}
			{
				ControllerMeta controller = reader.next();
				Assert.assertNull(controller, "controller list end");
			}
		}
	}
}
