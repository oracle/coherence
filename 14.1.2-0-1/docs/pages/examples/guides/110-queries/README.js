<doc-view>

<h2 id="_querying_caches">Querying Caches</h2>
<div class="section">
<p>This guide walks you through the basic concepts of querying Coherence caches. We will provide a quick overview and
examples of using <em>Coherence Query Language</em> (CohQL) before learning more about <code>Filters</code>, <code>ValueExtractors</code> and <code>Aggregators</code>
to query caches programmatically.</p>


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
<p><router-link to="#building-the-example-code" @click.native="this.scrollFix('#building-the-example-code')">Building The Example Code</router-link></p>

</li>
<li>
<p><router-link to="#the-power-of-cohql" @click.native="this.scrollFix('#the-power-of-cohql')">The Power of CohQL</router-link></p>

</li>
<li>
<p><router-link to="#query-caches-programmatically" @click.native="this.scrollFix('#query-caches-programmatically')">Query Caches Programmatically</router-link></p>

</li>
<li>
<p><router-link to="#create-the-test-class" @click.native="this.scrollFix('#create-the-test-class')">Create the Test Class</router-link></p>

</li>
<li>
<p><router-link to="#bootstrap-coherence" @click.native="this.scrollFix('#bootstrap-coherence')">Bootstrap Coherence</router-link></p>

</li>
<li>
<p><router-link to="#filter" @click.native="this.scrollFix('#filter')">Filter</router-link></p>

</li>
<li>
<p><router-link to="#value-extractor" @click.native="this.scrollFix('#value-extractor')">ValueExtractor</router-link></p>

</li>
<li>
<p><router-link to="#aggregate-results" @click.native="this.scrollFix('#aggregate-results')">Aggregate Results</router-link></p>

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
<p>The example code is written as a set of unit tests, showing you how can simply executed sophisticated queries against your
Coherence data.</p>


<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
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

<h4 id="building-the-example-code">Building the Example Code</h4>
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

<h3 id="the-power-of-cohql">The Power of CohQL</h3>
<div class="section">
<p>Before we start querying caches programmatically, you should be aware of the power of the <em>Coherence Query Language</em> (CohQL).
CohQL is inspired by SQL and is a quick and easy way to interact with your caches. Commonly it is used as a command-line tool.</p>

<p>Let&#8217;s assume we have a cache called <code>countries</code> that contains a map of <code>Country</code> classes with the 2-letter country code
being the key of each cache entry. The <code>Country</code> class will have some basic properties such as <code>name</code>, <code>capital</code> and <code>population</code>.</p>

<p>The simplest CohQL query you could write is a query that will return all countries is:</p>

<markup
lang="sql"

>select * from countries</markup>

<p>As you can see, if you&#8217;re familiar with SQL, you will feel right at home. And of course from here we can make the query
more sophisticated.</p>

<div class="admonition note">
<p class="admonition-inline">For detailed information on how to use CohQL, please visit the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/using-coherence-query-language.html#GUID-C0D082B1-FA62-4899-A043-4345156E6641">Using Coherence Query Language</a>
in the Coherence reference guide.</p>
</div>
<p>In order to give you a way experiment with the cache using CohQL, we provide a simple sample app that pre-populates a Coherence
cache <code>countries</code> wih countries and starts the CohQL Console. To get started, execute <code>com.oracle.coherence.guides.queries.StartCohQLConsole</code>.</p>

<div class="admonition tip">
<p class="admonition-inline">Typically, you would want to start the CohQL Console as a stand-alone application. Please see the
<a id="" title="" target="_blank" href="https://github.com/oracle/coherence#cohql-console">following instructions</a> to learn more.</p>
</div>
<p>Once the console application is started, let&#8217;s execute:</p>

<markup
lang="sql"

>select * from countries</markup>

<p>The result should be a list of 5 countries:</p>

<markup
lang="java"

>Results
Country{name='Colombia', capital='Bogotá', population=50.4}
Country{name='Australia', capital='Canberra', population=26.0}
Country{name='Ukraine', capital='Kyiv', population=41.2}
Country{name='France', capital='Paris', population=67.4}
Country{name='Germany', capital='Berlin', population=83.2}</markup>

<p>What if you would like to just retrieve the list of capitals? We can achieve that by selecting just the capital:</p>

<markup
lang="sql"

>select capital from countries</markup>

<p>which yields:</p>

<markup
lang="java"

>Results
"Bogotá"
"Paris"
"Canberra"
"Kyiv"
"Berlin"</markup>

<p>Of course, you can also apply <code>where</code> clauses to further limit the results. For example, if you like to retrieve the
countries with a population that is greater than 60 million you may add the following <code>where</code> clause:</p>

<markup
lang="sql"

>select capital from countries c where population &gt; 60.0</markup>

<p>which results in:</p>

<markup
lang="java"

>Results
"Paris"
"Berlin"</markup>

<p>Another option is to aggregate results. For example, let&#8217;s calculate the total population of countries with a population
larger than 60 million:</p>

<markup
lang="sql"

>select sum(population) from countries c where population &gt; 60.0</markup>

<p>which yields a value of <code>150.6</code>.</p>

<div class="admonition note">
<p class="admonition-inline">CohQL is not merely a tool for query caches. It can also be used to <em>create</em> and <em>delete</em> caches, to <em>insert</em>,
<em>delete</em> and <em>update</em> cache value, to <em>create indices</em> and more. For more information please see the official reference
documentation.</p>
</div>
</div>

<h3 id="query-caches-programmatically">Query Caches Programmatically</h3>
<div class="section">
<p>So how would we create queries programmatically to retrieve the same results? The key here is to understand the following
concepts:</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://coherence.community/14.1.2-0-1/api/java/com/tangosol/util/Filter.html">Filter</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://coherence.community/14.1.2-0-1/api/java/com/tangosol/util/ValueExtractor.html">ValueExtractor</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://coherence.community/14.1.2-0-1/api/java/com/tangosol/util/aggregator/package-summary.html">Aggregator</a></p>

</li>
</ul>
</div>

<h3 id="create-the-test-class">Create the Test Class</h3>
<div class="section">
<p>The first step is to create the test class that will show and test the various query operations,
we&#8217;ll call this class <code>QueryTests</code>. We will use Junit 5 for this test, so the class does not have to be public.</p>

<markup
lang="java"

>class QueryTests {
}</markup>


<h4 id="bootstrap-coherence">Bootstrap Coherence</h4>
<div class="section">
<p>The first thing the test class will do is start Coherence using the bootstrap API introduced in Coherence <code>20.12</code>. As this
is a JUnit test class, we can do this in a static <code>@BeforeAll</code> annotated setup method. We will also populate the cache with
several countries and thus let&#8217;s create a small helper class <code>CoherenceHelper</code>:</p>

<markup
lang="java"

>public static void startCoherence() {
    Coherence coherence = Coherence.clusterMember(); <span class="conum" data-value="1" />
    CompletableFuture&lt;Coherence&gt; future = coherence.start(); <span class="conum" data-value="2" />
    future.join(); <span class="conum" data-value="3" />

    Session session = coherence.getSession(); <span class="conum" data-value="4" />
    NamedCache&lt;String, Country&gt; countries = session.getCache("countries"); <span class="conum" data-value="5" />

    countries.put("de", new Country("Germany", "Berlin", 83.2)); <span class="conum" data-value="6" />
    countries.put("fr", new Country("France", "Paris", 67.4));
    countries.put("ua", new Country("Ukraine", "Kyiv", 41.2));
    countries.put("co", new Country("Colombia", "Bogotá", 50.4));
    countries.put("au", new Country("Australia", "Canberra", 26));
}</markup>

<ul class="colist">
<li data-value="1">Obtain a default storage enabled cluster member <code>Coherence</code> instance.</li>
<li data-value="2">Start the <code>Coherence</code> instance, this wil start all the Coherence services.</li>
<li data-value="3">Block until Coherence instance has fully started before proceeding with the tests</li>
<li data-value="4">Obtain the default <code>Session</code></li>
<li data-value="5">Get the <code>countries</code> cache</li>
<li data-value="6">Populate the <code>countries</code> cache with several new <code>Country</code> instances</li>
</ul>
<p>We are going to start a storage enabled cluster member using the most basic bootstrap API methods.
For more details on the bootstrap API see the <router-link to="#docs/core/02_bootstrap.adoc" @click.native="this.scrollFix('#docs/core/02_bootstrap.adoc')">corresponding guide</router-link>.</p>

<markup
lang="java"

>@BeforeAll
static void boostrapCoherence() {
    CoherenceHelper.startCoherence(); <span class="conum" data-value="1" />
}</markup>

<ul class="colist">
<li data-value="1">Call <code>CoherenceHelper</code> and start the <code>Coherence</code> instance and populate the country data.</li>
</ul>
<p>Lastly, we create a static <code>@AfterAll</code> annotated tear-down method that will shut down Coherence at the end of the test.</p>

<markup
lang="java"

>@AfterAll
static void shutdownCoherence() {
    Coherence coherence = Coherence.getInstance(); <span class="conum" data-value="1" />
    coherence.close();
}</markup>

<ul class="colist">
<li data-value="1">We only created a single default <code>Coherence</code> instance, so we can obtain that instance with the
<code>Coherence.getInstance()</code> method, and then close it.</li>
</ul>
<p>Now the basic framework of the test is in place we can add methods to show different querying operations.</p>

</div>

<h4 id="filter">Filter</h4>
<div class="section">
<p>To get started, we would like to retrieve all countries that have a population of more than 60 million people. For that
we will use a <code>Filter</code>:</p>

<markup
lang="java"

>@Test
void testGreaterEqualsFilter() {

    NamedMap&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter&lt;Country&gt; filter = Filters.greaterEqual(Country::getPopulation, 60.0); <span class="conum" data-value="2" />

    final Set&lt;Map.Entry&lt;String, Country&gt;&gt; results = map.entrySet(filter); <span class="conum" data-value="3" />

    assertThat(results).hasSize(2); <span class="conum" data-value="4" />

    map.entrySet(filter).forEach(entry -&gt; { <span class="conum" data-value="5" />
        assertThat(entry.getKey()).containsAnyOf("de", "fr");
        assertThat(entry.getValue().getPopulation()).isGreaterThan(60.0);
    });
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a Filter that will select only countries with more than 60 million people using the <code>Filters</code> helper class via <code>Filters.greaterEqual</code>.</li>
<li data-value="3">Apply the <code>Filter</code> by invoking <code>entrySet(filter)</code> on the Map</li>
<li data-value="4">The result should be 2 countries only</li>
<li data-value="5">Assert that only France and Germany were selected</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">The best practice for ValueExtractors is to use the method reference, e.g. <code>Country::getPopulation</code>, to extract falues as this provides compile time type checking.</p>
</div>
</div>

<h4 id="value-extractor">ValueExtractor</h4>
<div class="section">
<p>What if we don&#8217;t want to return <code>Countries</code> but just the collection of country names for which the population is
60 million people or higher? This is where we can use a <code>ValueExtractor</code> in combination with a <code>ReducerAggregator</code>.</p>

<p>A value extractor is used to extract a property from a given object. In most instances developers would use the
<code>ReflectionExtractor</code> as an implementation. The <code>ReducerAggregator</code> on the other hand, is used to run a <code>ValueExtractor</code>
against cache entries, and it returns the extracted value. The result returned by the <code>ReducerAggregator</code> is a <code>Map</code>
where the key is the key of the cache entry and the value is the extracted value.</p>

<markup
lang="java"

>@Test
void testValueExtractor() {

    NamedMap&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter&lt;Country&gt; filter = Filters.greaterEqual(Country::getPopulation, 60.0); <span class="conum" data-value="2" />

    ReducerAggregator&lt;String, Country, Country, String&gt; aggregator
            = new ReducerAggregator&lt;&gt;(Country::getName); <span class="conum" data-value="3" />

    Map&lt;String, String&gt; result = map.aggregate(filter, aggregator); <span class="conum" data-value="4" />

    result.forEach((key, value) -&gt; { <span class="conum" data-value="5" />
        assertThat(key).containsAnyOf("de", "fr");
        assertThat(value).containsAnyOf("Germany", "France");
    });
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create the same filter as in the previous test (Select countries with more than 60 million people, only)</li>
<li data-value="3">Create a <code>ReducerAggregator</code> instance and specify that we only want the name of the countries returned</li>
<li data-value="4">Apply the <code>Filter</code> and <code>Aggregator</code></li>
<li data-value="5">Verify that only the two country names <code>France</code> and <code>Germany</code> are returned as filtered values</li>
</ul>
</div>

<h4 id="aggregate-results">Aggregate Results</h4>
<div class="section">
<p>What if we want to group queried data together? Let&#8217;s query for countries, where the population is greater than 60
million but instead of returning the countries, we will return the sum of the population of thsoe 2 countries instead.</p>

<markup
lang="java"

>@Test
void testAggregate() {

    NamedMap&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter&lt;Country&gt; filter = Filters.greaterEqual(Country::getPopulation, 60.0); <span class="conum" data-value="2" />
    BigDecimalSum&lt;BigDecimal&gt; aggregator = new BigDecimalSum&lt;&gt;("getPopulation"); <span class="conum" data-value="3" />
    BigDecimal result = map.aggregate(filter, aggregator); <span class="conum" data-value="4" />
    String resultAsString = result.setScale(2, RoundingMode.HALF_UP) <span class="conum" data-value="5" />
            .stripTrailingZeros() <span class="conum" data-value="6" />
            .toPlainString(); <span class="conum" data-value="7" />
    assertThat(resultAsString).isEqualTo("150.6"); <span class="conum" data-value="8" />
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create the same filter as in the previous test (Select countries with more than 60 million people, only)</li>
<li data-value="3">We will use a different <code>Aggregator</code>. <code>BigDecimalSum</code> will aggregate the population and return a <code>Bigecimal</code> value. This shows that you can use a method name (not recommended) as well as method reference</li>
<li data-value="4">Apply the <code>Filter</code> and <code>Aggregator</code></li>
<li data-value="5">For assertion purposes we will convert the <code>BigDecimal</code> value to a <code>String</code></li>
<li data-value="6">The generated String shall not have any trailing zeros</li>
<li data-value="7">Return the String</li>
<li data-value="8">Verify that the returned value is <code>150.6</code></li>
</ul>
<div class="admonition tip">
<p class="admonition-inline">To learn much more about built-in Aggregators, please take a look at the
<a id="" title="" target="_blank" href="../120-built-in-aggregators/README.adoc">respective guide</a>.</p>
</div>
</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide we showed how easy it is to query Coherence caches either using CohQL or programmatically using Filters,
ValueExtractors and Aggregators. Please see the Coherence reference guide, specifically the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/querying-data-cache.html#GUID-A6A97011-A2FB-45A4-B9FC-AA0C8C49C057">Querying Data In a Cache</a>
for more details.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/using-coherence-query-language.html#GUID-C0D082B1-FA62-4899-A043-4345156E6641">Using Coherence Query Language</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/querying-data-cache.html#GUID-A6A97011-A2FB-45A4-B9FC-AA0C8C49C057">Querying Data In a Cache</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
