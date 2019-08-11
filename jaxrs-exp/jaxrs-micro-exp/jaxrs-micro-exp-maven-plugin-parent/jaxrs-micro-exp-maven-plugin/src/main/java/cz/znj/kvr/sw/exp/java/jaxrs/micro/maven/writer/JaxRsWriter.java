package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.writer;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.Configuration;

import java.io.IOException;
import java.util.List;


/**
 * Component which actually writes the localization files.
 */
public interface JaxRsWriter
{
	/**
	 * Write the messages into outputDirectory DB.
	 *
	 * @param configuration
	 * 	common configuration
	 * @param jaxRs
	 * 	Jax Rs metadata
	 */
	void writeJaxRs(Configuration configuration, List<JaxRsClassMeta> jaxRs) throws IOException;
}
