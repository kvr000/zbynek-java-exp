package cz.znj.kvr.sw.exp.java.jar.hugejar.generator;

import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;


/**
 * CSV to localization messages generator.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE)
public class GeneratorMojo extends AbstractMojo
{
	@SneakyThrows
	@SuppressWarnings("unchecked")
	@Override
	public void			execute() throws MojoExecutionException, MojoFailureException
	{
		try (
			ZipFile input = new ZipFile(this.input);
			ZipArchiveOutputStream output = new ZipArchiveOutputStream(this.output)
		) {
			if (!append)
				addDummy(output);
			input.copyRawEntries(output, (a) -> true);
			if (append)
				addDummy(output);
		}
	}

	private void addDummy(ZipArchiveOutputStream output) throws IOException
	{
		byte[] content = new byte[entrySize];
		long crc = crc(content);
		for (int i = 0; i < entryCount; ++i) {
			ZipArchiveEntry entry = new ZipArchiveEntry("dummy/dummy-" + i);
			entry.setMethod(ZipEntry.STORED);
			entry.setSize(entrySize);
			entry.setCrc(crc);
			output.putArchiveEntry(entry);
			output.write(new byte[entrySize]);
			output.closeArchiveEntry();
		}

	}

	private static long crc(byte[] buf)
	{
		CRC32 crc = new CRC32();
		crc.update(buf);
		return crc.getValue();
	}

	/** Input jar file. */
	@Parameter(required = true)
	protected File			input;

	/** Output jar file. */
	@Parameter(required = true)
	protected File			output;

	/** Size of generated file. */
	@Parameter(required = false)
	protected int			entrySize = 0;

	/** Number of generated entries. */
	@Parameter(required = true)
	protected int			entryCount;

	/** Indicator whether to append or prepend. */
	@Parameter(required = false)
	protected boolean		append = true;
}
