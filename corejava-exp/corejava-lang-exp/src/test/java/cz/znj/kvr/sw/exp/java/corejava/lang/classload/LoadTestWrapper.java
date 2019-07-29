package cz.znj.kvr.sw.exp.java.corejava.lang.classload;


/**
 * The test is not thread safe, parallel testing must be disabled.
 */
public class LoadTestWrapper
{
	public LoadTestClass method()
	{
		return new LoadTestClass();
	}
}
