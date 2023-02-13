package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.util.Map;

public interface ViewResult
{
	int status();

	boolean isFinal();

	Map<String, Object> metadata();

	Map<String, Link> links();

	InputStream content();
}
