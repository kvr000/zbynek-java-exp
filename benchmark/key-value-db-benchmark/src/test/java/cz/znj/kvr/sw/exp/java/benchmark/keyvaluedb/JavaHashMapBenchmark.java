package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb;

import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.AbstractCloseableConsumer;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.ByteArray;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialMultiThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SequentialSingleThreadedBenchmarker;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SingleThreadedPopulator;
import cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.support.SmallKeyValueSupplier;
import lombok.extern.log4j.Log4j2;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@State(value = Scope.Benchmark)
@Log4j2
public class JavaHashMapBenchmark
{
	private final int NUM_ITEMS = 10_000_000;

	private Map<ByteArray, ByteArray> map = createMap();

	public Map<ByteArray, ByteArray> createMap() {
		Map<ByteArray, ByteArray> map = new HashMap<>();
		try (SmallKeyValueSupplier keyValueSupplier = new SmallKeyValueSupplier()) {
			new SingleThreadedPopulator().populate(NUM_ITEMS, (Long partition) -> new AbstractCloseableConsumer<Long>() {
				@Override
				public void accept(Long object) {
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
		log.info("Populating finished");
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