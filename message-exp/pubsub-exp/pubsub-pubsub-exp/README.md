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
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS lettuce-subscribe -c 10 &
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS jedis-publish-workpool
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS jedis-publish-singleswork
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS jedis-publish-pooled
java -jar pubsub-jedis-benchmark.jar -r redis://$REDIS lettuce-publish
```

## Benchmarks

### Results

LettucePublishReceiveBenchmar is the fastest with 159000 msg/s, JedisPool client based on BatchWorkExecutor from
dryuf-concurrent did 58000 msg/s, JedisPooled was only at 7% of top performance at 10500 msg/s.

Data measured on 4-CPU Graviton-3 c7g.xlarge AWS EC2 instance, All Publisher, Redis and 100 other Subscribers running
remotely on different instances.

<!--- benchmark:table:publishreceive:order=lettuce-publish&compare=lettuce-publish: --->

|Benchmark           |Mode|Units|lettuce-publish|jedis-publish-singleswork|jedis-publish-workpool|jedis-publish-pooled|lettuce-publish%|jedis-publish-singleswork%|jedis-publish-workpool%|jedis-publish-pooled%|
|:-------------------|:---|:----|--------------:|------------------------:|---------------------:|-------------------:|---------------:|-------------------------:|----------------------:|--------------------:|
|Redis.publishReceive|avgt|ops/s|         159440|                    52349|                 57940|               10537|              +0|                       -67|                    -63|                  -93|

### Data

<!--- benchmark:data:publishreceive:jedis-publish-singleswork:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Redis.publishReceive  avgt    0  52349         ops/s
```

<!--- benchmark:data:publishreceive:jedis-publish-workpool:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Redis.publishReceive  avgt    0  57940         ops/s
```

<!--- benchmark:data:publishreceive:jedis-publish-pooled:: --->

```
Benchmark             Mode  Cnt  Score  Error  Units
Redis.publishReceive  avgt    0  10537         ops/s
```

<!--- benchmark:data:publishreceive:lettuce-publish:: --->

```
Benchmark             Mode  Cnt   Score  Error  Units
Redis.publishReceive  avgt    0  159440         ops/s
```

<!--- vim: set tw=120: --->
