package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public abstract class ModelView<T> implements View
{
	private final String name;

	private final T model;
}
