<doc-view>

<h2 id="_coherence_grpc_java_client">Coherence gRPC Java Client</h2>
<div class="section">

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>The Coherence gRPC Java client is a CDI enabled library that allows a CDI application to inject Coherence objects
into CDI beans.</p>

<p>In order to use Coherence gRPC Java client, you need to declare it as a dependency in your <code>pom.xml</code></p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-java-client&lt;/artifactId&gt;
        &lt;version&gt;${coherence.version}&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<div class="admonition note">
<p class="admonition-inline">Using the Coherence gRPC Java client assumes that there is a corresponding server process running that is
using the Coherence gRPC proxy to expose the required gRPC services.</p>
</div>
<p>Once the necessary dependency is in place, the simplest way to start using it is to just inject Coherence resources
into the application&#8217;s beans. A lot of the annotations and qualifiers are identical to those described in the
<router-link to="#coherence-cdi/README.adoc" @click.native="this.scrollFix('#coherence-cdi/README.adoc')">Coherence CDI</router-link> documentation. The major difference being that the <code>@Remote</code> qualifier
is used to indicate that the Coherence beans to be injected connect remotely (in this case using gRPC) to a
Coherence server.</p>

<p>The following sections describe different injection points in more detail.</p>

<ul class="ulist">
<li>
<p><router-link to="#connections" @click.native="this.scrollFix('#connections')">Configuring gRPC Connections</router-link></p>

</li>
<li>
<p><router-link to="#sessions" @click.native="this.scrollFix('#sessions')">Configuring Coherence Remote Sessions</router-link></p>

</li>
<li>
<p><router-link to="#inject-coherence-objects" @click.native="this.scrollFix('#inject-coherence-objects')">Injecting Coherence Objects into CDI Beans</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#inject-namedmap" @click.native="this.scrollFix('#inject-namedmap')">Injecting <code>NamedMap</code>, <code>NamedCache</code>, and related objects</router-link></p>

</li>
<li>
<p><router-link to="#inject-session" @click.native="this.scrollFix('#inject-session')"><code>Session</code> Injection</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#cdi-events" @click.native="this.scrollFix('#cdi-events')">Using CDI Observers to Handle Coherence Map or Cache Events</router-link></p>
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
<p><router-link to="#cdi-events-session" @click.native="this.scrollFix('#cdi-events-session')">Observe events for maps and caches owned by a specific <code>Session</code></router-link></p>

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

<h3 id="connections">Remote Connections</h3>
<div class="section">
<p>The Coherence gRPC client will attempt to connect to a default server endpoint on <code>localhost:1409</code> if no other channel
has been configured. This is fine for development and testing but in most real-world applications the client will need
to know the endpoint to connect to. Most applications would only require a channel to connect to a single Coherence
cluster but there are use-cases where clients connect to multiple clusters, and the Coherence gRPC Java client supports
these use-cases too.</p>

<p>The Coherence gRPC client has been built on top of the
<a id="" title="" target="_blank" href="https://helidon.io/docs/v2/#/mp/grpc/02_mp_clients">Helidon Microprofile gRPC</a> library and uses it to provide gRPC
channels.</p>


<h4 id="_configure_grpc_channels">Configure gRPC Channels</h4>
<div class="section">
<p>Remote gRPC connections are configured using Helidon configuration, typically this would be a configuration file, but
Helidon supports many ways to provide the configuration, or override the configuration with System properties or
environment variables. The examples here will just use a configuration file.</p>

<p>All gRPC channels are configured in the <code>grpc.channels</code> section of the application configuration.
The example below is a simple configuration for a gRPC channel:</p>

<markup
lang="yaml"

>grpc:
  channels:
    - name: default            <span class="conum" data-value="1" />
      host: storage.acme.com   <span class="conum" data-value="2" />
      port: 1408               <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">The name of the channel is <code>default</code>.</li>
<li data-value="2">The host name of the gRPC server is <code>storage.acme.com</code></li>
<li data-value="3">The port which the server is listening on is <code>1408</code></li>
</ul>
<p>The <code>default</code> channel name is a special case that the Coherence gRPC client will use to locate a channel configuration
if no channel name has been specified in CDI injection points.</p>

<p>The example below shows a configuration with multiple channels, one named <code>test</code> and one named <code>prod</code>.</p>

<markup
lang="yaml"

>grpc:
  channels:
    - name: test
      host: test.storage.acme.com
      port: 1408
    - name: prod
      host: storage.acme.com
      port: 1408</markup>

<p>The configuration may contain as many channels as required, the only stipulation being that each has a unique name.</p>

</div>
</div>

<h3 id="sessions">Coherence Sessions</h3>
<div class="section">
<p>Coherence uses the concept of a <code>Session</code> to manage a set of related Coherence resources, such as maps, caches,
topics, etc. When using the Coherence Java gRPC client a <code>Session</code> connects to a specific gRPC channel (described above)
and uses a specific serialization format to marshal requests and responses. This means that different sessions
using different serializers may connect to the same server endpoint. Typically, for efficiency the client and server
would be configured to use matching serialization formats to avoid deserialization of data on the server but this does
not have to be the case. If the server is using a different serializer for the server side caches it must be able
to deserialize the client&#8217;s requests, so there must be a serializer configured on the server to match that used by the
client.</p>

<p>As with gRPC channels above, Coherence Sessions can be configured using Helidon configuration.
Coherence sessions are configured in the <code>coherence.sessions</code> section of the configuration.
Each session has its own entry in the configuration hierarchy, as shown below:</p>

<markup
lang="yaml"

>coherence:
  sessions:
    - name: default
      serializer: pof
      channel: default</markup>

<p>The example above shows configuration for the <code>default</code> Coherence session, this is the session that will be used to
provide Coherence beans when no session name has been specified for an injection point.
In this example, the default session will use POF serialization and connect to the server using the <code>default</code> gRPC
channel.</p>

<p>The default session, if not configured, will use the <code>default</code> channel and will use Java serialization.</p>

<p>As with channels, multiple sessions can be configured:</p>

<markup
lang="yaml"

>coherence:
  sessions:
    - name: test
      serializer: pof
      channel: test
    - name: prod
      serializer: pof
      channel: prod

grpc:
  channels:
    - name: test
      host: test.storage.acme.com
      port: 1408
    - name: prod
      host: storage.acme.com
      port: 1408</markup>

<p>In the example above, there are two Coherence sessions configured and two corresponding gRPC channels.</p>


<h4 id="_referring_to_sessions_at_injection_points">Referring to Sessions at Injection Points</h4>
<div class="section">
<p>Coherence CDI uses the <code>@Remote</code> qualifier to indicate that the Coherence bean to be injected refers to a remote
resource. In the case of the gRPC client, the value set when using the <code>@Remote</code> qualifier refers to the name of the
name of the Coherence session to use.</p>

<p>For example:</p>

<markup
lang="java"

>@Remote <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">The <code>@Remote</code> annotation has no value, so the Coherence gRPC client CDI extensions will look-up the configuration
for the Session named <code>default</code>.</li>
</ul>
<markup
lang="java"

>@Remote("test") <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">The <code>@Remote</code> annotation here has a value of <code>test</code>, so the Coherence gRPC client CDI extensions will look-up the
configuration for a session named <code>test</code>.</li>
</ul>
</div>
</div>

<h3 id="inject-coherence-objects">Injecting Coherence Objects into CDI Beans</h3>
<div class="section">
<p>A number of commonly used Coherence objects can be injected when using Java gRPC client.</p>

</div>

<h3 id="inject-namedmap">Injecting NamedMap NamedCache and Related Objects</h3>
<div class="section">
<p>In order to inject an instance of a <code>NamedMap</code> into your gRPC client CDI bean, you simply need to define an injection
point for it:</p>

<markup
lang="java"

>@Inject
@Remote  <span class="conum" data-value="1" />
private NamedMap&lt;Long, Person&gt; people;</markup>

<ul class="colist">
<li data-value="1">The important annotation here is the <code>@Remote</code> qualifier that tell&#8217;s the Coherence CDI extensions that the map to be
injected is remote. In this case the <code>NamedMap</code> will come from the <code>default</code> Coherence session as the <code>@Remote</code>
annotation does not specify a session name.</li>
</ul>
<markup
lang="java"

>@Inject
@Remote("products")  <span class="conum" data-value="1" />
private NamedMap&lt;Long, Product&gt; products;</markup>

<ul class="colist">
<li data-value="1">In this example the Coherence CDI extensions will use the <code>products</code> session to provide the client side <code>NamedMap</code>
backed on the server by a <code>NamedMap</code> called <code>products</code>.</li>
</ul>
<p>Other remote resources, such a <code>NamedCache</code> can be injected the same way:</p>

<markup
lang="java"

>@Inject
@Remote
private NamedCache&lt;Long, Product&gt; products;</markup>

<p>The <router-link to="#coherence-cdi/README.adoc" @click.native="this.scrollFix('#coherence-cdi/README.adoc')">Coherence CDI</router-link> documentation covers the different types of resources supported by CDI.
When using them with the gRPC Java client, remember to also include the <code>@Remote</code> qualifier on the injection point.</p>

</div>

<h3 id="inject-session">Injecting Sessions</h3>
<div class="section">
<p>If an application bean requires multiple maps or caches where the names will only be known at runtime then a
Coherence <code>com.tangosol.net.Session</code> can be injected instead of other specific named resources.
The required maps or caches can then be obtained from the <code>Session</code> by calling methods such as <code>Session.getMap</code> or
<code>Session.getCache</code>, etc.</p>

<markup
lang="java"

>@Inject
@Remote   <span class="conum" data-value="1" />
private Session session;</markup>

<ul class="colist">
<li data-value="1">The plain <code>@Remote</code> qualifier has been used, so the default <code>Session</code> will be injected here.</li>
</ul>
<markup
lang="java"

>@Inject
@Remote("products")   <span class="conum" data-value="1" />
private Session session;</markup>

<ul class="colist">
<li data-value="1">The <code>@Remote</code> qualifier has the value <code>products</code>, so the <code>Session</code> injected here will be configured from the
<code>coherence.sessions.products</code> session configuration.</li>
</ul>
</div>

<h3 id="cdi-events">Using CDI Observers to Handle MapEvents</h3>
<div class="section">
<p>The Coherence <code>NamedMap</code> and <code>NamedCache</code> APIs allow implementations of <code>MapListener</code> to be added that will then
receive events as map/cache entries get inserted, updated or deleted. When using CDI it is possible to subscribe
to the same events using CDI observer methods.</p>

<p>For example, to observe events raised by a <code>NamedMap</code> with the name <code>people</code>, with keys of type <code>Long</code> and values of
type <code>Person</code>, you would define a CDI observer such as this one:</p>

<markup
lang="java"

>private void onMapChange(@Observes
                         @Remote
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

<div class="admonition note">
<p class="admonition-inline">The important qualifier here is the <code>@Remote</code> annotation. This tells the Coherence CDI extensions that the map
or cache to be observed is a remote cache.</p>
</div>
<p>The <code>Observes</code> qualifier is what makes this method a standard CDI observer.</p>

<p>The <code>MapName</code> qualifier determines which map/cache to observer. If this qualifier is not present events from all caches
will be observed.</p>


<h4 id="cdi-event-types">Observe Specific Event Types</h4>
<div class="section">
<p>The observer method above will receive all events for the <code>people</code> map, but you can also control the types of events
received using event type qualifiers.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Qualifier</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>@Inserted</code></td>
<td class="">Observes insert events, raised when new entries are added to a map or cache.</td>
</tr>
<tr>
<td class=""><code>@Updated</code></td>
<td class="">Observes update events, raised when entries in a map or cache are modified.</td>
</tr>
<tr>
<td class=""><code>@Deleted</code></td>
<td class="">Observes deleted events, raised when entries are deleted from a map or cache.</td>
</tr>
</tbody>
</table>
</div>
<p>For example:</p>

<markup
lang="java"

>private void onUpdate(@Observes @Updated @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle UPDATED events raised by the 'people' map/cache
}

private void onAddOrRemove(@Observes @Inserted @Deleted @MapName("people") MapEvent&lt;?, ?&gt; event) {
    // handle INSERTED and DELETED events raised by the 'people' map/cache
}</markup>

<p>The first observer method above will observe only update events.
Multiple event type qualifiers can be added, so the second observer method will observer insert or delete events.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The client supports connecting to a server using different named <code>Sessions</code> and different named <code>Scopes</code>.
The observer methods above are not qualified with either session name or scope name so will observe events for
<strong>all</strong> maps or caches with the name <code>people</code> in <strong>all</strong> sessions and scopes.</p>

<p>In most Coherence use-cases that only use a single client session and a single default server side scope this is not
an issue but is something to be aware of if using multiple sessiosn or scopes.</p>

<p>See the following sections on how to qualify the observer to restrict the maps and caches it observes.</p>
</p>
</div>
</div>

<h4 id="cdi-events-session">Observe Events for Maps and Caches from Specific Sessions</h4>
<div class="section">
<p>In addition, to the <code>@MapName</code> qualifier, you can also specify a <code>Session</code> name as a way to limit the events received
to maps or caches from a specific <code>Session</code>. This is achieved by specifying a value for the <code>@Remote</code> qualifier.
See the <router-link to="#sessions" @click.native="this.scrollFix('#sessions')">Sessions</router-link> section for more details on multiple `Session`s.</p>

<p>For example:</p>

<markup
lang="java"

>private void onMapChange(@Observes
                         @Remote("test")
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache owned by the test Session.
}</markup>

<p>In the example above the <code>@Remote</code> qualifier has a value <code>test</code>, so the events will only be observed from the <code>people</code>
map on the server that corresponds to the map of the same name owned by the client side <code>Session</code> named <code>test</code>.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Maps or caches in different client side <code>Sessions</code> may correspond to the same server side map or cache and hence
events in one server side map or cache can be observed by multiple client side observers.</p>

<p>For example:<br>
Suppose a Map named <code>people</code> has been created in the default scope on the server.<br>
On the client there are two <code>Sessions</code> configured, <code>session-one</code> and <code>session-two</code> but both of these connect to the
same server and have the same default scope.</p>

<p>The two observers below are on the client:</p>

<markup
lang="java"

>private void onMapChange(@Observes
                         @Remote("session-one")
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    //...
}

private void onMapChange(@Observes
                         @Remote("session-two")
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    //...
}</markup>

<p>In this case both observer methods are actually observing the same server-side map and will receive the same events
event though they have different qualifiers.</p>
</p>
</div>
</div>

<h4 id="cdi-events-scopes">Observe Events for Maps and Caches from Specific Server-side Scopes</h4>
<div class="section">
<p>In addition, to the <code>@MapName</code> qualifier, you can also specify a scope name as a way to limit the events received
to maps or caches from a specific server-side scope name.
This is achieved by specifying a value for the <code>@ScopeName</code> qualifier.
See the <router-link to="#sessions" @click.native="this.scrollFix('#sessions')">Sessions</router-link> section for more details on multiple `Session`s.</p>

<p>For example:</p>

<markup
lang="java"

>private void onMapChange(@ObservesAsync
                         @Remote
                         @ScopeName("employees")
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache owned by the employees scope.
}</markup>

<p>In the example above the <code>@ScopeName</code> qualifier has a value <code>employees</code>, so the events will only be observed from the
<code>people</code> map in by the scope named <code>employees</code> on the server.</p>

</div>

<h4 id="cdi-events-filter">Filter Observed Events</h4>
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
private void onMapChange(@Observes @Remote @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

</div>

<h4 id="cdi-events-transform">Transform Observed Events</h4>
<div class="section">
<p>When an event observer does not want to receive the full cache or map value in an event, the event can be transformed
into a different value to be observed. This is achieved using a <code>MapEventTransformer</code> that is applied to the observer
method using either an <code>ExtractorBinding</code> annotation or a <code>MapEventTransformerBinding</code> annotation.
Transformation of events happens on the server so can make observer&#8217;s more efficient as they do not need to receive
the original event with the full old and new values.</p>

<p><strong>Transforming Events Using ExtractorBinding Annotations</strong></p>

<p>An <code>ExtractorBinding</code> annotation is an annotation that represents a Coherence <code>ValueExtractor</code>.
When an observer method has been annotated with an <code>ExtractorBinding</code> annotation the resulting <code>ValueExtractor</code> is
applied to the event&#8217;s values, and a new event will be returned to the observer containing just the extracted
properties.</p>

<p>For example, an event observer that is observing events from a map named <code>people</code>, but only requires the last name,
the built in <code>@PropertyExtractor</code> annotation can be used.</p>

<markup
lang="java"

>@PropertyExtractor("lastName")
private void onMapChange(@Observes @Remote @MapName("people") MapEvent&lt;Long, String&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

<p>Unlike the previous examples above the received events of type <code>MapEvent&lt;Long, Person&gt;</code> this method will receive
events of type <code>MapEvent&lt;Long, String&gt;</code> because the property extractor will be applied to the <code>Person</code>
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
private void onMapChange(@Observes @Remote @MapName("people") MapEvent&lt;Long, List&lt;String&gt;&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

<p>Note, now the event is of type <code>MapEvent&lt;Long, List&lt;String&gt;&gt;</code> because multiple extracted values will be returned the
event value is a <code>List</code> and in this case both properties are of tyep <code>String</code>, so the value can be <code>List&lt;String&gt;</code>.</p>

<p><strong>Transforming Events Using MapEventTransformerBinding Annotations</strong></p>

<p>If more complex event transformations are required than just extracting properties from event values, a custom
<code>MapEventTransformerBinding</code> can be created that will produce a custom <code>MapEventTransformer</code> instance that will be
applied to the observer&#8217;s events.
See the <router-link to="#custom-transformer" @click.native="this.scrollFix('#custom-transformer')">Custom MapEventTransformerBinding Annotations</router-link> section below for details on how to create
<code>MapEventTransformerBinding</code> annotations.</p>

</div>

<h4 id="cdi-events-async">Using Asynchronous Observers</h4>
<div class="section">
<p>All the examples above used synchronous observers by specifying the <code>@Observes</code> qualifier for each observer method.
However, Coherence CDI fully supports asynchronous CDI observers as well.
All you need to do is replace <code>@Observes</code> with <code>@ObservesAsync</code> in any of the examples above.</p>

<markup
lang="java"

>private void onMapChange(@ObservesAsync
                         @Remote
                         @MapName("people") MapEvent&lt;Long, Person&gt; event) {
    // handle all events raised by the 'people' map/cache
}</markup>

</div>
</div>
</div>
</doc-view>
