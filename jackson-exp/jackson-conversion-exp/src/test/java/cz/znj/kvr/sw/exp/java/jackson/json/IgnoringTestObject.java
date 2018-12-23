package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class IgnoringTestObject extends TestObject
{
}
