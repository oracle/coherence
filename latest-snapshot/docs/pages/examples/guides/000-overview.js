<doc-view>

<h2 id="_guides">Guides</h2>
<div class="section">
<p>These simple guides are designed to be a quick hands-on introduction to a specific feature of Coherence.
In most cases they require nothing more than a Coherence jar and an IDE (or a text editor if you&#8217;re really old-school).
Guides are typically built as a combination Maven and Gradle project including the corresponding wrappers for those tools
making them simple to build as stand-alone projects without needing to build the whole Coherence source tree.</p>

<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/050-bootstrap/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Bootstrap Coherence</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through various methods to configure and
bootstrap a Coherence instance.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/070-coherence-extend/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Coherence*Extend</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Provides a guide for clients to connect to a Coherence Cluster via Coherence*Extend.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/100-put-get-remove/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Put Get and Remove</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through basic CRUD <code>put</code>, <code>get</code>, and <code>remove</code> operations on a <code>NamedMap</code>.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/110-queries/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Querying Caches</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through the basic concepts of querying Coherence caches.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/120-built-in-aggregators/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Built-in Aggregators</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use built-in aggregators within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/121-custom-aggregators/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Custom Aggregators</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to create custom aggregators within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/124-views/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Views</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Learn about the basic concepts of working with views using the <code>ContinuousQueryCache</code>.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/125-streams/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Streams</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use the Streams API with Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/128-entry-processors/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Entry Processors</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use Entry Processors with Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/200-federation/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Federation</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use Federation within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/460-topics/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Topics</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use Topics within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/130-near-caching/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Near Caching</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use near caching within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/140-client-events/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Client Events</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use client events within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/142-server-events/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Server-Side Events</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use server-side events within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/145-durable-events/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Durable Events</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use durable events within Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/190-cache-stores/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Cache Stores</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to use and configure Cache Stores.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/195-bulk-loading-caches/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Bulk Loading Caches</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide shows approaches to bulk load data into caches, typically this would be loading data into caches from a DB when applications start.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/210-ssl/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Securing with SSL</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to secure Coherence using SSL/TLS.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/220-performance/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Performance over Consistency & Availability</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through how to tweak Coherence to provide more performance at the expense of data consistency and availability.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/906-partition-level-transactions/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Executor Service</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide explains how to use the Coherence Executor Service.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/905-key-association/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Key Association</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide walks you through a use case for key association in Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/906-partition-level-transactions/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Partition Level Transactions</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This guide explains how to atomically access and update multiple related entries using
an <code>EntryProcessor</code> in a partition level transaction.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/910-multi-cluster-client/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Multi-Cluster Client</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>An example of how to connect an Extend or gRPC client to multiple Coherence clusters.</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>
</doc-view>
