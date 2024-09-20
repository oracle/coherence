
# How to use the V1 gRPC Proxy API

This document is for developers building gRPC clients that use the Coherence `ProxyService` API specified in the
protobuf file [proxy_service_v1.proto](../src/main/proto/proxy_service_v1.proto)

## Concepts

The `ProxyService` API is unlike a lot of other gRPC APIs because it only has a single method defined named `subChannel`.
This is a bidirectional streaming method which all requests and responses are sent over this single stream. 
This allows the `ProxyService` API to work a lot more like the Coherence Extend proxy works and provide some of the same features. This makes clients a little more complicated as they mist keep track if requests sent and route responses back to the relevant requester. It also makes things like cache events a little more complex.

The new API is versioned internally the same way that the Extend API is versioned. 
This allows a client to know the version of the API supported by the proxy it connects to and then use the correct API features.

As the API is a single bidirectional stream, it allows features such as member left and member joined to be built.

A single channel allows features such as synchronous map listeners to be supported, so events due to a requests that mutates a cache will be sent before the response to the mutating request. This can be important for features such as NearCache and CQC.

## ProxyService Requests and Responses

The messages for the `ProxyService` are defined in the protobuf file [proxy_service_messages_v1.proto](../src/main/proto/proxy_service_messages_v1.proto) 
Due to defining the Coherence API in protobuf files, a bidirectional stream only takes a single type of request message, in this case `ProxyRequest`, and returns a single type of response message `ProxyResponse`.

The `ProxyRequest` and `ProxyResponse` can wrap any other type of protobuf message, which allows the `ProxyService` to support various different message protocols (such as named cache, topics, etc.)
These payloads for the different protocols are also protobuf messages defined in other files.

### The ProxyRequest Message

A `ProxyRequest` is used by a client to send requests to the Coherence gRPC proxy. 
The content of a `ProxyRequest` message will depend on actual purpose of the message.

#### Request Identifier

The `ProxyRequest` message has an `int64` field named `id` (equivalent to a Java `long`) which is used to identify the request.
When the Coherence proxy sends a `ProxyResponse` this will contain the same `id` to allow the client to match a response to a request.

---
***IMPORTANT***

It is the clients responsibility to ensure that every `ProxyRequest` sent has a unique value for the `id` field.  

---

#### ProxyRequest Message Types

The `ProxyRequest` has a `request` field that contains the request payload. 
This field can be one of three types:

- `InitRequest` - a request to initialise the channel (documented below)
- `HeartbeatMessage` - an optional no-op message required by some gRPC client and server implementations to keep a connection alive
- `Any` - any other type of protobuf message specific to the protocol the `InitMessage` initialised

#### The InitRequest

After opening the bidirectional stream the first message sent by a client must be an `InitRequest`.
The purpose of this message is to notify the proxy which type of message protocol the client intends to use along with other information, such as the client's supported API versions etc. 
After sending the `InitRequest` the client must wait until it receives the `InitResponse` so that it knows the proxy server has correctly configured the stream to receive messages for the requested protocol. 

The `InitRequest` has the following fields:
           
| Name                       | Type     | Description                                                                                                                                                            |
|----------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `scope`                    | `string` | The scope name to use to obtain a `Session` on the proxy server.                                                                                                       |
| `format`                   | `string` | The serialization format being used by the client. This must be a format supported by the proxy server, e.g. `"java"`, `"pof"`, `"json"`, etc.                         |
| `protocol`                 | `string` | The name of the message protocol that the client intends to use this stream for. This must obviously correspond to one of the protocols supported by the proxy server. |
| `protocolVersion`          | `string` | The version of the protocol that the client wants to use, typically the highest version of the protocol supported by the client.                                       |
| `supportedProtocolVersion` | `string` | The minimum version of the protocol that the client can support.                                                                                                       |
| `heartbeat`                | `int64`  | An optional field that allows a client to specify how frequently the proxy should send a `Heartbeat` message.                                                          |

After receiving an `InitRequest` message from the client, the proxy server will initialise the stream to be able to receive messages for the requested protocol. If the protocol is not supported on the server, or the version on the server is not at least equivalent to the client's `supportedProtocolVersion`, then the server will close the stream by calling `onError`.

---
***NOTE***

A `ProxyService` stream can only be initialised once for a single protocol. 
If a client requires a different protocol then a separate `ProxyService/subChannel` should be created and initialised.   

---

#### Protocols

The name used for the `protocol` field in the `InitRequest` will depend on the actual API the client wants to use the stream for. Coherence has various protocols for different functionality, e.g. NamedCache, NamedTopic, etc. The documentation for the different protocols covers their names and version information.

The protocol names are case-insensitive when being processed on the server.

#### Heartbeats

In the `InitMessage` sent by the client, there is an optional `heartbeat` field. 
This field is used to tell the server how often (in milliseconds) to send heartbeat messages down the stream.
This can be important for some gRPC client implementations that will time out a stream that has no activity for more than a specific period of time. By default, no heartbeats are sent by the proxy.

A `ProxyResponse` that contains a `Heartbeat` message is effectively a no-op and can be discarded by the client.
If the client sends a `Heartbeat` message in a `ProxyRequest` this will also be a no-op on the server.

The `Heartbeat` message contains an optional `ack` field of type `bool`. 
If the client sends a heartbeat message with the `ack` field set to `true` the server will respond with a heartbeat message.

### The ProxyResponse Message

A `ProxyResponse` is used by a proxy server to send responses to the Coherence gRPC client. 
The content of a `ProxyResponse` message will depend on actual purpose of the message and the message protocol being used.

#### Request Identifier

The `ProxyResponse` message has an `int64` field named `id` (equivalent to a Java `long`) which is used to identify the request that the response is for.
Not all responses correspond to a request and may have an `id` field set to zero. For example heartbeat messages, or Coherence NamedCache events sent by the Named Cache Protocol.

#### ProxyResponse Message Types

The `ProxyResponse` has a `response` field that contains the response payload. 
This field can be one of three types:

- `InitResponse` - a request to initialise the channel (documented below)
- `HeartbeatMessage` - an optional no-op message required by some gRPC client and server implementations to keep a connection alive
- `ErrorMessage` - details of an error that occurred in the server when processing a request. This can be used to route an error or exception to the caller using the response `id` field.
- `Complete` - a complete message is used to indicate that the request identified by the `id` has completed and no further responses will be sent for that request `id`. Some requests only result in a complete response whereas others may have one or more other responses for the same `id` before a final complete response.
- `Any` - any other type of protobuf response message specific to the protocol the `InitMessage` initialised

#### The InitResponse

After the client initialises the stream by sending an `InitRequest`, the server will send an `InitResponse` message.
The client _**must wait for this response**_ before sending further messages up the stream.

The `InitResponse` contains the following fields:

| Name              | Type              | Description                                                                  |
|-------------------|-------------------|------------------------------------------------------------------------------|
| `uuid`            | `bytes`           | A byte array containing the client's unique identifier created by the proxy. |
| `version`         | `string`          | The version of Coherence that the proxy is running                           |
| `encodedVersion`  | `int32`           | The version of Coherence that the proxy is running encoded as an `int32`     |
| `protocolVersion` | `int32`           | The version of the protocol the client should use                            |
| `proxyMemberId`   | `int32`           | The Coherence member Id of the proxy                                         |
| `proxyMemberUuid` | `proxyMemberUuid` | The Coherence member UUID of the proxy                                       |

#### Protocol Versions

A client and a proxy may be running different versions of the Coherence API and support different versions of the various message protocols and functionality. This allows older clients to connect to newer proxies and vice-versa, newer clients to connect to older proxies. This is important for example, when performing zero downtime rolling updates.

When sending the `InitRequest` the client specifies the version of the protocol it wants to use, and also the lowest version it supports. The client will respond with the version of the protocol the client should use. 
This will be the highest version of the protocol the server supports that is greater than or equal to the client's `supportedProtocolVersion` and less than or equal to the client's `protocolVersion`.

With this information (and also  by knowing the Coherence version of the proxy) the client code can make decisions about what messages to send and what features can be used.

## Request Response Flow

A typical flow of messages between a client and server might look like this

1. Client calls the `ProxyService.subChannel` method to open the bidirectional stream
2. Client sends a `ProxyRequest` containing an `InitRequest`
3. Server responds with a `ProxyResponse` containing an `InitResponse`
4. Client sends a `ProxyRequest` with a unique id containing a protocol specific message 
5. The server asynchronously handles the message.
   1. If the request processing on the server has a result, then the server sends a `ProxyResponse` with the same request id containing a protocol specific message.
   2. The server sends a `ProxyResponse` with the same request id containing a `Complete` message

Once the stream has been initialised with an `InitRequest` and `InitResponse` the client may send as many requests as it wants to up the stream. The client does not have to wait for a response to a request before sending another request.

### Requests With A Stream of Results

Some requests may have multiple results (similar to how a normal gRPC server streaming method works).
In this case the client sends a request with an id and the server sends multiple responses with the same id.
When all the results have been sent, the server responds with a `Complete` message containing the request id.
After the complete message the client knows no more messages will be received for the request.

### Requests With No Results

Some types of request do not have a result (similar to calling a Java method with a `void` return type).
In this case the client sends a request with an id and the server will only respond with a `Complete` request with the same id.

### Errors

If a request results in an error on the server, then the server will send a `ProxyResponse` containing the request id and an `Error` message. The `Error` message wil contain the error text message and a serialized Java exception. 

Errors caused by a request will not cause the stream to close, just an error response message. Only unrecoverable server side errors cause  `onError()` to be called on the stream by the server. 

          

