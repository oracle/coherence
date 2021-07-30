<doc-view>

<h2 id="_topics">Topics</h2>
<div class="section">
<p>This tutorial walks through the steps to use Coherence Topics using a simple Chat Application.</p>


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
<p><router-link to="#review-the-initial-project" @click.native="this.scrollFix('#review-the-initial-project')">Review the Initial Project</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#maven" @click.native="this.scrollFix('#maven')">Maven Configuration</router-link></p>

</li>
<li>
<p><router-link to="#data-model" @click.native="this.scrollFix('#data-model')">Data Model</router-link></p>

</li>
<li>
<p><router-link to="#cache-config" @click.native="this.scrollFix('#cache-config')">Topics Cache Configuration</router-link></p>

</li>
<li>
<p><router-link to="#chat-application" @click.native="this.scrollFix('#chat-application')">The Chat Application</router-link></p>

</li>
</ul>
</li>
<li>
<p><router-link to="#run-the-example" @click.native="this.scrollFix('#run-the-example')">Build and Run the Example</router-link></p>

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
<p>You will review, build and run a simple chat client which showcases using Coherence Topics.
When running the chat client, the user can send a message in two ways:</p>

<ol style="margin-left: 15px;">
<li>
Send to all connected users using a publish/ subscribe model. For this functionality we
create a topic called <code>public-messages</code> and all users are anonymous subscribers. Any messages to
this topic will only be received by subscribers that are active.

</li>
<li>
Send a private message to an individual user using a subscriber group. This uses a separate
topic called <code>private-messages</code> and each subscriber to the topic specifies their userId as a
subscriber group. Each value is only delivered to one of its subscriber group members, meaning the message will
only be received by the individual user.

</li>
</ol>
<div class="admonition note">
<p class="admonition-inline">We do not cover all features in Coherence Topics, so if you wish to read more about Coherence Topics, please see the
<a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-topics.html">Coherence Documentation</a>.</p>
</div>
</div>

<h3 id="what-you-need">What You Need</h3>
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

</div>
</div>

<h3 id="review-the-initial-project">Review the Initial Project</h3>
<div class="section">

<h4 id="maven">Maven Configuration</h4>
<div class="section">
<p>The initial project is a Coherence project and imports the <code>coherence-bom</code> and <code>coherence-dependencies</code>
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
  &lt;/dependencies&gt;
&lt;/dependencyManagement&gt;</markup>

<p>The <code>coherence</code> library is also included:</p>

<markup
lang="xml"

>&lt;dependency&gt;
  &lt;groupId&gt;${coherence.group.id}&lt;/groupId&gt;
  &lt;artifactId&gt;coherence&lt;/artifactId&gt;
&lt;/dependency&gt;</markup>

<p>We also define a <code>server</code> profile to run one or more DefaultCacheServer processes.</p>

<markup
lang="xml"

>&lt;profile&gt;
  &lt;id&gt;server&lt;/id&gt;
  &lt;activation&gt;
    &lt;property&gt;
      &lt;name&gt;server&lt;/name&gt;
    &lt;/property&gt;
  &lt;/activation&gt;
  &lt;build&gt;
    &lt;plugins&gt;
      &lt;plugin&gt;
        &lt;groupId&gt;org.codehaus.mojo&lt;/groupId&gt;
        &lt;artifactId&gt;exec-maven-plugin&lt;/artifactId&gt;
        &lt;version&gt;${maven.exec.plugin.version}&lt;/version&gt;
        &lt;configuration&gt;
          &lt;executable&gt;java&lt;/executable&gt;
          &lt;arguments&gt;
            &lt;argument&gt;-classpath&lt;/argument&gt;
            &lt;classpath/&gt;
            &lt;argument&gt;${coherence.common.properties}&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;-Xmx512m&lt;/argument&gt;
            &lt;argument&gt;-Xms512m&lt;/argument&gt;
            &lt;argument&gt;-Dcoherence.log.level=3&lt;/argument&gt;
            &lt;argument&gt;com.tangosol.net.DefaultCacheServer&lt;/argument&gt;
          &lt;/arguments&gt;
        &lt;/configuration&gt;
      &lt;/plugin&gt;
    &lt;/plugins&gt;
  &lt;/build&gt;
&lt;/profile&gt;</markup>

</div>

<h4 id="data-model">Data Model</h4>
<div class="section">
<p>The data model consists of the <code>ChatMessage</code> client which contains chat messages sent either on the private or publish topics. The properties are shown below:</p>

<markup
lang="java"

>/**
 * Date the message was sent.
 */
private final long date;

/**
 * The user who sent the message.
 */
private final String fromUserId;

/**
 * The recipient of the message or null if public message.
 */
private final String toUserId;

/**
 * The type of message.
 */
private final Type type;

/**
 * The contents of the message.
 */
private final String message;</markup>

</div>

<h4 id="cache-config">Topics Cache Configuration</h4>
<div class="section">
<ol style="margin-left: 15px;">
<li>
The following <code>topic-scheme-mapping</code> element is defined in <code>src/main/resources/topics-cache-config.xml</code>:
<markup
lang="xml"

>&lt;topic-mapping&gt;
  &lt;topic-name&gt;public-messages&lt;/topic-name&gt;
  &lt;scheme-name&gt;topic-server&lt;/scheme-name&gt;
  &lt;value-type&gt;com.oracle.coherence.guides.topics.ChatMessage&lt;/value-type&gt;
&lt;/topic-mapping&gt;
&lt;topic-mapping&gt;
  &lt;topic-name&gt;private-messages&lt;/topic-name&gt;
  &lt;scheme-name&gt;topic-server&lt;/scheme-name&gt;
  &lt;value-type&gt;com.oracle.coherence.guides.topics.ChatMessage&lt;/value-type&gt;
  &lt;subscriber-groups&gt;
    &lt;subscriber-group&gt;
      &lt;name&gt;admin&lt;/name&gt;
    &lt;/subscriber-group&gt;
  &lt;/subscriber-groups&gt;
&lt;/topic-mapping&gt;</markup>

<p>The topics defined are described below:</p>

<ul class="ulist">
<li>
<p><strong>public-messages</strong> - contains public messages</p>

</li>
<li>
<p><strong>private-messages</strong> - contains private messages and contains an initial subscriber group named <code>admin</code> in configuration.</p>
<div class="admonition note">
<p class="admonition-inline">Because we have specifically add the <code>admin</code> subscriber group in the cache config, this means that
it will be created on startup of the cache server and messages to <code>admin</code> will be durable. Messages
for subscriber groups created on the fly, by specifying <code>Name.of("groupName")</code> when creating a subscriber,
are only durable from the time the subscribe group is created.</p>
</div>
</li>
</ul>
</li>
<li>
The following <code>caching-schemes</code> element is defined in <code>src/main/resources/topics-cache-config.xml</code>:
<markup
lang="xml"

>&lt;!-- partitioned topic scheme for servers --&gt;
&lt;paged-topic-scheme&gt;
  &lt;scheme-name&gt;topic-server&lt;/scheme-name&gt;
  &lt;service-name&gt;${coherence.service.name Partitioned}Topic&lt;/service-name&gt;
  &lt;local-storage system-property="coherence.distributed.localstorage"&gt;true&lt;/local-storage&gt;
  &lt;autostart system-property="coherence.topic.enabled"&gt;true&lt;/autostart&gt;
  &lt;high-units&gt;{topic-high-units-bytes 0B}&lt;/high-units&gt;
&lt;/paged-topic-scheme&gt;</markup>

<p>The above <code>paged-topic-scheme</code> has no size limit and is automatically started.</p>

</li>
</ol>
</div>

<h4 id="chat-application">The Chat Application</h4>
<div class="section">
<p>The chat application is a simple text based client which does the following:</p>

<ul class="ulist">
<li>
<p>Starts up with an argument specifying the user id of the user</p>

</li>
<li>
<p>Displays a menu, shown below, where a user can send a message to all connected users or privately to an individual.</p>
<markup
lang="bash"

>Commands:
quit - Quit the chat
help - Display help
send - Send public message
sendpm userId - Send private message</markup>

</li>
</ul>
<p>We will examine each of the components in detail below:</p>

<ol style="margin-left: 15px;">
<li>
Topics, Subscribers and Publishers
<markup
lang="java"

>/**
 * Publisher for public messages.
 */
private final Publisher&lt;ChatMessage&gt; publisherPublic;

/**
 * Publisher for private messages.
 */
private final Publisher&lt;ChatMessage&gt; publisherPrivate;

/**
 * Subscriber for public messages.
 */
private final Subscriber&lt;ChatMessage&gt; subscriberPublic;

/**
 * Subscriber for private messages.
 */
private final Subscriber&lt;ChatMessage&gt; subscriberPrivate;</markup>

</li>
<li>
System Properties
<p>As we are creating a shaded Jar, we are including the following system properties to set the cache configuration file, turn off local storage and reduce the log level.</p>

<markup
lang="java"

>System.setProperty("coherence.distributed.localstorage", "false");
System.setProperty("coherence.log.level", "2");</markup>

</li>
<li>
Obtain a Coherence session
<markup
lang="java"

>Coherence coherence = Coherence.getInstance();
if (coherence == null) {
    Coherence.clusterMember().start().join();
    coherence = Coherence.getInstance();
}
Session session = coherence.getSession();</markup>

</li>
<li>
Create the <strong>public</strong> Topic, Subscribers and Publishers
<markup
lang="java"

>// create a publisher to publish public messages
publisherPublic = session.createPublisher("public-messages");  <span class="conum" data-value="1" />
// create a subscriber to receive public messages
subscriberPublic = session.createSubscriber("public-messages");  <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">Creates a publisher to publish messages to the topic</li>
<li data-value="2">Creates a subscriber (anonymous) to receive all messages published to the topic</li>
</ul>
</li>
<li>
Create the <strong>private</strong> Topic, Subscribers and Publishers
<markup
lang="java"

>// create a publisher to publish private messages
publisherPrivate = session.createPublisher("private-messages");  <span class="conum" data-value="1" />
// create a subscriber to receive private messages
subscriberPrivate = session.createSubscriber("private-messages", inGroup(userId));  <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">Creates a publisher to publish messages to the topic</li>
<li data-value="2">Creates a subscriber with a subscriber group of the user to receive private messages</li>
</ul>
</li>
<li>
When the application starts, two subscriptions are initiated. One to receive messages from the public topic and one
to receive messages from the private topic.
<markup
lang="java"

>// subscription for anonymous subscriber/ public messages
subscriberPublic.receive().handle((v, err) -&gt; receive(v, err, subscriberPublic));

// subscription for subscriber group / private durable messages
subscriberPrivate.receive().handle((v, err) -&gt; receive(v, err, subscriberPrivate));</markup>

<div class="admonition note">
<p class="admonition-inline">We are just using the default ForkJoin pool for this example but handleAsync can accept and <code>Executor</code> which would be better practice.</p>
</div>
</li>
<li>
Each of the above subscribers call the <code>receive</code> message which will resubscribe.
<markup
lang="java"

>/**
 * Receive a message from a given {@link Subscriber} and once processed, re-subscribe.
 * @param element    {@link Element} received
 * @param throwable  {@link Throwable} if any errors
 * @param subscriber {@link Subscriber} to re-subscribe to
 * @return void
 */
public Void receive(Element&lt;ChatMessage&gt; element, Throwable throwable, Subscriber&lt;ChatMessage&gt; subscriber) {
    if (throwable != null) {
        if (throwable instanceof CancellationException) {
            // exiting process, ignore.
        } else {
            log(throwable.getMessage());
        }
    } else {
        ChatMessage chatMessage = element.getValue();  <span class="conum" data-value="1" />
        getMessageLog(chatMessage)                     <span class="conum" data-value="2" />
            .ifPresent(message -&gt; {
                messagesReceived.incrementAndGet();
                log(message);
            });
        element.commit();   <span class="conum" data-value="3" />
        subscriber.receive().handle((v, err) -&gt; receive(v, err, subscriber));  <span class="conum" data-value="4" />
    }
    return null;
}</markup>

<ul class="colist">
<li data-value="1">Retrieve the <code>ChatMessage</code></li>
<li data-value="2">Call a method to generate a string representation of the message and display it</li>
<li data-value="3">Commit the element so that we do not receive the message again</li>
<li data-value="4">Receive the next message</li>
</ul>
</li>
<li>
Generate a join message on startup
<markup
lang="java"

>// generate a join message and send synchronously
publisherPublic.publish(new ChatMessage(userId, null, ChatMessage.Type.JOIN, null)).join();</markup>

</li>
<li>
Send a public message when the user uses the <code>sendpm</code> command:
<markup
lang="java"

>} else if (line.startsWith("send ")) {
    // send public message synchronously
    publisherPublic.publish(new ChatMessage(userId, null, ChatMessage.Type.MESSAGE, line.substring(5)))
            .handle(this::handleSend);  <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">Asynchronously send the message and increment the messages sent when complete</li>
</ul>
</li>
<li>
Send a private message when the user uses the <code>sendpm</code> command:
<markup
lang="java"

>} else if (line.startsWith("sendpm ")) {
    // send private durable message
    String[] parts = line.split(" ");
    // extract the target user and message
    if (parts.length &lt; 3) {
        log("Usage: sendpm user message");
    } else {
        String user = parts[1];
        String message = line.replaceAll(parts[0] + " " + parts[1] + " ", "");
        publisherPrivate.publish(new ChatMessage(userId, user, ChatMessage.Type.MESSAGE, message))
                .handle(this::handleSend); <span class="conum" data-value="1" /></markup>

<ul class="colist">
<li data-value="1">Asynchronously send the message and increment the messages sent when complete</li>
</ul>
</li>
<li>
Generate a leave message on exit and cleanup
<markup
lang="java"

>private void cleanup() {
    // generate a leave message
    if (publisherPublic.isActive()) {
        publisherPublic.publish(new ChatMessage(userId, null, ChatMessage.Type.LEAVE, null)).join();
        publisherPublic.flush().join();
        publisherPublic.close();
    }
    if (subscriberPublic.isActive()) {
        subscriberPublic.close();
    }

    if (publisherPrivate.isActive()) {
        publisherPrivate.flush().join();
        publisherPrivate.close();
    }
    if (subscriberPrivate.isActive()) {
        subscriberPrivate.close();
    }
}</markup>

</li>
</ol>
</div>
</div>

<h3 id="run-the-example">Build and Run the Example</h3>
<div class="section">
<p>Build the project using either of the following:</p>

<markup
lang="bash"

>./mvnw clean package</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew clean build</markup>

<ol style="margin-left: 15px;">
<li>
Start one or more Coherence Cache Servers using the following:
<markup
lang="bash"

>./mvnw exec:exec -P server</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runServer</markup>

</li>
<li>
Start the first chat client with the user <code>Tim</code>
<markup
lang="bash"

>java -jar target/topics-1.0.0-SNAPSHOT.jar Tim</markup>

<p>or</p>

<markup
lang="bash"

>./gradlew runClient -PuserId=Tim --console=plain</markup>

<p>You will notice output similar to the following:</p>

<markup
lang="bash"

>Oracle Coherence Version 20.12 Build demo
 Grid Edition: Development mode
Copyright (c) 2000, 2021, Oracle and/or its affiliates. All rights reserved.

User: Tim
Commands:
quit - Quit the chat
help - Display help
send - Send public message
sendpm userId - Send private message

Chat (Tim)&gt;</markup>

</li>
<li>
Start a second second client with the name <code>Helen</code>. You will see a message on <code>Tim&#8217;s</code> chat application indicating <code>Helen</code> has joined the chat.
<markup
lang="bash"

>Chat (Tim)&gt; 14:14:30 Helen joined the chat</markup>

</li>
<li>
Use <code>send hello</code> from <code>Helen&#8217;s</code> chat and you will notice that the message is dispalyed on <code>Tim&#8217;s</code> chat.

</li>
<li>
To show how subscriber groups work, send a private message using the following from <code>Tim</code> to <code>JK</code>.
<markup
lang="bash"

>Chat (Tim)&gt; sendpm JK Hello JK</markup>

<p>Also send a private message to <code>admin</code>.</p>

<markup
lang="bash"

>Chat (Tim)&gt; sendpm admin Please ping me when you get in as i have an issue with my Laptop</markup>

</li>
<li>
Start a third chat application with <code>JK</code> as the user:
<markup
lang="bash"

>java -jar target/topics-1.0.0-SNAPSHOT.jar JK

User: JK
Commands:
quit - Quit the chat
help - Display help
send - Send public message
sendpm userId - Send private message

Chat (JK)&gt;</markup>

<div class="admonition note">
<p class="admonition-inline">You will notice that the private message for <code>JK</code> was not delivered as the subscriber group <code>JK</code> was
only created when he joined and therefore messages send previously are not stored.</p>
</div>
<div class="admonition note">
<p class="admonition-inline">You will also see join messages on the other terminals.</p>
</div>
</li>
<li>
Type <code>quit</code> in <code>Helen&#8217;s</code> terminal and restart the client as <code>admin</code>
<markup
lang="bash"

>java -jar target/topics-1.0.0-SNAPSHOT.jar admin

User: admin
Commands:
quit - Quit the chat
help - Display help
send - Send public message
sendpm userId - Send private message

Chat (admin)&gt; 14:18:29 Tim (Private) - Please ping me when you get in as i have an issue with my Laptop</markup>

<div class="admonition note">
<p class="admonition-inline">You will notice that the message sent before <code>admin</code> joined is now delivered as the <code>admin</code> subscriber group was
created in configuration and add on server startup.</p>
</div>
</li>
<li>
Type a message <code>send Got to go, bye</code> on <code>JK&#8217;s</code> chat application and then <code>quit</code>. The message
along with the leave notification will be shown on the other terminals.
<markup
lang="bash"

>Chat (JK)&gt; send Got to go, bye</markup>

</li>
<li>
Now that <code>JK</code> has quit the application, send a private message from <code>Tim</code> to <code>JK</code> using <code>sendpm JK please ping me</code>.
<markup
lang="bash"

>Chat (Tim)&gt; sendpm JK please ping me</markup>

</li>
<li>
Start the client as <code>JK</code> and you will see the message displayed now as the subscriber group is created.

</li>
<li>
Finally send a private messge from <code>Tim</code> to <code>admin</code> using <code>sendpm admin Are you free for lunch?</code>.
You will notice this message is only displayed for <code>admin</code>.
<markup
lang="bash"

>Chat (Tim)&gt; sendpm admin Are you free for lunch?</markup>

</li>
</ol>
</div>

<h3 id="summary">Summary</h3>
<div class="section">
<p>In this tutorial you have learned how use Coherence Topics.</p>

</div>

<h3 id="see-also">See Also</h3>
<div class="section">
<ul class="ulist">
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/using-topics.html">Topics Overview and Configuration</a></p>

</li>
<li>
<p><a id="" title="" target="_blank" href="https://docs.oracle.com/en/middleware/standalone/coherence/14.1.1.0/develop-applications/performing-basic-topic-publish-and-subscribe-operations.html#GUID-46CCE404-89D7-4396-854A-AF05227A04D6">Performing Topics Operations</a></p>

</li>
</ul>
</div>
</div>
</doc-view>
