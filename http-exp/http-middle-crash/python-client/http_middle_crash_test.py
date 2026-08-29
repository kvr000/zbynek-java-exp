#!/usr/bin/env python3
"""Dependency-free test client: no pip installs needed, just the standard
library (http.client, which defaults to HTTP/1.1 requests).

Exercises a server implementing the http-middle-crash contract (see
../go-gin-server, ../java-jdk-server, ../python-stdlib-server) against four scenarios:

  1. GET /stream/complete - a normal chunked response that finishes cleanly.
     Expectation: the client reads the whole body with no error.

  2. GET /stream/crash - the server aborts (panics / raises / throws,
     depending on implementation) partway through the chunked body, never
     sending the terminating chunk.
     Expectation: the client does NOT silently treat this as end-of-stream.
     It must surface an error, proving it can tell a truncated response
     apart from a complete one -- which is the whole point of forcing
     chunked encoding instead of relying on connection-close framing (see
     ../README.md).

  3. GET /stream/panic - same as /stream/crash, but the server's failure is
     a plain, undistinguished error rather than a deliberate abort sentinel,
     simulating a genuine unplanned bug mid-response. The server's own
     recovery logic is what makes this behave the same as /stream/crash
     instead of being recovered into a fake-clean 200 -- this case is what
     proves that general fix actually works, not just the specific
     sentinel value.
     Expectation: same as /stream/crash -- an error partway through.

  4. GET /panic/early - the server fails before writing anything at all (no
     headers, no body) -- the mirror image of case 3. Since nothing has
     reached the client yet, the server's recovery logic takes the OTHER
     branch and converts this into a clean, ordinary 500 rather than
     aborting the connection.
     Expectation: status 500, and the body reads to completion with no
     exception -- a failure that never reached the client should not be
     indistinguishable from a crash; it should look like an ordinary
     handled error.

Usage (see build.sh/run.sh for the standard way to invoke this):
  python3 http_middle_crash_test.py [baseUrl]
  # baseUrl defaults to http://127.0.0.1:8089

Exit code is 0 if all cases behave as expected, 1 otherwise.
"""

import http.client
import sys
import time
from urllib.parse import urlsplit

READ_CHUNK = 8192
HEALTH_TIMEOUT_SECONDS = 10


def main():
    base = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8089"

    wait_for_server(base, HEALTH_TIMEOUT_SECONDS)

    all_passed = True
    all_passed &= test_complete_stream_reads_fully(base)
    all_passed &= test_incomplete_stream_is_detected(
        base, "/stream/crash", "deliberate abort (AbortResponse)"
    )
    all_passed &= test_incomplete_stream_is_detected(
        base, "/stream/panic", "simulated unplanned bug (recovered-then-reaborted)"
    )
    all_passed &= test_early_panic_returns_clean_server_error(base)

    print()
    print("ALL TESTS PASSED" if all_passed else "SOME TESTS FAILED")
    sys.exit(0 if all_passed else 1)


def _connection(base):
    parts = urlsplit(base)
    return http.client.HTTPConnection(parts.hostname, parts.port, timeout=10)


def wait_for_server(base, timeout_seconds):
    deadline = time.monotonic() + timeout_seconds
    while time.monotonic() < deadline:
        try:
            conn = _connection(base)
            try:
                conn.request("GET", "/health")
                resp = conn.getresponse()
                resp.read()
                if resp.status == 200:
                    return
            finally:
                conn.close()
        except OSError:
            pass  # server not up yet, keep retrying
        time.sleep(0.1)
    raise RuntimeError(f"server at {base} never became healthy within {timeout_seconds}s")


def test_complete_stream_reads_fully(base):
    print("=== /stream/complete: expect a full, error-free read ===")
    conn = _connection(base)
    try:
        conn.request("GET", "/stream/complete")
        resp = conn.getresponse()
        log_headers(resp)

        body = resp.read()
        print(f"read {len(body)} bytes, no exception")

        passed = resp.status == 200 and len(body) > 0
        print("PASS" if passed else "FAIL")
        return passed
    except Exception as e:  # noqa: BLE001 - this is exactly what we're testing for
        print(f"UNEXPECTED EXCEPTION: {type(e).__name__}: {e}")
        print("FAIL")
        return False
    finally:
        conn.close()


def test_incomplete_stream_is_detected(base, path, label):
    """Shared by /stream/crash and /stream/panic: both should leave the
    client unable to read to a clean EOF, regardless of which failure the
    server used internally to trigger the abort.
    """
    print()
    print(f"=== {path}: expect the client to detect an incomplete response ({label}) ===")
    conn = _connection(base)
    try:
        conn.request("GET", path)
        resp = conn.getresponse()
        log_headers(resp)

        total_read = 0
        try:
            while True:
                data = resp.read(READ_CHUNK)
                if not data:
                    break
                total_read += len(data)
            # Reaching here means the stream reported a clean EOF -- i.e. the
            # client could NOT tell the truncated response apart from a
            # complete one. That's exactly the failure mode this whole
            # exercise is about avoiding.
            print(f"read {total_read} bytes, stream reported clean EOF (no exception)")
            print("FAIL: client could not distinguish the crash from a complete response")
            return False
        except Exception as e:  # noqa: BLE001 - this is exactly what we're testing for
            print(f"read {total_read} bytes before the read failed")
            print(f"caught {type(e).__name__}: {e}")
            print("PASS: client correctly detected the response as incomplete")
            return True
    except Exception as e:  # noqa: BLE001
        # A failure inside request()/getresponse() itself (before a body is
        # even handed back) is also a valid detection -- just log it
        # distinctly since it's a different point of failure than a
        # mid-body read error.
        print(f"request failed: {type(e).__name__}: {e}")
        print("PASS (detected before body reading started)")
        return True
    finally:
        conn.close()


def test_early_panic_returns_clean_server_error(base):
    """The mirror image of test_incomplete_stream_is_detected: here the
    failure happens before any response bytes go out, so the server's
    recovery logic takes the other branch and the client should see a
    normal, complete HTTP exchange -- just with a 500 status -- rather than
    a truncated/aborted connection.
    """
    print()
    print("=== /panic/early: expect a clean 500, not an aborted connection ===")
    conn = _connection(base)
    try:
        conn.request("GET", "/panic/early")
        resp = conn.getresponse()
        log_headers(resp)
        body = resp.read()
        print(f"read {len(body)} bytes, no exception")

        passed = resp.status == 500
        print("PASS" if passed else f"FAIL: expected status 500, got {resp.status}")
        return passed
    except Exception as e:  # noqa: BLE001
        print(f"UNEXPECTED EXCEPTION: {type(e).__name__}: {e}")
        print("FAIL: a failure before any output should be a clean 500, not a connection failure")
        return False
    finally:
        conn.close()


def log_headers(resp):
    transfer_encoding = resp.getheader("transfer-encoding", "<none>")
    content_length = resp.getheader("content-length", "<none>")
    print(
        f"status={resp.status} transfer-encoding={transfer_encoding} "
        f"content-length={content_length} version=HTTP/{resp.version // 10}.{resp.version % 10}"
    )


if __name__ == "__main__":
    main()
