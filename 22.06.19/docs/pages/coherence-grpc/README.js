<doc-view>

<h2 id="_coherence_grpc">Coherence gRPC</h2>
<div class="section">

</div>

<h2 id="_developing_remote_clients_for_oracle_coherence">Developing Remote Clients for Oracle Coherence</h2>
<div class="section">

</div>

<h2 id="getting-started">Part V Getting Started with gRPC</h2>
<div class="section">
<p>Learn how to use the Coherence gRPC library to interact with a Coherence data management services.</p>

<p>This part contains the following chapters:</p>

<p><router-link to="#intro" @click.native="this.scrollFix('#intro')">Introduction to gRPC</router-link><br>
Coherence gRPC provides the protobuf definitions necessary to interact with a Coherence data management services over gRPC.</p>

<p><router-link to="#server" @click.native="this.scrollFix('#server')">Using the Coherence gRPC Server</router-link><br>
The Coherence gRPC proxy is the server-side implementation of the services defined within the Coherence gRPC module. The gRPC proxy uses standard gRPC Java libraries to provide Coherence APIs over gRPC.</p>

<p><router-link to="#client" @click.native="this.scrollFix('#client')">Using the Coherence Java gRPC Client</router-link><br>
The Coherence Java gRPC Client is a library that enables a Java application to connect to a Coherence gRPC proxy server.</p>


<h3 id="intro">24 Introduction to gRPC</h3>
<div class="section">
<p>Coherence gRPC for Java allows Java applications to access Coherence clustered services, including data, data events, and data processing from outside the Coherence cluster. Typical uses for Java gRPC clients include desktop and Web applications that require access to remote Coherence resources. This provides an alternative to using Coherence*Extend when writing client applications.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The Coherence gRPC client and Coherence Extend client feature sets do not match exactly, some functionality in gRPC is not available in Extend and vice-versa.</p>
</p>
</div>
<p>The Coherence gRPC for Java library connects to a Coherence clustered service instance running within the Coherence cluster using a high performance gRPC based communication layer. This library sends all client requests to the Coherence  clustered gRPC proxy service which, in turn, responds to client requests by delegating to an actual Coherence clustered service (for example, a partitioned cache service).</p>

<p>Like cache clients that are members of the cluster, Java gRPC clients use the <code>Session</code> API call to retrieve a resources such as <code>NamedMap</code>, <code>NamedCache</code>, etc. After it is obtained, a client accesses these resources in the same way as it would if it were part of the Coherence cluster. The fact that operations on Coherence resources are being sent to a remote cluster node (over gRPC) is completely transparent to the client application.</p>

<p>There are two parts to Coherence gRPC, the <code>coherence-grpc-proxy</code> module, that provides the server-side gRPC proxy, and the <code>coherence-java-client</code> module that provides the gRPC client. Other non-java Coherence clients are also available that use the Coherence gRPC protocol.</p>

</div>

<h3 id="server">25 Using the Coherence gRPC Proxy Server</h3>
<div class="section">
<p>The Coherence gRPC proxy is the server-side implementation of the gRPC services defined within the Coherence gRPC module. The gRPC proxy uses standard gRPC Java libraries to provide Coherence APIs over gRPC.</p>

<p>This chapter includes the following sections:</p>

<p><router-link to="#setting-up" @click.native="this.scrollFix('#setting-up')">Setting Up the Coherence gRPC Server</router-link><br>
To set up and start using the Coherence gRPC Server, you should declare it as a dependency of your project.</p>

<p><router-link to="#config-server" @click.native="this.scrollFix('#config-server')">Configuring the Server</router-link><br>
Configuring the gRPC Server includes setting the server port, specifying the in-process server name, and enabling TLS.</p>

<p><router-link to="#disable-server" @click.native="this.scrollFix('#disable-server')">Disabling the gRPC Proxy Server</router-link><br>
The Coherence gRPC server starts automatically based on the lifecycle events of <code>DefaultCacheServer</code>, but it can be disabled.</p>

<p><router-link to="#helidon" @click.native="this.scrollFix('#helidon')">Deploying the Proxy Service with Helidon Microprofile gRPC Server</router-link><br>
If you use the Helidon Microprofile server with the microprofile gRPC server enabled, you can deploy the Coherence gRPC proxy into the Helidon gRPC server instead of the Coherence default gRPC server.</p>


<h4 id="setting-up">Setting Up the Coherence gRPC Proxy Server</h4>
<div class="section">
<p>To set up and start using the Coherence gRPC Server, you should declare it as a dependency of your project.</p>

<p>For example:</p>

<p>If using Maven, declare the server as follows (where <code>coherence.groupId</code> is either the Coherence commercial group id, <code>com.oracle.coherence</code> or the CE group id <code>com.oracle.coherence.ce</code>, and the <code>coherence.version</code> property is the version of Coherence being used:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependencyManagement&gt;
    &lt;dependencies&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
            &lt;artifactId&gt;coherence-bom&lt;/artifactId&gt;
            &lt;version&gt;${coherence.version}&lt;/version&gt;
            &lt;type&gt;pom&lt;/type&gt;
            &lt;scope&gt;import&lt;/scope&gt;
        &lt;/dependency&gt;
    &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;

&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;/dependency&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-grpc-proxy&lt;/artifactId&gt;
    &lt;/dependency&gt;
&lt;dependencies&gt;</markup>

<p>Or with Gradle, declare the server as follows (where <code>coherenceGroupId</code> is either the Coherence commercial group id, <code>com.oracle.coherence</code> or the CE group id <code>com.oracle.coherence.ce</code>, and the <code>coherenceVersion</code> property is the version of Coherence being used:</p>

<markup
lang="groovy"
title="build.gradle"
>dependencies {
    implementation platform("${coherenceGroupId}:coherence-bom:${coherenceVersion}")

    implementation "${coherenceGroupId}:coherence"
    implementation "${coherenceGroupId}:coherence-grpc-proxy"
}</markup>

</div>

<h4 id="_starting_the_server">Starting the Server</h4>
<div class="section">
<p>The gRPC server starts automatically when you run <code>com.tangosol.coherence.net.Coherence</code> (or <code>com.tangosol.coherence.net.DefaultCacheServer</code>). Typically, <code>com.tangosol.coherence.net.Coherence</code> class should be used as the application?s main class. Alternatively, you can start an instance of <code>com.tangosol.coherence.net.Coherence</code> by using the Bootstrap API.</p>

<p>By default, the gRPC server will listen on all local addresses using an ephemeral port. Just like with Coherence*Extend, the endpoints the gRPC server has bound to can be discovered by a client using the Coherence NameService, so using ephemeral ports allows the server to start without needing to be concerned with port clashes.</p>

<p>When reviewing the log output, two log messages appear as shown below to indicate which ports the gRPC server has bound to.</p>

<markup


>In-Process GrpcAcceptor is now listening for connections using name "default"
GrpcAcceptor now listening for connections on 0.0.0.0:55550</markup>

<p>The service is ready to process requests from one of the Coherence gRPC client implementations.</p>

</div>

<h4 id="config-server">Configuring the Server</h4>
<div class="section">
<p>The Coherence gRPC proxy is configured using an internal default cache configuration file named <code>grpc-proxy-cache-config.xml</code> which only contains a single <code>&lt;proxy-scheme&gt;</code> configuration for the gRPC proxy. There is no reason to override this file as the server can be configured with System properties and environment variables.</p>


<h5 id="_configuring_the_grpc_server_listen_address_and_port">Configuring the gRPC Server Listen Address and Port</h5>
<div class="section">
<p>The address and port that the gRPC server binds to when starting can be configured at runtime by setting system properties or environment variables.</p>

<p>By default, the server binds to the address <code>0.0.0.0</code> which equates to all the local host&#8217;s network interfaces.
This can be changed by setting the <code>coherence.grpc.server.address</code> system property or <code>COHERENCE_GRPC_SERVER_ADDRESS</code> environment variable.</p>

<p>For example, if the host had a local IP address <code>192.168.0.25</code> the server could be configured to bind to just this address as follows:</p>

<p>Using System properties</p>

<markup


>-Dcoherence.grpc.server.address=192.168.0.2</markup>

<p>Using environment variables</p>

<markup
lang="bash"

>export COHERENCE_GRPC_SERVER_ADDRESS=192.168.0.2</markup>

<p>The port that the gRPC server binds to can be configured using the <code>coherence.grpc.server.port</code> system property or <code>COHERENCE_GRPC_SERVER_PORT</code> environment variable</p>

<p>For example, to configure the server to listen on port 1408:</p>

<p>Using System properties</p>

<markup


>-Dcoherence.grpc.server.port=1408</markup>

<p>Using environment variables</p>

<markup
lang="bash"

>export COHERENCE_GRPC_SERVER_PORT=1408</markup>

</div>

<h5 id="_configuring_ssltls">Configuring SSL/TLS</h5>
<div class="section">
<p>In common with the rest of Coherence, the Coherence gRPC server can be configured to use SSL by specifying the name of a socket provider. Named socket providers are configured in the Coherence operational configuration file (override file). There are various ways to configure an SSL socket provider, which are covered in the Coherence documentation section <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a></p>

<p>Once a named socket provider has been configured, the gRPC server can be configured to use that provider by setting the <code>coherence.grpc.server.socketprovider</code> system property or <code>COHERENCE_GRPC_SERVER_SOCKETPROVIDER</code> environment variable.</p>

<p>For example, if a socket provider named <code>tls</code> has been configured in the operational configuration file, the gRPC server can be configured to use it:</p>

<markup
lang="xml"
title="tangosol-coherence-override.xml"
>    &lt;socket-providers&gt;
      &lt;socket-provider id="tls"&gt;
        &lt;ssl&gt;
          &lt;identity-manager&gt;
            &lt;key system-property="coherence.security.key"&gt;server.key&lt;/key&gt;
            &lt;cert system-property="coherence.security.cert"&gt;server.cert&lt;/cert&gt;
          &lt;/identity-manager&gt;
          &lt;trust-manager&gt;
            &lt;cert system-property="coherence.security.ca.cert"&gt;server-ca.cert&lt;/cert&gt;
          &lt;/trust-manager&gt;
        &lt;/ssl&gt;
      &lt;/socket-provider&gt;
    &lt;/socket-providers&gt;</markup>

<p>Using System properties</p>

<markup


>-Dcoherence.grpc.server.socketprovider=tls</markup>

<p>Using environment variables</p>

<markup
lang="bash"

>export COHERENCE_GRPC_SERVER_SOCKETPROVIDER=tls</markup>

<p>For more information on socket providers see
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a></p>

</div>

<h5 id="_configuring_the_grpc_server_thread_pool">Configuring the gRPC Server Thread Pool</h5>
<div class="section">
<p>Like other Coherence services, the gRPC server uses a dynamically sized thread pool to process requests.
The thread pool size can be configured if the dynamic sizing algorithm provies to not be optimal.</p>

<p><strong>Set the Minimum Thread Count</strong></p>

<p>Adjusting the minimum number of threads can be useful when dealing with bursts in load.
Sometimes it can take the dynamic pool some time to increase the thread count to a suitable number to quickly deal with an increase in load. Setting the minimum size will ensure there are always a certain number of threads to service load.
The minimum number of threads in the pool can be set using the <code>coherence.grpc.server.threads.min</code> system property, or the <code>COHERENCE_GRPC_SERVER_THREADS_MIN</code> environment variable.</p>

<p>For example, the minimum thread count can be set to 10 as shown below:</p>

<p>Using System properties</p>

<markup


>-Dcoherence.grpc.server.threads.min=10</markup>

<p>Using environment variables</p>

<markup
lang="bash"

>export COHERENCE_GRPC_SERVER_THREADS_MIN=10</markup>

<p><strong>Set the Maximum Thread Count</strong></p>

<p>Adjusting the maximum number of threads can be useful to stop the dynamic pool going too high and consuming too much CPU resource.
The maximum number of threads in the pool can be set using the <code>coherence.grpc.server.threads.max</code> system property, or the <code>COHERENCE_GRPC_SERVER_THREADS_MAX</code> environment variable.
If both maximum and minimum thread counts are specified, the maximum thread count should obviously be set to a value higher than the minimum thread count.</p>

<p>For example, the maximum thread count can be set to 20 as shown below:</p>

<p>Using System properties</p>

<markup


>-Dcoherence.grpc.server.threads.max=20</markup>

<p>Using environment variables</p>

<markup
lang="bash"

>export COHERENCE_GRPC_SERVER_THREADS_MAX=20</markup>

</div>
</div>

<h4 id="disable-server">Disabling the gRPC Proxy Server</h4>
<div class="section">
<p>If the <code>coherence-grpc-proxy</code> module is on the class path (or module path) then the gRPC server will be started automatically. This behaviour can be disabled by setting the <code>coherence.grpc.enabled</code> system property or <code>COHERENCE_GRPC_ENABLED</code> environment variable to <code>false</code>.</p>

</div>
</div>

<h3 id="client">26 Using the Coherence Java gRPC Client</h3>
<div class="section">
<p>The Coherence Java gRPC Client is a library that enables a Java application to connect to a Coherence gRPC proxy server.</p>

<p>This chapter includes the following sections:</p>

<p><router-link to="#client-setup" @click.native="this.scrollFix('#client-setup')">Setting Up the Coherence gRPC Client</router-link><br>
To set up and start using the Coherence gRPC Client, you should declare it as an application dependency. There should also be a corresponding Coherence server running the gRPC proxy to which the client can connect.</p>

<p><router-link to="#client-config" @click.native="this.scrollFix('#client-config')">Configure the Coherence gRPC Client</router-link><br>
Add the gRPC client configuration to the application&#8217;s cache configuration file.</p>

<p><router-link to="#client-resources" @click.native="this.scrollFix('#client-resources')">Accessing Coherence Resources</router-link><br>
The simplest way to access the remote Coherence resources, such as a <code>NamedMap</code> when using the gRPC client is through a Coherence <code>Session</code>.</p>


<h4 id="client-setup">Setting Up the Coherence gRPC Client</h4>
<div class="section">
<p>To set up and start using the Coherence gRPC Java client, you should declare it as a dependency of your project. The gRPC client is provided in the <code>coherence-java-client</code> module.</p>

<p>For example:</p>

<p>If using Maven, declare the server as follows (where <code>coherence.groupId</code> is either the Coherence commercial group id, <code>com.oracle.coherence</code> or the CE group id <code>com.oracle.coherence.ce</code>, and the <code>coherence.version</code> property is the version of Coherence being used:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependencyManagement&gt;
    &lt;dependencies&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
            &lt;artifactId&gt;coherence-bom&lt;/artifactId&gt;
            &lt;version&gt;${coherence.version}&lt;/version&gt;
            &lt;type&gt;pom&lt;/type&gt;
            &lt;scope&gt;import&lt;/scope&gt;
        &lt;/dependency&gt;
    &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;

&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;/dependency&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-java-client&lt;/artifactId&gt;
    &lt;/dependency&gt;
&lt;dependencies&gt;</markup>

<p>Or with Gradle, declare the server as follows (where <code>coherenceGroupId</code> is either the Coherence commercial group id, <code>com.oracle.coherence</code> or the CE group id <code>com.oracle.coherence.ce</code>, and the <code>coherenceVersion</code> property is the version of Coherence being used:</p>

<markup
lang="groovy"
title="build.gradle"
>dependencies {
    implementation platform("${coherenceGroupId}:coherence-bom:${coherenceVersion}")

    implementation "${coherenceGroupId}:coherence"
    implementation "${coherenceGroupId}:coherence-java-client"
}</markup>

</div>

<h4 id="client-config">Configure the Coherence gRPC Client</h4>
<div class="section">
<p>Just like Coherence*Extend, a Coherence gRPC client accesses remote clustered resources by configuring remote schemes in the applications cache configuration file.</p>


<h5 id="_defining_a_remote_grpc_cache">Defining a Remote gRPC Cache</h5>
<div class="section">
<p>A remote gRPC cache is specialized cache service that routes cache operations to a cache on the Coherence cluster via the gRPC proxy. The remote cache and the cache on the cluster must have the same cache name. Coherence gRPC clients use the <code>NamedMap</code> or <code>NamedCache</code> interfaces as normal to get an instance of the cache. At runtime, the cache operations are not executed locally but instead are sent using gRPC to a gRPC proxy service on the cluster. The fact that the cache operations are delegated to a cache on the cluster is transparent to the client.</p>

<p>A remote gRPC cache is defined within a <code>&lt;caching-schemes&gt;</code> section using the <code>&lt;remote-grpc-cache-scheme&gt;</code> element.
There are two approaches to configure a gRPC client:</p>

<ul class="ulist">
<li>
<p>NameService - the gRPC client uses the Coherence NameService to discover the gRPC endpoints in the cluster. This is the simplest configuration. Coherence will discover all the endpoints in the cluster that the gRPC proxy is listening on and the gRPC Java library&#8217;s standard client-side load balancer will load balance connections from the client to those proxy endpoints.</p>

</li>
<li>
<p>Fixed Endpoints - a fixed set of gRPC endpoints can be supplied, either hard coded or via a custom <code>AddressProvider</code> configuration. If multiple endpoints are provided, the gRPC Java library&#8217;s standard client-side load balancer will load balance connections from the client to those proxy endpoints.</p>

</li>
</ul>
<p>Some approaches work in some types of deployment environment and not in others, for example the NameService configurations are not suitable where the cluster is inside a containerized environment, such as Kubernetes and the client is external to this. Choose the simplest configuration that works in your environment. If both clients and cluster are inside the same containerized environment the NameService will work. In containerized environments such as Kubernetes, this is typically configured with a single ingress point which load balances connections to the Coherence cluster Pods. The address of this ingress point is then used as a single fixed address in the <code>&lt;remote-grpc-cache-scheme&gt;</code> configuration.</p>

</div>

<h5 id="_a_minimal_nameservice_configuration">A Minimal NameService Configuration</h5>
<div class="section">
<p>The simplest configuration for a gRPC client is to use the NameService to locate the gRPC proxy endpoints, but without adding any address or port information in the <code>&lt;remote-grpc-cache-scheme&gt;</code> in the configuration file. This configuration will use Coherence&#8217;s default cluster discovery mechanism to locate the Coherence cluster&#8217;s NameService and look up the gRPC endpoints. This requires the client to be configured with the same cluster name and well-known-address list (or multicast configuration) as the cluster being connected to.</p>

<p>The example below shows a <code>&lt;remote-grpc-cache-scheme&gt;</code> configured with just <code>&lt;scheme-name&gt;</code> and <code>&lt;service-name&gt;</code> elements. This is the absolute minimum, required configuration.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;caching-scheme-mapping&gt;
   &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
         &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
   &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
   &lt;remote-grpc-cache-scheme&gt;
      &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
      &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
   &lt;/remote-grpc-cache-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</div>

<h5 id="_a_minimal_nameservice_configuration_with_different_cluster_name">A Minimal NameService Configuration with Different Cluster Name</h5>
<div class="section">
<p>If the client is configured with a different cluster name to the cluster being connected to (for example the client is actually in a different Coherence cluster), then the <code>&lt;remote-grpc-cache-scheme&gt;</code> can be configured with a cluster name.</p>

<p>For example, the <code>&lt;remote-grpc-cache-scheme&gt;</code> below is configured with <code>&lt;cluster-name&gt;test-cluster&lt;/cluster-name&gt;</code> so Coherence will use the NameService to discover the gRPC endpoints in the Coherence cluster named <code>test-cluster</code>.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;caching-scheme-mapping&gt;
   &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
         &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
   &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
   &lt;remote-grpc-cache-scheme&gt;
      &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
      &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
      &lt;cluster-name&gt;test-cluster&lt;/cluster-name&gt;
   &lt;/remote-grpc-cache-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</div>

<h5 id="_configure_the_nameservice_endpoints">Configure the NameService Endpoints</h5>
<div class="section">
<p>If the client cannot use the standard Coherence cluster discovery mechanism to look up the target cluster, the NameService endpoints can be supplied in the <code>&lt;grpc-channel&gt;</code> section of the <code>&lt;remote-grpc-cache-scheme&gt;</code> configuration.</p>

<p>The example below creates a remote cache scheme that is named <code>RemoteGrpcCache</code>, which connects to the Coherence NameService on <code>198.168.1.5:7574</code>, which then redirects the request to the address of the gRPC proxy service.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;caching-scheme-mapping&gt;
   &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
         &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
   &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
    &lt;remote-grpc-cache-scheme&gt;
        &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
        &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
        &lt;grpc-channel&gt;
            &lt;name-service-addresses&gt;
               &lt;socket-address&gt;
                  &lt;address&gt;198.168.1.5&lt;/address&gt;
                  &lt;port&gt;7574&lt;/port&gt;
               &lt;/socket-address&gt;
            &lt;/name-service-addresses&gt;
        &lt;/grpc-channel&gt;
    &lt;/remote-grpc-cache-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</div>

<h5 id="_configure_fixed_endpoints">Configure Fixed Endpoints</h5>
<div class="section">
<p>If the NameService cannot be used to discover the gRPC endpoints, a fixed set of addresses can be configured.
In the <code>&lt;grpc-channel&gt;</code> section, configure a <code>&lt;remote-addresses&gt;</code> element containing one or more <code>&lt;socket-address&gt;</code> elements.</p>

<p>For example, the client configured below will connect to a gRPC proxy listening on the endpoint <code>test-cluster.svc:1408</code>.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;caching-scheme-mapping&gt;
   &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
         &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
   &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
    &lt;remote-grpc-cache-scheme&gt;
        &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
        &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
        &lt;grpc-channel&gt;
            &lt;remote-addresses&gt;
               &lt;socket-address&gt;
                  &lt;address&gt;test-cluster.svc&lt;/address&gt;
                  &lt;port&gt;1408&lt;/port&gt;
               &lt;/socket-address&gt;
            &lt;/remote-addresses&gt;
        &lt;/grpc-channel&gt;
    &lt;/remote-grpc-cache-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</div>

<h5 id="_configure_ssl">Configure SSL</h5>
<div class="section">
<p>To configure the client to use SSL a socket provider can be configured in the <code>&lt;grpc-channel&gt;</code> section.
Socket providers are configured exactly the same way as in other parts of Coherence.
The <code>&lt;socket-provider&gt;</code> element can either contain the name of a socket provider configured in the Operational override file, or can be configured with an inline socket provider configuration.</p>

<p>For example, the <code>&lt;remote-grpc-cache-scheme&gt;</code> is configured with a reference to the socket provider named <code>ssl</code> that is configured in the operational override file.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;remote-grpc-cache-scheme&gt;
    &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
    &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
    &lt;grpc-channel&gt;
        &lt;remote-addresses&gt;
           &lt;socket-address&gt;
              &lt;address&gt;test-cluster.svc&lt;/address&gt;
              &lt;port&gt;1408&lt;/port&gt;
           &lt;/socket-address&gt;
        &lt;/remote-addresses&gt;
        &lt;socket-provider&gt;ssl&lt;/socket-provider&gt;
    &lt;/grpc-channel&gt;
&lt;/remote-grpc-cache-scheme&gt;</markup>

<p>The <code>&lt;remote-grpc-cache-scheme&gt;</code> below is configured with an inline socket provider.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;remote-grpc-cache-scheme&gt;
    &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
    &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
    &lt;grpc-channel&gt;
        &lt;remote-addresses&gt;
           &lt;socket-address&gt;
              &lt;address&gt;test-cluster.svc&lt;/address&gt;
              &lt;port&gt;1408&lt;/port&gt;
           &lt;/socket-address&gt;
        &lt;/remote-addresses&gt;
        &lt;socket-provider&gt;
            &lt;ssl&gt;
                &lt;identity-manager&gt;
                    &lt;key&gt;server.key&lt;/key&gt;
                    &lt;cert&gt;server.cert&lt;/cert&gt;
                &lt;/identity-manager&gt;
                &lt;trust-manager&gt;
                    &lt;cert&gt;server-ca.cert&lt;/cert&gt;
                &lt;/trust-manager&gt;
            &lt;/ssl&gt;
        &lt;/socket-provider&gt;
    &lt;/grpc-channel&gt;
&lt;/remote-grpc-cache-scheme&gt;</markup>

<p>For more information on socket providers see
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a></p>

</div>

<h5 id="_configuring_the_client_thread_pool">Configuring the Client Thread Pool</h5>
<div class="section">
<p>Unlike an Extend client, the gRPC client is built on top of a gRPC asynchronous client.
This is configured with a thread pool, to allow the client to process multiple parallel requests and responses.
The thread pool used by the gRPC client is a standard Coherence dynamically sized thread pool, the number of threads will automatically be adjusted depending on load.
Sometimes Coherence does not adjust the thread pool optimally for an application use-case, so it can be configured to set the pool size. Any of the thread count, minimum thread count and maximum thread count can be configured.
Obviously the thread-count must be greater than or equal to the minimum count, and less than or equal the maximum count,
and the maximum count must be greater than or equal to the minimum count.</p>

<p>To configure a fixed size pool, just set the minimum and maximum to the same value.</p>

<p>The example below configures all three thread counts. The pool will start with 10 threads and by automatically sized between 5 and 15 threads depending on load.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;remote-grpc-cache-scheme&gt;
    &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
    &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
    &lt;grpc-channel&gt;
        &lt;remote-addresses&gt;
           &lt;socket-address&gt;
              &lt;address&gt;test-cluster.svc&lt;/address&gt;
              &lt;port&gt;1408&lt;/port&gt;
           &lt;/socket-address&gt;
        &lt;/remote-addresses&gt;
    &lt;/grpc-channel&gt;
    &lt;thread-count&gt;10&lt;/thread-count&gt;
    &lt;thread-count-max&gt;15&lt;/thread-count-max&gt;
    &lt;thread-count-min&gt;5&lt;/thread-count-min&gt;
&lt;/remote-grpc-cache-scheme&gt;</markup>

</div>
</div>

<h4 id="client-resources">Accessing Coherence Resources</h4>
<div class="section">
<p>As the gRPC client is configured as a remote scheme in the cache configuration file, Coherence resources can be accessed using the same Coherence APIs as used on cluster members or Extend clients.</p>

<p>If the client has been started using the Coherence bootstrap API, running a <code>com.tangosol.net.Coherence</code> instance, a <code>Session</code> and <code>NamedMap</code> can be accessed as shown below:</p>

<markup
lang="java"

>Session session = Coherence.getInstance().getSession();
NamedMap&lt;String, String&gt; map = session.getMap("test-cache");</markup>


<h5 id="_using_a_remote_grpc_cache_as_a_back_cache">Using a Remote gRPC Cache as a Back Cache</h5>
<div class="section">
<p>A remote gRPC cache can be used as the back cache of a near-cache or a view-cache in the same way as other types of caches.</p>

<p>The example below shows a near scheme configured to use a <code>&lt;remote-grpc-cache-scheme&gt;</code> as the back scheme.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;caching-scheme-mapping&gt;
   &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
         &lt;scheme-name&gt;near&lt;/scheme-name&gt;
   &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;

&lt;caching-schemes&gt;
    &lt;near-scheme&gt;
      &lt;scheme-name&gt;near&lt;/scheme-name&gt;
      &lt;front-scheme&gt;
        &lt;local-scheme&gt;
          &lt;high-units&gt;10000&lt;/high-units&gt;
        &lt;/local-scheme&gt;
      &lt;/front-scheme&gt;
      &lt;back-scheme&gt;
        &lt;remote-grpc-cache-scheme&gt;
          &lt;scheme-ref&gt;remote-grpc&lt;/scheme-ref&gt;
        &lt;/remote-grpc-cache-scheme&gt;
      &lt;/back-scheme&gt;
    &lt;/near-scheme&gt;

    &lt;remote-grpc-cache-scheme&gt;
      &lt;scheme-name&gt;remote-grpc&lt;/scheme-name&gt;
      &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
    &lt;/remote-grpc-cache-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</div>
</div>
</div>
</div>
</doc-view>
