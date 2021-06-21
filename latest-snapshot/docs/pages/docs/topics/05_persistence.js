<doc-view>

<h2 id="_topics_and_persistence">Topics and Persistence</h2>
<div class="section">
<p>Coherence topics store data in caches, and as such the Coherence persistence feature can be used to persist that data to disc.
As well as the actual topic message data, the metadata associated with a topic will also be stored in caches. This data tracks information such as the head and tail of a topic, it also tracks subscriber groups and subscriber lifetimes. When persistence has been enabled, the metadata caches will also be persisted.</p>

<p>Both active and on-demand persistence will work with topics, and in fact, a recommendation is to use active persistence, as data loss can severely impact topic functionality.</p>

<div class="admonition warning">
<p class="admonition-textlabel">Warning</p>
<p ><p>Care must be taken if recovering cache data from a persistence snapshot.</p>
</p>
</div>
<p>A snapshot of topics caches will also contain metadata for the topic, this includes the commits, heads and tails for subscribers and subscriber groups. When recovering a snapshot for a Coherence topics cache service, all the metadata will also be recovered. This will reset the state of subscribers and subscriber groups, as well as topic heads, and tails, which may affect publishers.
When recovering a snapshot it is important that no subscribers and publishers have been connected. Publishers and subscribers have their own state and on recovery this state will be out of date with the actual topic state in the topic metadata. This can cause subscribers to read the wrong data, or a publisher to attempt to publish to the wrong position. This may not immediately be apparent as it may not cause an exception to be thrown so data could be corrupted.</p>

</div>
</doc-view>
