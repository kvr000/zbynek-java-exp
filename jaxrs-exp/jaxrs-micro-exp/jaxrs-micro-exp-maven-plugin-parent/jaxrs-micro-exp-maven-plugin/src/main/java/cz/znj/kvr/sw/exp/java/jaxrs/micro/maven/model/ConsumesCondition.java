package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;


import lombok.EqualsAndHashCode;

import java.util.List;


/**
 *
 */
@EqualsAndHashCode(callSuper = true)
public class ConsumesCondition extends MediaTypeCondition
{
	public ConsumesCondition(List<String> values)
	{
		super(values);
	}

	@Override
	public String name()
	{
		return "consumes";
	}
}
