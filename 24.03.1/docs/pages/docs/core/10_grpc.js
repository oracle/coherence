<doc-view>

<h2 id="_coherence_grpc_server">Coherence gRPC Server</h2>
<div class="section">
<p>The Coherence gRPC proxy server can run with either of two gRPC implementations.</p>

<ul class="ulist">
<li>
<p>Netty</p>

</li>
<li>
<p>Helidon 4+</p>

</li>
</ul>

<h3 id="_using_coherence_grpc_proxy_with_netty">Using Coherence gRPC Proxy With Netty</h3>
<div class="section">
<p>Applications that are not using Helidon 4+ that wish to run the Coherence gRPC proxy
need to use the Netty based Coherence gRPC proxy module <code>coherence-grpc-proxy</code>.</p>


<h4 id="_setting_up_the_netty_coherence_grpc_proxy_server">Setting Up the Netty Coherence gRPC Proxy Server</h4>
<div class="section">
<p>To set up and start using the Netty Coherence gRPC Server, you should declare the <code>coherence-grpc-proxy</code> module as a dependency of your project.</p>

<p>For example:</p>

<p>If using Maven, declare the server as follows:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependencyManagement&gt;
    &lt;dependencies&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
            &lt;artifactId&gt;coherence-bom&lt;/artifactId&gt;
            &lt;version&gt;${coherence.version}&lt;/version&gt;
            &lt;type&gt;pom&lt;/type&gt;
            &lt;scope&gt;import&lt;/scope&gt;
        &lt;/dependency&gt;
    &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;

&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;/dependency&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-grpc-proxy&lt;/artifactId&gt;
    &lt;/dependency&gt;
&lt;dependencies&gt;</markup>

<p>In the pom.xml file, coherence.version property is the version of Coherence being used, and coherence.groupId property is either the Coherence commercial group id, com.oracle.coherence, or the CE group id, com.oracle.coherence.ce.</p>

<p>If using Gradle, declare the server as follows:</p>

<markup
lang="groovy"
title="build.gradle"
>dependencies {
implementation platform("${coherenceGroupId}:coherence-bom:${coherenceVersion}")

    implementation "${coherenceGroupId}:coherence"
    implementation "${coherenceGroupId}:coherence-grpc-proxy"
}</markup>

<p>In the build.gradle file, coherenceVersion property is the version of Coherence being used, and coherenceGroupId property is either the Coherence commercial group id, com.oracle.coherence or the CE group id, com.oracle.coherence.ce.</p>

</div>
</div>

<h3 id="_using_coherence_grpc_proxy_with_helidon_4">Using Coherence gRPC Proxy With Helidon 4+</h3>
<div class="section">
<p>Applications that are using Helidon 4+ that wish to run the Coherence gRPC proxy
have the option to use Helidon&#8217;s gRPC implementation for the gRPC server.</p>

<p>Te Coherence gRPC Proxy server will run its own Helidon server to serve the Coherence gRPC requests,
this will be separate from any other Helidon web servers that the application might be running.</p>


<h4 id="_setting_up_the_helidon_coherence_grpc_proxy_server">Setting Up the Helidon Coherence gRPC Proxy Server</h4>
<div class="section">
<p>To set up and start using the Helidon Coherence gRPC Server, you should declare the <code>coherence-grpc-proxy-helidon</code> module as a dependency of your project.</p>

<p>For example:</p>

<p>If using Maven, declare the server as follows:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependencyManagement&gt;
    &lt;dependencies&gt;
        &lt;dependency&gt;
            &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
            &lt;artifactId&gt;coherence-bom&lt;/artifactId&gt;
            &lt;version&gt;${coherence.version}&lt;/version&gt;
            &lt;type&gt;pom&lt;/type&gt;
            &lt;scope&gt;import&lt;/scope&gt;
        &lt;/dependency&gt;
    &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;

&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
    &lt;/dependency&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-grpc-proxy-helidon&lt;/artifactId&gt;
    &lt;/dependency&gt;
&lt;dependencies&gt;</markup>

<p>In the pom.xml file, coherence.version property is the version of Coherence being used, and coherence.groupId property is either the Coherence commercial group id, com.oracle.coherence, or the CE group id, com.oracle.coherence.ce.</p>

<p>If using Gradle, declare the server as follows:</p>

<markup
lang="groovy"
title="build.gradle"
>dependencies {
implementation platform("${coherenceGroupId}:coherence-bom:${coherenceVersion}")

    implementation "${coherenceGroupId}:coherence"
    implementation "${coherenceGroupId}:coherence-grpc-proxy-helidon"
}</markup>

<p>In the build.gradle file, coherenceVersion property is the version of Coherence being used, and coherenceGroupId property is either the Coherence commercial group id, com.oracle.coherence or the CE group id, com.oracle.coherence.ce.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>If both the <code>coherence-grpc-proxy-helidon</code> module and the <code>coherence-grpc-proxy</code> module are
on the class path, the Helidon gRPC server will be used.</p>
</p>
</div>
</div>
</div>
</div>
</doc-view>
