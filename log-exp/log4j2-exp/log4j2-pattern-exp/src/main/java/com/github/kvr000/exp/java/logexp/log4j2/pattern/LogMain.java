package com.github.kvr000.exp.java.logexp.log4j2.pattern;

import lombok.extern.log4j.Log4j2;


@Log4j2
public class LogMain
{
	public static void main(String[] args)
	{
		log.error("Hello World");
		try {
			Long.parseLong("hello");
		}
		catch (NumberFormatException ex) {
			log.error("Parse failed: ", ex);
		}
	}
}
