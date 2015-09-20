package cz.znj.kvr.sw.exp.java.disassemble;

import sun.misc.Unsafe;

/**
 * Created by rat on 2015-09-20.
 */
public class Main
{
	public static void main(String[] args)
	{
		System.exit(new Main().run());
	}

	public int run()
	{
		for (int i = 0; i < 10000000; ++i)
			incValue();
		return 0;
	}

	public void incValue()
	{
		++value;
		++more;
	}

	private volatile int value;
	private volatile int more;
}
