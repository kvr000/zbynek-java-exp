package com.github.kvr000.exp.java.spatial.restjpa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;


@RestController
public class HomeController
{
	@GetMapping(value = "/", produces = "text/plain")
	public String home() {
		return "Welcome!\n";
	}
}
