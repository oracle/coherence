# Coherence AI Module

This module provides Vector Database functionality on top of Oracle Coherence.

## What is a Vector DB

There are many online articles that describe in detail what a Vector DB is and what it does.
Briefly, a Vector DB stores vectors, which can be though of as arrays of numeric values.
In the case of the Coherence AI module, vectors are stored with a key and optional metadata.

As well as storing vectors, a VectorDB allows queries to be made against the vector data.
These queries are typically operations such as nearest neighbour queries, often known as kNN queries.
A kNN query will take a specific vector and then find a number (k) of nearest neighbours (NN) to that
vector from the vectors stored in the DB.

There are many different algorithms to determine how similar one vector is to another.
The Coherence AI module implements some of these algorithms.

## The Coherence AI VectorStore

The `VectorStore` provides the entry point for vector store functionality. 
Coherence AI stores vectors as arrays of Java primitives, `double[]`, `float[]`, `int[]`, `long[]` and `short[]`.
A `VectorStore` is generically typed using the vector primitive array type, the vector key type and the vector 
metadata type.

```java
/**
 * @param <V>  the type of the store (this will always be a primitive array type)
 * @param <K>  the type of the key
 * @param <M>  the type of the metadata
 */
public interface VectorStore<V, K, M>
    {
    }
```

### Creating a VectorStore

A `VectorStore` is backed by a Coherence `NamedMap` which stores the vector data.
The `VectorStore` interface has various factory methods on it to create vector stores of different types.

For example, a `VectorStore` to store `float[]` vectors using a `Long` as the key and a `String` as the 
metadata can be created like this:

```java
VectorStore<float[], Long, String> store = VectorStore.ofFloats("my-store");
```

The vector data will be stored in a `NamedMap` named `my-store` obtained from the default `Session`.

It is possible to specify a `Session` when creating a vector store, in which case the `NamedMap`
will be obtained from that `Session`

```java
VectorStore<float[], Long, String> store = VectorStore.ofFloats("my-store", session);
```
              
The local `VectorStore` instance is stateless, so applications can create multiple instances of a
`VectorStore` over the same `NamedMap` without worrying about mismatched local state.

### Add Vectors to a VectorStore

There are a number of different methods to add data to a `VectorStore`.
The simplest is just to add a simple primitive array:

For example, adding a simple `float[]` to a float `VectorStore` with the key `123L` and metadata `"foo"`
```java
VectorStore<float[], Long, String> store = VectorStore.ofFloats("my-store");

float[] vector = new float[]{1.0f, 2.0f, 3.0f};

store.add(123L, vector, "foo");
```

Or the vector can be added without metadata, even though the store has a metadata type:
```java
store.add(123L, vector);
```


The `VectorStore` has methods to add all types of primitive vector to a store.
If the vector is not of the same type as the underlying store it will be up-cast or down-cast to the correct type.
It is up to the developer to ensure that this casting does not alter the data.
For example, calling `store.addDoubles()` on a `VectorStore` of `float[]` will downcast the `double` values
to `float` values. This will be fine if all the values in the double vector are within the range for a valid float.
If they are outside this range they will be truncated, as they would for any normal java cast.

There are also methods to add vectors to a store in bulk, which can be more efficient than single calls.
     
### Query a VectorStore

The `VectorStore` has a `query` method to perform different types of query on the vectors.
The `query` method takes a `SimilarityQuery` instance as its parameter, which defines the query to execute.
The Coherence AI module will have a number of built-in queries for different kNN algorithms.

For example a Jaccard similarity query can be run on a store of `long[]` vectors like this:

```java
VectorStore<long[], Integer, Void> store = VectorStore.ofFloats("my-store");

long[] testVector = new long[]{1L, 2L, 3L};

Jaccard<long[]> query = Jaccard.forLongs(testVector).withMaxResults(100).build();

List<QueryResult<long[], Integer, Void>> results = store.query(query);
```

The query will return the 100 nearest neighbours to the `testVector`, or less than 100 if there are not 100 vectors in the store.

Another example using a Cosine similarity query (also called "Angular" query), 
the code is almost identical but this time the vectors are
`float[]` and the query created is a `Cosine` query.

```java
VectorStore<float[], Integer, Void> store = VectorStore.ofFloats("my-store");

float[] testVector = new float[]{0.1f, 0.2f, 0.3f};

Cosine<float[]> query = Cosine.forFloats(testVector).withMaxResults(100).build();

List<QueryResult<float[], Integer, Void>> results = store.query(query);
```

### Metadata

The metadata for a store can be any type that is serializable using the serializer configured for the 
underlying cache service. The metadata is also optional so can be set to `Void`.

A `VectorStore` without metadata can be created by using `Void` for the metadata generic argument like this:

```java
VectorStore<float[], Long, Void> store = VectorStore.ofFloats("my-store", session);
```

and then vectors added without specifying metadata.
```java
float[] vector = new float[]{1.0f, 2.0f, 3.0f};
store.addFloats(123L, vector);
```

## How Does Coherence Store Vectors

Vectors and their optional metadata are stored in a single cache.
The vector is stored as a Coherence `Binary` that wraps the memory representation of the underlying array.
This is very different to how Coherence normally stores data. The array is not serialized and deserialized
each time it is accessed, instead Java buffers are used to treat the binary blob of data as the correct array type.
This means accessing a vector is a more efficient, at the cost of slightly higher memory usage.
Given that most Vector DB usage is running queries and performing vector math on the arrays, being able to access them
faster without the cost of serialization is seen as a good tradeoff.

Storing the vectors this way allows then to be used directly with Java's primitive buffers, e.g. `FloatBuffer`, 
`LongBuffer` etc. These buffers wrap a portion of memory and access it as a primitive array.
It is simple to go from a Coherence `Binary` to a primitive buffer without necessarily copying the underlying data.
Ultimately in future it will be possible to switch to Java's `MemorySegment` model when that is out of preview. 
                                         
Metadata is stored as a decoration on the binary vector. This allows the metadata and vector to co-exist
easily in the same cache entry. It also allows the metadata to be easily used in `Filter` queries
to restrict vector searches. There is a cost of slightly more complex extraction of the metadata for queries,
but this is hidden from the end-user.
                              
This way of storing vectors means it is not possible to access a vector cache like a normal cache. It would be impossible for any of the normal Coherence serializers to work with the cache values. The cache keys are serialized as normal, so they could be accessed, but the values are not.

## Similarity Queries

The current implementation contains slow brute force examples of kNN queries.

### Cosine Similarity

cosine similarity is a measure of similarity between two non-zero vectors defined in an inner product space. Cosine similarity is the cosine of the angle between the vectors; that is, it is the dot product of the vectors divided by the product of their lengths. It follows that the cosine similarity does not depend on the magnitudes of the vectors, but only on their angle. The cosine similarity always belongs to the interval `[−1,1]`.
For example, two proportional vectors have a cosine similarity of 1, two orthogonal vectors have a similarity of 0, and two opposite vectors have a similarity of -1. In some contexts, the component values of the vectors cannot be negative, in which case the cosine similarity is bounded in `[0,1]`.
      
The `FloatBruteForceCosine` class is an implementation of cosine similarity that performs brute force calculations on `float` vector arrays.

The aggregator is a "top n" type aggregator, so it returns the requested maximum number of nearest neighbours.


The query is run like this:

```java
VectorStore<float[], Integer, Void> store = VectorStore.ofFloats("my-store");

float[] testVector = new float[]{0.1f, 0.2f, 0.3f};

Cosine<float[]> query = Cosine.forFloats(testVector).withMaxResults(100).build();

List<QueryResult<float[], Integer, Void>> results = store.query(query);
```


### Jaccard Similarity

A simple kNN query to build is a Jaccard similarity query.

Jaccard Similarity is a measure of similarity between two asymmetric binary vectors, or we can say a way to find the similarity between two sets. It is a common proximity measurement used to compute the similarity of two items, such as two text documents. The index ranges from 0 to 1. Range closer to 1 means more similarity in two sets of data.

It is denoted by J and is also referred as Jaccard Index, Jaccard Coefficient, Jaccard Dissimilarity, and Jaccard Distance. It is frequently used in Data Science and Machine Learning such as Text Mining, E-Commerce, Recommendation System, etc.

It is calculated by the formula:

Jaccard Similarity = (number of observations in both sets) / (number in either set)

or mathematically,

J(A, B) = |A∩B| / |A∪B|

If two datasets share exact same members then their Jaccard Similarity Index will be 1 and if there are no common members then Jaccard Similarity index will be 0.

Jaccard Similarity will tell us that how many features are similar to each other in the dataset.

 The Coherence AI `LongBruteForceJaccard` class is an implementation of this functionality.
 It performs the math above on `long[]` vectors.
 
The actual query is run by the `SimilarityAggregator` that wraps the `LongBruteForceJaccard` operation.
The aggregator is optionally run using a filter on the vector metadata.

The aggregator is a "top _n_" type aggregator, so it returns the requested maximum number of nearest neighbours.

The query is run like this:

```java
VectorStore<long[], Integer, Void> store = VectorStore.ofFloats("my-store");

long[] testVector = new long[]{1L, 2L, 3L};

Jaccard<long[]> query = Jaccard.forLongs(testVector).withMaxResults(100).build();

List<QueryResult<long[], Integer, Void>> results = store.query(query);
```

