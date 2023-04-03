package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.httpproxy;

import lombok.Builder;
import lombok.Value;
import net.dryuf.netty.address.AddressSpec;
import net.dryuf.netty.core.Server;

import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * HTTP proxy factory.
 */
public interface HttpProxyFactory
{
	CompletableFuture<Server> runProxy(Config config);

	@Builder
	@Value
	class Config
	{
		AddressSpec listenAddress;

		Map<String, String> remapHosts;

		Map<String, String> addedHeaders;
	}
}
