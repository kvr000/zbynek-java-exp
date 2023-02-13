package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect;


/**
 * Method arguments resolver.
 *
 * @param <C>
 *         type of context
 */
public interface MethodArgumentsResolver<C>
{
	Object[] resolveArguments(C context);
}
