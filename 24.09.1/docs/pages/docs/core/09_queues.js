<doc-view>

<h2 id="_queues">Queues</h2>
<div class="section">
<p>Starting with Coherence CE 24.03, Coherence supports Queues as data structure.
The Coherence <code>NamedQueue</code> is an implementation of <code>java.util.Queue</code> and <code>NamedDeque</code> is and
implementation of <code>java.util.Deque</code>.</p>

<p>Coherence has two implementations of <code>BlockingQueue</code>, one is a simple size limited queue, the second is a distributed paged queue that has a much larger capacity. The simple queue is available as both a <code>BlockingQueue</code> and a double ended <code>BlockingDeque</code>. The distributed paged queue is only available as a <code>BlockingQueue</code> implementation.</p>

<div class="admonition important">
<p class="admonition-textlabel">Important</p>
<p ><p>Coherence queues are mapped to caches with the same name as the queue.
If a cache is being used for a queue the same cache must not also be used as a normal data cache.</p>
</p>
</div>

<h3 id="_blocking_queue">Blocking Queue</h3>
<div class="section">
<p>The Coherence Concurrent module contains an implementation of <code>java.util.concurrent.BlockingQueue</code> called <code>NamedBlockingQueue</code> and an implementation of <code>java.util.concurrent.BlockingDeque</code> called <code>NamedBlockingDeque</code>.</p>

<p>To use the Coherence blocking queue or deque in your application you need to add a dependency on
the <code>coherence-concurrent</code> module.</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-concurrent&lt;/artifactId&gt;
        &lt;version&gt;24.09.1&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>The blocking queue implementations work by using Coherence events. When application code calls a blocking method, the calling thread will be blocked, the blocking is not on the server. The application code will become unblocked when it receives an event from the server.</p>

<p>For example, if application code calls the <code>NamedBlockingQueue</code> <code>take()</code> method, and the queue is empty, this method will block the calling thread. When an element is put into the queue by another thread (maybe on another JVM) the calling application will receive an event. This will retry the <code>take()</code> and if successful will return. If the retry of the <code>take()</code> is unsuccessful the calling thread remains blocked (for example another thread or another JVM was also blocked taking from the same queue and managed to get its retry in first.</p>

<p>Another example would be an application calling <code>NamedBlockingQueue</code> <code>put()</code> method, which will block when the queue is full (i.e. reaches the 2GB size limit). In this case the calling thread will be blocked until a delete event is received to signal that there is now space in the queue. The <code>put()</code> will be retried and if successful control returned to the calling thread. If the retry is unsuccessful the thread will remain blocked (for example another thread or JVM was also blocked on a <code>put()</code> and its retry succeeded and refilled the queue).</p>

<p>To obtain an instance of a blocking queue use the <code>com.oracle.coherence.concurrent.Queues</code> factory class.</p>


<h4 id="_simple_size_limited_queue_and_deque">Simple Size Limited Queue and Deque</h4>
<div class="section">
<p>This implementation of a queue does not scale the same way as a Coherence cache or a Coherence topic does. In the current queue implementation, all the data for the queue is stored in a single Coherence partition. This means that the total size of the elements in a single queue instance cannot be larger than 2GB. This is a limitation of Coherence where a partition for a cache cannot exceed 2GB, as that is the limit of the buffer size used to transfer data between cluster members. If Coherence queues allowed a partition to grow over 2GB, the queue would work fine up to the point where Coherence needed to transfer that partition to a new member on fail-over, at that point data would be lost.</p>

<p>The Coherence server will refuse to accept offers to the queue if its size will exceed 2GB.
The <code>java.util.Queue</code> contact allows for queues to reject offers, so this size limitation conforms to the queue contract. Application developers should check the response from offering data to the queue to determine whether the offer has succeeded or not.
We use the term "offer" here to cover all queue and deque methods that add data to the queue.
An alternative to checking the return boolean from an <code>offer()</code> call would be to use a <code>NamedBlockingQueue</code> where the <code>put()</code> method will block if the queue is full,</p>

<p>To obtain an instance of the simple size limited queue from the <code>Queues</code> factory class there are two methods, dependeing on whether a queue or deque is required.</p>

<p>For example to obtain a <code>BlockingQueue</code> named "my-queue":</p>

<markup
lang="java"

>NamedBlockingQueue&lt;String&gt; queue = Queues.queue("my-queue");</markup>

<p>For example to obtain a <code>BlockingDeque</code> named "my-deque":</p>

<markup
lang="java"

>NamedBlockingDeque&lt;String&gt; queue = Queues.deque("my-deque");</markup>

</div>

<h4 id="_distributed_paged_queue">Distributed Paged Queue</h4>
<div class="section">
<p>To obtain an instance of a distributed paged queue, from the <code>Queues</code> factory class use the <code>pagedQueue</code> method.</p>

<markup
lang="java"

>NamedBlockingQueue&lt;String&gt; queue = Queues.pagedQueue("my-queue");</markup>

</div>
</div>
</div>

<h2 id="_sizing_queues">Sizing Queues</h2>
<div class="section">
<p>It is important to understand how the two Coherence queue implementations store data and how this limits the size of a queue.</p>

<p>Simple Coherence Queue – the simple queue (and deque) implementation stores data in a single Coherence cache partition. This enforces a size limit of 2GB because a Coherence cache partition should not exceed 2GB in size, and in reality, a partition should be a lot smaller than this. Large partitions slow down recovery when a storage enabled member leaves the cluster. With a modern fast network 300Mb – 500MB should be a suitable maximum partition size, on a 10Gb network this could even go as high as 1GB.</p>

<p>The distributed paged queue stores data in pages that are distributed around the Coherence cluster over multiple partitions, the same as normal cache data. This means that the paged queue can store far more than 2GB. It is still important to be aware of how partition sizes limit the total queue size.</p>

<p>The absolute hard limit of 2GB per partition give the following size:</p>

<p>2GB x 257 = 514GB</p>

<p>But this is far too big to be reliable in production use. If we take a size limit of 500MB and the default partition count of 257 we can see how this affects queue size.</p>

<p>500MB x 257 = 128GB</p>

<p>So, by default a realistic limit for a paged queue is around 128GB. If the partition count is increased to 1087 the queue size becomes:</p>

<p>500MB x 1087 = 543GB</p>

<p>Of course, all these examples assume that there are enough JVMs with big enough heap sizes in the cluster to store the queue data in memory.</p>

</div>

<h2 id="_limitations">Limitations</h2>
<div class="section">
<p>The current queue implementation in Coherence has some limitations detailed below.</p>

<p>In normal operation queues should not get huge, this would usually mean that the processes reading from the queue are not keeping up with the processes writing to the queue. Application developers should obviously load test their applications using queues to ensure that they are not going to have issues with capacity.</p>

<p>Queue operations such as offering and polling will contend on certain data structures and this will limit the number of parallel requests and how fast requests can be processed. In order to maintain ordering, polling contends on either the head or tail entry, depending on which end of the queue is being polled. This means that poll methods can only be processed sequentially, so even though a poll is efficient and fast, a large number of concurrent poll requests will queue and be processed one at a time. Offer methods do not contend on the head or tail, but will contend on the atomic counters used to maintain the head and tail identifiers. So, whilst Coherence can process multiple offer requests on different worker threads, there will be minor contention on the <code>AtomicLong</code> updates.</p>

<p>Queue operations that work on the head and tail such as offering and polling are efficient. Some of the other methods in <code>java.util.Queue</code> and <code>java.util.Deque</code> are less efficient. For example, iterator methods, <code>contains()</code> and so on. These are not frequently used by applications that require basic queue functionality. Some optional methods on the <code>java.util.Queue</code> API that mutate the queue will throw UnsupportedOperationException (this is allowed by the Java Queue contract), for example <code>retainAll()</code>, <code>removeAll()</code>, and removal using an iterator.</p>

</div>
</doc-view>
