package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.reader;

import lombok.Data;

import java.util.List;


@Data
public class MethodMeta
{
	private List<String> methods;

	private String path;

	private String function;
}
