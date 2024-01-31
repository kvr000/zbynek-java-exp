package com.github.kvr000.exp.java.spatial.restjpa.jarloader;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;


/**
 * The OneJarLoader implements ClassLoader which extracts the nested jar files and then allows loading classes and
 * resources from them.
 *
 * <p>
 * Either top level directory entries are scanned as dependencies or jar top level entries, including nested jars.
 *
 * <p>
 * The ClassLoader supports native library with pattern os.name/os.arch/the-lib.ext and the-lib.ext
 *
 * <p>
 * This code was written with Apache license to allow repackaging it together with commercial software.
 *
 * <p>
 * Typical usage is as follows:
 *
 <pre>
 public class JarMyApplication
 {
	 public static void main(String[] args)
         {
	         OneJarLoader cl = new OneJarLoader();
                 cl.invokeMain(JarMyApplication.class.getName() + ".MyApplication", args);
	 }
 }
 </pre>
  *
  * <p>
  * VM parameters to configure logging:
  * <code>-DOneJarLoader.{Jar,Class,Native,Resource}.level={Level}</code>
  * Supported values are OFF, ERROR, WARN, INFO, DEBUG (default is ERROR)
 */
public class OneJarLoader extends ClassLoader
{
	/** VM parameter prefix to configure logging.  Full is OneJarLoader.CATEGORY.level=LEVEL */
	public static final String SYSPROP_LOGGER_PREFIX = "OneJarLoader.";

	private final Logger loggerJar = initLogger("Jar");
	private final Logger loggerClass = initLogger("Class");
	private final Logger loggerNative = initLogger("Native");
	private final Logger loggerResource = initLogger("Resource");

	private final List<JarFileInfo> jarFiles;
	private final Map<File, Closeable> filesDeleteOnExit = new LinkedHashMap<>();

	/**
	 * Constructor with system class loader as a parent class loader.
	 */
	public OneJarLoader()
	{
		this(ClassLoader.getSystemClassLoader());
	}

	/**
	 * Constructor.
	 *
	 * @param parent
	 *      class loader parent.
	 */
	public OneJarLoader(ClassLoader parent)
	{
		super(parent);

		final ProtectionDomain protectionDomain = getClass().getProtectionDomain();
		final CodeSource codeSource = protectionDomain.getCodeSource();
		final URL topJarUrl = codeSource.getLocation();
		final String protocol = topJarUrl.getProtocol();

		if ("file".equals(protocol)) {
			String mainJarUrl = URLDecoder.decode(topJarUrl.getFile(), StandardCharsets.UTF_8);
			File fileJar = new File(mainJarUrl);

			List<File> files;
			if (fileJar.isDirectory()) {
				// Launched from directory:
				loggerJar.info("Launched from exploded directory: directory=%s", mainJarUrl);
				try {
					files = Files.walk(fileJar.toPath(), 1)
						.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".jar"))
						.map(p -> p.toFile())
						.sorted()
						.collect(Collectors.toList());
				}
				catch (IOException e) {
					throw new OneJarLoaderException("Failed to list directory: directory=" + mainJarUrl + " : " + e.getMessage(), e);
				}
				jarFiles = files.stream()
					.map(file -> loadFileJar(null, file))
					.collect(Collectors.toList())
					.stream()
					.map(CompletableFuture::join)
					.flatMap(List::stream)
					.collect(Collectors.toList());
			}
			else {
				// Launched from jar:
				loggerJar.info("Launched from single jar: file=%s", mainJarUrl);
				JarFile jarFile;
				try {
					jarFile = new JarFile(fileJar);
				}
				catch (IOException ex) {
					throw new OneJarLoaderException("Failed to open top JarFile: " + fileJar, ex);
				}
				JarFileInfo jarFileInfo = createJarFileInfoFromParent(null, fileJar.getAbsolutePath(), topJarUrl, jarFile, null);
				jarFiles = traverseJarFile(jarFileInfo).join();
			}
		}
		else {
			throw new OneJarLoaderException("Unsupported protocol for jar: protocol=" + protocol);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

		logShadows();

		if (loggerJar.isEnabled(Logger.Level.INFO)) {
			loggerJar.info("Loaded jars: %s", jarFiles.stream().map(jfi -> jfi.fullPath).collect(Collectors.joining(" ")));
		}
	}

	/**
	 * Invokes main method on a class loaded with this classloader.
	 *
	 * @param className
	 *      class name to execute
	 * @param args
	 *      program parameters
	 *
	 * @throws Exception
	 *      when an error occurs.
	 */
	public void invokeMain(String className, String[] args) throws Exception {
		Thread.getAllStackTraces().keySet().forEach(t -> {
			try {
				t.setContextClassLoader(this);
			}
			catch (SecurityException ex) {
				// ignore, likely internal thread
			}
		});

		Class<?> clazz = loadClass(className);
		loggerClass.info("Executing main: classLoader=%s class=%s", clazz.getClassLoader(), className);
		Method method = clazz.getMethod("main", String[].class);

		int modifiers = method.getModifiers();

		if (!Modifier.isPublic(modifiers) || !Modifier.isStatic(modifiers) || method.getReturnType() != void.class) {
			throw new NoSuchMethodException("The main() method in class is not public static void: " + method);
		}

		method.invoke(null, (Object) args);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException
	{
		List<URL> urls = findJarEntries(name)
			.stream()
			.map(inf -> inf.getURL())
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		if (!urls.isEmpty()) {
			return Collections.enumeration(urls);
		}
		return super.findResources(name);
	}

	@Override
	protected URL findResource(String name)
	{
		JarEntryInfo inf = findJarEntry(name);
		if (inf != null) {
			URL url = inf.getURL();
			loggerResource.debug("Found resource: %s", url);
			return url;
		}
		return super.findResource(name);
	}

	@Override
	protected String findLibrary(String name)
	{
		JarEntryInfo inf = findJarNativeEntry(name);
		if (inf != null) {
			try {
				File file = copyToTempFile(inf).toFile();
				loggerNative.debug("Loading native library: name=%s file=%s", inf.jarEntry, file);
				filesDeleteOnExit.put(file, () -> {});
				return file.getAbsolutePath();
			} catch (IOException e) {
				throw new OneJarLoaderException(String.format("Failure to load native library %s: %s", name, e), e);
			}
		}
		return super.findLibrary(name);
	}

	private Logger initLogger(String sub)
	{
		Logger.Level logLevel = Optional.ofNullable(System.getProperty(SYSPROP_LOGGER_PREFIX + sub + ".level"))
			.map(Logger.Level::valueOf)
			.orElse(Logger.Level.ERROR);
		return new Logger("OneJarLoader." + sub, logLevel, System.err);
	}

	/**
	 * Creates temporary file from jar entry.
	 *
	 * @param inf
	 *      JAR entry information.
	 *
	 * @return
	 *      temporary file object presenting JAR entry.
	 *
	 * @throws OneJarLoaderException
	 */
	private Path copyToTempFile(JarEntryInfo inf) throws IOException
	{
		String name = inf.getBaseName();
		int ext = name.lastIndexOf('.');
		Path path = Files.createTempFile(name, ext >= 0 ? name.substring(ext) : "");
		try (InputStream input = inf.getInputStream(); OutputStream output = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
			byte[] buffer = new byte[256*1024];
			int read;
			while ((read = input.read(buffer)) > 0) {
				output.write(buffer, 0, read);
			}
		}
		return path;
	}

	private CompletableFuture<List<JarFileInfo>> loadFileJar(JarFileInfo parent, File file)
	{
		return CompletableFuture.supplyAsync(sneakySupplier(() -> {
				JarFile jarFile = new JarFile(file);
				JarFileInfo jarFileInfo = createJarFileInfoFromParent(parent, file.getAbsolutePath(), file.toURI().toURL(), jarFile, null);
				return jarFileInfo;
			}))
			.thenCompose((jarFileInfo) -> traverseJarFile(jarFileInfo)
				.thenApply((l) -> {
					l.add(0, jarFileInfo);
					return l;
				})
			);
	}

	@SneakyThrows
	private CompletableFuture<List<JarFileInfo>> loadNestedJar(JarFileInfo parent, JarEntryInfo inf)
	{
		return CompletableFuture.supplyAsync(sneakySupplier(() -> {
				File file = copyToTempFile(inf).toFile();
				loggerJar.info("Loading inner JAR from temp file: jar=%s temp=%s", inf.jarEntry, file);
				URL url = file.toURI().toURL();
				JarFile jarFile = new JarFile(file);
				return createJarFileInfoFromParent(parent, file.getAbsolutePath(), url, jarFile, file);
			}))
			.thenCompose((jarFileInfo) -> traverseJarFile(jarFileInfo)
				.thenApply((l) -> {
					l.add(0, jarFileInfo);
					return l;
				})
			);
	}

	private CompletableFuture<List<JarFileInfo>> traverseJarFile(JarFileInfo jarFileInfo)
	{
		List<CompletableFuture<List<JarFileInfo>>> children = new ArrayList<>();
		Enumeration<JarEntry> en = jarFileInfo.jarFile.entries();
		final String EXT_JAR = ".jar";
		while (en.hasMoreElements()) {
			JarEntry entry = en.nextElement();
			if (entry.isDirectory()) {
				continue;
			}
			if (entry.getName().endsWith(EXT_JAR)) {
				loggerJar.info("Found nested jar file: %s", entry.getName());
				JarEntryInfo inf = new JarEntryInfo(jarFileInfo, entry);
				children.add(loadNestedJar(jarFileInfo, inf));
			}
		}
		return CompletableFuture.allOf(children.toArray(new CompletableFuture[children.size()]))
			.thenApply((v) -> children.stream()
				.flatMap(future ->future.join().stream())
				.collect(Collectors.toList())
			);
	}

	private JarFileInfo createJarFileInfoFromParent(JarFileInfo parent, String entryPath, URL url, JarFile jarFile, File file)
	{
		ProtectionDomain pdParent = parent != null ? parent.protectionDomain : getClass().getProtectionDomain();
		CodeSource csParent = pdParent.getCodeSource();
		Certificate[] certParent = csParent.getCertificates();
		CodeSource csChild = (certParent == null ? new CodeSource(url, csParent.getCodeSigners())
			: new CodeSource(url, certParent));
		ProtectionDomain pdChild = new ProtectionDomain(csChild, pdParent.getPermissions(),
			pdParent.getClassLoader(), pdParent.getPrincipals());
		return new JarFileInfo(jarFile, parent, entryPath, pdChild, file);
	}

	private JarEntryInfo findJarEntry(String name)
	{
		return jarFiles.stream()
			.map(jarFile ->
				Optional.ofNullable(jarFile.jarFile.getJarEntry(name))
					.map(entry -> new JarEntryInfo(jarFile, entry))
					.orElse(null)
			)
			.filter(Objects::nonNull)
			.findFirst()
			.orElse(null);
	}

	private List<JarEntryInfo> findJarEntries(String name)
	{
		return jarFiles.stream()
			.map(jarFile ->
				Optional.ofNullable(jarFile.jarFile.getJarEntry(name))
					.map(entry -> new JarEntryInfo(jarFile, entry))
					.orElse(null)
			)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
	}

	/**
	 * Finds native library entry.
	 *
	 * @param library
	 *      library name without operating system specific suffix.
	 *
	 * @return
	 *      native library entry in path of os/cpu/native.extension, for example linux/aarch64/libmytool.so
	 */
	private JarEntryInfo findJarNativeEntry(String library)
	{
		String sysName = System.mapLibraryName(library);
		String fullPath = System.getProperty("os.name") + "/" + System.getProperty("os.arch") + "/" + sysName;

		JarEntryInfo entry = findJarEntry(fullPath);
		if (entry == null) {
			entry = findJarEntry(sysName);
		}
		if (entry == null) {
			return null;
		}
		loggerNative.debug("Loading native library: library=%s jar=%s full=%s",
			library, entry.jarFileInfo.fullPath, entry.jarEntry.getName());
		return entry;
	}

	/**
	 * Loads class from the jars.
	 *
	 * @param className
	 *      class to load.
	 *
	 * @return
	 *      loaded class or null if not found.
	 *
	 * @throws OneJarLoaderException
	 *      if load fails
	 */
	private Class<?> findJarClass(String className) throws IOException
	{
		Class<?> clazz = null;
		String path = className.replace('.', '/') + ".class";
		JarEntryInfo entry = findJarEntry(path);
		if (entry != null) {
			byte[] content = entry.getContent();
			definePackage(className, entry);
			try {
				clazz = defineClass(className, content, 0, content.length, entry.jarFileInfo.protectionDomain);
				loggerClass.debug("Loaded class: name=%s loaded=%s jar=%s", className, getClass().getName(), entry.jarFileInfo.fullPath);
			}
			catch (ClassFormatError e) {
				throw new OneJarLoaderException(null, e);
			}
		}
		return clazz;
	}

	private void logShadows()
	{
		if (!loggerJar.isEnabled(Logger.Level.WARN)) {
			return;
		}
		Set<String> ignore = new HashSet<>();
		ignore.add("module-info.class");
		ignore.add("license.txt");
		ignore.add("notice.txt");
		Map<String, JarFileInfo> present = new HashMap<>();
		for (JarFileInfo jarFileInfo : jarFiles) {
			JarFile jarFile = jarFileInfo.jarFile;
			jarFile.stream()
				.filter(entry -> !entry.isDirectory())
				.map(JarEntry::getName)
				.filter(name -> !name.startsWith("META-INF/"))
				.filter(name -> !ignore.contains(name))
				.forEach(name -> present.compute(name, (key, old) -> {
					if (old != null) {
						loggerJar.warn("Entry shadowed: entry=%s hidden=%s main=%s", name, jarFileInfo.fullPath, old.fullPath);
						return old;
					}
					else {
						return jarFileInfo;
					}
				}));
		}
	}

	/**
	 * Clean up temporary files.
	 */
	private void shutdown()
	{
		loggerJar.info("Shutting down");
		for (ListIterator<JarFileInfo> it = jarFiles.listIterator(jarFiles.size()); it.hasPrevious(); ) {
			JarFileInfo jarFileInfo = it.previous();
			if (false) {
				// do not close the jar files, the hook executes too early and may cause mess
				try {
					jarFileInfo.jarFile.close();
				}
				catch (IOException e) {
					// Ignore and attempt to delete later.
				}
			}
			File file = jarFileInfo.fileDeleteOnExit;
			if (file != null && !file.delete()) {
				filesDeleteOnExit.put(file, jarFileInfo.jarFile);
			}
		}
		if (!filesDeleteOnExit.isEmpty()) {
			for (Iterator<Map.Entry<File, Closeable>> it = filesDeleteOnExit.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<File, Closeable> entry = it.next();
				try {
					entry.getValue().close();
				}
				catch (Exception e) {
					// ignore
				}
				if (entry.getKey().delete()) {
					it.remove();
				}
			}
			// Best effort running gc twice to collect any unclosed handles or memory maps:
			System.gc();
			System.gc();
			for (Iterator<Map.Entry<File, Closeable>> it = filesDeleteOnExit.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<File, Closeable> entry = it.next();
				if (entry.getKey().delete()) {
					it.remove();
				}
				else {
					entry.getKey().deleteOnExit();
				}
			}
		}
		loggerJar.info("Completed cleanup");
	}

	@Override
	protected synchronized Class<?> findClass(String clazzName) throws ClassNotFoundException
	{
		loggerClass.debug("Loading class: class=%s", clazzName);

		Class<?> c;
		// Try to locate in our jars:
		try {
			c = findJarClass(clazzName);
			if (c != null) {
				loggerClass.info("Loaded class: requested=%s loaded=%s", clazzName, c);
				return c;
			}
		}
		catch (IOException ex) {
			throw new OneJarLoaderException(String.format("Error loading class: class=%s loaders=%s : %s",
				clazzName, getClass().getName(), ex.getCause()), ex);
		}
		// Delegate to parent ClassLoader:
		ClassLoader cl = getParent();
		c = cl.loadClass(clazzName);
		return c;
	}

	/**
	 * Creates a package for loaded class.
	 *
	 * @param className
	 *      class being loaded
	 */
	private void definePackage(String className, JarEntryInfo entry) throws IllegalArgumentException {
		int lastDot = className.lastIndexOf('.');
		String packageName = lastDot > 0 ? className.substring(0, lastDot) : "";
		try {
			if (getPackage(packageName) == null) {
				JarFileInfo jfi = entry.jarFileInfo;
				definePackage(
					packageName,
					jfi.getSpecificationTitle(), jfi.getSpecificationVersion(),
					jfi.getSpecificationVendor(), jfi.getImplementationTitle(),
					jfi.getImplementationVersion(), jfi.getImplementationVendor(),
					jfi.getSealURL()
				);
			}
		}
		catch (Throwable ex) {
			loggerClass.error("Failed to create package: jar=%s package=%s class=%s : %s", entry.jarFileInfo.fullPath, packageName, className, ex);
		}
	}

	@SneakyThrows
	private <X extends Throwable> RuntimeException sneakyThrow(X ex)
	{
		throw ex;
	}


	private <R> Supplier<R> sneakySupplier(Callable<R> callable)
	{
		return () -> {
			try {
				return callable.call();
			}
			catch (Exception ex) {
				throw sneakyThrow(ex);
			}
		};
	}

	/**
	 * JAR file details.
	 */
	private static class JarFileInfo
	{
		final JarFile jarFile;

		final String fsPath;

		@Getter
		final String fullPath; // full path to file, such as: "parentJar!subJar!moreJar"

		final File fileDeleteOnExit;

		final Manifest manifest;

		final ProtectionDomain protectionDomain;

		final String rootPath; // full path with protocol, ending with "!/": "jar:file:the-file!/"

		final URL rootUrl;

		/**
		 * @param jarFile
		 *      the archive
		 * @param jarFileParent
		 *      parent path
		 * @param protectionDomain
		 *      protection domain
		 * @param deleteOnExit
		 *      file to delete at exit
		 * @throws OneJarLoaderException
		 */
		JarFileInfo(
			JarFile jarFile,
			JarFileInfo jarFileParent,
			String entryPath,
			ProtectionDomain protectionDomain,
			File deleteOnExit
		) {
			this.jarFile = jarFile;
			this.fsPath = jarFile.getName();
			this.fullPath = (jarFileParent != null ? jarFileParent.fullPath + "!" : "") + entryPath;
			this.protectionDomain = protectionDomain;
			this.fileDeleteOnExit = deleteOnExit;
			Manifest manifest = null;
			try {
				manifest = jarFile.getManifest();
			}
			catch (IOException e) {
			}
			if (manifest == null) {
				manifest = new Manifest();
			}
			this.manifest = manifest;
			this.rootPath = "jar:file:" + this.fullPath + "!/";

			try {
				this.rootUrl = new URL(
					null,
					this.rootPath,
					new URLStreamHandler()
					{
						@Override
						protected URLConnection openConnection(URL url) throws IOException
						{
							String str = url.toExternalForm();
							if (!str.startsWith(rootPath)) {
								throw new IOException("Url does not belong to jar file: url=" + str + " jar=" + rootPath);
							}
							String relative = str.substring(rootPath.length());

							JarEntry entry = jarFile.getJarEntry(relative);
							if (entry == null) {
								throw new FileNotFoundException("Requested file not found: " + str);
							}

							return new URLConnection(url)
							{
								@Override
								public void connect() throws IOException
								{
								}

								public InputStream getInputStream() throws IOException
								{
									return jarFile.getInputStream(entry);
								}
							};
						}
					}
				);
			}
			catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}

		String getSpecificationTitle()
		{
			return manifest.getMainAttributes().getValue(Name.SPECIFICATION_TITLE);
		}

		String getSpecificationVersion()
		{
			return manifest.getMainAttributes().getValue(Name.SPECIFICATION_VERSION);
		}

		String getSpecificationVendor()
		{
			return manifest.getMainAttributes().getValue(Name.SPECIFICATION_VENDOR);
		}

		String getImplementationTitle()
		{
			return manifest.getMainAttributes().getValue(Name.IMPLEMENTATION_TITLE);
		}

		String getImplementationVersion()
		{
			return manifest.getMainAttributes().getValue(Name.IMPLEMENTATION_VERSION);
		}

		String getImplementationVendor()
		{
			return manifest.getMainAttributes().getValue(Name.IMPLEMENTATION_VENDOR);
		}

		URL getSealURL()
		{
			String seal = manifest.getMainAttributes().getValue(Name.SEALED);
			if (seal != null) {
				try {
					return new URL(seal);
				} catch (MalformedURLException e) {
					// Ignore, will return null
				}
			}
			return null;
		}
	}

	/**
	 * JAR entry details.
	 */
	@RequiredArgsConstructor
	static class JarEntryInfo {
		final JarFileInfo jarFileInfo;
		final JarEntry jarEntry;

		URL getURL()
		{
			try {
				return new URL(jarFileInfo.rootUrl, jarEntry.getName());
			}
			catch (MalformedURLException e) {
				throw new UncheckedIOException(e);
			}
		}

		String getBaseName()
		{
			String name = jarEntry.getName();
			int lastSlash = name.lastIndexOf('/');
			return lastSlash >= 0 ? name.substring(lastSlash) : name;
		}

		@Override
		public String toString()
		{
			return "JAR=" + jarFileInfo.jarFile.getName() + " ENTRY=" + jarEntry;
		}

		/**
		 * Read entry as byte array.
		 *
		 * @return
		 *      byte array for this entry
		 *
		 * @throws IOException
		 *      if reading failed.
		 */
		byte[] getContent() throws IOException
		{
			long size = jarEntry.getSize();
			if (size > Integer.MAX_VALUE) {
				throw new IOException("Entry exceeds max allowed size of Integer.MAX_VALUE: entry=" + jarEntry + " size=" + size);
			}
			byte[] content = new byte[(int) size];
			try (InputStream input = getInputStream()) {
				for (int off = 0; off < content.length; ) {
					int read = input.read(content, off, content.length - off);
					if (read <= 0) {
						throw new IOException("Jar entry stream ended prematurely: entry=" + jarEntry + " expected=" + content.length + " ended=" + off);
					}
					off += read;
				}
			}
			return content;
		}

		/**
		 * Open entry as InputStream.
		 *
		 * @return
		 *      InputStream for the entry.
		 *
		 * @throws IOException
		 *      if reading failed.
		 */
		InputStream getInputStream() throws IOException
		{
			return jarFileInfo.jarFile.getInputStream(jarEntry);
		}
	}

	/**
	 * OneJarLoaderException unchecked exception.
	 */
	private static class OneJarLoaderException extends RuntimeException
	{
		OneJarLoaderException(String message) {
			super(message);
		}

		OneJarLoaderException(String message, Throwable cause) {
			super(message, cause);
		}

		OneJarLoaderException(Throwable cause) {
			super(cause);
		}
	}

	@RequiredArgsConstructor
	public static class Logger
	{
		public enum Level
		{
			OFF,
			ERROR,
			WARN,
			INFO,
			DEBUG,
			TRACE,
		}

		private final String name;

		private final Level configuredLevel;

		private final PrintStream output;

		public boolean isEnabled(Level level)
		{
			return level.ordinal() <= configuredLevel.ordinal();
		}

		public void trace(String format, Object... args)
		{
			log(Level.DEBUG, format, args);
		}

		public void debug(String format, Object... args)
		{
			log(Level.DEBUG, format, args);
		}

		public void info(String format, Object... args)
		{
			log(Level.INFO, format, args);
		}

		public void warn(String format, Object... args)
		{
			log(Level.WARN, format, args);
		}

		public void error(String format, Object... args)
		{
			log(Level.ERROR, format, args);
		}

		public void log(Level level, String format, Object... args)
		{
			if (isEnabled(level)) {
				output.printf(name + "-" + level + ": " + format + "\n", args);
			}
		}

	}
}
