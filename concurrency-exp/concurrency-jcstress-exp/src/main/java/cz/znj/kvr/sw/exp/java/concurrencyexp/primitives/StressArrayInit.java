package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 *
 */
public class StressArrayInit
{
	@JCStressTest
	@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Original value")
	@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Initialized value")
	@Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Uninitialized, because of non-volatile")
	@Outcome(expect = Expect.FORBIDDEN, desc = "Unexpected")
	@State
	public static class ArrayInitUnsafe
	{
		int [] array = new int[]{ -1 };

		@Actor
		public void actor1()
		{
			int n[] = new int[1];
			n[0] = 1;
			array = n;
		}

		@Actor
		public void arbiter(I_Result result)
		{
			result.r1 = array[0];
		}
	}

	@JCStressTest
	@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Original value")
	@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Initialized value")
	@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Uninitialized, impossible because of volatile")
	@Outcome(expect = Expect.FORBIDDEN, desc = "Unexpected")
	@State
	public static class ArrayInitSafe
	{
		volatile int [] array = new int[]{ -1 };

		@Actor
		public void actor1()
		{
			int n[] = new int[1];
			n[0] = 1;
			array = n;
		}

		@Actor
		public void arbiter(I_Result result)
		{
			result.r1 = array[0];
		}
	}
}
