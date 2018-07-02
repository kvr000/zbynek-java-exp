package cz.znj.kvr.sw.exp.java.spring.websocket.chatter.web.controller;

import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model.LoggedUser;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.model.Message;
import cz.znj.kvr.sw.exp.java.spring.websocket.chatter.core.service.MessageService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Controller
@RequestMapping(value = "/chatter/")
@MessageMapping(value = "/chatter/")
@SessionAttributes("loggedUser")
@AllArgsConstructor(onConstructor = @__(@Inject))
public class ChatterController
{
	private MessageService messageService;

	private SimpMessagingTemplate messagingTemplate;

	@PostConstruct
	public void init()
	{
		messageService.listenMessages((Message message) -> {
					messagingTemplate.convertAndSend("/topic/chatter/public/", message);
				}
		);
	}

	@RequestMapping(method = RequestMethod.GET, value = "")
	public ModelAndView chatterPage()
	{
		return new ModelAndView("chatter");
	}

	@MessageMapping("postMessage")
	public void postMessage(
			@Valid ChatterController.MessageForm messageForm,
			@SessionAttribute("loggedUser") LoggedUser loggedUser
	)
	{
		messageService.postMessage(
				Message.builder()
						.username(loggedUser.getUsername())
						.time(System.currentTimeMillis())
						.content(messageForm.getContent())
						.build()
		);
	}

	@Data
	public static class MessageForm
	{
		@NotBlank
		String content;
	}
}
