package com.github.kvr000.exp.java.quarkus.securefield.bean;

import com.github.kvr000.exp.java.quarkus.securefield.ticket.TicketRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;


@Singleton
@RequiredArgsConstructor(onConstructor_ = @__(@Inject))
public class DynamicBean
{
	private final TicketRepository ticketRepository;

	public String greet() {
		return "Hello from DynamicBean: ticketRepository=" + ticketRepository;
	}
}
