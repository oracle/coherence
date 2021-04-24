<doc-view>

<h2 id="_repository_api">Repository API</h2>
<div class="section">
<p>Coherence Repository API provides a higher-level, DDD-friendly way to access data managed in Coherence. It is implemented on top of the existing <code>NamedMap</code> API, but it provides a number of features that make it easier to use for many typical use cases where Coherence is used as a Key-Value data store.</p>


<h3 id="_features_and_benefits">Features and Benefits</h3>
<div class="section">
<p>In addition to the basic CRUD functionality, the Repository API provides many features that simplify common data management tasks:</p>

<ul class="ulist">
<li>
<p>Powerful projection features</p>

</li>
<li>
<p>Flexible in-place entity updates</p>

</li>
<li>
<p>First-class data aggregation support</p>

</li>
<li>
<p>Stream API support</p>

</li>
<li>
<p>Event listener support</p>

</li>
<li>
<p>Declarative acceleration and index creation</p>

</li>
<li>
<p>CDI Support</p>

</li>
</ul>
</div>

<h3 id="_implementing_a_repository">Implementing a Repository</h3>
<div class="section">
<p>Coherence provides an abstract base class <code>com.oracle.coherence.repository.AbstractRepository</code>, which your custom repository implementation needs to extend and provide implementation of three abstract methods:</p>


<p>For example, a repository implementation that can be used to store <code>Person</code> entities, with <code>String</code> identifiers, can be as simple as:</p>

<markup
lang="java"

>public class PeopleRepository
        extends AbstractRepository&lt;String, Person&gt;
    {
    private NamedMap&lt;String, Person&gt; people;

    public PeopleRepository(NamedMap&lt;String, Person&gt; people)
        {
        this.people = people;
        }

    protected NamedMap&lt;String, Person&gt; getMap()            <span class="conum" data-value="1" />
        {
        return people;
        }

    protected String getId(Person person)                  <span class="conum" data-value="2" />
        {
        return person.getSsn();
        }

    protected Class&lt;? extends Person&gt; getEntityType()      <span class="conum" data-value="3" />
        {
        return Person.class;
        }
    }</markup>

<ul class="colist">
<li data-value="1">The <code>getMap</code> method returns the <code>NamedMap</code> that should be used as a backing data store for the repository, which is in this case provided via constructor argument, but could just as easily be injected via CDI</li>
<li data-value="2">The <code>getId</code> method returns an identifier for a given entity</li>
<li data-value="3">The <code>getEntityType</code> method returns the class of the entities stored in the repository</li>
</ul>
<p>That is it in a nutshell: a trivial repository implementation above will allow you to access all the Repository API features described in the remaining sections, which are provided by the <code>AbstractRepository</code> class you extended.</p>

<p>However, you are free (and encouraged) to add additional business methods to the repository class above that will make it easier to use within your application. The most common example of such methods would be various "finder" methods that your application needs. For example, if your application needs to frequently query the repository to find people based on their name, you may want to add a method for that purpose:</p>

<markup
lang="java"

>    public Collection&lt;Person&gt; findByName(String name)
        {
        Filter&lt;Person&gt; filter = Filters.like(Person::getFirstName, name)
                                    .or(Filters.like(Person::getLastName, name));
        return getAll(filter);
        }</markup>

<p>You can then invoke <code>findByName</code> method directly within the application to find all the people whose first or last name starts with a letter <code>A</code>, for example:</p>

<markup
lang="java"

>for (Person p : people.findByName("A%"))
    {
    // processing
    }</markup>

</div>

<h3 id="_basic_crud_operations">Basic CRUD Operations</h3>
<div class="section">
<p>We&#8217;ve already seen one read operation, <code>getAll</code>, in the example above, but let&#8217;s start from the beginning and look into how we can add, remove, update and query our repository.</p>

<p>To add new entities to the repository, or replace the existing ones, you can use either the <code>save</code> or the <code>saveAll</code> method.</p>

<p>The former takes a single entity as an argument and stores it in the backing <code>NamedMap</code>:</p>

<markup
lang="java"

>people.save(new Person("555-11-2222", "Aleks", 46));</markup>

<p>The latter allows you to store a batch of entities at once by passing either a collection or a stream of entities as an argument.</p>

<p>Once you have some entities stored in a repository, you can query the repository using <code>get</code> and <code>getAll</code> methods.</p>

<markup
lang="java"

>Person person = people.get("555-11-2222");                                                <span class="conum" data-value="1" />
assert person.getName().equals("Aleks");
assert person.getAge() == 46;

Collection&lt;Person&gt; allPeople = people.getAll();                                           <span class="conum" data-value="2" />
Collection&lt;Person&gt; allAdults = people.getAll(Filters.greaterOrEqual(Person::getAge, 18)); <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">get a single <code>Person</code> by identifier</li>
<li data-value="2">get all the people from the repository</li>
<li data-value="3">get all the people from the repository that are 18 or older</li>
</ul>
<p>You can retrieve sorted results by calling <code>getAllOrderedBy</code> method and specifying a <code>Comparable</code> property via a method reference:</p>

<markup
lang="java"

>Collection&lt;Person&gt; peopleOrderedByAge = people.getAllOrderedBy(Person::getAge)     <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">the result will contain all people from the repository, sorted by age from the youngest to the oldest</li>
</ul>
<p>For more complex use cases, you can specify a <code>Comparator</code> to use instead. For example, if we wanted to always sort the results of the <code>findByName</code> method defined above first by last name and then by first name, we could re-implement it as:</p>

<markup
lang="java"

>    public Collection&lt;Person&gt; findByName(String name)
        {
        Filter&lt;Person&gt; filter = Filters.like(Person::getFirstName, name)
                                    .or(Filters.like(Person::getLastName, name));
        return getAllOrderedBy(filter,
                               Remote.comparator(Person::getLastName)
                                     .thenComparing(Person::getFirstName));     <span class="conum" data-value="1" />
        }</markup>

<ul class="colist">
<li data-value="1">the results will be sorted by last name, and then by first name; note that we are using Coherence <code>Remote.comparator</code> instead of standard Java <code>Comparator</code> in order to ensure that the specified comparator is serializable and can be sent to remote cluster members</li>
</ul>
<p>Finally, to remove entities from a repository you can use one of the several <code>remove</code> methods:</p>

<markup
lang="java"

>boolean fRemoved = people.remove(person);              <span class="conum" data-value="1" />
boolean fRemoved = people.removeById("111-22-3333");   <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">removes specified entity from the repository</li>
<li data-value="2">removes entity with the specified identifier from the repository</li>
</ul>
<p>In both examples above the result will be a boolean indicating whether the entity was actually removed from the backing <code>NamedMap</code>, and it may be <code>false</code> if the entity wasn&#8217;t present in the repository.</p>

<p>If you are interested in the removed value itself, you can use the overloads of the methods above that allow you to express that:</p>

<markup
lang="java"

>Person removed = people.remove(person, true);              <span class="conum" data-value="1" />
Person removed = people.removeById("111-22-3333", true);   <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">removes specified entity from the repository and returns it as the result</li>
<li data-value="2">removes entity with the specified identifier from the repository and returns it as the result</li>
</ul>
<p>Note that this will result in additional network traffic, so unless you really need the removed entity it is probably best not to ask for it.</p>

<p>The examples above are useful when you want to remove a single entity from the repository. In cases when you want to remove multiple entities as part of a single network call, you should use one of <code>removeAll</code> methods instead, which allow you to remove a set of entities by specifying either their identifiers explicitly, or the criteria for removal via the <code>Filter</code>.</p>

<markup
lang="java"

>boolean fChanged = people.removeAll(Filters.equal(Person::getGender, Gender.MALE)); <span class="conum" data-value="1" />
boolean fChanged = people.removeAllById(Set.of("111-22-3333", "222-33-4444"));      <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">removes all men from the repository and returns <code>true</code> if any entity has been removed</li>
<li data-value="2">removes entities with the specified identifiers from the repository and returns <code>true</code> if any entity has been removed</li>
</ul>
<p>Just like with single-entity removal operations, you can also use overloads that allow you to return the removed entities as the result:</p>

<markup
lang="java"

>Map&lt;String, Person&gt; mapRemoved =
        people.removeAll(Filters.equal(Person::getGender, Gender.MALE), true);  <span class="conum" data-value="1" />
Map&lt;String, Person&gt; mapRemoved =
        people.removeAllById(Set.of("111-22-3333", "222-33-4444"), true);       <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">removes all men from the repository and returns the map of removed entities, keyed by identifier</li>
<li data-value="2">removes entities with the specified identifiers from the repository and returns the map of removed entities, keyed by identifier</li>
</ul>
</div>

<h3 id="_projection">Projection</h3>
<div class="section">
<p>While querying repository for a collection of entities that satisfy some criteria is certainly a common and useful operation, sometimes you don&#8217;t need all the attributes within the entity. For example, if you only need a person&#8217;s name, querying for and then discarding all the information contained within the <code>Person</code> instances is unnecessary and wasteful.</p>

<p>It is the equivalent of executing</p>

<markup
lang="sql"

>SELECT * FROM PEOPLE</markup>

<p>against a relational database, when a simple</p>

<markup
lang="sql"

>SELECT name FROM PEOPLE</markup>

<p>would suffice.</p>

<p>Coherence Repository API allows you to limit the amount of data collected by performing server-side projection of the entity attributes you are interested in. For example, if you only need a person&#8217;s name, you can get just the name:</p>

<markup
lang="java"

>String name  = people.get("111-22-3333", Person::getName);                 <span class="conum" data-value="1" />
Map&lt;String, String&gt; mapNames =
        people.getAll(Filters.less(Person::getAge, 18), Person::getName);  <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">return the name of the person with a specified identifier</li>
<li data-value="2">return the map of names of all the people younger than 18, keyed by person&#8217;s identifier</li>
</ul>
<p>Obviously, returning either the whole entity or a single attribute from an entity are two ends of the spectrum, and more often than not you need something in between. For example, you may need the person&#8217;s name and age. For situations like that, Coherence allows you to use <em>fragments</em>:</p>

<markup
lang="java"

>Fragment&lt;Person&gt; fragment = people.get("111-22-3333",
                                       Extractors.fragment(Person::getName, Person::getAge));  <span class="conum" data-value="1" />
String name = fragment.get(Person::getName);  <span class="conum" data-value="2" />
int    age  = fragment.get(Person::getAge);   <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">return a fragment containing the name and age of the person with a specified identifier</li>
<li data-value="2">retrieve the person&#8217;s name from a fragment</li>
<li data-value="3">retrieve the person&#8217;s age from a fragment</li>
</ul>
<p>You can, of course, perform the same projection across multiple entities using one of <code>getAll</code> methods:</p>

<markup
lang="java"

>Map&lt;String, Fragment&lt;Person&gt;&gt; fragments = people.getAll(
        Filters.less(Person::getAge, 18),
        Extractors.fragment(Person::getName, Person::getAge));  <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">return a map of fragments containing the name and age of all the people younger than 18, keyed by person&#8217;s identifier</li>
</ul>
<p>Unlike the relational database, which contains a set of columns for each row in the table, Coherence stores each entity as a full object graph, which means that the attributes can be other object graphs and can be nested to any level. This means that we also may need to be able to project attributes of the nested objects. For example, our <code>Person</code> class may have a nested <code>Address</code> object as an attribute, which in turn has <code>street</code>, <code>city</code>, and <code>country</code> attributes. If we want to retrieve the name and the country of a person in a repository, we can do it like this:</p>

<markup
lang="java"

>Fragment&lt;Person&gt; person = people.get(
        "111-22-3333",
        Extractors.fragment(Person::getName,
                            Extractors.fragment(Person::getAddress, Address::getCountry)));  <span class="conum" data-value="1" />
String            name    = person.get(Person::getName);                <span class="conum" data-value="2" />
Fragment&lt;Address&gt; address = person.getFragment(Person::getAddress);     <span class="conum" data-value="3" />
String            country = address.get(Address::getCountry);           <span class="conum" data-value="4" /></markup>

<ul class="colist">
<li data-value="1">return a fragment containing the name and the <code>Address</code> fragment of the person with a specified identifier</li>
<li data-value="2">retrieve the person&#8217;s name from the <code>Person</code> fragment</li>
<li data-value="3">retrieve the <code>Address</code> fragment from the <code>Person</code> fragment</li>
<li data-value="4">retrieve the person&#8217;s country from the <code>Address</code> fragment</li>
</ul>
</div>

<h3 id="_in_place_updates">In-place Updates</h3>
<div class="section">
<p>By far the most common approach for updating data in modern applications is the read-modify-write pattern. For example, the typical code to update an attribute of a <code>Person</code> may look similar to the following:</p>

<markup
lang="java"

>Person person = people.get("111-22-3333");
person.setAge(55);
people.save(person);</markup>

<p>This is true regardless of whether the underlying data store provides a better, more efficient way of updating data. For example, RDBMS provide stored procedures for that purpose, but very few developers use them because they are not as convenient to use, and do not fit well into popular application frameworks, such as JPA, Spring Data or Micronaut Data. They also fragment the code base to some extent, splitting the business logic across the application and the data store, and require that some application code is written in SQL.</p>

<p>However, the approach above is suboptimal, for a number of reasons:</p>

<ol style="margin-left: 15px;">
<li>
It at least doubles the number of network calls the application makes to the data store, increasing the overall latency of the operation.

</li>
<li>
It moves (potentially a lot) more data over the network than absolutely necessary.

</li>
<li>
It may require expensive construction of a complex entity in order to perform a very simple update operation of a single attribute (this is particularly true with JPA and RDBMS back ends).

</li>
<li>
It puts additional, unnecessary load on the data store, which is typically the hardest component of the application to scale.

</li>
<li>
It introduces concurrency issues (ie. what should happen if the entity in the data store changes between the initial read and subsequent write), which typically requires that both the read and the write happen within the same transaction.

</li>
</ol>
<p>A much better, more efficient way to perform the updates is to send the update <em>function</em> to the data store, and execute it locally, within the data store itself (which is pretty much what stored procedures are for).</p>

<p>Coherence has always had support for these types of updates via <em>entry processors</em>, but the Repository API makes it even simpler to do so. For example, the code above can be rewritten as:</p>

<markup
lang="java"

>people.update("111-22-3333", Person::setAge, 55);</markup>

<p>We are basically telling Coherence to update <code>Person</code> instance with a given identifier by calling <code>setAge</code> method on it with a number 55 as an argument. This is not only significantly more efficient, but I&#8217;m sure you&#8217;ll agree, shorter and easier to write, and to read.</p>

<p>Note that we don&#8217;t know, or care, where in the cluster a <code>Person</code> instance with a given identifier is&#8201;&#8212;&#8201;all we care about is that Coherence guarantees that it will invoke the <code>setAge</code> method on the entity with a specified ID, on a <em>primary owner</em>, and automatically create a backup of the modified entity for fault tolerance.</p>

<p>It is also worth pointing out that the approach above provides the same benefits stored procedures do in RDBMS, but without the downsides: you are still writing all your code in Java, and keeping it in the same place. As a matter of fact, this approach allows you to implement rich domain models for your data, and execute business logic on your entities remotely, which works exceptionally well with DDD applications.</p>

<p>Calling a setter on an entity remotely is only the tip of the iceberg, and far from sufficient for all data mutation needs. For example, conventional JavaBean setter returns <code>void</code>, but you often want to know what the entity value is after the update. The solution to that problem is simple: Coherence will return the result of the specified method invocation, so all you need to do is change the <code>setAge</code> method to implement fluent API:</p>

<markup
lang="java"

>public Person setAge(int age)
    {
    this.age = age;
    return this;
    }</markup>

<p>You will now get the modified <code>Person</code> instance as the result of the <code>update</code> call:</p>

<markup
lang="java"

>Person person = people.update("111-22-3333", Person::setAge, 55);
assert person.getAge() == 55;</markup>

<p>Sometimes you need to perform more complex updates, or update multiple attributes at the same time. While you could certainly accomplish both of those by making multiple <code>update</code> calls, that is inefficient because each <code>update</code> will result in a separate network call. You are better off using the <code>update</code> overload that allows you to specify the function to execute in that situation:</p>

<markup
lang="java"

>Person person = people.update("111-22-3333", p -&gt;
    {
    p.setAge(55);
    p.setGender(Gender.MALE);
    return p;
    });

assert person.getAge() == 55;
assert person.getGender() == Gender.MALE;</markup>

<p>This way you have full control of the update logic that will be executed, and the return value.</p>

<p>You may sometimes want to update an entity that does not exist in the repository yet, in which case you want to create a new instance. For example, you may want to create a shopping cart entity for a customer when they add the first item to the cart. While you could implement the code to check whether the <code>Cart</code> for a given customer exists, and create new one if it doesn&#8217;t, this again results in network calls that can be avoided if you simply create the <code>Cart</code> instance as part of <code>Cart::addItem</code> call. The Repository API allows you to accomplish that via optional <code>EntityFactory</code> argument:</p>

<markup
lang="java"

>carts.update(customerId,                  <span class="conum" data-value="1" />
             Cart::addItem,               <span class="conum" data-value="2" />
             item,                        <span class="conum" data-value="3" />
             Cart::new);                  <span class="conum" data-value="4" /></markup>

<ul class="colist">
<li data-value="1">the cart/customer identifier</li>
<li data-value="2">the method to invoke on a target <code>Cart</code> instance</li>
<li data-value="3">the <code>CartItem</code> to add to the cart</li>
<li data-value="4">the <code>EntityFactory</code> to use to create a new <code>Cart</code> instance if the cart with the specified identifier doesn&#8217;t exist</li>
</ul>
<p>The <code>EntityFactory</code> interface is quite simple:</p>

<markup
lang="java"

>@FunctionalInterface
public interface EntityFactory&lt;ID, T&gt;
        extends Serializable
    {
    /**
     * Create an entity instance with the specified identity.
     *
     * @param id identifier to create entity instance with
     *
     * @return a created entity instance
     */
    T create(ID id);
    }</markup>

<p>Basically, it has a single <code>create</code> method that accepts entity identifier and returns a new instance of the entity with a given identifier. In the example above, that implies that our <code>Cart</code> class has a constructor similar to this:</p>

<markup
lang="java"

>public Cart(Long cartId)
    {
    this.cartId = cartId;
    }</markup>

<p>Just like with projections and other operations, in addition to <code>update</code> methods that can be used to modify a single entity, there are also a number of <code>updateAll</code> methods that can be used to modify multiple entities in a single call. An example where this may be useful is when you want to apply the same exact function to multiple entities, as is the case when performing stock split:</p>

<markup
lang="java"

>positions.updateAll(
        Filters.equal(Position::getSymbol, "AAPL"),     <span class="conum" data-value="1" />
        Position::split, 5);                            <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">the <code>Filter</code> used to determine the set of positions to update</li>
<li data-value="2">the function to apply to each position; in this case <code>split(5)</code> will be called on each <code>Position</code> entity with <code>AAPL</code> symbol</li>
</ul>
<p>Just like with single-entity updates, the result of each function invocation will be returned to the client, this time in the form of a <code>Map</code> containing the identifiers of the processed entities as keys, and the result of the function applied to that entity as the value.</p>

</div>

<h3 id="_stream_api_and_data_aggregation">Stream API and Data Aggregation</h3>
<div class="section">
<p>We&#8217;ve already covered how you can query the repository to retrieve a subset of entities using a <code>getAll</code> method and a <code>Filter</code>, but sometimes you don&#8217;t need the entities themselves, but a result of some computation applied to a subset of entities in the repository. For example, you may need to calculate average salary of all the employees in a department, or the total value of all equity positions in a portfolio.</p>

<p>While you could certainly query the repository for the entities that need to be processed and perform processing itself on the client, this is very inefficient way to accomplish the task, as you may end up moving significant amount of data over the network, just to discard it after the client-side processing.</p>

<p>As you&#8217;ve probably noticed by now, Coherence provides a number of feature that allow you to perform various types of distributed processing efficiently, and this situation is no exception. Just like the in-place updates leverage Coherence Entry Processor API to perform data mutation on cluster members that store the data, Repository API support for data aggregation leverages Coherence Remote Stream API and the Aggregation API to perform read-only distributed computations efficiently. This once again allows you to move processing to the data, instead of the other way around, and to perform computation in parallel across as many CPU cores as your cluster has, instead of a handful of (or in many cases only one) cores on the client.</p>

<p>The first option is to use the Stream API, which you are probably already familiar with because it&#8217;s a standard Java API introduced in Java 8. For example, you could calculate the average salary of all employees like this:</p>

<markup
lang="java"

>double avgSalary = employees.stream()
         .collect(RemoteCollectors.averagingDouble(Employee::getSalary));</markup>

<p>If you wanted to calculate average salary only for the employees in a specific department instead, you could filter the employees to process:</p>

<markup
lang="java"

>double avgSalary = employees.stream()
         .filter(e -&gt; e.getDepartmentId == departmentId)
         .collect(RemoteCollectors.averagingDouble(Employee::getSalary));</markup>

<p>However, while it works, the code above is not ideal, as it will end up processing, and potentially deserializing all the employees in the repository in order to determine whether they belong to a specified department.</p>

<p>A better way to accomplish the same task is to use Coherence-specific <code>stream</code> method overload which allows you to specify the <code>Filter</code> to create a stream based on:</p>

<markup
lang="java"

>double avgSalary = employees.stream(Filters.equal(Employee::getDepartmentId, departmentId))
         .collect(RemoteCollectors.averagingDouble(Employee::getSalary));</markup>

<p>The difference is subtle, but important: unlike previous example, this allows Coherence to perform query <em>before</em> creating the stream, and leverage any indexes you may have in the process. This can significantly reduce the overhead when dealing with large data sets.</p>

<p>However, there is also an easier way to accomplish the same thing:</p>

<markup
lang="java"

>double avgSalary = employees.average(Employee::getSalary);</markup>

<p>or, for a specific department:</p>

<markup
lang="java"

>double avgSalary = employees.average(
        Filters.equal(Employee::getDepartmentId, departmentId),
        Employee::getSalary);</markup>

<p>These are the examples of using repository aggregation methods directly, which turn common tasks such as finding <code>min</code>, <code>max</code>, <code>average</code> and <code>sum</code> of any entity attribute as simple as it can be.</p>

<p>There are also more advanced aggregations, such as <code>groupBy</code> and <code>top</code>:</p>

<markup
lang="java"

>Map&lt;Gender, Set&lt;Person&gt;&gt; peopleByGender = people.groupBy(Person::getGender);

Map&lt;Long, Double&gt; avgSalaryByDept =
    employees.groupBy(Employee::getDepartmentId, averagingDouble(Employee::getSalary));

List&lt;Double&gt; top5salaries = employees.top(Employee::getSalary, 5);</markup>

<p>as well as the simpler ones, such as <code>count</code> and <code>distinct</code>.</p>

<p>Finally, in many cases you may care not only about <code>min</code>, <code>max</code> or <code>top</code> values of a certain attribute, but also about which entities those values belong to. For those situations, you can use <code>minBy</code>, <code>maxBy</code> and <code>topBy</code> methods, which returns the entities containing minimum, maximum and top values of an attribute, respectively:</p>

<markup
lang="java"

>Optional&lt;Person&gt; oldestPerson   = people.maxBy(Person::getAge);
Optional&lt;Person&gt; youngestPerson = people.minBy(Person::getAge);

List&lt;Employee&gt; highestPaidEmployees = employees.topBy(Employee::getSalary, 5);</markup>


<h4 id="_declarative_acceleration_and_index_creation">Declarative Acceleration and Index Creation</h4>
<div class="section">
<p>I mentioned earlier that Coherence can use indexes to optimize queries and aggregations. The indexes allow you to avoid deserializing entities stored across the cluster, which is a potentially expensive operation when you have large data set, with complex entity classes. The indexes themselves can also be sorted, which is helpful when executing range-based queries, such as <code>less</code>, <code>greater</code> or <code>between</code>.</p>

<p>The standard way to create indexes is by calling <code>NamedMap.addIndex</code> method, which is certainly still an option. However, Repository API introduces a simpler, declarative way of index creation.</p>

<p>To define an index, simply annotate the accessor for the entity attribute(s) that you&#8217;d like to create an index for with <code>@Indexed</code> annotation:</p>

<markup
lang="java"

>public class Person
    {
    @Indexed                                                  <span class="conum" data-value="1" />
    public String getName()
        {
        return name;
        }

    @Indexed(ordered = true)                                  <span class="conum" data-value="2" />
    public int getAge()
        {
        return age;
        }
    }</markup>

<ul class="colist">
<li data-value="1">defines an unordered index on <code>Person::getName</code>, which is suitable for filters such as <code>equal</code>, <code>like</code>, and <code>regex</code></li>
<li data-value="2">defines an ordered index on <code>Person::getAge</code>, which is better suited for filters such as <code>less</code>, <code>greater</code> and <code>between</code></li>
</ul>
<p>When the repository is created, it will introspect the entity class for <code>@Indexed</code> annotation and automatically create an index for each attribute that has one. The created index will then be used whenever that attribute is referenced within the query expression.</p>

<p>In some cases you may want to keep deserialized entity instances around instead of discarding them. This can be useful when you are making frequent queries, aggregations, and using Stream API, or even in-place updates or projection, as the cost of maintaining individual indexes on all the attributes you need may end up being greater than to simply keep deserialized entity instances around.</p>

<p>For situations like that Coherence provides a special index type you can use, <code>DeserializationAccelerator</code>, but if you are using Repository API you once again have an easier way of configuring it&#8201;&#8212;&#8201;simply annotate either the entity class, or the repository class itself with the <code>@Accelerated</code> annotation:</p>

<markup
lang="java"

>@Accelerated
public class Person
    {
    }</markup>

<p>Obviously, you will require additional storage capacity in your cluster in order to be able to store both the serialized and deserialized copy of all the entities, but in some situations the performance benefits can significantly outweigh the cost. In other words, acceleration is a classic example of a time vs. space tradeoff, and it is entirely up to you to decide when it makes sense to use it.</p>

</div>
</div>

<h3 id="_event_listeners">Event Listeners</h3>
<div class="section">
<p>Coherence not only allows you to store, modify, query and aggregate your data entities efficiently, but you can also register to receive event notifications whenever any entity in the repository changes.</p>

<p>To do that, you can create and register a listener that will be notified whenever an entity is inserted, updated or removed:</p>

<markup
lang="java"

>    public static class PeopleListener
            implements PeopleRepository.Listener&lt;Person&gt;
        {
        public void onInserted(Person personNew)
            {
            // handle INSERT event
            }

        public void onUpdated(Person personOld, Person personNew)
            {
            // handle UPDATE event
            }

        public void onRemoved(Person personOld)
            {
            // handle REMOVE event
            }
        }</markup>

<markup
lang="java"

>people.addListener(new PeopleListener());                                        <span class="conum" data-value="1" />
people.addListener("111-22-3333", new PeopleListener());                         <span class="conum" data-value="2" />
people.addListener(Filters.greater(Person::getAge, 17), new PeopleListener());   <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">registers a listener that will be notified whenever any entity in the repository is inserted, updated or removed</li>
<li data-value="2">registers a listener that will be notified when an entity with the specified identifier is inserted, updated or removed</li>
<li data-value="3">registers a listener that will be notified when any <code>Person</code> older than 17 is inserted, updated or removed</li>
</ul>
<p>As you can see from the example above, there are several ways to register only for the events you are interested in, in order to reduce the number of events received, and the amount of data sent over the network.</p>

<p>Note that all of the listener methods above have a default no-op implementation, so you only need to implement the ones you actually want to handle.</p>

<p>However, having to implement a separate class each time you want to register a listener is a bit cumbersome, so Repository API also provides a default listener implementation, and a fluent builder for it that make the task a bit easier:</p>

<markup
lang="java"

>people.addListener(
        people.listener()
              .onInsert(personNew -&gt; { /* handle INSERT event */ })
              .onUpdate((personOld, personNew) -&gt; { /* handle UPDATE event with old value */ })
              .onUpdate(personNew -&gt; { /* handle UPDATE event without old value */ })
              .onRemove(personOld -&gt; { /* handle REMOVE event */ })
              .build()
);</markup>

<p>Note that when using Listener Builder API you have the option of omitting the old entity value from the <code>onUpdate</code> event handler arguments list. You can also specify multiple handlers for the same event type, in which case they will be composed and invoked in the specified order.</p>

<p>There is also an option of providing a single event handler that will receive all the events, regardless of the event type:</p>

<markup
lang="java"

>people.addListener(
        people.listener()
              .onEvent(person -&gt; { /* handle all events */ })
              .build()
);</markup>

<p>Just like when implementing listener class explicitly, you can still pass entity identifier or a <code>Filter</code> as the first argument to <code>addListener</code> method in order to limit the scope of the events received.</p>

</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>The Coherence Repository API was introduced to make the implementation of data access layer within the applications easier, regardless of which framework you use to implement applications that use Coherence as a data store. It works equally well for plain Java applications and applications that use CDI, where you can simply create your own repository implementations, as described at the beginning of this document.</p>

<p>It is also the foundation for our <a id="" title="" target="_blank" href="https://micronaut-projects.github.io/micronaut-coherence/latest/guide/#repository">Micronaut Data</a> and Spring Data repository implementations, so all the functionality described here is available when using those frameworks as well. The only difference is how you define your own repositories, which is framework-specific and documented separately.</p>

<p>We hope you&#8217;ll find this new feature useful, and that it will make implementation of your Coherence-backed data access layers even easier.</p>

</div>
</div>
</doc-view>
