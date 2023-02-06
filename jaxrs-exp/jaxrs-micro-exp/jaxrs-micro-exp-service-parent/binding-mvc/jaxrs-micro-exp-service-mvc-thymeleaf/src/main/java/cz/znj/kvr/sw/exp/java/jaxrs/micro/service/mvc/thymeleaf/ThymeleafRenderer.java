package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.thymeleaf;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.processor.AbstractMvcRenderer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.thymeleaf.impl.ViewContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.CompositeWidget;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.ModelView;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.Widget;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Link;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.thymeleaf.TemplateEngine;

import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ThymeleafRenderer extends AbstractMvcRenderer
{
	private static final Pattern WIDGETMETA_PATTERN = Pattern.compile(
		"^\\s*<!--\\s+WIDGETMETA\\s+(\\w+)\\s+(.*?)\\s*-->[ \t]*\\n",
		Pattern.DOTALL | Pattern.UNIX_LINES);
	private static final Pattern WIDGETMETA_PARTIAL_PATTERN = Pattern.compile(
		"^\\s*(<!--\\s+WIDGETMETA\\s+.*|\\s+$)",
		Pattern.DOTALL | Pattern.UNIX_LINES);

	private static final Pattern RESOURCE_PATTERN = Pattern.compile(
		"^([^:]+):([^:]+)\\s+(\\S+)((\\s+\\w=\\S+)+)?$",
		Pattern.DOTALL | Pattern.UNIX_LINES);

	private static final Pattern TITLE_PATTERN = Pattern.compile(
		"^\"([^\"]*)\"$",
		Pattern.DOTALL | Pattern.UNIX_LINES);

	private static final Pattern FINAL_PATTERN = Pattern.compile(
		"^(\\w+)$",
		Pattern.DOTALL | Pattern.UNIX_LINES);

	private final TemplateEngine engine;

	@Inject
	public ThymeleafRenderer(TemplateEngine engine)
	{
		this.engine = engine;
	}

	@Override
	public Widget render(Response response, ModelView view)
	{
		Map<String, Link> links = new LinkedHashMap<>();
		Map<String, Object> metadata = new LinkedHashMap<>();

		StringBuilder output = new StringBuilder();
		MutableBoolean isFinal = new MutableBoolean(false);
		engine.process(
			view.name(),
			new ViewContext(view),
			new Writer() {
				boolean processing = true;

				@Override
				public void write(char[] cbuf, int off, int len) throws IOException
				{
					if (processing) {
						int old = output.length();
						output.append(cbuf, off, len);
						int p;
						while ((p = output.indexOf("\n", old)) >= 0) {
							int processed = processLine(output.substring(0, p+1));
							switch (processed) {
							case -1:
								processing = false;
								return;

							case 0:
								return;

							default:
								output.delete(0, processed);
								old = 0;
								break;
							}
						}
					}
					else {
						output.append(cbuf, off, len);
					}
				}

				@Override
				public void flush() throws IOException
				{
				}

				@Override
				public void close() throws IOException
				{
				}

				private int processLine(String line) throws IOException
				{
					Matcher m = WIDGETMETA_PATTERN.matcher(line);
					if (m.matches()) {
						processMatch(m.group(1), m.group(2));
						return m.end();
					}
					else if (WIDGETMETA_PARTIAL_PATTERN.matcher(line).matches()) {
						return 0;
					}
					else {
						return -1;
					}
				}

				private void processMatch(String key, String value) throws IOException
				{
					try {
						switch (key) {
						case "JS": {
							Matcher m;
							if ((m = RESOURCE_PATTERN.matcher(value)).matches()) {
								links.computeIfAbsent(m.group(1), (k) ->
									RuntimeDelegate.getInstance().createLinkBuilder()
										.uri(m.group(3))
										.type("text/javascript")
										.param("_wm_version", m.group(2))
										.build()
								);
							}
							else {
								throw new IOException("does not match name:version url");
							}
							break;
						}

						case "CSS": {
							Matcher m;
							if ((m = RESOURCE_PATTERN.matcher(value)).matches()) {
								links.computeIfAbsent(m.group(1), (k) ->
									RuntimeDelegate.getInstance().createLinkBuilder()
										.rel("stylesheet")
										.uri(m.group(3))
										.param("_wm_version", m.group(2))
										.build()
								);
							}
							else {
								throw new IOException("does no match name:version url");
							}
							break;
						}

						case "TITLE": {
							Matcher m;
							if ((m = TITLE_PATTERN.matcher(value)).matches()) {
								metadata.computeIfAbsent("title",
									k -> StringEscapeUtils.unescapeXml(m.group(1)));
							}
							else {
								throw new IOException("does not match \"html-encoded\": key="+key+" value="+value);
							}
							break;
						}

						case "FINAL": {
							Matcher m;
							if ((m = FINAL_PATTERN.matcher(value)).matches()) {
								isFinal.setValue(Boolean.parseBoolean(m.group(1)));
							}
							else {
								throw new IOException("does not match true|false");
							}
							break;
						}

						default:
							throw new IOException("Unknown key: key="+key);
						}
					}
					catch (IOException ex) {
						throw new IOException("Unsupported WIDGETMETA: " + ex.toString(), ex);
					}
				}
			}
		);

		return new CompositeWidget(
			response.getStatus(),
			isFinal.booleanValue(),
			-1,
			metadata,
			links,
			new StringReader(output.toString())
		);
	}
}
