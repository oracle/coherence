<doc-view>

<h2 id="_client_events">Client Events</h2>
<div class="section">
<p>This guide walks you through how to use client events within Coherence to listen for insert,
update or delete events on a Coherence <code>NamedMap</code>.</p>

<p>An application object that implements the <code>MapListener</code> interface can sign up for events from any Coherence
<code>NamedMap</code> simply by passing an instance of the application&#8217;s MapListener implementation to a <code>addMapListener()</code> method.
The MapListener can be registered against all entries, a specific key, or a Filter.</p>

<p>Registrations with filters can use <code>MapEventFilter</code> which provide more fine-grained control for event registrations
or <code>InKeySetFilter</code> which can be used to register against a Set of keys.</p>

<p>The <code>MapListener</code> interface provides a call back mechanism for <code>NamedMap</code> events where any changes that happen
to the source (NamedMap) are delivered to relevant clients asynchronously. The <code>MapEvent</code> object that is passed to the <code>MapListener</code> carries all the necessary
information about the event that has occurred Including the event type
(insert, update, or delete), the key, old value, new value, and the source (<code>NameMap</code>)
that emitted the event.</p>

<p>Client events are the key building blocks for other Coherence functionality including Near Cache
and Continuous Query Caches (CQC).</p>

<p>See the Coherence Documentation links below for more information:</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-map-events.html">Develop Applications using Map Events</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html">Understanding Near Caches</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-continuous-query-caching.html">Using Continuous Query Caches</a></p>

</li>
</ul>

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
<p>In this example you will run a number of tests and that show the following features of client events including:</p>

<ul class="ulist">
<li>
<p>Understanding the <code>MapListener</code> interface</p>

</li>
<li>
<p>Listening for all events</p>

</li>
<li>
<p>Using <code>SimpleMapListener</code> and <code>MultiplexingMapListener</code></p>

</li>
<li>
<p>Using lite events</p>

</li>
<li>
<p>Listening for events for a particular key</p>

</li>
<li>
<p>Listening for events based upon filters</p>

</li>
</ul>

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

<h3 id="example-tests-1">Review the Tests</h3>
<div class="section">
<p>The example code comprises the <code>ClientEventsTest</code> class, which runs a test showing various aspects of client events.</p>

<p>The <code>testMapListeners</code> runs the following test code for various scenarios</p>

<ul class="ulist">
<li>
<p><code>testStandardMapListener</code> - standard MapListener implementation listening to all events</p>

</li>
<li>
<p><code>testMultiplexingMapListener</code> -  <code>MultiplexingMapListener</code> listening to all events through the <code>onMapEvent()</code> method</p>

</li>
<li>
<p><code>testSimpleMapListener</code> - <code>SimpleMapListener</code> allows the use of lambdas to add event handlers to listen to events</p>

</li>
<li>
<p><code>testListenOnQueries</code> - listening for only for events on New York customers</p>

</li>
<li>
<p><code>testEventTypes</code> - listening for new or updated GOLD customers</p>

</li>
</ul>
<ol style="margin-left: 15px;">
<li>
Review the <code>Customer</code> class
<p>All the tests use the <code>Customer</code> class which has the following fields:</p>

<markup
lang="java"

>/**
 * Customer id.
 */
private int id;

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
Review the test boostrap and cleanup to start the cluster before all the tests and shutdown after the tests
<markup
lang="java"

>@BeforeAll
static void boostrapCoherence() {
    Coherence coherence = Coherence.clusterMember();
    coherence.start().join();

    customers = coherence.getSession().getMap("customers");
}</markup>

<markup
lang="java"

>@AfterAll
static void shutdownCoherence() {
    Coherence coherence = Coherence.getInstance();
    coherence.close();
}</markup>

</li>
<li>
Review the <code>testStandardMapListener</code> code
<p>This test uses the <code>CustomerMapListener</code> class which is an implementation of a standard <code>MapListener</code>
listening for all events.</p>

<markup
lang="java"

>/**
 * Simple {@link MapListener} implementation for Customers.
 */
public static class CustomerMapListener
        implements MapListener&lt;Integer, Customer&gt; { <span class="conum" data-value="1" />

    private final AtomicInteger insertCount = new AtomicInteger();  <span class="conum" data-value="2" />

    private final AtomicInteger updateCount = new AtomicInteger();

    private final AtomicInteger removeCount = new AtomicInteger();

    private final AtomicInteger liteEvents = new AtomicInteger();

    @Override
    public void entryInserted(MapEvent&lt;Integer, Customer&gt; mapEvent) { <span class="conum" data-value="3" />
        Logger.info("New customer: new key/value=" + mapEvent.getKey() + "/" + mapEvent.getNewValue());
        insertCount.incrementAndGet();
        if (mapEvent.getNewValue() == null) {
            liteEvents.incrementAndGet();
        }
    }

    @Override
    public void entryUpdated(MapEvent&lt;Integer, Customer&gt; mapEvent) { <span class="conum" data-value="4" />
        Logger.info("Updated customer key=" + mapEvent.getKey() + ", old=" + mapEvent.getOldValue() + ", new=" +
                    mapEvent.getNewValue());
        updateCount.incrementAndGet();
        if (mapEvent.getOldValue() == null) {
            liteEvents.incrementAndGet();
        }
    }

    @Override
    public void entryDeleted(MapEvent&lt;Integer, Customer&gt; mapEvent) { <span class="conum" data-value="5" />
        Logger.info("Deleted customer: old key/value=" + mapEvent.getKey() + "/" + mapEvent.getOldValue());
        removeCount.incrementAndGet();
        if (mapEvent.getOldValue() == null) {
            liteEvents.incrementAndGet();
        }
    }

    public int getInsertCount() {
        return insertCount.get();
    }

    public int getUpdateCount() {
        return updateCount.get();
    }

    public int getRemoveCount() {
        return removeCount.get();
    }

    public int getLiteEvents() {
        return liteEvents.get();
    }
}</markup>

<ul class="colist">
<li data-value="1">Implements <code>MapListener</code> interface</li>
<li data-value="2"><code>AtomicIntegers</code> for test validation</li>
<li data-value="3">Respond to <code>insert</code> events with new value</li>
<li data-value="4">Respond to <code>update</code> events with old and new values</li>
<li data-value="5">Respond to <code>delete</code> events with old value</li>
</ul>
<markup
lang="java"

>Logger.info("*** testStandardMapListener");
customers.clear();
CustomerMapListener mapListener = new CustomerMapListener(); <span class="conum" data-value="1" />
customers.addMapListener(mapListener); <span class="conum" data-value="2" />

customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

customers.put(customer1.getId(), customer1); <span class="conum" data-value="3" />
customers.put(customer2.getId(), customer2);

customers.invoke(1, Processors.update(Customer::setCreditLimit, 2000L));  <span class="conum" data-value="4" />
customers.remove(1);  <span class="conum" data-value="5" />

// ensure that we see all events <span class="conum" data-value="6" />
Eventually.assertThat(invoking(mapListener).getInsertCount(), is(2));
Eventually.assertThat(invoking(mapListener).getUpdateCount(), is(1));
Eventually.assertThat(invoking(mapListener).getRemoveCount(), is(1));

customers.removeMapListener(mapListener);</markup>

<ul class="colist">
<li data-value="1">Create the MapListener</li>
<li data-value="2">Add the MapListener to listen for all events</li>
<li data-value="3">Add the customers</li>
<li data-value="4">Update the credit limit for customer 1</li>
<li data-value="5">Remove customer 1</li>
<li data-value="6">Wait for all events</li>
</ul>
</li>
<li>
Review the <code>testMultiplexingMapListener</code> code
<p>This test uses the <code>MultiplexingCustomerMapListener</code> class which extends <code>MultiplexingMapListener</code> to
listen for all events.</p>

<markup
lang="java"

>/**
 * Simple {@link MultiplexingMapListener} implementation for Customers.
 */
public static class MultiplexingCustomerMapListener
        extends MultiplexingMapListener&lt;Integer, Customer&gt; {  <span class="conum" data-value="1" />

    private final AtomicInteger counter = new AtomicInteger(); <span class="conum" data-value="2" />

    @Override
    protected void onMapEvent(MapEvent&lt;Integer, Customer&gt; mapEvent) { <span class="conum" data-value="3" />
        Logger.info("isInsert=" + mapEvent.isInsert() +
                    ", isDelete=" + mapEvent.isDelete() +
                    ", isUpdate=" + mapEvent.isUpdate());
        Logger.info("key=" + mapEvent.getKey() + ", old=" + mapEvent.getOldValue() + ", new=" + mapEvent.getNewValue());
        Logger.info(mapEvent.toString());

        counter.incrementAndGet();
    }

    public int getCount() {
        return counter.get();
    }
}</markup>

<ul class="colist">
<li data-value="1">Extends abstract class <code>MultiplexingMapListener</code></li>
<li data-value="2"><code>AtomicInteger</code> for test validation</li>
<li data-value="3">Respond to all events and use <code>MapEvent</code> methods to determine type of event</li>
</ul>
<markup
lang="java"

>Logger.info("*** testMultiplexingMapListener");
customers.clear();
MapListener&lt;Integer, Customer&gt; multiplexingMapListener = new MultiplexingCustomerMapListener(); <span class="conum" data-value="1" />
// Multiplexing MapListener listening on all entries
customers.addMapListener(multiplexingMapListener);  <span class="conum" data-value="2" />

customer1 = new Customer(1, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

customers.put(customer1.getId(), customer1); <span class="conum" data-value="3" />
customers.invoke(1, Processors.update(Customer::setAddress, "Updated address"));
customers.remove(1);

// ensure that we see all events <span class="conum" data-value="4" />
Eventually.assertThat(invoking((MultiplexingCustomerMapListener) multiplexingMapListener).getCount(), is(3));

customers.removeMapListener(multiplexingMapListener);</markup>

<ul class="colist">
<li data-value="1">Create the MapListener</li>
<li data-value="2">Add the MapListener to listen for all events</li>
<li data-value="3">Mutate the customers</li>
<li data-value="4">Wait for all events</li>
</ul>
</li>
<li>
Review the <code>testSimpleMapListener</code> code
<p>This test uses the <code>SimpleMapListener</code> and lambdas to register event handlers for only the key <code>1</code>.</p>

<markup
lang="java"

>Logger.info("*** testSimpleMapListener");
customers.clear();
MapListener&lt;Integer, Customer&gt; simpleMapListener = new SimpleMapListener&lt;Integer, Customer&gt;()  <span class="conum" data-value="1" />
       .addInsertHandler((e) -&gt; Logger.info("New Customer added with id=" + e.getNewValue().getId())) <span class="conum" data-value="2" />
       .addDeleteHandler((e) -&gt; Logger.info("Deleted customer id =" + e.getOldValue().getId())) <span class="conum" data-value="3" />
       .addInsertHandler((e) -&gt; insertCount.incrementAndGet()) <span class="conum" data-value="4" />
       .addDeleteHandler((e) -&gt; deleteCount.incrementAndGet()); <span class="conum" data-value="5" />

customers.addMapListener(simpleMapListener, 1, false);  <span class="conum" data-value="6" />

customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

customers.put(customer1.getId(), customer1);
customers.put(customer2.getId(), customer2);

customers.clear();

// should only be 1 insert and 1 delete as we are listening on the key  <span class="conum" data-value="7" />
Eventually.assertThat(invoking(this).getInsertCount(), is(1));
Eventually.assertThat(invoking(this).getDeleteCount(), is(1));</markup>

<ul class="colist">
<li data-value="1">Create the <code>SimpleMapListener</code> instance</li>
<li data-value="2">Add an insert handler to display new customers</li>
<li data-value="3">Add delete a handler to display deleted customers</li>
<li data-value="4">Add an insert handler to increment an atomic</li>
<li data-value="5">Add delete a handler to increment an atomic</li>
<li data-value="6">Register the listener on the key <code>1</code> (customer id 1)</li>
<li data-value="7">wait for all events</li>
</ul>
</li>
<li>
Review the <code>testListenOnQueries</code> code
<p>This test uses the <code>CustomerMapListener</code> to listen on a query for customers in NY and returns lite events.</p>

<markup
lang="java"

>Logger.info("*** testListenOnQueries");
customers.clear();
mapListener = new CustomerMapListener(); <span class="conum" data-value="1" />

// MapListener listening only to new customers from NY
Filter&lt;Customer&gt;                  filter      = Filters.like(Customer::getAddress, "%NY%"); <span class="conum" data-value="2" />
MapEventFilter&lt;Integer, Customer&gt; eventFilter = new MapEventFilter&lt;&gt;(filter);

customer1 = new Customer(1, "Tim", "123 James Street, Perth, Australia", Customer.BRONZE, 1000);
customer2 = new Customer(2, "James Brown", "1 Main Street, New York, NY", Customer.GOLD, 10000);
customer3 = new Customer(3, "Tony Stark", "Malibu Point 10880, 90265 Malibu, CA", Customer.SILVER, 333333);
customer4 = new Customer(4, "James Stewart", "123 5th Ave, New York, NY", Customer.SILVER, 200);

// Listen only for events where address is in New York
customers.addMapListener(mapListener, eventFilter, true); <span class="conum" data-value="3" />

customers.put(customer1.getId(), customer1);
customers.put(customer2.getId(), customer2);
customers.put(customer3.getId(), customer3);
customers.put(customer4.getId(), customer4);

// ensure that we see all events <span class="conum" data-value="4" />
Eventually.assertThat(invoking(mapListener).getInsertCount(), is(2));

// ensure we only receive lite events
Eventually.assertThat(invoking(mapListener).getLiteEvents(), is(2));

customers.removeMapListener(mapListener, eventFilter);</markup>

<ul class="colist">
<li data-value="1">Create the <code>MapListener</code> instance</li>
<li data-value="2">Create a <code>like</code> filter to select only customers whose address contains <code>NY</code></li>
<li data-value="3">Add the map listener and specify a <code>MapEventFilter</code> which takes the filter created above as well as specifying the event is <code>lite</code> event where the
new and old values may not necessarily be present</li>
<li data-value="4">wait for all events</li>
</ul>
</li>
<li>
Review the <code>testEventTypes</code> code
<p>This test uses the <code>CustomerMapListener</code> but also applies a filter to only receive insert or update events for GOLD customers.</p>

<markup
lang="java"

>Logger.info("*** testEventTypes");
customers.clear();
mapListener = new CustomerMapListener(); <span class="conum" data-value="1" />

filter = Filters.equal(Customer::getCustomerType, Customer.GOLD); <span class="conum" data-value="2" />

// listen only for events where customers has been inserted as GOLD or updated to GOLD status or were changed from GOLD
int mask = MapEventFilter.E_INSERTED | MapEventFilter.E_UPDATED_ENTERED| MapEventFilter.E_UPDATED_LEFT;  <span class="conum" data-value="3" />
eventFilter = new MapEventFilter&lt;&gt;(mask, filter);

customers.addMapListener(mapListener, eventFilter, false); <span class="conum" data-value="4" />

customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);
customer3 = new Customer(3, "Tony Stark", "Malibu Point 10880, 90265 Malibu, CA", Customer.SILVER, 333333);

customers.put(customer1.getId(), customer1);
customers.put(customer2.getId(), customer2);
customers.put(customer3.getId(), customer3);

// update customer 1 from BRONZE to GOLD
customers.invoke(1, Processors.update(Customer::setCustomerType, Customer.GOLD));
customers.invoke(2, Processors.update(Customer::setCustomerType, Customer.SILVER));

// ensure that we see all events <span class="conum" data-value="5" />
Eventually.assertThat(invoking(mapListener).getInsertCount(), is(1));
Eventually.assertThat(invoking(mapListener).getUpdateCount(), is(2));

customers.removeMapListener(mapListener, eventFilter);</markup>

<ul class="colist">
<li data-value="1">Create the <code>CustomerMapListener</code> instance</li>
<li data-value="2">Create an <code>equals</code> filter to select only GOLD customers</li>
<li data-value="3">Create a mask for inserted events for when the filter is matched or events that are updated and now the filter matches</li>
<li data-value="4">Add the map listener and specify a <code>MapEventFilter</code> which takes the filter created above/</li>
<li data-value="5">wait for all events</li>
</ul>
</li>
</ol>
</div>

<h3 id="run-example-1">Run the Examples</h3>
<div class="section">
<p>Run the examples using the test case below.</p>

<ol style="margin-left: 15px;">
<li>
Run directly from your IDE by running either of the following test classes:
<ul class="ulist">
<li>
<p>com.oracle.coherence.guides.clientevents.ClientEventsTest</p>

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

</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">When the test is run you will see output from the various parts of the test code</p>
</div>
<p><strong>testStandardMapListener Output</strong></p>

<p>This test uses the <code>CustomerMapListener</code> class which is an implementation of a standard <code>MapListener</code>
listening for all events.</p>

<div class="admonition note">
<p class="admonition-inline">Output has been formatted for easier reading.</p>
</div>
<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): *** testStandardMapListener
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): <span class="conum" data-value="1" />
      New customer: new key/value=1/Customer{id=1, name='Tim', address='123 James Street Perth', customerType='BRONZE', balance=1000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): <span class="conum" data-value="2" />
      New customer: new key/value=2/Customer{id=2, name='James Brown', address='1 Main Street New York NY', customerType='GOLD', balance=10000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): <span class="conum" data-value="3" />
       Updated customer key=1, old=Customer{id=1, name='Tim', address='123 James Street Perth', customerType='BRONZE', balance=1000},
                               new=Customer{id=1, name='Tim', address='123 James Street Perth', customerType='BRONZE', balance=2000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): <span class="conum" data-value="4" />
      Deleted customer: old key/value=1/Customer{id=1, name='Tim', address='123 James Street Perth', customerType='BRONZE', balance=2000}</markup>

<ul class="colist">
<li data-value="1">Insert event from new customer id 1</li>
<li data-value="2">Insert event from new customer id 2</li>
<li data-value="3">Update event from updating of customer 1&#8217;s credit limit</li>
<li data-value="4">Delete event containing old version of deleted customer 1</li>
</ul>
<p><strong>testMultiplexingMapListener Output</strong></p>

<p>This test uses the <code>MultiplexingCustomerMapListener</code> class which extends <code>MultiplexingMapListener</code> to
listen for all events.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): isInsert=true, isDelete=false, isUpdate=false <span class="conum" data-value="1" />
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): key=1, old=null,
        new=Customer{id=1, name='James Brown', address='1 Main Street New York NY', customerType='GOLD', balance=10000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): isInsert=false, isDelete=false, isUpdate=true <span class="conum" data-value="2" />
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): key=1,
       old=Customer{id=1, name='James Brown', address='1 Main Street New York NY', customerType='GOLD', balance=10000},
       new=Customer{id=1, name='James Brown', address='Updated address', customerType='GOLD', balance=10000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): isInsert=false, isDelete=true, isUpdate=false <span class="conum" data-value="3" />
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): key=1,
       old=Customer{id=1, name='James Brown', address='Updated address', customerType='GOLD', balance=10000}, new=null</markup>

<ul class="colist">
<li data-value="1">Insert event from new customer id 1</li>
<li data-value="2">Update event from an update of customer 1 address</li>
<li data-value="3">Delete event from customer 1</li>
</ul>
<p><strong>testSimpleMapListener Output</strong></p>

<p>This test uses the <code>SimpleMapListener</code> and lambdas to register event handlers for only the key <code>1</code>.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): *** testSimpleMapListener
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): New Customer added with id=1
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): Deleted customer id =1</markup>

<p><strong>testListenOnQueries Output</strong></p>

<p>This test uses the <code>CustomerMapListener</code> to listen on a query for customers in NY and returns lite events.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): New customer: new key/value=2/null
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): New customer: new key/value=4/null</markup>

<div class="admonition note">
<p class="admonition-inline">Both above queries only return the key because they are <code>lite</code> events and only customer 2 and 4 are returned as they
are the only ones with <code>NY</code> in the address.</p>
</div>
<p><strong>testEventTypes Output</strong></p>

<p>This test uses the <code>CustomerMapListener</code> but also applies a filter to only receive insert or update events for GOLD customers.</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): *** testEventTypes
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): <span class="conum" data-value="1" />
      New customer: new key/value=2/Customer{id=2, name='James Brown', address='1 Main Street New York NY', customerType='GOLD', balance=10000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): Updated customer key=1, <span class="conum" data-value="2" />
      old=Customer{id=1, name='Tim', address='123 James Street Perth', customerType='BRONZE', balance=1000},
      new=Customer{id=1, name='Tim', address='123 James Street Perth', customerType='GOLD', balance=1000}
&lt;Info&gt; (thread=DistributedCache:PartitionedCache:EventDispatcher, member=1): Updated customer key=2, <span class="conum" data-value="3" />
      old=Customer{id=2, name='James Brown', address='1 Main Street New York NY', customerType='GOLD', balance=10000},
      new=Customer{id=2, name='James Brown', address='1 Main Street New York NY', customerType='SILVER', balance=10000}</markup>

<ul class="colist">
<li data-value="1">Insert event from new GOLD customer id 2</li>
<li data-value="2">Update event changing customer type from BRONZE to GOLD for customer id 1</li>
<li data-value="3">Update event chaging cusstomer type from GOLD to BRONZE for customer id 2</li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this example you have seen how to use the following features of client events:</p>

<ul class="ulist">
<li>
<p>Understanding the <code>MapListener</code> interface</p>

</li>
<li>
<p>Listening for all events</p>

</li>
<li>
<p>Using <code>SimpleMapListener</code> and <code>MultiplexingMapListener</code></p>

</li>
<li>
<p>Using lite events</p>

</li>
<li>
<p>Listening for events for a particular key</p>

</li>
<li>
<p>Listening for events based upon filters</p>

</li>
</ul>
</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-map-events.html">Develop Applications using Map Events</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/introduction-coherence-caches.html">Understanding Near Caches</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-continuous-query-caching.html">Using Continuous Query Caches</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
