<doc-view>

<v-layout row wrap>
<v-flex xs12 sm10 lg10>
<v-card class="section-def" v-bind:color="$store.state.currentColor">
<v-card-text class="pa-3">
<v-card class="section-def__card">
<v-card-text>
<dl>
<dt slot=title>Coherence Documentation Module</dt>
<dd slot="desc"><p>This is the module that builds the Coherence documentation.
The module is not part of the default build and must be built separately.</p>
</dd>
</dl>
</v-card-text>
</v-card>
</v-card-text>
</v-card>
</v-flex>
</v-layout>

<h3 id="_build_the_docs">Build the Docs</h3>
<div class="section">
<p>To build the docs, run the following Maven command from the top-level <code>prj/</code> directory:</p>

<markup
lang="shell"

>mvn clean install -DskipTests -P docs -pl docs</markup>

</div>

<h3 id="_view_the_docs">View the Docs</h3>
<div class="section">
<p>To view the documentation after building it, run the following command from the top-level <code>prj/</code> directory:</p>

<markup
lang="shell"

>mvn exec:exec -P docs -pl docs</markup>

<div class="admonition note">
<p class="admonition-inline">The installation requires you to install Python, which runs a small Pythin http server from the directory where the docs
are built.</p>
</div>
</div>
</doc-view>
