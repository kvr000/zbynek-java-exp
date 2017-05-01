package cz.znj.kvr.sw.exp.java.jackson.csv;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.Assert;
import org.junit.Test;

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
		Assert.assertEquals(1, o.getTestId());
		Assert.assertEquals("hello", o.getName());
	}

	@Test
	public void                     testList() throws IOException
	{
		CsvSchema schema = CsvSchema.emptySchema().withHeader();
		List<TestObject> list = csvMapper.readerFor(new TypeReference<List<TestObject>>(){}).with(schema).readValue("testId,name\n1,hello\n2,world\n");
		{
			TestObject o = list.get(0);
			Assert.assertEquals(1, o.getTestId());
			Assert.assertEquals("hello", o.getName());
		}
		{
			TestObject o = list.get(1);
			Assert.assertEquals(2, o.getTestId());
			Assert.assertEquals("world", o.getName());
		}
	}
}
