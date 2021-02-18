<doc-view>

<h2 id="_bootstrap_api">Bootstrap API</h2>
<div class="section">
<p>Coherence has a simple bootstrap API that allows a Coherence application to be configured and started by
building a <code>com.tangol.net.Coherence</code> instance and starting it.
The <code>Coherence</code> instance provides access to one or more <code>com.tangosol.net.Session</code> instances.
A <code>com.tangosol.net.Session</code> gives access to Coherence clustered resources, such as <code>NamedMap</code>, <code>NamedCache</code>,
<code>NamedTopic</code> etc.
Sessions can be of different types, for example a session can be related to a <code>ConfigurableCacheFactory</code>,
itself configured from a configuration file, or a session might be a client-side gRPC session.</p>

<p>An example of some application bootstrap code might look like this:</p>

<markup
lang="java"

>import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.SessionConfiguration;

public class Main
    {
    public static void main(String[] args)
        {
        // Create a Session configuration                                <span class="conum" data-value="1" />
        SessionConfiguration session = SessionConfiguration.builder()
                .named("Carts")
                .withConfigUri("cache-config.xml")
                .build();

        // Create a Coherence instance configuration                     <span class="conum" data-value="2" />
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.defaultSession())
                .withSession(session)
                .build();

        // Create the Coherence instance from the configuration          <span class="conum" data-value="3" />
        Coherence coherence = Coherence.clusterMember(cfg);

        // Start Coherence                                               <span class="conum" data-value="4" />
        coherence.start();
        }
    }</markup>

<ul class="colist">
<li data-value="1">A <code>SessionConfiguration</code> is created. In this case the <code>Session</code> will be named <code>Carts</code> and will be created
from the <code>cache-config.xml</code> configuration file.</li>
<li data-value="2">A <code>CoherenceConfiguration</code> is created to configure the <code>Coherence</code> instance. This configuration contains
the <code>Carts</code> session configuration.</li>
<li data-value="3">A <code>Coherence</code> cluster member instance is created from the <code>CoherenceConfiguration</code></li>
<li data-value="4">The <code>Coherence</code> instance is started.</li>
</ul>

<h3 id="_running_a_coherence_server">Running A Coherence Server</h3>
<div class="section">
<p>The <code>com.tangol.net.Coherence</code> contains a <code>main</code> method that allows it to be used to run a Coherence server as a
more powerful to alternative <code>DefaultCahceServer</code>.</p>

<markup
lang="bash"

>$ java -cp coherence.jar com.tangosol.net.Coherence</markup>

<p>Without any other configuration, the default <code>Coherence</code> instance started this way will run an identical server
to that run using <code>DefaultCahceServer</code>.</p>

<p>The steps above are covered in more detail below.</p>

</div>

<h3 id="_session_configurations">Session Configurations</h3>
<div class="section">
<p>A <code>SessionConfiguration</code> is created by using the <code>SessionConfiguration</code> builder as shown in the example above.</p>


<h4 id="_the_default_session">The Default Session</h4>
<div class="section">
<p>When running Coherence if no configuration is specified the default behaviour is to use the default configuration file
to configure Coherence. This behaviour still applies to the bootstrap API. If a <code>Coherence</code> instance is started without
specifying any session configurations then a single default <code>Session</code> will be created.
This default <code>Session</code> will wrap a <code>ConfigurableCacheFactory</code> that has been created from the default configuration file.
The default file name is <code>coherence-cache-config.xml</code> unless this has been overridden with the <code>coherence.cacheconfig</code>
system property.</p>

<p>When creating a <code>CoherenceConfiguration</code> the default session can be added using the <code>SessionConfiguration.defaultSession()</code>
helper method. This method returns a <code>SessionConfiguration</code> configured to create the default <code>Session</code>.</p>

<p>For example, in the code below the default session configuration is specifically added to the <code>CoherenceConfiguration</code>.</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .withSession(SessionConfiguration.defaultSession())
        .build();</markup>

</div>

<h4 id="_session_name">Session Name</h4>
<div class="section">
<p>All sessions have a name that must be unique within the application. If a name has not been specified when the
<code>SessionConfiguration</code> is built the default name of <code>$Default$</code> will be used. A <code>Coherence</code> instance will fail to start
if duplicate <code>Session</code> names exist.</p>

<p>For example, this configuration will have the default name.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .build();</markup>

<p>This configuration will have the name <code>Test</code>.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .name("Test")
        .build();</markup>

</div>

<h4 id="_session_configuration_uri">Session Configuration URI</h4>
<div class="section">
<p>The most common type of session is a wrapper around a <code>ConfigurableCacheFactory</code>.
When using the <code>SessionConfiguration</code> builder the configuration file URI is specified using the <code>withConfigUri()</code>
method, that takes a string value specifiying the configuration file location.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .withConfigUri("cache-config.xml")
        .build();</markup>

<p>The example above uses a configuration file a named <code>cache-config.xml</code>.</p>

<p>If a configuration URI is not specified then the default value will be used. This value is <code>coherence-cache-config.xml</code>
unless this has been overridden with the <code>coherence.cacheconfig</code> System property.</p>

</div>

<h4 id="_session_event_interceptors">Session Event Interceptors</h4>
<div class="section">
<p>Coherence provides many types of events, examples of a few would be life-cycle events for Coherence itself,
cache life-cycle events, cache entry events, partition events etc.
These events can be listened to by implementing an <code>EventInterceptor</code> that receives specific types of event.
Event interceptors can be registered with a <code>Session</code> as part of its configuration.</p>

<p>For example, suppose there is an interceptor class in the application called <code>CacheInterceptor</code> that listens to
<code>CacheLifecycleEvent</code> when caches get created or destroyed. This interceptor can be added to the session as shown
below:</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .withInterceptor(new CacheInterceptor())
        .build();</markup>

<p>The interceptor will receive cache life-cycle events for all caches created using the session.</p>

</div>

<h4 id="_session_scope">Session Scope</h4>
<div class="section">
<p>Scope is a concept that has been in Coherence for quite a while that allows services to be scoped and hence isolated
from other services with the same name. For example multiple <code>ConfigurableCacheFactory</code> instances could be loaded
from the same XML configuration file but given different scope names so that each CCF will have its own services
in the cluster.</p>

<p>Unless you require multiple Sessions, a scope will not generally be used in a configuration.</p>

<p>A scope for a session can be configured using the configuration&#8217;s <code>withScopeName()</code> method, for example:</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .withScopeName("Test")
        .build();</markup>

<p>The session (and any <code>ConfigurableCacheFactory</code> it wraps) created from the configuration above will have a scope name
of <code>Test</code>.</p>

<p>It is possible to set a scope name in the <code>&lt;defaults&gt;</code> section of the XML configuration file.</p>

<markup
lang="xml"
title="scoped-configuration.xml"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;defaults&gt;
    &lt;scope-name&gt;Test&lt;/scope-name&gt;
  &lt;/defaults&gt;</markup>

<p>A <code>ConfigurableCacheFactory</code> created from the XML above, and hence any <code>Session</code> that wraps it will have a scope
of <code>Test</code>.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>When using the bootstrap API any scope name specifically configured in the <code>SessionConfiguration</code>
(that is not the default scope name) will override the scope name in the XML file.</p>

<p>For example, using the <code>scoped-configuration.xml</code> file above:</p>

<p>In this case the scope name will be <code>Foo</code> because the scope name has been explicitly set in the <code>SessionConfiguration</code>.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .withConfigUri("scoped-configuration.xml")
        .withScopeName("Foo")
        .build();</markup>

<p>In this case the scope name will be <code>Foo</code> because although no scope name has been explicitly set in
the <code>SessionConfiguration</code>, the name has been set to <code>Foo</code>, so the scope name will default to <code>Foo</code>.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .named("Foo")
        .withConfigUri("scoped-configuration.xml")
        .build();</markup>

<p>In this case the scope name will be <code>Test</code> as no scope name or session name has been explicitly set in
the <code>SessionConfiguration</code> so the scope name of <code>Test</code> will be used from the XML configuration.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .withConfigUri("scoped-configuration.xml")
        .build();</markup>

<p>In this case the scope name will be <code>Test</code> as the session name has been set to <code>Foo</code> but the scope name has been
explicitly set to the default scope name using the constant <code>Coherence.DEFAULT_SCOPE</code> so the scope name
of <code>Test</code> will be used from the XML configuration.</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .named("Foo")
        .withScopeName(Coherence.DEFAULT_SCOPE)
        .withConfigUri("scoped-configuration.xml")
        .build();</markup>
</p>
</div>
</div>
</div>

<h3 id="_coherence_configuration">Coherence Configuration</h3>
<div class="section">
<p>A Coherence application is started by creating a <code>Coherence</code> instance from a <code>CoherenceConfiguration</code>.
An instance of <code>CoherenceConfiguration</code> is created using the builder. For example:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .build();</markup>


<h4 id="_adding_sessions">Adding Sessions</h4>
<div class="section">
<p>A <code>Coherence</code> instance manages one or more <code>Session</code> instances, which are added to the <code>CoherenceConfiguration</code> by
adding the <code>SessionConfiguration</code> instances to the builder.</p>

<p>If no sessions have been added to the builder the <code>Coherence</code> instance will run a single <code>Session</code> that uses the default
configuration file.</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .build();</markup>

<p>The configuration above will configure a <code>Coherence</code> instance with the default name and with a single <code>Sessions</code>
that wil use the default configuration file.</p>

<p>The default session can also be explicitly added to the <code>CoherenceConfiguration</code>:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .withSession(SessionConfiguration.defaultSession())
        .build();</markup>

<p>As already shown, other session configurations may also be added to the <code>CoherenceConfiguration</code>:</p>

<markup
lang="java"

>SessionConfiguration session = SessionConfiguration.builder()
        .named("Carts")
        .withConfigUri("cache-config.xml")
        .build();

CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .withSession(session)
        .build();</markup>

<p>Whilst there is no limit to the number of sessions that can be configured the majority of applications would only ever
require a single session - more than likely just the default session.</p>

</div>

<h4 id="session-discovery">Session Configuration Auto-Discovery</h4>
<div class="section">
<p>A <code>CoherenceConfiguration</code> can be configured to automatically discover <code>SessionConfiguration</code> instances.
These are discovered using the Java <code>ServiceLoader</code>. Any instances of <code>SessionConfiguration</code> or
<code>SessionConfiguration.Provider</code> configured as services in <code>META-INF/services/</code> files will be loaded.</p>

<p>This is useful if you are building modular applications where you want to include functionality in a separate application
module that uses its own <code>Session</code>. The <code>SessionConfiguration</code> for the module is made discoverable by the <code>ServiceLoader</code>
then whenever the module&#8217;s jar file is on the classpath the <code>Session</code> will be created, and the module&#8217;s functionality
will be available to the application.</p>

<p>For example:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .discoverSessions() <span class="conum" data-value="1" />
        .build();</markup>

<ul class="colist">
<li data-value="1">The call to <code>discoverSessions()</code> will load discovered <code>SessionConfiguration</code> instances.</li>
</ul>
</div>

<h4 id="_coherence_instance_name">Coherence Instance Name</h4>
<div class="section">
<p>Each <code>Coherence</code> instance must be uniquely named. A name can be specified using the <code>named()</code> method on the builder,
if no name has been specified the default name of <code>$Default$</code> will be used.</p>

<p>In the majority of use-cases an application would only ever require a single <code>Coherence</code> instance so there would be
no requirement to specify a name.</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .named("Carts")
        .build();</markup>

<p>The configuration above will create a <code>Coherence</code> instance with the name <code>Carts</code>.</p>

</div>

<h4 id="_add_global_event_interceptors">Add Global Event Interceptors</h4>
<div class="section">
<p>As already mentioned, event interceptors can be added to a <code>SessionConfiguration</code> to receive events for a session.
Event interceptors can also be added to the <code>Coherence</code> instance to receive events for all <code>Session</code> instances
managed by that <code>Coherence</code> instance.</p>

<p>For example, reusing the previous <code>CacheInterceptor</code> class, but this time for caches in all sessions:</p>

<markup
lang="java"

>SessionConfiguration cartsSession = SessionConfiguration.builder()
         .named("Carts")
         .withConfigUri("cache-config.xml")
         .build();

CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .withSession(SessionConfiguration.defaultSession())
        .withSession(cartsSession)
        .withInterceptor(new CacheInterceptor())
        .build();</markup>

<p>Now the <code>CacheInterceptor</code> will receive events for both the default session and the <code>Certs</code> session.</p>

</div>
</div>

<h3 id="_create_a_coherence_instance">Create a Coherence Instance</h3>
<div class="section">
<p>A <code>CoherenceConfiguration</code> can be used to create a <code>Coherence</code> instance.</p>

<p>A <code>Coherence</code> instance is created in one of two modes, either cluster member or client. The mode chosen affects how some
types of <code>Session</code> are created and whether auto-start services are started.</p>

<p>As the name suggests a "cluster member" is a <code>Coherence</code> instance that expects to start or join a Coherence cluster.
In a cluster member any <code>Session</code> that wraps a <code>ConfigurableCacheFactory</code> will be have its services auto-started and
monitored (this is the same behaviour that would have happened when using <code>DefaultCacheServer</code> to start a server).</p>

<p>A "client" <code>Coherence</code> instance is typically not a cluster member, i.e. it is a Coherence*Extend or gRPC client.
As such, <code>Session</code> instances that wrap a <code>ConfigurableCacheFactory</code> will not be auto-started, they will start on demand
as resources such as maps, caches or topics are requested from them.</p>

<p>The <code>com.tangosol.net.Coherence</code> class has static factory methods to create <code>Coherence</code> instances in different modes.</p>

<p>For example, to create a <code>Coherence</code> instance that is a cluster member the <code>Coherence.clusterMember</code> method is used:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .build();

Coherence coherence = Coherence.clusterMember(cfg);</markup>

<p>For example, to create a <code>Coherence</code> instance that is a client the <code>Coherence.client</code> method is used:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .build();

Coherence coherence = Coherence.client(cfg);</markup>


<h4 id="_create_a_default_coherence_instance">Create a Default Coherence Instance</h4>
<div class="section">
<p>It is possible to create a <code>Coherence</code> instance without specifying any configuration.</p>

<markup
lang="java"

>Coherence coherence = Coherence.clusterMember();</markup>

<markup
lang="java"

>Coherence coherence = Coherence.client();</markup>

<p>In both of the above examples the <code>Coherence</code> instance will have the default <code>Session</code> and any
<router-link to="#session-discovery" @click.native="this.scrollFix('#session-discovery')">discovered sessions</router-link>.</p>

</div>
</div>

<h3 id="_start_coherence">Start Coherence</h3>
<div class="section">
<p>A <code>Coherence</code> instance it must be started to start all the sessions that the <code>Coherence</code> instance
is managing. This is done by calling the <code>start()</code> method.</p>

<markup
lang="java"

>Coherence coherence = Coherence.clusterMember(cfg);

coherence.start();</markup>

</div>

<h3 id="_obtaining_a_coherence_instance">Obtaining a Coherence Instance</h3>
<div class="section">
<p>To avoid having to pass around the instance of <code>Coherence</code> that was used to bootstrap an application the
<code>Coherence</code> class has some static methods that make it simple to retrieve an instance.</p>

<p>If only a single instance of <code>Coherence</code> is being used in an application (which will cover most use-cases) then
the <code>getInstance()</code> method can be used:</p>

<markup
lang="java"

>Coherence coherence = Coherence.getInstance();</markup>

<p>It is also possible to retrieve an instance by name:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .named("Carts")
        .build();

Coherence.create(cfg);</markup>

<p>&#8230;&#8203;then later&#8230;&#8203;</p>

<markup
lang="java"

>Coherence coherence = Coherence.getInstance("Carts");</markup>

</div>

<h3 id="_ensuring_coherence_has_started">Ensuring Coherence Has Started</h3>
<div class="section">
<p>If application code needs to ensure that a <code>Coherence</code> instance has started before doing some work then the
<code>whenStarted()</code> method can be used to obtain a <code>CompletableFuture</code> that will be completed when the <code>Coherence</code>
instance has started.</p>

<markup
lang="java"

>Coherence               coherence = Coherence.getInstance("Carts");
CompletableFuture&lt;Void&gt; future    = coherence.whenStarted();

future.join();</markup>

<p>There is also a corresponding <code>whenStopped()</code> method that returns a future that will be completed when the
<code>Coherence</code> instance stops.</p>

</div>

<h3 id="_coherence_lifecycle_interceptors">Coherence Lifecycle Interceptors</h3>
<div class="section">
<p>Besides using the future methods described above it is possible to add and <code>EventInterceptor</code> to the configuration
of a <code>Coherence</code> instance that will receive life-cycle events.</p>

<p>Below is an example interceptor that implements <code>Coherence.LifecycleListener</code>.</p>

<markup
lang="java"

>public class MyInterceptor implements Coherence.LifecycleListener {
    public void onEvent(CoherenceLifecycleEvent event) {
        // process event
    }
}</markup>

<p>The interceptor can be added to the configuration:</p>

<markup
lang="java"

>CoherenceConfiguration cfg = CoherenceConfiguration.builder()
        .withSession(SessionConfiguration.defaultSession())
        .withInterceptor(new MyInterceptor())
        .build();</markup>

<p>When a <code>Coherence</code> instance created from this configuration is start or stopped the <code>MyInterceptor</code> instance will
receive events.</p>

</div>
</div>
</doc-view>
