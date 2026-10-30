package com.github.kvr000.exp.java.quarkus.securefield;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;


/**
 * Server entry point.
 *
 * Running {@link #main(String...)} from IDE starts the application in Quarkus dev mode (live reload, dev UI at
 * /q/dev-ui).  In the packaged jar, the Quarkus generated main delegates to this class.
 */
@QuarkusMain
public class SecureFieldServer
{
	public static void main(String... args)
	{
		Quarkus.run(args);
	}
}
