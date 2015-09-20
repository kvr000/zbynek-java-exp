package cz.znj.kvr.sw.exp.java.camel.camone;


import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Main
{
	public static void		main(String... args)
	{
		System.exit(new Main().run(args));
	}

	public int			run(String... args)
	{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
		System.out.println("Hello world");

		synchronized (daemonLock) {
			while (!daemonShutdown) {
				try {
					daemonLock.wait();
				}
				catch (InterruptedException ex) {
				}
			}
		}
		return 0;
	}

	protected Object		daemonLock = new Object();
	protected boolean		daemonShutdown = false;
}
