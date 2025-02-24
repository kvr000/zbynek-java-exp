# OS Metrics - Oshi -  experiments

## Execute AWS endpoint

```shell
-javaagent:target/aws-opentelemetry-agent.jar -Dotel.exporter.otlp.protocol=http -Dotel.exporter.otlp.endpoint=http://localhost:4318 -Dotel.javaagent.debug=true
```


## Execute AWS endpoint

```shell
-javaagent:target/aws-opentelemetry-agent.jar -Dotel.javaagent.debug=true
```
