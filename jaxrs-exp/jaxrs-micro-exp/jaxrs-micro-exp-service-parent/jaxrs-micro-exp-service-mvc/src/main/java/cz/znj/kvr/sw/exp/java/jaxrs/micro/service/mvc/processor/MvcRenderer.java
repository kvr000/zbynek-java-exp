package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.processor;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.ModelView;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view.Widget;

import jakarta.ws.rs.core.Response;


public interface MvcRenderer
{
	Widget render(Response response, ModelView view);
}
