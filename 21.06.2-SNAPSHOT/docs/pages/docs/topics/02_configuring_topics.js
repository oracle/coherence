<doc-view>

<h2 id="_configure_coherence_topics">Configure Coherence Topics</h2>
<div class="section">
<p>Coherence topics are configured in the cache configuration file.</p>

<p>This section includes the following topics:</p>

<ul class="ulist">
<li>
<p><router-link to="#topic-mapping" @click.native="this.scrollFix('#topic-mapping')">Defining Topic Mappings</router-link> - A topic mapping maps a topic name to a paged topic scheme definition.</p>
<ul class="ulist">
<li>
<p><router-link to="#groups" @click.native="this.scrollFix('#groups')">Define Subscriber Groups</router-link> - Subscriber groups can be defined in topic mappings</p>

</li>
</ul>
</li>
<li>
<p><router-link to="#scheme" @click.native="this.scrollFix('#scheme')">Defining a Distributed Topic Scheme</router-link> - Topic schemes are used to define the topic services that are available to an application.</p>
<ul class="ulist">
<li>
<p><router-link to="#channel-count" @click.native="this.scrollFix('#channel-count')">Channel Count</router-link> - configure the number of channels in a topic</p>

</li>
<li>
<p><router-link to="#retain" @click.native="this.scrollFix('#retain')">Retaining Messages (Topics as Persistent Logs)</router-link> - retain messages after consumption (rewindable topics)</p>

</li>
<li>
<p><router-link to="#serializer" @click.native="this.scrollFix('#serializer')">Configure the message Serializer</router-link></p>

</li>
<li>
<p><router-link to="#subscriber-timeout" @click.native="this.scrollFix('#subscriber-timeout')">Configure the Subscriber Timeout</router-link> - configure the group subscriber timout</p>

</li>
<li>
<p><router-link to="#storeage" @click.native="this.scrollFix('#storeage')">Storage Options for Topic Values</router-link></p>

</li>
<li>
<p><router-link to="#page-size" @click.native="this.scrollFix('#page-size')">Page Size</router-link> - configure the size of a page in a paged topic</p>

</li>
<li>
<p><router-link to="#size-limit" @click.native="this.scrollFix('#size-limit')">Size Limited Topics</router-link></p>

</li>
<li>
<p><router-link to="#expiry" @click.native="this.scrollFix('#expiry')">Expiring Messages</router-link> - expiring messages</p>

</li>
</ul>
</li>
</ul>

<h3 id="topic-mapping">Defining Topic Mappings</h3>
<div class="section">
<p>A topic mapping maps a topic name to a paged topic scheme definition.The mappings provide a level of separation between applications and the underlying topic definitions. The separation allows topic implementations to be changed as required without having to change application code. Topic mappings have optional initialization parameters that are applied to the underlying paged topic scheme definition.
Topic mappings are defined using a <code>&lt;topic-mapping&gt;</code> element within the <code>&lt;topic-scheme-mapping&gt;</code> node. Any number of topic mappings can be created. The topic mapping must include the topic name, and the scheme name to which the topic name is mapped.</p>


<h4 id="_using_exact_topic_mappings">Using Exact Topic Mappings</h4>
<div class="section">
<p>An exact topic mapping maps a specific topic name to a topic scheme definition. An application must provide the exact name as specified in the mapping to use a topic. The slash (/) and colon (:) are reserved characters and cannot be used in topic names. The example below creates a single topic mapping that maps the topic name <code>exampleTopic</code> to a <code>paged-topic-scheme definition with the scheme name `example-topic-scheme</code>.</p>

<markup
lang="xml"
title="Sample Exact Topic Mapping"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
   &lt;topic-scheme-mapping&gt;
      &lt;topic-mapping&gt;
         &lt;topic-name&gt;exampleTopic&lt;/topic-name&gt;
         &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
      &lt;/topic-mapping&gt;
   &lt;/topic-scheme-mapping&gt;
   &lt;caching-schemes&gt;
      &lt;paged-topic-scheme&gt;
         &lt;scheme-name&gt;example-topic-scheme&lt;/scheme-name&gt;
         &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
      &lt;/paged-topic-scheme&gt;
   &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

</div>

<h4 id="_using_named_pattern_topic_mappings">Using Named Pattern Topic Mappings</h4>
<div class="section">
<p>Name pattern topic mappings allow applications to use patterns when specifying a topic name. Patterns use the asterisk (<code>*</code>) wildcard. Name patterns alleviate an application from having to know the exact name of a topic. The slash (/) and colon (:) are reserved characters and cannot be used in topic names. The example below a topic mappings using the wildcard (<code>\*</code>) to map any topic name with the prefix <code>account-</code> to a <code>paged-topic-scheme</code> definition with the scheme name <code>account-topic-scheme</code>.</p>

<markup
lang="xml"
title="Sample Topic Name Pattern Mapping"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;
    &lt;topic-scheme-mapping&gt;
      &lt;topic-mapping&gt;
          &lt;topic-name&gt;account-*&lt;/topic-name&gt;
          &lt;scheme-name&gt;account-topic-scheme&lt;/scheme-name&gt;
      &lt;/topic-mapping&gt;
    &lt;/topic-scheme-mapping&gt;

    &lt;caching-schemes&gt;
      &lt;paged-topic-scheme&gt;
          &lt;scheme-name&gt;account-topic-scheme&lt;/scheme-name&gt;
          &lt;service-name&gt;AccountDistributedTopicService&lt;/service-name&gt;
      &lt;/paged-topic-scheme&gt;
    &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

</div>

<h4 id="groups">Subscriber Group</h4>
<div class="section">
<p>A topic can have zero, one, or more durable subscriber groups defined in the topic-mapping for the topic. The subscriber group(s) are created along with the topic and are therefore ensured to exist before any data is published to the topic.</p>

<div class="admonition note">
<p class="admonition-inline">A subscriber group does not have to be defined on a topic’s topic-mapping for a subscriber to be able to join its group. Groups can be created and destroyed dynamically at runtime in application code.</p>
</div>
<p>The example below adds the subscriber group <code>durableSubscription</code> to the <code>exampleTopic</code> mapping.
The <code>subscriber-groups</code> element can contain multiple <code>subscriber-group</code> elements to add as many groups as the application requires.</p>

<markup
lang="xml"
title="Sample Durable Subscriber Group"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

   &lt;topic-scheme-mapping&gt;
      &lt;topic-mapping&gt;
         &lt;topic-name&gt;exampleTopic&lt;/topic-name&gt;
         &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
         &lt;subscriber-groups&gt;
             &lt;subscriber-group&gt;
                 &lt;name&gt;durableSubscription&lt;/name&gt;
             &lt;/subscriber-group&gt;
         &lt;subscriber-groups&gt;
      &lt;/topic-mapping&gt;
   &lt;/topic-scheme-mapping&gt;

   &lt;caching-schemes&gt;
      &lt;paged-topic-scheme&gt;
         &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
         &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
      &lt;/paged-topic-scheme&gt;
   &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

</div>
</div>

<h3 id="scheme">Defining a Distributed Topic Scheme</h3>
<div class="section">
<p>Topic schemes are used to define the topic services that are available to an application.Topic schemes provide a declarative mechanism that allows topics to be defined independent of the applications that use them. This removes the responsibility of defining topics from the application and allows topics to change without having to change an application&#8217;s code. Topic schemes also promote topic definition reuse by allowing many applications to use the same topic definition.
Topic schemes are defined within the &lt;caching-schemes&gt; element. A &lt;paged-topic-scheme&gt; scheme element and its properties are used to define a topic of that type.</p>


<h4 id="_sample_distributed_topic_definition">Sample Distributed Topic Definition</h4>
<div class="section">
<p>The <code>&lt;paged-topic-scheme&gt;</code> element is used to define distributed topics. A distributed topic utilizes a distributed (partitioned) topic service instance. Any number of distributed topics can be defined in a cache configuration file.</p>

<p>The example below defines a basic distributed topic that uses <code>distributed-topic</code> as the scheme name and is mapped to the topic name <code>example-topic</code>. The <code>&lt;autostart&gt;</code> element is set to <code>true</code> to start the service on a cache server node.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

   &lt;topic-scheme-mapping&gt;
     &lt;topic-mapping&gt;
       &lt;topic-name&gt;example-topic&lt;/topic-name&gt;
       &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
     &lt;/topic-mapping&gt;
   &lt;/topic-scheme-mapping&gt;

   &lt;caching-schemes&gt;
     &lt;paged-topic-scheme&gt;
        &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
        &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
        &lt;autostart&gt;true&lt;/autostart&gt;
     &lt;/paged-topic-scheme&gt;
   &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<p>A <code>paged-topic-scheme</code> has various configuration elements, discussed further below.</p>

</div>

<h4 id="channel-count">Channel Count</h4>
<div class="section">
<p>Channels are used by topics both as a way to increase parallel processing of messages and also to retain published ordering. The number of channels in a topic is configurable using the <code>&lt;channel-count&gt;</code> sub-element of the <code>&lt;paged-topic-scheme&gt;</code>.</p>

<p>The default number of channels is based on the partition count of the underlying cache service used by the topic. With the Coherence default partition count of 257 giving a default topic channel count of 17.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
    &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;channel-count&gt;3&lt;/channel-count&gt; <span class="conum" data-value="1" />
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The example above configures the topics mapped to the <code>distributed-topic</code> scheme to have a channel count of <code>3</code>.</li>
</ul>
<p>Whether increasing or decreasing the channel count makes sense depends on how an application will publish messages, and the ordering guarantees required. To help show the pros and cons we&#8217;ll look at both the very small and the very big.</p>


<h5 id="_very_small_channel_count">Very Small Channel Count</h5>
<div class="section">
<p>The smallest channel count possible would be one. With one channel all messages published will go to this single channel. The channel will have a single tail location where messages are published to and subscribed from.</p>

<p>If there is only a single publisher that requires ordering of all messages it publishes then a single channel would work. If there are multiple publishers then with a single channel all publishers will publish to this one channel and there will be contention on the tail of the topic if the publishers all try to publish at the same time. If the multiple publishers require global ordering of messages across publishers then one channel will give this at the cost of performance due to contention. If the publishers are not publishing very often, the contention would be reduced.</p>

<p>Multiple anonymous subscribers can all subscribe to a single channel and receive messages in order.</p>

<p>Using a subscriber group on a single channel topic does not allow multiple subscribers in the group to scale out message processing. IN a subscriber group, subscribers own channels that they subscribe to, so with only a single channel, only one subscriber in the group can receive messages.</p>

</div>

<h5 id="_vary_large_channel_count">Vary Large Channel Count</h5>
<div class="section">
<p>Setting a very large channel count (100s or 1000s) whilst possible would be impractical. One reason is that various data structures used by topics are created per-channel (or per-partition per-channel) so having a very large number of channels will use more resources, such as heap, to maintain these structures.</p>

<p>A larger number of channels would have less contention where many publishers are publishing messages at the same time.</p>

<p>A larger number of channels would allow more subscribers in a group to process messages in parallel, assuming that either there are enough publishers publishing to all of those channels, or the publishers are configured to publish to multiple channels.</p>

<p>In most cases the default channel count should be about right. Applications may wish to slightly reduce or increase this and there are use-cases where one channel may be suitable. An excessively large number of channels is probably not justifiable.</p>

</div>
</div>

<h4 id="retain">Retaining Messages (Topics as Persistent Logs)</h4>
<div class="section">
<p>By default, messages in a topic are removed after all the currently connected anonymous subscribers and all the subscriber groups have processed and committed a message. This means a message can only be read once. When an anonymous subscriber connects to a topic it starts receiving messages with the next message published to the topic after it connects. The subscribers in a new subscriber group will also receive messages that were published after the group was created.</p>

<p>Sometimes it is desirable to have a topic behave more like a persistent log structure, where a subscriber, or subscriber group, in an application can receive the ordered messages from the log, then go back and re-read them as required. The <code>&lt;retain-consumed&gt;</code> sub-element of the <code>&lt;paged-topic-scheme&gt;</code> element controls this behaviour. The <code>&lt;retain-consumed&gt;</code> sub-element&#8217;s value is a boolean, <code>true</code> to retain elements, <code>false</code> to remove consumed elements.</p>

<p>In topics configured with <code>&lt;retain-consumed&gt;</code> set to <code>true</code>, new anonymous subscribers will start to receive messages from the beginning (head) of the topic, rather than the tail; new subscriber groups will also start from the head of the topic.</p>

<p>Messages in a retained topic are never deleted (unless the topic is also configured with expiry).</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
    &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;retain-consumed&gt;true&lt;/retain-consumed&gt; <span class="conum" data-value="1" />
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The example above configures the topics mapped to the <code>distributed-topic</code> scheme to retain messages.</li>
</ul>
<p>Topics configuration, like cache configuration, supports parameterizing certain configuration elements on a per-topic basis using parameter macros.</p>

<p>For example, the configuration below has tow topic mappings, <code>topic-one</code> and <code>topic-two</code>.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;?xml version="1.0"?&gt;
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

   &lt;topic-scheme-mapping&gt;
    &lt;topic-mapping&gt;
      &lt;topic-name&gt;topic-one&lt;/topic-name&gt;
      &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
      &lt;init-params&gt;
        &lt;init-param&gt;
          &lt;param-name&gt;keep-messages&lt;/param-name&gt;        <span class="conum" data-value="1" />
          &lt;param-value&gt;true&lt;/param-value&gt;
        &lt;/init-param&gt;
      &lt;/init-params&gt;
    &lt;/topic-mapping&gt;

    &lt;topic-mapping&gt;
      &lt;topic-name&gt;topic-two&lt;/topic-name&gt;                <span class="conum" data-value="2" />
      &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
    &lt;/topic-mapping&gt;

   &lt;/topic-scheme-mapping&gt;

   &lt;caching-schemes&gt;
     &lt;paged-topic-scheme&gt;
        &lt;scheme-name&gt;distributed-topic&lt;/scheme-name&gt;
        &lt;service-name&gt;DistributedTopicService&lt;/service-name&gt;
        &lt;autostart&gt;true&lt;/autostart&gt;
        &lt;retain-values&gt;{keep-messages false}&lt;/retain-values&gt;  <span class="conum" data-value="3" />
     &lt;/paged-topic-scheme&gt;
   &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">The <code>topic-one</code> mapping contains an <code>init-param</code> named <code>keep-messages</code> with a value of <code>true</code></li>
<li data-value="2">The <code>topic-two</code> mapping contains no <code>init-params</code></li>
<li data-value="3">The topic scheme contains the <code>retain-values</code> sub-element, but instead of a simple boolean value it uses a macro (a value inside curly brackets). The <code>{keep-messages false}</code> macro says to use the <code>keep-messages</code> parameter for the value of the <code>retain-values</code> sub-element, and default to <code>false</code> if <code>keep-messages</code> is not set.</li>
</ul>
<p>So, <code>topic-one</code>, which sets <code>keep-values</code> to <code>true</code> will use a configuration that retains messages, whereas <code>topic-two</code> has no <code>init-params</code> so <code>keep-values</code> will not be set and will default to `false.</p>

</div>

<h4 id="serializer">Topic Values Serializer</h4>
<div class="section">
<p>The <code>&lt;serializer&gt;</code> sub-element of <code>&lt;paged-topic-scheme&gt;</code> element enables specifying predefined serializers <code>pof</code> or <code>java</code> (default), or a custom <code>Serializer</code> implementation. The serializer is used to serialize and deserialize the message payload.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;serializer&gt;pof&lt;/serializer&gt;   <span class="conum" data-value="1" />
    &lt;autostart&gt;true&lt;/autostart&gt;
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">the example above sets the serializer for all topics mapped to the <code>distributed-topic</code> scheme to POF.</li>
</ul>
</div>

<h4 id="subscriber-timeout">Subscriber Timeout</h4>
<div class="section">
<p>The <code>subscriber-timeout</code> sub-element configures the maximum amount of time that can elapse after a subscribers that is part of a subscriber group polls for messages before that subscriber is considered dead. Each time a subscriber in a group calls on of the <code>receive</code> methods it sends a heartbeat to the server (heartbeats can also be sent manually by application code during long-running processing). If the server does not receive a heartbeat within the timeout the subscriber is considered dead and any channels that it owned will be redistributed to any remaining subscribers.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;subscriber-timeout&gt;1m&lt;/subscriber-timeout&gt;  <span class="conum" data-value="1" />
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The subscriber timeout has been set to 1 minute.</li>
</ul>
<p>The purpose of timing out subscribers is to stop channels being starved of subscriptions due to badly behaved, dead, or deadlocked subscribers. If a dead subscriber stayed connected and its channels were not redistributed, any messages published to those channels would never be processed.</p>

<p>A timed-out subscriber is not closed, application code that calls receive on a timed-ot subscriber will cause that subscriber to reconnect and be re-initialised with new channel ownership.</p>

<p>The default value for the subscriber timeout is five minutes. This should be sufficient for most applications unless the message processing code takes a very long time, for example it talks to other external slow system.</p>

</div>

<h4 id="storeage">Storage Options for Topic Values</h4>
<div class="section">
<p>The <code>&lt;storage&gt;</code> sub-element allows specification of <code>on-heap</code>, <code>ramjournal</code> and <code>flashjournal</code> to store the messages and metadata for a topic. The default is to use on-heap storage. The <code>ramjournal</code> and <code>flashjournal</code> options use the Elastic Data Feature to, which is a commercial only Coherence feature.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;storage&gt;on-heap&lt;/storage&gt;   <span class="conum" data-value="1" />
    &lt;autostart&gt;true&lt;/autostart&gt;
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">the example above sets storage to <code>on-heap</code>, so topic data is stored in memory.</li>
</ul>
<markup
lang="xml"
title="coherence-cache-config.xml"
> &lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;storage&gt;flashjournal&lt;/storage&gt;   <span class="conum" data-value="1" />
    &lt;autostart&gt;true&lt;/autostart&gt;
 &lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">the example above sets storage to <code>flashjournal</code>, so topic data is stored on disc, using Coherence&#8217;s commercial Elastic Data feature.</li>
</ul>
</div>

<h4 id="page-size">Page Size</h4>
<div class="section">
<p>A paged topic scheme configures a topic that stores data in pages. This is how Coherence scales topic data across the cluster, by distributing pages across storage enabled members of the cluster. Each channel has pages and each page holds a number of messages. The page size can be configured to determine how many messages can fit into a page. Publishers publish messages to the tail page in a channel, and when that page is full the page is sealed, and the next page becomes the tail.</p>

<p>Page size is configured in the <code>&lt;page-size&gt;</code> element of the <code>&lt;paged-topic-scheme&gt;</code> in the cache configuration file.
The format of the page size value is a positive integer, optionally followed by a byte size suffix (B, KB, MB).</p>

<p>When the page size is configured with a value using a byte size suffix, the size of the serialized message payload is used to determine the page size. In this case different pages may contain different numbers of messages if the serialized message size is different.</p>

<p>In the example below, the page size is set to 10 mega-bytes:</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;page-size&gt;10MB&lt;/page-size&gt;
&lt;/paged-topic-scheme&gt;</markup>

<p>If a page size element has a value without a byte size suffix it is treated as a number of messages rather than a binary size. For example, using the configuration below, each page will have a maximum size of 100 messages, regardless of the message&#8217;s size in bytes:</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;page-size&gt;100&lt;/page-size&gt;
&lt;/paged-topic-scheme&gt;</markup>

<p>Pages are used by subscribers as a way to batch receive calls. When receive is called on a subscriber, the server can return a batch of messages, upto a whole page, in the response. The subscriber then stores those messages locally and uses them to respond to further receive calls without needing to make a remote request back to the page. The default maximum batch size used in responses is the minimum of the page size, and the MTU of the network interface being used by Coherence, so that a batch fits into a network packet.</p>

<p>As with many configuration elements, there are pros and cons to making the value too small or too large.
A page that is too large will store a lot of data in a single page on a single storage member before moving to the next page, causing lumpy data distribution. Pages that are too small will cause an excessive number of pages wasting storage where the data structures created per page may exceed the storage used by the messages themselves.</p>

<p>The default page size is 1Mb. Page numbers are stored in a Java long value, so an application is unlikely to consume all 9,223,372,036,854,775,807 pages. Using the default page size of 1Mb, that is 9 peta-bytes of messages.</p>

<p>Due to the way publishing works, a full page usually slightly exceeds the configured page size. This is because during publishing, messages are accepted into a page until the pages size is &gt;= the configured maximum. For example, if a page has been configured with a maximum size of 1Mb and currently has 900kb of messages, so is below the maximum size. If the next message published that is 150kb, that message will still be accepted, as the page has some free space, pushing the total size to 1.5Mb. The page would then be considered sealed and accept no more messages, the next message published would go to the next page.</p>

</div>

<h4 id="size-limit">Size Limited Topics</h4>
<div class="section">
<p>Adding a <code>&lt;high-units&gt;</code> sub-element to <code>&lt;paged-topic-scheme&gt;</code> element limits the storage size of the values retained for the topic. The topic is considered full if this storage limit is reached. Not exceeding this high water-mark is managed by using flow control. When subscriber(s) are lagging in processing outstanding values retained on the topic, the publishers are throttled until there is space available. See Managing the Publisher Flow Control to Place Upper Bound on Topics Storage.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;high-units&gt;100MB&lt;/high-units&gt;  <span class="conum" data-value="1" />
&lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The <code>&lt;high-units&gt;</code> has been set to 100Mb so the total size of the topic will not exceed 100 mega-bytes.</li>
</ul>
</div>

<h4 id="expiry">Topics with Expiring Values</h4>
<div class="section">
<p>Adding a <code>&lt;expiry-delay&gt;</code> sub-element to <code>&lt;paged-topic-scheme&gt;</code> element limits the length of time that the published messages live in a topic, waiting to be received by a Subscriber. Once the expiry delay is past, those expired messages will never be received by subscribers. The default <code>expiry-delay</code> is zero, meaning elements never expire.</p>

<p>Messages will be expired regardless of the type of subscriber used. For example, even with a durable subscriber group, expired messaged will not be received after their expiry delay has passed.</p>

<markup
lang="xml"
title="coherence-cache-config.xml"
>&lt;paged-topic-scheme&gt;
    &lt;scheme-name&gt;topic-scheme&lt;/scheme-name&gt;
    &lt;service-name&gt;TopicService&lt;/service-name&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;expiry-delay&gt;30d&lt;/expiry-delay&gt;  <span class="conum" data-value="1" />
&lt;/paged-topic-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">The <code>&lt;expiry-delay&gt;</code> has been set to 30 days, so messages will be removed from the topic 30 days after publishing, regardless of whether they have been received by subscribers.</li>
</ul>
</div>
</div>
</div>
</doc-view>
