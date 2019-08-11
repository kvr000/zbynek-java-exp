package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsMethodMeta;
import lombok.extern.log4j.Log4j2;
import org.apache.maven.plugin.logging.Log;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;


public class ReflectionsDynamicJaxRsScanner extends AbstractJaxRsScanner
{
	public ReflectionsDynamicJaxRsScanner(Log log)
	{
		super(log);
	}

	public List<JaxRsClassMeta> scan(ScanConfiguration configuration)
	{
		throw new UnsupportedOperationException("commented out");

//		List<JaxRsClassMeta> classMetas = new ArrayList<>();
//		URL[] packageUrls = configuration.getClasspath()
//				.stream()
//				.map((item) -> {
//					try {
//						return Paths.get(item).toUri().toURL();
//					}
//					catch (MalformedURLException e) {
//						throw new RuntimeException(e);
//					}
//				})
//				.collect(Collectors.toList())
//				.toArray(URL_EMPTY_ARRAY);
//		ClassLoader classLoader = URLClassLoader.newInstance(packageUrls, Capturer.class.getClassLoader());
//
//		Reflections reflections = new Reflections(
//				new ConfigurationBuilder()
//						.forPackages(configuration.packageRoots)
//						.addUrls(packageUrls)
//						.addClassLoader(classLoader)
//						.addScanners(/*new SubTypesScanner(false), */ new TypeAnnotationsScanner())
//		);
//		/*
//		Class<? extends Annotation> httpMethodClass;
//		try {
//			@SuppressWarnings("unchecked")
//			Class<? extends Annotation> httpMethodClass0 = (Class<? extends Annotation>) classLoader.loadClass("javax.ws.rs.HttpMethod");
//			httpMethodClass = httpMethodClass0;
//		}
//		catch (ClassNotFoundException e) {
//			throw new RuntimeException(e);
//		}
//		*/
//
//		Class<?>[] classes = reflections.getTypesAnnotatedWith(Path.class).toArray(CLASSES_EMPTY_ARRAY);
//		Arrays.sort(classes, Comparator.comparing(Class::getName));
//		for (Class<?> clazz: classes) {
//			log.debug("Processing class {}", clazz);
//			Path pathAnno = clazz.getAnnotation(Path.class);
//			if (pathAnno == null) {
//				throw new RuntimeException(String.format("Got class from scan but no Path annotation exist there: %s", clazz.getName()));
//			}
//			String path = pathAnno.value();
//			JaxRsClassMeta classMeta = new JaxRsClassMeta();
//			classMeta.setClassName(clazz.getName());
//			classMeta.setPath(path);
//			List<JaxRsMethodMeta> methodMetas = new ArrayList<>();
//
//			Method[] functions = clazz.getMethods();
//			Arrays.sort(functions, METHOD_COMPARATOR);
//			for (Method function: functions) {
//				JaxRsMethodMeta methodMeta = new JaxRsMethodMeta();
//				Path mpathAnno = function.getAnnotation(Path.class);
//				TreeSet<String> methods = readMethods(function, null);
//				if (mpathAnno == null && methods.isEmpty()) {
//					continue;
//				}
//				methodMeta.setPath(mpathAnno == null ? "" : mpathAnno.value());
//				methodMeta.setFunction(formatMethod(function));
//				methodMeta.setMethod(String.join(",", methods));
//				methodMetas.add(methodMeta);
//			}
//			classMeta.setMethods(methodMetas);
//			classMetas.add(classMeta);
//		}
//		return classMetas;
//	}

//	private TreeSet<String> readMethods(Method function, Class<? extends Annotation> httpMethodClass)
//	{
//		TreeSet<String> methods = new TreeSet<>();
//		for (Annotation annotation: function.getAnnotations()) {
//			log.error("Processing function {}, annotation: {}", function, annotation);
//			String method = this.<String>checkAnnotation(annotation.getClass(), "javax.ws.rs.HttpMethod", "value");
//			log.error("Found {}, method={}", annotation.getClass(), method);
//			if (method != null)
//				methods.add(method);
//		}
//		String method = checkAnnotation(function, "javax.ws.rs.HttpMethod", "value");
//		if (method != null) {
//			methods.add(method);
//		}
//		return methods;
//	}
//
//
//	private <T> T checkAnnotation(AnnotatedElement element, String className, String methodName)
//	{
//		log.error("Processing element: {}", element);
//		for (Annotation anno: element.getAnnotations()) {
//			log.error("Found annotation: {} {}", anno.annotationType(), anno.annotationType().getCanonicalName());
//			if (anno.getClass().getName().equals(className)) {
//				try {
//					@SuppressWarnings("unchecked")
//					T result = (T) MethodUtils.invokeExactMethod(anno, methodName);
//					return result;
//				}
//				catch (NoSuchMethodException | IllegalAccessException |InvocationTargetException e) {
//					throw new RuntimeException(e);
//				}
//			}
//		}
//		return null;
//	}
//

	}

	@SuppressWarnings("unchecked")
	protected void configureClasses(Runtime runtime)
	{
		try {
			runtime.classHttpMethod = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.HttpMethod");
			runtime.classPath = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.Path");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
