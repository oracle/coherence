<doc-view>

<h2 id="_virtual_threads_support">Virtual Threads Support</h2>
<div class="section">
<p>Starting with Coherence CE 23.09, Coherence uses virtual threads for all <strong>dynamic</strong> daemon pools, when running on Java 21 or later. Considering that all Coherence services use dynamic daemon pool by default, this means that you will likely benefit from the switch to virtual threads without having to change anything other than the Java and Coherence version you use.</p>


<h3 id="_benefits">Benefits</h3>
<div class="section">
<p>The main benefit virtual threads provide is that they don&#8217;t tie up platform/hardware threads while waiting for a blocking operation to complete. While most operations in Coherence never block, there are some very important ones that do, such as <code>put</code> (while waiting for a backup, write to persistent store, or write-through to a database to complete), and sometimes even <code>get</code> (while waiting for a read-through from a database).</p>

<p>Another important example are Extend proxies, which translate remote client requests into cluster requests, and are effectively cluster clients which block on pretty much every proxied operation.</p>

<p>Using dynamic daemon pools, and thus virtual threads in any of these scenarios makes a lot of sense, as it allows us to significantly grow daemon pool when necessary, without paying the cost associated with starting many platform threads. This can help us achieve significantly higher throughput under heavy load, and reduce the amount of time requests spend in a queue waiting for a daemon thread to free up to process them.</p>

</div>

<h3 id="_downsides">Downsides</h3>
<div class="section">
<p>Of course, there is no such thing as free lunch, and virtual threads are no exception.</p>

<p>The biggest downside we see is that all virtual threads share the same pool of carrier platform threads to perform their processing on, and this pool can get quite busy. If you have some heavily used, critical services that you want to completely isolate and tune separately from other services, you may want to disable virtual threads either by switching to a fixed-size daemon pool, or via configuration, which I&#8217;ll discuss in a minute.</p>

<p>Another potential issue we have seen is that there are some libraries, especially ones that write to file system directly, that don&#8217;t play nice with virtual threads and can cause excessive pinning of carrier threads and even deadlocks. If you run into situation like that, it also makes sense to disable virtual threads, either for the service in question, or globally.</p>

<p>Finally, the tooling related to virtual threads is not quite there yet. It is hard to see what&#8217;s happening where in the debugger, profiler, and other tools you may use during development or production to better understand what&#8217;s happening in your application. In those cases it may also be beneficial to disable virtual threads, even if only temporary.</p>

</div>

<h3 id="_enabling_and_disabling_virtual_threads">Enabling and Disabling Virtual Threads</h3>
<div class="section">
<p>Because of these issues, and because any other unanticipated issues that may arise with such a promising, but very new technology, we have decided to provide a way to disable virtual threads, either at the service level or globally, using configuration properties.</p>

<p>To disable virtual threads for all services, you can specify <code>-Dcoherence.virtualthreads.enabled=false</code> system property when starting your Coherence JVMs (or an equivalent of that property in your Helidon, Spring, or Micronaut configuration file).</p>

<p>To disable virtual threads for a specific service, you can specify <code>-Dcoherence.service.&lt;serviceName&gt;.virtualthreads.enabled=false</code> configuration property. This will allow you to continue to use dynamic daemon pool, but to use standard platform threads instead of virtual threads.</p>

<p>Finally, you can combine the two settings, and disable virtual threads globally, but then selectively enable them for some services (Extend proxy, for example) by specifying config property such as <code>-Dcoherence.service.Proxy.virtualthreads.enabled=true</code> on the command line or in the config file.</p>

<p>Ultimately, we believe that virtual threads are an amazing addition to Java that will alleviate many issues our customers are currently facing and make Coherence applications better, faster, more scalable and much easier to configure and tune in a long run. However, we also understand that they are very new, and may not work for all possible workloads.</p>

<p>For those reasons, we have made them the default, but gave you the ability to disable them if necessary or desired.</p>

</div>
</div>
</doc-view>
