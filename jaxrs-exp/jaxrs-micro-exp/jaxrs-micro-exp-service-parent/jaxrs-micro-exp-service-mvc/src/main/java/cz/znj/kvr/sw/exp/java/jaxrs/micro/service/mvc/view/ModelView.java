package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dryuf.base.util.LocaleContext;

import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;


public interface ModelView
{
	String name();

	LocaleContext localeContext();

	Map<String, Object> model();
}
