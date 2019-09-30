package cz.znj.kvr.sw.exp.java.corejava.lang.benchmark.classload.generator;

import lombok.extern.java.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;


/**
 *
 */
@Log
public class ClassLoadingBenchmarkGenerator
{
	public static final int WARMUP_CLASS_COUNT = 1000;
	public static final int BENCHMARK_CLASS_COUNT = 1000;

	public static final String TEST_CLASS_PACKAGE = ClassLoadingBenchmarkGenerator.class.getPackage().getName()+".data";

	public static final String WARMUP_INTERFACE_PREFIX = TEST_CLASS_PACKAGE+".WarmupInterface_";
	public static final String BENCHMARK_INTERFACE_PREFIX = TEST_CLASS_PACKAGE+".BenchmarkInterface_";

	public static final String WARMUP_CLASS_INHERITED_PREFIX = TEST_CLASS_PACKAGE+".WarmupClassInherited_";
	public static final String BENCHMARK_CLASS_INHERITED_PREFIX = TEST_CLASS_PACKAGE+".BenchmarkClassInherited_";

	public static final String WARMUP_CLASS_ALONE_PREFIX = TEST_CLASS_PACKAGE+".WarmupClassAlone_";
	public static final String BENCHMARK_CLASS_ALONE_PREFIX = TEST_CLASS_PACKAGE+".BenchmarkClassAlone_";

	private static final String GENERATED_PREFIX = "target/generated/test/java/";

	private static String INTERFACE_TEMPLATE = ""+
			"package "+TEST_CLASS_PACKAGE+";\n"+
			"public interface {generatedLifecycle}Interface_{generatedItemId}\n"+
			"{\n"+
			"\t"+"public String greet(String name);\n"+
			"\t"+"public String goodbye(String name);\n"+
			"}\n";

	private static String CLASS_INHERITED_TEMPLATE = ""+
			"package "+TEST_CLASS_PACKAGE+";\n"+
			"public class {generatedLifecycle}ClassInherited_{generatedItemId} implements {generatedLifecycle}Interface_{generatedItemId}\n"+
			"{\n"+
			"\t"+"public String greet(String name) { return \"Hello, \"+name; }\n"+
			"\t"+"public String goodbye(String name) { return \"Bye, \"+name; }\n"+
			"}\n";

	private static String CLASS_ALONE_TEMPLATE = ""+
			"package "+TEST_CLASS_PACKAGE+";\n"+
			"public class {generatedLifecycle}ClassAlone_{generatedItemId}\n"+
			"{\n"+
			"\t"+"public String greet(String name) { return \"Hello, \"+name; }\n"+
			"\t"+"public String goodbye(String name) { return \"Bye, \"+name; }\n"+
			"}\n";

	public static void main(String[] args) throws IOException
	{
		for (int i = 0; i < WARMUP_CLASS_COUNT; ++i) {
			generateFile(WARMUP_INTERFACE_PREFIX, "Warmup", i, INTERFACE_TEMPLATE);
			generateFile(WARMUP_CLASS_INHERITED_PREFIX, "Warmup", i, CLASS_INHERITED_TEMPLATE);
			generateFile(WARMUP_CLASS_ALONE_PREFIX, "Warmup", i, CLASS_ALONE_TEMPLATE);
		}
		for (int i = 0; i < BENCHMARK_CLASS_COUNT; ++i) {
			generateFile(BENCHMARK_INTERFACE_PREFIX, "Benchmark", i, INTERFACE_TEMPLATE);
			generateFile(BENCHMARK_CLASS_INHERITED_PREFIX, "Benchmark", i, CLASS_INHERITED_TEMPLATE);
			generateFile(BENCHMARK_CLASS_ALONE_PREFIX, "Benchmark", i, CLASS_ALONE_TEMPLATE);
		}
		log.log(Level.INFO, String.format("Classes generated in %s", GENERATED_PREFIX));
	}

	private static void generateFile(String prefix, String lifecycle, int id, String template) throws IOException
	{
		File filename = new File(GENERATED_PREFIX+(prefix+id).replaceAll("\\.", "/")+".java");
		if (!filename.exists()) {
			filename.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(filename)) {
				writer.write(template.replaceAll("\\{generatedItemId}", String.valueOf(id)).replaceAll("\\{generatedLifecycle}", lifecycle));
			}
		}
	}
}
