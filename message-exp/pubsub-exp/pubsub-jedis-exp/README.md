# Redis publish-subscribe experiments

## JedisPublishReceive

Simple single thread client.

## JedisWorkPoolPublishReceive

Batching worker Jedis pool based implementation.

```
sudo apt-get install redis
brew install redis
```

```
( cd target && redis-server -p 7207 ) &
```

## Benchmarks

### Results

JedisWorkPoolPublishReceiveBenchmark (based on BatchWorkExecutor from dryuf-concurrent) is the fastest with 139934 msg/s,
JedisPooled and single item JedisPool based solutions are 9% slower at 128777 msg/s.

Data measured on 4-CPU Graviton-3 c7g.xlarge AWS EC2 instance.

<!--- benchmark:table:publishreceive:: --->

|Benchmark           |Mode|Units|JedisWorkPoolPublishReceiveBenchmark|JedisSinglesWorkPoolPublishReceiveBenchmark|JedisPooledPublishReceiveBenchmark|
|:-------------------|:---|:----|-----------------------------------:|------------------------------------------:|---------------------------------:|
|Jedis.publishReceive|avgt|ops/s|                              139934|                                     128731|                            128777|

### Data

<!--- benchmark:data:publishreceive:JedisWorkPoolPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt   Score  Error  Units
Jedis.publishReceive  avgt    0  139934         ops/s
```

<!--- benchmark:data:publishreceive:JedisSinglesWorkPoolPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt   Score  Error  Units
Jedis.publishReceive  avgt    0  128731         ops/s
```

<!--- benchmark:data:publishreceive:JedisPooledPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt   Score  Error  Units
Jedis.publishReceive  avgt    0  128777         ops/s
```
