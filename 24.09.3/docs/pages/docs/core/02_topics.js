<doc-view>

<h2 id="_topics_management">Topics Management</h2>
<div class="section">
<p>This version of Coherence introduces additional features and functionality to help you manage and monitor
topics within a Coherence cluster.  New Topics MBeans are at the core with three new
reports allowing for analysis of these MBeans overtime as well as additional Management over REST endpoints to interact with the MBeans.</p>

<p>The Coherence CLI and VisualVM plugin now support viewing and managing Topics and new Grafana Dashboards are available to view
Topics metrics over time.</p>

<ul class="ulist">
<li>
<p><router-link to="#mbeans" @click.native="this.scrollFix('#mbeans')">Topics MBeans</router-link></p>

</li>
<li>
<p><router-link to="#reports" @click.native="this.scrollFix('#reports')">Topics Reports</router-link></p>

</li>
<li>
<p><router-link to="#rest" @click.native="this.scrollFix('#rest')">Management over REST</router-link></p>

</li>
<li>
<p><router-link to="#mgmt" @click.native="this.scrollFix('#mgmt')">Topics Management via CLI and VisualVM</router-link></p>

</li>
<li>
<p><router-link to="#grafana" @click.native="this.scrollFix('#grafana')">Topics Grafana Dashboards</router-link></p>

</li>
</ul>

<h3 id="mbeans">Topics MBeans</h3>
<div class="section">
<p>Three new Topics MBeans are now available, <code>PagedTopic</code>, <code>PagedTopicSubscriber</code> and <code>PagedTopicSubscriberGroup</code> MBean.
These are described in more detail below:</p>


<h4 id="_pagedtopic_mbean">PagedTopic MBean</h4>
<div class="section">
<p>The <code>PagedTopic</code> MBean provides statistics for Topic services running in a cluster. A cluster contains zero or more instances of this MBean,
each instance representing an instance of a Topic on a member.</p>

<p>The object name of the MBean is:</p>

<pre>type=PagedTopic,service=ServiceName,name=TopicName,nodeId=node</pre>
<p><strong>Attributes</strong></p>

<div class="block-title"><span>PagedTopic MBean attributes</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Access</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">AllowUnownedCommits</td>
<td class="">Boolean</td>
<td class="">read-only</td>
<td class="">Allow Unowned Commits.</td>
</tr>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">Channels</td>
<td class="">TabularData</td>
<td class="">read-only</td>
<td class="">Channel statistics.</td>
</tr>
<tr>
<td class="">ElementCalculator</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">Element Calculator.</td>
</tr>
<tr>
<td class="">PageCapacity</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The capacity of a page.</td>
</tr>
<tr>
<td class="">PublishedCount</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of published messages.</td>
</tr>
<tr>
<td class="">PublishedFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The published messages fifteen-minute rate.</td>
</tr>
<tr>
<td class="">PublishedFiveMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The published messages five-minute rate.</td>
</tr>
<tr>
<td class="">PublishedMeanRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The published messages mean rate.</td>
</tr>
<tr>
<td class="">PublishedOneMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The published messages one-minute rate.</td>
</tr>
<tr>
<td class="">ReconnectRetry</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">Reconnect Retry.</td>
</tr>
<tr>
<td class="">ReconnectTimeout</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">Reconnect Timeout.</td>
</tr>
<tr>
<td class="">ReconnectWait</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">Reconnect Wait.</td>
</tr>
<tr>
<td class="">RetainConsumed</td>
<td class="">Boolean</td>
<td class="">read-only</td>
<td class="">Retain consumed values.</td>
</tr>
<tr>
<td class="">SubscriberTimeout</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">Subscriber Timeout.</td>
</tr>
</tbody>
</table>
</div>
<p><strong>Operations</strong></p>

<div class="block-title"><span>PagedTopic MBean operations</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Operation</th>
<th>Parameters</th>
<th>Return Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">disconnectAll</td>
<td class="">Not applicable</td>
<td class="">Void</td>
<td class="">Force this topic to disconnect all subscribers.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h4 id="_pagedtopicsubscriber_mbean">PagedTopicSubscriber MBean</h4>
<div class="section">
<p>The <code>PagedTopicSubscriber</code> MBean provides statistics for Topic Subscribers running in a cluster. A cluster contains zero or more instances of this MBean, each instance representing an instance of a Topic Subscriber on a member.</p>

<p>The object name of the MBean is:</p>

<pre>type=PagedTopicSubscriber,service=ServiceName,topic=TopicName,subtype=SubType,group=SubscriberGroup,id=SubscriberId,nodeId=node</pre>
<div class="block-title"><span>PagedTopicSubscriber MBean attributes</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Access</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Backlog</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The number of outstanding receive requests.</td>
</tr>
<tr>
<td class="">ChannelAllocations</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The subscriber&#8217;s allocated channels.</td>
</tr>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">Channels</td>
<td class="">TabularData</td>
<td class="">read-only</td>
<td class="">Channel statistics.</td>
</tr>
<tr>
<td class="">CompleteOnEmpty</td>
<td class="">Boolean</td>
<td class="">read-only</td>
<td class="">A flag indicating whether the subscriber completes receive requests with a null message when the topic is empty.</td>
</tr>
<tr>
<td class="">Converter</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The optional converter being used to transform messages.</td>
</tr>
<tr>
<td class="">Disconnections</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of times this subscriber has disconnected.</td>
</tr>
<tr>
<td class="">Filter</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The optional filter being used to filter messages.</td>
</tr>
<tr>
<td class="">Id</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The subscriber&#8217;s identifier.</td>
</tr>
<tr>
<td class="">IdentifyingName</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">An optional name to help identify this subscriber.</td>
</tr>
<tr>
<td class="">MaxBacklog</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The maximum number of outstanding receive requests allowed before flow control blocks receive calls.</td>
</tr>
<tr>
<td class="">Member</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The cluster member owning this subscriber.</td>
</tr>
<tr>
<td class="">NotificationId</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The subscriber&#8217;s notification identifier.</td>
</tr>
<tr>
<td class="">Notifications</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of channel notifications received.</td>
</tr>
<tr>
<td class="">Polls</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The total number of polls for messages.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsCount</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number completed receive requests.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The completed receive requests, fifteen-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsFiveMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The completed receive requests, five-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsMeanRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The completed receive requests, mean rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsOneMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The completed receive requests, one-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveEmpty</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number empty receive requests.</td>
</tr>
<tr>
<td class="">ReceiveErrors</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number exceptionally completed receive requests.</td>
</tr>
<tr>
<td class="">ReceivedCount</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of elements received.</td>
</tr>
<tr>
<td class="">Serializer</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The serializer used to deserialize messages.</td>
</tr>
<tr>
<td class="">State</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The state of the subscriber. Valid values are: 0 – Initial, 1 – Connected, 2 – Disconnected, 3 – Closing, 4 - Closed</td>
</tr>
<tr>
<td class="">StateName</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The state of the subscriber as a string.</td>
</tr>
<tr>
<td class="">SubTypeCode</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">Indicates if the subscriber is Durable (1) or Anonymous (0).</td>
</tr>
<tr>
<td class="">SubscriberGroup</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The subscriber group the subscriber belongs to.</td>
</tr>
<tr>
<td class="">Type</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The type of this subscriber.</td>
</tr>
<tr>
<td class="">Waits</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of waits on an empty channel.</td>
</tr>
</tbody>
</table>
</div>
<p><strong>Operations</strong></p>

<div class="block-title"><span>PagedTopicSubscriber MBean operations</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Operation</th>
<th>Parameters</th>
<th>Return Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">connect</td>
<td class="">Not applicable</td>
<td class="">Void</td>
<td class="">Ensure this subscriber is connected.</td>
</tr>
<tr>
<td class="">disconnect</td>
<td class="">Not applicable</td>
<td class="">Void</td>
<td class="">Force this subscriber to disconnect and reset itself.</td>
</tr>
<tr>
<td class="">heads</td>
<td class="">Not applicable</td>
<td class="">TabularData</td>
<td class="">Retrieve the current head positions for each channel.</td>
</tr>
<tr>
<td class="">notifyPopulated</td>
<td class="">Integer nChannel</td>
<td class="">Void</td>
<td class="">Send a channel populated notification to this subscriber.</td>
</tr>
<tr>
<td class="">remainingMessages</td>
<td class="">Not applicable</td>
<td class="">TabularData</td>
<td class="">Retrieve the count of remaining messages for each channel.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h4 id="_pagedtopicsubscribergroup_mbean">PagedTopicSubscriberGroup MBean</h4>
<div class="section">
<p>The <code>PagedTopicSubscriberGroup</code> MBean provides statistics for Topic Subscriber Groups running in a cluster. A cluster contains zero or more instances of this MBean, each instance representing an instance of a Topic Subscriber Group on a member.</p>

<p>The object name of the MBean is:</p>

<pre>type=PagedTopicSubscriberGroup,service=ServiceName,topic=TopicName,subtype=SubType,name=SubscriberGroup,nodeId=node</pre>
<div class="block-title"><span>PagedTopicSubscriberGroup MBean attributes</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Access</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">read-only</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">Channels</td>
<td class="">TabularData</td>
<td class="">read-only</td>
<td class="">Channel statistics.</td>
</tr>
<tr>
<td class="">Filter</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The filter.</td>
</tr>
<tr>
<td class="">PolledCount</td>
<td class="">Long</td>
<td class="">read-only</td>
<td class="">The number of polled messages.</td>
</tr>
<tr>
<td class="">PolledFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The polled messages fifteen-minute rate.</td>
</tr>
<tr>
<td class="">PolledFiveMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The polled messages five-minute rate.</td>
</tr>
<tr>
<td class="">PolledMeanRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The polled messages mean rate.</td>
</tr>
<tr>
<td class="">PolledOneMinuteRate</td>
<td class="">Double</td>
<td class="">read-only</td>
<td class="">The polled messages one-minute rate.</td>
</tr>
<tr>
<td class="">Transformer</td>
<td class="">String</td>
<td class="">read-only</td>
<td class="">The transformer.</td>
</tr>
</tbody>
</table>
</div>
<p><strong>Operations</strong></p>

<div class="block-title"><span>PagedPagedTopicSubscriberGroupTopic MBean operations</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Operation</th>
<th>Parameters</th>
<th>Return Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">disconnectAll</td>
<td class="">Not applicable</td>
<td class="">Void</td>
<td class="">Force this subscriber group to disconnect all subscribers.</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>

<h3 id="reports">Topics Reports</h3>
<div class="section">
<p>The following reports have been added in this release.</p>

<p><strong>Topic Report</strong></p>

<p>The topic report provides detailed metrics for topics defined within a cluster.
The name of the topic report is <code>timestamp-topic.txt</code> where the timestamp is in <code>YYYYMMDDHH</code> format.
For example, a file named <code>2009013101-topics.txt</code> represents a topics report for January 31, 2009 at 1:00 a.m.</p>

<div class="admonition note">
<p class="admonition-inline">This report is not included in <code>report-group.xml</code> but is available by running <code>report-all.xml</code>.</p>
</div>
<div class="block-title"><span>Topic Report</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Batch Counter</td>
<td class="">Long</td>
<td class="">A sequential counter to help integrate information between related files. This value resets when the reporter restarts and is not consistent across members. However, it is helpful when trying to integrate files.</td>
</tr>
<tr>
<td class="">Report Date</td>
<td class="">Date</td>
<td class="">A timestamp for each report refresh.</td>
</tr>
<tr>
<td class="">Service</td>
<td class="">String</td>
<td class="">The service name.</td>
</tr>
<tr>
<td class="">Name</td>
<td class="">String</td>
<td class="">The topic name.</td>
</tr>
<tr>
<td class="">NodeId</td>
<td class="">String</td>
<td class="">The numeric member identifier.</td>
</tr>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">PublishedCount</td>
<td class="">Long</td>
<td class="">The number of published messages since the last report refresh.</td>
</tr>
<tr>
<td class="">PublishedFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">The published messages fifteen-minute rate.</td>
</tr>
<tr>
<td class="">PublishedFiveMinuteRate</td>
<td class="">Double</td>
<td class="">The published messages five-minute rate.</td>
</tr>
<tr>
<td class="">PublishedMeanRate</td>
<td class="">Double</td>
<td class="">The published messages mean rate.</td>
</tr>
<tr>
<td class="">PublishedOneMinuteRate</td>
<td class="">Double</td>
<td class="">The published messages one-minute rate.</td>
</tr>
</tbody>
</table>
</div>
<p><strong>Topic Subscribers Report</strong></p>

<p>The topic subscriber report provides detailed metrics for topic subscribers defined within a cluster.
The name of the topic subscribers report is <code>timestamp-topic-subscribers.txt</code> where the timestamp is in
<code>YYYYMMDDHH</code> format. For example, a file named <code>2009013101-topic-subscribers.txt</code> represents a topic subscriber report for January 31, 2009 at 1:00 a.m.</p>

<div class="admonition note">
<p class="admonition-inline">This report is not included in <code>report-group.xml</code> but is available by running <code>report-all.xml</code>.</p>
</div>
<div class="block-title"><span>Topic Subscribers Report</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Batch Counter</td>
<td class="">Long</td>
<td class="">A sequential counter to help integrate information between related files. This value resets when the reporter restarts and is not consistent across members. However, it is helpful when trying to integrate files.</td>
</tr>
<tr>
<td class="">Report Date</td>
<td class="">Date</td>
<td class="">A timestamp for each report refresh.</td>
</tr>
<tr>
<td class="">Service</td>
<td class="">String</td>
<td class="">The service name.</td>
</tr>
<tr>
<td class="">Name</td>
<td class="">String</td>
<td class="">The topic name.</td>
</tr>
<tr>
<td class="">SubscriberGroup</td>
<td class="">String</td>
<td class="">The subscriber group the subscriber belongs to.</td>
</tr>
<tr>
<td class="">Id</td>
<td class="">Long</td>
<td class="">The Id of the subscriber.</td>
</tr>
<tr>
<td class="">NodeId</td>
<td class="">String</td>
<td class="">The numeric member identifier.</td>
</tr>
<tr>
<td class="">Backlog</td>
<td class="">Long</td>
<td class="">The number of outstanding receive requests.</td>
</tr>
<tr>
<td class="">ChannelAllocations</td>
<td class="">String</td>
<td class="">The subscriber&#8217;s allocated channels.</td>
</tr>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">Disconnections</td>
<td class="">Long</td>
<td class="">The number of times this subscriber has disconnected since the last report refresh.</td>
</tr>
<tr>
<td class="">Notifications</td>
<td class="">Long</td>
<td class="">The number of channel notifications received since the last report refresh.</td>
</tr>
<tr>
<td class="">Polls</td>
<td class="">Long</td>
<td class="">The total number of polls for messages since the last report refresh.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsCount</td>
<td class="">Long</td>
<td class="">The number completed receive requests since the last report refresh .</td>
</tr>
<tr>
<td class="">ReceiveCompletionsFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">The completed receive requests, fifteen-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsFiveMinuteRate</td>
<td class="">Double</td>
<td class="">The completed receive requests, five-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsMeanRate</td>
<td class="">Double</td>
<td class="">The completed receive requests, mean rate.</td>
</tr>
<tr>
<td class="">ReceiveCompletionsOneMinuteRate</td>
<td class="">Double</td>
<td class="">The completed receive requests, one-minute rate.</td>
</tr>
<tr>
<td class="">ReceiveEmpty</td>
<td class="">Long</td>
<td class="">The number empty receive requests since the last report refresh.</td>
</tr>
<tr>
<td class="">ReceiveErrors</td>
<td class="">Long</td>
<td class="">The number exceptionally completed receive requests since the last report refresh.</td>
</tr>
<tr>
<td class="">ReceivedCount</td>
<td class="">Long</td>
<td class="">The number of elements received since the last report refresh.</td>
</tr>
<tr>
<td class="">State</td>
<td class="">Integer</td>
<td class="">The state of the subscriber. Valid values are: 0 – Initial, 1 – Connected, 2 – Disconnected, 3 – Closing, 4 - Closed.</td>
</tr>
<tr>
<td class="">StateName</td>
<td class="">String</td>
<td class="">The state of the subscriber as a string.</td>
</tr>
<tr>
<td class="">Waits</td>
<td class="">Long</td>
<td class="">The number of elements received since the last report refresh.</td>
</tr>
</tbody>
</table>
</div>
<p><strong>Topic Subscriber Groups Report</strong></p>

<p>The topic subscriber groups report provides detailed metrics for topic subscriber groups defined within
a cluster. The name of the topic subscriber groups report is <code>timestamp-topic-subscriber-groups.txt</code>
where the timestamp is in <code>YYYYMMDDHH</code> format. For example, a file named <code>2009013101-topic-subscriber-groups.txt</code> represents a topic subscriber report for January 31, 2009 at 1:00 a.m.</p>

<div class="admonition note">
<p class="admonition-inline">This report is not included in report-group.xml but is available by running report-all.xml.</p>
</div>
<div class="block-title"><span>Topic Subscriber Groups Report</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Batch Counter</td>
<td class="">Long</td>
<td class="">A sequential counter to help integrate information between related files. This value resets when the reporter restarts and is not consistent across members. However, it is helpful when trying to integrate files.</td>
</tr>
<tr>
<td class="">Report Date</td>
<td class="">Date</td>
<td class="">A timestamp for each report refresh.</td>
</tr>
<tr>
<td class="">Service</td>
<td class="">String</td>
<td class="">The service name.</td>
</tr>
<tr>
<td class="">Topic</td>
<td class="">String</td>
<td class="">The topic name.</td>
</tr>
<tr>
<td class="">Name</td>
<td class="">String</td>
<td class="">The subscriber group the subscriber belongs to.</td>
</tr>
<tr>
<td class="">NodeId</td>
<td class="">String</td>
<td class="">The numeric member identifier.</td>
</tr>
<tr>
<td class="">ChannelCount</td>
<td class="">Integer</td>
<td class="">The number of channels in the topic.</td>
</tr>
<tr>
<td class="">PolledCount</td>
<td class="">Long</td>
<td class="">The total number of polls for messages since the last report refresh.</td>
</tr>
<tr>
<td class="">PolledFifteenMinuteRate</td>
<td class="">Double</td>
<td class="">The polled messages fifteen-minute rate</td>
</tr>
<tr>
<td class="">PolledFiveMinuteRate</td>
<td class="">Double</td>
<td class="">The polled messages five-minute rate</td>
</tr>
<tr>
<td class="">PolledOneMinuteRate</td>
<td class="">Double</td>
<td class="">The polled messages one-minute rate</td>
</tr>
<tr>
<td class="">PolledMeanRate</td>
<td class="">Double</td>
<td class="">The polled messages mean rate</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="rest">Management over REST</h3>
<div class="section">
<p>You are now able to view and manage Topics, Subscribers and Subscriber Groups using Management over REST API.</p>

<p>For example, to retrieve the topics for a service you can use the following <code>curl</code> command replacing <code>serviceName</code> with your Topics service name.</p>

<markup
lang="bash"

>curl http://host:port/management/coherence/cluster/services/serviceName/topics</markup>

<p>See <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/rest-reference/index.html">REST API for Managing Oracle Coherence</a> for full details of the available REST end points.</p>

</div>

<h3 id="mgmt">Topics Management via CLI and VisualVM</h3>
<div class="section">
<p>The Coherence VisualVM Plugin and Coherence CLI have been updated to provide management and monitoring of Topics within a Coherence cluster.
See the following links for more information on each of the tools.</p>

<ul class="ulist">
<li>
<p>Coherence CLI - See <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-cli">Coherence CLI</a> on GitHub and <a id="" title="" target="_blank" href="https://oracle.github.io/coherence-cli/docs/latest/#/docs/reference/01_overview">CLI Command Reference</a>.</p>

</li>
<li>
<p>Coherence VisualVM Plugin - See <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-visualvm">Coherence VisualVM</a> on GitHub</p>

</li>
</ul>
</div>

<h3 id="grafana">Topics Grafana Dashboards</h3>
<div class="section">
<p>There are four new Grafana Dashboards available to show Topics related information:</p>

<ul class="ulist">
<li>
<p>Topics Summary</p>

</li>
<li>
<p>Topic Details</p>

</li>
<li>
<p>Topic Subscriber Details</p>

</li>
<li>
<p>Topic Subscriber Group Details</p>

</li>
</ul>
<p>The above dashboards are available from the <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-operator/tree/main/dashboards/grafana">Coherence Operator</a> GitHub repository.</p>

<p>See the Oracle <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/manage/using-coherence-metrics.html">Metrics Documentation</a> for more information on configuring metrics.</p>

</div>
</div>
</doc-view>
