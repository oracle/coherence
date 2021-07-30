<doc-view>

<h2 id="_subscribers">Subscribers</h2>
<div class="section">
<p>Subscribers are used to receive messages to a Coherence topic, a subscriber receives messages from a single topic.</p>

<ul class="ulist">
<li>
<p><router-link to="#_creating_subscribers" @click.native="this.scrollFix('#_creating_subscribers')">Creating Subscribers</router-link></p>

</li>
<li>
<p><router-link to="#_creating_group_subscribers" @click.native="this.scrollFix('#_creating_group_subscribers')">Creating Subscriber Groups</router-link></p>

</li>
<li>
<p><router-link to="#_closing_subscribers" @click.native="this.scrollFix('#_closing_subscribers')">Closing Subscribers</router-link></p>

</li>
<li>
<p><router-link to="#_receiving_messages" @click.native="this.scrollFix('#_receiving_messages')">Receiving Messages</router-link></p>

</li>
<li>
<p><router-link to="#_committing_message_acknowledgement" @click.native="this.scrollFix('#_committing_message_acknowledgement')">Committing</router-link></p>

</li>
<li>
<p><router-link to="#_seeking__reposition_a_subscriber" @click.native="this.scrollFix('#_seeking__reposition_a_subscriber')">Seek to a Position</router-link></p>

</li>
</ul>

<h3 id="_creating_subscribers">Creating Subscribers</h3>
<div class="section">
<p>The simplest way to create a <code>Subscribers</code> is from the Coherence <code>Session</code> API, by calling the <code>createSubscriber</code> method.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;

Session session = Coherence.getSession();

Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic");</markup>

<p>The code snippet above creates an anonymous <code>Subscriber</code> that subscribes to <code>String</code> messages from the topic names <code>test-topic</code>.</p>

<p>Alternatively, a <code>Subscriber</code> can be obtained directly from a <code>NamedTopic</code> instance.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Subscriber;

Session            session = Coherence.getSession();
NamedTopic&lt;String&gt; topic   = session.getTopic("test-topic");

Subscriber&lt;String&gt; subscriber = topic.createSubscriber("test-topic");</markup>

<p>Both the <code>Session.createSubscriber()</code> and <code>NamedTopic.createSubscriber()</code> methods also take a var-args array of <code>Subscriber.Option</code> instances to futher configure the behaviour of the subscriber.Some of these options are described below.</p>

</div>

<h3 id="_creating_group_subscribers">Creating Group Subscribers</h3>
<div class="section">
<p>To create a subscriber that is part of a subscriber group the <code>Subscriber.Name</code> option can be used.
Subscriber groups have a unique name and a subscriber joins a group by specifying the group name.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import static com.tangosol.net.topic.Subscriber.Name.inGroup;

Session session = Coherence.getSession();

Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", inGroup("group-one"));</markup>

<p>The code above creates a subscriber that subscribes from the <code>test-topic</code>.The subscriber is part of the group named <code>group-one</code>.This is specified by adding a <code>Subscriber.Name</code> option using the static factory method <code>Subscriber.Name.inGroup</code>.</p>

</div>

<h3 id="_closing_subscribers">Closing Subscribers</h3>
<div class="section">
<p>Subscribers should ideally be closed when application code finishes with them so that any server side and client side resources associated with them are also closed and cleaned up.Orphaned subscribers, where the client application has gone away, will eventually be cleaned up by servr side code.Subscriber groups that are durable will remain until manually removed.</p>

<p>Subscribers have a <code>close()</code> method, and are in fact auto-closable, so can be used in a try with resources block.For example:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;

Session session = Coherence.getSession();

try (Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic"))
    {
    // ... receive messages ...
    }</markup>

<p>In the above example, the subscriber is used to receive messages inside the try/catch block.Once the try/catch block exits the subscriber is closed.</p>

<p>When a subscriber is closed, it can no longer be used.Calls to subscriber methods after closing will throw an <code>IllegalStateException</code>.</p>

</div>

<h3 id="_receiving_messages">Receiving Messages</h3>
<div class="section">
<p>The sole purpose of a subscriber is to receive messages from a topic.This is done by calling the <code>Subscriber.receive()</code> method to receive a single message or <code>Subscriber.receive(int)</code> to receive multiple messages in a batch.Both forms of the <code>receive</code> method are asynchronous, and return a <code>CompletableFuture</code> that will be completed with the result of polling the topic for messages.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic");

CompletableFuture&lt;Element&lt;String&gt;&gt; future subscriber.receive();                <span class="conum" data-value="1" />

CompletableFuture&lt;List&lt;Element&lt;String&gt;&gt;&gt; futureBatch subscriber.receive(10);   <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">The first call to receive will return a <code>CompletableFuture</code> that will complete with a <code>Subscriber.Element</code> that will contain the message from the topic and meta-data about the element.</li>
<li data-value="2">The second call to receive will return a <code>CompletableFuture</code> that will complete with a batch of upto 10 elements.The <code>int</code> parameter is a hint to the subscriber to return a batch and is the maximum number of messages that should be returned, the subscriber could return less messages.At most, a subscriber will return a full page of messages in a batch, so calling receive with a value higher than a page size will not return more messages than the page contains.</li>
</ul>

<h4 id="_future_completion">Future Completion</h4>
<div class="section">
<p>By default, the <code>CompletableFuture</code> returned from a call to receive will not complete until a message is received. If the topic is empty (or in the case of a group subscriber all the channels owned by the subscriber are empty) the future will not complete until a new message is published to the topic or channel.</p>

<p>This behaviour can be changed so that is a topic or owned channels are empty, the future will complete with a <code>null</code> element value. This is controled by creating the subscriber with the <code>CompleteOnEmpty</code> option.</p>

<p>For example, to create a subscriber where calls to receive return even if the topic is empty:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;
import com.tangosol.net.topic.Subscriber.Element;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", CompleteOnEmpty.enabled()); <span class="conum" data-value="1" />

CompletableFuture&lt;Element&lt;String&gt;&gt; future subscriber.receive();
Element&lt;String&gt; element = future.get();                           <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">The subscriber is created using the <code>CompleteOnEmpty.enabled()</code> option, so it will complete futures even if the topic is empty.</li>
<li data-value="2">The call to <code>future.get()</code> may return <code>null</code> if the topic or owned channels are emtpy.</li>
</ul>
</div>

<h4 id="_multiple_calls_to_receive_and_message_ordering">Multiple calls to Receive and Message Ordering</h4>
<div class="section">
<p>Because the subscriber API is asynchronous, multiple consecutive calls can be made to the <code>receive</code> methods, without waiting for the first call to complete.To maintain message delivery order, the subscriber will complete the futures in the order that the calls were made.</p>

<div class="admonition important">
<p class="admonition-textlabel">Important</p>
<p ><p>Any use of the <code>CompleteableFuture</code> async API (for example <code>future.thenApplyAsync()</code>, <code>future.handleAsync()</code> etc) to hand of completion handling to another thread will then remove any ordering guarantees for message processing. The same applies to application code that manually hands the returned elements off to other worker threads for processing.</p>

<p>It is up to the application code to then handle the futures in such a way that ordering is maintained if that is important to the application&#8217;s use-case..</p>
</p>
</div>
<p>The use of the synchronous <code>CompletableFuture</code> API (for example <code>future.thenApply()</code>, <code>future.handle()</code> etc) will cause completion of other futures by the subscriber to block until the handler code is complete.To maintain order of completion, the subscribe queues up the futures to be completed by a single daemon thread.</p>

<p>For examples:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;
import com.tangosol.net.topic.Subscriber.Element;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", CompleteOnEmpty.enabled()); <span class="conum" data-value="1" />

CompletableFuture&lt;Void&gt; futureOne = subscriber.receive()
        .thenAccept(element -&gt; {
            // handle first element...
        });

CompletableFuture&lt;Void&gt; futureTwo = subscriber.receive()
        .thenAccept(element -&gt; {
            // handle second element...
        });</markup>

<p>In the example above, the code that handles the first element must fully complete before the second future will complete.</p>

<p>In use cases where order of processing on the client is not important the full async API can be used.</p>

<div class="admonition important">
<p class="admonition-textlabel">Important</p>
<p ><p>Another important aspect of using the async API with subscribers is correct error handling.</p>

<p>This is bad code:</p>

<markup
lang="java"

>subscriber.receive()
        .thenAccept(element -&gt; {
            // handle first element...
        });</markup>

<p>If the call to <code>receive()</code> fails and the future completes exceptionally, or the handler code in the <code>thenAccept</code> call fails and throws an exception, those exceptions will be lost and not even logged.</p>

<p>A better way is to always finish with a <code>handle</code> call or use one of the other methods of the <code>CompletableFuture</code> API to check for exceptions.</p>

<markup
lang="java"

>subscriber.receive()
        .thenAccept(element -&gt; {
            // process second element...
        }).handle((_void, error) -&gt; {
            if (error != null)
                {
                // something went wrong!!!
                }
            return null;
        });</markup>
</p>
</div>
</div>
</div>

<h3 id="_committing_message_acknowledgement">Committing (Message Acknowledgement)</h3>
<div class="section">
<p>In order to provide at least once delivery guarantees, the subscriber API has methods that allow messages to be committed, so that the server knows they have been processed and will not re-deliver them in the case where a group subscriber fails over or is closed, and a new subscriber in the group takes over the channel ownership.</p>

<p>When a subscriber does a commit, it is actually committing a position in a channel of a topic.It effectively says that a specific position in a channel and all earlier positions have been processed.For example if a subscriber reads 10 messages from positions 0 - 9 and commits position 9, then positions 0 - 8 are also committed.</p>

<p>There are two ways to commit a position; either using the commit method on an <code>Element</code> returned from a call to <code>receive()</code>, or by calling the <code>commit</code> method on a <code>Subscriber</code> that takes a channel and  <code>Position</code> argument.</p>


<h4 id="_commit_a_received_element">Commit a Received Element</h4>
<div class="section">
<p>The <code>Element</code> returnd from a <code>receive</code> call has a <code>commit()</code> method that can be used to commit the element&#8217;s channel and position.</p>

<p>For example:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResult;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.Subscriber.Name.inGroup;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", Name.inGroup("test-group"));

CompletableFuture&lt;Element&lt;String&gt;&gt; future = subscriber.receive();   <span class="conum" data-value="1" />
Element&lt;String&gt; element = future.get();                             <span class="conum" data-value="2" />
String value = element.getValue();                                  <span class="conum" data-value="3" />

// process the message value                                        <span class="conum" data-value="4" />

CommitResult result = element.commit();                             <span class="conum" data-value="5" /></markup>

<ul class="colist">
<li data-value="1">The application calls <code>receive()</code></li>
<li data-value="2">The <code>element</code> will be returned when the future completes</li>
<li data-value="3">The message value can be obtained from the element</li>
<li data-value="4">Application code processes the message value</li>
<li data-value="5">The <code>commit</code> method is called to commit the position of the element.</li>
</ul>
<p>By committing the element directly, application code does not need to track the channel or positions of received elements.</p>

</div>

<h4 id="_commit_a_position">Commit a Position</h4>
<div class="section">
<p>To commit a <code>Position</code> in a channel directly the <code>Subscriber.commit(int, Position)</code> method can be used.
For example:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResult;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.Subscriber.Name.inGroup;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", Name.inGroup("test-group"));

CompletableFuture&lt;Element&lt;String&gt;&gt; future = subscriber.receive();   <span class="conum" data-value="1" />
Element&lt;String&gt; element = future.get();                             <span class="conum" data-value="2" />
String value = element.getValue();                                  <span class="conum" data-value="3" />

// process the message value   <span class="conum" data-value="4" />

int channel = element.getChannel();      <span class="conum" data-value="5" />
Position position = element.commit();

CommitResult result = subscriber.commit(channel, position);   <span class="conum" data-value="6" /></markup>

<ul class="colist">
<li data-value="1">The application calls <code>receive()</code></li>
<li data-value="2">The <code>element</code> will be returned when the future completes</li>
<li data-value="3">The message value can be obtained from the element</li>
<li data-value="4">Application code processes the message value</li>
<li data-value="5">The channel and <code>Position</code> can be obtained for the element</li>
<li data-value="6">The channel and <code>Position</code> can then be committed later by calling <code>commit</code> on the subscriber</li>
</ul>
</div>
</div>

<h3 id="_seeking__reposition_a_subscriber">Seeking - Reposition a Subscriber</h3>
<div class="section">
<p>The common behaviour for a subscriber is to connect and then receive messages in order until all the messages are processed.Sometimes though it is desirable to rewind a subscriber to reprocess previously consumed messages, or to move a subscriber forwards to skip messages.</p>

<p>When rewinding a position, whether the action is successful or not depends on how the topic has been configured.If the topic is configured to retain messages (not the default) then previously received messages are still available and can be re-received.For topics that do not retain messages, then messages are removed once all connected subscribers, or subscriber groups, have read the message.In the case of non-retained topics therefore, it may not be possible to rewind as the messages may have been removed.Even in topics that retain consumed messages, the messages may have been removed if the topic is configured with message expiry.</p>

<p>If an attempt is made to rewind further back than the first message in the topic, the seek will reposition the subscriber just before the first available message.If an attempt is made to reposition a subscriber much further ahead than the current tail of the topic, the subscriber will be positioned at the tail, so that it receives the next published message.</p>


<h4 id="_seek_to_a_position">Seek to a Position</h4>
<div class="section">
<p>The subscriber has a <code>seek</code> method that takes a channel, and a <code>Position</code> that moves the subscriber to the specified position in the channel.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResult;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.Subscriber.Name.inGroup;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", Name.inGroup("test-group"));

Element&lt;String&gt; firstElement = subscriber.receive().get();     <span class="conum" data-value="1" />

for (int i = 0; i &lt; 10; i++)                                   <span class="conum" data-value="2" />
    {
    Element&lt;String&gt; element = subscriber.receive().get();
    // process element...
    }

subscriber.seek(firstElement.getChannel(), firstElement.getPosition());   <span class="conum" data-value="3" /></markup>

<ul class="colist">
<li data-value="1">The example above is a bit contrived, but shows how seek can be used.The first element is received from the topic.</li>
<li data-value="2">Another 10 elements are then processed from the subscriber.</li>
<li data-value="3">The <code>seek</code> method is then used to move the subscriber back to the position of the first message.</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">When seeking, the next message received is the message <strong>after</strong> the seek position.In the example above, after the seek call the next message received wil not be the same as first element, it will be the next message, so the first message received in the for loop.</p>
</div>
<p>The subscriber also has methods to seek to the head (re-read the first message) or tail (read the next message published) for a channel without needing to know the head or tail positions.</p>

</div>

<h4 id="_seek_to_a_timestamp">Seek to a Timestamp</h4>
<div class="section">
<p>Subscribers can also be repositioned to the next message based on a timestamp that the message was published. All messages have a timestamp based on the Coherence cluster time in the storage member that accepted the published message. When seeking using a timestamp, the subscriber is repositioned such that the next message received is the first message <strong>after</strong> the specified timestamp.</p>

<p>The timestamp is specified as a <code>java.time.Instant</code> when seeking to a timestamp.</p>

<p>For example:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.Element;

Session            session    = Coherence.getSession();
Subscriber&lt;String&gt; subscriber = session.createSubscriber("test-topic", Name.inGroup("test-group"));

Instant timestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(20, 30))  <span class="conum" data-value="1" />
        .toInstant(ZoneOffset.UTC);

Position position = subscriber.seek(1, timestamp);                           <span class="conum" data-value="2" /></markup>

<ul class="colist">
<li data-value="1">A <code>java.time.Instant</code> is created for 20:30 today.</li>
<li data-value="2">Seek is called to reposition the subscriber so that the next message received from channel <code>1</code> will be the first message published after 20:30.</li>
</ul>
<div class="admonition note">
<p class="admonition-inline">Repositioning to a timestamp in the future will reposition the subscriber at the tail, so the next message received will be the next published message, regardless of the time. It is not possible to seek to a timestamp in the future so that messages are ignored until the time is reached.</p>
</div>
</div>
</div>
</div>
</doc-view>
