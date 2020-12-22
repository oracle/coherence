<doc-view>

<h2 id="_parallel_recovery">Parallel Recovery</h2>
<div class="section">
<p>Coherence introduced a <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/administer/persisting-caches.html#GUID-3DC46E44-21E4-4DC4-9D12-231DE57FE7A1">disk-based persistence feature</a> from version 12.2.1. This feature accommodates for the loss of an entire cluster, and/or simultaneous loss of a primary and backup ensuring data is recovered from disk and made available.</p>

<p>This process of making data available is parallel across the cluster with each storage member recovering a fair share of partitions. While this recovery is in parallel across different members/processes, each member uses a single thread to recover.</p>

<p>As of 20.12, Coherence now recovers data in parallel within a member/process as well as in parallel across the cluster. This allows the cluster, and more importantly the associated data, to be made available as quickly as possible. Ultimately the goal is to have recovery be only limited by the throughput and latency of underlying device. Therefore this feature does assume increasing usage of the device (by accessing data in parallel) will provide some benefit and reduce the overall time to recover data.</p>

<p>The number of threads Coherence uses can be tweaked by the following JVM argument:</p>

<div class="listing">
<pre> coherence.distributed.persistence.recover.threads</pre>
</div>

<p>This argument may be removed in a future release as we work towards deriving an optimal value. The default value is based on the following formula:</p>

<div class="listing">
<pre>num-cores * (max-heap-size / machine-memory) + 2</pre>
</div>

</div>
</doc-view>
