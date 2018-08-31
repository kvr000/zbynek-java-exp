package cz.znj.kvr.sw.exp.java.jackson.compatibility.annotationcompatibility.ignore;

import lombok.Data;
import org.codehaus.jackson.annotate.JsonIgnore;


/**
 * Test object for JsonIgnore testing.
 */
@Data
public class TestObject1
{
	@JsonIgnore
	public int getAge()
	{
		return 1;
	}
}
