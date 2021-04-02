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
        &lt;version&gt;21.06-M1&lt;/version&gt;
    &lt;/dependency&gt;</markup>

</div>
</div>

<h2 id="_access_coherence_resources">Access Coherence Resources</h2>
<div class="section">
<p>The simplest way to access remote Coherence resources, such as a <code>NamedMap</code> when using the gRPC client is via a
Coherence <code>Session</code>.</p>


<h3 id="_obtain_a_remote_session">Obtain a Remote Session</h3>
<div class="section">
<p>Client gRPC sessions to can be configured using system properties. By default, if no properties are provided a gRPC session named <code>default</code> will be configured to connect to <code>localhost:1408</code>.</p>

<p>For example, the code below will create a gRPC <code>Session</code> named <code>default</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

SessionConfiguration config = GrpcSessionConfiguration.builder().build();
Session session = Session.create(config);

NamedMap&lt;String, String&gt; map = session.getMap("foo");</markup>

</div>

<h3 id="_named_grpc_sessions">Named gRPC Sessions</h3>
<div class="section">
<p>Client sessions can be named, by providing a name to the configuration builder:
For example, the code below will create a gRPC <code>Session</code> named <code>foo</code>:</p>

<markup
lang="java"

>import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

SessionConfiguration config = GrpcSessionConfiguration.builder("foo").build();
Session session = Session.create(config);

NamedMap&lt;String, String&gt; map = session.getMap("foo");</markup>

</div>

<h3 id="_session_configuration_via_properties">Session Configuration via Properties</h3>
<div class="section">
<p>Client gRPC sessions can be configured using system properties.
The system property names follow the format <code>coherence.grpc.channels.&lt;name&gt;.xyz</code>, where <code>&lt;name&gt;</code> is replaced with the session name.</p>

<p>For example the property to set the the port to connect to is <code>coherence.grpc.channels.&lt;name&gt;.port</code>, so to configure the port for the default session to 9099, set the property <code>-Dcoherence.grpc.channels.default.port=9099</code></p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Property</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.host</code></td>
<td class="">The host name to connect to</td>
</tr>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.port</code></td>
<td class="">The port to connect to</td>
</tr>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.target</code></td>
<td class="">As an alternative to setting the host and port, setting target creates a channel using the <code>ManagedChannelBuilder.forTarget(target);</code> method (see the gRPC Java documentation).</td>
</tr>
</tbody>
</table>
</div>
<p>(replace <code>&lt;name&gt;</code> with the session name being configured).</p>

</div>

<h3 id="_using_tls">Using TLS</h3>
<div class="section">
<p>By default, the <code>Channel</code> used by the gRPC session will be configured as a plain text connection.
TLS can be configured by setting the required properties.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Property</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.credentials</code></td>
<td class="">Set to one of <code>plaintext</code>, <code>insecure</code> or <code>tls</code>. The default is <code>plaintext</code> and will create an insecure plain text channel. Using <code>insecure</code> will enable TLS but not verify the server certificate (useful in testing). Using <code>tls</code> will enable TLS on the client.</td>
</tr>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.tls.ca</code></td>
<td class="">The location of a CA file if required to verify the server certs.</td>
</tr>
</tbody>
</table>
</div>
<p>(replace <code>&lt;name&gt;</code> with the session name being configured).</p>

<p>If the server has been configured for mutual verification the client&#8217;s key and certificate can also be provided:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Property</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.tls.cert</code></td>
<td class="">The location of a client certificate file.</td>
</tr>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.tls.key</code></td>
<td class="">The location of a client key file.</td>
</tr>
<tr>
<td class=""><code>coherence.grpc.channels.&lt;name&gt;.tls.password</code></td>
<td class="">The optional password for the client key file.</td>
</tr>
</tbody>
</table>
</div>
<p>(replace <code>&lt;name&gt;</code> with the session name being configured).</p>

</div>

<h3 id="_create_a_session_with_a_custom_channel">Create a Session with a Custom Channel</h3>
<div class="section">
<p>If a fully custom channel configuration is required application code can configure the session with a <code>Channel</code>.</p>

<markup
lang="java"

>Channel channel = ManagedChannelBuilder.forAddress("localhost", 1408)
                                       .usePlaintext()
                                       .build();

SessionConfiguration config = GrpcSessionConfiguration.builder(channel)
        .named("foo")
        .build();

Session session = Session.create(config);

NamedMap&lt;String, String&gt; map = session.getMap("foo");</markup>

<p>The example above creates a simple gRPC channel to connect to <code>localhost:1408</code>.
A <code>Session</code> has been created with this channel by specifying the <code>GrpcSessions.channel(channel)</code> option.</p>

<p>Calls to <code>Session.create()</code> with the same parameters, in this case channel, will return the same <code>Session</code> instance.
Most gRPC <code>Channel</code> implementations do not implement an <code>equals()</code> method, so the same <code>Session</code> will only be returned for the exact same <code>Channel</code> instance.</p>


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

>Serializer serializer = new JsonSerializer();
String format = "json";

SessionConfiguration config = GrpcSessionConfiguration.builder()
        .withSerializer(serializer, format)
        .build();

Session session = Session.create(config);

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

>SessionConfiguration config = GrpcSessionConfiguration.builder()
        .withScopeName("foo")
        .build();

Session session = Session.create(config);</markup>

<p>In the example above the <code>GrpcSessions.scope("foo")</code> option is used to specify that the <code>Session</code> created should connect to resources on the server managed by the server side <code>Session</code> with the scope <code>foo</code>.</p>

</div>
</div>
</doc-view>
