package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * Condition based on Produces (accepts).
 */
@EqualsAndHashCode(callSuper = true)
public class ProducesCondition extends MediaTypeCondition
{
	public ProducesCondition(List<String> values)
	{
		super(values);
	}

	@Override
	public String name()
	{
		return "produces";
	}
}
