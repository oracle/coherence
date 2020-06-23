<doc-view>

<v-layout row wrap>
<v-flex xs12 sm10 lg10>
<v-card class="section-def" v-bind:color="$store.state.currentColor">
<v-card-text class="pa-3">
<v-card class="section-def__card">
<v-card-text>
<dl>
<dt slot=title>Introduction</dt>
<dd slot="desc"><p>First and foremost, Coherence provides a fundamental service that is responsible for all facets of clustering and is a
common denominator / building block for all other Coherence services.
This service, referred to as 'service 0' internally, ensures that the mesh of members is maintained and responsive,
taking action to collaboratively evict, shun, or in some cases, voluntarily depart the cluster when deemed necessary.
As members join and leave the cluster, other Coherence services are notified, thus enabling those services to react accordingly.</p>

<div class="admonition note">
<p class="admonition-inline">This part of the Coherence product has been in production for more than 10 years, being the subject of some extensive and
imaginative testing.
While this feature has been discussed here, it certainly is not something that customers, generally, interact with directly, but is
important to be aware of.</p>
</div>
<p>Coherence services build on top of the clustering service. The key implementations to be aware of are PartitionedService, InvocationService, and ProxyService.</p>

<p>In the majority of cases, customers deal with maps. A map is represented
by an implementation of <code>NamedMap&lt;K,V&gt;</code>. A <code>NamedMap</code> is hosted by a service,
generally the PartitionedService, and is the entry point to store, retrieve,
aggregate, query, and stream data.</p>

<p>Coherence Maps provide a number of features:</p>

<ul class="ulist">
<li>
<p>Fundamental <strong>key-based access</strong>: get/put getAll/putAll.</p>

</li>
<li>
<p>Client-side and storage-side events:</p>
<ul class="ulist">
<li>
<p><strong>MapListeners</strong> to asynchronously notify clients of changes to data.</p>

</li>
<li>
<p><strong>EventInterceptors</strong> (either sync or async) to notify storage level events, including mutations, partition transfer, failover, and so on.</p>

</li>
</ul>
</li>
<li>
<p><strong>NearCaches</strong> - Locally cached data based on previous requests with local content invalidated upon changes in the storage tier.</p>

</li>
<li>
<p><strong>ViewCaches</strong> - Locally stored view of remote data that can be a subset based on a predicate and is kept in sync, real time.</p>

</li>
<li>
<p><strong>Queries</strong> - Distributed, parallel query evaluation to return matching key, values, or entries with potential to optimize performance with indices.</p>

</li>
<li>
<p><strong>Aggregations</strong> - A map/reduce style aggregation where data is aggregated in parallel on all storage nodes, and results streamed back to the client for aggregation of those results to produce a final result.</p>

</li>
<li>
<p><strong>Data local processing</strong> - Ability to send a function to the relevant storage node to execute processing logic for the appropriate entries with exclusive access.</p>

</li>
<li>
<p><strong>Partition local transactions</strong> - Ability to perform scalable transactions by associating data (thus being on the same partition) and manipulating other entries on the same partition, potentially across different maps.</p>

</li>
<li>
<p><strong>Non-blocking / async NamedMap API</strong></p>

</li>
<li>
<p><strong>C&#43;&#43; and .NET clients</strong> - Access the same NamedMap API from either C&#43;&#43; or .NET.</p>

</li>
<li>
<p><strong>Portable Object Format</strong> - Optimized serialization format, with the ability to navigate the serialized form for optimized queries, aggregations, or data processing.</p>

</li>
<li>
<p><strong>Integration with Databases</strong> - Database and third party data integration with CacheStores, including synchronous or asynchronous writes.</p>

</li>
<li>
<p><strong>CohQL</strong> - Ansi-style query language with a console for adhoc queries.</p>

</li>
<li>
<p><strong>Topics</strong> - Distributed topics implementation that offers pub/sub messaging with the storage capacity, the cluster, and parallelizable subscribers.</p>

</li>
</ul>
<p>Coherence also provides a number of non-functional features:</p>

<ul class="ulist">
<li>
<p><strong>Rock solid clustering</strong> - Highly tuned and robust clustering stack that enables Coherence to scale to thousands of members in a cluster with thousands of partitions and terabytes of data being accessed, mutated, queried, and aggregated concurrently</p>

</li>
<li>
<p><strong>Safety first</strong> - Resilient data management that ensures backup copies are on distinct machines, racks, or sites, and the ability to maintain multiple backups.</p>

</li>
<li>
<p><strong>24/7 Availability</strong> - Zero downtime with rolling redeployment of cluster members to upgrade application or product versions.</p>
<ul class="ulist">
<li>
<p>Backward and forward compatibility of product upgrades, including major versions.</p>

</li>
</ul>
</li>
<li>
<p><strong>Persistent Maps</strong> - Ability to use local file system persistence (thus avoid extra network hops) and leverage Coherence consensus protocols to perform distributed disk recovery when appropriate.</p>

</li>
<li>
<p><strong>Distributed State Snapshot</strong> - Ability to perform distributed point-in-time snapshot of cluster state, and recover snapshot in this or a different cluster (leverages persistence feature).</p>

</li>
<li>
<p><strong>Lossy redundancy</strong> - Ability to reduce the redundancy guarantee by making backups and/or persistence asynchronous from a client perspective.</p>

</li>
<li>
<p><strong>Single Mangement View</strong> - Provides insight into the cluster  with a single JMX server that provides a view of all members of the cluster.</p>

</li>
<li>
<p><strong>Management over REST</strong> - All JMX data and operations can be performed over REST, including cluster wide thread dumps and heapdumps.</p>

</li>
<li>
<p><strong>Non-cluster Access</strong> - Provides access to the cluster from the outside via proxies, for distant (high latency) clients and for non-java languages such as C&#43;&#43; and .NET.</p>

</li>
<li>
<p><strong>Kubernetes friendly</strong> - Enables seemless and safe deployment of applications to k8s with our own <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-operator">operator</a>.</p>

</li>
</ul></dd>
</dl>
</v-card-text>
</v-card>
</v-card-text>
</v-card>
</v-flex>
</v-layout>

<h2 id="_features_not_included_in_coherence_community_edition">Features Not Included in Coherence Community Edition</h2>
<div class="section">
<p>The following Oracle Coherence features are not included in Coherence Community Edition:</p>

<ul class="ulist">
<li>
<p>Management of Coherence via the Oracle WebLogic Management Framework</p>

</li>
<li>
<p>Deployment of Grid Archives (GARs)</p>

</li>
<li>
<p>HTTP Session Management for Application Servers (Coherence*Web)</p>

</li>
<li>
<p>GoldenGate HotCache</p>

</li>
<li>
<p>TopLink-based CacheLoaders and CacheStores</p>

</li>
<li>
<p>Elastic Data</p>

</li>
<li>
<p>Federation and WAN (wide area network) Support</p>

</li>
<li>
<p>Transaction Framework</p>

</li>
<li>
<p>CommonJ Work Manager</p>

</li>
</ul>
</div>
</doc-view>
