package cz.znj.kvr.sw.exp.java.spring.thymeleaf.intro.controller;

import lombok.Data;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;


/**
 *
 */
@Controller
public class HomeController
{
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String home(Map<String, Object> model)
	{
		model.put("message", "Hello world");
		model.put("command", new InputModel());
		return "home";
	}

	@Data
	public static class InputModel
	{
		private String message;
	}
}
