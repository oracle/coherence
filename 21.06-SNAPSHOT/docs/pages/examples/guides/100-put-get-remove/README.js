<doc-view>

<h2 id="_put_get_and_remove_operations">Put Get and Remove Operations</h2>
<div class="section">
<p>This guide walks you through the basic CRUD operations on a Coherence <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedMap.html">NamedMap</a>.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>The example code is written as a set of unit tests, as this is the simplest way to demonstrate something as
basic as individual <code>NamedMap</code> operations.</p>


<h4 id="_what_you_need">What You Need</h4>
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
</div>

<h3 id="_coherence_namedmap">Coherence <code>NamedMap</code></h3>
<div class="section">
<p>The Coherence <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedMap.html">NamedMap</a> is an extension of Java&#8217;s <code>java.util.Map</code> interface
and as such, it has all the <code>Map</code> methods that a Java developer is familiar with. Coherence also has a
<a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedMap.html">NamedCache</a> which extends <code>NamedMap</code> and is form more transient data
storage in caching use cases.</p>

<p>The most basic operations on a <code>NamedMap</code> are the simple CRUD methods, <code>put</code>, <code>get</code> and <code>remove</code>, which this guide
is all about.</p>

</div>

<h3 id="_create_the_test_class">Create the Test Class</h3>
<div class="section">
<p>The first step is to create the test class that will show and test the various <code>NamedMap</code> operations,
we&#8217;ll call this class <code>BasicCrudTest</code>. We will use Junit 5 for this test, so the class does not have to be public.</p>

<markup
lang="java"

>class BasicCrudTest {
}</markup>


<h4 id="_bootstrap_coherence">Bootstrap Coherence</h4>
<div class="section">
<p>The first thing the test class will do is start Coherence using the bootstrap API introduced in Coherence v20.12.
As this is a JUnit test class, we can do this in a static <code>@BeforeAll</code> annotated setup method.
We are going to start a storage enabled cluster member using the most basic bootstrap API methods.
For more details on the bootstrap API see the corresponding guide</p>

<markup
lang="java"

>    @BeforeAll
    static void boostrapCoherence() {
        Coherence coherence = Coherence.clusterMember();      <span class="conum" data-value="1" />
        CompletableFuture&lt;Void&gt; future = coherence.start();   <span class="conum" data-value="2" />
        future.join();                                        <span class="conum" data-value="3" />
    }</markup>

<ul class="colist">
<li data-value="1">Obtain a default storage enabled cluster member <code>Coherence</code> instance.</li>
<li data-value="2">Start the <code>Coherence</code> instance, this wil start all the Coherence services.</li>
<li data-value="3">Block until Coherence instance has fully started before proceeding with the tests</li>
</ul>
<p>Second, we create a static <code>@AfterAll</code> annotated tear-down method that will shut down Coherence at the end of the test.</p>

<markup
lang="java"

>    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance(); <span class="conum" data-value="1" />
        coherence.close();
    }</markup>

<ul class="colist">
<li data-value="1">We only created a single default <code>Coherence</code> instance, so we can obtain that instance with the
<code>Coherence.getInstance()</code> method, and then close it.</li>
</ul>
<p>Now the basic framework of the test is in place we can add methods to show different <code>NamedMap</code> operations.</p>

</div>

<h4 id="_obtain_a_namedmap_instance">Obtain a <code>NamedMap</code> Instance</h4>
<div class="section">
<p>All the tests in this guide need to obtain a <code>NamedMap</code> instance,
we will use a Coherence <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/Session.html">Session</a> for this.
A <code>Session</code> is a means to access Coherence clustered resources.
Creation of <code>Session</code> instances is part of the bootstrap API, which we can obtain named <code>Session</code> instances from.
In this case we are using the bootstrap API&#8217;s default, so we can simply obtain the default <code>Session</code>.
To get a <code>NamedMap</code> from a <code>Session</code> we use the <code>Session.getMap()</code> method. This take a <code>String</code> value, which is
the name of the map to obtain from the <code>Session</code>.</p>

<p>There are a number of ways we could have encapsulated this common code in the test class.
In this case we will create a simple utility method to get a <code>NamedMap</code> with a give name that the different test
methods can call.</p>

<markup
lang="java"

>    &lt;K, V&gt; NamedMap&lt;K, V&gt; getMap(String name) {
        Coherence coherence = Coherence.getInstance();     <span class="conum" data-value="1" />
        Session   session   = coherence.getSession();      <span class="conum" data-value="2" />
        return session.getMap(name);                       <span class="conum" data-value="3" />
    }</markup>

<ul class="colist">
<li data-value="1">We only created a single default <code>Coherence</code> instance, so we can obtain that instance with the
<code>Coherence.getInstance()</code> method.</li>
<li data-value="2">Obtain the default <code>Session</code> from the <code>Coherence</code> instance.</li>
<li data-value="3">Obtain and return the <code>NamedMap</code> instance with the required name.</li>
</ul>
</div>
</div>

<h3 id="_a_quick_word_about_serialization">A Quick Word About Serialization</h3>
<div class="section">
<p>In almost every case a <code>NamedMap</code> is backed by a distributed, clustered, Coherence resource.
For this reason all <code>Objects</code> used as keys and values <strong>must</strong> be serializable so that they can be transferred between
cluster members and clients during requests.
Coherence Serialization support is a topic that deserves a guide of its own
The <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/io/Serializer.html">Serializer</a> implementation used by a <code>NamedMap</code> is configurable
and Coherence comes with some out of the box <code>Serializer</code> implementations.
The default is Java serialization, so all keys and values must be Java <code>Serializable</code> or implement Coherence
<a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/io/ExternalizableLite.html">ExternalizableLite</a> interface for more control of serialization.
Alternatively Coherence can also be configured to use Portable Object Format for serialization and additionaly
there is a JSON Coherence module that provides a JSON serializer that may be used.</p>

<p>To keep this guide simple we are going to stick with the default serializer, so all <code>NamedMap</code> operations will use
classes that are <code>Serializable</code>.</p>

</div>

<h3 id="_the_put_method">The Put Method</h3>
<div class="section">
<p>The obvious place to start is to add data to a <code>NamedMap</code> using the <code>put</code> method.
We will create a simple test method that uses <code>put</code> to add a new key and value to a <code>NamedMap</code>.</p>

<markup
lang="java"

>    @Test
    void shouldPutNewKeyAndValue() {
        NamedMap&lt;String, String&gt; map = getMap("data");    <span class="conum" data-value="1" />
        String oldValue = map.put("key-1", "value-1");          <span class="conum" data-value="2" />

        assertNull(oldValue);                                   <span class="conum" data-value="3" />
    }</markup>

<ul class="colist">
<li data-value="1">We call the <code>getMap</code> utility method we wrote above to get a <code>NamedMap</code> with the name <code>data</code>.
In this case the map&#8217;s keys and values are both of type <code>String</code>.</li>
<li data-value="2">We call the <code>put</code> method to map the key <code>"key-1"</code> to the value <code>"value-1"</code>.
As <code>NamedMap</code> implements <code>java.util.Map</code>, the <code>put</code> contract says that the <code>put</code> method returns the previous valued
mapped to the key.</li>
<li data-value="3">In this case there was no previous value mapped to <code>"key-1"</code>, so the returned value must be <code>null</code>.</li>
</ul>
<p>To show that we do indeed get back the old value returned from a <code>put</code>, we can write a slightly different test method
that puts a new key and value into a <code>NamedMap</code> then updates the mapping with a new value.</p>

<markup
lang="java"

>    @Test
    void shouldPutExistingKeyAndValue() {
        NamedMap&lt;String, String&gt; map = getMap("data");
        map.put("key-2", "value-1");

        String oldValue = map.put("key-2", "value-2");
        assertEquals("value-1", oldValue);
    }</markup>

</div>

<h3 id="_the_get_method">The Get Method</h3>
<div class="section">
<p>We have seen how we can add data to a <code>NamedMap</code> using the <code>put</code> method, so the obvious next step is to get the data
back out using the <code>get</code> method.</p>

<markup
lang="java"

>    @Test
    void shouldGet() {
        NamedMap&lt;String, String&gt; map = getMap("data");    <span class="conum" data-value="1" />
        map.put("key-3", "value-1");                            <span class="conum" data-value="2" />

        String value = map.get("key-3");                        <span class="conum" data-value="3" />

        assertEquals("value-1", value);
    }</markup>

<ul class="colist">
<li data-value="1">We call the <code>getMap</code> utility method we wrote above to get a <code>NamedMap</code> with the name <code>data</code>.
In this case the map&#8217;s keys and values are both of type <code>String</code>.</li>
<li data-value="2">We add some data to the <code>NamedMap</code> mapping the key <code>"key-3"</code> to the value <code>"value-1"</code>;</li>
<li data-value="3">We use the <code>get</code> method to get the value from the <code>NamedMap</code> that is mapped to the key <code>"key-3"</code>,
which obviously must be <code>"value-1"</code>.</li>
</ul>
</div>

<h3 id="_get_multiple_values">Get Multiple Values</h3>
<div class="section">
<p>The Coherence <code>NamedMap</code> contains a <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedMap.html#getAll(java.util.Collection)">getAll(java.util.Collection)</a>
method that takes a collection of keys as a parameter and returns a new <code>Map</code> that contains the requested mappings.</p>

<markup
lang="java"

>    @Test
    void shouldGetAll() {
        NamedMap&lt;String, String&gt; map = getMap("data");    <span class="conum" data-value="1" />

        map.put("key-5", "value-5");                            <span class="conum" data-value="2" />
        map.put("key-6", "value-6");
        map.put("key-7", "value-7");

        Map&lt;String, String&gt; results = map.getAll(Arrays.asList("key-5", "key-7", "key-8"));   <span class="conum" data-value="3" />

        assertEquals(2, results.size());                <span class="conum" data-value="4" />
        assertEquals("value-5", results.get("key-5"));  <span class="conum" data-value="5" />
        assertEquals("value-7", results.get("key-7"));  <span class="conum" data-value="6" />
    }</markup>

<ul class="colist">
<li data-value="1">We call the <code>getMap</code> utility method we wrote above to get a <code>NamedMap</code> with the name <code>data</code>.
In this case the map&#8217;s keys and values are both of type <code>String</code>.</li>
<li data-value="2">We add some data to the map.</li>
<li data-value="3">We call the <code>getAll</code> method requesting keys <code>"key-5"</code>, <code>"key-7"</code> and <code>"key-8"</code>.</li>
<li data-value="4">The <code>result</code> map returned should only contain two keys, because although we requested the mappings for three keys,
<code>"key-8"</code> was not added to the <code>NamedMap</code>.</li>
<li data-value="5">The value mapped to <code>"key-5"</code> should be <code>"value-5"</code>.</li>
<li data-value="6">The value mapped to <code>"key-7"</code> should be <code>"value-7"</code>.</li>
</ul>
</div>

<h3 id="_the_remove_method">The Remove Method</h3>
<div class="section">
<p>We&#8217;ve now seen adding data to and getting data from a <code>NamedMap</code>, we can also remove values mapped to a key with the
<code>remove</code> method.</p>

<markup
lang="java"

>    @Test
    void shouldRemove() {
        NamedMap&lt;String, String&gt; map = getMap("data");    <span class="conum" data-value="1" />
        map.put("key-9", "value-9");                            <span class="conum" data-value="2" />

        String oldValue = map.remove("key-9");             <span class="conum" data-value="3" />

        assertEquals("value-9", oldValue);             <span class="conum" data-value="4" />
    }</markup>

<ul class="colist">
<li data-value="1">We call the <code>getMap</code> utility method we wrote above to get a <code>NamedMap</code> with the name <code>data</code>.
In this case the map&#8217;s keys and values are both of type <code>String</code>.</li>
<li data-value="2">We add some data to the map.</li>
<li data-value="3">Call the remove method to remove the value mapped to key <code>"key-9"</code>.</li>
<li data-value="4">The contract of the remove method says that the value returned should be the value that was mapped to the key
that was removed (or <code>null</code> if there was no mapping to the key). In this case the returned value must be <code>"value-9"</code>.</li>
</ul>
</div>

<h3 id="_the_remove_mapping_method">The Remove Mapping Method</h3>
<div class="section">
<p>An alternate version of the <code>remove</code> method is the two argument remove method that removes a mapping to a key if the
key is mapped to a specific value.</p>

<markup
lang="java"

>    @Test
    void shouldRemoveMapping() {
        NamedMap&lt;String, String&gt; map = getMap("data");    <span class="conum" data-value="1" />
        map.put("key-10", "value-10");                          <span class="conum" data-value="2" />

        boolean removed = map.remove("key-10", "Foo");          <span class="conum" data-value="3" />
        assertFalse(removed);

        removed = map.remove("key-10", "value-10");             <span class="conum" data-value="4" />
        assertTrue(removed);
    }</markup>

<ul class="colist">
<li data-value="1">We call the <code>getMap</code> utility method we wrote above to get a <code>NamedMap</code> with the name <code>data</code>.
In this case the map&#8217;s keys and values are both of type <code>String</code>.</li>
<li data-value="2">We add some data to the map.</li>
<li data-value="3">Call the remove method to remove the value mapped to key <code>"key-10"</code> with a value of <code>"Foo"</code>.
This must return <code>false</code> as we mapped <code>"key-10"</code> to the value <code>"value-10"</code>, so nothing will be removed from the <code>NamedMap</code>.</li>
<li data-value="4">Call the remove method to remove the value mapped to key <code>"key-10"</code> with a value of <code>"value-10"</code>.
This must return <code>true</code> as we mapped <code>"key-10"</code> to the value <code>"value-10"</code>, so the mapping will be removed from the <code>NamedMap</code>.</li>
</ul>
</div>

<h3 id="_namedcache_transient_data"><code>NamedCache</code> Transient Data</h3>
<div class="section">
<p>As already stated, a <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedMap.html">NamedCache</a> is typically used to store transient data
in caching use-cases.</p>

<p>The <code>NamedCache</code> has an alternative <a id="" title="" target="_blank" href="https://coherence.community/21.06-SNAPSHOT/api/java/com/tangosol/net/NamedCache.html#put(K,V,long)">put(K,V,long)</a> method
that takes a key, value, and an expiry value. The expiry value is the number of milli-seconds that the key and value
should remain in the cache. When the expiry time has passed the key and value will be removed from the cache.</p>

<markup
lang="java"

>    @Test
    void shouldPutWithExpiry() throws Exception {
        Coherence coherence = Coherence.getInstance();
        Session   session   = coherence.getSession();

        NamedCache&lt;String, String&gt; cache = session.getCache("test");  <span class="conum" data-value="1" />

        cache.put("key-1", "value-1", 2000);  <span class="conum" data-value="2" />

        String value = cache.get("key-1");                     <span class="conum" data-value="3" />
        assertEquals("value-1", value);

        Thread.sleep(3000);                              <span class="conum" data-value="4" />

        value = cache.get("key-1");                            <span class="conum" data-value="5" />
        assertNull(value);
    }</markup>

<ul class="colist">
<li data-value="1">In the same way that we obtained a <code>NamedMap</code> from the default <code>Session</code>, we can obtain a <code>NamedCache</code>
using the <code>getCache</code> method, in this case the cache named <code>test</code>.</li>
<li data-value="2">Using the put with expiry method, we can add a key of <code>"key-1"</code> mapped to value <code>"value-1"</code> with an expiry of
<code>2000</code> milli-seconds (or 2 seconds).</li>
<li data-value="3">If we now do a <code>get</code> for <code>"key-1"</code> we should get back <code>"value-1"</code> because two seconds has not yet passed
(unless you are running this test on a terribly slow machine).</li>
<li data-value="4">Now we wait for three seconds to be sure the expiry time has passed.</li>
<li data-value="5">This time when we get <code>"key-1"</code> the value returned must be <code>null</code> because the value has expired, and been removed
from the cache.</li>
</ul>
</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>You have seen how simple it is to use simple CRUD methods on <code>NamedMap</code> and <code>NamedCache</code> instances, as
well as the simplest way to bootstrap a default Coherence storage enabled server instance.</p>

</div>
</div>
</doc-view>
