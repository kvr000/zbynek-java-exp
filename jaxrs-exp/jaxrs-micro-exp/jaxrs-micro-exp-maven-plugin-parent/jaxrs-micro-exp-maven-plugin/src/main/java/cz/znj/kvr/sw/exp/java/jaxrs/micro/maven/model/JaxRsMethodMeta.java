package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.Data;

import java.util.List;


/**
 * Meta information about method.
 */
@Data
public class JaxRsMethodMeta
{
	/** HTTP method. */
	private String method;

	/** URL path. */
	private String path;

	/** Additional matching conditions. */
	private List<Condition> conditions;

	/** Function signature. */
	private String function;
}
