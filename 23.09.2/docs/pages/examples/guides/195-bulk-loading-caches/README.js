<doc-view>

<h2 id="_bulk_loading_caches">Bulk Loading Caches</h2>
<div class="section">
<p>A common use case in Coherence is for caches to hold data that comes from other sources, typically a database. Often there is a requirement to preload data into caches when an application starts up. Using a <code>CacheLoader</code> to load data on demand may be suitable for many caching use cases, but other use cases such as querying and aggregating caches require all the data to be present in the cache. Over the years, there have been a number of patters to achieve preloading, this guide will cover some currently recommended approaches to preloading data.</p>

<p>Whilst this guide reads data from a database and pushes it into caches, the same patterns can apply to any data source, for example, preloading from files, messaging systems, data streaming systems, etc.</p>

</div>

<h2 id="_what_you_will_build">What You Will Build</h2>
<div class="section">
<p>The example in this guide builds a simple application that uses different techniques to preload caches from a database. This includes examples of preloading from a database into a cache that uses a cachestore to write cache data to the same database.</p>


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
</div>

<h3 id="_building_the_example_code">Building the Example Code</h3>
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

<h2 id="_typical_preloading_use_case">Typical Preloading Use Case</h2>
<div class="section">
<p>The typical preloading use case is to pull data from a datasource and load it into caches as fast as possible. This means ideally scaling out the preloading to be batched and multi-threaded, to load multiple caches at once. The pre-loaders in this example will be written as simple Java <code>Runnable</code> implementations. This allows them to be easily scaled up by running them in a Java <code>Executor</code> or daemon thread pool. A separate Coherence example will show how to use Coherence concurrent Executor Service module to scale out preloading by distributing the preload runnables to execute on Coherence cluster members.</p>


<h3 id="_extend_client_or_cluster_member">Extend Client or Cluster Member?</h3>
<div class="section">
<p>The cache preloader is typically implemented as a process that is run after the storage enabled cluster members have started. This is typically a simple Java class with a main method that runs and controls loading the data. As this application will need to communicate with the Coherence storage members, it can obviously run in two modes, either as a storage disabled cluster member or as an Extend client. As the preload application&#8217;s only job is to push a large amount of data into Coherence caches as fast as possible, it is recommended to run the preloader as a storage disabled cluster member. Running as an Extend client would cause a bottleneck trying to push all the data over a single Extend connection to the proxy, where it is then distributed across the cluster.</p>

</div>

<h3 id="_a_simple_preload_runnable">A Simple Preload Runnable</h3>
<div class="section">
<p>A simple preload task might look like the example below:</p>

<markup
lang="java"

>public class CustomerPreloadTask
        implements Runnable
    {
    private final Connection connection;

    private final Session session;

    public PreloadTask(Connection connection, Session session)
        {
        this.connection = connection;
        this.session = session;
        }

    @Override
    public void run()
        {
        NamedMap&lt;Integer, Customer&gt; namedMap = session.getMap("customers");     <span class="conum" data-value="1" />
        String query = "SELECT id, name, address, creditLimit FROM customers";
        int batchSize = 10;

        try (PreparedStatement statement = connection.prepareStatement(query);  <span class="conum" data-value="2" />
             ResultSet resultSet = statement.executeQuery())
            {
            Map&lt;Integer, Customer&gt; batch = new HashMap&lt;&gt;(batchSize);            <span class="conum" data-value="3" />

            while (resultSet.next())                                            <span class="conum" data-value="4" />
                {
                int key = resultSet.getInt("id");                               <span class="conum" data-value="5" />

                Customer value = new Customer(resultSet.getInt("id"),           <span class="conum" data-value="6" />
                                              resultSet.getString("name"),
                                              resultSet.getString("address"),
                                              resultSet.getInt("creditLimit"));

                batch.put(key, value);                                          <span class="conum" data-value="7" />

                if (batch.size() &gt;= batchSize)                                  <span class="conum" data-value="8" />
                    {
                    namedMap.putAll(batch);
                    batch.clear();
                    }
                }

            if (!batch.isEmpty())                                               <span class="conum" data-value="9" />
                {
                namedMap.putAll(batch);
                batch.clear();
                }
            }
        catch (SQLException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }
    }</markup>

<ul class="colist">
<li data-value="1">Obtain the cache to be loaded from the Coherence <code>Session</code>, in this case the "customers" cache.</li>
<li data-value="2">Perform the query on the database to get the data to load. There are many ways this could be done depending on the database. This is a very simple approach using a <code>PreparedStatement</code>.</li>
<li data-value="3">Create a <code>Map</code> to hold a batch of entries to load. This preload task will call <code>NamedMap.putAll()</code> to load the dat ain batches into the cache. This is more efficient that multiple single put calls.</li>
<li data-value="4">Iterate over all the rows of data returned by the <code>ResultSet</code>.</li>
<li data-value="5">Create the cache key from the current row in the <code>ResultSet</code> (in this case the key is just the "id" int).</li>
<li data-value="6">Create the cache value from the current row in the <code>ResultSet</code>.</li>
<li data-value="7">Add the key and new <code>Customer</code> to the batch map.</li>
<li data-value="8">If the batch map is &gt;= the batch size, put the batch into the cache using <code>putAll()</code>, then clear the batch map.</li>
<li data-value="9">After all the rows in the <code>ResultSet</code> have been iterated over, there may be entries in the batch map that need to be loaded, so put them in the cache,</li>
</ul>
<p>A different implementation of this class can be created to load different caches from different database tables.
Ideally the common code would be extracted into an abstract base class.
This is what the example code does in the <code>AbstractJdbcPreloadTask</code>, which is a base class for loading caches from a database. Concrete implementations are in the example test code in the <code>CustomerJdbcLoader</code> and <code>OrderJdbcLoader</code> classes.</p>

</div>

<h3 id="_running_the_loaders">Running the Loaders</h3>
<div class="section">
<p>If the cache loaders are written as <code>Runnable</code> instances, as shown above, they can easily be run in parallel using a Java <code>Executor</code>.</p>

<p>For example, if there were three preload tasks, written like the example above, to load Customers, Orders, and Products, they could be submitted to an <code>ExecutorService</code> as shown below.</p>

<markup
lang="java"

>Session session = Coherence.getInstance().getSession();

ExecutorService executor = Executors.newCachedThreadPool();

executor.submit(new CustomerPreloadTask(session));
executor.submit(new OrdersPreloadTask(session));
executor.submit(new ProductsPreloadTask(session));</markup>

<p>The loader application can wait for the executor to complete all the tasks, ideally with a timeout so that if there is an issue, it does not run forever.</p>

<markup
lang="java"

>Session session = Coherence.getInstance().getSession();

ExecutorService executor = Executors.newCachedThreadPool();

executor.submit(new CustomerPreloadTask(session));
executor.submit(new OrdersPreloadTask(session));
executor.submit(new ProductsPreloadTask(session));

executor.shutdown();  <span class="conum" data-value="1" />
boolean terminated = executor.awaitTermination(1, TimeUnit.HOURS); <span class="conum" data-value="2" />
if (!terminated)
    {
    executor.shutdownNow(); <span class="conum" data-value="3" />
    }</markup>

<ul class="colist">
<li data-value="1">Stop the executor accepting any more requests.</li>
<li data-value="2">Wait a maximum of one hour for the executor to complete running the tasks.</li>
<li data-value="3">If the executor has not terminated in one hour, forcefully terminate.</li>
</ul>
</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>The basic code to load a simple cache from a database (or other data source) is usually very simple, as shown above.
There are many variations on this pattern to make loading scale better and execute faster.</p>

</div>
</div>

<h2 id="_cachestore_complications">CacheStore Complications</h2>
<div class="section">
<p>As already shown, a basic cache loader can be very simple. Where it gets complicated is when the cache being loaded has a <code>CacheStore</code> configured that writes data to the same data source that the loader is loading from.
It should be obvious what the issue is, the data is read from the database, put into the cache, and the re-written back to the database - this is not desirable.</p>

<p>The solution to this problem is to have a <code>CacheStore</code> implementation that can determine whether an entry should be written to the database or not. There are a few ways to do this, a controllable <code>CacheStore</code> that can be turned on or off, or a <code>CacheStore</code> that can check a flag on a value to determine whether that value should be stored or not. In this example we will cover both of these options.</p>


<h3 id="_controllable_cachestore">Controllable CacheStore</h3>
<div class="section">
<p>As the name suggests, a controllable <code>CacheStore</code> can be turned on or off.
The <code>CacheStore</code> would be turned off before the preload tasks ran, then turned back on after the preload is complete.</p>

<p>There have been a few patterns for controllable cache stores suggested in the past, including an example in the official Coherence documentation where the enable/disable flag is a boolean value stored in another cache.
With a little more thought we can see that this is not really a good idea. Consider what a bulk preload is doing, it is loading a very large amount of data into caches, that will then call the <code>CacheStore</code> methods. If the <code>CacheStore</code> needed to look up a distributed cache value on every store call, that would be massively inefficient.
Even accessing a cache from inside a <code>CacheStore</code> could be problematic due to making a remote call from a cache service worker thread, which may cause a deadlock. Even introducing near caching or a view cache would not necessarily help, as updates to the flag would be async.</p>

<p>Checking the flag that controls the <code>CacheStore</code> needs to be as efficient as possible. For that reason, the example here just uses a simple <code>boolean</code> field in the controller itself. An <code>EntryProcessor</code> is then used to directly set the flag for the controller. How this works will be explained below.</p>

<p>The code in this example has a <a id="" title="" target="_blank" href="https://github.com/oracle/coherence/tree/master/prj/examples/guides/195-bulk-loading-caches/src/main/java/com/oracle/coherence/guides/preload/cachestore/ControllableCacheStore.java"><code>ControllableCacheStore</code> class</a> that implements <code>CacheStore</code> and has a <code>Controller</code> that enables or disables operations. This allows the <code>ControllableCacheStore</code> to be controlled in different ways just by implementing different types of <code>Controller</code>. The <code>ControllableCacheStore</code> also just delegates operations to another <code>CacheStore</code>, it does not do anything itself. The <code>ControllableCacheStore</code> calls the delegate if the controller says it is enabled, otherwise it does nothing. This makes the <code>ControllableCacheStore</code> a simple class that can be reused to make any existing, or new, <code>CacheStore</code> implementation be controllable.</p>

<p>A small section of the <code>ControllableCacheStore</code> class is shown below:</p>

<markup
lang="java"
title="ControllableCacheStore.java"
>public class ControllableCacheStore&lt;K, V&gt;
        implements CacheStore&lt;K, V&gt;
    {
    private final Controller controller;

    private final CacheStore&lt;K, V&gt; delegate;

    public ControllableCacheStore(Controller controller, CacheStore&lt;K, V&gt; delegate)
        {
        this.controller = controller;
        this.delegate = delegate;
        }

    @Override
    public void store(K key, V value)
        {
        if (controller.isEnabled())
            {
            delegate.store(key, value);
            }
        }

    @Override
    public void storeAll(Map&lt;? extends K, ? extends V&gt; mapEntries)
        {
        if (controller.isEnabled())
            {
            delegate.storeAll(mapEntries);
            }
        }

    // other methods omitted ...

    /**
     * Implementations of {@link Controller} can
     * control a {@link ControllableCacheStore}.
     */
    public interface Controller
        {
        boolean isEnabled();
        }
    }</markup>

<p>It should be obvious how the class works. The <code>Controller</code> is an inner interface, and an implementation of this is passed to the constructor, along with the delegate <code>CacheStore</code>.</p>

<p>Each method call (only <code>store</code> and <code>storeAll</code> are shown above) calls the controller&#8217;s <code>isEnabled()</code> method to determine whether the delegate should be called.</p>


<h4 id="_the_controller_implementation">The Controller Implementation</h4>
<div class="section">
<p>In this example, a simple controller is used with a <code>boolean</code> field for enabling or disabling the <code>CacheStore</code>.
The example source code contains the <a id="" title="" target="_blank" href="https://github.com/oracle/coherence/tree/master/prj/examples/guides/195-bulk-loading-caches/src/main/java/com/oracle/coherence/guides/preload/cachestore/SimpleController.java"><code>SimpleController</code> class</a> shown below:</p>

<markup
lang="java"
title="SimpleController.java"
>public class SimpleController
        implements ControllableCacheStore.Controller
    {
    @Override
    public boolean isEnabled()
        {
        return enabled;
        }

    public void setEnabled(boolean enabled)
        {
        this.enabled = enabled;
        }
    }</markup>

</div>

<h4 id="_configuring_and_creating_the_cachestore">Configuring and Creating the CacheStore</h4>
<div class="section">
<p>For a cache to use a cache store, it needs to be configured in the cache configuration file to use a <code>&lt;read-write-backing-map&gt;</code>, which in turn configures the <code>CacheStore</code> implementation to use.
There are a few ways to configure the <code>CacheStore</code>, either using the implementation class name directly, or using a factory class and static factory method.
In this example we will use the second approach, this means determining the cache store to use will be done in a factory class rather than in configuration, but this fits our use case better.</p>

<p>The <code>&lt;distributed-scheme&gt;</code> used in the example test code is shown below:</p>

<markup
lang="xml"
title="controllable-cachestore-cache-config.xml"
>    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;db-storage&lt;/scheme-name&gt;
      &lt;service-name&gt;StorageService&lt;/service-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;read-write-backing-map-scheme&gt;
          &lt;internal-cache-scheme&gt;
            &lt;local-scheme/&gt;
          &lt;/internal-cache-scheme&gt;
          &lt;cachestore-scheme&gt;
            &lt;class-scheme&gt;
              &lt;class-factory-name&gt;
                com.oracle.coherence.guides.preload.cachestore.CacheStoreFactory  <span class="conum" data-value="1" />
              &lt;/class-factory-name&gt;
              &lt;method-name&gt;createControllableCacheStore&lt;/method-name&gt;             <span class="conum" data-value="2" />
              &lt;init-params&gt;
                &lt;init-param&gt;
                  &lt;param-type&gt;java.lang.String&lt;/param-type&gt;                       <span class="conum" data-value="3" />
                  &lt;param-value&gt;{cache-name}&lt;/param-value&gt;
                &lt;/init-param&gt;
                &lt;init-param&gt;
                  &lt;param-type&gt;java.lang.String&lt;/param-type&gt;                       <span class="conum" data-value="4" />
                  &lt;param-value system-property="jdbc.url"&gt;
                    jdbc:hsqldb:mem:test
                  &lt;/param-value&gt;
                &lt;/init-param&gt;
              &lt;/init-params&gt;
            &lt;/class-scheme&gt;
          &lt;/cachestore-scheme&gt;
        &lt;/read-write-backing-map-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The <code>CacheStore</code> factory class is <a id="" title="" target="_blank" href="https://github.com/oracle/coherence/tree/master/prj/examples/guides/195-bulk-loading-caches/src/main/java/com/oracle/coherence/guides/preload/cachestore/CacheStoreFactory.java"><code>com.oracle.coherence.guides.preload.cachestore.CacheStoreFactory</code></a></li>
<li data-value="2">The static factory method on the <code>CacheStoreFactory</code> class that will be called to create a <code>CacheStore</code> is <code>createControllableCacheStore</code></li>
<li data-value="3">The <code>createControllableCacheStore</code> has two parameters, which are configured in the <code>&lt;init-params&gt;</code>, the first is the name of the cache. The Coherence configuration macro <code>{cache-name}</code> will pass the name of the cache being created to the factory method.</li>
<li data-value="4">The second parameter is the JDBC URL of the database to load data from, in the example this defaults to the HSQL in-memory test database.</li>
</ul>
<p>The <code>CacheStoreFactory</code> method <code>createControllableCacheStore</code> used in the example is shown below</p>

<markup
lang="java"
title="filename.java"
>public static CacheStore createControllableCacheStore(String cacheName, String jdbcURL) throws Exception
    {
    CacheStore delegate;
    switch (cacheName.toLowerCase())                        <span class="conum" data-value="1" />
        {
        case "customers":                                   <span class="conum" data-value="2" />
            delegate = new CustomerCacheStore(jdbcURL);
            break;
        case "orders":                                      <span class="conum" data-value="3" />
            delegate = new OrderCacheStore(jdbcURL);
            break;
        default:                                            <span class="conum" data-value="4" />
            throw new IllegalArgumentException("Cannot create cache store for cache " + cacheName);
        }

    return new ControllableCacheStore&lt;&gt;(new SimpleController(), delegate); <span class="conum" data-value="5" />
    }</markup>

<ul class="colist">
<li data-value="1">The code does a simple switch on the cache name to determine the actual <code>CacheStore</code> to create.</li>
<li data-value="2">If the cache name is "customers", then the <code>CustomerCacheStore</code> is created</li>
<li data-value="3">If the cache name is "orders", then the <code>OrderCacheStore</code> is created</li>
<li data-value="4">An exception is thrown for an unknown cache name</li>
<li data-value="5">Finally, a <code>ControllableCacheStore</code> is returned that uses a <code>SimpleController</code> and wraps the delegate <code>CacheStore</code></li>
</ul>
<p>Now, when application code first requests either the "customers" cache or the "orders" cache, the cache Coherence will create the cache and call the <code>CacheStoreFactory.createControllableCacheStore</code> method to create the <code>CacheStore</code>.</p>

</div>

<h4 id="_enabling_and_disabling_the_controllablecachestore">Enabling and Disabling the ControllableCacheStore</h4>
<div class="section">
<p>Now we have caches that are configured to use the <code>ControllableCacheStore</code> with the <code>SimpleController</code>, the next step is to actually be able to enable or disable the controller so that the preload can run.</p>

<p>In the <code>SimpleController</code> the <code>setEnabled()</code> method needs to be called to set the controlling <code>boolean</code> flag.
For each cache configured to use the <code>ControllableCacheStore</code> there will be an instance of <code>SimpleController</code> in every storage enabled cluster member. A method is needed of calling the <code>setEnabled()</code> method on all these instances, and this can be done with an <code>EntryProcessor</code>.</p>

<p>A section of the source for the
<a id="" title="" target="_blank" href="https://github.com/oracle/coherence/tree/master/prj/examples/guides/195-bulk-loading-caches/src/main/java/com/oracle/coherence/guides/preload/cachestore/SimpleController.java"><code>SetEnabledProcessor</code></a>
is shown below (the methods for serialization have been omitted here, but are in the actual GitHub source).</p>

<markup
lang="java"
title="SetEnabledProcessor.java"
>public static class SetEnabledProcessor&lt;K, V&gt;
        implements InvocableMap.EntryProcessor&lt;K, V, Void&gt;,
                PortableObject, ExternalizableLite
    {
    private boolean enabled;

    public SetEnabledProcessor()
        {
        }

    public SetEnabledProcessor(boolean enabled)
        {
        this.enabled = enabled;
        }

    @Override
    public Void process(InvocableMap.Entry&lt;K, V&gt; entry)
        {
        ObservableMap&lt;? extends K, ? extends V&gt; backingMap = entry.asBinaryEntry().getBackingMap();
        if (backingMap instanceof ReadWriteBackingMap)
            {
            ReadWriteBackingMap.StoreWrapper wrapper = ((ReadWriteBackingMap) backingMap).getCacheStore();
            Object o = wrapper.getStore();

            if (o instanceof ControllableCacheStore)
                {
                ControllableCacheStore.Controller controller = ((ControllableCacheStore) o).getController();
                if (controller instanceof SimpleController)
                    {
                    ((SimpleController) controller).setEnabled(enabled);
                    }
                }
            }
        return null;
        }

    // PortableObject and ExternalizableLite method are omitted here...
    }</markup>

<p>In the <code>process</code> method, the backing map is obtained from the <code>entry</code>.
The <code>getBackingMap()</code> is deprecated, mainly as a warning that this is quite a dangerous thing to do if you are not careful. The backing map is the internal structure used by Coherence to hold cache data, and directly manipulating it can have adverse effects. In this case we are not manipulating the backing map, so we are safe.
If the cache is configured as with a <code>&lt;read-write-backing-map&gt;</code> then the implementation of the backing map here will be <code>ReadWriteBackingMap</code>.
We can obtain the <code>CacheStore</code> being used from the <code>ReadWriteBackingMap</code> API, and check whether it is a <code>ControllableCacheStore</code>. If it is, we can get the <code>Controller</code> being used, and if it is a <code>SimpleController</code> set the flag.</p>

<p>Now we have the <code>SetEnabledProcessor</code> we need to execute it so that we guarantee it runs on every storage enabled member. Using something like <code>cache.invokeAll(Filters.always(), new SetEnabledProcessor())</code> will not work, because this will only execute on members where there are entries, and there are none as we are about to do a preload.</p>

<p>One of the things to remember about methods like <code>cache.invokeAll(keySet, new SetEnabledProcessor())</code> is that the <code>keySet</code> can contain keys that do not need to exist in the cache. As long as the entry processor does not call <code>entry.setValue()</code> the entry it executes against will never exist.</p>

<p>Another feature of Coherence is the ability to influence the partition a key belongs to by writing a key class that implements the <code>com.tangosol.net.partition.KeyPartitioningStrategy.PartitionAwareKey</code> interface. Coherence has a built-in implementation of this class called <code>com.tangosol.net.partition.SimplePartitionKey</code>.</p>

<p>We can make use of both these features to create a set of keys where we can guarantee we have one key for each partition in a cache. If we use this as the key set in an <code>invokeAll</code> method, we will guarantee to execute the <code>EntryProcessor</code> in every partition, and hence on every storage enable cluster member.</p>

<p>The snippet of code below shows how to execute the <code>SetEnabledProcessor</code> to disable the cache stores for a cache.
Changing the line <code>new SetEnabledProcessor(false)</code> to <code>new SetEnabledProcessor(true)</code> will instead enable the cache stores.</p>

<markup
lang="java"
title="SimpleController.java"
>public static void disableCacheStores(NamedMap&lt;?, ?&gt; namedMap)
    {
    CacheService service = namedMap.getService();                                    <span class="conum" data-value="1" />
    int partitionCount = ((DistributedCacheService) service).getPartitionCount();    <span class="conum" data-value="2" />
    Set&lt;SimplePartitionKey&gt; keys = new HashSet&lt;&gt;();                                  <span class="conum" data-value="3" />
    for (int i = 0; i &lt; partitionCount; i++)
        {
        keys.add(SimplePartitionKey.getPartitionKey(i));                             <span class="conum" data-value="4" />
        }
    namedMap.invokeAll(keys, new SetEnabledProcessor(false));                         <span class="conum" data-value="5" />
    }</markup>

<ul class="colist">
<li data-value="1">Obtain the cache service for the <code>NamedMap</code> that has the <code>ControllableCacheSTore</code> to enable</li>
<li data-value="2">The cache service should actually be an instance of <code>DistributedCacheService</code>, from which we can get the partition count. The default is 257, but this could have been changed.</li>
<li data-value="3">Create a <code>Set&lt;SimplePartitionKey&gt;</code> that will hold the keys for the <code>invokeAll</code></li>
<li data-value="4">In a for loop, create a <code>SimplePartitionKey</code> for every partition, and add it to the <code>keys</code> set</li>
<li data-value="5">The <code>keys</code> set can be used in the <code>invokeAll</code> call to invoke the <code>SetEnabledProcessor</code> on every partition</li>
</ul>
<p>Running the <code>SetEnabledProcessor</code> on every partition means it actually executes more times than it needs to, but this is not a problem, as repeated executions in the same JVM just set the same flag for the <code>enabled</code> value.</p>

<p>Now we have a way to enable and disable the <code>ControllableCacheStore</code>, we can execute this code before running the preload, and then re-enable the cache stores after running the preload.</p>

</div>

<h4 id="_controllablecachestore_caveats">ControllableCacheStore Caveats</h4>
<div class="section">
<p>A controllable cache store is reasonably simple, but it will really not work in cases where the cache is configured to use write-behind. With write-behind enabled, if the controllable cache store is turned back on too soon after loading (i.e. within the write delay time) then the data loaded to the cache that is still in the write-behind queue will be written to the database.</p>

<p>A controllable cache store is also not going to work in situations where the application could be updating entries in the cache while the preload is still running. If there are a mixture of entries, some needing to be written and some not, the controllable cache store will not be suitable.</p>

<p>Another caveat with the <code>SimpleController</code> above, is what happens during failure of a storage member. If a storage member in the cluster fails, that is not an issue, but in environments such as Kubernetes, where that failed member will be replaced automatically, that can be a problem. The new member will join the cluster, caches will be created on it, including the <code>ControllableCacheStore</code> for configured caches. The problem is that the <code>boolean</code> flag in the new member&#8217;s <code>SimpleController</code> will not be set to <code>false</code>, so the new member will start storing entries it receives to the database. Ideally, new members do not join during preload, but this may be out of the developers control. This could require a more complex controller, for example checking an entry in a cache for its initial state, etc.</p>

</div>
</div>

<h3 id="_smart_cachestore">Smart CacheStore</h3>
<div class="section">
<p>A smart cache store is a solution to the caveats that a controllable cache store has.
The code for a smart cache store and for preloading is slightly more complex, but it can be applied to more use cases.</p>

<p>Basically a smart cache store can use some sort of flag on the cache value to determine whether an entry should be stored in the data store. This could be a <code>boolean</code> field in the cache value itself, but this means corrupting the application data model with Coherence control data. This is not ideal, and in fact some applications may not actually own the class files that are being used in the cache and cannot add fields. A better way that leaves the cache value unchanged is to use a feature in Coherence that allows decorations to be added to the serialized binary values in a cache. Coherence uses decorations itself for a number of reasons (for example marking an entry as being successfully stored). In this case we can add a simple decoration to indicate whether an entry should be stored or not, the actual value of the decoration does not matter, we can just use the presence of the decoration to indicate that the entry should not be written. The preloader would then load the cache with decorated values, which would not be stored by the smart cache store. Any other entries updated by the application would be stored, even if they were updated while the preload was running.</p>

<p>A normal <code>CacheStore</code> does not have access to the binary values in the cache.
To be able to do this, the cache store needs to be an implementation of <code>com.tangosol.net.cache.BinaryEntryStore</code>.
The <a id="" title="" target="_blank" href="https://github.com/oracle/coherence/tree/master/prj/examples/guides/195-bulk-loading-caches/src/main/java/com/oracle/coherence/guides/preload/cachestore/SmartCacheStore.java"><code>SmartCacheStore</code></a> class in the example source code is an implementation of a <code>BinaryEntryStore</code>.
Like the <code>ControllableCacheStore</code> in the example above, the <code>SmartCacheStore</code> wraps a delagate <code>CacheStore</code> so it can be used to make any <code>CacheStore</code> implementation smart.</p>


<h4 id="_decorating_a_binary">Decorating a Binary</h4>
<div class="section">
<p>A <code>com.tangosol.util.Binary</code> is usually used to hold a serialized binary value (occasionally it may also be a <code>com.tangosol.io.ReadBuffer</code>). As well as the serialized data, that could have been serialized using any serializer configured in Coherence (Java, POF, json, etc) a <code>Binary</code> can be decorated with other information.
Each decoration has an <code>int</code> identifier that is used to add, remove or obtain a specific decoration.
Coherence itself uses decorations for a number of functions, so a number of decoration identifiers values are reserved. The identifiers are all stored in constants in <code>com.tangosol.util.ExternalizableHelper</code> class and all have the prefix <code>DECO_</code>, for example <code>DECO_EXPIRY</code>. There are three identifiers reserved for use by application code, <code>DECO_APP_1</code>, <code>DECO_APP_2</code> and <code>DECO_APP_3</code> which Coherence will not use, so for this example we can use <code>ExternalizableHelper.DECO_APP_1</code> for the <code>SmartCacheStore</code> decoration.</p>

<p>The method used to decorate a <code>Binary</code> is <code>ExternalizableHelper</code> method:</p>

<markup
lang="java"

>public static Binary decorate(Binary bin, int nId, Binary binDecoration)</markup>

<p>A <code>Binary</code> is decorated with another <code>Binary</code> value.
For the <code>SmartCacheStore</code> we do not care what the value of the decoration is, we only check whether it is present or not. Obviously we do not want to use a large <code>Binary</code> decoration, as this will add to the serialized size of the value, the smallest possible <code>Binary</code> is the constant value <code>Binary.NO_BINARY</code>, which is actually zero bytes, but still a <code>Binary</code>.</p>

<p>We can therefore decorate a <code>Binary</code> for use in the <code>SmartCacheStore</code> like this:</p>

<markup
lang="java"

>Binary binary = // create the serialized binary value
Binary decorated = ExternalizableHelper.decorate(binary, ExternalizableHelper.DECO_APP_1, Binary.NO_BINARY);</markup>

</div>

<h4 id="_the_smartcachestore_implementation">The SmartCacheStore Implementation</h4>
<div class="section">
<p>The example below shows part of the <code>SmartCacheStore</code> implementation.
The <code>store</code> and <code>storeAll</code> methods are shown, but the other <code>BinaryEntryStore</code> are omitted here to make things clearer.</p>

<markup
lang="java"
title="SmartCacheStore.java"
>public class SmartCacheStore&lt;K, V&gt;
        implements BinaryEntryStore&lt;K, V&gt;
    {
    private final BinaryEntryStore&lt;K, V&gt; delegate;

    public SmartCacheStore(CacheStore&lt;K, V&gt; delegate)
        {
        this.delegate = new WrapperBinaryEntryStore&lt;&gt;(Objects.requireNonNull(delegate));  <span class="conum" data-value="1" />
        }

    @Override
    public void store(BinaryEntry&lt;K, V&gt; entry)                                            <span class="conum" data-value="2" />
        {
        if (shouldStore(entry))
            {
            delegate.store(entry);
            }
        }

    @Override
    public void storeAll(Set&lt;? extends BinaryEntry&lt;K, V&gt;&gt; entries)                        <span class="conum" data-value="3" />
        {
        Set&lt;? extends BinaryEntry&lt;K, V&gt;&gt; entriesToStore = entries.stream()
                .filter(this::shouldStore)
                .collect(Collectors.toSet());

        if (entriesToStore.size() &gt; 0)
            {
            delegate.storeAll(entriesToStore);
            }
        }

    private boolean shouldStore(BinaryEntry&lt;K, V&gt; entry)                                  <span class="conum" data-value="4" />
        {
        return !ExternalizableHelper.isDecorated(entry.getBinaryValue(), ExternalizableHelper.DECO_APP_1);
        }

    // other BinaryEntryStore omitted...
    }</markup>

<ul class="colist">
<li data-value="1">The delegate <code>CacheStore</code> passed to the constructor is wrapped in a <code>WrapperBinaryEntryStore</code> to make it look like a <code>BinaryEntryStore</code> that the <code>SmartCacheStore</code> can delegate to.</li>
<li data-value="2">The <code>store</code> method is passed the <code>BinaryEntry</code> to be stored. To check whether the delegate should be called, it calls the <code>shouldStore</code> method. If <code>shouldStore</code> returns <code>true</code> the delegate is called to store the entry.</li>
<li data-value="3">The <code>storeAll</code> method is similar to the <code>store</code> method, but is passed a <code>Set</code> of entries to store. The set is filtered to create a new <code>Set</code> containing only entries that should be stored. If there are any entries to store the delegate is called.</li>
<li data-value="4">The <code>shouldStore</code> method checks to see whether an entry should be stored. The <code>Binary</code> value is obtained from the <code>BinaryEntry</code> and checked to see whether the <code>ExternalizableHelper.DECO_APP_1</code> decoration is present.</li>
</ul>
</div>

<h4 id="_loading_decorated_binary_values">Loading Decorated Binary Values</h4>
<div class="section">
<p>Now we have a <code>SmartCacheStore</code> that only stores non-decorated values, how do we load decorated <code>Binary</code> values into a cache?</p>

<p>This example only works on a cluster member, because it requires access to the server side cache service that the cache being loaded is using. This will allow the preloader to create serialized <code>Binary</code> keys and values using the correct serilzation method for the cache.</p>

<p><strong>Obtain a Binary NamedMap or NamedCache</strong></p>

<p>The usual way to obtain a <code>NamedMap</code> (or <code>NamedCache</code>) from a Coherence <code>Session</code> is to just call <code>session.getMap(cacheName);</code> but the <code>getMap()</code> and <code>getCache()</code> method allow options to be passed in to control the cache returned. One of these is the <code>WithClassLoader</code> option, that takes a <code>ClassLoader</code>. Coherence has a special class loader obtained from <code>com.tangosol.util.NullImplementation.getClassLoader()</code>. If this is used, the cache returned is a binary cache, this means that values passed to methods such as <code>get</code>, <code>put</code>, putAll` etc., must be instances of <code>Binary</code>, i.e. a binary cache gives access to the actual serialized data in the cache.</p>

<p>The code below gets the normal "customers" cache, with <code>Integer</code> keys and <code>Customer</code> values.</p>

<markup
lang="java"

>NamedMap&lt;Integer, Customer&gt; namedMap = session.getMap("customers");</markup>

<p>Whereas the code below gets the same "customers" cache but with <code>Binary</code> keys (serializer <code>Integer</code> instances in this example) and <code>Binary</code> values (serialized <code>Customer</code> instances in this example).</p>

<markup
lang="java"

>NamedMap&lt;Binary, Binary&gt; namedMap = session.getMap("customers",
        WithClassLoader.using(NullImplementation.getClassLoader()));</markup>

<p><strong>Load Data to a Binary NamedMap or NamedCache</strong></p>

<p>Now we can obtain a binary cache to use to load decorated binary values, we can put everything together to load data from the data source, convert it to decorated binaty values, and call <code>putAll</code>.</p>

<p>The method below from the <code>AbstractBinaryJdbcPreloadTask</code> class in the example source loads a batch of decorated values into a cache.  The method is passed a batch of values to load (in a <code>Map</code>), and a binary <code>NamedMap</code> to load the batch of data into,</p>

<markup
lang="java"
title="AbstractBinaryJdbcPreloadTask.java"
>private void load(Map&lt;K, V&gt; batch, NamedMap&lt;Binary, Binary&gt; namedMap)
    {
    BackingMapManagerContext context = namedMap.getService().getBackingMapManager().getContext(); <span class="conum" data-value="1" />
    Converter&lt;K, Binary&gt; keyConverter = context.getKeyToInternalConverter();                      <span class="conum" data-value="2" />
    Converter&lt;V, Binary&gt; valueConverter = context.getValueToInternalConverter();                  <span class="conum" data-value="3" />

    Map&lt;Binary, Binary&gt; decoratedBatch = new HashMap&lt;&gt;();                                         <span class="conum" data-value="4" />
    for (Map.Entry&lt;K, V&gt; entry : batch.entrySet())
        {
        Binary binary = valueConverter.convert(entry.getValue());                                 <span class="conum" data-value="5" />
        Binary decorated = ExternalizableHelper.decorate(binary, decorationId, Binary.NO_BINARY);
        decoratedBatch.put(keyConverter.convert(entry.getKey()), decorated);                      <span class="conum" data-value="6" />
        }

    namedMap.putAll(decoratedBatch);    <span class="conum" data-value="7" />
    batch.clear();
    }</markup>

<ul class="colist">
<li data-value="1">First the <code>BackingMapManagerContext</code> is obtained from the <code>NamedMap</code>. This will allow access to the converters to use to serialize the keys and values into <code>Binary</code> values.</li>
<li data-value="2">Obtain the key converter to serialize keys to <code>Binary</code>. Coherence uses different converters to serialize the key and value, because different logic is used internally to decorate a serialized key. If a key is converted to a <code>Binary</code> incorrectly it will not be possible to get the value back out again with something like a <code>get()</code> call.</li>
<li data-value="3">Obtain the converter to use to serialize values to <code>Binary</code></li>
<li data-value="4">Create a <code>Map</code> to hold the <code>Binary</code> keys and value we will put into the cache, them iterate over the values in the batch.</li>
<li data-value="5">Convert the value to a <code>Binary</code> and add the decoration to it.</li>
<li data-value="6">Put the serialized key and decorated value into the <code>decoratedBatch</code> map</li>
<li data-value="7">After converting all th keys and value to <code>Binary</code> keys and decorated <code>Binary</code> values the map of binaries can be passed to the <code>namedMap.putAll</code> method. As all the data is already serialized, Coherence will send it unchanged to the storage enabled cluster members that own those entries.</li>
</ul>
<p>The <code>SmartCacheStore</code> works around the caveats of a <code>ControllableCacheStore</code></p>

<ul class="ulist">
<li>
<p>As the decoration on the value controls whether it is written, this method will work with write-behind.</p>

</li>
<li>
<p>There is no need to turn on or off the cache stores</p>

</li>
<li>
<p>If application code updates caches during the preload, those updated values will not be decorated and will be stored by the cache store</p>

</li>
<li>
<p>If a new cluster member joins and becomes the owner of a number of entries, those entries will still have the decoration present and will not be written by the cache store.</p>

</li>
</ul>
</div>
</div>

<h3 id="_summary_2">Summary</h3>
<div class="section">
<p>This guide has shown a few solutions to the preload use case.
Which one is suitable depends on your applications requirements.
The example code has been built in such a way that it can be taken as a starting framework for a preloader and controllable/smart cache stores and expanded to suit application use cases.</p>

</div>
</div>
</doc-view>
