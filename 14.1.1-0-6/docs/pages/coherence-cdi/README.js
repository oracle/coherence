<doc-view>

<h2 id="_coherence_cdi">Coherence CDI</h2>
<div class="section">
<p>Coherence CDI provides support for <a id="" title="" target="_blank" href="http://cdi-spec.org/">CDI</a> (Contexts and Dependency  Injection) within Coherence cluster members.</p>

<p>It allows you both to inject Coherence-managed resources, such as <code>NamedCache</code> and <code>Session</code>  instances into CDI managed beans, and to inject CDI beans into Coherence-managed resources,  such as event interceptors and cache stores, and to handle Coherence server-side events using CDI observer methods.</p>

<p>In addition, Coherence CDI provides support for automatic injection of transient objects upon deserialization.
This allows you to inject CDI managed beans such as services and  repositories (to use DDD nomenclature) into transient objects, such as entry processor and even data class instances, greatly simplifying implementation of true Domain Driven  applications.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence CDI, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-cdi&lt;/artifactId&gt;
        &lt;version&gt;${coherence.version}&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>Once the necessary dependency is in place, you can start using CDI to inject Coherence objects into managed CDI beans, and vice versa, as the following sections describe.</p>

<ul class="ulist">
<li>
<p><router-link to="#injecting-coherence-objects-into-cdi-beans" @click.native="this.scrollFix('#injecting-coherence-objects-into-cdi-beans')">Injecting Coherence Objects into CDI Beans</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#8212;&#8203;namedcache&#8212;&#8203;and-related-objects" @click.native="this.scrollFix('#8212;&#8203;namedcache&#8212;&#8203;and-related-objects')">Injecting <code>NamedCache</code> and related objects</router-link></p>

</li>
<li>
<p><router-link to="#8212;&#8203;namedtopic&#8212;&#8203;and-related-objects" @click.native="this.scrollFix('#8212;&#8203;namedtopic&#8212;&#8203;and-related-objects')">Injecting <code>NamedTopic</code> and related objects</router-link></p>

</li>
<li>
<p><router-link to="#other-supported-injection-points" @click.native="this.scrollFix('#other-supported-injection-points')">Other Supported Injection Points</router-link></p>
<ul class="ulist">
<li>
<p>&lt;&#8592;cluster&#8212;&#8203;and&#8212;&#8203;operationalcontext&#8212;&#8203;injection,<code>Cluster</code> and <code>OperationalContext</code> Injection&gt;&gt;</p>

</li>
<li>
<p>&lt;&#8592;configurablecachefactory&#8212;&#8203;and&#8212;&#8203;session&#8212;&#8203;injection,<code>ConfigurableCacheFactory</code> and <code>Session</code> Injection&gt;&gt;</p>

</li>
<li>
<p>&lt;&#8592;serializer&#8212;&#8203;injection,<code>Serializer</code> Injection&gt;&gt;</p>

</li>
</ul>
</li>
</ul>
</li>
<li>
<p><router-link to="#injecting-cdi-beans-into-coherence-managed-objects" @click.native="this.scrollFix('#injecting-cdi-beans-into-coherence-managed-objects')">Injecting CDI Beans into Coherence-managed Objects</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#using-cdi-observers-to-handle-coherence-server-side-events" @click.native="this.scrollFix('#using-cdi-observers-to-handle-coherence-server-side-events')">Using CDI Observers to Handle Coherence Server-Side Events</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#using-asynchronous-observers" @click.native="this.scrollFix('#using-asynchronous-observers')">Using Asynchronous Observers</router-link></p>

</li>
</ul>
</li>
</ul>
</li>
<li>
<p><router-link to="#injecting-cdi-beans-into-transient-objects" @click.native="this.scrollFix('#injecting-cdi-beans-into-transient-objects')">Injecting CDI Beans into Transient Objects</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#8212;&#8203;injectable-" @click.native="this.scrollFix('#8212;&#8203;injectable-')">Making transient classes <code>Injectable</code></router-link></p>

</li>
</ul>
</li>
</ul>

<h3 id="_injecting_coherence_objects_into_cdi_beans">Injecting Coherence Objects into CDI Beans</h3>
<div class="section">
<p>CDI, and dependency injection in general, make it easy for application classes to declare the dependencies they need and let the runtime provide them when necessary.
This makes the applications easier to develop, test and reason about, and the code significantly cleaner.</p>

<p>Coherence CDI allows you to do the same for Coherence objects, such as <code>Cluster</code>, <code>Session</code>, <code>NamedCache</code>, <code>ContinuousQueryCache</code>, <code>ConfigurableCacheFactory</code>, etc.</p>


<h4 id="_injecting_namedcache_and_related_objects">Injecting <code>NamedCache</code> and related objects</h4>
<div class="section">
<p>In order to inject an instance of a <code>NamedCache</code> into your CDI bean, you simply need to define an injection point for it:</p>

<markup
lang="java"

>@Inject
private NamedCache&lt;Long, Person&gt; people;</markup>

<p>In the example above we&#8217;ve assumed that the cache name you want to inject is the same as the name of the field you are injecting into, <code>people</code>.
If that&#8217;s not the case, you can use <code>@Cache</code> qualifier to specify the name of the cache you want to obtain explicitly:</p>

<markup
lang="java"

>@Inject
@Cache("people")
private NamedCache&lt;Long, Person&gt; m_people;</markup>

<p>This is also what you have to do if you are using constructor injection instead:</p>

<markup
lang="java"

>@Inject
public MyClass(@Cache("people") NamedCache&lt;Long, Person&gt; people) {
    ...
}</markup>

<p>All of the examples above assume that you want to use the default <code>ConfigurableCacheFactory</code>, which is often, but not always the case.
For example, you may have an Extend client that connects  to multiple Coherence clusters, in which case you would have multiple Coherence cache config files, and multiple <code>ConfigurableCacheFactoriy</code> instances.</p>

<p>In this case you would use <code>@CacheFactory</code> qualifier to specify the URI of the cache configuration to use:</p>

<markup
lang="java"

>@Inject
@CacheFactory("products-cluster.xml")
private NamedCache&lt;Long, Product&gt; products;

@Inject
@CacheFactory("customers-cluster.xml")
private NamedCache&lt;Long, Customer&gt; customers;</markup>

<p>You can replace <code>NamedCache</code> in any of the examples above with <code>AsyncNamedCache</code> in order to inject  asynchronous variant of the <code>NamedCache</code> API:</p>

<markup
lang="java"

>@Inject
private AsyncNamedCache&lt;Long, Person&gt; people;</markup>

<p>You can also inject <strong>cache views</strong>, which are effectively instances of a <code>ContinuousQueryCache</code>, either by declaring the injection point as <code>ContinuousQueryCache</code> instead of <code>NamedCache</code>, or by simply adding <code>@CacheView</code> qualifier:</p>

<markup
lang="java"

>@Inject
private ContinuousQueryCache&lt;Long, Person&gt; people;

@Inject
@CacheView
private NamedCache&lt;Long, Person&gt; people;</markup>

<p>The examples above are equivalent, and both will bring <strong>all</strong> the data from the backing cache into a local view, as they will use <code>AlwaysFilter</code> when constructing <code>CQC</code>.
If you want to limit the data in the view to a subset, you can implement a custom <router-link to="">filter binding</router-link> (recommended), or use a built-in <code>@WhereFilter</code> for convenience, which allows you to specify a filter using CohQL:</p>

<markup
lang="java"

>@Inject
@CacheView
@WhereFilter("gender = 'MALE'")
@Cache("people")
private NamedCache&lt;Long, Person&gt; men;

@Inject
@CacheView
@WhereFilter("gender = 'FEMALE'")
@Cache("people")
private NamedCache&lt;Long, Person&gt; women;</markup>

<p>The <code>ContinuousQueryCache</code>, and <strong>cache views</strong> by extension, also support transformation of the  cached value on the server, in order to reduce both the amount of data stored locally and the amount of data transferred over the network.
For example, you may have a complex <code>Person</code> objects in the backing cache, but only need their names in order to populate a drop down on the client UI.</p>

<p>In that case, you can implement a custom <router-link to="">extractor binding</router-link> (recommended), or use a built-in <code>@PropertyExtractor</code> for convenience:</p>

<markup
lang="java"

>@Inject
@CacheView
@PropertyExtractor("fullName")
@Cache("people")
private NamedCache&lt;Long, String&gt; names;</markup>

<p>Note that the value type in the example above has changed from <code>Person</code> to <code>String</code>, due to server-side transformation caused by the specified <code>@PropertyExtractor</code>.</p>

</div>

<h4 id="_injecting_namedtopic_and_related_objects">Injecting <code>NamedTopic</code> and related objects</h4>
<div class="section">
<p>In order to inject an instance of a <code>NamedTopic</code> into your CDI bean, you simply need to define an injection point for it:</p>

<markup
lang="java"

>@Inject
private NamedTopic&lt;Order&gt; orders;</markup>

<p>In the example above we&#8217;ve assumed that the topic name you want to inject is the same as the name of the field you are injecting into, in this case<code>orders</code>.
If that&#8217;s not the case, you  can use <code>@Topic</code> qualifier to specify the name of the cache you want to obtain explicitly:</p>

<markup
lang="java"

>@Inject
@Topic("orders")
private NamedTopic&lt;Order&gt; m_orders;</markup>

<p>This is also what you have to do if you are using constructor injection instead:</p>

<markup
lang="java"

>@Inject
public MyClass(@Topic("orders") NamedTopic&lt;Order&gt; orders) {
    ...
}</markup>

<p>All of the examples above assume that you want to use the default <code>ConfigurableCacheFactory</code>, which is often, but not always the case.
For example, you may have an Extend client that connects  to multiple Coherence clusters, in which case you would have multiple Coherence cache config files, and multiple <code>ConfigurableCacheFactoriy</code> instances.</p>

<p>In this case you would use <code>@CacheFactory</code> qualifier to specify the URI of the cache configuration to use:</p>

<markup
lang="java"

>@Inject
@CacheFactory("payments-cluster.xml")
private NamedTopic&lt;PaymentRequest&gt; payments;

@Inject
@CacheFactory("shipments-cluster.xml")
private NamedTopic&lt;ShippingRequest&gt; shipments;</markup>

<p>The examples above allow you to inject a <code>NamedTopic</code> instance into your CDI bean, but it is often simpler and more convenient to inject <code>Publisher</code> or <code>Subscriber</code> for a given topic instead.</p>

<p>This can be easily accomplished by replacing <code>NamedTopic&lt;T&gt;</code> in any of the examples above with either <code>Publisher&lt;T&gt;</code>:</p>

<markup
lang="java"

>@Inject
private Publisher&lt;Order&gt; orders;

@Inject
@Topic("orders")
private Publisher&lt;Order&gt; m_orders;

@Inject
@CacheFactory("payments-cluster.xml")
private Publisher&lt;PaymentRequest&gt; payments;</markup>

<p>or <code>Subscriber&lt;T&gt;</code>:</p>

<markup
lang="java"

>@Inject
private Subscriber&lt;Order&gt; orders;

@Inject
@Topic("orders")
private Subscriber&lt;Order&gt; m_orders;

@Inject
@CacheFactory("payments-cluster.xml")
private Subscriber&lt;PaymentRequest&gt; payments;</markup>

<p>Basically, all topic-related details, such as topic name (based on either injection point name or the explicit name from <code>@Topic</code> annotation), cache factory URI and message type, will be used under the hood to retrieve the <code>NamedTopic</code>, and to obtain <code>Publisher</code> or <code>Subscriber</code> from it.</p>

<p>Additionally, if you want to place your <code>Subscriber</code>s into a subscriber group, you can easily accomplish that by adding <code>@SubscriberGroup</code> qualifier to the injection point:</p>

<markup
lang="java"

>@Inject
@SubscriberGroup("orders-queue")
private Subscriber&lt;Order&gt; orders;</markup>

</div>

<h4 id="_other_supported_injection_points">Other Supported Injection Points</h4>
<div class="section">
<p>While the injection of a <code>NamedCache</code>, <code>NamedTopic</code>, and related instances, as shown above,  is probably the single most useful feature of Coherence CDI, it is certainly not the only one.
The following sections describe other Coherence artifacts that can be injected using Coherence CDI.</p>


<h5 id="_cluster_and_operationalcontext_injection"><code>Cluster</code> and <code>OperationalContext</code> Injection</h5>
<div class="section">
<p>If you need an instance of a <code>Cluster</code> interface somewhere in your application, you can easily obtain it via injection:</p>

<markup
lang="java"

>@Inject
private Cluster cluster;</markup>

<p>You can do the same if you need an instance of an <code>OperationalContext</code>:</p>

<markup
lang="java"

>@Inject
private OperationalContext ctx;</markup>

</div>

<h5 id="_configurablecachefactory_and_session_injection"><code>ConfigurableCacheFactory</code> and <code>Session</code> Injection</h5>
<div class="section">
<p>On rare occasions when you need to use either of these directly, Coherence CDI makes it trivial to do so.</p>

<p>To obtain an instance of a default <code>CCF</code> or <code>Session</code>, all you need to do is inject them into the  class that needs to use them:</p>

<markup
lang="java"

>@Inject
private ConfigurableCacheFactory ccf;

@Inject
private Session session;</markup>

<p>If you need a specific <code>CCF</code> or <code>Session</code> you can simply qualify them using <code>@CacheFactory</code> qualifier and specifying the URI of the cache config file to use:</p>

<markup
lang="java"

>@Inject
@CacheFactory("my-cache-config.xml")
private ConfigurableCacheFactory ccf;

@Inject
@CacheFactory("my-cache-config.xml")
private Session session;</markup>

</div>

<h5 id="_serializer_injection"><code>Serializer</code> Injection</h5>
<div class="section">
<p>While in most cases you won&#8217;t have to deal with serializers directly, Coherence CDI makes it simple to obtain named serializers (and to register new ones) when you need.</p>

<p>To get a default <code>Serializer</code> for the current context class loader, you can simply inject it:</p>

<markup
lang="java"

>@Inject
private Serializer defaultSerializer;</markup>

<p>However, it may be more useful to inject one of the named serializers defined in the operational configuration, which can be easily accomplished using <code>@SerializerFormat</code> qualifier:</p>

<markup
lang="java"

>@Inject
@SerializerFormat("java")
private Serializer javaSerializer;

@Inject
@SerializerFormat("pof")
private Serializer pofSerializer;</markup>

<p>In addition to the serializers defined in the operational config, the example above will also perform <code>BeanManager</code> lookup for a named bean that implements <code>Serializer</code> interface.</p>

<p>That means that if you implemented a custom <code>Serializer</code> bean, such as:</p>

<markup
lang="java"

>@Named("json")
@ApplicationScoped
public class JsonSerializer implements Serializer {
    ...
}</markup>

<p>it would be automatically discovered and registered by the CDI, and you would then be able to inject it just as easily as the named serializers defined in the operational config:</p>

<markup
lang="java"

>@Inject
@SerializerFormat("json")
private Serializer jsonSerializer;</markup>

</div>
</div>
</div>

<h3 id="_injecting_cdi_beans_into_coherence_managed_objects">Injecting CDI Beans into Coherence-managed Objects</h3>
<div class="section">
<p>Coherence has a number of server-side extension points, which allow users to customize application  behavior in different ways, typically by configuring their extensions within various sections of the  cache configuration file.
For example, the users can implement event interceptors and cache stores,  in order to handle server-side events and integrate with the external data stores and other services.</p>

<p>Coherence CDI provides a way to inject named CDI beans into these extension points using custom  configuration namespace handler.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
        xmlns:cdi="class://com.oracle.coherence.cdi.CdiNamespaceHandler"
        xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;</markup>

<p>Once you&#8217;ve declared the handler for the <code>cdi</code> namespace above, you can specify <code>&lt;cdi:bean&gt;</code> element in any place where you would normally use <code>&lt;class-name&gt;</code> or <code>&lt;class-factory-name&gt;</code> elements:</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;

&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
        xmlns:cdi="class://com.oracle.coherence.cdi.CdiNamespaceHandler"
        xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

    &lt;interceptors&gt;
        &lt;interceptor&gt;
            &lt;instance&gt;
                &lt;cdi:bean&gt;registrationListener&lt;/cdi:bean&gt;
            &lt;/instance&gt;
        &lt;/interceptor&gt;
        &lt;interceptor&gt;
            &lt;instance&gt;
                &lt;cdi:bean&gt;activationListener&lt;/cdi:bean&gt;
            &lt;/instance&gt;
        &lt;/interceptor&gt;
    &lt;/interceptors&gt;

    &lt;caching-scheme-mapping&gt;
        &lt;cache-mapping&gt;
            &lt;cache-name&gt;*&lt;/cache-name&gt;
            &lt;scheme-name&gt;distributed-scheme&lt;/scheme-name&gt;
            &lt;interceptors&gt;
                &lt;interceptor&gt;
                    &lt;instance&gt;
                        &lt;cdi:bean&gt;cacheListener&lt;/cdi:bean&gt;
                    &lt;/instance&gt;
                &lt;/interceptor&gt;
            &lt;/interceptors&gt;
        &lt;/cache-mapping&gt;
    &lt;/caching-scheme-mapping&gt;

    &lt;caching-schemes&gt;
        &lt;distributed-scheme&gt;
            &lt;scheme-name&gt;distributed-scheme&lt;/scheme-name&gt;
            &lt;service-name&gt;PartitionedCache&lt;/service-name&gt;
            &lt;local-storage system-property="coherence.distributed.localstorage"&gt;true&lt;/local-storage&gt;
            &lt;partition-listener&gt;
                &lt;cdi:bean&gt;partitionListener&lt;/cdi:bean&gt;
            &lt;/partition-listener&gt;
            &lt;member-listener&gt;
                &lt;cdi:bean&gt;memberListener&lt;/cdi:bean&gt;
            &lt;/member-listener&gt;
            &lt;backing-map-scheme&gt;
                &lt;local-scheme/&gt;
            &lt;/backing-map-scheme&gt;
            &lt;autostart&gt;true&lt;/autostart&gt;
            &lt;interceptors&gt;
                &lt;interceptor&gt;
                    &lt;instance&gt;
                        &lt;cdi:bean&gt;storageListener&lt;/cdi:bean&gt;
                    &lt;/instance&gt;
                &lt;/interceptor&gt;
            &lt;/interceptors&gt;
        &lt;/distributed-scheme&gt;
    &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<p>Note that you can only inject named CDI beans (beans with an explicit <code>@Named</code> annotations) via  <code>&lt;cdi:bean&gt;</code> element.
For example, the <code>cacheListener</code> interceptor bean used above would look similar to this:</p>

<markup
lang="java"

>@ApplicationScoped
@Named("cacheListener")
@EntryEvents(INSERTING)
public class MyCacheListener
        implements EventInterceptor&lt;EntryEvent&lt;Long, String&gt;&gt; {
    @Override
    public void onEvent(EntryEvent&lt;Long, String&gt; e) {
        // handle INSERTING event
    }
}</markup>

<p>Also keep in mind that only <code>@ApplicationScoped</code> beans can be injected, which implies that they  may be shared.
For example, because we&#8217;ve used wildcard <code>*</code> as a cache name within the cache mapping in the example above, the same instance of <code>cacheListener</code> will receive events from multiple caches.</p>

<p>This is typically fine, as the event itself provides the details about the context that raised it, including cache name and the service it was raised from, but it does imply that any shared state that you may have within your listener class shouldn&#8217;t be context-specific and it must be safe for concurrent access from multiple threads.
If you can&#8217;t guarantee the latter, you may want to declare the <code>onEvent</code> method as <code>synchronized</code>, to ensure only one thread at a time can access any shared state you may have.</p>


<h4 id="_using_cdi_observers_to_handle_coherence_server_side_events">Using CDI Observers to Handle Coherence Server-Side Events</h4>
<div class="section">
<p>While the above examples show that you can implement any Coherence <code>EventInterceptor</code> as a CDI bean and register it using <code>&lt;cdi:bean&gt;</code> element within the cache configuration file, Coherence CDI  also provides a much simpler way to accomplish the same goal using standard CDI Events and Observers.</p>

<p>The first thing you need to do is register a single global interceptor within the cache config:</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns:cdi="class://com.oracle.coherence.cdi.CdiNamespaceHandler"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;interceptors&gt;
    &lt;interceptor&gt;
      &lt;instance&gt;
        &lt;cdi:bean&gt;com.oracle.coherence.cdi.EventDispatcher&lt;/cdi:bean&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;
  &lt;/interceptors&gt;

  &lt;!-- the rest of cache config as usual --&gt;
&lt;/cache-config&gt;</markup>

<p>Coherence CDI <code>EventDispatcher</code> bean will then listen to all events raised by all Coherence event dispatchers and re-raise them as CDI events that you can observe.
For example, to implement the  equivalent of <code>cacheListener</code> interceptor above, you would simply define an observer method in  any CDI bean that wants to be notified when the event happens:</p>

<markup
lang="java"

>private void onInserting(@Observes @Inserting EntryEvent&lt;?, ?&gt; event) {
    // handle INSERTING event on any cache
}</markup>

<p>The observer method above will receive all <code>INSERTING</code> events for all the caches, across all the services, but you can use CDI qualifiers to control that behavior:</p>

<markup
lang="java"

>private void onInserting(@Observes @Updated @CacheName("people") EntryEvent&lt;?, ?&gt; event) {
    // handle UPDATED event on 'people' cache only
}

private void onRemoved(@Observes @Removed @ServiceName("Products") EntryEvent&lt;?, ?&gt; event) {
    // handle REMOVED event on any cache on the 'Products' service
}</markup>

<p>Of course, you can also remove qualifiers to broaden the scope of events your handler receives:</p>

<markup
lang="java"

>private void onEntryEvent(@Observes EntryEvent&lt;?, ?&gt; event) {
    // handle any event on any cache
}</markup>

<p>The examples above show only how to handle <code>EntryEvent</code>s, but the same applies to all other event types:</p>

<markup
lang="java"

>private void onActivated(@Observes @Activated LifecycleEvent event) {
    // handle cache factory activation
}

private void onCreatedPeople(@Observes @Created @CacheName("people") CacheLifecycleEvent event) {
    // handle creation of the 'people' cache
}

private void onExecuting(@Observes @Executing @CacheName("people") @Processor(Uppercase.class) EntryProcessorEvent event) {
    // intercept 'Uppercase` entry processor execution against 'people' cache
}</markup>

<p>And again, you can broaden the scope by widening the type of events you observe:</p>

<markup
lang="java"

>private void onPartitionedCacheEvent(@Observes com.tangosol.net.events.partition.cache.Event&lt;?&gt; event) {
    // handle any/all events raised by the partitioned cache service (CacheLifecycleEvent, EntryEvent or EntryProcessorEvent)
    // can use @CacheName and @ServiceName as a narrowing qualifier
}

private void onPartitionedServiceEvent(@Observes com.tangosol.net.events.partition.Event&lt;?&gt; event) {
    // handle any/all events raised by the partitioned service (TransactionEvent, TransferEvent or UnsolicitedCommitEvent)
    // can use @ServiceName as a narrowing qualifier
}

private void onEvent(@Observes com.tangosol.net.events.Event&lt;?&gt; event) {
    // handle any/all events (all of the above, plus LifecycleEvent)
}</markup>


<h5 id="_using_asynchronous_observers">Using Asynchronous Observers</h5>
<div class="section">
<p>All of the examples above used synchronous observers by specifying <code>@Observes</code> qualifier for each observer method.
However, Coherence CDI fully supports asynchronous CDI observers as well.
All you need to do is replace <code>@Observes</code> with <code>@ObservesAsync</code> in any of the examples above.</p>

<markup
lang="java"

>private void onActivated(@ObservesAsync @Activated LifecycleEvent event) {
    // handle cache factory activation
}

private void onCreatedPeople(@ObservesAsync @Created @CacheName("people") CacheLifecycleEvent event) {
    // handle creation of the 'people' cache
}

private void onExecuting(@ObservesAsync @Executing @CacheName("people") @Processor(Uppercase.class) EntryProcessorEvent event) {
    // intercept 'Uppercase` entry processor execution against 'people' cache
}</markup>

<p>However, there is an important caveat.</p>

<p>Coherence events fall into two categories: pre- and post-commit events.
All of the events whose name ends  with <code>ing</code>, such as <code>Inserting</code>, <code>Updating</code>, <code>Removing</code> or <code>Executing</code> are pre-commit, which means that they can either modify the data or even completely cancel the operation by throwing an exception, but in  order to do so they must be synchronous to ensure that they are executed on the same thread that is  executing the operation that triggered the event.</p>

<p>That means that you can <em>observe</em> them using asynchronous CDI observers, but if you want to mutate the set of entries that are part of the event payload, or veto the event by throwing an exception, you must use synchronous CDI observer.</p>

</div>
</div>
</div>

<h3 id="_injecting_cdi_beans_into_transient_objects">Injecting CDI Beans into Transient Objects</h3>
<div class="section">
<p>Using CDI to inject Coherence objects into your application classes, and CDI beans into Coherence-managed objects will allow you to support many use cases where dependency injection may be useful, but it doesn&#8217;t cover an important use case that is somewhat specific to Coherence.</p>

<p>Coherence is a distributed system, and it uses serialization in order to send both the data and the  processing requests from one cluster member (or remote client) to another, as well as to store data, both in memory and on disk.</p>

<p>Processing requests, such as entry processors and aggregators, are then deserialized on a target cluster member(s) in order to be executed, and in some cases they could benefit from dependency injection in order to avoid service lookups.</p>

<p>Similarly, while the data is stored in a serialized, binary format, it may need to be deserialized into user supplied classes for server-side processing, such as when executing entry processors and aggregators, and can also benefit from dependency injection (in order to support Domain-Driven Design (DDD), for example).</p>

<p>While these transient objects are not managed by the CDI container, Coherence CDI does support their injection after deserialization, but for performance reasons requires that you explicitly opt-in by implementing <code>com.oracle.coherence.cdi.Injectable</code> interface.</p>


<h4 id="_making_transient_classes_injectable">Making transient classes <code>Injectable</code></h4>
<div class="section">
<p>While not technically a true marker interface, <code>Injectable</code> can be treated as such for all intents and purposes.
All you need to do is add it to the <code>implements</code> clause of your class in order for injection on deserialization to kick in:</p>

<markup
lang="java"

>public class InjectableBean
        implements Injectable, Serializable {

    @Inject
    private Converter&lt;String, String&gt; converter;

    private String text;

    InjectableBean() {
    }

    InjectableBean(String text) {
        this.text = text;
    }

    String getConvertedText() {
        return converter.convert(text);
    }
}</markup>

<p>Assuming that you have the following <code>Converter</code> service implementation in your application, it will be injected into <code>InjectableBean</code> after deserialization and the <code>getConvertedText</code> method will return the value of the <code>text</code> field converted to upper case:</p>

<markup
lang="java"

>@ApplicationScoped
public class ToUpperConverter
        implements Converter&lt;String, String&gt; {
    @Override
    public String convert(String s) {
        return s.toUpperCase();
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">If your <code>Injectable</code> class has <code>@PostConstruct</code> callback method, it will be called after the injection.
However, because we have no control over object&#8217;s lifecycle after that point, <code>@PreDestroy</code> callback will <strong>never</strong> be called).</p>
</div>
<p>You should note that the above functionality is not dependent on the serialization format and will work with both Java and POF serialization (or any other custom serializer), and for any object that is  deserialized on any Coherence member (or even on a remote client).</p>

<p>While the deserialized transient objects are not true CDI managed beans, being able to inject CDI managed dependencies into them upon deserialization will likely satisfy most dependency injection requirements you will ever have in those application components.
We hope you&#8217;ll find it useful.</p>

</div>
</div>
</div>
</doc-view>
