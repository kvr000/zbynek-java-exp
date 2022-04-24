package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.forward;

import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.AddressSpec;
import cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common.Server;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;


/**
 * Port forwarding component.
 */
public interface PortForwarderFactory extends AutoCloseable
{
	/**
	 * Runs port forwarding as specified by forward configuration.
	 *
	 * @param forwards forward configurations
	 * @return future completing once the port forwardings are successfully setup.  The inner future completes when
	 * any of forwarding is closed (due to error or cancellation).
	 * @apiNote All forwardings are cancelled when any of them fails.
	 */
	List<CompletableFuture<Server>> runForwards(List<ForwardConfig> forwards);

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
	CompletableFuture<Server> runForward(ForwardConfig forward);

	@Builder(builderClassName = "Builder")
	@Value
	class ForwardConfig
	{
		AddressSpec bind;
		AddressSpec connect;
	}
}
