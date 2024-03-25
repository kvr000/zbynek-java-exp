# CSV benchmark

## RAW data

```
Benchmark                        Mode  Cnt     Score   Error  Units
CommonsCsvBenchmark.columnRead   avgt    2  2287.945          ns/op
CommonsCsvBenchmark.keyRead      avgt    2  2290.876          ns/op
JacksonCsvBenchmark.keyRead      avgt    2   990.805          ns/op
SimpleSplitBenchmark.columnRead  avgt    2   681.443          ns/op
```

Measured on Intel Core i7-1185G7 4-core 8-thread, 32 GB RAM, 1 TB NVMe
