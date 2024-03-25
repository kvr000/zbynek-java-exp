# CSV parser benchmark

## RAW data

```
Benchmark                                Mode  Cnt     Score   Error  Units
ParsersBenchmark.benchmarkCsvColumnRead  avgt    2  2266.889          ns/op
ParsersBenchmark.benchmarkCsvKeyRead     avgt    2  2574.489          ns/op
ParsersBenchmark.benchmarkSplitRead      avgt    2  1003.134          ns/op
```

measured on Intel 1185G7 4-CPU 8-thread 32GB RAM, 1TB NVMe