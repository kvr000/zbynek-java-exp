package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.inject.guice;

import com.google.inject.Injector;
import lombok.experimental.Delegate;

import javax.inject.Inject;


/**
 * Straightforward proxy for now.
 */
public class GuiceInjector implements Injector
{
	@Delegate
	private final Injector injector;

	@Inject
	public GuiceInjector(Injector injector)
	{
		this.injector = injector;
	}
}
