<doc-view>

<h2 id="_helidon_mp_grpc_server">Helidon MP gRPC Server</h2>
<div class="section">
<p>Coherence gRPC proxy is the server-side implementation of the services defined within the <code>Coherence gRPC</code> module.
The gRPC proxy uses standard gRPC Java libraries to provide Coherence APIs over gRPC.</p>

<p>If using the <a id="" title="" target="_blank" href="https://helidon.io">Helidon Microprofile server</a> with the microprofile gRPC server enabled the Coherence
gRPC proxy can be deployed into the Helidon gRPC server instead of the Coherence default gRPC server.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence gRPC Server, you need to declare it as a dependency of your project;
for example if using Maven:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-helidon-grpc-proxy&lt;/artifactId&gt;
  &lt;version&gt;22.06.7&lt;/version&gt;
&lt;/dependency&gt;</markup>

<p>or for Gradle:</p>

<markup
lang="groovy"

>implementation 'com.oracle.coherence.ce:coherence-helidon-grpc-proxy:22.06.7'</markup>

</div>

<h2 id="_enable_the_proxy_service_in_helidon_mp">Enable the Proxy Service in Helidon MP</h2>
<div class="section">
<p>For this behaviour to happen automatically just set the <code>coherence.grpc.enabled</code> system property to <code>false</code>, which
will disable the built in server. A built-in <code>GrpcMpExtension</code> implementation will then deploy the proxy services
to the Helidon gRPC server.</p>

<div class="admonition warning">
<p class="admonition-inline">When using the Helidon MP gRPC server, if the <code>coherence.grpc.enabled</code> system property <strong>has not</strong> been set to
<code>false</code>, then both the Helidon gRPC server and the Coherence default gRPC server will start and could cause port
binding issues unless they are both specifically configured to use different ports.</p>
</div>
</div>
</doc-view>
