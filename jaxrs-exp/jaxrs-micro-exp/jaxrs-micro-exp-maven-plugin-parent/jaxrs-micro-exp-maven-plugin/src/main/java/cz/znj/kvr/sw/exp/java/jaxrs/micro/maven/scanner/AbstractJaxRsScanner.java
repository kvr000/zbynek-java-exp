package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.Capturer;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.Condition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.ConsumesCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsMethodMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.KeyValueCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.MediaTypeCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.ProducesCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.QueryParamCondition;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.maven.plugin.logging.Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class AbstractJaxRsScanner implements JaxRsScanner
{
	protected AbstractJaxRsScanner(Log log)
	{
		this.log = log;
	}

	protected static class Runtime
	{
		public ScanConfiguration configuration;

		public URL[] packageUrls;

		public ClassLoader classLoader;

		public Class<? extends Annotation> classPath;

		public Class<? extends Annotation> classHttpMethod;

		public Class<? extends Annotation> classProduces;

		public Class<? extends Annotation> classConsumes;

		public Class<? extends Annotation> classQueryParam;
	}

	protected URL[] getPackageUrls(Runtime runtime)
	{
		return runtime.configuration.getClasspath()
				.stream()
				.map((item) -> {
					try {
						return Paths.get(item).toUri().toURL();
					}
					catch (MalformedURLException e) {
						throw new RuntimeException(e);
					}
				})
				.toArray(URL[]::new);
	}

	protected ClassLoader createClassLoader(Runtime runtime)
	{
		return URLClassLoader.newInstance(runtime.packageUrls, Capturer.class.getClassLoader());
	}

	protected String getHttpMethodValue(Runtime runtime, Object httpMethod)
	{
		try {
			return (String) MethodUtils.invokeExactMethod(httpMethod, "value");
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getPathValue(Runtime runtime, Object path)
	{
		try {
			return (String) MethodUtils.invokeExactMethod(path, "value");
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected String[] getProducesValue(Runtime runtime, Object path)
	{
		try {
			return (String[]) MethodUtils.invokeExactMethod(path, "value");
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected String[] getConsumesValue(Runtime runtime, Object path)
	{
		try {
			return (String[]) MethodUtils.invokeExactMethod(path, "value");
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected String getQueryParamValue(Runtime runtime, Object path)
	{
		try {
			return (String) MethodUtils.invokeExactMethod(path, "value");
		}
		catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	protected String formatMethod(Method method)
	{
		StringBuilder sb = new StringBuilder(method.getName())
				.append("(");
		for (Class<?> param: method.getParameterTypes()) {
			sb.append(param.getName()).append(",");
		}
		if (method.getParameterTypes().length != 0)
			sb.deleteCharAt(sb.length()-1);
		sb.append(")");
		return sb.toString();
	}

	private static <T extends Comparable<T>> int compareLists(List<T> l1, List<T> l2)
	{
		int r = Boolean.compare(l1 != null, l2 != null);
		if (r != 0 || l1 == null) {
			return r;
		}
		int size = Math.min(l1.size(), l2.size());
		for (int i = 0; i < size; ++i) {
			if ((r = l1.get(i).compareTo(l2.get(i))) != 0) {
				return r;
			}
		}
		return Integer.compare(l1.size(), l2.size());
	}

	protected final Log log;

	protected static final URL[]	URL_EMPTY_ARRAY = new URL[0];
	protected static final Class<?>[] CLASSES_EMPTY_ARRAY = new Class<?>[0];
	protected static final Method[]	METHOD_EMPTY_ARRAY = new Method[0];

	@SuppressWarnings("unchecked")
	protected static final Comparator<String> ASCII_STRING_COMPARATOR = (Comparator<String>) (Object) Collator.getInstance(Locale.ROOT);

	protected static final Comparator<String> STRING_COMPARATOR_NULL_FIRST = Comparator.nullsFirst(ASCII_STRING_COMPARATOR);

	protected static final Comparator<String> STRING_COMPARATOR_NULL_LAST = Comparator.nullsLast(ASCII_STRING_COMPARATOR);

	protected static final Comparator<List<QueryParamCondition>> QUERY_PARAM_CONDITION_COMPARATOR = (q1, q2) -> {
		Map<String, String> m1 = q1.stream().collect(Collectors.toMap(QueryParamCondition::getKey, QueryParamCondition::getValue));
		Map<String, String> m2 = q2.stream().collect(Collectors.toMap(QueryParamCondition::getKey, QueryParamCondition::getValue));
		List<String> keys = Stream.concat(m1.keySet().stream(), m2.keySet().stream()).sorted(ASCII_STRING_COMPARATOR).collect(Collectors.toList());
		for (String key: keys) {
			String v1 = m1.get(key);
			String v2 = m2.get(key);
			int r = STRING_COMPARATOR_NULL_LAST.compare(v1, v2);
			if (r != 0) {
				return r;
			}
		}
		return 0;
	};

	protected static final Comparator<MediaTypeCondition> MEDIA_TYPE_CONDITION_COMPARATOR = (m1, m2) -> {
		List<String> c1 = Optional.ofNullable(m1).map(MediaTypeCondition::getValues).orElse(Collections.emptyList());
		List<String> c2 = Optional.ofNullable(m2).map(MediaTypeCondition::getValues).orElse(Collections.emptyList());
		// negating the sign, so the nulls go last, empty go next to last,
		// shorter arrays with the same prefix go later and stars go after more specific mime type
		return -compareLists(c1, c2);
	};

	protected static final Comparator<String> REQUEST_METHODS_COMPARATOR = (String o1, String o2) ->
		{
			String m1m = o1;
			if (m1m.equals("*")) m1m = "";
			String m2m = o2;
			if (m2m.equals("*")) m2m = "";
			int r = m1m.compareTo(m2m);
			if (m1m.isEmpty() || m2m.isEmpty()) {
				return -r;
			}
			return r;
		};

	protected static final Comparator<JaxRsClassMeta> CLASS_COMPARATOR = Comparator
			.comparing(JaxRsClassMeta::getPath)
			.thenComparing(
					o -> o.getConditions().stream().filter(c -> c instanceof QueryParamCondition).map(v -> (QueryParamCondition) v).collect(Collectors.toList()),
					QUERY_PARAM_CONDITION_COMPARATOR
			)
			.thenComparing(
					o -> (ConsumesCondition) o.getConditions().stream().filter(c -> c instanceof ConsumesCondition).findFirst().orElse(null),
					MEDIA_TYPE_CONDITION_COMPARATOR
			)
			.thenComparing(
					o -> (ProducesCondition) o.getConditions().stream().filter(c -> c instanceof ProducesCondition).findFirst().orElse(null),
					MEDIA_TYPE_CONDITION_COMPARATOR
			)
			.thenComparing(
					JaxRsClassMeta::getClassName,
					ASCII_STRING_COMPARATOR
			);

	protected static final Comparator<JaxRsMethodMeta> METHOD_COMPARATOR = Comparator
			.comparing(JaxRsMethodMeta::getPath)
			.thenComparing(
					o -> o.getConditions().stream().filter(c -> c instanceof QueryParamCondition).map(v -> (QueryParamCondition) v).collect(Collectors.toList()),
					QUERY_PARAM_CONDITION_COMPARATOR
			)
			.thenComparing(
					o -> (ConsumesCondition) o.getConditions().stream().filter(c -> c instanceof ConsumesCondition).findFirst().orElse(null),
					MEDIA_TYPE_CONDITION_COMPARATOR
			)
			.thenComparing(
					o -> (ProducesCondition) o.getConditions().stream().filter(c -> c instanceof ProducesCondition).findFirst().orElse(null),
					MEDIA_TYPE_CONDITION_COMPARATOR
			)
			.thenComparing(
					JaxRsMethodMeta::getMethod,
					REQUEST_METHODS_COMPARATOR
			)
			.thenComparing(
					JaxRsMethodMeta::getFunction,
					ASCII_STRING_COMPARATOR
			);
}
