<doc-view>

<h2 id="_coherence_documentation_module">Coherence Documentation Module</h2>
<div class="section">
<p>This is the module that builds the Coherence documentation.
The module is not part of the default build and must be built separately.</p>


<h3 id="_build_the_docs">Build the Docs</h3>
<div class="section">
<p>To build the docs run the following Maven command from the top-level <code>prj/</code> directory:</p>

<markup
lang="shell"

>mvn clean install -DskipTests -P docs -pl docs</markup>

</div>

<h3 id="_view_the_docs">View the Docs</h3>
<div class="section">
<p>To view the documentation to see what it looks like after building run the following command from the top-level <code>prj/</code> directory:</p>

<markup
lang="shell"

>mvn exec:exec -P docs -pl docs</markup>

<div class="admonition note">
<p class="admonition-inline">This requires Python to be installed and runs a small Pythin http server from the directory where the docs
have been built to.</p>
</div>
</div>
</div>
</doc-view>
