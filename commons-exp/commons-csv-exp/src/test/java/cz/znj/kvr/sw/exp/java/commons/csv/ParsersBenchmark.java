package cz.znj.kvr.sw.exp.java.commons.csv;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Warmup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;


public class ParsersBenchmark
{
	protected static InputStream getInput()
	{
		PipedOutputStream out = new PipedOutputStream();
		new Thread(() -> {
			try {
				out.write("col0,another,more,evenmore,col4,col5,col6,col7,col8,col9\n".getBytes());
				for (int i = 0; i < 100_000; ++i) {
					out.write("val0,another,more,evenmore,val4,val5,val6,val7,val8,val9\n".getBytes());
				}
				out.close();
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).start();
		try {
			return new PipedInputStream(out);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 1)
	public void                     benchmarkSplitRead() throws Exception
	{
		List<String[]> records = new LinkedList<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(getInput()));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] values = line.split(",");
			records.add(new String[]{ values[0], values[9] });
		}
	}

	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 2, batchSize = 1)
	@Fork(warmups = 1, value = 1)
	public void                     benchmarkCsvRead() throws Exception
	{
		List<String[]> records = new LinkedList<>();
		CSVParser reader = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(getInput()));
		for (CSVRecord record: reader) {
			records.add(new String[]{ record.get("col0"), record.get("col9") });
		}
	}
}
