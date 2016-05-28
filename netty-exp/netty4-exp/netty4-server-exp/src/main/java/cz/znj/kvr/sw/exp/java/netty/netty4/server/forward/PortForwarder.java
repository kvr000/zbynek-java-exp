package cz.znj.kvr.sw.exp.java.netty.netty4.server.forward;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Port forwarding component.
 */
public interface PortForwarder extends AutoCloseable
{
	/**
	 * Runs port forwarding as specified by forward configuration.
	 *
	 * @param forwards
	 * 	forward configurations
	 *
	 * @return
	 * 	future completing once the port forwardings are successfully setup.  The inner future completes when
	 * 	any of forwarding is closed (due to error or cancellation).
	 *
	 * @apiNote
	 * 	All forwardings are cancelled when any of them fails.
	 */
	CompletableFuture<CompletableFuture<Void>> runForwards(List<ForwardConfig> forwards);

	/**
	 * Runs port forwarding as specified by forward configuration.
	 *
	 * @param forward
	 * 	forward configuration
	 *
	 * @return
	 * 	future completing once the port forwarding is successfully setup.  The inner future completes when
	 * 	forwarding is closed (due to error or cancellation).
	 */
	CompletableFuture<CompletableFuture<Void>> runForward(ForwardConfig forward);

	@Builder(builderClassName = "Builder")
	@Value
	class ForwardConfig
	{
		@lombok.Builder(builderClassName = "Builder")
		@Value
		public static class AddressSpec
		{
			/** One of tcp4, tcp6, unix: */
			String proto;
			/** Domain socket path: */
			String path;
			/** Inet socket host: */
			String host;
			/** Inet socket port: */
			int port;
		}

		AddressSpec bind;
		AddressSpec connect;
	}
}
