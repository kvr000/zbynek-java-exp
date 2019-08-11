package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.writer;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.Configuration;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsMethodMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.util.FileUtil;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;


/**
 * {@link JaxRsWriter} which writes into .properties files.
 */
public class XmlJaxRsWriter implements JaxRsWriter
{
	@Override
	public void writeJaxRs(Configuration configuration, List<JaxRsClassMeta> jaxRs) throws IOException
	{
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
		try (Writer osWriter = new OutputStreamWriter(output, StandardCharsets.UTF_8)) {
			XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(osWriter);

			writer.writeStartDocument();
			writer.writeCharacters("\n");
			writer.writeStartElement("jaxrs-paths");
			writer.writeDefaultNamespace(jaxrspath_SCHEMA);
			writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
			writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation", jaxrspath_SCHEMA+" "+jaxrspath_LOCATION);
			writer.writeCharacters("\n");
			writer.writeCharacters("\t");
			writer.writeStartElement("controllers");
			writer.writeCharacters("\n");
			for (JaxRsClassMeta classMeta: jaxRs) {
				writer.writeCharacters("\t\t");
				writer.writeStartElement("controller");
				writer.writeAttribute("path", classMeta.getPath());
				writer.writeAttribute("class", classMeta.getClassName());
				writer.writeCharacters("\n");
				writer.writeCharacters("\t\t\t");
				writer.writeStartElement("methods");
				writer.writeCharacters("\n");
				for (JaxRsMethodMeta methodMeta: classMeta.getMethods()) {
					writer.writeCharacters("\t\t\t\t");
					writer.writeStartElement("method");
					writer.writeAttribute("path", methodMeta.getPath());
					writer.writeAttribute("method", methodMeta.getMethod());
					writer.writeAttribute("function", methodMeta.getFunction());
					writer.writeCharacters("\n");
					writer.writeCharacters("\t\t\t\t");
					writer.writeEndElement(); // method
					writer.writeCharacters("\n");
				}
				writer.writeCharacters("\t\t\t");
				writer.writeEndElement(); // methods
				writer.writeCharacters("\n");
				writer.writeCharacters("\t\t");
				writer.writeEndElement(); // controller
				writer.writeCharacters("\n");
			}
			writer.writeCharacters("\t");
			writer.writeEndElement(); // controllers
			writer.writeCharacters("\n");
			writer.writeEndElement(); // jaxrs-paths
			writer.writeCharacters("\n");
			writer.writeEndDocument();
			writer.close();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}

		FileUtil.updateFile(configuration.getOutput(), output.toByteArray());
	}

	private static final String jaxrspath_SCHEMA = "http://github.com/kvr000/zbynek-java-exp/jaxrs-exp/jaxrs-micro-exp/schema/jaxrs-paths";
	private static final String jaxrspath_LOCATION = "http://github.com/kvr000/zbynek-java-exp/raw/master/jaxrs-exp/jaxrs-micro-exp/schema/jaxrs-paths-1.0.0.xsd";
}
