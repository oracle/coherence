<doc-view>

<h2 id="_overview">Overview</h2>
<div class="section">
<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The documentation on this site covers new features and improvements that are currently only available in the open source <a id="" title="" target="_blank" href="https://github.com/oracle/coherence">Coherence Community Edition</a> (CE).</p>

<p>For complete documentation covering all the features that are available both in the latest commercial editions (Enterprise and Grid Edition) and the Community Edition, please refer to the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/index.html">Official Documentation</a>.</p>
</p>
</div>
<p>Coherence is scalable, fault-tolerant, cloud-ready, distributed platform for building grid-based applications and reliably storing data.
The product is used at scale, for both compute and raw storage, in a vast array of industries such as critical financial trading systems, high performance telecommunication products, and eCommerce applications.</p>

<p>Typically, these deployments do not tolerate any downtime and Coherence is chosen due its novel features in death detection, application data evolvability, and the robust, battle-hardened core of the product that enables it to be seamlessly deployed and adapted within any ecosystem.</p>

<p>At a high level, Coherence provides an implementation of the familiar <code>Map&lt;K,V&gt;</code> interface but rather than storing the associated data in the local process, it is partitioned (or sharded) across a number of designated remote nodes.
This partitioning enables applications to not only distribute (and therefore scale) their storage across multiple processes, machines, racks, and data centers, but also to perform grid-based processing to truly harness the CPU resources of the machines.</p>

<p>The Coherence interface <code>NamedMap&lt;K,V&gt;</code> (an extension of <code>Map&lt;K,V&gt;</code> provides methods to query, aggregate (map/reduce style), and compute (send functions to storage nodes for locally executed mutations) the data set.
These capabilities, in addition to numerous other features, enable Coherence to be used as a framework to write robust, distributed applications.</p>

</div>

<h2 id="_get_going">Get Going</h2>
<div class="section">
<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/docs/about/02_introduction"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">assistant</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Coherence</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>What is Oracle Coherence?</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/docs/about/03_quickstart"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-rocket</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Quick Start</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>A quick-start guide to using Coherence.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/examples/README"><div class="card__link-hover"/>
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
<span style="text-align:center">Guides & Tutorials</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Guides, examples and tutorial about Coherence features and best practice.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/index.html"><div class="card__link-hover"/>
</a>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">import_contacts</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Docs</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Oracle Coherence commercial edition product documentation.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<a id="" title="" target="_blank" href="../java/api/index.html"><div class="card__link-hover"/>
</a>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">library_books</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">API Docs</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Browse the Coherence CE API Docs.</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>

<h2 id="_new_features">New Features</h2>
<div class="section">
<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/docs/core/01_overview"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-cubes</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Core</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Coherence Core Improvements.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/coherence-cdi-server/README"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">extension</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">CDI</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Coherence CDI extensions.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/coherence-mp/README"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-cogs</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Microprofile</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Coherence Microprofile support.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/coherence-grpc-proxy/README"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">settings_ethernet</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">gRPC</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Coherence gRPC server and client.</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>

<h2 id="_tools">Tools</h2>
<div class="section">
<v-layout row wrap class="mb-5">
<v-flex xs12>
<v-container fluid grid-list-md class="pa-0">
<v-layout row wrap class="pillars">
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/plugins/maven/pof-maven-plugin/README"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-plug</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Plugins</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Build tool plugins to aid Coherence application development.</p>
</v-card-text>
</v-card>
</v-flex>
<v-flex xs12 sm4 lg3>
<v-card>
<router-link to="/coherence-docker/README"><div class="card__link-hover"/>
</router-link>
<v-layout align-center justify-center class="">
<v-avatar size="150px">
<v-icon class="xxx-large">fa-th</v-icon>
</v-avatar>
</v-layout>
<div class="px-3">
<v-divider class="indigo lighten-4"/>
</div>
<v-card-title primary class="headline layout justify-center">
<span style="text-align:center">Container Images</span>
</v-card-title>
<v-card-text class="caption">
<p></p>
<p>Example Coherence OCI container (Docker) images.</p>
</v-card-text>
</v-card>
</v-flex>
</v-layout>
</v-container>
</v-flex>
</v-layout>
</div>
</doc-view>
