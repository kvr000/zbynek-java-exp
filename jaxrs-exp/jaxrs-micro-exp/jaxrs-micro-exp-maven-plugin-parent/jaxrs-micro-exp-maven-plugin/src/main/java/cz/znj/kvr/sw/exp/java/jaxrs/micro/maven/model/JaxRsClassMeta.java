package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import lombok.Data;

import java.util.List;


/**
 * Meta information about class.
 */
@Data
public class JaxRsClassMeta
{
	private String path;

	private String className;

	private List<Condition> conditions;

	private List<JaxRsMethodMeta> methods;
}
