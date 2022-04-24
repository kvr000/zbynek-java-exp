package cz.znj.kvr.sw.exp.java.netty.netty4.proxy.common;

import io.netty.channel.unix.DomainSocketAddress;
import lombok.Builder;
import lombok.Value;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;


/**
 * Address specification, including protocol.
 */
@Builder(builderClassName = "Builder")
@Value
public class AddressSpec
{
	/**
	 * One of tcp4, tcp6, unix, domain:
	 */
	String proto;
	/**
	 * Domain socket path:
	 */
	String path;
	/**
	 * Inet socket host:
	 */
	String host;
	/**
	 * Inet socket port:
	 */
	int port;

	public static AddressSpec fromSocketAddress(SocketAddress address)
	{
		if (address instanceof DomainSocketAddress a) {
			return AddressSpec.builder()
				.proto("domain")
				.path(a.path())
				.build();
		}
		else if (address instanceof InetSocketAddress a) {
			return AddressSpec.builder()
				.proto(a.getAddress() instanceof Inet6Address ? "tcp6" : "tcp4")
				.host(a.getHostString())
				.port(a.getPort())
				.build();
		}
		else {
			throw new UnsupportedOperationException("Unsupported SocketAddress: " + address.getClass().getName());
		}
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder(proto);
		sb.append("://");
		if (proto.equals("unix") || proto.equals("domain")) {
			sb.append(path);
		}
		else {
			sb.append(host).append(":").append(port);
		}
		return sb.toString();
	}
}
