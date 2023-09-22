<doc-view>

<v-layout row wrap>
<v-flex xs12 sm10 lg10>
<v-card class="section-def" v-bind:color="$store.state.currentColor">
<v-card-text class="pa-3">
<v-card class="section-def__card">
<v-card-text>
<dl>
<dt slot=title>POF Gradle Plugin</dt>
<dd slot="desc"><p>The POF Gradle Plugin provides automated instrumentation of classes with the <code>@PortableType</code> annotation to generate
consistent (and correct) implementations of Evolvable POF serialization methods.</p>

<p>It is a far from a trivial exercise to manually write serialization methods that support serializing inheritance
hierarchies that support the Evolvable concept. However, with static type analysis these methods can be deterministically
generated.</p>

<p>This allows developers to focus on business logic rather than implementing boilerplate code for the above-mentioned
methods.</p>

<div class="admonition note">
<p class="admonition-inline">Please see
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.2206/develop-applications/using-portable-object-format.html#GUID-25206CEF-3271-494C-B43A-066A84E6B1BD">Portable Types documentation</a>
for more information and detailed instructions on Portable Types creation and usage.</p>
</div></dd>
</dl>
</v-card-text>
</v-card>
</v-card-text>
</v-card>
</v-flex>
</v-layout>

<h2 id="_usage">Usage</h2>
<div class="section">
<p>In order to use the POF Gradle Plugin, you need to declare it as a plugin dependency in your <code>build.gradle</code> file:</p>

<markup
lang="groovy"

>plugins {
    id 'java'
    id 'com.oracle.coherence.ce'
}</markup>

<p>Without any further configuration, the plugin will add a task named <code>coherencePof</code> to your project. The <code>coherencePof</code>
task will be executed at the end of the <code>compileJava</code> task. At the same time, <code>coherencePof</code> also depends on the <code>compileJava</code> task.</p>

<p>Therefore, calling:</p>

<markup
lang="bash"

>gradle compileJava</markup>

<p>will execute the <code>coherencePof</code> task. And similarly:</p>

<markup
lang="bash"

>gradle coherencePof</markup>

<p>will execute the <code>compileJava</code> task first. By <strong>default</strong>, the <code>coherencePof</code> task will take the <strong>build output directory</strong> as
input for classes to be instrumented excluding any test classes.</p>

<p>By just adding the plugin using the configuration above, the Coherence Gradle Plugin will discover and instrument all
project classes annotated with the <code>@PortableType</code> annotation, excluding test classes. If you do need to instrument test
classes, you can add the <code>coherencePof</code> closure and provide additional configuration properties.</p>


<h3 id="_custom_configuration">Custom Configuration</h3>
<div class="section">
<p>The default behavior of the Coherence Gradle Plugin, can be customized using several optional properties. Simply provide
a <code>coherencePof</code> closure to your <code>build.gradle</code> script containing any additional configuration properties, e.g.:</p>

<markup
lang="groovy"
title="Build.gradle"
>coherencePof {
  debug=true <span class="conum" data-value="1" />
}</markup>

<ul class="colist">
<li data-value="1">This will instruct Coherence to provide more logging output in regard to the instrumented classes</li>
</ul>
</div>

<h3 id="_available_configuration_properties">Available Configuration Properties</h3>
<div class="section">

<h4 id="_enable_debugging">Enable Debugging</h4>
<div class="section">
<p>Set the boolean <code>debug</code> property to <code>true</code> in order to instruct the underlying <code>PortableTypeGenerator</code> to generate debug
code in regards the instrumented classes.</p>

<p>If not specified, this property <em>defaults</em> to <code>false</code>.</p>

</div>

<h4 id="_instrumentation_of_test_classes">Instrumentation of Test Classes</h4>
<div class="section">
<p>Set the boolean <code>instrumentTestClasses</code> property to <code>true</code> in order to instrument test classes.
If not specified, this property <em>defaults</em> to <code>false</code>.</p>

</div>

<h4 id="_set_a_custom_testclassesdirectory">Set a Custom TestClassesDirectory</h4>
<div class="section">
<p>Provide a path to a custom test classes directory using property <code>testClassesDirectory</code>. If not set, it will default
to the default test output directory.</p>

</div>

<h4 id="_set_a_custom_mainclassesdirectory">Set a Custom MainClassesDirectory</h4>
<div class="section">
<p>Provide a path to a custom classes directory using property <code>mainClassesDirectory</code>. If not set, it will default
to the default output directory.</p>

</div>
</div>

<h3 id="_what_about_classes_without_the_portabletype_annotation">What about classes without the @PortableType annotation?</h3>
<div class="section">
<p>In some cases, it may be necessary to expand the type system with the types that are not annotated with the
<code>@PortableType</code> annotation, and are not discovered automatically. This is typically the case when some of your portable
types have <code>enum</code> values, or existing classes that implement the <code>PortableObject</code> interface explicitly as attributes.</p>

<p>You can add those types to the schema by creating a <code>META-INF/schema.xml</code> file and specifying them explicitly. For example,
if you assume that the <code>Color</code> class from the earlier code examples:</p>

<markup
lang="xml"
title="META-INF/schema.xml"
>&lt;?xml version="1.0"?&gt;

&lt;schema xmlns="http://xmlns.oracle.com/coherence/schema"
        xmlns:java="http://xmlns.oracle.com/coherence/schema/java" external="true"&gt;

  &lt;type name="Color"&gt;
    &lt;java:type name="petstore.Color"/&gt;
  &lt;/type&gt;

&lt;/schema&gt;</markup>

</div>
</div>

<h2 id="_jandex_index">Jandex Index</h2>
<div class="section">
<p>The portable type discovery feature of Coherence depends on the availability of a
<a id="" title="" target="_blank" href="https://github.com/smallrye/jandex">Jandex</a> index within the modules that provide portable types that need to be registered.</p>

<p>Therefore, in order to use POF instrumented classes at compile-time in your Gradle project, you should also add a
Jandex Gradle plugin. As Oracle Coherence uses Jandex 2 under the covers, there are 2 options available:</p>

<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://github.com/vlsi/vlsi-release-plugins">https://github.com/vlsi/vlsi-release-plugins</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://github.com/kordamp/jandex-gradle-plugin">https://github.com/kordamp/jandex-gradle-plugin</a> (Only versions <code>&lt; 1.0.0</code> as recent versions use Jandex 3)</p>

</li>
</ul>
<markup
lang="groovy"
title="Build.gradle"
>plugins {
  id 'java'
  id 'com.oracle.coherence.ce' version '23.09-SNAPSHOT'
  id 'com.github.vlsi.jandex' version '{com-github-vlsi-jandex-version}'
}</markup>

<markup
lang="groovy"
title="Build.gradle"
>plugins {
  id 'java'
  id 'com.oracle.coherence.ce' version '23.09-SNAPSHOT'
  id 'org.kordamp.gradle.jandex' version '0.13.2'
}</markup>

</div>

<h2 id="_example">Example</h2>
<div class="section">
<p>An example <code>Person</code> class (below) when processed with the plugin, results in the bytecode shown below.</p>

<markup
lang="java"
title="Person.java"
>@PortableType(id=1000)
public class Person
    {
    public Person()
        {
        }

    public Person(int id, String name, Address address)
        {
        super();
        this.id = id;
        this.name = name;
        this.address = address;
        }

    int id;
    String name;
    Address address;

    // getters and setters omitted for brevity
    }</markup>

<p>Let&#8217;s inspect the generated bytecode:</p>

<markup
lang="bash"

>javap Person.class</markup>

<p>This should yield the following output:</p>

<markup
lang="java"

>public class demo.Person implements com.tangosol.io.pof.PortableObject,com.tangosol.io.pof.EvolvableObject {
  int id;
  java.lang.String name;
  demo.Address address;
  public demo.Person();
  public demo.Person(int, java.lang.String, demo.Address);
  public int getId();
  public void setId(int);
  public java.lang.String getName();
  public void setName(java.lang.String);
  public demo.Address getAddress();
  public void setAddress(demo.Address);
  public java.lang.String toString();
  public int hashCode();
  public boolean equals(java.lang.Object);

  public void readExternal(com.tangosol.io.pof.PofReader) throws java.io.IOException; <span class="conum" data-value="1" />
  public void writeExternal(com.tangosol.io.pof.PofWriter) throws java.io.IOException;
  public com.tangosol.io.Evolvable getEvolvable(int);
  public com.tangosol.io.pof.EvolvableHolder getEvolvableHolder();
}</markup>

<ul class="colist">
<li data-value="1">Additional methods generated by Coherence POF plugin.</li>
</ul>

<h3 id="_skip_execution">Skip Execution</h3>
<div class="section">
<p>You can skip the execution of the <code>coherencePof</code> task by running the Gradle build using the <code>-x</code> flag, e.g.:</p>

<markup
lang="bash"

>gradle clean build -x coherencePof</markup>

</div>
</div>

<h2 id="_development">Development</h2>
<div class="section">
<p>During development, it is extremely useful to rapidly test the plugin code against separate example projects. For this,
we can use Gradle&#8217;s <a id="" title="" target="_blank" href="https://docs.gradle.org/current/userguide/composite_builds.html">composite build</a> feature. Therefore,
the Coherence POF Gradle Plugin module itself provides a separate <code>sample</code> module. From within
the sample directory you can execute:</p>

<markup
lang="bash"

>gradle clean compileJava --include-build ../plugin</markup>

<p>This will not only build the sample but will also build the plugin and developers can make plugin code changes and see
changes rapidly reflected in the execution of the sample module.</p>

<p>Alternatively, you can build and install the Coherence Gradle plugin to your local Maven repository using:</p>

<markup
lang="bash"

>gradle publishToMavenLocal</markup>

<p>For projects to pick up the local changes ensure the following configuration:</p>

<markup
lang="groovy"
title="Build.gradle"
>plugins {
  id 'java'
  id 'com.oracle.coherence.ce' version '23.09-SNAPSHOT'
  id 'com.github.vlsi.jandex' version '{com-github-vlsi-jandex-version}'
}</markup>

<markup
lang="groovy"
title="Settings.gradle"
>pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}</markup>

</div>
</doc-view>
