package cz.znj.kvr.sw.exp.java.nio.socket;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;


/**
 * Nio non-blocking socket experiments.
 */
public class NioSocketTest
{
	private static final byte[] DATA = StringUtils.repeat("hellocute\n", 100_000).getBytes(StandardCharsets.UTF_8);

	@Test(timeout = 50000L)
	public void testNioSocketCommunication() throws IOException, ExecutionException, InterruptedException
	{
		int wantedCount = 128;
		ExecutorService executor = new ThreadPoolExecutor(0, wantedCount*2, 5L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		AtomicInteger count = new AtomicInteger();
		try (Selector selector = Selector.open()) {
			ServerSocketChannel listener = ServerSocketChannel.open();
			listener.configureBlocking(false);
			listener.bind(new InetSocketAddress("localhost", 0), Integer.MAX_VALUE);
			InetSocketAddress address = (InetSocketAddress)listener.getLocalAddress();
			CompletableFuture<?> workers = CompletableFuture.allOf(IntStream.range(0, wantedCount)
				.mapToObj(i -> CompletableFuture.runAsync(() -> {
						try {
							Socket socket = new Socket(address.getAddress(), address.getPort());
							CompletableFuture<byte[]> reader =
								CompletableFuture.supplyAsync(() -> {
									try {
										return IOUtils.toByteArray(socket.getInputStream());
									}
									catch (IOException ex) {
										throw new UncheckedIOException(ex);
									}
								},
								executor
							);
							socket.getOutputStream().write(DATA);
							socket.shutdownOutput();
							byte[] output = reader.get();
							Assert.assertArrayEquals(DATA, output);
						}
						catch (IOException|InterruptedException|ExecutionException e) {
							throw new RuntimeException(e);
						}
					},
					executor
				))
				.toArray(CompletableFuture[]::new)
			)
				.whenComplete((v, ex) -> {
					if (ex != null)
						ex.printStackTrace();
					IOUtils.closeQuietly(listener);
					selector.wakeup();
				});
			listener.register(selector, SelectionKey.OP_ACCEPT);
			for (;;) {
				selector.select();
				if (!listener.isOpen())
					break;
				for (Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator(); keyIterator.hasNext(); ) {
					SelectionKey key = keyIterator.next();
					try {
						if (key.readyOps() != 0) {
							if (key.channel() == listener) {
								SocketChannel client = listener.accept();
								if (client != null) {
									client.configureBlocking(false);
									client.register(selector, SelectionKey.OP_READ);
								}
							}
							else {
								SocketChannel client = (SocketChannel)key.channel();
								ByteBuffer buf = (ByteBuffer)key.attachment();
								if (buf != null) {
									client.write(buf);
									if (!buf.hasRemaining()) {
										key.interestOps(SelectionKey.OP_READ);
										key.attach(null);
									}
								}
								else {
									buf = ByteBuffer.allocateDirect(1024);
									int read = client.read(buf);
									if (read > 0) {
										buf.flip();
										key.interestOps(SelectionKey.OP_WRITE);
										key.attach(buf);
									}
									else {
										key.cancel();
										client.shutdownOutput();
										client.close();
										count.incrementAndGet();
									}
								}
							}
						}
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
					finally {
						keyIterator.remove();
					}
				}
			}
			workers.get();
		}
	}
}
