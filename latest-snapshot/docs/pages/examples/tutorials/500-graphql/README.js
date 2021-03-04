<doc-view>

<h2 id="_graphql">GraphQL</h2>
<div class="section">
<p>This tutorial walks through the steps to enable access to Coherence data from GraphQL using Helidon’s
<a id="" title="" target="_blank" href="https://helidon.io/docs/v2/#/mp/graphql/01_mp_graphql">MicroProfile (MP) GraphQL support</a>
and <router-link to="/coherence-cdi-server/README">Coherence CDI</router-link>.</p>


<h3 id="_table_of_contents">Table of Contents</h3>
<div class="section">
<ul class="ulist">
<li>
<p><router-link to="#what-you-will-build" @click.native="this.scrollFix('#what-you-will-build')">What You Will Build</router-link></p>

</li>
<li>
<p><router-link to="#what-you-need" @click.native="this.scrollFix('#what-you-need')">What You Need</router-link></p>

</li>
<li>
<p><router-link to="#getting-started" @click.native="this.scrollFix('#getting-started')">Getting Started</router-link></p>

</li>
<li>
<p><router-link to="#follow-the-tutorial" @click.native="this.scrollFix('#follow-the-tutorial')">Follow the Tutorial</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#review-the-initial-project" @click.native="this.scrollFix('#review-the-initial-project')">Review the Initial Project</router-link></p>

</li>
<li>
<p><router-link to="#configure-mp-graphql" @click.native="this.scrollFix('#configure-mp-graphql')">Configure MicroProfile GraphQL</router-link></p>

</li>
<li>
<p><router-link to="#create-queries" @click.native="this.scrollFix('#create-queries')">Create Queries to Show Customer and Orders</router-link></p>

</li>
<li>
<p><router-link to="#inject-related-objects" @click.native="this.scrollFix('#inject-related-objects')">Inject Related Objects</router-link></p>

</li>
<li>
<p><router-link to="#add-mutations" @click.native="this.scrollFix('#add-mutations')">Add Mutations</router-link></p>

</li>
<li>
<p><router-link to="#add-a-dynamic-where-clause" @click.native="this.scrollFix('#add-a-dynamic-where-clause')">Add a Dynamic Where Clause</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#run-the-completed-tutorial" @click.native="this.scrollFix('#run-the-completed-tutorial')">Run the Completed Tutorial</router-link></p>

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
<p>You will build on an existing <code>mock</code> sample Coherence data model and create
an application that will expose a GraphQL endpoint to perform various queries and mutations
against the data model.</p>

<div class="admonition note">
<p class="admonition-inline">If you wish to read more about GraphQL or Helidon&#8217;s support in GraphQL, please see this
<a id="" title="" target="_blank" href="https://medium.com/helidon/microprofile-graphql-support-now-available-in-helidon-mp-dbc7bc0b4af">Medium post</a>.</p>
</div>
</div>

<h3 id="what-you-need">What You Need</h3>
<div class="section">
<ul class="ulist">
<li>
<p>About 30-45 minutes</p>

</li>
<li>
<p>A favorite text editor or IDE</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 11</a> or later</p>

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
<p><a id="" title="" target="_blank" href="/guides/gs/intellij-idea/">IntelliJ IDEA</a></p>

</li>
</ul>
</li>
</ul>

<h4 id="_building_the_example_code">Building the Example Code</h4>
<div class="section">
<p>Whenever you are asked to build the code, please refer to the instructions below.</p>

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

<v-divider class="my-5"/>
</div>
</div>

<h3 id="getting-started">Getting Started</h3>
<div class="section">
<p>This tutorial contains both the completed codebase as well as the initial state
from which you build the complete the tutorial on.</p>

<div class="admonition note">
<p class="admonition-inline">If you would like to run the completed example, please follow
the instructions <router-link to="#run-the-completed-tutorial" @click.native="this.scrollFix('#run-the-completed-tutorial')">here</router-link> otherwise continue below for the tutorial.</p>
</div>
</div>

<h3 id="follow-the-tutorial">Follow the Tutorial</h3>
<div class="section">
<p>Ensure you have the project in <code>tutorials/500-graphql/initial</code> imported into your IDE.</p>


<h4 id="review-the-initial-project">Review the Initial Project</h4>
<div class="section">
<p><strong>Maven Configuration</strong></p>

<p>The initial project is a Coherence-CDI and Helidon project and imports the <code>coherence-bom</code>, <code>helidon-bom</code> and <code>coherence-dependencies</code>
POMs as shown below:</p>

<markup
lang="xml"

>&lt;dependencyManagement&gt;
  &lt;dependencies&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
      &lt;artifactId&gt;coherence-bom&lt;/artifactId&gt;
      &lt;version&gt;${coherence.version}&lt;/version&gt;
      &lt;type&gt;pom&lt;/type&gt;
      &lt;scope&gt;import&lt;/scope&gt;
    &lt;/dependency&gt;
    &lt;dependency&gt;
      &lt;groupId&gt;io.helidon&lt;/groupId&gt;
      &lt;artifactId&gt;helidon-bom&lt;/artifactId&gt;
      &lt;version&gt;${helidon.version}&lt;/version&gt;
      &lt;type&gt;pom&lt;/type&gt;
      &lt;scope&gt;import&lt;/scope&gt;
    &lt;/dependency&gt;
  &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;</markup>

<p><code>helidon-microprofile-cdi</code> and <code>coherence-cdi-server</code> are also included:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;io.helidon.microprofile.cdi&lt;/groupId&gt;
  &lt;artifactId&gt;helidon-microprofile-cdi&lt;/artifactId&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
  &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
  &lt;artifactId&gt;coherence-cdi-server&lt;/artifactId&gt;
&lt;/dependency&gt;</markup>

<p>The POM also includes the <code>jandex-maven-plugin</code> to build an index, which is required by Helidon&#8217;s implementation.</p>

<markup
lang="xml"

>&lt;plugin&gt;
  &lt;groupId&gt;org.jboss.jandex&lt;/groupId&gt;
  &lt;artifactId&gt;jandex-maven-plugin&lt;/artifactId&gt;
  &lt;version&gt;${maven.jandex.plugin.version}&lt;/version&gt;
  &lt;executions&gt;
    &lt;execution&gt;
      &lt;id&gt;make-index&lt;/id&gt;
      &lt;goals&gt;
        &lt;goal&gt;jandex&lt;/goal&gt;
      &lt;/goals&gt;
    &lt;/execution&gt;
  &lt;/executions&gt;
&lt;/plugin&gt;</markup>

<p><strong>Data Model</strong></p>

<p>The data model consists of the following classes:</p>

<ul class="ulist">
<li>
<p><code>Customer</code> - contains customer details and keyed by customer id</p>

</li>
<li>
<p><code>Order</code> - contains orders for a customer and is keyed by order number</p>

</li>
<li>
<p><code>Order Lines</code> - contains order line information which is included directly within <code>Order</code> object</p>

</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">The Objects to be used must conform to the naming conventions for fields and their getters and setters according to the Java Bean Spec to ensure
full functionality works correctly in Helidon&#8217;s MicroProfile GraphQL implementation.</p>
</div>
<p><strong>Coherence Bootstrap</strong></p>

<p>The <code>Bootstrap</code> class is used to initialize the Coherence and includes the following <code>NamedMaps</code>:</p>

<markup
lang="java"

>/**
 * The {@link NamedMap} for customers.
 */
@Inject
private NamedMap&lt;Integer, Customer&gt; customers;

/**
 * The {@link NamedMap} for orders.
 */
@Inject
private NamedMap&lt;Integer, Order&gt; orders;</markup>

<p>The class is <code>ApplicationScoped</code> and <code>init</code> method is called on application startup.</p>

<markup
lang="java"

>/**
 * Initialize the Coherence {@link NamedMap}s with data.
 *
 * @param init init
 */
private void init(@Observes @Initialized(ApplicationScoped.class) Object init) {</markup>

<p><strong>Build and Run the Initial State</strong></p>

<p>Build and run using either of the following:</p>

<div class="block-title"><span>Commands to build and run for the rest of the tutorial</span></div>
<div class="table__overflow elevation-1  ">
<table class="datatable table">
<colgroup>
<col style="width: 33.333%;">
<col style="width: 33.333%;">
<col style="width: 33.333%;">
</colgroup>
<thead>
<tr>
<th>Build Tool</th>
<th>Build Command</th>
<th>Run Comments</th>
</tr>
</thead>
<tbody>
<tr>
<td class="">Maven</td>
<td class=""><code>./mvnw clean package</code></td>
<td class=""><code>./mvnw exec:exec</code></td>
</tr>
<tr>
<td class="">Gradle</td>
<td class=""><code>./gradlew build</code></td>
<td class=""><code>./gradlew runApp</code></td>
</tr>
</tbody>
</table>
</div>
<p>Running the application will output, amongst other things, messages indicating
Coherence has started and the following to show the data was loaded:</p>

<markup
lang="text"

>===CUSTOMERS===
Customer{customerId=1, name='Billy Joel', email='billy@billyjoel.com', address='Address 1', balance=0.0}
Customer{customerId=4, name='Tom Jones', email='tom@jones.com', address='Address 4', balance=0.0}
Customer{customerId=2, name='James Brown', email='soul@jamesbrown.net', address='Address 2', balance=100.0}
Customer{customerId=3, name='John Williams', email='john@statware.com', address='Address 3', balance=0.0}
===ORDERS===
....</markup>

</div>

<h4 id="configure-mp-graphql">Configure MicroProfile GraphQL</h4>
<div class="section">
<p><strong>Add Helidon MP GraphQL</strong></p>

<p>Add the following dependency to the project POM:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;io.helidon.microprofile.graphql&lt;/groupId&gt;
  &lt;artifactId&gt;helidon-microprofile-graphql-server&lt;/artifactId&gt;
&lt;/dependency&gt;</markup>

<p>or if you are using Gradle, then add the following to <code>build.gradle</code>:</p>

<markup
lang="properties"

>implementation ("io.helidon.microprofile.graphql:helidon-microprofile-graphql-server")</markup>

<p><strong>Add MicroProfile Properties</strong></p>

<p>Add the following to <code>src/main/resources/META-INF/microprofile-config.properties</code>:</p>

<markup
lang="java"

>server.static.classpath.context=/ui
server.static.classpath.location=/web
graphql.cors=Access-Control-Allow-Origin

mp.graphql.exceptionsWhiteList=java.lang.IllegalArgumentException</markup>

<div class="admonition note">
<p class="admonition-inline">The <code>server.static.classpath.context=/ui</code> defines the URL
to serve the contents found in resources location <code>server.static.classpath.location=/web</code>. E.g. <code>src/main/resources/web</code>.
The setting <code>graphql.cors=Access-Control-Allow-Origin</code> allows the GraphiQL UI to use CORS.
We will explain the <code>mp.graphql.exceptionsWhiteList=java.lang.IllegalArgumentException</code> later.</p>
</div>
<p>As the <a id="" title="" target="_blank" href="https://github.com/graphql/graphiql">GraphiQL UI</a> client used in this example is not included
in this repository, you must copy the <code>index.html</code> file contents
from <a id="" title="" target="_blank" href="https://github.com/graphql/graphiql/blob/main/packages/graphiql/README.md#cdn-bundle">https://github.com/graphql/graphiql/blob/main/packages/graphiql/README.md#cdn-bundle</a>
into the file in <code>src/main/resources/web/index.html</code> before you continue.</p>

</div>
</div>

<h3 id="create-queries">Create Queries to Show Customer and Orders</h3>
<div class="section">
<p><strong>Create the CustomerApi Class</strong></p>

<p>Firstly we need to create a class to expose our GraphQL endpoint.</p>

<ol style="margin-left: 15px;">
<li>
Create a new Class called <code>CustomerApi</code> in the package <code>com.oracle.coherence.tutorials.graphql.api</code>.

</li>
<li>
Add the <code>GraphQLApi</code> annotation to mark this class as a GraphQL Endpoint and make it application scoped.
<markup
lang="java"

>@ApplicationScoped
@GraphQLApi
public class CustomerApi {</markup>

</li>
<li>
Inject the Coherence `NamedMap`s for customers and orders
<markup
lang="java"

>/**
 * The {@link NamedMap} for customers.
 */
@Inject
private NamedMap&lt;Integer, Customer&gt; customers;

/**
 * The {@link NamedMap} for orders.
 */
@Inject
private NamedMap&lt;Integer, Order&gt; orders;</markup>

</li>
</ol>
<p><strong>Add a Query to return all customers</strong></p>

<ol style="margin-left: 15px;">
<li>
Add the following code to <code>CustomerApi</code> to create a query to return all customers:
<markup
lang="java"

>/**
 * Returns all of the {@link Customer}s.
 *
 * @return all of the {@link Customer}s.
 */
@Query
@Description("Displays customers")
public Collection&lt;Customer&gt; getCustomers() {
    return customers.values();
}</markup>

<div class="admonition note">
<p class="admonition-inline">Ensure you import the <code>Query</code> and <code>Description</code> annotations from <code>org.eclipse.microprofile.graphql</code></p>
</div>
</li>
<li>
Build and run the project.

</li>
<li>
Issue the following to display the automatically generated schema:
<markup
lang="bash"

>curl http://localhost:7001/graphql/schema.graphql

type Customer {
  address: String
  balance: String!
  customerId: Int!
  email: String
  name: String
  orders: [Order]
}
type Query {
  "Displays customers"
  customers: [Customer]
}</markup>

</li>
<li>
Open the URL <a id="" title="" target="_blank" href="http://localhost:7001/ui">http://localhost:7001/ui</a>. You should see the GraphiQL UI.
<p>Notice the <code>Documentation Explorer</code> on the right, which will allow you to explore the generated schema.</p>

</li>
<li>
Enter the following in the left-hand pane and click the <code>Play</code> button.
<markup
lang="graphql"

>query customers {
  customers {
    customerId
    name
    address
    email
    balance
  }
}</markup>

</li>
<li>
This will result in the following JSON output:
<markup
lang="json"

>{
  "data": {
    "customers": [
      {
        "customerId": 1,
        "name": "Billy Joel",
        "address": "Address 1",
        "email": "billy@billyjoel.com",
        "balance": 0
      },
      {
        "customerId": 4,
        "name": "Tom Jones",
        "address": "Address 4",
        "email": "tom@jones.com",
        "balance": 0
      },
      {
        "customerId": 2,
        "name": "James Brown",
        "address": "Address 2",
        "email": "soul@jamesbrown.net",
        "balance": 100
      },
      {
        "customerId": 3,
        "name": "John Williams",
        "address": "Address 3",
        "email": "john@statware.com",
        "balance": 0
      }
    ]
  }
}</markup>

</li>
</ol>
<p><strong>Add a Query to return all Orders</strong></p>

<ol style="margin-left: 15px;">
<li>
Add the following code to <code>CustomerApi</code> to create a query to return all orders:
<markup
lang="java"

>@Query("displayOrders")
public Collection&lt;Order&gt; getOrders() {
    return orders.values();
    }</markup>

<div class="admonition note">
<p class="admonition-inline">In this case we are overriding the default name for the query, which would be <code>orders</code>, with <code>displayOrders</code>.</p>
</div>
</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and enter the following in the left-hand pane and click the <code>Play</code> button and choose <code>orders</code>.
<markup
lang="graphql"

>query orders {
  displayOrders {
    orderId
    customerId
    orderDate
    orderTotal
    orderLines {
      lineNumber
      productDescription
      itemCount
      costPerItem
      orderLineTotal
    }
  }
}</markup>

</li>
<li>
This will result in the following JSON output. The output below has been shortened.
<p>Notice that because we included the <code>orderLines</code> field and it is an object, then we must specify the individual fields to return.</p>

<markup
lang="json"

>{
  "data": {
    "displayOrders": [
      {
        "orderId": 104,
        "customerId": 3,
        "orderDate": "2021-01-28",
        "orderTotal": 12163.024674447412,
        "orderLines": [
          {
            "lineNumber": 1,
            "productDescription": "Samsung TU8000 55 inch Crystal UHD 4K Smart TV [2020]",
            "itemCount": 1,
            "costPerItem": 1695.3084188228172,
            "orderLineTotal": 1695.3084188228172
          },
          {
            "lineNumber": 4,
            "productDescription": "Sony X7000G 49 inch 4k Ultra HD HDR Smart TV",
            "itemCount": 2,
            "costPerItem": 2003.1246529714456,
            "orderLineTotal": 4006.249305942891
          },
          {
            "lineNumber": 3,
            "productDescription": "TCL S615 40 inch Full HD Android TV",
            "itemCount": 2,
            "costPerItem": 1171.4274805289924,
            "orderLineTotal": 2342.854961057985
          },
          {
            "lineNumber": 2,
            "productDescription": "Samsung Q80T 85 inch QLED Ultra HD 4K Smart TV [2020]",
            "itemCount": 2,
            "costPerItem": 2059.305994311859,
            "orderLineTotal": 4118.611988623718
          }
        ]
      },
      {
        "orderId": 102,
        "customerId": 2,
      ...</markup>

</li>
</ol>
<p><strong>Format currency fields</strong></p>

<p>We can see from the above output that a number of the currency fields
are not formatted correctly. We will use the GraphQL annotation <code>NumberFormat</code> to format this as currency.</p>

<div class="admonition note">
<p class="admonition-inline">You may also use the <code>JsonbNumberFormat</code> annotation as well.</p>
</div>
<ol style="margin-left: 15px;">
<li>
Add the <code>NumberFormat</code> to <code>getBalance</code> on the <code>Customer</code> class.
<markup
lang="java"

>/**
 * Returns the customer's balance.
 *
 * @return the customer's balance
 */
@NumberFormat("$###,##0.00")
public double getBalance() {
    return balance;
}</markup>

<div class="admonition note">
<p class="admonition-inline">By adding the <code>NumberFormat</code> to the get method, the format will be applied to the output type only. If we
add the  <code>NumberFormat</code> to the set method it will be applied to the input type only. E.g. when Customer is used as a
parameter. If it is added to the attribute it will apply to both input and output types.</p>
</div>
</li>
<li>
Add the <code>NumberFormat</code> to <code>getOrderTotal</code> on the <code>Order</code> class.
<markup
lang="java"

>/**
 * Returns the order total.
 *
 * @return the order total
 */
@NumberFormat("$###,###,##0.00")
public double getOrderTotal() {
    return orderLines.stream().mapToDouble(OrderLine::getOrderLineTotal).sum();
}</markup>

</li>
<li>
Add the <code>NumberFormat</code> to <code>getCostPerItem</code> and <code>getOrderLineTotal</code> on the <code>OrderLine</code> class.
<markup
lang="java"

>/**
 * Return the cost per item.
 *
 * @return the cost per item
 */
@NumberFormat("$###,###,##0.00")
public double getCostPerItem() {
    return costPerItem;
}</markup>

<markup
lang="java"

>/**
 * Returns the order line total.
 *
 * @return he order line total
 */
@NumberFormat("$###,###,##0.00")
public double getOrderLineTotal() {
    return itemCount * costPerItem;
}</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and run the <code>customers</code> and <code>orders</code> queries and you will see the number values formatted as shown below:
<markup
lang="json"

>{
    "customerId": 2,
    "name": "James Brown",
    "address": "Address 2",
    "email": "soul@jamesbrown.net",
    "balance": "$100.00"
}</markup>

<markup
lang="json"

>...
    "orderTotal": "$13,029.54",
...
    "costPerItem": "$2,456.27",
    "orderLineTotal": "$2,456.27"</markup>

</li>
</ol>
</div>

<h3 id="inject-related-objects">Inject Related Objects</h3>
<div class="section">
<p>From the above output for orders, we can see we have <code>customerId</code> field only. It would be useful to
also be able to return any attributes for the customer customer. Conversely it would be useful to be able to show the
order details for a customer.</p>

<p>We can achieve this using Coherence by making the class implement <code>Injectable</code>. When the class is
deserialized on the client, any <code>@Inject</code> statements are processed and we will use this to inject the <code>NamedMap</code> for
customer and use to retrieve the customer details if required.</p>

<p><strong>Return the Customer for the Order</strong></p>

<ol style="margin-left: 15px;">
<li>
Make the <code>Order</code> class implement <code>com.oracle.coherence.inject.Injectable</code>.
<markup
lang="java"

>public class Order
        implements Serializable, Injectable {</markup>

</li>
<li>
Inject the customer <code>NamedMap</code>.
<markup
lang="java"

>/**
 * The {@link NamedMap} for customers.
 */
@Inject
private transient NamedMap&lt;Integer, Customer&gt; customers;</markup>

</li>
<li>
Finally add the <code>getCustomer</code> method.
<markup
lang="java"

>/**
 * Returns the {@link Customer} for this {@link Order}.
 *
 * @return the {@link Customer} for this {@link Order}
 */
public Customer getCustomer() {
    return customers.get(customerId);
}</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and run view the <code>Order</code> object in the <code>Documentation Explorer</code>.
You will see a customer field that returns a <code>Customer</code> object.
<p>Change the <code>orders</code> query to the following and execute. You will notice the customers name and email returned.</p>

<markup
lang="graphql"

>query orders {
  displayOrders {
    orderId
    customerId
    orderDate
    orderTotal
    customer {
      name
      email
    }
    orderLines {
      lineNumber
      productDescription
      itemCount
      costPerItem
      orderLineTotal
    }
  }
}</markup>

<markup
lang="json"

>  "data": {
    "displayOrders": [
      {
        "orderId": 104,
        "customerId": 3,
        "orderDate": "2021-01-28",
        "orderTotal": "$7,946.81",
        "customer": {
          "name": "John Williams",
          "email": "john@statware.com"
        },
...</markup>

</li>
</ol>
<p><strong>Return the Orders for a Customer</strong></p>

<ol style="margin-left: 15px;">
<li>
Make the <code>Customer</code> class implement <code>com.oracle.coherence.inject.Injectable</code>.
<markup
lang="java"

>public class Customer
        implements Serializable, Injectable {</markup>

</li>
<li>
Inject the orders <code>NamedMap</code>.
<markup
lang="java"

>/**
 * The {@link NamedMap} for orders.
 */
@Inject
private transient NamedMap&lt;Integer, Order&gt; orders;</markup>

</li>
<li>
Finally add the <code>getOrders</code> method to get the orders for the current customer
by specifying a Coherence filter.
<markup
lang="java"

>/**
 * Returns the {@link Order}s for a {@link Customer}.
 *
 * @return the {@link Order}s for a {@link Customer}
 */
public Collection&lt;Order&gt; getOrders() {
    return orders.values(Filters.equal(Order::getCustomerId, customerId));
}</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and run view the <code>Customer</code> object in the <code>Documentation Explorer</code>.
You will see an orders field that returns an array of <code>Customer</code> objects.
Change the <code>customers</code> query to add the orders for a customer and execute. You will notice
the orders for the customers returned.
<markup
lang="graphql"

>query customers {
  customers {
    customerId
    name
    address
    email
    balance
    orders {
      orderId
      orderDate
      orderTotal
    }
  }
}</markup>

<markup
lang="json"

>{
  "data": {
    "customers": [
      {
        "customerId": 1,
        "name": "Billy Joel",
        "address": "Address 1",
        "email": "billy@billyjoel.com",
        "balance": "$0.00",
        "orders": [
          {
            "orderId": 100,
            "orderDate": "2021-01-28",
            "orderTotal": "$1,572.23"
          },
          {
            "orderId": 101,
            "orderDate": "2021-01-28",
            "orderTotal": "$2,201.91"
          }
        ]
      },
...</markup>

</li>
</ol>
</div>

<h3 id="add-mutations">Add Mutations</h3>
<div class="section">
<p>In this section we will add mutations to create or update data.</p>

<p><strong>Create a Customer</strong></p>

<ol style="margin-left: 15px;">
<li>
Add the following to the <code>CustomerApi</code> class to create a customer:
<markup
lang="java"

>/**
 * Creates and saves a {@link Customer}.
 *
 * @param customer and saves a {@link Customer}
 *
 * @return the new {@link Customer}
 */
@Mutation
public Customer createCustomer(@Name("customer") Customer customer) {
    if (customers.containsKey(customer.getCustomerId())) {
        throw new IllegalArgumentException("Customer " + customer.getCustomerId() + " already exists");
    }

    customers.put(customer.getCustomerId(), customer);
    return customers.get(customer.getCustomerId());
}</markup>

<p>In the above code we throw an <code>IllegalArgumentException</code> if the customer already exists. By default in
the MicroProfile GraphQL specification, messages from unchecked exceptions are hidden from
the client and "Server Error" is returned. In this case we have overridden this behaviour
in the <code>META-INF/microprofile-config.properties</code> as shown below:</p>

<markup
lang="java"

>mp.graphql.exceptionsWhiteList=java.lang.IllegalArgumentException</markup>

<p>Checked exceptions, which we will show below will return the message back to the client by default
and the message can be hidden as well if required.</p>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and create a <code>fragment</code> to avoid having to repeat fields:
<markup
lang="graphql"

>fragment customer on Customer {
    customerId
    name
    address
    email
    balance
    orders {
      orderId
      orderTotal
    }
}</markup>

<p>You can also update your existing <code>customers</code> query to use this fragment.</p>

</li>
<li>
Execute the following mutation:
<markup
lang="graphql"

>mutation createNewCustomer {
  createCustomer(customer: { customerId: 12 name: "Tim" balance: 1000}) {
     ...customer
  }
}</markup>

<markup
lang="json"

>{
  "data": {
    "createCustomer": {
      "customerId": 12,
      "name": "Tim",
      "address": null,
      "email": null,
      "balance": "$1,000.00",
      "orders": []
    }
  }
}</markup>

</li>
</ol>
<p><strong>Making Attributes Mandatory</strong></p>

<ol style="margin-left: 15px;">
<li>
If you execute the following query, you will notice that a customer is created with a null name.
This is because in MP GraphQL any primitive is mandatory and all Objects are optional. Name is a String
and therefore is optional.
<markup
lang="graphql"

>mutation createNewCustomer {
  createCustomer(customer: { customerId: 11 balance: 1000}) {
     ...customer
  }
}</markup>

</li>
<li>
View the <code>Documentation Explorer</code> and note that the <code>createCustomer</code> mutation has the following
schema:
<markup
lang="graphql"

>createCustomer(customer: CustomerInput): Customer</markup>

<p><code>CustomerInput</code> has the following structure:</p>

<markup
lang="graphql"

>input CustomerInput {
  address: String
  balance: Float!
  customerId: Int!
  email: String
  name: String
  orders: [OrderInput]
}</markup>

</li>
<li>
Add the <code>NonNull</code> annotation to the name field in the <code>Customer</code> object:
<markup
lang="java"

>/**
 * Name.
 */
@NonNull
private String name;</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and try to execute the following mutation again. You will notice the UI will show an
error indicating that <code>name</code> is now mandatory.
<markup
lang="graphql"

>createCustomer(customer: CustomerInput): Customer</markup>

</li>
</ol>
<p><strong>Create an Order</strong></p>

<ol style="margin-left: 15px;">
<li>
Add the following to the <code>CustomerApi</code> class to create an order:
<markup
lang="java"

>/**
 * Creates and saves an {@link Order} for a given customer id.
 *
 * @param customerId customer id to create the {@link Order} for
 * @param orderId    order id
 *
 * @return the new {@link Order}
 *
 * @throws CustomerNotFoundException if the {@link Customer} was not found
 */
@Mutation
public Order createOrder(@Name("customerId") int customerId,
                         @Name("orderId") int orderId)
        throws CustomerNotFoundException {
    if (!customers.containsKey(customerId)) {
        throw new CustomerNotFoundException("Customer id " + customerId + " was not found");
    }

    if (orders.containsKey(orderId)) {
        throw new IllegalArgumentException("Order " + orderId + " already exists");
    }

    Order order = new Order(orderId, customerId);
    orders.put(orderId, order);
    return orders.get(orderId);
}</markup>

<div class="admonition note">
<p class="admonition-inline">The validation ensures that we have a valid customer and the order id does not already exist.</p>
</div>
</li>
<li>
Create a new checked exception called <code>CustomerNotFoundException</code> in the api package. By default in MP GraphQL the messages
from checked exceptions will be automatically returned to the client.
<markup
lang="java"

>public class CustomerNotFoundException
        extends Exception {

    /**
     * Constructs a new exception to indicate that a customer was not found.
     *
     * @param message the detail message.
     */
    public CustomerNotFoundException(String message) {
        super(message);
    }
}</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and add the following <code>fragment</code> to avoid having to repeat fields:
<markup
lang="graphql"

>fragment order on Order {
   orderId
    customerId
    customer {
      name
    }
    orderDate
    orderTotal
    orderLines {
      lineNumber
      productDescription
      itemCount
      costPerItem
      orderLineTotal
    }
}</markup>

<div class="admonition note">
<p class="admonition-inline">You can also update the <code>orders</code> query to use the new fragment:</p>
</div>
<markup
lang="graphql"

>query orders {
  displayOrders {
   ...order
  }
}</markup>

</li>
<li>
Try to create an order with a non-existent customer number 12.
<markup
lang="graphql"

>mutation createOrderForCustomer {
  createOrder(customerId: 12 orderId: 100) {
      ...order
  }
}</markup>

<p>This shows the following message from the <code>CustomerNotFoundException</code>:</p>

<markup
lang="json"

>{
  "data": {
    "createOrder": null
  },
  "errors": [
    {
      "path": [
        "createOrder"
      ],
      "locations": [
        {
          "column": 3,
          "line": 58
        }
      ],
      "message": "Customer id 12 was not found"
    }
  ]
}</markup>

</li>
<li>
Try to create an order with an already existing order id 100.
<markup
lang="graphql"

>mutation createOrderForCustomer {
  createOrder(customerId: 1 orderId: 100) {
      ...order
  }
}</markup>

<p>This shows the following message from the <code>IllegalArgumentException</code>:</p>

<markup
lang="json"

>{
  "data": {
    "createOrder": null
  },
  "errors": [
    {
      "path": [
        "createOrder"
      ],
      "locations": [
        {
          "column": 3,
          "line": 58
        }
      ],
      "message": "Order 100 already exists"
    }
  ]
}</markup>

</li>
<li>
Create a new order with valid values:
<markup
lang="graphql"

>mutation createOrderForCustomer {
  createOrder(customerId: 1 orderId: 200) {
      ...order
  }
}</markup>

<p>This shows the following message from the <code>IllegalArgumentException</code>:</p>

<markup
lang="json"

>{
  "data": {
    "createOrder": {
      "orderId": 200,
      "customerId": 1,
      "customer": {
        "name": "Billy Joel"
      },
      "orderDate": "2021-01-29",
      "orderTotal": "$0.00",
      "orderLines": []
    }
  }
}</markup>

</li>
</ol>
<p><strong>Add an OrderLine to an Order</strong></p>

<ol style="margin-left: 15px;">
<li>
Add the following to the <code>CustomerApi</code> class to add an OrderLine to an Order:
<markup
lang="java"

>/**
 * Adds an {@link OrderLine} to an existing {@link Order}.
 *
 * @param orderId   order id to add to
 * @param orderLine {@link OrderLine} to add
 *
 * @return the updates {@link Order}
 *
 * @throws OrderNotFoundException the the {@link Order} was not found
 */
@Mutation
public Order addOrderLineToOrder(@Name("orderId") int orderId,
                                 @Name("orderLine") OrderLine orderLine)
        throws OrderNotFoundException {
    if (!orders.containsKey(orderId)) {
        throw new OrderNotFoundException("Order number " + orderId + " was not found");
    }

    if (orderLine.getProductDescription() == null || orderLine.getProductDescription().equals("") ||
        orderLine.getItemCount() &lt;= 0 || orderLine.getCostPerItem() &lt;= 0) {
        throw new IllegalArgumentException("Supplied Order Line is invalid: " + orderLine);
    }

    return orders.compute(orderId, (k, v)-&gt;{
        v.addOrderLine(orderLine);
        return v;
    });

}</markup>

</li>
<li>
Create a new checked exception called <code>OrderNotFoundException</code> in the api package.
<markup
lang="java"

>public class OrderNotFoundException
        extends Exception {

    /**
     * Constructs a new exception to indicate that an order was not found.
     *
     * @param message the detail message.
     */
    public OrderNotFoundException(String message) {
        super(message);
    }
}</markup>

</li>
<li>
To make input easier, we can add <code>DefaultValue</code> annotations to the <code>setLineNumber</code> method and
<code>setItemCount</code> methods in the OrderLine` class.
<div class="admonition note">
<p class="admonition-inline">Ensure you import <code>DefaultValue</code> from the <code>org.eclipse.microprofile.graphql</code> package.</p>
</div>
<markup
lang="java"

>@DefaultValue("1")
public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
}</markup>

<markup
lang="java"

>@DefaultValue("1")
public void setItemCount(int itemCount) {
    this.itemCount = itemCount;
}</markup>

<div class="admonition note">
<p class="admonition-inline">By placing the <code>DefaultValue</code> on the setter methods only, it applies to input types only. If we wanted the
<code>DefaultValue</code> to apply to output type only we would apply to the getters. If we wish to appy to both input and
output we can place on the field.</p>
</div>
</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and run view the <code>OrderLineInput</code> object in the <code>Documentation Explorer</code>.
You will see the default values applied. They are also no longer mandatory as they have a default value.
<markup
lang="graphql"

>lineNumber: Int = 1
itemCount: Int = 1</markup>

</li>
<li>
Create a new order 200 for customer 1 and then add a new order line.
<markup
lang="graphql"

>mutation createOrderForCustomer {
  createOrder(customerId: 1 orderId: 200) {
      ...order
  }
}

mutation addOrderLineToOrder {
  addOrderLineToOrder(orderId: 200 orderLine: {productDescription: "iPhone 12" costPerItem: 1500 }) {
    ...order
  }
}</markup>

<p>This shows the following output for the new order.</p>

<markup
lang="json"

>{
  "data": {
    "createOrder": {
      "orderId": 200,
      "customerId": 1,
      "customer": {
        "name": "Billy Joel"
      },
      "orderDate": "2021-01-29",
      "orderTotal": "$0.00",
      "orderLines": []
    }
  }
}</markup>

<p>And the result of the new order line.</p>

<markup
lang="json"

>{
  "data": {
    "addOrderLineToOrder": {
      "orderId": 200,
      "customerId": 1,
      "customer": {
        "name": "Billy Joel"
      },
      "orderDate": "2021-01-29",
      "orderTotal": "$1,500.00",
      "orderLines": [
        {
          "lineNumber": 1,
          "productDescription": "iPhone 12",
          "itemCount": 1,
          "costPerItem": "$1,500.00",
          "orderLineTotal": "$1,500.00"
        }
      ]
    }
  }
}</markup>

</li>
<li>
Experiment with invalid order id and customer id as input.

</li>
</ol>
</div>

<h3 id="add-a-dynamic-where-clause">Add a Dynamic Where Clause</h3>
<div class="section">
<p>Finally we will enhance the orders query and add a dynamic where clause.</p>

<ol style="margin-left: 15px;">
<li>
Update the <code>getOrders</code> method in the <code>CustomerApi</code> to add the where clause and pass this to the <code>QuerHelper</code> to
generate the Coherence <code>Filter</code>. The code will ask return an error message if the where clause is invalid.
<markup
lang="java"

>/**
 * Returns {@link Order}s that match the where clause or all {@link Order}s
 * if the where clause is null.
 *
 * @param whereClause where clause to restrict selection of {@link Order}s
 *
 * @return {@link Order}s that match the where clause or all {@link Order}s
 * if the where clause is null
 */
@Query("displayOrders")
public Collection&lt;Order&gt; getOrders(@Name("whereClause") String whereClause) {
    try {
        Filter filter = whereClause == null
                        ? Filters.always()
                        : QueryHelper.createFilter(whereClause);
        return orders.values(filter);
    }
    catch (Exception e) {
        throw new IllegalArgumentException("Invalid where clause: [" + whereClause + "]");
    }
}</markup>

</li>
<li>
Stop the running project, rebuild and re-run.

</li>
<li>
Refresh GraphiQL and execute the following query to find all orders with a orderTotal greater than $4000.
<markup
lang="graphql"

>query ordersWithWhereClause {
  displayOrders(whereClause: "orderTotal &gt; 4000.0") {
    orderId
    orderTotal
    customerId
    customer {
      name
    }
  }
}</markup>

<markup
lang="json"

>{
  "data": {
    "displayOrders": [
      {
        "orderId": 101,
        "orderTotal": "$4,077.69",
        "customerId": 1,
        "customer": {
          "name": "Billy Joel"
        }
      },
      {
        "orderId": 105,
        "orderTotal": "$4,629.24",
        "customerId": 3,
        "customer": {
          "name": "John Williams"
        }
      },
      {
        "orderId": 104,
        "orderTotal": "$8,078.11",
        "customerId": 3,
        "customer": {
          "name": "John Williams"
        }
      }
    ]
  }
}</markup>

</li>
<li>
Use a more complex where clause:
<markup
lang="graphql"

>query ordersWithWhereClause2 {
  displayOrders(whereClause: "orderTotal &gt; 4000.0 and customerId = 1") {
    orderId
    orderTotal
    customerId
    customer {
      name
    }
  }
}</markup>

<markup
lang="json"

>{
  "data": {
    "displayOrders": [
      {
        "orderId": 101,
        "orderTotal": "$4,077.69",
        "customerId": 1,
        "customer": {
          "name": "Billy Joel"
        }
      }
    ]
  }
}</markup>

</li>
</ol>
</div>

<h3 id="run-the-completed-tutorial">Run the Completed Tutorial</h3>
<div class="section">

<h4 id="_building_the_example_code_2">Building the Example Code</h4>
<div class="section">
<div class="admonition note">
<p class="admonition-inline">As the <a id="" title="" target="_blank" href="https://github.com/graphql/graphiql">GraphiQL UI</a> client used in this example is not included
in this repository, before carrying out the build instructions below you must copy the <code>index.html</code> file contents
from <a id="" title="" target="_blank" href="https://github.com/graphql/graphiql/blob/main/packages/graphiql/README.md#cdn-bundle">https://github.com/graphql/graphiql/blob/main/packages/graphiql/README.md#cdn-bundle</a>
into the file in <code>src/main/resources/web/index.html</code>.</p>
</div>
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

<ul class="ulist">
<li>
<p>Run with Maven</p>

</li>
</ul>
<markup
lang="bash"

>./mvnw clean package</markup>

<ul class="ulist">
<li>
<p>Run with Gradle</p>

</li>
</ul>
<markup
lang="bash"

>./gradlew runApp</markup>

</div>

<h4 id="_run_the_example_code">Run the Example Code</h4>
<div class="section">
<p>Open the GraphiQL UI at <a id="" title="" target="_blank" href="http://localhost:7001/ui">http://localhost:7001/ui</a> and
copy the sample GraphQL queries and mutations below into the editor
and use the <code>Play</code> button at the top to try out GraphQL against your Coherence cluster.</p>

<markup
lang="graphql"

>fragment customer on Customer {
    customerId
    name
    address
    email
    balance
    orders {
      orderId
      orderTotal
    }
}

fragment order on Order {
   orderId
    customerId
    customer {
      name
    }
    orderDate
    orderTotal
    orderLines {
      lineNumber
      productDescription
      itemCount
      costPerItem
      orderLineTotal
    }
}

query customers {
  customers {
     ...customer
  }
}

query orders {
  displayOrders {
     ...order
  }
}

query ordersWithWhereClause {
  displayOrders(whereClause: "orderTotal &gt; 4000.0") {
    orderId
    orderTotal
    customerId
    customer {
      name
    }
  }
}

query ordersWithWhereClause2 {
  displayOrders(whereClause: "orderTotal &gt; 4000.0 and customerId = 1") {
    orderId
    orderTotal
    customerI
    customer {
      name
    }
  }
}

mutation createNewCustomer {
  createCustomer(customer: { customerId: 12 name: "Tim" balance: 1000}) {
     ...customer
  }
}

mutation createOrderForCustomer {
  createOrder(customerId: 12 orderId: 200) {
      ...order
  }
}

mutation addOrderLineToOrder {
  addOrderLineToOrder(orderId: 200 orderLine: {productDescription: "iPhone 12" costPerItem: 1500 }) {
    ...order
  }
}</markup>

</div>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this tutorial you have seen how easy it is to expose Coherence Data using GraphQL.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://helidon.io/docs/v2/#/mp/introduction/01_introduction">Helidon MP Documentation</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://github.com/eclipse/microprofile-graphql">Microprofile GraphQL Specification</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
