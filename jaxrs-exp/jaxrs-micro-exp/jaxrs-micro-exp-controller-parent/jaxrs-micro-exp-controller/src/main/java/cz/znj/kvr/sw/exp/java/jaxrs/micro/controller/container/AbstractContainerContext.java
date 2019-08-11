package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodId;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.util.Util;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.MethodInvokerStatic;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


public abstract class AbstractContainerContext implements ContainerContext
{
	@Override
	public Function<RequestContext, Object> contextObjectResolver(Type type)
	{
		throw new IllegalArgumentException("Unsupported context type: "+type);
	}

	@Override
	public <C> BeanMethod<C> resolveMethod(OwnedMethodHolder methodHolder, BiFunction<ContainerContext, OwnedMethodHolder, MethodInvokerStatic<C>> invokerProvider)
	{
		@SuppressWarnings("unchecked")
		BeanMethodHolder<C> holder = (BeanMethodHolder<C>) beanMethodHolders.get(methodHolder.methodId());
		if (holder == null) {
			Method method = resolveMethod(methodHolder.methodId());
			holder = new BeanMethodHolder<>(method.getDeclaringClass(), method, invokerProvider.apply(this, methodHolder));
			beanMethodHolders.put(methodHolder.methodId(), holder);
		}
		return holder.toObjectBeanMethod(getBean(holder.clazz()));
	}

	private Method resolveMethod(OwnedMethodId methodId)
	{
		Class<?> clazz = Util.loadClass(AbstractContainerContext.class.getClassLoader(), methodId.className());
		int p = methodId.methodName().indexOf('(');
		if (p < 0 || !methodId.methodName().endsWith(")")) {
			throw new IllegalArgumentException("Cannot parse method name, expected method(arg0,arg1,...): "+methodId);
		}
		String methodName = methodId.methodName().substring(0, p);
		Class<?>[] args = StreamSupport.stream(Util.splitByChar(methodId.methodName().substring(p+1, methodId.methodName().length()-1), ',').spliterator(), false)
				.map(arg -> Util.loadClass(AbstractContainerContext.class.getClassLoader(), arg))
				.collect(Collectors.toList())
				.toArray(Util.EMPTY_CLASS_ARRAY);
		try {
			return clazz.getMethod(methodName, args);
		}
		catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<OwnedMethodId, BeanMethodHolder<?>> beanMethodHolders = new ConcurrentHashMap<>();

	@RequiredArgsConstructor
	public static class ObjectBeanMethod<C> implements BeanMethod<C>
	{
		@Override
		public Object invoke(C context)
		{
			try {
				return method.invoke(object, invokerStatic.resolveArguments(context));
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		private final Object object;

		private final Method method;

		private final MethodInvokerStatic<C> invokerStatic;
	}

	@RequiredArgsConstructor
	public static class BeanMethodHolder<C>
	{
		public BeanMethod<C> toObjectBeanMethod(Object object)
		{
			return new ObjectBeanMethod<>(object, method, invokerStatic);
		}

		@Getter
		@Accessors(fluent = true)
		private final Class<?> clazz;

		private final Method method;

		private final MethodInvokerStatic<C> invokerStatic;
	}
}
