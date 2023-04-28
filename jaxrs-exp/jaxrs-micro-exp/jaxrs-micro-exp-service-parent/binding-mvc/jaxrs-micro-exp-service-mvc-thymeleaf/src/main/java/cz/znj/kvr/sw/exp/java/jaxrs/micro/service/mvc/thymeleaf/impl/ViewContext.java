package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.thymeleaf.impl;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.ModelView;
import lombok.RequiredArgsConstructor;
import org.thymeleaf.context.IContext;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;


@RequiredArgsConstructor
public class ViewContext implements IContext
{
	private static final Map<String, Function<ModelView, Object>> SPECIAL_RESOLVER = ImmutableMap.of(
		"locale", mv -> mv.localeContext().locale(),
		"timeZone", mv -> mv.localeContext().timeZone()
	);

	private final ModelView view;

	@Override
	public Locale getLocale()
	{
		return view.localeContext().locale();
	}

	@Override
	public boolean containsVariable(String name)
	{
		return SPECIAL_RESOLVER.containsKey(name) || view.model().containsKey(name);
	}

	@Override
	public Set<String> getVariableNames()
	{
		return view.model().keySet();
	}

	@Override
	public Object getVariable(String name)
	{
		return Optional.ofNullable(SPECIAL_RESOLVER.get(name))
			.orElse((ModelView view) -> view.model().get(name))
			.apply(view);
	}
}
