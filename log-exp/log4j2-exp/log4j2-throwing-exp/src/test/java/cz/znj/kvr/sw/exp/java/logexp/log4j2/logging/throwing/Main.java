package cz.znj.kvr.sw.exp.java.logexp.log4j2.logging.throwing;

import lombok.extern.log4j.Log4j2;
import org.testng.annotations.Test;


/**
 * Test object for JsonIgnore testing.
 */
@Log4j2
public class Main
{
	public static void main(String[] args)
	{
		log.error("Hello");
		log.error("Throwing parameter: {}", new Object() { public String toString() { throw new NumberFormatException("bad number"); }});
		log.error("After throwing");
	}
}
