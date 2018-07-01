package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.controller;

import com.google.common.collect.ImmutableMap;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model.LoggedUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(value = "/login/")
public class LoginController
{
	@RequestMapping(method = RequestMethod.GET, value = "/")
	public ModelAndView showLogin(@RequestParam("redirect") String redirect)
	{
		LoginForm loginForm = LoginForm.builder()
				.redirect(redirect)
				.build();
		return new ModelAndView("login", ImmutableMap.of("loginForm", loginForm));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/")
	public ModelAndView submitLogin(
			@Valid @ModelAttribute LoginForm loginForm,
			BindingResult result,
			HttpSession session
	)
	{
		if (result.hasErrors()) {
			return new ModelAndView("login", ImmutableMap.of("loginForm", loginForm));
		}
		LoggedUser loggedUser = new LoggedUser();
		loggedUser.setUsername(loginForm.getName());
		session.setAttribute("loggedUser", loggedUser);
		return new ModelAndView(
				new RedirectView(StringUtils.defaultIfEmpty(loginForm.getRedirect(), "/"))
		);
	}

	@NoArgsConstructor
	@AllArgsConstructor
	@Data
	@Builder
	public static class LoginForm
	{
		private String redirect;

		@NotBlank
		private String name;
	}
}
