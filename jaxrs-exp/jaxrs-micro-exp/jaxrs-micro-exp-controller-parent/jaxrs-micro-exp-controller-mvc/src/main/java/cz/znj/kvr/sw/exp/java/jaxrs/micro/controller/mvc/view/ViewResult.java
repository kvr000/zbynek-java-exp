package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.mvc.view;


import javax.ws.rs.core.Link;
import java.util.Map;

public interface ViewResult
{
	int status();

	Map<String, Link> links();

	String content();
}
