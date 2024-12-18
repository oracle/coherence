<doc-view>

<h2 id="_entry_processors">Entry Processors</h2>
<div class="section">
<p>This guide walks you through the concepts of creating <em>Entry Processors</em>. Entry Processors allow you to perform data
grid processing across a cluster. That means without moving cache entries across the wire, you can process one
or more cache entries locally on the storage node.</p>


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
<p><router-link to="#why-use-entry-processors" @click.native="this.scrollFix('#why-use-entry-processors')">Why use Entry Processors?</router-link></p>

</li>
<li>
<p><router-link to="#creating-an-entry-processor" @click.native="this.scrollFix('#creating-an-entry-processor')">Creating an Entry Processor</router-link></p>

</li>
<li>
<p><router-link to="#lambda-expressions" @click.native="this.scrollFix('#lambda-expressions')">Using Lambda Expressions</router-link></p>

</li>
<li>
<p><router-link to="#lambda-expressions-single-value" @click.native="this.scrollFix('#lambda-expressions-single-value')">Process Single Map Keys Using Lambda Expressions</router-link></p>

</li>
<li>
<p><router-link to="#update-all-map-entries" @click.native="this.scrollFix('#update-all-map-entries')">Update all Map Entries</router-link></p>

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
<p>The example code is written as a set of unit tests, showing how you can use Entry Processors with Coherence.</p>


<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 20 minutes</p>

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

<h3 id="why-use-entry-processors">Why use Entry Processors?</h3>
<div class="section">
<p>In our example, we do have several countries loaded into the cache. Let&#8217;s assume we want to increase the population of
several countries by a million each. More specifically, we only want to increase the population for those countries that
have a population of 60 million or more.</p>

<p>The obvious choice would be to query the cache using a <code>GreaterEqualsFilter</code> as we have done in the
<router-link to="/examples/guides/124-views/README">previous example on Views</router-link>, iterate over the results and update the respective
countries.</p>

<markup
lang="java"

>@Test
void testIncreasePopulationWithoutEntryProcessor() {

    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); <span class="conum" data-value="2" />

    Set&lt;String&gt; filteredKeys = map.keySet(filter); <span class="conum" data-value="3" />
    assertThat(filteredKeys).hasSize(2); <span class="conum" data-value="4" />

    for (String key : filteredKeys) {  <span class="conum" data-value="5" />
        map.lock(key, 0); <span class="conum" data-value="6" />
        try {
            Country country = map.get(key);
            country.setPopulation(country.getPopulation() + 1); <span class="conum" data-value="7" />
            map.put(key, country); <span class="conum" data-value="8" />
        }
        finally {
            map.unlock(key);
        }
    }

    assertThat(map).hasSize(5);
    Country germany = map.get("de");
    Country france = map.get("fr");
    assertThat(germany.getPopulation()).isEqualTo(84.2d);
    assertThat(france.getPopulation()).isEqualTo(68.4d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a Filter that will select only countries with at least 60 million people using a <code>GreaterEqualsFilter</code></li>
<li data-value="3">Apply the <code>Filter</code> by invoking <code>keySet(filter)</code> on the <code>NamedCache</code> that will return a Set of keys</li>
<li data-value="4">Assert that the <code>Set</code> of filtered keys only contains 2 keys</li>
<li data-value="5">Loop over the keys</li>
<li data-value="6">Make sure we lock the cache entry</li>
<li data-value="7">Increment the population by 1 million</li>
<li data-value="8">Update the map</li>
</ul>
<div class="admonition important">
<p class="admonition-inline">This is an example of how NOT to do this!</p>
</div>
<p>While this works, it will be inefficient in use-cases where you have to update high number of cache entries.This approach
would cause a lot of data to be moved over the wire, first for the retrieval of countries and then when pushing the
updated countries back into the cluster.</p>

<p>This is where <em>Entry Processors</em> come into play. Entry Processors allow us to perform data grid processing inside the Coherence
cluster. You can either apply Entry Processors for single cache keys or you can perform parallel processing against a
collection of cache entries (map-reduce functionality).</p>

<div class="admonition note">
<p class="admonition-inline">For a more in-depth introduction to Entry Processors, please refer to the respective chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/develop-applications/processing-data-cache.html#GUID-1CF26403-2922-4514-9395-84BCFF81849B">Processing Data In a Cache</a>
in the Oracle Coherence reference guide.</p>
</div>
</div>

<h3 id="creating-an-entry-processor">Creating an Entry Processor</h3>
<div class="section">
<p>Let&#8217;s rewrite the inefficient example above to use an Entry Processor. First, we will create a class called
<code>IncrementingEntryProcessor</code> that implements <code>InvocableMap.EntryProcessor</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.guides.entryprocessors.model.Country;
import com.tangosol.util.InvocableMap;

/**
 *  @author Gunnar Hillert  2022.02.25
 */
public class IncrementingEntryProcessor implements InvocableMap.EntryProcessor&lt;String, Country, Double&gt; { <span class="conum" data-value="1" />

	@Override
	public Double process(InvocableMap.Entry&lt;String, Country&gt; entry) { <span class="conum" data-value="2" />
		Country country = entry.getValue();
		country.setPopulation(country.getPopulation() + 1); <span class="conum" data-value="3" />
		return country.getPopulation(); <span class="conum" data-value="4" />
	}
}</markup>

<ul class="colist">
<li data-value="1">The Entry Processor implements Coherence&#8217;s <code>InvocableMap.EntryProcessor</code> class. The type parameters represent the key, the value
and the return type of the Entry Processor.</li>
<li data-value="2">The <code>process()</code> method gives us access to the value of the <code>countries</code> Map</li>
<li data-value="3">Increment the population by 1 million</li>
<li data-value="4">Return the incremented population</li>
</ul>
<p>The <code>IncrementingEntryProcessor</code> contains one method <code>process()</code> that provides us with access to the <code>Country</code> via
the <code>InvocableMap.Entry</code> argument. We will increase the population and the return the population. Now it is time to use
the <code>IncrementingEntryProcessor</code>.</p>

<markup
lang="java"

>@Test
void testIncreasePopulationWithCustomEntryProcessor() {
    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); <span class="conum" data-value="2" />

    final Map&lt;String, Double&gt; results = map.invokeAll(filter, new IncrementingEntryProcessor()); <span class="conum" data-value="3" />

    assertThat(results).hasSize(2); <span class="conum" data-value="4" />
    assertThat(results.get("de")).isEqualTo(84.2d);
    assertThat(results.get("fr")).isEqualTo(68.4d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a Filter that will select only countries with at least 60 million people using a <code>GreaterEqualsFilter</code></li>
<li data-value="3">Call <code>invokeAll</code> on the <code>countries</code> Map, passing in both the filter and the <code>IncrementingEntryProcessor</code></li>
<li data-value="4">The result should be the Map containing the key and the new population value for the 2 affected countries</li>
</ul>
<p>In this example we are processing multiple map entries at once. You can of course apply Entry Processors to single map keys
as well by using:</p>

<markup
lang="java"

>@Test
void testIncreasePopulationForSingleEntry() {
    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    final Double result = map.invoke("de", new IncrementingEntryProcessor()); <span class="conum" data-value="2" />
    assertThat(result).isEqualTo(84.2d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">Call <code>invoke</code> on the <code>countries</code> Map, passing in the key (instead of the filter) and the <code>IncrementingEntryProcessor</code></li>
<li data-value="3">The result should be the double value representing the new population value of Germany</li>
</ul>
<p>In the next section we will see how we can simplify the example even further using lambda expressions.</p>

</div>

<h3 id="lambda-expressions">Using Lambda Expressions</h3>
<div class="section">
<p>Instead of creating dedicated Entry Processor classes, it may be advisable to pass in lambda expressions instead.
Especially in use-cases such as our very simple contrived example, lambda expressions simplify the code noticeably.</p>

<markup
lang="java"

>@Test
void testIncreasePopulationUsingLambdaExpression() {
    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = new GreaterEqualsFilter("getPopulation", 60.0); <span class="conum" data-value="2" />

    final Map&lt;String, Double&gt; results = map.invokeAll(filter, entry -&gt; {  <span class="conum" data-value="3" />
        Country country = entry.getValue();
        country.setPopulation(country.getPopulation() + 1);
        return country.getPopulation();
    });

    assertThat(results).hasSize(2); <span class="conum" data-value="4" />
    assertThat(results.get("de")).isEqualTo(84.2d);
    assertThat(results.get("fr")).isEqualTo(68.4d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">We create a Filter that will select only countries with at least 60 million people using a <code>GreaterEqualsFilter</code></li>
<li data-value="3">Call <code>invokeAll</code> on the <code>countries</code> Map passing in the filter and the function that increments the population</li>
<li data-value="4">The result should be the Map containing the key and the new population value for the 2 affected countries</li>
</ul>
</div>

<h3 id="lambda-expressions-single-value">Process Single Map Keys Using Lambda Expressions</h3>
<div class="section">
<p>When using lambda expressions for single map keys, you can use Coherence&#8217;s <code>invoke()</code> as well as Java&#8217;s <code>Map.compute()</code> method.
Let&#8217;s see the code for Coherence&#8217;s <code>invoke()</code> method first:</p>

<markup
lang="java"

>@Test
void testIncreasePopulationUsingInvokeForSingleCountry() {
    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />

    final Double results = map.invoke("de", entry -&gt; {  <span class="conum" data-value="2" />
        Country country = entry.getValue();
        country.setPopulation(country.getPopulation() + 1);
        entry.setValue(country);  <span class="conum" data-value="3" />
        return country.getPopulation();
    });

    assertThat(results).isEqualTo(84.2d);
    assertThat(map.get("de").getPopulation()).isEqualTo(84.2d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">Call <code>invoke</code> on the <code>NamedCache</code>, passing the key for Germany and the lambda expression</li>
<li data-value="3">It is important to explicitly call <code>setValue</code> on the cache entry</li>
</ul>
<p>If using <code>compute()</code>, the code will look like this:</p>

<markup
lang="java"

>@Test
void testIncreasePopulationUsingComputeForSingleCountry() {
    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />

    final Country results = map.compute("de", (key, country) -&gt; { <span class="conum" data-value="2" />
        country.setPopulation(country.getPopulation() + 1);  <span class="conum" data-value="3" />
        return country;
    });

    assertThat(results.getPopulation()).isEqualTo(84.2d);
    assertThat(map.get("de").getPopulation()).isEqualTo(84.2d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">Call <code>compute</code> on the <code>NamedCache</code>, passing the key for Germany and the lambda expression.</li>
<li data-value="3">Set the new population but as you can see that there is no need to set the country explicitly on the cache entry.</li>
</ul>
<p>The code when using <code>compute</code> looks a little simpler, as <code>compute</code> implicitly updates the value to whatever you return. When
using <code>invoke</code>, you have to explicitly call <code>entry.setValue(country)</code>. On the other hand, <code>compute</code> will return the entire
country, whereas with <code>invoke</code> you can return any data object. This is advantageous in situations where you need to
minimize the amount of data passed over the wire.</p>

</div>

<h3 id="update-all-map-entries">Update all Map Entries</h3>
<div class="section">
<p>Sometimes we may need to update all entries in a Coherence Map. In that use-case we simply change the passed-in Filter.
The lambda expression on the other hand remains the same. All we need to do is to pass in an instance of the <code>AlwaysFilter</code>:</p>

<markup
lang="java"

>@Test
void testIncreasePopulationForAllCountries() {

    NamedCache&lt;String, Country&gt; map = getMap("countries"); <span class="conum" data-value="1" />
    Filter filter = AlwaysFilter.INSTANCE(); <span class="conum" data-value="2" />

    final Map&lt;String, Double&gt; results = map.invokeAll(filter, entry -&gt; { <span class="conum" data-value="3" />
        Country country = entry.getValue();
        country.setPopulation(country.getPopulation() + 1);
        return country.getPopulation();
    });

    assertThat(results).hasSize(5); <span class="conum" data-value="4" />

    assertThat(results.get("ua")).isEqualTo(42.2d);
    assertThat(results.get("co")).isEqualTo(51.4d);
    assertThat(results.get("au")).isEqualTo(27d);
    assertThat(results.get("de")).isEqualTo(84.2d);
    assertThat(results.get("fr")).isEqualTo(68.4d);
}</markup>

<ul class="colist">
<li data-value="1">Get the <code>countries</code> Map</li>
<li data-value="2">Get an instance of the <code>AlwaysFilter</code> that will select all entries in the <code>countries</code> Map</li>
<li data-value="3">Call <code>invokeAll</code> on the <code>countries</code> Map passing in the <code>AlwaysFilter</code> and the function that increments the population</li>
<li data-value="4">The result should be the Map containing the key and the new population value for all 5 countries in the Map</li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide we showed how you can easily create <em>Entry Processors</em> to perform data grid processing across a cluster.
Please see the Coherence reference guide, specifically the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/develop-applications/processing-data-cache.html#GUID-1CF26403-2922-4514-9395-84BCFF81849B">Processing Data In a Cache</a>
for more details.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/develop-applications/processing-data-cache.html#GUID-1CF26403-2922-4514-9395-84BCFF81849B">Processing Data In a Cache</a></p>

</li>
<li>
<p><router-link to="/examples/guides/110-queries/README">Querying Caches</router-link></p>

</li>
<li>
<p><router-link to="/examples/guides/124-views/README">Views</router-link></p>

</li>
</ul>
</div>
</div>
</doc-view>
