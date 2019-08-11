package cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.scanner;

import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.Condition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.ConsumesCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsClassMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.JaxRsMethodMeta;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.ProducesCondition;
import cz.znj.kvr.sw.exp.java.jaxrs.micro.maven.model.QueryParamCondition;
import org.apache.maven.plugin.logging.Log;
import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ReflectionsIndirectJaxRsScanner extends AbstractJaxRsScanner
{
	public ReflectionsIndirectJaxRsScanner(Log log)
	{
		super(log);
	}

	public List<JaxRsClassMeta> scan(ScanConfiguration configuration)
	{
		Runtime runtime = new Runtime();
		runtime.configuration = configuration;
		runtime.packageUrls = getPackageUrls(runtime);
		runtime.classLoader = createClassLoader(runtime);

		Reflections reflections = new Reflections(
				new ConfigurationBuilder()
						.forPackages(configuration.getPackageRoots())
						.addUrls(runtime.packageUrls)
						.addClassLoader(runtime.classLoader)
						.addScanners(/*new SubTypesScanner(false), */ new TypeAnnotationsScanner())
		);

		Map<Class<? extends Annotation>, String> jaxRsMethods = new HashMap<>();
		Reflections jaxRsReflections = new Reflections(
				new ConfigurationBuilder()
						.forPackages("")
						.addUrls(runtime.packageUrls)
						.addClassLoader(runtime.classLoader)
						.addScanners(/*new SubTypesScanner(false), */ new TypeAnnotationsScanner())
		);

		configureClasses(runtime);

		for (Class<?> clazz: jaxRsReflections.getTypesAnnotatedWith(runtime.classHttpMethod)) {
			@SuppressWarnings("unchecked")
			Class<? extends Annotation> annoClazz = (Class<? extends Annotation>) clazz;
			jaxRsMethods.put(annoClazz, getHttpMethodValue(runtime, clazz.getAnnotation(runtime.classHttpMethod)));
		}

		List<JaxRsClassMeta> classMetas = new ArrayList<>();
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(runtime.classPath);
		for (Class<?> clazz: classes) {
			log.debug(String.format("Processing class %s", clazz));
			Object pathAnno = clazz.getAnnotation(runtime.classPath);
			if (pathAnno == null) {
				throw new RuntimeException(String.format("Got class from scan but no Path annotation exist there: %s", clazz.getName()));
			}
			List<Condition> conditionsClass = new ArrayList<>();
			String path = getPathValue(runtime, pathAnno);
			Optional<List<String>> producesClass = Optional.ofNullable(clazz.getAnnotation(runtime.classProduces)).map((anno) -> getProducesValue(runtime, anno)).map(Arrays::asList);
			producesClass.ifPresent(c -> conditionsClass.add(new ProducesCondition(c)));
			Optional<List<String>> consumesClass = Optional.ofNullable(clazz.getAnnotation(runtime.classConsumes)).map((anno) -> getConsumesValue(runtime, anno)).map(Arrays::asList);
			consumesClass.ifPresent(c -> conditionsClass.add(new ConsumesCondition(c)));
			Stream.of(clazz.getAnnotationsByType(runtime.classQueryParam))
					.map((anno) -> getQueryParamValue(runtime, anno))
					.sorted(ASCII_STRING_COMPARATOR)
					.map((value) -> {
						String[] split = value.split("=", 2);
						if (split.length != 2) {
							throw new IllegalArgumentException("Expected QueryParam of form key=value on class "+clazz+ ", got: "+value);
						}
						return new QueryParamCondition(split[0], split[1]);
					})
					.forEach((condition) -> conditionsClass.add(condition));
			JaxRsClassMeta classMeta = new JaxRsClassMeta();
			classMeta.setClassName(clazz.getName());
			classMeta.setPath(path);
			classMeta.setConditions(conditionsClass);
			List<JaxRsMethodMeta> methodMetas = new ArrayList<>();

			Method[] functions = clazz.getMethods();
			for (Method function: functions) {
				JaxRsMethodMeta methodMeta = new JaxRsMethodMeta();
				Object mpathAnno = function.getAnnotation(runtime.classPath);
				TreeSet<String> methods = new TreeSet<>();
				for (Map.Entry<Class<? extends Annotation>, String> entry: jaxRsMethods.entrySet()) {
					if (function.getAnnotation(entry.getKey()) != null) {
						methods.add(entry.getValue());
					}
				}
				Object methodAnno = function.getAnnotation(runtime.classHttpMethod);
				if (methodAnno != null) {
					methods.add(getHttpMethodValue(runtime, methodAnno));
				}
				if (mpathAnno == null && methods.isEmpty()) {
					continue;
				}
				List<Condition> conditions = new ArrayList<>();
				Optional<List<String>> produces = Optional.ofNullable(function.getAnnotation(runtime.classProduces)).map((anno) -> getProducesValue(runtime, anno)).map(Arrays::asList).or(() -> producesClass);
				produces.ifPresent((value) -> conditions.add(new ProducesCondition(value)));
				Optional<List<String>> consumes = Optional.ofNullable(function.getAnnotation(runtime.classConsumes)).map((anno) -> getConsumesValue(runtime, anno)).map(Arrays::asList).or(() -> consumesClass);
				consumes.ifPresent((value) -> conditions.add(new ConsumesCondition(value)));
				Stream.of(function.getAnnotationsByType(runtime.classQueryParam))
						.map((anno) -> getQueryParamValue(runtime, anno))
						.sorted(ASCII_STRING_COMPARATOR)
						.map((value) -> {
							String[] split = value.split("=", 2);
							if (split.length != 2) {
								throw new IllegalArgumentException("Expected QueryParam of form key=value on function "+function+", got: "+value);
							}
							return new QueryParamCondition(split[0], split[1]);
						})
						.forEach((condition) -> conditions.add(condition));
				methodMeta.setPath(mpathAnno == null ? "" : getPathValue(runtime, mpathAnno));
				methodMeta.setFunction(formatMethod(function));
				methodMeta.setMethod(String.join(",", methods.stream().sorted().distinct().collect(Collectors.toList())));
				methodMeta.setConditions(conditions);
				methodMetas.add(methodMeta);
			}
			Collections.sort(methodMetas, METHOD_COMPARATOR);
			classMeta.setMethods(methodMetas);
			classMetas.add(classMeta);
		}
		Collections.sort(classMetas, CLASS_COMPARATOR);
		return classMetas;
	}

	@SuppressWarnings("unchecked")
	protected void configureClasses(Runtime runtime)
	{
		try {
			runtime.classHttpMethod = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.HttpMethod");
			runtime.classPath = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.Path");
			runtime.classProduces = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.Produces");
			runtime.classConsumes = (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.Consumes");
			runtime.classQueryParam= (Class<? extends Annotation>) runtime.classLoader.loadClass("javax.ws.rs.QueryParam");
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
}
