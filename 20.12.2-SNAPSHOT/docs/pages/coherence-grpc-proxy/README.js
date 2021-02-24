<doc-view>

<h2 id="_coherence_grpc_server">Coherence gRPC Server</h2>
<div class="section">
<p>Coherence gRPC proxy is the server-side implementation of the services defined within the <code>Coherence gRPC</code> module.
The gRPC proxy uses standard gRPC Java libraries to provide Coherence APIs over gRPC.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence gRPC Server, you need to declare it as a dependency of your project;
for example if using Maven:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-grpc-proxy&lt;/artifactId&gt;
  &lt;version&gt;20.12.2-SNAPSHOT&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>or for Gradle:</p>

<markup
lang="groovy"

>implementation 'com.oracle.coherence.ce:coherence-grpc-proxy:20.12.2-SNAPSHOT'</markup>


<h3 id="_start_the_server">Start the Server</h3>
<div class="section">
<p>The gRPC server will start automatically when <code>com.tangosol.coherence.net.DefaultCacheServer</code> is started. Typically,
<code>DefaultCacheServer</code> will be used as the application&#8217;s main class, alternatively an instance of <code>DefaultCacheServer</code>
can be started by calling its <code>main()</code> method directly from application initialisation code:</p>

<markup
lang="java"

>import com.tangosol.net.DefaultCacheServer;

public class MyApplication
    {
    public static void main(String[] args)
        {
        // do application initialisation...

        // this is a blocking call...
        DefaultCacheServer.main(args);
        }
    }</markup>

<p>or using the non-blocking <code>startServerDaemon</code> method:</p>

<markup
lang="java"

>import com.tangosol.net.DefaultCacheServer;

public class MyApplication
    {
    public static void main(String[] args)
        {
        // do application initialisation...

        DefaultCacheServer.startServerDaemon();

        // do more application initialisation...
        }
    }</markup>

<p>When reviewing the log output, you should see the following two log messages:</p>

<markup
lang="log"

>Coherence gRPC proxy is now listening for connections on 0.0.0.0:1408
Coherence gRPC in-process proxy 'default' is now listening for connections</markup>

<p>The service is now ready to process requests from one of the Coherence gRPC client implementations.</p>

</div>
</div>

<h2 id="_configuration">Configuration</h2>
<div class="section">
<p>The default gRPC server will listen for remote connections on port <code>1408</code> as well as in-process connections on an
in-process server named <code>default</code>.</p>


<h3 id="_set_the_port">Set the Port</h3>
<div class="section">
<p>The port the gRPC server listens on can be changed using the <code>coherence.grpc.server.port</code> system property,
for example <code>-Dcoherence.grpc.server.port=7001</code> will cause the server to bind to port <code>7001</code>.</p>

</div>

<h3 id="_set_the_in_process_server_name">Set the In-Process Server Name</h3>
<div class="section">
<p>The name used by the in-process server can be changed using the <code>coherence.grpc.inprocess.name</code> system property,
for example <code>-Dcoherence.grpc.inprocess.name=foo</code> will set the in-process server name to <code>foo</code>.</p>

</div>

<h3 id="_advanced_grpc_proxy_server_configuration">Advanced gRPC Proxy Server Configuration</h3>
<div class="section">
<p>It is possible to have full control over the configuration of the server by implementing the interface
<code>com.oracle.coherence.grpc.proxy.GrpcServerConfiguration</code>. Implementations of this interface will be loaded
using the Java <code>ServiceLoader</code> before the server starts allowing the <code>ServerBuilder</code> used to build both the
server and in-process server to be modified.</p>

<p>For example, the class below implements <code>GrpcServerConfiguration</code> and configures both servers to use
transport security certificates.</p>

<markup
lang="java"
title="MyServerConfig.java"
>package com.acme.application;

import com.oracle.coherence.grpc.proxy.GrpcServerConfiguration;

import io.grpc.ServerBuilder;import io.grpc.inprocess.InProcessServerBuilder;

public class MyServerConfig
        implements GrpcServerConfiguration
    {
    public void configure(ServerBuilder&lt;?&gt; serverBuilder, InProcessServerBuilder inProcessServerBuilder)
        {
        File fileCert = new File("/grpc.crt");
        File fileKey  = new File("grpc.key");
        serverBuilder.useTransportSecurity(fileCert, fileKey);
        inProcessServerBuilder.useTransportSecurity(fileCert, fileKey);
        }
    }</markup>

<p>For the Coherence gRPC proxy to find the above configuration class via the <code>ServiceLoader</code> a file named
<code>com.oracle.coherence.grpc.proxy.GrpcServerConfiguration</code> needs to be added to application classes <code>META-INF/services</code>
directory.</p>

<markup

title="com.oracle.coherence.grpc.proxy.GrpcServerConfiguration"
>com.acme.application.MyServerConfig</markup>

<p>When the gRPC proxy starts it will now discover the <code>MyServerConfig</code> and will call it to modify the server builders.</p>

<p>As well as security as in the example, other configuration such as interceptors and even additional gRPC services can be
added to the server before it starts.</p>

</div>
</div>

<h2 id="_disabling_the_grpc_proxy_server">Disabling the gRPC Proxy Server</h2>
<div class="section">
<p>As already stated above, the Coherence gRPC server will be started automatically based on <code>DefaultCacheServer</code> lifecycle
events. This behaviour can be disabled by setting the <code>coherence.grpc.enabled</code> system property to <code>false</code>, in which case
a gRPC server will not be started.</p>

</div>

<h2 id="_programmatically_starting_the_grpc_proxy_server">Programmatically starting the gRPC Proxy Server</h2>
<div class="section">
<p>If the <code>coherence.grpc.enabled</code> system property has been set to <code>false</code>, the gRPC server can be started manually by
calling the <code>start()</code> method on the <code>GrpcController</code> singleton instance, for example:</p>

<markup
lang="java"

>import com.oracle.coherence.grpc.proxy.GrpcServerController;

public class MyApplication
    {
    public static void main(String[] args)
        {
        // do application initialisation...

        GrpcServerController.INSTANCE.start();

        // do more application initialisation...
        }
    }</markup>

<p>The gRPC server can be stopped by calling the corresponding <code>GrpcServerController.INSTANCE.stop()</code> method.</p>

</div>

<h2 id="_waiting_for_grpc_server_start">Waiting For gRPC Server Start</h2>
<div class="section">
<p>If you have application code that needs to run only after the gRPC server has started this can be achieved by using
the <code>GrpcServerController.whenStarted()</code> method. This method returns a <code>CompletionStage</code> that will be completed
when the gRPC server has started.</p>

<markup
lang="java"

>GrpcServerController.INSTANCE.whenStarted().thenRun(() -&gt; {
    // run post-start code...
    System.out.println("The gRPC server has started");
});</markup>

</div>

<h2 id="_deploy_the_proxy_service_with_helidon_microprofile_grpc_server">Deploy the Proxy Service with Helidon Microprofile gRPC Server</h2>
<div class="section">
<p>If using the <a id="" title="" target="_blank" href="https://helidon.io">Helidon Microprofile server</a> with the microprofile gRPC server enabled the Coherence
gRPC proxy can be deployed into the Helidon gRPC server instead of the Coherence default gRPC server.</p>

<p>For this behaviour to happen automatically just set the <code>coherence.grpc.enabled</code> system property to <code>false</code>, which
will disable the built in server. A built-in <code>GrpcMpExtension</code> implementation will then deploy the proxy services
to the Helidon gRPC server.</p>

<div class="admonition warning">
<p class="admonition-inline">When using the Helidon MP gRPC server, if the <code>coherence.grpc.enabled</code> system property <strong>has not</strong> been set to
<code>false</code>, then both the Helidon gRPC server and the Coherence default gRPC server will start and could cause port
binding issues unless they are both specifically configured to use different ports.</p>
</div>
</div>

<h2 id="_manually_deploy_the_grpc_proxy_service">Manually Deploy the gRPC Proxy Service</h2>
<div class="section">
<p>If you are running your own instance of a gRPC server and want to just deploy the Coherence gRPC proxy service to this
server then that is possible.</p>

<div class="admonition note">
<p class="admonition-inline">If manually deploying the service, ensure that auto-start of the Coherence gRPC server has been disabled by
setting the system property <code>coherence.grpc.enabled=false</code></p>
</div>
<markup
lang="java"

>// Create your gRPC ServerBuilder
ServerBuilder builder = ServerBuilder.forPort(port);

// Obtain the Coherence gRPC services and add them to the builder
List&lt;BindableService&gt; services = GrpcServerController.INSTANCE.createGrpcServices()
services.forEach(serverBuilder::addService);

// Build and start the server
Server server = serverBuilder.build();
server.start();</markup>

</div>
</doc-view>
