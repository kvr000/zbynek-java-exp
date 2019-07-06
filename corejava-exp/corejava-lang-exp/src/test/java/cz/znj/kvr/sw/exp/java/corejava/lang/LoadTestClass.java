package cz.znj.kvr.sw.exp.java.corejava.lang.classload;


/**
 * The test is not thread safe, parallel testing must be disabled.
 */
public class LoadTestClass
{
	static
	{
		LoadTest.loaded = true;
	}
}
