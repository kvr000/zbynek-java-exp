package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.util.Map;


@Getter
@Accessors(fluent = true)
public class AbstractViewResult implements ViewResult
{
	int status;

	boolean isFinal;

	Map<String, Object> metadata;

	Map<String, Link> links;

	InputStream content;
}
