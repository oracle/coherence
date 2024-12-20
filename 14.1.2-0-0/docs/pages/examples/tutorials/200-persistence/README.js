<doc-view>

<h2 id="_persistence">Persistence</h2>
<div class="section">
<p>This tutorial walks through Coherence Persistence by using Coherence Query Language (CohQL)
to create, recover and manage snapshots, monitor snapshot operations via JMX MBean notifications and
work with archived snapshots.</p>

<p>Coherence Persistence is a set of tools and technologies that manage the persistence and
recovery of Coherence distributed caches. Cached data is persisted so that it can be quickly recovered after
a catastrophic failure or after a cluster restart due to planned maintenance.</p>

<p>Persistence can operate in two modes:</p>

<ul class="ulist">
<li>
<p>On-Demand persistence mode – a cache service is manually persisted and recovered upon request using the
persistence coordinator. The persistence coordinator is exposed as an MBean interface that provides
operations for creating, archiving, and recovering snapshots of a cache service.</p>

</li>
<li>
<p>Active persistence mode – In this mode, cache contents are automatically persisted on all mutations and are
automatically recovered on cluster/service startup. The persistence coordinator can still
be used in active persistence mode to perform on-demand snapshots.</p>

</li>
</ul>
<p>For more information on Coherence Persistence, please see the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/administer/persisting-caches.html">Coherence Documentation</a>.</p>


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
<p><router-link to="#cache-config" @click.native="this.scrollFix('#cache-config')">Persistence Configuration</router-link></p>

</li>
<li>
<p><router-link to="#listener" @click.native="this.scrollFix('#listener')">Listening to JMX Notifications</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#run-the-example" @click.native="this.scrollFix('#run-the-example')">Build and Run the Example</router-link></p>

</li>
<li>
<p><router-link to="#active-persistence" @click.native="this.scrollFix('#active-persistence')">Enable Active Persistence</router-link></p>

</li>
<li>
<p><router-link to="#archiver" @click.native="this.scrollFix('#archiver')">Enable a Snapshot Archiver</router-link></p>

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
<p>You will review the requirements for running both <code>on-demand</code> and <code>active</code> persistence and carry out the following:</p>

<ol style="margin-left: 15px;">
<li>
Start one or more cache servers with <code>on-demand</code> persistence

</li>
<li>
Start a <code>CohQL</code> session to insert data and create and manage snapshots

</li>
<li>
Start a JMX MBean listener to monitor Persistence operations

</li>
<li>
Change <code>on-demand</code> to <code>active</code> persistence and show this in action

</li>
<li>
Work with archiving snapshots

</li>
</ol>
</div>

<h3 id="what-you-need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 20-30 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 17</a> or later</p>

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
<p>The project is a Coherence project and imports the <code>coherence-bom</code> and <code>coherence-dependencies</code>
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
<p><router-link to="#cache-server" @click.native="this.scrollFix('#cache-server')">cache-server - Runs a DefaultCacheServer</router-link></p>

</li>
<li>
<p><router-link to="#cohql" @click.native="this.scrollFix('#cohql')">cohql - Runs a CohQL session</router-link></p>

</li>
<li>
<p><router-link to="#notifications" @click.native="this.scrollFix('#notifications')">notifications - Runs a process to subscribe to JMX Notification events</router-link></p>

</li>
</ul>
<p id="cache-server"><strong>cache-server</strong> - Runs a <code>DefaultCacheServer</code></p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;cache-server&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;cache-server&lt;/name&gt;
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
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.wka=127.0.0.1&lt;/argument&gt;
            &lt;argument&gt;-Xmx512m&lt;/argument&gt;
            &lt;argument&gt;-Xms512m&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.cacheconfig=persistence-cache-config.xml&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.distributed.persistence.mode=on-demand&lt;/argument&gt;  <span class="conum" data-value="1" />
            &lt;argument&gt;-Dcoherence.distributed.persistence.base.dir=persistence-data&lt;/argument&gt;  <span class="conum" data-value="2" />
            &lt;argument&gt;com.tangosol.net.DefaultCacheServer&lt;/argument&gt;
          &lt;/arguments&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

<ul class="colist">
<li data-value="1">Set on-demand mode, which is the default</li>
<li data-value="2">Set the base directory for all persistence directories</li>
</ul>
<p id="cohql"><strong>cohql</strong> - Runs a <code>CohQL</code> session</p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;cohql&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;cohql&lt;/name&gt;
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
              &lt;value&gt;persistence-cache-config.xml&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.distributed.persistence.base.dir&lt;/key&gt;
              &lt;value&gt;persistence-data&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.distributed.persistence.mode&lt;/key&gt;
              &lt;value&gt;on-demand&lt;/value&gt;
            &lt;/property&gt;
            &lt;property&gt;
              &lt;key&gt;coherence.wka&lt;/key&gt;
              &lt;value&gt;127.0.0.1&lt;/value&gt;
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

<p id="notifications"><strong>notifications</strong> - Runs a process to subscribe to JMX Notification events</p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;notifications&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;notifications&lt;/name&gt;
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
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.wka=127.0.0.1&lt;/argument&gt;
            &lt;argument&gt;-Xmx128m&lt;/argument&gt;
            &lt;argument&gt;-Xms128m&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.cacheconfig=persistence-cache-config.xml&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.distributed.localstorage=false&lt;/argument&gt;
            &lt;argument&gt;com.oracle.coherence.tutorials.persistence.NotificationWatcher&lt;/argument&gt;
            &lt;argument&gt;PartitionedCache&lt;/argument&gt;
          &lt;/arguments&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

</div>

<h4 id="cache-config">Persistence Configuration</h4>
<div class="section">
<p>By default, any partitioned service, including Federated services, will default to <code>on-demand</code> mode. This mode allows you
to create and manage snapshots to default directories without any setup. A <code>coherence</code> directory off the users home
directory is used to store all persistence-related data.</p>

<p>If you wish to enable <code>active</code> persistence mode you can use a system property <code>-Dcoherence.distributed.persistence.mode=active</code>
and this will use the default directories as described above.</p>

<p>In this example we are also defining the base persistence directory using a system property <code>-Dcoherence.distributed.persistence.base.dir=persistence-data</code>.
All other persistence directories will be created below this directory.</p>

<p>Please see <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/administer/persisting-caches.html#GUID-AA98D601-5CE9-4E33-BB16-487B417BA5A8">here</a> for more
details on configuring your persistence locations.</p>

<p>In this tutorial, Persistence is configured in two files:</p>

<ul class="ulist">
<li>
<p>An operational override file is used to configure non-default persistence environments and archive locations</p>

</li>
<li>
<p>A cache configuration with file the <code>&lt;persistence&gt;</code> element is used to associate services with persistence environments, if you are not using the defaults. (This is initially commented out.)</p>

</li>
</ul>
<p><strong>Cache Configuration File</strong></p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;server&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;server&lt;/scheme-name&gt;
      &lt;service-name&gt;PartitionedCache&lt;/service-name&gt;
      &lt;partition-count&gt;31&lt;/partition-count&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme&gt;
          &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
        &lt;/local-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;!-- initially commented out as we are using system properties
      &lt;persistence&gt;
        &lt;environment&gt;default-active&lt;/environment&gt;
        &lt;archiver&gt;shared-directory-archiver&lt;/archiver&gt;
      &lt;/persistence&gt; --&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<div class="admonition note">
<p class="admonition-inline">The above cache configuration has the <code>&lt;persistence&gt;</code> element commented out for the first part of this tutorial.</p>
</div>
<p><strong>Operational Override File</strong></p>

<markup
lang="xml"

>&lt;coherence xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://xmlns.oracle.com/coherence/coherence-operational-config"
           xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-operational-config coherence-operational-config.xsd"&gt;

  &lt;cluster-config&gt;
    &lt;snapshot-archivers&gt;
      &lt;directory-archiver id="shared-directory-archiver"&gt; <span class="conum" data-value="1" />
        &lt;archive-directory system-property="coherence.distributed.persistence.archive.dir"&gt;persistence-data/archives&lt;/archive-directory&gt;
      &lt;/directory-archiver&gt;
    &lt;/snapshot-archivers&gt;
  &lt;/cluster-config&gt;

  &lt;management-config&gt;
    &lt;managed-nodes system-property="coherence.management"&gt;all&lt;/managed-nodes&gt;
  &lt;/management-config&gt;
&lt;/coherence&gt;</markup>

<ul class="colist">
<li data-value="1">Defines a snapshot archiver to archive to a given directory</li>
</ul>
</div>

<h4 id="listener">Listening to JMX Notifications</h4>
<div class="section">
<p>Persistence operations generate JMX notifications. You can register for these notifications to monitor and
understand how long these operations are taking.</p>

<div class="admonition note">
<p class="admonition-inline">You can use a tool such as VisualVM with the Coherence VisualVM plugin to monitor and manage persistence.
See the VisualVM Plugin project on <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-visualvm">GitHub</a>.</p>
</div>
<p><strong>Main entry point</strong></p>

<markup
lang="java"

>public static void main(String[] args) {
    if (args.length == 0) {
        System.out.println("Please provide a list of services to listen for notifications on");
        System.exit(1);
    }

    Set&lt;String&gt; setServices = new HashSet&lt;&gt;(Arrays.asList(args));

    System.out.println("\n\nGetting MBeanServer...");

    Cluster cluster = CacheFactory.ensureCluster(); <span class="conum" data-value="1" />
    MBeanServer server = MBeanHelper.findMBeanServer();
    Registry registry = cluster.getManagement();

    if (server == null) {
        throw new RuntimeException("Unable to find MBeanServer");
    }

    try {
        for (String serviceName : setServices) {
            System.out.println("Registering listener for " + serviceName);

            String mBeanName = "Coherence:" + CachePersistenceHelper.getMBeanName(serviceName);  <span class="conum" data-value="2" />

            waitForRegistration(registry, mBeanName);  <span class="conum" data-value="3" />

            ObjectName           beanName = new ObjectName(mBeanName);
            NotificationListener listener = new PersistenceNotificationListener(serviceName);

            server.addNotificationListener(beanName, listener, null, null);  <span class="conum" data-value="4" />
            mapListeners.put(beanName, listener);
        }

        System.out.println("Waiting for notifications. Use CTRL-C to interrupt.");

        Thread.sleep(Long.MAX_VALUE);
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    finally {
        // unregister all registered notifications
        mapListeners.forEach((k, v) -&gt; {
            try {
                server.removeNotificationListener(k, v);
            }
            catch (Exception eIgnore) {
                // ignore
            }
        });
    }
}</markup>

<ul class="colist">
<li data-value="1">Join the cluster and retrieve the <code>MBeanServer</code> and <code>Registry</code></li>
<li data-value="2">Loop through the services provided as arguments and get the MBean name for the Persistence MBean</li>
<li data-value="3">Ensure the MBean is registered</li>
<li data-value="4">Add a notification listener on the Persistence MBean</li>
</ul>
<p><strong>PersistenceNotificationListener implementation</strong></p>

<markup
lang="java"

>public static class PersistenceNotificationListener
        implements NotificationListener {</markup>

<p><strong>Handle the notification</strong></p>

<markup
lang="java"

>@Override
public synchronized void handleNotification(Notification notification, Object oHandback) {
    counter.incrementAndGet();

    String userData = notification.getUserData().toString();
    String message  = notification.getMessage() + " " + notification.getUserData();    // default

    // determine if it's a begin or end notification
    String type = notification.getType();

    if (type.indexOf(BEGIN) &gt; 0) {  <span class="conum" data-value="1" />
        // handle begin notification and save the start time
        mapNotify.put(type, notification.getTimeStamp());
        message = notification.getMessage();
    }
    else if (type.indexOf(END) &gt; 0) {  <span class="conum" data-value="2" />
        // handle end notification and try and find the matching begin notification
        String begin = type.replaceAll(END, BEGIN);
        Long   start = mapNotify.get(begin);

        if (start != null) {
            message = "  " + notification.getMessage()
                      + (userData == null || userData.isEmpty() ? "" : userData) + " (Duration="
                      + (notification.getTimeStamp() - start) + "ms)";
            mapNotify.remove(begin);
        }
    }
    else {
        message = serviceName + ": " + type + "";
    }

    System.out.println(new Date(notification.getTimeStamp()) + " : " + serviceName + " (" + type + ") " + message);
}</markup>

<ul class="colist">
<li data-value="1">Store the details of the begin notification</li>
<li data-value="2">Handle the end notification and determine the operation length</li>
</ul>
</div>
</div>

<h3 id="run-the-example">Run the Example</h3>
<div class="section">
<p>Once you have built the project as described earlier in this document, you can run it via Maven or Gradle.</p>


<h4 id="_maven">Maven</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Start one or more DefaultCache servers in separate terminals.
<markup
lang="bash"

>./mvnw exec:exec -P cache-server</markup>

</li>
<li>
Start a CohQL session.
<markup
lang="bash"

>./mvnw exec:java -P cohql</markup>

</li>
<li>
Start a JMX Listener.
<markup
lang="bash"

>./mvnw exec:exec -P notifications</markup>

</li>
</ol>
</div>

<h4 id="_gradle">Gradle</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Start one or more DefaultCache servers in separate terminals.
<markup
lang="bash"

>./gradlew runCacheServer</markup>

</li>
<li>
Start a CohQL session.
<markup
lang="bash"

>./gradlew runCohql --console=plain</markup>

<p>and</p>

<markup
lang="bash"

>./gradlew runNotifications --console=plain</markup>

</li>
</ol>
</div>

<h4 id="_run_the_following_commands_to_exercise_on_demand_persistence">Run the following commands to exercise <code>on-demand</code> Persistence</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
In the <code>CohQL</code> session, run the following commands to add data:
<markup
lang="bash"

>CohQL&gt; select count() from test
Results
0

CohQL&gt;
CohQL&gt; insert into test key(1) value("one")

CohQL&gt; insert into test key(2) value("two")

CohQL&gt; insert into test key(3) value("three")

select count() from test
Results
3</markup>

</li>
<li>
Create a snapshot containing this data
<markup
lang="bash"

>CohQL&gt; list snapshots
Results
"PartitionedCache": []

CohQL&gt; create snapshot "data" "PartitionedCache"
Are you sure you want to create a snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Creating snapshot 'data' for service 'PartitionedCache'
Results
"Success"

CohQL&gt; list snapshots
Results
"PartitionedCache": ["data"]</markup>

<div class="admonition note">
<p class="admonition-inline">You should see messages similar to the following in the notifications window:</p>
</div>
<markup
lang="bash"

>Tue Apr 26 10:57:03 AWST 2022 : PartitionedCache (create.snapshot.begin) Building snapshot "data"
Tue Apr 26 10:57:06 AWST 2022 : PartitionedCache (create.snapshot.end)   Successfully created snapshot "data" (Duration=3445ms)</markup>

</li>
<li>
Clear the cache and recover the snapshot
<markup
lang="bash"

>CohQL&gt; delete from test
Results

CohQL&gt; select count() from test
Results
0

CohQL&gt; recover snapshot "data" "PartitionedCache"
Are you sure you want to recover a snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Recovering snapshot 'data' for service 'PartitionedCache'
2022-04-11 16:23:06.691/499.700 Oracle Coherence GE 14.1.1.0.0 &lt;D5&gt; (thread=DistributedCache:PartitionedCache, member=3): Service PartitionedCache has been suspended
2022-04-11 16:23:09.247/502.256 Oracle Coherence GE 14.1.1.0.0 &lt;D5&gt; (thread=DistributedCache:PartitionedCache, member=3): Service PartitionedCache has been resumed
Results
"Success"

select count() from test
Results
3</markup>

<div class="admonition note">
<p class="admonition-inline">You should see messages similar to the following in the notifications window:</p>
</div>
<markup
lang="bash"

>Tue Apr 26 10:57:48 AWST 2022 : PartitionedCache (recover.snapshot.begin) Recovering Snapshot "data"
Tue Apr 26 10:57:48 AWST 2022 : PartitionedCache (recover.begin) Recovering snapshot "data"
Tue Apr 26 10:57:49 AWST 2022 : PartitionedCache (recover.end)   Recovery Completed (Duration=623ms)
Tue Apr 26 10:57:49 AWST 2022 : PartitionedCache (recover.snapshot.end)   Successfully recovered snapshot "data" (Duration=631ms)</markup>

</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">You will be able to see the snapshots in the directory <code>persistence-data/snapshots</code>.</p>
</div>
</div>
</div>

<h3 id="active-persistence">Enable Active Persistence</h3>
<div class="section">

<h4 id="_run_the_following_commands_to_exercise_active_persistence">Run the following commands to exercise <code>active</code> Persistence</h4>
<div class="section">
<p>After shutting down all running processes, in the file <code>pom.xml</code> change
<code>on-demand</code> to <code>active</code> for the <code>cohql</code> and <code>cache-server</code> profiles to enable active persistence.</p>

<p>Run the <code>cache-server</code>, <code>cohql</code> and <code>notifications</code> as described above.</p>

<ol style="margin-left: 15px;">
<li>
In the <code>CohQL</code> session, run the following commands to add data
<markup
lang="bash"

>CohQL&gt; select count() from test
Results
0

CohQL&gt;
CohQL&gt; insert into test key(1) value("one")

CohQL&gt; insert into test key(2) value("two")

CohQL&gt; insert into test key(3) value("three")

select count() from test
Results
3</markup>

</li>
<li>
Shutdown all three processes and restart the <code>cache-server</code> and <code>cohql</code> processes, then continue below.

</li>
<li>
Re-query the <code>test</code> cache
<markup
lang="bash"

>CohQL&gt; select * from test
Results
"two"
"three"
"one"</markup>

<div class="admonition note">
<p class="admonition-inline">You can see that the cache data has automatically been recovered from disk during cluster startup.
This active persistence data is stored in the directory <code>persistence-data/active</code> below the persistence tutorial directory.</p>
</div>
</li>
</ol>
</div>
</div>

<h3 id="archiver">Enable a Snapshot Archiver</h3>
<div class="section">
<p>Snapshots can be archived to a central location and then later retrieved and restored. Archiving snapshots requires defining the directory where
archives are stored and configuring cache services to use an archive directory.</p>

<p>To enable a snapshot archiver in this example, you need to uncomment the <code>&lt;persistence&gt;</code> element in the
cache config file <code>src/main/resources/persistence-cache-config.xml</code>, and rebuild the project using
Maven or Gradle.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;server&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;server&lt;/scheme-name&gt;
      &lt;service-name&gt;PartitionedCache&lt;/service-name&gt;
      &lt;partition-count&gt;31&lt;/partition-count&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme&gt;
          &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
        &lt;/local-scheme&gt;
      &lt;/backing-map-scheme&gt;
      &lt;!-- initially commented out as we are using system properties
      &lt;persistence&gt;
        &lt;environment&gt;default-active&lt;/environment&gt;
        &lt;archiver&gt;shared-directory-archiver&lt;/archiver&gt;
      &lt;/persistence&gt; --&gt;
      &lt;autostart&gt;true&lt;/autostart&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ol style="margin-left: 15px;">
<li>
Archive the existing <code>data</code> snapshot.
<markup
lang="bash"

>CohQL&gt; archive snapshot "data" "PartitionedCache"
Are you sure you want to archive a snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Archiving snapshot 'data' for service 'PartitionedCache'
Results
"Success"</markup>

</li>
<li>
Inspect the archive directory contents to determine the cluster name
<markup
lang="bash"

>$ cd persistence-data/archives/
$ ls
timmiddleton-s-cluster</markup>

<div class="admonition note">
<p class="admonition-inline">In the above the cluster name is <code>timmiddleton&#8217;s cluster</code> but a sanitized directory of <code>timmiddleton-s-cluster</code> has been used.</p>
</div>
</li>
<li>
View the contents of the archived snapshot directory, substituting your cluster directory.
<markup
lang="bash"

>$ cd timmiddleton-s-cluster/PartitionedCache/data/
$ ls
0-1-18063c8cc4c-1	12-1-18063c8cc4c-1	16-1-18063c8cc4c-1	2-1-18063c8cc4c-1	23-1-18063c8cc4c-1	27-1-18063c8cc4c-1	30-1-18063c8cc4c-1	7-1-18063c8cc4c-1
1-1-18063c8cc4c-1	13-1-18063c8cc4c-1	17-1-18063c8cc4c-1	20-1-18063c8cc4c-1	24-1-18063c8cc4c-1	28-1-18063c8cc4c-1	4-1-18063c8cc4c-1	8-1-18063c8cc4c-1
10-1-18063c8cc4c-1	14-1-18063c8cc4c-1	18-1-18063c8cc4c-1	21-1-18063c8cc4c-1	25-1-18063c8cc4c-1	29-1-18063c8cc4c-1	5-1-18063c8cc4c-1	9-1-18063c8cc4c-1
11-1-18063c8cc4c-1	15-1-18063c8cc4c-1	19-1-18063c8cc4c-1	22-1-18063c8cc4c-1	26-1-18063c8cc4c-1	3-1-18063c8cc4c-1	6-1-18063c8cc4c-1	meta.properties</markup>

<div class="admonition note">
<p class="admonition-inline">The directory shows 31 different data files and a <code>meta.properties</code> file that contains some metadata.
These files are binary files and can only be used by recovering an archived snapshot.</p>
</div>
</li>
<li>
Validate the archived snapshot
<p>The following command will ensure that the archived snapshot can be retrieved and is valid. You
should always use this command to ensure the integrity of your archived snapshots.</p>

<markup
lang="bash"

>CohQL&gt; validate archived snapshot "data" "PartitionedCache" verbose

...
various messages left out
...

Results
Attribute                    Value
---------------------------- -------------------------------------------------------------
Partition Count              31
Archived Snapshot            Name=data, Service=PartitionedCache
Original Storage Format      BDB
Storage Version              0
Implementation Version       0
Number of Partitions Present 31
Is Complete?                 true
Is Archived Snapshot?        true
Persistence Version          14
Statistics
test                         Size=3, Bytes=41, Indexes=0, Triggers=0, Listeners=0, Locks=0</markup>

</li>
<li>
Remove the local snapshot
<p>In this tutorial, we remove the local snapshot so that we can then retrieve the archived snapshot.</p>

<div class="admonition note">
<p class="admonition-inline">A local snapshot of the same name cannot exist already if we want to retrieve an archived snapshot.</p>
</div>
<markup
lang="bash"

>CohQL&gt; remove snapshot "data" "PartitionedCache"
Are you sure you want to remove snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Removing snapshot 'data' for service 'PartitionedCache'
Results
"Success

CohQL&gt; list snapshots
Results
"PartitionedCache": []</markup>

</li>
<li>
Retrieve the archived snapshot
<markup
lang="bash"

>CohQL&gt; retrieve archived snapshot "data" "PartitionedCache"
Are you sure you want to retrieve a snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Retrieving snapshot 'data' for service 'PartitionedCache'
Results
"Success"

CohQL&gt; list snapshots
Results
"PartitionedCache": ["data"]</markup>

</li>
<li>
Remove all data from the cache and recover the snapshot
<markup
lang="bash"

>CohQL&gt; select count() from test
Results
3
CohQL&gt; delete from test
Results

select count() from test
Results
0

CohQL&gt; recover snapshot "data" "PartitionedCache"
Are you sure you want to recover a snapshot called 'data' for service 'PartitionedCache'? (y/n): y
Recovering snapshot 'data' for service 'PartitionedCache'
2022-04-26 12:53:26.866/1734.102 Oracle Coherence GE 14.1.2.0.0 &lt;D5&gt; (thread=DistributedCache:PartitionedCache, member=2): Service PartitionedCache has been suspended
2022-04-26 12:53:28.709/1735.944 Oracle Coherence GE 14.1.2.0.0 &lt;D5&gt; (thread=DistributedCache:PartitionedCache, member=2): Service PartitionedCache has been resumed
Results
"Success"

CohQL&gt; select count() from test
Results
3</markup>

</li>
</ol>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this tutorial, you have learnt about Coherence Persistence and how you can use it with the Coherence Query Language (CohQL)
to create, recover and managed snapshots, monitor snapshot operations via JMX MBean notifications as well
as work with archived snapshots.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/administer/persisting-caches.html#GUID-AA98D601-5CE9-4E33-BB16-487B417BA5A8">Persistence Configuration</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
