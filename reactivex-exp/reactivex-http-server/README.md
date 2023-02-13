# Testing ReactiveX HTTP server

## Usage

```shell
$ ./target/reactivex-http-server -h
```

### Testing example

This is testing example to run one proxy directly, another proxy via 10 forwards and run dummy HTTP server:

```
./target/reactivex-http-server \
    --http-server tcp4:localhost:5555 
```

