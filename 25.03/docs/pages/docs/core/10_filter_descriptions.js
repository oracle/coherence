<doc-view>

<h2 id="_query_and_trace_record_improvements">Query and Trace Record Improvements</h2>
<div class="section">
<p>In 25.03, we have improved the display of filters in the output of the Query and Trace Recorder. This also applies to the
<code>MaxQueryDescription</code> attribute on the <code>StorageManager</code> MBean.</p>

<p>Below are two examples of the updated filter descriptions, the first showing using Portable Object Format (POF), and <code>PortableTypes</code> and the second showing using standard Java serialization.</p>

<p>Using <code>PortableTypes</code> is the preferred method for serializing objects using POF as it provides the following benefits:</p>

<ol style="margin-left: 15px;">
<li>
You do not have to implement serialization logic as you just annotate the class and fields,
and then include the pof-maven-plugin to your build process to instrument the annotated classes

</li>
<li>
Instrumenting the classes automatically ensures they can evolve as you change your data model

</li>
<li>
Using the POF serialization format is very compact and can save considerable space

</li>
<li>
It is cross-platform compatible with C++, Java and .NET

</li>
</ol>
<p>See <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/fusion-middleware/coherence/14.1.2/develop-applications/using-portable-object-format.html">Using Portable Object Format</a> for more information on the above.</p>


<h3 id="_example_1_portable_types">Example 1 - Portable Types</h3>
<div class="section">
<p>Consider that we have the following classes annotated using <code>PortableTypes</code>.</p>

<div class="admonition note">
<p class="admonition-inline">We have not included all getters and setting for brevity.</p>
</div>
<markup
lang="java"
title="Employee"
>@PortableType(id=10000)
public static class Employee {
    @Portable
    private final String firstName;

    @Portable
    private final String lastName;

    @Portable
    private final int age;

    @Portable
    public Address homeAddress;

    @Portable
    public Address workAddress;

    // constructors, getters, setters, hashCode and toString omitted
}</markup>

<markup
lang="java"
title="Address"
>@PortableType(id = 10001)
public static class Address {
    @Portable
    private String addressLine1;

    @Portable
    private String addressLine2;

    @Portable
    private String city;

    @Portable
    private String state;

    @Portable
    private String zip;

    // constructors, getters, setters, hashCode and toString omitted
}</markup>

<p>Consider the following code which:</p>

<ol style="margin-left: 15px;">
<li>
Gets a cache and adds indexes

</li>
<li>
Creates a filter

</li>
<li>
Adds 15,000 entries to the cache

</li>
<li>
Runs a query explain

</li>
</ol>
<markup
lang="java"

>NamedCache&lt;String, Employee&gt; cache = CacheFactory.getCache("employees");

PofExtractor&lt;Employee, Integer&gt; age       = Extractors.fromPof(Employee.class, "age");
PofExtractor&lt;Employee, String&gt;  lastName  = Extractors.fromPof(Employee.class, "lastName");
PofExtractor&lt;Employee, String&gt;  firstName = Extractors.fromPof(Employee.class, "firstName");
PofExtractor&lt;Employee, String&gt;  homeCity  = Extractors.fromPof(Employee.class, "homeAddress.city");

cache.addIndex(age, true, null);
cache.addIndex(homeCity, true, null);

AllFilter filter = new AllFilter(new Filter[]
        {
        equal(age, 16).or(equal(age, 19)),
        equal(lastName, "Smith"),
        equal(firstName, "Bob"),
        equal(homeCity, "Boston"),
        });

populateCache(cache);

QueryRecorder&lt;String, Employee&gt; agent = new QueryRecorder&lt;&gt;(RecordType.EXPLAIN);
Object resultsExplain = cache.aggregate(filter, agent);
System.out.println("\nExplain Plan=\n" + resultsExplain + "\n");</markup>

<p>The filter and index output is much clearer and easy to read than in previous versions.</p>

<markup
lang="text"
title="Output"
>Explain Plan
Filter Name                                                         Index   Cost
======================================================================================
AllFilter                                                         | ----  | 0
  OrFilter                                                        | ----  | 0
    age == 16                                                     | 0     | 1500
    age == 19                                                     | 0     | 1500
  homeAddress.city == 'Boston'                                    | 1     | 7394
  lastName == 'Smith'                                             | 2     | 15000000
  firstName == 'Bob'                                              | 3     | 15000000


Index Lookups
Index Description                             Extractor                        Ordered
======================================================================================
0   | Partitioned: Footprint=1.76MB,... (0) | age                             | true
1   | Partitioned: Footprint=1.51MB, Size=2 | homeAddress.city                | true
2   | No index found                        | lastName                        | false
3   | No index found                        | firstName                       | false</markup>

<p>As a comparison, the following would have been output in previous versions:</p>

<markup
lang="text"
title="Old Output"
>Explain Plan
Filter Name                                                         Index   Cost
======================================================================================
AllFilter                                                         | ----  | 0
  OrFilter                                                        | ----  | 0
    EqualsFilter(PofExtractor(target=VALUE, navigator=Simp... (0) | 0     | 1500
    EqualsFilter(PofExtractor(target=VALUE, navigator=Simp... (1) | 0     | 1500
  EqualsFilter(PofExtractor(target=VALUE, navigator=Simple... (2) | 1     | 7517
  EqualsFilter(PofExtractor(target=VALUE, navigator=Simple... (3) | 2     | 15000000
  EqualsFilter(PofExtractor(target=VALUE, navigator=Simple... (4) | 3     | 15000000


Index Lookups
Index Description                             Extractor                        Ordered
======================================================================================
0   | Partitioned: Footprint=1.76MB,... (5) | PofExtractor(target=VALU... (6) | true
1   | Partitioned: Footprint=1.51MB, Size=2 | PofExtractor(target=VALU... (7) | true
2   | No index found                        | PofExtractor(target=VALU... (8) | false
3   | No index found                        | PofExtractor(target=VALU... (9) | false</markup>

</div>

<h3 id="_example_2_java_serialization">Example 2 - Java Serialization</h3>
<div class="section">
<p>In this example we have a <code>Person</code> class using Java serialization.</p>

<markup
lang="java"
title="Person.java"
>public class Person implements Serializable {
    private final String firstName;
    private final String lastName;
    private final int    age;

    // constructors, getters, setters, hashCode and toString omitted
}</markup>

<p>Consider the following code which:</p>

<ol style="margin-left: 15px;">
<li>
Gets a cache and add an index

</li>
<li>
Creates a filter

</li>
<li>
Adds 15,000 entries to the cache

</li>
<li>
Runs a query explain

</li>
</ol>
<markup
lang="java"
title="Example Query"
>NamedCache&lt;String, Person&gt; cache = CacheFactory.getCache("people");
cache.addIndex(Person::getAge, true, null);

AllFilter filter = new AllFilter(new Filter[]
    {
    Filters.equal(Person::getAge, 16).or(Filters.equal(Person::getAge, 19)),
    Filters.equal(Person::getLastName, "Smith"),
    Filters.equal(Person::getFirstName, "Bob"),
    });

// populate the cache
Map&lt;String, Person&gt; buffer = new HashMap&lt;&gt;();
for (int i = 0; i &lt; 15000; ++i)
    {
    Person person = new Person(i % 3 == 0 ? "Joe" : "Bob", i % 2 == 0 ? "Smith" : "Jones", 15 + i % 10);
    buffer.put("key" + i, person);
    }
cache.putAll(buffer);

QueryRecorder&lt;String, Person&gt; agent = new QueryRecorder&lt;&gt;(RecordType.EXPLAIN);
Object resultsExplain = cache.aggregate(filter, agent);
System.out.println("\nTrace =\n" + resultsExplain + "\n")</markup>

<p>In this version the output looks like the following, showing the filters in a more human readable format.</p>

<markup
lang="text"
title="New Results"
>Explain Plan
Filter Name                                                         Index   Cost
======================================================================================
AllFilter                                                         | ----  | 0
  OrFilter                                                        | ----  | 0
    age == 16                                                     | 0     | 1500
    age == 19                                                     | 0     | 1500
  lastName == 'Smith'                                             | 1     | 15000000
  firstName == 'Bob'                                              | 2     | 15000000

Index Lookups
Index Description                             Extractor                        Ordered
======================================================================================
0   | Partitioned: Footprint=1.76MB,... (0) | age                             | true
1   | No index found                        | lastName                        | false
2   | No index found                        | firstName                       | false

Complete filter and index descriptions
N     Full Name
======================================================================================
0   | Partitioned: Footprint=1.76MB, Size=10</markup>

<p>Whereas the previous versions the output was a bit more cryptic.</p>

<markup
lang="text"
title="Old Results"
>Explain Plan
Filter Name                                                         Index   Cost
======================================================================================
AllFilter                                                         | ----  | 0
  OrFilter                                                        | ----  | 0
    EqualsFilter(.getAge(), 16)                                   | 0     | 1500
    EqualsFilter(.getAge(), 19)                                   | 0     | 1500
  EqualsFilter(.getLastName(), Smith)                             | 1     | 15000000
  EqualsFilter(.getFirstName(), Bob)                              | 2     | 15000000

Index Lookups
Index Description                             Extractor                        Ordered
======================================================================================
0   | Partitioned: Footprint=1.76MB,... (0) | .getAge()                       | true
1   | No index found                        | .getLastName()                  | false
2   | No index found                        | .getFirstName()                 | false


Complete filter and index descriptions
N     Full Name
======================================================================================
0   | Partitioned: Footprint=1.76MB, Size=10</markup>

</div>
</div>
</doc-view>
