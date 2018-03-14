package cz.znj.kvr.sw.exp.java.jackson.csv;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;


public class CsvConversionTest
{
	private CsvMapper 		csvMapper = new CsvMapper();

	@Test
	public void                     testObject() throws IOException
	{
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		TestObject o = csvMapper.readerFor(TestObject.class).with(schema).readValue("testId,name\n1,hello\n");
		AssertJUnit.assertEquals(1, o.getTestId());
		AssertJUnit.assertEquals("hello", o.getName());
	}

	@Test(expectedExceptions = MismatchedInputException.class)
	public void                     testList() throws IOException
	{
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		List<TestObject> list = csvMapper.readerFor(new TypeReference<List<TestObject>>(){}).with(schema).readValue("testId,name\n1,hello\n2,world\n");
		{
			TestObject o = list.get(0);
			AssertJUnit.assertEquals(1, o.getTestId());
			AssertJUnit.assertEquals("hello", o.getName());
		}
		{
			TestObject o = list.get(1);
			AssertJUnit.assertEquals(2, o.getTestId());
			AssertJUnit.assertEquals("world", o.getName());
		}
	}
}
