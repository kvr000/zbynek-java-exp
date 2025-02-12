package com.github.kvr000.exp.java.logexp.log4j2.pattern;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Slf4jMain
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
