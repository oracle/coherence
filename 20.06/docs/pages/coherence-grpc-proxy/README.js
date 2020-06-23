<doc-view>

<h2 id="_coherence_grpc_server">Coherence gRPC Server</h2>
<div class="section">
<p>Coherence gRPC Server is the server-side implementation of the services defined within the <code>Coherence gRPC</code> module.</p>


<h3 id="_usage">Usage</h3>
<div class="section">
<p>In order to use Coherence gRPC Server, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-grpc-proxy&lt;/artifactId&gt;
  &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>When used <a id="" title="" target="_blank" href="http://helidon.io">Helidon</a> Microprofile Server, Coherence will be automatically started, and
the gRPC services will be detected and deployed.</p>

<p>The server can be started by just running the <code>main</code> method of the Helidon Microprofile CDI server
<code>io.helidon.microprofile.cdi.Main</code>,
The Coherence gRPC proxy service will be discovered by CDI and started.</p>

<p>Alternatively it is possible to start the Helidon server from code, for example:</p>

<markup
lang="java"

>public class MyServer
    {
    public static void main(String[] args)
        {
        Server server = Server.create().start();
        try
            {
            Thread.currentThread().join();
            }
        catch (InterruptedException e)
            {
            server.stop();
            }
        }
    }</markup>

<p>When reviewing the log output, you should see the following:</p>

<p>As Helidon uses Weld for its CDI implementation, we can see it being bootstrapped:</p>

<markup
lang="log"

>io.helidon.microprofile.cdi.LogConfig doConfigureLogging
INFO: Logging at initialization configured using defaults
org.jboss.weld.bootstrap.WeldStartup &lt;clinit&gt;
INFO: WELD-000900: 3.1.3 (Final)
...</markup>

<p>Next, Coherence is started:</p>

<markup
lang="log"

>Oracle Coherence Version 20.06 Build 0
Grid Edition: Development mode
Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.

Oracle Coherence GE 20.06 (dev) &lt;Info&gt; (thread=main, member=n/a): Loaded cache configuration from "jar:file: ...
...</markup>

<p>Lastly, the gRPC proxy and <code>NamedCacheService</code> are started up:</p>

<markup
lang="log"

>Started DefaultCacheServer...

io.helidon.grpc.server.GrpcServerImpl deploy
INFO: gRPC server [grpc.server]: registered service [coherence.NamedCacheService]
io.helidon.grpc.server.GrpcServerImpl deploy
INFO: gRPC server [grpc.server]:       with methods [coherence.NamedCacheService/addIndex]
io.helidon.grpc.server.GrpcServerImpl deploy
INFO: gRPC server [grpc.server]:                    [coherence.NamedCacheService/aggregate]
...
io.helidon.grpc.server.GrpcServerImpl deploy
INFO: gRPC server [grpc.server]:                    [coherence.NamedCacheService/values]
io.helidon.grpc.server.GrpcServerImpl start
INFO: gRPC server [grpc.server]: listening on port 1408 (TLS=false)
io.helidon.microprofile.grpc.server.GrpcServerCdiExtension lambda$startServer$3
INFO: gRPC server started on localhost:1408 (and all other host addresses) in 199 milliseconds.</markup>

<p>The service is now ready to process requests from the <code>coherence-java-client</code>.</p>

</div>

<h3 id="_configuration">Configuration</h3>
<div class="section">
<p>We recommend reviewing <a id="" title="" target="_blank" href="https://helidon.io/docs/v2/#/mp/guides/03_config">Helidon&#8217;s Microprofile Documentation</a> if not
familiar with how MP configuration works.</p>

<p>Here&#8217;s a simple <code>application.yaml</code> changing the default gRPC listen port:</p>

<markup
lang="yaml"

>server:
  port: 9091</markup>

<p>No change to the code previously used to start the server.  Run it again
to see that the port has indeed changed:</p>

<markup
lang="log"

>io.helidon.grpc.server.GrpcServerImpl start
INFO: gRPC server [grpc.server]: listening on port 9091 (TLS=false)
PM io.helidon.microprofile.grpc.server.GrpcServerCdiExtension lambda$startServer$3
INFO: gRPC server started on localhost:9091 (and all other host addresses) in 301 milliseconds.</markup>

<p>See <a id="" title="" target="_blank" href="https://helidon.io/docs/v2/apidocs/io.helidon.grpc.server/io/helidon/grpc/server/GrpcServerConfiguration.html">here</a>
for details about available configuration options for the gRPC proxy.</p>

</div>

<h3 id="_custom_grpc_services">Custom gRPC Services</h3>
<div class="section">
<p>See Helidon&#8217;s <a id="" title="" target="_blank" href="https://helidon.io/docs/v2/#/mp/grpc/01_mp_server_side_services">guidelines</a> for developing gRPC services.
As long as the server implementation is annotated with <code>javax.enterprise.context.ApplicationScoped</code> and
<code>io.helidon.microprofile.grpc.core.Grpc</code>, the gRPC proxy will deploy the service automatically.</p>

</div>
</div>
</doc-view>
