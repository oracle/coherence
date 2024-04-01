<doc-view>

<h2 id="_server_side_events">Server-Side Events</h2>
<div class="section">
<p>This guide walks you through how to use server-side events, (also known as "Live Events"), within Coherence to listen
for various events on a Coherence <code>NamedMap</code> or <code>NamedCache</code>.</p>

<p>Coherence provides an event programming model that allows extensibility within a cluster
when performing operations against a data grid. The model uses events to represent observable
occurrences of cluster operations. The events that are currently supported include:</p>

<ul class="ulist">
<li>
<p>Partitioned Cache Events – A set of events that represent the operations being performed against a
set of entries in a cache. Partitioned cache events include both entry events and entry processor events. Entry events
are related to inserting, removing, and updating entries in a cache.
Entry processor events are related to the execution of entry processors.</p>

</li>
<li>
<p>Partitioned Cache Lifecycle Events – A set of events that represent the operations for creating a cache, destroying a cache,
and clearing all entries from a cache.</p>

</li>
<li>
<p>Partitioned Service Events – A set of events that represent the operations being performed by a partitioned service. Partitioned
service events include both partition transfer events  and partition transaction events. Partition transfer events are related
to the movement of partitions among cluster members. Partition transaction events are related to changes that may span multiple
caches and are performed within the context of a single request.</p>

</li>
<li>
<p>Lifecycle Events – A set of events that represent the activation and disposal of a ConfigurableCacheFactory instance.</p>

</li>
<li>
<p>Federation Events – A set of events that represent the operations being performed by a federation service. Federation events include
both Federated connection events and federated change events. Federated connection events are related to the interaction of federated
participants and federated change events are related to cache updates.</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">In this example we will not cover Federation Events.</p>
</div>
<p>Events are registered in the cache configuration against either a cache service or individual caches via cache mappings.
The classes are annotated to identify what types of events they will receive.</p>

<p>For more information on server-side events, see the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-live-events.html#">Coherence</a> documentation.</p>

<div class="admonition note">
<p class="admonition-inline">Please see <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-map-events.html">the Coherence documentation</a> for more information on client events.</p>
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
<p><router-link to="#example-tests-1" @click.native="this.scrollFix('#example-tests-1')">Review the Tests</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#example-tests-classes" @click.native="this.scrollFix('#example-tests-classes')">Review the classes</router-link></p>

</li>
<li>
<p><router-link to="#example-tests-config" @click.native="this.scrollFix('#example-tests-config')">Review the cache configuration</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Tests</router-link></p>

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
<p>In this example you will run a number of tests that demonstrate the following features of server-side events including:</p>

<ul class="ulist">
<li>
<p>Understanding where to declare interceptors in your cache config</p>

</li>
<li>
<p>Listening for <code>cache events</code> related to mutations of cache data, and execution of entry processors</p>

</li>
<li>
<p>Listening for <code>transfer events</code> related to partition transfers and loss events</p>

</li>
<li>
<p>Listening for <code>partitioned cache events</code> related to creation, destruction and truncating of caches</p>

</li>
<li>
<p>Listening for <code>lifecycle events</code> for <code>ConfigurableCacheFactory</code> instantiation</p>

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
<p class="admonition-inline">You can skip the tests in the initial build by adding the following options: <code>-DskipTests</code> for Maven or <code>-x test</code> for Gradle.</p>
</div>
</div>

<h4 id="running">Running the Examples</h4>
<div class="section">
<p>This example comprises a number of tests showing various server-side events features.</p>

<ol style="margin-left: 15px;">
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
<p>The example code comprises a number of classes:</p>

<p><strong>Tests</strong></p>

<ul class="ulist">
<li>
<p><code>ServerCacheEventsTest</code> - tests for various cache events</p>

</li>
<li>
<p><code>ServerPartitionEventsTest</code> - tests for partition based events</p>

</li>
<li>
<p><code>AbstractEventsTest</code> - a class used by both tests which starts the clusters</p>

</li>
</ul>
<p><strong>Model</strong></p>

<ul class="ulist">
<li>
<p><code>Customer</code> - represents a fictional customer</p>

</li>
<li>
<p><code>AuditEvent</code> - represents an audit event</p>

</li>
</ul>
<p><strong>Interceptors</strong></p>

<ul class="ulist">
<li>
<p><code>AuditingInterceptor</code> - creates audit events after inserts, updates or removes on a cache</p>

</li>
<li>
<p><code>EntryProcessorAuditingInterceptor</code> - creates audit events after entry processor executions</p>

</li>
<li>
<p><code>UppercaseInterceptor</code> - a mutating interceptor that changes the <code>name</code> and <code>address</code> attributes to uppercase</p>

</li>
<li>
<p><code>ValidationInterceptor</code> - a mutating interceptor that optionally rejects updates if certain business rules are not met</p>

</li>
<li>
<p><code>TransferEventsInterceptor</code> - creates audit events after any partition transfers made</p>

</li>
<li>
<p><code>CacheLifecycleEventsInterceptor</code> - creates audit events after caches are created, truncated or destroyed</p>

</li>
<li>
<p><code>LifecycleEventsInterceptor</code> - logs a message when ConfigurableCacheFactories are activated or destroyed</p>

</li>
</ul>

<h4 id="example-tests-classes">Review the classes</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Review the <code>Customer</code> class.
<p>Some tests use the <code>Customer</code> class which has the following fields:</p>

<markup
lang="java"

>private int id;
private String name;
private String address;
private String customerType;
private long creditLimit;</markup>

</li>
<li>
Review the <code>AuditEvent</code> class.
<p>Some tests use the <code>AuditEvent</code> class which has the following fields:</p>

<markup
lang="java"

>/**
 * Unique Id for the audit event.
 */
private UUID id;

/**
 * The target of the event such as cache, partition, etc.
 */
private String target;

/**
 * The type of event.
 */
private String eventType;

/**
 * Specific event data.
 */
private String eventData;

/**
 * Time of the event.
 */
private long   eventTime;</markup>

</li>
<li>
Review the <code>AuditingInterceptor</code> which audits any mutations to caches using post-commit events.
<div class="admonition note">
<p class="admonition-inline">See <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-live-events.html#GUID-7CC4EAFB-7A82-4B6F-A7FD-A776D33F36CD">here</a> for details of all Partitioned Cache events.</p>
</div>
<markup
lang="java"

>@Interceptor(identifier = "AuditingInterceptor", order = Interceptor.Order.HIGH)  <span class="conum" data-value="1" />
@EntryEvents({EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})  <span class="conum" data-value="2" />
public class AuditingInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor&lt;EntryEvent&lt;?, ?&gt;&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(EntryEvent&lt;?, ?&gt; event) {  <span class="conum" data-value="4" />
        String          oldValue  = null;
        String          newValue  = null;
        EntryEvent.Type eventType = event.getType();
        Object          key       = event.getKey();

        if (eventType == EntryEvent.Type.REMOVED || eventType == EntryEvent.Type.UPDATED) {  <span class="conum" data-value="5" />
            oldValue = event.getOriginalValue().toString();
        }
        if (eventType == EntryEvent.Type.INSERTED || eventType == EntryEvent.Type.UPDATED) {
            newValue = event.getValue().toString();
        }

        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), eventType.toString(),  <span class="conum" data-value="6" />
                String.format("key=%s, old=%s, new=%s", key, oldValue, newValue));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name and optional order of HIGH or LOW as the priority</li>
<li data-value="2">Identifies the <code>EntryEvents</code> that will be intercepted. INSERTED, UPDATED and REMOVED are raised asynchronously after the event has happened</li>
<li data-value="3">Identifies the type of events, in this case <code>EntryEvents</code></li>
<li data-value="4">Overrides method to respond to the event</li>
<li data-value="5">Identifies the type of event and sets the payload accordingly</li>
<li data-value="6">Adds the audit event to the auditing cache</li>
</ul>
<p>This is used in the following test in <code>ServerCacheEventsTest</code>:</p>

<markup
lang="java"

>/**
 * Test the {@link AuditingInterceptor} which will audit any changes to caches
 * that fall thought and match the '*' cache-mapping.
 */
@Test
public void testAuditingInterceptor() {
    System.out.println("testAuditingInterceptor");
    CoherenceClusterMember member = getMember1();

    // create two different caches to be audited which will match to the auditing-scheme
    NamedCache&lt;Integer, String&gt;   cache1 = member.getCache("test-cache");
    NamedCache&lt;Integer, Customer&gt; cache2 = member.getCache("test-customer");

    cache1.truncate();
    cache2.truncate();
    Eventually.assertDeferred(() -&gt; auditEvents.size(), Matchers.is(4));

    // clear the audit-events cache, so we miss the created and truncated events
    auditEvents.clear();

    // generate some mutations that will be audited
    cache1.put(1, "one");
    cache1.put(2, "two");
    cache1.put(1, "ONE");
    cache1.remove(1);

    dumpAuditEvents("testAuditingInterceptor-1");

    // ensure 3 inserts and 1 remove events are received
    Eventually.assertDeferred(() -&gt; auditEvents.size(), Matchers.is(4));

    auditEvents.clear();

    // generate new set of mutations for customers
    cache2.put(1, new Customer(1, "Tim", "Address 1", Customer.GOLD, 10000));
    cache2.put(2, new Customer(2, "John", "Address 2", Customer.SILVER, 4000));
    cache2.clear();

    dumpAuditEvents("testAuditingInterceptor-2");

    // ensure 2 insert and 2 remove events are received
    Eventually.assertDeferred(() -&gt; auditEvents.values().size(), Matchers.is(4));
}</markup>

</li>
<li>
Review the <code>EntryProcessorAuditingInterceptor</code> which audits entry processors executions using post-commit events.
<markup
lang="java"

>@Interceptor(identifier = "EntryProcessorAuditingInterceptor")  <span class="conum" data-value="1" />
@EntryProcessorEvents({EntryProcessorEvent.Type.EXECUTED})  <span class="conum" data-value="2" />
public class EntryProcessorAuditingInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor&lt;EntryProcessorEvent&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(EntryProcessorEvent event) {  <span class="conum" data-value="4" />
        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), event.getType().toString(),
                String.format("Entries=%d, processor=%s", event.getEntrySet().size(), event.getProcessor().toString()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name</li>
<li data-value="2">Identifies the <code>EntryProcessorEvents</code> that will be intercepted. EXECUTED event is raised asynchronously after the event has happened</li>
<li data-value="3">Identifies the type of events, in this case <code>EntryProcessorEvents</code></li>
<li data-value="4">Overrides method to respond to the event and add to the auditing cache</li>
</ul>
<p>This is used in the following test in <code>ServerCacheEventsTest</code>:</p>

<markup
lang="java"

>/**
 * Test the {@link EntryProcessorAuditingInterceptor} which will audit any entry processor
 * executions on caches that match the '*' cache-mapping.
 */
@Test
public void testEntryProcessorInterceptor() {
    System.out.println("testEntryProcessorInterceptor");
    CoherenceClusterMember member = getMember1();

    // create a cache to audit entry processor events on
    NamedCache&lt;Integer, Customer&gt; cache = member.getCache("test-customer");
    cache.truncate();
    Eventually.assertDeferred(() -&gt; auditEvents.size(), Matchers.is(4));

    // clear the audit-events cache, so we miss the created and truncated events
    auditEvents.clear();

    // add some entries
    cache.put(1, new Customer(1, "Tim", "Address 1", Customer.GOLD, 10_000));
    cache.put(2, new Customer(2, "Tom", "Address 2", Customer.SILVER, 10_000));
    cache.put(3, new Customer(3, "Helen", "Address 3", Customer.BRONZE, 10_000));
    Eventually.assertDeferred(() -&gt; auditEvents.size(), Matchers.is(3));

    auditEvents.clear();

    cache.invokeAll(Processors.update(Customer::setCreditLimit, 100_000L));

    dumpAuditEvents("testEntryProcessorInterceptor-1");
    // up to 3 entry processor events and 3 updates
    Eventually.assertDeferred(() -&gt; auditEvents.aggregate(Filters.equal(AuditEvent::getEventType, "EXECUTED"), Aggregators.count()), Matchers.lessThanOrEqualTo(3));
    Eventually.assertDeferred(() -&gt; auditEvents.aggregate(Filters.equal(AuditEvent::getEventType, "UPDATED"), Aggregators.count()), Matchers.equalTo(3));

    auditEvents.clear();

    // invoke an entry processor across all customers to update credit limit to 100,000
    cache.invokeAll(Processors.update(Customer::setCreditLimit, 100_000L));
    cache.invoke(1, Processors.update(Customer::setCreditLimit, 100_000L));

    dumpAuditEvents("testEntryProcessorInterceptor-2");

    // ensure up to 4 EXECUTED events are received
    Eventually.assertDeferred(() -&gt; auditEvents.aggregate(Filters.equal(AuditEvent::getEventType, "EXECUTED"), Aggregators.count()), Matchers.lessThanOrEqualTo(4));
}</markup>

</li>
<li>
Review the <code>UppercaseInterceptor</code> which changes the <code>name</code> and <code>address</code> attributes to uppercase.
<markup
lang="java"

>@Interceptor(identifier = "UppercaseInterceptor")  <span class="conum" data-value="1" />
@EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.UPDATING})  <span class="conum" data-value="2" />
public class UppercaseInterceptor
        implements EventInterceptor&lt;EntryEvent&lt;Integer, Customer&gt;&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(EntryEvent&lt;Integer, Customer&gt; event) {  <span class="conum" data-value="4" />
        BinaryEntry&lt;Integer, Customer&gt; entry = event.getEntry();
        Customer customer = entry.getValue();
        customer.setName(customer.getName().toUpperCase());
        customer.setAddress(customer.getAddress().toUpperCase());
        entry.setValue(customer);  <span class="conum" data-value="5" />
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name</li>
<li data-value="2">Identifies the <code>EntryEvents</code> that will be intercepted. INSERTING and UPDATING are raised synchronously before the operation is performed. Care must be taken to ensure these operations take as short amount of time as possible as implicit locks are held for the keys while updating.</li>
<li data-value="3">Identifies the type of events, in this case <code>EntryEvent</code> and the key and value are also defined using generics</li>
<li data-value="4">Overrides method to respond to the event</li>
<li data-value="5">Ensures the changes are persisted by calling <code>entry.setValue()</code></li>
</ul>
<p>This is used in the following test in <code>ServerCacheEventsTest</code>:</p>

<markup
lang="java"

>/**
 *  Test the {@link UppercaseInterceptor} which is defined on the 'customers' cache only,
 *  to update name and address fields to uppercase.
 */
@Test
public void testCustomerUppercaseInterceptor() {
    System.out.println("testCustomerUppercaseInterceptor");

    NamedCache&lt;Integer, Customer&gt; customers = getMember1().getCache("customers");
    customers.truncate();

    // put a new Customer with lowercase names and addresses
    customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.GOLD, 10000L));

    // validate that the name and address are uppercase
    Customer customer = customers.get(1);
    assertEquals(customer.getName(), "TIM");
    assertEquals(customer.getAddress(), "123 JAMES STREET, PERTH");

    // update a customers name and ensure that it is updated to uppercase
    customers.invoke(1, Processors.update(Customer::setName, "timothy"));
    assertEquals(customers.get(1).getName(), "TIMOTHY");
}</markup>

</li>
<li>
Review the <code>ValidationInterceptor</code> which rejects or accepts changes based upon some simple business rules.
<markup
lang="java"

>@Interceptor(identifier = "ValidationInterceptor") <span class="conum" data-value="1" />
@EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.UPDATING}) <span class="conum" data-value="2" />
public class ValidationInterceptor
        implements EventInterceptor&lt;EntryEvent&lt;Integer, Customer&gt;&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(EntryEvent&lt;Integer, Customer&gt; event) {  <span class="conum" data-value="4" />
        BinaryEntry&lt;Integer, Customer&gt; entry       = event.getEntry();
        Customer                       customerOld = entry.getOriginalValue();
        Customer                       customerNew = entry.getValue();
        EntryEvent.Type                eventType   = event.getType();

        if (eventType == EntryEvent.Type.INSERTING) {  <span class="conum" data-value="5" />
            // Rule 1 - New customers cannot have credit limit above 1,000,000 unless they are GOLD
            if (customerNew.getCreditLimit() &gt;= 1_000_000L &amp;&amp; !customerNew.getCustomerType().equals(Customer.GOLD)) {
                // reject the update
                throw new RuntimeException("Only gold customers may have credit limits above 1,000,000");
            }
        }
        else if (eventType == EntryEvent.Type.UPDATING) {  <span class="conum" data-value="6" />
            // Rule 2 - Cannot change customer type from BRONZE directly to GOLD, must go BRONZE -&gt; SILVER -&gt; GOLD
            if (customerNew.getCustomerType().equals(Customer.GOLD) &amp;&amp; customerOld.getCustomerType().equals(Customer.BRONZE)) {
                // reject the update
                throw new RuntimeException("Cannot update customer directly to GOLD from BRONZE");
            }
        }

        // otherwise, continue with update
        entry.setValue(customerNew);  <span class="conum" data-value="7" />
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name</li>
<li data-value="2">Identifies the <code>EntryEvents</code> that will be intercepted. INSERTING and UPDATING are raised synchronously before the operation is performed. Care must be taken to ensure these operations take as short amount of time as possible as implicit locks are held for the keys while updating.</li>
<li data-value="3">Identifies the type of events, in this case <code>EntryEvent</code> and the key and value are also defined using generics</li>
<li data-value="4">Overrides method to respond to the event</li>
<li data-value="5">Validates the first business rule if the event is an insert. If the rule fails, then throw a <code>RuntimeException</code></li>
<li data-value="6">Validates the second business rule if the event is an update. If the rule fails, then throw a <code>RuntimeException</code></li>
<li data-value="7">Saves the entry if all the business rules pass</li>
</ul>
<p>This is used in the following test in <code>ServerCacheEventsTest</code>:</p>

<markup
lang="java"

>/**
 * Test the {@link ValidationInterceptor} which will reject updates if business rules fail.
 */
@Test
public void testValidatingInterceptor() {
    System.out.println("testValidatingInterceptor");
    NamedCache&lt;Integer, Customer&gt; customers = getMember1().getCache("customers");
    customers.truncate();

    // try adding a BRONZE customer with credit limit &gt; 1,000,000
    try {
        customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.BRONZE, 2_000_000L));
        fail("Put succeeded but should have failed");
    }
    catch (Exception e) {
        System.out.printf("Put was correctly rejected: %s\n", e.getMessage());
    }

    // should be rejected
    assertEquals(customers.size(), 0);

    // add a normal BRONZE customer, should succeed with credit limit 10,000
    customers.put(1, new Customer(1, "tim", "123 james street, perth", Customer.BRONZE, 10_000L));
    assertEquals(customers.size(), 1);

    // try and update credit limit to GOLD from BRONZE, should fail
    try {
        customers.invoke(1, Processors.update(Customer::setCustomerType, Customer.GOLD));
        fail("Put succeeded but should have failed");
    }
    catch (Exception e) {
            System.out.printf("Update was correctly rejected: %s\n", e.getMessage());
    }

    assertEquals(customers.get(1).getCustomerType(), Customer.BRONZE);
}</markup>

</li>
<li>
Review the <code>TransferEventsInterceptor</code> which audits partition transfer events.
<markup
lang="java"

>@Interceptor(identifier = "TransferEventsInterceptor")  <span class="conum" data-value="1" />
@TransferEvents({TransferEvent.Type.ARRIVED, TransferEvent.Type.DEPARTING, TransferEvent.Type.LOST}) <span class="conum" data-value="2" />
public class TransferEventsInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor&lt;TransferEvent&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(TransferEvent event) {  <span class="conum" data-value="4" />
        AuditEvent auditEvent = new AuditEvent("partition=" + event.getPartitionId(), event.getType().toString(),
                String.format("Partitions from remote member %s", event.getRemoteMember()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name</li>
<li data-value="2">Identifies the <code>TransferEvents</code> that will be intercepted. Transfer events are raised while holding a lock on the partition being transferred that blocks any operations for the partition.</li>
<li data-value="3">Identifies the type of events, in this case <code>TransferEvents</code></li>
<li data-value="4">Overrides method to respond to the event</li>
</ul>
<p>This is used in the following test in <code>ServerPartitionEventsTest</code>:</p>

<markup
lang="java"

>@Test
public void testPartitionEvents() {
    System.out.println("testPartitionEvents");
    CoherenceClusterMember member1 = getMember1();
    CoherenceClusterMember member2 = getMember2();

    NamedCache&lt;Integer, String&gt; cache = member1.getCache("test-cache");

    for (int i = 0; i &lt; 10; i++) {
        cache.put(i, "value-" + i);
    }

    // ensure all audit events are received = 10 insert events plus 2 cache created events
    Eventually.assertDeferred(()-&gt;auditEvents.size(), Matchers.is(12));

    // shutdown the second member
    member2.close();

    // wait for additional partition events to be received
    Eventually.assertDeferred(() -&gt; auditEvents.size(), Matchers.greaterThan(16));

    dumpAuditEvents("testPartitionEvents");
}</markup>

</li>
<li>
Review the <code>CacheLifecycleEventsInterceptor</code> which audits cache lifecycle events.
<markup
lang="java"

>@Interceptor(identifier = "CacheLifecycleEventsInterceptor") <span class="conum" data-value="1" />
@CacheLifecycleEvents( {CacheLifecycleEvent.Type.CREATED, CacheLifecycleEvent.Type.DESTROYED, CacheLifecycleEvent.Type.TRUNCATED})  <span class="conum" data-value="2" />
public class CacheLifecycleEventsInterceptor
        extends AbstractAuditingInterceptor
        implements EventInterceptor&lt;CacheLifecycleEvent&gt;, Serializable {  <span class="conum" data-value="3" />

    @Override
    public void onEvent(CacheLifecycleEvent event) {  <span class="conum" data-value="4" />
        AuditEvent auditEvent = new AuditEvent("cache=" + event.getCacheName(), event.getType().toString(),
                String.format("Event from service %s", event.getServiceName()));
        getAuditCache().put(auditEvent.getId(), auditEvent);
    }
}</markup>

<ul class="colist">
<li data-value="1">Defines the interceptor name</li>
<li data-value="2">Identifies the <code>CacheLifecycleEvent</code> that will be intercepted. CREATED, DESTROYED and TRUNCATED are raised asynchronously after the operation is completed.</li>
<li data-value="3">Identifies the type of events, in this case <code>CacheLifecycleEvent</code></li>
<li data-value="4">Overrides method to respond to the event</li>
</ul>
<p>This is used in the following test in <code>ServerCacheEventsTest</code>:</p>

<markup
lang="java"

>@Test
public void testTruncate() {
    System.out.println("testTruncate");
    auditEvents.clear();

    NamedCache&lt;Integer, String&gt; cache1 = getMember1().getCache("test-cache");
    cache1.truncate();

    // ensure we get two events, one from each storage node
    Eventually.assertDeferred(() -&gt; auditEvents.values(equal(AuditEvent::getEventType, "TRUNCATED")).size(), Matchers.is(2));

    dumpAuditEvents("truncate");
}</markup>

</li>
</ol>
</div>

<h4 id="example-tests-config">Review the cache config</h4>
<div class="section">
<p>The interceptors are added via cache config and can be applied at the service or cache level.</p>

<ol style="margin-left: 15px;">
<li>
Review the Cache Scheme Mapping
<markup
lang="xml"

>  &lt;interceptors&gt;
      &lt;interceptor&gt;  <span class="conum" data-value="1" />
        &lt;name&gt;LifecycleEventsInterceptor&lt;/name&gt;
        &lt;instance&gt;
          &lt;class-name&gt;
            com.oracle.coherence.guides.serverevents.interceptors.LifecycleEventsInterceptor
          &lt;/class-name&gt;
        &lt;/instance&gt;
      &lt;/interceptor&gt;
  &lt;/interceptors&gt;

&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;customers&lt;/cache-name&gt;  <span class="conum" data-value="2" />
    &lt;scheme-name&gt;server-scheme&lt;/scheme-name&gt;
    &lt;interceptors&gt;
      &lt;interceptor&gt;
        &lt;name&gt;UppercaseInterceptor&lt;/name&gt;
        &lt;instance&gt;
          &lt;class-name&gt;
            com.oracle.coherence.guides.serverevents.interceptors.UppercaseInterceptor
          &lt;/class-name&gt;
        &lt;/instance&gt;
      &lt;/interceptor&gt;
      &lt;interceptor&gt;
        &lt;name&gt;ValidationInterceptor&lt;/name&gt;
        &lt;instance&gt;
          &lt;class-name&gt;
            com.oracle.coherence.guides.serverevents.interceptors.ValidationInterceptor
          &lt;/class-name&gt;
        &lt;/instance&gt;
      &lt;/interceptor&gt;
    &lt;/interceptors&gt;
  &lt;/cache-mapping&gt;

  &lt;!-- cache to store auditing events --&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;audit-events&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-scheme&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;

  &lt;!-- any caches other than are defined above will be audited --&gt;
  &lt;cache-mapping&gt;
    &lt;cache-name&gt;*&lt;/cache-name&gt;
    &lt;scheme-name&gt;auditing-scheme&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;</markup>

<ul class="colist">
<li data-value="1">Defines an interceptor called <code>LifecycleEventsInterceptor</code> to log any <code>ConfigurableCacheFactory</code> events.</li>
<li data-value="2">Defines customers cache which has the <code>UppercaseInterceptor</code> and <code>ValidationInterceptor</code> enabled for only this cache</li>
</ul>
</li>
<li>
Review the Caching Schemes
<markup
lang="xml"

>&lt;!--
   Any caches in this scheme will be audited and data put in "audit-events" cache.
--&gt;
&lt;distributed-scheme&gt;
  &lt;scheme-name&gt;auditing-scheme&lt;/scheme-name&gt;  <span class="conum" data-value="1" />
  &lt;service-name&gt;DistributedCacheAudit&lt;/service-name&gt;
  &lt;partition-count&gt;31&lt;/partition-count&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme/&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;interceptors&gt;
    &lt;interceptor&gt;
      &lt;name&gt;AuditingInterceptor&lt;/name&gt;
      &lt;instance&gt;
        &lt;class-name&gt;
          com.oracle.coherence.guides.serverevents.interceptors.AuditingInterceptor
        &lt;/class-name&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;

    &lt;interceptor&gt;
      &lt;name&gt;EntryProcessorAuditingInterceptor&lt;/name&gt;
      &lt;instance&gt;
        &lt;class-name&gt;
          com.oracle.coherence.guides.serverevents.interceptors.EntryProcessorAuditingInterceptor
        &lt;/class-name&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;

    &lt;interceptor&gt;
      &lt;name&gt;TransferEventsInterceptor&lt;/name&gt;
      &lt;instance&gt;
        &lt;class-name&gt;
          com.oracle.coherence.guides.serverevents.interceptors.TransferEventsInterceptor
        &lt;/class-name&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;

    &lt;interceptor&gt;
      &lt;name&gt;CacheLifecycleEventsInterceptor&lt;/name&gt;
      &lt;instance&gt;
        &lt;class-name&gt;
          com.oracle.coherence.guides.serverevents.interceptors.CacheLifecycleEventsInterceptor
        &lt;/class-name&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;
  &lt;/interceptors&gt;
&lt;/distributed-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">Defines <code>auditing-scheme</code> which has the <code>AuditingInterceptor</code>, <code>EntryProcessorAuditingInterceptor</code>, <code>CacheLifecycleEventsInterceptor</code> and <code>TransferEventsInterceptor</code> enabled for any caches using this scheme.</li>
</ul>
</li>
</ol>
</div>
</div>

<h3 id="run-example-1">Run the Tests</h3>
<div class="section">
<p>Run the examples using the test case below.</p>

<ol style="margin-left: 15px;">
<li>
Run directly from your IDE by running either of the following test classes:
<ul class="ulist">
<li>
<p>com.oracle.coherence.guides.serverevents.ServerPartitionEventsTest</p>

</li>
<li>
<p>com.oracle.coherence.guides.serverevents.ServerCacheEventsTest</p>

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
<p>When the test is run you will see output from the various parts of the test code.</p>

<div class="admonition note">
<p class="admonition-inline">Output has been truncated and formatted for easier reading.</p>
</div>
<p><strong>testPartitions Output</strong></p>

<markup
lang="bash"

>testPartitionEvents
Dumping the audit events testPartitionEvents
<span class="conum" data-value="1" />
AuditEvent{id=2E1E1FE69E, target='cache=test-cache', eventType='CREATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255209123}
AuditEvent{id=54A54A5CED, target='cache=test-cache', eventType='CREATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255209129}
<span class="conum" data-value="2" />
AuditEvent{id=AAA54A5CEE, target='cache=test-cache', eventType='INSERTED', eventData='key=0, old=null, new=value-0', eventTime=1652255209135}
AuditEvent{id=A51E1FE69F, target='cache=test-cache', eventType='INSERTED', eventData='key=1, old=null, new=value-1', eventTime=1652255209141}
...
AuditEvent{id=A1A54A5CF3, target='cache=test-cache', eventType='INSERTED', eventData='key=9, old=null, new=value-9', eventTime=1652255209169}

...
<span class="conum" data-value="3" />
AuditEvent{id=961E1FE6A3, target='partition=0', eventType='ARRIVED', eventData='Partitions from remote member Member(Id=1, ...', eventTime=1652255209572}
AuditEvent{id=261E1FE6A4, target='partition=1', eventType='ARRIVED', eventData='Partitions from remote member Member(Id=1, ...', eventTime=1652255209580}
...
AuditEvent{id=531E1FE6B1, target='partition=14', eventType='ARRIVED', eventData='Partitions from remote member Member(Id=1, ...', eventTime=1652255209587}</markup>

<ul class="colist">
<li data-value="1">Lifecycle events from creation of cache from two storage nodes</li>
<li data-value="2">Insert events for cache entries</li>
<li data-value="3">Partitions arriving from remove member before shutdown</li>
</ul>
<p><strong>testTruncate Output</strong></p>

<markup
lang="bash"

>testTruncate
Dumping the audit events truncate
<span class="conum" data-value="1" />
AuditEvent{id=B8127D2701, target='cache=test-cache', eventType='CREATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255218772}
AuditEvent{id=6BD64A90EA, target='cache=test-cache', eventType='CREATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255218784}
AuditEvent{id=7E127D2702, target='cache=test-cache', eventType='TRUNCATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255218802}
AuditEvent{id=17D64A90EB, target='cache=test-cache', eventType='TRUNCATED', eventData='Event from service DistributedCacheAudit', eventTime=1652255218806}</markup>

<ul class="colist">
<li data-value="1">Both CREATED and TRUNCATED events are shown.</li>
</ul>
<p><strong>testEntryProcessorInterceptor Output</strong></p>

<markup
lang="bash"

>testEntryProcessorInterceptor
Dumping the audit events testEntryProcessorInterceptor-1

<span class="conum" data-value="1" />
AuditEvent{id=AE5BC2D3EB, target='cache=test-customer', eventType='EXECUTED', eventData='Entries=1, processor=UpdaterProcessor(Customer$setCreditLimit...', eventTime=1652319479550}
AuditEvent{id=C25BC2D3EC, target='cache=test-customer', eventType='UPDATED', eventData='key=1,
   old=Customer{id=1, name='Tim', address='Address 1', customerType='GOLD', balance=10000},
   new=Customer{id=1, name='Tim', address='Address 1', customerType='GOLD', balance=100000}', eventTime=1652319479553}
AuditEvent{id=3D82ADF7F7, target='cache=test-customer', eventType='EXECUTED', eventData='Entries=2, processor=UpdaterProcessor(Customer$setCreditLimit...'}}, arguments=[]}}, 100000)', eventTime=1652319479553}
AuditEvent{id=4382ADF7F8, target='cache=test-customer', eventType='UPDATED', eventData='key=2,
   old=Customer{id=2, name='Tom', address='Address 2', customerType='SILVER', balance=10000},
   new=Customer{id=2, name='Tom', address='Address 2', customerType='SILVER', balance=100000}', eventTime=1652319479556}
AuditEvent{id=575BC2D3ED, target='cache=test-customer', eventType='UPDATED', eventData='key=3,
   old=Customer{id=3, name='Helen', address='Address 3', customerType='BRONZE', balance=10000},
   new=Customer{id=3, name='Helen', address='Address 3', customerType='BRONZE', balance=100000}', eventTime=1652319479556}

Dumping the audit events testEntryProcessorInterceptor-2

<span class="conum" data-value="2" />
AuditEvent{id=F05BC2D3EE, target='cache=test-customer', eventType='EXECUTED', eventData='Entries=2, processor=UpdaterProcessor(...'}}, arguments=[]}}, 100000)', eventTime=1652319479577}
AuditEvent{id=7982ADF7F9, target='cache=test-customer', eventType='EXECUTED', eventData='Entries=1, processor=UpdaterProcessor(...'}}, arguments=[]}}, 100000)', eventTime=1652319479578}
AuditEvent{id=235BC2D3EF, target='cache=test-customer', eventType='EXECUTED', eventData='Entries=1, processor=UpdaterProcessor(...'}}, arguments=[]}}, 100000)', eventTime=1652319479584}</markup>

<ul class="colist">
<li data-value="1">Three insert events and two entry processor events. One from each storage-enabled node</li>
<li data-value="2">Three entry processor events, one for an individual invoke() on a key and two from the invokeAll as per item 1</li>
</ul>
<p><strong>testValidatingInterceptor Output</strong>
<strong>testValidatingInterceptor Output</strong></p>

<markup
lang="bash"

>Put was correctly rejected: Failed to execute [put] with arguments <span class="conum" data-value="1" />
   [1, Customer{id=1, name='tim', address='123 james street, perth', customerType='BRONZE', balance=2000000}]
Update was correctly rejected: Failed to execute [invoke] with arguments [1,
   UpdaterProcessor(com.oracle.coherence.guides.serverevents.ServerCacheEventsTest$$Lambda$475/0x00000008003da040@783ecb80, GOLD)]
testCustomerUppercaseInterceptor</markup>

<ul class="colist">
<li data-value="1">Messages from rejected updates</li>
</ul>
<p><strong>testAuditingInterceptor Output</strong></p>

<markup
lang="bash"

>testAuditingInterceptor
Dumping the audit events testAuditingInterceptor-1

<span class="conum" data-value="1" />
AuditEvent{id=1D127D270E, target='cache=test-cache', eventType='INSERTED', eventData='key=1, old=null, new=one', eventTime=1652255219418}
AuditEvent{id=25D64A90F4, target='cache=test-cache', eventType='INSERTED', eventData='key=2, old=null, new=two', eventTime=1652255219428}
AuditEvent{id=A5127D270F, target='cache=test-cache', eventType='UPDATED', eventData='key=1, old=one, new=ONE', eventTime=1652255219432}
AuditEvent{id=EF127D2710, target='cache=test-cache', eventType='REMOVED', eventData='key=1, old=ONE, new=null', eventTime=1652255219436}

Dumping the audit events testAuditingInterceptor-2

<span class="conum" data-value="2" />
AuditEvent{id=A5127D2711, target='cache=test-customer', eventType='INSERTED', eventData='key=1, old=null,
   new=Customer{id=1, name='Tim', address='Address 1', customerType='GOLD', balance=10000}', eventTime=1652255219456}
AuditEvent{id=5BD64A90F5, target='cache=test-customer', eventType='INSERTED', eventData='key=2, old=null,
   new=Customer{id=2, name='John', address='Address 2', customerType='SILVER', balance=4000}', eventTime=1652255219460}
AuditEvent{id=CAD64A90F6, target='cache=test-customer', eventType='REMOVED', eventData='key=2,
   old=Customer{id=2, name='John', address='Address 2', customerType='SILVER', balance=4000}, new=null', eventTime=1652255219466}
AuditEvent{id=27127D2712, target='cache=test-customer', eventType='REMOVED', eventData='key=1,
   old=Customer{id=1, name='Tim', address='Address 1', customerType='GOLD', balance=10000}, new=null', eventTime=1652255219466}</markup>

<ul class="colist">
<li data-value="1">Two inserts, one update and a remove</li>
<li data-value="2">Two inserts and two removes as a result of <code>clear()</code></li>
</ul>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide we walked you through how to use server-side events within Coherence to listen
for various events on a Coherence <code>NamedMap</code> or <code>NamedCache</code>.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-live-events.html#">Develop Applications using Server Side Events</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-map-events.html">Client Side Events</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
