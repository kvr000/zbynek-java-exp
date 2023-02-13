package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;


public class PageView<T> extends ModelView<T>
{
	public PageView(String name, T model)
	{
		super(name, model);
	}
}
