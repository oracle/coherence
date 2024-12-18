<doc-view>

<h2 id="_health_checks">Health Checks</h2>
<div class="section">
<p>Version 22.06 of Coherence introduced a Health Check API to provide simple checks for the overall health of a Coherence member. This guide shows some ways this API can be used.</p>


<h3 id="_what_you_will_build">What You Will Build</h3>
<div class="section">
<p>This guide will build simple examples showing different uses of the Health Check API from application code and in containerized environments.</p>

<ul class="ulist">
<li>
<p><router-link to="#basic" @click.native="this.scrollFix('#basic')">The Basic Health Check API</router-link> introduces the basic health check APIs.</p>

</li>
<li>
<p><router-link to="#application" @click.native="this.scrollFix('#application')">Application Health Checks</router-link> shows how to add custom application health checks</p>

</li>
<li>
<p><router-link to="#container" @click.native="this.scrollFix('#container')">Container Health Checks</router-link> shows how to add health checks to be used in containers</p>
<ul class="ulist">
<li>
<p><router-link to="#docker" @click.native="this.scrollFix('#docker')">Docker Image Health Checks</router-link> shows adding health checks to an image built with Docker</p>

</li>
<li>
<p><router-link to="#buildah" @click.native="this.scrollFix('#buildah')">Buildah Image Health Checks</router-link> shows adding health checks to an image built with Buildah</p>

</li>
</ul>
</li>
</ul>
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
<p><a id="" title="" target="_blank" href="http://www.oracle.com/technetwork/java/javase/downloads/index.html">JDK 17</a> or later</p>

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

</div>
</div>
</div>

<h2 id="basic">The Basic Health Check API</h2>
<div class="section">
<p>The basic health check API includes methods to check the local member&#8217;s health and obtain health check instances.
This API is demonstrated using a simple integration test in <code>src/test/java/com/oracle/coherence/guides/health/BasicHealthIT.java</code></p>

<p>The test first bootstraps a Coherence storage member using the Coherence bootstrap API.
The test will fail if Coherence takes longer than five minutes to start (it should be up in seconds).</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @BeforeAll
    static void startCoherence() throws Exception {
        Coherence.clusterMember()
                 .start()
                 .get(5, TimeUnit.MINUTES);
    }</markup>

<p>When the tests finish Coherence is shut down.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @AfterAll
    static void cleanup() {
        Coherence coherence = Coherence.getInstance();
        if (coherence != null) {
            coherence.close();
        }
    }</markup>


<h3 id="_check_all_health_checks_are_started">Check All Health Checks are Started</h3>
<div class="section">
<p>The first test in <code>BasicHealthIT</code> checks that everything is "started".
The <code>Coherence</code> instance is obtained (there is only one instance running in this case so the <code>Coherence.getInstance()</code> method can be used). From the <code>Coherence</code> instance the management <code>Registry</code> is obtained.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @Test
    void shouldEventuallyBeStarted() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksStarted, is(true));
    }</markup>

<p>The test then asserts that "eventually", the call to <code>registry.allHealthChecksStarted()</code> returns <code>true</code>, which it should as soon as all services are started. At this point Coherence may not be "ready" or "safe", but it is started.</p>

</div>

<h3 id="_check_all_health_checks_are_ready">Check All Health Checks are Ready</h3>
<div class="section">
<p>The second test in <code>BasicHealthIT</code> checks that everything is "ready".
The <code>Coherence</code> instance is obtained and from the <code>Coherence</code> instance the management <code>Registry</code> is obtained.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @Test
    void shouldEventuallyBeStarted() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksStarted, is(true));
    }</markup>

<p>The test then asserts that "eventually", the call to <code>registry.allHealthChecksReady()</code> returns <code>true</code>, which it should as soon as all services reach the "ready" state.</p>

</div>

<h3 id="_check_all_health_checks_are_safe">Check All Health Checks are Safe</h3>
<div class="section">
<p>The third test in <code>BasicHealthIT</code> checks that everything is "safe".
The <code>Coherence</code> instance is obtained and from the <code>Coherence</code> instance the management <code>Registry</code> is obtained.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @Test
    void shouldEventuallyBeStarted() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Eventually.assertDeferred(registry::allHealthChecksStarted, is(true));
    }</markup>

<p>The test then asserts that "eventually", the call to <code>registry.allHealthChecksSafe()</code> returns <code>true</code>, which it should as soon as all services reach the "safe" state.</p>

</div>

<h3 id="_get_health_check_instances">Get Health Check Instances</h3>
<div class="section">
<p>The <code>Registry</code> health check API has methods to obtain instances of the health checks that have been registered on the local member.</p>


<h4 id="_gat_all_health_checks">Gat All Health Checks</h4>
<div class="section">
<p>A <code>Collection</code> of health checks can be obtained using the <code>Registry</code> instances <code>getHealthChecks()</code> method.</p>

<p>The example below shows a simple test case that obtains all the registered health checks.
There is an assertion that the collection returned is not empty.
As the test uses the default Coherence cache configuration file, this will start a distributed cache service named <code>PartitionedCache</code>, so there will be a health check registered with this name.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @Test
    void shouldGetHealthChecks() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Collection&lt;HealthCheck&gt; healthChecks = registry.getHealthChecks();

        assertThat(healthChecks.isEmpty(), is(false));

        HealthCheck healthCheck = healthChecks.stream()
                                              .filter(h-&gt;"PartitionedCache".equals(h.getName()))
                                              .findFirst()
                                              .orElse(null);

        assertThat(healthCheck, is(notNullValue()));
    }</markup>

</div>

<h4 id="_get_a_health_check_by_name">Get a Health Check by Name</h4>
<div class="section">
<p>Instead of getting the collection of all health checks, a single health check can be obtained by using its name.</p>

<p>The <code>Registry</code> instances <code>getHealthCheck(String name)</code> method can be used to obtain a health check instance by name. The method returns an <code>Optional</code> that will be empty if there is no health check registered with the specified name.</p>

<p>The example below obtains the health check named <code>PartitionedCache</code>, which should exist as the test uses the default Coherence cache configuration file.</p>

<markup
lang="java"
title="BasicHealthIT.java"
>    @Test
    void shouldGetHealthCheckByName() {
        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        Optional&lt;HealthCheck&gt; optional = registry.getHealthCheck("PartitionedCache");

        assertThat(optional.isPresent(), is(true));
    }</markup>

</div>
</div>
</div>

<h2 id="application">Application Health Checks</h2>
<div class="section">
<p>Applications can add custom health checks by creating a class that implements the <code>com.tangosol.util.HealthCheck</code> interface, and registering the health check with the <code>Registry</code>.</p>

<p>The example <code>ApplicationHealth</code> class below implements the <code>HealthCheck</code> interface.
The <code>getName()</code> method returns <code>"Demo"</code>, which is a unique name for this health check.
In this example, the class does not have any processing in the health check methods.
In a real application health check these methods would perform custom application specific checks.</p>

<markup
lang="java"
title="ApplicationHealth.java"
>/**
 * A simple custom health check.
 */
public class ApplicationHealth
        implements HealthCheck {

    /**
     * The health check name.
     */
    public static final String NAME = "Demo";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean isLive() {
        return true;
    }

    @Override
    public boolean isStarted() {
        return true;
    }

    @Override
    public boolean isSafe() {
        return true;
    }
}</markup>

<p>The health check can be registered in application code using the <code>Registry.register(HealthCheck hc)</code> method.</p>

<markup
lang="java"

>        Coherence coherence = Coherence.getInstance();
        Registry  registry  = coherence.getManagement();

        ApplicationHealth healthCheck = new ApplicationHealth();

        registry.register(healthCheck);

        Optional&lt;HealthCheck&gt; optional = registry.getHealthCheck(ApplicationHealth.NAME);
        assertThat(optional.isPresent(), is(true));</markup>

<p>When no longer required, the health check can be unregistered using the <code>Registry.unregister(String name)</code> method.</p>

<markup
lang="java"

>        registry.unregister(healthCheck);
        optional = registry.getHealthCheck(ApplicationHealth.NAME);
        assertThat(optional.isPresent(), is(false));</markup>


<h3 id="_health_check_auto_registration">Health Check Auto-Registration</h3>
<div class="section">
<p>Application health checks can be automatically registered by Coherence during start-up.
When Coherence starts, it will use the Java <code>ServiceLoader</code> to discover any <code>HealthCheck</code> implementations, and
automatically register them.</p>

<p>To automatically register the example <code>ApplicationHealth</code> class above, create
a <code>META-INF/service/com.tangosol.util.HealthCheck</code> file, containing a single line that is the name of the application health check.</p>

<markup

title="META-INF/service/com.tangosol.util.HealthCheck"
>com.oracle.coherence.guides.health.ApplicationHealth;</markup>

<p>Alternatively, if using a <code>module-info.java</code> file add the health check using the <code>provides</code> clause.</p>

<markup
lang="java"
title="module-info.java"
>module coherence.guides.health {
  provides com.tangosol.util.HealthCheck
      with com.oracle.coherence.guides.health.ApplicationHealth;
}</markup>

<p>When Coherence starts, it will use <code>ServiceLoader</code> to load <code>HealthCheck</code> instances, which will discover and load an <code>ApplicationHealth</code> instance.</p>

</div>
</div>

<h2 id="container">Container Health Checks</h2>
<div class="section">
<p>Health checks are extremely useful when running Coherence in containers, as they can signal to the container management system (e.g. Docker, or Kubernetes) that the Coherence container is running and healthy.</p>

<p>The OCI specification allows an image to define a command to run to check its health.
This is supported by image build tools such as Docker and Buildah.</p>


<h3 id="docker">Docker Image Health Checks</h3>
<div class="section">
<p>To use health checks in Docker, the <a id="" title="" target="_blank" href="https://docs.docker.com/engine/reference/builder/#healthcheck"><code>HEALTHCHECK</code></a> instruction can be used in the <code>Dockerfile</code>.</p>

<p>The format of the <code>HEALTHCHECK</code> instruction is shown below:</p>

<markup


>HEALTHCHECK [OPTIONS] CMD command</markup>

<p>The <code>command</code> is typically a simple command line, such as <code>curl</code> or a shell script, or Java command line.
For example, if the Coherence health check endpoint is enabled on a fixed port <code>6676</code>, then the <code>HEALTHCHECK</code> instruction&#8217;s <code>CMD</code> can be set to <code>curl -f <a id="" title="" target="_blank" href="http://127.0.0.1:6676/ready">http://127.0.0.1:6676/ready</a></code></p>

<p>An example of a simple Coherence <code>Dockerfile</code> with a health check is shown below.
This example image uses OpenJDK as a base image. Coherence jar is added to the image and the health check port fixed to <code>6676</code> using the <code>COHERENCE_HEALTH_HTTP_PORT</code> environment variable.
When the image runs, the entry point will just start <code>Coherence</code>.</p>

<markup

title="src/docker/OpenJDK.Dockerfile"
># Copyright (c) 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
FROM openjdk:11-jre

ADD coherence.jar /coherence/lib/coherence.jar

ENV COHERENCE_HEALTH_HTTP_PORT=6676

HEALTHCHECK  --start-period=30s --interval=30s \
        CMD curl -f http://127.0.0.1:6676/ready || exit 1

ENTRYPOINT ["java"]
CMD ["-cp", "/coherence/lib/*", "com.tangosol.net.Coherence"]</markup>


<h4 id="_build_and_run_the_image">Build and Run the Image</h4>
<div class="section">
<p>The example above can be built and tested using a simple Maven command.
The command will run a Maven build with the <code>docker</code> profile enabled, which will use Docker to build an image.
The name of the image is configured in the properties' section of the example <code>pom.xml</code> to be <code>coherence-health:1.0.0</code>.</p>

<markup
lang="bash"

>mvn clean package -DskipTests -Pdocker</markup>

<p>A container can then be run using the image.
The normal <code>docker run</code> command is used, in this case the container is given the name <code>test</code>.</p>

<markup
lang="bash"

>docker run -d --name test coherence-health:1.0.0</markup>

<p>After starting the container, the set of running containers can be listed using <code>docker ps</code>, which should display the <code>test</code> container:</p>

<markup
lang="bash"

>CONTAINER ID  IMAGE                   COMMAND                  CREATED        STATUS                           PORTS   NAMES
520559d772e3  coherence-health:1.0.0  "java -cp /coherence…"   3 seconds ago  Up 2 seconds (health: starting)          test</markup>

<p>Because the image has a health check configured the status in this case included the current health state <code>Up 2 seconds (health: starting)</code>.
At this point the container is still starting, so the Coherence health endpoint (<code><a id="" title="" target="_blank" href="http://127.0.0.1:6676/ready">http://127.0.0.1:6676/ready</a></code>) has not returned a 200 response, as Coherence is still starting. Once Coherence has started and te health check reports ready, the container status will change to <code>healthy</code>.</p>

<markup
lang="bash"

>CONTAINER ID  IMAGE                   COMMAND                  CREATED        STATUS                  PORTS   NAMES
520559d772e3  coherence-health:1.0.0  "java -cp /coherence…"   3 seconds ago  Up 4 minutes (healthy)          test</markup>

</div>
</div>

<h3 id="buildah">Buildah Image Health Checks</h3>
<div class="section">
<p>The Podman and Buildah tools are common replacements for Docker when running in Linux.
When using Buildah to create images, health checks can be added to an image using the Buildah CLI.
To support health checks Buildah must be configured to use "Docker" format. The simplest way to do this is to export
the <code>BUILDAH_FORMAT</code> environment variable</p>

<markup
lang="bash"

>export BUILDAH_FORMAT=docker</markup>

<p>Now, the Buildah CLI can be used to create an image.</p>

<markup
lang="bash"

>buildah from --name coherence openjdk:11-jre

buildah copy coherence coherence.jar  /coherence/lib/coherence.jar

buildah config --healthcheck-start-period 10s --healthcheck-interval 10s \
    --healthcheck "CMD curl -f http://127.0.0.1:6676/ready || exit 1" coherence

buildah config \
    --entrypoint '["java"]' --cmd '-cp /coherence/lib/* com.tangosol.net.Coherence' \
    -e COHERENCE_HEALTH_HTTP_PORT=6676 \
    coherence

buildah commit coherence coherence-health:1.0.0

buildah push -f v2s2 coherence-health:1.0.0 docker-daemon:coherence-health:1.0.0</markup>

<p>The Buildah commands above build the same image that was built with the Dockerfile in the previous section.
The final command above pushes the <code>coherence-health:1.0.0</code> image built by Buildah into a Docker daemon, so it can be run using Docker.</p>

</div>

<h3 id="_health_checks_in_distroless_base_images">Health Checks in Distroless Base Images</h3>
<div class="section">
<p>Sometimes a distroless image is used as a base image for applications.
These are images that do not contain a Linux distribution.
There are various reasons for this such as image size, but mainly security, as the base image does not contain a lot of
Linux utilities that may introduce CVEs. The example Coherence images use distroless base images.</p>

<p>When using a distroless base image, the <code>curl</code> utility is not present, so it cannot be used as the health check command.
In the distroless base images used by Coherence, all that is present is a Linux kernel and Java.
This means that the only way to run any health check commands would be to execute a Java command.</p>

<p>As part of the Coherence health check API there is a simple http client class <code>com.tangosol.util.HealthCheckClient</code>
that can be used to execute a health check as a Java command.</p>

<p>The Java command line to execute a health check would be:</p>

<markup
lang="bash"

>java -cp coherence.jar com.tangosol.util.HealthCheckClient http://127.0.0.1:6676/ready</markup>

<p>This <code>Distroless.Dockerfile</code> in the source code contains an example of using a Java health check command.
Because the health check command is running Java and not a simple O/S command, the format of the <code>CMD</code> parameters
is slightly different than the previous example.</p>

<markup

title="src/docker/Distroless.Dockerfile"
># Copyright (c) 2022, Oracle and/or its affiliates.
#
# Licensed under the Universal Permissive License v 1.0 as shown at
# https://oss.oracle.com/licenses/upl.
#
FROM gcr.io/distroless/java11-debian11

ADD coherence.jar /coherence/lib/coherence.jar

ENV COHERENCE_HEALTH_HTTP_PORT=6676

HEALTHCHECK  --start-period=30s --interval=30s \
    CMD ["java",
    "-cp", "/coherence/lib/coherence.jar",
    "com.tangosol.util.HealthCheckClient",
    "http://127.0.0.1:6676/ready",
    "||", "exit", "1"]

ENTRYPOINT ["java"]
CMD ["-cp", "/coherence/lib/*", "com.tangosol.net.Coherence"]</markup>

<p>The example distroless image can be built using Maven, as before but specifying the distroless Dockerfile.</p>

<markup
lang="bash"

>mvn clean package -DskipTests -Pdocker -Ddocker.file=Distroless.Dockerfile</markup>

<p>The Maven command builds the same test image with the tag <code>coherence-health:1.0.0</code>
which can be run in the same way as the previous examples.</p>

</div>
</div>
</doc-view>
