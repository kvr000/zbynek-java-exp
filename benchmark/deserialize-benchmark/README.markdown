# Deserialization Benchmark

## Java JSON, XML and Properties stream deserialization benchmark

Set of benchmarks measuring deserialization of simple data structure for various serialization formats.

Benchmarks use low-level stream parsers to test parsers pure performance, not including any high level data binding,
annotations etc.

### Performance

Sorted by performance (fastest to slowest). Measured on low voltage Intel Core i7-4510U .

```
Benchmark                                                     Mode  Cnt       Score       Error  Units
DeserializeBenchmark.benchmarkDeserializeJsonJackson          avgt    4   38815.603 ±  1740.291  ns/op
DeserializeBenchmark.benchmarkDeserializeJsonJsr353Glassfish  avgt    4   65326.657 ± 10733.195  ns/op
DeserializeBenchmark.benchmarkDeserializeJsonGson             avgt    4   78592.116 ±  9179.913  ns/op
DeserializeBenchmark.benchmarkDeserializePropertiesIterate    avgt    4  115583.187 ±  2283.274  ns/op
DeserializeBenchmark.benchmarkDeserializePropertiesLookup     avgt    4  164464.482 ±  7008.497  ns/op
DeserializeBenchmark.benchmarkDeserializeXmlStaxCoreJava      avgt    4  175127.421 ± 10417.048  ns/op
```


## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com or http://kvr.znj.cz/software/java/ and https://github.com/kvr000

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
