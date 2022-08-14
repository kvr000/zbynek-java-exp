# Deserialization Benchmark

## Java JSON, XML and Properties stream deserialization benchmark

Set of benchmarks measuring deserialization of simple data structure for various serialization formats.

Benchmarks use low-level stream parsers to test parsers pure performance, not including any high level data binding,
annotations etc.

### Performance

Sorted by performance (fastest to slowest).  Measured on laptop CPU i7-1185G7 @ 3.00GHz :

```
Benchmark                                                     Mode  Cnt       Score       Error  Units
DeserializeBenchmark.benchmarkDeserializeJsonJackson          avgt    4   19677.678 ± 2961.419  ns/op
DeserializeBenchmark.benchmarkDeserializeJsonGson             avgt    4   25001.741 ± 2710.641  ns/op
DeserializeBenchmark.benchmarkDeserializeJsonJsr353Glassfish  avgt    4   30247.284 ± 1480.820  ns/op
DeserializeBenchmark.benchmarkDeserializePropertiesIterate    avgt    4   68602.967 ± 2365.392  ns/op
DeserializeBenchmark.benchmarkDeserializeXmlStaxCoreJava      avgt    4   95300.197 ± 4763.139  ns/op
DeserializeBenchmark.benchmarkDeserializePropertiesLookup     avgt    4  105332.912 ± 6115.232  ns/op
```

Apple M1 is about 20% slower and XmlStaxCoreJava was 3% slower than PropertiesLookup.


## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com or http://kvr.znj.cz/software/java/ and https://github.com/kvr000

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
