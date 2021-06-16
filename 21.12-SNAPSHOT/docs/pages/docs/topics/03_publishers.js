<doc-view>

<h2 id="_publishers">Publishers</h2>
<div class="section">
<p>Publishers are used to publish messages to a Coherence topic, a publisher publishes to a single topic.</p>

<ul class="ulist">
<li>
<p><router-link to="#_creating_publishers" @click.native="this.scrollFix('#_creating_publishers')">Creating Publishers</router-link></p>

</li>
<li>
<p><router-link to="#_closing_a_publisher" @click.native="this.scrollFix('#_closing_a_publisher')">Closing a Publisher</router-link></p>

</li>
<li>
<p><router-link to="#_configure_ordering_guarantees" @click.native="this.scrollFix('#_configure_ordering_guarantees')">Configure Ordering Guarantees</router-link></p>
<ul class="ulist">
<li>
<p><router-link to="#_orderby_thread" @click.native="this.scrollFix('#_orderby_thread')">Ordering by publishing thread</router-link></p>

</li>
<li>
<p><router-link to="#_orderby_value" @click.native="this.scrollFix('#_orderby_value')">Ordering by message value</router-link></p>

</li>
<li>
<p><router-link to="#_orderby_id" @click.native="this.scrollFix('#_orderby_id')">Ordering by fixed channel</router-link></p>

</li>
<li>
<p><router-link to="#_publish_orderable_messages" @click.native="this.scrollFix('#_publish_orderable_messages')">Orderable messages</router-link></p>

</li>
</ul>
</li>
</ul>

<h3 id="_creating_publishers">Creating Publishers</h3>
<div class="section">
<p>The simplest way to create a <code>Publisher</code> is from the Coherence <code>Session</code> API, by calling the <code>createPublisher</code> method.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;

Session session = Coherence.getSession();

Publisher&lt;String&gt; publisher = session.createPublisher("test-topic");</markup>

<p>The code snippet above creates an anonymous <code>Publisher</code> that publishes to <code>String</code> messages to the topic names <code>test-topic</code>.</p>

<p>Alternatively, a <code>Publisher</code> can be obtained directly from a <code>NamedTopic</code> instance.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;

Session            session  = Coherence.getSession();
NamedTopic&lt;String&gt; topic    = session.getTopic("test-topic");

Publisher&lt;String&gt;  publisher = topic.createPublisher("test-topic");</markup>

<p>Both the <code>Session.createPublisher()</code> and <code>NamedTopic.createPublisher()</code> methods also take a var-args array of <code>Publisher.Option</code> instances to further configure the behaviour of the publisher.Some of these options are described below.</p>

</div>

<h3 id="_closing_a_publisher">Closing a Publisher</h3>
<div class="section">
<p>Publishers should ideally be closed when application code finishes with them so that any resources associated with them are also closed and cleaned up.</p>

<p>Pubishers have a <code>close()</code> method, and are in fact auto-closable, so can be used in a try with resources block.For example:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;

Session session = Coherence.getSession();

try (Publisher&lt;String&gt; publisher = session.createPublisher("test-topic"))
    {
    // ... publish messages ...
    }</markup>

<p>In the above example, the publisher is used to publish messages inside the try/catch block.Once the try/catch block exits, the publisher is closed.</p>

<p>When a publisher is closed, it can no longer be used.Calls to publish methods after closing will throw an <code>IllegalStateException</code>.</p>

</div>

<h3 id="_configure_ordering_guarantees">Configure Ordering Guarantees</h3>
<div class="section">
<p>In a lot of use cases it is important that messages are processed by subscribers in a guaranteed order.The publisher can be configured when it is created to use different ordering guarantees by using the <code>Publisher.OrderBy</code> option.</p>

<p>The OrderBy options controls which channel (or channels) in a topic a publisher publishes messages to.Subscribers receive messages from a specific channel in the order that they were published.</p>


<h4 id="_orderby_thread">OrderBy Thread</h4>
<div class="section">
<p>The default behaviour in Coherence Topics is that messages published by a publisher on a specific JVM thread are received in order they were published.Messages published on different threads, or by different publishers could be received interleaved with each other but always in order from the viewpoint of the publishing thread.</p>

<p>The <code>OrderBy.thread()</code> option can be specified explicitly as shown below:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;

Session session = Coherence.getSession();

Publisher&lt;String&gt; publisher = session.createPublisher("test-topic", OrderBy.thread());</markup>

</div>

<h4 id="_orderby_value">OrderBy Value</h4>
<div class="section">
<p>It is possible to publish messages to a channel based on a function that derives a channel from the value being published.This is achieved using the <code>OrderBy.value(ToIntFunction&lt;? super V&gt; orderIdFunction)</code> option, where the <code>orderIdFunction</code> is a <code>java.util.function.ToIntFunction</code> that takes the value being published and returns an <code>int</code>.The publisher channel the value is published to will be derived by modding the returned int with the number of channels available for the topic.For example, if there were 17 channels (0 - 16), and the function returned 8, the message would be published to channel 8, if the function returned 19, the message would be published to <code>19 % 17</code> - which is 2. If the int returned is negative, a positive channel number is calculated equal to <code>n % channelCount + channelCount</code>)</p>

<p>For example, suppose a <code>Publisher</code> is publishing <code>Order</code> messages, and we only care about ordering by the orders <code>customerId</code> (which coincidentally is an <code>int</code>).We could do something like this:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;

Session session = Coherence.getSession();

OrderBy orderBy = OrderBy.value(order -&gt; order.getCustomerId());
Publisher&lt;Order&gt; publisher = session.createPublisher("test-topic", orderBy);</markup>

</div>

<h4 id="_orderby_id">OrderBy Id</h4>
<div class="section">
<p>Messages can be published to a fixed channel by using the <code>OrderBy.id(int nOrderId)</code> option, where the <code>nOrderId</code> parameter is used to determine the channel.The exact channel is worked out in the same way that it is for the <code>OrderBy.value()</code> option, by modding the <code>int</code> value used in the <code>OrderBy.id()</code> option with the number of channels.For example, if there were 17 channels, and the option used was <code>OrderBy.id(8)</code> then all messages would be published to channel 8, if the <code>OrderBy.id(19)</code> was used all messages would go to channel <code>19 % 17</code>, which is channel 2.</p>

<p>For example, to publish all messages from a <code>Publisher</code> to channel 5:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;

Session session = Coherence.getSession();

Publisher&lt;Order&gt; publisher = session.createPublisher("test-topic", OrderBy.is(5));</markup>

<p>Using the <code>OrderBy.id()</code> option with multiple publishers with the same Id value will cause all those publishers to publish to the same channel.This would mean that message ordering would be global across all of those publishers, with the caveat that there would be more contention with publishers against the tail of the channel.</p>

</div>

<h4 id="_orderby_none">OrderBy None</h4>
<div class="section">
<p>Finally, using <code>OrderBy.none()</code> will guarantee no ordering, each message would be published to a random channel.This would allow the least contention in use cases where the order of message processing by subscribers did not matter.</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;

Session session = Coherence.getSession();

Publisher&lt;Order&gt; publisher = session.createPublisher("test-topic", OrderBy.none());</markup>

</div>

<h4 id="_publish_orderable_messages">Publish Orderable Messages</h4>
<div class="section">
<p>It is possible to use the values themselves to determine ordering by making the message value implement the interface <code>com.tangosol.net.topic.Publisher.Orderable</code>.This interface has a single <code>getOrderId()</code> method that returns an <code>int</code> that is used in the same way as other options above to determine the channel to publish to.</p>

<p>Publishing <code>Orderable</code> values will override any <code>OrderBy</code> option that might have been specified for the publisher.</p>

<p>This is an similar to the <code>OrderBy.value()</code> option, but in this case the code that creates the publisher does not need to know how to determine the order, this can be different for each type of message that the publisher publishes.</p>

<p>For example, if there is a <code>Transaction</code> class with a <code>String</code> customer identifier that should be used to order published <code>Transactions</code>, we might implement it like this:</p>

<markup
lang="java"
title="Transaction.java"
>public class Transaction
        implements Publisher.Orderable
    {
    private String customerId;

    @Override
    public int getOrderId()
        {
        return Objects.hashCode(customerId);
        }
    }</markup>

<p>Now we can configure a publisher:</p>

<markup
lang="java"

>import com.tangosol.net.Session;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;

Session session = Coherence.getSession();

Publisher&lt;Transaction&gt; publisher = session.createPublisher("test-topic");</markup>

<p>The publisher above did not specify an <code>OrderBy</code> option, so the default or <code>OrderBy.thread()</code> will be used, but as the <code>Transaction</code> class implements <code>Publisher.Orderable</code> then it&#8217;s <code>getOrderId</code> method wil be used to determine message ordering.</p>

</div>
</div>
</div>
</doc-view>
