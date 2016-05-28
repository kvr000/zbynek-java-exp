package cz.znj.kvr.sw.exp.java.netty.netty4.server.forward;

import lombok.Builder;
import lombok.Value;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;


/**
 * HTTP proxy factory.
 */
public interface HttpProxyFactory
{
	CompletableFuture<CompletableFuture<Void>> runProxy(Config config);

	@Builder
	@Value
	class Config
	{
		InetSocketAddress listenAddress;

		Map<String, String> remapHosts;

		Map<String, String> addedHeaders;
	}
}
