package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

public interface Widget
{
	int status();

	boolean isFinal();

	long size();

	Map<String, Object> metadata();

	Map<String, Link> links();

	Reader content();
}
