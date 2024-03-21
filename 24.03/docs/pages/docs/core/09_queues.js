<doc-view>

<h2 id="_queues">Queues</h2>
<div class="section">
<p>Starting with Coherence CE 24.03, Coherence supports Queues as data structure.
The Coherence <code>NamedQueue</code> is an implementation of <code>java.util.Queue</code> and <code>NamedDeque</code> is and
implementation of <code>java.util.Deque</code>.</p>

<div class="admonition important">
<p class="admonition-textlabel">Important</p>
<p ><p>Coherence queues are mapped to caches with the same name as the queue.
If a cache is being used for a queue the same cache should not also be used as a normal data cache.</p>
</p>
</div>

<h3 id="_blocking_queue">Blocking Queue</h3>
<div class="section">
<p>The Coherence Concurrent module contains an implementation of <code>java.util.concurrent.BlockingQueue</code> called <code>NamedBlockingQueue</code> and an implementation of <code>java.util.concurrent.BlockingDeque</code> called <code>NamedBlockingDeque</code>.</p>

<p>To use a Coherence blocking queue in your application you need to add a dependency on
the <code>coherence-concurrent</code> module.</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-concurrent&lt;/artifactId&gt;
        &lt;version&gt;24.03&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>To obtain an instance of a blocking queue use the <code>com.oracle.coherence.concurrent.Queues</code> factory class.</p>

<p>For example to obtain a <code>BlockingQueue</code> named "my-queue":</p>

<markup
lang="java"

>NamedBlockingQueue&lt;String&gt; queue = Queues.blocking("my-queue");</markup>

<p>For example to obtain a <code>BlockingDeque</code> named "my-deque":</p>

<markup
lang="java"

>NamedBlockingDeque&lt;String&gt; queue = Queues.blockingDeque("my-deque");</markup>

<p>The blocking queue implementations work by using Coherence events. When application code calls a blocking method, the calling thread will be blocked, the blocking is not on the server. The application code will become unblocked when it receives an event from the server.</p>

<p>For example, if application code calls the <code>NamedBlockingQueue</code> <code>take()</code> method, and the queue is empty, this method will block the calling thread. When an element is put into the queue by another thread (maybe on another JVM) the calling application will receive an event. This will retry the <code>take()</code> and if successful will return. If the retry of the <code>take()</code> is unsuccessful the calling thread remains blocked (for example another thread or another JVM was also blocked taking from the same queue and managed to get its retry in first.</p>

<p>Another example would be an application calling <code>NamedBlockingQueue</code> <code>put()</code> method, which will block when the queue is full (i.e. reaches the 2GB size limit). In this case the calling thread will be blocked until a delete event is received to signal that there is now space in the queue. The <code>put()</code> will be retried and if successful control returned to the calling thread. If the retry is unsuccessful the thread will remain blocked (for example another thread or JVM was also blocked on a <code>put()</code> and its retry succeeded and refilled the queue).</p>

</div>
</div>

<h2 id="_limitations">Limitations</h2>
<div class="section">
<p>The current queue implementation in Coherence has some limitations detailed below.</p>

<p>This implementation of a queue does not scale the same way as a Coherence cache or a Coherence topic does. In the current queue implementation, all the data for the queue is stored in a single Coherence partition. This means that the total size of the elements in a single queue instance cannot be larger than 2GB. This is a limitation of Coherence where a partition for a cache cannot exceed 2GB, as that is the limit of the buffer size used to transfer data between cluster members. If Coherence queues allowed a partition to grow over 2GB, the queue would work fine up to the point where Coherence needed to transfer that partition to a new member on fail-over, at that point data would be lost.</p>

<p>The Coherence server will refuse to accept offers to the queue if its size will exceed 2GB.
The <code>java.util.Queue</code> contact allows for queues to reject offers, so this size limitation conforms to the queue contract. Application developers should check the response from offering data to the queue to determine whether the offer has succeeded or not.
We use the term "offer" here to cover all queue and deque methods that add data to the queue.
An alternative to checking the return boolean from an <code>offer()</code> call would be to use a <code>NamedBlockingQueue</code> where the <code>put()</code> method will block if the queue is full,</p>

<p>In normal operation queues should not get huge, this would usually mean that the processes reading from the queue are not keeping up with the processes writing to the queue. Application developers should obviously load test their applications using queues to ensure that they are not going to have issues with capacity.</p>

<p>Queue operations such as offering and polling will contend on certain data structures and this will limit the number of parallel requests and how fast requests can be processed. In order to maintain ordering, polling contends on either the head or tail entry, depending on which end of the queue is being polled. This means that poll methods can only be processed sequentially, so even though a poll is efficient and fast, a large number of concurrent poll requests will queue and be processed one at a time. Offer methods do not contend on the head or tail, but will contend on the atomic counters used to maintain the head and tail identifiers. So, whilst Coherence can process multiple offer requests on different worker threads, there will be minor contention on the <code>AtomicLong</code> updates.</p>

<p>Queue operations that work on the head and tail, such as offering and polling are efficient. Some of the other methods in <code>java.util.Queue</code> and <code>java.util.Deque</code> are less efficient, for example iterators, <code>retainAll()</code>, <code>removeAll()</code>, <code>contains()</code> etc, but these are not frequently used by applications that require basic queue functionality.</p>

</div>
</doc-view>
