<doc-view>

<h2 id="_overview">Overview</h2>
<div class="section">
<p>These guides and tutorials are designed to help you be productive as quickly as possible in whatever use-case you
are building with Coherence.
Coherence has a long history and having been around for twenty years its APIs have evolved over that time.
Occasionally there are multiple ways to implement a specific use-case, typically because to remain backwards compatible
with older releases, features cannot be removed from the product. For that reason these guides use the latest Coherence
versions and best practice and approaches recommended by the Coherence team for that version.</p>

<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/000-overview"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">explore</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Simple Guides</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/tutorials/000-overview"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-graduation-cap</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Tutorials</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>

<h2 id="simple">Guides</h2>
<div class="section">
<p>These simple guides are designed to be a quick hands-on introduction to a specific feature of Coherence.
In most cases they require nothing more than a Coherence jar and an IDE (or a text editor it you&#8217;re really old-school).
Guides are typically built as a combination Maven and Gradle project including the corresponding wrappers for those tools
making them simple to build as stand-alone projects without needing to build the whole Coherence source tree.</p>

<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/guides/100-put-get-remove/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Put Get and Remove</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>A guide showing basic CRUD <code>put</code>, <code>get</code>, and <code>remove</code> operations on a <code>NamedMap</code>.</p>
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
<p>A guide to using Caching Data Stores</p>
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
<p>This guide walks you through how to use near caching within Coherence</p>
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
<p>This guide walks you through how to use client events within Coherence</p>
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
<p>This guide walks you through how to use and configure Cache Stores</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>

<h2 id="tutorials">Tutorials</h2>
<div class="section">
<p>These tutorials provide a deeper understanding of larger Coherence features and concepts that cannot be usually be
explained with a few simple code snippets. They might, for example, require a running Coherence cluster to properly show
a feature.
Tutorials are typically built as a combination Maven and Gradle project including the corresponding wrappers for those tools
making them simple to build as stand-alone projects without needing to build the whole Coherence source tree.</p>

<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/tutorials/500-graphql/README"><div class="card__link-hover"/>
</router-link>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">GraphQL</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>This tutorial shows you how to access Coherence Data using GraphQL.</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>
</doc-view>
