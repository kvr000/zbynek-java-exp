package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.router;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container.ContainerContext;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.context.RequestExchange;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.response.ServicerResponse;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CompletableFuture;


public interface ServicerRouter
{
	CompletableFuture<ServicerResponse> call(RequestExchange requestExchange, ContainerContext container);

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface RootRouter
	{
	}
}
