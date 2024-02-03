package com.github.kvr000.exp.java.spatial.restjpa;

import java.util.Optional;


public class Empty
{
	public static void main(String[] args)
	{
		System.err.printf("Starting, JarApplication took: %d ms\n", Optional.ofNullable(Long.getLong("JarApplication.start")).map(l -> System.currentTimeMillis()-l).orElse(-1L));
	}
}
