package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect;


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
