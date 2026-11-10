# Testing Slf4j behavior while throwing exception from parameter

```shell
java -Duser.timezone=UTC -Dorg.slf4j.simpleLogger.defaultLogLevel=WARN -Dorg.slf4j.simpleLogger.showDateTime=true -Dorg.slf4j.simpleLogger.log.Second=OFF  -jar target/slf4j-throwing-exp-shade
```

## Results

Slf4j continues and reports the parameter to throw exception:

```text
SLF4J(E): Failed toString() invocation on an object of type [cz.znj.kvr.sw.exp.java.logexp.slf4j.throwing.Main$1]
SLF4J(E): Reported exception:
java.lang.NumberFormatException: bad number
	at cz.znj.kvr.sw.exp.java.logexp.slf4j.throwing.Main$1.toString(Main.java:12)
	at org.slf4j.helpers.MessageFormatter.safeObjectAppend(MessageFormatter.java:291)
	at org.slf4j.helpers.MessageFormatter.deeplyAppendParameter(MessageFormatter.java:263)
	at org.slf4j.helpers.MessageFormatter.arrayFormat(MessageFormatter.java:225)
	at org.slf4j.helpers.MessageFormatter.basicArrayFormat(MessageFormatter.java:170)
	at org.slf4j.simple.SimpleLogger.innerHandleNormalizedLoggingCall(SimpleLogger.java:434)
	at org.slf4j.simple.SimpleLogger.handleNormalizedLoggingCall(SimpleLogger.java:377)
	at org.slf4j.helpers.AbstractLogger.handle_1ArgsCall(AbstractLogger.java:386)
	at org.slf4j.helpers.AbstractLogger.info(AbstractLogger.java:209)
	at cz.znj.kvr.sw.exp.java.logexp.slf4j.throwing.Main.main(Main.java:12)
[main] INFO cz.znj.kvr.sw.exp.java.logexp.slf4j.throwing.Main - Throwing parameter: [FAILED toString()]
```