<doc-view>

<h2 id="_microprofile_health">Microprofile Health</h2>
<div class="section">
<p>Coherence MicroProfile (MP) Health provides support for Eclipse MicroProfile Health within the Coherence cluster members.
See the documentation on the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/manage/using-health-check-api.html">Coherence Health Check API</a>
and <a id="" title="" target="_blank" href="https://microprofile.io/project/eclipse/microprofile-health">MicroProfile Health</a>.</p>

<p>Coherence MP Health is a very simple module that enables you to publish Coherence health checks into the MicroProfile Health Check Registries available at runtime.</p>


<h3 id="_enabling_the_use_of_coherence_mp_health">Enabling the Use of Coherence MP Health</h3>
<div class="section">
<p>To use Coherence MP Health, you should first declare it as a dependency in the project&#8217;s pom.xml file.</p>

<p>You can declare Coherence MP Health as follows:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependency&gt;
    &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-mp-health&lt;/artifactId&gt;
    &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>Where <code>${coherence.groupId}</code> is the Maven groupId for the Coherence edition being used, <code>com.oracle.coherence</code>
for the commercial edition or <code>com.oracle.coherence.ce</code> for the community edition.
And <code>${coherence.version}</code> is the version of Coherence being used.</p>

<p>After the module becomes available in the class path, the Coherence <code>HealthCheck</code> producer CDI bean will be automatically
discovered and be registered as a Microprofile health check provider.
The Coherence health checks will then be available via any health endpoints served by the application and included in started, readiness and liveness checks.</p>

</div>
</div>
</doc-view>
