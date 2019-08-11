package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reflect;


public interface MethodInvokerStatic<C>
{
	Object[] resolveArguments(C context);
}
