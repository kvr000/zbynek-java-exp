package cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.generator;

import lombok.extern.java.Log;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.zip.ZipEntry;


/**
 *
 */
@Log
public class RejarArchiver
{
	private String destination;
	private String source;
	private String[] toAddList;
	private int compressionLevel;

	public static void main(String[] args) throws IOException
	{
		RejarArchiver archiver = new RejarArchiver();
		if (args.length < 4) {
			printUsageAndExit();
		}
		if (!args[0].equals("-z")) {
			printUsageAndExit();
		}
		try {
			archiver.compressionLevel = Integer.parseInt(args[1]);
		}
		catch (NumberFormatException ex) {
			throw new RuntimeException("Failed to parse compression level, expected number 0-9, got "+args[1], ex);
		}
		archiver.destination = args[2];
		archiver.source = args[3];
		if (!new File(archiver.source).exists()) {
			throw new RuntimeException("Source file does not exist: "+args[3]);
		}
		archiver.toAddList = Arrays.copyOfRange(args, 4, args.length);

		long startTime = System.nanoTime();
		archiver.execute();
		log.log(Level.INFO, "Finished compression={0} in {1} ns", new Object[]{ archiver.compressionLevel, System.nanoTime()-startTime });
	}

	private void execute() throws IOException
	{
		// Apache Commons has good performance and still offers simple raw copy for original archive
		try (
				ZipFile sourceZip = new ZipFile(source);
				ZipArchiveOutputStream targetZip = new ZipArchiveOutputStream(new File(destination))
		) {
			sourceZip.copyRawEntries(targetZip, e -> true);
			targetZip.setMethod(compressionLevel == 0 ? ZipEntry.STORED : ZipEntry.DEFLATED);
			targetZip.setLevel(compressionLevel);
			for (String toAdd: toAddList) {
				Files.find(Paths.get(toAdd),
						Integer.MAX_VALUE,
						(filePath, fileAttr) -> fileAttr.isRegularFile())
						.sorted()
						.forEach((Path filepath) -> {
									try {
										Path relativePath = Paths.get(toAdd).relativize(filepath);
										ArchiveEntry entry = targetZip.createArchiveEntry(filepath.toFile(), relativePath.toString());
										targetZip.putArchiveEntry(entry);
										Files.copy(filepath, targetZip);
										targetZip.closeArchiveEntry();
									}
									catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
						);
			}
		}

		/*
		// This works but is terribly slow because it reopens archive every time it adds new file:
		Files.copy(Paths.get(source), Paths.get(destination), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		ZipFile zipFile = new ZipFile(destination);
		for (String toAdd: toAddList) {
			Files.find(Paths.get(toAdd),
					Integer.MAX_VALUE,
					(filePath, fileAttr) -> fileAttr.isRegularFile())
					.forEach((Path filepath) -> {
								try {
									Path relativePath = Paths.get(toAdd).relativize(filepath);
									ZipParameters parameters = new ZipParameters();
									parameters.setFileNameInZip(relativePath.toString());
									parameters.setCompressionMethod(compressionLevel == 0 ? CompressionMethod.STORE : CompressionMethod.DEFLATE);
									parameters.setCompressionLevel(CompressionLevel.MAXIMUM);
									zipFile.addFile(filepath.toFile(), parameters);
								}
								catch (IOException e) {
									throw new RuntimeException(e);
								}
							}
					);
		}
		*/

		/*
		// Cannot control compression level
		Files.copy(Paths.get(source), Paths.get(destination), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
		try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:"+Paths.get(destination).toAbsolutePath().toUri()), Collections.singletonMap("compression", compressionLevel))) {
		//try (FileSystem fs = FileSystems.newFileSystem(Paths.get(destination), null)) {
			for (String toAdd: toAddList) {
				Files.find(Paths.get(toAdd),
						Integer.MAX_VALUE,
						(filePath, fileAttr) -> fileAttr.isRegularFile())
						.forEach((Path filepath) -> {
									try {
										Path relativePath = Paths.get(toAdd).relativize(filepath);
										Path fsPath = fs.getPath(relativePath.toString());
										Files.createDirectories(fsPath.getParent());
										Files.copy(filepath, fsPath);
									}
									catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
						);
			}
		}
		*/
	}

	private static void printUsageAndExit()
	{
		System.err.println("Usage: -z compression-level(0-9) destination-jar-file source-jar-file directories-to-add...");
		System.exit(126);
	}
}
