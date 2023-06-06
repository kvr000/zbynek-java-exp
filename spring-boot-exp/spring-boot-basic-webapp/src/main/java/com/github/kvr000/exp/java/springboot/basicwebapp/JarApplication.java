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

package com.github.kvr000.exp.java.springboot.basicwebapp;

import com.jdotsoft.jarloader.JarClassLoader;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;


public class JarApplication
{
	public static void main(String[] args) throws Exception, Throwable
	{
		{
			String classpathStr = System.getProperty("java.class.path");
			System.out.println(Arrays.asList(classpathStr.split(Pattern.quote(File.pathSeparator))));
		}

		JarClassLoader jcl = new JarClassLoader();
		jcl.invokeMain(JarApplication.class.getPackageName() + ".Application", args);
	}
}
