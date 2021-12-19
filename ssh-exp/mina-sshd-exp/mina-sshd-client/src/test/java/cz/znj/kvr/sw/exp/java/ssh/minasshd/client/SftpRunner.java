package cz.znj.kvr.sw.exp.java.ssh.minasshd.client;

import com.google.inject.Guice;
import net.dryuf.cmdline.app.AppContext;
import net.dryuf.cmdline.app.BeanFactory;
import net.dryuf.cmdline.app.CommonAppContext;
import net.dryuf.cmdline.app.guice.GuiceBeanFactoryModule;
import net.dryuf.cmdline.command.AbstractCommand;
import net.dryuf.cmdline.command.RootCommandContext;
import org.apache.commons.io.IOUtils;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


/**
 * SFTP client test, synchronous.
 */
public class SftpRunner extends AbstractCommand
{
	public static void main(String[] args) throws Exception
	{
		runMain(args, (args0) -> {
			AppContext appContext = new CommonAppContext(Guice.createInjector(new GuiceBeanFactoryModule()).getInstance(BeanFactory.class));
			return appContext.getBeanFactory().getBean(SftpRunner.class).run(
				new RootCommandContext(appContext).createChild(null, "sftprunner", null),
				Arrays.asList(args0)
			);
		});
	}

	public int execute() throws Exception
	{
		SshClient client = SshClient.setUpDefaultClient();
		client.start();

		try (ClientSession session = initializeSession(client.connect(System.getProperty("user.name"), "localhost", 22)
			.verify(10, TimeUnit.SECONDS).getSession());
		     SftpClient sftp = SftpClientFactory.instance().createSftpClient(session)
		) {
			sftp.readDir(".").forEach((SftpClient.DirEntry de) -> {
				System.out.println(de.getFilename());
			});
			try (InputStream channel = sftp.read(".bashrc")) {
				IOUtils.copy(channel, System.out);
			}
		}
		finally {
			client.stop();
		}
		return 0;
	}

	private ClientSession initializeSession(ClientSession session) throws IOException
	{
		session.auth().verify(10, TimeUnit.SECONDS);
		return session;
	}
}
