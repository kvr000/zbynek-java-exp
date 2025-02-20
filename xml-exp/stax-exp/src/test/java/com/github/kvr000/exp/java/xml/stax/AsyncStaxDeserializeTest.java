package com.github.kvr000.exp.java.xml.stax;

import org.testng.annotations.Ignore;
import org.testng.annotations.Test;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;

import static org.testng.Assert.assertEquals;


public class AsyncStaxDeserializeTest
{
	String xml =
		"""
			<?xml version='1.0'?>
			<events>
				<event><name>Zbynek</name></event>
				<event><name>Vyskovsky</name></event>
			</events>
			""";
	@Test
	public void testDeserialize_whenFull_returnFull() throws Exception
	{
		ArrayList<String> events = new ArrayList<>();
		XMLInputFactory factory = XMLInputFactory.newFactory();
		XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

		boolean insideEvent = false;

		try {
			while (reader.hasNext()) {
				int eventType = reader.next();

				if (eventType == XMLStreamConstants.START_ELEMENT && "event".equals(reader.getLocalName())) {
					insideEvent = true;
				}

				if (eventType == XMLStreamConstants.END_ELEMENT && "event".equals(reader.getLocalName())) {
					insideEvent = false;
					events.add("number");
				}
			}
		} finally {
			reader.close();
		}
		assertEquals(events.size(), 2);
	}

	@Test
	@Ignore("no input cache in XML parser, does not work")
	public void testDeserialize_whenPartial_returnFull() throws Exception
	{
		ArrayList<String> events = new ArrayList<>();

		PartialStringReader part = null;
		int offset = 0;
		XMLInputFactory factory = XMLInputFactory.newFactory();
		XMLStreamReader reader = null;

		boolean insideEvent = false;

		try {
			for (;;) {
				int eventType;
				try {
					if (reader == null) {
						part = new PartialStringReader(xml.substring(0, ++offset));
						reader = factory.createXMLStreamReader(part);
					}
					boolean hasNext = reader.hasNext();
					if (!hasNext) {
						break;
					}
					eventType = reader.next();
				}
				catch (PartialStringReader.AsyncEofError ex) {
					part.add(xml.substring(offset, offset + 1));
					offset++;
					continue;
				}

				if (eventType == XMLStreamConstants.START_ELEMENT && "event".equals(reader.getLocalName())) {
					insideEvent = true;
				}

				if (eventType == XMLStreamConstants.END_ELEMENT && "event".equals(reader.getLocalName())) {
					insideEvent = false;
					events.add("number");
				}
			}
		} finally {
			reader.close();
		}
		assertEquals(events.size(), 2);
	}

	public static class PartialStringReader extends Reader
	{
		String buffer = "";

		public PartialStringReader(String init)
		{
			buffer = init;
		}

		public void add(String part)
		{
			buffer += part;
		}

		@Override
		public int read(char[] chars, int off, int len) throws IOException
		{
			if (len > 0 && buffer.isEmpty()) {
				throw new AsyncEofError();
			}
			if (len > buffer.length()) {
				len = buffer.length();
			}
			buffer.getChars(0, len, chars, off);
			buffer = buffer.substring(len);
			System.out.print(new String(chars, off, len));
			return len;
		}

		@Override
		public void close() throws IOException
		{
		}

		public static class AsyncEofError extends Error
		{
		}
	}
}
