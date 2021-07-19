<doc-view>

<h2 id="_cache_stores">Cache Stores</h2>
<div class="section">
<p>This guide walks you through how to use and configure Cache Stores within Coherence.</p>

<p>Coherence supports transparent read/write caching of any data source, including databases,
web services, packaged applications and file systems; however, databases are the most common use case.</p>

<p>As shorthand, "database" is used to describe any back-end data source. Effective caches must
support both intensive read-only and read/write operations, and for read/write operations,
the cache and database must be kept fully synchronized. To accomplish caching of data sources,
Coherence supports Read-Through, Write-Through, Refresh-Ahead and Write-Behind caching. Coherence also
supports <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java//com/tangosol/net/cache/BinaryEntryStore.html">BinaryEntryStore</a> which provides access to the serialized form of entries for
data sources capable of manipulating those. A variant of <code>BinaryEntryStore</code> is
the <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java//com/tangosol/net/cache/NonBlockingEntryStore.html">NonBlockingEntryStore</a>
which, besides providing access to entries in their <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java//com/tangosol/util/BinaryEntry.html">BinaryEntry</a> form,
integrates with data sources with non-blocking APIs such as R2DBC or Kafka.</p>

<p>See the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/caching-data-sources.html#GUID-9FAD1BFB-5063-4995-B0A7-3C6F9C64F600">Coherence Documentation</a>
for detailed information on Cache Stores.</p>


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
<p><router-link to="#cache-loaders-and-stores" @click.native="this.scrollFix('#cache-loaders-and-stores')">CacheLoader and CacheStore Interface</router-link></p>

</li>
<li>
<p><router-link to="#simple-example" @click.native="this.scrollFix('#simple-example')">Simple Cache Store Example</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#simple-cache-loader" @click.native="this.scrollFix('#simple-cache-loader')">Simple CacheLoader</router-link></p>

</li>
<li>
<p><router-link to="#simple-cache-store" @click.native="this.scrollFix('#simple-cache-store')">Simple CacheStore</router-link></p>

</li>
<li>
<p><router-link to="#enable-write-behind" @click.native="this.scrollFix('#enable-write-behind')">Enable Write Behind</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#file-cache-store" @click.native="this.scrollFix('#file-cache-store')">File Cache Store Example</router-link></p>

</li>
<li>
<p><router-link to="#hsqldb-cache-store" @click.native="this.scrollFix('#hsqldb-cache-store')">HSQLDb Cache Store Example</router-link></p>

</li>
<li>
<p><router-link to="#expiring-hsqldb-cache-store" @click.native="this.scrollFix('#expiring-hsqldb-cache-store')">Refresh Ahead Expiring HSQLDb Cache Store Example</router-link></p>

</li>
<li>
<p><router-link to="#write-behind-hsqldb-cache-store" @click.native="this.scrollFix('#write-behind-hsqldb-cache-store')">Write Behind HSQLDb Cache Store Example</router-link></p>

</li>
<li>
<p><router-link to="#h2-non-blocking-entry-store" @click.native="this.scrollFix('#h2-non-blocking-entry-store')">H2 R2DBC Non Blocking Entry Store Example</router-link></p>

</li>
<li>
<p><router-link to="#pluggable" @click.native="this.scrollFix('#pluggable')">Pluggable Cache Stores</router-link></p>

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
<p>This code is written as a number of separate classes representing the different types of
cache stores and can be run as a series of Junit tests to show the functionality.</p>


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

<div class="admonition note">
<p class="admonition-inline">As this example consists of Junit tests, please add <code>-DskipTests</code> for Maven or <code>-x test</code> for Gradle.</p>
</div>
</div>
</div>

<h3 id="cache-loaders-and-stores">CacheLoader and CacheStore Interfaces</h3>
<div class="section">
<p>Before we go into some examples, we should review two interfaces that are key.</p>

<ul class="ulist">
<li>
<p>CacheLoader - <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java/com/tangosol/net/cache/CacheLoader.html">CacheLoader</a> - defines an interface for loading
individual entries via a key or a collection keys from a backend <code>database</code>.</p>

</li>
<li>
<p>CacheStore - <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java/com/tangosol/net/cache/CacheStore.html">CacheStore</a> - defines and interface for storing
ior erasing individual entries via a key or collection of keys into a backend <code>database</code>. This interface also
extends <code>CacheLoader</code>.</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">In the rest of this document we will refer to <code>CacheLoaders</code> and <code>CacheStores</code> as just "Cache Stores" for simplicity.</p>
</div>
<p>Coherence caches have an in-memory backing map on each storage-enabled member to store cache data. When
cache stores are defined against a cache, operations are carried out on the cache stores in addition to the backing map.
We will explain this in more detail below.</p>

</div>

<h3 id="simple-example">Simple Cache Store Example</h3>
<div class="section">
<p>Before we jump straight into using a "Database", we will demonstrate how CacheLoaders and CacheStores
work by implementing a mock cache loader that outputs messages to help us understand how this works behind the scenes.</p>


<h4 id="simple-cache-loader">Simple CacheLoader</h4>
<div class="section">
<p>The <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java/com/tangosol/net/cache/CacheLoader.html">CacheLoader</a> interface defines the following methods:</p>

<ul class="ulist">
<li>
<p><code>public V load(K key)</code> - Return the value associated with the specified key</p>

</li>
<li>
<p><code>public default Map&lt;K, V&gt; loadAll(Collection&lt;? extends K&gt; colKeys)</code> - Return the values associated with each the specified keys in the passed collection</p>

</li>
</ul>
<p>We just need to implement the <code>load</code> method. See below for the <code>SimpleCacheLoader</code> implementation.</p>

<p>The implementation of a <code>CacheLoader</code> is also known as <strong>Read-Through Caching</strong> as if the data is not present in the cache it is read from the cache loader.</p>

<ol style="margin-left: 15px;">
<li>
Review the SimpleCacheLoader
<markup
lang="java"

>public class SimpleCacheLoader implements CacheLoader&lt;Integer, String&gt; { <span class="conum" data-value="1" />

    private String cacheName;

    /**
     * Constructs a {@link SimpleCacheLoader}.
     *
     * @param cacheName cache name
     */
    public SimpleCacheLoader(String cacheName) {  <span class="conum" data-value="2" />
        this.cacheName = cacheName;
        Logger.info("SimpleCacheLoader constructed for cache " + this.cacheName);
    }

    /**
     * An implementation of a load which returns the String "Number " + the key.
     *
     * @param key key whose associated value is to be returned
     * @return the value for the given key
     */
    @Override
    public String load(Integer key) {  <span class="conum" data-value="3" />
        Logger.info("load called for key " + key);
        return "Number " + key;
    }
}</markup>

<ul class="colist">
<li data-value="1">Implement a <code>CacheLoader</code> with key <code>Integer</code> and value of <code>String</code></li>
<li data-value="2">Construct the cache loader passing in the cache name (not used in this case)</li>
<li data-value="3">Implement the <code>load</code> method by returning a String "Number " plus the key and log the message</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">We are just logging messages for the sake of this example, and we would recommend that logging only used in rare cases where you might need to signify an error.</p>
</div>
</li>
<li>
Review the Cache Configuration <code>simple-cache-loader-cache-config.xml</code>
<markup
lang="xml"

>  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;  <span class="conum" data-value="1" />
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;simple-cache-loader&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;simple-cache-loader&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme/&gt;
        &lt;/internal-cache-scheme&gt;
        &lt;cachestore-scheme&gt;  <span class="conum" data-value="2" />
          &lt;class-scheme&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.cachestores.SimpleCacheLoader&lt;/class-name&gt;   <span class="conum" data-value="3" />
            &lt;init-params&gt;
              &lt;init-param&gt;
                &lt;param-type&gt;java.lang.String&lt;/param-type&gt;   <span class="conum" data-value="4" />
                &lt;param-value&gt;{cache-name}&lt;/param-value&gt;
              &lt;/init-param&gt;
            &lt;/init-params&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<ul class="colist">
<li data-value="1">Cache mapping for all caches to the <code>simple-cache-loader</code> scheme</li>
<li data-value="2">Specifies this schema has a cache store</li>
<li data-value="3">Specify the class that implements the <code>CacheLoader</code> interface</li>
<li data-value="4">Pass the cache name using the in-built macro to the constructor</li>
</ul>
</li>
<li>
Review the Test
<markup
lang="java"

>public class SimpleCacheLoaderTest
        extends AbstractCacheStoreTest {

    @BeforeAll
    public static void startup() {
        startupCoherence("simple-cache-loader-cache-config.xml"); <span class="conum" data-value="1" />
    }

    @Test
    public void testSimpleCacheLoader() {
        NamedMap&lt;Integer, String&gt; namedMap = getSession()
                .getMap("simple-test", TypeAssertion.withTypes(Integer.class, String.class)); <span class="conum" data-value="2" />

        namedMap.clear();

        // initial get will cause read-through and the object is placed in the cache and returned to the user
        assertEquals("Number 1", namedMap.get(1));  <span class="conum" data-value="3" />
        assertEquals(1, namedMap.size());

        // subsequent get will not cause read-through as value is already in cache
        assertEquals("Number 1", namedMap.get(1));  <span class="conum" data-value="4" />

        // Remove the cache entry will cause a read-through again
        namedMap.remove(1);  <span class="conum" data-value="5" />
        assertEquals("Number 1", namedMap.get(1));
        assertEquals(1, namedMap.size());

        // load multiple keys will load all values
        namedMap.getAll(new HashSet&lt;&gt;(Arrays.asList(2, 3, 4)));  <span class="conum" data-value="6" />
        assertEquals(4, namedMap.size());
    }
}</markup>

<ul class="colist">
<li data-value="1">Startup the test with the specified cache config</li>
<li data-value="2">Obtain the <code>NamedMap</code></li>
<li data-value="3">Issue a get against the key <strong>1</strong> and as the cache entry is not present, the value will be loaded from the cache store and placed in the cache and returned to the user. See the message from the cache store.</li>
<li data-value="4">Issue a second get against the key <strong>1</strong> and the cache store is not called and returned from the cache</li>
<li data-value="5">Remove the cache entry for key <strong>1</strong> and re-issue the get. The value is read-through from the cache store.</li>
<li data-value="6">Load a <code>Collection</code> of keys, causing each one to be loaded from cache loader.</li>
</ul>
</li>
<li>
Run the Test
<p>For this test and all others you can run the test in one of three ways:</p>

<ul class="ulist">
<li>
<p>Using your IDE</p>

</li>
<li>
<p>Using Maven via <code>mvn clean verify -Dtest=SimpleCacheLoaderTest verify</code></p>

</li>
<li>
<p>Using Gradle via <code>./gradlew test --tests SimpleCacheLoaderTest</code></p>
<p>Running the test shows the following (abbreviated) output on the cache server, where the cache store is running.</p>

<markup
lang="text"

>... &lt;Info&gt; (thread=DistributedCache, member=1): SimpleCacheLoader constructed for cache simple-test
...
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:6, member=1): load called for key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:6, member=1): load called for key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:6, member=1): load called for key 4
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:6, member=1): load called for key 2
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:6, member=1): load called for key 3</markup>

<div class="admonition note">
<p class="admonition-inline">Notice there are two loads of the key <strong>1</strong> which are the first get and subsequent get after the value was removed. The following loads are fom the getAll().</p>
</div>
</li>
</ul>
</li>
</ol>
</div>

<h4 id="simple-cache-store">Simple CacheStore</h4>
<div class="section">
<p>The <a id="" title="" target="_blank" href="https://coherence.community/21.12-SNAPSHOT/api/java/com/tangosol/net/cache/CacheStore.html">CacheStore</a> interface defines the following methods:</p>

<ul class="ulist">
<li>
<p><code>public void store(K key, V value)</code> - Store the specified value under the specified key in the underlying  store</p>

</li>
<li>
<p><code>public default void storeAll(Map&lt;? extends K, ? extends V&gt; mapEntries)</code> - Store the specified values under the specified keys in the underlying store</p>

</li>
<li>
<p><code>public void erase(K key)</code> - Remove the specified key from the underlying store if present</p>

</li>
<li>
<p><code>public default void eraseAll(Collection&lt;? extends K&gt; colKeys)</code> - Remove the specified keys from the underlying store if present</p>

</li>
</ul>
<p>Our implementation will extend the <code>SimpleCacheLoader</code> and implement the <code>store</code> and <code>erase</code> methods. See below for the <code>SimpleCacheStore</code> implementation.</p>

<p>The implementation of a <code>CacheStore</code> is also known as <strong>Write-Through Caching</strong> as when the data is written to the cache
it is also written through to the back end cache store in the same synchronous operation as the primate and backup.
E.g. the client will block until primary, backup and cache store operations are complete.
See <router-link to="#enable-write-behind" @click.native="this.scrollFix('#enable-write-behind')">write-behind</router-link> on changing this behaviour.</p>

<div class="admonition note">
<p class="admonition-inline">We can change</p>
</div>
<ol style="margin-left: 15px;">
<li>
Review the SimpleCacheStore
<markup
lang="java"

>public class SimpleCacheStore
        extends SimpleCacheLoader
        implements CacheStore&lt;Integer, String&gt; { <span class="conum" data-value="1" />

    /**
     * Constructs a {@link SimpleCacheStore}.
     *
     * @param cacheName cache name
     */
    public SimpleCacheStore(String cacheName) {  <span class="conum" data-value="2" />
        super(cacheName);
        Logger.info("SimpleCacheStore instantiated for cache " + cacheName);
    }

    @Override
    public void store(Integer integer, String s) {  <span class="conum" data-value="3" />
        Logger.info("Store key " + integer + " with value " + s);
    }

    @Override
    public void erase(Integer integer) {  <span class="conum" data-value="4" />
        Logger.info("Erase key " + integer);
    }
}</markup>

<ul class="colist">
<li data-value="1">Implement a <code>CacheStore</code> with key <code>Integer</code> and value of <code>String</code> which extends <code>SimpleCacheLoader</code></li>
<li data-value="2">Construct the cache store passing in the cache name (not used in this case)</li>
<li data-value="3">Implement the <code>store</code> method by logging a message</li>
<li data-value="4">Implement the <code>erase</code> method by logging a message</li>
</ul>
</li>
<li>
Review the Cache Configuration <code>simple-cache-store-cache-config.xml</code>
<markup
lang="xml"

>  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;  <span class="conum" data-value="1" />
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;simple-cache-store&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;simple-cache-store&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme/&gt;
        &lt;/internal-cache-scheme&gt;
        &lt;cachestore-scheme&gt;  <span class="conum" data-value="2" />
          &lt;class-scheme&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.cachestores.SimpleCacheStore&lt;/class-name&gt;   <span class="conum" data-value="3" />
            &lt;init-params&gt;
              &lt;init-param&gt;
                &lt;param-type&gt;java.lang.String&lt;/param-type&gt; <span class="conum" data-value="4" />
                &lt;param-value&gt;{cache-name}&lt;/param-value&gt;
              &lt;/init-param&gt;
            &lt;/init-params&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
        &lt;write-delay&gt;0s&lt;/write-delay&gt;
        &lt;write-batch-factor&gt;0&lt;/write-batch-factor&gt;
        &lt;write-requeue-threshold&gt;0&lt;/write-requeue-threshold&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<ul class="colist">
<li data-value="1">Cache mapping for all caches to the <code>simple-cache-store</code> scheme</li>
<li data-value="2">Specifies this schema has a cache store</li>
<li data-value="3">Specify the class that implements the <code>CacheStore</code> interface</li>
<li data-value="4">Pass the cache name using the in-built macro to the constructor</li>
</ul>
</li>
<li>
Review the Test
<markup
lang="java"

>public class SimpleCacheStoreTest
        extends AbstractCacheStoreTest {

    @BeforeAll
    public static void startup() {
        startupCoherence("simple-cache-store-cache-config.xml"); <span class="conum" data-value="1" />
    }

    @Test
    public void testSimpleCacheStore() {
        NamedMap&lt;Integer, String&gt; namedMap = getSession()
                .getMap("simple-test", TypeAssertion.withTypes(Integer.class, String.class)); <span class="conum" data-value="2" />

        namedMap.clear();

        // initial get will cause read-through and the object is placed in the cache and returned to the user
        assertEquals("Number 1", namedMap.get(1));  <span class="conum" data-value="3" />
        assertEquals(1, namedMap.size());

        // update the cache and the the store method is called
        namedMap.put(1, "New Value"); <span class="conum" data-value="4" />
        assertEquals("New Value", namedMap.get(1));

        // remove the entry from the cache and the erase method is called
        assertEquals("New Value", namedMap.remove(1));  <span class="conum" data-value="5" />

        // Get the cache entry will cause a read-through again (cache loader)
        assertEquals("Number 1", namedMap.get(1));   <span class="conum" data-value="6" />
        assertEquals(1, namedMap.size());

        // Issue a puAll
        Map&lt;Integer, String&gt; map = new HashMap&lt;&gt;();
        map.put(2, "value 2");
        map.put(3, "value 3");
        map.put(4, "value 4");
        namedMap.putAll(map);  <span class="conum" data-value="7" />
        assertEquals(4, namedMap.size());

        Base.sleep(20000L);
    }
}</markup>

<ul class="colist">
<li data-value="1">Startup the test with the specified cache config</li>
<li data-value="2">Obtain the <code>NamedMap</code></li>
<li data-value="3">Issue a get against the key <strong>1</strong> and as the cache entry is not present, the value will be loaded from the cache store. (This is the SimpleCacheLoader.load() method)</li>
<li data-value="4">Issue a put against the key <strong>1</strong> and the cache store <code>store</code> method is called and the message is logged</li>
<li data-value="5">Remove the cache entry for key <strong>1</strong> and the cache store <code>erase</code> method is called and a message is logged</li>
<li data-value="6">Issue a get against the key <strong>1</strong> and it will be loaded my the cache loader</li>
<li data-value="7">Issue a <code>putAll</code> on the cache and the cache store <code>storeAll</code> method is called</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">We are not exercising the <code>eraseAll</code> method as this is used internally.</p>
</div>
</li>
<li>
Run the Test, using Maven in our case
<markup
lang="bash"

>mvn clean verify -Dtest=SimpleCacheStoreTest verify</markup>

<p>Running the test shows the following (abbreviated) output on the cache server, where the cache store is running.</p>

<markup
lang="text"

>... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): load called for key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): Store key 1 with value New Value
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): Erase key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): load called for key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): Store key 4 with value value 4
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): Store key 2 with value value 2
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:7, member=1): Store key 3 with value value 3</markup>

<div class="admonition note">
<p class="admonition-inline">Notice the store and erase for key <strong>1</strong> and the store for key <strong>2</strong>, <strong>3</strong> and <strong>4</strong> from the <code>putAll</code></p>
</div>
</li>
</ol>
</div>

<h4 id="enable-write-behind">Enable Write Behind</h4>
<div class="section">
<p>Typically, the time taken to write the primary and backup copy of an object is much less that writing to a back-end
data store such as a database. These operations may be many orders of magnitude slower e.g. 1-2 ms to write primary and backup and
100-200ms to write to a database.</p>

<p>In these cases we can change a cache store to use write-behind. In the Write-Behind scenario, modified cache entries are asynchronously
written to the data source after a configured delay, whether after 10 seconds or a day.
This only applies to cache inserts and updates - cache entries are removed synchronously from the data source.</p>

<p>See the <a id="" title="" target="_blank" href="https://docs.oracle.com/middleware/12213/coherence/develop-applications/caching-data-sources.htm#COHDG5181">Coherence Documentation</a> for detailed
information and explanations on write-behind.</p>

<p>The advantages of write-behind are:
1. Improved application performance as the client does not have to wait for the value to be written to the back-end cache store. As long as the primary and backup are complete, the control is returned to the client.
2. The back-end cache store, usually a database, can more efficiently batch updates that one at a time
3. The application can be mostly immune from back-end database failures as the failure can be requeued.</p>

<ol style="margin-left: 15px;">
<li>
Open the Cache Configuration <code>simple-cache-store-cache-config.xml</code> and change the value of the <code>write-delay</code> from the default value of <code>0s</code> to <code>5s</code>.
This simple change will make the cache store write-behind with a delay of 5 seconds before entries are written to the cache.
<markup
lang="xml"

>&lt;write-delay&gt;0s&lt;/write-delay&gt;</markup>

</li>
<li>
Uncomment out the sleep in the <code>SimpleCacheStoreTest</code> class.
<div class="admonition note">
<p class="admonition-inline">This is to ensure that the unit test does not exit before the values are written asynchronously to the cache store. This is not required in production systems.</p>
</div>
<markup
lang="java"

>        Base.sleep(20000L);</markup>

</li>
<li>
Run the <code>SimpleCacheStoreTest</code> test
<markup
lang="text"

>... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): load called for key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): Erase key 1
... &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): load called for key 1

DELAY of approx 5s

... &lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.SimpleCacheStore):DistributedCache:simple-test, member=1): Store key 4 with value value 4
... &lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.SimpleCacheStore):DistributedCache:simple-test, member=1): Store key 2 with value value 2
... &lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.SimpleCacheStore):DistributedCache:simple-test, member=1): Store key 3 with value value 3</markup>

<div class="admonition note">
<p class="admonition-inline">You will see that there is a delay of at least 5 seconds before the stores for keys 2, 3 and 4. You can see that they are on the thread <code>WriteBehindThread</code>. The <code>load</code> and <code>erase</code>
operations are on a <code>DistributedCacheWorker</code> thread and are executed as synchronous operations.</p>
</div>
</li>
</ol>
</div>
</div>

<h3 id="file-cache-store">File Cache Store Example</h3>
<div class="section">
<p>In this next example, we will create a file-based cache store which stores values in files with the name of the key
under a specific directory. This is to show how a back-end cache store, and the cache interact.</p>

<div class="admonition note">
<p class="admonition-inline">This is an example only to see how cache stores work under the covers and will not work with multiple cache servers running and is not recommended for production use.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Review the FileCacheStore
<markup
lang="java"

>public class FileCacheStore
        implements CacheStore&lt;Integer, String&gt; {  <span class="conum" data-value="1" />

    /**
     * Base directory off which to store data.
     */
    private final File directory;

    public FileCacheStore(String directoryName) {  <span class="conum" data-value="2" />
        if (directoryName == null || directoryName.equals("")) {
            throw new IllegalArgumentException("A directory must be specified");
        }

        directory = new File(directoryName);
        if (!directory.isDirectory() || !directory.canWrite()) {
            throw new IllegalArgumentException("Unable to open directory " + directory);
        }
        Logger.info("FileCacheStore constructed with directory " + directory);
    }

    @Override
    public void store(Integer key, String value) {  <span class="conum" data-value="3" />
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(directory, key), false));
            writer.write(value);
            writer.close();
        }
        catch (IOException e) {
            throw new RuntimeException("Unable to delete key " + key, e);
        }
    }

    @Override
    public void erase(Integer key) {  <span class="conum" data-value="4" />
        // we ignore result of delete as the key may not exist
        getFile(directory, key).delete();
    }

    @Override
    public String load(Integer key) {  <span class="conum" data-value="5" />
        File file = getFile(directory, key);
        try {
            // use Java 1.8 method
            return Files.readAllLines(file.toPath()).get(0);
        }
        catch (IOException e) {
            return null;  // does not exist in cache store
        }
    }

    protected static File getFile(File directory, Integer key) {
        return new File(directory, key + ".txt");
    }
}</markup>

<ul class="colist">
<li data-value="1">Implement a <code>CacheStore</code> with key <code>Integer</code> and value of <code>String</code> which extends <code>SimpleCacheLoader</code></li>
<li data-value="2">Construct the cache store passing in the directory to use</li>
<li data-value="3">Implement the <code>store</code> method by writing the String value to a file in the base directory with the key + ".txt" as the name</li>
<li data-value="4">Implement the <code>erase</code> method by removing the file with the key + ".txt" as the name</li>
<li data-value="5">Implement the <code>load</code> method by loading the contents of the file with the key + ".txt" as the name</li>
</ul>
</li>
<li>
Review the Cache Configuration <code>file-cache-store-cache-config.xml</code>
<markup
lang="xml"

>  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;file-cache-store&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;file-cache-store&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme/&gt;
        &lt;/internal-cache-scheme&gt;
        &lt;cachestore-scheme&gt;
          &lt;class-scheme&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.cachestores.FileCacheStore&lt;/class-name&gt;   <span class="conum" data-value="1" />
            &lt;init-params&gt;
              &lt;init-param&gt;
                &lt;param-type&gt;java.lang.String&lt;/param-type&gt; <span class="conum" data-value="2" />
                &lt;param-value system-property="test.base.dir"&gt;/tmp/&lt;/param-value&gt;
              &lt;/init-param&gt;
            &lt;/init-params&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
        &lt;write-delay&gt;0s&lt;/write-delay&gt;
        &lt;write-batch-factor&gt;0&lt;/write-batch-factor&gt;
        &lt;write-requeue-threshold&gt;0&lt;/write-requeue-threshold&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<ul class="colist">
<li data-value="1">Specify the class that implements the <code>CacheStore</code> interface</li>
<li data-value="2">Pass the directory to the constructor and optionally using a system property to override</li>
</ul>
</li>
<li>
Uncomment the commented line below to a directory of your choice which must already exist. Comment out the line containg the <code>FileHelper</code> call.
<markup
lang="java"

>baseDirectory = FileHelper.createTempDir();
// baseDirectory = new File("/tmp/tim");</markup>

<p>Also comment out the <code>deleteDirectory</code> below so you can look at the contents of the directory.</p>

<markup
lang="java"

>FileHelper.deleteDir(baseDirectory);</markup>

</li>
<li>
Inspect the contents of your directory:
<markup
lang="bash"

>$ ls -l /tmp/tim
total 64
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 2.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 3.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 4.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 5.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 6.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 7.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 8.txt
-rw-r--r--  1 timmiddleton  wheel  8 18 Feb 14:37 9.txt</markup>

<p>You will see there are 8 files for the 8 entries that were written to the cache store. entry <code>1.txt</code> was removed so does not exist in the cache store.</p>

</li>
<li>
Create a file <code>1.txt</code> in the directory and put the text <code>One</code>. Re-run the test.
<p>You will notice that the test fails as when the test issues the following assertion as the value was not in the cache, but it was in the cache store and loaded into memory:</p>

<markup
lang="java"

>assertNull(namedMap.get(1));</markup>

<markup
lang="bash"

>org.opentest4j.AssertionFailedError:
Expected :null
Actual   :One</markup>

</li>
</ol>
</div>

<h3 id="hsqldb-cache-store">HSQLDb Cache Store Example</h3>
<div class="section">
<p>In this next example, we will manually create a database backed cache store using a HSQLDb database in embedded mode.
This will show how a cache store could interact with a back-end database.</p>

<div class="admonition note">
<p class="admonition-inline">In this example we are using an embedded HSQLDb database just as an example and normally the back-end database would
be on a physically separate machine and not in-memory.</p>
</div>
<p>In this example we are storing a simple <code>Customer</code> class in our cache and cache-store. Continue below to review the <code>HSQLDbCacheStore</code> class.</p>

<p><strong>Review the HSQLDbCacheStore</strong></p>

<ol style="margin-left: 15px;">
<li>
Specify the class that implements the <code>CacheStore</code> interface
<markup
lang="java"

>public class HSQLDbCacheStore
        extends Base
        implements CacheStore&lt;Integer, Customer&gt; {</markup>

</li>
<li>
Construct the CacheStore passing the cache name to the constructor
<markup
lang="java"

>/**
 * Construct a cache store.
 *
 * @param cacheName cache name
 *
 * @throws SQLException if any SQL errors
 */
public HSQLDbCacheStore(String cacheName) throws SQLException {
    this.tableName = cacheName;
    dbConn = DriverManager.getConnection(DB_URL);
    Logger.info("HSQLDbCacheStore constructed with cache Name " + cacheName);
}</markup>

</li>
<li>
Implement the <code>load</code> method by selecting the customer from the database based upon the primary key of <code>id</code>
<markup
lang="java"

>@Override
public Customer load(Integer key) {
    String            query     = "SELECT id, name, address, creditLimit FROM " + tableName + " where id = ?";
    PreparedStatement statement = null;
    ResultSet         resultSet = null;

    try {
        statement = dbConn.prepareStatement(query);
        statement.setInt(1, key);
        resultSet = statement.executeQuery();

        return resultSet.next() ? createFromResultSet(resultSet) : null;
    }
    catch (SQLException sqle) {
        throw ensureRuntimeException(sqle);
    }
    finally {
        close(resultSet);
        close(statement);
    }
}</markup>

</li>
<li>
Implement the <code>store</code> method by calling <code>storeInternal</code> and then issuing a commit.
<markup
lang="java"

>@Override
public void store(Integer key, Customer customer) {
    try {
        storeInternal(key, customer);
        dbConn.commit();
    }
    catch (Exception e) {
        throw ensureRuntimeException(e);
    }
}</markup>

</li>
<li>
Internal implementation of <code>store</code> to be re-used by <code>store</code> and <code>storeAll</code> to insert or update the record in the database
<markup
lang="java"

>/**
 * Store a {@link Customer} object using the id. This method does not issue a
 * commit so that either the store or storeAll method can reuse this.
 *
 * @param key      customer id
 * @param customer {@link Customer} object
 */
private void storeInternal(Integer key, Customer customer) {
    // the following is very inefficient; it is recommended to use DB
    // specific functionality that is, REPLACE for MySQL or MERGE for Oracle
    String query = load(key) != null
                   ? "UPDATE " + tableName + " SET name = ?, address = ?, creditLimit = ? where id = ?"
                   : "INSERT INTO " + tableName + " (name, address, creditLimit, id) VALUES(?, ?, ?, ?)";
    PreparedStatement statement = null;

    try {
        statement = dbConn.prepareStatement(query);
        statement.setString(1, customer.getName());
        statement.setString(2, customer.getAddress());
        statement.setInt(3, customer.getCreditLimit());
        statement.setInt(4, customer.getId());
        statement.execute();
    }
    catch (SQLException sqle) {
        throw ensureRuntimeException(sqle);
    }
    finally {
        close(statement);
    }
}</markup>

</li>
<li>
Implement the storeAll method
<markup
lang="java"

>@Override
public void storeAll(Map&lt;? extends Integer, ? extends Customer&gt; mapEntries) {
    try {
        for (Customer customer : mapEntries.values()) {
            storeInternal(customer.getId(), customer);
        }

        dbConn.commit();
        Logger.info("Ran storeAll on " + mapEntries.size() + " entries");
    }
    catch (Exception e) {
        try {
            dbConn.rollback();
        }
        catch (SQLException ignore) { }
        throw ensureRuntimeException(e);
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">The <code>storeAll</code> method will use a single transaction to insert/update all values. This method will be used internally for write-behind only.</p>
</div>
</li>
<li>
Implement the <code>erase</code> method by removing the entry from the database.
<markup
lang="java"

>@Override
public void erase(Integer key) {  <span class="conum" data-value="7" />
    String            query     = "DELETE FROM " + tableName + " where id = ?";
    PreparedStatement statement = null;

    try {
        statement = dbConn.prepareStatement(query);
        statement.setInt(1, key);
        statement.execute();
        dbConn.commit();
    }
    catch (SQLException sqle) {
        throw ensureRuntimeException(sqle);
    }
    finally {
        close(statement);
    }
}</markup>

</li>
</ol>
<p><strong>Review the Cache Configuration</strong></p>

<p>Review the Cache Configuration <code>hsqldb-cache-store-cache-config.xml</code></p>

<markup
lang="xml"

>&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt;  <span class="conum" data-value="1" />
    &lt;cache-name&gt;Customer&lt;/cache-name&gt;
    &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;
  &lt;cache-mapping&gt; <span class="conum" data-value="2" />
    &lt;cache-name&gt;CustomerExpiring&lt;/cache-name&gt;
    &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
    &lt;init-params&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;back-expiry&lt;/param-name&gt;   <span class="conum" data-value="3" />
        &lt;param-value&gt;20s&lt;/param-value&gt;
      &lt;/init-param&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;refresh-ahead-factor&lt;/param-name&gt;  <span class="conum" data-value="4" />
        &lt;param-value&gt;0.5&lt;/param-value&gt;
      &lt;/init-param&gt;
    &lt;/init-params&gt;
  &lt;/cache-mapping&gt;
  &lt;cache-mapping&gt; <span class="conum" data-value="2" />
    &lt;cache-name&gt;CustomerWriteBehind&lt;/cache-name&gt;
    &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
    &lt;init-params&gt;
      &lt;init-param&gt;
        &lt;param-name&gt;write-delay&lt;/param-name&gt;
        &lt;param-value&gt;10s&lt;/param-value&gt;
      &lt;/init-param&gt;
    &lt;/init-params&gt;
  &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme&gt;
            &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
            &lt;expiry-delay&gt;{back-expiry 0}&lt;/expiry-delay&gt;
          &lt;/local-scheme&gt;
        &lt;/internal-cache-scheme&gt;
        &lt;cachestore-scheme&gt;
          &lt;class-scheme&gt;
            &lt;class-name&gt;    <span class="conum" data-value="5" />
              com.oracle.coherence.guides.cachestores.HSQLDbCacheStore
            &lt;/class-name&gt;
            &lt;init-params&gt;
              &lt;init-param&gt;
                &lt;!-- Normally the assumption is the cache name will be the same as the table name
                     but in this example we are hard coding the table name --&gt;
                &lt;param-type&gt;java.lang.String&lt;/param-type&gt; <span class="conum" data-value="6" />
                &lt;param-value&gt;Customer&lt;/param-value&gt;
              &lt;/init-param&gt;
            &lt;/init-params&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
        &lt;write-delay&gt;{write-delay 0s}&lt;/write-delay&gt;
        &lt;write-batch-factor&gt;0&lt;/write-batch-factor&gt;
        &lt;write-requeue-threshold&gt;0&lt;/write-requeue-threshold&gt;
        &lt;refresh-ahead-factor&gt;{refresh-ahead-factor 0.0}&lt;/refresh-ahead-factor&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<ul class="colist">
<li data-value="1">Cache mapping for Customer cache to the <code>hsqldb-cache-loader</code> scheme</li>
<li data-value="2">Cache mapping for CustomerExpiring cache to the <code>hsqldb-cache-loader</code> scheme (see next section)</li>
<li data-value="3">Set the expiry to 20 seconds for the expiring cache</li>
<li data-value="4">Override the refresh-ahead factor for the expiring cache</li>
<li data-value="5">Specify the class that implements the <code>CacheStore</code> interface</li>
<li data-value="6">Specify the cache name</li>
</ul>
<p><strong>Run the Unit Test</strong></p>

<p>Next we will run the <code>HSQLDbCacheStoreTest.java</code> unit test below and observe the behaviour.</p>

<ol style="margin-left: 15px;">
<li>
Start and confirm NamedMap and database contents.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=initial]</markup>

</li>
<li>
Issue an initial get on the NamedMap and validate the object is read from the cache store.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=load1]</markup>

<div class="admonition note">
<p class="admonition-inline">You will see a message similar to the following indicating the time to retrieve a NamedMap entry that is not in the cache.
<code>(thread=main, member=1): Time for read-through 17.023 ms</code></p>
</div>
</li>
<li>
Issue a second get, the entry will be retrieved directly from memory and not the cache store.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=load2]</markup>

<div class="admonition note">
<p class="admonition-inline">You will see a message similar to the following indicating the time to retrieve a NamedMap entry is significantly quicker.
<code>(thread=main, member=1): Time for no read-through 0.889 ms</code></p>
</div>
</li>
<li>
Remove and entry from the NamedMap and the value should be removed from the underlying store.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=remove]</markup>

</li>
<li>
Issue a get for another customer and then update the customer details.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=update]</markup>

</li>
<li>
Add a new customer and ensure it is created in the database. Then remove the same customer.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=addRemove]</markup>

</li>
<li>
Clear the NamedMap and show how to preload the data from the cache store.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreTest.java[tag=loadData]</markup>

</li>
</ol>
</div>

<h3 id="expiring-hsqldb-cache-store">Refresh Ahead HSQLDb Cache Store Example</h3>
<div class="section">
<p>In this next example, we use the <code>CustomerExpiring</code> cache which will expire data after 20 seconds and also has a
refresh-ahead-factor of 0.5 meaning that if the cache is accessed after 10 seconds then an asynchronous refresh-ahead
will be performed to speed up the next access to the data.</p>

<p><strong>Review the Cache Configuration</strong></p>

<p>The <code>hsqldb-cache-store-cache-config.xml</code> below shows the <code>CustomerExpiring</code> cache passing in parameters to the <code>caching-scheme</code> to override
expiry and refresh ahead values.</p>

<markup
lang="xml"

>&lt;cache-mapping&gt; <span class="conum" data-value="2" />
  &lt;cache-name&gt;CustomerExpiring&lt;/cache-name&gt;
  &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
  &lt;init-params&gt;
    &lt;init-param&gt;
      &lt;param-name&gt;back-expiry&lt;/param-name&gt;   <span class="conum" data-value="3" />
      &lt;param-value&gt;20s&lt;/param-value&gt;
    &lt;/init-param&gt;
    &lt;init-param&gt;
      &lt;param-name&gt;refresh-ahead-factor&lt;/param-name&gt;  <span class="conum" data-value="4" />
      &lt;param-value&gt;0.5&lt;/param-value&gt;
    &lt;/init-param&gt;
  &lt;/init-params&gt;
&lt;/cache-mapping&gt;</markup>

<p>The <code>local-scheme</code> uses the <code>back-expiry</code> parameter passed in:</p>

<markup
lang="xml"

>&lt;local-scheme&gt;
  &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
  &lt;expiry-delay&gt;{back-expiry 0}&lt;/expiry-delay&gt;
&lt;/local-scheme&gt;</markup>

<p>The <code>read-write-backing-map-scheme</code> uses the <code>refresh-ahead-factor</code> parameter passed in:</p>

<markup
lang="xml"

>&lt;refresh-ahead-factor&gt;{refresh-ahead-factor 0.0}&lt;/refresh-ahead-factor&gt;</markup>

<p><strong>Run the Unit Test</strong></p>

<p>Next we will run the <code>HSQLDbCacheStoreExpiringTest.java</code> unit test below and observe the behaviour.</p>

<ol style="margin-left: 15px;">
<li>
Start and confirm NamedMap and database contents.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreExpiringTest.java[tag=initial]</markup>

</li>
<li>
Issue a get for customer 1 and log the time to load
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreExpiringTest.java[tag=readThrough1]</markup>

<div class="admonition note">
<p class="admonition-inline">Notice the initial read through time similar to the following in the log: <code>(thread=main, member=1): Time for read-through 19.129 ms</code></p>
</div>
</li>
<li>
Update the credit limit to 10000 in the database for customer 1 and ensure that after 11 seconds the value is still 5000 in the NamedMap.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreExpiringTest.java[tag=readThrough2]</markup>

<div class="admonition note">
<p class="admonition-inline">The get within the 10 seconds (20s * 0.5), will cause an asynchronous refresh-ahead.</p>
</div>
</li>
<li>
Wait for 10 seconds and then retrieve the customer object which has been updated.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreExpiringTest.java[tag=readThrough3]</markup>

<div class="admonition note">
<p class="admonition-inline">Notice the time to retrieve the entry is significantly reduced: <code>(thread=main, member=1): Time for after refresh-ahead 1.116 ms</code></p>
</div>
</li>
</ol>
</div>

<h3 id="write-behind-hsqldb-cache-store">Write Behind HSQLDb Cache Store Example</h3>
<div class="section">
<p>In this HSQLDb cache store example, we use the <code>CustomerWriteBehind</code> cache which has a write delay of 10 seconds.</p>

<p><strong>Review the Cache Configuration</strong></p>

<p>The <code>hsqldb-cache-store-cache-config.xml</code> below shows the <code>CustomerWriteBehind</code> cache passing in parameters to the <code>caching-scheme</code> to override
<code>write-delay</code> value.</p>

<markup
lang="xml"

>&lt;cache-mapping&gt; <span class="conum" data-value="2" />
  &lt;cache-name&gt;CustomerWriteBehind&lt;/cache-name&gt;
  &lt;scheme-name&gt;hsqlb-cache-store&lt;/scheme-name&gt;
  &lt;init-params&gt;
    &lt;init-param&gt;
      &lt;param-name&gt;write-delay&lt;/param-name&gt;
      &lt;param-value&gt;10s&lt;/param-value&gt;
    &lt;/init-param&gt;
  &lt;/init-params&gt;
&lt;/cache-mapping&gt;</markup>

<p><strong>Run the Unit Test</strong></p>

<p>Next we will run the <code>HSqlDbCacheStoreExpiringTest</code> unit test below and observe the behaviour.</p>

<ol style="margin-left: 15px;">
<li>
Start and confirm NamedMap and database contents. In this example we are not preloading the database.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreWriteBehindTest.java[tag=initial]</markup>

</li>
<li>
Insert 10 customers using an efficient <code>putAll</code> operation and confirm the data is not yet in the cache.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreWriteBehindTest.java[tag=insert]</markup>

</li>
<li>
Wait till after the write-delay has passed and confirm that the customers are in the database.
<markup
lang="java"

>Unresolved directive in README.adoc - include::src/test/java/com/oracle/coherence/guides/cachestores/HSQLDbCacheStoreWriteBehindTest.java[tag=wait]</markup>

<div class="admonition note">
<p class="admonition-inline">You will notice that you should see messages indicating 100 entries have been written. You may also see multiple writes as the data will be added in different partitions.
load.</p>
</div>
</li>
</ol>
<div class="listing">
<pre>&lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.HSQLDbCacheStore):DistributedCache:CustomerWriteBehind, member=1):
   Ran storeAll on 3 entries
&lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.HSQLDbCacheStore):DistributedCache:CustomerWriteBehind, member=1):
   Ran storeAll on 97 entries
OR
&lt;Info&gt; (thread=WriteBehindThread:CacheStoreWrapper(com.oracle.coherence.guides.cachestores.HSQLDbCacheStore):DistributedCache:CustomerWriteBehind, member=1):
   Ran storeAll on 10 entries</pre>
</div>

</div>

<h3 id="h2-non-blocking-entry-store">H2 R2DBC Non Blocking Entry Store Example</h3>
<div class="section">
<p>In this H2 R2DBC cache store example, we use the <code>H2Person</code> cache which implements the <code>NonBlockingEntryStore</code> for non-blocking APIs
and access to entries in their serialized (<code>BinaryEntry</code>) form.</p>

<p><strong>Review the Cache Configuration</strong></p>

<p>The <code>h2r2dbc-entry-store-cache-config.xml</code> below shows the <code>H2Person</code> cache specifying the class name of the <code>NonBlockingEntryStore</code> implementation.</p>

<markup
lang="xml"

>&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;H2Person&lt;/cache-name&gt;
    &lt;scheme-name&gt;distributed-h2r2dbc&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;distributed-h2r2dbc&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme&gt;&lt;/local-scheme&gt;
        &lt;/internal-cache-scheme&gt;

        &lt;cachestore-scheme&gt;
          &lt;class-scheme&gt;
            &lt;class-name&gt;com.oracle.coherence.guides.cachestores.H2R2DBCEntryStore&lt;/class-name&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<p><strong>Run the Unit Test</strong></p>

<p>Next we will run the <code>H2R2DBCEntryStoreTest</code> unit test below and observe the behaviour.</p>

<ol style="margin-left: 15px;">
<li>
Start and confirm NamedMap and database contents.
<markup
lang="java"

>@BeforeAll
public static void startup() throws SQLException
    {
    createTable();

    startupCoherence("h2r2dbc-entry-store-cache-config.xml");
    }

/**
 * Performs some cache manipulations.
 */
@Test
public void testNonBlockingEntryStore()
    {
    NamedMap&lt;Long, Person&gt; namedMap = getSession()
            .getMap("H2Person", TypeAssertion.withTypes(Long.class, Person.class));

    Person person1 = namedMap.get(Long.valueOf(101));
    assertEquals("Robert", person1.getFirstname());</markup>

</li>
<li>
Insert 1 person using a <code>put</code> operation and confirm the data is in the cache.
<markup
lang="java"

>Person person2 = new Person(Long.valueOf(102), 40, "Tony", "Soprano");
namedMap.put(Long.valueOf(102), person2);

Person person3 = namedMap.get(Long.valueOf(102));
assertEquals("Tony", person3.getFirstname());</markup>

</li>
<li>
Delete a couple records and verify the state of the cache.
<markup
lang="java"

>namedMap.remove(Long.valueOf(101));
namedMap.remove(Long.valueOf(102));
assertEquals(null, namedMap.get(Long.valueOf(101)));
assertEquals(null, namedMap.get(Long.valueOf(102)));</markup>

</li>
<li>
Insert 10 persons using a <code>putAll</code> operation and confirm the data is in the cache. The actual database operations take place in parallel.s
<markup
lang="java"

>Map&lt;Long, Person&gt; map = new HashMap&lt;&gt;();
for (int i = 1; i &lt;= 10; i++)
    {
    map.put(Long.valueOf(i), new Person(Long.valueOf(i), 20 + i, "firstname" + i, "lastname" + i));
    }
namedMap.putAll(map);
Person person5 = namedMap.get(Long.valueOf(5));
assertEquals("firstname5", person5.getFirstname());
assertEquals(10, namedMap.size());</markup>

<div class="admonition note">
<p class="admonition-inline">You should see messages indicating activity on the store side:</p>
</div>
</li>
</ol>
<div class="listing">
<pre>2021-06-29 15:01:36.365/5.583 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore load key: 101
2021-06-29 15:01:36.495/5.713 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore store
2021-06-29 15:01:36.501/5.720 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore erase
2021-06-29 15:01:36.504/5.722 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): Rows updated: 1
2021-06-29 15:01:36.507/5.726 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore erase
2021-06-29 15:01:36.508/5.727 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): Rows updated: 1
2021-06-29 15:01:36.509/5.728 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore load key: 101
2021-06-29 15:01:36.512/5.730 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): Could not find row for key: 101
2021-06-29 15:01:36.515/5.734 Oracle Coherence GE 14.1.2.0.0 &lt;Info&gt; (thread=DistributedCacheWorker:0x0000:5, member=1): H2R2DBCEntryStore storeAll</pre>
</div>

</div>

<h3 id="pluggable">Pluggable Cache Stores</h3>
<div class="section">
<p>A cache store is an application-specific adapter used to connect a cache to an
underlying data source. The cache store implementation accesses the data source
by using a data access mechanism (for example, Hibernate, Toplink, JPA, application-specific JDBC calls, etc).
The cache store understands how to build a Java object using data retrieved
from the data source, map and write an object to the data source, and erase
an object from the data source.</p>

<p>In this example we are going to use a Hibernate cache store from the <a id="" title="" target="_blank" href="https://github.com/coherence-community/coherence-hibernate">Coherence Hibernate OpenSource Project</a>.</p>

<p><strong>Review the Configuration</strong></p>

<ol style="margin-left: 15px;">
<li>
Review the Cache Configuration <code>hibernate-cache-store-cache-config.xml</code>
<markup
lang="xml"

>&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;Person&lt;/cache-name&gt;  <span class="conum" data-value="1" />
    &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;distributed-hibernate&lt;/scheme-name&gt;
    &lt;backing-map-scheme&gt;
      &lt;read-write-backing-map-scheme&gt;
        &lt;internal-cache-scheme&gt;
          &lt;local-scheme&gt;&lt;/local-scheme&gt;
        &lt;/internal-cache-scheme&gt;

        &lt;cachestore-scheme&gt;
          &lt;class-scheme&gt;
            &lt;class-name&gt;com.oracle.coherence.hibernate.cachestore.HibernateCacheStore&lt;/class-name&gt;  <span class="conum" data-value="2" />
            &lt;init-params&gt;
              &lt;init-param&gt;
                &lt;param-type&gt;java.lang.String&lt;/param-type&gt;
                &lt;param-value&gt;com.oracle.coherence.guides.cachestores.{cache-name}&lt;/param-value&gt; <span class="conum" data-value="3" />
              &lt;/init-param&gt;
            &lt;/init-params&gt;
          &lt;/class-scheme&gt;
        &lt;/cachestore-scheme&gt;
      &lt;/read-write-backing-map-scheme&gt;
    &lt;/backing-map-scheme&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

<ul class="colist">
<li data-value="1">Cache mapping for all caches to the <code>distributed-hibernate</code> scheme</li>
<li data-value="2">Specify the <code>HibernateCacheStore</code> scheme</li>
<li data-value="3">Pass the cache name using the in-built macro to the constructor</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">In this case we do not have to write any code for our cache store as the Hibernate cache store understands the entity mapping and will deal with this.</p>
</div>
</li>
<li>
Review the Hibernate Configuration
<markup
lang="xml"

>&lt;hibernate-configuration&gt;
  &lt;session-factory&gt;
    &lt;!-- Database connection settings --&gt;
    &lt;property name="connection.driver_class"&gt;org.hsqldb.jdbcDriver&lt;/property&gt;
    &lt;property name="connection.url"&gt;jdbc:hsqldb:mem:test&lt;/property&gt;
    &lt;property name="connection.username"&gt;sa&lt;/property&gt;
    &lt;property name="connection.password"&gt;&lt;/property&gt;

    &lt;!-- JDBC connection pool (use the built-in) --&gt;
    &lt;property name="connection.pool_size"&gt;1&lt;/property&gt;

    &lt;!-- SQL dialect --&gt;
    &lt;property name="dialect"&gt;org.hibernate.dialect.HSQLDialect&lt;/property&gt;

    &lt;!-- Enable Hibernate's automatic session context management --&gt;
    &lt;property name="current_session_context_class"&gt;thread&lt;/property&gt;

    &lt;!-- Echo all executed SQL to stdout --&gt;
    &lt;property name="show_sql"&gt;true&lt;/property&gt;

    &lt;!-- Drop and re-create the database schema on startup --&gt;
    &lt;property name="hbm2ddl.auto"&gt;update&lt;/property&gt;

    &lt;mapping resource="Person.hbm.xml"/&gt;  <span class="conum" data-value="1" />
  &lt;/session-factory&gt;
&lt;/hibernate-configuration&gt;</markup>

<ul class="colist">
<li data-value="1">- Specifies the Person mapping</li>
</ul>
</li>
<li>
Review the Hibernate Mapping
<markup
lang="xml"

>&lt;hibernate-mapping package="com.oracle.coherence.guides.cachestores"&gt;
  &lt;class name="Person" table="PERSON"&gt;
    &lt;id name="id" column="id"&gt;
      &lt;generator class="native"/&gt;
    &lt;/id&gt;
    &lt;property name="age"/&gt;
    &lt;property name="firstname"/&gt;
    &lt;property name="lastname"/&gt;
  &lt;/class&gt;
&lt;/hibernate-mapping&gt;</markup>

<ul class="colist">
<li data-value="1">Specifies the Person mapping</li>
</ul>
</li>
</ol>
<p><strong>Run the Unit Test</strong></p>

<p>Next we will run the <code>HibernateCacheStoreTest</code> unit test below and observe the behaviour.</p>

<ol style="margin-left: 15px;">
<li>
Start and confirm NamedMap and database contents. In this example we are not preloading the database.
<markup
lang="java"

>@BeforeAll
public static void startup() throws SQLException {
    startupCoherence("hibernate-cache-store-cache-config.xml");
    connection = DriverManager.getConnection("jdbc:hsqldb:mem:test");
}

@Test
public void testHibernateCacheStore() throws SQLException {
    NamedMap&lt;Long, Person&gt; namedMap = getSession()
            .getMap("Person", TypeAssertion.withTypes(Long.class, Person.class));</markup>

</li>
<li>
Create a new Person and put it into the NamedMap.
<markup
lang="java"

>Person person1 = new Person(1L, 50, "Tom", "Jones");
namedMap.put(person1.getId(), person1);
assertEquals(1, namedMap.size());</markup>

</li>
<li>
Retrieve the Person from the database and validate that the person from the database and cache are equal.
<markup
lang="java"

>Person person2 = getPersonFromDB(1L);
person1 = namedMap.get(1L);
assertNotNull(person2);
assertEquals(person2, person1);</markup>

</li>
<li>
Update the persons age in the NamedMap and confirm it is saved in the database
<markup
lang="java"

>person2.setAge(100);
namedMap.put(person2.getId(), person2);
Person person3 = getPersonFromDB(1L);
assertNotNull(person2);
assertEquals(person3.getAge(), 100);</markup>

</li>
<li>
Remove person 1 and ensure they are also removed from the database.
<markup
lang="java"

>namedMap.remove(1L);
Person person4 = getPersonFromDB(1L);
assertNull(person4);</markup>

</li>
</ol>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>You have seen how to use and configure Cache Stores within Coherence.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/caching-data-sources.html#GUID-9FAD1BFB-5063-4995-B0A7-3C6F9C64F600">Caching Data Stores</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://github.com/coherence-community/coherence-hibernate">Coherence Hibernate OpenSource Project</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
