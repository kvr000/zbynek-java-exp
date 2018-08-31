package cz.znj.kvr.sw.exp.java.jackson.compatibility.annotationcompatibility.ignore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;



/**
 * Test object for JsonIgnore testing.
 */
@Data
public class TestObject2
{
	@JsonIgnore
	public int getAge()
	{
		return 1;
	}
}
