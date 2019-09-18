package cz.znj.kvr.sw.exp.java.corejava.stream;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;


/**
 *
 */
public class MulticastCollector<T, A> implements Collector<T, MulticastCollector, Void>
{
	public MulticastCollector(Collection<Stream<T>> consumers)
	{
		this.consumers = consumers;
	}

	@Override
	public Supplier<MulticastCollector> supplier()
	{
		return () -> this;
	}

	@Override
	public BiConsumer<MulticastCollector, T> accumulator()
	{
		for (Stream<T> s: consumers) {
			s.
		}
	}

	@Override
	public BinaryOperator<MulticastCollector> combiner()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Function<MulticastCollector, Void> finisher()
	{
		for (Stream<T> s: consumers) {
			s.close();
		}
		return null;
	}

	@Override
	public Set<Characteristics> characteristics()
	{
		return Collections.emptySet();
	}

	private Collection<Stream<T>> consumers;
}
