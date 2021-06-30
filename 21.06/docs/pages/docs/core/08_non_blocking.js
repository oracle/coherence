<doc-view>

<h2 id="_non_blocking_data_sources">Non Blocking Data Sources</h2>
<div class="section">
<p>Coherence provides a means of <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/caching-data-sources.html#GUID-9FAD1BFB-5063-4995-B0A7-3C6F9C64F600">integrating with underlying data sources</a> using a number of existing strategies; namely, read-through, write-through, write-behind and refresh-ahead.</p>

<p>Coherence now also provides a <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/NonBlockingEntryStore.html">NonBlockingEntryStore</a> interface for integrating with data sources that provide non-blocking APIs.
This new strategy is similar in nature to write-behind, as it is asynchronous to the original mutation, however does not require a queue to defer the call to the store and immediately passes the intent to store to the implementer.
The implementer can in-turn immediately call the non-blocking api of the data source and on success or failure a future can pass that information to the provided <code>StoreObserver</code> via <code>onNext</code> or <code>onError</code> respectively.
The primary methods of the <code>NonBlockingEntryStore</code> are highlighted below:</p>

<markup
lang="java"

>public interface NonBlockingEntryStore&lt;K, V&gt;
    {
    public void load(BinaryEntry&lt;K, V&gt; binEntry, StoreObserver&lt;K, V&gt; observer);

    public void store(BinaryEntry&lt;K, V&gt; binEntry, StoreObserver&lt;K, V&gt; observer);

    public void erase(BinaryEntry&lt;K, V&gt; binEntry);
    }</markup>

<p>There are similar methods to the above in existing <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/CacheStore.html">CacheStore</a> and <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/BinaryEntryStore.html">BinaryEntryStore</a> interfaces, however with the <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/NonBlockingEntryStore.html">NonBlockingEntryStore</a> interface the calls are non-blocking thus Coherence does not expect the operation to be completed when control is returned. To allow the implementer to notify Coherence of operation completion, the <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/StoreObserver.html">StoreObserver</a> is provided and <strong>must</strong> be called upon success or failure. This allows Coherence to process the result of the operation.</p>

<p>It is worth pointing out that similar to the write-behind strategy upon failure, and therefore restore of primary partitions, Coherence will call <code>NonBlockingEntryStore.store</code> for the entries it did not receive a success or error notification for. This provides at least once semantics allowing implementers to call the non-blocking data source if deemed necessary.</p>

<p>The diagrams below illustrate the flow from the initial request, to the invocation of the NonBlockingEntryStore on the storage enabled nodes:</p>

<ul class="ulist">
<li>
<p>for a <code>get()</code> operation inducing a load:</p>

</li>
</ul>
<p><img src="docs/images/08_non_blocking_load.png" alt="08 non blocking load"width="80%" />
</p>

<ul class="colist">
<li data-value="1">the application calls <code>get()</code> on entry <code>A</code> that is not in the cache yet.</li>
<li data-value="2">a request goes to the storage member that owns the entry, in this instance JVM2. Entry ownership, and thus partition ownership, is determined algorithmically based on the raw (or binary) value of the key and the number of partitions the associated partitioned service is configured with.
Since it has not been accessed yet or has expired, a miss takes place and the call is relayed to the configure entry store.</li>
<li data-value="3">the <code>load()</code> operation for the entry store that implements <code>NonBlockingEntryStore</code> is called; custom logic is provided a <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/util/BinaryEntry.html">BinaryEntry</a> with a null initial value
and a <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/StoreObserver.html">StoreObserver</a>. The implementer performs the datastore operation(s) necessary to populate the cache entry.</li>
<li data-value="4">When the operation on the underlying data source completes, the implementation will call either <code>observer.onNext</code> or <code>observer.onError</code>, whether the value was successfully loaded or not.
The implementer will update the <code>BinaryEntry</code> via <code>setValue</code> or <code>updateBinaryValue</code>, prior to calling <code>onNext</code>. This will allow Coherence to ensure data is inserted in the primary partition owner (JVM2) and backed up accordingly.</li>
<li data-value="5">the primary partition owner sends the value to another storage member in the cluster for backup purposes.</li>
<li data-value="6">the entry value is sent back to the calling application where a transient reference is kept.
Note that although the data source operation can be performed asynchronously and the call to <code>load()</code> does not need to wait for its completion to return, the <code>get()</code> invocation is synchronous from the caller&#8217;s perspective.</li>
</ul>

<ul class="ulist">
<li>
<p>for a <code>put()</code> operation:</p>

</li>
</ul>
<p><img src="docs/images/08_non_blocking_store.png" alt="08 non blocking store"width="80%" />
</p>

<ul class="colist">
<li data-value="1">the application calls <code>put()</code> on entry <code>A</code> with value <code>A</code>.</li>
<li data-value="2">the entry is stored on the owning member.</li>
<li data-value="3">since the cache is configured with a <code>NonBlockingEntryStore</code>, the <code>store()</code> operation is called. <code>store()</code> is provided a <code>BinaryEntry</code> and a a <a id="" title="" target="_blank" href="https://coherence.community/21.06/api/java//com/tangosol/net/cache/StoreObserver.html">StoreObserver</a>.
The implementer performs the datastore operation(s) necessary to save the cache entry into a datastore.</li>
<li data-value="4">at this point, the <code>store()</code> call of the <code>NonBlockingEntryStore</code> can return, and <code>put()</code> will then give control back to the calling application.</li>
<li data-value="5">the datastore asynchronously performs the datastore operation(s) necessary to save the cache entry into a datastore, then calls the <code>observer.onNext()</code> method for normal operations (or <code>observer.onError()</code> in case of a problem).
If necessary (for example, the value of the <code>BinaryEntry</code> has been updated), the value is put back into the cache.</li>
<li data-value="6">the value is then sent to the backup owning member for safekeeping.</li>
</ul>

<ul class="ulist">
<li>
<p><code>getAll()</code> functions comparably to <code>get()</code>, except it processes a set of entries.
This provides an opportunity for an implementer to optimize batch operations (multi-entry) against the datasource thus reduce the communication overhead with the datasource.
Once the associated entry has been successfully written the implementer must call <code>StoreObserver.onNext</code> passing the relevant entry (or <code>onError()</code> if an error occurred processing this particular entry).</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>Coherence expects all entries to be processed before concluding.</p>
</p>
</div>
<ul class="ulist">
<li>
<p><code>putAll()</code> also functions comparably to <code>put()</code>, except on a set of entries. The same expectation is in effect here: either all entries are processed using <code>onNext()/onError()</code>, or <code>onComplete()</code> can be used to interrupt the operation. The difference with <code>putAll()</code> is that the caller will not wait for completion, thus any exception will not be thrown but printed out in the log.</p>

</li>
<li>
<p>the <code>remove()</code> operation functions in the same way as CacheStore or BinaryEntryStore from the application standpoint.</p>

</li>
</ul>
<p>Besides providing a natural way of integrating with non-blocking data stores, this model takes advantage of the benefits of such stores in terms of performance and scalability.</p>


<h3 id="_nonblockingentrystore">NonBlockingEntryStore</h3>
<div class="section">
<p>Certain data source libraries have APIs that do not necessitate the caller to wait for the result to come back before doing something else. For example, making HTTP calls can lead to relatively long waits between the time a request to store data is sent and the response comes back. By implementing non-blocking APIs, the caller can immediately do other work without having to wait for the actual store operation to complete.</p>

<p>By implementing the <code>NonBlockingEntryStore</code> interface, the store implementer will be able to use non-blocking APIs in a more natural way.</p>

<p><code>NonBlockingEntryStore</code> is being provided in the context of <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/caching-data-sources.html#GUID-6F84A2D6-43FE-4852-B48F-2A250CABEB36">pluggable data stores</a>: in order to use it, an implementation class needs to be provided and configured. This class will either load, store or remove data from the data source by way of a <code>ReadWriteBackingMap</code>. This backing map provides two elements: an internal map to cache the data, and a data source access portion to interact with the data base.</p>

<p>The <code>NonBlockingEntryStore</code> interface is provided the <code>BinaryEntry</code> that represents the <code>load</code>, <code>store</code> or <code>erase</code> operation. This provides an opportunity for implementers to avoid deserialization if desired; this is similar to <code>BinaryEntryStore</code>. Avoiding deserialization generally is possible if the raw binary is stored in the downstream system, or the binary can be navigated to extract relevant parts, as opposed to deserializing the entire key or value. Note: <code>getKey</code>, <code>getValue</code> and <code>getOriginalValue</code> will induce deserialization for the first call.</p>

</div>

<h3 id="_how_to_use">How to Use</h3>
<div class="section">

<h4 id="_configuration">Configuration</h4>
<div class="section">
<p>To specify a non-blocking cache store implementation, provide the implementation class name within the read-write-backing-map-scheme as shown below.</p>

<markup
lang="xml"

>...
&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
   xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config
   coherence-cache-config.xsd"&gt;
    &lt;cache-mapping&gt;
...
        &lt;cache-name&gt;myCache&lt;/cache-name&gt;
        &lt;scheme-name&gt;distributed-rwbm-nonblocking&lt;/scheme-name&gt;
...
    &lt;/cache-mapping&gt;

    &lt;distributed-scheme&gt;
...
        &lt;scheme-name&gt;distributed-rwbm-nonblocking&lt;/scheme-name&gt;
        &lt;backing-map-scheme&gt;
            &lt;read-write-backing-map-scheme&gt;

                &lt;cachestore-scheme&gt;
                    &lt;class-scheme&gt;
                        &lt;class-name&gt;com.company.NonBlockingStoreImpl&lt;/class-name&gt;
                    &lt;/class-scheme&gt;
                &lt;/cachestore-scheme&gt;

            &lt;/read-write-backing-map-scheme&gt;
        &lt;/backing-map-scheme&gt;
        &lt;autostart&gt;true&lt;/autostart&gt;
...
    &lt;/distributed-scheme&gt;

&lt;/cache-config&gt;</markup>

</div>

<h4 id="_implementation">Implementation</h4>
<div class="section">
<p>Once configured, a class implementing the <code>NonBlockingEntryStore</code> interface needs to be added to the added to the classpath of the storage enabled members. See below for example code.</p>

<p>With the class in place, the equivalency below is established:</p>

<ul class="ulist">
<li>
<p><code>get()</code> -invokes&#8594; <code>load()</code> Note: If data is already in the cache, <code>load()</code> does not get called. Also, calling <code>get()</code> will wait for <code>onNext()</code>/<code>onError()</code> to complete before returning.</p>

</li>
<li>
<p><code>getAll()</code> -invokes&#8594; <code>loadAll()</code></p>

</li>
<li>
<p><code>put()</code> -invokes&#8594; <code>store()</code></p>

</li>
<li>
<p><code>putAll()</code> -invokes&#8594; <code>storeAll()</code></p>

</li>
<li>
<p><code>remove()</code> -invokes&#8594; <code>erase()</code></p>

</li>
<li>
<p><code>removeAll()</code> -invokes&#8594; <code>eraseAll()</code></p>

</li>
</ul>
<p>The code below contains portions of code is using a reactive API to access a data source.</p>

<markup
lang="java"

>...
/**
 * An example NonBlockingEntryStore implementation
 */
public class ExampleNonBlockingEntryStore&lt;K, V&gt;
    {
    @Override
    public void load(BinaryEntry&lt;K, V&gt; binEntry, StoreObserver&lt;K, V&gt; observer)
        {
        K key = binEntry.getKey();

        Flux.from(getConnection())
                .flatMap(connection -&gt; connection.createStatement(LOAD_STMT)
                        .bind("$1", key)
                        .execute())
                .flatMap(result -&gt;
                         result.map((row, meta) -&gt;
                                 {
                                 return
                                     new Student(
                                         (String) row.get("name"),
                                         (String) row.get("address"));
                                 }
                         ))
                .collectList()
                .doOnNext(s -&gt;
                          {
                          binEntry.setValue((V) s.get(0));
                          observer.onNext(binEntry);
                          })
                .doOnError(t -&gt;
                           {
                           if (t instanceof IndexOutOfBoundsException)
                               {
                               CacheFactory.log("Could not find row for key: " + key);
                               }
                           else
                               {
                               CacheFactory.log("Error: " + t);
                               }
                           observer.onError(binEntry, new Exception(t));
                           })
                .subscribe();
        }
...
    @Override
    public void store(BinaryEntry&lt;K, V&gt; binEntry, StoreObserver&lt;K, V&gt; observer)
        {
        K       key      = binEntry.getKey();
        Student oStudent = (Student) binEntry.getValue();

        Flux.from(getConnection())
                .flatMap(connection -&gt; connection.createStatement(STORE_STMT)
                        .bind("$1", key)
                        .bind("$2", oStudent.getName())
                        .bind("$3", oStudent.getAddress())
                        .execute())
                .flatMap(Result::getRowsUpdated)
                .doOnNext((s) -&gt;
                          {
                          CacheFactory.log("store done, rows updated: " + s);
                          observer.onNext(binEntry);
                          })
                .doOnError(t -&gt; new Exception(t))
                .subscribe();
        }
...
    private static final String STORE_STMT = "INSERT INTO student VALUES ($1, $2, $3) ON conflict (id) DO UPDATE SET name=$2, address=$2";
    private static final String LOAD_STMT = "SELECT NAME, ADDRESS FROM student WHERE id=$1";</markup>

<p>Be sure to consult these <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/caching-data-sources.html#GUID-106C9FE6-6407-4375-A297-AC99D779B77E">best practices</a> when implementing an entry store for your data sources.</p>

</div>
</div>
</div>
</doc-view>
