package cz.znj.kvr.sw.exp.java.benchmark.deserialize;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;
import org.glassfish.json.JsonProviderImpl;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import javax.json.stream.JsonParser;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;


/**
 * Set of benchmarks measuring deserialization of simple data structure for various serialization formats.
 *
 * Benchmarks use low-level access to parsers to test parsers pure performance, not including any high level data
 * binding, annotations etc.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
public class DeserializeBenchmark
{
	public static final int ELEMENT_NUM = 100;

	private static byte[] propertiesContent = readResource("DeserializeProperties.properties");

	private static byte[] xmlContent = readResource("DeserializeXml.xml");

	private static byte[] jsonContent = readResource("DeserializeJson.json");

	private static final JsonFactory jacksonFactory = new JsonFactory();

	/**
	 * Properties loader, looking for specific keys.
	 */
	@Benchmark
	public void benchmarkDeserializePropertiesLookup() throws IOException
	{
		Map<String, Element> elements = new HashMap<>();
		Properties properties = new Properties();
		try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(propertiesContent))) {
			properties.load(reader);
		}

		for (int i = 0; i < ELEMENT_NUM; ++i) {
			Element el = new Element();
			el.key = String.format("key%02d", i);
			el.content = properties.getProperty(el.key+".content");
			el.property = properties.getProperty(el.key+".property");
			el.other = properties.getProperty(el.key+".other");
			elements.put(el.key, el);
		}
	}

	/**
	 * Properties loader, iterating the keys.
	 */
	@Benchmark
	@SuppressWarnings("unchecked")
	public void benchmarkDeserializePropertiesIterate() throws IOException
	{
		Map<String, Element> elements = new HashMap<>();
		Properties properties = new Properties();
		try (InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(propertiesContent))) {
			properties.load(reader);
		}

		((Map<String, String>) (Object) properties).entrySet().stream()
				.filter(e -> e.getKey().endsWith(".content"))
				.forEach(e -> {
					Element el = new Element();
					el.key = e.getKey().substring(0, e.getKey().length()-8);
					el.content = e.getValue();
					el.property = properties.getProperty(el.key+".property");
					el.other = properties.getProperty(el.key+".other");
					elements.put(el.key, el);
				});
	}

	/**
	 * XML Core Java StaX parser.
	 */
	@Benchmark
	public void benchmarkDeserializeXmlStaxCoreJava() throws IOException, XMLStreamException
	{
		Map<String, Element> elements = new HashMap<>();
		try (InputStream inputStream = new ByteArrayInputStream(xmlContent)) {
			XMLEventReader eventStream = XMLInputFactory.newInstance().createXMLEventReader(inputStream);
			StartElement event = nextElementEvent(eventStream);
			if (!event.getName().equals(DATA_EL_QNAME)) {
				throw new IOException("Expected data element at the root");
			}
			for (;;) {
				StartElement elementEl = nextElementEvent(eventStream);
				if (elementEl == null)
					break;
				if (elementEl.getName().equals(ELEMENT_EL_QNAME)) {
					Element el = new Element();
					el.key = mandatoryAttribute(elementEl, KEY_ATTR_QNAME);
					el.property = mandatoryAttribute(elementEl, PROPERTY_ATTR_QNAME);
					el.other = mandatoryAttribute(elementEl, OTHER_ATTR_QNAME);
					el.content = eventStream.getElementText();
					elements.put(el.key, el);
				}
				else {
					throw new IOException("Unexpected element "+elementEl);
				}
			}
		}
	}

	/**
	 * JSON parser specified by JSR-353 and provided by Glassfish. The benchmark is slightly misleading because
	 * it avoids ServiceLoader work. Json wrapper class currently doesn't cache provider.
	 *
	 * Using ServiceLoader, the benchmark is about 15% slower, using cached instance about 10% faster.
	 */
	@Benchmark
	public void benchmarkDeserializeJsonJsr353Glassfish() throws IOException
	{
		Map<String, Element> elements = new HashMap<>();
		try (
				InputStream inputStream = new ByteArrayInputStream(jsonContent);
				JsonParser parser = new JsonProviderImpl().createParser(inputStream)
		) {
			JsonParser.Event rootEvent = parser.next();
			if (rootEvent != JsonParser.Event.START_OBJECT) {
				throw new IOException("Expected object element at the root");
			}
			for (;;) {
				JsonParser.Event elementEvent = parser.next();
				if (elementEvent == JsonParser.Event.END_OBJECT) {
					break;
				}
				if (elementEvent != JsonParser.Event.KEY_NAME) {
					throw new IOException("Expected key name in root array");
				}

				Element el = new Element();
				el.key = parser.getString();

				JsonParser.Event objectEvent = parser.next();
				if (objectEvent != JsonParser.Event.START_OBJECT) {
					throw new IOException("Expected object element in root array");
				}

				for (;;) {
					JsonParser.Event keyEvent = parser.next();
					if (keyEvent == JsonParser.Event.END_OBJECT) {
						break;
					}
					if (keyEvent != JsonParser.Event.KEY_NAME) {
						throw new IOException("Expected key in element definition");
					}
					String keyName = parser.getString();
					JsonParser.Event valueEvent = parser.next();
					if (valueEvent != JsonParser.Event.VALUE_STRING) {
						throw new IOException("Expected value in element definition for key "+keyName);
					}
					String keyValue = parser.getString();
					switch (keyName) {
					case "property":
						el.property = keyValue;
						break;
					case "other":
						el.other = keyValue;
						break;
					case "content":
						el.content = keyValue;
						break;
					default:
						throw new IOException("Unexpected key in element definition: "+keyName);
					}
				}
				if (el.key == null || el.property == null || el.other == null || el.content == null) {
					throw new IOException("Incomplete content of element: "+el);
				}
				elements.put(el.key, el);
			}
		}
	}

	/**
	 * JSON parser using Jackson library and cached factory.
	 */
	@Benchmark
	public void benchmarkDeserializeJsonJackson() throws IOException
	{
		Map<String, Element> elements = new HashMap<>();
		try (
				InputStream inputStream = new ByteArrayInputStream(jsonContent);
				com.fasterxml.jackson.core.JsonParser parser = jacksonFactory.createParser(inputStream)
		) {
			JsonToken rootEvent = parser.nextToken();
			if (rootEvent != JsonToken.START_OBJECT) {
				throw new IOException("Expected object element at the root");
			}
			for (;;) {
				JsonToken elementEvent = parser.nextToken();
				if (elementEvent == JsonToken.END_OBJECT) {
					break;
				}
				if (elementEvent != JsonToken.FIELD_NAME) {
					throw new IOException("Expected key name in root array");
				}

				Element el = new Element();
				el.key = parser.getCurrentName();

				JsonToken objectEvent = parser.nextToken();
				if (objectEvent != JsonToken.START_OBJECT) {
					throw new IOException("Expected object element in root array");
				}

				for (;;) {
					JsonToken keyEvent = parser.nextToken();
					if (keyEvent == JsonToken.END_OBJECT) {
						break;
					}
					if (keyEvent != JsonToken.FIELD_NAME) {
						throw new IOException("Expected key in element definition");
					}
					String keyName = parser.getCurrentName();
					JsonToken valueEvent = parser.nextToken();
					if (valueEvent != JsonToken.VALUE_STRING) {
						throw new IOException("Expected value in element definition for key "+keyName);
					}
					String keyValue = parser.getText();
					switch (keyName) {
					case "property":
						el.property = keyValue;
						break;
					case "other":
						el.other = keyValue;
						break;
					case "content":
						el.content = keyValue;
						break;
					default:
						throw new IOException("Unexpected key in element definition: "+keyName);
					}
				}
				if (el.key == null || el.property == null || el.other == null || el.content == null) {
					throw new IOException("Incomplete content of element: "+el);
				}
				elements.put(el.key, el);
			}
		}
	}

	/**
	 * JSON parser using Gson library JsonReader.
	 */
	@Benchmark
	public void benchmarkDeserializeJsonGson() throws IOException
	{
		Map<String, Element> elements = new HashMap<>();
		try (
				Reader inputReader = new InputStreamReader(new ByteArrayInputStream(jsonContent));
				JsonReader parser = new JsonReader(inputReader)
		) {
			com.google.gson.stream.JsonToken rootEvent = parser.peek();
			if (rootEvent != com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
				throw new IOException("Expected object element at the root");
			}
			parser.beginObject();
			for (;;) {
				com.google.gson.stream.JsonToken elementEvent = parser.peek();
				if (elementEvent == com.google.gson.stream.JsonToken.END_OBJECT) {
					break;
				}
				if (elementEvent != com.google.gson.stream.JsonToken.NAME) {
					throw new IOException("Expected key name in root array");
				}

				Element el = new Element();
				el.key = parser.nextName();

				com.google.gson.stream.JsonToken objectEvent = parser.peek();
				if (objectEvent != com.google.gson.stream.JsonToken.BEGIN_OBJECT) {
					throw new IOException("Expected object element in root array");
				}
				parser.beginObject();

				for (;;) {
					com.google.gson.stream.JsonToken keyEvent = parser.peek();
					if (keyEvent == com.google.gson.stream.JsonToken.END_OBJECT) {
						break;
					}
					if (keyEvent != com.google.gson.stream.JsonToken.NAME) {
						throw new IOException("Expected key in element definition");
					}
					String keyName = parser.nextName();
					com.google.gson.stream.JsonToken valueEvent = parser.peek();
					if (valueEvent != com.google.gson.stream.JsonToken.STRING) {
						throw new IOException("Expected value in element definition for key "+keyName);
					}
					String keyValue = parser.nextString();
					switch (keyName) {
					case "property":
						el.property = keyValue;
						break;
					case "other":
						el.other = keyValue;
						break;
					case "content":
						el.content = keyValue;
						break;
					default:
						throw new IOException("Unexpected key in element definition: "+keyName);
					}
				}
				parser.endObject();

				if (el.key == null || el.property == null || el.other == null || el.content == null) {
					throw new IOException("Incomplete content of element: "+el);
				}
				elements.put(el.key, el);
			}
			parser.endObject();
		}
	}

	private static StartElement nextElementEvent(XMLEventReader reader)
	{
		for (;;) {
			XMLEvent event;
			try {
				event = reader.nextEvent();
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
			switch (event.getEventType()) {
			case XMLStreamConstants.START_ELEMENT:
				return event.asStartElement();

			case XMLStreamConstants.END_ELEMENT:
				return null;

			default:
				break;
			}
		}
	}

	private static String mandatoryAttribute(StartElement el, QName name) throws IOException
	{
		Attribute value = el.getAttributeByName(name);
		if (value == null) {
			throw new IOException("Mandatory attribute does not exist,  name="+name+": "+el);
		}
		return value.getValue();
	}

	private static byte[] readResource(String name)
	{
		try {
			return IOUtils.toByteArray(Objects.requireNonNull(DeserializeBenchmark.class.getResource(name), "Cannot open "+name));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Data
	private static class Element
	{
		private String key;

		private String content;

		private String property;

		private String other;
	}

	private static final QName DATA_EL_QNAME = new QName("data");
	private static final QName ELEMENT_EL_QNAME = new QName("element");
	private static final QName KEY_ATTR_QNAME = new QName("key");
	private static final QName PROPERTY_ATTR_QNAME = new QName("property");
	private static final QName OTHER_ATTR_QNAME = new QName("other");
}
