package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@RequestMapping(value = "/")
public class HomeController
{
	@RequestMapping(method = RequestMethod.GET, value = "/")
	public ModelAndView homepage()
	{
		return new ModelAndView(new RedirectView("/chatter/"));
	}
}
