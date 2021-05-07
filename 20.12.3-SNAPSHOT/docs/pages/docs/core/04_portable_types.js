<doc-view>

<h2 id="_portable_types">Portable Types</h2>
<div class="section">
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-portable-object-format.html#GUID-F331E5AB-0B3B-4313-A2E3-AA95A40AD913"><strong>Portable Object Format (POF)</strong></a> was first introduced in Coherence 3.2 (2006), as a way to serialize classes in a platform and language independent format, and is the only serialization format supported by the legacy non-Java Extend clients, such as .NET and C++ Extend client implementations.</p>

<p>As soon as it was released, POF became the preferred serialization format even for customers writing pure Java applications, for several reasons:</p>

<ol style="margin-left: 15px;">
<li>
It is significantly faster than other supported serialization formats, such as Java serialization and <code>ExternalizableLite</code>.

</li>
<li>
It is significantly more compact that other supported serialization formats, allowing you to store more data in a cluster of a given size, and to move less data over the wire.

</li>
<li>
It supports seamless evolution of data classes, allowing you to upgrade various parts of the application (both storage members and clients) independently of one another, without the risk of losing data in the process.

</li>
</ol>
<p>Over the years POF remained largely unchanged, even though it did receive a number of additional features that simplified its use:</p>

<ol style="margin-left: 15px;">
<li>
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-portable-object-format.html#GUID-6E77D329-3A4F-4956-9412-BE34D727A772"><strong>POF Reflection</strong></a> was introduced in Coherence 3.5 (2009), allowing users to extract individual attributes from the POF stream via <code>PofNavigator</code>.

</li>
<li>
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-portable-object-format.html#GUID-0142E1B5-143A-404F-A961-F41340C5921D"><strong>POF Annotations</strong></a> were introduced in Coherence 3.7.1 (2011), as a way to eliminate the need for the manual implementation of the serialization-related code.

</li>
</ol>
<p>Unfortunately, the latter fell a bit short. The implementation was heavily dependent on Java reflection, which sacrificed some performance benefits of POF. More importantly, they provide no support for class evolution, thus sacrificing another important POF benefit.</p>

<p>As such, POF Annotations were deemed somewhat inadequate, and we started working on their replacement in 2013. Some supporting features, such as <em>schema support</em>, were included in Coherence 12.2.1 (2015) and 14.1.1 (2020), and the remaining work was completed and released as part of the Coherence CE 20.12 release and will be available in the next commercial release.</p>


<h3 id="_features_and_benefits">Features and Benefits</h3>
<div class="section">
<p><strong>Portable Types</strong> provide a way to add support for POF serialization to your classes via annotations and without the need to implement serialization code by hand, just like POF Annotations did.</p>

<p>However, unlike POF Annotations, Portable Types:</p>

<ol style="margin-left: 15px;">
<li>
Implement serialization code at compile-time using byte code instrumentation, and do not rely on Java reflection at runtime at all. This makes them just as fast, but less error-prone, as manually implemented serialization code.

</li>
<li>
Support, <em>but do not require</em> explicit registration via POF config file, as all the metadata required for POF type registration, such as type identifier, and the serializer class to use, are already available in the <code>@PortableType</code> annotation.

</li>
<li>
Fully support class evolution.

</li>
</ol>
<p>As a matter of fact, Portable Types provide a better and more complete evolution support than if you implemented <code>Evolvable</code> interface by hand.</p>

<p>One of the limitations of <code>Evolvable</code> is that it only supports evolution of the leaf classes in the class hierarchy.
Portable Types do not have this limitation, and allow you not only to evolve any class in the hierarchy, but also to evolve the class hierarchy itself, by adding new classes to any level of the class hierarchy.</p>

<p>When we first introduced POF back in 2006, it was never the goal to require manual implementation of the serialization code&#8201;&#8212;&#8201;we always wanted to provide the tooling that would do the heavy lifting and allow users to simply express their intent via annotations. It may have taken us almost 15 years, but we feel that with the release of Portable Types, we are finally there.</p>

</div>

<h3 id="_usage_basics">Usage Basics</h3>
<div class="section">
<p>There are only two basic requirements for Portable Types:</p>

<ol style="margin-left: 15px;">
<li>
The class must be annotated with <code>@PortableType</code> annotation, and

</li>
<li>
The fields that should be serialized must be annotated with <code>@Portable</code> or one of related annotations (<code>@PortableDate</code>, <code>@PortableArray</code>, <code>@PortableSet</code>, <code>@PortableList</code>, or <code>@PortableMap</code>)

</li>
</ol>
<markup
lang="java"

>@PortableType(id = 1)
public class Pet
    {
    @Portable
    protected String name;

    // constructors, accessors, etc.
    }

@PortableType(id = 2)
public class Dog extends Pet
    {
    @Portable
    private String breed;

    // constructors, accessors, etc.
    }</markup>

<p>Additional attribute-level annotations allow you to control certain serialization behaviors that are specific to the type of the attribute.</p>

<p>For example, <code>@PortableDate</code> allows you to control whether you want to serialize date, time, or both when serializing <code>java.util.Date</code> instances (via <code>mode</code> property), and whether time zone information should be included (via <code>includeTimezone</code> property).</p>

<p>If you are using Java 8 (or later) <code>java.time</code> classes, that information can be derived from the class itself, so you can (and should) simply use <code>@Portable</code> annotation instead. For example, <code>LocalTime</code> will be serialized as time only, with no time zone information, while the <code>OffsetDateTime</code> will be serialized as both date and time, with time zone information.</p>

<p>Similarly, when serializing arrays, collections and maps, POF allows you to use <em>uniform encoding</em>, where the element type (or key and/or value type, in case of maps) is written into the POF stream only once, instead of once for each element of the collection, resulting in a more compact serialized form.</p>

<markup
lang="java"

>public class MyClass
    {
    @PortableArray(elementClass = String.class)
    private String[] m_stringArray;

    @PortableSet(elementClass = String.class, clazz = LinkedHashSet.class)
    private Set&lt;String&gt; m_setOfStrings;

    @PortableList(elementClass = String.class)
    private List&lt;String&gt; m_listOfStrings;

    @PortableMap(keyClass = Integer.class, valueClass = String.class, clazz = TreeMap.class)
    private Map&lt;Integer, String&gt; m_uniformMap;
    }</markup>

<p>As you can see from the examples above, these annotations also allow you to specify the concrete class that should be created during deserialization for a given attribute. If the <code>clazz</code> property is not specified, <code>HashSet</code> will be used as a default set type, <code>ArrayList</code> as a default list type, and <code>HashMap</code> as a default map type.</p>

</div>

<h3 id="_class_versioning_and_evolution">Class Versioning and Evolution</h3>
<div class="section">
<p>Coherence is a distributed system, and there is no guarantee that every cluster member, and every client process that connects to the cluster, will have the same version of each and every class. As a matter of fact, for systems that use rolling upgrades in order to avoid any downtime, it is pretty much guaranteed that they won&#8217;t!</p>

<p>It is also neither safe nor practical for most Coherence customers to upgrade the cluster and all the clients at the same time, so being able to tolerate different versions of the same class across cluster members and clients is not only nice to have, but a necessity for many Coherence users.</p>

<p>The issue is that when a process that has an older version of the class reads serialized data created from the newer version of the same class, it may encounter some attributes that it knows nothing about. Ideally, it should be able to ignore them and read the attributes it needs and knows about, instead of crashing, but that only solves part of the problem. If it ignores the unknown attributes completely, what will happen when it writes the same data back, by serializing an older version of the class that is only aware of some attributes? Unfortunately, the most likely answer is that it will lose the data it previously received but knows nothing about.</p>

<p>Obviously, this is not a desirable scenario for a system that is intended for long-term data storage, so POF supports class evolution in a way that ensures that no data will be lost, regardless of how many versions of the same class are present across the various cluster and client processes, and regardless of which of those processes read or write the data. The support for class evolution has been in POF from the very beginning, via the <code>Evolvable</code> interface, but Portable Types remove some of the limitations and make the whole process significantly simpler.</p>

<p>Both the class annotation (<code>@PortableType</code>) and the attribute annotations (<code>@Portable</code> and related annotations) provide a way to specify versioning information necessary for class evolution.</p>

<p>At the class level, whenever you modify a class by introducing a new attribute, you should increment the <code>version</code> property of the <code>@PortableType</code> annotation.</p>

<p>At the same time, you should specify <code>since</code> attribute that matches the new class version number for any new class attribute.
For example, to add <code>age</code> attribute to the <code>Pet</code> class, and <code>color</code> attribute to the <code>Dog</code> class, we would change the code above to:</p>

<markup
lang="java"

>@PortableType(id = 1, version = 1)
public class Pet
    {
    @Portable
    protected String name;

    @Portable(since = 1)
    protected int age;

    // constructors, accessors, etc.
    }

@PortableType(id = 2, version = 1)
public class Dog extends Pet
    {
    @Portable
    private String breed;

    @Portable(since = 1)
    private Color color;

    // constructors, accessors, etc.
    }</markup>

<p>Notice that both <code>version</code> and <code>since</code> properties are zero-based, which allows you to omit them completely in the initial implementation. It also means that for the first subsequent revision they should be set to <code>1</code>.</p>

<p>Of course, those are just the defaults. You can certainly set the class and attribute version explicitly to any value even for the initial implementation, if you are so inclined. The only thing that matters is that you bump the version and set the <code>since</code> property to the latest version number whenever you make changes to the class in the future.</p>

<p>For example, if in the future we decide to add <code>height</code> and <code>weight</code> attributes to the <code>Pet</code> class, we would simply increment the <code>version</code> to <code>2</code> and set the <code>since</code> property for the new attributes accordingly:</p>

<markup
lang="java"

>@PortableType(id = 1, version = 2)
public class Pet
    {
    @Portable
    protected String name;

    @Portable(since = 1)
    protected int age;

    @Portable(since = 2)
    protected int height;

    @Portable(since = 2)
    protected int weight;

    // constructors, accessors, etc.
    }</markup>

<div class="admonition warning">
<p class="admonition-textlabel">Warning</p>
<p ><p>It may be obvious by now, but it&#8217;s probably worth calling out explicitly: class evolution allows you to add attributes to the new version of the class, but you should <strong>never</strong> remove existing attributes, as that will break serialization across class versions.</p>

<p>You can certainly remove or deprecate attribute <em>accessors</em> from the class, but you should leave the field itself as-is, in order to preserve backwards compatibility of the serialized form.</p>

<p>Along the same lines, you should avoid renaming the fields, as the default serialization order of fields is determined based on the alphabetical order of field names within a given class version (all fields with the same <code>since</code> value).</p>
</p>
</div>
</div>

<h3 id="_compile_time_instrumentation">Compile-time Instrumentation</h3>
<div class="section">
<p>Annotating the classes is the first step in the implementation of Portable Types, but it is not sufficient on its own. In order to implement the necessary serialization logic, the classes also need to be instrumented at compile time.</p>

<p>This is accomplished using the <code>pof-maven-plugin</code>, which should be configured in your POM file:</p>

<markup
lang="xml"

>&lt;plugin&gt;
  &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
  &lt;artifactId&gt;pof-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;20.12&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;id&gt;instrument&lt;/id&gt;
      &lt;goals&gt;
        &lt;goal&gt;instrument&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
    &lt;execution&gt;
      &lt;id&gt;instrument-tests&lt;/id&gt;
      &lt;goals&gt;
        &lt;goal&gt;instrument-tests&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
&lt;/plugin&gt;</markup>

<p>The configuration above will discover and instrument all project classes annotated with <code>@PortableType</code> annotation, including test classes. If you don&#8217;t need to instrument test classes you can omit the <code>instrument-tests</code> execution from the plugin configuration.</p>

<p>The <code>pof-maven-plugin</code> uses <code>Schema</code> support to define the type system that contains all reachable portable types. This type system includes not only project classes that need to be instrumented, but also all portable types that exist in project dependencies. This is necessary because those dependent types may be used as attributes within the project classes, and need to be serialized appropriately.</p>

<p>In some cases it may be necessary to expand the type system with the types that are not annotated with <code>@PortableType</code> annotation, and are not discovered automatically. This is typically the case when some of your portable types have enum values, or existing classes that implement <code>PortableObject</code> interface explicitly as attributes.</p>

<p>You can add those types to the schema by creating a <code>META-INF/schema.xml</code> file and specifying them explicitly. For example, assuming the <code>Color</code> class from the code examples above is an enum type, you would need to create the following <code>META-INF/schema.xml</code> file to register it and allow <code>pof-maven-plugin</code> to instrument <code>Dog</code> class correctly:</p>

<markup
lang="xml"

>&lt;?xml version="1.0"?&gt;

&lt;schema xmlns="http://xmlns.oracle.com/coherence/schema"
       xmlns:java="http://xmlns.oracle.com/coherence/schema/java"
       external="true"&gt;

  &lt;type name="Color"&gt;
    &lt;java:type name="petstore.Color"/&gt;
  &lt;/type&gt;

&lt;/schema&gt;</markup>

<p>Once all these bits and pieces are in place, you can simply run your build as usual:</p>

<markup
lang="text"

>$ mvn clean install</markup>

<p>You can verify that the classes were instrumented successfully by checking the Maven output log. You should see something similar to the following:</p>

<markup
lang="text"

>[INFO] --- pof-maven-plugin:20.12:instrument (instrument) @ petstore ---
[INFO] Running PortableTypeGenerator for classes in /projects/petstore/target/classes
[INFO] Instrumenting type petstore.Pet
[INFO] Instrumenting type petstore.Dog</markup>

<p>Once the classes are successfully instrumented, they are ready to be registered and used.</p>

</div>

<h3 id="_registration_and_discovery">Registration and Discovery</h3>
<div class="section">
<p>Portable Object Format is not a self-describing serialization format: it replaces platform-specific class names with integer-based <em>type identifiers</em>, so it needs a way of mapping those type identifiers back to the platform-specific classes. This enables <em>portability</em> across platforms, which was, as the name clearly says, the main objective of POF.</p>

<p>To manage the mappings between the type identifiers and concrete types, POF uses <code>com.tangosol.io.pof.PofContext</code>:</p>

<markup
lang="java"

>public interface PofContext extends Serializer
    {
    PofSerializer getPofSerializer(int nTypeId);

    int getUserTypeIdentifier(Object o);
    int getUserTypeIdentifier(Class&lt;?&gt; clz);
    int getUserTypeIdentifier(String sClass);

    String getClassName(int nTypeId);
    Class&lt;?&gt; getClass(int nTypeId);

    boolean isUserType(Object o);
    boolean isUserType(Class&lt;?&gt; clz);
    boolean isUserType(String sClass);
    }</markup>

<p>It is worth noting that <code>PofContext</code> extends <code>com.tangosol.io.Serializer</code> interface, which means that any <code>PofContext</code> implementation can be used wherever Coherence expects a <code>Serializer</code> to be specified: within cache services as a storage-level serializer for data classes, as a transport-level serializer between thin clients and the proxy servers, etc. The <code>PofContext</code> performs the actual serialization by delegating to the appropriate <code>PofSerializer</code>, which is obtained via the <code>PofContext.getPofSerializer</code> method, based on a type identifier.</p>

<p>There are several built-in implementations of <code>PofContext</code>. The <code>SimplePofContext</code> allows you to programmatically register type mappings by providing all the metadata needed for serialization, such as type identifier, class, and the <code>PofSerializer</code> to use:</p>

<markup
lang="java"

>SimplePofContext ctx = new SimplePofContext();
ctx.registerUserType(1, Pet.class, new PortableTypeSerializer&lt;&gt;(1, Pet.class));
ctx.registerUserType(2, Dog.class, new PortableTypeSerializer&lt;&gt;(2, Dog.class));
ctx.registerUserType(3, Color.class, new EnumPofSerializer());</markup>

<p>Notice that a lot of this information is somewhat repetitive and unnecessary when working with portable types, as all the metadata you need can be obtained from the class itself or the <code>@PortableType</code> annotation.</p>

<p>Because of that, <code>SimplePofContext</code> also provides several convenience methods, specifically for portable types:</p>

<markup
lang="java"

>ctx.registerPortableType(Pet.class);
ctx.registerPortableType(Dog.class);</markup>

<p>Or even simpler:</p>

<markup
lang="java"

>ctx.registerPortableTypes(Pet.class, Dog.class);</markup>

<p>While the <code>SimplePofContext</code> is useful for testing and quick prototyping, a <code>PofContext</code> implementation that is much more widely used within Coherence applications is <code>ConfigurablePofContext</code>.</p>

<p>The <code>ConfigurablePofContext</code> allows you to provide type mappings via an external XML file:</p>

<markup
lang="xml"

>&lt;pof-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-pof-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-pof-config coherence-pof-config.xsd"&gt;

  &lt;user-type-list&gt;

    &lt;user-type&gt;
      &lt;type-id&gt;1&lt;/type-id&gt;
      &lt;class-name&gt;petstore.Pet&lt;/class-name&gt;
    &lt;/user-type&gt;

    &lt;user-type&gt;
      &lt;type-id&gt;2&lt;/type-id&gt;
      &lt;class-name&gt;petstore.Dog&lt;/class-name&gt;
    &lt;/user-type&gt;

    &lt;user-type&gt;
      &lt;type-id&gt;3&lt;/type-id&gt;
      &lt;class-name&gt;petstore.Color&lt;/class-name&gt;
      &lt;serializer&gt;
        &lt;class-name&gt;com.tangosol.io.pof.EnumPofSerializer&lt;/class-name&gt;
      &lt;/serializer&gt;
    &lt;/user-type&gt;

  &lt;/user-type-list&gt;

&lt;/pof-config&gt;</markup>

<p>You may notice that we didn&#8217;t have to specify <code>serializer</code> explicitly for <code>Pet</code> and <code>Dog</code> classes. This is because <code>ConfigurablePofContext</code> has the logic to determine which of the built-in <code>PofSerializer</code> implementations to use depending on the interfaces implemented by, or the annotations present on the specified class. In this case, it will automatically use <code>PortableTypeSerializer</code> because the classes have <code>@PortableType</code> annotation.</p>

<p>However, we can make the configuration even simpler by enabling portable type discovery:</p>

<markup
lang="xml"

>&lt;pof-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-pof-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-pof-config coherence-pof-config.xsd"&gt;

  &lt;user-type-list&gt;

    &lt;user-type&gt;
      &lt;type-id&gt;3&lt;/type-id&gt;
      &lt;class-name&gt;petstore.Color&lt;/class-name&gt;
      &lt;serializer&gt;
        &lt;class-name&gt;com.tangosol.io.pof.EnumPofSerializer&lt;/class-name&gt;
      &lt;/serializer&gt;
    &lt;/user-type&gt;

  &lt;/user-type-list&gt;

  &lt;enable-type-discovery&gt;true&lt;/enable-type-discovery&gt;

&lt;/pof-config&gt;</markup>

<p>Once you set the <code>enable-type-discovery</code> flag to <code>true</code>, the <code>ConfigurablePofContext</code> will discover all the classes annotated with <code>@PortableType</code> and register them automatically, based on the annotation metadata. If we didn&#8217;t have the <code>Color</code> enum that has to be registered explicitly, we could even omit the configuration file completely, as the default <code>pof-config.xml</code> file that is built into Coherence looks like this:</p>

<markup
lang="xml"

>&lt;pof-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-pof-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-pof-config coherence-pof-config.xsd"&gt;

  &lt;user-type-list&gt;
    &lt;!-- by default just include coherence POF user types --&gt;
    &lt;include&gt;coherence-pof-config.xml&lt;/include&gt;
  &lt;/user-type-list&gt;

  &lt;enable-type-discovery&gt;true&lt;/enable-type-discovery&gt;

&lt;/pof-config&gt;</markup>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>The portable type discovery feature depends on the availability of a <a id="" title="" target="_blank" href="https://github.com/wildfly/jandex">Jandex</a> index within the modules that provide portable types that need to be registered.</p>

<p>Make sure that you configure Jandex Maven Plugin to index classes in your modules at build time:</p>

<markup
lang="xml"

>&lt;plugin&gt;
  &lt;groupId&gt;org.jboss.jandex&lt;/groupId&gt;
  &lt;artifactId&gt;jandex-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;1.0.8&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;id&gt;make-index&lt;/id&gt;
      &lt;goals&gt;
        &lt;goal&gt;jandex&lt;/goal&gt;
      &lt;/goals&gt;
      &lt;phase&gt;process-classes&lt;/phase&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
&lt;/plugin&gt;</markup>
</p>
</div>
</div>

<h3 id="_ide_support">IDE Support</h3>
<div class="section">
<p>Once you have annotated, instrumented and registered portable types as described in the sections above, you can use them with Coherence just as easily as you would use plain Java <code>Serializable</code> classes, by configuring Coherence services to use <code>pof</code> serializer instead of the default <code>java</code> serializer.</p>

<p>However, there is still one problem: serialization code is implemented by the <code>pof-maven-plugin</code> at compile-time, and only if you run Maven build, which can make it a bit cumbersome to run unit and integration tests within your IDE.</p>

<p>In order to solve that problem, we have implemented IDE plugins for IntelliJ IDEA and Eclipse, which can instrument your classes during incremental or full compilation performed by your IDE. This allows you to test both the serialization of your classes and the code that depends on it without having to run Maven build or leave your IDE.</p>

<p>Please follow the documentation for the <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-idea-plugin">Coherence IntelliJ Plugin</a> or <a id="" title="" target="_blank" href="https://github.com/oracle/coherence-eclipse-plugin">Coherence Eclipse Plugin</a> for detailed instructions on how to install and use the plugin for your favorite IDE.</p>

<div class="admonition note">
<p class="admonition-textlabel">Note</p>
<p ><p>We&#8217;ve used 1, 2, and 3 as type identifiers in the code and configuration examples above for simplicity, but it is worth noting that Coherence reserves type identifiers from 0 to 999 for internal use.</p>

<p>That means that you should only use type identifiers of 1000 or higher for your own classes.</p>
</p>
</div>
</div>
</div>
</doc-view>
