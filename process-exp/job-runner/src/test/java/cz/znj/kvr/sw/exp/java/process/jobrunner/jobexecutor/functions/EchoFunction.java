package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor.functions;

import java.util.function.Function;


/**
 * Function which sleeps for a while.
 */
public class EchoFunction implements Function<String[], Integer>
{
	@Override
	public Integer apply(String[] args)
	{
		System.out.println(String.join(" ", args));
		return 0;
	}
}
