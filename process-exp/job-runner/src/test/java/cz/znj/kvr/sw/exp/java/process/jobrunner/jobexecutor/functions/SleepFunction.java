package cz.znj.kvr.sw.exp.java.process.jobrunner.jobexecutor.functions;

import lombok.SneakyThrows;

import java.util.function.Function;


/**
 * Function which sleeps for a while.
 */
public class SleepFunction implements Function<String[], Integer>
{
	@SneakyThrows
	@Override
	public Integer apply(String[] strings)
	{
		Thread.sleep((long) (Double.parseDouble(strings[0])*1000));
		return 0;
	}
}
