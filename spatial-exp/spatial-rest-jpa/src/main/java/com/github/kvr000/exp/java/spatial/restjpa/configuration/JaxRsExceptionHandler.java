package com.github.kvr000.exp.java.spatial.restjpa.configuration;


import jakarta.ws.rs.ClientErrorException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class JaxRsExceptionHandler
{
	@ExceptionHandler
	public ResponseEntity<String> handlerJaxRsClientErrorException(ClientErrorException ex)
	{
		return new ResponseEntity<>(ex.getMessage(), HttpStatusCode.valueOf(ex.getResponse().getStatus()));
	}
}
