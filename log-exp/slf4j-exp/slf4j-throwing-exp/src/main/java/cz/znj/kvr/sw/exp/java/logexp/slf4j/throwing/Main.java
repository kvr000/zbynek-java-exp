package cz.znj.kvr.sw.exp.java.logexp.slf4j.throwing;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class Main
{
	public static void main(String[] args)
	{
		log.info("Hello");
		log.info("Throwing parameter: {}", new Object() { public String toString() { throw new NumberFormatException("bad number"); }});
		log.info("After throwing");
	}
}
