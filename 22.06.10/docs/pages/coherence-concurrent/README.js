<doc-view>

<h2 id="_distributed_concurrency">Distributed Concurrency</h2>
<div class="section">
<p>Coherence Concurrent module provides distributed implementations of the concurrency primitives from the <code>java.util.concurrent</code> package that you are already familiar with, such as executors, atomics, locks, semaphores and latches.</p>

<p>This allows you to implement concurrent applications using the constructs you are already familiar with, but to expand the "scope" of concurrency from a single process to potentially hundreds of processes within a Coherence cluster. For example - you can use executors to submit tasks to be executed somewhere in the cluster; you can use locks, latches and semaphores to synchronize execution across many cluster members; you can use atomics to implement global counters across many processes, etc.</p>

<p>Please keep in mind that while these features are extremely powerful and allow you to reuse the knowledge you already have, they may have detrimental effect on scalability and/or performance. Whenever you synchronize execution via locks, latches or semaphores, you are introducing a potential bottleneck into the architecture. Whenever you use a distributed atomic to implement a global counter, you are turning very simple operations that take mere nanoseconds locally, such as increment and decrement, into fairly expensive network calls that could take milliseconds (and potentially block even longer under heavy load).</p>

<p>So, use these features sparingly. In many cases there is a better, faster and more scalable way to accomplish the same goal using Coherence primitives such as entry processors, aggregators and events, which were designed to perform and scale well in a distributed environment from the get-go.</p>


<h3 id="_factory_classes">Factory Classes</h3>
<div class="section">
<p>Each of the features above is backed by one or more Coherence caches, possibly with preconfigured interceptors, but for the most part you shouldn&#8217;t care about that: all interaction with lower level Coherence primitives is hidden behind various factory classes that allow you to get the instances of the classes you need.</p>

<p>For example, you will use factory methods within <code>Atomics</code> class to get instances of various atomic types, <code>Locks</code> to get lock instances, <code>Latches</code> and <code>Semaphores</code> to get, well, latches and semaphores.</p>

</div>

<h3 id="_local_vs_remote">Local vs Remote</h3>
<div class="section">
<p>In many cases the factory classes will allow you to get both the <strong>local</strong> and the <strong>remote</strong> instances of various constructs. For example, <code>Locks.localLock</code> will give you an instance of a standard <code>java.util.concurrent.locks.ReentrantLock</code>, while <code>Locks.remoteLock</code> will return an instance of a <code>RemoteLock</code>. In cases where JDK doesn&#8217;t provide a standard interface, which is the case with atomics, latches and semaphores, we&#8217;ve extracted the interface from the existing JDK class, and created a thin wrapper around the corresponding JDK implementation. For example, Coherence Concurrent provides a <code>Semaphore</code> interface, and <code>LocalSemaphore</code> class that wraps <code>java.util.concurrent.Semaphore</code>. The same is true for the <code>CountDownLatch</code>, and all atomic types.</p>

<p>The main advantage of using factory classes to construct both the local and remote instances is that it allows you to name local locks the same way you name remote locks.: calling <code>Locks.localLock("foo")</code> will always return the same <code>Lock</code> instance, as the <code>Locks</code> class internally caches both the local and the remote instances it created. Of course, in the case of remote locks, every locally cached remote lock instance is ultimately backed by a shared lock instance somewhere in the cluster, which is used to synchronize lock state across the processes.</p>

</div>

<h3 id="_serialization">Serialization</h3>
<div class="section">
<p>Coherence Concurrent supports both Java serialization and POF out-of-the-box, with Java serialization being the default.</p>

<p>If you want to use POF instead, you will need to specify that by setting <code>coherence.concurrent.serializer</code> system property to <code>pof</code>. You will also need to include <code>coherence-concurrent-pof-config.xml</code> into your own POF configuration file, in order to register built-in Coherence Concurrent types.</p>

</div>

<h3 id="_persistence">Persistence</h3>
<div class="section">
<p>Coherence Concurrent supports both active and on-demand persistence, but just like in the rest of Coherence it is set to <code>on-demand</code> by default.</p>

<p>In order to use active persistence you should set <code>coherence.concurrent.persistence.environment</code> system property to <code>default-active</code>, or another persistence environment that has active persistence enabled.</p>

</div>
</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence Concurrent features, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-concurrent&lt;/artifactId&gt;
        &lt;version&gt;22.06.10&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>Once the necessary dependency is in place, you can start using the features it provides, as the following sections describe.</p>

<ul class="ulist">
<li>
<p><router-link to="#executors" @click.native="this.scrollFix('#executors')">Executors</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#executors-overview" @click.native="this.scrollFix('#executors-overview')">Executors Overview</router-link></p>

</li>
<li>
<p><router-link to="#executors-usage" @click.native="this.scrollFix('#executors-usage')">Executors Usage</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#executors-orchestration" @click.native="this.scrollFix('#executors-orchestration')">Advanced Orchestration</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#executors-orchestration-tasks" @click.native="this.scrollFix('#executors-orchestration-tasks')">Tasks</router-link></p>

</li>
<li>
<p><router-link to="#executors-orchestration-context" @click.native="this.scrollFix('#executors-orchestration-context')">Task Context</router-link></p>

</li>
<li>
<p><router-link to="#executors-orchestration-orchestration" @click.native="this.scrollFix('#executors-orchestration-orchestration')">Task Orchestration</router-link></p>

</li>
<li>
<p><router-link to="#executors-orchestration-collect" @click.native="this.scrollFix('#executors-orchestration-collect')">Task Collector and Collectable</router-link></p>

</li>
<li>
<p><router-link to="#executors-orchestration-coordinator" @click.native="this.scrollFix('#executors-orchestration-coordinator')">Task Coordinator</router-link></p>

</li>
<li>
<p><router-link to="#executors-orchestration-subscriber" @click.native="this.scrollFix('#executors-orchestration-subscriber')">Task Subscriber</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#executors-orchestration-examples" @click.native="this.scrollFix('#executors-orchestration-examples')">Advanced Orchestration Examples</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#executors-configuration" @click.native="this.scrollFix('#executors-configuration')">Executors Configuration</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#executors-configuration" @click.native="this.scrollFix('#executors-configuration')">Executors Configuration Examples</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#executors-management" @click.native="this.scrollFix('#executors-management')">Executors Management</router-link></p>

</li>
<li>
<p><router-link to="#executors-management-rest" @click.native="this.scrollFix('#executors-management-rest')">Executors Management over REST</router-link></p>

</li>
<li>
<p><router-link to="#cdi-executors" @click.native="this.scrollFix('#cdi-executors')">CDI Support for Executors</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#atomics" @click.native="this.scrollFix('#atomics')">Atomics</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#atomics-async" @click.native="this.scrollFix('#atomics-async')">Non-blocking Atomics</router-link></p>

</li>
<li>
<p><router-link to="#cdi-atomics" @click.native="this.scrollFix('#cdi-atomics')">CDI Support for Atomics</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#locks" @click.native="this.scrollFix('#locks')">Locks</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#exclusive-locks" @click.native="this.scrollFix('#exclusive-locks')">Exclusive Locks</router-link></p>

</li>
<li>
<p><router-link to="#read-write-locks" @click.native="this.scrollFix('#read-write-locks')">Read/Write Locks</router-link></p>

</li>
<li>
<p><router-link to="#cdi-locks" @click.native="this.scrollFix('#cdi-locks')">CDI Support for Locks</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#latches-semaphores" @click.native="this.scrollFix('#latches-semaphores')">Latches and Semaphores</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#count-down-latch" @click.native="this.scrollFix('#count-down-latch')">Count Down Latch</router-link></p>

</li>
<li>
<p><router-link to="#semaphore" @click.native="this.scrollFix('#semaphore')">Semaphore</router-link></p>

</li>
<li>
<p><router-link to="#cdi-latches-semaphores" @click.native="this.scrollFix('#cdi-latches-semaphores')">CDI Support for Latches and Semaphores</router-link></p>

</li>
</ul>
</li>
</ul>

<h3 id="executors">Executors</h3>
<div class="section">

</div>

<h3 id="executors-overview">Overview</h3>
<div class="section">
<p>Coherence Concurrent provides a facility to dispatch tasks, either a <code>Runnable</code>, <code>Callable</code>, or <code>Task</code> to
a Coherence cluster for execution.</p>

<p>Executors that will actually  execute the submitted tasks are configured on each cluster
member by defining one or more named executors within a cache configuration resource.</p>

</div>

<h3 id="executors-usage">Usage Examples</h3>
<div class="section">
<p>By default, each Coherence cluster with the <code>coherence-concurrent</code> module on the classpath,
will include a single-threaded executor that may be used to execute dispatched tasks.</p>

<p>Given this, the simplest example would be:</p>

<markup
lang="java"

>RemoteExecutor remoteExecutor = RemoteExecutor.getDefault();

Future&lt;Void&gt; result = remoteExecutor.submit(() -&gt; System.out.println("Executed"));

result.get(); // block until completion</markup>

<p>If for example, an executor was configured named <code>Fixed5</code>, the code would be:</p>

<markup
lang="java"

>RemoteExecutor remoteExecutor = RemoteExecutor.get("Fixed5");</markup>

<p>If no executor has been configured with the given name, the <code>RemoteExecutor</code>
will throw <code>RejectedExecutionException</code>.</p>

<p>Each <code>RemoteExecutor</code> instance may hold local resources that should be released
when the <code>RemoteExecutor</code> is no longer needed.  Like an <code>ExecutorService</code>,
a <code>RemoteExecutor</code> has similar methods to shut the executor down.
When calling these methods, it will have no impact on the executors registered
within the cluster.</p>

</div>

<h3 id="executors-orchestration">Orchestration</h3>
<div class="section">
<p>While the <code>RemoteExecutor</code> does provide functionality similar to the standard <code>ExecutorService</code> included in the JDK, this may not be enough in the context of Coherence. A task might need to run across multiple Coherence members, produce intermediate results, and remain durable in case a cluster member executing the task fails. In such cases, task orchestration can be used. Before diving into the details of
orchestration, the following concepts should be understood:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Interface</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Task</td>
<td class="">Tasks are like <code>Callable</code> and <code>Runnable</code> classes in that
they are designed to be potentially executed by one or more threads.
Unlike <code>Callable</code> and <code>Runnable</code> classes, the execution may occur in different Java Virtual Machines, fail and/or recover between different Java Virtual Machine processes.</td>
</tr>
<tr>
<td class="">Task.Context</td>
<td class="">Provides contextual information for a <code>Task</code> as it is executed, including
the ability to access and update intermediate results for the <code>Executor</code>
executing the said <code>Task</code>.</td>
</tr>
<tr>
<td class="">Task.Orchestration</td>
<td class="">Defines information concerning the orchestration of a <code>Task</code> across a
set of executors defined across multiple Coherence members for a given
<code>RemoteExecutor</code>.</td>
</tr>
<tr>
<td class="">Task.Coordinator</td>
<td class="">A publisher of collected <code>Task</code> results that additionally permits
* coordination of the submitted <code>Task</code>.</td>
</tr>
<tr>
<td class="">Task.Subscriber</td>
<td class="">A receiver of items produced by a <code>Task.Coordinator</code>.</td>
</tr>
<tr>
<td class="">Task.Properties</td>
<td class="">State sharing mechanism for tasks.</td>
</tr>
<tr>
<td class="">Task.Collector</td>
<td class="">A mutable reduction operation that accumulates results into a mutable result
container, optionally transforming the accumulated result into a final
representation after all results have been processed.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="executors-orchestration-tasks">Tasks</h3>
<div class="section">
<p><code>Task</code> implementations define a single method called <code>execute(Context)</code>
that performs the task, possibly yielding execution to some later point.
Once the method has completed execution, by returning a result or throwing
an exception (but not a <code>Yield</code> exception), the task is considered completed
for the assigned <code>Executor</code>.</p>

<p>A <code>Task</code> may yield execution for a given time by throwing a <code>Yield</code> exception.
This exception type signals the execution of a <code>Task</code> by an <code>Executor</code> is to
be suspended and resumed at some later point in time, typically by the same <code>Executor</code>.</p>

</div>

<h3 id="executors-orchestration-context">Task Context</h3>
<div class="section">
<p>When a <code>Task</code> is executed a <code>Context</code> instance will be passed as an execution
argument.
The <code>Context</code> provides access to task properties allowing shared state between tasks running in multiple Java Virtual Machines.
The <code>Context</code> provides details on overall execution status:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Execution State</th>
<th>Method</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Complete</td>
<td class=""><code>Context.isDone()</code></td>
<td class="">Allows a <code>Task</code> to determine if the task is complete.
Completion may be due to normal termination, an exception or cancellation.
In all of these cases, this method will return <code>true</code>.</td>
</tr>
<tr>
<td class="">Cancelled</td>
<td class=""><code>Context.isCancelled()</code></td>
<td class="">Allows a <code>Task</code> to determine if the task is effectively cancelled.</td>
</tr>
<tr>
<td class="">Resuming</td>
<td class=""><code>Context.isResuming()</code></td>
<td class="">Determines if a <code>Task</code> execution by an <code>Executor</code> resuming
after being recovered (i.e. fail-over) or due to resumption after a task
had previously thrown a <code>Yield</code> exception.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="executors-orchestration-orchestration">Task Orchestration</h3>
<div class="section">
<p>Orchestrations begin by calling <code>RemoteExecutor.orchestrate(Task)</code> which
will return a <code>Task.Orchestration</code> instance for the given <code>Task</code>.
With the <code>Task.Orchestration</code>, it&#8217;s possible to configure the aspects
of where the task will be run.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Method</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">concurrently()</td>
<td class="">Tasks will be run, concurrently, across all Java Virtual Machines where
the named executor is defined/configured.  This is the default.</td>
</tr>
<tr>
<td class="">sequentially()</td>
<td class="">Tasks will be run, in sequence, across all Java Virtual Machines where
the named executor is defined/configured.</td>
</tr>
<tr>
<td class="">limit(int)</td>
<td class="">Limit the task to <code>n</code> executors.  Use this to limit the number of
executors that will be considered for task execution.  If not set, the default behavior is to run the task on all Java Virtual Machine where
the named executor is defined/configured.</td>
</tr>
<tr>
<td class="">filter(Predicate)</td>
<td class="">Filtering provides an additional way to constrain where a task may be run.
The predicates will be applied against metadata associated with each executor on each Java Virtual Machine.  Some examples of metadata would be the member in which the executor is running, or the role of a member.
Predicates may be chained to provide boolean logic in determining an appropriate executor.</td>
</tr>
<tr>
<td class="">define(String, &lt;V&gt;)</td>
<td class="">Define initial state that will be available to all tasks no matter which Java Virtual Machine that task is running on.</td>
</tr>
<tr>
<td class="">retrain(Duration)</td>
<td class="">When specified, the task will be retained allowing new subscribers to be notified of the final result of a task computation after it has completed.</td>
</tr>
<tr>
<td class="">collect(Collector)</td>
<td class="">This is the terminal of the orchestration builder returning a <code>Task.Collectable</code> which defines how results are to be collected and ultimately submits the task to the grid.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="executors-orchestration-collect">Task Collector and Collectable</h3>
<div class="section">
<p>The <code>Task.Collector</code> passed to the orchestration will collect results from
tasks and optionally transforms the collected results into a final format.
Collectors are best illustrated by using examples of Collectors that are available in the <code>TaskCollector</code> class:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Method</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">count()</td>
<td class="">The count of non-null results that have been collected from the executing task(s).</td>
</tr>
<tr>
<td class="">firstOf()</td>
<td class="">Collects and returns the first result provided by the executing task(s).</td>
</tr>
<tr>
<td class="">lastOf()</td>
<td class="">Collects and returns the last result returned by the executing task(s).</td>
</tr>
<tr>
<td class="">setOf()</td>
<td class="">Collects and returns all non-null results as a Set.</td>
</tr>
<tr>
<td class="">listOf()</td>
<td class="">Collects and returns all non-null results as a List.</td>
</tr>
</tbody>
</table>
</div>
<p>The <code>Task.Collectable</code> instance returned by calling <code>collect</code> on the orchestration allows, among other things, setting the condition under which
no more results will be collected or published any registered subscribers.
Calling <code>submit()</code> on the <code>Task.Collectable</code> will being the orchestration of the task.</p>

</div>

<h3 id="executors-orchestration-coordinator">Task Coordinator</h3>
<div class="section">
<p>Upon calling <code>submit()</code> on the orchestration <code>Collectable</code>, a <code>Task.Coordinator</code> is returned.  Like the <code>Task.Collectable</code> the <code>Task.Coordinator</code> allows for the registration of subscribers.  Additionally,
provides the ability to cancel or check the completion status of the orchestration.</p>

</div>

<h3 id="executors-orchestration-subscriber">Task Subscriber</h3>
<div class="section">
<p>The <code>Task.Subscriber</code> receives various events pertaining to the execution status of the orchestration:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Method</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">onComplete()</td>
<td class="">Signals the completion of the orchestration.</td>
</tr>
<tr>
<td class="">onError(Throwable)</td>
<td class="">Called when an unrecoverable error (given as the argument)
has occurred.</td>
</tr>
<tr>
<td class="">onNext(&lt;T&gt;)</td>
<td class="">Called when the <code>Task.Coordinator</code> has produced a result.</td>
</tr>
<tr>
<td class="">onSubscribe(Task.Subscription)</td>
<td class="">Called prior to any calls to <code>onComplete()</code>, <code>onError(Throwable)</code>, or <code>onNext(&lt;T&gt;)</code> are called.  The <code>Task.Subscription</code> provided gives access to
cancelling the subscription, or obtaining a reference to the <code>Task.Coordinator</code>.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="executors-orchestration-examples">Advanced Orchestration Examples</h3>
<div class="section">
<p>To begin, consider the following code common to the orchestration examples:</p>

<markup
lang="java"

>// demonstrate orchestration using the default RemoteExecutor
RemoteExecutor executor = RemoteExecutor.getDefault();

// WaitingSubscriber is an implementation of the
// com.oracle.coherence.concurrent.executor.Task.Subscriber interface
// that has a get() method that blocks until Subscriber.onComplete() is
// called and will return the results received by onNext()
WaitingSubscriber subscriber = new WaitingSubscriber();

// ValueTask is an implementation of the
// com.oracle.coherence.concurrent.executor.Task interface
// that returns the value provided at construction time
ValueTask task = new ValueTask("Hello World");</markup>

<p>Given the above, the simplest example of an orchestration:</p>

<markup
lang="java"

>// orchestrate the task, subscribe, and submit
executor.orchestrate(task)
        .subscribe(subscriber)
        .submit();

// wait for the task to complete
// if this was run on four cluster members running the default executor service,
// the returned Collection will have four results
Collection&lt;String&gt; results = subscriber.get();</markup>

<p>Building on the above, assume a cluster with two storage and two proxy members.
The cluster members are configured with the roles of <code>storage</code> and <code>proxy</code>,
respectively.  Let&#8217;s say the task needs to run on <code>storage</code> members only, then
the orchestration could look like:</p>

<markup
lang="java"

>// orchestrate the task, filtering by a role, subscribe, and submit
executor.orchestrate(task)
        .filter(Predicates.role("storage"))
        .subscribe(subscriber)
        .submit();

// wait for the task to complete
// as there are only two storage members in this hypothetical, only two
// results will be returned
Collection&lt;String&gt; results = subscriber.get();</markup>

<p>There are several predicates available for use in <code>com.oracle.coherence.concurrent.executor.function.Predicates</code>,
however, in the case none apply to the target use case, simply implement the
<code>Remote.Predicate</code> interface.</p>

<p>Collection of results and how they are presented to the subscriber
can be customized by using <code>collect(Collector)</code> and <code>until(Predicate)</code>:</p>

<markup
lang="java"

>// orchestrate the task, collecting the first non-null result,
// subscribe, and submit
executor.orchestrate(new MayReturnNullTask())
        .collect(TaskCollectors.firstOf())
        .until(Predicates.nonNullValue())
        .subscribe(subscriber)
        .submit();

// wait for the task to complete
// the first non-result returned will be the one provided to the
// subscriber
Collection&lt;String&gt; results = subscriber.get();</markup>

<p>Several collectors are provided in <code>com.oracle.coherence.concurrent.executor.TaskCollectors</code>,
however, in the case none apply to the target use case, implement the
<code>Task.Collector</code> interface.</p>

</div>

<h3 id="executors-configuration">Configuration</h3>
<div class="section">
<p>Several executor types are available for configuration.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>ExecutorService Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Single thread</td>
<td class="">Creates an ExecutorService with a single thread.</td>
</tr>
<tr>
<td class="">Fixed thread</td>
<td class="">Creates an ExecutorService with a fixed number of threads.</td>
</tr>
<tr>
<td class="">Cached</td>
<td class="">Create an ExecutorService that will create new threads as needed and reuse existing threads when possible.</td>
</tr>
<tr>
<td class="">Work stealing</td>
<td class="">Creates a work-stealing thread pool using the number of available processors as its target parallelism level.</td>
</tr>
<tr>
<td class="">Custom</td>
<td class="">Allows the creation of non-standard executors.</td>
</tr>
</tbody>
</table>
</div>

<h4 id="_configuration_elements">Configuration Elements</h4>
<div class="section">

<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Element Name</th>
<th>Required</th>
<th>Expected Type</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">single</td>
<td class="">no</td>
<td class="">N/A</td>
<td class="">Defines a single-thread executor</td>
</tr>
<tr>
<td class="">fixed</td>
<td class="">no</td>
<td class="">N/A</td>
<td class="">Defines a fixed-thread-pool executor</td>
</tr>
<tr>
<td class="">cached</td>
<td class="">no</td>
<td class="">N/A</td>
<td class="">Defines a cached-thread-pool executor</td>
</tr>
<tr>
<td class="">work-stealing</td>
<td class="">no</td>
<td class="">N/A</td>
<td class="">Defines a work-stealing-pool executor</td>
</tr>
<tr>
<td class="">custom-executor</td>
<td class="">no</td>
<td class="">java.util.concurrent.ExecutorService</td>
<td class="">Defines a custom executor</td>
</tr>
<tr>
<td class="">name</td>
<td class="">yes</td>
<td class="">java.lang.String</td>
<td class="">Defines the logical <code>name</code> of the executor</td>
</tr>
<tr>
<td class="">thread-count</td>
<td class="">yes</td>
<td class="">java.lang.Integer</td>
<td class="">Defines the thread count for a <code>fixed</code> thread pool executor.</td>
</tr>
<tr>
<td class="">parallelism</td>
<td class="">no</td>
<td class="">java.lang.Integer</td>
<td class="">Defines the parallelism of a <code>work-stealing</code> thread pool executor.  If not defined, it will default to the number of processors available on the system.</td>
</tr>
<tr>
<td class="">thread-factory</td>
<td class="">no</td>
<td class="">N/A</td>
<td class="">Defines a java.util.concurrent.ThreadFactory.  Used by <code>single</code>, <code>fixed</code>, and <code>cached</code> executors.</td>
</tr>
<tr>
<td class="">instance</td>
<td class="">yes</td>
<td class="">Depending on the context, it will yield either a <code>java.util.concurrent.ExecutorService</code> or a <code>java.util.concurrent.ThreadFactory</code></td>
<td class="">Defines how the ThreadFactory or the ExecutorService will be instantiated.  See the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/cache-configuration-elements.html#GUID-D81B8574-CC8F-4AF1-BD0F-7068BC6432FD">docs</a> for details on the <code>instance</code> element.  This element must be a child of the <code>thread-factory</code> element.</td>
</tr>
</tbody>
</table>
</div>
<p>See the <a id="" title="" target="_blank" href="https://github.com/oracle/coherence/blob/master/prj/coherence-concurrent/src/main/resources/concurrent.xsd">schema</a> for full details.</p>


<h5 id="executors-configuration-examples">Configuration Examples</h5>
<div class="section">
<p>To define executors, the <code>cache-config</code> root element needs to include the <code>coherence-concurrent</code> NamespaceHandler in order to recognize the configuration elements.</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
               xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
               xmlns:c="class://com.oracle.coherence.concurrent.config.NamespaceHandler"
               xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd class://com.oracle.coherence.concurrent.config.NamespaceHandler concurrent.xsd"&gt; .
.
.
&lt;/cache-config&gt;</markup>

<div class="admonition tip">
<p class="admonition-inline">Executors defined by configuration must precede any other elements in the document.  Failing to do so, will prevent the document from validating.</p>
</div>
<p>The following examples assume the xml namespace defined for the NamespaceHandler is <code>c</code>:</p>

<markup
lang="xml"

>&lt;!-- creates a single-threaded executor named 'Single' --&gt;
&lt;c:single&gt;
  &lt;c:name&gt;Single&lt;/c:name&gt;
&lt;/c:single&gt;</markup>

<markup
lang="xml"

>&lt;!-- creates a single-threaded executor named `SingleTF` with a thread factor --&gt;
&lt;c:single&gt;
  &lt;c:name&gt;SingleTF&lt;/c:name&gt;
  &lt;c:thread-factory&gt;
    &lt;instance&gt;
      &lt;class-name&gt;my.custom.ThreadFactory&lt;/class-name&gt;
    &lt;/instance&gt;
  &lt;/c:thread-factory&gt;
&lt;/c:single&gt;</markup>

<markup
lang="xml"

>&lt;!-- creates a fixed-thread executor named 'Fixed5' with a thread-count of 5 --&gt;
&lt;c:fixed&gt;
  &lt;c:name&gt;Fixed5&lt;/c:name&gt;
  &lt;c:thread-count&gt;5&lt;/c:thread-count&gt;
&lt;/c:fixed&gt;</markup>

<markup
lang="xml"

>&lt;!-- creates a custom executor named 'custom' by calling com.acme.CustomExecutorFactory.createExecutor() --&gt;
&lt;c:custom-executor&gt;
  &lt;c:name&gt;custom&lt;/c:name&gt;
  &lt;instance&gt;
    &lt;class-factory-name&gt;com.acme.CustomExecutorFactory&lt;/class-factory-name&gt;
    &lt;method-name&gt;createExecutor&lt;/method-name&gt;
  &lt;/instance&gt;
&lt;/c:custom-executor&gt;</markup>

</div>
</div>

<h4 id="executors-management">Management</h4>
<div class="section">
<p>The ExecutorMBean represents the operational state of a registered executor.</p>

<p>The object name of the MBean is:</p>

<markup


>type=Executor,name=&lt;executor name&gt;,nodeId=&lt;cluster node&gt;</markup>


<h5 id="_executormbean_attributes">ExecutorMBean Attributes</h5>
<div class="section">

<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Attribute</th>
<th>Type</th>
<th>Access</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">MemberId</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The member ID where the executor is running.</td>
</tr>
<tr>
<td class="">Name</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The logical name of the executor.</td>
</tr>
<tr>
<td class="">Id</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The ID of the registered executor.</td>
</tr>
<tr>
<td class="">Description</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The generated description of the executor.</td>
</tr>
<tr>
<td class="">Location</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The complete location details of the executor.</td>
</tr>
<tr>
<td class="">State</td>
<td class="">java.lang.String</td>
<td class="">read-only</td>
<td class="">The current state of the executor.  May be one of <code>JOINING</code>, <code>RUNNING</code>, <code>CLOSING_GRACEFULLY</code>, <code>CLOSING</code>, <code>CLOSED</code> or <code>REJECTING</code>.</td>
</tr>
<tr>
<td class="">TaskCompletedCount</td>
<td class="">java.lang.Long</td>
<td class="">read-only</td>
<td class="">The number of tasks completed by this executor.</td>
</tr>
<tr>
<td class="">TaskRejectedCount</td>
<td class="">java.lang.Long</td>
<td class="">read-only</td>
<td class="">The number of tasks rejected by this executor.</td>
</tr>
<tr>
<td class="">TaskInProgressCount</td>
<td class="">java.lang.Long</td>
<td class="">read-only</td>
<td class="">The number of tasks currently running or pending to be run by this executor.</td>
</tr>
<tr>
<td class="">TraceLogging</td>
<td class="">java.lang.Boolean</td>
<td class="">read-write</td>
<td class="">Enables executor trace logging (WARNING! VERBOSE).  Disabled by default.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h5 id="_operations">Operations</h5>
<div class="section">
<p>The ExecutorMBean MBean includes a <code>resetStatistics</code> operation that resets the statistics
for this executor.</p>

</div>
</div>

<h4 id="executors-management-rest">Management over REST</h4>
<div class="section">
<p>Coherence Management over REST exposes endpoints to query and invoke actions against
ExecutorMBean instances.</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
<col style="width: 25%;">
</colgroup>
<thead>
<tr>
<th>Description</th>
<th>Method</th>
<th>Path</th>
<th>Produces</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">View all Executors</td>
<td class="">GET</td>
<td class="">/management/coherence/cluster/executors</td>
<td class="">JSON</td>
</tr>
<tr>
<td class="">View all Executors with matching name</td>
<td class="">GET</td>
<td class="">/management/coherence/cluster/executors/{name}</td>
<td class="">JSON</td>
</tr>
<tr>
<td class="">Reset Executor statistics by name</td>
<td class="">POST</td>
<td class="">/management/coherence/cluster/executors/{name}/resetStatistics</td>
<td class="">JSON</td>
</tr>
</tbody>
</table>
</div>
</div>

<h4 id="cdi-executors">CDI Support</h4>
<div class="section">
<p>RemoteExecutors may be injected via CDI.
For example:</p>

<markup
lang="java"

>@Inject
private RemoteExecutor single; <span class="conum" data-value="1" />

@Inject
@Name("Fixed5")
private RemoteExecutor fixedPoolRemoteExecutor; <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">injects a RemoteExecutor named <code>single</code>.</li>
<li data-value="2">injects a <code>RemoteExecutor</code> named <code>Fixed5</code>.</li>
</ul>
</div>
</div>

<h3 id="atomics">Atomics</h3>
<div class="section">
<p>Coherence Concurrent provides distributed implementations of atomic types, such as <code>AtomicInteger</code>, <code>AtomicLong</code> and <code>AtomicReference</code>. It also provides local implementations of the same types. The local implementations are just thin wrappers around existing <code>java.util.concurrent.atomic</code> types, which implement the same interface as their distributed variants, in order to be interchangeable.</p>

<p>To create instances of atomic types you need to call the appropriate factory method on the <code>Atomics</code> class:</p>

<markup
lang="java"

>AtomicInteger localFoo  = Atomics.localAtomicInteger("foo");   <span class="conum" data-value="1" />
AtomicInteger remoteFoo = Atomics.remoteAtomicInteger("foo");  <span class="conum" data-value="2" />
AtomicLong    remoteBar = Atomics.remoteAtomicLong("bar", 5L); <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">creates a local, in-process instance of named <code>AtomicInteger</code> with an implicit initial value of 0</li>
<li data-value="2">creates a remote, distributed instance of named <code>AtomicInteger</code>, distinct from the local instance <code>foo</code>, with an implicit initial value of 0</li>
<li data-value="3">creates a remote, distributed instance of named <code>AtomicLong</code>, with an initial value of 5</li>
</ul>
<p>Note that the <code>AtomicInteger</code> and <code>AtomicLong</code> types used above <em>are not</em> types from the <code>java.util.concurrent.atomic</code> package that you are familiar with&#8201;&#8212;&#8201;they are actually interfaces defined within <code>com.oracle.coherence.concurrent.atomic</code> package, that both <code>LocalAtomicXyz</code> and <code>RemoteAtomicXyz</code> classes implement, which are the instances that are actually returned by the methods above.</p>

<p>That means that the above code could be rewritten as:</p>

<markup
lang="java"

>LocalAtomicInteger  localFoo  = Atomics.localAtomicInteger("foo");
RemoteAtomicInteger remoteFoo = Atomics.remoteAtomicInteger("foo");
RemoteAtomicLong    remoteBar = Atomics.remoteAtomicLong("bar", 5L);</markup>

<p>However, we strongly suggest that you use interfaces instead of concrete types, as they make it easy to switch between local and distributed implementations when necessary.</p>

<p>Once created, these instances can be used the same way you would use any of the corresponding <code>java.util.concurrent.atomic</code> types:</p>

<markup
lang="java"

>int  counter1 = remoteFoo.incrementAndGet();
long counter5 = remoteBar.addAndGet(5L);</markup>


<h4 id="atomics-async">Asynchronous Implementations</h4>
<div class="section">
<p>The instances of numeric atomic types, such as <code>AtomicInteger</code> and <code>AtomicLong</code>, are frequently used to represent various counters in the application, where a client may need to increment the value, but doesn&#8217;t necessarily need to know what the new value is.</p>

<p>When working with the local atomics, the same API shown above can be used, and the return value simply ignored. However, when using distributed atomics that would introduce unnecessary blocking on the client while waiting for the response from the server, which would then simply be discarded. Obviously, this would have negative impact on both performance and throughput of the atomics.</p>

<p>To reduce the impact of remote calls in those situations, Coherence Concurrent also provides non-blocking, asynchronous implementations of all atomic types it supports.</p>

<p>To obtain a non-blocking instance of any supported atomic type, simply call <code>async</code> method on the blocking instance of that type:</p>

<markup
lang="java"

>AsyncAtomicInteger asyncFoo = Atomics.remoteAtomicInteger("foo").async();      <span class="conum" data-value="1" />
AsyncAtomicLong    asyncBar = Atomics.remoteAtomicLong("bar", 5L).async();     <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">creates a remote, distributed instance of named, non-blocking <code>AsyncAtomicInteger</code>, with an implicit initial value of 0</li>
<li data-value="2">creates a remote, distributed instance of named, non-blocking <code>AsyncAtomicLong</code>, with an initial value of 5</li>
</ul>
<p>Once created, these instances can be used the same way you would use any of the corresponding blocking types. The only difference is that they will simply return a <code>CompletableFuture</code> for  the result, and will not block:</p>

<markup
lang="java"

>CompletableFuture&lt;Integer&gt; futureCounter1 = asyncFoo.incrementAndGet();
CompletableFuture&lt;Long&gt;    futureCounter5 = asyncBar.addAndGet(5L);</markup>

<p>Both the blocking and the non-blocking instance of any distributed atomic type, with the same name, are backed by the same cluster-side atomic instance state, so they can be used interchangeably.</p>

</div>

<h4 id="cdi-atomics">CDI Support</h4>
<div class="section">
<p>Atomic types from Coherence Concurrent can also be injected using CDI, which eliminates the need for explicit factory method calls on the <code>Atomics</code> class.</p>

<markup
lang="java"

>@Inject
@Name("foo")
private AtomicInteger localFoo;   <span class="conum" data-value="1" />

@Inject
@Remote
@Name("foo")
private AtomicInteger remoteFoo;  <span class="conum" data-value="2" />

@Inject
@Remote
private AsyncAtomicLong asyncBar;  <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">injects a local, in-process instance of an <code>AtomicInteger</code> named <code>foo</code>, with an implicit initial value of 0</li>
<li data-value="2">injects a remote, distributed instance of an <code>AtomicInteger</code> named <code>foo</code>, distinct from the local instance <code>foo</code>, with an implicit initial value of 0</li>
<li data-value="3">injects a remote, distributed instance of non-blocking <code>AsyncAtomicLong</code>, with an implicit name of <code>asyncBar</code></li>
</ul>
<p>Once an instance of an atomic type is obtained via CDI injection, it can be used the same way as an instance obtained directly from the <code>Atomics</code> factory class.</p>

</div>
</div>

<h3 id="locks">Locks</h3>
<div class="section">
<p>Coherence Concurrent provides distributed implementations of <code>Lock</code> and <code>ReadWriteLock</code> interfaces from the <code>java.util.concurrent.locks</code> package, allowing you to implement lock-based concurrency control across cluster members when necessary.</p>

<p>Unlike local JDK implementations, the classes in this package use cluster member/process ID and thread ID to identify lock owner, and store shared lock state within a Coherence <code>NamedMap</code>. However, that also implies that the calls to acquire and release locks are remote, network calls, as they need to update shared state that is likely stored on a different cluster member, which will have an impact on performance of <code>lock</code> and <code>unlock</code> operations.</p>


<h4 id="exclusive-locks">Exclusive Locks</h4>
<div class="section">
<p>A <code>RemoteLock</code> class provides an implementation of a <code>Lock</code> interface and allows you to ensure that only one thread on one member is running critical section guarded by the lock at any given time.</p>

<p>To obtain an instance of a <code>RemoteLock</code>, call <code>Locks.remoteLock</code> factory method:</p>

<markup
lang="java"

>Lock foo = Locks.remoteLock("foo");</markup>

<p>Just like with <code>Atomics</code>, you can also obtain a local <code>Lock</code> instance from the <code>Locks</code> class, with will simply return an instance of a standard <code>java.util.concurrent.locks.ReentrantLock</code>, by calling <code>localLock</code> factory method:</p>

<markup
lang="java"

>Lock foo = Locks.localLock("foo");</markup>

<p>Once you have a <code>Lock</code> instance, you can use it as you normally would:</p>

<markup
lang="java"

>foo.lock();
try {
    // critical section guarded by the exclusive lock `foo`
}
finally {
    foo.unlock();
}</markup>

</div>

<h4 id="read-write-locks">Read/Write Locks</h4>
<div class="section">
<p>A <code>RemoteReadWriteLock</code> class provides an implementation of a <code>ReadWriteLock</code> interface and allows you to ensure that only one thread on one member is running critical section guarded by the write lock at any given time, while allowing multiple concurrent readers.</p>

<p>To obtain an instance of a <code>RemoteReadWriteLock</code>, call <code>Locks.remoteReadWriteLock</code> factory method:</p>

<markup
lang="java"

>ReadWriteLock bar = Locks.remoteReadWriteLock("bar");</markup>

<p>Just like with <code>Atomics</code>, you can also obtain a local <code>ReadWriteLock</code> instance from the <code>Locks</code> class, with will simply return an instance of a standard <code>java.util.concurrent.locks.ReentrantReadWriteLock</code>, by calling <code>localReadWriteLock</code> factory method:</p>

<markup
lang="java"

>ReadWriteLock bar = Locks.localReadWriteLock("bar");</markup>

<p>Once you have a <code>ReadWriteLock</code> instance, you can use it as you normally would:</p>

<markup
lang="java"

>bar.writeLock().lock();
try {
    // critical section guarded by the exclusive write lock `bar`
}
finally {
    bar.writeLock().unlock();
}</markup>

<p>Or:</p>

<markup
lang="java"

>bar.readLock().lock();
try {
    // critical section guarded by the shared read lock `bar`
}
finally {
    bar.readLock().unlock();
}</markup>

</div>

<h4 id="cdi-locks">CDI Support</h4>
<div class="section">
<p>You can also use CDI to inject both the exclusive and read/write lock instances into objects that need them:</p>

<markup
lang="java"

>@Inject
@Remote
@Name("foo")
private Lock lock;           <span class="conum" data-value="1" />

@Inject
@Remote
private ReadWriteLock bar;   <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">injects distributed exclusive lock named <code>foo</code> into <code>lock</code> field</li>
<li data-value="2">injects distributed read/write lock named <code>bar</code> into <code>bar</code> field</li>
</ul>
<p>Once an instance of lock is obtained via CDI injection, it can be used the same way as an instance obtained directly from the <code>Locks</code> factory class.</p>

</div>
</div>

<h3 id="latches-semaphores">Latches and Semaphores</h3>
<div class="section">
<p>Coherence Concurrent also provides distributed implementations of a <code>CountDownLatch</code> and <code>Semaphore</code> classes from <code>java.util.concurrent</code> package, allowing you to implement synchronization of execution across multiple Coherence cluster members as easily as you can implement it within a single process using those two JDK classes. It also provides interfaces for those two concurrency primitives, that both remote and local implementations conform to.</p>

<p>Just like with atomics, the local implementations are nothing more than thin wrappers around corresponding JDK classes.</p>


<h4 id="count-down-latch">Count Down Latch</h4>
<div class="section">
<p>The <code>RemoteCoundDownLatch</code> class provides a distributed implementation of a <code>CountDownLatch</code>, and allows you to ensure that the execution of the code on any cluster member that is waiting for the latch proceeds only when the latch reaches zero. Any cluster member can both wait for a latch, and count down.</p>

<p>To obtain an instance of a <code>RemoteCountDownLatch</code>, call <code>Latches.remoteCountDownLatch</code> factory method:</p>

<markup
lang="java"

>CoundDownLatch foo = Latches.remoteCountDownLatch("foo", 5);     <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">create an instance of a <code>RemoteCountDownLatch</code> with the initial count of 5</li>
</ul>
<p>Just like with <code>Atomics</code> and <code>Locks</code>, you can also obtain a local <code>CountDownLatch</code> instance from the <code>Latches</code> class by calling <code>remoteCountDownLatch</code> factory method:</p>

<markup
lang="java"

>CoundDownLatch foo = Latches.localCountDownLatch("foo", 10);     <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">create an instance of a <code>LocalCountDownLatch</code> with the initial count of 10</li>
</ul>
<p>Once you have a <code>RemoteCountDownLatch</code> instance, you can use it as you normally would, by calling <code>countDown</code> and <code>await</code> methods on it.</p>

</div>

<h4 id="semaphore">Semaphore</h4>
<div class="section">
<p>The <code>RemoteSemaphore</code> class provides a distributed implementation of a <code>Semaphore</code>, and allows any cluster member to acquire and release permits from the same semaphore instance.</p>

<p>To obtain an instance of a <code>RemoteSemaphore</code>, call <code>Semaphores.remoteSemaphore</code> factory method:</p>

<markup
lang="java"

>Semaphore foo = Semaphores.remoteSemaphore("foo", 5);            <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">create an instance of a <code>RemoteSemaphore</code> with 5 permits</li>
</ul>
<p>Just like with <code>Atomics</code> and <code>Locks</code>, you can also obtain a local <code>Semaphore</code> instance from the <code>Semaphores</code> class by calling <code>localSemaphore</code> factory method:</p>

<markup
lang="java"

>Semaphore foo = Semaphores.localSemaphore("foo");                <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">create an instance of a <code>LocalSemaphore</code> with 0 permits</li>
</ul>
<p>Once you have a <code>Semaphore</code> instance, you can use it as you normally would, by calling <code>release</code> and <code>acquire</code> methods on it.</p>

</div>

<h4 id="cdi-latches-semaphores">CDI Support</h4>
<div class="section">
<p>You can also use CDI to inject both the <code>CountDownLatch</code> and <code>Semaphore</code> instances into objects that need them:</p>

<markup
lang="java"

>@Inject
@Name("foo")
@Count(5)
private CountDownLatch localLatchFoo;           <span class="conum" data-value="1" />

@Inject
@Name("foo")
@Remote
@Count(10)
private CountDownLatch remoteLatchFoo;          <span class="conum" data-value="2" />

@Inject
@Name("bar")
@Remote
private Semaphore localSemaphoreBar;            <span class="conum" data-value="3" />

@Inject
@Name("bar")
@Remote
@Permits(1)
private Semaphore remoteSemaphoreBar;           <span class="conum" data-value="4" /></markup>

<ul class="colist">
<li data-value="1">inject an instance of a <code>LocalCountDownLatch</code> with the initial count of five</li>
<li data-value="2">inject an instance of a <code>RemoteCountDownLatch</code> with the initial count of ten</li>
<li data-value="3">inject an instance of a <code>LocalSemaphore</code> with zero permits available</li>
<li data-value="4">inject an instance of a <code>RemoteSemaphore</code> with one permit available</li>
</ul>
<p>Once a latch or a semaphore instance is obtained via CDI injection, it can be used the same way as an instance obtained directly from the <code>Latches</code> or <code>Semaphores</code> factory classes.</p>

<p>The <code>@Name</code> annotation is optional in both cases, as long as the member name (in the examples above, the field name) can be obtained from the injection point, but is required otherwise (such as when using constructor injection).</p>

<p>The <code>@Count</code> annotation specifies the initial latch count, and if omitted will be defaulted to one. The <code>@Permits</code> annotation specifies the number of available permits for a semaphore, and if omitted will be defaulted to zero, which means that the first <code>acquire</code> call will block until another thread releases one or more permits.</p>

</div>
</div>
</div>
</doc-view>
