package cz.znj.kvr.sw.exp.java.logexp.slf4j.cmdline;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Test object for JsonIgnore testing.
 */
@Slf4j
public class Print
{
	public static void main(String[] args)
	{
		Logger main = LoggerFactory.getLogger("Main");
		main.debug("Debug");
		main.info("Info");
		main.warn("Warn");
		main.error("Error");

		Logger second = LoggerFactory.getLogger("Second");
		second.debug("Debug");
		second.info("Info");
		second.warn("Warn");
		second.error("Error");
	}
}
