package cz.znj.kvr.sw.exp.java.jackson.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;


/**
 *
 */
@Log4j2
public class IncompatibleFieldTest
{
    private final ObjectMapper jackson = new ObjectMapper();

    @Test
    public void deserializeIncompatible() throws IOException {
        String input = "{\"mapField\":[\"hello\"]}";
        MismatchedInputException ex = Assert.expectThrows(
                MismatchedInputException.class,
                () -> jackson.readValue(input, Wrapper.class));
        log.error("Exception: ", ex);
        MatcherAssert.assertThat(ex.getMessage(), StringContains.containsString("Cannot deserialize instance"));
        MatcherAssert.assertThat(ex.getMessage(), StringContains.containsString("mapField"));
    }

    @Data
    private static class Wrapper
    {
        Map<String, String> mapField;
    }
}
