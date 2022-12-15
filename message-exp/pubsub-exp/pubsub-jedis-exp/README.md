# Redis publish-subscribe experiments

## JedisPublishReceive

Simple single thread client.

## JedisWorkPoolPublishReceive

Batching worker Jedis pool based implementation.

```
sudo apt-get update && sudo apt-get -y install net-tools redis openjdk-17-jre-headless
brew install redis
```

```
( cd target && redis-server -p 7207 ) &
export REDIS=localhost:7207
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS subscribe -c 10 &
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS publish-workpool
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS publish-singleswork
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS publish-pooled
```

## Benchmarks

### Results

JedisWorkPoolPublishReceiveBenchmark (based on BatchWorkExecutor from dryuf-concurrent) is the fastest with 139934 msg/s,
JedisPooled and single item JedisPool based solutions are 9% slower at 128777 msg/s.

Data measured on 4-CPU Graviton-3 c7g.xlarge AWS EC2 instance, All Publisher, Redis and 10 other Subscribers running remotely.

<!--- benchmark:table:publishreceive:: --->

|Benchmark           |Mode|Units|JedisWorkPoolPublishReceiveBenchmark|JedisSinglesWorkPoolPublishReceiveBenchmark|JedisPooledPublishReceiveBenchmark|
|:-------------------|:---|:----|-----------------------------------:|------------------------------------------:|---------------------------------:|
|Jedis.publishReceive|avgt|ops/s|                               79418|                                      73183|                             20851|

### Data

<!--- benchmark:data:publishreceive:JedisWorkPoolPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Jedis.publishReceive  avgt    0  79418         ops/s
```

<!--- benchmark:data:publishreceive:JedisSinglesWorkPoolPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Jedis.publishReceive  avgt    0  73183         ops/s
```

<!--- benchmark:data:publishreceive:JedisPooledPublishReceiveBenchmark:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Jedis.publishReceive  avgt    0  20851         ops/s
```
