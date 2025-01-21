<doc-view>

<h2 id="_vector_db">Vector DB</h2>
<div class="section">
<p>With the increased popularity of Gen AI, and Retrieval Augmented Generation (RAG) use cases in particular, the need for a way to store and search a large number of dense vector embeddings efficiently is larger than ever.</p>

<p>Coherence already provides a number of features that make this possible, such as efficient serialization format, filters and aggregators, which allow users to search across large data sets in parallel by leveraging all the CPU cores in a cluster, and gRPC proxy, which allows remote clients written in any supported language, including Python, to access data in a Coherence cluster efficiently.</p>

<p>This release adds the missing bits that turn Coherence into a full-fledged Vector Database:</p>

<ol style="margin-left: 15px;">
<li>
Built-in support for Vector Types
<ul class="ulist">
<li>
<p><code>float32</code>, <code>int8</code> and <code>bit</code> dense vectors of arbitrary dimension</p>

</li>
</ul>
</li>
<li>
Built-in support for Semantic Search, including
<ul class="ulist">
<li>
<p>HNSW indexing</p>

</li>
<li>
<p>Binary Quantization</p>

</li>
<li>
<p>Index-optimized Exact Searches</p>

</li>
<li>
<p>Metadata Filtering</p>

</li>
</ul>
</li>
<li>
Built-in support for Document Chunks, addressing a common RAG use case

</li>
<li>
Integration with <a id="" title="" target="_blank" href="https://www.langchain.com">LangChain</a> and <a id="" title="" target="_blank" href="https://docs.langchain4j.dev">LangChain4j</a>

</li>
<li>
Integration with <a id="" title="" target="_blank" href="https://docs.spring.io/spring-ai/reference/index.html">Spring AI</a>

</li>
</ol>
<p>The following sections provide more details about each of the features above.</p>


<h3 id="_vector_types">Vector Types</h3>
<div class="section">
<p>To support arbitrary vector types, Coherence provides <code>com.oracle.coherence.ai.Vector&lt;T&gt;</code> interface, with three built-in implementations:</p>

<ol style="margin-left: 15px;">
<li>
<code>BitVector</code>, which internally uses a <code>java.util.Bitset</code> to represent each vector element using a single bit,

</li>
<li>
<code>Int8Vector</code>, which internally uses a <code>byte[]</code>, and

</li>
<li>
<code>Float32Vector</code>, which internally uses a <code>float[]</code>.

</li>
</ol>
<p>These types allow users to add a vector property to their own classes the same way they would add any other property: by simply creating a field and accessors for it:</p>

<markup
lang="java"
title="Book.java"
>@PortableType(id = 2001)
public class Book
    {
    @Portable private String isbn;
    @Portable private String title;
    @Portable private String author;
    @Portable private String summary;
    @Portable private Vector&lt;float[]&gt; summaryEmbedding;

    // constructors, getters and setters omitted
    }</markup>

<p>In the example above, the <code>summaryEmbedding</code> field is used to store vector representation of the <code>summary</code> field, so we can use vector similarity to search book summaries.</p>

<p>In the subsequent sections, we&#8217;ll discuss how the <code>summaryEmbedding</code> property can be used to define both standard and vector indexes, and to perform similarity search against them.</p>

</div>

<h3 id="_performing_similarity_search">Performing Similarity Search</h3>
<div class="section">
<p>To perform similarity search against vectors stored in Coherence you can use <code>SimilaritySearch</code> aggregator. The easiest way to construct one is by using <code>Aggregators.similaritySearch</code> factory method.</p>

<p>You need to specify three arguments when constructing the <code>SimilaritySearch</code> aggregator:</p>

<ol style="margin-left: 15px;">
<li>
A <code>ValueExtractor</code> that should be used to retrieve the vector attribute from the map entries

</li>
<li>
The search vector to compare the extracted values with, and

</li>
<li>
The maximum number of the results to return

</li>
</ol>
<p>For example, to search the map containing <code>Book</code> objects, and return up to 10 most similar books, you would create <code>SimilaritySearch</code> aggregator instance like this:</p>

<markup
lang="java"

>var searchVector = createEmbedding(searchQuery);  // outside of Coherence
var search       = Aggregators.similaritySearch(Book::getSummaryEmbedding, searchVector, 10);</markup>

<p>By default, the aggregator will use <em>cosine distance</em> to calculate distance between vectors, but you can change that by calling fluent <code>algorithm</code> method on the created aggregator instance and passing an instance of a different <code>DistanceAlgorithm</code> implementation:</p>

<markup
lang="java"

>var search = Aggregators.similaritySearch(Book::getSummaryEmbedding, searchVector, 10)
                        .algorithm(new L2SquaredDistance());</markup>

<p>Out of the box Coherence provides <code>CosineDistance</code>, <code>L2SquaredDistance</code> and <code>InnerProductDistance</code> implementation, but you can easily add support for additional algorithms by implementing <code>DistanceAlgorithm</code> interface yourself.</p>

<p>Once you have an instance of a <code>SimilaritySearch</code> aggregator, you can perform similarity search by calling <code>NamedMap.aggregate</code> method like you normally would:</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt;          books   = session.getMap("books");
List&lt;QueryResult&lt;String, Book&gt;&gt; results = books.aggregate(search);</markup>

<p>The result of the search is a list of up to maximum specified <code>QueryResult</code> objects (10, in the example above), which contain entry key, value, and calculated distance between the search vector and a vector extracted from the specified entry. The results are sorted by distance, in ascending order, from closest to farthest.</p>


<h4 id="_brute_force_search">Brute-force Search</h4>
<div class="section">
<p>By default, if no index is defined for the vector attribute, Coherence will perform a brute-force search by deserializing every entry, extracting the vector attribute from it, and performing distance calculation between the extracted vector and the search vector using specified distance algorithm.</p>

<p>This is fine for small or medium-sized data sets, because Coherence will still perform search in parallel across cluster members and aggregate the results, but can be very inefficient as the data sets get larger and larger, in which case using one of supported index types (described below) is recommended.</p>

<p>However, even when using indexes, it may be beneficial to execute the same query using brute force, in order to test recall by comparing the results returned by the (approximate) index-based search, and the (exact) brute-force search.</p>

<p>To accomplish that, you can configure <code>SimilaritySearch</code> aggregator to ignore any configured index and to perform brute-force search anyway, by calling <code>bruteForce</code> method on the aggregator instance:</p>

<markup
lang="java"

>var search = Aggregators.similaritySearch(Book::getSummaryEmbedding, searchVector, 10)
                        .bruteForce();</markup>


<h5 id="_indexed_brute_force_search">Indexed Brute-Force Search</h5>
<div class="section">
<p>It is possible to improve performance of a brute-force search by creating a forward-only index on the vector attribute using <code>DeserializationAccelerator</code>:</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt; books = session.getMap("books");
books.addIndex(new DeserializationAccelerator(Book::getSummaryEmbedding));</markup>

<p>This will avoid repeated deserialization of <code>Book</code> values when performing brute-force search, at the cost of additional memory consumed by the indexed vector instances.</p>

<p>The search will still perform the exact distance calculation, so the results will be exact, just like with the non-indexed brute-force search.</p>

</div>
</div>

<h4 id="_index_based_search">Index-based Search</h4>
<div class="section">
<p>While the brute force searches work fine with small data sets, as the data set gets larger it is highly recommended to create a vector index for a vector property.</p>

<p>Coherence supports two vector index types out of the box: HNSW index and Binary Quantization index.</p>


<h5 id="_hnsw_index">HNSW Index</h5>
<div class="section">
<p>HNSW index performs approximate vector search using <a id="" title="" target="_blank" href="https://arxiv.org/abs/1603.09320">Hierarchical Navigable Small World graphs</a>, as described by Malkov and Yashunin.</p>

<p>Coherence uses embedded native implementation of <a id="" title="" target="_blank" href="https://github.com/nmslib/hnswlib">hnswlib</a> for HNSW index implementation, so in order to use HNSW index you need to add a dependency on <code>coherence-hnsw</code> module, which contains all Java code and pre-built native libraries for Linux (ARM and x86), Mac (ARM and x86) and Windows (x86 only) that you need:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-hnsw&lt;/artifactId&gt;
  &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>Once you add the dependency above, creating HNSW index is as simple as</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt; books = session.getMap("books");
books.addIndex(new HnswIndex&lt;&gt;(Book::getSummaryEmbedding, 768));</markup>

<p>The first argument to <code>HnswIndex</code> constructor is the extractor for the vector attribute to index, and the second is the number of dimensions each indexed vector will have (which must be identical), which will allow native index implementation to pre-allocate memory required for index.</p>

<p>By default, <code>HnswIndex</code> will use cosine distance to calculate vector distances, but this can be overridden by specifying <code>spaceName</code> argument ina constructor:</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt; books = session.getMap("books");
books.addIndex(new HnswIndex&lt;&gt;(Book::getSummaryEmbedding, "L2", 768));</markup>

<p>The valid values for space name are <code>COSINE</code>, <code>L2</code> and <code>IP</code> (inner product).</p>

<p><code>HnswIndex</code> also provides a number of options that can be used to fine-tune its behavior, which can be specified using fluent API:</p>

<markup
lang="java"

>var hnsw = new HnswIndex&lt;&gt;(Book::getSummaryEmbedding, 768)
                    .setEfConstr(200)
                    .setEfSearch(50)
                    .setM(16)
                    .setRandomSeed(100);
books.addIndex(hnsw);</markup>

<p>The algorithm parameters above are described in more detail in <a id="" title="" target="_blank" href="https://github.com/nmslib/hnswlib/blob/master/ALGO_PARAMS.md"><code>hnswlib</code> documentation</a>.</p>

<p>You can also specify maximum index size by calling <code>setMaxElements</code> method. By default, the index will be created with a maximum size of 4,096 elements, and will be resized as necessary to accommodate data set growth. However, resize operation is somewhat costly and can be avoided if you know ahead of time how many entries will be stored in a Coherence map you are creating the index on, in which case you should configure the index size accordingly.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Remember that Coherence partitions indexes, so there will be as many instances of HNSW index as there are partitions.</p>

<p>This means that the ideal <code>maxElements</code> settings is just a bit over <code>mapSize / partitionCount</code>, and not the actual map size, which would be way too big.</p>
</p>
</div>
<p>Once you have HNSW index configured and created, you can simply perform searches the same way as we did earlier using brute-force search. Coherence will automatically detect and use HNSW index, if one is available.</p>

</div>

<h5 id="_binary_quantization">Binary Quantization</h5>
<div class="section">
<p>Coherence also supports <a id="" title="" target="_blank" href="https://huggingface.co/blog/embedding-quantization#binary-quantization">Binary Quantization</a>-based index, which provides significant space savings (32x) compared to vector indexes that use <code>float32</code> vectors, such as HNSW. It does this by converting each 32-bit float in the original vector into either 0 or 1, and representing it using a single bit in a <code>BitSet</code>.</p>

<p>The downside is that the recall may not be as accurate, especially with smaller vectors, but that can be largely addressed by oversampling and re-scoring of the results, which Coherence automatically performs.</p>

<p><code>BinaryQuantIndex</code> is implemented in pure Java, and is a part of the main Coherence distribution, so it requires no additional dependencies. To create it, simply call <code>NamedMap.addIndex</code> method:</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt; books = session.getMap("books");
books.addIndex(new BinaryQuantIndex&lt;&gt;(Book::getSummaryEmbedding));</markup>

<p>The only option you can specify is the <code>oversamplingFactor</code>, which is the multiplier for the maximum number of the results to return, and is 3 by default, meaning that if your search aggregator is configured to return 10 results, binary quantization search will initially return 30 results based on the Hamming distance between the binary representation of the search vector and index vectors, re-score all 30 results using exact distance calculation and then re-order and return top 10 results based on the calculated exact distance.</p>

<p>To change <code>oversamplingFactor</code>, you can specify it using fluent API when creating an index</p>

<markup
lang="java"

>NamedMap&lt;String, Book&gt; books = session.getMap("books");
books.addIndex(new BinaryQuantIndex&lt;&gt;(Book::getSummaryEmbedding).oversamplingFactor(5));</markup>

<p>which will cause <code>SimilaritySearch</code> aggregator to return and re-score 50 results initially instead of 30, in the example above.</p>

<p>Just like with HNSW index, once you have Binary Quantization index configured and created, you can simply perform searches the same way as we did earlier using brute-force search. Coherence will automatically detect it and use it.</p>

</div>
</div>

<h4 id="_metadata_filtering">Metadata Filtering</h4>
<div class="section">
<p>In addition to vector-based similarity search, you can use standard Coherence filters to perform metadata-based filtering of the results. For example, if we only wanted to search books by a specific author, we could specify a metadata filter <code>SimilaritySearch</code> aggregator should use in conjunction with a vector similarity search:</p>

<markup
lang="java"

>var search  = Aggregators.similaritySearch(Book::getSummaryEmbedding, searchVector, 3)
                         .filter(Filters.equal(Book::getAuthor, "Jules Verne"));
var results = books.aggregate(search);</markup>

<p>The above should return only top 3 books written by Jules Verne, sorted according to vector similarity.</p>

<p>Metadata filtering works the same regardless of whether you use brute-force or index-based search, and will use any indexes you may have on the metadata attributes you are filtering on, such as <code>Book::getAuthor</code> in this case, to speed up filter evaluation.</p>

<p>If you are a long-time Coherence user, you may be wondering why we are setting the filter on the aggregator itself and performing filter evaluation inside the aggregator, instead of using <code>aggregate</code> method that accepts a filter and allows us to pre-filter the set of entries to aggregate.</p>

<p>The reason is that both vector index implementations need to evaluate the filter internally, and only include the result if it evaluates to <code>true</code>, so the example above will work in all situations.</p>

<p>However, if you are using brute-force search, you may achieve the same result, and likely improve performance, by pre-filtering the entries:</p>

<markup
lang="java"

>var search  = Aggregators.similaritySearch(Book::getSummaryEmbedding, searchVector, 3);
var results = books.aggregate(Filters.equal(Book::getAuthor, "Jules Verne"), search);</markup>

</div>
</div>

<h3 id="_retrieval_augmented_generation_rag_support">Retrieval Augmented Generation (RAG) Support</h3>
<div class="section">
<p>Coherence provides <code>DocumentChunk</code> class, which can be used to represent document chunks containing text, embedding and metadata, as typically represented in various RAG frameworks.</p>

<p>While this class can certainly be used on its own, its main purpose is to support various RAG framework integrations in a consistent manner.</p>

</div>

<h3 id="_integrations">Integrations</h3>
<div class="section">
<p>The following sections describe integrations with popular AI frameworks that Coherence supports.</p>

<p>These integrations are not part of Coherence itself, but the contributions we&#8217;ve made to the frameworks below to allow them to use Coherence as a Vector Store, Chat Memory, etc.</p>


<h4 id="_langchain_python">LangChain (Python)</h4>
<div class="section">
<p>TBD</p>

</div>

<h4 id="_langchain4j">LangChain4j</h4>
<div class="section">
<p><a id="" title="" target="_blank" href="https://github.com/coherence-community/langchain4j/tree/coherence/langchain4j-coherence">langchain4j-coherence</a> module provides implementation of <code>EmbeddingStore</code> and <code>ChatMemoryStore</code> interfaces, allowing Coherence to be easily used as either (or both) in LangChain4j applications.</p>

<p>It also provides a Spring Boot Starter for LangChain4j applications via <a id="" title="" target="_blank" href="https://github.com/coherence-community/langchain4j-spring/tree/coherence/langchain4j-coherence-spring-boot-starter">langchain4j-coherence-spring-boot-starter</a> module.</p>

<p>For more information, see documentation provided by the integration modules above.</p>

</div>

<h4 id="_spring_ai">Spring AI</h4>
<div class="section">
<p><a id="" title="" target="_blank" href="https://github.com/coherence-community/spring-ai/tree/coherence-store/vector-stores/spring-ai-coherence-store">spring-ai-coherence-store</a> module provides implementation of <code>VectorStore</code> interface, allowing Coherence to be easily used as such in Spring AI applications.</p>

<p>It also provides a Spring Boot Starter via <a id="" title="" target="_blank" href="https://github.com/coherence-community/spring-ai/tree/coherence-store/spring-ai-spring-boot-starters/spring-ai-starter-coherence-store">spring-ai-starter-coherence-store</a> module.</p>

<p>For more information, see documentation provided by the integration modules above.</p>

</div>
</div>
</div>
</doc-view>
