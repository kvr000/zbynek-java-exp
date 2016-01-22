package cz.znj.kvr.sw.exp.java.concurrencyexp.primitives.test;

import org.junit.Test;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class CyclicBarrierTest
{
	@Test(timeout = 1000L)
	public void			testSingle() throws BrokenBarrierException, InterruptedException
	{
		CyclicBarrier barrier = new CyclicBarrier(1);
		barrier.await();
	}

	@Test(timeout = 1000L)
	public void			testReuse() throws BrokenBarrierException, InterruptedException
	{
		CyclicBarrier barrier = new CyclicBarrier(1);
		barrier.await();
		barrier.reset();
		barrier.await();
	}

	@Test(/*expected = BrokenBarrierException.class,*/ timeout = 1000L)
	public void			testBroken() throws BrokenBarrierException, InterruptedException
	{
		CyclicBarrier barrier = new CyclicBarrier(1);
		try {
			barrier.await(0, TimeUnit.NANOSECONDS);
		}
		catch (TimeoutException ex) {
		}
		barrier.await();
	}
}
