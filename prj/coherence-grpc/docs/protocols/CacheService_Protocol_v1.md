# The CacheService Protocol v1

This document describes version 1 of the Coherence gRPC CacheService protocol.

The Cache Service protocol allows the client to interact with Coherence caches using functionality equivalent to the Coherence NamedMap and NamedCache APIs. The protocol allows clients to receive cache lifecycle events such as destroy and truncate as well as registering listeners for cache map listener events.                                                                    
The protobuf messages used by the CacheService protocol are defined in the file [cache_service_messages_v1.proto](../../src/main/proto/cache_service_messages_v1.proto)                                    
                                                          
## Protocol Messages

All requests in the Cache Service protocol are `NamedCacheRequest` messages. When the client sends a request up the `ProxyService` stream it sends a `ProxyRequest` where the `request` field contains a protobuf `Any` message that wraps a `NamedCacheRequest` message. The contents of the `NamedCacheRequest` message will depend on the actual purpose of the request.

All responses sent by the server in the CacheService protocol will be a `ProxyResponse` where the `response` field contains a protobuf `Any` message that wraps a `NamedCacheResponse` message. Alternatively the `ProxyResponse` may just contain a `Complete` message or an `Error` message. The content of the `NamedCacheResponse` will vary depending on the purpose of the response.

For example, to perform a "get" request, the `ProxyServiceRequest` would be created like this in Java code:
```java
long         cacheId   = ... // the cache identifier returned by ensure cache
long         requestId = ... // unique id for this request
ByteString   keyBytes  = ... // serialized key

ProxyRequest request = ProxyRequest.newBuilder()
        .setId(requestId)
        .setMessage(Any.pack(NamedCacheRequest.newBuilder()
                        .setCacheId(cacheId)
                        .setType(NamedCacheRequestType.Get)
                        .setMessage(Any.pack(BytesValue.of(keyBytes)))
                .build()))
        .build();
```

To unpack a response for a `Get` message in Java:
```java
ProxyResponse response; // response received by the StreamObserver
long requestId = response.getId(); // the id of the request that this response is for
switch (response.getResponseCase())
    {
    case MESSAGE:
        NamedCacheResponse ncr = response.getMessage().unpack(NamedCacheResponse.class);
        OptionalValue optional = ncr.getMessage().unpack(OptionalValue.class);
        if (optional.getPresent())
            {
            // the key mapped to a value
            ByteString valueBytes = optional.getValue();
            }
        else
            {
            // the key was not in the cache
            }
        break;
    case ERROR:
        // request failed
        ErrorMessage error = response.getError();
        // handle error...
        break;
    case COMPLETE:
        // request completed
        break;
    }
```

The code above is just an example of unpacking a `ProxyResponse`.
The actual code in the Java client is more complex than this and a little more generic.
- The first step is to read the request id, which will either match the response to a request or will be an event with no request id.
- The next step depends on if the response is for a request. In the example above it is a response to a `Get` request, so the expected response will be either a `Message` and `Error` or a `Complete`.
- If this is an `Error` then send the error response back to the requesting thread
- If this is a `Message` then as the request is a `Get` the `ProxyResponse` message field will be a `NamedCacheResponse` which will contain an `Any` field wrapping a `OptionalValue`. 

### The NamedCacheRequest
   
The `NamedCacheRequest` message contains the following fields:

| Name      | Type                    | Description                                                                                        |
 |-----------|-------------------------|----------------------------------------------------------------------------------------------------|
 | `type`    | `NamedCacheRequestType` | An enum identifying the message type                                                               |
 | `cacheId` | `int32`                 | The unique identifier for the cache that the request is for (or zero for an ensure cache message). |
 | `message` | `Any`                   | A optional `Any` message containing a sub-message specific to the request `type`.                  |
       
### The NamedCacheResponse

The `NamedCacheResponse` message contains the following fields:

| Name      | Type           | Description                                                                                        |
 |-----------|----------------|----------------------------------------------------------------------------------------------------|
 | `cacheId` | `int32`        | The unique identifier for the cache that the request is for (or zero for an ensure cache message). |
| `type`    | `ResponseType` | An enum identifying the message type                                                               |
 | `message` | `Any`          | A optional `Any` message containing a sub-message specific to the request `type`.                  |

The `type` field in the `ResponseType` can be used by the client to determine the type of message that has been sent by the server. This field is an emu with oe of the following values:

- `Message`  - The response is a response related to a request (the request identifier will be in the `id` field of the `ProxyResponse`)
  - `MapEvent` - The response is a MapEvent and should be routed to the MapListener instances registered on the client. Handling MapEvents is covered in detail below. The `message` field in the `NamedCacheResponse` will contains a `MapEventMessage`. 
- `Destroyed` - a cache has been destroyed. The `cacheId` field of the `NamedCacheResponse` contains the identifier for the destroyed cache. The `message` field will not be set for a destroy event.
- `Truncated` a cache has been truncated. The `cacheId` field of the `NamedCacheResponse` contains the identifier for the truncated cache. The `message` field will not be set for a destroy event.

For event messages (types `MapEvent`, `Destroyed` and `Truncated`) the `id` field of the `ProxyResponse` should be ignored.

The content of the `message` field of a `NamedCacheResponse` will depend on the type of the request that the response is for. Details of this is covered in the documentation for each request type below. Some requests types will only result in a `ProxyService` message containing a `Complete` response (for example `clear()`).


## Initialize the ProxyService Stream for the CacheService Protocol

When a gRPC client wishes to use this protocol with the `ProxyService` API, it sends an `InitRequest` with the `protocol` field set to `CacheService`.

A stream is initialized for a single server side scope, so the only caches accessible on a stream will be caches from the scope name used in the `InitRequest`. If a client needs to access caches in different scoped sessions on the server it must create and initialize multiple `ProxyService/subChannel` streams.

The current API version is `1` so the client must set the `protocolVersion` and `supportedProtocolVersion` fields to `1`.

## Ensure Cache

Before a client can interact with a cache, it **must** send an `EnsureCache` message and **must* wait for the response.
After the server has created the requested cache a `NamedCacheResponse` will be sent with the `cacheId` field set to the unique identifier that the proxy server is using for that cache.

All subsequent requests for that cache should be sent using a `NamedCacheRequest` containing the same `cacheId` value.

---
***IMPORTANT***

It is the clients responsibility to ensure that every `NamedCacheRequest` sent has a unique value for the correct value for the `cacheId` field corresponding to the cache the request is for. Requests sent will an invalid `cacheId` will receive an error response.  

---

Once an `EnsureCache` message has been sent, the client may receive cache lifecycle events for that cache. 
These will be for cache truncate and cache destroy. 

A client may ensure multiple cache using the same `ProxyService` stream.

### Cache Truncated Events

If the cache is truncated, the client will receive a `ProxyResponse` with a zero `id` field as this is an event not related to a request.
The `response` field of the `ProxyResponse` will contain an `Any` message wrapping a `NamedCacheResponse`. 
The `cacheId` field of the `NamedCacheResponse` will be set to the identifier of the truncated cache. 

### Cache Destroyed Events

If the cache is destroyed, the client will receive a `ProxyResponse` with a zero `id` field as this is an event not related to a request.
The `response` field of the `ProxyResponse` will contain an `Any` message wrapping a `NamedCacheResponse`. 
The `cacheId` field of the `NamedCacheResponse` will be set to the identifier of the destroyed cache.

---
***IMPORTANT***

Once a cache is destroyed, no further messages should be sent by the client for that cache. Doing so will result in an error response from the server. The client should not receive any further messages from the server related to this cache.
The client should clean up any state related to the destroyed cache, e.g. map listeners etc. 

---

## NamedCacheRequest Message Types

There are a number of values that can be specified in the `type` field of a `NamedCacheRequest`, each corresponding to a different NamedCache request.

### EnsureCache

Ensuring a cache is discussed above. A `NamedCacheRequest` message with a `type` field set to `EnsureCache` must have a `message` field containing an `Any` value wrapping an `EnsureCacheRequest`.
                   
An `EnsureCacheRequest` message contains a single `string` field, which is the name of the cache to ensure.

The response to an `EnsureCache` request is a `NamedCacheResponse` containing the `cacheId` for the new cache and an empty `message` field.

### Aggregate

An `Aggregate` request type is used to execute an aggregator on the server.
The aggregate message support all the different types of NamedCache aggregate method. Which method is executed depends on the content of the request message.

- `cache.aggregate(aggregator)`
- `cache.aggregate(keys, aggregator)`
- `cache.aggregate(filter, aggregator)`

An `Aggregate` request requires a `NamedCacheRequest` message with a `type` field set to `Aggregate` must have a `message` field containing an `Any` value wrapping an `ExecuteRequest`.

An `ExecuteRequest` has the following fields:

| Name    | Type           | Description                                                                                                       |
|---------|----------------|-------------------------------------------------------------------------------------------------------------------|
| `agent` | `bytes`        | The serialized aggregator to invoke                                                                               |
| `keys`  | `KeysOrFilter` | An optional field to specify the key, or keys, or filter to use to select the entries to invoke the aggregator on |

If the `keys` field is not set an `AlwaysFilter` will be used to execute the aggregator against all the entries in the cache.

A `KeysOrFilter` message contains one of the following:

| Name     | Type                      | Description                     |
|----------|---------------------------|---------------------------------|
| `key`    | `bytes`                   | A single serialized key         |                                                  
| `keys`   | `CollectionOfBytesValues` | A collection of serialized keys |                                                  
| `filter` | `bytes`                   | A serialized `Filter`           |    

An `Aggregate` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the aggregator result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the aggregator result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BytesValue`. This `BytesValue` is the serialized result from executing the aggregator.

### Clear

A `Clear` message is sent to clear a cache, equivalent to calling `cache.clear()`.

A `Clear` request requires a `NamedCacheRequest` message with a `type` field set to `Clear` should not set the `message` field (if set it is ignored). 

After the cache has been cleared the client will a `ProxyResponse` containing a `Complete` message. No `NamedCacheResponse` is sent.

### ContainsEntry
                       
The `ContainsEntry` request type is used to determine whether the specified cache contains a specified key mapped to a specified value. This is used by the Java client to implement calls such as `cache.entrySet().contains(entry)`, where `entry` is a key/value pair in a `Map.Entry`. 

A `ContainsEntry` request requires a `NamedCacheRequest` message with a `type` field set to `ContainsEntry` should have a `message` field set to an `Any` message wrapping a `BinaryKeyAndValue` message. The `BinaryKeyAndValue` message contains two fields, `key` and `value` which should contain the serialized key and value to use.

A `ContainsEntry` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the contains entry result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the contains entry request.

### ContainsKey

The `ContainsKey` request type is used to determine whether the specified cache contains a specified key, equivalent to `cache.containsKey(key)` or `cache.keySet().contains(key)`.  

A `ContainsKey` request requires a `NamedCacheRequest` message with a `type` field set to `ContainsKey` should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized key.

A `ContainsKey` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the contains key result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the contains key request.

### ContainsValue

The `ContainsValue` request type is used to determine whether the specified cache contains a specified value, equivalent to `cache.containsValue(value)` or `cache.values().contains(value)`.  

A `ContainsValue` request requires a `NamedCacheRequest` message with a `type` field set to `ContainsValue` should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized value.

A `ContainsValue` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the contains value result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the contains value request.

### Destroy

A `Destroy` message is sent to destroy a cache, equivalent to calling `cache.destroy()`.

A `Destroy` request requires a `NamedCacheRequest` message with a `type` field set to `Destroy`. 
The `message` field should not be set (if set it is ignored). 

After the cache has been destroyed the client will a `ProxyResponse` containing a `Complete` message. No `NamedCacheResponse` is sent. Sending a `Destroy` message should result in the client receiving a cache destroyed event message before the complete message is received.

### IsEmpty

An `IsEmpty` message is sent to determine whether a cache is empty or not, equivalent to calling `cache.isEmpty()`.

A `IsEmpty` request requires a `NamedCacheRequest` message with a `type` field set to `IsEmpty`. 
The `message` field should not be set (if set it is ignored). 

An `IsEmpty` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the boolean result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the contains value result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the isEmpty() request.

### IsReady

An `IsReady` message is sent to determine whether a cache is ready on the server or not, equivalent to calling `cache.isReady()`.

A `IsReady` request requires a `NamedCacheRequest` message with a `type` field set to `IsReady`.
The `message` field should not be set (if set it is ignored). 

An `IsReady` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the boolean result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the contains value result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` is the result of the isReady() request.

### Get
  
A `Get` message is sent to obtain the value from the cache for a specific key. It is used to implement methods such as `cahce.get(key)` or `cache.getOrDefault(key, value)`.

A `Get` request requires a `NamedCacheRequest` message with a `type` field set to `Get` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized value.

A `Get` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the result, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the get result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `OptionalValue`. This `OptionalValue` is the result of the get request. An `OptionalValue` is like a Java `Optional` as it has a `getPresent()` method that allows the caller to know whether the key was actually present in the cache, if so the `getValue()` method will return the serialized cache value.
       
### GetAll

A `GetAll` message is sent to obtain the values from the cache for a specific set of keys. It is used to implement methods such as `cahce.getAll(keySet)`.

A `GetAll` request requires a `NamedCacheRequest` message with a `type` field set to `GetAll` and should have a `message` field set to an `Any` message wrapping a `CollectionOfBytesValues` message. The `CollectionOfBytesValues` should contain the set of serialized keys to get from the cache.
                                              
A `GetAll` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
There will be zero or more `Message` responses containing a `NamedCacheResponse` for each key that is present in the cache.
These will be followed by a `ProxyResponse` containing a `Complete` response.

Each `NamedCacheResponse` will have a message field which is an `Any` wrapping a `BinaryKeyAndValue` message, which will contain the serialized key and value from the cache.

### Index

An `Index` request is used to add or remove an index.

An `Index` request requires a `NamedCacheRequest` message with a `type` field set to `Index` and should have a `message` field set to an `Any` message wrapping a `IndexRequest` message.

An `IndexRequest` contains the following fields:

| Name         | Type    | Description                                                                                                                            |
 |--------------|---------|----------------------------------------------------------------------------------------------------------------------------------------|
 | `add`        | `bool`  | A boolean field to indicate whether this is an "addIndex" request (true) or "removeIndex" request (false).                             |
| `extractor`  | `bytes` | The serialized `ValueExtractor` to use to create the index.                                                                            |
| `sorted`     | `bool`  | A boolean value, set to `true` if the index should be sorted. This field is optional and ignored for a remove request.                 |
| `comparator` | `bytes` | A bytes value containing the serialized comparator to use for a sorted index. This field is optional and ignored for a remove request. |

The response for an `Index` request will just be a `ProxyResponse` containing a `Complete` message.

### Invoke

An `Invoke` message is sent to execute an entry processor in a cache. It is used to implement methods such as `cahce.invoke(key, processor)` or `cache.invokeAll(keys, processor)` or `cache.invoke(filter, processor)` etc.

An `Invoke` request requires a `NamedCacheRequest` message with a `type` field set to `Invoke` and should have a `message` field set to an `Any` message wrapping a `ExecuteRequest` message. 

An `ExecuteRequest` has the following fields:

| Name    | Type           | Description                                                                                                            |
|---------|----------------|------------------------------------------------------------------------------------------------------------------------|
| `agent` | `bytes`        | The serialized entry processor to invoke                                                                               |
| `keys`  | `KeysOrFilter` | An optional field to specify the key, or keys, or filter to use to select the entries to invoke the entry processor on |

If the `keys` field is not set an `AlwaysFilter` will be used to execute the entry processor against all the entries in the cache.

A `KeysOrFilter` message contains one of the following:

| Name     | Type                      | Description                     |
|----------|---------------------------|---------------------------------|
| `key`    | `bytes`                   | A single serialized key         |                                                  
| `keys`   | `CollectionOfBytesValues` | A collection of serialized keys |                                                  
| `filter` | `bytes`                   | A serialized `Filter`           |    

An `Invoke` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
There will be zero or more responses of type `Message` containing a `NamedCacheResponse` for each entry the entry processor was invoked on.
These will be followed by a `ProxyResponse` containing a `Complete` response to signal the end of the responses.

The `NamedCacheResponse` messages will contain a `message` field containing an `Any` value wrapping a `BinaryKeyAndValue`.
The `BinaryKeyAndValue` contains the serialized cache key and corresponding entry processor result for that entry.

### PageOfEntries
              
A `PageOfEntries` request is used to obtain the entries in a cache, one page (partition) at a time. 
This is used to iterator over cache entries, for example `cache.entrySet()` or `cache.values()`.

An `PageOfEntries` request requires a `NamedCacheRequest` message with a `type` field set to `PageOfEntries` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. 
The `BytesValue` is the cookie to identify the page to return. 
It should be empty (or not set) to retrieve the first page. 
Subsequent `PageOfEntries` requests should pass the cookie returned from the previous page.

An `PageOfEntries` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
- The first `ProxyResponse` will have a type of `Message` containing a `NamedCacheResponse` with its `message` field set to an `Any` containing a `BytesValue`. This is the cookie to use to request the next page. 
- There will then be zero or more responses of type `Message` containing a `NamedCacheResponse` for each entry in the current page. Each of these will contain a `message` field set to an `Any` wrapping a `BinaryKeyAndValue`, which contains the serialized cache key and value.
- These will be followed by a `ProxyResponse` containing a `Complete` response to signal the end of the responses.

### PageOfKeys

A `PageOfKeys` request is used to obtain the keys in a cache, one page (partition) at a time. 
This is used to iterator over cache keys, for example `cache.keySet()`.

An `PageOfKeys` request requires a `NamedCacheRequest` message with a `type` field set to `PageOfKeys` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. 
The `BytesValue` is the cookie to identify the page to return. 
It should be empty (or not set) to retrieve the first page. 
Subsequent `PageOfKeys` requests should pass the cookie returned from the previous page.

An `PageOfKeys` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
- The first `ProxyResponse` will have a type of `Message` containing a `NamedCacheResponse` with its `message` field set to an `Any` containing a `BytesValue`. This is the cookie to use to request the next page. 
- There will then be zero or more responses of type `Message` containing a `NamedCacheResponse` for each entry in the current page. Each of these will contain a `message` field set to an `Any` wrapping a `BytesValue`, which contains a serialized cache key.
- These will be followed by a `ProxyResponse` containing a `Complete` response to signal the end of the responses.

### Put

A `Put` message is sent to insert or update an entry in a cache.
This is used to implement methods such as `cache.put(key, value)` or `cache.put(key, value, expiry)`.

A `Put` request requires a `NamedCacheRequest` message with a `type` field set to `Put` and should have a `message` field set to an `Any` message wrapping a `PutRequest` message.

A `PutRequest` contains the following fields:

| Name      | Type    | Description                                              |
|-----------|---------|----------------------------------------------------------|
| `key`     | `bytes` | The serialized key                                       |                                          
| `value`   | `bytes` | A serialized value                                       |                                          
| `ttl`     | `int64` | An optional expiry value (in milliseconds) for the entry |    

A `Put` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the previous cache value, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the put result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BytesValue`. This `BytesValue` is the serialized previous cache value (or may be empty if there was no cache value).

### PutAll

A `PutAll` message is sent to insert or update a number of entries in a cache.
This is used to implement methods such as `cache.putAll(map)`.

A `PutAll` request requires a `NamedCacheRequest` message with a `type` field set to `PutAll` and should have a `message` field set to an `Any` message wrapping a `PutAllRequest` message.

A `PutAllRequest` contains the following fields:

| Name      | Type                | Description                                                                                                                                         |
|-----------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `entries` | `BinaryKeyAndValue` | A repeated field that contains on or more `BinaryKeyAndValue` messages. Each `BinaryKeyAndValue` contains a serialized key and value to add/update. |                                          
| `ttl`     | `int64`             | An optional expiry value (in milliseconds) for all the entries                                                                                      |    
After the cache has been updated the client will receive a `ProxyResponse` containing a `Complete` message. No `NamedCacheResponse` is sent.

### PutIfAbsent

A `PutIfAbsent` message is sent to insert or update an entry in a cache.
This is used to implement methods such as `cache.putIfAbsent(key, value)`.

A `PutIfAbsent` request requires a `NamedCacheRequest` message with a `type` field set to `PutIfAbsent` and should have a `message` field set to an `Any` message wrapping a `PutRequest` message.

A `PutRequest` contains the following fields:

| Name      | Type    | Description                                              |
|-----------|---------|----------------------------------------------------------|
| `key`     | `bytes` | The serialized key                                       |                                          
| `value`   | `bytes` | A serialized value                                       |                                          
| `ttl`     | `int64` | An optional expiry value (in milliseconds) for the entry |    

A `PutIfAbsent` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the previous cache value, followed by a `ProxyResponse` containing a `Complete` message.

The `NamedCacheResponse` containing the putIfAbsent result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BytesValue`. This `BytesValue` is the serialized current cache value (or may be empty if there was no current cache value).

### QueryEntries

A `QueryEntries` request is used to query the cache for a set of entries. 
This is used to implement methods such as `cache.entrySet(filter)` or `cache.entrySet(filter, comparator)`.

A `QueryEntries` request requires a `NamedCacheRequest` message with a `type` field set to `QueryEntries` and should have a `message` field set to an `Any` message wrapping a `QueryRequest` message.

A `QueryRequest` contains the following fields:

| Name         | Type    | Description                       |
|--------------|---------|-----------------------------------|
| `filter`     | `bytes` | An optional serialized filter     |                                          
| `comparator` | `bytes` | An optional serialized comparator |                                          

If the `filter` field is not set an `AlwaysFilter` will be used to execute the cache query. This may be unwise for a large cache.
If the `comparator` field is not set the results will be unsorted.

A `QueryEntries` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
- There will be zero or more `Message` responses containing a `NamedCacheResponse` for each entry that is present in the cache. Each `NamedCacheResponse` will have a message field which is an `Any` wrapping a `BinaryKeyAndValue` message, which will contain the serialized key and value from the cache.
- Finally, there will be followed by a `ProxyResponse` containing a `Complete` response to indicate the end of the stream of results.

### QueryKeys

A `QueryKeys` request is used to query the cache for a set of keys. 
This is used to implement methods such as `cache.keySet(filter)` or `cache.keySet(filter, comparator)`.

A `QueryKeys` request requires a `NamedCacheRequest` message with a `type` field set to `QueryKeys` and should have a `message` field set to an `Any` message wrapping a `QueryRequest` message.

A `QueryRequest` contains the following fields:

| Name         | Type    | Description                       |
|--------------|---------|-----------------------------------|
| `filter`     | `bytes` | An optional serialized filter     |                                          
| `comparator` | `bytes` | An optional serialized comparator |                                          

If the `filter` field is not set an `AlwaysFilter` will be used to execute the cache query. This may be unwise for a large cache.
If the `comparator` field is not set the results will be unsorted.

A `QueryKeys` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
- There will be zero or more `Message` responses containing a `NamedCacheResponse` for each key that is present in the cache. Each `NamedCacheResponse` will have a message field which is an `Any` wrapping a `BytesValue` message, which will contain the serialized key from the cache.
- Finally, there will be followed by a `ProxyResponse` containing a `Complete` response to indicate the end of the stream of results.

### QueryValues

A `QueryValues` request is used to query the cache for a collections of cache values. 
This is used to implement methods such as `cache.values(filter)` or `cache.values(filter, comparator)`.

A `QueryValues` request requires a `NamedCacheRequest` message with a `type` field set to `QueryValues` and should have a `message` field set to an `Any` message wrapping a `QueryRequest` message.

A `QueryRequest` contains the following fields:

| Name         | Type    | Description                       |
|--------------|---------|-----------------------------------|
| `filter`     | `bytes` | An optional serialized filter     |                                          
| `comparator` | `bytes` | An optional serialized comparator |                                          

If the `filter` field is not set an `AlwaysFilter` will be used to execute the cache query. This may be unwise for a large cache.
If the `comparator` field is not set the results will be unsorted.

A `QueryValues` request will result in multiple `ProxyResponse` responses for the request (similar to a server streaming response). 
- There will be zero or more `Message` responses containing a `NamedCacheResponse` for each value that is present in the cache. Each `NamedCacheResponse` will have a message field which is an `Any` wrapping a `BytesValue` message, which will contain the serialized value from the cache.
- Finally, there will be followed by a `ProxyResponse` containing a `Complete` response to indicate the end of the stream of results.

### Remove

A `Remove` request is used to remove an entry from a cache for a specific key, for example `cache.remove(key)`

A `Remove` request requires a `NamedCacheRequest` message with a `type` field set to `Remove` and should have a `message` field set to an `Any` message wrapping a `BytesValue` message. The `BytesValue` should contain the serialized key of the entry to remove.

A `Remove` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the previous cache value, followed by a `ProxyResponse` containing a `Complete` message. The `NamedCacheResponse` containing the remove result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BytesValue`. This `BytesValue` is the serialized removed cache value (or may be empty if there was no cache value).

### RemoveMapping

A `RemoveMapping` request is used to remove an entry from a cache for a specific key if that key is mapped to a specific value, for example `cache.remove(key, value)`

A `RemoveMapping` request requires a `NamedCacheRequest` message with a `type` field set to `RemoveMapping` and should have a `message` field set to an `Any` message wrapping a `BinaryKeyAndValue` message. The `BinaryKeyAndValue` should contain the serialized key and serialized value of the entry to remove.

A `RemoveMapping` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse`, followed by a `ProxyResponse` containing a `Complete` message. The `NamedCacheResponse` containing the remove mapping result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` will be `true` if the entry was removed, or `false` if it was not removed.

### Replace

A `Replace` request is used to replace an entry in a cache for a specific key if that key is present and mapped to any value, for example `cache.replace(key, value)`

A `Replace` request requires a `NamedCacheRequest` message with a `type` field set to `Replace` and should have a `message` field set to an `Any` message wrapping a `BinaryKeyAndValue` message. The `BinaryKeyAndValue` should contain the serialized key and serialized value of the entry to replace.

A `Replace` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse` containing the previous cache value, followed by a `ProxyResponse` containing a `Complete` message. The `NamedCacheResponse` containing the remove result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BytesValue`. This `BytesValue` is the serialized previous cache value.

### ReplaceMapping

A `ReplaceMapping` request is used to replace an entry in a cache for a specific key if that key is present and mapped to a specific value, for example `cache.replace(key, oldValue, newValue)`

A `ReplaceMapping` request requires a `NamedCacheRequest` message with a `type` field set to `Replace` and should have a `message` field set to an `Any` message wrapping a `ReplaceMappingRequest` message. The `ReplaceMappingRequest` should contain the serialized key, the serialized expected cache value and the serialized new cache value.

A `RemoveMapping` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse`, followed by a `ProxyResponse` containing a `Complete` message. The `NamedCacheResponse` containing the replace mapping result will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `BoolValue`. This `BoolValue` will be `true` if the entry was replaced, or `false` if it was not replaced.

### Size

An `Size` message is sent to determine the number of entries in a cache, equivalent to calling `cache.size()`.

A `Size` request requires a `NamedCacheRequest` message with a `type` field set to `Size`.
The `message` field should not be set (if set it is ignored). 

A `Size` request will result in a `ProxyResponse` wrapping a `NamedCacheResponse`, followed by a `ProxyResponse` containing a `Complete` message.
The `NamedCacheResponse` response will have a `type` field set to `Message` and a `message` field containing an `Any` value wrapping a `Int32Value`. This `Int32Value` is the result of the size() request.

### Truncate

A `Truncate` message is sent to truncate a cache, equivalent to calling `cache.truncate()`.

A `Truncate` request requires a `NamedCacheRequest` message with a `type` field set to `Truncate`. The `message` field should not be set (if set it is ignored). 

After the cache has been destroyed the client will a `ProxyResponse` containing a `Complete` message. No `NamedCacheResponse` is sent. Sending a `Truncate` message should result in the client receiving a cache truncated event message before the complete message is received.

   
## MapListeners and MapEvents
                        
For a client to receive cache `MapEvent` messages it must register map listeners with the server.
The map listener itself is just a client side structure and is not sent to the server. 
The server only requires either has a key or filter to specify the entries the client wants events for.

After a client registers a listener for cache events, it will receive event messages when the cache content changes.
Only one event will be sent to the client regardless of how many listeners the client has that match the event.
It is up to the client to keep track of listeners and route received events to the listeners, this is covered below.

### MapListener Request

A `MapListener` request is used to add or remove a map listener.

| Name          | Type          | Description                                                                                                                                                                  |
|---------------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `subscribe`   | `bool`        | A flag indicating whether to subscribe to (true) or unsubscribe from (false) events.                                                                                         |
| `keyOrFilter` | `KeyOrFilter` | The optional serialized key, or serialized Filter, to identify the entry (or entries) to subscribe to. If neither key nor filter are set then an Always filter will be used. |
| `filterId`    | `int64`       | For filter listeners this will be a unique identifier that the client will use to identify the listener                                                                      |
| `lite`        | `bool`        | A flag set to true to indicate that the MapEvent objects do not have to include the OldValue and NewValue property values in order to allow optimizations                    |
| `synchronous` | `bool`        | `true` if the listener is synchronous                                                                                                                                        |
| `priming`     | `bool`        | `true` if this is a priming listener                                                                                                                                         |
| `trigger`     | `bytes`       | An optional serialized MapTrigger                                                                                                                                            |
### Add a Key Listener

### Add a Filter Listener
                
### MapEvent Message Handling

When the server sends an event to the client, the client will receive a `ProxyResponse`.
The `ProxyResponse` request id will not be set, as the response does not correspond to a request.
The `ProxyResponse` will have its `message` field set to an `Any` message wrapping a `NamedCacheResponse` containing the event.
       
The `NamedCacheResponse` will have a `cacheId` to identify the cache the event is for.
The `NamedCacheResponse` will have a `type` field set to `MapEvent`.
The `NamedCacheResponse` will have a `message` field containing an `Any` message wrapping a `MapEventMessage`.
The `MapEventMessage` contains the details fo the event.

A `MapEventMessage` has the following fields:

| Name                  | Type                  | Description                                                                                                                                                                                                           |
|-----------------------|-----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                  | `int32`               | The id (type) of the event (see the JavaDoc for MapEvent in Coherence).                                                                                                                                               |      
| `key`                 | `bytes`               | The serialized key that the event is for                                                                                                                                                                              |      
| `newValue`            | `bytes`               | The serialized new value for the cache entry                                                                                                                                                                          |      
| `oldValue`            | `bytes`               | The serialized previous value for the cache entry                                                                                                                                                                     |      
| `transformationState` | `TransformationState` | An enum representing the transformation state of the event                                                                                                                                                            |      
| `filterIds`           | `int64`               | If the event matches a filter listener this will be the filter identifier the client sent when registering the listener. This field is an array, so there will be multiple ids if the event matches multiple filters. |      
| `synthetic`           | `bool`                | A flag indicating whether the event is synthetic or not                                                                                                                                                               |      
| `priming`             | `bool`                | A flag indicating whether this is a priming event                                                                                                                                                                     |      
| `expired`             | `bool`                | A flag indicating whether the event is due to an entry expiring                                                                                                                                                       |      
| `versionUpdate`       | `bool`                | true iff this event is caused by a synthetic version update sent by the server to notify clients of the current version                                                                                               |      

### Routing Events to Key Listeners

When an event is received by the client, it should be routed to all listeners that registered specifically for the key.
The client can simply keep a Map of key listeners mapped by key to easily find the listeners corresponding to the event.

### Routing Events to Filter Listeners
                            
When an event is received, it may contain zero or more `filterIds`. 
These identifiers are the unique identifiers the client sent to the server when adding the listener. 
The client should route the event to the listeners with the corresponding identifiers.
The client can simply keep a Map of filter listeners mapped by the identifier to easily find the listeners corresponding to the event.

### Synchronous or Non-Synchronous Listeners

For synchronous listeners the events should be routed to the listener on the same thread the event was received on.
For all other non-synchronous listeners events can be handed off to a dispatcher thread. Like in Coherence Java there should only be one event dispatcher thread for a cache, otherwise events could be routed out of order.
