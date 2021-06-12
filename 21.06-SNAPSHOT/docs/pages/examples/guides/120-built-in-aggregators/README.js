<doc-view>

<h2 id="_built_in_aggregators">Built-In Aggregators</h2>
<div class="section">
<p>This guide walks you through how to use built-in aggregators such as including count, sum,
min, average and top which allow you to process data stored in Coherence in parallel.</p>

<p>Coherence supports entry aggregators that perform operations against all, or a subset
of entries to obtain a single result. This aggregation is carried out in parallel across the cluster
and is a map-reduce type of operation which can be performed efficiently across large amounts of data.</p>

<p>See the <a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/processing-data-cache.html#GUID-DFB7AB0C-1CE6-4259-8854-9DA1F40B6F15">Coherence Documentation</a>
for detailed information on Aggregations.</p>


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
<p>In this example you will utilize the built-in aggregators such as <code>count</code>, <code>sum</code>,
<code>min</code>, <code>average</code> and <code>top</code> on orders and customers maps.</p>

<p>You will also use the <code>Aggregators</code> class and its helpers to simplify aggregator usage.</p>


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
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 1.8</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://maven.apache.org/download.cgi">Maven 3.5+</a> or <a id="" title="" target="_blank" href="http://www.gradle.org/downloads">Gradle 4+</a>
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
<p>This example can be run directly in your IDE, but to best demonstrate the functionality
you should run 1 or more cache servers and then run the example class.</p>

<ol style="margin-left: 15px;">
<li>
Running Cache Servers
<markup
lang="bash"

>./mvnw exec:exec -P server</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runServer -x test</markup>

</li>
<li>
Running each example
<p>Each example can be run direct from the IDE, or can be run via executing the tests.</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

</li>
</ol>
</div>
</div>

<h3 id="data-model">Example Data Model</h3>
<div class="section">
<p>The data model consists of the following classes in two maps, <code>customers</code> and <code>orders</code></p>

<ul class="ulist">
<li>
<p><code>Customer</code> - Represents a customer</p>

</li>
<li>
<p><code>Order</code> - Represents and order for a customer and contains order lines</p>

</li>
<li>
<p><code>OrderLine</code> - Represents an individual order line for an order</p>

</li>
<li>
<p><code>Address</code> - Represents an address for a customer</p>

</li>
</ul>
</div>

<h3 id="example-code-1">Review the Example Code</h3>
<div class="section">
<p>In this example we will show a number of the built-in aggregation functions in action. The full list is:</p>

<ul class="ulist">
<li>
<p>count</p>

</li>
<li>
<p>distinct</p>

</li>
<li>
<p>average</p>

</li>
<li>
<p>max</p>

</li>
<li>
<p>min</p>

</li>
<li>
<p>top</p>

</li>
<li>
<p>sum</p>

</li>
</ul>
<p>All the above aggregators can be implemented using static helpers in the <code>Aggregators</code> class, for example
<code>Aggregators.count()</code>. The helpers create the right aggregator type based on the type of method reference/extractor
that is passed as an argument. They all return the <code>EntryAggregator</code> implementations
 which allows them to be passed as argument to the aggregate methods below.</p>

<ul class="ulist">
<li>
<p><code>public default &lt;R&gt; R aggregate(EntryAggregator&lt;? super K, ? super V, R&gt; aggregator)</code> - Aggregate across all entries in a cache</p>

</li>
<li>
<p><code>public &lt;R&gt; R aggregate(Collection&lt;? extends K&gt; collKeys, EntryAggregator&lt;? super K, ? super V, R&gt; aggregator);</code> - Aggregate across a set of entries defined by the keys</p>

</li>
<li>
<p><code>public &lt;R&gt; R aggregate(Filter filter, EntryAggregator&lt;? super K, ? super V, R&gt; aggregator);</code> - Aggregate across a set of entries defines by the filter</p>

</li>
</ul>
<p>The <code>SimpleAggregationExample</code> runs various aggregations using a number of the above functions.</p>

<ol style="margin-left: 15px;">
<li>
Example Details
<p>The <code>runExample()</code> method contains the code that exercises the above aggregators. Refer to the inline
code comments for explanations of what each aggregator is doing.</p>

<markup
lang="java"

>/**
 * Run the example.
 */
public void runExample() {
    NamedMap&lt;Integer, Customer&gt; customers = getCustomers();
    NamedMap&lt;Integer, Order&gt; orders = getOrders();

    // count the customers using the Aggregators helper
    int customerCount = customers.aggregate(Aggregators.count());
    Logger.info("Customer Count = " + customerCount);

    // count the orders
    int orderCount = orders.aggregate(Aggregators.count());
    Logger.info("Order Count = " + orderCount);

    // get the total value of all orders - requires index on Order::getOrderTotal to be efficient
    Double totalOrders = orders.aggregate(Aggregators.sum(Order::getOrderTotal));
    Logger.info("Total Order Value " + formatMoney(totalOrders));

    // get the average order value across all orders - requires index to be efficient
    Double averageOrderValue = orders.aggregate(Aggregators.sum(Order::getOrderTotal));
    Logger.info("Average Order Value " + formatMoney(averageOrderValue));

    // get the minimum order value where then is only 1 order line - requires index on Order::getOrderLineCount to be efficient
    Double minOrderValue1Line = orders.aggregate(Filters.equal(Order::getOrderLineCount, 1),
            Aggregators.min(Order::getOrderTotal));
    Logger.info("Min Order Value for orders with 1 line " + formatMoney(minOrderValue1Line));

    // get the outstanding balances by state - requires index on the full ValueExtractor to be efficient
    ValueExtractor&lt;Customer, String&gt; officeState = ValueExtractor.of(Customer::getOfficeAddress).andThen(Address::getState);
    Map&lt;String, BigDecimal&gt; mapOutstandingByState = customers.aggregate(
            GroupAggregator.createInstance(officeState, Aggregators.sum(Customer::getOutstandingBalance)));
    mapOutstandingByState.forEach((k, v) -&gt; Logger.info("State: " + k + ", outstanding total is " + formatMoney(v)));

    // get the top 5 order totals by value
    Logger.info("Top 5 orders by value");
    Object[] topOrderValues = orders.aggregate(Aggregators.topN(Order::getOrderTotal, 5));
    for (Object value : topOrderValues) {
        Logger.info(formatMoney((Double) value));
    }
}</markup>

</li>
</ol>
</div>

<h3 id="run-example-1">Run the Example</h3>
<div class="section">
<p>Carry out the following to run this example:</p>

<ol style="margin-left: 15px;">
<li>
Start 2 cache servers using the method described above:
<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw exec:exec -P server</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runServer -x test</markup>

</li>
<li>
Run the following to load the data and run the example.
<p>E.g. for Maven use:</p>

<markup
lang="bash"

>./mvnw clean verify</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean test</markup>

<p>This will generate output similar to the following: (timestamps have been removed from output)</p>

<markup
lang="bash"

>&lt;Info&gt; (thread=main, member=1): Creating 10000 customers
&lt;Info&gt; (thread=main, member=1): Creating orders for customers
&lt;Info&gt; (thread=main, member=1): Orders created
&lt;Info&gt; (thread=main, member=1): Customer Count = 10000
&lt;Info&gt; (thread=main, member=1): Order Count = 29848
&lt;Info&gt; (thread=main, member=1): Total Order Value $89,689,872.00
&lt;Info&gt; (thread=main, member=1): Average Order Value $3,004.89
&lt;Info&gt; (thread=main, member=1): Min Order Value for orders with 1 line $500.08
&lt;Info&gt; (thread=main, member=1): State: QLD, outstanding total is $567,600.00
&lt;Info&gt; (thread=main, member=1): State: WA, outstanding total is $585,800.00
&lt;Info&gt; (thread=main, member=1): State: SA, outstanding total is $561,900.00
&lt;Info&gt; (thread=main, member=1): State: VIC, outstanding total is $556,500.00
&lt;Info&gt; (thread=main, member=1): State: NT, outstanding total is $528,700.00
&lt;Info&gt; (thread=main, member=1): State: ACT, outstanding total is $566,800.00
&lt;Info&gt; (thread=main, member=1): State: TAS, outstanding total is $563,900.00
&lt;Info&gt; (thread=main, member=1): State: NSW, outstanding total is $530,900.00
&lt;Info&gt; (thread=main, member=1): Top 5 orders by value
&lt;Info&gt; (thread=main, member=1): $8,304.27
&lt;Info&gt; (thread=main, member=1): $8,273.82
&lt;Info&gt; (thread=main, member=1): $8,229.51
&lt;Info&gt; (thread=main, member=1): $8,197.35
&lt;Info&gt; (thread=main, member=1): $8,194.63</markup>

</li>
</ol>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>You have seen how to use built-in aggregators which include <code>count</code>, <code>sum</code>,
<code>min</code>, <code>average</code> and <code>top</code> on orders and customers maps.</p>

<p>You also used the <code>Aggregators</code> class and its helpers to simplify aggregator usage.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/processing-data-cache.html#GUID-C9DF96E0-FAF2-4CD9-958E-4DC5CF06B18A">Performing Data Grid Operations</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
