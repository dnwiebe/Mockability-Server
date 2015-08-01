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
There are clients already written for Mockability in multiple languages for multiple frameworks, but it's fairly easy
to use it over naked HTTP without a pre-written client, too.  (You'll probably want library support for client HTTP, 
JSON, and Base64; otherwise it's pretty straightforward, as see below.)
