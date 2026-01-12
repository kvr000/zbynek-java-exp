package com.github.kvr000.exp.java.corejava.exit;


/**
 * Demonstrates daemon thread continuing running while main already exited and only shutdown hooks are executing.
 */
public class SyncDaemonExp
{
	private static int counter;

	public static void main(String[] args) throws Exception
	{
		new Daemon().start();
		Runtime.getRuntime().addShutdownHook(new Thread(SyncDaemonExp::syncExit));
		System.out.println("Exiting...");
	}

	private static void syncExit()
	{
		int current = getCounter();
		synchronized (SyncDaemonExp.class) {
			while (getCounter() == current) {
				try {
					SyncDaemonExp.class.wait();
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		System.out.println("counter increased, unblocking exit");
	}

	public static synchronized void incCounter()
	{
		++counter;
		SyncDaemonExp.class.notify();
	}

	public static synchronized int getCounter()
	{
		return counter;
	}

	public static class Daemon extends Thread
	{
		public Daemon() {
			setDaemon(true);
		}

		public void run() {
			for (;;) {
				synchronized (SyncDaemonExp.class) {
					try {
						Thread.sleep(1000);
						incCounter();
					}
					catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
