package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.container;


public interface BeanMethod<C> extends AutoCloseable
{
	Object invoke(C context);

	@Override
	default void close()
	{
	}
}
