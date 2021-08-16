<doc-view>

<h2 id="_monitoring_statusha">Monitoring StatusHA</h2>
<div class="section">
<p>This guide walks you through how to monitor the High Available (HA) Status or <code>StatusHA</code>
value for Coherence Partitioned Services within a cluster.</p>

<p><code>StatusHA</code> is most commonly used to ensure services are in a
safe state between restarting cache servers during a rolling restart.</p>

<p>See the Coherence documentation on
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/starting-and-stopping-cluster-members.html">Starting and Stopping Cluster Members</a>
for more information on rolling redeploys.</p>


<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-will-need" @click.native="this.scrollFix('#what-you-will-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#building" @click.native="this.scrollFix('#building')">Building the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#example-classes-1" @click.native="this.scrollFix('#example-classes-1')">Review the Classes</router-link></p>

</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Example</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#run-example-usage" @click.native="this.scrollFix('#run-example-usage')">Show Usage</router-link></p>

</li>
<li>
<p><router-link to="#run-example-start-cache-servers" @click.native="this.scrollFix('#run-example-start-cache-servers')">Start 4 cache servers</router-link></p>

</li>
<li>
<p><router-link to="#run-example-start" @click.native="this.scrollFix('#run-example-start')">Start example</router-link></p>

</li>
<li>
<p><router-link to="#run-example-kill" @click.native="this.scrollFix('#run-example-kill')">Kill a cache server</router-link></p>

</li>
<li>
<p><router-link to="#run-example-kill-more" @click.native="this.scrollFix('#run-example-kill-more')">Kill more cache servers</router-link></p>

</li>
<li>
<p><router-link to="#run-example-other" @click.native="this.scrollFix('#run-example-other')">Experiment with other connection options</router-link></p>

</li>
<li>
<p><router-link to="#run-example-package" @click.native="this.scrollFix('#run-example-package')">Packaging the example</router-link></p>

</li>
</ul>
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
<p>In this example you will build and run a utility allowing you to monitor <code>StatusHA</code> values for Coherence services.</p>

<p>At its core, this example uses the <code>ServiceMBean</code> as described
in the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/manage/oracle-coherence-mbeans-reference.html">Coherence documentation</a>.
In particular the following attributes are queried:</p>

<ul class="ulist">
<li>
<p>StatusHA - The High Availability (HA) status for this service. A value of MACHINE-SAFE indicates that all the cluster members running on any given computer could be stopped without data loss. A value of NODE-SAFE indicates that a cluster member could be stopped without data loss. A value of ENDANGERED indicates that abnormal termination of any cluster member that runs this service may cause data loss. A value of N/A indicates that the service has no high availability impact.</p>

</li>
<li>
<p>StorageEnabledCount - Specifies the total number of cluster members running this service for which local storage is enabled</p>

</li>
<li>
<p>PartitionsUnbalanced - The total number of primary and backup partitions that remain to be transferred until the partition distribution across the storage enabled service members is fully balanced</p>

</li>
<li>
<p>PartitionsVulnerable - The total number of partitions that are backed up on the same machine where the primary partition owner resides</p>

</li>
<li>
<p>PartitionsEndangered - The total number of partitions that are not currently backed up</p>

</li>
<li>
<p>PartitionsAll - The total number of partitions that every cache storage is divided into</p>

</li>
</ul>
<p>The utility can connect to a Coherence cluster and query the MBeans using the following methods:</p>

<ul class="ulist">
<li>
<p>Use MBeanServer from a cluster. Requires correct cluster config to join cluster. (Default)</p>

</li>
<li>
<p>Use JMX URL to connect to a cluster</p>

</li>
<li>
<p>Use a host and port to connect to a remote JMX process</p>

</li>
<li>
<p>Use Management over REST to connect to a cluster via HTTP</p>

</li>
</ul>
<p><strong>Continue on to review the example code or go directly <router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">here</router-link> to run the example.</strong></p>


<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 20 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 1.8</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.5+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
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
</div>

<h4 id="building">Building the Example Code</h4>
<div class="section">
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

<div class="admonition note">
<p class="admonition-inline">This example can be built via Maven only. It is not supported to be run via Gradle.</p>
</div>
<div class="admonition note">
<p class="admonition-inline">You can include the <code>-DskipTests</code> for Maven or <code>-x test</code> for Gradle, to skip the tests for now.</p>
</div>
</div>
</div>

<h3 id="example-classes-1">Review the Classes</h3>
<div class="section">
<p>The example consists of the following main classes:</p>

<ul class="ulist">
<li>
<p><code>StatusHAWatcher</code> - Main entrypoint to parse arguments and run the example</p>

</li>
<li>
<p><code>ServiceData</code> - Data structure to hold the service information queried from the cluster</p>

</li>
<li>
<p><code>DataFetcher</code> - Interface implemented by various classes to retrieve <code>ServiceMBean</code> details</p>

</li>
<li>
<p><code>MBeanServerProxyDataFetcher</code> - Implementation to retrieve data from <code>MBeanServerProxy</code></p>

</li>
<li>
<p><code>JMXDataFetcher</code> - Implementation to retrieve data from a remote or local JMX connection</p>

</li>
<li>
<p><code>HTTPDataFetcher</code> - Implementation to retrieve data from a Management over REST connection from either standalone cluster or WebLogic Server</p>
<ol style="margin-left: 15px;">
<li>
Review the <code>ServiceData</code> class
<p>This class contains the following fields:</p>

<markup
lang="java"

>/**
 * The service name.
 */
private final String serviceName;

/**
 * The number of storage-enabled members for this service.
 */
private final int storageCount;

/**
 * The StatusHA value for the service.
 */
private final String statusHA;

/**
 * The total number of partitions in this service.
 */
private final int partitionCount;

/**
 * The number of partitions that are vulnerable, e.g. backed up on the same machine.
 */
private final int partitionsVulnerable;

/**
 * The number of partitions yet to be balanced.
 */
private final int partitionsUnbalanced;

/**
 * The number of partitions that do not have a backup.
 */
private final int partitionsEndangered;</markup>

</li>
<li>
Review the <code>DataFetcher</code> interface
<p>This interface defines the following methods which are used to retrieve <code>ServiceMBean</code> attributes via different methods</p>

<markup
lang="java"

>public interface DataFetcher {
    /**
     * Returns the cluster name.
     *
     * @return the cluster name
     */
    String getClusterName();

    /**
     * Returns the cluster version.
     *
     * @return the cluster version.
     */
    String getClusterVersion();

    /**
     * Returns the {@link ServiceData}.
     *
     * @return the {@link ServiceData}
     */
    Set&lt;ServiceData&gt; getStatusHaData();

    /**
     * Returns the {@link Set} of service names that are partitioned services.
     *
     * @return the {@link Set} of service names that are partitioned services
     */
    Set&lt;String&gt; getServiceNames();
}</markup>

</li>
<li>
Review the <code>MBeanServerProxyDataFetcher</code> class
<p>This class is an implementation of the <code>DataFetcher</code> interface to retrieve data from <code>MBeanServerProxy</code>.</p>

<p><strong>Constructor</strong></p>

<markup
lang="java"

>public MBeanServerProxyDataFetcher(String serviceName) {
    super(serviceName);

    // be as quiet as we can
    System.setProperty("coherence.log.level", "1");
    Registry registry = CacheFactory.ensureCluster().getManagement(); <span class="conum" data-value="1" />
    if (registry == null) {
        throw new RuntimeException("Unable to get registry from cluster");
    }

    proxy = registry.getMBeanServerProxy(); <span class="conum" data-value="2" />

    if (proxy == null) {
        throw new RuntimeException("Unable to get MBeanServerProxy");
    }</markup>

<ul class="colist">
<li data-value="1">Join the cluster and retrieve <code>Management</code>. The correct cluster operational override must be supplied to connect to the cluster.</li>
<li data-value="2">Retrieve the <code>MBeanServerProxy</code> instance</li>
</ul>
<p><strong>getStatusHaData method</strong></p>

<markup
lang="java"

>@Override
public Set&lt;ServiceData&gt; getStatusHaData() {
    Set&lt;ServiceData&gt; setData = new HashSet&lt;&gt;();
    getMBeans().forEach(bean -&gt; {  <span class="conum" data-value="1" />
        String sServiceName = extractService(bean);

        // retrieve values from one node as all of them will have the same values
        Optional&lt;String&gt; serviceMBean =
                proxy.queryNames(COHERENCE + Registry.SERVICE_TYPE + ",name=" + sServiceName + ",*", null)
                     .stream().findAny(); <span class="conum" data-value="2" />

        if (!serviceMBean.isPresent()) {
            throw new RuntimeException("Unable to find ServiceMBean for service " + sServiceName);
        }

        String sServiceMbean = serviceMBean.get();
        Map&lt;String, Object&gt; mapServiceAttr = proxy.getAttributes(sServiceMbean, Filters.always()); <span class="conum" data-value="3" />

        String sStatusHA = getSafeStatusHA((String) mapServiceAttr.get(ATTR_STATUS_HA));
        int nPartitionCount = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITION_COUNT).toString());
        int nStorageCount = Integer.parseInt(mapServiceAttr.get(ATTR_STORAGE_ENABLED_COUNT).toString());
        int nVulnerable = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_VULNERABLE).toString());
        int nUnbalanced = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_UNBALANCED).toString());
        int nEndangered = Integer.parseInt(mapServiceAttr.get(ATTR_PARTITIONS_ENDANGERED).toString());

        setData.add(new ServiceData(sServiceName, nStorageCount, sStatusHA, nPartitionCount,
                nVulnerable, nUnbalanced, nEndangered)); <span class="conum" data-value="4" />
    });
    return setData;
}</markup>

<ul class="colist">
<li data-value="1">Call <code>getMBeans()</code> to query the distribution coordinator MBean to retrieve all partitioned services</li>
<li data-value="2">Use <code>MBeanServerProxy</code> to get any <code>ServiceMBean</code> for the given service</li>
<li data-value="3">Retrieve all attributes from the <code>ServiceMBean</code></li>
<li data-value="4">Add a new <code>ServiceData</code> instance to the set to return</li>
</ul>
</li>
<li>
Review the <code>JMXDataFetcher</code> class
<p>This class is an implementation of the <code>DataFetcher</code> interface to retrieve data from a JMX connection.</p>

<p><strong>Constructor</strong></p>

<markup
lang="java"

>public JMXDataFetcher(String jmxConnectionURL, String serviceName) {
    super(serviceName);

    try {
        JMXConnector connect = JMXConnectorFactory.connect(new JMXServiceURL(jmxConnectionURL));  <span class="conum" data-value="1" />
        mbs = connect.getMBeanServerConnection();  <span class="conum" data-value="2" />
    } catch (Exception e) {
        throw new RuntimeException("Unable to connect to JMX Url " + jmxConnectionURL, e);
    }</markup>

<ul class="colist">
<li data-value="1">Retrieve an <code>JMXConnector</code> from the given JMX URL</li>
<li data-value="2">Retrieve an <code>MBeanServerConnection</code> from the <code>JMXConnector</code></li>
</ul>
<p><strong>getStatusHaData method</strong></p>

<markup
lang="java"

>@Override
public Set&lt;ServiceData&gt; getStatusHaData() {
    Set&lt;ServiceData&gt; setData = new HashSet&lt;&gt;();
    getMBeans().forEach(bean -&gt; {
        String serviceName = extractService(bean);
        AttributeList listServiceAttrs;

        String sQuery = COHERENCE + Registry.SERVICE_TYPE + ",name=" + serviceName + ",*";
        try {
            Set&lt;ObjectName&gt; setServices = mbs.queryNames(new ObjectName(sQuery), null); <span class="conum" data-value="1" />
            if (setServices.size() == 0) {
                throw new RuntimeException("Cannot query for service " + serviceName);
            }
            String mbean = setServices.stream().findAny().map(ObjectName::toString).get();

            listServiceAttrs = mbs.getAttributes(new ObjectName(mbean), new String[]{ <span class="conum" data-value="2" />
                    ATTR_PARTITIONS_VULNERABLE, ATTR_PARTITIONS_UNBALANCED, ATTR_PARTITIONS_ENDANGERED,
                    ATTR_STATUS_HA, ATTR_STORAGE_ENABLED_COUNT, ATTR_PARTITION_COUNT
            });
        } catch (Exception e) {
            throw new RuntimeException("Unable to find attributes for " + sQuery, e);
        }

        int partitionCount = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITION_COUNT));
        int storageCount = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_STORAGE_ENABLED_COUNT));
        String statusHA = getSafeStatusHA(getAttributeValue(listServiceAttrs, ATTR_STATUS_HA));
        int vulnerable = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_VULNERABLE));
        int unbalanced = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_UNBALANCED));
        int endangered = Integer.parseInt(getAttributeValue(listServiceAttrs, ATTR_PARTITIONS_ENDANGERED));

        setData.add(new ServiceData(serviceName, storageCount, statusHA, partitionCount,
                vulnerable, unbalanced, endangered));

    });
    return setData;
}</markup>

<ul class="colist">
<li data-value="1">Query the <code>MBeanServerConnection</code> to get any <code>ServiceMBean</code> for the given service</li>
<li data-value="2">Retrieve all required attributes</li>
</ul>
</li>
<li>
Review the <code>HTTPDataFetcher</code> class
<p>This class is an implementation of the <code>DataFetcher</code> interface to retrieve data from a Management over REST connection.</p>

<p><strong>Constructor</strong></p>

<markup
lang="java"

>public HTTPDataFetcher(String url, String serviceName) {
    super(serviceName);
    if (url == null) {
        throw new IllegalArgumentException("Http URL must not be null");
    }

    httpUrl = url;

    // Managed Coherence Servers URL http://&lt;admin-host&gt;:&lt;admin-port&gt;/management/coherence/&lt;version&gt;/clusters
    isWebLogic = httpUrl.contains("/management/coherence/") &amp;&amp; httpUrl.contains("clusters");

    if (isWebLogic) {  <span class="conum" data-value="1" />
        System.out.println("Enter basic authentication information for WebLogic Server connection");
        System.out.print("\nEnter username: ");

        Console console = System.console();

        String username = console.readLine();
        System.out.print("Enter password. (will not be displayed): ");
        char[] password= console.readPassword();

        if (username == null || password.length == 0) {
            throw new RuntimeException("Please enter username and password");
        }

        String authentication = username + ":" + new String(password);

        byte[] encodedData = Base64.getEncoder().encode(authentication.getBytes(StandardCharsets.UTF_8));
        basicAuth = "Basic " + new String(encodedData);
    }</markup>

<ul class="colist">
<li data-value="1">If the URL is for a WebLogic Server connection, prompt for username/ password</li>
</ul>
<p><strong>getMBeans method</strong></p>

<p>This method constructs a HTTP Request to retrieve the data from Management over REST endpoint.</p>

<markup
lang="java"

>private JsonNode getMBeans(String serviceName) {
    try {
        URLBuilder builder = new URLBuilder(httpUrl);
        if (isWebLogic) {
            // WebLogic Server requires the cluster name as a path segement
            builder.addPathSegment(getClusterName());
        }

        builder.addPathSegment("services");

        if (serviceName != null) {
            builder = builder.addPathSegment(serviceName.replaceAll("\"", ""));
        }
        builder = builder
                .addPathSegment("members")
                .addQueryParameter("fields", "name,type,statusHA,partitionsAll,partitionsEndangered," +
                        "partitionsVulnerable,partitionsUnbalanced,storageEnabledCount,requestPendingCount,outgoingTransferCount")
                .addQueryParameter("links", "");
        JsonNode rootNode = getResponse(builder);

        return isWebLogic ? rootNode : rootNode.get("items");
    } catch (Exception e) {
        throw new RuntimeException("Unable to get service info from " + httpUrl, e);
    }
}</markup>

</li>
</ol>
</li>
</ul>
</div>

<h3 id="run-example-1">Run the Example</h3>
<div class="section">

<h4 id="run-example-usage">Show Usage</h4>
<div class="section">
<p>The supported way to run this example is to build using Maven as described <router-link to="#building" @click.native="this.scrollFix('#building')">here</router-link> and running using
<code>java -jar target/status-ha-{version}.jar</code>
from a terminal in the base directory of this example: <code>examples/guides/500-status-ha</code>.</p>

<p>Firstly, issue the command with the <code>-u</code> option which displays the usage.</p>

<markup
lang="bash"

>java -jar target/status-ha-{version}.jar -h

Usage: StatusHAWatcher [options]

Connection options:
 -m               Use MBeanServer from cluster. Requires correct cluster config to join cluster. (Default)
 -h url           Use Management over REST to connect to cluster
 -j url           Use JMX URL to connect to cluster
 -hp host:port    Connect to a JMX process via host:port

Other Options:
 -d delay         Delay between each check in seconds
 -s service       Service name to monitor or all services if not specified
 -u               Display usage

StatusHA meanings:
  ENDANGERED - abnormal termination of any cluster node that runs this service may cause data loss
  NODE-SAFE - any single cluster node could be stopped without data loss
  MACHINE-SAFE - all the cluster nodes running on any given machine could be stopped at once without data loss
  RACK-SAFE - all the cluster nodes running on any given rack could be stopped at once without data loss
  SITE-SAFE - all the cluster nodes running on any given rack could be stopped at once without data loss

Partition meanings:
  Endangered  - The total number of partitions that are not currently backed up
  Vulnerable  - The total number of partitions that are backed up on the same machine
                where the primary partition owner resides
  Unbalanced  - The total number of primary and backup partitions which remain to be transferred until the
                partition distribution across the storage enabled service members is fully balanced
  Remaining   - The number of partition transfers that remain to be completed before the service
                achieves the goals set by this strategy</markup>

<div class="admonition note">
<p class="admonition-inline">To test the utility we will start some cache servers.</p>
</div>
</div>

<h4 id="run-example-start-cache-servers">Start 4 cache servers</h4>
<div class="section">
<div class="admonition note">
<p class="admonition-inline">If you want to connect using the default option, MBeanServer connection, you must ensure you build the example with the
same Coherence version of the cluster you are going to connect to.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Change to the <code>`examples/guides/500-status-ha/target/libs</code> directory

</li>
<li>
Issue the following command in four terminals to start 4 DefaultCacheServer processes.
<div class="admonition note">
<p class="admonition-inline">On two of the processes use <code>-Dcoherence.machine=machine1</code> and on the other two use <code>-Dcoherence.machine=machine2</code>
to simulate processes running on separate physical servers.</p>
</div>
<markup
lang="bash"

>java -Dcoherence.machine=machine1 -jar coherence-{version}.jar</markup>

</li>
</ol>
</div>

<h4 id="run-example-start">Start the example</h4>
<div class="section">
<p>When the cache servers have started, ensure you are in the <code>examples/guides/500-status-ha</code> directory and run the following:</p>

<markup
lang="bash"

>java -jar target/status-ha-{version}.jar</markup>

<p>Notes:</p>

<ol style="margin-left: 15px;">
<li>
By default, the MBeans will be queried every 5 seconds. You can change this by using the <code>-d</code> option and specify a delay in seconds.

</li>
<li>
All services are queried. You can select only a single service to be monitored by using <code>-s</code> option and specifying a service name.

</li>
</ol>
<p>You will see output similar to the following showing the status HA values for the cluster services.</p>

<markup
lang="bash"

>Connection:   Cluster MBean Server
Service:      all
Delay:        5 seconds

Oracle Coherence Version 21.06.1
 Grid Edition: Development mode
Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.


Cluster Name: timmiddleton's cluster (21.06.1)

Press CTRL-C to quit

Date/Time                        Service Name         Storage Count  StatusHA         Partitions  Endangered  Vulnerable  Unbalanced  Status
----------------------------     ------------        --------------  ------------    ----------- ----------- ----------- -----------  -------------
Tue Aug 03 11:27:13 AWST 2021    PartitionedTopic                 4  MACHINE-SAFE            257           0           0           0  Safe
Tue Aug 03 11:27:13 AWST 2021    PartitionedCache                 4  MACHINE-SAFE            257           0           0           0  Safe

Tue Aug 03 11:27:18 AWST 2021    PartitionedTopic                 4  MACHINE-SAFE            257           0           0           0  Safe
Tue Aug 03 11:27:18 AWST 2021    PartitionedCache                 4  MACHINE-SAFE            257           0           0           0  Safe

Tue Aug 03 11:27:23 AWST 2021    PartitionedCache                 4  MACHINE-SAFE            257           0           0           0  Safe
Tue Aug 03 11:27:23 AWST 2021    PartitionedTopic                 4  MACHINE-SAFE            257           0           0           0  Safe</markup>

<div class="admonition note">
<p class="admonition-inline">You will notice that the StatusHA of all services is MACHINE-SAFE as there are an even number of cache
servers on each "machine".</p>
</div>
</div>

<h4 id="run-example-kill">Kill a cache server</h4>
<div class="section">
<p>Kill one of the cache server processes using <code>CTRL-C</code> and note the change in the output of the example:</p>

<markup
lang="bash"

>Date/Time                        Service Name         Storage Count  StatusHA         Partitions  Endangered  Vulnerable  Unbalanced  Status
----------------------------     ------------        --------------  ------------    ----------- ----------- ----------- -----------  -------------
Tue Aug 03 11:29:39 AWST 2021    PartitionedCache                 3  NODE-SAFE               257           0         257         170  170 partitions are unbalanced
Tue Aug 03 11:29:39 AWST 2021    PartitionedTopic                 3  NODE-SAFE               257           0          86           0  86 partitions are vulnerable

Tue Aug 03 11:29:45 AWST 2021    PartitionedCache                 3  NODE-SAFE               257           0          86           0  86 partitions are vulnerable
Tue Aug 03 11:29:45 AWST 2021    PartitionedTopic                 3  NODE-SAFE               257           0          86           0  86 partitions are vulnerable

Tue Aug 03 11:29:50 AWST 2021    PartitionedTopic                 3  NODE-SAFE               257           0          86           0  86 partitions are vulnerable
Tue Aug 03 11:29:50 AWST 2021    PartitionedCache                 3  NODE-SAFE               257           0          86           0  86 partitions are vulnerable</markup>

<div class="admonition note">
<p class="admonition-inline">You will notice that the StatusHA values are now node safe as there are not enough servers on each machine to
provide a higher level of safety.</p>
</div>
</div>

<h4 id="run-example-kill-more">Kill more cache servers</h4>
<div class="section">
<p>Kill all but one cache server, and you will notice the StatusHA value is ENDANGERED as there is only one cache server
with no backups available on other cache servers.</p>

<markup
lang="bash"

>Date/Time                        Service Name         Storage Count  StatusHA         Partitions  Endangered  Vulnerable  Unbalanced  Status
----------------------------     ------------        --------------  ------------    ----------- ----------- ----------- -----------  -------------
Tue Aug 03 11:33:14 AWST 2021    PartitionedTopic                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED
Tue Aug 03 11:33:14 AWST 2021    PartitionedCache                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED

Tue Aug 03 11:33:19 AWST 2021    PartitionedCache                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED
Tue Aug 03 11:33:19 AWST 2021    PartitionedTopic                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED

Tue Aug 03 11:33:24 AWST 2021    PartitionedTopic                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED
Tue Aug 03 11:33:24 AWST 2021    PartitionedCache                 1  ENDANGERED              257         257         257           0  StatusHA is ENDANGERED</markup>

<p>Start the remaining cache servers, and you will see the StatusHA return to MACHINE-SAFE.</p>

</div>

<h4 id="run-example-other">Experiment with other connection options</h4>
<div class="section">
<p>Other connection options are available which do not require you to have the same Coherence version as the example.</p>

<ol style="margin-left: 15px;">
<li>
Connect via JMX to a Host/Port
<p>If you have a Coherence MBean server running on a host/port you can connect to the cluster using the following:</p>

<markup
lang="bash"

>java -jar target/status-ha-{version}.jar -hp host:port</markup>

</li>
<li>
Connect via JMX to a JMX URL
<markup
lang="bash"

>java -jar target/status-ha-{version}.jar -j service:jmx:rmi:///jndi/rmi://localhost:8888/jmxrmi</markup>

</li>
<li>
Connect via Management over REST
<p>If you have a stand-alone Coherence cluster with Management over REST enabled, use the following:</p>

<markup
lang="bash"

>java -jar target/status-ha-{version}.jar -h http://host:management-port/management/coherence/cluster</markup>

</li>
<li>
Connect via Management over REST to WebLogic Server
<p>If you have a stand-alone Coherence cluster within WebLogic Server, use the following:</p>

<markup
lang="bash"

>java -jar target/status-ha-{version}.jar -h http://host:admin-port/management/coherence/latest/clusters</markup>

</li>
</ol>
</div>

<h4 id="run-example-package">Packaging the example</h4>
<div class="section">
<p>When the example is built, the following artifacts are created:</p>

<ul class="ulist">
<li>
<p><code>target/status-ha-{version}.jar</code> - executable jar with META-INF/MANIFEST.MF adding <code>libs</code> directory contents to classpath</p>

</li>
<li>
<p><code>target/libs/coherence-{version}.jar</code> - Coherence version the example was built with</p>

</li>
<li>
<p><code>target/libs/jackson-annotations-2.12.0.jar</code> - required dependencies</p>

</li>
<li>
<p><code>target/libs/jackson-core-2.12.0.jar</code> - required dependencies</p>

</li>
<li>
<p><code>target/libs/jackson-databind-2.12.0.jar</code> - required dependencies</p>

</li>
</ul>
<p>If you wish to take the example and run it on a separate machine, create a temporary directory and carry out the following:</p>

<div class="admonition note">
<p class="admonition-inline">We are using a temporary directory <code>/tmp/build</code> as our example. Modify as you need.</p>
</div>
<markup
lang="bash"

>cp target/status-ha-{version}.jar /tmp/build
mkdir /tmp/build/libs
cp target/libs/jackson-annotations-2.12.0.jar /tmp/build/libs
cp target/libs/jackson-core-2.12.0.jar /tmp/build/libs
cp target/libs/jackson-databind-2.12.0.jar /tmp/build/libs</markup>

<p>You can then change to the <code>/tmp/build</code> directory and run the example using:</p>

<markup
lang="bash"

>cd /tmp/build

java -jar status-ha-{version}.jar</markup>

<p>Zip or Tar the directory up and transfer to your target machine.</p>

<p>If you wish to change the Coherence version used to build the example you can set the following system properties:</p>

<ul class="ulist">
<li>
<p><code>-Dcoherence.version</code> - the coherence version</p>

</li>
<li>
<p><code>-Dcoherence.group.id</code> - defaults to <code>com.oracle.coherence.ce</code>. Change to <code>com.oracle.coherence</code> for commercial edition.</p>

</li>
</ul>
<markup
lang="bash"

>mvn clean install -DskipTests -Dcoherence.version=14.1.1-0-0 -Dcoherence.groupid=com.oracle.coherence</markup>

</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this example you built and ran a utility allowing you to monitor <code>StatusHA</code> values for Coherence services.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/manage/oracle-coherence-mbeans-reference.html">Coherence MBean Reference</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/starting-and-stopping-cluster-members.html">Starting and Stopping Cluster Members</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/manage/using-jmx-manage-oracle-coherence.html">Using JMX to Manage Oracle Coherence</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
