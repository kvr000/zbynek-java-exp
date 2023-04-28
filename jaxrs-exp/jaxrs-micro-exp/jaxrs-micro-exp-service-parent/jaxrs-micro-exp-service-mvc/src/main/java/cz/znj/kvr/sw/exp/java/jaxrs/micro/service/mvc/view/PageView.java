package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.dryuf.base.util.LocaleContext;

import java.util.Map;


@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public class PageView implements ModelView
{
	private final String name;

	private final LocaleContext localeContext;

	private final Map<String, Object> model;
}
