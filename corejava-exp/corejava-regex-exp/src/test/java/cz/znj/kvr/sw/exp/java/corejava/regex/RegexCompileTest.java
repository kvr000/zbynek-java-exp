package cz.znj.kvr.sw.exp.java.corejava.regex;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/**
 * @author
 * 	Zbynek Vyskovsky
 */
public class RegexCompileTest
{
	@Test
	public void compile_whenIncludeSlash_thenTreatedAsLiteral()
	{
		Pattern pattern = Pattern.compile("/hello/");
		String[] matcher = pattern.split("a/hello/b");
		assertEquals(Arrays.asList(matcher), List.of("a", "b"));

		Pattern pattern2 = Pattern.compile("/hello/");
		String[] matcher2 = pattern2.split("ahellob");
		assertEquals(Arrays.asList(matcher2), List.of("ahellob"));
	}
}
