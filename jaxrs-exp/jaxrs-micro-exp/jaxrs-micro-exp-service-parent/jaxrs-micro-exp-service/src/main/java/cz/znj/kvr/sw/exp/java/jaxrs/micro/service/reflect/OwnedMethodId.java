package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reflect;

import lombok.Value;
import lombok.experimental.Accessors;


/**
 * Class method, specified by its name.
 */
@Value
@Accessors(fluent = true)
public class OwnedMethodId
{
	@Override
	public String toString()
	{
		return className+"."+methodName;
	}

	Class<?> owner;

	String className;

	String methodName;
}
