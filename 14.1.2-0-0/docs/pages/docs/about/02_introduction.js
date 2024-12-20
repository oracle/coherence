<doc-view>

<h2 id="_introduction">Introduction</h2>
<div class="section">
<p>First and foremost, Coherence provides a fundamental service that is responsible for all facets of clustering and is a
common denominator / building block for all other Coherence services.
This service, referred to as 'service 0' internally, ensures the mesh of members is maintained and responsive,
taking action to collaboratively evict, shun, or in some cases voluntarily depart the cluster when deemed necessary.
As members join and leave the cluster, other Coherence services are notified thus allowing those services to react accordingly.</p>

<div class="admonition note">
<p class="admonition-inline">This part of the Coherence product has been in production for 20+ years, and has been the subject of some extensive and
imaginative testing.
While it has been discussed here it certainly is not something that customers, generally, interact with directly but is
valuable to be aware of.</p>
</div>
<p>Coherence services build on top of the clustering service, with the key implementations to be aware of being
PartitionedService, InvocationService, and ProxyService.</p>

<p>In the majority of cases customers will deal with caches;
a cache will be represented by an implementation of <code>NamedCache&lt;K,V&gt;</code>.
Cache is an unfortunate name, as many customers use Coherence as a system-of-record rather than a lossy store of data.
A cache is hosted by a service, generally the PartitionedService, and is the entry point to storing, retrieving,
aggregating, querying, and streaming data.</p>

<p>There are a number of features that caches provide:</p>

<ul class="ulist">
<li>
<p>Fundamental <strong>key-based access</strong>: get/put getAll/putAll</p>

</li>
<li>
<p>Client-side and storage-side events</p>
<ul class="ulist">
<li>
<p><strong>MapListeners</strong> to asynchronously notify clients of changes to data</p>

</li>
<li>
<p><strong>EventInterceptors</strong> (either sync or async) to be notified storage level events, including mutations, partition transfer, failover, etc</p>

</li>
</ul>
</li>
<li>
<p><strong>NearCaches</strong> - locally cached data based on previous requests with local content invalidated upon changes in storage tier</p>

</li>
<li>
<p><strong>ViewCaches</strong> - locally stored view of remote data that can be a subset based on a predicate and is kept in sync real time</p>

</li>
<li>
<p><strong>Queries</strong> - distributed, parallel query evaluation to return matching key, values or entries with potential to optimize performance with indices</p>

</li>
<li>
<p><strong>Aggregations</strong> - a map/reduce style aggregation where data is aggregated in parallel on all storage nodes and results streamed back to the client for aggregation of those results to produce a final result</p>

</li>
<li>
<p><strong>Data local processing</strong> - an ability to send a function to the relevant storage node to execute processing logic for the appropriate entries with exclusive access</p>

</li>
<li>
<p><strong>Partition local transactions</strong> - an ability to perform scalable transactions by associating data (thus being on the same partition) and manipulating other entries on the same partition potentially across caches</p>

</li>
<li>
<p><strong>Non-blocking / async NamedCache API</strong></p>

</li>
<li>
<p><strong>Polyglot clients</strong> - access the same NamedCache API from <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-cpp-extend-client">C++</a>, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-go-client">Go</a>, Java, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-js-client">JavaScript</a>, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-dotnet-extend-client">.NET</a>, or <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-py-client">Python</a></p>

</li>
<li>
<p><strong>Portable Object Format</strong> - optimized serialization format, with the ability to navigate the serialized form for optimized queries, aggregations, or data processing</p>

</li>
<li>
<p><strong>Integration with Databases</strong> - Database &amp; third party data integration with CacheStores including both synchronous or asynchronous writes</p>

</li>
<li>
<p><strong>CohQL</strong> - ansi-style query language with a console for adhoc queries</p>

</li>
<li>
<p><strong>Topics</strong> - distributed topics implementation offering pub/sub messaging with the storage capacity the cluster and parallelizable subscribers</p>

</li>
<li>
<p><strong>Repository API</strong> - a framework implementing the Repository pattern from Domain-Driven Design, abstracting persistent storage implementation from application code, with advanced features like support for pagination, projections, streaming, and updating in-place</p>

</li>
<li>
<p><strong>coherence-concurrent</strong> - Coherence-backed implementations of types from the <code>java.util.concurrent</code> package enabling distributed process coordination through the grid</p>
<ul class="ulist">
<li>
<p><strong>Atomics</strong> - for implementing e.g. atomic counters shared between cluster, with an optional asynchronous API</p>

</li>
<li>
<p><strong>Executors</strong> - for submitting tasks to be executed in the cluster</p>

</li>
<li>
<p><strong>Locks</strong> - for implementing lock-based concurrency control across multiple cluster members</p>

</li>
<li>
<p><strong>Queues</strong> - for implementing blocking queue / dequeue behavior across multiple cluster members</p>

</li>
<li>
<p><strong>Semaphores</strong> - for implementing synchronization of execution across multiple cluster members</p>

</li>
</ul>
</li>
<li>
<p><strong>Microservices integration</strong> - broad and close integration with <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/integrate/index.html">Helidon</a>, <a id="" title="" target="_blank" href="https://micronaut-projects.github.io/micronaut-coherence/latest/guide/">Micronaut</a>, and <a id="" title="" target="_blank" href="https://spring.coherence.community/4.3.0/#/about/01_overview">Spring</a> for developing microservices applications using Coherence as a data source or cache</p>

</li>
</ul>
<p>There are also a number of non-functional features that Coherence provides:</p>

<ul class="ulist">
<li>
<p><strong>Rock solid clustering</strong> - highly tuned and robust clustering stack that allows Coherence to scale to thousands of members in a cluster with thousands of partitions and terabytes of data being accessed, mutated, queried and aggregated concurrently</p>

</li>
<li>
<p><strong>Safety first</strong> - resilient data management that ensures backup copies are on distinct machines, racks, or sites and the ability to maintain multiple backups</p>

</li>
<li>
<p><strong>24/7 Availability</strong> - zero down time with rolling redeploy of cluster members to upgrade application or product versions</p>
<ul class="ulist">
<li>
<p>Backwards and forwards compatibility of product upgrades, including major versions</p>

</li>
</ul>
</li>
<li>
<p><strong>Persistent Caches</strong> - with the ability to use local file system persistence (thus avoid extra network hops) and leverage Coherence consensus protocols to perform distributed disk recovery when appropriate</p>

</li>
<li>
<p><strong>Distributed State Snapshot</strong> - ability to perform distributed point-in-time snapshot of cluster state, and recover snapshot in this or a different cluster (leverages persistence feature)</p>

</li>
<li>
<p><strong>Lossy redundancy</strong> - ability to reduce the redundancy guarantee by making backups and/or persistence asynchronous from a client perspective</p>

</li>
<li>
<p><strong>Single Mangement View</strong> - provides insight into the cluster  with a single JMX server that provides a view of all members of the cluster</p>

</li>
<li>
<p><strong>Management over REST</strong> - all JMX data and operations can be performed over REST, including cluster wide thread dumps and heapdumps</p>

</li>
<li>
<p><strong>Non-cluster Access</strong> - access to the cluster from the outside via proxies, for distant (high latency) clients and for non-Java languages such as <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-cpp-extend-client">C++</a>, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-go-client">Go</a>, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-js-client">JavaScript</a>, <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-dotnet-extend-client">.NET</a>, or <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-py-client">Python</a></p>

</li>
<li>
<p><strong>Kubernetes friendly</strong> - seamlessly and safely deploy applications to k8s with our own <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-operator">operator</a></p>

</li>
</ul>
</div>

<h2 id="_coherence_community_edition_disabled_and_excluded_functionality">Coherence Community Edition Disabled and Excluded Functionality</h2>
<div class="section">
<p>Coherence Community Edition does not include the following Oracle Coherence commercial edition functionality</p>

<ul class="ulist">
<li>
<p>Management of Coherence via the Oracle WebLogic Management Framework</p>

</li>
<li>
<p>WebLogic Server Multi-tenancy support</p>

</li>
<li>
<p>Deployment of Grid Archives (GARs)</p>

</li>
<li>
<p>HTTP session management for application servers (Coherence*Web)</p>

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
<p>Federation and WAN (wide area network) support</p>

</li>
<li>
<p>Transaction Framework</p>

</li>
<li>
<p>CommonJ work manager</p>

</li>
</ul>
</div>
</doc-view>
