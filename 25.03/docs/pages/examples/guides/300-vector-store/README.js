<doc-view>

<h2 id="_vector_db_and_knn_search">Vector DB and Knn Search</h2>
<div class="section">
<p>This example shows how to use some of the Coherence Vector DB features to store vectors and
perform a Knn search on those vectors.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>This example will use the Coherence AI features in <code>coherence.jar</code> to store vectors produced from a <code>.pdf</code>
file of Coherence documentation. The example will show how to then perform a nearest neighbour search (Knn search)
of those vectors to find matches for search text.</p>

<p>Coherence includes an implementation of the HNSW index which can be used to index vectors to improve search times.</p>

<p>Coherence is only a vector store so in order to actually create vectors from text snippets this example uses
the <a id="" title="" target="_blank" href="https://github.com/langchain4j/langchain4j">LangChain4J</a> library to integrate with a model and produce vector
embeddings from text.</p>

<p>This example shows just some basic usages of vectors in Coherence including using Coherence HNSW indexes.
It has not been optimised at all for speed of loading vector data or searches.</p>

</div>

<h3 id="_what_you_need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://www.oracle.com/java/technologies/downloads/">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="https://gradle.org/install/">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included, so they can be built without first installing
either build tool.</p>

</li>
<li>
<p>You can also import the code straight into your IDE:</p>
<ul class="ulist">
<li>
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>

<h4 id="_building_the_example_code">Building the Example Code</h4>
<div class="section">
<p>The source code for the guides and tutorials can be found in the
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence/tree/master/prj/examples">Coherence CE GitHub repo</a></p>

<p>The example source code is structured as both a Maven and a Gradle project and can be easily built with either
of those build tools. The examples are stand-alone projects so each example can be built from the
specific project directory without needing to build the whole Coherence project.</p>

<ul class="ulist">
<li>
<p>Build with Maven</p>

</li>
</ul>
<p>Using the included Maven wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Build with Gradle</p>

</li>
</ul>
<p>Using the included Gradle wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./gradlew build</markup>

</div>
</div>

<h3 id="_coherence_vectors">Coherence Vectors</h3>
<div class="section">
<p>Coherence can handle few different types of vector, this example will use the Coherence
<code>com.oracle.coherence.ai.Vector&lt;float[]&gt;</code> vector type.</p>

<p>Just like any other data type in Coherence, vectors are stored in normal Coherence caches.
The vector may be stored as the actual cache value, or it may be in a field of another type that is the cache value.
Vector data is then loaded into Coherence the same way that any other data is loaded using the <code>NamedMap</code> API.</p>

</div>

<h3 id="_movie_database">Movie Database</h3>
<div class="section">
<p>This example is going to build a small database of movies.
The database is small because the data used is stored in the source repository along with the code.
The same techniques could be used to load any of the freely available much larger JSON datasets with the required field names.</p>


<h4 id="_the_data_model">The Data Model</h4>
<div class="section">
<p>This example is not going to use an specialized classes to store the data in the cache.
The dataset is a json file and the example will use Coherence json support to read and store the data.
The actual type used for the value will be a Coherence <code>com.oracle.coherence.io.json.JsonObject</code> class, which is
part of the <code>coherence-json</code> module.</p>

<p>The schema of the JSON movie data looks like this</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Field Name</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">title</td>
<td class="">The title of the movie</td>
</tr>
<tr>
<td class="">plot</td>
<td class="">A short summary of the plot of the movie</td>
</tr>
<tr>
<td class="">fullplot</td>
<td class="">A longer summary of the plot of the movie</td>
</tr>
<tr>
<td class="">cast</td>
<td class="">A list of the names of the actors in the movie</td>
</tr>
<tr>
<td class="">genres</td>
<td class="">A list of string values representing the different genres the movie belongs to</td>
</tr>
<tr>
<td class="">runtime</td>
<td class="">How long the move runs for in minutes</td>
</tr>
<tr>
<td class="">poster</td>
<td class="">A link to the poster for the movie</td>
</tr>
<tr>
<td class="">languages</td>
<td class="">A list of string values representing the different languages for the movie</td>
</tr>
<tr>
<td class="">directors</td>
<td class="">A list of the names of the directors of the movie</td>
</tr>
<tr>
<td class="">writers</td>
<td class="">A list of the names of the writers of the movie</td>
</tr>
</tbody>
</table>
</div>
<p>This example uses the fullplot to create the vector embeddings for each movie.
Other fields can be used by normal Coherence filters to further narrow down vector searches.</p>

</div>

<h4 id="_the_movierepository_class">The <code>MovieRepository</code> Class</h4>
<div class="section">
<p>The <code>com.oracle.coherence.guides.vectors.MovieRepository</code> class contains all the code to load and search movie data.
The constructor for the class takes a <code>NamedMap&lt;String, JsonObject&gt;</code> which is the cache to use to store the data.</p>

</div>

<h4 id="_loading_the_dataset">Loading the Dataset</h4>
<div class="section">
<p>The <code>load()</code> method in the <code>com.oracle.coherence.guides.vectors.MovieRepository</code> class will load JSON data
from an <code>InputStream</code> into the cache.
The JSON data should be a JSON list of movie objects in the format described above.
The example uses the <code>src/main/resources/movies.json.gzip</code> file, which is JSON data g-zipped to make it smaller.</p>

<p>The JSON data is deserialized into a <code>List</code> of <code>JsonObject</code> instances with the following code.</p>

<markup
lang="java"

>JsonSerializer serializer = new JsonSerializer();
List&lt;JsonObject&gt; list = serializer.deserialize(in.readAllBytes(), List.class);</markup>

<p>The code above uses a Coherence <code>JsonSerializer</code> to simply deserialize the whole dataset in one go.
Obviously for a larger dataset this may not be practical and other ways would need to be written to read
the dataset in chunks.</p>

<p>The list of movies that have now been converted to <code>JsonObject</code> are then loaded into the cache.</p>

<markup
lang="java"

>for (JsonObject jsonObject : list) {
    String plot = (String) jsonObject.get("fullplot");      <span class="conum" data-value="1" />
    Vector&lt;float[]&gt; vector = vectorize(plot);               <span class="conum" data-value="2" />
    jsonObject.put("embeddings", vector);                   <span class="conum" data-value="3" />
    movies.put(jsonObject.getString("title"), jsonObject);  <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">For each <code>JsonObject</code> the "fullplot" field is extracted.</li>
<li data-value="2">The text of the plot is converted to a vector by calling the <code>vectorize()</code> method (see below)</li>
<li data-value="3">The vector is added to the <code>JsonObject</code> in the <code>"embeddings"</code> field</li>
<li data-value="4">The <code>JsonObject</code> is added to the cache using the "title" field as the cache key</li>
</ul>
</div>

<h4 id="_vectorizing_text">Vectorizing Text</h4>
<div class="section">
<p>The <code>MovieRepository</code> class has a <code>vectorize</code> method that takes a <code>String</code>
value and returns a <code>Vector&lt;float[]&gt;</code>.</p>

<markup
lang="java"

>public Vector&lt;float[]&gt; vectorize(String s) {
    Response&lt;Embedding&gt; response  = this.model.embed(s);    <span class="conum" data-value="1" />
    Embedding embedding = response.content();               <span class="conum" data-value="2" />
    float[] vector = Vectors.normalize(embedding.vector()); <span class="conum" data-value="3" />
    return new Float32Vector(Vectors.normalize(vector));    <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">The model being used by the <code>MovieRepository</code> is used to create embeddings for the text. In this example the model used is the LangChain4J <code>AllMiniLmL6V2EmbeddingModel</code> embedding model.</li>
<li data-value="2">The embedding is obtained from the result of the <code>model.embed()</code> call</li>
<li data-value="3">The vector returned is normalized, which makes future operations on the vector more efficient</li>
<li data-value="4">The <code>float[]</code> vector is then returned as a Coherence <code>Float32Vector</code> instance.</li>
</ul>
</div>

<h4 id="_storing_movies_with_vectorized_plots">Storing Movies with Vectorized Plots</h4>
<div class="section">
<p>In the <code>MovieRepository</code> example we can load JSON data from the <code>movies.json.gzip</code> file like this:</p>

<markup
lang="java"

>NamedMap&lt;String, JsonObject&gt; movies  = session.getMap("movies");
MovieRepository              movieDb = new MovieRepository(movies);

URL url = Resources.findFileOrResource("movies.json.gzip", null);
try (GZIPInputStream in = new GZIPInputStream(url.openStream())) {
    movieDb.load(in);
}</markup>

<p>After running the code above the <code>movies</code> cache will contain the movie dataset with embedding vectors created from the
movie&#8217;s plot. This data can then be searched to find movies with plots matching a search term.</p>

</div>
</div>

<h3 id="_searching_vectors">Searching Vectors</h3>
<div class="section">
<p>A common way to search data in Coherence caches is to use Coherence aggregators.
The aggregator feature has been used to implement nearest neighbour (Knn) vector searching using a
new built-in aggregator named <code>com.oracle.coherence.ai.search.SimilaritySearch</code>.
When invoking a <code>SimilaritySearch</code> aggregator on a cache the results are returned as a list
of <code>com.oracle.coherence.ai.QueryResult</code> instances. The list is ordered with the nearest neighbour returned first in the list.</p>

<p>The <code>SimilaritySearch</code> aggregator is used to perform a Knn vector search on a cache in the same way that
normal Coherence aggregators are used.</p>

<p>The example below shows using the <code>SimilaritySearch</code> aggregator on a simple cache that uses a <code>String</code> key and a <code>JsonObject</code> as the cache value.
The search will find the nearest 10 neighbours to a float array vector.</p>

<markup
lang="java"

>float[] searchFor = new float[]{1.0, 2.0, 3.0};
Vector&lt;float[]&gt; vector = new Float32Vector(Vectors.normalize(searchFor)); <span class="conum" data-value="1" />
ValueExtractor&lt;JsonObject, Vector&lt;float[]&gt;&gt; extractor = Extractors.extract("embeddings"); <span class="conum" data-value="2" />
SimilaritySearch&lt;String, JsonObject, float[]&gt; search = new SimilaritySearch&lt;&gt;(extractor, vector, 10); <span class="conum" data-value="3" />
List&lt;QueryResult&lt;String, JsonObject&gt;&gt; results = cache.aggregate(search); <span class="conum" data-value="4" /></markup>

<ul class="colist">
<li data-value="1">A <code>Vector</code> instance is created from the <code>searchFor</code> float array, the 10 nearest neighbours to this vector will be searched for.</li>
<li data-value="2">The vector is stored in the <code>embeddings</code> field of a <code>JsonObject</code> in the cache, so a <code>ValueExtractor</code> is required that
can extract the vector.</li>
<li data-value="3">A <code>SimilaritySearch</code> instance is created that uses the extractor, the search vector and sets the result size to ten.</li>
<li data-value="4">The search is performed with a simple call to <code>cache.aggregate()</code>.</li>
</ul>
<p>The <code>MovieRepository</code> class has a <code>search</code> method that takes a <code>String</code> which is the text to use to convert to a vector
and search the movie plot for the nearest matches.
The second parameter is a count of the number of nearest neighbours to search for.</p>

<markup
lang="java"

>public List&lt;QueryResult&lt;String, JsonObject&gt;&gt; search(String text, int count) {
    Vector&lt;float[]&gt; vector = vectorize(text);    <span class="conum" data-value="1" />
    ValueExtractor&lt;JsonObject, Vector&lt;float[]&gt;&gt; extractor = Extractors.extract("embeddings"); <span class="conum" data-value="2" />
    SimilaritySearch&lt;String, JsonObject, float[]&gt; search = new SimilaritySearch&lt;&gt;(extractor, vector, count); <span class="conum" data-value="3" />
    return movies.aggregate(filter, search); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">The <code>text</code> parameter is converted to a normalized vector using the method described in the "Vectorizing Text" section above.</li>
<li data-value="2">The movie plot vector is stored in the <code>embeddings</code> field of a <code>JsonObject</code> in the cache, so a <code>ValueExtractor</code>
is required that can extract the vector.</li>
<li data-value="3">A <code>SimilaritySearch</code> instance is created using the extractor, vector and count.</li>
<li data-value="4">The aggregator is invoked on the <code>movies</code> cache and the results returned.</li>
</ul>

<h4 id="_search_for_interesting_movies">Search for Interesting Movies</h4>
<div class="section">
<p>Using the dataset in the example, we can search for movies based on a plot we are interested in.</p>

<p>For example, looking for five movies roughly based on <em>"star travel and space ships"</em> could be
done by calling the <code>search</code> method like this:</p>

<markup


>List&lt;QueryResult&lt;String, JsonObject&gt;&gt; results = movieDb.search("star travel and space ships", 5);</markup>

<p>If we ran this search against the example dataset the five movies returned are shown below.
The lower the distance value, the nearer the plot is to the search term.
All five are good matches for movies about  <em>"star travel and space ships"</em>.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Title</th>
<th>Distance</th>
<th>Plot</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Star Trek</td>
<td class="">0.551528</td>
<td class="">Captain Picard, with the help of supposedly dead Captain Kirk, must stop a madman willing to murder on a planetary scale in order to enter an energy ribbon.</td>
</tr>
<tr>
<td class="">Babylon 5</td>
<td class="">0.561844</td>
<td class="">A space station in neutral territory is the focus of a unique five year saga</td>
</tr>
<tr>
<td class="">Serenity</td>
<td class="">0.632477</td>
<td class="">he crew of the ship Serenity tries to evade an assassin sent to recapture one of their number who is telepathic.</td>
</tr>
<tr>
<td class="">Dune</td>
<td class="">0.635018</td>
<td class="">A Duke&#8217;s son leads desert warriors against the galactic emperor and his father&#8217;s evil nemesis when they assassinate his father and free their desert world from the emperor&#8217;s rule.</td>
</tr>
<tr>
<td class="">Starcrash</td>
<td class="">0.648616</td>
<td class="">An outlaw smuggler and her alien companion are recruited by the Emperor of the Galaxy to rescue his son and destroy a secret weapon by the evil Count Zarth Arn.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h4 id="_searching_with_metadata">Searching with Metadata</h4>
<div class="section">
<p>When a vector is stored in a cache inside another class, such as a <code>JsonObject</code> in this example,
the Knn search can also include a <code>Filter</code>. The filter is used to reduce the cache entries used to
perform the Knn search.</p>

<p>The <code>MovieRepository</code> class has a <code>search</code> method that takes a <code>String</code> which is the text to use to convert to a vector
and search the movie plot for the nearest matches.</p>

<markup
lang="java"

>public List&lt;QueryResult&lt;String, JsonObject&gt;&gt; search(String text, Filter&lt;JsonObject&gt; filter, int count) {
    Vector&lt;float[]&gt; vector = vectorize(text);
    SimilaritySearch&lt;String, JsonObject, float[]&gt; search = new SimilaritySearch&lt;&gt;(extractor, vector, count);
    return movies.aggregate(filter, search);
}</markup>

</div>

<h4 id="_search_for_interesting_movies_with_a_specific_cast">Search for Interesting Movies with a Specific Cast</h4>
<div class="section">
<p>In the example above any movie with a plot similar to <em>"star travel and space ships"</em> was searched for.
If we want to find a similar movie, but one that starred "Harrison Ford" we can use a <code>Filter</code> to narrow down the
search. The filter will be applied to the <code>cast</code> field of the <code>JsonObject</code>.</p>

<markup
lang="java"

>ValueExtractor&lt;JsonObject, List&lt;String&gt;&gt; castExtractor = Extractors.extract("cast");  <span class="conum" data-value="1" />
Filter&lt;JsonObject&gt; filter = Filters.contains(castExtractor, "Harrison Ford"); <span class="conum" data-value="2" />

results = movieDb.search("star travel and space ships", filter, 2); <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">The <code>Filter</code> will need a <code>ValueExtractor</code> to extract the <code>cast</code> field from the <code>JsonObject</code></li>
<li data-value="2">The <code>cast</code> field is a JSON array of <code>String</code> values. In a <code>JsonObject</code> this would be a field <code>List&lt;String&gt;</code> so
the <code>Filter</code> needs to be a <code>ContainsFilter</code> to match any cast list containing ""Harrison Ford".</li>
<li data-value="3">The <code>search</code> method is called specifying the plot search term, the cast <code>Filter</code> and the number of matches.</li>
</ul>
<p>This time the search is for the nearest two matches (as the example movie dataset is very small and does not contain many "Harrison Ford" movies).</p>

<p>Executing the code above returns the following two movies, which are pretty good matches:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Title</th>
<th>Distance</th>
<th>Plot</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Star Wars: Episode V - The Empire Strikes Back</td>
<td class="">0.662698</td>
<td class="">After the rebels have been brutally overpowered by the Empire on their newly established base, Luke Skywalker takes advanced Jedi training with Master Yoda, while his friends are pursued by Darth Vader as part of his plan to capture Luke.</td>
</tr>
<tr>
<td class="">Star Wars: Episode IV - A New Hope</td>
<td class="">0.686102</td>
<td class="">Luke Skywalker joins forces with a Jedi Knight, a cocky pilot, a wookiee and two droids to save the universe from the Empire&#8217;s world-destroying battle-station, while also attempting to rescue Princess Leia from the evil Darth Vader.</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>

<h3 id="_adding_indexes">Adding Indexes</h3>
<div class="section">
<p>As data is stored in normal caches, indexes can be used to speed up searching.
An Coherence index can be created like normal on a cache, so an index can be created for the vector field that
the <code>SimilaritySearch</code> will use. Indexes can also be created on any metadata fields that may be used when searches
include an additional <code>Filter</code>.</p>

</div>

<h3 id="_hnsw_indexing">HNSW Indexing</h3>
<div class="section">
<p>Coherence includes an implementation of the HNSW index that can be used to speed up searches.
This implementation is in the <code>coherence-hnsw</code> module.
When this module is on the class path, a HNSW index can be created on a cache.</p>

<p>An index is added to a cache in Coherence by calling the <code>addIndex</code> method on the cache and passing in
a <code>ValueExtractor</code> specifying the field to index.
The <code>coherence-hnsw</code> module contains a special extractor implementation named <code>com.oracle.coherence.ai.hnsw.HnswIndex</code>
which can be used to create a HNSW index on a vector stored in the cache.</p>

<p>An instance of <code>HnswIndex</code> is constructed with a <code>ValueExtractor</code> that will extract the vector field
from the cache value and an <code>int</code> parameter that specifies the number of dimensions the vector has.</p>

<p>For example, in the <code>MovieRepository</code> the vector to index is stored in the <code>embeddings</code> field.
The number of vector dimensions in the example is 384, which is the number of dimensions used by
the LangChain4j "AllMiniLmL6V2EmbeddingModel" embedding model.</p>

<p>A HNSW index can be added to the movies cache with the code shown below.</p>

<markup
lang="java"

>ValueExtractor&lt;JsonObject, Vector&lt;float[]&gt;&gt; extractor = Extractors.extract("embeddings");
int dimensions = model.dimension();
movies.addIndex(new HnswIndex&lt;&gt;(extractor, dimensions));</markup>

<p>The <code>HnswIndex</code> class will create a special type of cache index implementing <code>com.oracle.coherence.ai.VectorIndex</code>
which the <code>SimilaritySearch</code> aggregator knows how to use to speed up searching.
In the case of a HNSW index, the cache data will not be used to perform the search, the HNSW index in memory is used.</p>

</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>This example has shown how easy it is to add vector search capabilities to cache data in Coherence
and how to easily add HNSW indexes to those searches.</p>

<p>Using Coherence vector features combined with AI libraries such as LangChain4J can help to build powerful AI
applications for RAG and vector searches.</p>

</div>
</div>
</doc-view>
