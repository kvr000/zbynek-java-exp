package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner.JaxRsScanner;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner.ScanConfiguration;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.writer.JaxRsWriter;
import lombok.Setter;

import java.io.IOException;
import java.util.List;


/**
 * Main capturer.
 */
public class Capturer
{
	public Capturer(JaxRsScanner jaxRsScanner)
	{
		this.jaxRsScanner = jaxRsScanner;
	}

	public void			execute() throws IOException
	{
		List<JaxRsClassMeta> classes = parseSources();
		writeResults(classes);
	}

	private List<JaxRsClassMeta> parseSources()
	{
		ScanConfiguration scanConfiguration = new ScanConfiguration();
		scanConfiguration.setPackageRoots(configuration.getPackageRoots());
		scanConfiguration.setClasspath(configuration.getClasspath());
		return jaxRsScanner.scan(scanConfiguration);
	}

	private void writeResults(List<JaxRsClassMeta> classes) throws IOException
	{
		for (JaxRsWriter jaxRsWriter: jaxRsWriters) {
			jaxRsWriter.writeJaxRs(configuration, classes);
		}
	}

	protected JaxRsScanner		jaxRsScanner;

	@Setter
	protected Configuration		configuration;

	@Setter
	protected Iterable<JaxRsWriter> jaxRsWriters;
}
