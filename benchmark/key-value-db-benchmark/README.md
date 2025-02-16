# Benchmarks of persisted Map implementations


## raw data

```
Benchmark                                          Mode  Cnt          Score   Error  Units
ChronicleMapBenchmark.benchmarkSequentialMulti1M   avgt    2     121494.548          us/op
ChronicleMapBenchmark.benchmarkSequentialSingle1M  avgt    2    1384949.552          us/op
JavaHashMapBenchmark.benchmarkSequentialMulti1M    avgt    2     186505.220          us/op
JavaHashMapBenchmark.benchmarkSequentialSingle1M   avgt    2     216202.293          us/op
MapDbBenchmark.benchmarkSequentialMulti1M          avgt    2  127153371.950          us/op
MapDbBenchmark.benchmarkSequentialSingle1M         avgt    2  129396766.650          us/op
RocksDbMapBenchmark.benchmarkSequentialMulti1M     avgt    2     759150.672          us/op
RocksDbMapBenchmark.benchmarkSequentialSingle1M    avgt    2    1792926.208          us/op
```
