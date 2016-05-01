package cz.znj.kvr.sw.exp.java.corejava.stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.stream.Stream;


/**
 * Optional.findFirst tests.
 */
public class OptionalTest
{
    @Test(expectedExceptions = NullPointerException.class)
    public void findFirstNull() {
        Optional<Integer> result = Stream.of(null, 1)
                .findFirst();
    }

    @Test
    public void findFirstNull_flatMap()
    {
        Optional<Integer> result = Stream.of(null, 1)
                .map(Optional::ofNullable)
                .findFirst()
                .orElseGet(Optional::empty);

        Assert.assertEquals(result, Optional.ofNullable(null));
    }
}
