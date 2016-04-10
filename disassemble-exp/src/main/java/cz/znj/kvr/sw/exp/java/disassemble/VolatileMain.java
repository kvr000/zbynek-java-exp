package cz.znj.kvr.sw.exp.java.disassemble;

/**
 * -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly
 */
public class VolatileMain
{
	public static void              main(String[] args)
	{
		System.exit(new VolatileMain().run());
	}

	public int                      run()
	{
		for (int i = 0; i < 10000000; ++i)
			incValue();
		return 0;
	}

	public void                     incValue()
	{
		++value;
		++more;
	}

	private volatile int            value;

	private volatile int            more;
}
