<doc-view>

<h2 id="_coherence_documentation_module">Coherence Documentation Module</h2>
<div class="section">
<p>This is the module that builds the Coherence documentation.
The module is not part of the default build and must be built separately.</p>


<h3 id="_build_the_docs">Build the Docs</h3>
<div class="section">
<p>To build the docs, run the following Maven command from the top-level <code>prj/</code> directory:</p>

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

<p>Docs can be viewd at <a id="" title="" target="_blank" href="http://localhost:8080">http://localhost:8080</a></p>

<div class="admonition note">
<p class="admonition-inline">This requires Python to be installed and runs a small Python http server from the directory where the docs
have been built to.</p>
</div>
</div>
</div>

<h2 id="_version_numbers">Version Numbers</h2>
<div class="section">
<p>When putting version numbers in <code>.adoc</code> files, we use attribute substitutions.
Attributes are set in the <code>sitegen.yaml</code> file, for example</p>

<v-card flat color="grey lighten-3"  class="card__example">
<v-card-text><p>engine:
  asciidoctor:
    images-dir: "docs/images"
    libraries:
      - "asciidoctor-diagram"
    attributes:
      plantumlconfig: "_plantuml-config.txt"
      coherence-maven-group-id: "${coherence.group.id}"
      version-coherence: "${revision}"
      version-commercial-docs: "14.1.1.0"
      version-helidon: "${helidon.version}"</p>
</v-card-text>
</v-card>


<p>The format of an attribute is name followed by a colon, and the attribute value in quotes,
so above the value of the <code>version-commercial-docs</code> attribute is <code>14.1.1.0</code>.</p>

<p>Attributes can be taken from Maven build properties by using the normal Maven property replacement string as the value.
For example the <code>version-coherence</code> attribute&#8217;s value will be the Maven <code>revision</code> property value.</p>

<p>In the <code>.adoc</code> files the attributes are then substituted by putting the attribute name in curly brackets.</p>

<p>For example:</p>

<v-card flat color="grey lighten-3"  class="card__example">
<v-card-text><p>The current commercial Coherence version is 14.1.1.0.</p>
</v-card-text>
</v-card>


<p>would become</p>

<v-card flat color="grey lighten-3"  class="card__example">
<v-card-text><p>The current commercial Coherence version is 14.1.1.0.</p>
</v-card-text>
</v-card>


</div>
</doc-view>
