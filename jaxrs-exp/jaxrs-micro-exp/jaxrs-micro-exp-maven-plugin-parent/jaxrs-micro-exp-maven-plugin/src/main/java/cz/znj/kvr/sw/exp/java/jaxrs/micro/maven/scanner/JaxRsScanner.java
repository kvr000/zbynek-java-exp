package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;

import java.util.List;


public interface JaxRsScanner
{
	List<JaxRsClassMeta> scan(ScanConfiguration configuration);
}
