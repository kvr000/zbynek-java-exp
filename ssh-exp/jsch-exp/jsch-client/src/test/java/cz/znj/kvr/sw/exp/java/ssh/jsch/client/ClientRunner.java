package cz.znj.kvr.sw.exp.java.ssh.jsch.client;

import com.google.inject.Guice;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.OpenSSHConfig;
import com.jcraft.jsch.Session;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactoryModule;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.RootCommandContext;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;


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
		JSch jSch = new JSch();
		jSch.setConfigRepository(OpenSSHConfig.parseFile("~/.ssh/config"));
		//jSch.addIdentity(Paths.get(System.getProperty("user.home"), ".ssh/id_rsa").toString());

		Session session = jSch.getSession("localhost");
		try {
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			ChannelExec channel = (ChannelExec) session.openChannel("exec");
			channel.setCommand("ls");
			CompletableFuture<String> result = new CompletableFuture<>();
			try (ByteArrayOutputStream responseStream = new ByteArrayOutputStream() {
				@Override
				public void close()
				{
					result.complete(this.toString(StandardCharsets.UTF_8));
				}
			}) {
				channel.setOutputStream(responseStream);
				channel.connect();
				System.out.print(result.get());
			}
		}
		finally {
			session.disconnect();
		}
		return 0;
	}
}
