package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.EqualsAndHashCode;


/**
 * Condition based on query parameter.
 */
@EqualsAndHashCode(callSuper = true)
public class QueryParamCondition extends KeyValueCondition
{
	public QueryParamCondition(String key, String value)
	{
		super(key, value);
	}

	@Override
	public String name()
	{
		return "queryParam";
	}
}
