package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner.ReflectionsIndirectJaxRsScanner;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.writer.XmlJaxRsWriter;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;


/**
 * CSV to localization messages generator.
 */
@Mojo(name = "capture-jaxrs", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class CapturerMojo extends AbstractMojo
{
	@SuppressWarnings("unchecked")
	@Override
	public void			execute() throws MojoExecutionException, MojoFailureException
	{
		Capturer capturer = new Capturer(new ReflectionsIndirectJaxRsScanner(getLog()));
		Configuration configuration = new Configuration();
		try {
			configuration.setClasspath(useTestClasspath ? project.getTestClasspathElements() : project.getRuntimeClasspathElements());
		}
		catch (DependencyResolutionRequiredException e) {
			throw new RuntimeException(e);
		}
		configuration.setPackageRoots(packageRoots);
		configuration.setOutput(output);
		capturer.setConfiguration(configuration);
		capturer.setJaxRsWriters(Collections.singleton(new XmlJaxRsWriter()));
		try {
			capturer.execute();
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	/**
	 * List of languages to generate.
	 */
	@Parameter(required = false)
	protected boolean		useTestClasspath = false;

	@Parameter(required = true)
	protected String[]		packageRoots;

	@Parameter(required = true)
	protected File			output;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject		project;
}
