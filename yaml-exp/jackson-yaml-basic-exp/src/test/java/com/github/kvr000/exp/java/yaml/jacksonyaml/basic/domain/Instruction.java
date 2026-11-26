package com.github.kvr000.exp.java.yaml.jacksonyaml.basic.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Value;


@Value
@lombok.Builder(builderClassName = "Builder")
@JsonDeserialize(builder = Instruction.Builder.class)
public class Instruction
{
	String code;

	String value;

	@JsonPOJOBuilder(withPrefix = "")
	public static class Builder
	{
	}
}
