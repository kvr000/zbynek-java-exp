# Testing Log4j2 behavior while throwing exception from parameter

```shell
java -Duser.timezone=UTC -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.log.Second=OFF  -jar target/slf4j-throwing-exp-shade
```

## Results

Log4j2 continues and formats parameter to report throwing exception:

```text
09:45:02.123 [main] ERROR cz.znj.kvr.sw.exp.java.logexp.log4j2.logging.throwing.Main - Throwing parameter: [!!!cz.znj.kvr.sw.exp.java.logexp.log4j2.logging.throwing.Main$1@33d512c1=>java.lang.NumberFormatException:bad number!!!]
```
