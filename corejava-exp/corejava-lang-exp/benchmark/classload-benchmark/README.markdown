# Class loading Java benchmarks

## Class and interface loading benchmarks

The project measures performance of Java class loader for interface, class, class implementing an interface. Both of them are measured on Jar with stored and deflate compression methods.

### Performance

The performance comparison looks like (measured on my low voltage i7 x86_64):

####
```
Benchmark                                    Time/unit(ns)
InterfaceLoadingBenchmark/STORED                     51447
ClassAloneLoadingBenchmark/STORED                    86915
ClassInheritedLoadingBenchmark/STORED               106206
InterfaceLoadingBenchmark/DEFLATED                   74462
ClassAloneLoadingBenchmark/DEFLATED                 105710
ClassInheritedLoadingBenchmark/DEFLATED             125594
```
