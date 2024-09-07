package cz.znj.kvr.sw.exp.java.corejava.stream;

import org.mockito.invocation.InvocationOnMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 *
 */
public class NullTest
{
	@Test
	public void testNullInOriginal()
	{
		Function<Integer, Integer> mapper = mock(Function.class);
		Predicate<Integer> filter = mock(Predicate.class);

		when(mapper.apply(any()))
				.thenAnswer((InvocationOnMock inv) -> inv.getArgument(0));
		when(filter.test(any()))
				.thenAnswer((InvocationOnMock inv) -> true);

		List<Integer> result = Arrays.asList(1, null, 3).stream()
				.map(mapper)
				.filter(filter)
				.collect(Collectors.toList());

		verify(mapper, times(1))
				.apply(1);
		verify(mapper, times(1))
				.apply(null);
		verify(mapper, times(1))
				.apply(3);

		verify(filter, times(1))
				.test(1);
		verify(filter, times(1))
				.test(null);
		verify(filter, times(1))
				.test(3);

		Assert.assertEquals(result, Arrays.asList(1, null, 3));
	}
}
