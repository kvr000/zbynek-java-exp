package com.github.kvr000.exp.java.yaml.jacksonyaml.basic;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain.Instruction;
import com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain.SimplifiedDocument;
import com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain.TestDocument;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;


public class YamlReaderTest
{
	YAMLMapper yamlMapper = new YAMLMapper();

	@Test
	public void mapDocument_complex_returnCorrect() throws IOException
	{
		try (InputStream stream = YamlReaderTest.class.getResourceAsStream("instructions.yaml")) {
			TestDocument document = yamlMapper.readValue(stream, TestDocument.class);

			assertEquals(document.getDocumentId(), "hello");
			assertEquals(document.getInstructions().size(), 4);
			assertEquals(document.getInstructions(), List.of(
				Instruction.builder().code("include").value("this").build(),
				Instruction.builder().code("exclude").value("that").build(),
				Instruction.builder().code("include").value("more").build(),
				Instruction.builder().code("exclude").value("none").build()
			));
		}
	}

	@Test
	public void mapDocument_simplified_returnCorrect() throws IOException
	{
		try (InputStream stream = YamlReaderTest.class.getResourceAsStream("simplified.yaml")) {
			SimplifiedDocument document = yamlMapper.readValue(stream, SimplifiedDocument.class);

			assertEquals(document.getDocumentId(), "hello");
			assertEquals(document.getInstructions().size(), 4);
			assertEquals(document.getInstructions(), List.of(
				Map.of("include", "this"),
				Map.of("exclude", "that"),
				Map.of("include", "more"),
				Map.of("exclude", "none")
			));
		}
	}
}
