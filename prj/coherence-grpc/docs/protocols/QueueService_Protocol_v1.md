# The QueueService Protocol v1

This document describes version 1 of the Coherence gRPC QueueService protocol.

The Queue Service protocol allows the client to interact with Coherence queues using a simplified APIon top of the Coherence NamedQueue and NamedDeque APIs. 

The protobuf messages used by the QueueService protocol are defined in the file [queue_service_messages_v1.proto](../../src/main/proto/queue_service_messages_v1.proto)

## Protocol Messages

All requests in the Queue Service protocol are `NamedQueueRequest` messages. When the client sends a request up the `ProxyService` stream it sends a `ProxyRequest` where the `request` field contains a protobuf `Any` message that wraps a `NamedQueueRequest` message. The contents of the `NamedQueueRequest` message will depend on the actual purpose of the request.

All responses sent by the server in the QueueService protocol will be a `ProxyResponse` where the `response` field contains a protobuf `Any` message that wraps a `NamedQueueResponse` message. Alternatively the `ProxyResponse` may just contain a `Complete` message or an `Error` message. The content of the `NamedQueueResponse` will vary depending on the purpose of the response.

For example, to perform an "offer" to the tail of a queue, the `ProxyServiceRequest` would be created like this in Java code:
```java
long         queueId   = ... // the queue identifier returned by ensure queue
long         requestId = ... // unique id for this request
ByteString   value     = ... // serialized key

ProxyRequest request = ProxyRequest.newBuilder()
        .setId(requestId)
        .setMessage(Any.pack(NamedQueueRequest.newBuilder()
                        .setQueueId(queueId)
                        .setType(NamedQueueRequestType.OfferTail)
                        .setMessage(Any.pack(BytesValue.of(value)))
                .build()))
        .build();
```

### The NamedQueueRequest
   
The `NamedQueueRequest` message contains the following fields:

| Name      | Type                    | Description                                                                                        |
 |-----------|-------------------------|----------------------------------------------------------------------------------------------------|
 | `type`    | `NamedQueueRequestType` | An enum identifying the message type                                                               |
 | `queueId` | `int32`                 | The unique identifier for the queue that the request is for (or zero for an ensure queue message). |
 | `message` | `Any`                   | A optional `Any` message containing a sub-message specific to the request `type`.                  |
       
### The NamedQueueResponse

The `NamedQueueResponse` message contains the following fields:

| Name      | Type           | Description                                                                                        |
 |-----------|----------------|----------------------------------------------------------------------------------------------------|
 | `queueId` | `int32`        | The unique identifier for the queue that the request is for (or zero for an ensure queue message). |
| `type`    | `ResponseType` | An enum identifying the message type                                                               |
 | `message` | `Any`          | A optional `Any` message containing a sub-message specific to the request `type`.                  |

The `type` field in the `ResponseType` can be used by the client to determine the type of message that has been sent by the server. This field is an emu with oe of the following values:

- `Message`  - The response is a response related to a request (the request identifier will be in the `id` field of the `ProxyResponse`)
- `Destroyed` - a queue has been destroyed. The `queueId` field of the `NamedQueueResponse` contains the identifier for the destroyed queue. The `message` field will not be set for a destroy event.

The content of the `message` field of a `NamedQueueResponse` will depend on the type of the request that the response is for. Details of this is covered in the documentation for each request type below. Some requests types will only result in a `ProxyService` message containing a `Complete` response (for example `clear()`).


## Initialize the ProxyService Stream for the queueService Protocol

When a gRPC client wishes to use this protocol with the `ProxyService` API, it sends an `InitRequest` with the `protocol` field set to `queueService`.

A stream is initialized for a single server side scope, so the only queues accessible on a stream will be queues from the scope name used in the `InitRequest`. If a client needs to access queues in different scoped sessions on the server it must create and initialize multiple `ProxyService/subChannel` streams.

The current API version is `1` so the client must set the `protocolVersion` and `supportedProtocolVersion` fields to `1`.

## Ensure Queue

Before a client can interact with a queue, it **must** send an `EnsureQueue` message and **must** wait for the response.
After the server has created the requested queue a `NamedQueueResponse` will be sent with the `queueId` field set to the unique identifier that the proxy server is using for that queue.

All subsequent requests for that queue should be sent using a `NamedQueueRequest` containing the same `queueId` value.

---
***IMPORTANT***

It is the clients responsibility to ensure that every `NamedQueueRequest` sent has a unique value for the correct value for the `queueId` field corresponding to the queue the request is for. Requests sent will an invalid `queueId` will receive an error response.  

---

Once an `EnsureQueue` message has been sent, the client may receive queue lifecycle events for that queue. 
These will be for queue truncate and queue destroy. 

A client may ensure multiple queue using the same `ProxyService` stream.

### Queue Destroyed Events

If the queue is destroyed, the client will receive a `ProxyResponse` with a zero `id` field as this is an event not related to a request.
The `response` field of the `ProxyResponse` will contain an `Any` message wrapping a `NamedQueueResponse`. 
The `queueId` field of the `NamedQueueResponse` will be set to the identifier of the destroyed queue.

### Queue Truncated Events

If the queue is truncated, the client will receive a `ProxyResponse` with a zero `id` field as this is an event not related to a request.
The `response` field of the `ProxyResponse` will contain an `Any` message wrapping a `NamedQueueResponse`. 
The `queueId` field of the `NamedQueueResponse` will be set to the identifier of the truncated queue.

---
***IMPORTANT***

Once a queue is destroyed, no further messages should be sent by the client for that queue. Doing so will result in an error response from the server. The client should not receive any further messages from the server related to this queue.
The client should clean up any state related to the destroyed queue.
---

## NamedQueueRequest Message Types

There are a number of values that can be specified in the `type` field of a `NamedQueueRequest`, each corresponding to a different NamedQueue request.

### EnsureQueue

Ensuring a queue is discussed above. A `NamedQueueRequest` message with a `type` field set to `EnsureQueue` must have a `message` field containing an `Any` value wrapping an `EnsureQueueRequest`.
                   
An `EnsureQueueRequest` message contains a single `string` field, which is the name of the queue to ensure and a `NamedQueueType` enum to specify the type of the queue to ensure.

The `QueueType` enum has three values:

-  `Queue` - a simple `NamedQueue` that stores data in a single partition
-  `Deque` - a simple double ended `NamedDeque` that stores data in a single partition
-  `PagedQueue` - a distributed `NamedQueue` that distributes data over the cluster

The response to an `EnsureQueue` request is a `NamedQueueResponse` containing the `queueId` for the new queue and an empty `message` field.
       
If multiple `EnsureQueue` requests are sent for the same queue name the same `queueId` will be returned as long as the requested queue type is compatible.

| Existing Queue Type | Requested Type | Action                                                                                |
 |---------------------|----------------|---------------------------------------------------------------------------------------|
 | `Queue`             | `Queue`        | The same `queueId` will be returned                                                   | 
 | `Queue`             | `Deque`        | An error response will be returned as a `Deque` is not compatible with a `Queue`      | 
 | `Queue`             | `PagedQueue`   | An error response will be returned as a `PagedQueue` is not compatible with a `Queue` | 
 | `Deque`             | `Queue`        | The same `queueId` will be returned                                                   | 
 | `Deque`             | `Deque`        | The same `queueId` will be returned                                                   | 
 | `Deque`             | `PagedQueue`   | An error response will be returned as a `PagedQueue` is not compatible with a `Deque` | 
 | `PagedQueue`        | `Queue`        | The same `queueId` will be returned                                                   | 
 | `PagedQueue`        | `Deque`        | An error response will be returned as a `Deque` is not compatible with a `PagedQueue` | 
 | `PagedQueue`        | `PagedQueue`   | The same `queueId` will be returned                                                   | 
     


### Clear

A `Clear` message is sent to clear a queue, equivalent to calling `queue.clear()`.

A `Clear` request requires a `NamedQueueRequest` message with a `type` field set to `Clear` should not set the `message` field (if set it is ignored). 

After the queue has been cleared the client will a `ProxyResponse` containing a `Complete` message. No `NamedQueueResponse` is sent.

### Destroy

A `Destroy` message is sent to destroy a queue, equivalent to calling `queue.destroy()`.

A `Destroy` request requires a `NamedQueueRequest` message with a `type` field set to `Destroy`. 
The `message` field should not be set (if set it is ignored). 

After the queue has been destroyed the client will a `ProxyResponse` containing a `Complete` message. No `NamedQueueResponse` is sent. Sending a `Destroy` message should result in the client receiving a queue destroyed event message before the complete message is received.

### IsEmpty

An `IsEmpty` message is sent to determine whether a queue is empty or not, equivalent to calling `queue.isEmpty()`.

A `IsEmpty` request requires a `NamedQueueRequest` message with a `type` field set to `IsEmpty`. 
The `message` field should not be set (if set it is ignored). 

An `IsEmpty` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the boolean result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the isEmpty() request.

### IsReady

An `IsReady` message is sent to determine whether a queue is ready on the server or not, equivalent to calling `queue.isReady()`.

A `IsReady` request requires a `NamedQueueRequest` message with a `type` field set to `IsReady`.
The `message` field should not be set (if set it is ignored). 

An `IsReady` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the boolean result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` containing will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the isReady() request.

### OfferTail
          
An `OfferTail` message is sent to offer a value to the tail of a queue or deque.

A `OfferTail` request requires a `NamedQueueRequest` message with a `type` field set to `OfferTail` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized 
value of the element to offer to the queue.

The `NamedQueueResponse` will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `QueueOfferResult`. This `QueueOfferResult` will contain a boolean `succeeded` value to indicate whether the offer was successful, if set to `true` the `QueueOfferResult` will have an `index` int64 (Java long) value set to the "index" of the offered element.

### OfferHead

An `OfferHead` message is sent to offer a value to the head of a deque.

---
***IMPORTANT***

The `OfferHead` request only applies to a Deque so the `queueId` **must** correspond to an `EnsureQueue` request 
that used a `NamedQueueType` of `Deque`.
---

A `OfferHead` request requires a `NamedQueueRequest` message with a `type` field set to `OfferHead` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized 
value of the element to offer to the queue.

The `NamedQueueResponse` will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `QueueOfferResult`. This `QueueOfferResult` will contain a boolean `succeeded` value to indicate whether the offer was successful, if set to `true` the `QueueOfferResult` will have an `index` int64 (Java long) value set to the "index" of the offered element.

### PollHead

An `PollHead` message is sent to poll the value from the head of a queue.

A `PollHead` request requires a `NamedQueueRequest` message with a `type` field set to `PollHead`.
The `message` field should not be set (if set it is ignored). 

A `PollHead` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` containing the get result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `OptionalValue`. This `OptionalValue` is the result of the get request. An `OptionalValue` is like a Java `Optional` as it has a `getPresent()` method that allows the caller to know whether the poll returned a result, or the queue was empty.

### PeekHead

An `PeekHead` message is sent to peek at the value from the head of a queue.

A `PeekHead` request requires a `NamedQueueRequest` message with a `type` field set to `PeekHead`.
The `message` field should not be set (if set it is ignored). 

A `PeekHead` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` containing the get result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `OptionalValue`. This `OptionalValue` is the result of the get request. An `OptionalValue` is like a Java `Optional` as it has a `getPresent()` method that allows the caller to know whether the poll returned a result, or the queue was empty. 

### PollTail

A `PollTail` message is sent to poll the value from the tail of a deque.

---
***IMPORTANT***

The `PollTail` request only applies to a Deque so the `queueId` **must** correspond to an `EnsureQueue` request 
that used a `NamedQueueType` of `Deque`.
---

A `PollTail` request requires a `NamedQueueRequest` message with a `type` field set to `PollTail`.
The `message` field should not be set (if set it is ignored). 

A `PollTail` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` containing the get result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `OptionalValue`. This `OptionalValue` is the result of the get request. An `OptionalValue` is like a Java `Optional` as it has a `getPresent()` method that allows the caller to know whether the poll returned a result, or the queue was empty.

### PeekTail

An `PeekTail` message is sent to peek at the value at the tail of a deque.

---
***IMPORTANT***

The `PeekTail` request only applies to a Deque so the `queueId` **must** correspond to an `EnsureQueue` request 
that used a `NamedQueueType` of `Deque`.
---

A `PeekTail` request requires a `NamedQueueRequest` message with a `type` field set to `PeekTail`.
The `message` field should not be set (if set it is ignored). 

A `PeekTail` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedQueueResponse` containing the get result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `OptionalValue`. This `OptionalValue` is the result of the get request. An `OptionalValue` is like a Java `Optional` as it has a `getPresent()` method that allows the caller to know whether the peek returned a result, or the queue was empty.

### Size

An `Size` message is sent to determine the number of entries in a queue, equivalent to calling `queue.size()`.

A `Size` request requires a `NamedQueueRequest` message with a `type` field set to `Size`.
The `message` field should not be set (if set it is ignored). 

A `Size` request will result in a `ProxyResponse` wrapping a `NamedQueueResponse`, followed by a `ProxyResponse` containing a `Complete` message.
The `NamedQueueResponse` response will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `Int32Value`. This `Int32Value` is the result of the size() request.
