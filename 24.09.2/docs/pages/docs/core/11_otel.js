<doc-view>

<h2 id="_opentelemetry_support">OpenTelemetry Support</h2>
<div class="section">
<p>This version of Coherence adds support for <code>OpenTelemetry</code> in addition to <code>OpenTracing</code>
as an option for distributed tracing within a Coherence cluster.</p>

<p>Coherence does not include any tracing implementation libraries. Therefore, the
developer will need to provide the desired tracing runtime.  As OpenTracing is no
longer maintained, it is recommended that OpenTelemetry be used instead.
A minimum of OpenTelemetry for Java version 1.29 or later is recommended.  OpenTracing,
while now deprecated in Coherence, is still a supported option using the latest
OpenTracing 0.33.0.</p>

</div>

<h2 id="_dependencies">Dependencies</h2>
<div class="section">
<p>At a minimum, the following OpenTelemetry dependencies (version 1.29 or later) are required in order
to enable support in Coherence:</p>

<ul class="ulist">
<li>
<p>opentelemetry-api</p>

</li>
<li>
<p>opentelemetry-context</p>

</li>
<li>
<p>opentelemetry-sdk</p>

</li>
</ul>
</div>

<h2 id="_configuration">Configuration</h2>
<div class="section">
<p>If it&#8217;s desirable for Coherence to manage the initialization and lifecycle
of the tracing runtime, the following dependency is also required:</p>

<ul class="ulist">
<li>
<p>opentelemetry-sdk-extension-autoconfigure</p>

</li>
</ul>
<p>Refer to the <a id="" title="" target="_blank" href="https://github.com/open-telemetry/opentelemetry-java/tree/main/sdk-extensions/autoconfigure">documentation</a> for this library for details on how to configure
the tracing runtime.</p>

<p>This will also require setting the following system property <code>-Dotel.java.global-autoconfigure.enabled=true</code> when starting Coherence
(in addition to any other telemetry related properties that may be used)</p>

<p>In order for Coherence to generate tracing spans, edit the operational override tangosol-coherence-override.xml file
and add a &lt;tracing-config&gt; element with a child &lt;sampling-ratio&gt; element.</p>

<p>For example:</p>

<markup
lang="xml"

>&lt;tracing-config&gt;
  &lt;sampling-ratio&gt;0&lt;/sampling-ratio&gt; &lt;!-- user-initiated tracing --&gt;
&lt;/tracing-config&gt;</markup>

<p>The <code>coherence.tracing.ratio</code> system property is used to specify the tracing sampling ratio instead
of using the operational override file. For example:</p>

<markup
lang="bash"

>-Dcoherence.tracing.ratio=0</markup>

<p>Tracing operates in three modes:</p>

<ul class="ulist">
<li>
<p><code>-1</code> - This value disables tracing.</p>

</li>
<li>
<p><code>0</code> - This value enables user-initiated tracing. This means that Coherence will not initiate tracing on its own and the application should start an outer tracing span, from which Coherence will collect the inner tracing spans. If the outer tracing span is not started, the tracing activity will not be performed.</p>

</li>
<li>
<p><code>0.01-1.0</code> - This range indicates the tracing span being collected. For example, a value of 1.0 will result in all spans being collected, while a value of 0.1 will result in roughly 1 out of every 10 spans being collected.</p>

</li>
</ul>

<h3 id="_externally_managed_tracer">Externally Managed Tracer</h3>
<div class="section">
<p>It is possible to use a Tracer that has already been created with Coherence
by simply ensuring that the Tracer is available via the <code>GlobalOpenTelemtry</code> API included
with the OpenTelemetry. When this is the case, Coherence will use the available Tracer,
but will not attempt to configure or close the tracer when the cluster member is terminated.</p>

</div>
</div>

<h2 id="_traced_operations">Traced Operations</h2>
<div class="section">
<p>The following Coherence traced operations may be captured:</p>

<ul class="ulist">
<li>
<p>All operations exposed by the NamedCache API when using partitioned caches.</p>

</li>
<li>
<p>Events processed by event listeners (such as EventInterceptor or MapListener).</p>

</li>
<li>
<p>Persistence operations.</p>

</li>
<li>
<p>CacheStore operations.</p>

</li>
<li>
<p>ExecutorService operations.</p>

</li>
</ul>
</div>

<h2 id="_user_initiated_tracing">User Initiated Tracing</h2>
<div class="section">
<p>When the sampling ratio is set to <code>0</code>, the application will be required to start a tracing
span prior to invoking a Coherence operation.</p>

<markup
lang="java"

>Tracer     tracer    = GlobalOpenTelemetry.getTracer("your-tracer");
Span       span      = tracer.spanBuilder("test").startSpan();
NamedCache cache     = CacheFactory.get("some-cache");

try (Scope scope = span.makeCurrent())
    {
    cache.put("a", "b");
    cache.get("a");
    }
finally
    {
    span.end();
    }</markup>

</div>
</doc-view>
