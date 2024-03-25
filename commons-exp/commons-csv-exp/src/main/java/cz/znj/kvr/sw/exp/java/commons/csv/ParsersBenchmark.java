package cz.znj.kvr.sw.exp.java.commons.csv;

import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


@Warmup(iterations = 1)
@Measurement(iterations = 2, batchSize = 1)
@Fork(warmups = 1, value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ParsersBenchmark
{
	public static final int DOCUMENT_LENGTH = 100_000;

	@SneakyThrows
	protected static PipedInputStream getInput()
	{
		PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in;
		in = new PipedInputStream(out);
		new Thread(() -> {
			try {
				out.write("col0,another,more,evenmore,col4,col5,col6,col7,col8,col9\n".getBytes());
				for (;;) {
					out.write("val0,another,more,evenmore,val4,val5,val6,val7,val8,val9\n".getBytes());
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}).start();
		return in;
	}

	@State(value = Scope.Benchmark)
	public static class SplitReadState
	{
		PipedInputStream input = getInput();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@State(value = Scope.Benchmark)
	public static class CsvReadState
	{
		PipedInputStream input = getInput();
		CSVParser reader;

		Iterator<CSVRecord> recordIterator;

		@SneakyThrows
		public CsvReadState()
		{
			reader = CSVFormat.DEFAULT.withHeader().parse(new InputStreamReader(input));
			recordIterator = reader.iterator();
		}

		@TearDown
		public void teardown() throws IOException
		{
			input.close();
		}
	}

	@Benchmark
	public void                     benchmarkSplitRead(SplitReadState state, Blackhole blackhole) throws Exception
	{
		String line;
		line = state.reader.readLine();
		String[] values = line.split(",");
		blackhole.consume(new String[]{ values[0], values[9] });
	}

	@Benchmark
	public void                     benchmarkCsvColumnRead(CsvReadState state, Blackhole blackhole) throws Exception
	{
		CSVRecord record = state.recordIterator.next();
		blackhole.consume(new String[]{record.get(0), record.get(9)});
	}

	@Benchmark
	public void                     benchmarkCsvKeyRead(CsvReadState state, Blackhole blackhole) throws Exception
	{
		CSVRecord record = state.recordIterator.next();
		blackhole.consume(new String[]{ record.get("col0"), record.get("col9") });
	}
}
