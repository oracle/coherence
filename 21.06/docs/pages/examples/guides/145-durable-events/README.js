<doc-view>

<h2 id="_durable_events">Durable Events</h2>
<div class="section">
<p>Coherence provides the <code>MapListener</code> interface as described in <router-link to="/examples/guides/140-client-events/README">Client Events</router-link>,
where clients can sign up for events from any Coherence <code>NamedMap</code>.  With traditional client events, if a client disconnects for
any reason and then reconnects and automatically re-registers a <code>MapListener</code>, it will miss any events that were sent during that disconnected time.</p>

<p>Durable Events is a new (experimental) feature that allows clients to create a versioned listener which will allow
a client, if disconnected, to receive events missed while they were in a disconnected state. As for standard `MapListener`s
you are able to register for all events, events based upon a filter or events for a specific key.</p>

<p>More advanced use cases for Durable Events include the ability to replay all events for a <code>NamedMap</code>.</p>

<p>Please see <router-link to="/docs/core/06_durable_events">Durable Events Documentation</router-link> for more information on Durable Events.</p>

<div class="admonition note">
<p class="admonition-inline">Durable events are an experimental feature only and should not be used in product as yet.</p>
</div>
<div class="admonition note">
<p class="admonition-inline">Durable Events are not yet supported for Coherence*Extend clients.</p>
</div>

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
<p><router-link to="#building" @click.native="this.scrollFix('#building')">Building the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#example-classes-1" @click.native="this.scrollFix('#example-classes-1')">Review the Classes</router-link></p>

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
<p>In this example you will run a test that will demonstrate using Durable Events. The test does the following:</p>

<ul class="ulist">
<li>
<p>Starts 2 Cache Servers using <a id="" title="" target="_blank" href="https://github.com/coherence-community/oracle-bedrock">Oracle Bedrock</a></p>

</li>
<li>
<p>Creates and registers a version aware <code>MapListener</code></p>

</li>
<li>
<p>Inserts, updates and deletes cache entries</p>

</li>
<li>
<p>Simulates the client being disconnected</p>

</li>
<li>
<p>Issues cache mutations remotely while the client is disconnected</p>

</li>
<li>
<p>Reconnects the client and validate that events generated while the client was disconnected are received</p>

</li>
</ul>
<p>To enable Durable Events you must have the following system properties set for cache servers:</p>

<ul class="ulist">
<li>
<p>Enable active persistence by using <code>-Dcoherence.distributed.persistence.mode=active</code></p>

</li>
<li>
<p>Set the directory to store Durable Events using <code>-Dcoherence.distributed.persistence.events.dir=/my/events/dir</code></p>

</li>
<li>
<p>Optionally set the directory to store active persistence using <code>-Dcoherence.distributed.persistence.base.dir=/my/persistence/dir</code></p>

</li>
<li>
<p>Register a versioned <code>MapListener</code> on a <code>NamedMap</code></p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">If you do not set the directory to store active persistence the default directory <code>coherence</code> off the users home directory will be chosen.</p>
</div>

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
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>
</div>

<h4 id="building">Building the Example Code</h4>
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
<p class="admonition-inline">You can include the <code>-DskipTests</code> for Maven or <code>-x test</code> for Gradle, to skip the tests for now.</p>
</div>
</div>
</div>

<h3 id="example-classes-1">Review the Classes</h3>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Review the <code>Customer</code> class
<p>This example uses the <code>Customer</code> class which has the following fields:</p>

<markup
lang="java"

>/**
 * Customer id.
 */
private long id;

/**
 * Customers name.
 */
private String name;

/**
 * Customers address.
 */
private String address;

/**
 * Customers type, BRONZE, SILVER or GOLD.
 */
private String customerType;

/**
 * Credit limit.
 */
private long creditLimit;</markup>

</li>
<li>
Review how the 2 cache servers are started by Oracle Bedrock
<markup
lang="java"

>/**
 * Startup 2 cache servers using Oracle Bedrock.
 *
 * @throws IOException if any errors creating temporary directory
 */
@BeforeAll
public static void startup() throws IOException {
    persistenceDir = FileHelper.createTempDir();
    String path = persistenceDir.getAbsolutePath();
    LocalPlatform platform = LocalPlatform.get();

    props = new Properties();
    props.put("coherence.distributed.partitions", "23");  <span class="conum" data-value="1" />
    props.put("coherence.distributed.persistence.mode", "active"); <span class="conum" data-value="2" />
    props.put("coherence.distributed.persistence.base.dir", path); <span class="conum" data-value="3" />
    props.put("coherence.distributed.persistence.events.dir", path + FILE_SEP + "events"); <span class="conum" data-value="4" />

    OptionsByType optionsByType = OptionsByType.empty();
    optionsByType.addAll(LocalStorage.enabled(), Multicast.ttl(0), Logging.at(2));

    // add the properties to the Bedrock startup
    props.forEach((k,v) -&gt; optionsByType.add(SystemProperty.of((String) k, (String) v)));

    OptionsByType optionsByTypeMember1 = OptionsByType.of(optionsByType).add(RoleName.of("member1"));
    OptionsByType optionsByTypeMember2 = OptionsByType.of(optionsByType).add(RoleName.of("member2"));

    member1 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember1.asArray());
    member2 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember2.asArray());

    Eventually.assertThat(invoking(member1).getClusterSize(), CoreMatchers.is(2));
}</markup>

<ul class="colist">
<li data-value="1">Set the partition count to 23 to reduce the startup time</li>
<li data-value="2">Set active persistence mode</li>
<li data-value="3">Set the base directory to store persistence files</li>
<li data-value="4">Set the base directory to store persistence events</li>
</ul>
</li>
<li>
Review the <code>DurableEventsTest</code> class
<markup
lang="java"

>/**
 * Runs a test to simulate a client registering a versioned {@link MapListener},
 * being disconnected, reconnecting, and then receiving all the events that were
 * missed while the client was disconnected.
 */
@Test
public void testDurableEvents()  {
    try {
        final AtomicInteger eventCount = new AtomicInteger();
        final String CACHE_NAME = "customers";

        System.getProperties().putAll(props); <span class="conum" data-value="1" />
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.log.level", "3");

        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();

        NamedMap&lt;Long, Customer&gt; customers = coherence.getSession().getMap(CACHE_NAME);

        MapListener&lt;Long, Customer&gt; mapListener = new SimpleMapListener&lt;Long, Customer&gt;() <span class="conum" data-value="2" />
                                       .addEventHandler(System.out::println) <span class="conum" data-value="3" />
                                       .addEventHandler((e) -&gt; eventCount.incrementAndGet()) <span class="conum" data-value="4" />
                                       .versioned(); <span class="conum" data-value="5" />
        customers.addMapListener(mapListener); <span class="conum" data-value="6" />

        Logger.info("Added Map Listener, generating 3 events");
        // generate 3 events, insert, update and delete
        Customer customer = new Customer(100L, "Customer 100", "Address", Customer.GOLD, 5000);
        customers.put(customer.getId(), customer);
        customers.invoke(100L, Processors.update(Customer::setAddress, "New Address"));
        customers.remove(100L);

        // wait until we receive first three events
        Eventually.assertDeferred(eventCount::get, is(3));

        // cause a service distribution for PartitionedCache service to simulate disc
        Logger.info("Disconnecting client");
        causeServiceDisruption(customers); <span class="conum" data-value="7" />

        Logger.info("Remotely insert, update and delete a new customer");
        // do a remote invocation to insert, update and delete a customer. This is done
        // remotely via Oracle Bedrock as not to reconnect the client
        member2.invoke(() -&gt; { <span class="conum" data-value="8" />
            NamedMap&lt;Long, Customer&gt; customerMap = CacheFactory.getCache(CACHE_NAME);
            Customer newCustomer = new Customer(100L, "Customer 101", "Customer address", Customer.SILVER, 100);
            customerMap.put(newCustomer.getId(), newCustomer);
            customerMap.invoke(100L, Processors.update(Customer::setAddress, "New Address"));
            customerMap.remove(100L);
            return null;
        });

        // Events should still only be 3 as client has not yet reconnected
        Eventually.assertDeferred(eventCount::get, is(3));

        Logger.info("Issuing size to reconnect client");
        // issue an operation that will cause a service restart and listener to be re-registered
        customers.size(); <span class="conum" data-value="9" />

        // we should now see the 3 events we missed because we were disconnected
        Eventually.assertDeferred(eventCount::get, is(6)); <span class="conum" data-value="10" />
        }
    finally {
        Coherence coherence = Coherence.getInstance();
        coherence.close();
    }
}</markup>

<ul class="colist">
<li data-value="1">Set system properties for the client</li>
<li data-value="2">Create a new <code>SimpleMapListener</code></li>
<li data-value="3">Add an event handler to output the events received</li>
<li data-value="4">Add an event handler to increment the number of events received</li>
<li data-value="5">Indicate that this <code>MapListener</code> is versioned</li>
<li data-value="6">Add the <code>MapListener</code> to the <code>NamedMap</code></li>
<li data-value="7">Simulate the client being disconnected by stopping the service for the <code>NamedMap</code></li>
<li data-value="8">Generate 3 new events remotely on one of the members</li>
<li data-value="9">Issue an operator that will cause the client to restart and re-register the listener</li>
<li data-value="10">Assert that we now see the additional 3 events that were generated while the client was disconnected</li>
</ul>
</li>
</ol>
</div>

<h3 id="run-example-1">Run the Examples</h3>
<div class="section">
<p>You can run the test in one of three ways:</p>

<ul class="ulist">
<li>
<p>Using your IDE to run <code>DurableEventsTest</code> class</p>

</li>
<li>
<p>Using Maven via <code>./mvnw clean verify</code></p>

</li>
<li>
<p>Using Gradle via <code>./gradlew test</code></p>

</li>
</ul>
<p>After initial cache server startup, you will see output similar to the following:</p>

<div class="admonition note">
<p class="admonition-inline">Timestamps have been removed and output has been formatted for easier reading.</p>
</div>
<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=3): Added Map Listener, generating 3 events <span class="conum" data-value="1" />
ConverterCollections$ConverterMapEvent{SafeNamedCache inserted: key=100,  &lt;2&gt;
    value=Customer{id=100, name='Customer 100', address='Address', customerType='GOLD', balance=5000}, partition=20, version=1}
ConverterCollections$ConverterMapEvent{SafeNamedCache updated: key=100,
    old value=Customer{id=100, name='Customer 100', address='Address', customerType='GOLD', balance=5000},
    new value=Customer{id=100, name='Customer 100', address='New Address', customerType='GOLD', balance=5000}, partition=20, version=2}
ConverterCollections$ConverterMapEvent{SafeNamedCache deleted: key=100,
    value=Customer{id=100, name='Customer 100', address='New Address', customerType='GOLD', balance=5000}, partition=20, version=3}
&lt;Info&gt; (thread=main, member=3): Disconnecting client <span class="conum" data-value="3" />
&lt;Info&gt; (thread=main, member=3): Remotely insert, update and delete a new customer
&lt;Info&gt; (thread=DistributedCache:PartitionedCache, member=3): Service PartitionedCache left the cluster <span class="conum" data-value="4" />
&lt;Info&gt; (thread=main, member=3): Issuing size to reconnect client
&lt;Info&gt; (thread=main, member=3): Restarting NamedCache: customers <span class="conum" data-value="5" />
&lt;Info&gt; (thread=main, member=3): Restarting Service: PartitionedCache
&lt;Info&gt; (thread=DistributedCache:PartitionedCache, member=3): Service PartitionedCache joined the cluster with senior service member 1
ConverterCollections$ConverterMapEvent{SafeNamedCache inserted: key=100, <span class="conum" data-value="6" />
    value=Customer{id=100, name='Customer 101', address='Customer address', customerType='SILVER', balance=100}, partition=20, version=4}
ConverterCollections$ConverterMapEvent{SafeNamedCache updated: key=100,
    old value=Customer{id=100, name='Customer 101', address='Customer address', customerType='SILVER', balance=100},
    new value=Customer{id=100, name='Customer 101', address='New Address', customerType='SILVER', balance=100}, partition=20, version=5}
ConverterCollections$ConverterMapEvent{SafeNamedCache deleted: key=100,
    value=Customer{id=100, name='Customer 101', address='New Address', customerType='SILVER', balance=100}, partition=20, version=6}</markup>

<ul class="colist">
<li data-value="1">Adding the versioned <code>SimpleMapListener</code></li>
<li data-value="2">Output of three events while the client is connected</li>
<li data-value="3">Message indicating we are disconnecting client</li>
<li data-value="4">Service for the client leaving as it is disconnected</li>
<li data-value="5">Restarting the cache and service due to <code>size()</code> request which will also automatically re-register the <code>MapListener</code></li>
<li data-value="6">Client now receives the events it missed during disconnect</li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this example you ran a test that demonstrated using Durable Events by:</p>

<ul class="ulist">
<li>
<p>Starting 2 Cache Servers using <a id="" title="" target="_blank" href="https://github.com/coherence-community/oracle-bedrock">Oracle Bedrock</a></p>

</li>
<li>
<p>Creating and registering a version aware <code>MapListener</code></p>

</li>
<li>
<p>Inserting, updating and deleting cache entries</p>

</li>
<li>
<p>Simulating the client being disconnected</p>

</li>
<li>
<p>Issuing cache mutations remotely while the client is disconnected</p>

</li>
<li>
<p>Reconnecting the client and validate that events generated while the client was disconnected are received</p>

</li>
</ul>
</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="/docs/core/06_durable_events">Durable Events Overview</router-link></p>

</li>
<li>
<p><router-link to="/examples/guides/140-client-events/README">Client Events</router-link></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-map-events.html">Develop Applications using Map Events</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
