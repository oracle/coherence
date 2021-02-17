<doc-view>

<h2 id="_coherence_micrometer_metrics">Coherence Micrometer Metrics</h2>
<div class="section">
<p>The <code>coherence-micrometer</code> module provides integration between Coherence metrics and Micrometer allowing Coherence
metrics to be published via any of the Micrometer registries.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence Micrometer metrics, you need to declare the module as a dependency in your <code>pom.xml</code>
and bind your Micrometer registry with the Coherence metrics adapter:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-micrometer&lt;/artifactId&gt;
        &lt;version&gt;20.12.1&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>The <code>coherence-micrometer</code> provides a Micrometer <code>MeterBinder</code> implementation class called <code>CoherenceMicrometerMetrics</code>.
This class is a singleton and cannot be constructed, to access it use the <code>CoherenceMicrometerMetrics.INSTANCE</code> field.</p>

<p>Micrometer provides many registry implementations to support different metrics applications and formats.
For example, to bind Coherence metrics to the Micrometer <code>PrometheusMeterRegistry</code>, create the <code>PrometheusMeterRegistry</code>
as documented in the <a id="" title="" target="_blank" href="https://micrometer.io/docs">Micrometer documentation</a>, and call the <code>CoherenceMicrometerMetrics</code>
class&#8217;s <code>bindTo</code> method:</p>

<markup
lang="java"

>PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

// complete registy configuration...

CoherenceMicrometerMetrics.INSTANCE.bindTo(prometheusRegistry);</markup>

<p>Micrometer registries can be bound to Coherence at any time, before or after Coherence starts. As Coherence creates
or removed metrics they will be registered with or removed from the Micrometer registries.</p>

</div>

<h2 id="_automatic_global_registry_binding">Automatic Global Registry Binding</h2>
<div class="section">
<p>Micrometer has a global registry available which Coherence will bind to automatically if the
<code>coherence.micrometer.bind.to.global</code> system property has been set to <code>true</code> (this property is <code>false</code> by default).</p>

</div>
</doc-view>
