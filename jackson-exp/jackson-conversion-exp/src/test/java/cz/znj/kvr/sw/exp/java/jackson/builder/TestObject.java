package cz.znj.kvr.sw.exp.java.jackson.builder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;


@Value
@Builder(builderClassName = "Builder")
@JsonDeserialize(builder = TestObject.Builder.class)
public class TestObject
{
	private int			testId;

	private String			name;

	@JsonPOJOBuilder(withPrefix = "")
	public static final class Builder
	{
	}
}
