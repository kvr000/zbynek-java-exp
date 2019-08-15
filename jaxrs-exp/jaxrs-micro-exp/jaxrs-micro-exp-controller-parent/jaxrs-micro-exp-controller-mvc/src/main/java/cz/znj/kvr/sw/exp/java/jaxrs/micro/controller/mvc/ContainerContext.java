package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.mvc;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container.BeanMethod;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect.MethodInvokerStatic;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;


public interface MvcProcessor
{
	<T> T getBean(Class<T> name);

	Function<RequestContext, Object> contextObjectResolver(Type type);

	<C> BeanMethod<C> resolveMethod(OwnedMethodHolder method, BiFunction<ContainerContext, OwnedMethodHolder, MethodInvokerStatic<C>> invokerProvider);
}
