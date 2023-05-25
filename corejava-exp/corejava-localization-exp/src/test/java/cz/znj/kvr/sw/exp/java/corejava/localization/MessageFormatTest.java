package cz.znj.kvr.sw.exp.java.corejava.localization;

import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Locale;

import static org.testng.Assert.assertEquals;


public class MessageFormatTest
{
	@Test
	public void format_withDate_printedDate()
	{
		MessageFormat mf = new MessageFormat("Hello, it is {0,date,short}", Locale.ENGLISH);
		String result =
			mf.format(new Object[]{Date.from(LocalDate.of(2023, 03, 12).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()) });

		assertEquals(result, "Hello, it is 3/12/23");
	}
}
