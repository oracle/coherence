<doc-view>

<h2 id="_federation">Federation</h2>
<div class="section">
<p>This guide walks through the steps to use Coherence Federation to federate cache data asynchronously between two Coherence clusters.
Federation is typically used across multiple geographically dispersed clusters to provide redundancy, off-site backup, and multiple points
of access for application users in different locations.</p>

<p>Federated caching supports multiple federation topologies. These include: active-active, active-passive, hub-spoke,
and central-federation. The topologies define common federation strategies between clusters and support a wide variety of use cases.
Custom federation topologies can also be created as required.</p>

<p><strong>Connecting Clusters</strong></p>

<p>When configuring the connection between clusters, there are two methods:</p>

<ol style="margin-left: 15px;">
<li>
Specify the Coherence Name Service (or cluster port) of one or more cluster members in the destination cluster. Destination members are automatically discovered and connected to. This method is easiest to configure, but not always practical with load balancer or firewall in between clsuters.

</li>
<li>
Specify a host and port of a destination cluster load balancer, which will load balance across specified hosts and ports on the designation cluster.

</li>
</ol>
<p>In this example we will use the first method by running two clusters on the same host, with different cluster ports, for simplicity of setup.</p>

<div class="admonition note">
<p class="admonition-inline">We are only federating a single service. If you wish to federate multiple services, the section on <router-link to="#mutliple-services" @click.native="this.scrollFix('#mutliple-services')">multiple services</router-link> for more information on this.</p>
</div>
<p>This example starts <code>ClusterA</code> on cluster port 7574 and <code>ClusterB</code> on port 7575 as shown in the diagram below:</p>



<v-card>
<v-card-text class="overflow-y-hidden" >
<img src="docs/images/federation.png" alt="federation"width="80%" />
</v-card-text>
</v-card>

<p>Notes - when using Name Service lookup</p>

<ol style="margin-left: 15px;">
<li>
On initial connection to destination cluster from any member, the destination Name Service is contacted to lookup an address to connect to

</li>
<li>
Once the address of a federated service on one of the members has been provided, the source member communicates directly to the destination member&#8217;s address to send data

</li>
</ol>
<p>In scenarios where you require Federation across data centres and through firewalls or load balancers, you must specify the address of
a load balancer. See <router-link to="#load-balancer-setup" @click.native="this.scrollFix('#load-balancer-setup')">here</router-link> for more information on this example.</p>

<p>Federation is only available when using Coherence Grid Edition (GE) 12.2.1.4.X and above, and is not available in the open-source
Coherence Community Edition (CE).</p>

<div class="admonition note">
<p class="admonition-inline">As Coherence Grid Edition JAR&#8217;s is not available in Maven central, to build and run
this example you, must first install the Coherence JAR into your Maven Repository from your
local Grid Edition Install. See <router-link to="#installing-coherence" @click.native="this.scrollFix('#installing-coherence')">here</router-link> for instructions on how to complete this.</p>
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
<p><router-link to="#installing-coherence" @click.native="this.scrollFix('#installing-coherence')">Installing Coherence</router-link></p>

</li>
<li>
<p><router-link to="#cache-config" @click.native="this.scrollFix('#cache-config')">Review the Federation Configuration</router-link></p>

</li>
<li>
<p><router-link to="#run-the-example" @click.native="this.scrollFix('#run-the-example')">Start Cache servers and CohQL</router-link></p>

</li>
<li>
<p><router-link to="#run-the-example-2" @click.native="this.scrollFix('#run-the-example-2')">Run the Example</router-link></p>

</li>
<li>
<p><router-link to="#load-balancer-setup" @click.native="this.scrollFix('#load-balancer-setup')">Using Federation with a Load Balancer</router-link></p>

</li>
<li>
<p><router-link to="#mutliple-services" @click.native="this.scrollFix('#mutliple-services')">Federating Multiple Services</router-link></p>

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
Start one or more cache servers for <code>ClusterA</code>

</li>
<li>
Start one or more cache servers for <code>ClusterB</code>

</li>
<li>
Start a <code>CohQL</code> session for <code>ClusterA</code>

</li>
<li>
Start a <code>CohQL</code> session for <code>ClusterB</code>

</li>
<li>
Carry out various data operations on each cluster and observe the data being replicated

</li>
</ol>
<p>Rather than running using Maven, we will start individual cache servers using the command line so you can
get a better idea of how federation works.</p>

<p>We will start the clusters in this example using the following ports.</p>

<ol style="margin-left: 15px;">
<li>
<code>ClusterA</code> on 127.0.0.1:7574

</li>
<li>
<code>ClusterB</code> on 127.0.0.1:7575

</li>
</ol>
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

<h4 id="installing-coherence">Installing Coherence</h4>
<div class="section">
<p><strong>Important</strong></p>

<p>Because Coherence Federation is only available in Grid Edition, you must carry out the following changes to the project before building and running:</p>

<ol style="margin-left: 15px;">
<li>
Download the Coherence Grid Edition Release from <a id="" title="" target="_blank" href="https://www.oracle.com/au/middleware/technologies/coherence-downloads.html">the Oracle website</a>

</li>
<li>
Install Coherence locally using the instructions in the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/install/installing-oracle-coherence-java.html">Coherence Documentation</a>

</li>
<li>
Add Coherence Grid Edition into your local Maven repository by running the following:
<p>This example assumes you have Coherence 14.1.2. Please adjust for your Coherence version and set the <code>COHERENCE_HOME</code> environment variable to the <code>coherence</code> directory of your install.</p>

<p>Linux/ MacOS</p>

<markup
lang="bas"

>mvn install:install-file -Dfile=$COHERENCE_HOME/lib/coherence.jar \
    -DpomFile=$COHERENCE_HOME/plugins/maven/com/oracle/coherence/coherence/14.1.2/coherence.14.1.2.pom</markup>

<p>Windows</p>

<markup
lang="bas"

>mvn install:install-file -Dfile=%COHERENCE_HOME%\lib\coherence.jar ^
    -DpomFile=%COHERENCE_HOME%\plugins\maven\com\oracle\coherence\coherence\14.1.2\coherence.14.1.2.pom</markup>

</li>
</ol>
</div>

<h4 id="cache-config">Review the Federation Configuration</h4>
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
      &lt;name&gt;ClusterA&lt;/name&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address&gt;127.0.0.1&lt;/address&gt;
          &lt;port system-property="test.primary.cluster.port"&gt;7574&lt;/port&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/participant&gt;
    &lt;participant&gt;  <span class="conum" data-value="2" />
      &lt;name&gt;ClusterB&lt;/name&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address&gt;127.0.0.1&lt;/address&gt;
          &lt;port system-property="test.secondary.cluster.port"&gt;7575&lt;/port&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/participant&gt;
  &lt;/participants&gt;
  &lt;topology-definitions&gt;  <span class="conum" data-value="3" />
    &lt;active-active&gt;
      &lt;name&gt;MyTopology&lt;/name&gt;
      &lt;active&gt;ClusterA&lt;/active&gt;
      &lt;active&gt;ClusterB&lt;/active&gt;
    &lt;/active-active&gt;
  &lt;/topology-definitions&gt;
&lt;/federation-config&gt;</markup>

<ul class="colist">
<li data-value="1"><code>ClusterA</code> participant with its host and port for the cluster Name Service - 127.0.0.1:7574</li>
<li data-value="2"><code>ClusterB</code> participant with its host and port for the cluster Name Service - 127.0.0.1:7575</li>
<li data-value="3">Topology that defines an <code>active-active</code> configuration between clusters. This is the default and not strictly required.</li>
</ul>
</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">System properties are for the guides internal integration tests and can be ignored.</p>
</div>
</div>
</div>

<h3 id="run-the-example">Start Cache servers and CohQL</h3>
<div class="section">
<div class="admonition note">
<p class="admonition-inline">As mentioned previously, this example is not run via Maven or Gradle, but via running the <code>java</code> via the command line
so that you can see exactly how federation works.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Set the following environment variables in each terminal or command window you open, and ensure you change to the <code>src/main/resources</code> directory.
<p>Linux/MacOS</p>

<markup
lang="bash"

>export COHERENCE_VERSION=14.1.2-0-0
export COH_JAR=~/.m2/repository/com/oracle/coherence/coherence/$COHERENCE_VERSION/coherence-$COHERENCE_VERSION.jar</markup>

<p>Windows</p>

<markup
lang="command"

>set COHERENCE_VERSION=14.1.2-0-0
set COH_JAR=%USERPROFILE%\.m2\repository\com\oracle\coherence\coherence\%COHERENCE_VERSION%\coherence-%COHERENCE_VERSION%.jar</markup>

</li>
<li>
Start a Coherence server for <code>ClusterA</code> in a separate terminal
<p>Linux/MacOS</p>

<markup
lang="bash"

>java -cp $COH_JAR:. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7574 -Dcoherence.cluster=ClusterA  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml com.tangosol.net.Coherence</markup>

<p>Windows</p>

<markup
lang="bash"

>java -cp %COH_JAR%;. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7574 -Dcoherence.cluster=ClusterA  ^
    -Dcoherence.override=tangosol-coherence-override.xml ^
    -Dcoherence.cacheconfig=federation-cache-config.xml com.tangosol.net.Coherence</markup>

<p>Explanation of system properties:</p>

<ul class="ulist">
<li>
<p><code>-Dcoherence.wka=127.0.0.1</code> - Uses the loopback adapter for the cluster, only for development</p>

</li>
<li>
<p><code>-Dcoherence.clusterport=7574</code> - Defines the coherence cluster port</p>

</li>
<li>
<p><code>-Dcoherence.cluster=ClusterA</code> - Defines the cluster name</p>

</li>
<li>
<p><code>-Dcoherence.override</code> - override file to define the participants</p>

</li>
<li>
<p><code>-Dcoherence.cacheconfig=federation-cache-config.xml</code> - cache configuration</p>

</li>
</ul>
</li>
<li>
Start a Coherence server for <code>ClusterB</code> in a separate terminal
<p>Linux/MacOS</p>

<markup
lang="bash"

>java -cp $COH_JAR:. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7575 -Dcoherence.cluster=ClusterB  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml com.tangosol.net.Coherence</markup>

<p>Windows</p>

<markup
lang="bash"

>java -cp %COH_JAR%;. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7575 -Dcoherence.cluster=ClusterB  ^
    -Dcoherence.override=tangosol-coherence-override.xml ^
    -Dcoherence.cacheconfig=federation-cache-config.xml com.tangosol.net.Coherence</markup>

</li>
<li>
Start a CohQL session for <code>ClusterA</code> in a separate terminal
<p>Linux/MacOS</p>

<markup
lang="bash"

>java -cp $COH_JAR:. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7574 -Dcoherence.cluster=ClusterA  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml \
    -Dcoherence.distributed.localstorage=false com.tangosol.coherence.dslquery.QueryPlus</markup>

<p>Windows</p>

<markup
lang="bash"

>java -cp %COH_JAR%;. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7574 -Dcoherence.cluster=ClusterA  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml \
    -Dcoherence.distributed.localstorage=false com.tangosol.coherence.dslquery.QueryPlus</markup>

</li>
<li>
Start a CohQL session for <code>ClusterB</code> in a separate terminal
<p>Linux/MacOS</p>

<markup
lang="bash"

>java -cp $COH_JAR:. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7575 -Dcoherence.cluster=ClusterB  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml \
    -Dcoherence.distributed.localstorage=false com.tangosol.coherence.dslquery.QueryPlus</markup>

<p>Windows</p>

<markup
lang="bash"

>java -cp %COH_JAR%;. -Dcoherence.wka=127.0.0.1 -Dcoherence.clusterport=7575 -Dcoherence.cluster=ClusterB  \
    -Dcoherence.override=tangosol-coherence-override.xml \
    -Dcoherence.cacheconfig=federation-cache-config.xml \
    -Dcoherence.distributed.localstorage=false com.tangosol.coherence.dslquery.QueryPlus</markup>

</li>
</ol>
</div>

<h3 id="run-the-example-2">Run the Example</h3>
<div class="section">
<ol style="margin-left: 15px;">
<li>
In each of the <code>CohQL</code> sessions, run the following command to verify the caches are empty in each cluster:
<markup
lang="bash"

>select count() from 'test'</markup>

<markup
lang="bash"
title="Output"
>0</markup>

</li>
<li>
In the first (ClusterA) <code>CohQL</code> session, add an entries to the cache <code>test</code>:
<markup
lang="bash"

>insert into 'test' key(1) value('Tim')</markup>

<markup
lang="bash"

>insert into 'test' key(2) value('John')</markup>

<markup
lang="bash"

>select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Results
[1, "Tim"]
[2, "John"]</markup>

<div class="admonition note">
<p class="admonition-inline">After the data has been inserted, it will be asynchronously queued for replication to cluster <code>ClusterB</code>. This is done automatically
by Federation and no intervention is required by the developer or user. It will be sent almost immediately if the destination cluster is available.
If there are many updates to send, they will be queued in order and sent efficiently as batches if possible.</p>
</div>
</li>
<li>
In the second (ClusterB) <code>CohQL</code> session, verify the entries were sent from the ClusterA and then update the name to <code>Timothy</code> for key(1). As the clusters are <code>active-active</code>, the changes will be sent back to the primary cluster.
<markup
lang="bash"

>CohQL&gt; select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Results
[1, "Tim"]
[2, "John"]</markup>

<markup
lang="bash"

>update 'test' set value() = "Timothy" where key() = 1</markup>

<markup
lang="bash"
title="Output"
>Results
1: true</markup>

<markup
lang="bash"

>select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Results
[1, "Timothy"]
[2, "John"]</markup>

</li>
<li>
In the first (ClusterA) <code>CohQL</code> session, verify the entry was changed via the change in the <code>ClusterB</code>, then delete the entry and confirm it was deleted in the <code>ClusterB</code>
<markup
lang="bash"

>select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Results
[1, "Timothy"]
[2, "John"]</markup>

<markup
lang="bash"

>delete from 'test' where key() = 1</markup>

<markup
lang="bash"
title="Output"
>Results</markup>

<markup
lang="bash"

>select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Result
[2, "John"]</markup>

</li>
<li>
In the second (ClusterB) <code>CohQL</code> session verify the entry has been deleted
<markup
lang="bash"

>select key(), value() from 'test'</markup>

<markup
lang="bash"
title="Output"
>Result
[2, "John"]</markup>

</li>
<li>
Continue experimenting
<p><strong>Add More Data</strong></p>

<p>You can continue to experiment by inserting, updating or removing data using various <code>CohQL</code> commands.</p>

<p>For detailed information on how to use CohQL, please visit the chapter
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-coherence-query-language.html#GUID-C0D082B1-FA62-4899-A043-4345156E6641">Using Coherence Query Language</a>
in the Coherence reference guide.</p>

<p><strong>Start a second cache server on either cluster</strong></p>

<p>Use the commands above to start a second cache server on either of the clusters.</p>

</li>
<li>
Monitor Federation
<p>If you want to monitor Federation you do this via the Coherence VisualVM Plugin.</p>

<p>See <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-visualvm#install">here</a>
for how to install the Plugin if you have <code>VisualVM</code> already, otherwise visit <a id="" title="" target="_blank" href="https://visualvm.github.io/">https://visualvm.github.io/</a> to download
and install <code>VisualVM</code>.</p>

<p>Once you have installed the plugin, you can click on one of the <code>Coherence</code> process, and you will see the <code>Federation</code> tab as shown below:</p>



<v-card>
<v-card-text class="overflow-y-hidden" >
<img src="docs/images/visualvm-federation.png" alt="visualvm federation"width="80%" />
</v-card-text>
</v-card>

<p>There are other options outlined below for monitoring Federation:</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://github.com/oracle/coherence-cli">Coherence CLI</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.coherence.community/coherence-operator/docs/latest/docs/metrics/010_overview">Grafana Dashboards from the Coherence Operator</a></p>

</li>
</ul>
</li>
</ol>
</div>

<h3 id="load-balancer-setup">Using Federation with a Load Balancer</h3>
<div class="section">
<p>In this example we have used a simplified Federation setup using the Name Service to demonstrate the capabilities. In most cases
you will need to send data to another data centre via either a load balancer and/or through a firewall.</p>

<p>This requires a couple of changes to the setup we have configured for this example.</p>

<ol style="margin-left: 15px;">
<li>
A load balancer on either side needs to be configured on a specified host and chosen cluster port, e.g. 40000 in our case.

</li>
<li>
The participant addresses needs to be set to this load balancer IP and port

</li>
<li>
The cache configuration must be updated to a specific listen port for federation

</li>
</ol>
<p>This setup is shown below:</p>



<v-card>
<v-card-text class="overflow-y-hidden" >
<img src="docs/images/federation-lbr.png" alt="federation lbr"width="80%" />
</v-card-text>
</v-card>

<p>See below for the changes required, excluding the load balancer setup.</p>

<ol style="margin-left: 15px;">
<li>
Create a load balancer on each site to load balance across the federation port 40000 on all back-end storage-enabled members for that cluster.

</li>
<li>
Update the <code>tangosol-coherence-override.xml</code>, and add each of the cluster&#8217;s respective load balancer IP address and a port you are going use for federation. We have chosen 40000, in this example.
<p>We have updated each of the <code>&lt;paticipant&gt;</code> entries for the clusters below.</p>

<markup
lang="xml"

>&lt;participants&gt;
  &lt;participant&gt;
    &lt;name&gt;ClusterA&lt;/name&gt;
    &lt;remote-addresses&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterA-load-balancer-ip&lt;/address&gt;  <span class="conum" data-value="1" />
        &lt;port&gt;40000&lt;/port&gt;  <span class="conum" data-value="2" />
      &lt;/socket-address&gt;
    &lt;/remote-addresses&gt;
  &lt;/participant&gt;
  &lt;participant&gt;
    &lt;name&gt;ClusterB&lt;/name&gt;
    &lt;remote-addresses&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterB-load-balancer-ip&lt;/address&gt;  <span class="conum" data-value="3" />
        &lt;port&gt;40000&lt;/port&gt;  <span class="conum" data-value="4" />
      &lt;/socket-address&gt;
    &lt;/remote-addresses&gt;
  &lt;/participant&gt;
&lt;/participants&gt;</markup>

<ul class="colist">
<li data-value="1">ClusterA load balancer IP</li>
<li data-value="2">ClusterA port, 40000</li>
<li data-value="3">ClusterB load balancer IP</li>
<li data-value="4">ClusterB port, 40000</li>
</ul>
</li>
<li>
Update the <code>federated-cache-config.xml</code> and add the <code>&lt;address-provider&gt;</code> element in the <code>&lt;federated-scheme&gt;</code> to specify a port for the member to listen on for federation.
<markup
lang="xml"

>&lt;federated-scheme&gt;
  &lt;scheme-name&gt;federated&lt;/scheme-name&gt;
  &lt;service-name&gt;FederatedPartitionedCache&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;address-provider&gt;
    &lt;local-address&gt;
      &lt;address/&gt; <span class="conum" data-value="1" />
      &lt;port&gt;40000&lt;/port&gt;  <span class="conum" data-value="2" />
    &lt;/local-address&gt;
  &lt;/address-provider&gt;
  &lt;topologies&gt;
    &lt;topology&gt;
      &lt;name&gt;MyTopology&lt;/name&gt;
    &lt;/topology&gt;
  &lt;/topologies&gt;
&lt;/federated-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">Optional local address to listen on, defaults to 0.0.0.0 or all addresses if not specified</li>
<li data-value="2">Local port to listen on. This is the port that will be redirected to by the load balancer.</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">This will ensure that the member starts federation on a specify port, instead of using and ephemeral port. This
fixed port can then be load balanced to by the load balancer.</p>
</div>
</li>
</ol>
</div>

<h3 id="mutliple-services">Federating Multiple Services</h3>
<div class="section">
<p>If you are using the Name Service method, then there are no changes required, but if you are using a load balancer, then you will need to do the following:</p>

<ol style="margin-left: 15px;">
<li>
Add a second port on your load balancers, e.g. 40001, which will across the federation port 40001 on all back-end storage-enabled members for that cluster.

</li>
<li>
Update the <code>tangosol-coherence-override.xml</code>, and add each of the cluster&#8217;s respective load balancer IP address <strong>and each port</strong> you are going use for federation. We have chosen 40000, in this example.
<p>We have updated each of the <code>&lt;paticipant&gt;</code> entries for the clusters below.</p>

<markup
lang="xml"

>&lt;participants&gt;
  &lt;participant&gt;
    &lt;name&gt;ClusterA&lt;/name&gt;
    &lt;remote-addresses&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterA-load-balancer-ip&lt;/address&gt;  <span class="conum" data-value="1" />
        &lt;port&gt;40000&lt;/port&gt;  <span class="conum" data-value="2" />
      &lt;/socket-address&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterA-load-balancer-ip&lt;/address&gt;  <span class="conum" data-value="3" />
        &lt;port&gt;40001&lt;/port&gt;  <span class="conum" data-value="4" />
      &lt;/socket-address&gt;
    &lt;/remote-addresses&gt;
  &lt;/participant&gt;
  &lt;participant&gt;
    &lt;name&gt;ClusterB&lt;/name&gt;
    &lt;remote-addresses&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterB-load-balancer-ip&lt;/address&gt;
        &lt;port&gt;40000&lt;/port&gt;
      &lt;/socket-address&gt;
      &lt;socket-address&gt;
        &lt;address&gt;ClusterB-load-balancer-ip&lt;/address&gt;
        &lt;port&gt;40001&lt;/port&gt;
      &lt;/socket-address&gt;
    &lt;/remote-addresses&gt;
  &lt;/participant&gt;
&lt;/participants&gt;</markup>

<ul class="colist">
<li data-value="1">ClusterA load balancer IP</li>
<li data-value="2">ClusterA port, 40000 (first service)</li>
<li data-value="3">ClusterA load balancer IP</li>
<li data-value="4">ClusterA port, 40001 (second service)</li>
</ul>
</li>
<li>
Update the <code>federated-cache-config.xml</code> and add an additional <code>&lt;address-provider&gt;</code> element in the <code>&lt;federated-scheme&gt;</code> to specify a port for the member to listen on for the second service.
<markup
lang="xml"

>&lt;federated-scheme&gt;
  &lt;scheme-name&gt;federated&lt;/scheme-name&gt;
  &lt;service-name&gt;FederatedPartitionedCache&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;address-provider&gt;
    &lt;local-address&gt;
      &lt;address/&gt; <span class="conum" data-value="1" />
      &lt;port&gt;40000&lt;/port&gt;  <span class="conum" data-value="2" />
    &lt;/local-address&gt;
  &lt;/address-provider&gt;
  &lt;topologies&gt;
    &lt;topology&gt;
      &lt;name&gt;MyTopology&lt;/name&gt;
    &lt;/topology&gt;
  &lt;/topologies&gt;
&lt;/federated-scheme&gt;

&lt;federated-scheme&gt;
  &lt;scheme-name&gt;federated2&lt;/scheme-name&gt;
  &lt;service-name&gt;FederatedPartitionedCache2&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;address-provider&gt;
    &lt;local-address&gt;
      &lt;address/&gt; <span class="conum" data-value="3" />
      &lt;port&gt;40001&lt;/port&gt;  <span class="conum" data-value="4" />
    &lt;/local-address&gt;
  &lt;/address-provider&gt;
  &lt;topologies&gt;
    &lt;topology&gt;
      &lt;name&gt;MyTopology&lt;/name&gt;
    &lt;/topology&gt;
  &lt;/topologies&gt;
&lt;/federated-scheme&gt;</markup>

<ul class="colist">
<li data-value="1">Optional local address to listen on, defaults to 0.0.0.0 or all addresses if not specified</li>
<li data-value="2">Local port to listen on for <code>FederatedPartitionedCache</code> service . This is the port that will be redirected to by the load balancer.</li>
<li data-value="3">Optional local address to listen on, defaults to 0.0.0.0 or all addresses if not specified</li>
<li data-value="4">Local port to listen on for <code>FederatedPartitionedCache2</code> service . This is the port that will be redirected to by the load balancer.</li>
</ul>
</li>
</ol>
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
<li>
<p><a id="" title="" target="_blank" href="https://docs.coherence.community/coherence-operator/docs/latest/examples/100_federation/README">Detailed Federation example using the Coherence Operator on Oracle&#8217;s Cloud Infrastructure (OCI)</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
