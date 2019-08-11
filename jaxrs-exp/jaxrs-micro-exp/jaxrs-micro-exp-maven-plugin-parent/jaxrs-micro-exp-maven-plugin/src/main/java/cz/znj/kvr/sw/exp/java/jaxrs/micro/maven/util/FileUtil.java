package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

public class FileUtil
{
	public static void		updateFile(File file, byte[] content) throws IOException
	{
		if (file.exists() && Arrays.equals(Files.readAllBytes(file.toPath()), content))
			return;
		file.getParentFile().mkdirs();
		Files.write(file.toPath(), content);
	}
}
