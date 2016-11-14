package cz.znj.kvr.sw.exp.java.xml.jaxb;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.output.StringBuilderWriter;
import org.junit.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Attr;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.StringReader;


public class JaxbTest
{
	@Test
	public void testDeserialize()
	{
		TestDeserializeObject read = JAXB.unmarshal(new StringReader(
				"<?xml version='1.0'?>\n"+
				"<testDeserializeObject>\n"+
				" <name>Zbynek</name>\n"+
				" <value>64</value>\n"+
				"</testDeserializeObject>\n"
		), TestDeserializeObject.class);
		Assert.assertEquals("Zbynek", read.name);
		Assert.assertEquals(64, read.value);
	}

	@Test
	public void testSerialize() throws JAXBException
	{
		TestDeserializeObject o = new TestDeserializeObject();
		o.setName("Zbynek");
		o.setValue(64);
		StringBuilderWriter out = new StringBuilderWriter();
		Marshaller marshaller = JAXBContext.newInstance(TestDeserializeObject.class).createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
		//marshaller.setProperty("com.sun.xml.bind.xmlHeaders", "<?xml version=\"1.0\"?>");
		marshaller.marshal(o, out);
		Diff diff = DiffBuilder.compare("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>\n"+
						"<testDeserializeObject>\n"+
						" <name>Zbynek</name>\n"+
						" <value>64</value>\n"+
						"</testDeserializeObject>\n")
				.ignoreWhitespace()
				.withAttributeFilter((Attr attr) -> !attr.getName().equals("standalone"))
				.withTest(out.toString())
				.build();
		Assert.assertFalse(diff.toString(), diff.hasDifferences());
	}

	@Getter
	@Setter
	@XmlRootElement
	public static class TestDeserializeObject
	{
		private String name;
		private int value;
	}
}
