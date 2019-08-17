package cz.znj.kvr.sw.exp.java.jaxrs.micro.controller.mvc.view;

import lombok.Getter;
import lombok.experimental.Accessors;

import javax.ws.rs.core.Link;
import java.util.Map;


@Getter
@Accessors(fluent = true)
public class AbstractViewResult implements ViewResult
{
	int status;

	Map<String, Link> links;

	String content;
}
