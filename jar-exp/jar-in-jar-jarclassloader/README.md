# Jar in Jar classpath

Loading jar files as dependencies from a jar file.  com.jdotsoft.jarloader.JarClassLoader is used for this purpose.

```
mvn package && ( unzip -v target/jar-in-jar || true ) && ./target/jar-in-jar
```
