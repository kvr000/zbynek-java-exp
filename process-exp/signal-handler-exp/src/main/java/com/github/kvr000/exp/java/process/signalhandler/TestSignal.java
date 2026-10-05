package com.github.kvr000.exp.java.process.signalhandler;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import sun.misc.Signal;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Main class of TestSignal.
 *
 * This tests process lifecycle from start, handling signals and terminating.  User is expected to terminate this
 * process within 60 seconds.
 */
@Log4j2
public class TestSignal
{
	private static final AtomicInteger status = new AtomicInteger();

	@SneakyThrows
	public static void main(String[] args)
	{
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			switchStatus(3, 4);
		}) { { setName("shutdown"); }});
		sun.misc.SignalHandler handler = new sun.misc.SignalHandler()
		{
			@Override
			public void handle(Signal sig)
			{
				switchStatus(1, 2);
			}
		};
		Signal.handle(new Signal("INT"), handler);
		Signal.handle(new Signal("TERM"), handler);
		try {
			Signal.handle(new Signal("HUP"), handler);
		}
		catch (IllegalArgumentException ex) {
			log.warn("Cannot register HUP signal: ", ex);
		}
		switchStatus(0, 1);
		Thread.sleep(100L);
		waitStatus(2);
		switchStatus(2, 3);
	}

	@SneakyThrows
	public static void waitStatus(int expected) {
		synchronized (status) {
			if (status.get() < expected) {
				status.wait(60_000L);
			}
			if (status.get() != expected) {
				new IllegalStateException("Unexpected status: old=" + status + " expected=" + expected);
			}
		}
	}

	public static void switchStatus(int expected, int toset) {
		synchronized (status) {
			if (!status.compareAndSet(expected, toset)) {
				IllegalStateException ex =
					new IllegalStateException("Unexpected status: old=" + status + " expected=" + expected);
				log.error(ex);
				throw ex;
			}
			log.info("Switch status: old=" + expected + " toset=" + toset);
			status.notify();
		}
	}
}
