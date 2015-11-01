package cz.znj.kvr.sw.exp.java.jmx.jminixexp.bo;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


@ManagedResource(value = "cz.znj.kvr.sw.exp.java.jmx.jminixexp:name=MainController", description = "MainController")
public class GreetingManager
{
	@ManagedAttribute(description = "greeting")
	public void			setGreeting(String greeting)
	{
		this.greeting = greeting;
	}

	@ManagedAttribute(description = "greeting")
	public String			getGreeting()
	{
		return greeting;
	}

	private String			greeting = "Hello";
}
