package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.MethodArgumentsResolver;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;


public interface ContainerContext
{
	<T> T getBean(Class<T> name);

	Function<RequestContext, Object> contextObjectResolver(Type type);

	<C> BeanMethod<C> resolveMethod(OwnedMethodHolder method, BiFunction<ContainerContext, OwnedMethodHolder, MethodArgumentsResolver<C>> invokerProvider);
}
