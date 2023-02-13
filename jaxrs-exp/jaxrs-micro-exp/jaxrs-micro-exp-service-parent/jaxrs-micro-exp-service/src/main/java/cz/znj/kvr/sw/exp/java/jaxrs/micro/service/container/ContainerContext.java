package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container;

import com.google.inject.Injector;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect.OwnedMethodHolder;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect.MethodArgumentsResolver;

import java.lang.reflect.Type;
import java.util.function.BiFunction;
import java.util.function.Function;


public interface ContainerContext
{
	Injector getInjector();

	Function<RequestContext, Object> contextObjectResolver(Type type);

	<C> BeanMethod<C> resolveMethod(OwnedMethodHolder method, BiFunction<ContainerContext, OwnedMethodHolder, MethodArgumentsResolver<C>> invokerProvider);
}
