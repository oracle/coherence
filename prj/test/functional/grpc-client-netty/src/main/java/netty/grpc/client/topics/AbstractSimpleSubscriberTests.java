/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import topics.AbstractNamedTopicTests;
import topics.data.Customer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.tangosol.net.topic.Subscriber.inGroup;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unchecked")
public abstract class AbstractSimpleSubscriberTests
    {
    // ----- constructors ---------------------------------------------------

    protected AbstractSimpleSubscriberTests(String sSerializer)
        {
        this(sSerializer, false);
        }

    protected AbstractSimpleSubscriberTests(String sSerializer, boolean fIncompatibleClientSerializer)
        {
        m_sSerializer                   = sSerializer;
        m_fIncompatibleClientSerializer = fIncompatibleClientSerializer;
        }

    // ----- test lifecycle methods -----------------------------------------

    @BeforeClass
    public static void setProperties()
        {
        System.setProperty("coherence.log.level", "5");
        }

    @Before
    public void beforeEach()
        {
        System.err.println(">>>>> Starting test: " + m_testWatcher.getMethodName());
        System.err.flush();
        }

    @After
    public void cleanup()
        {
        System.err.println(">>>>> Starting cleanup: " + m_testWatcher.getMethodName());
        try
            {
            if (m_topic != null)
                {
                System.err.println("Destroying topic " + m_topic);
                m_topic.destroy();
                m_topic = null;
                m_sTopicName = null;
                }
            else if (m_sTopicName != null)
                {
                System.err.println("Destroying topic " + m_sTopicName + " by first getting from Session");
                NamedTopic<?> topic = getSession().getTopic(m_sTopicName);
                topic.destroy();
                m_sTopicName = null;
                }
            }
        catch (Throwable e)
            {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            }
        finally
            {
            System.err.println(">>>>> Finished test: " + m_testWatcher.getMethodName());
            System.err.flush();
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldSubscribeWithSingleSubscriber() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (SimpleSubscriber<String> subscriber = SimpleSubscriber.createSubscriber(topic))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                Publisher.Status status = publisher.publish("Element-0").get(2, TimeUnit.MINUTES);

                Subscriber.Element<String> element = subscriber.receive();
                assertThat(element, is(notNullValue()));
                assertThat(element.getChannel(), is(status.getChannel()));
                assertThat(element.getPosition(), is(status.getPosition()));
                assertThat(element.getValue(), is("Element-0"));
                }
            }
        }

    @Test
    public void shouldReceiveMultipleMessagesSameChannel() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (SimpleSubscriber<String> subscriber = SimpleSubscriber.createSubscriber(topic))
            {
            int nChannel = -1;
            int cMessage = 1000;

            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderById.id(5)))
                {
                for (int i = 0; i < cMessage; i++)
                    {
                    Publisher.Status status = publisher.publish("Element-" + i).get(2, TimeUnit.MINUTES);
                    int nPublishedChannel = status.getChannel();
                    if (nChannel == -1)
                        {
                        nChannel = nPublishedChannel;
                        }
                    else
                        {
                        assertThat(nPublishedChannel, is(nChannel));
                        }
                    }
                }

            Subscriber.Element<String> element;
            for (int i = 0; i < cMessage; i++)
                {
                element = subscriber.receive();
                assertThat(element, is(notNullValue()));
                assertThat(element.getChannel(), is(nChannel));
                assertThat(element.getValue(), is("Element-" + i));
                }
            }
        }

    @Test
    public void shouldReceiveMultipleMessagesMultipleChannel() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (SimpleSubscriber<String> subscriber = SimpleSubscriber.createSubscriber(topic))
            {
            int cMessage = 1000;

            Map<Integer, List<String>> mapByChannel = new HashMap<>();
            Map<String, Publisher.Status> mapByMessage = new HashMap<>();

            try (Publisher<String> publisher = topic.createPublisher(Publisher.OrderBy.roundRobin()))
                {
                for (int i = 0; i < cMessage; i++)
                    {
                    String sMsg = "Element-" + i;
                    Publisher.Status status = publisher.publish(sMsg).get(2, TimeUnit.MINUTES);
                    int nChannel = status.getChannel();
                    mapByChannel.compute(nChannel, (k, list) ->
                        {
                        if (list == null)
                            {
                            list = new ArrayList<>();
                            }
                        list.add(sMsg);
                        return list;
                        });
                    mapByMessage.put(sMsg, status);
                    }
                }

            Subscriber.Element<String> element;
            for (int i = 0; i < cMessage; i++)
                {
                element = subscriber.receive();
                assertThat(element, is(notNullValue()));
                String sMsg = element.getValue();
                Publisher.Status status = mapByMessage.get(sMsg);
                assertThat(status, is(notNullValue()));
                assertThat(element.getChannel(), is(status.getChannel()));
                assertThat(element.getPosition(), is(status.getPosition()));
                List<String> list = mapByChannel.get(element.getChannel());
                assertThat(list.isEmpty(), is(false));
                String sExpected = list.remove(0);
                assertThat(sMsg, is(sExpected));
                }
            }
        }

    @Test
    public void shouldSubscribeWithMultipleSubscribers() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        String             sGroupOne = ensureGroupName() + "-One";
        String             sGroupTwo = ensureGroupName() + "-Two";

        try (SimpleSubscriber<String> subscriberOne   = SimpleSubscriber.createSubscriber(topic, inGroup(sGroupOne));
             SimpleSubscriber<String> subscriberTwo   = SimpleSubscriber.createSubscriber(topic, inGroup(sGroupTwo));
             SimpleSubscriber<String> subscriberThree = SimpleSubscriber.createSubscriber(topic))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("Element-0").get(2, TimeUnit.MINUTES);
                }

            assertThat(subscriberOne.receive().getValue(), is("Element-0"));
            assertThat(subscriberTwo.receive().getValue(), is("Element-0"));
            assertThat(subscriberThree.receive().getValue(), is("Element-0"));
            }
        }

    // ----- helper methods -------------------------------------------------

    protected String ensureGroupName()
        {
        return "group-" + m_nGroup.incrementAndGet();
        }

    protected synchronized String ensureTopicName()
        {
        return ensureTopicName(m_sSerializer);
        }

    protected synchronized String ensureTopicName(String sPrefix)
        {
        if (m_sTopicName == null)
            {
            m_sTopicName = sPrefix + "-" + m_nTopic.incrementAndGet();
            }
        return m_sTopicName;
        }

    protected synchronized NamedTopic<String> ensureTopic()
        {
        if (m_topic == null)
            {
            String sName = ensureTopicName();
            m_topic = getSession().getTopic(sName);
            }

        return (NamedTopic<String>) m_topic;
        }

    @SuppressWarnings("unchecked")
    protected synchronized <V> NamedTopic<V> ensureRawTopic()
        {
        if (m_topic == null)
            {
            String sName = ensureTopicName(m_sSerializer + "-raw");
            m_topic = getSession().getTopic(sName);
            }

        return (NamedTopic<V>) m_topic;
        }

    @SuppressWarnings("unchecked")
    protected synchronized NamedTopic<String> ensureTopic(String sTopicName)
        {
        if (m_topic != null)
            {
            assertThat(m_topic.getName(), is(sTopicName + "-" + m_nTopic.get()));
            return (NamedTopic<String>) m_topic;
            }
        String sName = ensureTopicName(sTopicName);
        return (NamedTopic<String>) (m_topic = getSession().getTopic(sName, ValueTypeAssertion.withType(String.class)));
        }

    protected synchronized NamedTopic<Customer> ensureCustomerTopic(String sTopicName)
        {
        if (m_topicCustomer != null)
            {
            m_topicCustomer.destroy();
            }
        String sName = ensureTopicName(sTopicName);
        return m_topicCustomer = getSession().getTopic(sName, ValueTypeAssertion.withType(Customer.class));
        }

    protected abstract Session getSession();

    // ----- constants ------------------------------------------------------

    static public final String DEFAULT_COHERENCE_CACHE_CONFIG = "coherence-cache-config.xml";

    static public final String CUSTOMIZED_CACHE_CONFIG        = "topic-cache-config.xml";

    // ----- data members ---------------------------------------------------

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @Rule
    public AbstractNamedTopicTests.Watcher m_testWatcher = new AbstractNamedTopicTests.Watcher();

    // MUST BE STATIC because JUnit creates a new test class per test method
    protected static final AtomicInteger m_nTopic = new AtomicInteger(0);

    // MUST BE STATIC because JUnit creates a new test class per test method
    protected static final AtomicInteger m_nGroup = new AtomicInteger(0);

    protected String               m_sSerializer;

    protected boolean              m_fIncompatibleClientSerializer;

    protected NamedTopic<?>        m_topic;

    protected String               m_sTopicName;

    protected NamedTopic<Customer> m_topicCustomer;

    protected ExecutorService      m_executorService = Executors.newFixedThreadPool(4);
    }
