package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.Data;

import java.util.List;


/**
 *
 */
@Data
public class JaxRsMethodMeta
{
	private String method;

	private String path;

	private List<Condition> conditions;

	private String function;
}
