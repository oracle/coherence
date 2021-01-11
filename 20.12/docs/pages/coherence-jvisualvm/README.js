<doc-view>

<h2 id="_coherence_visualvm_plugin">Coherence VisualVM Plugin</h2>
<div class="section">
<p>The Coherence-VisualVM plug-in provides management and monitoring of a single Coherence cluster using the VisualVM management utility.</p>

<p>The plug-in aggregates Coherence MBean data and shows a concise operational view of a single Coherence cluster.
Some management information is presented over time, which allows real-time analysis and troubleshooting.</p>

<div class="admonition note">
<p class="admonition-inline">This version of the plugin requires VisualVM release 2.0.2 or later which is available from <a id="" title="" target="_blank" href="https://visualvm.github.io/">https://visualvm.github.io/</a>.</p>
</div>
</div>

<h2 id="_building_the_plugin">Building the Plugin</h2>
<div class="section">
<p>As the libraries required to build the VisualVM plugin are not hosted on Maven Central, the plugin is
not built by default when building Coherence.</p>

<p>To build the plugin is a two step process:</p>

<ol style="margin-left: 15px;">
<li>
Generate the VisualVM dependencies

</li>
<li>
Build Coherence CE enabling the <code>visualvm</code> profile.

</li>
</ol>

<h3 id="_pre_requisites">Pre-requisites</h3>
<div class="section">
<p>You must have the following:</p>

<ol style="margin-left: 15px;">
<li>
Java JDK 1.8

</li>
<li>
Ant version &gt;= 1.9.9

</li>
<li>
Maven 3.6.3+

</li>
<li>
Git

</li>
</ol>
</div>

<h3 id="_generate_the_visualvm_dependencies">Generate the VisualVM dependencies</h3>
<div class="section">
<div class="admonition note">
<p class="admonition-inline">These instructions have been summarized from <a id="" title="" target="_blank" href="https://github.com/oracle/visualvm/blob/release202/README.md">https://github.com/oracle/visualvm/blob/release202/README.md</a>.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Checkout the VisualVM repository
<markup
lang="shell"

>$ git clone https://github.com/oracle/visualvm.git

Cloning into 'visualvm'...</markup>

</li>
<li>
Checkout the <code>release202</code> branch
<markup
lang="shell"

>$ cd visualvm

$ git checkout release202

Switched to a new branch 'release202'</markup>

</li>
<li>
Unzip the NetBeans Platform 11
<markup
lang="shell"

>$ cd visualvm

$ unzip nb111_platform_08102019.zip</markup>

</li>
<li>
Build the Plugins
<markup
lang="shell"

>$ ant build-zip</markup>

</li>
<li>
Unzip the artefacts
<markup
lang="shell"

>$ cd dist

$ unzip visualvm.zip

$ cd ..</markup>

</li>
<li>
Generate the NBM&#8217;s
<markup
lang="shell"

>$ ant nbms</markup>

</li>
<li>
Install into the local repository
<markup
lang="shell"

>$ mvn -DnetbeansInstallDirectory=dist/visualvm   \
    -DnetbeansNbmDirectory=build/updates   \
    -DgroupIdPrefix=org.graalvm.visualvm  \
    -DforcedVersion=RELEASE202  org.apache.netbeans.utilities:nb-repository-plugin:populate</markup>

</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">See <a id="" title="" target="_blank" href="https://github.com/oracle/visualvm/blob/release202/README.md">here</a> for instructions on how to
push the artefacts to a remote Maven repository.</p>
</div>
</div>

<h3 id="_build_the_visualvm_plugin">Build the VisualVM Plugin</h3>
<div class="section">
<div class="admonition note">
<p class="admonition-inline">Ensure you change to a completely new directory to close the Coherence CE repository.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Clone the Coherence CE repository
<markup
lang="shell"

>$ https://github.com/oracle/coherence.git</markup>

</li>
<li>
Build the Plugin
<markup
lang="shell"

>$ cd coherence/prj/coherence-jvisualvm

$ mvn clean install -P visualvm</markup>

</li>
<li>
Install the Plugin
<p>The plugin will be available in the location <code>target/coherence-jvisualvm-20.12.nbm</code></p>

</li>
</ol>
<p>Follow the instructions <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/manage/using-jmx-manage-oracle-coherence.html">here</a>
to install the plugin.</p>

</div>
</div>
</doc-view>
