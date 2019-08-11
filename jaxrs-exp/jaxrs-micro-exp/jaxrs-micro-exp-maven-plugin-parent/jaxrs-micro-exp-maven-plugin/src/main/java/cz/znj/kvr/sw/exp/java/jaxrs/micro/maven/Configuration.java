package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.List;


/**
 * Main generator class
 */
@Data
public class Configuration
{
	protected List<String>		classpath;

	protected String[]		packageRoots;

	protected File			output;
}
