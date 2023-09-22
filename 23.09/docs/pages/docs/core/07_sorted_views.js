<doc-view>

<h2 id="_sorted_views">Sorted Views</h2>
<div class="section">
<p>Sorted Views allow you to create a client-side views of data managed in Coherence that are sorted based either on
the natural sort order of the entry values, or on the provided <code>Comparator</code>.</p>


<h3 id="_creating_a_sorted_view">Creating a Sorted View</h3>
<div class="section">
<p>Sorted Views can be created programmatically, using the existing <code>ViewBuilder</code> API:</p>

<markup
lang="java"

>NamedMap&lt;String, String&gt; states       = session.getMap("states");           <span class="conum" data-value="1" />
NamedMap&lt;String, String&gt; sortedStates = states.view().sorted().build();     <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">Obtain a reference to a distributed cache that stores master copy of state names, keyed by two-letter state code</li>
<li data-value="2">Create a client-side view of states that will be sorted by the natural order of map values, in this case state name</li>
</ul>
<p>Just like with other views, the contents of the view will be kept in sync automatically by Coherence, so any changes made to the master list of states will be automatically reflected in each client-side view.</p>

<p>You can also create a view for more complex data types by passing a custom <code>Comparator</code> to the <code>sorted</code> method:</p>

<markup
lang="java"

>NamedMap&lt;Long, Person&gt; people       = session.getMap("people");
NamedMap&lt;Long, Person&gt; sortedPeople = states.view()
                                            .sorted(Comparator.comparing(Person::getAge).reversed())
                                            .build();</markup>

<p>The above will give you a view of all people, sorted by age from the oldest to the youngest person.</p>

<p>Of course, you can also perform all other operations that the <code>ViewBuilder</code> API supports, such as filtering entries in a view before they are sorted. For example, to create a view of all women sorted by age from youngest to oldest, you would define a view like this:</p>

<markup
lang="java"

>NamedMap&lt;Long, Person&gt; people       = session.getMap("people");
NamedMap&lt;Long, Person&gt; sortedPeople = states.view()
                                            .filter(Filters.equal(Person::getGender, Gender.FEMALE))
                                            .sorted(Comparator.comparing(Person::getAge))
                                            .build();</markup>

<p>One thing to keep in mind is that the sorting is always performed on the client after the data is retrieved from the server. In most cases that doesn&#8217;t matter, and will happen regardless of the order that you specify the operations in.</p>

<p>For example, the above example would work exactly the same if you reversed the order of <code>filter</code> and <code>sorted</code> operations and created a view like this:</p>

<markup
lang="java"

>NamedMap&lt;Long, Person&gt; people       = session.getMap("people");
NamedMap&lt;Long, Person&gt; sortedPeople = states.view()
                                            .sorted(Comparator.comparing(Person::getAge))
                                            .filter(Filters.equal(Person::getGender, Gender.FEMALE))
                                            .build();</markup>

<p>The only exception is <code>map</code> operation, as it changes the type of the values that are stored on the client by transforming them on the server, to reduce the amount of data that is transferred across the network. Because we can only sort what we have, the sort operation always has to be specified <strong>after</strong> the <code>map</code> operation, in order to use the correct value type:</p>

<markup
lang="java"

>NamedMap&lt;Long, Person&gt; people      = session.getMap("people");
NamedMap&lt;Long, Name&gt;   sortedNames = states.view()
                                            .filter(Filters.equal(Person::getGender, Gender.MALE))
                                            .map(Person::getName)
                                            .sorted(Comparator.comparing(Name::getLast)
                                                              .thenComparing(Name::getFirst))
                                            .build();</markup>

<p>The above will extract the <code>Names</code> of all men on the server, and sort them first by last and then by first name on the client.</p>

<p>In general, a good rule to follow is to specify <code>filter</code>, <code>map</code> and <code>sorted</code> operations in that order, as that&#8217;s the order they are actually executed in.</p>

</div>
</div>
</doc-view>
