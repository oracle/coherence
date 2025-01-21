<doc-view>

<h2 id="_cdi_response_caching">CDI Response Caching</h2>
<div class="section">
<p>CDI Response Caching allows you to cache the results of method invocations.
Each time a target method is invoked, CDI interceptors check whether the method
has already been invoked for the given arguments. If the method has been invoked,
the cached result is returned without invoking the target method again.
If there are no cached results because the method hasn&#8217;t been invoked yet
or because the result was removed from the cache, the target method is invoked,
the result is cached, and then returned to the caller.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>The example code is written as a set of unit tests, showing you how to use
CDI Caching Response annotations.</p>

</div>

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
<p><a id="" title="" target="_blank" href="https://www.oracle.com/java/technologies/downloads/">JDK 17</a> or later</p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://maven.apache.org/download.cgi">Maven 3.8+</a> or <a id="" title="" target="_blank" href="https://gradle.org/install/">Gradle 4+</a>
Although the source comes with the Maven and Gradle wrappers included, so they can be built without first installing
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
</div>

<h3 id="_example_data_model">Example Data Model</h3>
<div class="section">
<p>The data model for this guide consists of a single class named <code>Message</code>. It
represents a message for a user and has a single property: message.</p>

</div>

<h3 id="_create_the_resource_class_and_junit_test_class">Create the Resource Class and JUnit Test Class</h3>
<div class="section">
<p>The first step is to create root JAX-RS resource class that will be used to
test the various response caching operations. Resource will be using <code>messages-cache</code>
to store cached messages.</p>

<markup
lang="java"

>@Path("/")
@RequestScoped
@CacheName("messages-cache")
public class GreetResource {
    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheGet}.
     */
    public static final AtomicInteger GET_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheAdd}.
     */
    public static final AtomicInteger ADD_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CachePut}.
     */
    public static final AtomicInteger PUT_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method annotated with {@link CacheRemove}.
     */
    public static final AtomicInteger REMOVE_CALLS = new AtomicInteger();

    /**
     * This is used to track the count of invocations of a method with multiple parameters that
     * are used to build cache key.
     */
    public static final AtomicInteger MULTI_PARAM_CALLS = new AtomicInteger();</markup>

<p>and test class:</p>

<markup
lang="java"

>@HelidonTest
public class GreetResourceTest {

    @Inject
    private WebTarget target;

    @Inject
    @Name("messages-cache")
    private NamedMap cache; <span class="conum" data-value="1" />

    @Inject
    @Name("another-cache")
    private NamedMap anotherCache;

    @BeforeAll
    static void boot() {
        System.setProperty("coherence.wka", "127.0.0.1");
    }

    @BeforeEach
    void setup() { <span class="conum" data-value="2" />
        cache.clear();
        anotherCache.clear();
        GreetResource.GET_CALLS.set(0);
        GreetResource.ADD_CALLS.set(0);
        GreetResource.PUT_CALLS.set(0);
        GreetResource.REMOVE_CALLS.set(0);
    }</markup>

<ul class="colist">
<li data-value="1">Inject cache so we can verify its content</li>
<li data-value="2">Reset cache content and counters before each test</li>
</ul>
</div>

<h3 id="_response_caching_operations">Response Caching Operations</h3>
<div class="section">
<p>Let&#8217;s add resource method and test for each response caching operation:</p>


<h4 id="_cacheget">@CacheGet</h4>
<div class="section">
<p>@CacheGet gets the value from the cache if present; invokes the target method
and caches the result otherwise.</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("greet/{name}")
@GET
@Produces(MediaType.APPLICATION_JSON)
@CacheGet <span class="conum" data-value="1" />
public Message getMessage(@PathParam("name") String name) { <span class="conum" data-value="2" />
    GET_CALLS.incrementAndGet(); <span class="conum" data-value="3" />
    return new Message("Hello " + name); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">We&#8217;ll test @CacheGet annotation processing</li>
<li data-value="2">Cache key will be <code>name</code> argument</li>
<li data-value="3">We&#8217;ll count number of method invocations</li>
<li data-value="4">Result of the method invocation that will be cached</li>
</ul>
<p>Add test method for @CacheGet operation:</p>

<markup
lang="java"

>@Test
void testGet() {
    Message getResponse = target.path("/greet/John") <span class="conum" data-value="1" />
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .get(Message.class);
    final Message expected = new Message("Hello John");
    assertThat(getResponse, is(expected)); <span class="conum" data-value="2" />
    assertThat(GreetResource.GET_CALLS.get(), is(1)); <span class="conum" data-value="3" />
    assertThat(cache.get("John"), is(expected)); <span class="conum" data-value="4" />

    getResponse = target.path("/greet/John") <span class="conum" data-value="5" />
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .get(Message.class);
    assertThat(getResponse, is(expected));
    assertThat(GreetResource.GET_CALLS.get(), is(1));
}</markup>

<ul class="colist">
<li data-value="1">Invoke caching resource method</li>
<li data-value="2">Verify that response is the expected one</li>
<li data-value="3">Verify that target method was invoked</li>
<li data-value="4">Verify that response was cached</li>
<li data-value="5">Verify that repeated invocation of the caching resource method won&#8217;t result
in the method execution as result will be returned from the cache</li>
</ul>
</div>

<h4 id="_cacheadd">@CacheAdd</h4>
<div class="section">
<p>@CacheAdd always calls the target method and then caches the result.</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("greet/{name}")
@POST
@Produces(MediaType.APPLICATION_JSON)
@CacheAdd <span class="conum" data-value="1" />
public Message addMessage(@PathParam("name") String name) { <span class="conum" data-value="2" />
    ADD_CALLS.incrementAndGet(); <span class="conum" data-value="3" />
    return new Message("ADD executed"); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">We&#8217;ll test @CacheAdd annotation processing</li>
<li data-value="2">Cache key will be <code>name</code> argument</li>
<li data-value="3">We&#8217;ll count number of method invocations</li>
<li data-value="4">Result of the method invocation that will be cached</li>
</ul>
<p>Test method for @CacheAdd operation:</p>

<markup
lang="java"

>@Test
void testAdd() {
    Message getResponse = target.path("/greet/John") <span class="conum" data-value="1" />
            .request()
            .get(Message.class);
    final Message expectedGetResponse = new Message("Hello John");
    assertThat(getResponse, is(expectedGetResponse));
    assertThat(GreetResource.GET_CALLS.get(), is(1));
    assertThat(cache.get("John"), is(expectedGetResponse));

    final Message addedMessage = new Message("ADD executed");
    Message addResponse = target.path("/greet/John") <span class="conum" data-value="2" />
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .post(null, Message.class);
    assertThat(addResponse, is(addedMessage));
    assertThat(GreetResource.ADD_CALLS.get(), is(1));
    assertThat(cache.get("John"), is(addedMessage)); <span class="conum" data-value="3" />

    addResponse = target.path("/greet/John") <span class="conum" data-value="4" />
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .post(null, Message.class);
    assertThat(addResponse, is(addedMessage));
    assertThat(GreetResource.ADD_CALLS.get(), is(2)); <span class="conum" data-value="5" />
    assertThat(cache.get("John"), is(addedMessage));
}</markup>

<ul class="colist">
<li data-value="1">Populate cache by invoking caching resource method</li>
<li data-value="2">Invoke resource method annotated with @CacheAdd</li>
<li data-value="3">Verify that the target method was invoked and its returning value was cached</li>
<li data-value="4">Invoke @CacheAdd annotated method again</li>
<li data-value="5">Verify that the target method was executed once again</li>
</ul>
</div>

<h4 id="_cacheput">@CachePut</h4>
<div class="section">
<p>@CachePut stores the value annotated with @CacheValue in the cache and calls the target method</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("greet/{name}")
@PUT
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@CachePut <span class="conum" data-value="1" />
public Message putMessage(@CacheKey @PathParam("name") String name, @CacheValue Message message) { <span class="conum" data-value="2" />
    PUT_CALLS.incrementAndGet(); <span class="conum" data-value="3" />
    return new Message("PUT executed"); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">We&#8217;ll test @CachePut annotation processing</li>
<li data-value="2">Cache key will be <code>name</code> argument and <code>message</code> will be new cache value</li>
<li data-value="3">We&#8217;ll count number of method invocations</li>
<li data-value="4">Result of the method invocation that won&#8217;t be cached</li>
</ul>
<p>Test method for @CachePut operation:</p>

<markup
lang="java"

>@Test
void testPut() {
    final Message messageToCache = new Message("Hola");
    final Message expectedPutResponse = new Message("PUT executed");
    Message putResponse = target.path("/greet/John") <span class="conum" data-value="1" />
            .request() <span class="conum" data-value="1" />
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .put(Entity.entity(messageToCache, MediaType.APPLICATION_JSON_TYPE), Message.class);
    assertThat(putResponse, is(expectedPutResponse));
    assertThat(GreetResource.PUT_CALLS.get(), is(1));
    assertThat(cache.get("John"), is(messageToCache)); <span class="conum" data-value="2" />

    putResponse = target.path("/greet/John") <span class="conum" data-value="3" />
            .request() <span class="conum" data-value="3" />
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .put(Entity.entity(messageToCache, MediaType.APPLICATION_JSON_TYPE), Message.class);
    assertThat(putResponse, is(expectedPutResponse));
    assertThat(GreetResource.PUT_CALLS.get(), is(2)); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">Pass new cache value to the caching resource method</li>
<li data-value="2">Verify that passed value is stored in the cache</li>
<li data-value="3">Invoke the same @CachePut annotated method again</li>
<li data-value="4">Verify that the target method was executed once again</li>
</ul>
</div>

<h4 id="_cacheremove">@CacheRemove</h4>
<div class="section">
<p>@CacheRemove removes the key from the cache and calls the target method</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("greet/{name}")
@DELETE
@Produces(MediaType.APPLICATION_JSON)
@CacheRemove <span class="conum" data-value="1" />
public Message removeMessage(@PathParam("name") String name) { <span class="conum" data-value="2" />
    REMOVE_CALLS.incrementAndGet(); <span class="conum" data-value="3" />
    return new Message("Deleted cached value for " + name); <span class="conum" data-value="4" />
}</markup>

<ul class="colist">
<li data-value="1">We&#8217;ll test @CacheRemove annotation processing</li>
<li data-value="2">Cache key to remove from the cache</li>
<li data-value="3">We&#8217;ll count number of method invocations</li>
<li data-value="4">Result of the method invocation that will be returned</li>
</ul>
<p>Test method for @CacheRemove operation:</p>

<markup
lang="java"

>@Test
void testRemove() {
    final Message hola = new Message("Hola");
    Message putResponse = target.path("/greet/John") <span class="conum" data-value="1" />
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .put(Entity.entity(hola, MediaType.APPLICATION_JSON_TYPE), Message.class);
    assertThat(putResponse, is(new Message("PUT executed")));
    assertThat(GreetResource.PUT_CALLS.get(), is(1));
    assertThat(cache.get("John"), is(hola)); <span class="conum" data-value="2" />

    Message deleteResponse = target.path("/greet/John") <span class="conum" data-value="3" />
            .request()
            .delete(Message.class);
    assertThat(deleteResponse, is(new Message("Deleted cached value for John")));
    assertThat(GreetResource.REMOVE_CALLS.get(), is(1));
    assertThat(cache.get("John"), is(nullValue())); <span class="conum" data-value="4" />
    assertThat(cache.size(), is(0));
}</markup>

<ul class="colist">
<li data-value="1">Store initial value in the cache</li>
<li data-value="2">Verify that cache is populated</li>
<li data-value="3">Remove key from the cache by invoking resource method marked with @CacheRemove</li>
<li data-value="4">Verify that key was removed from the cache</li>
</ul>
</div>

<h4 id="_cachename">@CacheName</h4>
<div class="section">
<p>The @CacheName annotation defines the cache that will be used for response caching. If both the class and methods are annotated with @CacheName, the value from the method annotation takes precedence.</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("another")
@GET
@Produces(MediaType.APPLICATION_JSON)
@CacheGet
@CacheName("another-cache") <span class="conum" data-value="1" />
public Message getFromAnotherCache(@QueryParam("name") @CacheKey String name) {
    return new Message("Another " + name + "?");
}</markup>

<ul class="colist">
<li data-value="1">Specify cache name that will override cache name defined on a class (message-cache)</li>
</ul>
<p>Test method for @CacheName annotation:</p>

<markup
lang="java"

>@Test
void testCacheName() {
    Message anotherGetResponse = target.path("/another") <span class="conum" data-value="1" />
            .queryParam("name", "John")
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .get(Message.class);
    assertThat(anotherGetResponse, is(new Message("Another John?")));
    assertThat(cache.size(), is(0)); <span class="conum" data-value="2" />
    assertThat(anotherCache.size(), is(1));
    assertThat(anotherCache.get("John"), is(new Message("Another John?"))); <span class="conum" data-value="3" />
}</markup>

<ul class="colist">
<li data-value="1">Populate cache</li>
<li data-value="2">Verify that cache specified by class @CacheName is not populated</li>
<li data-value="3">Verify that cache specified by method @CacheName is populated</li>
</ul>
</div>

<h4 id="_multiple_arguments_as_a_cache_key">Multiple arguments as a cache key</h4>
<div class="section">
<p>Unless the @CacheKey annotation is applied to a parameter, all parameters except for one marked with @CacheValue
will be used as a part of the cache key.</p>

<p>Add resource method to the resource class:</p>

<markup
lang="java"

>@Path("parameters")
@GET
@Produces(MediaType.APPLICATION_JSON)
@CacheGet
public Message get(@QueryParam("firstName") String firstName, @QueryParam("lastName") String lastName) { <span class="conum" data-value="1" />
    MULTI_PARAM_CALLS.incrementAndGet();
    return new Message("Message for " + firstName + " " + lastName);
}</markup>

<ul class="colist">
<li data-value="1">Cache key will be assembled from both <code>firstName</code> and <code>lastName</code> arguments.</li>
</ul>
<p>Test method:</p>

<markup
lang="java"

>@Test
void testCacheName() {
    Message anotherGetResponse = target.path("/another") <span class="conum" data-value="1" />
            .queryParam("name", "John")
            .request()
            .acceptEncoding(MediaType.APPLICATION_JSON)
            .get(Message.class);
    assertThat(anotherGetResponse, is(new Message("Another John?")));
    assertThat(cache.size(), is(0)); <span class="conum" data-value="2" />
    assertThat(anotherCache.size(), is(1));
    assertThat(anotherCache.get("John"), is(new Message("Another John?"))); <span class="conum" data-value="3" />
}</markup>

<ul class="colist">
<li data-value="1">Store initial value in the cache</li>
<li data-value="2">Verify that cache is populated correctly</li>
<li data-value="3">Verify that value was fetched from the cache</li>
</ul>
</div>
</div>

<h3 id="_summary">Summary</h3>
<div class="section">
<p>You have seen how to use CDI Caching Response annotations.</p>

</div>
</div>
</doc-view>
