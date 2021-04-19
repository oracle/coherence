<doc-view>

<h2 id="_near_caching">Near Caching</h2>
<div class="section">
<p>This guide walks you through how to use near caching within Coherence by providing
various examples and configurations that showcase the different features available.</p>

<p>A near cache is a hybrid cache; it typically fronts a distributed cache or a remote cache
with a local cache. Near cache invalidates front cache entries, using a configured
invalidation strategy, and provides excellent performance and synchronization.
Near cache backed by a partitioned cache offers zero-millisecond local access for
repeat data access, while enabling concurrency and ensuring coherency and fail over,
effectively combining the best attributes of replicated and partitioned caches.</p>

<p>See the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html">Coherence Documentation</a>
for detailed information on near caches.</p>


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
<p><router-link to="#example-code-1" @click.native="this.scrollFix('#example-code-1')">Review the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#example-tests-1" @click.native="this.scrollFix('#example-tests-1')">Review the Tests</router-link></p>

</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Examples</router-link></p>

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
<p>In this example you will run a number of tests and that show the following features of near caches:</p>

<ul class="ulist">
<li>
<p>Configuring near caches</p>

</li>
<li>
<p>Setting near cache size limits</p>

</li>
<li>
<p>Changing the invalidation strategy</p>

</li>
<li>
<p>Configuring expiry and eviction policies</p>

</li>
<li>
<p>Exploring MBeans related to near caching</p>

</li>
</ul>

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
<p><a id="" title="" target="_blank" href="/guides/gs/intellij-idea/">IntelliJ IDEA</a></p>

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
<p>This example can be run directly in your IDE, but you can also run
1 or more cache servers and then run the example class.</p>

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

<h3 id="example-code-1">Review the Example Code</h3>
<div class="section">
<p>The example code comprises the <code>SimpleNearCachingExample</code> class, which uses the <code>near-cache-config.xml</code>
configuration to define a near cache. The front cache is configured with 100 entries as the <code>high-units</code>
as well as optionally with an expiry. The back cache is a distributed cache.</p>

<p>When a near cache has reached it&#8217;s <code>high-units</code> limit, it prunes itself back to the
value of the <code>low-units</code> element (or not less than 80% of <code>high-units</code> if not set).  The entries
chosen are done so according to the configured <code>eviction-policy</code>.</p>

<p>There are a number of eviction policies that can be used including: Least Recently Used (LRU),
Least Frequently Used (LFU), Hybrid or custom.</p>

<p>The test class carries out the following steps:</p>

<ul class="ulist">
<li>
<p>Inserts 100 entries into the cache</p>

</li>
<li>
<p>Issues a get on each of the 100 entries and displays the time taken (populates the near cache&#8217;s front cache)</p>

</li>
<li>
<p>Displays <code>Cache</code> MBean metrics for the front cache</p>

</li>
<li>
<p>Carries out a second get on the 100 entries and notes the difference in the time to retrieve the entries</p>

</li>
<li>
<p>Inserts an additional 10 entries then issue gets for those entries, which will cause cache pruning</p>

</li>
<li>
<p>Displays <code>Cache</code> MBean metrics for the front cache to show cache pruning happening</p>

</li>
<li>
<p>Displays <code>StorageManager</code> MBean metrics to show listener registrations</p>

</li>
</ul>
<p>There are three tests that exercise the above <code>SimpleNearCachingExample</code> class and using different caches
as well as different invalidation strategies set via a system property. They are described in more detail in the following sections.</p>

<ul class="ulist">
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleNearCachingExampleALLTest</p>

</li>
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleNearCachingExamplePRESENTTest</p>

</li>
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleExpiringNearCachingExampleTest</p>

</li>
</ul>
<ol style="margin-left: 15px;">
<li>
Review the Cache Config
<markup
lang="java"

>&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;size-cache-*&lt;/cache-name&gt;  <span class="conum" data-value="1" />
    &lt;scheme-name&gt;near-scheme&lt;/scheme-name&gt;
    &lt;init-params&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;front-limit-entries&lt;/param-name&gt;
        &lt;param-value&gt;100&lt;/param-value&gt;
      &lt;/init-param&gt;
    &lt;/init-params&gt;
  &lt;/cache-mapping&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;expiring-cache-*&lt;/cache-name&gt;  <span class="conum" data-value="2" />
    &lt;scheme-name&gt;near-scheme&lt;/scheme-name&gt;
    &lt;init-params&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;front-limit-entries&lt;/param-name&gt;
        &lt;param-value&gt;100&lt;/param-value&gt;
      &lt;/init-param&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;front-expiry&lt;/param-name&gt;
        &lt;param-value&gt;8s&lt;/param-value&gt;
      &lt;/init-param&gt;
    &lt;/init-params&gt;
  &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;near-scheme&gt;
    &lt;scheme-name&gt;near-scheme&lt;/scheme-name&gt;
    &lt;front-scheme&gt;
      &lt;local-scheme&gt;
        &lt;eviction-policy&gt;LRU&lt;/eviction-policy&gt; <span class="conum" data-value="3" />
        &lt;high-units&gt;{front-limit-entries 10}&lt;/high-units&gt; <span class="conum" data-value="4" />
        &lt;expiry-delay&gt;{front-expiry 0s}&lt;/expiry-delay&gt;  <span class="conum" data-value="5" />
      &lt;/local-scheme&gt;
    &lt;/front-scheme&gt;
    &lt;back-scheme&gt;   <span class="conum" data-value="6" />
      &lt;distributed-scheme&gt;
        &lt;scheme-name&gt;sample-distributed&lt;/scheme-name&gt;
        &lt;service-name&gt;DistributedCache&lt;/service-name&gt;
        &lt;backing-map-scheme&gt;
          &lt;local-scheme/&gt;
        &lt;/backing-map-scheme&gt;
      &lt;/distributed-scheme&gt;
    &lt;/back-scheme&gt;
    &lt;invalidation-strategy system-property="test.invalidation.strategy"&gt;all&lt;/invalidation-strategy&gt; <span class="conum" data-value="7" />
    &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;/near-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">Define cache mapping for caches matching <code>size-cache-*</code> to the <code>near-scheme</code> using macros to set the
front limit to 100</li>
<li data-value="2">Define cache mapping for caches matching <code>expiring-cache-*</code> to the <code>near-scheme</code> using macros to set the front limit
to 100, and the expiry to 8 seconds</li>
<li data-value="3">Define an eviction policy to apply when <code>high-units</code> are reached</li>
<li data-value="4">Define front scheme <code>high-units</code> using the macro and defaulting to 10 if not set</li>
<li data-value="5">Define front scheme <code>expiry-delay</code> using the macro and defaulting to 0s if not set</li>
<li data-value="6">Define back scheme as standard distributed scheme</li>
<li data-value="7">System property to set the invalidation strategy for each test</li>
</ul>
</li>
<li>
Review the <code>SimpleNearCachingExample</code> class
<p><strong>Constructor</strong></p>

<markup
lang="java"

>/**
 * Construct the example.
 *
 * @param cacheName cache name
 * @param invalidationStrategy invalidation strategy to use
 */
public SimpleNearCachingExample(String cacheName, String invalidationStrategy) {
    this.cacheName = cacheName;
    if (invalidationStrategy != null) {
        System.setProperty("test.invalidation.strategy", invalidationStrategy);
    }
    System.setProperty("coherence.cacheconfig", "near-cache-config.xml");
    System.setProperty("coherence.management.refresh.expiry", "1s");
    System.setProperty("coherence.management", "all");
}</markup>

<p><strong>Main Example</strong></p>

<p>The <code>runExample()</code> method contains the code that exercises the near cache. A loop in the test runs twice to show the difference second time around with the near cache populated.</p>

<markup
lang="java"

>/**
 * Run the example.
 */
public void runExample() throws Exception {
    Session                   session = Session.create();
    NamedMap&lt;Integer, String&gt; map     = session.getMap(cacheName);
    map.clear();

    final int MAX = 100;

    Logger.info("Running test with cache " + cacheName);

    // sleep so we don't get distribution messages intertwined with test output
    Base.sleep(5000L);

    // fill the map with MAX values <span class="conum" data-value="1" />
    putValues(map, 0, MAX);

    // execute two times to see the difference in access times and MBeans once the
    // near cache is populated on the first iteration
    for (int j = 1; j &lt;= 2; j++) {

        // issue MAX get operations and get the total time taken
        long start = System.nanoTime();
        getValues(map, 0, MAX);  <span class="conum" data-value="2" />

        long duration = (System.nanoTime() - start);
        Logger.info("Iteration #" + j + " Total time for gets "
                    + String.format("%.3f", duration / 1_000_000f) + "ms");

        // Wait for some time for the JMX stats to catch up, and expiry to happen if we are using expiring front cache
        Base.sleep(5000L); <span class="conum" data-value="3" />

        logJMXNearCacheStats(); <span class="conum" data-value="4" />
    }

    // issue 10 more puts
    putValues(map, MAX, 10); <span class="conum" data-value="5" />

    // issue 10 more gets and the high-units will be hit and cache pruning will happen when using size cache
    getValues(map, MAX, 10);
    Logger.info("After extra 10 values put and get");

    logJMXNearCacheStats(); <span class="conum" data-value="6" />
    logJMXStorageStats();
}</markup>

<ul class="colist">
<li data-value="1">Populate the cache with 100 entries</li>
<li data-value="2">Issue a get for each of the 100 entries</li>
<li data-value="3">Sleep for 5 seconds to ensure JMX stats are up to date as well as expire entries second time around if the expiring cache is being used</li>
<li data-value="4">Display the Cache MBean front cache metrics</li>
<li data-value="5">Issue 10 more puts and gets which will cause the front cache to be pruned</li>
<li data-value="6">Display the Cache MBean front cache metrics and StorageManager metrics</li>
</ul>
</li>
</ol>
</div>

<h3 id="example-tests-1">Review the Tests</h3>
<div class="section">
<p>The main <code>SimpleNearCachingExample</code> class is exercised by running the following tests :</p>

<ul class="ulist">
<li>
<p>SimpleNearCachingExampleALLTest - uses <code>all</code> invalidation strategy and high units of 100</p>

</li>
<li>
<p>SimpleNearCachingExamplePRESENTTest - uses <code>present</code> invalidation strategy and high units of 100</p>

</li>
<li>
<p>SimpleExpiringNearCachingExampleTest - uses <code>all</code> invalidation strategy and front expiry of 3 seconds</p>

</li>
</ul>
<p>There are a number of invalidation strategies, described <a id="" title="" target="_blank" href="https://docs.oracle.com/pls/topic/lookup?ctx=en/middleware/standalone/coherence/14.1.1.0/install&amp;id=COHDG5219">here</a>,
but we will utilize the following for the tests above:</p>

<ul class="ulist">
<li>
<p><code>all</code> - This strategy instructs a near cache to listen to all back cache events. This strategy is optimal for read-heavy tiered access patterns where there is significant overlap between the different instances of front caches.</p>

</li>
<li>
<p><code>present</code> - This strategy instructs a near cache to listen to the back cache events related only to the items currently present in the front cache. This strategy works best when each instance of a front cache contains distinct subset of data relative to the other front cache instances (for example, sticky data access patterns).</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">The default strategy is <code>auto</code>, which is identical to the <code>present</code> strategy.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Review the <code>SimpleNearCachingExampleALLTest</code>
<markup
lang="java"

>public class SimpleNearCachingExampleALLTest {

    @Test
    public void testNearCacheAll() throws Exception {
        System.setProperty("coherence.log.level", "3");
        SimpleNearCachingExample example = new SimpleNearCachingExample("size-cache-all", "all");
        example.runExample();

        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">This test runs with a cache called <code>size-cache-all</code>, which matches the size limited near cache and
invalidation strategy of <code>all</code>.</p>
</div>
</li>
<li>
Review the <code>SimpleNearCachingExamplePRESENTTest</code>
<markup
lang="java"

>public class SimpleNearCachingExamplePRESENTTest {

    @Test
    public void testNearCachePresent() throws Exception {
        System.setProperty("coherence.log.level", "3");
        SimpleNearCachingExample example = new SimpleNearCachingExample("size-cache-present", "present");
        example.runExample();

        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">This test runs with a cache called <code>size-cache-present</code>, which matches the size limited near cache and
invalidation strategy of `present.</p>
</div>
</li>
<li>
Review the <code>SimpleExpiringNearCachingExampleTest</code>
<markup
lang="java"

>public class SimpleExpiringNearCachingExampleTest {

    @Test
    public void testExpiringNearCache() throws Exception {
        System.setProperty("coherence.log.level", "3");
        SimpleNearCachingExample example = new SimpleNearCachingExample("expiring-cache-all", "all");
        example.runExample();

        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">This test runs with a cache called <code>expiring-cache-all</code>, which matches the size limited and expiring near cache and
invalidation strategy of <code>all</code>.
Due to the expiry behaviour, the output will be different from the size limited cache.</p>
</div>
</li>
</ol>
</div>

<h3 id="run-example-1">Run the Examples</h3>
<div class="section">
<p>Run the examples using one of the methods below:</p>

<ol style="margin-left: 15px;">
<li>
Run directly from your IDE by running either of the following test classes:
<ul class="ulist">
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleNearCachingExampleALLTest or</p>

</li>
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleNearCachingExamplePRESENTTest</p>

</li>
<li>
<p>com.oracle.coherence.guides.nearcaching.SimpleExpiringNearCachingExampleTest</p>

</li>
</ul>
</li>
<li>
Run using Maven or Gradle
<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

<div class="admonition note">
<p class="admonition-inline">If you run one or more cache servers as described earlier, you will see additional StorageManager MBean output below.</p>
</div>
</li>
</ol>
<p><strong>SimpleNearCachingExampleALLTest Output</strong></p>

<p>This test will generate output similar to the following: (timestamps have been removed from output)</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): Running test with cache size-cache-all
&lt;Info&gt; (thread=main, member=1): Iteration #1 Total time for gets 38.094ms <span class="conum" data-value="1" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-all,nodeId=1,tier=front,loader=414493378 <span class="conum" data-value="2" />
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=100
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=0
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.0
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.37
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): Iteration #2 Total time for gets 0.143ms <span class="conum" data-value="3" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-all,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=200
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=100
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.5 <span class="conum" data-value="4" />
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.37
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): After extra 10 values put and get
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-all,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=210
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=110
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=109
&lt;Info&gt; (thread=main, member=1): Name: Size, value=90 <span class="conum" data-value="5" />
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.5190476190476191
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.36633663366336633
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=1 <span class="conum" data-value="6" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=StorageManager,service=DistributedCache,cache=size-cache-all,nodeId=1 <span class="conum" data-value="7" />
&lt;Info&gt; (thread=main, member=1): Name: ListenerRegistrations, value=1
&lt;Info&gt; (thread=main, member=1): Name: InsertCount, value=110</markup>

<ul class="colist">
<li data-value="1">Iteration #1 for gets takes 38.094ms which includes the time to populate the front cache</li>
<li data-value="2">The Cache MBean object name for the front cache and various metrics</li>
<li data-value="3">Iteration #2 for gets takes only 0.143ms which is considerably quicker due to the entries being in the front cache</li>
<li data-value="4">The Hit Probability is 0.5 or 50% as 100 out of 200 entries were read from the front cache</li>
<li data-value="5">After the extra puts and gets, we can see that the cache was pruned the size of the front cache is now 90</li>
<li data-value="6">Number of prune operations</li>
<li data-value="7">Because we are using the <code>all</code> invalidation strategy there is only 1 listener registered for all the entries</li>
</ul>
<p><strong>SimpleNearCachingExamplePRESENTTest Output</strong></p>

<p>The output is similar to the above output, but you will notice that the number of listeners registered are higher as we are using the <code>Present</code> strategy
that will register a listener for each entry in the front of the near cache.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): Running test with cache size-cache-present
&lt;Info&gt; (thread=main, member=1): Iteration #1 Total time for gets 38.474ms
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-present,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=100
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=0
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.0
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.39
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): Iteration #2 Total time for gets 0.236ms
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-present,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=200
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=100
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.5
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.39
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): After extra 10 values put and get
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=size-cache-present,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=210
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=110
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=100
&lt;Info&gt; (thread=main, member=1): Name: Size, value=89
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.47619047619047616
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.4818181818181818
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=1
&lt;Info&gt; (thread=main, member=1): Coherence:type=StorageManager,service=DistributedCache,cache=size-cache-present,nodeId=1
&lt;Info&gt; (thread=main, member=1): Name: ListenerRegistrations, value=110 <span class="conum" data-value="1" />
&lt;Info&gt; (thread=main, member=1): Name: InsertCount, value=110</markup>

<ul class="colist">
<li data-value="1">Number of listener registrations</li>
</ul>
<p><strong>SimpleExpiringNearCachingExampleTest Output</strong></p>

<p>The output of this test is slightly different from the previous two as and expiring cache is used.
See below for details of the output.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): Running test with cache expiring-cache-all
&lt;Info&gt; (thread=main, member=1): Iteration #1 Total time for gets 21.364ms <span class="conum" data-value="1" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=expiring-cache-all,nodeId=1,tier=front,loader=414493378 <span class="conum" data-value="2" />
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=100
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=0
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.0
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.2
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): Iteration #2 Total time for gets 0.543ms <span class="conum" data-value="3" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=expiring-cache-all,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=200
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=100
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=100
&lt;Info&gt; (thread=main, member=1): Name: Size, value=100
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.5 <span class="conum" data-value="4" />
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.2
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0
&lt;Info&gt; (thread=main, member=1): After extra 10 values put and get
&lt;Info&gt; (thread=main, member=1): Coherence:type=Cache,service=DistributedCache,name=expiring-cache-all,nodeId=1,tier=front,loader=414493378
&lt;Info&gt; (thread=main, member=1): Name: TotalGets, value=210
&lt;Info&gt; (thread=main, member=1): Name: TotalPuts, value=110
&lt;Info&gt; (thread=main, member=1): Name: CacheHits, value=110
&lt;Info&gt; (thread=main, member=1): Name: Size, value=10 <span class="conum" data-value="5" />
&lt;Info&gt; (thread=main, member=1): Name: HitProbability, value=0.5238095238095238
&lt;Info&gt; (thread=main, member=1): Name: AverageMissMillis, value=0.2
&lt;Info&gt; (thread=main, member=1): Name: CachePrunes, value=0 <span class="conum" data-value="6" />
&lt;Info&gt; (thread=main, member=1): Coherence:type=StorageManager,service=DistributedCache,cache=expiring-cache-all,nodeId=1
&lt;Info&gt; (thread=main, member=1): Name: ListenerRegistrations, value=1 <span class="conum" data-value="7" />
&lt;Info&gt; (thread=main, member=1): Name: InsertCount, value=110</markup>

<ul class="colist">
<li data-value="1">Iteration #1 for gets takes 21.364ms which includes the time to populate the front cache</li>
<li data-value="2">The Cache MBean object name for the front cache and various metrics</li>
<li data-value="3">Iteration #2 for gets takes only 0.543ms which is considerably quicker due to the entries being in the front cache</li>
<li data-value="4">The Hit Probability is 0.5 or 50% as 100 out of 200 entries were read from the front cache</li>
<li data-value="5">After the extra puts and gets, we can see the size is 10 as the total sleep time was &gt; 8 seconds which meant
the near cache expired 100 entries before the next 10 entries were added</li>
<li data-value="6">Number of prune operations are 0 as no size limiting pruning has been done</li>
<li data-value="7">Because we are using the <code>all</code> invalidation strategy there is only 1 listener registered for all the entries</li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this example you have seen how to use near caching within Coherence by covering the following:</p>

<ul class="ulist">
<li>
<p>Configured near caches</p>

</li>
<li>
<p>Set near cache size limits</p>

</li>
<li>
<p>Changed the invalidation strategy</p>

</li>
<li>
<p>Configured expiry and eviction policies</p>

</li>
<li>
<p>Explored MBeans related to near caching</p>

</li>
</ul>
</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html#GUID-5C066CC9-575F-4D7D-9D53-7BB674D69FD1">Understanding Near Caches</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/configuring-caches.html#GUID-F91E64DD-2C46-4ED9-BD41-04D2922312F6">Defining Near Cache Schemes</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/pls/topic/lookup?ctx=en/middleware/standalone/coherence/14.1.1.0/install&amp;id=COHDG5219">Near Cache Invalidation Strategies</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html#GUID-9E6ABD8C-AD27-48C7-9C57-2A90133CEB3A">Understanding Local Caches</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/cache-configuration-elements.html#GUID-2DA5531C-4D2B-4582-9ED7-012120122BB9">Near Cache local-scheme Configuration</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/administer/performance-tuning.html#GUID-20600648-0C50-4275-9AE2-782CE32CAC2D">Near Cache and Cluster-node Affinity</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/administer/performance-tuning.html#GUID-068ABDF6-19A2-4C54-8B5B-8D1059EFFFC7">Concurrent Near Cache Misses on a Specific Hot Key</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
