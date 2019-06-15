package cz.znj.kvr.sw.exp.java.corejava.localization;


import lombok.extern.java.Log;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 */
@Log
public class LocaleStringTest
{
	@Test
	public void testRootToLanguageTag()
	{
		Locale locale = Locale.ROOT;
		AssertJUnit.assertEquals("und", locale.toLanguageTag());
	}

	@Test
	public void testLangToLanguageTag()
	{
		Locale locale = Locale.forLanguageTag("en");
		AssertJUnit.assertEquals("en", locale.toLanguageTag());
	}

	@Test
	public void testLangCountryToLanguageTag()
	{
		Locale locale = Locale.forLanguageTag("en-GB");
		AssertJUnit.assertEquals("en-GB", locale.toLanguageTag());
	}

	@Test
	public void testRootFromLanguageTagUnd()
	{
		Locale locale = Locale.forLanguageTag("und");
		AssertJUnit.assertEquals(Locale.ROOT, locale);
		AssertJUnit.assertSame(Locale.ROOT, locale);
	}

	@Test
	public void testRootFromLanguageTagEmptyString()
	{
		Locale locale = Locale.forLanguageTag("");
		AssertJUnit.assertEquals(Locale.ROOT, locale);
		AssertJUnit.assertSame(Locale.ROOT, locale);
	}
}
