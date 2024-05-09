package com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Data
public class SimplifiedDocument
{
	private String			documentId;

	private List<Map<String, String>> instructions = new ArrayList<>();
}
