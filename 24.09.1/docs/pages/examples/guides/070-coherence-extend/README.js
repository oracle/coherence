<doc-view>

<h2 id="_coherenceextend">Coherence*Extend</h2>
<div class="section">
<p>In the previous guide <router-link to="/examples/guides/050-bootstrap/README">Bootstrap Coherence</router-link> we briefly talked about connecting
to a Coherence Cluster via a <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/introduction-coherenceextend.html">Coherence*Extend</a>
client using the default cache configuration file. This guide will go a bit deeper in regard to using Coherence*Extend and
cover the following use-cases:</p>

<ul class="ulist">
<li>
<p>Connect using the name service using a custom cache configuration file</p>

</li>
<li>
<p>Demonstrate Proxy load balancing</p>

</li>
<li>
<p>Setting specific host &amp; port (Firewall use-case)</p>

</li>
</ul>
<p>In all 3 use-cases we will use custom cache configuration files.</p>


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
<p><router-link to="#building-the-example-code" @click.native="this.scrollFix('#building-the-example-code')">Building The Example Code</router-link></p>

</li>
<li>
<p><router-link to="#data-model" @click.native="this.scrollFix('#data-model')">Example Data Model</router-link></p>

</li>
<li>
<p><router-link to="#why-use-coherence-extend" @click.native="this.scrollFix('#why-use-coherence-extend')">Why use Coherence*Extend?</router-link></p>

</li>
<li>
<p><router-link to="#connect-to-name-service" @click.native="this.scrollFix('#connect-to-name-service')">Connect via the Name Service</router-link></p>

</li>
<li>
<p><router-link to="#use-proxy-load-balancing" @click.native="this.scrollFix('#use-proxy-load-balancing')">Using Proxy Load Balancing</router-link></p>

</li>
<li>
<p><router-link to="#specific-host-port" @click.native="this.scrollFix('#specific-host-port')">Setting Host and Port Explicitly</router-link></p>

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
<p>The example code is written as a set of JUnit integration tests, showing how you can use Coherence*Extend. For our
test cases we will also use <a id="" title="" target="_blank" href="https://github.com/coherence-community/oracle-bedrock">Oracle Bedrock</a> to start server instances
of Oracle Coherence for testing purposes.</p>


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

<h4 id="building-the-example-code">Building the Example Code</h4>
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

<h3 id="data-model">Example Data Model</h3>
<div class="section">
<p>The data model for this guide consists of a single class named <code>Country</code>. This model class represents a country with the
following properties:</p>

<ul class="ulist">
<li>
<p>name</p>

</li>
<li>
<p>capital</p>

</li>
<li>
<p>population</p>

</li>
</ul>
<p>The data is being stored in a Coherence cache named <code>countries</code> with the key being the two-letter
<a id="" title="" target="_blank" href="https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2">ISO 3166 country code</a>.</p>

</div>

<h3 id="why-use-coherence-extend">Why use Coherence*Extend?</h3>
<div class="section">
<p>Although recommended, it may not always be possible that your application can be directly part of a Coherence cluster using
the <em>Tangosol Cluster Management Protocol</em> (TCMP). For example, your application may be located in a different network, you need
to access Oracle Coherence from desktop applications, or you need to use languages other than Java, e.g. C++ or .NET.</p>

<div class="admonition note">
<p class="admonition-inline">Another alternative is to use the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/getting-started-grpc.html">gRPC integration</a>.</p>
</div>
</div>

<h3 id="connect-to-name-service">Connect via the Name Service</h3>
<div class="section">
<p>When connecting to a Coherence Cluster via Coherence*Extend, we recommend the use of the Name Service. The use of the name
service simplifies port management as the name service will look up the actual Coherence*Extend ports. That way
Coherence*Extend ports can be ephemeral. For this example, let&#8217;s start with the Server Cache Configuration file at
<code>src/main/resources/name-service/server-coherence-cache-config.xml</code>.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt; <span class="conum" data-value="1" />
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme/&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
    &lt;proxy-scheme&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt; <span class="conum" data-value="2" />
      &lt;autostart&gt;true&lt;/autostart&gt; <span class="conum" data-value="3" />
    &lt;/proxy-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">In the <code>&lt;cache-mapping&gt;</code> element, we state that the <code>countries</code> cache maps to the <code>country-scheme</code></li>
<li data-value="2">The <code>country-scheme</code> then declares the <code>&lt;proxy-scheme&gt;</code> with the name <code>MyCountryExtendService</code></li>
<li data-value="3">The <code>MyCountryExtendService</code> will start automatically</li>
</ul>
<p>The <code>MyCountryExtendService</code> will be registered with the default name service. If you wanted to customize that behavior,
you would need to provide an <code>&lt;acceptor-config&gt;</code> element. See the load-balancing use-case below for details. We will also
create a corresponding Client Cache Configuration file at <code>src/main/resources/name-service/client-coherence-cache-config.xml</code>.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt; <span class="conum" data-value="1" />
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;remote-cache-scheme&gt; <span class="conum" data-value="2" />
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt; <span class="conum" data-value="3" />
      &lt;initiator-config&gt;
        &lt;outgoing-message-handler&gt;
          &lt;request-timeout&gt;5s&lt;/request-timeout&gt; <span class="conum" data-value="4" />
        &lt;/outgoing-message-handler&gt;
      &lt;/initiator-config&gt;
    &lt;/remote-cache-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">The cache mapping for the client will look similar to the server one, but we name the scheme <code>remote-country-scheme</code></li>
<li data-value="2">The client specifies a <code>&lt;remote-cache-scheme&gt;</code> element</li>
<li data-value="3">The service name <code>MyCountryExtendService</code> must match the name we use in the server cache configuration file</li>
<li data-value="4">We also define a request-timeout of 5 seconds. This means that if a connection cannot be established within that time,
an exception is raised</li>
</ul>
<p>The client will be using the default name service port of <code>7574</code> to lookup the proxy endpoint for the <code>MyCountryExtendService</code>.
You could customize that configuration by providing an <code>&lt;initiator-config&gt;</code> element. See the firewall example below for
details. In the test case itself, we will use Oracle Bedrock to boostrap the Coherence server using the Server Cache Configuration file:</p>

<markup
lang="java"

>static CoherenceClusterMember server;

@BeforeAll
static void setup() {

    final LocalPlatform platform = LocalPlatform.get();

    // Start the Coherence server
    server = platform.launch(CoherenceClusterMember.class,
            CacheConfig.of("name-service/server-coherence-cache-config.xml"), <span class="conum" data-value="1" />
            IPv4Preferred.yes(),
            SystemProperty.of("coherence.wka", "127.0.0.1"),
            ClusterName.of("myCluster"), <span class="conum" data-value="2" />
            DisplayName.of("server"));

    // Wait for Coherence to start
    Eventually.assertDeferred(() -&gt; server.invoke(
            new IsServiceRunning("MyCountryExtendService")), is(true)); <span class="conum" data-value="3" />
}</markup>

<ul class="colist">
<li data-value="1">Specify the server cache configuration file</li>
<li data-value="2">Give the Server Cluster an explicit name <code>myCluster</code></li>
<li data-value="3">Make sure that we wait until the <code>MyCountryExtendService</code> proxy service is available</li>
</ul>
<p>Then we configure and start the Coherence client.</p>

<markup
lang="java"

>@Test
void testNameServiceUseCase() {
    System.setProperty("coherence.tcmp.enabled", "false"); <span class="conum" data-value="1" />
    System.setProperty("coherence.cluster", "myCluster"); <span class="conum" data-value="2" />
    System.setProperty("coherence.wka", "127.0.0.1");
    CoherenceHelper.startCoherenceClient(
            CoherenceHelper.NAME_SERVICE_INSTANCE_NAME,
            "name-service/client-coherence-cache-config.xml"); <span class="conum" data-value="3" />
    NamedCache&lt;String, Country&gt; countries = CoherenceHelper.getMap(CoherenceHelper.NAME_SERVICE_INSTANCE_NAME,"countries"); <span class="conum" data-value="4" />
    countries.put("de", new Country("Germany", "Berlin", 83.2));
}</markup>

<ul class="colist">
<li data-value="1">Disable TCMP to ensure that we only connect via Coherence*Extend</li>
<li data-value="2">Set the cluster name of the client to the same name as the server</li>
<li data-value="3">Specify the client cache configuration file</li>
<li data-value="4">Get the <code>NamedCache</code> and add a new country</li>
</ul>
<div class="admonition important">
<p class="admonition-textlabel">Important</p>
<p ><p>When configuring your Coherence*Extend client, it is important that your client&#8217;s Cluster Name match the name of the
Coherence Server Cluster.</p>
</p>
</div>
<div class="admonition tip">
<p class="admonition-textlabel">Tip</p>
<p ><p>Java-based clients located on the same network as the Coherence server should disable TCMP communication in order to
ensure that the client connect to clustered services exclusively using extend proxies. This can be achieved
by setting System property <code>coherence.tcmp.enabled</code> to <code>false</code>. Please
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/configuring-extend-clients.html#GUID-F8832F4C-2F88-4DE4-A804-D100D47B552C">see the reference documentation</a> for more detailed information.</p>
</p>
</div>

<h4 id="_run_the_test">Run the Test</h4>
<div class="section">
<p>Running the test should be fairly uneventful. If successful, you will see Bedrock starting up the Coherence server with
1 instance followed by the client starting up and connecting. Let&#8217;s do a quick test of the <code>request-timeout</code> and see what
happens when the Coherence Server is not available. Comment out the <code>setup()</code> method, and re-run the test. After the
specified <code>request-timeout</code> of 5 seconds, you should get a stacktrace with an exception similar to the following:</p>

<markup
lang="bash"

>com.tangosol.net.messaging.ConnectionException: Unable to locate cluster 'myCluster' while looking for its ProxyService 'MyCountryExtendService'</markup>

<p>In the next section we will see how we can use multiple Coherence servers, and thus taking advantage of proxy load-balancing.</p>

</div>
</div>

<h3 id="use-proxy-load-balancing">Using Proxy Load Balancing</h3>
<div class="section">
<p>When you have multiple Coherence servers that you are connecting to via Coherence*Extend, connection load-balancing is
automatically applied. The default load-balancing behavior is based on the load of each Coherence server member and
client connections are evenly spread across the Coherence cluster. The default load balance algorithm is called ‘proxy’,
which if you were to explicitly configure that setting, your Server Cache Configuration file would add the following
<code>&lt;proxy-scheme&gt;</code>:</p>

<markup
lang="xml"

>&lt;proxy-scheme&gt;
   &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt;
   &lt;load-balancer&gt;proxy&lt;/load-balancer&gt;
   &lt;autostart&gt;true&lt;/autostart&gt;
&lt;/proxy-scheme&gt;</markup>

<p>Under the covers, this configuration will use the class <code>DefaultProxyServiceLoadBalancer</code>.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The other build-in load-balancing option is <code>client</code> for client-based load-balancing. We will use that option in the
firewall use-case below.</p>
</p>
</div>
<p>You can, however, customize the load-balancing logic depending on your needs by providing an implementation of the
<code>ProxyServiceLoadBalancer</code> interface. As mentioned above, Coherence&#8217;s default implementation is the <code>DefaultProxyServiceLoadBalancer</code>.
For our test-case, lets simply customize it by adding some more logging:</p>

<markup
lang="java"

>public class CustomProxyServiceLoadBalancer extends DefaultProxyServiceLoadBalancer {
	@Override
	public int compare(ProxyServiceLoad load1, ProxyServiceLoad load2) {

		int result = super.compare(load1, load2);
		System.out.println(String.format("Local Member Id: %s (Total # of Members: %s) - Connection Count: %s",
				super.getLocalMember().getId(),
				super.getMemberList(null).size(),
				load1.getConnectionCount()));
		return result;
	}
}</markup>

<p>The Server Cache Configuration file at <code>src/main/resources/load-balancing/server-coherence-cache-config.xml</code> is almost the
same compared to the name-service example, but we add a <code>&lt;load-balancer&gt;</code> element.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme/&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
    &lt;proxy-scheme&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt;
      &lt;load-balancer&gt; <span class="conum" data-value="1" />
        &lt;instance&gt;
          &lt;class-name&gt;com.oracle.coherence.guides.extend.loadbalancer.CustomProxyServiceLoadBalancer&lt;/class-name&gt; <span class="conum" data-value="2" />
        &lt;/instance&gt;
      &lt;/load-balancer&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/proxy-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">The <code>MyCountryExtendService</code> also specifies a <code>&lt;load-balancer&gt;</code> element</li>
<li data-value="2">The load-balancer uses the customized <code>CustomProxyServiceLoadBalancer</code></li>
</ul>
<p>The corresponding Client Cache Configuration file at <code>src/main/resources/load-balancing/client-coherence-cache-config.xml</code>
will be identical to the Client Cache Configuration files used for the name-service example.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;remote-cache-scheme&gt;
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt;
      &lt;initiator-config&gt;
        &lt;outgoing-message-handler&gt;
          &lt;request-timeout&gt;5s&lt;/request-timeout&gt;
        &lt;/outgoing-message-handler&gt;
      &lt;/initiator-config&gt;
    &lt;/remote-cache-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<p>In this example, we will beef up the usage of Oracle Bedrock quite a bit. In order to demonstrate the load-balancer,
we will create a Coherence Cluster with 4 nodes (members) and 3 Coherence*Extend clients that connect to those members.</p>

<markup
lang="java"

>@Test
void testCoherenceExtendConnection() throws InterruptedException {

    LocalPlatform platform = LocalPlatform.get();

    int numberOfServers = 4; <span class="conum" data-value="1" />
    int numberOfClients = 3; <span class="conum" data-value="2" />

    List&lt;CoherenceClusterMember&gt; servers = new ArrayList&lt;&gt;(numberOfServers);
    List&lt;CoherenceClusterMember&gt; clients = new ArrayList&lt;&gt;(numberOfClients);

    try {
        for (int i = 1; i &lt;= numberOfServers; i++) {
            CoherenceClusterMember server = platform.launch(CoherenceClusterMember.class,
                    CacheConfig.of("load-balancing/server-coherence-cache-config.xml"), <span class="conum" data-value="3" />
                    ClassName.of(Coherence.class),
                    LocalHost.only(),
                    Logging.atInfo(),
                    IPv4Preferred.yes(),
                    ClusterName.of("myCluster"),
                    RoleName.of("server"),
                    SystemProperty.of("coherence.log.level", "5"),
                    DisplayName.of("server-" + i));
            servers.add(server);
        }

        for (CoherenceClusterMember server : servers) {
            Eventually.assertDeferred(() -&gt; server.invoke(
                    new IsServiceRunning("MyCountryExtendService")), is(true)); <span class="conum" data-value="4" />
            assertThat(server.getExtendConnectionCount("MyCountryExtendService"), is(0)); <span class="conum" data-value="5" />
        }

        for (int i = 1; i &lt;= numberOfClients; i++) {
            CoherenceClusterMember client = platform.launch(CoherenceClusterMember.class,
                    CacheConfig.of("load-balancing/client-coherence-cache-config.xml"),  <span class="conum" data-value="6" />
                    ClassName.of(Coherence.class),
                    LocalHost.only(),
                    Logging.atInfo(),
                    IPv4Preferred.yes(),
                    SystemProperty.of("coherence.client", "remote"),
                    SystemProperty.of("coherence.tcmp.enabled", "false"),
                    SystemProperty.of("coherence.log.level", "5"),
                    ClusterName.of("myCluster"),
                    RoleName.of("client"),
                    DisplayName.of("client-" + i));
            clients.add(client);
        }

        for (CoherenceClusterMember client : clients) {
            Eventually.assertDeferred(client::isCoherenceRunning, is(true)); <span class="conum" data-value="7" />
            client.invoke(new Connect()); <span class="conum" data-value="8" />
        }

        int clientCount = servers.stream()
                .map(server -&gt; server.getExtendConnectionCount("MyCountryExtendService"))
                .reduce(0, Integer::sum);  <span class="conum" data-value="9" />

        assertThat(clientCount, is(3));  <span class="conum" data-value="10" />

        TimeUnit.MILLISECONDS.sleep(20000);  <span class="conum" data-value="11" />
    } finally {
        for (CoherenceClusterMember client : clients) {
            client.close();
        }
        for (CoherenceClusterMember server : servers) {
            server.close();
        }
    }
}</markup>

<ul class="colist">
<li data-value="1">First we specify the desired number of Coherence servers, 4 in this case</li>
<li data-value="2">We also need 3 Coherence*Extend clients</li>
<li data-value="3">In a loop we create the Coherence servers using the server cache configuration file</li>
<li data-value="4">For each server we make sure it is running</li>
<li data-value="5">None of the servers should have a client connected to them, yet</li>
<li data-value="6">Next we start all the clients using the client cache configuration file</li>
<li data-value="7">We also make sure that all clients are running</li>
<li data-value="8">Once running, we invoke a task on the client that establishes the Coherence*Extend connection. See the source code snippet
below</li>
<li data-value="9">Let&#8217;s introspect all the started servers. For each of them, we get the Coherence*Extend connection count for
the <code>MyCountryExtendService</code> and sum the result</li>
<li data-value="10">The client connection count should be <code>3</code></li>
<li data-value="11">Let&#8217;s wait for 20 seconds, so you can observe the logging activity of our custom load-balancer</li>
</ul>
<p>As mentioned above, we execute a task for each Coherence*Extend client, to establish the actual connection. In Bedrock, we
can submit a <code>RemoteCallable</code> to achieve this:</p>

<markup
lang="java"

>public static class Connect implements RemoteCallable&lt;UUID&gt; { <span class="conum" data-value="1" />
    @Override
    public UUID call() {
        Session session = Coherence.getInstance().getSession(); <span class="conum" data-value="2" />
        NamedCache&lt;Object, Object&gt; cache = session.getCache("countries"); <span class="conum" data-value="3" />
        Member member = cache.getCacheService().getInfo().getServiceMember(0);
        return member.getUuid();
    }
}</markup>

<ul class="colist">
<li data-value="1">Our class implements Bedrock&#8217;s <code>RemoteCallable</code> interface</li>
<li data-value="2">Get a Coherence session</li>
<li data-value="3">Retrieve the countries cache from the session</li>
</ul>

<h4 id="_run_the_test_2">Run the Test</h4>
<div class="section">
<p>When running the test you should notice the logging to the console from our <code>CustomProxyServiceLoadBalancer</code>:</p>

<markup
lang="bash"

>[server-1:out:44488]    2: Local Member Id: 1 (Total # of Members: 4) - Connection Count: 1
[server-3:out:44488]    2: Local Member Id: 4 (Total # of Members: 4) - Connection Count: 1
[server-2:out:44488]    2: Local Member Id: 2 (Total # of Members: 4) - Connection Count: 1
[server-4:out:44488]    2: Local Member Id: 3 (Total # of Members: 4) - Connection Count: 0</markup>

<p>As we have 4 Cluster Servers but only 3 clients, 1 Cluster Server will have 0 client connections, while each other server
has 1 client connection each.</p>

</div>
</div>

<h3 id="specific-host-port">Setting Specific Host and Port</h3>
<div class="section">
<p>Generally we recommend using the name service to connect to Coherence, but you may have specific firewall constraints.
In that case, you can configure the Coherence server to listen to a specific address and port instead.</p>

<p>The Server Cache Configuration file at <code>src/main/resources/firewall/server-coherence-cache-config.xml</code> will look almost
identical to the example using the name service. However, here we add an <code>&lt;acceptor-config&gt;</code> XML element.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;country-scheme&lt;/scheme-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme/&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
    &lt;proxy-scheme&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt;
      &lt;acceptor-config&gt; <span class="conum" data-value="1" />
        &lt;tcp-acceptor&gt;
          &lt;address-provider&gt;
            &lt;local-address&gt;
              &lt;address&gt;127.0.0.1&lt;/address&gt; <span class="conum" data-value="2" />
              &lt;port&gt;7077&lt;/port&gt; <span class="conum" data-value="3" />
            &lt;/local-address&gt;
          &lt;/address-provider&gt;
        &lt;/tcp-acceptor&gt;
      &lt;/acceptor-config&gt;
      &lt;load-balancer&gt;client&lt;/load-balancer&gt; <span class="conum" data-value="4" />
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/proxy-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">Add a <code>&lt;acceptor-config&gt;</code> element</li>
<li data-value="2">Define an explicit Coherence*Extend host address, in this case <code>127.0.0.1</code></li>
<li data-value="3">Define the port <code>7077</code> on which we will be listening for Coherence*Extend clients</li>
<li data-value="4">We need to set load-balancing to <code>client</code></li>
</ul>
<p>We will also create a corresponding Client Cache Configuration file at <code>src/main/resources/firewall/client-coherence-cache-config.xml</code>.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;countries&lt;/cache-name&gt;
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;remote-cache-scheme&gt;
      &lt;scheme-name&gt;remote-country-scheme&lt;/scheme-name&gt;
      &lt;service-name&gt;MyCountryExtendService&lt;/service-name&gt;
      &lt;initiator-config&gt; <span class="conum" data-value="1" />
        &lt;tcp-initiator&gt;
          &lt;remote-addresses&gt; <span class="conum" data-value="2" />
            &lt;socket-address&gt;
              &lt;address&gt;127.0.0.1&lt;/address&gt;
              &lt;port&gt;7077&lt;/port&gt;
            &lt;/socket-address&gt;
          &lt;/remote-addresses&gt;
        &lt;/tcp-initiator&gt;
        &lt;outgoing-message-handler&gt;
          &lt;request-timeout&gt;5s&lt;/request-timeout&gt;
        &lt;/outgoing-message-handler&gt;
      &lt;/initiator-config&gt;
    &lt;/remote-cache-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">Define the <code>&lt;initiator-config&gt;</code> element</li>
<li data-value="2">Specify the Coherence*Extend server address and port under the <code>&lt;remote-addresses&gt;</code> element</li>
</ul>
<p>The test case itself is identical to the name service test above:</p>

<markup
lang="java"

>@Test
void testFirewallUseCase() {
    System.setProperty("coherence.tcmp.enabled", "false");
    System.setProperty("coherence.cluster", "myCluster");
    System.setProperty("coherence.wka", "127.0.0.1");
    CoherenceHelper.startCoherenceClient(
            CoherenceHelper.FIREWALL_INSTANCE_NAME,
            "firewall/client-coherence-cache-config.xml");
    NamedCache&lt;String, Country&gt; countries = CoherenceHelper.getMap(CoherenceHelper.FIREWALL_INSTANCE_NAME, "countries"); <span class="conum" data-value="4" />
    countries.put("de", new Country("Germany", "Berlin", 83.2));
}</markup>


<h4 id="_run_the_test_3">Run the Test</h4>
<div class="section">
<p>The client should be able to connect the server on the explicitly defined host and port.</p>

</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide, we gave a few deeper examples of how to set up Coherence*Extend to connect clients to a Coherence Cluster.
As part of the Coherence reference documentation, we provide an entire guide on
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/">Developing Remote Clients for Oracle Coherence</a>. Part I of that
guide provides not only an introduction to Coherence*Extend but also covers advanced topics as well as best practices.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="/examples/guides/050-bootstrap/README">Bootstrap Coherence</router-link></p>
<ul class="ulist">
<li>
<p><router-link :to="{path: '/examples/guides/050-bootstrap/README', hash: '#run-extend'}">Run Coherence as an Extend Client</router-link></p>

</li>
<li>
<p><router-link :to="{path: '/examples/guides/050-bootstrap/README', hash: '#configure-extend'}">Configure an Extend Client</router-link></p>

</li>
</ul>
</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/introduction-coherenceextend.html#GUID-E935592F-DCA2-44BD-96D5-E276DFA3D3F9">Introduction to Coherence*Extend</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/configuring-extend-clients.html#GUID-B5E4F4D6-8A9D-4FD2-A0CE-B07C25DF580A">Configuring Extend Clients</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/advanced-extend-configuration.html#GUID-A5D5F565-1544-4840-80DC-97D052C54649">Advanced Extend Configuration</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-remote-clients/best-practices-coherenceextend.html#GUID-E7982E16-CC78-426C-9098-46F4FC8204A3">Best Practices for Coherence*Extend</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
