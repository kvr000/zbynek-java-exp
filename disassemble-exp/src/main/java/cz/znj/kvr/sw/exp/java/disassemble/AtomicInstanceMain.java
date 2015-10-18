package cz.znj.kvr.sw.exp.java.disassemble;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly
 */
public class AtomicInstanceMain
{
	public static void              main(String[] args)
	{
		System.exit(new AtomicInstanceMain().run());
	}

	public int                      run()
	{
		for (int i = 0; i < 10000000; ++i)
			incValue();
		return 0;
	}

	public void                     incValue()
	{
		finValue.incrementAndGet();
		modifValue.incrementAndGet();
	}

	private final AtomicInteger     finValue = new AtomicInteger();

	private AtomicInteger           modifValue = new AtomicInteger();
}
