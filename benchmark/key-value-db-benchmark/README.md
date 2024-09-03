# Benchmarks of persisted Map implementations


## raw data

```
Benchmark                                          Mode  Cnt          Score   Error  Units
ChronicleMapBenchmark.benchmarkSequentialMulti1M   avgt    2     223463.312          us/op
ChronicleMapBenchmark.benchmarkSequentialSingle1M  avgt    2     843370.947          us/op
JavaHashMapBenchmark.benchmarkSequentialMulti1M    avgt    2     104265.519          us/op
JavaHashMapBenchmark.benchmarkSequentialSingle1M   avgt    2     169991.695          us/op
MapDbBenchmark.benchmarkSequentialMulti1M          avgt    2  153360135.600          us/op
MapDbBenchmark.benchmarkSequentialSingle1M         avgt    2  120093503.950          us/op
```