package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner;

import lombok.Data;

import java.io.File;
import java.util.List;


/**
 * Main generator class
 */
@Data
public class ScanConfiguration
{
	protected List<String>		classpath;

	protected String[]		packageRoots;
}
