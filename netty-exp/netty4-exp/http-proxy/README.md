# Simple HTTP server based on Netty

In fact, it supports HTTP proxy, port forwarding and dummy HTTP server for testing purposes.

## Usage

```shell
$ ./target/netty-http-proxy -h
Usage: netty-http-proxy options... 
netty-http-proxy - runs port forwards or http proxy

Options:
-f,--forward proto:host:port proto:host:port    forwards first argument to
                                                second argument, proto can be
                                                one of tcp4, tcp6
--proxy-remap oldhost[:port]=newhost[:port]     remaps request from oldhost to
                                                newhost
--proxy-header name:value                       adds header to HTTP requests
--proxy-reset                                   reset proxy settings for next
                                                instance
-p,--proxy proto:[host:]port                    runs proxy on specified host and
                                                port
-http-server proto:[host:]port                  runs simple HTTP server on
                                                specified host and port
```

### Testing example

This is testing example to run one proxy directly, another proxy via 10 forwards and run dummy HTTP server:

```
./target/netty-http-proxy \
    -f tcp4:localhost:5554 tcp4:localhost:5555 \
    -f tcp4:localhost:4445 domain:/tmp/proxy9.sock \
    -f tcp4:localhost:4444 domain:/tmp/proxy0.sock \
    -f domain:/tmp/proxy0.sock domain:/tmp/proxy1.sock \
    -f domain:/tmp/proxy1.sock domain:/tmp/proxy2.sock \
    -f domain:/tmp/proxy2.sock domain:/tmp/proxy3.sock \
    -f domain:/tmp/proxy3.sock domain:/tmp/proxy4.sock \
    -f domain:/tmp/proxy4.sock domain:/tmp/proxy5.sock \
    -f domain:/tmp/proxy5.sock domain:/tmp/proxy6.sock \
    -f domain:/tmp/proxy6.sock domain:/tmp/proxy7.sock \
    -f domain:/tmp/proxy7.sock domain:/tmp/proxy8.sock \
    -f domain:/tmp/proxy8.sock domain:/tmp/proxy9.sock \
    -p domain:/tmp/proxy9.sock \
    -p tcp4:localhost:4446 \
    --http-server tcp4:localhost:5555 
```

## Build

`mvn package` should usually work.  However, under load, Netty sometimes behave incorrectly when using native transport
implementations (Epoll, Kqueue) and starts reading immediately (which should be handled by workaround) or closes channel
without removing it from poll set - the latter seems to happen only for Kqueue. In such situation, repeating build or
skipping tests may be necessary: `mvn -DskipTests=true package`
