<doc-view>

<h2 id="_performance_over_consistency_availability">Performance over Consistency &amp; Availability</h2>
<div class="section">
<p>This guide walks you through how to tweak Coherence to provide more performance <strong>at the expense of data consistency and availability.</strong>
Out of the box, Coherence provides many features that ensure data consistency, including:</p>

<ol style="margin-left: 15px;">
<li>
Backups - By default there is 1 backup, which will automatically be stored on a separate node, machine or site from the primary to provide redundancy
in the case of the loss of a node, machine or site.

</li>
<li>
Synchronous backups - When entries are mutated, control will not be returned to the client until the primary and backup have been written.

</li>
<li>
Data consistency - All data access is always directed to the primary copy of the data to ensure that the data received is the most recent and consistent.

</li>
</ol>
<p>These guarantees of data consistency and availability are one of the many hallmarks of Coherence, but there may be cases where you may
need to maximize cache reads/ writes by removing some of the above guarantees.</p>

<p>This guide will explore and compare various methods of achieving better performance and the expenses of data consistency and availability.</p>


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
<p><router-link to="#example-tests-1" @click.native="this.scrollFix('#example-tests-1')">Review the Tests</router-link></p>

</li>
<li>
<p><router-link to="#example-tests-config" @click.native="this.scrollFix('#example-tests-config')">Review the cache configuration</router-link></p>

</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Tests</router-link></p>

</li>
<li>
<p><router-link to="#oci" @click.native="this.scrollFix('#oci')">OCI Test Results</router-link></p>

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
<p>In this example you will run a test that will demonstrate different methods for improving Coherence read/ write performance at
the expense of data consistency and availability. These methods are outlined below with their potential impact.</p>

<div class="block-title"><span>Tested Cache Config Scenarios</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
</colgroup>
<thead>
<tr>
<th>Method</th>
<th>Description</th>
<th>Data Read Consistency Impact</th>
<th>Data Availability/Loss Impact</th>
<th>Other comments</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Changing the default read-locator from <code>primary</code> to <code>closest</code></td>
<td class="">Changing the <code>read-locator</code> to <code>closest</code> (primary or backup) can balance request load or reduce latency</td>
<td class="">Dirty / stale read from out of date backup</td>
<td class="">None</td>

</tr>
<tr>
<td class="">Using async backups</td>
<td class="">Enabling asynchronous backups allows the client to continue processing without waiting for the backup to complete</td>
<td class="">None</td>
<td class="">Medium - If the node with the primary copy failed before the backup is complete, data may be lost</td>
<td class="">Async backups result in <code>n</code> backup requests, see below</td>
</tr>
<tr>
<td class="">Using scheduling backups</td>
<td class="">Enabling scheduled backups allows backups to be scheduled at regular intervals after a delay</td>
<td class="">None</td>
<td class="">Medium&#8594;High - There is potentially more risk of data loss as multiple batched backups could be lost with node failure</td>
<td class="">Can be more efficient than asynchronous backups as backups can be batched</td>
</tr>
<tr>
<td class="">Disabling backups</td>
<td class="">Setting <code>backup-count</code> to zero</td>
<td class="">None</td>
<td class="">High - If any node is lost, then the primary data will be lost</td>

</tr>
</tbody>
</table>
</div>
<p>See below for documentation links:</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-FCC1ADAE-8A67-4E5E-BB17-381D91DB5AC3">Using the Read Locator</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-7D64D271-7F74-4AFB-ACB8-6F8BB5B37A33">Using Asynchronous Backups</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-84223FCD-A1CE-4CB6-81FA-FC3980907D3C">Using Scheduled Backups</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/cache-configuration-elements.html">Disabling Backups</a></p>

</li>
</ul>

<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://www.oracle.com/java/technologies/downloads/">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="https://gradle.org/install/">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included, so they can be built without first installing
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
<p class="admonition-inline">You can skip the tests in the initial build by adding the following options: <code>-DskipTests</code> for Maven or <code>-x test</code> for Gradle.</p>
</div>
</div>

<h4 id="running">Running the Examples</h4>
<div class="section">
<p>This example comprises a number of tests running with difference cache configurations.</p>

<ol style="margin-left: 15px;">
<li>
Running each example
<p>Each example can be run direct from the IDE, or can be run via executing the tests.</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

</li>
</ol>
</div>
</div>

<h3 id="example-tests-1">Review the Tests</h3>
<div class="section">
<p>The example code comprises a main test class <code>PerformanceOverConsistencyTest</code> which has number of methods.</p>

<p>The following JUnit test method creates multiple caches with different setup and runs the <code>runTest()</code> method against
each to record times.</p>

<p>Each test-run is run once first and then results are recorded to try and get more consistent results.</p>

<markup
lang="java"

>@Test
/**
 * Run the same test against different cache types.
 */
public void testDifferentScenarios() throws Exception {
    // set the system properties to join the cluster
    System.setProperty("coherence.wka", "127.0.0.1");
    System.setProperty("coherence.ttl", "0");
    System.setProperty("coherence.cluster", CLUSTER_NAME);
    System.setProperty("coherence.clusterport", Integer.toString(clusterPort));
    System.setProperty("coherence.cacheconfig", CACHE_CONFIG);
    System.setProperty("coherence.log.level", "1");
    System.setProperty("coherence.distributed.localstorage", "false");

    Coherence coh     = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
    Session   session = coh.getSession();

    System.out.println("Running Tests");
    System.out.flush();
    final String headerFormat = "%-15s %12s %12s %12s %12s %12s\n";
    final String lineFormat   = "%-15s %,10dms %,10dms %,10dms %,10dms %,10dms\n";

    NamedCache&lt;Integer, Customer&gt; cache            = session.getCache("base-customers");
    NamedCache&lt;Integer, Customer&gt; cacheReadLocator = session.getCache("rl-customers");
    NamedCache&lt;Integer, Customer&gt; cacheAsyncBackup = session.getCache("async-backup-customers");
    NamedCache&lt;Integer, Customer&gt; cacheSchedBackup = session.getCache("sched-backup-customers");
    NamedCache&lt;Integer, Customer&gt; cacheNoBackup    = session.getCache("no-backup-customers");

    List&lt;TestResult&gt; listResults = new ArrayList&lt;&gt;();

    log("");
    // discard the first run of each of the tests to ensure we have more consistent test results
    runTest(cache, "base");
    listResults.add(runTest(cache, "base"));

    runTest(cacheReadLocator, "base-rl");
    listResults.add(runTest(cacheReadLocator, "base-rl"));

    runTest(cacheAsyncBackup, "async-backup");
    listResults.add(runTest(cacheAsyncBackup, "async-backup"));

    runTest(cacheSchedBackup, "sched-backup");
    listResults.add(runTest(cacheSchedBackup, "sched-backup"));

    runTest(cacheNoBackup, "no-backup");
    listResults.add(runTest(cacheNoBackup, "no-backup"));

    // output the results
    System.out.printf(headerFormat, "Cache Type", "2k Put", "8 PutAll","100 Invoke", "2k Get", "100 GetAll");

    listResults.forEach(
            (v)-&gt;System.out.printf(lineFormat, v.getType(), v.getPutDuration(), v.getPutAllDuration(),
                    v.getInvokeDuration(), v.getGetDuration(), v.getGetAllDuration()));
    log("\nNote: The above times are to run the individual parts of tests, not to do an individual put/get, etc.\n");
}</markup>

<div class="admonition note">
<p class="admonition-inline">The test results you get may vary as they are run on a single machine. You should carry out tests of different configurations in
your own development/test environments to see the effect these scenarios have.</p>
</div>
<p>The following JUnit test method runs various operations including <code>get()</code>, <code>put()</code>, <code>getAll()</code> and <code>invoke()</code> and times them to compare
the different cache configuration.</p>

<p>See comments in code for explanations.</p>

<markup
lang="java"

>/**
 * Run various cache operations to test different configurations to achieve performance over consistency.
 *
 * @param cache {@link NamedCache} to test against
 * @param type  type of test
 * @return the test results
 */
private TestResult runTest(NamedCache&lt;Integer, Customer&gt; cache, String type) {
    cache.clear();

    long start = System.currentTimeMillis();

    // insert multiple customers using individual put()
    for (int i = 1; i &lt;= 2_000; i++) {
        Customer c = getCustomer(i);
        cache.put(c.getId(), c);
    }
    long putDuration = System.currentTimeMillis() - start;

    Map&lt;Integer, Customer&gt; buffer = new HashMap&lt;&gt;();

    start = System.currentTimeMillis();
    // insert customers using putAll in batches
    for (int i = 2_001; i &lt;= 10_000; i++) {
        Customer c = getCustomer(i);
        buffer.put(c.getId(), c);
        if (i % 1_000 == 0) {
            cache.putAll(buffer);
            buffer.clear();
        }
    }
    if (!buffer.isEmpty()) {
        cache.putAll(buffer);

    }

    long putAllDuration = System.currentTimeMillis() - start;

    start = System.currentTimeMillis();
    // issue 2,000 get() operations
    for (int i = 1; i &lt; 2_000; i++) {
        Customer value = cache.get(i);
    }

    long getDuration = System.currentTimeMillis() - start;

    start = System.currentTimeMillis();
    // issue 100 getAll() operations
    for (int i = 1; i &lt; 100; i++) {
        Map&lt;Integer, Customer&gt; all = cache.getAll(Set.of(i, i + 1, i + 2, i + 3, i + 4, i + 5));
    }

    long getAllDuration = System.currentTimeMillis() - start;

    start = System.currentTimeMillis();
    // issue 100 entry processor updates which require backup updates
    for (int i = 1; i &lt; 100L; i++) {
        cache.invoke(i, Processors.update(Customer::setCustomerType, Customer.GOLD));
    }
    long invokeDuration = System.currentTimeMillis() - start;

    cache.clear();
    return new TestResult(type, putDuration, putAllDuration, getDuration, getAllDuration, invokeDuration);
}</markup>


<h4 id="example-tests-config">Review the cache config</h4>
<div class="section">
<p>The cache configuration contains various <code>cache-scheme-mappings</code> and related <code>distributed-scheme</code> entries to test various scenarios.</p>

<ol style="margin-left: 15px;">
<li>
Review the Caching Scheme Mapping
<markup
lang="xml"

>&lt;caching-scheme-mapping&gt;
  &lt;cache-mapping&gt; <span class="conum" data-value="1" />
    &lt;cache-name&gt;base-*&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-base&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;

  &lt;cache-mapping&gt; <span class="conum" data-value="2" />
    &lt;cache-name&gt;rl-*&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-rl&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;

  &lt;cache-mapping&gt; <span class="conum" data-value="3" />
    &lt;cache-name&gt;async-backup-*&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-async-backup&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;

  &lt;cache-mapping&gt; <span class="conum" data-value="4" />
    &lt;cache-name&gt;sched-backup-*&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-sched-backup&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;

  &lt;cache-mapping&gt; <span class="conum" data-value="5" />
    &lt;cache-name&gt;no-backup-*&lt;/cache-name&gt;
    &lt;scheme-name&gt;server-no-backup&lt;/scheme-name&gt;
  &lt;/cache-mapping&gt;
&lt;/caching-scheme-mapping&gt;</markup>

<ul class="colist">
<li data-value="1">Base mapping with out-of-the box defaults</li>
<li data-value="2">Read locator</li>
<li data-value="3">Async backup</li>
<li data-value="4">Scheduled backup</li>
<li data-value="5">No backup</li>
</ul>
</li>
<li>
Review the base distributed scheme
<markup
lang="xml"

>&lt;distributed-scheme&gt;
  &lt;scheme-name&gt;server-base&lt;/scheme-name&gt;
  &lt;service-name&gt;PartitionedCache&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
&lt;/distributed-scheme&gt;</markup>

</li>
<li>
Review distributed scheme with read-locator set to <code>closest</code>
<markup
lang="xml"

>&lt;distributed-scheme&gt;
  &lt;scheme-name&gt;server-rl&lt;/scheme-name&gt;
  &lt;service-name&gt;PartitionedCacheReadLocator&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;read-locator&gt;closest&lt;/read-locator&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
&lt;/distributed-scheme&gt;</markup>

</li>
<li>
Review distributed scheme with backups set to asynchronous
<markup
lang="xml"

>&lt;distributed-scheme&gt;
  &lt;scheme-name&gt;server-async-backup&lt;/scheme-name&gt;
  &lt;service-name&gt;PartitionedCacheAsyncBackup&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;async-backup&gt;true&lt;/async-backup&gt;
&lt;/distributed-scheme&gt;</markup>

</li>
<li>
Review distributed scheme with backups set the scheduled every 2 seconds
<markup
lang="xml"

>&lt;distributed-scheme&gt;
  &lt;scheme-name&gt;server-sched-backup&lt;/scheme-name&gt;
  &lt;service-name&gt;PartitionedCacheSchedBackup&lt;/service-name&gt;
  &lt;backing-map-scheme&gt;
    &lt;local-scheme&gt;
      &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
    &lt;/local-scheme&gt;
  &lt;/backing-map-scheme&gt;
  &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;async-backup&gt;2s&lt;/async-backup&gt;
&lt;/distributed-scheme&gt;</markup>

</li>
<li>
Review distributed scheme with no backups
<markup
lang="xml"

>  &lt;distributed-scheme&gt;
    &lt;scheme-name&gt;server-no-backup&lt;/scheme-name&gt;
    &lt;service-name&gt;PartitionedCacheNoBackup&lt;/service-name&gt;
    &lt;backup-count&gt;0&lt;/backup-count&gt;
    &lt;backing-map-scheme&gt;
      &lt;local-scheme&gt;
        &lt;unit-calculator&gt;BINARY&lt;/unit-calculator&gt;
      &lt;/local-scheme&gt;
    &lt;/backing-map-scheme&gt;
    &lt;autostart&gt;true&lt;/autostart&gt;
  &lt;/distributed-scheme&gt;
&lt;/caching-schemes&gt;</markup>

</li>
</ol>
</div>
</div>

<h3 id="run-example-1">Run the Tests</h3>
<div class="section">
<p>Run the examples using the test case below using Maven or Gradle.</p>

<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

<p>When the test is run you will see output from the test code. The tests with take approximately 5 minutes and there
may be a lot of other output regarding server startup, but the main output we are interested is below.  Search for <code>Running Tests</code> in your output.</p>

<div class="admonition note">
<p class="admonition-inline">Output has been truncated and formatted for easier reading.</p>
</div>
<markup
lang="bash"

>[Coherence:err:70562]    6: 2023-07-28 11:35:18.334/2.520 Oracle Coherence GE 14.1.1.2206.5 &lt;Warning&gt; (thread=Coherence, member=n/a): Local address "127.0.0.1" is a loopback address; this cluster node will not connect to nodes located on different machines

Running Tests
####
Cache Type            2k Put     8 PutAll   100 Invoke       2k Get   100 GetAll
base                 1,245ms        153ms        103ms        600ms         45ms
base-rl                904ms         81ms         89ms        414ms         36ms
async-backup           541ms        100ms         70ms        379ms         24ms
sched-backup           393ms         57ms         65ms        437ms         31ms
no-backup              354ms         56ms         50ms        364ms         24ms
####
Note: The above times are to run the individual parts of tests, not to do an individual put/get, etc.

[Coherence:err:70562]    7: (terminated)
[Coherence:out:70562]    1: (terminated)</markup>

<ul class="colist">
<li data-value="1">The base results with defaults</li>
<li data-value="2">Read locator set to <code>closest</code> means data could be read from backup or primary which could be on the same machine. As all the members are running on the same machine, the results if this test may not be relevant. See the OCI results below for more relevant test results.</li>
<li data-value="3">Async backup improves <code>put()</code>, <code>putAll()</code> and <code>invoke()</code> operations significantly</li>
<li data-value="4">Scheduled backups can have small improvements over async backups</li>
<li data-value="5">No backups has marginal or negligible improvement from asynchronous oe scheduled backups</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">The test results you get may vary as they are run on a single machine. You should carry out tests of different configurations in
your own development/test environments to see the effect these scenarios have.</p>
</div>
</div>

<h3 id="oci">OCI Test Results</h3>
<div class="section">
<p>Below are the results of additional tests run on an Oracle Cloud Infrastructure (OCI)
where there is a some latency between nodes, (rather then being on a single machine) as well as cache servers on multiple machines to show the difference more clearly.</p>

<p>The setup was 3 storage nodes across 3 availability domains as well as 3 JMeter runners running tests.</p>


<h4 id="_various_backup_types">Various Backup Types</h4>
<div class="section">


<v-card>
<v-card-text class="overflow-y-hidden" style="text-align:center">
<img src="docs/images/backup-types.png" alt="backup types"width="100%" />
</v-card-text>
</v-card>

<p>From the above you can see the default put time for this environment with standard (one) backup was around 1.8ms and the scheduled, async and no-backup were considerably less.</p>

</div>

<h4 id="_default_and_closest_read_locators">Default and Closest Read Locators</h4>
<div class="section">
<p>Running random get operations (50,000) using default read-locator of <code>primary</code> and then running using read-locator of <code>closest</code>, the following results were observed:</p>

<div class="block-title"><span>Read Locator Test on OCI</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
<col style="width: 20%;">
</colgroup>
<thead>
<tr>
<th>Read Locator</th>
<th>Test Runner 1</th>
<th>Test Runner 2</th>
<th>Test Runner 3</th>
<th>Average</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">primary</td>
<td class="">0.439ms</td>
<td class="">0.517ms</td>
<td class="">0.575ms</td>
<td class="">0.518ms</td>
</tr>
<tr>
<td class="">closest</td>
<td class="">0.308ms</td>
<td class="">0.441ms</td>
<td class="">0.413ms</td>
<td class="">0.387ms</td>
</tr>
</tbody>
</table>
</div>
</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>This guide walks you through how to tweak Coherence to provide more performance <strong>at the expense of data consistency and availability.</strong></p>

<p>A few notes from the above results:</p>

<ol style="margin-left: 15px;">
<li>
If your application can tolerate some data loss, then rather than using zero backups, you should use async or scheduled backups as this does provide better availability that no backups.

</li>
<li>
If your application can tolerate potential dirty reads, then use the <code>closest</code> read locator.

</li>
</ol>
</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-FCC1ADAE-8A67-4E5E-BB17-381D91DB5AC3">Using Read Locator</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-7D64D271-7F74-4AFB-ACB8-6F8BB5B37A33">Using Asynchronous Backups</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/implementing-storage-and-backing-maps.html#GUID-84223FCD-A1CE-4CB6-81FA-FC3980907D3C">Using Scheduled Backups</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/cache-configuration-elements.html">Disabling Backups</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
