package cz.znj.kvr.sw.exp.java.disassemble;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly
 */
public class AtomicFieldMain
{
	public static void              main(String[] args)
	{
		System.exit(new AtomicFieldMain().run());
	}

	public int                      run()
	{
		for (int i = 0; i < 10000000; ++i)
			incValue();
		return 0;
	}

	public void                     incValue()
	{
		valueUpdater.incrementAndGet(this);
		moreUpdater.incrementAndGet(this);
	}

	private volatile int            value;

	private volatile int            more;

	private static final AtomicIntegerFieldUpdater<AtomicFieldMain> valueUpdater = AtomicIntegerFieldUpdater.newUpdater(AtomicFieldMain.class, "value");

	private static final AtomicIntegerFieldUpdater<AtomicFieldMain> moreUpdater = AtomicIntegerFieldUpdater.newUpdater(AtomicFieldMain.class, "more");
}
