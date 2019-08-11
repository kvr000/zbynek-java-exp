package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.container;


public interface BeanMethod<C> extends AutoCloseable
{
	Object invoke(C context);

	@Override
	default void close()
	{
	}
}
