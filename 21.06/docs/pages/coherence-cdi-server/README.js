<doc-view>

<h2 id="_coherence_cdi">Coherence CDI</h2>
<div class="section">
<p>Coherence CDI provides support for <a id="" title="" target="_blank" href="http://cdi-spec.org/">CDI</a> (Contexts and Dependency  Injection) within Coherence
cluster members.</p>

<p>It allows you both to inject Coherence-managed resources, such as <code>NamedMap</code>, <code>NamedCache</code> and <code>Session</code>  instances into
CDI managed beans, to inject CDI beans into Coherence-managed resources, such as event interceptors and cache stores,
and to handle Coherence server-side events using CDI observer methods.</p>

<p>In addition, Coherence CDI provides support for automatic injection of transient objects upon deserialization.
This allows you to inject CDI managed beans such as services and repositories (to use DDD nomenclature) into transient
objects, such as entry processor and even data class instances, greatly simplifying implementation of true Domain Driven
applications.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence CDI, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-cdi-server&lt;/artifactId&gt;
        &lt;version&gt;21.06&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>Once the necessary dependency is in place, you can start using CDI to inject Coherence objects into managed CDI beans,
and vice versa, as the following sections describe.</p>

<ul class="ulist">
<li>
<p><router-link to="#inject-coherence-objects" @click.native="this.scrollFix('#inject-coherence-objects')">Injecting Coherence Objects into CDI Beans</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#inject-namedmap" @click.native="this.scrollFix('#inject-namedmap')">Injecting <code>NamedMap</code>, NamedCache`, and related objects</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#inject-views" @click.native="this.scrollFix('#inject-views')">Injecting <code>NamedMap</code> or <code>NamedCache</code> Views</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#inject-namedtopic" @click.native="this.scrollFix('#inject-namedtopic')">Injecting <code>NamedTopic</code> and related objects</router-link></p>

</li>
<li>
<p><router-link to="#other-injection-points" @click.native="this.scrollFix('#other-injection-points')">Other Supported Injection Points</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#inject-cluster" @click.native="this.scrollFix('#inject-cluster')"><code>Cluster</code> and <code>OperationalContext</code> Injection</router-link></p>

</li>
<li>
<p><router-link to="#inject-ccf" @click.native="this.scrollFix('#inject-ccf')">Named <code>Session</code> Injection</router-link></p>

</li>
<li>
<p><router-link to="#inject-serializer" @click.native="this.scrollFix('#inject-serializer')"><code>Serializer</code> Injection</router-link></p>

</li>
</ul>
</li>
</ul>
</li>
<li>
<p><router-link to="#inject-into-coherence" @click.native="this.scrollFix('#inject-into-coherence')">Injecting CDI Beans into Coherence-managed Objects</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#cdi-events" @click.native="this.scrollFix('#cdi-events')">Using CDI Observers to Handle Coherence Server-Side Events</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#cdi-event-types" @click.native="this.scrollFix('#cdi-event-types')">Observer specific event types</router-link></p>

</li>
<li>
<p><router-link to="#cdi-events-filter" @click.native="this.scrollFix('#cdi-events-filter')">Filter the events to be observed</router-link></p>

</li>
<li>
<p><router-link to="#cdi-events-transform" @click.native="this.scrollFix('#cdi-events-transform')">Transform the events to be observed</router-link></p>

</li>
<li>
<p><router-link to="#cdi-events-scopes" @click.native="this.scrollFix('#cdi-events-scopes')">Observe events for maps and caches in specific scopes or services</router-link></p>

</li>
<li>
<p><router-link to="#cdi-events-async" @click.native="this.scrollFix('#cdi-events-async')">Using Asynchronous Observers</router-link></p>

</li>
</ul>
</li>
</ul>
</li>
<li>
<p><router-link to="#inject-transient" @click.native="this.scrollFix('#inject-transient')">Injecting CDI Beans into Transient Objects</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#transient-injectable" @click.native="this.scrollFix('#transient-injectable')">Making transient classes <code>Injectable</code></router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#filter-bindings" @click.native="this.scrollFix('#filter-bindings')">Filter Binding Annotations</router-link></p>

</li>
<li>
<p><router-link to="#extractor-binding" @click.native="this.scrollFix('#extractor-binding')">Extractor Binding Annotations</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#extractor-binding-built-in" @click.native="this.scrollFix('#extractor-binding-built-in')">Built-In Extractor Binding Annotations</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#extractor-binding-property" @click.native="this.scrollFix('#extractor-binding-property')">@PropertyExtractor</router-link></p>

</li>
<li>
<p><router-link to="#extractor-binding-chained" @click.native="this.scrollFix('#extractor-binding-chained')">@ChainedExtractor</router-link></p>

</li>
<li>
<p><router-link to="#extractor-binding-pof" @click.native="this.scrollFix('#extractor-binding-pof')">@PofExtractor</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#extractor-binding-custom" @click.native="this.scrollFix('#extractor-binding-custom')">Custom Extractor Binding Annotations</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#transformer-binding" @click.native="this.scrollFix('#transformer-binding')">MapEventTransformer Binding Annotations</router-link></p>

</li>
</ul>

<h3 id="inject-coherence-objects">Injecting Coherence Objects into CDI Beans</h3>
<div class="section">
<p>CDI, and dependency injection in general, make it easy for application classes to declare the dependencies they need and
let the runtime provide them when necessary.
This makes the applications easier to develop, test and reason about, and the code extremely clean.</p>

<p>Coherence CDI allows you to do the same for Coherence objects, such as <code>Cluster</code>, <code>Session</code>, <code>NamedMap</code>,<code>NamedCache</code>,
<code>ContinuousQueryCache</code>, <code>ConfigurableCacheFactory</code>, etc.</p>


<h4 id="inject-namedmap">Injecting <code>NamedMap</code>, <code>NamedCache</code> and related objects</h4>
<div class="section">
<p>In order to inject an instance of a <code>NamedMap</code> into your CDI bean, you simply need to define an injection point for it:</p>

<markup
lang="java"

>import javax.inject.Inject;

@Inject
private NamedMap&lt;Long, Person&gt; people;</markup>

<p>In the example above we&#8217;ve assumed that the map name you want to inject is the same as the name of the field you are
injecting into, <code>people</code>.
If that&#8217;s not the case, you can use <code>@Name</code> qualifier to specify the name of the map you want to obtain explicitly:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
@Name("people")
private NamedMap&lt;Long, Person&gt; m_people;</markup>

<p>This is also what you have to do if you are using constructor injection or setter injection:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
public MyClass(@Name("people") NamedMap&lt;Long, Person&gt; people) {
    ...
}

@Inject
public void setPeople(@Name("people") NamedMap&lt;Long, Person&gt; people) {
    ...
}</markup>

<p>All the examples above assume that you want to use the default scope, which is often, but not always the case.
For example, you may have an Extend client that connects  to multiple Coherence clusters, in which case you would have
multiple scopes.</p>

<p>In this case you would use <code>@SessionName</code> qualifier to specify the name of the configured <code>Session</code>,
that will be used to supply the cache or map:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.SessionName;
import javax.inject.Inject;

@Inject
@Name("Products")
private NamedCache&lt;Long, Product&gt; products;

@Inject
@SessionName("Customers")
private NamedCache&lt;Long, Customer&gt; customers;</markup>

<p>You can replace <code>NamedMap</code> or <code>NamedCache</code> in any of the examples above with <code>AsyncNamedCache</code> and <code>AsyncNamedCache</code>
respectively, in order to inject  asynchronous variant of those APIs:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.SessionName;
import javax.inject.Inject;

@Inject
private AsyncNamedMap&lt;Long, Person&gt; people;

@Inject
@SessionName("Products")
private AsyncNamedCache&lt;Long, Person&gt; Product;</markup>


<h5 id="inject-views">Inject Views</h5>
<div class="section">
<p>You can also inject <strong>views</strong>, by simply adding <code>View</code> qualifier to either <code>NamedMap</code> or <code>NamedCache</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.View;
import javax.inject.Inject;

@Inject
@View
private NamedMap&lt;Long, Person&gt; people;

@Inject
@View
private NamedCache&lt;Long, Product&gt; products;</markup>

<p>The examples above are equivalent, and both will bring <strong>all</strong> the data from the backing map into a local view, as they
will use <code>AlwaysFilter</code> when constructing a view.
If you want to limit the data in the view to a subset, you can implement a <router-link to="#filter-bindings" @click.native="this.scrollFix('#filter-bindings')">Custom FilterBinding</router-link>
(recommended), or use a built-in <code>@WhereFilter</code> for convenience, which allows you to specify a filter using CohQL:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.View;
import com.oracle.coherence.cdi.WhereFilter;
import javax.inject.Inject;

@Inject
@View
@WhereFilter("gender = 'MALE'")
@Name("people")
private NamedMap&lt;Long, Person&gt; men;

@Inject
@View
@WhereFilter("gender = 'FEMALE'")
@Name("people")
private NamedMap&lt;Long, Person&gt; women;</markup>

<p>The  <strong>views</strong> also support transformation of the entry values on the server, in order to reduce both the amount of data
stored locally, and the amount of data transferred over the network.
For example, you may have a complex <code>Person</code> objects in the backing map, but only need their names in order to populate
a drop down on the client UI.</p>

<p>In that case, you can implement a custom <router-link to="#custom-extractor" @click.native="this.scrollFix('#custom-extractor')">ExtractorBinding</router-link> (recommended), or use a built-in
<code>@PropertyExtractor</code> for convenience:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.View;
import com.oracle.coherence.cdi.PropertyExtractor;
import javax.inject.Inject;

@Inject
@View
@PropertyExtractor("fullName")
@Name("people")
private NamedMap&lt;Long, String&gt; names;</markup>

<p>Note that the value type in the example above has changed from <code>Person</code> to <code>String</code>, due to server-side transformation
caused by the specified <code>@PropertyExtractor</code>.</p>

</div>
</div>

<h4 id="inject-namedtopic">Injecting <code>NamedTopic</code> and related objects</h4>
<div class="section">
<p>In order to inject an instance of a <code>NamedTopic</code> into your CDI bean, you simply need to define an injection point for it:</p>

<markup
lang="java"

>import com.tangosol.net.NamedTopic;
import javax.inject.Inject;

@Inject
private NamedTopic&lt;Order&gt; orders;</markup>

<p>In the example above we&#8217;ve assumed that the topic name you want to inject is the same as the name of the field you are
injecting into, in this case <code>orders</code>.
If that&#8217;s not the case, you  can use <code>@Name</code> qualifier to specify the name of the topic you want to obtain explicitly:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.tangosol.net.NamedTopic;
import javax.inject.Inject;

@Inject
@Name("orders")
private NamedTopic&lt;Order&gt; topic;</markup>

<p>This is also what you have to do if you are using constructor or setter injection instead:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.tangosol.net.NamedTopic;
import javax.inject.Inject;

@Inject
public MyClass(@Name("orders") NamedTopic&lt;Order&gt; orders) {
    ...
}

@Inject
public void setOrdersTopic(@Name("orders") NamedTopic&lt;Order&gt; orders) {
    ...
}</markup>

<p>All the examples above assume that you want to use the default scope, which is often, but not always the case.
For example, you may have an Extend client that connects to multiple Coherence clusters, in which case you would have
multiple scopes.</p>

<p>In this case you would use <code>@SessionName</code> qualifier to specify the name of the configured <code>Session</code>,
that will be used to supply the topic:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.SessionName;
import com.tangosol.net.NamedTopic;
import javax.inject.Inject;

@Inject
@SessionName("Finance")
private NamedTopic&lt;PaymentRequest&gt; payments;

@Inject
@SessionName("Shipping")
private NamedTopic&lt;ShippingRequest&gt; shipments;</markup>

<p>The examples above allow you to inject a <code>NamedTopic</code> instance into your CDI bean, but it is often simpler and more
convenient to inject <code>Publisher</code> or <code>Subscriber</code> for a given topic instead.</p>

<p>This can be easily accomplished by replacing <code>NamedTopic&lt;T&gt;</code> in any of the examples above with either <code>Publisher&lt;T&gt;</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.SessionName;
import javax.inject.Inject;

@Inject
private Publisher&lt;Order&gt; orders;

@Inject
@Name("orders")
private Publisher&lt;Order&gt; m_orders;

@Inject
@SessionName("payments-cluster.xml")
private Publisher&lt;PaymentRequest&gt; payments;</markup>

<p>or <code>Subscriber&lt;T&gt;</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import com.oracle.coherence.cdi.SessionName;
import javax.inject.Inject;

@Inject
private Subscriber&lt;Order&gt; orders;

@Inject
@Name("orders")
private Subscriber&lt;Order&gt; m_orders;

@Inject
@SessionName("Finance")
private Subscriber&lt;PaymentRequest&gt; payments;</markup>

<p>Topic metadata, such as topic name (based on either injection point name or the explicit name from <code>@Name</code> annotation),
scope and message type, will be used under the hood to retrieve the <code>NamedTopic</code>, and to obtain <code>Publisher</code> or
<code>Subscriber</code> from it.</p>

<p>Additionally, if you want to place your <code>Subscriber`s into a subscriber group (effectively turning a topic into a
queue), you can easily accomplish that by adding `@SubscriberGroup</code> qualifier to the injection point:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.SubscriberGroup;
import javax.inject.Inject;

@Inject
@SubscriberGroup("orders-queue")
private Subscriber&lt;Order&gt; orders;</markup>

</div>

<h4 id="other-injection-points">Other Supported Injection Points</h4>
<div class="section">
<p>While the injection of a <code>NamedMap</code>, <code>NamedCache</code>, <code>NamedTopic</code>, and related instances, as shown above,  is probably
the single most used feature of Coherence CDI, it is certainly not the only one.
The following sections describe other Coherence artifacts that can be injected using Coherence CDI.</p>


<h5 id="inject-cluster"><code>Cluster</code> and <code>OperationalContext</code> Injection</h5>
<div class="section">
<p>If you need an instance of a <code>Cluster</code> interface somewhere in your application, you can easily obtain it via injection:</p>

<markup
lang="java"

>import com.tangosol.net.Cluster;
import javax.inject.Inject;

@Inject
private Cluster cluster;</markup>

<p>You can do the same if you need an instance of an <code>OperationalContext</code>:</p>

<markup
lang="java"

>import com.tangosol.net.OperationalContext;
import javax.inject.Inject;

@Inject
private OperationalContext ctx;</markup>

</div>

<h5 id="inject-ccf">Named <code>Session</code> Injection</h5>
<div class="section">
<p>On rare occasions when you need to use a <code>Session</code> directly, Coherence CDI makes it trivial to do so.</p>

<p>Coherence will create a default <code>Session</code> when the CDI server starts, this will be created using the normal default
cache configuration file.
Other named sessions can be configured as CDI beans of type <code>SessionConfiguration</code>.</p>

<p>For example:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.SessionInitializer;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MySession
        implements SessionInitializer
    {
    public String getName()
        {
        return "Foo";
        }
    // implement session configuration methods
    }</markup>

<p>The bean above will create the configuration for a <code>Session</code> named <code>Foo</code>. When the CDI server starts the session
will be created and can then be injected into other beans.</p>

<p>A simpler way to create a <code>SessionConfiguration</code> is to implement the <code>SessionIntializer</code> interface and annotate the class.
For example:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@ApplicationScoped
@Named("Foo")
@Scope("Foo")
@ConfigUri("my-coherence-config.xml")
public class MySession
        implements SessionInitializer
    {
    }</markup>

<p>The above configuration will create a <code>Session</code> bean with a name of <code>Foo</code> a scoep of <code>Foo</code> with an underlying
<code>ConfigurableCacheFactory</code> created from the <code>my-coherence-config.xml</code> configuration file.</p>

<p>To obtain an instance of the default <code>Session</code>, all you need to do is inject it into the
class which needs to use it:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import javax.inject.Inject;

@Inject
private Session session;</markup>

<p>If you need a specific named <code>Session</code> you can simply qualify one using <code>@Name</code> qualifier and
specifying the <code>Session</code> name:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
@Name("SessionOne")
private Session sessionOne;

@Inject
@Name("SessionTwo")
private Session sessionTwo;</markup>

</div>

<h5 id="inject-serializer"><code>Serializer</code> Injection</h5>
<div class="section">
<p>While in most cases you won&#8217;t have to deal with serializers directly, Coherence CDI makes it simple to obtain named
serializers (and to register new ones) when you need.</p>

<p>To get a default <code>Serializer</code> for the current context class loader, you can simply inject it:</p>

<markup
lang="java"

>import com.tangosol.io.Serializer;
import javax.inject.Inject;

@Inject
private Serializer defaultSerializer;</markup>

<p>However, it may be more useful to inject one of the named serializers defined in the operational configuration, which
can be easily accomplished using <code>@Name</code> qualifier:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
@Name("java")
private Serializer javaSerializer;

@Inject
@Name("pof")
private Serializer pofSerializer;</markup>

<p>In addition to the serializers defined in the operational config, the example above will also perform <code>BeanManager</code>
lookup for a named bean that implements <code>Serializer</code> interface.</p>

<p>That means that if you implemented a custom <code>Serializer</code> bean, such as:</p>

<markup
lang="java"

>import com.tangosol.io.Serializer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

@Named("json")
@ApplicationScoped
public class JsonSerializer implements Serializer {
    ...
}</markup>

<p>it would be automatically discovered and registered by the CDI, and you would then be able to inject it just as easily
as the named serializers defined in the operational config:</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
@Name("json")
private Serializer jsonSerializer;</markup>

</div>

<h5 id="_inject_a_pof_serializer_with_a_specific_pof_configuration">Inject a POF <code>Serializer</code> With a Specific POF Configuration</h5>
<div class="section">
<p>POF serializers can be injected by using both the <code>@Name</code> and <code>@ConfigUri</code> qualifiers to inject a POF serializer
which uses a specific POF configuration file.</p>

<markup
lang="java"

>import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Name;
import javax.inject.Inject;

@Inject
@Name("pof")
@ConfigUri("test-pof-config.xml")
private Serializer pofSerializer;</markup>

<p>The code above will inject a POF serializer that uses <code>test-pof-config.xml</code> as its configuration file.</p>

</div>
</div>
</div>

<h3 id="inject-into-coherence">Injecting CDI Beans into Coherence-managed Objects</h3>
<div class="section">
<p>Coherence has a number of server-side extension points, which allow users to customize application  behavior in
different ways, typically by configuring their extensions within various sections of the  cache configuration file.
For example, the users can implement event interceptors and cache stores,  in order to handle server-side events and
integrate with the external data stores and other services.</p>

<p>Coherence CDI provides a way to inject named CDI beans into these extension points using custom  configuration
namespace handler.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
        xmlns:cdi="class://com.oracle.coherence.cdi.server.CdiNamespaceHandler"
        xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;</markup>

<p>Once you&#8217;ve declared the handler for the <code>cdi</code> namespace above, you can specify <code>&lt;cdi:bean&gt;</code> element in any place
where you would normally use <code>&lt;class-name&gt;</code> or <code>&lt;class-factory-name&gt;</code> elements:</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;

&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
        xmlns:cdi="class://com.oracle.coherence.cdi.server.CdiNamespaceHandler"
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

<p>Note that you can only inject <em>named</em> CDI beans (beans with an explicit <code>@Named</code> annotations) via  <code>&lt;cdi:bean&gt;</code> element.
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
For example, because we&#8217;ve used a wildcard, <code>*</code>, as a cache name within the cache mapping in the example above, the same
instance of <code>cacheListener</code> will receive events from multiple caches.</p>

<p>This is typically fine, as the event itself provides the details about the context that raised it, including cache name,
and the service it was raised from, but it does imply that any shared state that you may have within your listener class
shouldn&#8217;t be context-specific, and it must be safe for concurrent access from multiple threads.
If you can&#8217;t guarantee the latter, you may want to declare the <code>onEvent</code> method as <code>synchronized</code>, to ensure only one
thread at a time can access any shared state you may have.</p>


<h4 id="cdi-events">Using CDI Observers to Handle Coherence Server-Side Events</h4>
<div class="section">
<p>While the above examples show that you can implement any Coherence <code>EventInterceptor</code> as a CDI bean and register it
using <code>&lt;cdi:bean&gt;</code> element within the cache configuration file, Coherence CDI  also provides a much simpler way to
accomplish the same goal using standard CDI Events and Observers.</p>

<p>For example, to observe events raised by a <code>NamedMap</code> with the name <code>people</code>, with keys of type <code>Long</code> and values of
type
<code>Person</code>, you would define a CDI observer such as this one:</p>

<markup
lang="java"

>private void onMapChange(@Observes @MapName("people") EntryEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>


<h5 id="cdi-event-types">Observe Specific Event Types</h5>
<div class="section">
<p>The observer method above will receive all events for the <code>people</code> map, but you can also control the types of events
received using event qualifiers:</p>

<markup
lang="java"

>private void onUpdate(@Observes @Updated @MapName("people") EntryEvent&lt;Long, Person&gt; event) {
    // handle UPDATED events raised by the 'people' map/cache
}

private void onChange(@Observes @Inserted @Updated @Removed @MapName("people") EntryEvent&lt;?, ?&gt; event) {
    // handle INSERTED, UPDATED and REMOVED events raised by the 'people' map/cache
}</markup>

</div>

<h5 id="cdi-events-filter">Filter Observed Events</h5>
<div class="section">
<p>The events observed can be restricted further by using a Coherence <code>Filter</code>.
If a filter has been specified, the events will be filtered on the server and will never be sent to the client.
The filter that will be used is specified using a qualifier annotation that is itself annotated with <code>@FilterBinding</code>.</p>

<p>You can implement a <router-link to="#filter-bindings" @click.native="this.scrollFix('#filter-bindings')">Custom FilterBinding</router-link> (recommended), or use a built-in <code>@WhereFilter</code> for
convenience, which allows you to specify a filter using CohQL.</p>

<p>For example to receive all event types in the <code>people</code> map, but only for <code>People</code> with a <code>lastName</code> property value of
<code>Smith</code>, the built-in <code>@WhereFilter</code> annotation can be used:</p>

<markup
lang="java"

>@WhereFilter("lastName = 'Smith'")
private void onMapChange(@Observes @MapName("people") EntryEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

</div>

<h5 id="cdi-events-transform">Transform Observed Events</h5>
<div class="section">
<p>When an event observer does not want to receive the full cache or map value in an event, the event can be transformed
into a different value to be observed. This is achieved using a <code>MapEventTransformer</code> that is applied to the observer
method using either an <code>ExtractorBinding</code> annotation or a <code>MapEventTransformerBinding</code> annotation.
Transformation of events happens on the server so can make observer&#8217;s more efficient as they do not need to receive
the original event with the full old and new values.</p>

<p><strong>Transforming Events Using ExtractorBinding Annotations</strong></p>

<p>An <code>ExtractorBinding</code> annotation is an annotation that represents a Coherence <code>ValueExtractor</code>.
When an observer method has been annotated with an <code>ExtractorBinding</code> annotation the resulting <code>ValueExtractor</code> is
applied to the event&#8217;s values and a new event will be returned to the observer containing just the extracted
properties.</p>

<p>For example, an event observer that is observing events from a map named <code>people</code>, but only requires the last name,
the built in <code>@PropertyExtractor</code> annotation can be used.</p>

<markup
lang="java"

>@PropertyExtractor("lastName")
private void onMapChange(@Observes @MapName("people") EntryEvent&lt;Long, String&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

<p>Unlike the previous examples above the received events of type <code>EntryEvent&lt;Long, Person&gt;</code> this method will receive
events of type <code>EntryEvent&lt;Long, String&gt;</code> because the property extractor will be applied to the <code>Person</code>
values in the original event to extract just the <code>lastName</code> property, creating a new event with <code>String</code> values.</p>

<p>There are a number of built in <code>ExtractorBinding</code> annotations, and it is also possible to create custom
<code>ExtractorBinding</code> annotation - see the <router-link to="#custom-extractor" @click.native="this.scrollFix('#custom-extractor')">Custom ExtractorBinding Annotations</router-link> section below.</p>

<p>Multiple extractor binding annotations can be added to an injection point, in which case multiple properties will be
extracted, and the event will contain a <code>List</code> of the extracted property values.</p>

<p>For example, if the <code>Person</code> also contains an <code>address</code> field of type <code>Address</code> that contains a <code>city</code> field, this
can be extracted with a <code>@ChainedExtractor</code> annotation. By combining this with the <code>@PropertyExtractor</code> in the
example above both the <code>lastName</code> and <code>city</code> can be observed in the event.</p>

<markup
lang="java"

>@PropertyExtractor("lastName")
@ChainedExtractor({"address", "city"})
private void onMapChange(@Observes @MapName("people") EntryEvent&lt;Long, List&lt;String&gt;&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

<p>Note, now the event is of type <code>EntryEvent&lt;Long, List&lt;String&gt;&gt;</code> because multiple extracted values will be returned the
event value is a <code>List</code> and in this case both properties are of tyep <code>String</code>, so the value can be <code>List&lt;String&gt;</code>.</p>

<p><strong>Transforming Events Using MapEventTransformerBinding Annotations</strong></p>

<p>If more complex event transformations are required than just extracting properties from event values, a custom
<code>MapEventTransformerBinding</code> can be created that will produce a custom <code>MapEventTransformer</code> instance that will be
applied to the observer&#8217;s events.
See the <router-link to="#custom-transformer" @click.native="this.scrollFix('#custom-transformer')">Custom MapEventTransformerBinding Annotations</router-link> section below for details on how to create
<code>MapEventTransformerBinding</code> annotations.</p>

</div>

<h5 id="cdi-events-scopes">Observe Events for Maps and Caches in Specific Services and Scopes</h5>
<div class="section">
<p>In addition, to the <code>@MapName</code> qualifier, you can also use <code>@ServiceName</code> and <code>@ScopeName</code> qualifiers as a way to limit
the events received.</p>

<p>The examples above show only how to handle <code>EntryEvent</code>s, but the same applies to all other server-side event types:</p>

<markup
lang="java"

>private void onActivated(@Observes @Activated LifecycleEvent event) {
    // handle cache factory activation
}

private void onCreatedPeople(@Observes @Created @MapName("people") CacheLifecycleEvent event) {
    // handle creation of the 'people' map/cache
}

private void onExecuted(@Observes @Executed @MapName("people") @Processor(Uppercase.class) EntryProcessorEvent event) {
    // intercept 'Uppercase` entry processor execution against 'people' map/cache
}</markup>

</div>

<h5 id="cdi-events-async">Using Asynchronous Observers</h5>
<div class="section">
<p>All the examples above used synchronous observers by specifying <code>@Observes</code> qualifier for each observer method.
However, Coherence CDI fully supports asynchronous CDI observers as well.
All you need to do is replace <code>@Observes</code> with <code>@ObservesAsync</code> in any of the examples above.</p>

<markup
lang="java"

>private void onActivated(@ObservesAsync @Activated LifecycleEvent event) {
    // handle cache factory activation
}

private void onCreatedPeople(@ObservesAsync @Created @MapName("people") CacheLifecycleEvent event) {
    // handle creation of the 'people' map/cache
}

private void onExecuted(@ObservesAsync @Executed @MapName("people") @Processor(Uppercase.class) EntryProcessorEvent event) {
    // intercept 'Uppercase` entry processor execution against 'people', map/cache
}</markup>

<div class="admonition warning">
<p class="admonition-textlabel">Warning</p>
<p ><p>Coherence events fall into two categories: pre- and post-commit events.
All the events whose name ends  with <code>"ing"</code>, such as <code>Inserting</code>, <code>Updating</code>, <code>Removing</code> or <code>Executing</code> are
pre-commit, which means that they can either modify the data or even veto the operation by throwing an exception,
but in  order to do so they must be synchronous to ensure that they are executed on the same thread that is
executing the operation that triggered the event.</p>

<p>That means that you can <em>observe</em> them using asynchronous CDI observers, but if you want to mutate the set of
entries that are part of the event payload, or veto the event by throwing an exception, you must use synchronous
CDI observer.</p>
</p>
</div>
</div>
</div>
</div>

<h3 id="inject-transient">Injecting CDI Beans into Transient Objects</h3>
<div class="section">
<p>Using CDI to inject Coherence objects into your application classes, and CDI beans into Coherence-managed objects will
allow you to support many use cases where dependency injection may be useful, but it doesn&#8217;t cover an important use
case that is somewhat specific to Coherence.</p>

<p>Coherence is a distributed system, and it uses serialization in order to send both the data and the  processing requests
from one cluster member (or remote client) to another, as well as to store data, both in memory and on disk.</p>

<p>Processing requests, such as entry processors and aggregators, have to be deserialized on a target cluster member(s) in
order to be executed. In some cases, they could benefit from dependency injection in order to avoid service lookups.</p>

<p>Similarly, while the data is stored in a serialized, binary format, it may need to be deserialized into user supplied
classes for server-side processing, such as when executing entry processors and aggregators. In this case, data classes
can often also benefit from dependency injection (in order to support Domain-Driven Design (DDD), for example).</p>

<p>While these transient objects are not managed by the CDI container, Coherence CDI does support their injection during
deserialization, but for performance reasons requires that you explicitly opt-in by implementing
<code>com.oracle.coherence.cdi.Injectable</code> interface.</p>


<h4 id="transient-injectable">Making transient classes <code>Injectable</code></h4>
<div class="section">
<p>While not technically a true marker interface, <code>Injectable</code> can be treated as such for all intents and purposes.
All you need to do is add it to the <code>implements</code> clause of your class in order for injection on deserialization to
kick in:</p>

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

<p>Assuming that you have the following <code>Converter</code> service implementation in your application, it will be injected
into <code>InjectableBean</code> during deserialization, and the <code>getConvertedText</code> method will return the value of the <code>text</code>
field converted to upper case:</p>

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
<p>You should note that the above functionality is not dependent on the serialization format and will work with both
Java and POF serialization (or any other custom serializer), and for any object that is  deserialized on any Coherence
member (or even on a remote client).</p>

<p>While the deserialized transient objects are not true CDI managed beans, being able to inject CDI managed dependencies
into them upon deserialization will likely satisfy most dependency injection requirements you will ever have in those
application components.
We hope you&#8217;ll find it useful.</p>

</div>
</div>

<h3 id="filter-bindings">FilterBinding Annotations</h3>
<div class="section">
<p>As already mentioned above, when creating views or subscribing to events, the view or events can be modified using
<code>Filters</code>.
The exact <code>Filter</code> implementation injected will be determined by the view or event observers qualifiers.
Specifically any qualifier annotation that is itself annotated with the <code>@FilterBinding</code> annotation.
This should be a familiar pattern to anyone who has worked with CDI interceptors.</p>

<p>For example, if there is an injection point for a view that is a filtered view of an underlying map, but the filter
required
is more complex than those provided by the build in qualifiers, or is some custom filter implementation.
The steps required are:</p>

<ul class="ulist">
<li>
<p>Create a custom annotation class to represent the required <code>Filter</code>.</p>

</li>
<li>
<p>Create a bean class implementing <code>com.oracle.coherence.cdi.FilterFactory</code> annotated with the custom annotation that
will be the factory for producing instances of the custom <code>Filter</code>.</p>

</li>
<li>
<p>Annotate the view injection point with the custom annotation.</p>

</li>
</ul>

<h4 id="_create_the_custom_filter_annotation">Create the Custom Filter Annotation</h4>
<div class="section">
<p>Creating the filter annotation is simply creating a normal Java annotation class that is annotated with
the <code>@com.oracle.coherence.cdi.FilterBinding</code> annotation.</p>

<markup
lang="java"

>@Inherited
@FilterBinding  <span class="conum" data-value="1" />
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomFilter {
}</markup>

<ul class="colist">
<li data-value="1">The most important part is that this new annotation is annotated with <code>FilterBinding</code> so that the Coherence CDI
extensions can recognise that it represents a <code>Filter</code>.</li>
</ul>
</div>

<h4 id="_create_the_custom_filter_factory">Create the Custom Filter Factory</h4>
<div class="section">
<p>Once the custom annotation has been created a <code>FilterFactories</code> implementation can be created that will be responsible
for producing instances of the required <code>Filter</code>.</p>

<markup
lang="java"

>@ApplicationScoped    <span class="conum" data-value="1" />
@CustomFilter         <span class="conum" data-value="2" />
static class CustomFilterSupplier
        implements FilterFactory&lt;CustomFilter, Object&gt;
    {
    @Override
    public Filter&lt;Object&gt; create(CustomFilter annotation)
        {
        return new CustomComplexFilter(); <span class="conum" data-value="3" />
        }
    }</markup>

<ul class="colist">
<li data-value="1">The <code>CustomFilterSupplier</code> class has been annotated with <code>@ApplicationScoped</code> to make is discoverable by CDI.</li>
<li data-value="2">The <code>CustomFilterSupplier</code> class has been annotated with the new filter binding annotation <code>@CustomFilter</code>
so that the Coherence CDI extension can locate it when it needs to create <code>Filters</code>.</li>
<li data-value="3">The <code>CustomFilterSupplier</code> implements the <code>FilterFactories</code> interface&#8217;s <code>create</code> method where it creates the
custom <code>Filter</code> implementation.</li>
</ul>
</div>

<h4 id="_annotate_the_injection_point">Annotate the Injection Point</h4>
<div class="section">
<p>Now there is both a custom annotation, and an annotated <code>FilterFactories</code>, the injection point requiring the <code>Filter</code>
can be annotated with the new annotation.</p>

<markup
lang="java"

>@Inject
@View
@CustomFilter
private NamedMap&lt;Long, Person&gt; people;</markup>

<p>As well as views, custom filter binding annotations can also be used for event observers.
For example if there is an event observer method that should only receive events matching the same custom <code>Filter</code>
then the method can be annotated with the same custom filter annotation.</p>

<markup
lang="java"

>@CustomFilter
private void onPerson(@Observes @MapName("people") EntryEvent&lt;Long, Person&gt; event) {</markup>

</div>
</div>

<h3 id="extractor-binding">ExtractorBinding Annotations</h3>
<div class="section">
<p>Extractor bindings are annotations that are themselves annotated with <code>@ExtractorBinding</code> and are used in conjunction
with an implementation of <code>com.oracle.coherence.cdi.ExtractorFactory</code> to produce Coherence <code>ValueExtractor</code> instances.</p>

<p>There are a number of built-in extractor binding annotations in the Coherence CDI module and it is a simple process
to provide custom implementations.</p>


<h4 id="extractor-binding-built-in">Built-In ExtractorBinding Annotations</h4>
<div class="section">

<h5 id="extractor-binding-property">PropertyExtractor</h5>
<div class="section">
<p>The <code>@PropertyExtractor</code> annotation can used to obtain an extractor that extracts a named property from an object.
The value field of the <code>@PropertyExtractor</code> annotation is name of the property to extract.</p>

<p>For example, this <code>@PropertyExtractor</code> annotation represents a <code>ValueExtractor</code> that will extract the <code>lastName</code>
property from a value.</p>

<markup
lang="java"

>@PropertyExtractor("lastName")</markup>

<p>The extractor produced will be an instance of <code>com.tangosol.util.extractor.UniversalExtractor</code>,
so the example above is the same as calling:</p>

<markup
lang="java"

>new UniversalExtractor("lastName");</markup>

<p>The <code>@PropertyExtractor</code> annotation can be applied multiple times to create a <code>MultiExtractor</code> that will extract
a <code>List</code> of properties from a value.</p>

<p>For example, if there was a map named <code>people</code>, where the map values are instances of <code>Person</code>, that has a <code>firstName</code>
and a <code>lastName</code> property. The event observer below would observe events on that map, but the event received would only
contain the event key, and a <code>List</code> containing the extracted <code>firstName</code> and <code>lastName</code> from the original event.
where the event values will be a list of</p>

<markup
lang="java"

>@PropertyExtractor("firstName")
@PropertyExtractor("lastName")
private void onPerson(@Observes @MapName("people") EntryEvent&lt;Long, List&lt;String&gt;&gt; event) {</markup>

</div>

<h5 id="extractor-binding-chained">ChainedExtractor</h5>
<div class="section">
<p>The <code>@ChainedExtractor</code> annotation can be used to extract a chain of properties.</p>

<p>For example, a <code>Person</code> instance might contain an <code>address</code> property that contains a <code>city</code> property.
The <code>@ChainedExtractor</code> takes the chain of fields to be extracted, in this case, extract the <code>address</code> from <code>Person</code>
and then extract the <code>city</code> from the <code>address</code>.</p>

<markup
lang="java"

>@ChainedExtractor("address", "city")</markup>

<p>Each of the property names is used to create a <code>UniversalExtractor</code>, and the array of these extractors is used to
create an instance of <code>com.tangosol.util.extractor.ChainedExtractor</code>.</p>

<p>The example above would be the same as calling:</p>

<markup
lang="java"

>UniversalExtractor[] chain = new UniversalExtractor[] {
        new UniversalExtractor("address"),
        new UniversalExtractor("city")
};
ChainedExtractor extractor = new ChainedExtractor(chain);</markup>

</div>

<h5 id="extractor-binding-pof">PofExtractor</h5>
<div class="section">
<p>The <code>@PofExtractor</code> annotation can be used to produce extractors that can extract properties from POF encoded values.
The value passed to the <code>@PofExtractor</code> annotation is the POF path to navigate to the property to extract.</p>

<p>For example, if a <code>Person</code> value has been serialized using POF with a <code>lastName</code> property at index <code>4</code> a <code>@PofExtractor</code>
annotation could be used like this:</p>

<markup
lang="java"

>@PofExtractor(index = 4)</markup>

<p>The code above will create a Coherence <code>com.tangosol.util.extractor.PofExtractor</code> equivalent to calling:</p>

<markup
lang="java"

>com.tangosol.util.extractor.PofExtractor(null, 4);</markup>

<p>Sometimes (for example when dealing with certain types of <code>Number</code>) the <code>PofExtractor</code> needs to know they type to be
extracted. In this case the <code>type</code> value can be set in the <code>@PofExtractor</code> annotation.</p>

<p>For example, if a <code>Book</code> value had a <code>sales</code> field of type <code>Long</code> at POF index 2, the <code>sales</code> field could be
extracted using the following <code>@PofExtractor</code> annotation:</p>

<markup
lang="java"

>@PofExtractor(index = {2}, type = Long.class)</markup>

<p>The code above will create a Coherence <code>com.tangosol.util.extractor.PofExtractor</code> equivalent to calling:</p>

<markup
lang="java"

>com.tangosol.util.extractor.PofExtractor(Long.class, 2);</markup>

<p>The <code>index</code> value for a <code>@PofExtractor</code> annotation is an int array so multiple POF index values can be passed to navigate
down a chain of properties to extract. For example if <code>Person</code> contained an <code>Address</code> at POF index <code>5</code> and <code>Address</code>
contained a <code>city</code> property at POF index <code>3</code> the <code>city</code> could be extracted from a <code>Person</code> using the <code>@PofExtractor</code>
annotation like this:</p>

<markup
lang="java"

>@PofExtractor(index = {5, 3})</markup>

<p>Alternatively if the value that will be extracted from is annotated with <code>com.tangosol.io.pof.schema.annotation.PortableType</code>
and the POF serialization code for the class has been generated using the Coherence
<code>com.tangosol.io.pof.generator.PortableTypeGenerator</code> then property names can be passed to the <code>@PofExtractor</code> annotation
using its <code>path</code> field.</p>

<p>For example to extract the <code>lastName</code> field from a POF serialized <code>Person</code> the <code>@PofExtractor</code> annotation can be
used like this:</p>

<markup
lang="java"

>@PofExtractor(path = "lastName")</markup>

<p>the <code>address</code> <code>city</code> example would be:</p>

<markup
lang="java"

>@PofExtractor(path = {"address", "city"})</markup>

<p>and the <code>Book</code> <code>sales</code> example would be:</p>

<markup
lang="java"

>@PofExtractor(path = "sales", type Long.class)</markup>

</div>
</div>

<h4 id="extractor-binding-custom">Custom ExtractorBinding Annotations</h4>
<div class="section">
<p>When the built-in extractor bindings are not suitable, or when a custom <code>ValueExtractor</code> implementation is required,
then a custom extractor binding annotation can be created with a corresponding <code>com.oracle.coherence.cdi.ExtractorFactory</code>
implementation.
The steps required are:</p>

<ul class="ulist">
<li>
<p>Create a custom annotation class to represent the required <code>ValueExtractor</code>.</p>

</li>
<li>
<p>Create a bean class implementing <code>com.oracle.coherence.cdi.ExtractorFactory</code> annotated with the custom annotation that
will be the factory for producing instances of the custom <code>ValueExtractor</code>.</p>

</li>
<li>
<p>Annotate the view injection point with the custom annotation.</p>

</li>
</ul>
</div>

<h4 id="_create_the_custom_extractor_annotation">Create the Custom Extractor Annotation</h4>
<div class="section">
<p>Creating the extractor annotation is simply creating a normal Java annotation class which is annotated with
the <code>@com.oracle.coherence.cdi.ExtractorBinding</code> annotation.</p>

<markup
lang="java"

>@Inherited
@ExtractorBinding  <span class="conum" data-value="1" />
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomExtractor {
}</markup>

<ul class="colist">
<li data-value="1">The most important part is that this new annotation has been annotated with <code>ExtractorBinding</code> so that the
Coherence CDI extensions can recognise that it represents a <code>ValueExtractor</code>.</li>
</ul>
</div>

<h4 id="_create_the_custom_extractor_factory">Create the Custom Extractor Factory</h4>
<div class="section">
<p>Once the custom annotation has been created an <code>ExtractorFactory</code> implementation can be created that will be responsible
for producing instances of the required <code>ValueExtractor</code>.</p>

<markup
lang="java"

>@ApplicationScoped    <span class="conum" data-value="1" />
@CustomExtractor      <span class="conum" data-value="2" />
static class CustomExtractorSupplier
        implements ExtractorFactory&lt;CustomExtractor, Object, Object&gt;
    {
    @Override
    public ValueExtractor&lt;Object, Object&gt; create(CustomExtractor annotation)
        {
        return new CustomComplexExtractor(); <span class="conum" data-value="3" />
        }
    }</markup>

<ul class="colist">
<li data-value="1">The <code>CustomExtractorSupplier</code> class has been annotated with <code>@ApplicationScoped</code> to make is discoverable by CDI.</li>
<li data-value="2">The <code>CustomExtractorSupplier</code> class has been annotated with the new extractor binding annotation <code>@CustomExtractor</code>
so that the Coherence CDI extension can locate it when it needs to create <code>ValueExtractor</code> instances.</li>
<li data-value="3">The <code>CustomExtractorSupplier</code> implements the <code>ExtractorFactory</code> interface&#8217;s <code>create</code> method where it creates the
custom <code>ValueExtractor</code> implementation.</li>
</ul>
</div>

<h4 id="_annotate_the_injection_point_2">Annotate the Injection Point</h4>
<div class="section">
<p>Now there is both a custom annotation, and an annotated <code>ExtractorFactory</code>, the injection point requiring the
<code>ValueExtractor</code> can be annotated with the new annotation.</p>

<markup
lang="java"

>@Inject
@View
@CustomExtractor
private NamedMap&lt;Long, String&gt; people;</markup>

<p>As well as views, custom filter binding annotations can also be used for event observers.
For example if there is an event observer method that should only receive transformed events using the custom extractor
to transform the event:</p>

<markup
lang="java"

>@CustomExtractor
private void onPerson(@Observes @MapName("people") EntryEvent&lt;Long, String&gt; event) {</markup>

</div>
</div>

<h3 id="transformer-binding">MapEventTransformerBinding Annotations</h3>
<div class="section">
<p>Coherence CDI supports event observers that can observe events for cache, or map, entries
(see the <router-link to="#cdi-events" @click.native="this.scrollFix('#cdi-events')">Events</router-link> section). The observer method can be annotated with a <code>MapEventTransformerBinding</code>
annotation to indicate that the observer requires a transformer to be applied to the original event before it is observed.</p>

<p>There are no built-in <code>MapEventTransformerBinding</code> annotations, this feature is to support use of custom
<code>MapEventTransformer</code> implementations.</p>

<p>The steps to create and use a <code>MapEventTransformerBinding</code> annotation are:</p>

<ul class="ulist">
<li>
<p>Create a custom annotation class to represent the required <code>MapEventTransformer</code>.</p>

</li>
<li>
<p>Create a bean class implementing <code>com.oracle.coherence.cdi.MapEventTransformerFactory</code> annotated with the custom
annotation that will be the factory for producing instances of the custom <code>MapEventTransformer</code>.</p>

</li>
<li>
<p>Annotate the view injection point with the custom annotation.</p>

</li>
</ul>

<h4 id="_create_the_custom_extractor_annotation_2">Create the Custom Extractor Annotation</h4>
<div class="section">
<p>Creating the extractor annotation is simply creating a normal Java annotation class which is annotated with
the <code>@com.oracle.coherence.cdi.MapEventTransformerBinding</code> annotation.</p>

<markup
lang="java"

>@Inherited
@MapEventTransformerBinding  <span class="conum" data-value="1" />
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomTransformer {
}</markup>

<ul class="colist">
<li data-value="1">The most important part is that this new annotation has been annotated with <code>MapEventTransformerBinding</code> so that the
Coherence CDI extensions can recognise that it represents a <code>MapEventTransformer</code>.</li>
</ul>
</div>

<h4 id="_create_the_custom_extractor_factory_2">Create the Custom Extractor Factory</h4>
<div class="section">
<p>Once the custom annotation has been created an <code>MapEventTransformerFactory</code> implementation can be created that will be
responsible for producing instances of the required <code>MapEventTransformer</code>.</p>

<markup
lang="java"

>@ApplicationScoped      <span class="conum" data-value="1" />
@CustomTransformer      <span class="conum" data-value="2" />
static class CustomTransformerSupplier
        implements MapEventTransformerFactory&lt;CustomTransformer, Object, Object, Object&gt;
    {
    @Override
    public MapEventTransformer&lt;Object, Object, Object&gt; create(CustomTransformer annotation)
        {
        return new CustomComplexTransformer(); <span class="conum" data-value="3" />
        }
    }</markup>

<ul class="colist">
<li data-value="1">The <code>CustomTransformerSupplier</code> class has been annotated with <code>@ApplicationScoped</code> to make is discoverable by CDI.</li>
<li data-value="2">The <code>CustomTransformerSupplier</code> class has been annotated with the new extractor binding annotation <code>@CustomTransformer</code>
so that the Coherence CDI extension can locate it when it needs to create <code>MapEventTransformer</code> instances.</li>
<li data-value="3">The <code>CustomTransformerSupplier</code> implements the <code>MapEventTransformerFactory</code> interface&#8217;s <code>create</code> method where it
creates the custom <code>MapEventTransformer</code> implementation.</li>
</ul>
</div>

<h4 id="_annotate_the_injection_point_3">Annotate the Injection Point</h4>
<div class="section">
<p>Now there is both a custom annotation, and an annotated <code>MapEventTransformerFactory</code>, the observer method
requiring the <code>MapEventTransformer</code> can be annotated with the new annotation.</p>

<markup
lang="java"

>@CustomTransformer
private void onPerson(@Observes @MapName("people") EntryEvent&lt;Long, String&gt; event) {</markup>

</div>
</div>
</div>
</doc-view>
