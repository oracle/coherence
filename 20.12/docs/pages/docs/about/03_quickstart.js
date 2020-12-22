<doc-view>

<h2 id="_quick_start">Quick Start</h2>
<div class="section">

<h3 id="_prerequisites">Prerequisites</h3>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Java - jdk8 or higher

</li>
<li>
Maven - 3.6.3 or higher

</li>
</ol>
</div>

<h3 id="_how_to_get_coherence_community_edition">How to Get Coherence Community Edition</h3>
<div class="section">
<p>As Coherence is generally embedded into an application by using Coherence APIs,
the natural place to consume this dependency is from Maven:</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
        &lt;version&gt;20.06&lt;/version&gt;
    &lt;/dependency&gt;
&lt;/dependencies&gt;</markup>

<p>You can also get Coherence from the official <a id="" title="" target="_blank" href="https://hub.docker.com/r/oraclecoherence/coherence-ce">Docker image</a>.
For other language clients, use     (<a id="" title="" target="_blank" href="http://github.com/oracle/coherence-cpp-extend-client">C&#43;&#43;</a> and
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence-dotnet-extend-client">.NET</a>), and for the non-community edition, see <a id="" title="" target="_blank" href="https://www.oracle.com/middleware/technologies/coherence-downloads.html">Oracle Technology Network</a>.</p>

</div>
</div>

<h2 id="_cli_hello_coherence">CLI Hello Coherence</h2>
<div class="section">
<p>The following example illustrates the procedure to start a <strong>storage enabled</strong> Coherence Server, followed by a <strong>storage disabled</strong>
Coherence Console.
Using the console, data is inserted, retrieved, and then the console is terminated. The console is restarted
and data is once again retrieved to illustrate the permanence of the data.</p>

<div class="admonition note">
<p class="admonition-inline">This example uses the out-of-the-box cache configuration and therefore explicitly specifying the console is
storage disabled is unnecessary.</p>
</div>
<div class="admonition note">
<p class="admonition-inline">Coherence cluster members discover each other via one of two mechanisms;
multicast (default) or Well Known Addressing (deterministic broadcast).
If your system does not support multicast, enable WKA by specifying <code>-Dcoherence.wka=localhost</code> for both processes
started in the following console examples.</p>
</div>

<h3 id="_cohql_console"><a name="cohql"></a>CohQL Console</h3>
<div class="section">
<p>To run a CohQL console:</p>

<markup
lang="shell"

>$&gt; mvn -DgroupId=com.oracle.coherence.ce -DartifactId=coherence -Dversion=20.06 dependency:get

$&gt; export COH_JAR=~/.m2/repository/com/oracle/coherence/ce/coherence/20.06/coherence-20.06.jar

$&gt; java -jar $COH_JAR &amp;

$&gt; java -cp $COH_JAR com.tangosol.coherence.dslquery.QueryPlus

CohQL&gt; select * from welcomes

CohQL&gt; insert into welcomes key 'english' value 'Hello'

CohQL&gt; insert into welcomes key 'spanish' value 'Hola'

CohQL&gt; insert into welcomes key 'french' value 'Bonjour'

CohQL&gt; select key(), value() from welcomes
Results
["french", "Bonjour"]
["english", "Hello"]
["spanish", "Hola"]

CohQL&gt; bye

$&gt; java -cp $COH_JAR com.tangosol.coherence.dslquery.QueryPlus

CohQL&gt; select key(), value() from welcomes
Results
["french", "Bonjour"]
["english", "Hello"]
["spanish", "Hola"]

CohQL&gt; bye

$&gt; kill %1</markup>

</div>

<h3 id="_coherence_console"><a name="coh-console"></a>Coherence Console</h3>
<div class="section">
<p>To run the Coherence console:</p>

<markup
lang="shell"

>$&gt; mvn -DgroupId=com.oracle.coherence.ce -DartifactId=coherence -Dversion=20.06 dependency:get

$&gt; export COH_JAR=~/.m2/repository/com/oracle/coherence/ce/coherence/20.06/coherence-20.06.jar

$&gt; java -jar $COH_JAR &amp;

$&gt; java -cp $COH_JAR com.tangosol.net.CacheFactory

Map (?): cache welcomes

Map (welcomes): get english
null

Map (welcomes): put english Hello
null

Map (welcomes): put spanish Hola
null

Map (welcomes): put french Bonjour
null

Map (welcomes): get english
Hello

Map (welcomes): list
french = Bonjour
spanish = Hola
english = Hello

Map (welcomes): bye

$&gt; java -cp $COH_JAR com.tangosol.net.CacheFactory

Map (?): cache welcomes

Map (welcomes): list
french = Bonjour
spanish = Hola
english = Hello

Map (welcomes): bye

$&gt; kill %1</markup>

</div>
</div>

<h2 id="_programmatic_hello_coherence_example"><a name="hello-coh"></a>Programmatic Hello Coherence Example</h2>
<div class="section">
<p>The following example illustrates starting a <strong>storage enabled</strong> Coherence server, followed by running the <code>HelloCoherence</code>
application.
The <code>HelloCoherence</code> application inserts and retrieves data from the Coherence server.</p>


<h3 id="_build_hellocoherence">Build <code>HelloCoherence</code></h3>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Create a maven project either manually or by using an archetype such as maven-archetype-quickstart

</li>
<li>
Add a dependency to the pom file:

</li>
</ol>
<markup
lang="xml"
title="pom.xml"
>&lt;dependencies&gt;
    &lt;dependency&gt;
        &lt;groupId&gt;com.oracle.coherence.ce&lt;/groupId&gt;
        &lt;artifactId&gt;coherence&lt;/artifactId&gt;
        &lt;version&gt;20.06&lt;/version&gt;
    &lt;/dependency&gt;
&lt;/dependencies&gt;</markup>

<ol style="margin-left: 15px;">
<li>
Copy and paste the following source to a file named src/main/java/HelloCoherence.java:

</li>
</ol>
<markup
lang="java"
title="HelloCoherence.java"
>import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedMap

public class HelloCoherence
    {
    // ----- static methods -------------------------------------------------

    public static void main(String[] asArgs)
        {
        NamedMap&lt;String, String&gt; map = CacheFactory.getCache("welcomes");

        System.out.printf("Accessing map \"%s\" containing %d entries\n",
                map.getName(),
                map.size());

        map.put("english", "Hello");
        map.put("spanish", "Hola");
        map.put("french" , "Bonjour");

        // list
        map.entrySet().forEach(System.out::println);
        }
    }</markup>

<ol style="margin-left: 15px;">
<li>
Compile the maven project:
<markup
lang="shell"

>mvn package</markup>

</li>
<li>
Start a Storage server
<markup
lang="shell"

>mvn exec:java -Dexec.mainClass="com.tangosol.net.DefaultCacheServer" &amp;</markup>

</li>
<li>
Run <code>HelloCoherence</code>
<markup
lang="shell"

>mvn exec:java -Dexec.mainClass="HelloCoherence"</markup>

</li>
<li>
Confirm that you see the output including the following:
<markup
lang="shell"

>Accessing map "welcomes" containing 3 entries
ConverterEntry{Key="french", Value="Bonjour"}
ConverterEntry{Key="spanish", Value="Hola"}
ConverterEntry{Key="english", Value="Hello"}</markup>

</li>
<li>
Kill the storage server started earlier:
<markup
lang="shell"

>kill %1</markup>

</li>
</ol>
</div>
</div>

<h2 id="_building"><a name="build"></a>Building</h2>
<div class="section">
<markup
lang="shell"

>$&gt; git clone git@github.com:oracle/coherence.git
$&gt; cd coherence/prj

# build all modules
$&gt; mvn clean install

# build all modules skipping tests
$&gt; mvn clean install -DskipTests

# build a specific module, including all dependent modules and run tests
$&gt; mvn -am -pl test/functional/persistence clean verify

# build only coherence.jar without running tests
$&gt; mvn -am -pl coherence clean install -DskipTests

# build only coherence.jar and skip compilation of CDBs and tests
$&gt; mvn -am -pl coherence clean install -DskipTests -Dtde.compile.not.required</markup>

</div>
</doc-view>
