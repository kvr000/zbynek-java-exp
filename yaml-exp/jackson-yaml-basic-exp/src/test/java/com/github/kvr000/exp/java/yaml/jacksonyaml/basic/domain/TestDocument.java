package com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class TestDocument
{
	private String			documentId;

	private List<Instruction> instructions = new ArrayList<>();
}
