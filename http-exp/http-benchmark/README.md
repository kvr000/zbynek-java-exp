# Http client benchmark

Transferring 16 GB of data.

## RAW data

```
Benchmark                            Mode  Cnt           Score   Error  Units
ClientBenchmark.b1_UrlConnection     avgt    2  8794955295.500          ns/op
ClientBenchmark.b2_ApacheHttpClient  avgt    2  8672964168.000          ns/op
ClientBenchmark.b3_VertxHttpClient   avgt    2  9216141888.750          ns/op
ClientBenchmark.b4_VertxWebClient    avgt    2  9291373917.500          ns/op
```

Measured on Intel Core i7-1185G7 4-core 8-thread, 32 GB RAM, 2 TB NVMe
