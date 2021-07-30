<doc-view>

<h2 id="_coherence_microprofile_config">Coherence MicroProfile Config</h2>
<div class="section">
<p>Coherence MP Config provides support for <a id="" title="" target="_blank" href="https://microprofile.io/project/eclipse/microprofile-config">Eclipse MicroProfile Config</a> within Coherence cluster members.</p>

<p>It allows you both to configure various Coherence parameters from the values specified in any of the supported config sources, and to use Coherence cache as another, mutable config source.</p>

</div>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use Coherence MP Config, you need to declare it as a dependency in your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence-mp-config&lt;/artifactId&gt;
        &lt;version&gt;21.06.2-SNAPSHOT&lt;/version&gt;
    &lt;/dependency&gt;</markup>

<p>You will also need an implementation of the Eclipse MP Config specification as a  dependency.
For example, if you are using <a id="" title="" target="_blank" href="https://helidon.io/">Helidon</a>, add the following to your <code>pom.xml</code>:</p>

<markup
lang="xml"

>    &lt;dependency&gt;
      &lt;groupId&gt;io.helidon.microprofile.config&lt;/groupId&gt;
      &lt;artifactId&gt;helidon-microprofile-config&lt;/artifactId&gt;
      &lt;version&gt;2.2.1&lt;/version&gt;
    &lt;/dependency&gt;

    &lt;!-- optional: add it if you want YAML config file support --&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;io.helidon.config&lt;/groupId&gt;
      &lt;artifactId&gt;helidon-config-yaml&lt;/artifactId&gt;
      &lt;version&gt;2.2.1&lt;/version&gt;
    &lt;/dependency&gt;</markup>

</div>

<h2 id="_configuring_coherence_using_mp_config">Configuring Coherence using MP Config</h2>
<div class="section">
<p>Coherence provides a number of configuration properties that can be specified by the users in order to define certain attributes or to customize cluster member behavior at runtime.
For example, attributes such as cluster and role name, as well as whether a cluster member should or should not store data,  can be specified via system properties:</p>

<div class="listing">
<pre>-Dcoherence.cluster=MyCluster -Dcoherence.role=Proxy -Dcoherence.distributed.localstorage=false</pre>
</div>

<p>Most of these attributes can also be defined within the operational or cache  configuration file.</p>

<p>For example, you could define first two attributes, cluster name and role, within  the operational config override file:</p>

<markup
lang="xml"

>  &lt;cluster-config&gt;
    &lt;member-identity&gt;
      &lt;cluster-name&gt;MyCluster&lt;/cluster-name&gt;
      &lt;role-name&gt;Proxy&lt;/role-name&gt;
    &lt;/member-identity&gt;
  &lt;/cluster-config&gt;</markup>

<p>While these two options are more than enough in most cases, there are some issues with them being the <strong>only</strong> way to configure Coherence:</p>

<ol style="margin-left: 15px;">
<li>
When you are using one of Eclipse MicroProfile implementations, such as  <a id="" title="" target="_blank" href="https://helidon.io/">Helidon</a> as the foundation of your application, it would be nice to define some of Coherence configuration parameters along with your other configuration parameters, and not in the separate file or via system properties.

</li>
<li>
In some environments, such as Kubernetes, Java system properties are cumbersome to use, and environment variables are a preferred way of passing configuration  properties to containers.

</li>
</ol>
<p>Unfortunately, neither of the two use cases above is supported out of the box,  but that&#8217;s the gap Coherence MP Config is designed to fill.</p>

<p>As long as you have <code>coherence-mp-config</code> and an implementation of Eclipse MP Config specification to your class path, Coherence will use any of the standard or custom config sources to resolve various configuration options it understands.</p>

<p>Standard config sources in MP Config include <code>META-INF/microprofile-config.properties</code> file, if present in the class path, environment variables, and system properties (in that order, with the properties in the latter overriding the ones from the former).
That will directly address problem #2 above, and allow you to specify Coherence configuration options via environment variables within Kubernetes YAML files, for example:</p>

<markup
lang="yaml"

>  containers:
    - name: my-app
      image: my-company/my-app:1.0.0
       env:
        - name: COHERENCE_CLUSTER
          value: "MyCluster"
        - name: COHERENCE_ROLE
          value: "Proxy"
        - name: COHERENCE_DISTRIBUTED_LOCALSTORAGE
          value: "false"</markup>

<p>Of course, the above is just an example&#8201;&#8212;&#8201;if you are running your Coherence cluster in Kubernetes, you should really be using <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-operator">Coherence Operator</a> instead, as it will make both the configuration and the operation of your Coherence cluster much easier.</p>

<p>You will also be able to specify Coherence configuration properties along with the other configuration properties of your application, which will allow you to keep everything in one place, and not scattered across many files.
For example, if you are writing a Helidon application, you can simply add <code>coherence</code> section to your <code>application.yaml</code>:</p>

<markup
lang="yaml"

>coherence:
  cluster: MyCluster
  role: Proxy
  distributed:
    localstorage: false</markup>

</div>

<h2 id="_using_coherence_cache_as_a_config_source">Using Coherence Cache as a Config Source</h2>
<div class="section">
<p>Coherence MP Config also provides an implementation of Eclipse MP Config <code>ConfigSource</code> interface, which allows you to store configuration parameters in a Coherence cache.</p>

<p>This has several benefits:</p>

<ol style="margin-left: 15px;">
<li>
Unlike pretty much all of the default configuration sources, which are static, configuration options stored in a Coherence cache can be modified without forcing you to rebuild your application JARs or Docker images.

</li>
<li>
You can change the value in one place, and it will automatically be visible and up to date on all the members.

</li>
</ol>
<p>While the features above give you incredible amount of flexibility, we also understand that such flexibility is not always desired, and the feature is disabled by default.</p>

<p>If you want to enable it, you need to do so explicitly, by registering <code>CoherenceConfigSource</code> as a global interceptor in your cache configuration file:</p>

<markup
lang="xml"

>&lt;cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd"&gt;

  &lt;interceptors&gt;
    &lt;interceptor&gt;
      &lt;instance&gt;
        &lt;class-name&gt;com.oracle.coherence.mp.config.CoherenceConfigSource&lt;/class-name&gt;
      &lt;/instance&gt;
    &lt;/interceptor&gt;
  &lt;/interceptors&gt;

  &lt;!-- your cache mappings and schemes... --&gt;

&lt;/cache-config&gt;</markup>

<p>Once you do that, <code>CoherenceConfigSource</code> will be activated as soon as your cache  factory is initialized, and injected into the list of available config sources for your application to use via standard MP Config APIs.</p>

<p>By default, it will be configured with a priority (ordinal) of 500, making it higher priority than all the standard config sources, thus allowing you to override the values provided via config files, environment variables and system properties.
However, you have full control over that behavior and can specify different ordinal via <code>coherence.mp.config.source.ordinal</code> configuration property.</p>


</div>
</doc-view>
