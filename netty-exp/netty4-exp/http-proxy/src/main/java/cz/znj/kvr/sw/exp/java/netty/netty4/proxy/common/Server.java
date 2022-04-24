package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common;

import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Identifies running server.
 */
public interface Server extends AutoCloseable
{
	/** Future completed when server is closed. */
	CompletableFuture<Void> closedFuture();

	/** Server listening address. */
	SocketAddress listenAddress();

	/** Closes the server asynchronously. */
	CompletableFuture<Void> cancel();

	/** Closes the server. */
	@Override
	void close();

	/**
	 * Waits for server to complete and closes all others.
	 *
	 * @param servers
	 * 	list of servers
	 *
	 * @return
	 * 	future completing when one server closes.
	 */
	static CompletableFuture<Void> waitOneAndClose(List<? extends Server> servers)
	{
		return new CompletableFuture<>() {
			{
				servers.forEach(s -> {
					s.closedFuture().whenCompleteAsync((v, ex) -> closeAll(ex));
				});
			}

			private synchronized void closeAll(Throwable ex)
			{
				servers.forEach(s -> s.close());
				if (ex != null) {
					completeExceptionally(ex);
				}
				else {
					complete(null);
				}
			}
		};
	}
}
