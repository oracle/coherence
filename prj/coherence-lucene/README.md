# Coherence Lucene Integration

A library that integrates Oracle Coherence with Apache Lucene to provide distributed full-text search capabilities across cached data. This integration enables sophisticated ranking and flexible query support across a distributed cache.

## Features

- Distributed Lucene indexing for full-text searches over Coherence caches
- Flexible configuration with a fluent builder API
- Supports custom analyzers and index settings

## Getting Started

### Prerequisites

- Java 17 or later
- Oracle Coherence CE 25.09 or later
- Apache Lucene 10.x

### Maven Dependencies

```xml
<dependencies>
    <!-- Oracle Coherence Lucene -->
    <dependency>
        <groupId>com.oracle.coherence.ce</groupId>
        <artifactId>coherence-lucene</artifactId>
        <version>25.09</version>
    </dependency>
</dependencies>
```

## Main Components

### LuceneIndex
A distributed, partitioned Lucene index for Coherence caches. Maintains a separate Lucene index for each cache partition and supports custom analyzers, directory storage, and index writer configuration.

#### Example: Adding a Lucene index to your cache
```java
static final ValueExtractor<Document, String> CONTENT = ValueExtractor.of(Document::getContent);

NamedMap<Long, Document> documents = coherence.getSession().getMap("documents");
documents.addIndex(new LuceneIndex<>(CONTENT));
```

#### Index Configuration

The `LuceneIndex` can be configured using a fluent builder API to customize various aspects of the index:

##### Index Writer Configuration

Control the index writing behavior and performance settings:

```java
// Configure custom merge policy
cache.addIndex(new LuceneIndex<>(CONTENT)
    .configureIndexWriter(config -> {
        LogByteSizeMergePolicy policy = new LogByteSizeMergePolicy();
        policy.setMinMergeMB(32.0);
        policy.setMaxMergeMB(256.0);
        policy.setMergeFactor(5);
        config.setMergePolicy(policy);
    }));

// Configure RAM buffer size and other settings
cache.addIndex(new LuceneIndex<>(CONTENT)
    .configureIndexWriter(config -> {
        config.setRAMBufferSizeMB(64.0);
        config.setMaxBufferedDocs(10000);
        config.setUseCompoundFile(true);
    }));

// Default uses Lucene's default settings
cache.addIndex(new LuceneIndex<>(CONTENT));
```

The index writer configuration allows you to fine-tune:
- Merge policies and factors
- RAM buffer size
- Compound file usage
- Maximum buffered documents
- Other Lucene-specific optimizations

##### Text Analysis

Configure how text is analyzed and indexed:

```java
// Use custom analyzer
cache.addIndex(new LuceneIndex<>(CONTENT)
    .analyzer(() -> new CustomAnalyzer()));

// Use language-specific analyzer
cache.addIndex(new LuceneIndex<>(CONTENT)
    .analyzer(() -> new FrenchAnalyzer()));

// Default is StandardAnalyzer
cache.addIndex(new LuceneIndex<>(CONTENT));
```

The analyzer configuration uses a supplier to ensure proper serialization when the configuration is distributed to cluster members.

##### Directory Configuration

Control where and how the Lucene index files are stored:

```java
// Use memory-mapped files (better for large indices)
cache.addIndex(new LuceneIndex<>(CONTENT)
    .directory(partId -> new MMapDirectory(
        Path.of(".lucene", "partition-" + partId))));

// Use regular file system directory
cache.addIndex(new LuceneIndex<>(CONTENT)
    .directory(partId -> FSDirectory.open(
        Path.of(".lucene", "partition-" + partId))));

// Default is ByteBuffersDirectory (on-heap)
cache.addIndex(new LuceneIndex<>(CONTENT));
```

The directory configuration receives a partition ID and must return a unique directory instance for each partition. This ensures proper isolation between partition indices.

##### Search Configuration

Customize how searches are performed by configuring a custom searcher supplier:

```java
// Use custom similarity scoring
cache.addIndex(new LuceneIndex<>(CONTENT)
    .searcher((cur, prev) -> {
        IndexSearcher searcher = new IndexSearcher(cur);
        searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));
        return searcher;
    }));

// Implement index warming
cache.addIndex(new LuceneIndex<>(CONTENT)
    .searcher((cur, prev) -> {
        IndexSearcher searcher = new IndexSearcher(cur);
        if (prev != null) {
            // Warm new searcher using popular terms from previous reader
            searcher.warmUp();
        }
        return searcher;
    }));

// Default uses standard Lucene similarity scoring
cache.addIndex(new LuceneIndex<>(CONTENT));
```

The searcher supplier allows you to customize various aspects of search behavior:
- Similarity scoring algorithms
- Index warming and caching strategies
- Custom collector implementations
- Field boost factors
- Result ranking customization

The supplier receives both the current and previous IndexReader instances, enabling optimizations such as:
- Warming new searchers with popular terms from the previous reader
- Maintaining custom caches across reader transitions
- Implementing sophisticated index warming strategies
- Monitoring index changes between readers

##### Combining Options

All configuration options can be combined as needed:

```java
cache.addIndex(new LuceneIndex<>(CONTENT)
    .analyzer(() -> new StandardAnalyzer())
    .directory(partId -> new MMapDirectory(Path.of(".lucene", "partition-" + partId)))
    .searcher((cur, prev) -> {
        IndexSearcher searcher = new IndexSearcher(cur);
        searcher.setSimilarity(new BM25Similarity(1.2f, 0.75f));
        return searcher;
    })
    .configureIndexWriter(config -> {
        config.setRAMBufferSizeMB(64.0);
        config.setMaxBufferedDocs(10000);
        config.setUseCompoundFile(true);
    })        
);
```

### LuceneSearch
A distributed aggregator for full-text search across Coherence partitions using Lucene. Executes Lucene queries in parallel, collects and normalizes results, and supports limiting results using standard Coherence filters.

#### Example: Executing a search
```java
LuceneQueryParser parser = LuceneQueryParser.create(Document::getContent);
Query query = parser.parse("How can I implement PortableType in Coherence?");

List<QueryResult<Long, Document>> results = cache.aggregate(
    new LuceneSearch<>(CONTENT, query, 10));  // top 10 results

for (QueryResult<Long, Document> result : results) {
    System.out.printf("Score: %.2f | ID: %s | Doc: %s%n",
        result.getScore(),
        result.getKey(),
        result.getValue());
}
```

#### Search Configuration

The `LuceneSearch` aggregator inherits its configuration from the `LuceneIndex` being searched. The only required configuration when creating a `LuceneSearch` instance is:

- The extractor for the property to search
- The search query 
- The maximum number of results to return

Beyond these required arguments, the only additional configuration option is the ability to limit the search scope, which is covered in the next section.

#### Limiting Search Scope

If you would like to limit the scope of the search, and reduce the number of Lucene indexes that must be queried, you can do so via standard Coherence `Filter`. 

```java
Filter<Document> filterByAuthor = Filters.equal(Document::getAuthor, "Mark Twain");

List<QueryResult<Long, Document>> results = cache.aggregate(
    filterByAuthor,                                                  // pre-filter partitions to search      
    new LuceneSearch<>(CONTENT, query, 10).filter(filterByAuthor));  // post-filter Lucene results

// Print results
for (QueryResult<Long, Document> result : results) {
    System.out.printf("Score: %.2f | ID: %s | Doc: %s%n",
        result.getScore(),
        result.getKey(),
        result.getValue());
}
```

The reason you need to provide filter twice, both as a parameter to the `aggregate` call, and as an option of the `LuceneSearch` aggregator, is because it is used multiple times: 

1. For pre-filtering, in order to limit the number of partitions, and thus the number of Lucene indices to search to only those that contain documents that satisfy the criteria, and
2. For post-filtering, to eliminate Lucene results that do not satisfy the criteria

### LuceneQueryParser
A fluent, flexible, and thread-safe builder for Lucene queries. Recommended for robust query construction. Supports multiple fields, boosts, analyzers, stop words, synonyms (map, Solr, WordNet), preprocessing, and custom parser configuration.

#### Example: Basic and Advanced Usage
```java
// Basic usage
LuceneQueryParser parser = LuceneQueryParser.create(Document::getContent);
Query query = parser.parse("search terms");

// Advanced usage
LuceneQueryParser parser = LuceneQueryParser.builder()
    .field(Document::getTitle, 2.0f)
    .field(Document::getBody)
    .stopWords(Set.of("the", "and"))
    .synonyms(Map.of("car", List.of("automobile", "vehicle")))
    .configureParser(qp -> qp.setDefaultOperator(QueryParser.Operator.AND))
    .build();
Query query = parser.parse("fast car");
```

#### Using Synonyms from a Solr-Format File
```java
LuceneQueryParser parser = LuceneQueryParser.builder()
    .field(Document::getBody)
    .synonyms(LuceneQueryParser.solrSynonyms(Paths.get("synonyms.txt"), true))
    .build();
```

#### Using Synonyms from a WordNet-Format File
```java
LuceneQueryParser parser = LuceneQueryParser.builder()
    .field(Document::getBody)
    .synonyms(LuceneQueryParser.wordNetSynonyms(Paths.get("wordnet.txt")))
    .build();
```

#### Custom Preprocessing
```java
LuceneQueryParser parser = LuceneQueryParser.builder()
    .field(Document::getBody)
    .preprocessor(q -> q.replaceAll("\\d", "")) // Remove digits
    .build();
```

#### Custom QueryParser Injection
```java
QueryParser custom = new MyCustomQueryParser("body", new StandardAnalyzer());
LuceneQueryParser parser = LuceneQueryParser.builder()
    .parser(custom)
    .build();
```

## License

This project is licensed under the Universal Permissive License (UPL) 1.0 - see the [LICENSE](LICENSE) file for details.

## Contributing

We welcome contributions! Please feel free to submit a Pull Request.
