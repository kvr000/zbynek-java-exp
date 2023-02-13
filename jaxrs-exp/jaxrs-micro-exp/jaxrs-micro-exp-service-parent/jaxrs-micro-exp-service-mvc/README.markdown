# JaxRs external file based implementation

## JaxRs Capture and Router

Proof of concept of lazily initialized JaxRs router. The implementation is
split into post-compile processing which captures existing JaxRs annotations
and saves them into XML metadata. The file is then loaded in the runtime and
all classes are lazily initialized only when needed. This avoids expensive
reflection scan at the application launch and is therefore useful for short
lived applications.

## Nested MVC

Unlike other JaxRs implementations, this one allows nesting controllers,
therefore one calling another and nesting the output of latter in the former.
This is useful to develop MVC applications based on reusable controllers and
views.

### Performance

The performance of both loader and path resolver are fast as it does not load
real controller classes (measured on my low voltage i7 x86_64).

#### Loading JaxRs metadata
```
Benchmark                                          Mode  Cnt      Score       Error  Units
JaxRsPathResolverLoaderBenchmark.benchmarkResolve  avgt    3  92530.940 ± 19756.251  ns/op
```

Loader takes about 92µs to load one JaxRs metadata file.

#### Resolving path
```
Benchmark                                           Mode  Cnt    Score   Error  Units
JaxRsPathResolverResolveBenchmark.benchmarkResolve  avgt    2  231.952          ns/op
```

Resolver takes only 231ns to resolve single path.

## License

The code is released under version 2.0 of the [Apache License][].

## Stay in Touch

Feel free to contact me at kvr000@gmail.com and http://github.com/kvr000

[Apache License]: http://www.apache.org/licenses/LICENSE-2.0
