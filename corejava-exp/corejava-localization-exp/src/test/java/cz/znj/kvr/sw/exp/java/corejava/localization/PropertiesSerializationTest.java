package cz.znj.kvr.sw.exp.java.corejava.localization;

import lombok.extern.java.Log;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;


/**
 *
 */
@Log
public class PropertiesSerializationTest
{
	@Test
	public void testSerialization() throws IOException
	{
		Properties props = new Properties();
		props.put("czech", "Žluťoučký kůn úpěl ďábelské ódy, íá!");
		props.put("german", "Ä/ä, Ö/ö, Ü/ü, ß");
		String outputPrefix = "target/"+PropertiesSerializationTest.class.getName()+".testSerialization";

		try (OutputStream out = new FileOutputStream(outputPrefix+"-stream.properties")) {
			props.store(out, null);
		}
		try (Writer out = new FileWriter(outputPrefix+"-writer.properties", StandardCharsets.UTF_8)) {
			props.store(out, null);
		}

		Set<String> streamLines = new HashSet<>(Files.readAllLines(Path.of(outputPrefix+"-stream.properties")));
		assertThat(streamLines, Matchers.hasItem("german=\\u00C4/\\u00E4, \\u00D6/\\u00F6, \\u00DC/\\u00FC, \\u00DF"));
		assertThat(streamLines, Matchers.hasItem("czech=\\u017Dlu\\u0165ou\\u010Dk\\u00FD k\\u016Fn \\u00FAp\\u011Bl \\u010F\\u00E1belsk\\u00E9 \\u00F3dy, \\u00ED\\u00E1\\!"));

		Set<String> writerLines = new HashSet<>(Files.readAllLines(Path.of(outputPrefix+"-writer.properties")));
		assertThat(writerLines, Matchers.hasItem("german=Ä/ä, Ö/ö, Ü/ü, ß"));
		assertThat(writerLines, Matchers.hasItem("czech=Žluťoučký kůn úpěl ďábelské ódy, íá\\!"));
	}
}
