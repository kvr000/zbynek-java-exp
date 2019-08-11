package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.router.impl;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodId;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util.Util;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.MethodInvokerStatic;
import lombok.RequiredArgsConstructor;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


@RequiredArgsConstructor
public class FunctionData implements Predicate<RequestExchange>, OwnedMethodHolder
{
	public static final FunctionData DUMMY;

	static {
		DUMMY = new FunctionData(null);
		DUMMY.runtime = new RuntimeData();
		DUMMY.runtime.conditions = Util.EMPTY_CONDITIONS_ARRAY;
	}

	@Override
	public boolean test(RequestExchange requestExchange)
	{
		RuntimeData localRuntime = runtime;
		if (localRuntime == null) {
			runtime = localRuntime = resolveRuntime();
		}
		for (Predicate<RequestExchange> condition: localRuntime.conditions) {
			if (!condition.test(requestExchange))
				return false;
		}
		return true;
	}

	@Override
	public OwnedMethodId methodId()
	{
		return methodId;
	}

	private FunctionData.RuntimeData resolveRuntime()
	{
		FunctionData.RuntimeData runtime = new FunctionData.RuntimeData();

		try {
			List<Predicate<RequestExchange>> conditions = new ArrayList<>();
			runtime.clazz = Util.loadClass(FunctionData.class.getClassLoader(), methodId.className());
			int p = methodId.methodName().indexOf('(');
			if (p < 0 || !methodId.methodName().endsWith(")")) {
				throw new IllegalArgumentException("Expected methodName(arg1,arg2,...,argN), got: "+methodId.methodName());
			}
			String name = methodId.methodName().substring(0, p);
			Class<?>[] args = StreamSupport.stream(Util.splitByChar(methodId.methodName().substring(p+1, methodId.methodName().length()-1), ',').spliterator(), false)
					.map(className -> Util.loadClass(FunctionData.class.getClassLoader(), className))
					.collect(Collectors.toList())
					.toArray(Util.EMPTY_CLASS_ARRAY);
			runtime.resolvedMethod =  runtime.clazz.getMethod(name, args);

			Optional.ofNullable(runtime.resolvedMethod.getAnnotation(Produces.class))
					.or(() -> Optional.ofNullable(runtime.clazz.getAnnotation(Produces.class)))
					.ifPresent((Produces producesAnno) -> {
						conditions.add(new ProducesPredicate(producesAnno.value()));
						runtime.produces = Arrays.asList(producesAnno.value()).stream()
								.map(MediaType::valueOf)
								.collect(Collectors.toList())
								.toArray(Util.EMPTY_MEDIATYPE_ARRAY);
					});

			Optional.ofNullable(runtime.resolvedMethod.getAnnotation(Consumes.class))
					.or(() -> Optional.ofNullable(runtime.clazz.getAnnotation(Consumes.class)))
					.ifPresent((Consumes consumes) -> conditions.add(new ConsumesPredicate(consumes.value())));

			Stream.of(runtime.resolvedMethod.getAnnotationsByType(QueryParam.class))
					.forEach((QueryParam qp) -> conditions.add(new QueryParamPredicate(qp.value())));

			runtime.conditions = conditions.toArray(Util.EMPTY_CONDITIONS_ARRAY);

		}
		catch (Exception e) {
			throw new RuntimeException("Failed to load controller: "+methodId, e);
		}
		return runtime;
	}

	public static MethodInvokerStatic<CallContext> resolverContainerInvoker(ContainerContext container, OwnedMethodHolder methodHolder)
	{
		FunctionData function = (FunctionData) methodHolder;
		RuntimeData runtime = function.runtime;
		@SuppressWarnings("unchecked")
		Function<CallContext, Object>[] argsResolvers = new Function[runtime.resolvedMethod.getGenericParameterTypes().length];
		Annotation[][] parameterAnnotations = runtime.resolvedMethod.getParameterAnnotations();
		for (int i = 0; i < runtime.resolvedMethod.getParameterCount(); ++i) {
			try {
				argsResolvers[i] = getArgResolver(container, runtime.resolvedMethod.getGenericParameterTypes()[i], parameterAnnotations[i]);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Failed to populate argument "+i+" on method: "+runtime.resolvedMethod, ex);
			}
		}
		return new MethodInvokerStaticImpl(argsResolvers);
	}

	private static Function<CallContext, Object> getArgResolver(ContainerContext container, Type type, Annotation[] annotations)
	{
		ResolverData resolverData = new ResolverData(container, type, annotations);
		Function<ResolverData, Function<CallContext, Object>> resolverProvider = null;
		for (Annotation annotation: annotations) {
			@SuppressWarnings("unchecked")
			Function<ResolverData, Function<CallContext, Object>> triedProvider = ARG_RESOLVERS.get(annotation.annotationType());
			if (triedProvider != null) {
				if (resolverProvider != null) {
					throw new IllegalArgumentException("Two different resolver annotations specified for parameter: "+type);
				}
				resolverProvider = triedProvider;
				resolverData.resolverAnnotation = annotation;
			}
			else if (annotation instanceof DefaultValue) {
				resolverData.defaultValue = ((DefaultValue) annotation).value();
			}
			else {
				throw new IllegalArgumentException("Unsupported annotation on parameter "+type+": "+annotation.annotationType());
			}
		}
		if (resolverProvider == null) {
			throw new IllegalArgumentException("Do not know how to resolve parameter: "+type+", annotated with "+Arrays.deepToString(annotations));
		}
		return resolverProvider.apply(resolverData);
	}

	private static Function<CallContext, Object> contextResolverResolver(ResolverData resolverData)
	{
		Function<ResolverData, Function<CallContext, Object>> resolverResolver = COMMON_CONTEXT_RESOLVERS.get(resolverData.type);
		if (resolverResolver != null) {
			return resolverResolver.apply(resolverData);
		}
		return (CallContext context) -> resolverData.container.contextObjectResolver(resolverData.type);
	}

	private static Function<CallContext, Object> queryParamResolverResolver(ResolverData resolverData)
	{
		if (resolverData.type instanceof Class) {
			return new QueryParamResolver(resolverData);
		}
		else if (resolverData.type instanceof ParameterizedType) {
			Class<?> rawType = (Class<?>) ((ParameterizedType) resolverData.type).getRawType();
			if (!Iterable.class.isAssignableFrom(rawType)) {
				throw new IllegalArgumentException("Only collections of String re supported for multiple QueryParameter, got: "+resolverData.type);
			}
			return new QueryParamsResolver(resolverData);
		}
		else {
			throw new IllegalArgumentException("Only collections of String re supported for multiple QueryParameter, got: "+resolverData.type);
		}
	}

	private static Function<String, Object> findTypeConverter(Type type)
	{
		Function<String, Object> converter = COMMON_TYPE_CONVERTERS.get(type);
		if (converter == null) {
			throw new IllegalArgumentException("Cannot find converter from String to "+type);
		}
		return converter;
	}

	public MediaType produces(RequestExchange request)
	{
		if (runtime.produces == null) {
			return null;
		}
		for (MediaType produce: runtime.produces) {
			for (MediaType accept: runtime.produces) {
				if (accept.isCompatible(produce)) {
					return produce;
				}
			}
		}
		return null;
	}

	public static class RuntimeData
	{
		Class<?> clazz;

		Method resolvedMethod;

		Predicate<RequestExchange>[] conditions;

		MediaType[] produces;
	}

	@RequiredArgsConstructor
	private static class ResolverData
	{
		public Annotation resolverAnnotation;

		private String defaultValue;

		private final ContainerContext container;

		private final Type type;

		private final Annotation[] annotations;
	}

	private static abstract class MimeTypePredicate implements Predicate<RequestExchange>
	{
		public MimeTypePredicate(String[] patterns)
		{
			this.patterns = Stream.of(patterns)
					.map(MediaType::valueOf)
					.collect(Collectors.toList())
					.toArray(Util.EMPTY_MEDIATYPE_ARRAY);
		}

		protected final MediaType[] patterns;
	}

	private static class ProducesPredicate extends MimeTypePredicate
	{
		public ProducesPredicate(String[] patterns)
		{
			super(patterns);
		}

		@Override
		public boolean test(RequestExchange callContext)
		{
			List<MediaType> contextMimeTypes = callContext.getAcceptsType();
			if (contextMimeTypes.isEmpty()) {
				return true;
			}
			for (MediaType pattern: patterns) {
				for (MediaType contextPattern: contextMimeTypes) {
					if (contextPattern.isCompatible(pattern)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	private static class ConsumesPredicate extends MimeTypePredicate
	{
		public ConsumesPredicate(String[] patterns)
		{
			super(patterns);
		}

		@Override
		public boolean test(RequestExchange callContext)
		{
			MediaType contextMimeTypes = callContext.getContentType();
			for (MediaType pattern: patterns) {
				if (contextMimeTypes.isCompatible(pattern)) {
					return true;
				}
			}
			return false;
		}
	}

	private static class QueryParamPredicate implements Predicate<RequestExchange>
	{
		public QueryParamPredicate(String queryCondition)
		{
			String[] split = queryCondition.split(queryCondition, 2);
			if (split.length != 2) {
				throw new IllegalArgumentException("Expected key=value syntax for QueryParameter, got: "+queryCondition);
			}
			this.key = split[0];
			this.value = split[1];
		}

		@Override
		public boolean test(RequestExchange callContext)
		{
			return value.equals(callContext.getQueryParam(key));
		}

		private final String key;

		private final String value;
	}

	@RequiredArgsConstructor
	private static class MethodInvokerStaticImpl implements MethodInvokerStatic<CallContext>
	{
		@Override
		public Object[] resolveArguments(CallContext context)
		{
			Object[] args = new Object[argResolvers.length];
			for (int i = 0; i < args.length; ++i) {
				Function<CallContext, Object> resolver = argResolvers[i];
				args[i] = resolver.apply(context);
			}
			return args;
		}

		private final Function<CallContext, Object>[] argResolvers;
	}

	private static class CustomContextResolver implements Function<CallContext, Object>
	{
		private CustomContextResolver(ContainerContext container, Type classType, Annotation annotation)
		{
			this.classType = classType;
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.customContext();
		}

		private final Type classType;
	}

	private static abstract class FromStringResolver implements Function<CallContext, Object>
	{
		private FromStringResolver(ResolverData resolverData, String name)
		{
			this.name = name;
			this.type = resolverData.type;
			this.converter = findTypeConverter(type);
			this.defaultValue = resolverData.defaultValue == null ? null : this.converter.apply(resolverData.defaultValue);
		}

		@Override
		public Object apply(CallContext callContext)
		{
			String value = getString(callContext);
			return value == null ? defaultValue : converter.apply(value);
		}

		protected abstract String getString(CallContext callContext);

		protected final String name;

		protected final Type type;

		protected final Function<String, Object> converter;

		protected final Object defaultValue;
	}

	private static class PathParamResolver extends FromStringResolver
	{
		private PathParamResolver(ResolverData resolverData)
		{
			super(resolverData, ((PathParam) resolverData.resolverAnnotation).value());
		}

		@Override
		public String getString(CallContext callContext)
		{
			String value = callContext.match.placeholderValues().get(name);
			if (value == null) {
				throw new IllegalArgumentException("Cannot find PathParam: "+name);
			}
			return value;
		}
	}

	private static class QueryParamResolver extends FromStringResolver
	{
		private QueryParamResolver(ResolverData resolverData)
		{
			super(resolverData, ((QueryParam) resolverData.resolverAnnotation).value());
		}

		@Override
		public String getString(CallContext callContext)
		{
			String value = callContext.serverRequest().getQueryParam(name);
			return value;
		}
	}

	private static class QueryParamsResolver implements Function<CallContext, Object>
	{
		private QueryParamsResolver(ResolverData resolverData)
		{
			name = ((QueryParam) resolverData.resolverAnnotation).value();
			@SuppressWarnings("unchecked")
			Class<? extends Iterable> clazz = (Class<? extends Iterable>) ((ParameterizedType) resolverData.type).getRawType();
			if (clazz == List.class || clazz == Collection.class || clazz == Iterable.class) {
				clazz = ArrayList.class;
			}
			else if (clazz == Set.class) {
				clazz = HashSet.class;
			}
			try {
				constructor = clazz.getConstructor(Collection.class);
			}
			catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
			converter = findTypeConverter(((ParameterizedType) resolverData.type).getRawType());
		}

		@Override
		public Object apply(CallContext callContext)
		{
			try {
				return constructor.newInstance(callContext.serverRequest().getQueryParams(name));
			}
			catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		private final String name;

		private final Constructor constructor;

		private final Function<String, Object> converter;
	}

	private static class ContentBodyResolver implements Function<CallContext, Object>
	{
		private ContentBodyResolver(ResolverData resolverData)
		{
			this.classType = resolverData.type;
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.readBody(classType);
		}

		private final Type classType;
	}

	private static class ServerRequestResolver implements Function<CallContext, Object>
	{
		private ServerRequestResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.serverRequest();
		}
	}

	private static class ContainerContextResolver implements Function<CallContext, Object>
	{
		private ContainerContextResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.container();
		}
	}

	private static class SecurityContextResolver implements Function<CallContext, Object>
	{
		private SecurityContextResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.securityContext();
		}
	}

	private static class RequestContextResolver implements Function<CallContext, Object>
	{
		private RequestContextResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.requestContext;
		}
	}

	private static class HttpHeadersResolver implements Function<CallContext, Object>
	{
		private HttpHeadersResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.requestContext.headers();
		}
	}

	private static class RequestResolver implements Function<CallContext, Object>
	{
		private RequestResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.requestContext.request();
		}
	}

	private static class UriInfoResolver implements Function<CallContext, Object>
	{
		private UriInfoResolver(ResolverData resolverData)
		{
		}

		@Override
		public Object apply(CallContext callContext)
		{
			return callContext.requestContext.uriInfo();
		}
	}

	private final OwnedMethodId methodId;

	private volatile RuntimeData runtime;

	private static final Map<Class<?>, Function<ResolverData, Function<CallContext, Object>>> ARG_RESOLVERS = Map.of(
			PathParam.class, PathParamResolver::new,
			QueryParam.class, QueryParamResolver::new,
			Context.class, FunctionData::contextResolverResolver,
			Void.class, ContentBodyResolver::new
	);

	private static final Map<Type, Function<ResolverData, Function<CallContext, Object>>> COMMON_CONTEXT_RESOLVERS = Map.of(
			RequestExchange.class, ServerRequestResolver::new,
			SecurityContext.class, SecurityContextResolver::new,
			ContainerContext.class, ContainerContextResolver::new,
			RequestContext.class, RequestContextResolver::new,
			Request.class, RequestResolver::new,
			HttpHeaders.class, HttpHeadersResolver::new,
			UriInfo.class, UriInfoResolver::new
	);

	private static final Map<Type, Function<String, Object>> COMMON_TYPE_CONVERTERS = ImmutableMap.<Type, Function<String, Object>>builder()
			.put(boolean.class, Boolean::valueOf)
			.put(byte.class, Byte::valueOf)
			.put(short.class, Short::valueOf)
			.put(int.class, Integer::valueOf)
			.put(long.class, Long::valueOf)
			.put(Boolean.class, Boolean::valueOf)
			.put(Byte.class, Byte::valueOf)
			.put(Short.class, Short::valueOf)
			.put(Integer.class, Integer::valueOf)
			.put(Long.class, Long::valueOf)
			.put(String.class, s -> s)
			.build();
}
