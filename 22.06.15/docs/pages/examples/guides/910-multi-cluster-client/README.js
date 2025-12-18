<doc-view>

<h2 id="_multi_cluster_client">Multi-Cluster Client</h2>
<div class="section">
<p>A Coherence application JVM can only be a member of zero or one Coherence cluster at any point in time. It can however, connect to zero, one or many Coherence clusters as a client.
When building a Coherence client application (either an Extend client or a gRPC client) the application may need to connect to more than one Coherence cluster. Exactly how this is achieved, and the relative simplicity, depends on the version of Coherence being used.</p>

<p>This example uses the Bootstrap API introduced in Coherence CE 20.12, and enhancements made to it in 22.06.</p>

</div>

<h2 id="_client_sessions">Client Sessions</h2>
<div class="section">
<p>Using the bootstrap API, access to Coherence resources is via an instance of <code>com.tangosol.net.Session</code> where either the default <code>Session</code> is used, or one or more sessions are configured at start-up. In a multi-tenant client, where all the tenants are known ahead of time, this pattern is still usable, a client <code>Session</code> can be configured at start-up for each tenant.
In this example the tenant list can be dynamic, to make the example a little more interesting and to show how to create a new <code>Session</code> at runtime.</p>

<p>The bootstrap API starts one or more <code>Coherence</code> instances, each of which manages one or more <code>Session</code> instances.
For the multi-tenant use case there is no need to have multiple <code>Coherence</code> instances.
The default instance can be used, then new tenant specific client sessions are created as required.</p>

<p>Every <code>Session</code> must have a unique name (and for an Extend client session, either a unique cache configuration URI, or a unique scope name). This example uses the same cache configuration file for all tenants (which is how most multi-tenant applications would work)
so the session&#8217;s name and scope name are both set to the tenant name to ensure uniqueness.</p>


<h3 id="_create_an_extend_client_session">Create an Extend Client Session</h3>
<div class="section">
<p>Creating an Extend client could be as simple as the example below.</p>

<p>This example will create an Extend client using the default Coherence cache configuration (as the <code>withConfigUri</code> has not been used to specify a cache configuration file). Both the session name and scope name are set to the tenant name. A number of parameters are also set in the configuration, this is explained further below.</p>

<markup
lang="java"

>public Session getSession(String tenant)
    {
    Coherence coherence = Coherence.getInstance();    <span class="conum" data-value="1" />
    Optional&lt;Session&gt; optional = Coherence.findSession(tenant); <span class="conum" data-value="2" />
    if (optional.isPresent())
        {
        return optional.get();  <span class="conum" data-value="3" />
        }

    coherence.addSessionIfAbsent(tenant, () -&gt;     <span class="conum" data-value="4" />
            SessionConfiguration.builder()
                    .named(tenant)
                    .withScopeName(tenant)
                    .withParameter("coherence.client", "remote-fixed")
                    .withParameter("coherence.serializer", "java")
                    .withParameter("coherence.extend.address", "127.0.0.1")
                    .withParameter("coherence.extend.port", 20000)
                    .build());

    return coherence.getSession(tenant);  <span class="conum" data-value="5" />
    }</markup>

<ul class="colist">
<li data-value="1">Obtain the default <code>Coherence</code> instance</li>
<li data-value="2">Find the <code>Session</code> with the tenant name</li>
<li data-value="3">The <code>Session</code> has already been created, so use it</li>
<li data-value="4">Use the <code>Coherence.addSessionIfAbsent()</code> method to add a <code>SessionConfiguration</code> for the tenant. The <code>addSessionIfAbsent</code> method is used to be slightly more thread safe.</li>
<li data-value="5">Return the <code>Session</code> for the configuration name just added</li>
</ul>

<h4 id="_session_configuration_parameters">Session Configuration Parameters</h4>
<div class="section">
<p>The <code>withParameter</code> method on the <code>SessionConfiguration.Builder</code> is used to pass parameters to the cache configuration.
Cache configuration files can be parameterized using the <code>system-property</code> attribute on elements.
Typically, the values used for these elements are taken from corresponding system properties or environment variables.
By using the <code>withParameter</code> method on a <code>SessionConfiguration.Builder</code> values for these elements can also be provided.</p>

<p>For example, the <code>&lt;remote-cache-scheme&gt;</code> below has the <code>&lt;address&gt;</code> and <code>&lt;port&gt;</code> elements parameterized.
The <code>&lt;address&gt;</code> element&#8217;s value will come from the <code>coherence.extend.address</code> System property (or <code>COHERENCE_EXTEND_ADDRESS</code> environment variable).
The <code>&lt;port&gt;</code> element&#8217;s value will come from the <code>coherence.extend.port</code> System property (or <code>COHERENCE_EXTEND_PORT</code> environment variable).
There are a number of alternative ways to configures the address for a remote gRPC scheme, which are covered on the Coherence documentation.</p>

<markup
lang="xml"

>&lt;remote-cache-scheme&gt;
  &lt;scheme-name&gt;thin-remote-fixed&lt;/scheme-name&gt;
  &lt;service-name&gt;RemoteCache&lt;/service-name&gt;
  &lt;initiator-config&gt;
    &lt;tcp-initiator&gt;
      &lt;remote-addresses&gt;
        &lt;socket-address&gt;
          &lt;address system-property="coherence.extend.address"/&gt;
          &lt;port system-property="coherence.extend.port"/&gt;
        &lt;/socket-address&gt;
      &lt;/remote-addresses&gt;
    &lt;/tcp-initiator&gt;
  &lt;/initiator-config&gt;
&lt;/remote-cache-scheme&gt;</markup>

<p>When creating a <code>SessionConfiguration</code>, those values can also be specified as configuration parameters.</p>

<markup
lang="java"

>SessionConfiguration.builder()
        .withMode(Coherence.Mode.ClientFixed)
        .withParameter("coherence.extend.address", "127.0.0.1")
        .withParameter("coherence.extend.port", 20000)</markup>

</div>
</div>

<h3 id="_create_a_grpc_client_session">Create a gRPC Client Session</h3>
<div class="section">
<p>Since Coherence 22.06.2, creating a gRPC client session is a simple as creating an Extend client session. A <code>&lt;remote-grpc-cache-scheme&gt;</code> can be configured in a Coherence cache configuration file. The <code>&lt;remote-grpc-cache-scheme&gt;</code> can contain a <code>&lt;grpc-channel&gt;</code> element that configures the channel that the client will use to connect to the gRPC proxy in the Coherence cluster.
There are a number of alternative ways to configure the <code>&lt;remote-grpc-cache-scheme&gt;</code> and <code>&lt;grpc-channel&gt;</code> elements, which are covered on the Coherence documentation.</p>

<p>An example of a <code>&lt;remote-grpc-cache-scheme&gt;</code> is shown below. In this case the <code>&lt;grpc-channel&gt;</code> is configured with a single fixed address that the gRPC client connects to. The <code>&lt;address&gt;</code> and <code>&lt;port&gt;</code> elements below do not actually have values, the values of those elements will be supplied by the <code>coherence.grpc.address</code> and <code>coherence.grpc.port</code> system properties or by the <code>COHERENCE_GRPC_ADDRESS</code> and <code>COHERENCE_GRPC_PORT</code> environment variables, or by setting them in the Session configuration properties.</p>

<markup
lang="xml"

>&lt;remote-grpc-cache-scheme&gt;
  &lt;scheme-name&gt;thin-grpc-fixed&lt;/scheme-name&gt;
  &lt;service-name&gt;RemoteGrpcCache&lt;/service-name&gt;
  &lt;grpc-channel&gt;
    &lt;remote-addresses&gt;
      &lt;socket-address&gt;
        &lt;address system-property="coherence.grpc.address"/&gt;
        &lt;port system-property="coherence.grpc.port"/&gt;
      &lt;/socket-address&gt;
    &lt;/remote-addresses&gt;
  &lt;/grpc-channel&gt;
&lt;/remote-grpc-cache-scheme&gt;</markup>

<p>When creating a <code>SessionConfiguration</code>, the <code>&lt;address&gt;</code> and <code>&lt;port&gt;</code> values can also be specified as configuration parameters.
For example, the <code>SessionConfiguration</code> below will configure the gRPC channel to connect to loopback (<code>127.0.0.1</code>) and port <code>1408</code>.</p>

<markup
lang="java"

>SessionConfiguration.builder()
        .withMode(Coherence.Mode.GrpcFixed)
        .withParameter("coherence.grpc.address", "127.0.0.1")
        .withParameter("coherence.grpc.port", 1408)</markup>

</div>
</div>

<h2 id="_building_the_example">Building the Example</h2>
<div class="section">
<p>The example application is a web-server that gets data to service requests from a specific Coherence cluster depending on a header value in the http request. This demonstrates a simple stateless multi-tenant web-server, where the tenant&#8217;s data is segregated into different Coherence clusters. The information about a tenant&#8217;s connection details are held in a meta-data cache in a separate admin cluster.</p>


<h3 id="_what_you_need">What You Need</h3>
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

<h3 id="_building_the_example_code">Building the Example Code</h3>
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


<h4 id="_build_the_example_image">Build the Example Image</h4>
<div class="section">
<p>The simplest way to run the example is to build the image and run the application web-server in a container.</p>

<p>The example Maven and Gradle build files contain tasks to pull together all the dependencies and docker file into a directory. For Maven this will be <code>target/docker</code> and for Gradle this will be <code>build/docker</code>.
The build then executes the Docker build command in that directory to build the image.</p>

<p>Using Maven:</p>

<markup
lang="bash"

>./mvnw clean package -DskipTests -P build-image</markup>

<p>Using Gradle</p>

<markup
lang="bash"

>./gradlew clean buildImage</markup>

<p>Both of the commands above will create two images, one for the example server and one for the client</p>

<ul class="ulist">
<li>
<p>Server image <code>ghcr.io/coherence-community/multi-cluster-server:latest</code></p>

</li>
<li>
<p>Client image <code>ghcr.io/coherence-community/multi-cluster-client:latest</code></p>

</li>
</ul>
</div>
</div>
</div>

<h2 id="_running_the_example">Running the Example</h2>
<div class="section">
<p>The point of this example is to show a client connecting to multiple clusters, so running the examples requires also running a number of Coherence clusters.
To make running simple, each clusters will just be a single member.</p>


<h3 id="_create_a_docker_network">Create a Docker Network</h3>
<div class="section">
<p>So that the client can communicate with the cluster members, a Docker network is required.
The command below will create a Docker network named <code>coherence-net</code></p>

<markup
lang="bash"

>docker network create --driver bridge coherence-net</markup>

</div>

<h3 id="_start_the_coherence_clusters">Start the Coherence Clusters</h3>
<div class="section">
<p>The example requires three clusters.
The first is the tenant "admin" cluster that holds information about the tenants.
Then there are two additional clusters for each tenant, in this case "Marvel" and "Star Wars"</p>

<p>Start the admin cluster, this will hold tenant meta-data.</p>

<markup
lang="bash"

>docker run -d --name tenants --network coherence-net \
    -e COHERENCE_CLUSTER=tenants \
    ghcr.io/coherence-community/multi-cluster-server:latest</markup>

<p>Start the cluster for the Marvel tenant.</p>

<markup
lang="bash"

>docker run -d --name marvel --network coherence-net \
    -e COHERENCE_CLUSTER=marvel \
    ghcr.io/coherence-community/multi-cluster-server:latest</markup>

<p>Start the cluster for the Star Wars tenant.</p>

<markup
lang="bash"

>docker run -d --name star-wars --network coherence-net \
    -e COHERENCE_CLUSTER=star-wars \
    ghcr.io/coherence-community/multi-cluster-server:latest</markup>

<p>After starting all three clusters, the <code>docker ps</code> command can be used to check their status.
Eventually the <code>STATUS</code> colum of each container should say <code>(healthy)</code>.</p>

<markup


>CONTAINER ID   IMAGE                                                     COMMAND                  CREATED         STATUS                   PORTS                           NAMES
4abdc735b7bd   ghcr.io/coherence-community/multi-cluster-server:latest   "java -cp /app/class?"   2 minutes ago   Up 2 minutes (healthy)   1408/tcp, 9612/tcp, 20000/tcp   star-wars
5df54737eb6a   ghcr.io/coherence-community/multi-cluster-server:latest   "java -cp /app/class?"   2 minutes ago   Up 2 minutes (healthy)   1408/tcp, 9612/tcp, 20000/tcp   marvel
87f9ee53dfc5   ghcr.io/coherence-community/multi-cluster-server:latest   "java -cp /app/class?"   3 minutes ago   Up 3 minutes (healthy)   1408/tcp, 9612/tcp, 20000/tcp   tenants</markup>

</div>

<h3 id="_start_the_web_server">Start the Web-Server</h3>
<div class="section">
<p>When all the clusters are running and healthy, the multi-tenant client can be started using the command below.
This will start the webserver and expose the endpoints on <code><a id="" title="" target="_blank" href="http://127.0.0.1:8080">http://127.0.0.1:8080</a></code>.</p>

<markup
lang="bash"

>docker run -d --name webserver --network coherence-net \
    -e COHERENCE_EXTEND_ADDRESS=tenants \
    -e COHERENCE_EXTEND_PORT=20000 \
    -p 8080:8080 \
    ghcr.io/coherence-community/multi-cluster-client:latest</markup>

<p>Using <code>docker ps</code> the status of the <code>webserver</code> container should eventually be <code>(healthy)</code> too.</p>

</div>

<h3 id="_create_the_tenant_meta_data">Create the Tenant Meta-Data</h3>
<div class="section">
<p>Once the <code>webserver</code> container is healthy the <code>/tenants</code> endpoint can be used to create the metadata for the two tenants.</p>

<p>The curl command below will add the meta-data for the Marvel tenant. This will connect to the Marvel cluster using Coherence Extend on port 20000. The default extend proxy port in the server container is 20000.</p>

<markup
lang="bash"

>curl -i -w '\n' -X POST http://127.0.0.1:8080/tenants \
    -d '{"tenant":"marvel","type":"extend","hostName":"marvel","port":20000,"serializer":"java"}'</markup>

<p>This should return a 200 response as show below:</p>

<markup
lang="bash"

>HTTP/1.1 200 OK
Date: Thu, 07 Jul 2022 15:15:26 GMT
Transfer-encoding: chunked
{
  "@class":"com.oracle.coherence.guides.client.model.TenantMetaData",
  "hostName":"marvel",
  "port":20000,
  "serializer":"java",
  "tenant":"marvel",
  "type":"extend"
}</markup>

<p>The curl command below will add the meta-data for the Star Wars tenant. This will connect to the Star Wars cluster using Coherence gRPC API on port 1408. The default gRPC port in the server container is 1408.</p>

<markup
lang="bash"

>curl -i -w '\n' -X POST http://127.0.0.1:8080/tenants \
    -d '{"tenant":"star-wars","type":"grpc","hostName":"star-wars","port":1408,"serializer":"java"}'</markup>

<p>This should return a 200 response as show below:</p>

<markup
lang="bash"

>HTTP/1.1 200 OK
Date: Thu, 07 Jul 2022 15:17:49 GMT
Transfer-encoding: chunked
{
  "@class":"com.oracle.coherence.guides.client.model.TenantMetaData",
  "hostName":"star-wars",
  "port":1408,
  "serializer":"java",
  "tenant":"star-wars",
  "type":"grpc"
}</markup>

</div>

<h3 id="_access_the_multi_tenant_endpoints">Access the Multi-Tenant Endpoints</h3>
<div class="section">
<p>First, try a simple GET request without a tenant header value.</p>

<markup
lang="bash"

>curl -i -w '\n' -X GET http://127.0.0.1:8080/users/foo</markup>

<p>This should return a 400 bad request response as shown below</p>

<markup
lang="bash"

>HTTP/1.1 400 Bad Request
Date: Thu, 07 Jul 2022 15:33:23 GMT
Transfer-encoding: chunked

{"Error":"Missing tenant identifier"}</markup>

<p>Now try the same get, with a valid tenant identifier in the header.</p>

<markup
lang="bash"

>curl -i -w '\n' -H 'tenant: marvel' -X GET http://127.0.0.1:8080/users/foo</markup>

<p>This should return a 404, as no users have been created yet.</p>

<markup
lang="bash"

>HTTP/1.1 404 Not Found
Date: Thu, 07 Jul 2022 15:35:26 GMT
Transfer-encoding: chunked

{"Error":"Unknown user foo"}</markup>

<p>Create a <code>User</code> in the Marvel cluster with the command below, using the <code>marvel</code> tenant identifier in the header:</p>

<markup
lang="bash"

>curl -i -w '\n' -H 'tenant: marvel' -X POST http://127.0.0.1:8080/users \
    -d '{"firstName":"Iron","lastName":"Man","email":"iron.man@marvel.com"}'</markup>

<p>The response should be a 200 response, with the json of the user created. This will include the ID of the new user,
in this case the ID is <code>Iron.Man</code>.</p>

<markup
lang="bash"

>HTTP/1.1 200 OK
Date: Thu, 07 Jul 2022 15:37:04 GMT
Transfer-encoding: chunked

{
  "@class":"com.oracle.coherence.guides.client.model.User",
  "email":"iron.man@marvel.com",
  "firstName":"Iron",
  "id":"Iron.Man",
  "lastName":"Man"
}</markup>

<p>Now get the <code>Iron.Man</code> user from the Marvel cluster:</p>

<markup
lang="bash"

>curl -i -w '\n' -H 'tenant: marvel' -X GET http://127.0.0.1:8080/users/Iron.Man</markup>

<p>This should respond with a 200 response code and the same json as above.</p>

<p>Next, try to get the <code>Iron.Man</code> user from the Star Wars cluster by using the <code>star-wars</code> tenant ID in the header</p>

<markup
lang="bash"

>curl -i -w '\n' -H 'tenant: star-wars' -X GET http://127.0.0.1:8080/users/Iron.Man</markup>

<p>The response should be a 404, not-found, as the <code>Iron.Man</code> user is not in the Star Wars tenant&#8217;s cluster.</p>


<h4 id="_clean_up">Clean-Up</h4>
<div class="section">
<p>The demo is complete so everything can be cleaned up.</p>

<markup
lang="bash"

>docker rm -f webserver tenants marvel star-wars
docker network rm coherence-net</markup>

</div>
</div>
</div>

<h2 id="_the_example_application">The Example Application</h2>
<div class="section">
<p>The application is a very basic CRUD application to manage simple <code>User</code> entities in a Coherence cache.
The application exposes a REST API with get, create (POST), update (PUT) and delete methods.
The application is multi-tenanted, so each request has the relevant tenant identifier in a request header.
Requests without a tenant identifier, or with an unknown tenant, are rejected.</p>

<p>The web-server is a Coherence client application, and is hence storage disabled.
The data for each tenant is held in a separate storage enabled Coherence cluster for each tenant.
There is an admin cluster that holds the meta-data about tenants.
This allows tenants and their cluster details to be added and maintained at runtime.
The main reason for doing this in the example, is because it makes testing much simpler.</p>

<p>A stateless web server that accesses data to service requests from different Coherence clusters has various pros and cons with its design, but the purpose of this example is to show connecting Coherence clients to different clusters, its purpose is not to produce the best, most efficient, web application.
There is certainly no security built in to this example.</p>

<p>The actual web-server implementation used in this example is unimportant and not really relevant to the example code.
The code shown here could easily be ported to other web-application frameworks, such as Coherence CDI with <a id="" title="" target="_blank" href="http://helidon.io">Helidon</a>,
Coherence and <a id="" title="" target="_blank" href="https://github.com/coherence-community/coherence-spring">Spring</a>, or Coherence and
<a id="" title="" target="_blank" href="https://micronaut-projects.github.io/micronaut-coherence/latest/guide/">Micronaut</a>, etc.</p>


<h3 id="_the_data_model">The Data Model</h3>
<div class="section">
<p>The data model used in this example is a simple <code>User</code> entity, with an id, a first name, a last name and an email address.</p>

<p>A snippet of the source is shown below, the actual code includes serialization support for both Java and portable object format serialization.</p>

<markup
lang="java"
title="User.java"
>/**
 * A simple user entity.
 */
public class User
        implements PortableObject, ExternalizableLite {

    /**
     * The user's identifier.
     */
    private String id;

    /**
     * The user's first name.
     */
    private String firstName;

    /**
     * The user's last name.
     */
    private String lastName;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * A default constructor, required for Coherence serialization.
     */
    public User() {
    }

    /**
     * Create a user.
     *
     * @param id         the user's identifier
     * @param firstName  the user's first name
     * @param lastName   the user's last name
     * @param email      the user's email address
     */
    public User(String id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Returns the user's identifier.
     *
     * @return the user's identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Set the user's identifier.
     *
     * @param id  the user's identifier
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the user's first name.
     *
     * @return the user's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the user's first name.
     *
     * @param firstName  the user's first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the user's last name.
     *
     * @return the user's last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the user's last name.
     *
     * @param lastName  the user's last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns the user's email address.
     *
     * @return the user's email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Set the user's email address.
     *
     * @param email  the user's email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
    }</markup>

</div>

<h3 id="_the_main_class">The Main Class</h3>
<div class="section">
<p>The application does not have a main class with a main method.
The <code>Application</code> class in the example code implements <code>Coherence.LifecycleListener</code> and will be discovered by <code>Coherence</code> using the Java <code>ServiceLoader</code>. The <code>Application</code> class then receives events when the Coherence bootstrap API starts and stops Coherence.
Using these events, the <code>Application</code> class configures, starts and stops the web-server.
The actual code is not discussed in detail here as it is not particularly relevant for the example.</p>

<p>The application is started by running the <code>com.tangosol.net.Coherence</code> class; in this case <code>Coherence</code> is started as a client.</p>

</div>

<h3 id="_the_tenantcontroller">The TenantController</h3>
<div class="section">
<p>The <code>TenantController</code> class is one of two classes that expose REST endpoints in the web-server.
The purpose of the <code>TenantController</code> is to perform CRUD operations on tenants, to allow runtime configuration of tenants,
and which cluster a given tenant should connect to.</p>

<p>Tenant meta-data is contained in a <code>TenantMetaData</code> class. This holds the tenant name, the host name and port of the Coherence cluster holding the tenants data and whether the client session should use Coherence Extend or Coherence gRPC to connect to the cluster.</p>

</div>

<h3 id="_the_usercontroller">The UserController</h3>
<div class="section">
<p>The <code>UserController</code> class is the main class in the example.
This class exposes some REST endpoints to perform CRUD operations on <code>User</code> entities.
There are four methods supported by the controller, <code>POST</code> to create a user, 'PUT' to update a user, <code>GET</code> to get a user and <code>DELETE</code> to delete a user. Every request much contain a <code>tenant</code> header with the name of the tenant as the header value.
Any request without a tenant header is rejected.</p>

</div>
</div>

<h2 id="_implementing_multi_tenancy">Implementing Multi-Tenancy</h2>
<div class="section">
<p>In this example, the tenants are dynamic and Coherence client sessions are created on-demand using meta-data held in a tenants cache.
An alternative would have been to create all the tenant client sessions when the application started up using the Coherence Bootstrap API to configure them.
This would have been a much simpler example, but then testing and demonstrating it would have been harder.
A more dynamic multi-tenant system is probably closer to a real-world scenario.</p>

<p>When the web-server starts it connects using Coherence Extend to the tenant meta-data cluster and obtains a reference to a Coherence <code>NamedMap</code> to hold tenant meta-data.</p>


<h3 id="_creating_a_tenants_session">Creating a Tenant&#8217;s Session</h3>
<div class="section">
<p>When a request comes to the <code>UserController</code> a Coherence <code>Session</code> must be obtained for the tenant.
All the requests perform the same logic, the code below is from the get request handler.</p>

<markup
lang="java"
title="UserController.java"
>    public Response get(HttpRequest request) {
        String tenant = request.getHeaderString(TENANT_HEADER);   <span class="conum" data-value="1" />
        if (tenant == null || tenant.isBlank()) {
            <span class="conum" data-value="2" />
            return Response.status(400).entity(Map.of("Error", "Missing tenant identifier")).build();
        }
        Session session = ensureSession(tenant);  <span class="conum" data-value="3" />
        if (session == null) {
            <span class="conum" data-value="4" />
            return Response.status(400).entity(Map.of("Error", "Unknown tenant " + tenant)).build();
        }</markup>

<ul class="colist">
<li data-value="1">The "tenant" header value is obtained from the request</li>
<li data-value="2">If there is no tenant header a 400 response is returned</li>
<li data-value="3">A <code>Session</code> is obtained for the tenant (this is covered in detail below)</li>
<li data-value="4">If the <code>Session</code> is <code>null</code>, a 400 response is returned</li>
</ul>
<p>Once a valid <code>Session</code> has been obtained for the tenant, the rest of the request processing can continue.</p>


<h4 id="_the_ensuresession_method">The ensureSession Method</h4>
<div class="section">
<p>The work of obtaining or creating a <code>Session</code> for a tenant is in the <code>UserController.ensureSession</code> method.</p>

<markup
lang="java"
title="UserController.java"
>    private Session ensureSession(String tenant) {
        TenantMetaData metaData = tenants.get(tenant);  <span class="conum" data-value="1" />
        if (metaData == null) {
            return null;  <span class="conum" data-value="2" />
        }
        Coherence coherence = Coherence.getInstance();  <span class="conum" data-value="3" />
        return coherence.getSessionIfPresent(tenant)  <span class="conum" data-value="4" />
                        .orElseGet(()-&gt;createSession(coherence, metaData));
    }</markup>

<ul class="colist">
<li data-value="1">The meta-data for the tenant is obtained from the tenants cache.</li>
<li data-value="2">If there is no meta-data in the cache, the method returns <code>null</code>.</li>
<li data-value="3">The default <code>Coherence</code> instance is obtained, as this will be the owner of all the client <code>Session</code> instances.</li>
<li data-value="4">The <code>Coherence.getSessionIfPresent()</code> method is called, which will return an existing <code>Session</code> for a given tenant name if one exists.</li>
</ul>
<p>The <code>Coherence.getSessionIfPresent()</code> returns an <code>Optional&lt;Session&gt;</code> and if this is empty,
the supplier in the <code>orElseGet()</code> method is called, which calles the <code>UserController.createSession()</code> method to actually create a <code>Session</code>.</p>

</div>

<h4 id="_the_createsession_method">The createSession Method</h4>
<div class="section">
<p>If a <code>Session</code> does not yet exist for a tenant, one must be created from the <code>TenantMetaData</code> for the tenant.
The <code>UserController.createSession()</code> method is responsible for creating a <code>Session</code> for a tenant.</p>

<markup
lang="java"
title="UserController.java"
>    private Session createSession(Coherence coherence, TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        if (metaData.isExtend()) {
            coherence.addSessionIfAbsent(tenant, ()-&gt;createExtendConfiguration(metaData));
        }
        else {
            coherence.addSessionIfAbsent(tenant, ()-&gt;createGrpcConfiguration(metaData));
        }
        return coherence.getSession(tenant);
    }</markup>

<p>The <code>createSession</code> method is very simple, it just delegates to another method, depending on whether the required <code>Session</code> is for an Extend client or a gRPC client.
A <code>SessionConfiguration</code> is created, either for an Extend client, or gRPC client, and is passed to the <code>Coherence.addSessionIfAbsent()</code> method. The add if absent method is used in case multiple threads attempt to create the same tenant&#8217;s session, it will only be added once.</p>

</div>

<h4 id="_creating_an_extend_session">Creating an Extend Session</h4>
<div class="section">
<p>A <code>Session</code> is simple to add to a running <code>Coherence</code> instance. It just requires creating a <code>SessionConfiguration</code> instance and adding it to the <code>Coherence</code> instance.
An Extend client configuration can be created using the <code>SessionConfiguration</code> builder.</p>

<markup
lang="java"
title="UserController.java"
>    private SessionConfiguration createExtendConfiguration(TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        return SessionConfiguration.builder()
                                   .named(tenant)             <span class="conum" data-value="1" />
                                   .withScopeName(tenant)     <span class="conum" data-value="2" />
                                   .withMode(Coherence.Mode.ClientFixed)  <span class="conum" data-value="3" />
                                   .withParameter("coherence.serializer", metaData.getSerializer())   <span class="conum" data-value="4" />
                                   .withParameter("coherence.extend.address", metaData.getHostName()) <span class="conum" data-value="5" />
                                   .withParameter("coherence.extend.port", metaData.getPort())        <span class="conum" data-value="6" />
                                   .build();  <span class="conum" data-value="7" />
    }</markup>

<ul class="colist">
<li data-value="1">The session configuration has a unique name, in this case the tenant name</li>
<li data-value="2">A session configuration typically has a unique scope, in this case also the tenant name</li>
<li data-value="3">The <code>coherence.client</code> parameter is set to <code>remote-fixed</code>. This is used by the default Coherence cache configuration file to make it use a fixed address Extend client configuration.</li>
<li data-value="4">The name of the serializer is configured (in this example Java serialization is used, but "pof" would also be supported)</li>
<li data-value="5">The <code>coherence.extend.address</code> property is passed through to the cache configuration file, in this case the address comes from the tenant meta-data.</li>
<li data-value="6">The <code>coherence.extend.port</code> property is passed through to the cache configuration file, in this case the address comes from the tenant meta-data.</li>
<li data-value="7">finally the configuration is built and returned.</li>
</ul>
</div>

<h4 id="_creating_a_grpc_session">Creating a gRPC Session</h4>
<div class="section">
<p>Creating a gRPC <code>Session</code> is as simple as creating an Extend <code>Session</code>.</p>

<markup
lang="java"
title="UserController.java"
>    private SessionConfiguration createGrpcConfiguration(TenantMetaData metaData) {
        String tenant = metaData.getTenant();
        return SessionConfiguration.builder()
                                   .named(tenant)             <span class="conum" data-value="1" />
                                   .withScopeName(tenant)     <span class="conum" data-value="2" />
                                   .withMode(Coherence.Mode.GrpcFixed)  <span class="conum" data-value="3" />
                                   .withParameter("coherence.serializer", metaData.getSerializer()) <span class="conum" data-value="4" />
                                   .withParameter("coherence.grpc.address", metaData.getHostName()) <span class="conum" data-value="5" />
                                   .withParameter("coherence.grpc.port", metaData.getPort())        <span class="conum" data-value="6" />
                                   .build();  <span class="conum" data-value="7" />
    }</markup>

<ul class="colist">
<li data-value="1">The session configuration has a unique name, in this case the tenant name</li>
<li data-value="2">A session configuration typically has a unique scope, in this case also the tenant name</li>
<li data-value="3">The <code>coherence.client</code> parameter is set to <code>grpc-fixed</code>. This is used by the default Coherence cache configuration file to make it use a fixed address gRPC client configuration.</li>
<li data-value="4">The name of the serializer is configured (in this example Java serialization is used, but "pof" would also be supported)</li>
<li data-value="5">The <code>coherence.grpc.address</code> property is passed through to the cache configuration file, in this case the address comes from the tenant meta-data.</li>
<li data-value="6">The <code>coherence.grpc.port</code> property is passed through to the cache configuration file, in this case the address comes from the tenant meta-data.</li>
<li data-value="7">finally the configuration is built and returned.</li>
</ul>
</div>

<h4 id="_summary">Summary</h4>
<div class="section">
<p>The example code could be simplified if the application only ever used Extend or only ever used gRPC.
There are also many alternative approaches to holding tenant metata data used to create the sessions.</p>

<p>The important parts of the example are the methods in <code>UserController</code> to obtain a session from the <code>Coherence</code> instance, and create a new <code>Session</code> is one does not already exist.</p>

</div>
</div>
</div>
</doc-view>
