<doc-view>

<h2 id="_cdi_response_caching">CDI Response Caching</h2>
<div class="section">
<p>CDI Response Caching allows you to apply caching to Java methods transparently. CDI Response Caching will be enabled once coherence-cdi dependency is added.</p>


<h3 id="_usage">Usage</h3>
<div class="section">
<p>To use CDI Response Caching, you should first declare a coherence-cdi as a dependency in the project&#8217;s pom.xml file.</p>

<markup
lang="xml"
title="pom.xml"
>&lt;dependency&gt;
    &lt;groupId&gt;${coherence.groupId}&lt;/groupId&gt;
    &lt;artifactId&gt;coherence-cdi&lt;/artifactId&gt;
    &lt;version&gt;${coherence.version}&lt;/version&gt;
&lt;/dependency&gt;</markup>

</div>

<h3 id="_response_caching_annotations">Response Caching Annotations</h3>
<div class="section">
<p>The following response caching annotations are supported:</p>


<p>The specific cache to be used for response caching can be declared by the @CacheName and @SessionName annotations on a class or method.</p>

<v-divider class="my-5"/>

<h4 id="CacheAdd">@CacheAdd</h4>
<div class="section">
<p>Method marked with @CacheAdd is <strong>always</strong> invoked, and its execution result stored in the cache. Key is made of the values of all parameters (in this case just the string parameter <code>name</code>).</p>

<markup
lang="java"

>    @Path("{name}")
    @POST
    @CacheAdd
    @CacheName("messages")
    public Message addMessage(@PathParam("name") String name)
        {
        return new Message("Hello " + name);
        }</markup>

</div>

<h4 id="CacheGet">@CacheGet</h4>
<div class="section">
<p>If the return value is present in the cache, it is fetched and returned. Otherwise, the target method is invoked, and the invocation result is stored in the cache and returned to the caller.</p>

<markup
lang="java"

>    @Path("{name}")
    @GET
    @CacheGet
    @CacheName("messages")
    public Message getMessage(@PathParam("name") String name)
        {
        return new Message("Hello " + name);
        }</markup>

</div>

<h4 id="CachePut">@CachePut</h4>
<div class="section">
<p id="CacheValue">The value of the @CacheValue annotated parameter is stored in the cache, the target method is invoked, and the invocation result is returned to the caller.</p>

<p>In this example, the passed message will be stored in the cache for the key whose value was passed as the <code>name</code> parameter.</p>

<markup
lang="java"

>    @Path("{name}")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @CachePut
    @CacheName("messages")
    public Response putMessage(@CacheKey @PathParam("name") String name,
                               @CacheValue Message message)
        {
        return Response.status(Response.Status.CREATED).build();
        }</markup>

</div>

<h4 id="CacheRemove">@CacheRemove</h4>
<div class="section">
<p>Removes the key from the cache and returns the result of the method invocation.</p>

<p>In this example, the key whose value was passed as the <code>name</code> parameter will be removed from the cache.</p>

<markup
lang="java"

>    @Path("{name}")
    @DELETE
    @CacheRemove
    public Response removeMessage(@PathParam("name") String name)
        {
        return Response.ok().build();
        }</markup>

</div>

<h4 id="CacheKey">@CacheKey</h4>
<div class="section">
<p>The cache key is assembled from the values of all parameters not explicitly annotated with the @CacheValue annotation. If one or more parameters are annotated with the @CacheKey annotation, only those parameters will be used to create the key.</p>

<p>In this example, only the values of the <code>lastName</code> and <code>firstName</code> parameters will be used to create the cache key.</p>

<markup
lang="java"

>    @Path("{lastName}/{firstName}")
    @GET
    @CacheGet
    public Message getMessage(@PathParam("lastName") @CacheKey String lastName,
                              @PathParam("firstName") @CacheKey String firstName,
                              @HeaderParam("Accept-Language") String acceptLanguage)
        {
        return new Message("Hello " + firstName + " " + lastName);
        }</markup>

</div>
</div>
</div>
</doc-view>
