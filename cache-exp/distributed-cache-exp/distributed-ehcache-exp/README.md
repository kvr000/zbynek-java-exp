# Distributed EhCache experiment

```
./target/ehcache-clustered-3.9.6-kit/server/bin/start-tc-server.sh -f src/server/server/terracotta-config.cfg &
./target/ehcache-clustered-3.9.6-kit/tools/bin/config-tool.sh activate -f src/server/server/terracotta-config.cfg &

mvn exec:java@execDistributedTest
# or
java -jar target/distributed-ehcache-exp-tests.jar
```
