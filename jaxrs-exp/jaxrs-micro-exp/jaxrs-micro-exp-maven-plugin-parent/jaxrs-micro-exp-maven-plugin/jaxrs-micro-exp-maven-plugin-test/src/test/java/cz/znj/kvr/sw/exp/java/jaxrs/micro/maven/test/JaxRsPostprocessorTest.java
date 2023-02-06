package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.test;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.io.InputStream;


/**
 * Postprocessor result test.
 */
public class JaxRsPostprocessorTest
{
	@Test
	public void testResult() throws IOException
	{
		try (
				InputStream generated = getClass().getResourceAsStream("/cz/znj/kvr/sw/exp/java/jaxrs/micro/maven/test/JaxRsMetadata.xml");
				InputStream expected = getClass().getResourceAsStream("expected-JaxRsMetadata.xml")
		) {
			Assert.assertNotNull(generated, "Generated file does not exist");
			Assert.assertNotNull(expected, "Expected file does not exist");
			Diff diff = DiffBuilder.compare(expected)
					.ignoreWhitespace()
					.withTest(generated)
					.build();
			Assert.assertFalse(diff.hasDifferences(), diff.toString());
		}
	}
}
