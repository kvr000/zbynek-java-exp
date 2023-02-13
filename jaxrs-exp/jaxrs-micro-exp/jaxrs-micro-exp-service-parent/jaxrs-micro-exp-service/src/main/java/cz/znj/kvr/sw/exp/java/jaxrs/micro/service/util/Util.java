package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.util;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;

import java.util.Iterator;
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

	private static final Map<String, Class<?>> BUILTIN_CLASSES = ImmutableMap.<String, Class<?>>builderWithExpectedSize(16)
		.put(boolean.class.getName(), boolean.class)
		.put(byte.class.getName(), byte.class)
		.put(short.class.getName(), short.class)
		.put(int.class.getName(), int.class)
		.put(long.class.getName(), long.class)
		.put(float.class.getName(), float.class)
		.put(double.class.getName(), double.class)
		.put(String.class.getName(), String.class)
		.put(Boolean.class.getName(), Boolean.class)
		.put(Short.class.getName(), short.class)
		.put(Integer.class.getName(), Integer.class)
		.put(Long.class.getName(), Long.class)
		.put(Float.class.getName(), Float.class)
		.put(Double.class.getName(), Double.class)
		.build();
}
