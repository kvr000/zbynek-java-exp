package cz.znj.kvr.sw.exp.java.benchmark.keyvaluedb.keyvaluedb.support;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.StopWatch;

import java.io.IOException;
import java.util.function.Function;

/**
 * Database populator.
 */
@Log4j2
public class SingleThreadedPopulator implements Populator
{
	public void populate(long numItems, Function<Long, CloseableConsumer<Long>> writerSupplier) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		try (CloseableConsumer<Long> writer = writerSupplier.apply(0L)) {
			for (long i = 0; i < numItems; ++i) {
				writer.accept(i);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
		log.info("Populating done in "+stopWatch.getTime()+" ms");
	}
}
