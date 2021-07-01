<doc-view>

<h2 id="_partition_events_logging">Partition Events Logging</h2>
<div class="section">
<p>The most commonly used service in Coherence to store and access data is the distributed / partitioned service. It offers partitioned access to store and retrieve data, thus provides scalability, in addition to redundancy by ensuring replicas are in sync.</p>

<p>This concept of partitioning can be entirely opaque to an end user as Coherence will transparently map keys to partitions and partitions to members. As ownership members join and leave the partitioned service, the partitions are redistributed across the new/remaining members avoiding an entire rehash of the data. Coherence also designates replicas providing them an initial snapshot followed by a journal of updates as they occur on the primary.</p>

<p>These partition lifecycle events (members joining and leaving the service) result in partitions being blocked and therefore Coherence attempts to reduce this time of unavailability as much as possible. Until now, there has been minimal means to track this partition unavailability. This feature provides insight into these partition lifecycle events, highlighting when they start and end allowing customers to correlate increased response times with said lifecycle events.</p>


<h3 id="_data_availability">Data Availability</h3>
<div class="section">
<p>In order to preserve the integrity of data, when partition events occur such as partition movements between members, read or write access to data will be temporarily blocked.
This happens when re-distribution takes place or indices are built. The amounts of time involved are usually extremely short, but can add up if the cache contains significant amounts of data.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 23.077%;">
<col style="width: 76.923%;">
</colgroup>
<thead>
<tr>
<th>Event Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Redistribution</td>
<td class="">When a server joins or leaves a cluster, a number of events occur on each member in the cluster, existing and new, which correspond to the movement of data and backup partitions according to the partitioning strategy. This scheme determines which members own which partitions, and which members own which backups.</td>
</tr>
<tr>
<td class="">Restoring from backup</td>
<td class="">After primary partitions are lost, the backups are moved into primary storage in the members where they are located. Naturally, the partitions in question are locked until the event finishes.</td>
</tr>
<tr>
<td class="">Recovery from persistence</td>
<td class="">Persistence maintenance, such as snapshot creation and recovery from persistence, will cause the affected partitions to be unavailable.</td>
</tr>
<tr>
<td class="">Index building</td>
<td class="">If an application needs to have data indexed, this is typically done by calling <code>addIndex</code> on a <code>NamedCache</code>. If the cache already contains a significant amount of data, or the cost of computing the index per entry (the <code>ValueExtractor</code>) is high, this operation can take some time during which any submitted queries will be blocked waiting for the index data structures to be populated. Note that regular index maintenance, such as adding or deleting elements, does not incur the same unavailability penalty.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="_feature_usage">Feature Usage</h3>
<div class="section">
<p>By default, logging of times when partitions are unavailable is turned off as it generates a significant amount of logs.</p>

<p>To enable logging of partition events, set the property <code>coherence.distributed.partition.events</code> to <code>log</code> and <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/debugging-coherence.html#GUID-2C427606-5F61-4640-863F-20183E519E52">set log level</a> to 8 or more.</p>

<p>e.g.:</p>

<markup
lang="text"

>-Dcoherence.distributed.partition.events=log</markup>

</div>

<h3 id="_events_logged">Events Logged</h3>
<div class="section">
<p>The events below are logged, one per partition except when partitions are initially assigned. Along with that, the owning member and possibly the time it made the partition unavailable are also logged.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 23.077%;">
<col style="width: 76.923%;">
</colgroup>
<thead>
<tr>
<th>Event</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>ASSIGN</code></td>
<td class="">The partition is either initially, or as a result of losing primary and all backups, assigned to a cluster member.</td>
</tr>
<tr>
<td class=""><code>PRIMARY_TRANSFER_OUT</code></td>
<td class="">The partition is being transferred to a different member.</td>
</tr>
<tr>
<td class=""><code>BACKUP_TRANSFER_OUT</code></td>
<td class="">This primary is transferring a snapshot of the partition and all its content to the targeted member as it will be in the chain of backup replicas.</td>
</tr>
<tr>
<td class=""><code>PRIMARY_TRANSFER_IN</code></td>
<td class="">This member is receiving a partition and all related data for primary ownership. This will be due to a <code>PRIMARY_TRANSFER_OUT</code> from the existing owner of the partition.</td>
</tr>
<tr>
<td class=""><code>RESTORE</code></td>
<td class="">The loss of primary partitions results in backup owners (replicas) restoring data from backup storage to primary for the affected partitions.</td>
</tr>
<tr>
<td class=""><code>INDEX_BUILD</code></td>
<td class="">Index data structures were populated for the relevant partitions. This will effect queries that use said indices but does not block key based data access or mutation.</td>
</tr>
<tr>
<td class=""><code>PERSISTENCE</code></td>
<td class="">The relevant partitions were made unavailable due to persistence maintenance operations; this will minimally include recovery from persistence and snapshot creation.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="_example">Example</h3>
<div class="section">
<p>On Member 1:</p>

<p>(at startup)</p>

<div class="listing">
<pre>2021-06-11 09:26:10.159/5.522 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedTopicDedicated:0x000A:5, member=1): PartitionSet{0..256}, Owner: 1, Action: ASSIGN, UnavailableTime: 0
...</pre>
</div>

<p>(application calls <code>addIndex()</code> on a cache)</p>

<div class="listing">
<pre>2021-06-11 09:28:36.872/152.234 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedCacheDedicated:0x000B:152, member=1): PartitionId: 43, Owner: 1, Action: INDEX_BUILD, UnavailableTime: 3
...</pre>
</div>

<p>(the partitions listed are being transferred to another member, along with backups; note how backups and partitions are not the same)</p>

<div class="listing">
<pre>2021-06-11 09:28:45.573/160.935 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 132, Owner: 1, Action: BACKUP_TRANSFER_OUT, UnavailableTime: 1
2021-06-11 09:28:45.678/161.040 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 133, Owner: 1, Action: BACKUP_TRANSFER_OUT, UnavailableTime: 1
...
2021-06-11 09:28:49.911/165.273 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 2, Owner: 1, Action: PRIMARY_TRANSFER_OUT, UnavailableTime: 5
2021-06-11 09:28:50.017/165.379 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 3, Owner: 1, Action: PRIMARY_TRANSFER_OUT, UnavailableTime: 3
...</pre>
</div>

<p>On Member 2:</p>

<p>(partitions are being received; if they have indices, they are rebuilt immediately after reception)</p>

<div class="listing">
<pre>2021-06-11 09:28:49.805/8.033 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=2): PartitionId: 1, Owner: 2, Action: PRIMARY_TRANSFER_IN, UnavailableTime: 1
2021-06-11 09:28:49.806/8.034 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedCacheDedicated:0x000B:8, member=2): PartitionId: 1, Owner: 2, Action: INDEX_BUILD, UnavailableTime: 0</pre>
</div>

<p>Member 2 stops, back on Member 1:</p>

<p>(partitions are being restored from backup, and the indices related to them rebuilt)</p>

<div class="listing">
<pre>2021-06-11 10:29:19.041/3794.322 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 0, Owner: 1, Action: RESTORE, UnavailableTime: 109
2021-06-11 10:29:19.041/3794.322 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=DistributedCache:PartitionedCache, member=1): PartitionId: 1, Owner: 1, Action: RESTORE, UnavailableTime: 109
...
2021-06-11 10:29:19.062/3794.343 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedCacheDedicated:0x000E:3794, member=1): PartitionId: 1, Owner: 1, Action: INDEX_BUILD, UnavailableTime: 12
2021-06-11 10:29:19.066/3794.347 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedCacheDedicated:0x000D:3794, member=1): PartitionId: 0, Owner: 1, Action: INDEX_BUILD, UnavailableTime: 16
2021-06-11 10:29:19.067/3794.349 Oracle Coherence GE 14.1.2.0.0 (dev-mycomputer) &lt;D8&gt; (thread=PartitionedCacheDedicated:0x000E:3794, member=1): PartitionId: 2, Owner: 1, Action: INDEX_BUILD, UnavailableTime: 5
...</pre>
</div>

</div>

<h3 id="_future">Future</h3>
<div class="section">
<p>While logging gives valuable details on partition&#8217;s lifecycle, it is a simple means of providing this information. Ultimately, better forms of presenting it for consumption will be provided, such as a JMX MBean or a Coherence report.</p>

</div>
</div>
</doc-view>
