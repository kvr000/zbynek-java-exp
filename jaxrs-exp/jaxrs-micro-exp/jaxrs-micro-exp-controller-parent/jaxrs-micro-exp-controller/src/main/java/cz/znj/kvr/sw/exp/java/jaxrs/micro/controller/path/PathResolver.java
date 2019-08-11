package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.path;

import java.util.Map;
import java.util.function.Predicate;


/**
 *
 */
public interface PathResolver<C, H extends Predicate<C>>
{
	Match<H> resolvePath(String path, C context);

	interface Match<H>
	{
		H handler();

		Map<String, String> placeholderValues();

		int separatorStatus();

		boolean matchesFully();

		String pattern();
	}
}
