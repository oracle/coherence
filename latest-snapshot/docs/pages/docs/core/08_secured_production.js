<doc-view>

<h2 id="_secured_production_mode">Secured Production Mode</h2>
<div class="section">
<p>When Coherence is running in secured production mode, TLS/SSL is enabled for communication between cluster nodes, and between Coherence*Extend clients and proxy servers.</p>


<h3 id="_enabling_coherence_secured_production_mode">Enabling Coherence Secured Production Mode</h3>
<div class="section">
<p>Prior to enable secured production mode, an SSL Socket Provider must be defined to be used by TCMP for communication between cluster nodes and by clients and proxies for Coherence*Extend. The global socket provider must also be set to use the SSL socket provider.  See the documentation on
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/secure/using-ssl-secure-communication.html">Using SSL to Secure Communication</a>.</p>

<p>To enable secured production mode, you can either configure it in the Coherence operational override file or use the system property, coherence.secured.production. By default, secured production mode is not enabled. If Coherence is running in development mode, the secured production mode setting is ignored.</p>

<p>You can configure secured production mode as follows:</p>

<markup
lang="xml"
title="operational-override.xml"
>&lt;cluster-config&gt;
  ...
  &lt;global-socket-provider system-property="coherence.global.socketprovider"&gt;mySSL&lt;/global-socket-provider&gt;
  &lt;socket-providers&gt;
    &lt;socket-provider id="mySSL"&gt;
      &lt;ssl&gt;
      ...
      &lt;/ssl&gt;
    &lt;/socket-provider&gt;
  &lt;/socket-providers&gt;
  ...
  &lt;secured-production system-property="coherence.secured.production"&gt;true&lt;/secured-production&gt;
&lt;/cluster-config&gt;</markup>

<p>The <strong>coherence.secured.production</strong> system property can also be used to enable secured production mode. For example, if an SSL socket provider, "mySSL", is defined in the operational override:</p>

<markup
lang="bash"

>-Dcoherence.global.socketprovider=mySSL -Dcoherence.secured.production=true</markup>

</div>
</div>
</doc-view>
