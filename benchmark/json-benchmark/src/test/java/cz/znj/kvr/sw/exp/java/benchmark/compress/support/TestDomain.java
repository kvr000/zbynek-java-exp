package cz.znj.kvr.sw.exp.java.benchmark.compress.support;

import lombok.Data;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;


/**
 * Small data supplier of key and value.
 */
@Data
public class TestDomain
{
	private String firstname;
	private String surname;
}
