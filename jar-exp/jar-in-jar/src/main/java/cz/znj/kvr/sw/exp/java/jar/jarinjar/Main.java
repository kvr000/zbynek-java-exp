/*
 * Copyright 2015 Zbynek Vyskovsky mailto:kvr000@gmail.com http://github.com/kvr000/zbynek-java/exp/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.znj.kvr.sw.exp.java.jar.jarinjar;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.jdotsoft.jarloader.JarClassLoader;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Main
{
	public static void main(String[] args) throws Exception
	{
		ImmutableMap.of("key", "value");
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			CompletableFuture.runAsync(() -> ImmutableList.of("list"), executor).get();
			CompletableFuture.runAsync(() -> ImmutableSet.of("set")).get();

			System.out.println("lsof :");
			System.out.println("lsof exit: "+
				new ProcessBuilder("lsof", "-p", String.valueOf(ProcessHandle.current().pid()))
					.redirectOutput(ProcessBuilder.Redirect.INHERIT)
					.redirectErrorStream(false)
					.start()
					.waitFor()
			);
			System.out.println();

			try (InputStream in = JarMain.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
				System.out.println("/META-INF/MANIFEST.MF :");
				in.transferTo(System.out);
				System.out.println();
			}

			try (InputStream in = JarMain.class.getResourceAsStream("/META-INF/maven/com.google.guava/guava/pom.properties")) {
				System.out.println("/META-INF/maven/com.google.guava/guava/pom.properties :");
				in.transferTo(System.out);
				System.out.println();
			}

			System.out.println("OK");
			System.exit(0);
		}
		finally {
			executor.shutdown();
		}
	}
}
