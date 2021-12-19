package cz.znj.kvr.sw.exp.java.ssh.minasshd.client;

import com.google.inject.Guice;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactoryModule;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.RootCommandContext;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.session.ClientSession;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
 * SSH client test, partially asynchronous.
 */
public class ClientRunner extends AbstractCommand
{
	public static void main(String[] args) throws Exception
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceBeanFactoryModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(ClientRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "clientrunner", null),
				Arrays.asList(args0)
			);
		});
	}

	public int execute() throws Exception
	{
		SshClient client = SshClient.setUpDefaultClient();
		client.start();

		try (ClientSession session = client.connect(System.getProperty("user.name"), "localhost", 22)
			.verify(10, TimeUnit.SECONDS).getSession()
		) {
			session.auth().verify(10, TimeUnit.SECONDS);

			try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
			     ClientChannel channel = session.createExecChannel("ls")
			) {
				CompletableFuture<String> result = new CompletableFuture<>();
				channel.setOut(responseStream);
				try {
					channel.open().verify().addListener(f -> {
						if (channel.isOpen()) {
							try {
								channel.waitFor(
									EnumSet.of(ClientChannelEvent.EOF),
									TimeUnit.SECONDS.toMillis(10));
							}
							finally {
								try {
									channel.close();
									Integer exit = channel.getExitStatus();
									if (exit == null || exit != 0) {
										result.completeExceptionally(new IOException("Command exited with status: exit="+exit));
									}
								}
								catch (IOException e) {
									result.completeExceptionally(e);
								}
							}
						}
						else {
							result.completeExceptionally(f.getException());
						}
						String responseString = responseStream.toString(StandardCharsets.UTF_8);
						result.complete(responseString);
					});
				}
				catch (Throwable ex) {
					channel.close();
					result.completeExceptionally(ex);
				}
				System.out.print(result.get());
			}
		}
		finally {
			client.stop();
		}
		return 0;
	}
}
