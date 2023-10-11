<doc-view>

<h2 id="_key_association">Key Association</h2>
<div class="section">
<p>Key association in Coherence is a way of associating related data together in a single partition.
This data could be entries in a single cache, or it could be entries in multiple caches managed by the same cache service.
If related data is known to exist in a single partition, then this allows those related entries to be accessed as part of
a single atomic partition level transaction. For example a single entry processor call could atomically update multiple
related entries, possibly across multiple caches. Queries could also make use of this, for example a custom aggregator could
aggregate results from multiple entries possibly from multiple caches, in a single partition.
This can be a way to simulate certain types of join query for related data.</p>

<p>Key association can be used to implement similar behaviour to a multi-map, where a single key maps to a list or set of related data.
Using key association and related caches instead of a single multi-map offers a lot more flexibility for supporting various use-cases.</p>

</div>

<h2 id="_what_you_will_build">What You Will Build</h2>
<div class="section">
<p>This example is going to demonstrate a simple use case of handling notifications sent to customers.
A customer can have zero or more notifications. A customer may span regions, so notifications are region specific.
A notification also has an expiry time, so it will be automatically evicted when the expiry time is reached.
Using key association, notifications for a customer will be co-located in the same partition.</p>


<h3 id="_what_you_need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://www.oracle.com/java/technologies/downloads/">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="https://gradle.org/install/">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included, so they can be built without first installing
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
</div>

<h2 id="_the_example_data_model">The Example Data Model</h2>
<div class="section">
<p>The data model used in this example is very simple and is made up of two entities, a <code>Customer</code> and a <code>Notification</code>. A customer can have zero or more notifications. A notification is specific to a region and has an expiry time.</p>

<p>For example, in json the customer notification data may look like this:</p>

<markup
lang="json"
title="customers.json"
>[
    {
        "id": "User01",
        "notificationsByRegion": [
            {
                "region": "US",
                "notifications": [
                    {
                        "body": "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
                        "ttl": "24:00:00"
                    },
                    {
                        "body": "Eu turpis egestas pretium aenean.",
                        "ttl": "12:00:00"
                    }
                ]
            },
            {
                "region": "EU",
                "notifications":  [
                    {
                        "body": "Tincidunt id aliquet risus feugiat.",
                        "ttl": "06:00:00"
                    },
                    {
                        "body": "Quis risus sed vulputate odio ut enim blandit volutpat.",
                        "ttl": "48:00:00"
                    },
                    {
                        "body": "Sem et tortor consequat id porta nibh.",
                        "ttl": "01:00:00"
                    }
                ]
            }
        ]
    },
    {
        "id": "User02",
        "notificationsByRegion": [
            {
                "region": "US",
                "notifications": [
                    {
                        "body": "Et malesuada fames ac turpis egestas sed tempus urna.",
                        "ttl": "01:23:45"
                    }
                ]
            }
        ]
    }
]</markup>

<p>This structure could be contained in Java, and hence in Coherence, in a <code>Map&lt;String, Map&lt;String, List&lt;Notification&gt;&gt;&gt;</code> or some other multi-map type of data structure. The disadvantages of this are that a customers' notifications are then treated as a single blob of data which could make certain operations less efficient. Any mutation or addition of notifications would require everything to be deserialized.</p>

<p>There is also a requirement in this example to automatically expire notifications from the cache based on their TTL is reached.
If all the notifications for a customer are in a single map structure, this would require some complex server side logic whereas holding each notification as a separate cache entry can leverage Coherence&#8217;s built in expiry functionality.</p>

<p>The json data above is really just notification data and this example could use just a single cache, but using two entities and two caches, for Customer and Notification, will make the example a bit more interesting.</p>


<h3 id="_model_classes">Model Classes</h3>
<div class="section">
<p>In this example there will be two Java mode classes, <code>Customer</code> and <code>Notification</code>.
The <code>Customer</code> has a <code>String</code> id field and <code>String</code> first name and last name fields.</p>

<markup
lang="java"
title="Customer.java"
>@PortableType(id = 1001, version = 1)
public class Customer {

    /**
     * The customer's identifier.
     */
    private String id;

    /**
     * The customer's first name.
     */
    private String firstName;

    /**
     * The customer's last name.
     */
    private String lastName;

    /**
     * Create a customer.
     *
     * @param id         the customer's identifier
     * @param firstName  the customer's first name
     * @param lastName   the customer's last name
     */
    public Customer(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Returns the customer's identifier.
     *
     * @return the customer's identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the customer's first name.
     *
     * @return the customer's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the customer's first name.
     *
     * @param firstName  the customer's first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the customer's last name.
     *
     * @return the customer's last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the customer's last name.
     *
     * @param lastName  the customer's last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}</markup>

<p>The <code>Notification</code> has a <code>String</code> body and a Java time <code>LocalDateTime</code> ttl field, to represent its expiry time.</p>

<markup
lang="java"
title="Notification.java"
>@PortableType(id = 1010, version = 1)
public class Notification {

    /**
     * The notification text.
     */
    private String body;

    /**
     * The time the notification expires.
     */
    private LocalDateTime ttl;

    /**
     * Create a {@link Notification}.
     *
     * @param body  the notification text
     * @param ttl   the time the notification expires
     */
    public Notification(String body, LocalDateTime ttl) {
        this.body = body;
        this.ttl = ttl;
    }

    /**
     * Returns the notification text.
     *
     * @return the notification text
     */
    public String getBody() {
        return body;
    }

    /**
     * Returns the time the notification expires.
     *
     * @return the time the notification expires
     */
    public LocalDateTime getTTL() {
        return ttl;
    }
}</markup>

<p>Both of the model classes are annotated with the <code>@PortableType</code>.
This annotation is used by the Coherence POF Maven plugin to generate Portable Object code for the classes.
Using the Coherence POF generator in this way avoids having to manually write serialization code and ensures that the
serialization code generated is supports evolvability between versions.</p>

</div>

<h3 id="_caches">Caches</h3>
<div class="section">
<p>The <code>customers</code> cache will be used to store customer data. The cache key will be the <code>String</code> customer <code>id</code>.</p>

<p>The <code>notifications</code> cache will be used to store notification data. A <code>NotificationId</code> class will be used for the key of the cache. The <code>NotificationId</code> will hold the notification&#8217;s corresponding customer id, region and a unique UUID identifier for the notifications.</p>

<p>The caches in this example do not require and special functionality, so the default cache configuration file will support everything required.</p>

</div>
</div>

<h2 id="_coherence_key_association">Coherence Key Association</h2>
<div class="section">
<p>In this use case key association will be used to co-locate a <code>Customer</code> and all the <code>Notification</code> entries for that customer in the same Coherence partition. This will allow notifications to be added and queried for as specific customer as an atomic operation.</p>

<p>To use key association, the key classes fo the caches to be associated must either be the same or implement the Coherence
<code>com.tangosol.net.cache.KeyAssociation</code> interface. If notifications for a customer were going to be held in a map or list in a single cache entry, we could just use the same String customer identifier as the key and the customer and the notification map would automatically be assigned to the same partition, as they would have the same key value. In this case though, there will be many notification entries for a single customer so the notifications cache requires a custom key class that implements <code>KeyAssociation</code>.</p>

<p>The <code>NotificationId</code> class is shown below:</p>

<markup
lang="java"
title="NotificationId.java"
>@PortableType(id = 1011, version = 1)
public class NotificationId
        implements KeyAssociation&lt;String&gt;, Comparable&lt;NotificationId&gt; {

    /**
     * The customer the notification is for.
     */
    private String customerId;

    /**
     * The region the notification applies to.
     */
    private String region;

    /**
     * The notification unique identifier.
     */
    private UUID id;

    /**
     * Create a notification identifier.
     *
     * @param customerId  the customer the notification is for
     * @param region      the region the notification applies to
     * @param id          the notification identifier
     */
    public NotificationId(String customerId, String region, UUID id) {
        this.customerId = customerId;
        this.region = region;
        this.id = id;
    }

    /**
     * Returns the identifier of the customer the notification is for.
     *
     * @return the identifier of the customer the notification is for
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Returns the region the notification applies to.
     *
     * @return the region the notification applies to
     */
    public String getRegion() {
        return region;
    }

    /**
     * Returns the notification identifier.
     *
     * @return the notification identifier
     */
    public UUID getId() {
        return id;
    }

    @Override
    public String getAssociatedKey() {
        return customerId;
    }

    @Override
    public int compareTo(NotificationId o) {
        int n = SafeComparator.compareSafe(Comparator.naturalOrder(), customerId, o.customerId);
        if (n == 0) {
            n = Long.compare(id.getTimestamp(), o.id.getTimestamp());
            if (n == 0) {
                n = Long.compare(id.getCount(), o.id.getCount());
            }
        }
        return n;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotificationId that = (NotificationId) o;
        return Objects.equals(customerId, that.customerId) &amp;&amp; Objects.equals(region, that.region)
               &amp;&amp; Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, region, id);
    }
}</markup>

<p>Like the <code>Customer</code> and <code>Notification</code> classes, the <code>NotificationId</code> class is annotated with <code>@PortableType</code> to automatically generate the <code>PortableObject</code> serialization code.</p>

<p>All classes that will be used as cache keys in Coherence must properly implement the <code>equals</code> and <code>hashCode</code> methods and include all fields in those methods.</p>

<p>The important method for this example is the <code>getAssociatedKey()</code> method from the <code>KeyAssociation</code> interface.
This method should return the value that this key is to be associated with. In this case notifications are associated to customers, so the customer identifier is returned. This will then guarantee that a customer and its notifications are all located in the same partition in Coherence.</p>

<markup
lang="java"
title="NotificationId.java"
>    @Override
    public String getAssociatedKey() {
        return customerId;
    }</markup>

</div>

<h2 id="_the_customer_repository">The Customer Repository</h2>
<div class="section">
<p>This example is going to use the "repository" functionality in Coherence. A repository is a simple class that provides CRUD operations for an entity. In this case the repository will be for the <code>Customer</code> entity, because that is the root entry point for all operations, including those on notifications. Making all updates and queries access caches via the customer in this way, ensures that updates to notifications are treated as a single atomic operation.</p>

<p>The example does not require the use of a repository class, but it is a nice way to group all the customer related operations together in a single class.</p>

<p>The minimum amount of code to implement a repository is shown below.
The <code>CustomerRepository</code> class extends the <code>com.oracle.coherence.repository.AbstractRepository</code> base class and implements the required abstract methods.</p>

<markup
lang="java"
title="CustomerRepository.java"
>public class CustomerRepository
        extends AbstractRepository&lt;String, Customer&gt; {

    /**
     * The customer's cache.
     */
    private final NamedMap&lt;String, Customer&gt; customers;

    public CustomerRepository(NamedCache&lt;String, Customer&gt; customers)
        {
        this.customers = customers;
        }

    @Override
    protected String getId(Customer entity) {
        return entity.getId();
    }

    @Override
    protected Class&lt;? extends Customer&gt; getEntityType() {
        return Customer.class;
    }

    @Override
    protected NamedMap&lt;String, Customer&gt; getMap() {
        return customers;
    }
    }</markup>

<p>In the rest of the example the <code>CustomerRepository</code> will be enhanced to add additional functionality for notifications.</p>

</div>

<h2 id="_adding_notifications">Adding Notifications</h2>
<div class="section">
<p>The obvious starting point would be to enhance the repository to be able to add notifications for a customer. Read operations will come later, as they&#8217;d be a bit pointless without first having add operations.</p>

<p>The use case here is to allow multiple notifications to be added to a customer is a single atomic operations. Notifications are specific to a region, so the obvious structure to hold the notifications to be added would be a map of the form <code>Map&lt;String, List&lt;Notification&gt;&gt;</code> where the key is the region and the value is a list of notifications for that region.</p>


<h3 id="_the_addnotifications_entry_processor">The AddNotifications Entry Processor</h3>
<div class="section">
<p>To perform the add operation, a custom Coherence entry processor can be written.
This entry processor will take the map of notifications and apply it to the customer.
As key association is being used, the entry processor will be executed against the customer identifier in the customer cache and apply all the notifications in a single atomic partition level transaction.
For the duration of the operation on the server the customer will effectively be locked, guaranteeing that only a single concurrent mutation operation can happen to a customer.</p>

<p>The boilerplate code for the <code>AddNotifications</code> entry processor is shown below.
As with other classes, the entry processor is annotated with <code>@PortableType</code> to generate <code>PortableObject</code> code.</p>

<p>The result returned from this entry processor&#8217;s <code>process</code> method is <code>Void</code> as there is no information that the caller requires as a result.</p>

<markup
lang="java"
title="AddNotifications.java"
>@PortableType(id = 1100, version = 1)
public class AddNotifications
        implements InvocableMap.EntryProcessor&lt;String, Customer, Void&gt; {

    /**
     * The notifications to add to the customer.
     */
    private Map&lt;String, List&lt;Notification&gt;&gt; notifications;

    /**
     * Create a {@link AddNotifications} processor.
     *
     * @param notifications  the notifications to add to the customer
     */
    public AddNotifications(Map&lt;String, List&lt;Notification&gt;&gt; notifications) {
        this.notifications = notifications;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry&lt;String, Customer&gt; entry)
        {
        return null;
        }</markup>

<p>A new <code>addNotifications</code> method can be added to the repository, which will invoke the <code>AddNotifications</code> entry processor against a specific customer identifier. The <code>addNotifications</code> first ensures the repository is initialized and then invokes the entry processor. using the map of notifications.
The method will throw a <code>NullPointerException</code> if the customer identifier is <code>null</code>.</p>

<markup
lang="java"
title="CustomerRepository.java"
>    public void addNotifications(String customerId, Map&lt;String, List&lt;Notification&gt;&gt; notifications) {
        ensureInitialized();
        customers.invoke(Objects.requireNonNull(customerId),
                new AddNotifications(Objects.requireNonNull(notifications)));
    }</markup>


<h4 id="_implement_the_process_method">Implement the Process Method</h4>
<div class="section">
<p>Although the <code>CustomerRepository.addNotifications</code> method could be called and would execute,
the <code>AddNotifications.process</code> method is empty, so no notifications will actually be added.
The next step is to implement the <code>process</code> method to add the notifications to the notifications cache.</p>

<p>At this point it is worth going over what the process method must do for each entry in the notification map.</p>

<ul class="ulist">
<li>
<p>Check the ttl of the entry, if it has already passed then ignore the notification as there i sno point adding it to be immediately expired</p>

</li>
<li>
<p>Create a <code>NotificationId</code> for the key of the new notification cache entry.</p>

</li>
<li>
<p>Use the key to obtain the cache entry to insert</p>

</li>
<li>
<p>Set the notification as the value for the cache entry</p>

</li>
<li>
<p>Set the expiry value for the new entry based on the <code>ttl</code> value of the notification.</p>

</li>
</ul>
<p><strong>Iterate Over the Notifications</strong></p>

<p>The process method can simply iterate over the map of notifications like this:</p>

<markup
lang="java"

>public Void process(InvocableMap.Entry&lt;String, Customer&gt; entry)
    {
    notifications.forEach((region, notificationsForRegion) -&gt;
        {
        notificationsForRegion.forEach(notification -&gt;
            {
            // process notification...
            });
        });
    }</markup>

<p><strong>Work out the Expiry Delay</strong></p>

<p>A Coherence cache entry expects the expiry for an entry to be the number of milliseconds after the entry is inserted or updated before it expires. The <code>ttl</code> value in the <code>Notification</code> class is a Java <code>LocalDateTime</code> so the expiry is the difference between now and the <code>ttl</code> in milliseconds. In Java that can be written as shown below:</p>

<markup
lang="java"

>long ttlInMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), notification.getTTL());</markup>

<p>If the <code>ttlInMillis</code> is greater than zero the notification can be added. If it is less than or equal to zero, then there is no point adding the notification as the <code>ttl</code> is already in the past.</p>

<markup
lang="java"

>public Void process(InvocableMap.Entry&lt;String, Customer&gt; entry)
    {
    notifications.forEach((region, notificationsForRegion) -&gt;
        {
        notificationsForRegion.forEach(notification -&gt;
            {
            long ttlInMillis = ChronoUnit.MILLIS.between(LocalDateTime.now(), notification.getTTL());
            if (ttlInMillis &gt; 0)
                {
                // add the notification...
                }
            });
        });
    }</markup>

<p><strong>Create a NotificationId</strong></p>

<p>Creating the <code>NotificationId</code> is simple.
The customer identifier can be taken from the key of the entry passed to the process method <code>String customerId = entry.getKey();</code>,
the region comes from the notifications map and the UUID is just a new UUID created at runtime.</p>

<markup
lang="java"

>String customerId = entry.getKey();
NotificationId id = new NotificationId(customerId, region, new UUID());</markup>

<p><strong>Obtain the Notification Cache Entry</strong></p>

<p>When using Coherence partition level transactions to atomically update other cache entries in an entry processor, those additional entries must be properly obtained from the relevant cache&#8217;s <code>BackingMapContext</code>. Coherence will then ensure that all mutations are properly handled, backup messages sent, events fired, etc. Each additional entry enlisted in this sort of lite partition transactions, will be locked until the entry processor completes processing.</p>

<p>This can cause issues if two entry processors run that try to enlist the same set of entries but in different orders. Each processor may be holding locks on a sub-set of the entries, and then each is unable to obtain locks on the remaining entries it requires. The safest way around this is to sort the keys that will be enlisted so both processors always enlist entries in the same order. In this example, notifications are only ever inserted, so there is no chance of two processors enlisting the same entries.</p>

<p>The entry processor is executing on an entry from the customers cache, so to obtain the <code>BackingMapContext</code> for the notifications cache can be obtained via the customer entry.</p>

<markup
lang="java"

>BackingMapManagerContext context = entry.asBinaryEntry().getContext();
BackingMapContext ctxNotifications = context.getBackingMapContext("notifications");</markup>

<p>To obtain the entry to insert from the <code>BackingMapContext</code> the <code>BackingMapContext.getBackingMapEntry()</code> method is used. This method takes the key of the entry to obtain, but this key must be in serialized <code>Binary</code> format, not a plain <code>NotificationId</code>. The <code>BackingMapManagerContext</code> conveniently has a converter that can do the serialization.</p>

<markup
lang="java"

>String customerId = entry.getKey();
NotificationId id = new NotificationId(customerId, region, new UUID());
BackingMapManagerContext context = entry.asBinaryEntry().getContext();
BackingMapContext ctxNotifications = context.getBackingMapContext("notifications");
Converter&lt;NotificationId, Binary&gt; converter = context.getKeyToInternalConverter();
Binary binaryKey = converter.convert(id);

BinaryEntry&lt;NotificationId, Notification&gt; binaryEntry =
        (BinaryEntry&lt;NotificationId, Notification&gt;) ctxNotifications.getBackingMapEntry(binaryKey);</markup>

<p>The notification is then set as the entry value using the <code>setValue()</code> method and the expiry set using the <code>expire()</code> method.</p>

<markup
lang="java"

>binaryEntry.setValue(notification);
binaryEntry.expire(ttlInMillis);</markup>

<p>This can all be put together in the final process method:</p>

<markup
lang="java"
title="AddNotifications.java"
>    @Override
    @SuppressWarnings("unchecked")
    public Void process(InvocableMap.Entry&lt;String, Customer&gt; entry)
    {
        BackingMapManagerContext          context          = entry.asBinaryEntry().getContext();
        Converter&lt;NotificationId, Binary&gt; converter        = context.getKeyToInternalConverter();
        BackingMapContext                 ctxNotifications = context.getBackingMapContext(
                CustomerRepository.NOTIFICATIONS_MAP_NAME);
        String                            customerId       = entry.getKey();
        LocalDateTime                     now              = LocalDateTime.now();

        notifications.forEach((region, notificationsForRegion)-&gt;
        {
            notificationsForRegion.forEach(notification-&gt;
            {
                long ttlInMillis = ChronoUnit.MILLIS.between(now, notification.getTTL());
                if (ttlInMillis &gt; 0) {
                    NotificationId id        = new NotificationId(customerId, region, new UUID());
                    Binary         binaryKey = converter.convert(id);
                    BinaryEntry&lt;NotificationId, Notification&gt; binaryEntry =
                            (BinaryEntry&lt;NotificationId, Notification&gt;) ctxNotifications.getBackingMapEntry(binaryKey);

                    binaryEntry.setValue(notification);
                    binaryEntry.expire(ttlInMillis);
                }
            });
        });

        return null;
    }</markup>

</div>
</div>

<h3 id="_adding_notifications_via_the_customerrepository">Adding Notifications via the CustomerRepository</h3>
<div class="section">
<p>The <code>CustomerRepository</code> can then be used to add customers and notifications, this can be seen in the functional tests that are part of this example.</p>

<markup
lang="java"

>        CustomerRepository repository = new CustomerRepository();

        Customer customer = new Customer("QA22", "Julian", "Alaphilippe");
        repository.save(customer);

        Notification notification = new Notification("Ride TdF", LocalDateTime.now().plusDays(1));
        repository.addNotifications(customer, "FRA", notification);</markup>

</div>
</div>

<h2 id="_getting_notifications">Getting Notifications</h2>
<div class="section">
<p>Now that notifications can be added for a customer, the read functions can be added to get notifications for a customer. There are two use cases to implement, first get all notifications for a customer, second get notification for a customer and specific region.</p>

<p>As notifications are in their own cache, the notifications for a customer and customer/region could be obtained by simply running a filter query on the notifications cache. This example is all about uses of key association though, so the method used here will be slightly more complex, but it will show how key association can be used for reading entries as well as updating entries.</p>

<p>Reading notifications could be implemented using an entry processor, which is invoked against the customer cache, that then returns the required notifications, either all for the customer or for a specific region. An entry processor is typically used for mutations and will cause an entry (or entries) to be locked for the duration of its execution. For read operations an aggregator is more efficient as it will not involve locking entries.</p>

<p>To recap the use case, the aggregator needs to return either all the notifications for a customer, or just the notifications for a region. At this point a custom aggregator could be written, but sometimes writing aggregators can be complex and Coherence already has an aggregator that does most of what is required.</p>

<p><strong>The ReducerAggregator</strong></p>

<p>Coherence contains a built-in aggregator named <code>com.tangosol.util.aggregator.ReducerAggregator</code>.
This aggregator takes a <code>ValueExtractor</code> and executes it against each entry and returns the results.
The results returned will be a map of with the keys of the entries the aggregator ran over and the extracted values.
By using the <code>ReducerAggregator</code> aggregator in this use case all that is required is a custom <code>ValueExtractor</code>.
In this example the aggregator will only be run against a single entry (the customer) and the custom <code>ValueExtractor</code> will "extract" the required notifications.</p>


<h3 id="_the_notificationextractor">The NotificationExtractor</h3>
<div class="section">
<p>The purpose of the custom <code>ValueExtractor</code> will be to obtain the notifications for a customer.
The notifications are all co-located in a single partition, so when the extractor is run against an entry in the customer cache, all the notifications are also stored locally.
This particular <code>ValueExtract</code> is going to need access to the entry the aggregator is executing on, so it needs to extend the Coherence <code>com.tangosol.util.extractor.AbstractExtractor</code> class.
The <code>AbstractExtractor</code> is treated as a special case by Coherence when it is extracting data from a cache entry, where Coherence will call its <code>extractFromEntry</code> method.</p>

<p>The boilerplate code for a custom extractor is shown below.
All <code>ValueExtractor</code> implementations should have a correct <code>equals()</code> and <code>hashCode()</code> methods.
The <code>extractFromEntry</code> method returns <code>null</code>, and will be completed in the next section.</p>

<markup
lang="java"
title="NotificationExtractor.java"
>@PortableType(id = 1200, version = 1)
public class NotificationExtractor
        extends AbstractExtractor&lt;Customer, List&lt;Notification&gt;&gt; {

    /**
     * An optional region identifier to use to retrieve
     * only notifications for a specific region.
     */
    private String region;

    /**
     * Create a {@link NotificationExtractor} that will specifically
     * target the key when used to extract from a cache entry.
     *
     * @param region an optional region identifier
     */
    public NotificationExtractor(String region) {
        this.region = region;
    }

    @Override
    public List&lt;Notification&gt; extractFromEntry(Map.Entry entry)
        {
        return null;
        }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        NotificationExtractor that = (NotificationExtractor) o;
        return Objects.equals(region, that.region);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), region);
    }</markup>


<h4 id="_find_the_customers_notifications">Find the Customer&#8217;s Notifications</h4>
<div class="section">
<p>When the extractor&#8217;s <code>extractFromEntry</code> method executes, in this case the entry passed in by the aggregator will be an instance of <code>BinaryEntry</code>, so just like in the entry processor above, the <code>BackingMapContext</code> for the notifications cache can be obtained and from there access to the notification entries.</p>

<p>Coherence does not currently have an API on a <code>BackingMapContext</code> that allows the data to be queried.
For example, in this case some sort of filter query over all the entries in the partition with a specific customer id would get the notification required. This can be worked around by using cache indexes. The indexes on a cache are accessible via the <code>BackingMapContext</code> and from the index contents the required cache entries can be obtained.</p>

<p>Take the first requirement, all notifications for a customer. By creating an index of customer id on the notifications cache, the keys of the entries for a given customer can be obtained from the index and the corresponding notifications returned from the extractor.</p>

<p><strong>Customer Id Index</strong></p>

<p>Indexes are created on a cache using a <code>ValueExtractor</code> to extract the values to be indexed.
In the case of the customer id for a notification, this is a field in the <code>NotificationId</code>, which is the key to the notifications cache. An extractor to extract customer id can be created as shown below:</p>

<markup
lang="java"

>ValueExtractor&lt;?, ?&gt; extractor = ValueExtractor.of(NotificationId::getCustomerId).fromKey();</markup>

<p>This extractor can be used as an index by calling the <code>addIndex</code> method on <code>NamedCache</code> or <code>NamedMap</code>.</p>

<markup
lang="java"

>ValueExtractor&lt;?, ?&gt; extractor = ValueExtractor.of(NotificationId::getCustomerId).fromKey();
notifications.addIndex(extractor);</markup>

<p><strong>The Region Index</strong></p>

<p>The second index required is to be able to find notifications for a customer and region.
In theory this index is not required, the index to find all notifications for a customer could be used, then those notifications filtered to only return those for the required region.
If there will only be a small number of notifications per customer, that may be a suitable approach.
This is one of the typical pros and cons that needs to be weighed up when using indexes. Does the cost in memory usage of the index and time to maintain the index on every mutation outweigh the benefits in speed gained by queries.</p>

<p>This example is going to add an index on region, because it is an example there are no concerns over performance, and it will show how to perform an indexed query.</p>

<p>The extractor to extract region from the <code>NotificationId</code> cache entry key is shown below:</p>

<markup
lang="java"

>ValueExtractor&lt;?, ?&gt; extractor = ValueExtractor.of(NotificationId::getRegion).fromKey();</markup>

<p>This can be used to create an index:</p>

<markup
lang="java"

>ValueExtractor&lt;?, ?&gt; extractor = ValueExtractor.of(NotificationId::getRegion).fromKey();
notifications.addIndex(extractor);</markup>

<p><strong>Creating the Indexes</strong></p>

<p>The repository class already has a method that is called to create any required indexes when it is initialized.
This method can be overridden and used to ensure the notifications indexes are added.</p>

<markup
lang="java"
title="CustomerRepository.java"
>    @Override
    @SuppressWarnings( {"unchecked", "resource"})
    protected void createIndices() {
        super.createIndices();
        CacheService                             service       = customers.getService();
        NamedCache&lt;NotificationId, Notification&gt; notifications = service.ensureCache(NOTIFICATIONS_MAP_NAME,
                service.getContextClassLoader());
        notifications.addIndex(ValueExtractor.of(NotificationId::getCustomerId).fromKey());
        notifications.addIndex(ValueExtractor.of(NotificationId::getRegion).fromKey());
    }</markup>

<p>Note, that the super class <code>createIndicies()</code> method must be called to ensure any other indicies required by the customer repository are created.</p>

</div>

<h4 id="_write_the_notificationextractor_extractfromentry_method">Write the NotificationExtractor extractFromEntry method</h4>
<div class="section">
<p>Now that the required indexes will be present the <code>NotificationExtractor.extractFromEntry()</code> method can be written.
The techniques used below rely on the indexes being present and would not work if there were no indexes.
Without indexes other less efficient methods would be required.
The steps the extract method must perform are shown below:</p>

<ul class="ulist">
<li>
<p>Obtain the map of indexes for the notifications cache</p>

</li>
<li>
<p>From the index map, obtain the customer id index</p>

</li>
<li>
<p>From the customer id index obtain the set of notification keys matching the customer id</p>

</li>
<li>
<p>If the region is specified, reduce the set of keys to only those matching the required region</p>

</li>
<li>
<p>For each remaining key, obtain the read-only cache entry containing the notification and add it to the results list</p>

</li>
<li>
<p>return the list of notifications found</p>

</li>
</ul>
<p>Each step is covered in detail below:</p>

<p><strong>Obtain the map of indexes for the notifications cache</strong></p>

<p>The entry passed to the <code>NotificationExtractor.extractFromEntry</code> method when used in an aggregator will be an instance of a <code>BinaryEntry</code> so the entry can safely be cast to <code>BinaryEntry</code>.
From a <code>BinaryEntry</code> it is possible to obtain the <code>BackingMapManagerContext</code> and from there the <code>BackingMapContext</code> of other caches.
Remember, in this example the aggregator is executed on an entry in the customers cache, so the extractor needs to obtain the <code>BackingMapContext</code> of the notifications cache. From the notifications cache <code>BackingMapContext</code> the map of indexes can be obtained.</p>

<markup
lang="java"

>BinaryEntry binaryEntry = (BinaryEntry) entry;
BackingMapContext ctx = binaryEntry.getContext().getBackingMapContext("notifications");
Map&lt;ValueExtractor, MapIndex&gt; indexMap = ctx.getIndexMap();</markup>

<p><strong>From the index map, obtain the customer id index</strong></p>

<p>The index map is a map of <code>MapIndex</code> instances keyed by the <code>ValueExtractor</code> used to create the index.
To obtain the customer id index just call the <code>get()</code> method using the same customer id extractor used to create the index above.
This is one of the main reasons that all <code>ValueExtractor</code> implementations must properly implement <code>equals()</code> and <code>hashCode()</code> so that they can be used in indexes.</p>

<markup
lang="java"

>BinaryEntry binaryEntry = (BinaryEntry) entry;
BackingMapContext ctx = binaryEntry.getContext().getBackingMapContext("notifications");
Map&lt;ValueExtractor, MapIndex&gt; indexMap = ctx.getIndexMap();

MapIndex&lt;Binary, Notification, String&gt; index = indexMap
        .get(ValueExtractor.of(NotificationId::getCustomerId).fromKey());</markup>

<p><strong>From the customer id index obtain the set of notification keys matching the customer id</strong></p>

<p>A Coherence <code>MapIndex</code> typically holds two internal indexes.
The keys in the index are in serialized binary format, that is, they can be used directly to obtain corresponding entries.</p>

<ul class="ulist">
<li>
<p>A map of cache key to the extracted index value for that key</p>

</li>
<li>
<p>A map of extracted index value to the set of keys that match that value</p>

</li>
</ul>
<p>In the case of the customer id index that means the index holds a map of binary key to corresponding customer id and a map of customer id to keys of entries for that customer id. The second map is the one required for this use case, which can be obtained from the <code>MapIndex.getIndexContents()</code> method. The set of keys for the customer can then be obtained with a simple <code>get(customerId)</code> on the index contents map (the customer id is just the key of the entry passed to the <code>extractFromEntry</code> method.</p>

<markup
lang="java"

>BinaryEntry binaryEntry = (BinaryEntry) entry;
BackingMapContext ctx = binaryEntry.getContext().getBackingMapContext("notifications");
Map&lt;ValueExtractor, MapIndex&gt; indexMap = ctx.getIndexMap();

MapIndex&lt;Binary, Notification, String&gt; index = indexMap
        .get(ValueExtractor.of(NotificationId::getCustomerId).fromKey());

String customerId = (String) entry.getKey();
Set&lt;Binary&gt; keys = index.getIndexContents().get(customerId);</markup>

<p>At this point the <code>keys</code> set is the key of all the notification entries for the customer.</p>

<p><strong>Further Filter by Region</strong></p>

<p>If the region has been specified, the set of keys needs to be further filtered to just those for the required region.
This could be achieved a number of ways, but this example is going to show how Coherence filters and indexes can be used to reduce a set of keys. Almost all filters in Coherence implement <code>IndexAwareFilter</code> which means they have an <code>applyIndex</code> method:</p>

<markup
lang="java"

>public &lt;RK&gt; Filter&lt;V&gt; applyIndex(
    Map&lt;? extends ValueExtractor&lt;? extends V, Object&gt;, ? extends MapIndex&lt;? extends RK, ? extends V, Object&gt;&gt; mapIndexes,
    Set&lt;? extends RK&gt; setKeys);</markup>

<p>When the <code>applyIndex</code> method is called, the <code>Set</code> of keys passed in will be reduced to only those keys matching the filter.
This means that an <code>EqualsFilter</code> using the region extractor can be used to reduce the set of all keys for the customer down to just those keys matching the region too.</p>

<p>Again, the extractor used in the <code>EqualsFilter</code> must be the same extractor used to create the region index.</p>

<markup
lang="java"

>if (region != null &amp;&amp; !region.isBlank())
    {
    ValueExtractor&lt;NotificationId, String&gt; extractor = ValueExtractor.of(NotificationId::getRegion).fromKey();
    EqualsFilter&lt;NotificationId, String&gt; filter = new EqualsFilter&lt;&gt;(extractor, region);
    filter.applyIndex(indexMap, keys);
    }</markup>

<p>Now the <code>keys</code> set has been reduced to only key matching both customer id and region.</p>

<p><strong>Obtain the Notifications</strong></p>

<p>The set of keys can be used to obtain notification from the notifications cache.
The safest way to do this is to use the <code>BackingMapContext.getReadOnlyEntry()</code> method.
The final list of notifications will be ordered by creation data. This is possible because the <code>NotificationId</code> class used in this example implements <code>Comparable</code> and makes use of the fact that the Coherence <code>UUID</code> used as a unique id in the notification contains a timestamp.</p>

<p>The example used Java streams to process the keys into a list of notifications, the code is shown below:</p>

<markup
lang="java"

>        Comparator&lt;InvocableMap.Entry&gt; comparator = (e1, e2)-&gt;
                SafeComparator.compareSafe(Comparator.naturalOrder(), e1.getKey(), e2.getKey());

        return keys.stream()
                   .map(ctx::getReadOnlyEntry)             <span class="conum" data-value="1" />
                   .filter(InvocableMap.Entry::isPresent)  <span class="conum" data-value="2" />
                   .sorted(comparator)                     <span class="conum" data-value="3" />
                   .map(InvocableMap.Entry::getValue)      <span class="conum" data-value="4" />
                   .map(Notification.class::cast)          <span class="conum" data-value="5" />
                   .collect(Collectors.toList());          <span class="conum" data-value="6" /></markup>

<ul class="colist">
<li data-value="1">The key is mapped to a read-only <code>InvocableMap.Entry</code></li>
<li data-value="2">Only process entries that are present for the key (in case it has just been removed)</li>
<li data-value="3">Sort the entries using the comparator to sort by key (i.e. <code>NotificationId</code>)</li>
<li data-value="4">Map the entry to just the value (i.e. the <code>Notification</code>)</li>
<li data-value="5">Cast the value to a <code>Notification</code> (this is because Java does not know the <code>InvocableMap.Entry</code> generic types)</li>
<li data-value="6">Collect the final <code>Notification</code> instances into a list</li>
</ul>
<p><strong>The Final Method</strong></p>

<p>All the code above can be combined into the final <code>extractFromEntry()</code> method.</p>

<markup
lang="java"

>    @Override
    @SuppressWarnings( {"rawtypes", "unchecked"})
    public List&lt;Notification&gt; extractFromEntry(Map.Entry entry) {
        BinaryEntry binaryEntry = (BinaryEntry) entry;
        BackingMapContext ctx = binaryEntry.getContext()
                                           .getBackingMapContext(CustomerRepository.NOTIFICATIONS_MAP_NAME);
        Map&lt;ValueExtractor, MapIndex&gt; indexMap = ctx.getIndexMap(binaryEntry.getKeyPartition());

        MapIndex&lt;Binary, Notification, String&gt; index = indexMap
                .get(ValueExtractor.of(NotificationId::getCustomerId).fromKey());

        String      customerId = (String) entry.getKey();
        Set&lt;Binary&gt; keys       = index.getIndexContents().get(customerId);

        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }

        if (region != null &amp;&amp; !region.isBlank()) {
            // copy the keys, so we don't modify the underlying index
            keys = new HashSet&lt;&gt;(keys);

            ValueExtractor&lt;NotificationId, String&gt; extractor = ValueExtractor.of(NotificationId::getRegion).fromKey();
            EqualsFilter&lt;NotificationId, String&gt;   filter    = new EqualsFilter&lt;&gt;(extractor, region);
            filter.applyIndex(indexMap, keys);
        }

        Comparator&lt;InvocableMap.Entry&gt; comparator = (e1, e2)-&gt;
                SafeComparator.compareSafe(Comparator.naturalOrder(), e1.getKey(), e2.getKey());

        return keys.stream()
                   .map(ctx::getReadOnlyEntry)             <span class="conum" data-value="1" />
                   .filter(InvocableMap.Entry::isPresent)  <span class="conum" data-value="2" />
                   .sorted(comparator)                     <span class="conum" data-value="3" />
                   .map(InvocableMap.Entry::getValue)      <span class="conum" data-value="4" />
                   .map(Notification.class::cast)          <span class="conum" data-value="5" />
                   .collect(Collectors.toList());          <span class="conum" data-value="6" />
    }</markup>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Looking at the source code, or JavaDoc, for <code>BackingMapContext</code> will show the <code>getBackingMap()</code> method, which returns the actual map of <code>Binary</code> keys and values in the cache; it should also be obvious that this method is deprecated. It may seem like this is a good way to access the data in the cache for the use case above, but directly accessing the data this way can break the guarantees and locks provided by Coherence. Ideally this method would have been removed, but backwards compatibility constraints mean it is still there, but it should not be used.</p>
</p>
</div>
</div>
</div>

<h3 id="_add_get_notification_methods_to_the_customerrepository">Add Get Notification Methods to the CustomerRepository</h3>
<div class="section">
<p>Now the <code>NotificationExtractor</code> is complete, methods can now be added to the <code>CustomerRepository</code> to get notifications for a customer and optionally a region.</p>

<markup
lang="java"
title="CustomerRepository.java"
>    /**
     * Returns the notifications for a customer.
     *
     * @param customerId  the identifier of the customer to obtain the notifications for
     *
     * @return the notifications for the customer
     */
    public List&lt;Notification&gt; getNotifications(String customerId) {
        return getNotifications(customerId, null);
    }

    /**
     * Returns the notifications for a customer, and optionally a region.
     *
     * @param customerId  the identifier of the customer to obtain the notifications for
     * @param region      an optional region to get notifications for
     *
     * @return the notifications for the customer, optionally restricted to a region
     */
    public List&lt;Notification&gt; getNotifications(String customerId, String region) {
        Map&lt;String, List&lt;Notification&gt;&gt; map = getAll(List.of(customerId), new NotificationExtractor(region));
        return map.getOrDefault(customerId, Collections.emptyList());
    }</markup>

<p>The <code>getNotifications()</code> method calls the <code>getAll()</code> method on the <code>AbstractRepository</code> super class, which takes a collection of keys and a <code>ValueExtractor</code>. Under the covers, the <code>AbstractRepository.getAll()</code> method just runs a <code>ReducerAggregator</code> with the provided <code>ValueExtractor</code> after ensuring the repository is properly initialized. The map of results returned by <code>getAll()</code> will only ever contain a single entry, as it is only ever called here with a singleton list of keys. The result map will be a map of customer id to a list of notifications.</p>

</div>
</div>

<h2 id="_a_poor_mans_join">A Poor Man&#8217;s Join</h2>
<div class="section">
<p>A question often asked about Coherence is whether it can support joins in queries like a database, the answer is that it does not.
Efficiently performing distributed joins in queries is extremely difficult to do, and typically data ends up being pulled back to a single member where it is joined. Using key association to guarantee associated data is in a single partition can be used to implement join type aggregations across those related entities. These techniques have been used by customers to implement quite complex join and data enrichment queries in large Coherence applications.</p>

</div>

<h2 id="_summary">Summary</h2>
<div class="section">
<p>The examples above show just some uses of key association in Coherence.
It can be quite a powerful concept if used wisely. There are some downsides, mainly in cases where the amount of associated data is not very even. For example, in the use case above, if some customers has a very large number of notifications, all those would be stored in single partition. This can lead to some partitions and hence some cluster members using a larger amount of memory than others. Generally in a Coherence cache, keys are reasonably evenly distributed over partitions and cache entry sizes are relatively consistent so uneven memory usage is not an issue, but when using key association it is something to be aware of.</p>

</div>
</doc-view>
