<doc-view>

<h2 id="_coherence_java_grpc_client">Coherence Java gRPC Client</h2>
<div class="section">
<p>The Coherence Java gRPC Client is a library that allows a Java application to connect to a Coherence gRPC proxy server.</p>


<h3 id="_usage">Usage</h3>
<div class="section">
<p>In order to use Coherence gRPC client, you need to declare it as a dependency in your <code>pom.xml</code>.
There also needs to be a corresponding Coherence server running the gRPC proxy for the client to connect to.</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-java-client&lt;/artifactId&gt;
        &lt;version&gt;${coherence.version}&lt;/version&gt;
    &lt;/dependency&gt;</markup>

</div>
</div>

<h2 id="_access_coherence_resources">Access Coherence Resources</h2>
<div class="section">
<p>The simplest way to access remote Coherence resources, such as a <code>NamedMap</code> when using the gRPC client is via a
Coherence <code>Session</code>.</p>


<h3 id="_obtain_a_remote_session">Obtain a Remote Session</h3>
<div class="section">
<p>To obtain a <code>Session</code> that connects to a remote gRPC proxy the first requirement is a gRPC <code>Channel</code>.
The client application must create the gRPC <code>Channel</code> with the required configuration corresponding to that
required by the server (for example any security settings etc).</p>

<p>The <code>Channel</code> can then be used to create a <code>Session</code> using the Coherence <code>Session</code> factory methods.
The <code>Session</code> can ten be used to access remote resources.</p>

<markup
lang="java"

>Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                                       .usePlaintext()
                                       .build();

Session session = Session.create(GrpcSessions.channel(channel));

NamedMap&lt;String, String&gt; map = session.getMap("foo");</markup>

<p>The example above creates a simple gRPC channel to connect to <code>localhost:1408</code>.
A <code>Session</code> has been created with this channel by specifying the <code>GrpcSessions.channel(channel)</code> option.
The <code>Session</code> can used as a normal Coherence <code>Sessions</code> to create, maps, caches and topics.</p>

<p>Calls to <code>Session.create()</code> with the same parameters, in this case channel, will return the same <code>Session</code> instance.
Most gRPC <code>Channel</code> implementations do not implement an <code>equals()</code> method so the same <code>Session</code> will only be returned
for the exact same <code>Channel</code> instance.</p>


<h4 id="_close_a_session">Close a Session</h4>
<div class="section">
<p>When client code has finished with a <code>Session</code> it can be closed to free up and close any gRPC requests that are still
open by calling the <code>session.close()</code> method. This will also locally release (but not destroy) all Coherence resources
manged by that <code>Session</code>.</p>

</div>
</div>

<h3 id="_specify_a_serializer">Specify a Serializer</h3>
<div class="section">
<p>The <code>Serializer</code> used by the remote session will default to Java serialization, unless the system property
<code>coherence.pof.enabled</code> is set to <code>true</code>, in which case POF will be used for the serializer.
The serializer for a session can be set specifically when creating a <code>Session</code>.</p>

<markup
lang="java"

>Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                                       .usePlaintext()
                                       .build();

Serializer ser = new JsonSerializer();
String format = "json";

Session session = Session.create(GrpcSessions.channel(channel),
                                 GrpcSessions.serializer(ser, format));</markup>

<p>In the example above a json serializer is being used. The <code>GrpcSessions.serializer(ser, format)</code> session option is used
to specify the serializer and its format name. The format name will be used by the server to select the correct server
side serializer to process the session requests and responses.</p>

<div class="admonition note">
<p class="admonition-inline">The serializer format configured must also have a compatible serializer available on the server so that the server
can deserialize message payloads.</p>
</div>
</div>

<h3 id="_specify_a_scope_name">Specify a Scope Name</h3>
<div class="section">
<p>In most cases a Coherence server only has a single <code>ConfigurableCacheFactory</code>, but it is possible to run multiple and
hence multiple different cache services managed by a different <code>ConfigurableCacheFactory</code>.
Typically, a scope name will be used to isolate different <code>ConfigurableCacheFactory</code> instances.</p>

<p>A gRPC client session can be created for a specific server side scope name by specifying the scope as an option when
creating the session.</p>

<markup
lang="java"

>Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                                       .usePlaintext()
                                       .build();

Session session = Session.create(GrpcSessions.channel(channel),
                                 GrpcSessions.scope("foo"));</markup>

<p>In the example above the <code>GrpcSessions.scope("foo")</code> option is used to specify that the <code>Session</code> created should
connect to resources on the server managed by the <code>ConfigurableCacheFactory</code> with the scope <code>foo</code>.</p>

</div>
</div>
</doc-view>
