package cz.znj.kvr.sw.exp.java.jaxrs.micro.service.reader;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ControllerMeta
{
	private String path;

	private String className;

	private List<MethodMeta> methods;
}
