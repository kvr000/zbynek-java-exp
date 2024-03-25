package cz.znj.kvr.sw.exp.java.benchmark.csv;

import lombok.SneakyThrows;
import org.apache.commons.io.input.InfiniteCircularInputStream;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class InputGenerator
{
	@SneakyThrows
	public static InputStream getInput()
	{
		return new InfiniteCircularInputStream("col0,another,more,evenmore,col4,col5,col6,col7,col8,col9\n".getBytes(StandardCharsets.UTF_8));
	}
}
