// Command server is a minimal Gin app for reproducing "server crashes mid-response"
// against a real HTTP/1.1 client. It exposes four endpoints:
//
//	GET /stream/complete  - streams all chunks and finishes normally (terminating
//	                         zero-length chunk is written for us).
//	GET /stream/crash     - streams a few chunks, then panics with
//	                         http.ErrAbortHandler (the deliberate "abort this
//	                         response" sentinel).
//	GET /stream/panic     - streams a few chunks, then panics with a plain
//	                         error, simulating a genuine, unplanned bug rather
//	                         than a deliberate abort.
//	GET /panic/early      - panics before writing any part of the response at
//	                         all (no headers, no body). Unlike the other two,
//	                         this should come back as a clean, ordinary 500 --
//	                         there's nothing yet to salvage or abort.
//
// The two /stream/... crash endpoints leave the terminating chunk unwritten,
// so to an HTTP client they're indistinguishable from the server process
// actually dying mid-response. See ../README.md for what this demonstrates
// and how to run it.
package main

import (
	"bytes"
	"errors"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/gin-gonic/gin"
)

const (
	totalChunks     = 5         // chunks written on a fully successful stream
	crashAfterChunk = 3         // the crash endpoints write this many chunks, then abort
	chunkSize       = 32 * 1024 // bytes per chunk
)

var chunk = bytes.Repeat([]byte{'A'}, chunkSize)

// crashMode selects how (or whether) streamHandler aborts partway through.
type crashMode int

const (
	modeComplete      crashMode = iota // finish normally
	modeAbortSentinel                  // panic(http.ErrAbortHandler): deliberate abort
	modeGenericPanic                   // panic(error): stand-in for an unplanned bug
)

// streamHandler forces chunked transfer encoding by flushing the response
// immediately after setting the status, before Go/Gin has buffered enough
// output to auto-compute a Content-Length. It then writes chunks one at a
// time, flushing after each so they actually hit the wire individually
// (rather than getting coalesced) -- this is what makes the crash land
// mid-stream from the client's point of view instead of before anything
// was sent.
func streamHandler(mode crashMode) gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Content-Type", "application/octet-stream")
		c.Status(http.StatusOK)
		c.Writer.Flush() // commits headers now; no Content-Length set -> chunked encoding

		for i := 1; i <= totalChunks; i++ {
			if mode != modeComplete && i > crashAfterChunk {
				switch mode {
				case modeAbortSentinel:
					// http.ErrAbortHandler is the standard library's sentinel for
					// "abandon this response deliberately" -- net/http's per-connection
					// recovery special-cases it (no stack trace logged) but still tears
					// the connection down without finishing the response normally, so
					// the terminating chunk is never written.
					panic(http.ErrAbortHandler)
				case modeGenericPanic:
					// A completely ordinary panic, standing in for a real bug (nil
					// pointer, index out of range, whatever) that happens to occur
					// partway through an in-flight response. recoverAbortingIfCommitted
					// (see main) is what makes this behave the same as the deliberate
					// abort above, instead of being swallowed into a fake-clean 200.
					panic(errors.New("simulated application bug (not the deliberate ErrAbortHandler)"))
				}
			}
			if _, err := c.Writer.Write(chunk); err != nil {
				return // client already disconnected on its own
			}
			c.Writer.Flush()
			time.Sleep(30 * time.Millisecond)
		}
		// Normal return: the net/http machinery underneath Gin writes the
		// terminating zero-length chunk for us here.
	}
}

// recoverAbortingIfCommitted is a Recovery replacement that behaves correctly
// for streaming handlers, which gin.Recovery() does not. The distinction is
// whether the response has already been committed (c.Writer.Written()):
//
//   - Not yet written: nothing has reached the client, so it's safe to
//     recover cleanly and return a normal error response, same as
//     gin.Recovery() would.
//   - Already written: headers (and maybe some body) are already on the
//     wire. There is no way to retroactively turn that into a clean error --
//     the client already saw a 200 and a chunked body starting. Recovering
//     here and returning normally would let net/http finish the response
//     "successfully" with silently truncated data, which is precisely the
//     failure mode this whole project exists to catch. So instead this
//     re-panics with http.ErrAbortHandler, which propagates out to
//     net/http's own recovery and aborts the connection instead of
//     completing it.
//
// This is what makes /stream/panic behave the same as /stream/crash even
// though it panics with an ordinary error rather than the deliberate
// sentinel: whatever the original panic value was, once the response is
// committed this middleware converts it to the same abort behavior.
func recoverAbortingIfCommitted(c *gin.Context, err any) {
	if err != http.ErrAbortHandler {
		log.Print("Recovered from a panic while serving a request: path=", c.Request.URL.Path,
			" responseStarted=", c.Writer.Written(), " err=", err)
	}
	if c.Writer.Written() {
		panic(http.ErrAbortHandler)
	}
	c.AbortWithStatus(http.StatusInternalServerError)
}

// panicBeforeAnyOutput panics immediately, before ever calling c.Status,
// c.Writer.Flush, or c.Writer.Write -- i.e. before the response is committed
// in any way. This exercises the other branch of recoverAbortingIfCommitted:
// since c.Writer.Written() is still false when the panic is recovered, it
// should come back as a clean, ordinary 500 rather than an aborted
// connection, proving the recovery logic doesn't over-abort a panic that
// never reached the client in the first place.
func panicBeforeAnyOutput(c *gin.Context) {
	panic(errors.New("simulated application bug before any response was written"))
}

func main() {
	gin.SetMode(gin.ReleaseMode)

	r := gin.New()
	r.Use(gin.Logger())
	r.Use(gin.CustomRecovery(recoverAbortingIfCommitted))

	r.GET("/health", func(c *gin.Context) { c.String(http.StatusOK, "ok") })
	r.GET("/stream/complete", streamHandler(modeComplete))
	r.GET("/stream/crash", streamHandler(modeAbortSentinel))
	r.GET("/stream/panic", streamHandler(modeGenericPanic))
	r.GET("/panic/early", panicBeforeAnyOutput)

	port := os.Getenv("PORT")
	if port == "" {
		port = "8089"
	}

	srv := &http.Server{
		Addr:    "127.0.0.1:" + port,
		Handler: r,
		// No TLS and no h2c wiring here, so net/http serves plain HTTP/1.1
		// only -- there's no ALPN negotiation and nothing upgrades this to
		// HTTP/2 on its own.
	}
	log.Println("listening on", srv.Addr)
	log.Fatal(srv.ListenAndServe())
}
