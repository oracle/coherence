<doc-view>

<h2 id="_federation">Federation</h2>
<div class="section">
<p>This guide walks through the steps to use Coherence Federation by using Coherence Query Language (CohQL)
to insert, update and remove data in Federated clusters.</p>

<p>Federated caching federates cache data asynchronously across multiple geographically dispersed clusters.
Cached data is federated across clusters to provide redundancy, off-site backup, and multiple points
of access for application users in different geographical locations.</p>

<p>Federated caching supports multiple federation topologies. These include: active-active, active-passive, hub-spoke,
and central-federation. The topologies define common federation strategies between clusters and support a wide variety of use cases.
Custom federation topologies can also be created as required.</p>

<p>Federated caching provides applications with the ability to accept, reject, or modify cache entries being stored
locally or remotely. Conflict resolution is application specific
to allow the greatest amount of flexibility when defining federation rules.</p>

<div class="admonition note">
<p class="admonition-inline">Federation is only available when using Coherence Grid Edition (GE) 12.2.1.4.X and above, and is not available in the open-source
Coherence Community Edition (CE).</p>
</div>
<div class="admonition note">
<p class="admonition-inline">As Coherence Grid Edition JAR&#8217;s and not available in Maven central, to build and run
this example you, must first install the Coherence JAR into your Maven Repository from your
local Grid Edition Install. See <router-link to="#building" @click.native="this.scrollFix('#building')">here</router-link> for instructions on how to complete this.</p>
</div>

<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-need" @click.native="this.scrollFix('#what-you-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#building" @click.native="this.scrollFix('#building')">Building the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#review-the-initial-project" @click.native="this.scrollFix('#review-the-initial-project')">Review the Project</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#maven" @click.native="this.scrollFix('#maven')">Maven Configuration</router-link></p>

</li>
<li>
<p><router-link to="#cache-config" @click.native="this.scrollFix('#cache-config')">Federation Configuration</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#run-the-example" @click.native="this.scrollFix('#run-the-example')">Build and Run the Example</router-link></p>

</li>
<li>
<p><router-link to="#summary" @click.native="this.scrollFix('#summary')">Summary</router-link></p>

</li>
<li>
<p><router-link to="#see-also" @click.native="this.scrollFix('#see-also')">See Also</router-link></p>

</li>
</ul>
</div>

<h3 id="what-you-will-build">What You Will Build</h3>
<div class="section">
<p>You will review the operational and cache configuration required to set up Federated Coherence clusters and carry out the following:</p>

<ol style="margin-left: 15px;">
<li>
Start one or more cache servers for <code>PrimaryCluster</code>

</li>
<li>
Start one or more cache servers for <code>SecondaryCluster</code>

</li>
<li>
Start a <code>CohQL</code> session for <code>PrimaryCluster</code>

</li>
<li>
Start a <code>CohQL</code> session for <code>SecondaryCluster</code>

</li>
<li>
Carry out various data operations on each cluster and observe the data being replicated

</li>
</ol>
<p>The default configuration for this example runs the following clusters:</p>

<ol style="margin-left: 15px;">
<li>
<code>PrimaryCluster</code> on 127.0.0.1:7574

</li>
<li>
<code>SecondaryCluster</code> on 127.0.0.1:7575

</li>
</ol>
<p>You can change these hosts/ports by changing the following in the <code>pom.xml</code>:</p>

<markup
lang="xml"

>&lt;primary.cluster.host&gt;127.0.0.1&lt;/primary.cluster.host&gt;
&lt;primary.cluster.port&gt;7574&lt;/primary.cluster.port&gt;
&lt;secondary.cluster.host&gt;127.0.0.1&lt;/secondary.cluster.host&gt;
&lt;secondary.cluster.port&gt;7575&lt;/secondary.cluster.port&gt;</markup>

<div class="admonition note">
<p class="admonition-inline">If you wish to know more about Coherence Federation, please see the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/administer/federating-caches-clusters.html">Coherence Documentation</a>.</p>
</div>
</div>

<h3 id="what-you-need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 11</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included so they can be built without first installing
either build tool.</p>

</li>
<li>
<p>You can also import the code straight into your IDE:</p>
<ul class="ulist">
<li>
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>

<h4 id="building">Building the Example Code</h4>
<div class="section">
<p><strong>Important</strong></p>

<p>Because Coherence Federation is only available in Grid Edition, you must carry out the following changes to the project before building and running:</p>

<ol style="margin-left: 15px;">
<li>
Update the <code>coherence.version</code> property in your <code>pom.xml</code> and <code>gradle.properties</code> to the Coherence Grid Edition version you are going to use.

</li>
<li>
Change the <code>coherence.group.id</code> in the above files to <code>com.oracle.coherence</code>.

</li>
<li>
Install Coherence Grid Edition into your local Maven repository by running the following:
<p>This example assumes you have Coherence 14.1.1. Please adjust for your Coherence version.</p>

<markup
lang="bas"

>mvn install:install-file -Dfile=$COHERENCE_HOME/lib/coherence.jar \
    -DpomFile=$COHERENCE_HOME/plugins/maven/com/oracle/coherence/coherence/14.1.1/coherence.14.1.1.pom</markup>

</li>
</ol>
<p>Whenever you are asked to build the code, please refer to the instructions below.</p>

<p>The source code for the guides and tutorials can be found in the
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence/tree/master/prj/examples">Coherence CE GitHub repo</a></p>

<p>The example source code is structured as both a Maven and a Gradle project and can be easily built with either
of those build tools. The examples are stand-alone projects so each example can be built from the
specific project directory without needing to build the whole Coherence project.</p>

<ul class="ulist">
<li>
<p>Build with Maven</p>

</li>
</ul>
<p>Using the included Maven wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Build with Gradle</p>

</li>
</ul>
<p>Using the included Gradle wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./gradlew build</markup>

</div>
</div>

<h3 id="review-the-initial-project">Review the Project</h3>
<div class="section">

<h4 id="maven">Maven Configuration</h4>
<div class="section">
<p>The initial project is a Coherence project and imports the <code>coherence-bom</code> and <code>coherence-dependencies</code>
POMs as shown below:</p>

<markup
lang="xml"

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
&lt;/dependencyManagement&gt;</markup>

<p>The <code>coherence</code> library is also included:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
  &lt;artifactId&gt;coherence&lt;/artifactId&gt;
&lt;/dependency&gt;</markup>

<p>We also define a number of profiles to run the <code>DefaultCacheServer</code> for each cluster and <code>CohQL</code> for each cluster.</p>

<ul class="ulist">
<li>
<p><router-link to="#primary-storage" @click.native="this.scrollFix('#primary-storage')">primary-storage - Runs a DefaultCacheServer for the PrimaryCluster</router-link></p>

</li>
<li>
<p><router-link to="#primary-cohql" @click.native="this.scrollFix('#primary-cohql')">primary-cohql - Runs a CohQL session for the PrimaryCluster</router-link></p>

</li>
<li>
<p><router-link to="#secondary-storage" @click.native="this.scrollFix('#secondary-storage')">secondary-storage - Runs a DefaultCacheServer for the SecondaryCluster</router-link></p>

</li>
<li>
<p><router-link to="#secondary-cohql" @click.native="this.scrollFix('#secondary-cohql')">secondary-cohql - Runs a CohQL session for the SecondaryCluster</router-link></p>

</li>
</ul>
<p id="primary-storage"><strong>primary-storage</strong> - Runs a <code>DefaultCacheServer</code> for the <code>PrimaryCluster</code></p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;primary-storage&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;primary-storage&lt;/name&gt;
    &lt;/property&gt;
  &lt;/activation&gt;
  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
        &lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;${maven.exec.plugin.version}&lt;/version&gt;
        &lt;configuration&gt;
          &lt;executable&gt;java&lt;/executable&gt;
          &lt;arguments&gt;
            &lt;argument&gt;-classpath&lt;/argument&gt;&lt;classpath/&gt;
            &lt;argument&gt;-Dcoherence.cacheconfig=federation-cache-config.xml&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Xmx512m&lt;/argument&gt;
            &lt;argument&gt;-Xms512m&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Dprimary.cluster.port=${primary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;-Dsecondary.cluster.port=${secondary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;-Dprimary.cluster.host=${primary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dsecondary.cluster.host=${secondary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.wka=${primary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.cluster=PrimaryCluster&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.clusterport=${primary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;com.tangosol.net.DefaultCacheServer&lt;/argument&gt;
          &lt;/arguments&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

<p id="primary-cohql"><strong>primary-cohql</strong> - Runs a <code>CohQL</code> session for the <code>PrimaryCluster</code></p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;primary-cohql&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;primary-cohql&lt;/name&gt;
    &lt;/property&gt;
  &lt;/activation&gt;
  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
        &lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;${maven.exec.plugin.version}&lt;/version&gt;
        &lt;configuration&gt;
          &lt;systemProperties&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.cacheconfig&lt;/key&gt;
              &lt;value&gt;federation-cache-config.xml&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.cluster&lt;/key&gt;
              &lt;value&gt;PrimaryCluster&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.wka&lt;/key&gt;
              &lt;value&gt;${primary.cluster.host}&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.clusterport&lt;/key&gt;
              &lt;value&gt;${primary.cluster.port}&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.distributed.localstorage&lt;/key&gt;
              &lt;value&gt;false&lt;/value&gt;
            &lt;/property&gt;
          &lt;/systemProperties&gt;
          &lt;mainClass&gt;com.tangosol.coherence.dslquery.QueryPlus&lt;/mainClass&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

<p id="secondary-storage"><strong>secondary-storage</strong> - Runs a <code>DefaultCacheServer</code> for the <code>SecondaryCluster</code></p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;secondary-storage&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;secondary-storage&lt;/name&gt;
    &lt;/property&gt;
  &lt;/activation&gt;
  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
        &lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;${maven.exec.plugin.version}&lt;/version&gt;
        &lt;configuration&gt;
          &lt;executable&gt;java&lt;/executable&gt;
          &lt;arguments&gt;
            &lt;argument&gt;-classpath&lt;/argument&gt;
            &lt;classpath/&gt;
            &lt;argument&gt;-Dcoherence.cacheconfig=federation-cache-config.xml&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Xmx512m&lt;/argument&gt;
            &lt;argument&gt;-Xms512m&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Dprimary.cluster.port=${primary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;-Dsecondary.cluster.port=${secondary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;-Dprimary.cluster.host=${primary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dsecondary.cluster.host=${secondary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.wka=${secondary.cluster.host}&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.cluster=SecondaryCluster&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.clusterport=${secondary.cluster.port}&lt;/argument&gt;
            &lt;argument&gt;com.tangosol.net.DefaultCacheServer&lt;/argument&gt;
          &lt;/arguments&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

<p id="secondary-cohql"><strong>primary-cohql</strong> - Runs a <code>CohQL</code> session for the <code>SecondaryCluster</code></p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;secondary-cohql&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;secondary-cohql&lt;/name&gt;
    &lt;/property&gt;
  &lt;/activation&gt;
  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
        &lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;${maven.exec.plugin.version}&lt;/version&gt;
        &lt;configuration&gt;
          &lt;systemProperties&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.cacheconfig&lt;/key&gt;
              &lt;value&gt;federation-cache-config.xml&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.log.level&lt;/key&gt;
              &lt;value&gt;3&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.cluster&lt;/key&gt;
              &lt;value&gt;SecondaryCluster&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.wka&lt;/key&gt;
              &lt;value&gt;${secondary.cluster.host}&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.clusterport&lt;/key&gt;
              &lt;value&gt;${secondary.cluster.port}&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.distributed.localstorage&lt;/key&gt;
              &lt;value&gt;false&lt;/value&gt;
            &lt;/property&gt;
          &lt;/systemProperties&gt;
          &lt;mainClass&gt;com.tangosol.coherence.dslquery.QueryPlus&lt;/mainClass&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

</div>

<h4 id="cache-config">Federation Configuration</h4>
<div class="section">
<p>Federated caching is configured using Coherence configuration files and requires no changes to application code.</p>

<p>There are two areas that require configuration for Federation:</p>

<ol style="margin-left: 15px;">
<li>
An operational override file is used to configure federation participants and the federation topology.

</li>
<li>
A cache configuration file is used to create federated caches schemes.

</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">A federated cache is a type of partitioned cache service and is managed by a federated cache service instance.</p>
</div>
<ol style="margin-left: 15px;">
<li>
The following cache configuration file is used to define the Federated service:
<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;  <span class="conum" data-value="1" />
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;federated&lt;/scheme-name&gt;
      &lt;key-type&gt;java.lang.Integer&lt;/key-type&gt;
      &lt;value-type&gt;java.lang.String&lt;/value-type&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;federated-scheme&gt;  <span class="conum" data-value="2" />
      &lt;scheme-name&gt;federated&lt;/scheme-name&gt;
      &lt;service-name&gt;FederatedPartitionedCache&lt;/service-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme&gt;
          &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
        &lt;/local-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
      &lt;topologies&gt; <span class="conum" data-value="3" />
        &lt;topology&gt;
          &lt;name&gt;MyTopology&lt;/name&gt;
        &lt;/topology&gt;
      &lt;/topologies&gt;
    &lt;/federated-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">A cache-mapping for all caches (*) to map to a scheme called <code>federated</code></li>
<li data-value="2">The federated-scheme in a similar way to a distributed-scheme</li>
<li data-value="3">A topology for the federated-scheme. The default topology is <code>active-active</code> so this element is not required and just included for completeness.</li>
</ul>
</li>
<li>
The following operational configuration file is used to define the participants and topology:
<markup
lang="xml"

>&lt;federation-config&gt;
  &lt;participants&gt;
    &lt;participant&gt;  <span class="conum" data-value="1" />
      &lt;name system-property="primary.cluster"&gt;PrimaryCluster&lt;/name&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address system-property="primary.cluster.host"&gt;127.0.0.1&lt;/address&gt;
          &lt;port system-property="primary.cluster.port"&gt;7574&lt;/port&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/participant&gt;
    &lt;participant&gt;  <span class="conum" data-value="2" />
      &lt;name system-property="primary.cluster"&gt;SecondaryCluster&lt;/name&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address system-property="secondary.cluster.host"&gt;127.0.0.1&lt;/address&gt;
          &lt;port system-property="secondary.cluster.port"&gt;7575&lt;/port&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/participant&gt;
  &lt;/participants&gt;
  &lt;topology-definitions&gt;  <span class="conum" data-value="3" />
    &lt;active-active&gt;
      &lt;name&gt;MyTopology&lt;/name&gt;
      &lt;active system-property="primary.cluster"&gt;PrimaryCluster&lt;/active&gt;
      &lt;active system-property="secondary.cluster"&gt;SecondaryCluster&lt;/active&gt;
    &lt;/active-active&gt;
  &lt;/topology-definitions&gt;
&lt;/federation-config&gt;</markup>

<ul class="colist">
<li data-value="1"><code>PrimaryCluster</code> participant with its host and port for the cluster Name Service</li>
<li data-value="2"><code>SecondaryCluster</code> participant with its host and port for the cluster Name Service</li>
<li data-value="3">Topology that defines an <code>active-active</code> configuration between clusters. This is the default and not strictly required.</li>
</ul>
</li>
</ol>
</div>
</div>

<h3 id="run-the-example">Run the Example</h3>
<div class="section">
<p>After you have built the project as described earlier in this document you can run via Maven or Gradle.</p>


<h4 id="_maven">Maven</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Start a DefaultCache server for the primary and secondary clusters in separate terminals.
<markup
lang="bash"

>./mvnw exec:exec -P primary-storage</markup>

<p>and</p>

<markup
lang="bash"

>./mvnw exec:exec -P secondary-storage</markup>

</li>
<li>
Start a CohQL session for the primary and secondary clusters in separate terminals.
<markup
lang="bash"

>./mvnw exec:java -P primary-cohql</markup>

<p>and</p>

<markup
lang="bash"

>./mvnw exec:java -P secondary-cohql</markup>

</li>
</ol>
</div>

<h4 id="_gradle">Gradle</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Start a DefaultCache server for the primary and secondary clusters in separate terminals.
<markup
lang="bash"

>./gradlew runServerPrimary</markup>

<p>and</p>

<markup
lang="bash"

>./gradlew runServerSecondary</markup>

</li>
<li>
Start a CohQL session for the primary and secondary clusters in separate terminals.
<markup
lang="bash"

>./gradlew runCohQLPrimary --console=plain</markup>

<p>and</p>

<markup
lang="bash"

>./gradlew runCohQLSecondary --console=plain</markup>

</li>
</ol>
</div>

<h4 id="_run_the_following_commands_to_exercise_federation">Run the following commands to exercise Federation</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
In each of the <code>CohQL</code> sessions, run the following command to verify the caches are empty in each cluster:
<markup
lang="bash"

>CohQL&gt; select count() from test
Results
0</markup>

</li>
<li>
In the first (PrimaryCluster) <code>CohQL</code> session, add an entry to the cache <code>test</code>:
<markup
lang="bash"

>CohQL&gt; insert into test key(1) value("Tim")

CohQL&gt; select key(), value() from test
Results
[1, "Tim"]</markup>

</li>
<li>
In the second (SecondaryCluster) <code>CohQL</code> session, verify the entry was sent from the PrimaryCluster and
then update the name to <code>Timothy</code>. As the clusters are <code>active-active</code>, the changes will be sent back to the primary cluster.
<markup
lang="bash"

>CohQL&gt; select key(), value() from test
Results
[1, "Tim"]

CohQL&gt; update 'test' set value() = "Timothy"
Results
1: true

CohQL&gt; select key(), value() from test
Results
[1, "Timothy"]</markup>

</li>
<li>
In the first (PrimaryCluster) <code>CohQL</code> session, verify the entry was changed via the change in the <code>SecondaryCluster</code>, then delete the entry and confirm it was deleted in the <code>SecondaryCluster</code>
<markup
lang="bash"

>CohQL&gt; select key(), value() from test
Results
[1, "Timothy"]

CohQL&gt; update 'test' set value() = "Timothy"
Results
1: true

CohQL&gt; select key(), value() from test
Results
[1, "Timothy"]

CohQL&gt; delete from test
Results

CohQL&gt; select key(), value() from test
Results</markup>

</li>
<li>
Continue experimenting:
<p>You can continue to experiment by inserting, updating or removing data using various <code>CohQL</code> commands.</p>

<p>For detailed information on how to use CohQL, please visit the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-coherence-query-language.html#GUID-C0D082B1-FA62-4899-A043-4345156E6641">Using Coherence Query Language</a>
in the Coherence reference guide.</p>

</li>
<li>
Monitor Federation
<p>If you want to monitor Federation, you can do this via the Coherence VisualVM Plugin.
See <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-visualvm#install">here</a>
for how to install the Plugin if you have <code>VisualVM</code> already, otherwise visit <a id="" title="" target="_blank" href="https://visualvm.github.io/">https://visualvm.github.io/</a> to download
and install <code>VisualVM</code>.</p>

<p>Once you have installed the plugin, you can click on one of the <code>DefaultCacheServer</code> process, and you will see the <code>Federation</code> tab as shown below:</p>



<v-card>
<v-card-text class="overflow-y-hidden" >
<img src="docs/images/visualvm-federation.png" alt="visualvm federation"width="80%" />
</v-card-text>
</v-card>

</li>
</ol>
</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this guide you walked through the steps to use Coherence Federation by using Coherence Query Language (CohQL)
to insert, update and remove data in Federated clusters.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/administer/federating-caches-clusters.html">Federation Documentation</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-coherence-query-language.html#GUID-C0D082B1-FA62-4899-A043-4345156E6641">Using Coherence Query Language</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
