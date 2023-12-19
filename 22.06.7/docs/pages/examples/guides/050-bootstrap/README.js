<doc-view>

<h2 id="_bootstrap_coherence">Bootstrap Coherence</h2>
<div class="section">
<p>Whether you are running a Coherence cluster member or client, you&#8217;ll need to configure and bootstrap Coherence.
Coherence does not actually need any configuring or bootstrapping, you could just do something like <code>CacheFactory.getCache("foo");</code>,
but when there is an alternative, static method calls to get Coherence resources are poor coding practice (especially when it comes
to unit testing with mocks and stubs).</p>

<p>Coherence CE v20.12 introduced a new bootstrap API for Coherence, which this guide is going to cover.
Not only does the bootstrap API make it simpler to start Coherence, it makes some other uses cases simpler,
for example where a client application needs to connect to multiple clusters.</p>

<p>A number of the integrations between Coherence and application frameworks, such as
<a id="" title="" target="_blank" href="https://github.com/coherence-community/coherence-spring">Coherence Spring</a>,
<a id="" title="" target="_blank" href="https://github.com/micronaut-projects/micronaut-coherence">Coherence Micronaut</a>,
<a id="" title="" target="_blank" href="https://helidon.io">Coherence CDI and Helidon</a>, use the bootstrap API under the covers
to initialize Coherence when using those frameworks.
When using these types of "DI" frameworks, Coherence and Session instances and other Coherence resources can just be
injected into application code without even needing to directly access the bootstrap API.</p>


<h3 id="_contents">Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#starting" @click.native="this.scrollFix('#starting')">Simply Starting Coherence</router-link> - Running <code>Coherence</code> as the main class.</p>

</li>
<li>
<p><router-link to="#coherence-instance" @click.native="this.scrollFix('#coherence-instance')">The Coherence Instance</router-link> - Accessing and using the bootstrapped Coherence instance</p>
<ul class="ulist">
<li>
<p><router-link to="#starting" @click.native="this.scrollFix('#starting')">Ensure Coherence is Started</router-link> - Obtaining a fully running Coherence instance</p>

</li>
<li>
<p><router-link to="#session" @click.native="this.scrollFix('#session')">Coherence Sessions</router-link> - Obtaining Coherence Session instances and other Coherence resources</p>

</li>
</ul>
</li>
<li>
<p><router-link to="#app-init" @click.native="this.scrollFix('#app-init')">Application Initialization</router-link> - Initializing application code without needing a custom main class</p>

</li>
<li>
<p><router-link to="#bootstrap-code" @click.native="this.scrollFix('#bootstrap-code')">Bootstrap Coherence</router-link> - Starting Coherence from Application Code</p>
<ul class="ulist">
<li>
<p><router-link to="#simple-cluster" @click.native="this.scrollFix('#simple-cluster')">Simple Cluster Member</router-link> - Start a simple cluster member</p>

</li>
<li>
<p><router-link to="#configure-cluster" @click.native="this.scrollFix('#configure-cluster')">Configured Cluster Member</router-link> - Configure and start a simple cluster member</p>

</li>
</ul>
</li>
</ul>
</div>
</div>

<h2 id="_what_you_will_build">What You Will Build</h2>
<div class="section">
<p>This guide will look at some ways to bootstrap a Coherence application.</p>


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

<h2 id="_a_brief_history">A Brief History</h2>
<div class="section">
<p>A Coherence application is either a cluster member, or it is a client. Historically a client would be a Coherence*Extend client, but more recently Coherence has also introduced a gRPC client.
Prior to CE v20.12, applications typically used Coherence in a couple of ways; either cluster members that started by running <a id="" title="" target="_blank" href="https://coherence.community/22.06.7/api/java/com/tangosol/net/DefaultCacheServer.html">DefaultCacheServer</a>, or by running a custom main class and obtaining Coherence resources directly from a <code>Session</code> or <code>ConfigurableCacheFactory</code> instance - possibly using static methods on <code>com.tangosol.net.CacheFactory</code>. By far the majority of applications only had a single <code>ConfigurableCacheFactory</code> instance, but occasionally an application would add more (for example an Extend client connecting to multiple cluster). Adding of additional <code>ConfigurableCacheFactory</code> required custom start-up code and management code. In an effort to make it possible to build more modular applications with multiple <code>ConfigurableCacheFactory</code> or <code>Session</code> instances a new bootstrap API was added.</p>

</div>

<h2 id="starting">Starting Coherence</h2>
<div class="section">
<p>The <a id="" title="" target="_blank" href="https://coherence.community/22.06.7/api/java/com/tangosol/net/Coherence.html">Coherence</a> class is the main entry point into a Coherence application.
A Coherence server can be started by simply running the <code>Coherence.main()</code> method.
From Coherence CE v22.06, this is the default way that Coherence starts using <code>java -jar coherence.jar</code>.</p>

<p>An important point when using the <code>Coherence</code> class to start Coherence is that this will automatically include starting some of
the additional Coherence extensions if they are on the class path, or module path. For example, starting the Coherence health check http endpoints, or if the Coherence Concurrent module is on the class path, its services will automatically be started. The same applies to the Coherence gRPC server, Coherence metrics and Coherence REST management.</p>

<markup
lang="bash"

>java -cp coherence.jar com.tangosol.net.Coherence</markup>

<markup
lang="bash"

>java -jar coherence.jar</markup>

<p>Or with Java modules</p>

<markup
lang="bash"

>java -p coherence.jar -m com.oracle.coherence</markup>

<p>Functionally this is almost identical to the old way of running <code>DefaultCacheServer</code>, but will now use the new bootstrap API to configure and start Coherence.
When run in this way Coherence will use the default configuration file <code>coherence-cache-config.xml</code>, either from <code>coherence.jar</code> or elsewhere on the classpath. The name of this configuration file can be overridden as normal with the <code>coherence.cacheconfig</code> system property.
Running the Coherence class, or using the bootstrap API, will also start various system services, such as the health check http endpoints.</p>


<h3 id="_running_coherence_as_an_extend_client">Running Coherence as an Extend Client</h3>
<div class="section">
<p>By default, Coherence will run as a storage enabled cluster member, unless Coherence system properties or environment variables have been used to override this.</p>

<p>For example, <em>when using the default <code>coherence-cache-config.xml</code> file from <code>coherence.jar`</em> it is possible to run `Coherence</code> as an Extend client by setting the <code>coherence.client</code> system property (or <code>COHERENCE_CLIENT</code> environment variable) to a value of <code>remote</code>.</p>

<markup
lang="bash"

>java -cp coherence.jar -Dcoherence.client=remote com.tangosol.net.Coherence</markup>

</div>
</div>

<h2 id="coherence-instance">Using a Coherence Instance</h2>
<div class="section">
<p>Once a <code>Coherence</code> instance has been started, using either the <code>Coherence.main()</code> method, or one of the other ways described below,
application code can obtain the running Coherence instance and obtain a Coherence <a id="" title="" target="_blank" href="https://coherence.community/22.06.7/api/java/com/tangosol/net/Session.html">Session</a>
which can then be used to access Coherence resources such as <code>NamedMap</code>, <code>NamedCache</code>, <code>NamedTopic</code> etc.</p>

<p>More than one Coherence instance can be running simultaneously (but in the case of a cluster member, all these instances will be a
single cluster member, they are not able to be parts of separate clusters). Each Coherence instance has a unique name and can be accessed by name.</p>

<p>If Coherence has been started using <code>Coherence.main()</code> there will be a single instance of Coherence with the default name.
The simplest way to access the default <code>Coherence</code> instance is using the static accessor.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.getInstance();</markup>

<p>Coherence instances can also be obtained by name, the default instance&#8217;s name can be accessed using the static field <code>Coherence.DEFAULT_NAME</code>:</p>

<markup
lang="java"

>        Coherence coherence = Coherence.getInstance(Coherence.DEFAULT_NAME);</markup>


<h3 id="ensure-started">Ensure Coherence is Started</h3>
<div class="section">
<p>Sometimes, application code may need to ensure Coherence has fully started before running.
A <code>Coherence</code> instance has a <code>whenStarted()</code> method that returns a <code>CompletableFuture</code> that will be completed when
the Coherence instance has finished starting.</p>

<p>The example below obtains the default Coherence instance and waits up to five minuts for the instance to be running.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.getInstance()
                                       .whenStarted()
                                       .get(5, TimeUnit.MINUTES);</markup>

</div>

<h3 id="session">Obtain a Coherence Session</h3>
<div class="section">
<p>Each Coherence instance will be running one or more uniquely named <code>Session</code> instances, depending on how it was configured.
By running <code>Coherence.main()</code> the default <code>Coherence</code> instance will be running the default <code>Session</code>.
A <code>Session</code> can be obtained from a <code>Coherence</code> instance using a number of methods.</p>

<p>The example below obtains the default Coherence Session from the default Coherence instance.
This method would be used if Coherence has been started using the default <code>Coherence.main()</code> method.</p>

<markup
lang="java"

>        Coherence                coherence = Coherence.getInstance();
        Session                  session   = coherence.getSession();
        NamedMap&lt;String, String&gt; map       = session.getMap("test");</markup>

<p>A <code>Session</code> can also be obtained using its name.
The example below obtains the <code>Session</code> named "foo".</p>

<markup
lang="java"

>        Coherence                coherence = Coherence.getInstance();
        Session                  session   = coherence.getSession("foo");
        NamedMap&lt;String, String&gt; map       = session.getMap("test");</markup>

<p>It is also possible to use the static <code>Coherence.findSession()</code> method to find a <code>Session</code> by name across all
configured Coherence instances. This method returns an optional containing the <code>Session</code> or empty if no <code>Session</code>
exists with the requested name.</p>

<markup
lang="java"

>        Optional&lt;Session&gt; optional = Coherence.findSession("foo");
        if (optional.isPresent()) {
            Session                  session = optional.get();
            NamedMap&lt;String, String&gt; map     = session.getMap("test");
        }</markup>

</div>
</div>

<h2 id="app-init">Initialize Application Code</h2>
<div class="section">
<p>Sometimes an application needs to perform some initialization when it starts up.
Before the new bootstrap API existed, this was a common reason for applications having to add a custom main class.
The <code>Coherence</code> class has an inner interface <a id="" title="" target="_blank" href="https://coherence.community/22.06.7/api/java/com/tangosol/net/Coherence.LifecycleListener.html">LifecycleListener</a> that
application code can implement to be notified of Coherence start-up and shutdown events.
Instances of <code>LifecycleListener</code> are automatically discovered by Coherence at runtime using the Java <code>ServiceLoader</code>, which means that an
applications can be initialised without needing a custom main class, but instead by just implementing a <code>LifecycleListener</code>.
This is particularly useful where an application is made up of modules that may or may not be on the class path or module path at runtime.
A module just needs to implement a Coherence <code>LifecycleListener</code> as a Java service and whenever it is on the class path it will be initialized.</p>

<p>For example, an application that needs to start a web-server could implement <code>LifecycleListener</code> as shown below.
The <code>STARTED</code> event type is fired after a Coherence instance is started, the <code>STOPPING</code> event type is fired before a Coherence instance is stopped.</p>

<markup
lang="java"

>import com.tangosol.net.Coherence;
import com.tangosol.net.events.CoherenceLifecycleEvent;

public class WebServerController
        implements Coherence.LifecycleListener {

    private final HttpServer server = new HttpServer();

    @Override
    public void onEvent(CoherenceLifecycleEvent event) {
        switch (event.getType()) {
            case STARTED:
                server.start();
                break;
            case STOPPING:
                server.stop();
                break;
        }
    }
}</markup>

<p>The event also contains the <code>Coherence</code> instance that raised the event, so this could then be used to obtain a <code>Session</code>
and other Coherence resources that are needed as part of the application initialisation.</p>

<p>Adding the <code>WebServerController</code> class above to a <code>META-INF/services</code> file or module-info file will make it discoverable by Coherence.</p>

<markup
lang="java"
title="META_INF/services/com.tangosol.net.Coherence$LifecycleListener"
>com.oracle.coherence.guides.bootstrap.WebServerController;</markup>

<markup
lang="java"
title="module-info.java"
>open module com.oracle.coherence.guides.bootstrap {
    requires com.oracle.coherence;

    exports com.oracle.coherence.guides.bootstrap;

    provides com.tangosol.net.Coherence.LifecycleListener
        with com.oracle.coherence.guides.bootstrap.WebServerController;
}</markup>

</div>

<h2 id="bootstrap-code">Bootstrap Coherence in Application Code</h2>
<div class="section">
<p>If your application needs to control start-up and shutdown of Coherence, then the bootstrap API can be called from application code.
This is often useful in integration JUnit test code too, where a test class may need to configure and start Coherence for a set of tests.
It is possible for application code to run multiple <code>Coherence</code> instances, which each manage one or more scoped Coherence sessions.
Where multiple <code>Coherence</code> cluster member instances are created, they will still all be part of a single Coherence cluster member, they
cannot be part of separate clusters.</p>


<h3 id="simple-cluster">Run a Simple Cluster Member</h3>
<div class="section">
<p>The simplest way to start Coherence as a cluster member in application code is shown below:</p>

<markup
lang="java"

>        Coherence coherence = Coherence.clusterMember();
        coherence.start();</markup>

<p>The <code>start()</code> method returns a <code>CompletableFuture</code> so application code that needs to wit for start-up to complete can use the future for this purpose.
The example below ensures <code>Coherence</code> is started as a cluster member (waiting a maximum of five minutes) before proceeding.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.clusterMember()
                                       .start()
                                       .get(5, TimeUnit.MINUTES);</markup>

<p>Running Coherence in this way will create a single <code>Session</code> using the default cache configuration file
(or another file specified using the <code>-Dcoherence.cacheconfig</code> system property).
By default, this will be a storage enabled cluster member, unless Coherence system properties or environment variables have been used to override this.</p>

</div>

<h3 id="configure-cluster">Configure a Cluster Member</h3>
<div class="section">
<p>The bootstrap API allows the <code>Coherence</code> instance to be configured before starting, for example adding one or more session configurations.</p>

<p>In the example below, a <code>Coherence</code> cluster member instance is created using a configuration.
The configuration in this case does not specify a name, so the default name will be used.
The configuration contains two <code>Session</code> configurations. The first is named "foo" and uses the cache configuration loaded from
<code>foo-cache-config.xml</code> with the scope name "Foo". The second <code>Session</code> will be the default session using the default cache configuration file.</p>

<markup
lang="java"

>        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                                                                        .named("foo")
                                                                        .withScopeName("Foo")
                                                                        .withConfigUri("foo-cache-config.xml")
                                                                        .build();

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                                                              .withSession(sessionConfiguration)
                                                              .withSession(SessionConfiguration.defaultSession())
                                                              .build();

        Coherence coherence = Coherence.clusterMember(config)
                                       .start()
                                       .join();</markup>

<p>There are various other methods on the configuration builders, for example configuring parameters to pass into the cache configuration files, configuring interceptors, etc.</p>

</div>

<h3 id="run-extend">Run Coherence as an Extend Client</h3>
<div class="section">
<p>If the application code will is an Extend client, then Coherence can be bootstrapped in client mode.</p>

<p>The example below starts Coherence as an Extend client, which will use the Coherence NameService to locate the cluster and look up the Extend Proxy to connect to. This works by configuring the client to have the same cluster name and same well-known address list (or multicast settings) as the cluster being connected to, either using System properties or environment variables.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.client();
        coherence.start();</markup>

<p>Alternatively, instead of using the NameService a fixed address and port can be configured for the Extend client to use.
If the System property <code>coherence.extend.address</code> is set to the IP address or host name of the Extend proxy,
and <code>coherence.extend.port</code> is set to the port of the Extend proxy
(or the corresponding environment variables <code>COHERENCE_EXTEND_ADDRESS</code> and <code>COHERENCE_EXTEND_PORT</code>)
then Coherence can be bootstrapped as shown below.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.fixedClient();
        coherence.start();</markup>

<p>Coherence will then be bootstrapped as an Extend client and connect to the proxy on the configured address and port.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The code snippets above work with the default cache configuration file.
The default cache configuration file in the <code>coherence.jar</code> is configured with certain injectable property values,
which are configured by the bootstrap API when running as a client. Using other cache configuration files that are not configured with these properties would mean "client" mode is effectively ignored. The Coherence instance will still be started and will run correctly, the client mode properties will just have no affect.</p>
</p>
</div>
</div>

<h3 id="configure-extend">Configure an Extend Client</h3>
<div class="section">
<p>Coherence can be configured in client mode in code.</p>

<p>In the example below, a <code>Coherence</code> client instance is created using a configuration.
The configuration in this case does not specify a name, so the default name will be used.
The configuration contains two <code>Session</code> configurations. The first is named "foo" and uses the cache configuration loaded from
<code>foo-cache-config.xml</code> with the scope name "Foo". The second <code>Session</code> will be the default session using the default cache configuration file.</p>

<markup
lang="java"

>        SessionConfiguration sessionConfiguration = SessionConfiguration.builder()
                                                                        .named("Foo")
                                                                        .withScopeName("Foo")
                                                                        .withConfigUri("foo-cache-config.xml")
                                                                        .build();

        CoherenceConfiguration config = CoherenceConfiguration.builder()
                                                              .withSession(sessionConfiguration)
                                                              .withSession(SessionConfiguration.defaultSession())
                                                              .build();

        Coherence coherence = Coherence.client(config)
                                       .start()
                                       .join();</markup>

<p>Using Coherence Extend and application can configure in this way, with multiple <code>Session</code> instances,
where each session will connect as an Extend client to a different Coherence cluster.
Each configured session is given a different name and scope.
The required sessions can then be obtained from the running <code>Coherence</code> instance by application code at runtime.</p>

</div>
</div>
</doc-view>
