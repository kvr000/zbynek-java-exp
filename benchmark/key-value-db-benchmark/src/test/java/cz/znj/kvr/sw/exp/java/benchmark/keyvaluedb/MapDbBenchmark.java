package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb;

import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.AbstractCloseableConsumer;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.ByteArray;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialMultiThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialSingleThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SingleThreadedPopulator;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SmallKeyValueSupplier;
import lombok.extern.log4j.Log4j2;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;


@State(value = Scope.Benchmark)
@Log4j2
public class MapDbBenchmark
{
	private final int NUM_ITEMS = 1_100_000;

	private Map<ByteArray, ByteArray> map = createMap();

	public Map<ByteArray, ByteArray> createMap() {
		File file = new File("target/MapDb.mapdb");
		if (!file.exists()) {
			try (DB db = DBMaker.fileDB(file)
					.allocateStartSize(NUM_ITEMS)
					.fileChannelEnable()
					.fileMmapEnableIfSupported()
					.make()
			) {
				Map<ByteArray, ByteArray> map = (Map<ByteArray, ByteArray>) db.hashMap("test").createOrOpen();
				try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
					new SingleThreadedPopulator().populate(NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>()
					{
						@Override
						public void accept(Long object)
						{
							map.put(
									new ByteArray(keyValueSupplier.generateKey(object)),
									new ByteArray(keyValueSupplier.generateValue(object))
							);
						}
					});
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		log.info("Populating finished");
		DB db = DBMaker.fileDB(file)
				.readOnly()
				.allocateStartSize(NUM_ITEMS)
				.fileChannelEnable()
				.fileMmapEnableIfSupported()
				.make();
		Map<ByteArray, ByteArray> map = (Map<ByteArray, ByteArray>) db.hashMap("test").open();
		return map;
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 2)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkSequentialSingle1M() throws IOException {
		try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
			new SequentialSingleThreadedBenchmarker().benchmark(1_000_000, NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>() {
				@Override
				public void accept(Long object) {
					if (map.get(new ByteArray(keyValueSupplier.generateKey(object))) == null)
						throw new IllegalStateException("Value not found for "+object);
				}
			});
		}
	}

	@Benchmark
	@Fork(0)
	@Warmup(iterations = 2)
	@Measurement(iterations = 2, time = 10)
	public void benchmarkSequentialMulti1M() throws IOException {
		try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
			new SequentialMultiThreadedBenchmarker().benchmark(1_000_000, NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>() {
				@Override
				public void accept(Long object) {
					if (map.get(new ByteArray(keyValueSupplier.generateKey(object))) == null)
						throw new IllegalStateException("Value not found for "+object);
				}
			});
		}
	}
}
