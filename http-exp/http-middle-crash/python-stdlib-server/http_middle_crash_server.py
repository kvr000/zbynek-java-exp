#!/usr/bin/env python3
"""Dependency-free HTTP/1.1 server for reproducing "server crashes
mid-response" against a real HTTP/1.1 client. Uses only the standard
library (http.server), no framework -- the Python counterpart to
../go-gin-server/main.go and ../java-jdk-server/.../HttpMiddleCrashServer.java, same
four endpoints and same semantics, so it drops into the run.sh contract as
another --server-dir and can be exercised by any of the client
implementations.

Endpoints:

  GET /health           - liveness check, used by run.sh to know when the
                           server is ready.
  GET /stream/complete   - streams 5 chunks and finishes normally (the
                           terminating zero-length chunk is written for us).
  GET /stream/crash      - streams 3 chunks, then raises AbortResponse, a
                           deliberate "abort this response" sentinel
                           (analogous to Go's http.ErrAbortHandler and
                           java-jdk-server's AbortResponse).
  GET /stream/panic      - streams 3 chunks, then raises a plain
                           RuntimeError, simulating a genuine unplanned bug
                           rather than a deliberate abort.
  GET /panic/early       - raises before writing anything at all (no
                           headers, no body). The mirror image of the two
                           above: nothing has reached the client yet, so
                           this should come back as a clean, ordinary 500.

The two /stream/... crash endpoints leave the terminating chunk unwritten,
so to an HTTP client they're indistinguishable from the server process
actually dying mid-response -- see ../README.md for what that demonstrates.

How the recovery split works (recovering(), below): http.server has no
Recovery-style middleware built in, but confirmed empirically (see
../README.md) is the raw material we need: if a request handler raises and
the exception is never caught, socketserver abruptly tears the connection
down without completing the response -- a chunk that was written and
flushed but never followed by the terminating chunk leaves the client
exactly as unable to reach a clean EOF as a genuine crash would.
recovering() wraps every route handler and tracks, with a plain boolean set
right after send_response()/end_headers(), whether the response has
already been committed -- the same distinction Go's c.Writer.Written() and
java-jdk-server's AtomicBoolean make:

  - Not yet committed: nothing has reached the client, so it's safe to
    catch the exception ourselves and write a normal 500.
  - Already committed: headers (and maybe some body) are already on the
    wire. There's no way to retroactively turn that into a clean error, so
    recovering() logs (unless it's the deliberate AbortResponse sentinel)
    and re-raises -- which is what leaves the connection to be torn down
    instead of finished "successfully" with silently truncated data.

Usage (see build.sh/run.sh for the standard way to invoke this):
  python3 http_middle_crash_server.py
  # PORT env var selects the listen port, default 8089
"""

import os
import sys
import time
import traceback
from enum import Enum, auto
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

TOTAL_CHUNKS = 5        # chunks written on a fully successful stream
CRASH_AFTER_CHUNK = 3   # the crash endpoints write this many chunks, then abort
CHUNK_SIZE = 32 * 1024  # bytes per chunk
CHUNK = b"A" * CHUNK_SIZE


class Mode(Enum):
    """How (or whether) a stream handler aborts partway through."""

    COMPLETE = auto()        # finish normally
    ABORT_SENTINEL = auto()  # raise AbortResponse: deliberate abort
    GENERIC_PANIC = auto()   # raise RuntimeError: stand-in for an unplanned bug


class AbortResponse(Exception):
    """Deliberate "abandon this response" sentinel, mirroring Go's
    http.ErrAbortHandler and java-jdk-server's AbortResponse: recovering() still
    aborts the connection for it like any other exception once the response
    is committed, but skips logging it as if it were a genuine bug.
    """


def stream_handler(mode):
    """Forces chunked transfer encoding by sending a Transfer-Encoding:
    chunked header ourselves (http.server does not compute this
    automatically the way Gin/JDK's HttpServer do) and flushing after each
    chunk so it actually hits the wire on its own, rather than getting
    coalesced with the next one. That's what makes the crash land
    mid-stream from the client's point of view instead of before anything
    was sent.
    """

    def handler(request):
        request.send_response(HTTPStatus.OK)
        request.send_header("Content-Type", "application/octet-stream")
        request.send_header("Transfer-Encoding", "chunked")
        request.end_headers()
        request.committed = True

        for i in range(1, TOTAL_CHUNKS + 1):
            if mode is not Mode.COMPLETE and i > CRASH_AFTER_CHUNK:
                if mode is Mode.ABORT_SENTINEL:
                    # See the module docstring: an uncaught exception after the
                    # response is committed leaves the terminating chunk
                    # unwritten, which is exactly what a genuine mid-response
                    # crash looks like to the client.
                    raise AbortResponse("deliberate simulated crash")
                if mode is Mode.GENERIC_PANIC:
                    # An ordinary exception, standing in for a real bug (a
                    # KeyError, an off-by-one, whatever) that happens to occur
                    # partway through an in-flight response. recovering()
                    # treats this the same as the deliberate abort above,
                    # because by this point the response is already committed.
                    raise RuntimeError(
                        "simulated application bug (not the deliberate abort sentinel)"
                    )
            _write_chunk(request, CHUNK)
            time.sleep(0.03)

        # Normal return: write the terminating zero-length chunk ourselves.
        request.wfile.write(b"0\r\n\r\n")
        request.wfile.flush()

    return handler


def panic_early(request):
    raise RuntimeError("simulated application bug before any response was written")


def health(request):
    _respond_plain(request, HTTPStatus.OK, "ok")


def recovering(handler):
    """Wraps a route handler so an uncaught exception doesn't just crash the
    request-handling thread. See the module docstring for the
    committed/not-committed distinction this makes.
    """

    def wrapped(request):
        request.committed = False
        try:
            handler(request)
        except Exception as exc:
            if not isinstance(exc, AbortResponse):
                print(
                    "Recovered from an exception while serving a request: "
                    f"path={request.path} responseStarted={request.committed} err={exc!r}",
                    file=sys.stderr,
                )
                traceback.print_exc()
            if not request.committed:
                # Nothing has reached the client yet: safe to recover cleanly
                # and return a normal error response.
                _respond_plain(request, HTTPStatus.INTERNAL_SERVER_ERROR, "internal error\n")
                return
            # Already streaming: there's no way to retroactively turn this
            # into a clean error, so re-raise. ThreadingHTTPServer then
            # abruptly tears the connection down rather than completing it --
            # the same failure mode a genuine crash produces. (Server.handle_error,
            # below, stays quiet about it since we've already logged here.)
            raise

    return wrapped


def _write_chunk(request, data):
    request.wfile.write(("%x\r\n" % len(data)).encode("ascii"))
    request.wfile.write(data)
    request.wfile.write(b"\r\n")
    request.wfile.flush()


def _respond_plain(request, status, body):
    data = body.encode("utf-8")
    request.send_response(status)
    request.send_header("Content-Type", "text/plain; charset=utf-8")
    request.send_header("Content-Length", str(len(data)))
    request.end_headers()
    request.wfile.write(data)


ROUTES = {
    "/health": health,
    "/stream/complete": recovering(stream_handler(Mode.COMPLETE)),
    "/stream/crash": recovering(stream_handler(Mode.ABORT_SENTINEL)),
    "/stream/panic": recovering(stream_handler(Mode.GENERIC_PANIC)),
    "/panic/early": recovering(panic_early),
}


class Handler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        route = ROUTES.get(self.path)
        if route is None:
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        route(self)


class Server(ThreadingHTTPServer):
    daemon_threads = True

    def handle_error(self, request, client_address):
        # recovering() has already logged (and, for genuine bugs, printed a
        # traceback for) anything worth knowing about a failed request; the
        # default handle_error would print a second, differently-formatted
        # dump of the same exception. This is the mirror image of the
        # gin.CustomRecovery nuance documented in ../README.md -- there Gin
        # logs unconditionally before ever calling our RecoveryFunc and that
        # couldn't be suppressed from inside it; here we have full control
        # over the framework's own error path, so we take it.
        pass


def main():
    port = int(os.environ.get("PORT") or "8089")
    server = Server(("127.0.0.1", port), Handler)
    print(f"listening on 127.0.0.1:{port}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
