package com.github.kvr000.exp.java.quarkus.securefield.container;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.inject.Singleton;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.util.Set;


/**
 * Does not work now.
 */
public class BeanScanner implements Extension
{
	// Invoked by Quarkus/CDI during container initialization
	public void afterBeanDiscovery(@Observes AfterBeanDiscovery abd, BeanManager bm) {
		if (true) return;
		System.out.println("Starting classpath scan for external @Singleton classes...");

		// 1. Configure the scanner to look at your base package
		// (You can also use ClasspathHelper.forClassLoader() to scan everything)
		Reflections reflections = new Reflections(new ConfigurationBuilder()
			.setUrls(ClasspathHelper.forPackage("com.github.kvr000.exp.java")));

		// 2. Locate all classes annotated with @Singleton
		Set<Class<?>> singletonClasses = reflections.getTypesAnnotatedWith(Singleton.class);

		// 3. Register every found class dynamically as an official CDI Bean
		for (Class<?> clazz : singletonClasses) {
			abd.addBean()
				.read(bm.createAnnotatedType(clazz))
				.beanClass(clazz)
				.scope(Singleton.class);

			System.out.println("Successfully registered dynamic bean: " + clazz.getName());
		}
	}
}
