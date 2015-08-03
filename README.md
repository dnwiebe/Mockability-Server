# Mockability
Mockability is a general-purpose mock HTTP server for use in automated integration tests.

## General Operation
Once you have a Mockability server running somewhere, your tests will interact with it in two or three stages:

### Arrange
First, your test will **prepare** the server by sending it information about the requests it should expect to receive
and the responses it should return.

### Act
Then your test will **activate** the subject of the test (also known as the "system under test") to stimulate it to send
the requests you arranged in the first step, and to receive and process the pre-arranged responses to those requests.
Your test may then be able to examine the output from the subject to determine whether it processed those responses
correctly.

### Assert (optional)
Sometimes it's difficult to prove, just by looking at the output from the subject, that it sent exactly the requests
that the test required of it.  In these situations, once the subject is finished interacting with the Mockability
server, your test can request a report from Mockability, whereupon Mockability will respond with a catalog of all the
requests it received from your subject during the test.  Your test can then **assert** on this catalog and make sure it
contains everything it should and nothing it shouldn't.

## Clients
Clients can be written for Mockability in multiple languages for multiple frameworks, but it's fairly easy
to use it over naked HTTP without a pre-written client, too.  (You'll probably want library support for client HTTP, 
JSON, and Base64; otherwise it's pretty straightforward, as see below.)

The REST interface for Mockability centers around the `/mockability` URL and consists of three HTTP methods: DELETE,
POST, and GET.

### DELETE (clear)
If you send a DELETE request to `/mockability/<method>/<uri>`, then the Mockability server will forget everything it
ever knew about any preparations for or records of HTTP requests to <uri> with method <method> from your IP address.
This is handy at the beginning of a test, just in case a previous test or previous test run aborted early, without
consuming everything the server had waiting.

### POST (prepare)
If you send a POST request to `/mockability/<method>/<uri>`, with a body describing one or more HTTP responses
represented in a JSON format (see below), then the Mockability server will remember those responses and watch for
HTTP requests for <uri> with method <method>.  When it sees one, it will remove the oldest response from its list
and reply with it.  If it sees a request for which its list of prepared responses is empty, it will reply with a
499 status code and a `text/plain` body explaining why it can't make a more substantive contribution.

The JSON in the POST body should take the following form:
```
[
  {
    "status": 200,                                   -> Response status code
    "headers": [                                     -> HTTP headers, if any
      {"name": "Content-Type", "value": "text/html"},
      {"name": "Content-Length", "value": 25}        -> Length of content, not of BASE64 string
    ],
    "body": "PGI+VGF4YXRpb24gaXMgdGhlZnQhPC9iPg=="   -> Response body, if any, BASE64 encoded
  },
  {
    "status": 404,
    "headers": [
      {"name": "Location", "value": "http://qotd.com"}
    ]                                                -> No body, so not specified
  }
]
```

### GET (report)
If you send a GET request to `/mockability/<method>/<uri>`, then the Mockability server will respond with a list of
all the requests it has seen so far (since startup or the last matching DELETE request) for <uri> with <method> from
your IP address.  If there are no such requests, it will respond with a 499 status and a `text/plain` body with an
error message.

The JSON in the response body will take the following form:
```
[
  {
    "method": "GET",
    "uri": "/fartsound/list",
    "headers": [
    ]                                                -> No body, so not specified
  },
  {
    "method": "PUT",
    "uri": "/fartsound?desc=juicy",
    "headers": [
      {"name": "Content-Type", "value": "audio/mpeg"},
      {"name": "Content-Length", "value": 736743}
    ],
    "body": "<736743 bytes of .mp3 data, BASE64-encoded>"
  }
]
```
