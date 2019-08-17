package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestExchange;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class Util
{
	public static Iterable<String> splitByChar(String s, char c)
	{
		return new Iterable<>()
		{
			@Override
			public Iterator<String> iterator()
			{
				return new Iterator<String>()
				{
					@Override
					public boolean hasNext()
					{
						return current >= 0;
					}

					@Override
					public String next()
					{
						int old = current;
						current = s.indexOf(c, old);
						return current < 0 ? s.substring(old) : s.substring(old, current++);
					}

					private int current = s.isEmpty() ? -1 : 0;
				};
			}
		};
	}

	public static Class<?> loadClass(ClassLoader loader, String className)
	{
		Class<?> clazz = BUILTIN_CLASSES.get(className);
		if (clazz == null) {
			try {
				clazz = loader.loadClass(className);
			}
			catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return clazz;
	}


	public static final String[] EMPTY_STRING_ARRAY = new String[0];

	@SuppressWarnings("unchecked")
	public static final Predicate<RequestExchange>[] EMPTY_CONDITIONS_ARRAY = new Predicate[0];

	private static final Map<String, Class<?>> BUILTIN_CLASSES = Map.of(
			boolean.class.getName(), boolean.class,
			byte.class.getName(), byte.class,
			short.class.getName(), short.class,
			int.class.getName(), int.class,
			long.class.getName(), long.class,
			String.class.getName(), String.class,
			Boolean.class.getName(), Boolean.class,
			Short.class.getName(), short.class,
			Integer.class.getName(), Integer.class,
			Long.class.getName(), Long.class
	);
}
