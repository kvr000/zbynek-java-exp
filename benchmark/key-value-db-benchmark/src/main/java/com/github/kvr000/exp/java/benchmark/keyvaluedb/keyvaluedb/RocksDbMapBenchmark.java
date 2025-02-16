package com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb;

import com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support.AbstractCloseableConsumer;
import com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support.MultiThreadedPopulator;
import com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support.SequentialMultiThreadedBenchmarker;
import com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support.SequentialSingleThreadedBenchmarker;
import com.github.kvr000.exp.java.benchmark.keyvaluedb.keyvaluedb.support.SmallKeyValueSupplier;
import lombok.extern.log4j.Log4j2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@State(value = Scope.Benchmark)
@Fork(value = 1)
@Warmup(iterations = 1)
@Measurement(iterations = 2, time = 10)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Log4j2
public class RocksDbMapBenchmark
{
	private final int NUM_ITEMS = 10_000_000;

	private RocksDB map;

	@Setup
	public void createMap() throws IOException
	{
		File file = new File("target/RockDb.rocksdb");
		if (!file.exists()) {
			try (
				RocksDB db = RocksDB.open(file.toString());
				SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()
			) {
				new MultiThreadedPopulator().populate(NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>()
				{
					@Override
					public void accept(Long object)
					{
						try {
							db.put(
									keyValueSupplier.generateKey(object),
									keyValueSupplier.generateValue(object)
							);
						}
						catch (RocksDBException e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
			catch (RocksDBException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			map = RocksDB.open(file.toString());
		}
		catch (RocksDBException e) {
			throw new RuntimeException(e);
		}
		log.info("Populating finished");
	}

	@TearDown
	public void teardown()
	{
		map.close();
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
					try {
						if (map.get(keyValueSupplier.generateKey(object)) == null)
							throw new IllegalStateException("Value not found for "+object);
					}
					catch (RocksDBException e) {
						throw new RuntimeException(e);
					}
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
					try {
						if (map.get(keyValueSupplier.generateKey(object)) == null)
							throw new IllegalStateException("Value not found for "+object);
					}
					catch (RocksDBException e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}
}
