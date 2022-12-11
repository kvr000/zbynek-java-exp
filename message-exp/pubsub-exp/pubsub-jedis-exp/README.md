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
