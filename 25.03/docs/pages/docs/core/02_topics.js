<doc-view>

<h2 id="_topics">Topics</h2>
<div class="section">
<p>The main documentation on Coherence topics can be found in the commercial Coherence documentation
 <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/using-topics.html">Using Topics</a> section.
The documentation below covers new features added to topics since the latest commercial release.</p>


<h3 id="_remote_topics">Remote Topics</h3>
<div class="section">
<p>Coherence topics can be used from Extend and gRPC clients.
Coherence topics running on a client use remote topic services to communicate with a proxy on the server.
A remote topic is configured in the cache configuration file the same way as caches are configured.</p>

<p>A remote topic is specialized topic service that routes operations to a topic on the Coherence cluster. The remote topic and the topic on the cluster must have the same topic name. Extend clients use the NamedTopic interface as normal to get an instance of the topic. At run time, the topic operations are not executed locally but instead are sent using TCP/IP to an Extend or gRPC proxy service on the cluster. The fact that the topic operations are delegated to a topic on the cluster is transparent to the client code. There is no API difference between topics on a cluster member and topics on a client.</p>


<h4 id="_defining_a_remote_topic">Defining a Remote Topic</h4>
<div class="section">
<p>In the cache configuration file, a remote topic is defined within the <code>&lt;caching-schemes&gt;</code> node using the <code>&lt;remote-topic-scheme&gt;</code> element for Extend clients or <code>&lt;remote-grpc-topic-scheme&gt;</code> element for gRPC clients.</p>

<p>The example below creates an Extend remote topic service that is named RemoteTopic and connects to the name service, which then redirects the request to the address of the requested proxy service. The use of the name service simplifies port management and firewall configuration.</p>

<markup
lang="xml"

>...
&lt;topic-scheme-mapping&gt;
   &lt;topic-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;topic-name&gt;extend-topic&lt;/scheme-name&gt;
   &lt;/topic-mapping&gt;
&lt;/topic-scheme-mapping&gt;

&lt;caching-schemes&gt;
   &lt;remote-topic-scheme&gt;
      &lt;scheme-name&gt;extend-topic&lt;/scheme-name&gt;
      &lt;service-name&gt;RemoteTopic&lt;/service-name&gt;
   &lt;/remote-topic-scheme&gt;
&lt;/caching-schemes&gt;
...</markup>

<p>The next example below creates a gRPC remote topic service that is named RemoteTopic and connects to the name service, which then redirects the request to the address of the requested gRPC proxy service. The use of the name service simplifies port management and firewall configuration.</p>

<markup
lang="xml"

>...
&lt;topic-scheme-mapping&gt;
   &lt;topic-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;topic-name&gt;grpc-topic&lt;/scheme-name&gt;
   &lt;/topic-mapping&gt;
&lt;/topic-scheme-mapping&gt;

&lt;caching-schemes&gt;
   &lt;remote-grpc-topic-scheme&gt;
      &lt;scheme-name&gt;grpc-topic&lt;/scheme-name&gt;
      &lt;service-name&gt;RemoteTopic&lt;/service-name&gt;
   &lt;/remote-grpc-topic-scheme&gt;
&lt;/caching-schemes&gt;
...</markup>

</div>
</div>

<h3 id="_subscribing_to_specific_channels">Subscribing to Specific Channels</h3>
<div class="section">
<p>Data in a Coherence topic is partitioned into channels.
A full explanation of channels is given in the commercial documentation
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/introduction-coherence-topics.html#GUID-8B389C21-BAD8-43DD-A08C-94162B575E37">About Channels</a> section.</p>

<p>By default, the server manages allocations of channels to subscribers to ensure that every channel is owned by a subscriber in a subscriber group. Each time a new subscriber subscribes to a group or unsubscribes from a group the channels are reallocated.</p>

<p>A new feature has been introduced to allow application code to manually decide which channel or channels a subscriber will subscribe to. This allows applications more control over assigning subscribers to channels but
with the caveat that it is then up to the application to ensure that the channels subscribed to correspond to the channels being published to otherwise a subscriber may not receive any messages or alternatively the messages
published by a publisher may never be received by a subscriber.</p>

<ul class="ulist">
<li>
<p>If a subscriber is manually allocated channels then it will be allocated those channels and only those channels.</p>

</li>
<li>
<p>If multiple subscribers in a subscriber group are manually allocated the same channels then only one of them will be assigned as the channel owner. Exactly which subscriber this will be is not deterministic. For example if Subscriber-A and Subscriber-B both try to subscriber to channel 1, then only one will own the channel. If it was Subscriber-A that was assigned ownership, then Subscriber-B would receive no messages from channel 1.
But, if Subscriber-A was closed, then channel 1 would be reassigned to Subscriber-B.</p>

</li>
<li>
<p>It is possible to mix subscribers with manual allocations and subscribers with auto-allocations. For example if Subscriber-A was manually subscribed to channels 0, 1 and 2 and Subscriber-B had no manual allocations then Subscriber-B would automatically be assigned all the remaining channels.</p>

</li>
</ul>
<p>A subscriber can be manually allocated channels using the <code>Subscriber.subscribeTo()</code> option when it is created.
For example:</p>

<p>A subscriber could subscribe to channel 1 as shown below:</p>

<markup
lang="java"

>Subscriber&lt;MyMessage&gt; subscriber = topic.createSubscriber(Subscriber.subscribeTo(1));</markup>

<p>A subscriber could subscribe to multiple channels 0, 1, 3 and 16 as shown below:</p>

<markup
lang="java"

>Subscriber&lt;MyMessage&gt; subscriber = topic.createSubscriber(Subscriber.subscribeTo(0, 1, 3, 16));</markup>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Any invalid channel numbers used in the <code>Subscriber.subscribeTo()</code> method will be ignored.
If all the channel identifiers passed to <code>Subscriber.subscribeTo()</code> are invalid the subscriber will not be subscribed to any channels.</p>
</p>
</div>
</div>

<h3 id="_topics_resilience_and_availability">Topics Resilience and Availability</h3>
<div class="section">
<p>On a cluster member topics are highly resilient and will continue to function during rolling upgrades of cluster members. As the topics API is generally asynchronous, API methods that publish to or subscribe to topics will continue to function, but will just take longer to complete. Publishing and subscribing on a cluster member can also survive the loss of all storage enabled members as the publish and subscribe operations will pause until storage members restart.</p>

<p>In an Extend or gRPC client, just like with caches, a client will reconnect if the proxy it is connected to dies, or it becomes disconnected for some other reason such as a network issue. This means that generally a topics operation will work as long as the client is able to reconnect to a proxy. Publishers and subscribers will attempt to connect behind the scenes for asynchronous calls, so these to would generally succeed. Where there may be issues or exceptions thrown is when a request is actually in-flight when the proxy connection fails. In this case, just like with caches, the caller will receive an exception, but the operation may still be executing on the cluster and may actually complete. For a publisher for example, the client may receive an exception and then has no knowledge whether a publish request was actually successful on the cluster. It is up to the application code to properly handle the <code>CompletableFuture</code> returned from asynchronous topic API calls and decide what action to take on errors.</p>


<h4 id="_anonymous_subscribers">Anonymous Subscribers</h4>
<div class="section">
<p>An anonymous subscriber is not durable and hence if disconnected from the cluster and later reconnects it may
miss messages that were previously unread or that were published while it was disconnected.</p>

</div>
</div>
</div>
</doc-view>
