package cz.znj.kvr.sw.exp.java.jmx.jminixexp.controller;

import cz.znj.kvr.sw.exp.java.jmx.jminixexp.bo.GreetingManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;


@Controller
@RequestMapping("/")
public class MainController
{
	@RequestMapping(value = "/", method = RequestMethod.GET)
	@ResponseBody
	public String			welcome()
	{
		return greetingManager.getGreeting();
	}

	@Inject
	protected GreetingManager	greetingManager;		
}
