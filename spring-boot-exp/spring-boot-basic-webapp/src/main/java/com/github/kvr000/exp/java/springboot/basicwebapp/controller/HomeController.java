package com.github.kvr000.exp.java.springboot.basicwebapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController("/")
public class HomeController
{
	@GetMapping("/")
	public String home() {
		return "Welcome!\n";
	}
}
