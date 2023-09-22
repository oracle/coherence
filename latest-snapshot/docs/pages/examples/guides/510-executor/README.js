<doc-view>

<h2 id="_the_coherence_executor_service">The Coherence Executor Service</h2>
<div class="section">
<p>The Coherence Executor service allows cluster members or *Extend clients to submit
arbitrary tasks to the grid for execution.  Cluster members may define one or more
custom Executors via configuration to support submission of tasks to different executors
based on the required work load.</p>

<p>The functionality offered by the Coherence Executor service is through the
<a id="" title="" target="_blank" href="https://coherence.community/23.09-SNAPSHOT/api/java/com/oracle/coherence/concurrent/executor/RemoteExecutor.html">RemoteExecutor</a> class,
which, upon inspection, should look similar to <code>java.util.concurrent.Executors</code> in the JDK.
Also notice that <code>RemoteExecutor</code> doesn&#8217;t use <code>Runnable</code> or <code>Callable</code>, and instead
uses <code>Remote.Runnable</code> and <code>Remote.Callable</code>.  The remote versions are functionally
equivalent, but are <code>Serializable</code>.  Serialization is necessary as these tasks
may be dispatched to a remote JVM for execution.  Additionally, internally, the tasks
are stored within Coherence caches (which requires keys/values to be Serializable)
to allow task re-execution of a member executing as task fails (for example, the member dies)
- the task is still within the cache ready to be dispatched to another member for execution.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>The example code is written as a set of unit tests.  This guide will
walk the reader through the code that obtains and uses a <code>RemoteExecutor</code>.</p>

</div>

<h3 id="_what_you_need">What You Need</h3>
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

<h4 id="_building_the_example_code">Building the Example Code</h4>
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

<h3 id="_executor_configuration">Executor Configuration</h3>
<div class="section">
<p>While the Coherence Executor service offers a default executor, which will
be demonstrated, this guide will also show how to configure and use a
custom Executor.  Thus, we&#8217;ll begin with the configuration of a fixed-size
thread pool that the Executor service may submit tasks to.  Custom
thread pool definitions are defined within a cache configuration resource.
For the purpose of this guide, the resource will be called <code>custom-executors.xml</code>
which will be placed in <code>src/test/resources</code>:</p>

<markup
lang="xml"

>&lt;cache-config
    xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"
    xmlns:c="class://com.oracle.coherence.concurrent.config.NamespaceHandler"&gt; <span class="conum" data-value="1" />
  &lt;c:fixed&gt; <span class="conum" data-value="2" />
    &lt;c:name&gt;fixed-5&lt;/c:name&gt; <span class="conum" data-value="3" />
    &lt;c:thread-count&gt;5&lt;/c:thread-count&gt; <span class="conum" data-value="4" />
  &lt;/c:fixed&gt;

  &lt;caching-scheme-mapping&gt;
    &lt;cache-mapping&gt;
      &lt;cache-name&gt;*&lt;/cache-name&gt;
      &lt;scheme-name&gt;dist-scheme&lt;/scheme-name&gt;
    &lt;/cache-mapping&gt;
  &lt;/caching-scheme-mapping&gt;

  &lt;caching-schemes&gt;
    &lt;distributed-scheme&gt;
      &lt;scheme-name&gt;dist-scheme&lt;/scheme-name&gt;
      &lt;service-name&gt;DistributedCache&lt;/service-name&gt;
      &lt;backing-map-scheme&gt;
        &lt;local-scheme/&gt;
      &lt;/backing-map-scheme&gt;
    &lt;/distributed-scheme&gt;
  &lt;/caching-schemes&gt;
&lt;/cache-config&gt;</markup>

<ul class="colist">
<li data-value="1">Defines the <code>NamespaceHandler</code> which validates and configures the executor services</li>
<li data-value="2">Defines a fixed thread pool (think <code>Executors.newFixedThreadPool(int)</code>)</li>
<li data-value="3">Gives the thread pool a logical name of <code>fixed-5</code>.  This name will be used to obtain a reference to a <code>RemoteExecutor</code></li>
<li data-value="4">Defines the number of threads this pool should have</li>
</ul>
<p>Each Coherence member using this configuration will have a local executor service
defined with the logical name of <code>fixed-5</code>.  If there are multiple executor services
with the same logical identifier, tasks will be submitted to a random member&#8217;s Executor
Service managing the named pool.</p>

</div>

<h3 id="_create_the_test_class">Create the Test Class</h3>
<div class="section">
<p>The first step is to create the test class that will show and test the various NamedMap operations,
we’ll call this class ExecutorBasicTests:</p>

<markup
lang="java"

>class ExecutorBasicTests {
}</markup>

</div>

<h3 id="_bootstrap_coherence">Bootstrap Coherence</h3>
<div class="section">
<p>The first thing the test class will do is start Coherence using the bootstrap API introduced in Coherence v20.12.
As this is a JUnit test class, this can be accomplished in a static <code>@BeforeAll</code> annotated setup method.</p>

<markup
lang="java"

>    @BeforeAll
    static void boostrapCoherence()
        {
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.cacheconfig", "custom-executors.xml"); <span class="conum" data-value="1" />

        Coherence                    coherence = Coherence.clusterMember(); <span class="conum" data-value="2" />
        CompletableFuture&lt;Coherence&gt; future    = coherence.start(); <span class="conum" data-value="3" />

        future.join(); <span class="conum" data-value="4" />
        }</markup>

<ul class="colist">
<li data-value="1">Pass in the cache configuration, <code>custom-executors.xml</code>, created previously in this guide.</li>
<li data-value="2">Obtain a default storage enabled cluster member <code>Coherence</code> instance.</li>
<li data-value="3">Start the <code>Coherence</code> instance, this wil start all the Coherence services.</li>
<li data-value="4">Block until Coherence instance has fully started before proceeding with the tests</li>
</ul>
<p>Second, add a static <code>@AfterAll</code> annotated tear-down method that will shut down Coherence at the end of the test.</p>

<markup
lang="java"

>    @AfterAll
    static void shutdownCoherence()
        {
        Coherence coherence = Coherence.getInstance(); <span class="conum" data-value="1" />

        coherence.close();
        }</markup>

<ul class="colist">
<li data-value="1">Since only a single default <code>Coherence</code> instance was created, obtain that instance with the
<code>Coherence.getInstance()</code> method, and then close it.</li>
</ul>
<p>Now that the basic framework of the test is in place, tests may now be added to demonstrate some simple
api usages of <code>RemoteExecutor</code>.</p>

</div>

<h3 id="_submitting_a_runnable_to_the_grid">Submitting a Runnable to the Grid</h3>
<div class="section">
<p>The first test will demonstrate submitting a single <code>Remote.Runnable</code> task to the grid.
This task will add an entry to a <code>NamedCache</code> which will allow the test to verify the task
was properly run.</p>

<markup
lang="java"

>    @Test
    void testSimpleRunnable()
            throws Exception
        {
        NamedMap&lt;String, String&gt; map             = getMap(); <span class="conum" data-value="1" />
        RemoteExecutor           defaultExecutor = RemoteExecutor.getDefault(); <span class="conum" data-value="2" />

        map.truncate(); <span class="conum" data-value="3" />
        assertTrue(map.isEmpty());

        Future&lt;?&gt; result = defaultExecutor.submit((Remote.Runnable) () -&gt;
                Coherence.getInstance()
                        .getSession().getMap("data").put("key-1", "value-1")); <span class="conum" data-value="4" />

        result.get(); <span class="conum" data-value="5" />

        String sValue = map.get("key-1"); <span class="conum" data-value="6" />

        assertEquals(sValue, "value-1"); <span class="conum" data-value="7" />
        }</markup>

<ul class="colist">
<li data-value="1">Obtain a local reference to the <code>NamedMap</code>, <code>data</code>.</li>
<li data-value="2">Obtain a reference to the default <code>RemoteExecutor</code>.</li>
<li data-value="3">Truncate the map to ensure no data is present.</li>
<li data-value="4">Submit a <code>Remote.Runnable</code> to the grid which will obtain a reference to the <code>NamedMap</code>, <code>data</code>, and insert an entry.
The <code>map</code> reference isn&#8217;t used in the lambda as the <code>Remote.Runnable</code> may be executed in a remote JVM, therefore,
a reference local to the executing JVM is obtained instead.</li>
<li data-value="5">Wait for the <code>Future</code> to complete.</li>
<li data-value="6">Get the value for <code>key-1</code> from the local <code>NamedMap</code> reference.</li>
<li data-value="7">Assert the expected value was returned.</li>
</ul>
</div>

<h3 id="_submitting_a_callable_to_the_grid">Submitting a Callable to the Grid</h3>
<div class="section">
<p>The next test will demonstrate submitting a single <code>Remote.Callable</code> task to the grid.
The test will first add an entry to the cache.  Next, it will submit a <code>Remote.Callable</code>
that will change the value for the previously added entry and return the previous value.
Finally, the test will ensure the current cache value and the value returned by the
<code>Remote.Callable</code> are the expected values.
This task will add an entry to a <code>NamedCache</code> which will allow the test to verify the task
was properly run.</p>

<markup
lang="java"

>    @Test
    void testSimpleCallable()
            throws Exception
        {
        NamedMap&lt;String, String&gt; map             = getMap(); <span class="conum" data-value="1" />
        RemoteExecutor           defaultExecutor = RemoteExecutor.getDefault(); <span class="conum" data-value="2" />

        map.truncate(); <span class="conum" data-value="3" />
        assertTrue(map.isEmpty());

        map.put("key-1", "value-1"); <span class="conum" data-value="4" />

        Future&lt;String&gt; result = defaultExecutor.submit((Remote.Callable&lt;String&gt;) () -&gt;
                (String) Coherence.getInstance().getSession().getMap("data").put("key-1", "value-2")); <span class="conum" data-value="5" />

        String sResult = result.get(); <span class="conum" data-value="6" />
        String sValue  = map.get("key-1"); <span class="conum" data-value="7" />

        assertEquals(sResult, "value-1"); <span class="conum" data-value="8" />
        assertEquals(sValue, "value-2"); <span class="conum" data-value="9" />
        }</markup>

<ul class="colist">
<li data-value="1">Obtain a local reference to the <code>NamedMap</code>, <code>data</code>.</li>
<li data-value="2">Obtain a reference to the default <code>RemoteExecutor</code>.</li>
<li data-value="3">Truncate the map to ensure no data is present.</li>
<li data-value="4">Insert an entry that the task should change.</li>
<li data-value="5">Submit a <code>Remote.Callable</code> to the grid which will obtain a reference to the <code>NamedMap</code>, <code>data</code>, update the existing entry, and return the previous value.</li>
<li data-value="6">Wait for the <code>Future</code> to complete and obtain the returned value.</li>
<li data-value="7">Get the value for <code>key-1</code> from the local <code>NamedMap</code> reference.</li>
<li data-value="8">Assert the expected value was returned from the <code>Remote.Callable</code> execution.</li>
<li data-value="9">Assert the cache has the value updated by the <code>Remote.Callable</code>.</li>
</ul>
</div>

<h3 id="_submitting_a_task_to_a_specific_thread_pool_in_the_grid">Submitting a Task to a Specific Thread Pool in the Grid</h3>
<div class="section">
<p>The last test will use the thread pool <code>fixed-5</code> that was configured earlier within
this guide.  As this thread pool has five threads, this test will submit several
<code>Remote.Callable</code> instances that will, upon execution, wait for one second, then
return the name of the executing thread.  The test will then obtain the results
for the multiple <code>Remote.Callable</code> executions, and verify all five threads were
used.</p>

<markup
lang="java"

>    @Test
    void testCustomExecutor()
            throws Exception
        {
        RemoteExecutor fixed5 = RemoteExecutor.get("fixed-5"); <span class="conum" data-value="1" />

        List&lt;Remote.Callable&lt;String&gt;&gt; listCallables = new ArrayList&lt;&gt;(5); <span class="conum" data-value="2" />
        for (int i = 0; i &lt; 10; i++)
            {
            listCallables.add(() -&gt;
                              {
                              Thread.sleep(1000);
                              return Thread.currentThread().getName();
                              });
            }

        List&lt;Future&lt;String&gt;&gt; listFutures = fixed5.invokeAll(listCallables); <span class="conum" data-value="3" />

        Set&lt;String&gt; results = new LinkedHashSet&lt;&gt;(); <span class="conum" data-value="4" />
        for (Future&lt;String&gt; listFuture : listFutures)
            {
            results.add(listFuture.get());
            }

        System.out.printf("Tasks executed on threads %s", results);

        assertEquals(5, results.size()); <span class="conum" data-value="5" />
        }</markup>

<ul class="colist">
<li data-value="1">Obtain a reference to the <code>fixed-5</code> <code>RemoteExecutor</code>.</li>
<li data-value="2">Create a list of ten <code>Remote.Callable</code> instances where each instance sleeps for one second and then returns the name of the executing thread.</li>
<li data-value="3">Invoke all <code>Remote.Callable</code> instances within the list by calling <code>RemoteExecutor.invokeAll</code> which returns a List containing a <code>Future</code> for each <code>Remote.Callable</code>.</li>
<li data-value="4">Create a <code>Set</code> of the execution thread names.  As this thread pool servicing these tasks has five threads, this <code>Set</code> should only have five entries.</li>
<li data-value="5">Assert that all five threads of the pool were used.</li>
</ul>
</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>These tests demonstrate the basic usage of the Coherence Executor service.
Developers are encouraged to explore the other functionality defined by <code>RemoteExecutor</code></p>

</div>

<h3 id="_see_also">See Also</h3>
<div class="section">
<p>The Javadoc for <a id="" title="" target="_blank" href="https://coherence.community/23.09-SNAPSHOT/api/java/com/oracle/coherence/concurrent/executor/RemoteExecutor.html">RemoteExecutor</a></p>

</div>
</div>
</doc-view>
