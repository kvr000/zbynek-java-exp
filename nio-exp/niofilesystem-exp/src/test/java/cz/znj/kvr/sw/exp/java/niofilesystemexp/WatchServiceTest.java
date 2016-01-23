package cz.znj.kvr.sw.exp.java.niofilesystemexp;


import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * NIO WatchService experiments.
 */
public class WatchServiceTest
{
	Logger log = Logger.getLogger(WatchServiceTest.class.getName());

	@Test(timeout = 30000L)
	public void testWatchService() throws IOException, InterruptedException
	{
		Path tmp = Files.createTempDirectory("watch-exp-");
		log.log(Level.SEVERE, "Using directory: "+tmp);
		try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
			tmp.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
			Assert.assertEquals(null, watcher.poll());
			Path file = tmp.resolve("test-file");
			log.log(Level.SEVERE, "Creating file");
			try (OutputStream out = Files.newOutputStream(file)) {
				{
					WatchKey event = watcher.take();
					log.log(Level.SEVERE, "Got events");
					Assert.assertNotEquals(null, event);
					List<WatchEvent<?>> events = event.pollEvents();
					Assert.assertEquals(1, events.size());
					Assert.assertEquals(StandardWatchEventKinds.ENTRY_CREATE, events.get(0).kind());
					Assert.assertEquals(Paths.get("test-file"), events.get(0).context());
					event.reset();
				}
				log.log(Level.SEVERE, "Writing to file");
				out.write("second\n".getBytes(StandardCharsets.UTF_8));
				out.flush();
				{
					WatchKey event = watcher.take();
					log.log(Level.SEVERE, "Got events");
					Assert.assertNotEquals(null, event);
					List<WatchEvent<?>> events = event.pollEvents();
					Assert.assertEquals(1, events.size());
					Assert.assertEquals(StandardWatchEventKinds.ENTRY_MODIFY, events.get(0).kind());
					Assert.assertEquals(Paths.get("test-file"), events.get(0).context());
				}
			}
			finally {
				Files.deleteIfExists(file);
			}
		}
		finally {
			Files.delete(tmp);
		}
	}
}
