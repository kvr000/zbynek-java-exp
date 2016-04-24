package cz.znj.kvr.sw.exp.java.process.processwatcher.process;

import java.util.concurrent.CompletableFuture;


/**
 * Task handle, allows controlling running processes.
 */
public interface TaskHandle
{
	CompletableFuture<Integer> waitExit();

	void terminate();

	void kill();
}
