package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;


public class JaxRsCaptureReaderImpl implements JaxRsCaptureReader
{
	public JaxRsCaptureReaderImpl(XMLEventReader reader) throws IOException
	{
		this.reader = reader;
		StartElement docEvent = nextElement();
		if (docEvent == null) {
			throw new IOException("Unexpected end of file");
		}
		if (!docEvent.getName().equals(jaxrspaths_ELEMENT)) {
			throw new IOException("Expected jaxrs-paths at document start, got: "+docEvent.getName()+": "+docEvent.getLocation());
		}
		StartElement controllersEvent = nextElement();
		if (controllersEvent == null) {
			throw new IOException("Unexpected end of file, expected controllers: "+docEvent.getLocation());
		}
		if (!controllersEvent.getName().equals(controllers_ELEMENT)) {
			throw new IOException("Expected controllers, got: "+controllersEvent.getName());
		}
	}

	public JaxRsCaptureReaderImpl(InputStream inputStream) throws IOException
	{
		this(((Function<InputStream, XMLEventReader>) (inputStream0) -> {
			try {
				return XMLInputFactory.newInstance().createXMLEventReader(inputStream0);
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		}).apply(inputStream));
	}

	@Override
	public ControllerMeta next() throws IOException
	{
		StartElement el = nextElement();
		if (el == null)
			return null;
		if (!el.getName().equals(controller_ELEMENT)) {
			throw new IOException("Expected controller, got: "+el.getName()+": "+el.getLocation());
		}
		return parseController(el);
	}

	public void close() throws IOException
	{
		try {
			reader.close();
		}
		catch (XMLStreamException e) {
			throw new IOException(e);
		}
	}

	private ControllerMeta parseController(StartElement controllerEl) throws IOException
	{
		ControllerMeta controller = new ControllerMeta();
		controller.setClassName(mandatoryAttribute(controllerEl, class_ATTR));
		controller.setPath(mandatoryAttribute(controllerEl, path_ATTR));

		for (;;) {
			StartElement el = nextElement();
			if (el == null) {
				break;
			}
			else if (el.getName().equals(methods_ELEMENT)) {
				controller.setMethods(parseMethods(el));
			}
			else {
				throw new IOException("Unexpected element under controller: "+el.getName()+": "+el.getLocation());
			}
		}
		if (controller.getMethods() == null) {
			throw new IOException("Expected methods element under controller: "+controllerEl.getLocation());
		}
		return controller;
	}

	private List<MethodMeta> parseMethods(StartElement methodsEl) throws IOException
	{
		List<MethodMeta> methods = new ArrayList<>();

		for (;;) {
			StartElement el = nextElement();
			if (el == null) {
				break;
			}
			else if (el.getName().equals(method_ELEMENT)) {
				methods.add(parseMethod(el));
			}
			else {
				throw new IOException("Unexpected element under methods: "+el.getName()+": "+el.getLocation());
			}
		}
		return methods;
	}

	private MethodMeta parseMethod(StartElement methodEl) throws IOException
	{
		MethodMeta method = new MethodMeta();
		method.setPath(mandatoryAttribute(methodEl, path_ATTR));
		method.setFunction(mandatoryAttribute(methodEl, function_ATTR));
		method.setMethods(splitByChar(mandatoryAttribute(methodEl, method_ATTR), ','));
		StartElement el = nextElement();
		if (el != null) {
			throw new IOException("Unexpected element in method: "+el.getName()+": "+el.getLocation());
		}
		return method;
	}

	private StartElement nextElement()
	{
		for (;;) {
			XMLEvent event;
			try {
				event = this.reader.nextEvent();
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

	private String mandatoryAttribute(StartElement element, QName qname) throws IOException
	{
		Attribute attribute = element.getAttributeByName(qname);
		if (attribute == null || !attribute.isSpecified()) {
			throw new IOException("Mandatory attribute does not exist on "+element.getName()+": "+qname+": "+element.getLocation());
		}
		return attribute.getValue();
	}

	private static List<String> splitByChar(String s, char split)
	{
		int p = s.indexOf(split);
		if (p < 0) {
			return Collections.singletonList(s);
		}
		ArrayList<String> out = new ArrayList<>();
		int o = 0;
		do {
			out.add(s.substring(o, p));
			o = p+1;
			p = s.indexOf(split, o);
		} while (p >= 0);
		out.add(s.substring(o));

		return out;
	}

	private final XMLEventReader reader;

	private static final String jaxrspath_SCHEMA = "http://github.com/kvr000/zbynek-java-exp/jaxrs-exp/jaxrs-micro-exp/schema/jaxrs-paths";
	private static final QName jaxrspaths_ELEMENT = new QName(jaxrspath_SCHEMA, "jaxrs-paths");
	private static final QName controllers_ELEMENT = new QName(jaxrspath_SCHEMA, "controllers");
	private static final QName controller_ELEMENT = new QName(jaxrspath_SCHEMA, "controller");
	private static final QName class_ATTR = new QName(null, "class");
	private static final QName path_ATTR = new QName(null, "path");
	private static final QName methods_ELEMENT = new QName(jaxrspath_SCHEMA, "methods");
	private static final QName method_ELEMENT = new QName(jaxrspath_SCHEMA, "method");
	private static final QName method_ATTR = new QName(null, "method");
	private static final QName function_ATTR = new QName(null, "function");
}
