package cz.znj.kvr.sw.exp.java.niofilesystemexp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by rat on 09/10/2015.
 */
public class NioZipFsExp
{
	public static void              main(String[] args)
	{
		System.exit(new NioFsExp().run(args));
	}

	public int                      run(String[] args)
	{
		Path zipLocation = FileSystems.getDefault().getPath("target", "testfile.zip");
		Map<String, Object> env = new HashMap<String, Object>();
		zipLocation.toFile().delete();
		env.put("create", "true");
		URI zipUri = null;
		try {
			zipUri = zipLocation.toUri();
			zipUri = new URI("jar:"+zipUri.getScheme(), zipUri.getPath(), null);
		}
		catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		try (FileSystem zipfs = FileSystems.newFileSystem(zipUri, env)) {
			Files.createDirectory(zipfs.getPath("doc"));
			Files.copy(new ByteArrayInputStream("hello\n".getBytes()), zipfs.getPath("doc", "README"));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return 0;
	}
}
