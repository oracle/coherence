<doc-view>

<h2 id="_streams">Streams</h2>
<div class="section">
<p>This guide walks you through how to use the Streams API with Coherence.</p>

<p>The Java streams implementation provides an efficient way to query and process data
sequentially or in parallel to take advantage of multi-core architectures.</p>

<p>The processing occurs in steps:</p>

<ol style="margin-left: 15px;">
<li>
Data is aggregated from a source (such as collections or arrays) into a read-only stream.
The stream represents object references and does not actually store the data.

</li>
<li>
Intermediate operations are then declared on the stream. Intermediate operations for filtering,
sorting, mapping, and so on are supported. Lambda expressions are often used when declaring
intermediate operations and provide a functional way to work on the data.
Intermediate operations are aggregated and can be chained together: each subsequent operation
is performed on a stream that contains the result of the previous operation. Intermediate operations are lazy and are not actually executed until a final terminal operation is performed.

</li>
<li>
A final terminal operation is declared. Terminal operations for counting, adding, averaging,
and so on are supported. The terminal operation automatically iterates over the objects in the
stream returns an aggregated result.

</li>
</ol>
<p>Java streams provide similar functionality as Coherence data grid aggregation.
However, streams are not efficient when executed in a distributed environment.
To leverage the stream programming model and also ensure that streams can be executed remotely
across the cluster, Coherence has extended the streams API.</p>

<p>For details see the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2.0/java-reference/com/tangosol/util/stream/package-summary.html">com.tangosol.util.stream</a>
in the Java API Reference for Oracle Coherence.</p>


<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-will-need" @click.native="this.scrollFix('#what-you-will-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#data-model" @click.native="this.scrollFix('#data-model')">Example Data Model</router-link></p>

</li>
<li>
<p><router-link to="#example-code-1" @click.native="this.scrollFix('#example-code-1')">Review the Example Code</router-link></p>

</li>
<li>
<p><router-link to="#run-example-1" @click.native="this.scrollFix('#run-example-1')">Run the Example</router-link></p>

</li>
<li>
<p><router-link to="#summary" @click.native="this.scrollFix('#summary')">Summary</router-link></p>

</li>
<li>
<p><router-link to="#see-also" @click.native="this.scrollFix('#see-also')">See Also</router-link></p>

</li>
</ul>
</div>

<h3 id="what-you-will-build">What You Will Build</h3>
<div class="section">
<p>In this example you will utilize streams methods on the <code>NamedMap</code> API to query and aggregate
and group data from a contacts <code>NamedMap</code>.</p>


<h4 id="what-you-will-need">What You Need</h4>
<div class="section">
<ul class="ulist">
<li>
<p>About 15 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 11</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included so they can be built without first installing
either build tool.</p>

</li>
<li>
<p>You can also import the code straight into your IDE:</p>
<ul class="ulist">
<li>
<p><router-link to="/examples/setup/intellij">IntelliJ IDEA</router-link></p>

</li>
</ul>
</li>
</ul>
</div>

<h4 id="_building_the_example_code">Building the Example Code</h4>
<div class="section">
<p>The source code for the guides and tutorials can be found in the
<a id="" title="" target="_blank" href="http://github.com/oracle/coherence/tree/master/prj/examples">Coherence CE GitHub repo</a></p>

<p>The example source code is structured as both a Maven and a Gradle project and can be easily built with either
of those build tools. The examples are stand-alone projects so each example can be built from the
specific project directory without needing to build the whole Coherence project.</p>

<ul class="ulist">
<li>
<p>Build with Maven</p>

</li>
</ul>
<p>Using the included Maven wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Build with Gradle</p>

</li>
</ul>
<p>Using the included Gradle wrapper the example can be built with the command:</p>

<markup
lang="bash"

>./gradlew build</markup>

</div>

<h4 id="running">Running the Examples</h4>
<div class="section">
<p>This example can be run direct from the IDE, or can be run via executing the tests.</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

</div>
</div>

<h3 id="data-model">Example Data Model</h3>
<div class="section">
<p>The data model consists of the following classes in two maps, <code>customers</code> and <code>orders</code></p>

<ul class="ulist">
<li>
<p><code>Contact</code> - Represents a contact</p>

</li>
<li>
<p><code>Address</code> - Represents an address for a contact</p>

</li>
</ul>

<h4 id="_contact">Contact</h4>
<div class="section">
<p>Contacts have various attributes as described below including home and work addresses stored in the <code>Address</code> class.</p>

<markup
lang="java"

>public class Contact
        implements Serializable {

    private int id;
    private String    firstName;
    private String    lastName;
    private LocalDate doB;
    private int       age;
    private Address   homeAddress;
    private Address   workAddress;</markup>

</div>

<h4 id="_address">Address</h4>
<div class="section">
<p>Address contains address details for a <code>Contact</code>.</p>

<markup
lang="java"

>public class Address
        implements Serializable {

    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String zip;</markup>

</div>
</div>

<h3 id="example-code-1">Review the Example Code</h3>
<div class="section">
<ol style="margin-left: 15px;">
<li>
Example Details
<p>The <code>runExample()</code> method contains the code that exercises the streams API.</p>

<div class="admonition note">
<p class="admonition-inline">Refer to the inline
code comments for explanations of what each operation is carrying out.</p>
</div>
<markup
lang="java"

>/**
 * Run the example.
 */
public void runExample() {
    NamedMap&lt;Integer, Contact&gt; contacts = getContacts();

    System.out.println("Cache size is " + contacts.size());

    // get the distinct years that the contacts were born in
    Set&lt;Integer&gt; setYears = contacts.stream(Contact::getDoB)
                                    .map(LocalDate::getYear)
                                    .distinct()
                                    .collect(RemoteCollectors.toSet());
    System.out.println("Distinct years the contacts were born in:\n" + setYears);

    // get a set of contact names where the age is &gt; 40
    Set&lt;String&gt; setNames = contacts.stream(greater(Contact::getAge, 60))
                                   .map(entry-&gt;entry.extract(Contact::getLastName) + " " +
                                               entry.extract(Contact::getFirstName) + " age=" +
                                               entry.extract(Contact::getAge))
                                   .collect(RemoteCollectors.toSet());
    System.out.println("\nSet of contact names where age &gt; 60:\n" + setNames);

    // get the distinct set of states for home addresses
    Set&lt;String&gt; setStates = contacts.stream(Contact::getHomeAddress)
                                    .map(Address::getState)
                                    .distinct()
                                    .collect(RemoteCollectors.toSet());
    System.out.println("\nDistinct set of states for home addresses:\n" + setStates);

    // get the average ages of all contacts
    double avgAge = contacts.stream(Contact::getAge)
                            .mapToInt(Number::intValue)
                            .average()
                            .orElse(0);  // in-case of no values
    System.out.println("\nThe average age of all contacts is: " + avgAge);

    // get average age using collectors
    avgAge = contacts.stream()
                     .collect(RemoteCollectors.averagingInt(Contact::getAge));
    System.out.println("\nThe average age of all contacts using collect() is: " + avgAge);

    // get the maximum age of all contacts
    int maxAge = contacts.stream(Contact::getAge)
                         .mapToInt(Number::intValue)
                         .max()
                         .orElse(0);  // in-case of no values
    System.out.println("\nThe maximum age of all contacts is: " + maxAge);

    // get average age of contacts who live in MA
    // Note: The filter should be applied as early as possible, e.g as an argument
    // to the stream() call in order to take advantage of indexes
    avgAge = RemoteStream.toIntStream(contacts.stream(equal(homeState(), "MA"), Contact::getAge))
                         .average()
                         .orElse(0);
    System.out.println("\nThe average age of contacts who work in MA is: " + avgAge);

    // get a map of birth months and the contact names for that month
    Map&lt;String, List&lt;Contact&gt;&gt; mapContacts =
            contacts.stream()
                    .map(Map.Entry::getValue)
                    .collect(RemoteCollectors.groupingBy(birthMonth()));
    System.out.println("\nContacts born in each month:");
    mapContacts.forEach(
            (key, value)-&gt;System.out.println("\nMonth: " + key + ", Contacts:\n" +
                                             displayNames(value)));

    // get a map of states and the contacts living in each state
    Map&lt;String, List&lt;Contact&gt;&gt; mapStateContacts =
            contacts.stream()
                    .map(Map.Entry::getValue)
                    .collect(RemoteCollectors.groupingBy(homeState()));
    System.out.println("\nContacts with home addresses in each state:");
    mapStateContacts.forEach(
            (key, value)-&gt;System.out.println("State " + key + " has " + value.size() +
                                             " Contacts\n" + displayNames(value)));
}</markup>

<p>The following static extractors are referenced in the above example:</p>

<markup
lang="java"

>/**
 * A {@link ValueExtractor} to extract the birth month from a {@link Contact}.
 *
 * @return the birth month
 */
protected static ValueExtractor&lt;Contact, String&gt; birthMonth() {
    return contact-&gt;contact.getDoB().getMonth().toString();
}

 /**
 * A {@link ValueExtractor} to extract the home state from a {@link Contact}.
 *
 * @return the home state
 */
protected static ValueExtractor&lt;Contact, String&gt; homeState() {
    return contact-&gt;contact.getHomeAddress().getState();
}</markup>

</li>
</ol>
</div>

<h3 id="run-example-1">Run the Example</h3>
<div class="section">
<p>Carry out the following to run this example:</p>

<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

<p>This will generate output similar to the following: (output is truncated)</p>

<markup
lang="bash"

>Creating 100 customers
Cache size is 100
Distinct years the contacts were born in:
[1984, 1985, 1986, 1987, 1989, 1950, 1951, 1952, 1953, 1954, 1955, 1956, 1957, 1958,
1959, 1960, 1961, 1962, 1963, 1964, 1966, 1967, 1968, 1969, 1970, 1971, 1972, 1973,
1974, 1975, 1976, 1977, 1979, 1980, 1981, 1983]

Set of contact names where age &gt; 60:
[Lastname12 Firstname12 age=64, Lastname100 Firstname100 age=70, Lastname77 Firstname77 age=63, Lastname82 Firstname82 age=66,
Lastname45 Firstname45 age=71, Lastname84 Firstname84 age=63, Lastname40 Firstname40 age=62, Lastname20 Firstname20 age=68,
Lastname63 Firstname63 age=68, Lastname85 Firstname85 age=69,
...
truncated
...
Lastname96 Firstname96 age=61, Lastname7 Firstname7 age=71, Lastname73 Firstname73 age=61, Lastname14 Firstname14 age=69,
Lastname35 Firstname35 age=61

Distinct set of states for home addresses:
[HI, TX, MA, TN, AK, WA, NY, AL, CA]

The average age of all contacts is: 52.48

The average age of all contacts using collect() is: 52.48

The maximum age of all contacts is: 72

The average age of contacts who work in MA is: 46.666666666666664

Contacts born in each month:

Month: JUNE, Contacts:
    Firstname77 Lastname77
    Firstname38 Lastname38
    Firstname32 Lastname32
    Firstname91 Lastname91
    Firstname48 Lastname48
    Firstname92 Lastname92
    Firstname80 Lastname80
    Firstname34 Lastname34

Month: JANUARY, Contacts:
    Firstname47 Lastname47
    Firstname94 Lastname94
    Firstname16 Lastname16
    Firstname46 Lastname46
    Firstname57 Lastname57
    Firstname10 Lastname10
    Firstname100 Lastname100
    Firstname4 Lastname4

Month: MAY, Contacts:
    Firstname65 Lastname65
    Firstname55 Lastname55
    Firstname1 Lastname1
    Firstname93 Lastname93
    Firstname96 Lastname96
    Firstname42 Lastname42
    Firstname14 Lastname14
    Firstname25 Lastname25
    Firstname54 Lastname54

...
truncated
...

Month: APRIL, Contacts:
    Firstname59 Lastname59
    Firstname15 Lastname15
    Firstname90 Lastname90
    Firstname50 Lastname50
    Firstname45 Lastname45
    Firstname33 Lastname33
    Firstname76 Lastname76
    Firstname23 Lastname23

Contacts with home addresses in each state:
State HI has 6 Contacts
    Firstname32 Lastname32
    Firstname68 Lastname68
    Firstname17 Lastname17
    Firstname42 Lastname42
    Firstname18 Lastname18
    Firstname39 Lastname39

State TX has 13 Contacts
    Firstname71 Lastname71
    Firstname30 Lastname30
    Firstname82 Lastname82
    Firstname62 Lastname62
    Firstname40 Lastname40
    Firstname43 Lastname43
    Firstname93 Lastname93
    Firstname11 Lastname11
    Firstname92 Lastname92
    Firstname96 Lastname96
    Firstname7 Lastname7
    Firstname58 Lastname58
    Firstname76 Lastname76

...
truncated
...

State AL has 10 Contacts
    Firstname47 Lastname47
    Firstname46 Lastname46
    Firstname22 Lastname22
    Firstname66 Lastname66
    Firstname81 Lastname81
    Firstname15 Lastname15
    Firstname25 Lastname25
    Firstname35 Lastname35
    Firstname34 Lastname34
    Firstname89 Lastname89

State CA has 14 Contacts
    Firstname77 Lastname77
    Firstname61 Lastname61
    Firstname28 Lastname28
    Firstname5 Lastname5
    Firstname1 Lastname1
    Firstname91 Lastname91
    Firstname87 Lastname87
    Firstname79 Lastname79
    Firstname80 Lastname80
    Firstname12 Lastname12
    Firstname33 Lastname33
    Firstname95 Lastname95
    Firstname98 Lastname98
    Firstname100 Lastname100</markup>

</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this example you have seen how to utilized streams methods on the <code>NamedMap</code> API to query and aggregate
and group data from a contacts <code>NamedMap</code>.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="/examples/guides/120-built-in-aggregators/README">Built in Aggregators</router-link></p>

</li>
<li>
<p><router-link to="/examples/guides/121-custom-aggregators/README">Custom Aggregators</router-link></p>

</li>
</ul>
</div>
</div>
</doc-view>
