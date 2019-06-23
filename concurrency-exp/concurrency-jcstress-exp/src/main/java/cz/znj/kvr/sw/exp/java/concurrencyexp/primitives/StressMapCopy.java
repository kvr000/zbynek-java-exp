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
import java.util.Optional;


/**
 *
 */
public class StressMapCopy
{
	@JCStressTest
	@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Initial value")
	@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Copied value")
	@Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Uninitialized, because of non-volatile")
	@Outcome(expect = Expect.FORBIDDEN, desc = "Unexpected")
	@State
	public static class MapCopyUnsafe
	{
		static Map<Integer, Integer> copiedMap = Collections.singletonMap(1, 1);
		Map<Integer, Integer> map = Collections.singletonMap(1, -1);

		@Actor
		public void actor1()
		{
			map = new HashMap<>(copiedMap);
		}

		@Actor
		public void arbiter(I_Result result)
		{
			// dummy, just doing similar amount of work to increase chance of conflict:
			int expected = new HashMap<>(copiedMap).get(1);
			result.r1 = map.getOrDefault(expected, 0);
		}
	}

	@JCStressTest
	@Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Initial value")
	@Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Copied value")
	@Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Uninitialized, impossible with volatile")
	@Outcome(expect = Expect.FORBIDDEN, desc = "Unexpected")
	@State
	public static class MapCopySafe
	{
		static Map<Integer, Integer> copiedMap = Collections.singletonMap(1, 1);
		volatile Map<Integer, Integer> map = Collections.singletonMap(1, -1);

		@Actor
		public void actor1()
		{
			map = new HashMap<>(copiedMap);
		}

		@Actor
		public void arbiter(I_Result result)
		{
			// dummy, just doing similar amount of work to increase chance of conflict:
			int expected = new HashMap<>(copiedMap).get(1);
			result.r1 = map.getOrDefault(expected, 0);
		}
	}
}
