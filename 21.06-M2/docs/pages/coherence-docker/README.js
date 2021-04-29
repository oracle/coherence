<doc-view>

<h2 id="_coherence_oci_image">Coherence OCI Image</h2>
<div class="section">
<p>This module builds an example Coherence OCI compatible image.</p>

<div class="admonition note">
<p class="admonition-inline">The image built in this module is a demo and example of how to build a Coherence image using
the <a id="" title="" target="_blank" href="https://github.com/GoogleContainerTools/jib/tree/master/jib-maven-plugin/">JIB Maven Plugin</a>.
The image is not intended to be used in production deployments or as a base image, it is specifically
for demos, experimentation and learning purposes.</p>
</div>

<h3 id="_image_contents">Image Contents</h3>
<div class="section">
<p>The Coherence image uses a distroless base image containing OpenJDK.
There are many advantages of a distroless image, security being the main one.
Of course, you are free to use whatever base image or build mechanism you want for your own images.</p>

<p>The image built by the <code>coherence-docker</code> module contains the following Coherence components:</p>


<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 50%;">
<col style="width: 50%;">
</colgroup>
<thead>
<tr>
<th>Component</th>
<th>Description</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Coherence</td>
<td class="">The core Coherence server</td>
</tr>
<tr>
<td class="">Coherence Extend</td>
<td class="">A Coherence*Extend proxy, exposed on port <code>20000</code></td>
</tr>
<tr>
<td class="">Coherence gRPC Proxy</td>
<td class="">A Coherence gRPC proxy, exposed on port <code>1408</code></td>
</tr>
<tr>
<td class="">Coherence Management</td>
<td class="">Coherence Management over REST, exposed on port <code>30000</code></td>
</tr>
<tr>
<td class="">Coherence Metrics</td>
<td class="">Standard Coherence metrics is installed and exposed on port <code>9612</code>, but is disabled by default.
Coherence metrics can be enabled with the System property <code>coherence.metrics.http.enabled=true</code></td>
</tr>
<tr>
<td class="">Coherence Tracing</td>
<td class="">Coherence tracing is configured to use a Jaeger tracing server. See the <router-link to="#tracing" @click.native="this.scrollFix('#tracing')">Tracing</router-link> section below.</td>
</tr>
</tbody>
</table>
</div>
</div>

<h3 id="_building_the_image">Building the Image</h3>
<div class="section">
<p>Assuming you have first cloned the Coherence CE project the to build the Coherence image run the following command
from the top-level Maven <code>prj/</code> folder:</p>

<markup
lang="bash"

>mvn clean install -P docker -pl coherence-docker</markup>

<p>The name of the image produced comes from properties in the <code>coherence-docker</code> module <code>pom.xml</code> file.</p>

<p><code>${docker.registry}/coherence-ce:&lt;version&gt;</code></p>

<p>Where <code>&lt;version&gt;</code>, is the version of the product from the <code>pom.xml</code> file.
The <code>${docker.registry}</code> property is the name of the registry that the image will be published to, by default
this is <code>oraclecoherence</code>.</p>

<p>So, if the version in the <code>pom.xml</code> is <code>21.06-M2</code> the image produced will be
<code>oraclecoherence/coherence-ce:21.06-M2</code></p>

<p>To change the registry name the image can be built by specifying the <code>docker.registry</code> property, for example:</p>

<markup
lang="bash"

>mvn clean install -P docker -pl coherence-docker -Ddocker.registry=foo</markup>

<p>The example above would build an image named <code>foo/coherence:21.06-M2</code></p>

</div>

<h3 id="_run_the_image">Run the image</h3>
<div class="section">
<p>Run the image just like any other image. In Docker this command would be:</p>

<markup
lang="bash"

>docker run -d -P oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>The <code>-P</code> parameter will ensure that the Extend, gRPC, management and metrics ports will all be exposed.</p>

<p>By default, when started the image will run <code>com.tangosol.net.DefaultCacheServer</code>.
This may be changed by setting the <code>COH_MAIN_CLASS</code> environment variable to the name of another main class.</p>

<markup
lang="bash"

>docker run -d -P \
    -e COH_MAIN_CLASS=com.tangosol.net.DefaultCacheServer \
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>


<h4 id="_run_the_image_in_kubernetes">Run the Image in Kubernetes</h4>
<div class="section">
<p>This image can be run in Kubernetes using the <a id="" title="" target="_blank" href="https://oracle.github.io/coherence-operator/docs/3.0.0">Coherence Operator</a>.</p>

<div class="admonition note">
<p class="admonition-inline">The sections below on additional configurations do not apply when using the Coherence Operator to run the image
in Kubernetes. The operator provides functionality to configure the container correctly.</p>
</div>
</div>
</div>

<h3 id="coherence-properties">Specifying Coherence System Properties</h3>
<div class="section">
<p>Many options in Coherence can be set from System properties prefixed with <code>coherence.</code>.
The issue here is that System properties are not very easy to pass into the JVM in the container, whereas environment
variables are. To help with this the main class which runs in the container will convert any environment variable
prefixed with <code>coherence.</code> into a System property before it starts Coherence.</p>

<markup
lang="bash"

>docker run -d -P \
    -e coherence.cluster=testing \
    -e coherence.role=storage \
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>The example above sets two environment variables, <code>coherence.cluster=testing</code> and <code>coherence.role=storage</code>.
These will be converted to System properties so Coherence will start the same as it would if the variables
had been passed to the JVM command line as <code>-Dcoherence.cluster=testing -Dcoherence.role=storage</code></p>

<div class="admonition note">
<p class="admonition-inline">This <em>only</em> applies to environment variables prefixed with <code>coherence.</code> that have not already set as System
properties some other way.</p>
</div>
</div>

<h3 id="_specifying_jvm_options">Specifying JVM Options</h3>
<div class="section">
<p>Images built with JIB have a fixed entrypoint configured to run the application. This is not very flexible if additional
options need to be passed to the JVM. The Coherence image makes use of the JVM&#8217;s ability to load options at start-up
from a file by using a JVM option <code>@&lt;file-name&gt;</code>. The Coherence image entrypoint contains <code>@/args/jvm-args.txt</code>, so the
JVM will load additional options on start-up from a file named <code>/args/jvm-args.txt</code>. This means that additional
options can be provided by adding a volume mapping that adds this file to the container.</p>

<p>For example, to set the heap to 5g, the Coherence cluster name to <code>test-cluster</code> and role name to <code>storage</code> then
additional JVM arguments will be required. Create a file named <code>jvm-args.txt</code> containing these properties:</p>

<markup

title="jvm-args.txt"
>-Xms5g
-Xmx5g
-Dcoherence.cluster=test-cluster
-Dcoherence.role=storage</markup>

<p>If the file has been created in a local directory named <code>/home/oracle/test-args</code> then the image can be run with the following
command:</p>

<markup
lang="bash"

>docker run -d -P -v /home/oracle/test-args:/args oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>This will cause Docker to mount the local <code>/home/oracle/test-args</code> directory to the <code>/args</code> directory in the container
where the JVM will find the <code>jvm-args.txt</code> file.</p>

</div>

<h3 id="_adding_to_the_classpath">Adding to the Classpath</h3>
<div class="section">
<p>Images built with JIB have a fixed classpath configured, which is not very flexible if additional resources need to be
added to the classpath. The Coherence image maps two additional directories to the classpath that are empty in the image
and may be used to add items to the classpath by mapping external volumes to these directories.</p>

<p>The additional classpath entries are:</p>

<ul class="ulist">
<li>
<p><code>/coherence/ext/lib/*</code> - this will add all <code>.jar</code> files under the <code>/coherence/ext/lib/</code> directory to the classpath</p>

</li>
<li>
<p><code>/coherence/ext/conf</code>  - this adds <code>/coherence/ext/conf</code> to the classpath so that any classes, packages or other
resource files in this directory will be added to the classpath.</p>

</li>
</ul>
<p>For example:</p>

<p>On the local Docker host there is a folder called <code>/dev/my-app/lib</code> that contains <code>.jar</code> files to be added to the
container classpath.</p>

<markup
lang="bash"

>docker run -d -P -v /dev/my-app/lib:/coherence/ext/lib oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>The command above maps the local directory <code>/dev/my-app/lib</code> to the <code>/coherence/ext/lib</code> in the container so that any
<code>.jar</code> files in the <code>/dev/my-app/lib</code> directory will now be on the Coherence JVM&#8217;s classpath.</p>

<p>On the local Docker host there is a folder called <code>/dev/my-app/classes</code> that contains <code>.class</code> files and other
application resources to be added to the container classpath.</p>

<markup
lang="bash"

>docker run -d -P -v /dev/my-app/classes:/coherence/ext/conf oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>The command above maps the local directory <code>/dev/my-app/classes</code> to the <code>/coherence/ext/conf</code> in the container so that
any classes and resource files in the <code>/dev/my-app/classes</code> directory will now be on the Coherence JVM&#8217;s classpath.</p>

</div>
</div>

<h2 id="clustering">Clustering</h2>
<div class="section">
<p>Multiple containers can be started to form a cluster. By default, Coherence uses multi-cast for cluster discovery but
in containers this either will not work, or is not reliable, so well-known-addressing can be used.</p>

<p>This example is going to use basic Docker commands and links between containers.
There are other ways to achieve the same sort of functionality depending on the network configurations you want to
use in Docker.</p>

<p>First, determine the name to be used for the first container, in this example it will be <code>storage-1</code>.</p>

<p>Next, create a `
Start the first container in the cluster:</p>

<markup
lang="bash"

>docker run -d -P \
    --name storage-1 \
    --hostname storage-1 \
    -e coherence.wka=storage-1 \
    -e coherence.cluster=testing \
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>The first container has been started with a container name of <code>storage-1</code>, and the host name also set to <code>storage-1</code>.
The container sets the WKA host name to <code>storage-1</code> using <code>-e coherence.wka=storage-1</code> (this will be converted to the
System property <code>coherence.wka=storage-1</code> see <router-link to="#coherence-properties" @click.native="this.scrollFix('#coherence-properties')">Specifying Coherence System Properties</router-link> above).
The container sets the Coherence cluster name to <code>testing</code> using <code>-e coherence.cluster=testing</code> (this will be converted
to the System property <code>coherence.cluster=testing</code> see <router-link to="#coherence-properties" @click.native="this.scrollFix('#coherence-properties')">Specifying Coherence System Properties</router-link> above).</p>

<div class="admonition note">
<p class="admonition-inline">The important part here is that the container has a name, and the <code>--hostname</code> option has also been set.
This will allow the subsequent cluster members to find this container.</p>
</div>
<p>Now, subsequent containers can be started using the same cluster name and WKA host name, but with different container
names and a link to the first container, all the containers will form a single Coherence cluster:</p>

<markup
lang="bash"

>docker run -d -P \
    --name storage-2 \
    --link storage-1 \
    -e coherence.wka=storage-1 \
    -e coherence.cluster=testing \
    oraclecoherence/coherence-ce:{version-coherence-maven}

docker run -d -P \
    --name storage-3 \
    --link storage-1 \
    -e coherence.wka=storage-1 \
    -e coherence.cluster=testing \
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>Two more containers, <code>storage-2</code> and <code>storage-3</code> will now be part of the cluster.</p>

<div class="admonition note">
<p class="admonition-inline">All the members must have a <code>--link</code> option to the first container and have the same WKA and cluster name properties.</p>
</div>
</div>

<h2 id="tracing">Tracing</h2>
<div class="section">
<p>The Coherence image comes with tracing already configured, it just requires a suitable Jaeger server to send spans to.</p>

<p>The simplest way to start is deploy the Jaeger all-in-one server, for example:</p>

<markup
lang="bash"

>docker run -d --name jaeger \
    -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
    -p 5775:5775/udp \
    -p 6831:6831/udp \
    -p 6832:6832/udp \
    -p 5778:5778 \
    -p 16686:16686 \
    -p 14268:14268 \
    -p 14250:14250 \
    -p 9411:9411 \
    jaegertracing/all-in-one:latest</markup>

<p>The Jaeger UI will be available to browse to at <a id="" title="" target="_blank" href="http://127.0.0.1:16686">http://127.0.0.1:16686</a></p>

<p>Jaeger has been started with a container name of <code>jaeger</code>, so it will be discoverable using that host name by the Coherence
containers. Start the Coherence container with a link to the Jaeger container and set the <code>JAEGER_AGENT_HOST</code>
environment variable to <code>jaeger</code>:</p>

<markup
lang="bash"

>docker run -d -P --link jaeger \
    -e JAEGER_AGENT_HOST=jaeger \
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>Once the Coherence container is running perform some interactions with it using one of the exposed services, i.e Extend
or gRPC, and spans will be sent to the Jaeger collector and will be visible in the UI by querying for the <code>coherence</code>
service name. The service name used can be changed by setting the <code>JAEGER_SERVICE_NAME</code> environment variable when
starting the container, for example:</p>

<markup
lang="bash"

>docker run -d -P --link jaeger \
    -e JAEGER_AGENT_HOST=jaeger \
    -e JAEGER_SERVICE_NAME=coherence-test
    oraclecoherence/coherence-ce:{version-coherence-maven}</markup>

<p>Spans will now be sent to Jaeger with the service name <code>coherence-test</code>.</p>

<p>Tracing is very useful to show what happens under the covers for a given Coherence API call. Traces are more interesting
when they come from a Coherence cluster with multiple members, where the traces span different cluster members.
This can easily be done by running multiple containers with tracing enabled and configuring <router-link to="#clustering" @click.native="this.scrollFix('#clustering')">Clustering</router-link> as
described above.</p>

</div>
</doc-view>
