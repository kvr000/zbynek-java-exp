# http-middle-crash

A minimal, runnable reproduction of how an HTTP/1.1 client can tell a server
crashing mid-response apart from a response that finished normally.

Without `Content-Length` or `Transfer-Encoding: chunked`, an HTTP/1.1 body
is delimited only by the connection closing, which makes a truncated
response and a genuinely complete one look identical on the wire. Chunked
encoding fixes this with an explicit end-of-body marker (a zero-length
"last chunk"): if the connection dies before that marker arrives, the
client can tell.

## What's tested

Every server implementation exposes the same four endpoints:

| Endpoint | Behavior |
|---|---|
| `GET /stream/complete` | Streams 5 chunks and finishes normally. The terminating chunk is sent; the client reads everything with no error. |
| `GET /stream/crash` | Streams 3 chunks, then aborts with a deliberate "abort this response" sentinel instead of returning normally. |
| `GET /stream/panic` | Streams 3 chunks, then fails with a plain, undistinguished error — standing in for a genuine unplanned bug rather than a deliberate abort. |
| `GET /panic/early` | Fails **before writing anything at all** — no headers, no body. Since nothing has reached the client yet, this comes back as an ordinary, clean `500`. |

`/stream/crash` and `/stream/panic` both leave the terminating chunk
unsent, so to the client either is indistinguishable from the server
process actually dying mid-response. A client is expected to detect both as
errors, and to receive a clean `500` for `/panic/early` rather than a
broken connection.

## Implementations

Any server works with any client — they only share the HTTP contract above.

- **go-gin-server** — Go, using [Gin](https://github.com/gin-gonic/gin). Chunked
  encoding is forced by omitting `Content-Length` and flushing before Gin
  can compute one. The crash endpoints panic with `http.ErrAbortHandler` (a
  deliberate sentinel) or a plain error; a custom `gin.CustomRecovery`
  handler checks `c.Writer.Written()` to decide whether to return a clean
  `500` or re-panic and abort the connection.
- **java-jdk-server** — Java, using the JDK's built-in
  `com.sun.net.httpserver.HttpServer`, no dependencies and no build step.
  The source file itself is the executable: it opens with a shebang line
  (`#!/usr/bin/env -S java --source 17`), so JEP 330's single-file
  source-launcher compiles it in memory and runs it directly when the OS
  execs it (JDK 17+, no `javac`, no `.class` files on disk). Per JEP 330
  the shebang mechanism only applies to files that do *not* end in `.java`,
  so the file has no extension. It also lives directly at the module's
  root rather than under `src/main/java`: on at least one dev setup, the
  shebang launcher fails when the script path is long/deeply nested, so
  it's invoked as the short `./HttpMiddleCrashServer` instead. Chunked
  encoding is forced via `sendResponseHeaders(200, 0)`. An uncaught
  exception after the response is committed aborts the connection; a
  `recovering()` wrapper tracks commitment with an `AtomicBoolean` to
  decide between a clean `500` and letting the abort happen.
- **python-stdlib-server** — Python, using the standard library's `http.server`,
  no dependencies. Chunked encoding is hand-written (`http.server` doesn't
  compute it). Same recovery pattern as the other two, implemented as a
  `recovering()` decorator tracking commitment with a plain attribute.
- **java-client** — Java, using the built-in `java.net.http.HttpClient`,
  forced to HTTP/1.1. Same shebang/no-extension, no-`javac`,
  module-root single-file-source-launcher setup as java-jdk-server (JDK
  17+, invoked as `./HttpMiddleCrashTest`).
- **python-client** — Python, using the standard library's `http.client`,
  which defaults to HTTP/1.1.

## Layout

```
go-gin-server/                    Gin server implementation
java-jdk-server/                  JDK-only server implementation (com.sun.net.httpserver)
  HttpMiddleCrashServer           (no .java extension, lives at module root -- see above)
python-stdlib-server/             stdlib-only server implementation (http.server)
  http_middle_crash_server.py
java-client/                      JDK-only client implementation
  HttpMiddleCrashTest             (no .java extension, lives at module root -- see above)
python-client/                    stdlib-only client implementation (http.client)
  http_middle_crash_test.py
run.sh                            orchestrator: builds+runs a server against a client
```

Each implementation directory exposes `build.sh` (no arguments, safe to
re-run) and `run.sh` (server: foreground, `$PORT`, exposes `GET /health`;
client: takes a base URL as `$1`, exit code reflects pass/fail). `run.sh`
only relies on that contract, so a new implementation just needs those two
scripts. For the two Java implementations, `build.sh` doesn't compile
anything -- it just sets the executable bit on the source file (in case it
didn't survive a checkout) and checks that `java` is 17+.

## Running it

```bash
./run.sh
./run.sh --server java-jdk-server
./run.sh --server python-stdlib-server --client python-client
./run.sh --server go-gin-server --client python-client --port 9090
```

`--server`/`--client` default to `go-gin-server`/`java-client`. See
`./run.sh --help` for the full option list.

### Manual steps

```bash
# terminal 1 -- pick one server
cd go-gin-server && ./build.sh && PORT=8089 ./run.sh
# or java-jdk-server, or python-stdlib-server

# terminal 2 -- pick one client
cd java-client && ./build.sh && ./run.sh http://127.0.0.1:8089
# or python-client
```
