package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import net.dryuf.base.util.LocaleContext;

import java.util.Map;


public interface ModelView
{
	String name();

	LocaleContext localeContext();

	Map<String, Object> model();
}
