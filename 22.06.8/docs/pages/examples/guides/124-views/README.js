<doc-view>

<h2 id="_views">Views</h2>
<div class="section">
<p>This guide walks you through the concepts of creating <em>Views</em>, also known as <em>Continuous Queries</em>. <em>Views</em> allow you to
execute queries against your Coherence data with the added benefit that <em>Views</em> stay up-to-date, allowing you to retrieve
the latest results of your query from the Coherence cache in real-time.</p>


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
<p><router-link to="#data-model" @click.native="this.scrollFix('#data-model')">Example Data Model</router-link></p>

</li>
<li>
<p><router-link to="#why-to-use-views" @click.native="this.scrollFix('#why-to-use-views')">Why to use Views</router-link></p>

</li>
<li>
<p><router-link to="#using-a-continuous-query-cache" @click.native="this.scrollFix('#using-a-continuous-query-cache')">Using a ContinuousQueryCache</router-link></p>

</li>
<li>
<p><router-link to="#observing-continuous-query-caches" @click.native="this.scrollFix('#observing-continuous-query-caches')">Observing Continuous Query Caches</router-link></p>

</li>
<li>
<p><router-link to="#continuous-aggregation" @click.native="this.scrollFix('#continuous-aggregation')">Continuous Aggregation</router-link></p>

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
<p>The example code is written as a set of unit tests, showing you how can create <em>Views</em> against your
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
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 11</a> or later</p>

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

<h3 id="data-model">Example Data Model</h3>
<div class="section">
<p>The data model for this guide consists of a single class named <code>Country</code>. This model class represents a country with the
following properties:</p>

<ul class="ulist">
<li>
<p>name</p>

</li>
<li>
<p>capital</p>

</li>
<li>
<p>population</p>

</li>
</ul>
<p>The data is being stored in a Coherence cache named <code>countries</code> with the key being the two-letter
<a id="" title="" target="_blank" href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166 country code</a>.</p>

</div>

<h3 id="why-to-use-views">Why to use Views</h3>
<div class="section">
<p>With <em>Views</em>, also referred to as <em>Continuous Queries</em>, you can ensure that a query always retrieves the latest results
from a cache in real-time. For instance, in the <router-link to="/examples/guides/110-queries/README">queries guide</router-link>, we used a
<code>Filter</code> to query for a subset of data from a Coherence cache. However, what happens with the underlying cache if changes
<em>DO</em> happen, and you need the updates immediately? Queries, as used previously, will only retrieve a snapshot
of the underlying data. They will not reflect future data changes. Thus, let&#8217;s revisit a
<router-link :to="{path: '/examples/guides/110-queries/README', hash: '#filter'}">previous example</router-link> that queries a cache containing <code>Countries</code> using a <code>Filter</code>. The
<code>Filter</code>, as in the previous query example, will ensure that only countries with a population of 60 million or more people
are returned.</p>

<markup
lang="java"

>@Test
void testGreaterEqualsFilterWithChanges() {

    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); <span class="conum" data-value="2" />

    Set&lt;Map.Entry&lt;String, Country&gt;&gt; results = map.entrySet(filter); <span class="conum" data-value="3" />

    assertThat(results, hasSize(2)); <span class="conum" data-value="4" />

    Country mexico = new Country("Mexico", "Ciudad de México", 126.01); <span class="conum" data-value="5" />
    map.put("mx", mexico);

    assertThat(results, hasSize(2)); <span class="conum" data-value="6" />

}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a Filter that will select only countries with at least 60 million people using a <code>GreaterEqualsFilter</code></li>
<li data-value="3">Apply the <code>Filter</code> by invoking <code>entrySet(filter)</code> on the <code>NamedCache</code></li>
<li data-value="4">The result should be 2 countries only</li>
<li data-value="5">We add a new country Mexico to the map</li>
<li data-value="6">Assert that still only France and Germany were selected</li>
</ul>
<p>In this test we have added a new country Mexico to the <code>countries</code> Map but as you can see, the change will not be
reflected in the already filtered results map. In order to get updates in real-time, we have to use a <code>ContinuousQueryCache</code>.</p>

<p><em>Views</em> are extremely useful in all those situations where we need immediate access to any changes of the underlying data,
such as trading systems or Complex Event Processing (CEP) systems. They can be used in both client-based and server-based
applications and are reminiscent of SQL Views.</p>

</div>

<h3 id="using-a-continuous-query-cache">Using a ContinuousQueryCache</h3>
<div class="section">
<p>The following test will look almost exactly the same as the previous test. However, instead of calling the <code>entrySet()</code>
method on the <code>NamedCache</code>, we will create a new instance of <code>ContinuousQueryCache</code> and pass in the <code>Filter</code> and the Coherence
map as constructor arguments.</p>

<markup
lang="java"

>@Test
void testGreaterEqualsFilterWithContinuousQueryCache() {

    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); <span class="conum" data-value="2" />

    ContinuousQueryCache results = new ContinuousQueryCache(map, filter); <span class="conum" data-value="3" />

    assertThat(results.size(), is(2)); <span class="conum" data-value="4" />

    Country mexico = new Country("Mexico", "Ciudad de México", 126.01); <span class="conum" data-value="5" />
    map.put("mx", mexico);

    assertThat(results.size(), is(3)); <span class="conum" data-value="6" />
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a <code>Filter</code> that will select only countries with more than 60 million people using a <code>GreaterEqualsFilter</code></li>
<li data-value="3">Create a new instance of <code>ContinuousQueryCache</code></li>
<li data-value="4">The result should consist of 2 countries only</li>
<li data-value="5">We add a new country Mexico to the original map</li>
<li data-value="6">Assert that the <code>ContinuousQueryCache</code> now contains 3 countries</li>
</ul>
<p>Under the covers, the <code>ContinuousQueryCache</code> will use Coherence cache events on the map to react to changes in the
Coherence <code>NamedCache</code>.</p>

<div class="admonition note">
<p class="admonition-inline">In order to create a <code>ContinuousQueryCache</code> without filtering, use the <code>AlwaysFilter</code>, e.g.
<code>new ContinuousQueryCache(map, AlwaysFilter.instance)</code>.</p>
</div>
</div>

<h3 id="observing-continuous-query-caches">Observing Continuous Query Caches</h3>
<div class="section">
<p>Proactively querying for updates is all fun and games but what if you need to execute logic as soon as data changes
happen? The <code>ContinuousQueryCache</code> implements the <code>ObservableMap</code> interface to react to Coherence cache events. As such,
you can subscribe to cache events by registering <code>MapListener</code> implementations.</p>

<p>In the following test, we will add a <code>MapListener</code> to keep track of countries being added to the underlying <code>NamedCache</code>.
But because this listener is added to the <code>ContinuousQueryCache</code>, the listener will only get invoked for countries
that have a population of 60 million or more.</p>

<markup
lang="java"

>@Test
void testContinuousQueryCacheWithListener() {

    NamedCache&lt;String, Country&gt; map = getMap("countries");
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0);

    ContinuousQueryCache results = new ContinuousQueryCache(map, filter);

    AtomicInteger counter = new AtomicInteger(0); <span class="conum" data-value="1" />

    MapListener&lt;String, Double&gt; listener = new SimpleMapListener&lt;String, Double&gt;() <span class="conum" data-value="2" />
            .addInsertHandler((event) -&gt; {
                counter.incrementAndGet();
            });
    results.addMapListener(listener);  <span class="conum" data-value="3" />


    assertThat(results.size(), is(2)); <span class="conum" data-value="4" />

    Country mexico = new Country("Mexico", "Ciudad de México", 126.01);  <span class="conum" data-value="5" />
    map.put("mx", mexico);

    assertThat(results.size(), is(3)); <span class="conum" data-value="6" />
    Eventually.assertDeferred(counter::get, is(1)); <span class="conum" data-value="7" />
}</markup>

<ul class="colist">
<li data-value="1">Create a counter to keep track of added countries</li>
<li data-value="2">Instantiate a <code>MapListener</code> that will increment the counter for each new country being added</li>
<li data-value="3">Add the <code>MapListener</code> to the <code>ContinuousQueryCache</code></li>
<li data-value="4">Assert that the <code>ContinuousQueryCache</code> contains 2 countries</li>
<li data-value="5">Add a new country with a population larger than 60 million</li>
<li data-value="6">Assert that the <code>ContinuousQueryCache</code> now contain 3 countries</li>
<li data-value="7">The counter of the <code>MapListener</code> should have increased by 1</li>
</ul>
</div>

<h3 id="continuous-aggregation">Continuous Aggregation</h3>
<div class="section">
<p>What about aggregated results? In an earlier example for Queries, we had used a <code>Filter</code> and a <code>BigDecimalSum</code> aggregator
to calculate the sum of the population for those countries whose population is at least 60 million.</p>

<p>We can use a <code>MapListener</code> to achieve that, as the <code>ContinuousQueryCache</code> does not directly support aggregators.</p>

<markup
lang="java"

>@Test
void testAggregate() {

    NamedCache&lt;String, Country&gt; map = getMap("countries");
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0);
    ReflectionExtractor&lt;Country, Double&gt; extractor = new ReflectionExtractor&lt;&gt;("getPopulation");
    ContinuousQueryCache&lt;String, Country, Double&gt; results = new ContinuousQueryCache(map, filter, extractor);

    BigDecimalSum&lt;BigDecimal&gt; aggregator = new BigDecimalSum(new IdentityExtractor&lt;&gt;()); <span class="conum" data-value="1" />
    AtomicReference&lt;BigDecimal&gt; aggregatedPopulation = new AtomicReference&lt;&gt;(formatNumber(results.aggregate(aggregator))); <span class="conum" data-value="2" />

    MapListener&lt;String, Double&gt; listener = new SimpleMapListener&lt;String, Double&gt;()  <span class="conum" data-value="3" />
            .addInsertHandler((event) -&gt; {
                aggregatedPopulation.set(formatNumber(results.aggregate(aggregator)));
            });
    results.addMapListener(listener); <span class="conum" data-value="4" />

    assertThat(aggregatedPopulation.get(), is(formatNumber(150.6))); <span class="conum" data-value="5" />

    Country mexico = new Country("Mexico", "Ciudad de México", 126.01); <span class="conum" data-value="6" />
    map.put("mx", mexico);

    assertThat(results.size(), is(3)); <span class="conum" data-value="7" />
    Eventually.assertDeferred(aggregatedPopulation::get, is(formatNumber(276.61))); <span class="conum" data-value="8" />

}</markup>

<ul class="colist">
<li data-value="1">Create a <code>BigDecimalSum</code> aggregator. The <code>IdentityExtractor</code> will use the actual value (does not actually extract
anything)</li>
<li data-value="2">Create a holder for the aggregated population and trigger the initial aggregation explicitly</li>
<li data-value="3">Instantiate a <code>MapListener</code> that will trigger the aggregation of the population for each new country being added</li>
<li data-value="4">Add the <code>MapListener</code> to the <code>ContinuousQueryCache</code></li>
<li data-value="5">Assert that the aggregated population is initially 150.6 million</li>
<li data-value="6">Add a new country with a population larger than 60 million</li>
<li data-value="7">Assert that the <code>ContinuousQueryCache</code> now contain 3 countries</li>
<li data-value="8">The aggregated population should now be 276.61 million</li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide we showed, how you can easily create <em>Views</em> with a <code>ContinuousQueryCache</code> that reflects changes of the data
in the underlying Coherence <code>NamedCache</code> in real-time. Please see the Coherence reference guide, specifically the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-continuous-query-caching.html#GUID-5FB6F1B5-F1C3-4049-B69D-CC07BDF88883">Using Continuous Query Caching</a>
for more details.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-continuous-query-caching.html#GUID-5FB6F1B5-F1C3-4049-B69D-CC07BDF88883">Using Continuous Query Caching</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-map-events.html#GUID-A91B66C9-F449-49A3-9165-073459BA1B3E">Using Map Events</a></p>

</li>
<li>
<p><router-link to="/examples/guides/110-queries/README">Querying Caches</router-link></p>

</li>
<li>
<p><router-link to="/examples/guides/120-built-in-aggregators/README">Built-In Aggregators</router-link></p>

</li>
</ul>
</div>
</div>
</doc-view>
