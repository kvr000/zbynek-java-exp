package com.github.kvr000.exp.java.springboot.basicwebapp;

import com.github.kvr000.exp.java.springboot.basicwebapp.controller.HomeController;
import com.github.kvr000.exp.java.springboot.basicwebapp.servlet.InitializationService;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import javax.inject.Inject;
import java.util.Arrays;


@Log4j2
@SpringBootApplication
@ComponentScan(basePackageClasses = { HomeController.class, InitializationService.class })
public class Application
{
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
