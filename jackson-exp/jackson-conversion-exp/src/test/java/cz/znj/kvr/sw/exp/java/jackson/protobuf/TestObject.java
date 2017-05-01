package cz.znj.kvr.sw.exp.java.jackson.protobuf;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class TestObject
{
	private int			testId;

	private String			name;
}
