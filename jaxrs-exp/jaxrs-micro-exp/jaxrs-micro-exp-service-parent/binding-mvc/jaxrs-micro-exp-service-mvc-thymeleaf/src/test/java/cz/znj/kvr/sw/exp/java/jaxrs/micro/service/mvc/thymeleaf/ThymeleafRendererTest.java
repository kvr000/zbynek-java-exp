package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.thymeleaf;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.PageView;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.Widget;
import net.dryuf.base.util.BasicLocaleContext;
import org.testng.annotations.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;
import java.time.ZoneOffset;
import java.util.Locale;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.testng.Assert.assertEquals;


public class ThymeleafRendererTest
{
	TemplateEngine engine = new TemplateEngine();
	{
		engine.setTemplateResolver(new ClassLoaderTemplateResolver());
	}

	ThymeleafRenderer renderer = new ThymeleafRenderer(engine);

	@Test
	public void render_whenMetadata_parseMetadata()
	{
		Widget result = renderer.render(Response.ok().build(), new PageView(
			ThymeleafRenderer.class.getPackage().getName().replace('.', '/') + "/sample/sample1.html",
			BasicLocaleContext.builder()
				.locale(Locale.ROOT)
				.timeZone(ZoneOffset.UTC)
				.build(),
			ImmutableMap.of(
				"dynamic", "Me"
			)
		));

		assertEquals(result.status(), Response.Status.OK.getStatusCode());
		assertThat(result.metadata(), hasEntry("title", "Hello world, how are you? <-->, \"love it\"! Me."));
		assertThat(result.links(), hasEntry("jquery.js", RuntimeDelegate.getInstance().createLinkBuilder()
				.type("text/javascript")
				.uri("https://ajax.googleapis.com/ajax/libs/jquery/3.6.4/jquery.min.js")
				.param("_wm_version", "3")
				.build()
			));
		assertThat(result.links(), hasEntry("jquery-ui.css", RuntimeDelegate.getInstance().createLinkBuilder()
			.rel("stylesheet")
			.uri("https://ajax.googleapis.com/ajax/libs/jqueryui/1.13.2/themes/smoothness/jquery-ui.css")
			.param("_wm_version", "3")
			.build()
		));
		assertEquals(result.isFinal(), true);
	}
}
