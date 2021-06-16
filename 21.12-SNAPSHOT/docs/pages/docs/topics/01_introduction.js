<doc-view>

<h2 id="_introduction_to_coherence_topics">Introduction to Coherence Topics</h2>
<div class="section">
<p>Coherence Topics introduces publish and subscribe messaging, also often referred to as streaming, functionality in Oracle Coherence.</p>


<h3 id="_overview">Overview</h3>
<div class="section">
<p>The Topics API enables the building of data pipelines between loosely coupled publishers and consumers.
One or more publishers publish a stream of values to a topic. One or more subscribers consume the stream of values from a topic. The topic values are spread evenly across all Oracle Coherence data servers, enabling high throughput processing in a distributed and fault tolerant manner.</p>


<h4 id="_version_compatibility">Version Compatibility</h4>
<div class="section">
<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p >Topics was first introduced in 14.1.1.0 and was considerable enhanced to improve message delivery guarantees in CE 21.06.
Whilst the changes made in 21.06 are API compatible, the underlying data-structures and entry processors used are
not backwards compatible, meaning that a rolling upgrade of a cluster using Topics from 14.1.1.0 (or CE version prior to 21.06) upgrading to CE 21.06 will not work.</p>
</div>
</div>
</div>

<h3 id="_concepts">Concepts</h3>
<div class="section">
<p>Some core concepts of Coherence Topics are described below.</p>


<h4 id="_channels">Channels</h4>
<div class="section">
<p>In order to scale publishers and subscribers whilst still guaranteeing message ordering, Coherence Topics introduces the concept of channels. A channel is similar in idea to how Coherence partitions data in distributed <code>NamedMap</code> but to avoid confusion the name partition was not reused. Channels are an important part of the operation of both publishers and subscribers.</p>

<p>A publisher is configured to control what ordering guarantees exist for the messages it publishes when they are received by subscribers. This is achieved by publishing messages to a specific channel. All messages published to a channel will be received by subscribers in the same order. Messages published to different channels may be interleaved as they are received by subscribers.</p>

<p>The number of channels that a topic has allows publishers to scale better as they avoid contention that may occur with many publishers publishing to a single channel. Message consumption can be scaled because multiple subscribers (in a group) will subscribe to different channels, so scaling up receiving of messages, whilst maintaining order.</p>

<p>The channel count for a topic is configurable, ideally a small prime (the default is 17). There are pros and cons with very small or very large channel counts, depending on the application use case and what sort of scaling or ordering guarantees it requires.</p>

</div>

<h4 id="_position">Position</h4>
<div class="section">
<p>Every element published to a topic has a position. A position is an opaque data structure used by the underlying <code>NamedTopic</code> implementation to track the position of an element in a channel and maintain ordering of messages. Positions are then used by subscribers to track the elements that they have received and when committing a position to determine which preceding elements are also committed and to then recover to the correct position in the topic when subscribers reconnect or recover from failure.</p>

<p>Whilst a position data structure is opaque, they are serializable, meaning that they can be stored into a separate data store by application code that wants to manually track message element processing. The combination of channel and position should be unique for each message element published and received.</p>

</div>

<h4 id="_namedtopic">NamedTopic</h4>
<div class="section">
<p>A <code>NamedTopic</code> is the name of the Coherence data structure that stores data as an ordered stream of messages. Generally most application code does not need to interact directly with a <code>NamedTopic</code>, but instead with a <code>Publisher</code> or <code>Subscriber</code> to either publish to or subscribe to a <code>NamedTopic</code>.</p>

<p>See: <router-link to="/docs/topics/02_configuring_topics">Configuring NamedTopics</router-link></p>

</div>
</div>

<h3 id="_publishers">Publishers</h3>
<div class="section">
<p>A <code>Publisher</code> publishes messages to topic. Each <code>Publisher</code> is created with a configurable ordering option that determines the ordering guarantees for how those messages are then received by <code>Subscribers</code>. The topic values are spread evenly across all Coherence storage enabled cluster members, enabling high throughput processing in a distributed and fault tolerant manner. Multiple publishers can be created to publish messages to the same topic.</p>

<p>See: <router-link to="/docs/topics/03_publishers">Publishers</router-link></p>

</div>

<h3 id="_subscribers">Subscribers</h3>
<div class="section">
<p>A <code>Subscriber</code> subscribes to a <code>NamedTopic</code> and receives messages from that topic. The subscriber mechanism is based on polling, as <code>Subscriber</code> calls its <code>receive</code> method to fetch the next message, messages are not pushed to a subscriber. The <code>Subscriber</code> API is asynchronous so applications can receive and process messages in a non-blocking "push-like" fashion using the <code>CompletableFuture</code> API.</p>

<p>There are two types of <code>Subscriber</code>, anonymous subscribers and group subscribers.</p>

<ul class="ulist">
<li>
<p>An anonymous subscriber connects to a topic and receives all messages published to that topic. Anonymous subscribers are not durable, if they are closed (or fail) then when they reconnect they are effectively seen as new subscribers and will restart processing messages from the topic&#8217;s tail (or from the head if the topic is configured to retain messages).</p>

</li>
<li>
<p>A group subscriber is part of a group of one or more subscribers that all belong to the same named subscriber group. In a subscriber group each message is only delivered to one of the members of the group. This allows message processing to be scaled up by using multiple subscribers that will process messages from the same topic, whilst maintaining the publishers ordering. Message delivery in a subscriber group is controlled by assigning ownership of the channels of the topic to each subscriber in the group, so that each subscriber is polling a sub-set of channels. As subscribers in a group are created, or closed, or fail, their channel ownership is reallocated across the remaining subscribers. When new subscribers are created in a group, channels from the existing subscribers are redistributed to the new subscriber. This all happens automatically to try to maintain an evenly balanced distribution of channels to group subscribers. Obviously if more subscribers are created in a group than there are channels in the topic then some of those subscribers cannot be allocated a channel as there are not enough channels to go around. A subscriber group is durable, if all the subscribers in a group are closed, next time a subscriber in the group re-subscribes it will start processing messages from next message after the last committed position.</p>

</li>
</ul>
<p>See: <router-link to="/docs/topics/04_subscribers">Subscribers</router-link></p>

</div>
</div>
</doc-view>
