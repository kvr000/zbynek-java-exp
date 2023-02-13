package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader;

import lombok.Data;

import java.util.List;


/**
 * Method metadata, supported methods, path and function to be executed.
 */
@Data
public class MethodMeta
{
	private List<String> methods;

	private String path;

	private String function;
}
