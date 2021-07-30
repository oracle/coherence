<doc-view>

<h2 id="_custom_aggregators">Custom Aggregators</h2>
<div class="section">
<p>This guide walks you through how to create custom aggregators that allow you to process data stored in Coherence in parallel.</p>

<p>Coherence supports entry aggregators that perform operations against all, or a subset
of entries to obtain a single result. This aggregation is carried out in parallel across the cluster
and is a map-reduce type of operation which can be performed efficiently across large amounts of data.</p>

<p>See the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/processing-data-cache.html#GUID-DFB7AB0C-1CE6-4259-8854-9DA1F40B6F15">Coherence Documentation</a>
for detailed information on Aggregations.</p>


<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-will-need" @click.native="this.scrollFix('#what-you-will-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#data-model" @click.native="this.scrollFix('#data-model')">Example Data Model</router-link></p>

</li>
<li>
<p><router-link to="#example-code-2" @click.native="this.scrollFix('#example-code-2')">Review the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#run-example-2" @click.native="this.scrollFix('#run-example-2')">Run the Example</router-link></p>

</li>
<li>
<p><router-link to="#summary" @click.native="this.scrollFix('#summary')">Summary</router-link></p>

</li>
<li>
<p><router-link to="#see-also" @click.native="this.scrollFix('#see-also')">See Also</router-link></p>

</li>
</ul>
</div>

<h3 id="what-you-will-build">What You Will Build</h3>
<div class="section">
<p>This example shows how to build a custom aggregator which we will
use to count how many times a particular word occurs in documents stored in Coherence maps. The <code>Document</code>
class is a standard POJO with an identifier, and a string for the document contents.</p>


<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 30 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 1.8</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.5+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included so they can be built without first installing
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
</div>

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

<h4 id="running">Running the Examples</h4>
<div class="section">
<p>This example can be run directly in your IDE, but to best demonstrate the functionality
you should run 1 or more cache servers and then run the example class.</p>

<ol style="margin-left: 15px;">
<li>
Running Cache Servers
<markup
lang="bash"

>./mvnw exec:exec -P server</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runServer -x test</markup>

</li>
<li>
Running each example
<p>Each example can be run direct from the IDE, or can be run via executing the tests.</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

</li>
</ol>
</div>
</div>

<h3 id="data-model">Example Data Model</h3>
<div class="section">
<p>The data model consists of the <code>Document</code> class which represents a document with text contents that we
are going to search.</p>

<markup
lang="java"

>public class Document
        implements Serializable {

    private String id;
    private String contents;</markup>


<h4 id="example-code-2">Review the Example Code</h4>
<div class="section">
<p>The <code>WordCount</code> class implements the <code>InvocableMap.StreamingAggregator</code> as well as <code>Serializable</code> for serialization.</p>

<p>When you implement <code>InvocableMap.StreamingAggregator</code>, you must implement the following methods:</p>

<ul class="ulist">
<li>
<p><code>supply()</code> - creates an instance we can accumulate into in parallel</p>

</li>
<li>
<p><code>accumulate()</code> - adds single entry to partial result when executing on storage members</p>

</li>
<li>
<p><code>getPartialResult()</code> - returns the partial result</p>

</li>
<li>
<p><code>combine()</code> - combines partial results on the client</p>

</li>
<li>
<p><code>finalizeResult()</code> - applies finishing transformation to the final result and returns it</p>

</li>
</ul>
<p>See below for details of each of the <code>WordCount</code> class.</p>

<ol style="margin-left: 15px;">
<li>
Implementing interfaces
<markup
lang="java"

>public class WordCount&lt;K extends String, V extends Document&gt;
        implements InvocableMap.StreamingAggregator&lt;K, V, Map&lt;String, Integer&gt;, Map&lt;String, Integer&gt;&gt;, <span class="conum" data-value="1" />
        Serializable { <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">Implement the <code>InvocableMap.StreamingAggregator</code> with key, value, partial result and final result</li>
<li data-value="2">Implement serialization</li>
</ul>
</li>
<li>
The following constructor is used to set the words to search for.
<markup
lang="java"

>/**
 * Constructs a {@link WordCount}.
 *
 * @param setWords  {@link Set} of words to search for
 */
public WordCount(Set&lt;String&gt; setWords) {
    this.setWords = setWords;
}</markup>

</li>
<li>
Creates an instance we can accumulate into in parallel when executing on the storage members
<markup
lang="java"

>@Override
public InvocableMap.StreamingAggregator&lt;K, V, Map&lt;String, Integer&gt;, Map&lt;String, Integer&gt;&gt; supply() {
    return new WordCount&lt;&gt;(setWords);
}</markup>

</li>
<li>
Adds single entry to partial result when executing on storage members
<markup
lang="java"

>@Override
public boolean accumulate(InvocableMap.Entry&lt;? extends K, ? extends V&gt; entry) {
    Document document = entry.getValue();

    for (String word : setWords) {
        // count how many times the word exists in the the documents and accumulate
        int count = document.getContents().split("\\b" + word + "\\b", -1).length - 1;  <span class="conum" data-value="1" />
        this.mapResults.compute(word, (k, v) -&gt; v == null ? count : v + count);  <span class="conum" data-value="2" />
    }

    return true;
}</markup>

<ul class="colist">
<li data-value="1">Count the number of times the word occurs in the document</li>
<li data-value="2">Add or update the count for the word in the results Map</li>
</ul>
</li>
<li>
Return the partial result
<markup
lang="java"

>@Override
public Map&lt;String, Integer&gt; getPartialResult() {
    Logger.info("getPartialResult: " + mapResults);
    return mapResults;
}</markup>

</li>
<li>
Combine all the partial results on the client
<markup
lang="java"

>@Override
public boolean combine(Map&lt;String, Integer&gt; mapPartialResult) {
    Logger.info("combine: Received partial result " + mapPartialResult);
    // combine the results passed in with the current set of results.
    if (!mapPartialResult.isEmpty()) {
        mapPartialResult.forEach((k, v) -&gt; mapResults.compute(k, (key, value) -&gt; value == null ? v : value + v));
    }
    return true;
}</markup>

<div class="admonition note">
<p class="admonition-inline">This method is called on the client to combine the results passed in with the current result. This is
used to get the final set of results from all members.</p>
</div>
</li>
<li>
Take the final partial result and applies any finishing transformation
<markup
lang="java"

>@Override
public Map&lt;String, Integer&gt; finalizeResult() {
    return mapResults;
}</markup>

</li>
<li>
Characteristics for the aggregator
<markup
lang="java"

>@Override
public int characteristics() {
    return PARALLEL | PRESENT_ONLY;
}</markup>

<div class="admonition note">
<p class="admonition-inline">We specifically set the PARALLEL and PRESENT_ONLY characteristics to indicate this can be run in parallel and
to execute to only run on entries that are present.</p>
</div>
</li>
</ol>
<p><strong>CustomAggregationExample Class</strong></p>

<p>The <code>runExample()</code> method contains the code that exercises the above custom aggregator.</p>

<markup
lang="java"

>/**
 * Run the example.
 */
public void runExample() {
    System.out.println("Documents added " + documents.size());

    // choose up to 5 random words from the list to search for
    Set&lt;String&gt; setWords = new HashSet&lt;&gt;();
    for (int i = 0; i &lt; 5; i++) {
        setWords.add(getRandomValue(WORDS));
    }

    System.out.println("Running against the following words: " + setWords);
    Map&lt;String, Integer&gt; results = documents.aggregate(new WordCount&lt;&gt;(setWords)); <span class="conum" data-value="1" />

    results.forEach((k, v) -&gt; System.out.println("Word " + k + ", number of occurrences: " + v));
}</markup>

<ul class="colist">
<li data-value="1">Run the aggregator against 5 randomly chosen words</li>
</ul>
</div>

<h4 id="run-example-2">Run the Example</h4>
<div class="section">
<p>Carry out the following to run this example:</p>

<ol style="margin-left: 15px;">
<li>
Start 2 cache servers using the method described above:
<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw exec:exec -P server</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runServer -x test</markup>

</li>
<li>
Running the example
<p>The example can be run direct from the IDE by directly running the <code>CustomAggregationExample</code> class
, or can be run via executing the tests.</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

<p>This will generate output similar to the following indicating the documents and times that a
word exists in a document.</p>

<markup
lang="bash"

>Documents added 2000
Running against the following words: [fifteen, tv, trumpet, this, launch]
&lt;Info&gt; (thread=PartitionedCacheWorker:0x0000:2, member=2): ***** getPartialResult: {fifteen=181, tv=350, trumpet=194, this=1155, launch=189}
&lt;Info&gt; (thread=main, member=2): ***** combine: Received partial result {fifteen=177, tv=376, trumpet=210, this=1173, launch=193}
&lt;Info&gt; (thread=main, member=2): ***** combine: Received partial result {fifteen=181, tv=350, trumpet=194, this=1155, launch=189}
Word fifteen, number of occurrences: 358
Word tv, number of occurrences: 726
Word trumpet, number of occurrences: 404
Word this, number of occurrences: 2328
Word launch, number of occurrences: 382</markup>

<div class="admonition note">
<p class="admonition-inline">The messages above containing <code>combine</code> are when the client called the <code>combine()</code> method to
aggregate the final results returned from the storage members. In this case we had 2 storage members including
the test itself.</p>
</div>
</li>
</ol>
</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide we have shown you how to create custom aggregators that
allow you to process data stored in Coherence in parallel.</p>

<p>You have created a custom aggregator to count the number of
times a word appears in documents stored in Coherence.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/processing-data-cache.html#GUID-C9DF96E0-FAF2-4CD9-958E-4DC5CF06B18A">Performing Data Grid Operations</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
