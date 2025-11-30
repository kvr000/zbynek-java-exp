# Http client benchmark

Transferring 16 GB of data.

## RAW data

```
Benchmark                            Mode  Cnt   Score   Error  Units
ClientBenchmark.b1_UrlConnection     avgt    2   9.055           s/op
ClientBenchmark.b2_HttpClient        avgt    2  14.583           s/op
ClientBenchmark.b3_ApacheHttpClient  avgt    2   8.909           s/op
ClientBenchmark.b4_OkHttpClient      avgt    2  11.272           s/op
ClientBenchmark.b6_VertxWebClient    avgt    2   9.341           s/op
```

Measured on Intel Core i7-1185G7 4-core 8-thread, 32 GB RAM, 2 TB NVMe
