package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.mvc.view;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.ws.rs.core.Link;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;


@Getter
@Accessors(fluent = true)
public class AbstractWidget implements Widget
{
	int status;

	boolean isFinal;

	long size;

	Map<String, Object> metadata;

	Map<String, Link> links;

	Reader content;
}
