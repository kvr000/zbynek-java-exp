package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb;

import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.AbstractCloseableConsumer;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.ByteArray;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.MultiThreadedPopulator;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialMultiThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialSingleThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SingleThreadedPopulator;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SmallKeyValueSupplier;
import lombok.extern.log4j.Log4j2;
import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@State(value = Scope.Benchmark)
@Fork(0)
@Warmup(iterations = 1)
@Measurement(iterations = 2, time = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Log4j2
public class ChronicleMapBenchmark
{
	private final int NUM_ITEMS = 10_000_000;

	private Map<byte[], byte[]> map;

	@Setup
	public void createMap() throws IOException
	{
		File file = new File("target/ChronicleMapBenchmark.chrodb");
		if (!file.exists()) {
			try (
					ChronicleMap<byte[], byte[]> map = ChronicleMapBuilder.of(byte[].class, byte[].class)
							.entries(NUM_ITEMS)
							.averageKeySize(10)
							.averageValueSize(20)
							.maxBloatFactor(100)
							.createPersistedTo(new File("target/ChronicleMapBenchmark.chrodb"));
					SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()
			) {
				new MultiThreadedPopulator().populate(NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>()
				{
					@Override
					public void accept(Long object)
					{
						map.put(
								keyValueSupplier.generateKey(object),
								keyValueSupplier.generateValue(object)
						);
					}
				});
			}
		}
		map = ChronicleMapBuilder.of(byte[].class, byte[].class)
				.recoverPersistedTo(file, true);
		log.info("Populating finished");
	}

	@Benchmark
	public void benchmarkSequentialSingle1M() throws IOException
	{
		try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
			new SequentialSingleThreadedBenchmarker().benchmark(1_000_000, NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>()
			{
				@Override
				public void accept(Long object)
				{
					if (map.get(keyValueSupplier.generateKey(object)) == null)
						throw new IllegalStateException("Value not found for "+object);
				}
			});
		}
	}

	@Benchmark
	public void benchmarkSequentialMulti1M() throws IOException
	{
		try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
			new SequentialMultiThreadedBenchmarker().benchmark(1_000_000, NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>()
			{
				@Override
				public void accept(Long object)
				{
					if (map.get(keyValueSupplier.generateKey(object)) == null)
						throw new IllegalStateException("Value not found for "+object);
				}
			});
		}
	}
}
