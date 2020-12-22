<doc-view>

<h2 id="_coherence_microprofile_metrics">Coherence MicroProfile Metrics</h2>
<div class="section">
<p>Coherence MP Metrics provides support for [Eclipse MicroProfile Metrics] (<a id="" title="" target="_blank" href="https://microprofile.io/project/eclipse/microprofile-metrics">https://microprofile.io/project/eclipse/microprofile-metrics</a>) within Coherence cluster members.</p>

<p>This is a very simple module that allows you to publish Coherence metrics into MicroProfile Metric Registries available at runtime, and adds Coherence-specific tags to all the metrics published within the process, in order to distinguish them on the monitoring server, such as Prometheus.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence MP Metrics, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;dependency&gt;
    &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-mp-metrics&lt;/artifactId&gt;
    &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>That&#8217;s it&#8201;&#8212;&#8201;once the module above is in the class path, Coherence will discover <code>MpMetricRegistryAdapter</code> service it provides, and use it to publish all standard Coherence metrics to the vendor registry, and any user-defined application metrics to the application registry.</p>

<p>All the metrics will be published as gauges, because they represent point-in-time values of various MBean attributes.</p>

</div>

<h2 id="_coherence_global_tags">Coherence Global Tags</h2>
<div class="section">
<p>There could be hundreds of members in a Coherence cluster, with each member  publishing potentially the same set of metrics.
There could also be many Coherence clusters in the environment, possibly publishing to the same monitoring server instance.</p>

<p>In order to distinguish metrics coming from different clusters, as well as from different members of the same cluster, Coherence MP Metrics will automatically add several tags to <strong>ALL</strong> the metrics published within the process.</p>

<p>The tags added are:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Tag Name</th>
<th>Tag Value</th>
</tr>
</thead>
<tbody>
<tr>
<td class=""><code>cluster</code></td>
<td class="">the cluster name</td>
</tr>
<tr>
<td class=""><code>site</code></td>
<td class="">the site the member belongs to (if set)</td>
</tr>
<tr>
<td class=""><code>machine</code></td>
<td class="">the machine member is on (if set)</td>
</tr>
<tr>
<td class=""><code>member</code></td>
<td class="">the name of the member (if set)</td>
</tr>
<tr>
<td class=""><code>node_id</code></td>
<td class="">the node ID of the member</td>
</tr>
<tr>
<td class=""><code>role</code></td>
<td class="">the member&#8217;s role</td>
</tr>
</tbody>
</table>
</div>
<p>This ensures that the metrics published by one member do not collide with and  overwrite the metrics published by another members, and allows you to query and  aggregate metrics based on the values of the tags above if desired.</p>

</div>
</doc-view>
