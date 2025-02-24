package com.github.kvr000.exp.java.monitoring.opentelemetry.aws;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import lombok.extern.log4j.Log4j2;
import net.dryuf.base.concurrent.future.ScheduledUtil;

import java.util.Random;
import java.util.concurrent.TimeUnit;


@Log4j2
public class Main
{
	private static final Random random = new Random();
	private static final Meter meter = GlobalOpenTelemetry.get().getMeter("ZbynekOpenTelemetryApp");
	private static final DoubleHistogram histogram = meter.histogramBuilder("zbynek_latency")
		.setUnit("ms")
		.build();
	private static final LongCounter counter = meter.counterBuilder("zbynek_executions")
		.setUnit("int")
		.build();

	public static void main(String[] args) throws Exception
	{
		ScheduledUtil.sharedExecutor().scheduleAtFixedRate(Main::doOperation, 0, 10, TimeUnit.SECONDS);
		int key = System.in.read();
	}

	public static void doOperation()
	{
		double latency = random.nextDouble(100, 2000);
		recordLatency(latency);
		counter.add(latency < 1000 ? 1 : 0);
	}

	public static void recordLatency(double latency)
	{
		log.error("Recording latency: latency={}", latency);
		histogram.record(latency);
	}
}
