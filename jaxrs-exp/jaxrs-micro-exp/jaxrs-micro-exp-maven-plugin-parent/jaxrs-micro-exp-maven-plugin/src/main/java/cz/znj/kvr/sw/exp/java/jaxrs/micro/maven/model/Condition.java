package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model;

import java.util.Map;


/**
 * Matching condition interface.
 */
public interface Condition
{
	String name();

	Map<String, Object> attributes();
}
