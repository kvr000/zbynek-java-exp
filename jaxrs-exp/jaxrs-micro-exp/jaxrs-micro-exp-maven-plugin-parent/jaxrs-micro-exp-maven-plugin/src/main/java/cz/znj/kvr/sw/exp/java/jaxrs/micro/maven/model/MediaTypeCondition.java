package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.EqualsAndHashCode;

import java.util.List;


/**
 * Media type, accept or produce condition.
 */
@EqualsAndHashCode(callSuper = true)
public abstract class MediaTypeCondition extends ListValueCondition
{
	protected MediaTypeCondition(List<String> values)
	{
		super(values);
	}
}
