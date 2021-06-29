/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;


import com.oracle.bedrock.deferred.DeferredHelper;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.coherence.common.base.NonBlocking;

import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;

import com.tangosol.io.Serializer;

import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;

import com.tangosol.net.management.MBeanHelper;

import com.tangosol.net.topic.FixedElementCalculator;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.Status;
import com.tangosol.net.topic.Publisher.OrderBy;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.TopicException;
import com.tangosol.net.topic.TopicPublisherException;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.EntryExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;

import java.io.IOException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import topics.data.Address;
import topics.data.AddressExternalizableLite;
import topics.data.AddressPof;
import topics.data.Customer;
import topics.data.CustomerExternalizableLite;
import topics.data.CustomerPof;
import topics.data.OrderableMessage;

import java.util.Arrays;
import java.util.Set;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import java.util.function.ToIntFunction;

import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static com.oracle.bedrock.testsupport.deferred.Eventually.assertDeferred;

import static com.tangosol.net.topic.Subscriber.Name.inGroup;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;


/**
 * @author jk 2015.05.28
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    protected AbstractNamedTopicTests(String sSerializer)
        {
        m_sSerializer = sSerializer;
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
        System.err.println(">>>>> Starting test: " + m_testName.getMethodName());
        m_nSuffix.getAndIncrement();
        }

    @After
    public void cleanup()
        {
        try
            {
            if (m_topic != null)
                {
                m_topic.destroy();
                m_topic = null;
                m_sTopicName = null;
                }
            else if (m_sTopicName != null)
                {
                NamedTopic<?> topic = getSession().getTopic(m_sTopicName);
                topic.destroy();
                m_sTopicName = null;
                }
            }
        catch (Throwable e)
            {
            e.printStackTrace();
            }
        System.err.println(">>>>> Finished test: " + m_testName.getMethodName());
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldFilter() throws Exception
        {
        Session session    = getSession();
        String  sTopicName = ensureTopicName();

        try (Subscriber<String> subscriberD   = session.createSubscriber(sTopicName, Subscriber.Filtered.by(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "d")));
             Subscriber<String> subscriberA   = session.createSubscriber(sTopicName, Subscriber.Filtered.by(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "a")));
             Subscriber<String> subscriberLen = session.createSubscriber(sTopicName, Subscriber.Filtered.by(new GreaterFilter<>(String::length, 1))))
            {
            try (Publisher<String> publisher = session.createPublisher(sTopicName))
                {
                publisher.publish("a").thenAccept(v -> System.err.println("**** Published a"));
                publisher.publish("zoo").thenAccept(v -> System.err.println("**** Published zoo"));
                publisher.publish("b").thenAccept(v -> System.err.println("**** Published b"));
                publisher.publish("c").thenAccept(v -> System.err.println("**** Published c"));
                publisher.publish("d").thenAccept(v -> System.err.println("**** Published d"));
                publisher.publish("e").thenAccept(v -> System.err.println("**** Published e"));
                publisher.publish("f").thenAccept(v -> System.err.println("**** Published f"));

                CompletableFuture<Void> future = publisher.flush();
                // wait upto one minute for the future
                future.get(2, TimeUnit.MINUTES);
                assertThat(future.isCompletedExceptionally(), is(false));
                }

            assertThat(subscriberD.receive().get(1, TimeUnit.MINUTES).getValue(), is("zoo"));
            assertThat(subscriberD.receive().get(1, TimeUnit.MINUTES).getValue(), is("e"));
            assertThat(subscriberD.receive().get(1, TimeUnit.MINUTES).getValue(), is("f"));

            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("zoo"));
            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("b"));
            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("c"));
            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("d"));
            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("e"));
            assertThat(subscriberA.receive().get(1, TimeUnit.MINUTES).getValue(), is("f"));

            assertThat(subscriberLen.receive().get(1, TimeUnit.MINUTES).getValue(), is("zoo"));
            }
        }

    @Test
    public void shouldConvert() throws Exception
        {
        NamedTopic<String>  topic       = ensureTopic();
        try(Subscriber<Integer> subscriber1 = topic.createSubscriber(Subscriber.Convert.using(Integer::parseInt));
            Subscriber<Integer> subscriber2 = topic.createSubscriber(Subscriber.Convert.using(String::length)))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("1");
                publisher.publish("22");
                publisher.publish("333");
                publisher.publish("4444");
                publisher.publish("55555");
                }

            assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(1));
            assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(22));
            assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(333));
            assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(4444));
            assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(55555));

            assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(1));
            assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(2));
            assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(3));
            assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(4));
            assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(5));
            }
        }

    @Test
    public void shouldCreateTopicNoValueTypeAssertionOption()
        {
        Session            session = getSession();
        String             sName   = ensureTopicName(m_sSerializer + "-raw-test");
        NamedTopic<String> topic   = session.getTopic(sName);

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }

        topic.destroy();
        }

    @Test
    public void shouldCreateAndDestroyTopic()
        {
        Session                  session = getSession();
        String                   sName   = ensureTopicName(m_sSerializer + "-test");
        final NamedTopic<String> topic   = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }

        assertThat("assert topic is active", topic.isActive(), is(true));
        assertFalse("assert topic is not destroyed", topic.isDestroyed());

        topic.destroy();

        assertDeferred("Assert topic " + sName + " is not active",
                       topic::isActive, Matchers.is(false),
                       DeferredHelper.within(30, TimeUnit.SECONDS));
        assertDeferred("Assert topic " + sName + " is destroyed",
                       topic::isDestroyed, Matchers.is(true),
                       DeferredHelper.within(30, TimeUnit.SECONDS));
        assertThat(topic.isReleased(), is(true));


        NamedTopic<String> topic2 = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        try (Publisher<String> publisher = topic2.createPublisher())
            {
            populate(publisher, 20);
            }
        }

    @Test
    public void shouldCreateAndReleaseTopic()
        {
        Session                  session = getSession();
        String                   sName   = ensureTopicName(m_sSerializer + "-test");
        final NamedTopic<String> topic   = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }

        assertThat("assert topic is active", topic.isActive(), is(true));
        assertFalse("assert topic is not released", topic.isReleased());

        topic.release();

        assertDeferred("Assert topic " + sName + " is released",
                       topic::isReleased, Matchers.is(true),
                       DeferredHelper.within(15, TimeUnit.SECONDS));

        assertFalse(topic.isActive());
        }

    @Test
    public void shouldRunOnPublisherClose()
        {
        AtomicBoolean fRan       = new AtomicBoolean(false);
        String        sTopicName = ensureTopicName();

        try (Publisher publisher = getSession().createPublisher(sTopicName))
            {
            publisher.onClose(() -> fRan.set(true));
            publisher.close();
            assertThat(fRan.get(), is(true));
            }
        }

    @Test
    public void shouldRunOnSubscriberClose()
        {
        AtomicBoolean fRan       = new AtomicBoolean(false);
        String        sTopicName = ensureTopicName();

        try(Subscriber subscriber = getSession().createSubscriber(sTopicName))
            {
            subscriber.onClose(() -> fRan.set(true));
            subscriber.close();
            assertThat(fRan.get(), is(true));
            }
        }

    @Test
    public void shouldFillAndEmpty()
            throws Exception
        {
        int     nCount     = 1000;
        Session session    = getSession();
        String  sTopicName = ensureTopicName();

        try (Publisher<String>  publisher     = session.createPublisher(sTopicName);
             Subscriber<String> subscriberFoo = session.createSubscriber(sTopicName, inGroup("Foo"), Subscriber.CompleteOnEmpty.enabled());
             Subscriber<String> subscriberBar = session.createSubscriber(sTopicName, inGroup("Bar"), Subscriber.CompleteOnEmpty.enabled()))
            {
            for (int i = 0; i < nCount; i++)
                {
                if (i % 100 == 0)
                    {
                    System.out.print("S");
                    }
                else if (i % 10 == 0)
                    {
                    System.out.print(".");
                    }

                String sElement = "Element-" + i;

                publisher.publish(sElement).join();
                }

            System.out.println();
            for (int i = 0; i < nCount; i++)
                {
                if (i % 100 == 0)
                    {
                    System.out.print("S");
                    }
                else if (i % 10 == 0)
                    {
                    System.out.print(".");
                    }

                String sExpected = "Element-" + i;
                String sElement  = subscriberFoo.receive().get(1, TimeUnit.MINUTES).getValue();

                assertThat(sElement, is(sExpected));
                }

            assertThat("Subscriber 'Foo' should see empty topic", subscriberFoo.receive().get(1, TimeUnit.MINUTES), is(nullValue()));

            for (int i = 0; i < nCount; i++)
                {
                if (i % 100 == 0)
                    {
                    System.out.print("R");
                    }
                else if (i % 10 == 0)
                    {
                    System.out.print(".");
                    }

                String sExpected = "Element-" + i;
                String sElement  = subscriberBar.receive().get(1, TimeUnit.MINUTES).getValue();

                assertThat(sElement, is(sExpected));
                }

            assertThat("Subscriber 'Bar' should see empty topic", subscriberBar.receive().get(1, TimeUnit.MINUTES), is(nullValue()));
            }
        }

    @Test
    public void shouldFillAndEmptyOneByOne()
            throws Exception
        {
        int                nCount = 1000;
        NamedTopic<String> topic  = ensureTopic();

        try (Publisher<String>  publisher     = topic.createPublisher();
             Subscriber<String> subscriberFoo = topic.createSubscriber(inGroup("Foo"));
             Subscriber<String> subscriberBar = topic.createSubscriber(inGroup("Bar")))
            {
            assertThat(publisher, is(notNullValue()));
            assertThat(subscriberFoo, is(notNullValue()));
            assertThat(subscriberBar, is(notNullValue()));

            for (int i = 0; i < nCount; i++)
                {
                if (i % 100 == 0)
                    {
                    System.out.print("X");
                    }
                else if (i % 10 == 0)
                    {
                    System.out.print(".");
                    }

                String sElement = "Element-" + i;

                CompletableFuture<Status> future = publisher.publish(sElement);
                assertThat(future, is(notNullValue()));
                future.get(2, TimeUnit.MINUTES);

                assertThat(subscriberFoo.receive().get(10, TimeUnit.SECONDS).getValue(), is(sElement));
                assertThat(subscriberBar.receive().get(10, TimeUnit.SECONDS).getValue(), is(sElement));
                }
            }
        }

    @Test
    public void shouldHaveCorrectMetadataOnElements() throws Exception
        {
        Session session    = getSession();
        String  sTopicName = ensureTopicName();

        try (Subscriber<String> subscriber = session.createSubscriber(sTopicName))
            {
            try (Publisher<String> publisher = session.createPublisher(sTopicName))
                {
                for (int i = 0; i < 10; i++)
                    {
                    String          sValue   = "value-" + i;
                    Status          metadata = publisher.publish(sValue).get(2, TimeUnit.MINUTES);
                    Element<String> element  = subscriber.receive().get(2, TimeUnit.MINUTES);

                    assertThat(element.getValue(), is(sValue));
                    assertThat(element.getChannel(), is(metadata.getChannel()));
                    assertThat(element.getPosition(), is(metadata.getPosition()));
                    }
                }
            }
        }

    @Test
    public void shouldPopulateAndConsumeInParallel() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        TopicPublisher     publisher = new TopicPublisher(topic, "Element-", 500, true);

        assertPopulateAndConsumeInParallel(publisher);
        }

    // Validates Publisher.OrderBy default of OrderByThread.
    @Test
    public void shouldPopulateAndConsumeInParallelWithAsyncPublisher() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        TopicPublisher     publisher = new TopicPublisher(topic, "Element-", 500, false);

        PagedTopicCaches caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());
        assertThat(caches.Pages.isEmpty(), is(true));

        assertPopulateAndConsumeInParallel(publisher);
        }

    @Test
    public void shouldPopulateWithOrderByValueAndConsume() throws Exception
        {
        ToIntFunction<String> getValue = s -> 5;

        shouldPopulateWithOrderByOptionAndConsumeInParallel(ensureTopic(), Publisher.OrderBy.value(getValue));
        }

    @Test
    public void shouldPopulateWithOrderByThreadAndConsume() throws Exception
        {
        shouldPopulateWithOrderByOptionAndConsumeInParallel(ensureTopic(), OrderBy.thread());
        }

    @Test
    public void shouldPopulateWithOrderByIdAndConsume() throws Exception
        {
        shouldPopulateWithOrderByOptionAndConsumeInParallel(ensureTopic(), OrderBy.id(10));
        }

    @Test
    public void shouldPopulateWithOrderByNoneAndConsume() throws Exception
        {
        shouldPopulateWithOrderByOptionAndConsumeInParallel(ensureTopic(), OrderBy.none());
        }

    public void shouldPopulateWithOrderByOptionAndConsumeInParallel(NamedTopic<String> topic, OrderBy option)
        throws Exception
        {
        boolean fVerifyOrder = !(option instanceof Publisher.OrderByNone);

        assertPopulateAndConsumeInParallel(new TopicPublisher(topic, "", 10, false, option), fVerifyOrder);
        }

    public void assertPopulateAndConsumeInParallel(TopicPublisher publisher, boolean fVerifyOrder) throws Exception
        {
        NamedTopic<String> topic   = publisher.getTopic();
        int                nCount  = publisher.getCount();
        String             sPrefix = publisher.getPrefix();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("Foo"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("Bar")))
            {
            TopicSubscriber    topicSubscriber1 = new TopicSubscriber(subscriber1, sPrefix, nCount, 2, TimeUnit.MINUTES, false, fVerifyOrder);
            TopicSubscriber    topicSubscriber2 = new TopicSubscriber(subscriber2, sPrefix, nCount, 2, TimeUnit.MINUTES, false, fVerifyOrder);

            Future<?> futurePublisher = m_executorService.submit(publisher);
            Future<?> futureSubscriber1 = m_executorService.submit(topicSubscriber1);
            Future<?> futureSubscriber2 = m_executorService.submit(topicSubscriber2);

            futurePublisher.get(1, TimeUnit.MINUTES);
            futureSubscriber1.get(1, TimeUnit.MINUTES);
            futureSubscriber2.get(1, TimeUnit.MINUTES);

            assertThat(publisher.getPublished(), is(nCount));
            assertThat(topicSubscriber1.getConsumedCount(), is(nCount));
            assertThat(topicSubscriber2.getConsumedCount(), is(nCount));
            }
        }

    public void assertPopulateAndConsumeInParallel(TopicPublisher publisher) throws Exception
        {
        assertPopulateAndConsumeInParallel(publisher, /*fVerifyOrder*/ true);
        }

    @Test
    public void shouldPublishAndReceive() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriberFoo = topic.createSubscriber(inGroup("Foo"));
             Subscriber<String> subscriberBar = topic.createSubscriber(inGroup("Bar")))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("Element-0").get(2, TimeUnit.MINUTES);
                }

            assertThat(subscriberFoo.receive().get(5, TimeUnit.MINUTES).getValue(), is("Element-0"));
            assertThat(subscriberBar.receive().get(5, TimeUnit.MINUTES).getValue(), is("Element-0"));
            }
        }

    @Test
    public void shouldReconnectAnonymousSubscriber() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber();
             Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 5; i++)
                {
                publisher.publish("Element-" + i).get(2, TimeUnit.MINUTES);
                }

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("Element-0"));

            // disconnect....
            ((PagedTopicSubscriber<String>) subscriber).disconnect();

            // should reconnect at the element-1, which is effectively empty
            CompletableFuture<Element<String>> future = subscriber.receive();

            // publish something
            publisher.publish("Element-Last").get(2, TimeUnit.MINUTES);

            // the subscriber should receive the just published message
            element = future.get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("Element-0"));
            }
        }

    @Test
    public void shouldReconnectAnonymousSubscriberToRewindableTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-2");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        try (Subscriber<String> subscriber = topic.createSubscriber())
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i = 0; i < 5; i++)
                    {
                    publisher.publish("Element-" + i).get(2, TimeUnit.MINUTES);
                    }
                }

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("Element-0"));
            Subscriber.CommitResult result = element.commit();
            assertThat(result.isSuccess(), is(true));

            // receive some more but do not commit
            subscriber.receive().get(2, TimeUnit.MINUTES);
            subscriber.receive().get(2, TimeUnit.MINUTES);

            // disconnect....
            ((PagedTopicSubscriber<String>) subscriber).disconnect();

            // the reconnected subscriber should reconnect at the commit point (element-0) and read the next element
            element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("Element-1"));
            }
        }

    @Test
    public void shouldPublishAndReceiveBatch() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriberOne   = topic.createSubscriber(inGroup("One"));
             Subscriber<String> subscriberTwo   = topic.createSubscriber(inGroup("Two"));
             Subscriber<String> subscriberThree = topic.createSubscriber(inGroup("Three")))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("Element-0").join();
                publisher.publish("Element-1").join();
                publisher.publish("Element-2").join();
                }

            List<Element<String>> elementsOne   = subscriberOne.receive(1).get(1, TimeUnit.MINUTES);
            List<Element<String>> elementsTwo   = subscriberTwo.receive(2).get(1, TimeUnit.MINUTES);
            List<Element<String>> elementsThree = subscriberThree.receive(3).get(1, TimeUnit.MINUTES);

            assertThat(elementsOne.size(), is(1));
            assertThat(elementsTwo.size(), is(2));
            assertThat(elementsThree.size(), is(3));
            }
        }

    @Test
    public void shouldPublishAndReceiveBatchFromEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriberOne   = topic.createSubscriber(inGroup("One"), Subscriber.CompleteOnEmpty.enabled());
             Subscriber<String> subscriberTwo   = topic.createSubscriber(inGroup("Two"), Subscriber.CompleteOnEmpty.enabled());
             Subscriber<String> subscriberThree = topic.createSubscriber(inGroup("Three"), Subscriber.CompleteOnEmpty.enabled()))
            {
            List<Element<String>> elementsOne   = subscriberOne.receive(1).get(1, TimeUnit.MINUTES);
            List<Element<String>> elementsTwo   = subscriberTwo.receive(2).get(1, TimeUnit.MINUTES);
            List<Element<String>> elementsThree = subscriberThree.receive(3).get(1, TimeUnit.MINUTES);

            assertThat(elementsOne.size(), is(0));
            assertThat(elementsTwo.size(), is(0));
            assertThat(elementsThree.size(), is(0));
            }
        }

    @Test
    public void shouldRejectUnownedCommits() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("test"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("test")))
            {
            // publish to all channels so that both subscriber get something
            try (Publisher<String> publisher = topic.createPublisher(OrderBy.roundRobin()))
                {
                for (int i = 0; i < topic.getChannelCount(); i++)
                    {
                    publisher.publish("element-" + i).get(1, TimeUnit.MINUTES);
                    }
                }

            Element<String> element1 = subscriber1.receive().get(2, TimeUnit.MINUTES);
            Element<String> element2 = subscriber2.receive().get(2, TimeUnit.MINUTES);

            Subscriber.CommitResult resultFoo = subscriber1.commit(element2.getChannel(), element2.getPosition());
            Subscriber.CommitResult resultBar = subscriber2.commit(element1.getChannel(), element1.getPosition());

            assertThat(resultFoo, is(notNullValue()));
            assertThat(resultBar.getStatus(), is(Subscriber.CommitResultStatus.Rejected));
            assertThat(resultFoo, is(notNullValue()));
            assertThat(resultBar.getStatus(), is(Subscriber.CommitResultStatus.Rejected));
            }
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void shouldGetCommits() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        int                cChannel = topic.getChannelCount();

        List<Subscriber<String>> listSubscriber = new ArrayList<>();
        try
            {
            for (int i = 0; i < cChannel; i++)
                {
                listSubscriber.add(topic.createSubscriber(inGroup("test-commits"), Subscriber.CompleteOnEmpty.enabled()));
                }

            long count = listSubscriber.stream()
                    .filter(s -> s.getChannels().length == 0)
                    .count();

            assertThat("not all subscribers have channels", count, is(not(0)));

            // no commits yet - should all return NULL_POSITION
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> mapCommit = subscriber.getLastCommitted();
                int                    nChannel  = subscriber.getChannels()[0];
                assertThat(mapCommit, is(notNullValue()));
                assertThat(mapCommit.size(), is(1));
                assertThat(mapCommit.get(nChannel), is(PagedPosition.NULL_POSITION));
                }

            // add data
            int cMsgPerChannel = 100;
            populateAllChannels(topic, cMsgPerChannel);

            // still no commits yet - should all return NULL_POSITION
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> mapCommit = subscriber.getLastCommitted();
                int                    nChannel  = subscriber.getChannels()[0];
                assertThat(mapCommit, is(notNullValue()));
                assertThat(mapCommit.size(), is(1));
                assertThat(mapCommit.get(nChannel), is(PagedPosition.NULL_POSITION));
                }

            // Randomly poll and commit
            Map<Integer, Position> mapActualCommit = new HashMap<>();
            Random                 random          = new Random(System.currentTimeMillis());

            for (Subscriber<String> subscriber : listSubscriber)
                {
                int cMax  = cMsgPerChannel / 2;
                int cPoll = random.nextInt(cMax) + 1;
                for (int i = 0; i < cPoll; i++)
                    {
                    Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
                    assertThat(element, is(notNullValue()));
                    Subscriber.CommitResult result = element.commit();
                    assertThat(result.isSuccess(), is(true));
                    mapActualCommit.put(result.getChannel().getAsInt(), result.getPosition().get());
                    }
                }

            // should have commits
            Map<Integer, Position> mapCommit = new HashMap<>();
            for (Subscriber<String> subscriber : listSubscriber)
                {
                mapCommit.putAll(subscriber.getLastCommitted());
                }
            assertThat(mapCommit, is(notNullValue()));
            assertThat(mapCommit.size(), is(cChannel));
            assertThat(mapCommit, is(mapActualCommit));
            }
        finally
            {
            listSubscriber.forEach(Subscriber::close);
            }
        }

    @Test
    public void shouldBeCommitted() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriberGrouped = topic.createSubscriber(inGroup("test"));
             Subscriber<String> subscriberAnon    = topic.createSubscriber();
             Publisher<String>  publisher         = topic.createPublisher())
            {
            Status   status   = publisher.publish("value").get(2, TimeUnit.MINUTES);
            int      nChannel = status.getChannel();
            Position position = status.getPosition();

            assertThat(isCommitted(subscriberGrouped, nChannel, position), is(false));
            assertThat(isCommitted(subscriberAnon, nChannel, position), is(false));

            Element<String> elementGrouped = subscriberGrouped.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementGrouped, is(notNullValue()));
            assertThat(elementGrouped.getValue(), is("value"));
            assertThat(elementGrouped.getChannel(), is(nChannel));
            assertThat(elementGrouped.getPosition(), is(position));

            assertThat(isCommitted(subscriberGrouped, nChannel, position), is(false));

            Element<String> elementAnon = subscriberAnon.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementAnon, is(notNullValue()));
            assertThat(elementAnon.getValue(), is("value"));
            assertThat(elementAnon.getChannel(), is(nChannel));
            assertThat(elementAnon.getPosition(), is(position));

            assertThat(isCommitted(subscriberAnon, nChannel, position), is(false));

            assertThat(elementGrouped.commit().isSuccess(), is(true));
            assertThat(isCommitted(subscriberGrouped, nChannel, position), is(true));
            assertThat(isCommitted(subscriberAnon, nChannel, position), is(false));

            assertThat(elementAnon.commit().isSuccess(), is(true));
            assertThat(isCommitted(subscriberAnon, nChannel, position), is(true));
            assertThat(isCommitted(subscriberGrouped, nChannel, position), is(true));
            }
        }

    @Test
    public void shouldManuallyCommit() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber();
             Publisher<String>  publisher  = topic.createPublisher())
            {
            publisher.publish("value").get(2, TimeUnit.MINUTES);

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(isCommitted(subscriber, element.getChannel(), element.getPosition()), is(false));
            Subscriber.CommitResult result = element.commit();
            assertThat(result, is(notNullValue()));
            assertThat(result.getStatus(), is(Subscriber.CommitResultStatus.Committed));
            }
        }

    @Test
    public void shouldManuallyCommitGroupSubscriberByDefault() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("test"));
             Publisher<String>  publisher  = topic.createPublisher())
            {
            publisher.publish("value").get(2, TimeUnit.MINUTES);

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(isCommitted(subscriber, element.getChannel(), element.getPosition()), is(false));
            Subscriber.CommitResult result = element.commit();
            assertThat(result, is(notNullValue()));
            assertThat(result.getStatus(), is(Subscriber.CommitResultStatus.Committed));
            }
        }

    @Test
    public void shouldNotStoreDataWhenNoSubscribersAndNotRewindable() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sName  = topic.getName();
        PagedTopicCaches   caches = new PagedTopicCaches(sName, (CacheService) topic.getService());

        // must not be rewindable
        PagedTopic.Dependencies dependencies = getDependencies(topic);
        assertThat(dependencies.isRetainConsumed(), is(false));

        // should have no pages or data
        assertThat(caches.Pages.size(), is(0));
        assertThat(caches.Data.size(), is(0));

        try(Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 1000; i ++)
                {
                publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            }

        // should have no pages or data
        assertThat(caches.Pages.size(), is(0));
        assertThat(caches.Data.size(), is(0));
        }

    @Test
    public void shouldRemovePagesAfterAnonymousSubscribersHaveCommitted() throws Exception
        {
        NamedTopic<String>      topic        = ensureTopic();
        String                  sName        = topic.getName();
        CacheService            service      = (CacheService) topic.getService();
        PagedTopicCaches        caches       = new PagedTopicCaches(sName, service);
        PagedTopic.Dependencies dependencies = getDependencies(topic);
        int                     cbPageSize   = dependencies.getPageCapacity();
        int                     cMessage     = cbPageSize * caches.getPartitionCount() * 3; // publish enough to have multiple pages per partition

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(100));

        // should have no pages or data
        assertThat(caches.Pages.size(), is(0));
        assertThat(caches.Data.size(), is(0));

        try (Subscriber<String> subscriberOne = topic.createSubscriber(CompleteOnEmpty.enabled());
             Subscriber<String> subscriberTwo = topic.createSubscriber(CompleteOnEmpty.enabled());
             Publisher<String> publisher = topic.createPublisher(OrderBy.id(0)))
            {
            for (int i = 0; i < cMessage; i++)
                {
                publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // read the first element with subscriber one
            Element<String> elementOne  = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            PagedPosition   positionOne = (PagedPosition) elementOne.getPosition();
            int             nChannel    = elementOne.getChannel();
            Page            page        = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            int             nTail       = page.getTail();

            // The reference count for the page should be the same as the number of subscribers
            assertThat(page.getReferenceCount(), is(2));

            // the next page should have a reference count of one
            long lPageNext = page.getNextPartitionPage();
            Page pageNext  = caches.Pages.get(new Page.Key(nChannel, lPageNext));
            assertThat(pageNext.getReferenceCount(), is(1));

            // read the tail of the first page
            while (positionOne.getOffset() < nTail)
                {
                elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
                positionOne = (PagedPosition) elementOne.getPosition();
                }

            // Commit the element, which is the the tail of the first page so we have fully read it
            // this should remove subscriber one's ref count but not remove the page
            assertThat(elementOne.commit().isSuccess(), is(true));

            // The commit should NOT have removed the page (should have a ref count of one)
            page = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            assertThat(page, is(notNullValue()));
            assertThat(page.getReferenceCount(), is(1));

            // the next page should now have a reference count of two
            pageNext  = caches.Pages.get(new Page.Key(nChannel, lPageNext));
            assertThat(pageNext.getReferenceCount(), is(2));

            // read the first page with subscriber two
            Element<String> elementTwo  = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            PagedPosition   positionTwo = (PagedPosition) elementTwo.getPosition();
            while (positionTwo.getOffset() < nTail)
                {
                elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                positionTwo = (PagedPosition) elementTwo.getPosition();
                }

            // Commit the element, which is the the tail of the first page so we have fully read it
            // this should remove subscriber two's ref count and so remove the page
            assertThat(elementTwo.commit().isSuccess(), is(true));

            // The commit should have removed the page
            assertThat(caches.Pages.get(new Page.Key(nChannel, positionOne.getPage())), is(nullValue()));

            // the next page should still have a reference count of two
            pageNext  = caches.Pages.get(new Page.Key(nChannel, lPageNext));
            assertThat(pageNext.getReferenceCount(), is(2));

            // read half the topic with both subscribers, tracking the pages read
            SortedSet<Long> setPages = new TreeSet<>();
            for (int i = 0; i < (cMessage / 2); i++)
                {
                elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
                positionOne = (PagedPosition) elementOne.getPosition();
                elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                setPages.add(positionOne.getPage());
                }

            // check we have NOT read the whole of the last page
            page  = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            nTail = page.getTail();
            if (positionOne.getOffset() == nTail)
                {
                // we have just read a tail element, so read another so we're into the next page
                elementOne  = subscriberOne.receive().get(2, TimeUnit.MINUTES);
                positionOne = (PagedPosition) elementOne.getPosition();
                elementTwo  = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                setPages.add(positionOne.getPage());
                }

            // The element is now NOT at a page tail
            long lPageLast = setPages.last();
            setPages.remove(lPageLast);

            // the head page is pageNext and should still have a ref count of 2
            pageNext  = caches.Pages.get(new Page.Key(nChannel, lPageNext));
            assertThat(pageNext.getReferenceCount(), is(2));

            // the page we're about to commit should still have a ref count of 1
            page = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            assertThat(page.getReferenceCount(), is(1));

            
            // Now do the commit with subscriber one, we should not remove any pages
            int cPage = caches.Pages.size();
            assertThat(elementOne.commit().isSuccess(), is(true));
            assertThat(caches.Pages.size(), is(cPage));

            // the head page (pageNext) should now have a ref count of 1
            pageNext  = caches.Pages.get(new Page.Key(nChannel, lPageNext));
            assertThat(pageNext.getReferenceCount(), is(1));

            // the committed page should now have a ref count of 2
            page = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            assertThat(page.getReferenceCount(), is(2));

            // Now do the commit with subscriber two
            assertThat(elementTwo.commit().isSuccess(), is(true));

            // the committed page should still have a ref count of 2
            page = caches.Pages.get(new Page.Key(nChannel, positionOne.getPage()));
            assertThat(page.getReferenceCount(), is(2));

            // none of the pages before the committed page should exist (i.e. all the pages we read and stored in the set)
            for (long lPage : setPages)
                {
                assertThat(caches.Pages.get(new Page.Key(nChannel, lPage)), is(nullValue()));
                }
            }
        }

    @Test
    public void shouldRemovePagesAsTheyAreCommitted() throws Exception
        {
        NamedTopic<String>      topic        = ensureTopic(m_sSerializer + "-page-test");
        String                  sName        = topic.getName();
        CacheService            service      = (CacheService) topic.getService();
        PagedTopicCaches        caches       = new PagedTopicCaches(sName, service);
        int                     cChannel     = caches.getChannelCount();
        PagedTopic.Dependencies dependencies = caches.getDependencies();
        int                     cbPageSize   = dependencies.getPageCapacity();
        int                     cMessage     = 500;

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(100));

        Assume.assumeThat("Test only applies if paged-topic-scheme is not set to retain pages",
            dependencies.isRetainConsumed(), is(false));

        // should not have any existing subscriptions
        assertThat(caches.Subscriptions.isEmpty(), is(true));

        try (Subscriber<String> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled());
             Publisher<String>  publisher  = topic.createPublisher(OrderBy.id(0)))
            {
            for (int i = 0; i < cMessage; i++)
                {
                publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            int cPage = caches.Pages.size();

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            while (element != null)
                {
                element.commit();
                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                }

            Eventually.assertDeferred("Started with " + cPage + " pages and expected to have " + cChannel,
                                      caches.Pages::size, is(cChannel));
            }
        }

    @Test
    public void shouldGetHeadsAndTails() throws Exception
        {
        NamedTopic<String>       topic          = ensureTopic();
        int                      cChannel       = topic.getChannelCount();
        List<Subscriber<String>> listSubscriber = new ArrayList<>();

        try
            {
            for (int i = 0; i < cChannel; i++)
                {
                listSubscriber.add(topic.createSubscriber(inGroup("test-commits"), Subscriber.CompleteOnEmpty.enabled()));
                }

            // Channel allocation is async so we need to wait until all subscribers have one channel and
            // all channels are allocated to a subscriber
            Eventually.assertDeferred(() -> subscribersHaveDistinctChannels(listSubscriber), is(true));

            Map<Integer, Position> mapHeadsStart = new HashMap<>();
            for (Subscriber<String> subscriber : listSubscriber)
                {
                int                    nChannel = subscriber.getChannels()[0];
                Map<Integer, Position> map      = subscriber.getHeads();
                assertThat("Heads already contains " + nChannel, mapHeadsStart.containsKey(nChannel), is(false));
                assertThat(map, is(notNullValue()));
                assertThat(map.size(), is(1));
                Position position = map.get(nChannel);
                assertThat(position, is(notNullValue()));
                mapHeadsStart.put(nChannel, position);
                }

            // should have a head for each channel
            assertThat(mapHeadsStart.size(), is(cChannel));

            // no tails yet
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> map      = subscriber.getTails();
                int                    nChannel = subscriber.getChannels()[0];
                assertThat(map, is(notNullValue()));
                assertThat(map.size(), is(1));
                assertThat(map.get(nChannel), is(mapHeadsStart.get(nChannel)));
                }

            // add data
            int                      cMsgPerChannel = 100;
            Map<Integer, Position[]> mapHeadTail    = populateAllChannels(topic, cMsgPerChannel);
            Map<Integer, Position>   mapActualHeads = mapHeadTail.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
            Map<Integer, Position>   mapActualTails = mapHeadTail.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[1]));

            // should have no heads until first poll
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> map      = subscriber.getHeads();
                int                    nChannel = subscriber.getChannels()[0];
                assertThat(map, is(notNullValue()));
                assertThat(map.size(), is(1));
                assertThat(map.get(nChannel), is(mapHeadsStart.get(nChannel)));
                }

            // should have tails
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> map      = subscriber.getTails();
                int                    nChannel = subscriber.getChannels()[0];
                assertThat(map, is(notNullValue()));
                assertThat(map.size(), is(1));
                assertThat(map.get(nChannel), is(mapActualTails.get(nChannel)));
                }

            // read some messages
            Random random = new Random(System.currentTimeMillis());
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Subscriber.Element<String> element = null;
                int                        cPoll   = random.nextInt(cMsgPerChannel / 2) + 1;
                for (int i = 0; i < cPoll; i++)
                    {
                    element = subscriber.receive().get(2, TimeUnit.MINUTES);
                    }
                assertThat(element, is(notNullValue()));
                element.commit();
                }

            // should have heads greater than initial heads
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> map      = subscriber.getHeads();
                int                    nChannel = subscriber.getChannels()[0];
                assertThat(map, is(notNullValue()));
                assertThat(map.size(), is(1));
                assertThat(map.get(nChannel).compareTo(mapActualHeads.get(nChannel)), is(greaterThan(0)));
                }
            }
        finally
            {
            listSubscriber.forEach(Subscriber::close);
            }
        }

    protected boolean subscribersHaveDistinctChannels(List<Subscriber<String>> listSubscriber)
        {
        Map<Integer, Integer> mapChannel = new HashMap<>();
        for (Subscriber<String> subscriber : listSubscriber)
            {
            int[] aChannel = subscriber.getChannels();
            for (int nChannel : aChannel)
                {
                mapChannel.compute(nChannel, (k, v) -> v == null ? 1 : v + 1);
                }
            }
        return mapChannel.values().stream().noneMatch(v -> v != 1);
        }

    protected Map<Integer, Position[]> populateAllChannels(NamedTopic<String> topic, int cMsgPerChannel) throws Exception
        {
        int             cChannel = topic.getChannelCount();

        Map<Integer,Position[]> mapHeadTail = new HashMap<>();
        try (Publisher<String> publisher = topic.createPublisher(OrderBy.roundRobin()))
            {
            for (int i = 0; i < cMsgPerChannel; i++)
                {
                for (int c = 0; c < cChannel; c++)
                    {
                    Status status = publisher.publish("element-" + c + "-" + i).get(2, TimeUnit.MINUTES);
                    if (i == 0)
                        {
                        mapHeadTail.put(status.getChannel(), new Position[]{status.getPosition(), null});
                        }
                    else
                        {
                        mapHeadTail.get(status.getChannel())[1] = status.getPosition();
                        }
                    }
                }
            }

        return mapHeadTail;
        }

    @Test
    public void shouldPublishAndPollUsingJavaSerializationOnly() throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-1");

        try (Publisher<Customer>  publisher     = topic.createPublisher();
             Subscriber<Customer> subscriberFoo = topic.createSubscriber(inGroup("Foo"));
             Subscriber<Customer> subscriberBar = topic.createSubscriber(inGroup("Bar")))
            {
            Customer customer = new Customer("Mr Smith", 25, AddressExternalizableLite.getRandomAddress());
            try
                {
                publisher.publish(customer).join();
                assertThat(subscriberFoo.receive().get(1, TimeUnit.MINUTES).getValue().equals(customer), is(true));
                assertThat(subscriberBar.receive().get(1, TimeUnit.MINUTES).getValue().equals(customer), is(true));

                if (m_sSerializer.equals("pof"))
                    {
                    fail("serializer " + m_sSerializer + " must throw an IOException for this test");
                    }
                }
            catch (Exception e)
                {
                if (m_sSerializer.equals("java"))
                    {
                    throw e;
                    }
                assertThat(IOException.class.isAssignableFrom(e.getCause().getClass()), is(true));
                // ignore IOException for pof serialization, it is expected due to payload is only Serializable.
                }
            }
        }

    @Test
    public void shouldOfferAndPollUsingAppropriateSerializer()
        throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-2");

        try (Publisher<Customer>  publisher     = topic.createPublisher();
             Subscriber<Customer> subscriberFoo = topic.createSubscriber(inGroup("Foo"));
             Subscriber<Customer> subscriberBar = topic.createSubscriber(inGroup("Bar")))
            {
            Customer customer = m_sSerializer.equals("pof") ?
                    new CustomerPof("Mr Smith", 25, AddressPof.getRandomAddress()) :
                    new CustomerExternalizableLite("Mr Smith", 25, AddressExternalizableLite.getRandomAddress());
            assertThat(publisher.publish(customer).get(1, TimeUnit.MINUTES), is(Matchers.notNullValue()));
            assertThat(subscriberFoo.receive().get(1, TimeUnit.MINUTES).getValue().equals(customer), is(true));
            assertThat(subscriberBar.receive().get(1, TimeUnit.MINUTES).getValue().equals(customer), is(true));
            }
        }

    @Test
    public void shouldFilterUsingAppropriateSerializer()
        throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-3");

        try (Publisher<Customer>  publisher     = topic.createPublisher();
             Subscriber<Customer> subscriberD   = topic.createSubscriber(CompleteOnEmpty.enabled(),
                    Subscriber.Filtered.by(new GreaterEqualsFilter<>(new UniversalExtractor<>("id"), 12)));
             Subscriber<Customer> subscriberA   = topic.createSubscriber(CompleteOnEmpty.enabled(),
                    Subscriber.Filtered.by(new LessFilter<>(new UniversalExtractor<>("id"), 12))))
            {
            List<Customer> list = new ArrayList<>();

            for (int i = 0; i < 25; i++)
                {
                Customer customer = m_sSerializer.equals("pof") ?
                        new CustomerPof("Mr Smith " + i, i, AddressPof.getRandomAddress()) :
                        new CustomerExternalizableLite("Mr Smith " + i, i, AddressExternalizableLite.getRandomAddress());
                list.add(customer);
                }

            for (Customer cust : list)
                {
                assertThat(publisher.publish(cust).get(1, TimeUnit.MINUTES), is(notNullValue()));
                }

            int cSubscriberDReceive = 0;
            for (CompletableFuture<Element<Customer>> future = subscriberD.receive(); future.get(1, TimeUnit.MINUTES) != null ; future = subscriberD.receive(), cSubscriberDReceive++)
                {
                Customer customer = future.get(1, TimeUnit.MINUTES).getValue();
                assertThat(customer.getId(), greaterThanOrEqualTo(12));
                }
            assertThat(cSubscriberDReceive, is(13));

            int cSubscriberAReceive = 0;
            for (CompletableFuture<Element<Customer>> future = subscriberA.receive(); future.get(1, TimeUnit.MINUTES) != null ; future = subscriberA.receive(), cSubscriberAReceive++)
                {
                Customer customer = future.get(1, TimeUnit.MINUTES).getValue();
                assertThat(customer.getId(), lessThan(12));
                }
            assertThat(cSubscriberAReceive, is(12));
            }
        }

    @Test
    public void shouldNotAllowSubscribersToChangeGroupFilter()
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-4");

        try (@SuppressWarnings("unused")
             Subscriber<Customer> subscriber = topic.createSubscriber(inGroup("durableSubscriber"), CompleteOnEmpty.enabled(),
                        Subscriber.Filtered.by(new GreaterEqualsFilter<>(new UniversalExtractor<>("id"), 12))))
            {
            assertThrows(TopicException.class, () ->
                    topic.createSubscriber(inGroup("durableSubscriber"), CompleteOnEmpty.enabled(),
                               Subscriber.Filtered.by(new LessFilter<>(new UniversalExtractor<>("id"), 12))));
            }
        }

    @Test
    public void shouldConvertUsingAppropriateSerializer() throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-5");

        try (Publisher<Customer>  publisher           = topic.createPublisher();
             Subscriber<Integer>  subscriberOfId      = topic.createSubscriber(Subscriber.Convert.using(Customer::getId));
             Subscriber<String>   subscriberOfName    = topic.createSubscriber(Subscriber.Convert.using(Customer::getName));
             Subscriber<Address>  subscriberOfAddress = topic.createSubscriber(Subscriber.Convert.using(Customer::getAddress)))
            {
            assertThat(subscriberOfId.getChannels().length, is(not(0)));
            assertThat(subscriberOfAddress.getChannels().length, is(not(0)));
            assertThat(subscriberOfName.getChannels().length, is(not(0)));

            List<Customer> list = new ArrayList<>();

            for (int i = 1; i < 6; i++)
                {
                Customer customer = m_sSerializer.equals("pof") ?
                        new CustomerPof("Mr Smith " + i, i, AddressPof.getRandomAddress()) :
                        new CustomerExternalizableLite("Mr Smith " + i, i, AddressExternalizableLite.getRandomAddress());
                list.add(customer);
                }

            for (Customer cust : list)
                {
                publisher.publish(cust).get(2, TimeUnit.MINUTES);
                }

            assertThat(subscriberOfId.receive().get(2, TimeUnit.MINUTES).getValue(), is(1));
            assertThat(subscriberOfId.receive().get(2, TimeUnit.MINUTES).getValue(), is(2));
            assertThat(subscriberOfId.receive().get(2, TimeUnit.MINUTES).getValue(), is(3));
            assertThat(subscriberOfId.receive().get(2, TimeUnit.MINUTES).getValue(), is(4));
            assertThat(subscriberOfId.receive().get(2, TimeUnit.MINUTES).getValue(), is(5));

            assertThat(subscriberOfName.receive().get(2, TimeUnit.MINUTES).getValue(), is("Mr Smith 1"));
            assertThat(subscriberOfName.receive().get(2, TimeUnit.MINUTES).getValue(), is("Mr Smith 2"));
            assertThat(subscriberOfName.receive().get(2, TimeUnit.MINUTES).getValue(), is("Mr Smith 3"));
            assertThat(subscriberOfName.receive().get(2, TimeUnit.MINUTES).getValue(), is("Mr Smith 4"));
            assertThat(subscriberOfName.receive().get(2, TimeUnit.MINUTES).getValue(), is("Mr Smith 5"));

            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), isOneOf(Address.arrAddress));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), isOneOf(Address.arrAddress));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), isOneOf(Address.arrAddress));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), isOneOf(Address.arrAddress));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), isOneOf(Address.arrAddress));
            }
        }

    @Test
    public void shouldNotAllowSubscribersToChangeGroupConverter()
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-6");

        try (@SuppressWarnings("unused")
             Subscriber<Serializable> subscriber = topic.createSubscriber(inGroup("durable-subscriber-3"),
                        Subscriber.Convert.using(Customer::getAddress)))
            {
            assertThrows(TopicException.class, () ->
                    topic.createSubscriber(inGroup("durable-subscriber-3"), Subscriber.Convert.using(Customer::getId)));
            }
        }

    @Test
    public void shouldFilterAndConvert()
        throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-7");

        try (Publisher<Customer>  publisher          = topic.createPublisher();
             Subscriber<Customer> subscriberMA       = topic.createSubscriber(CompleteOnEmpty.enabled(),
                    Subscriber.Filtered.by(new EqualsFilter<>(new ChainedExtractor<>("getAddress.getState"), "MA")));
            Subscriber<Address> subscriberMAAddress = topic.createSubscriber(Subscriber.Convert.using(Customer::getAddress),
                CompleteOnEmpty.enabled(),
                Subscriber.Filtered.by(new EqualsFilter(new ChainedExtractor("getAddress.getState"), "MA"))))
            {
            ValueExtractor extractor                   = m_sSerializer.equals("pof") ?
                    new PofExtractor(String.class, new SimplePofPath(new int[] {CustomerPof.ADDRESS, AddressPof.STATE})) :
                    new ChainedExtractor(new UniversalExtractor("address"), new UniversalExtractor("state"));

            Subscriber<Customer> subscriberCA          = topic.createSubscriber(CompleteOnEmpty.enabled(),
                                                                                Subscriber.Filtered.by(new EqualsFilter<>(extractor, "CA")));

            List<Customer> list = new ArrayList<>();
            for (int i = 0; i < 25; i++)
                {
                Customer customer = m_sSerializer.equals("pof") ?
                        new CustomerPof("Mr Smith " + i, i, AddressPof.getRandomAddress()) :
                        new CustomerExternalizableLite("Mr Smith " + i, i, AddressExternalizableLite.getRandomAddress());
                list.add(customer);
                }

            int cMA = 0;
            int cCA = 0;
            for (Customer customer : list)
                {
                publisher.publish(customer).join();

                if (customer.getAddress().getState().equals("MA"))
                    {
                    cMA++;
                    }
                else if (customer.getAddress().getState().equals("CA"))
                    {
                    cCA++;
                    }
                }

            int cSubscriberMAReceive = 0;
            for (CompletableFuture<Element<Customer>> future = subscriberMA.receive(); future.get(1, TimeUnit.MINUTES) != null ; future = subscriberMA.receive(), cSubscriberMAReceive++)
                {
                Customer customer = future.get(1, TimeUnit.MINUTES).getValue();
                assertThat(customer.getAddress().getState(), is("MA"));
                }
            assertThat("number of customers in MA", cSubscriberMAReceive, is(cMA));

            int cSubscriberCAReceive = 0;
            for (CompletableFuture<Element<Customer>> future = subscriberCA.receive(); future.get(1, TimeUnit.MINUTES) != null ; future = subscriberCA.receive(), cSubscriberCAReceive++)
                {
                Customer customer = future.get(1, TimeUnit.MINUTES).getValue();
                assertThat(customer.getAddress().getState(), is("CA"));
                }
            assertThat("number of customers in CA", cSubscriberCAReceive, is(cCA));

            int cSubscriberMAAddress = 0;
            for (CompletableFuture<Element<Address>> future = subscriberMAAddress.receive(); future.get(1, TimeUnit.MINUTES) != null ; future = subscriberMAAddress.receive(), cSubscriberMAAddress++)
                {
                Address address = future.get(1, TimeUnit.MINUTES).getValue();
                assertThat(address.getState(), is("MA"));
                }
            assertThat("number of customer addresses in MA", cSubscriberMAAddress, is(cMA));
            }
        }

    @Test
    public void shouldOfferAndPollAcrossPages()
          throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriberA  = topic.createSubscriber();
             Subscriber<String> subscriberB  = topic.createSubscriber())
            {
            DistributedCacheService service      = (DistributedCacheService) topic.getService();
            PagedTopic.Dependencies dependencies = getDependencies(topic);
            int                     cbPageSize   = dependencies.getPageCapacity();

            Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
                              cbPageSize, lessThanOrEqualTo(100));

            PagedTopicCaches caches   = new PagedTopicCaches(topic.getName(), service);
            int              cPart    = service.getPartitionCount();
            int              cChan    = caches.getChannelCount();
            int              cRecords = cbPageSize * cPart; // ensure we use every partition a few times

            assertThat(caches.Pages.size(), is(0)); // subs waiting on Usage rather then page

            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i = 0; i < cRecords; ++i)
                    {
                    publisher.publish(Integer.toString(i));
                    }
                publisher.flush().join();
                }

            int cPages = caches.Pages.size();
            assertThat(cPages, greaterThan(cPart * 3)); // we have partitions which have chains of pages

            // There should be one page per partition with a reference count of two
            ValueExtractor<Page, Integer>  extractor = Page::getReferenceCount;
            Set<Map.Entry<Page.Key, Page>> setPage   = caches.Pages.entrySet(new EqualsFilter(extractor, 2));

            assertThat(setPage.size(), is(cPart));
            // none of the entries should have a previous page (i.e. they are the first page in each partition)
            assertThat(setPage.stream().allMatch(entry -> entry.getValue().getPreviousPartitionPage() == Page.NULL_PAGE), is(true));

            // Read all of the elements with subscriber A
            Element<String> elementA = null;
            int             i        = 0;

            // read half the records
            for ( ; i < (cRecords / 2); ++i)
                {
                elementA = subscriberA.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementA.getValue(), is(Integer.toString(i)));
                }

            // cPages populated pages, subA waits on last page plus cChan empty pages in each channel, also waited by subA
            int cExpected = cPages + cChan - 1;
            assertThat(caches.Pages.size(), is(cExpected));

            // commit A
            assertThat(elementA, is(notNullValue()));
            elementA.commit();
            // should not have removed anything
            assertThat(caches.Pages.size(), is(cExpected));

            // read the remaining records
            for ( ; i < cRecords; ++i)
                {
                elementA = subscriberA.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementA.getValue(), is(Integer.toString(i)));
                }

            // commit A
            elementA.commit();
            // should not have removed anything
            assertThat(caches.Pages.size(), is(cExpected));

            // Read all of the elements with subscriber B
            Element<String> elementB = null;
            for (int j = 0; j < cRecords; ++j)
                {
                elementB = subscriberB.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementB.getValue(), is(Integer.toString(j)));
                }

            // commit B
            assertThat(elementB, is(notNullValue()));
            elementB.commit();

            assertThat(caches.Pages.size(), is(cChan));  // all empty pages, waited on by subA and subB
            }
        }

    @Test
    public void shouldOfferAsyncAndMaintainOrder() throws Exception
        {
        NamedTopic<String>  topic     = ensureTopic();
        String              sPrefix   = "Element-";
        int                 cOffers   = 100;
        CompletableFuture[] aFutures  = new CompletableFuture[cOffers];

        try (@SuppressWarnings("unused") Subscriber<String>  subscriber = topic.createSubscriber()) // ensure published data is retained
            {
            try(Publisher<String> publisher = topic.createPublisher())
                {
                for (int i=0; i<cOffers; i++)
                    {
                    String sElement = sPrefix + i;
                    aFutures[i] = publisher.publish(sElement);
                    }

                CompletableFuture.allOf(aFutures).get(1, TimeUnit.MINUTES);

                TopicAssertions.assertPublishedOrder(topic, cOffers, sPrefix);
                }
            }
        }

    @Test
    public void shouldPublishInParallel()
            throws Exception
        {
        NamedTopic<String> topic      = ensureTopic();
        int                nCount     = 300;
        String             sPrefix    = "Element-";
        TopicPublisher     publisher1 = new TopicPublisher(topic, "1-" + sPrefix, nCount, true);
        TopicPublisher     publisher2 = new TopicPublisher(topic, "2-" + sPrefix, nCount, true);

        try (@SuppressWarnings("unused")Subscriber<String> subscriberPin = topic.createSubscriber())
            {
            Future<?> futurePub1 = m_executorService.submit(publisher1);
            Future<?> futurePub2 = m_executorService.submit(publisher2);

            futurePub1.get(1, TimeUnit.MINUTES);
            futurePub2.get(1, TimeUnit.MINUTES);

            assertThat(publisher1.getPublished(), is(nCount));
            assertThat(publisher2.getPublished(), is(nCount));

            TopicAssertions.assertPublishedOrder(topic, nCount, "1-Element-", "2-Element-");
            }
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConsumeAsynchronously() throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 99;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);

        try (@SuppressWarnings("unused") Subscriber<String> subscriberPin = topic.createSubscriber();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.Name.inGroup("Foo")))
            {
            publisher.run();

            assertThat(publisher.getPublished(), is(nCount));
            TopicAssertions.assertPublishedOrder(topic, nCount, sPrefix);


            CompletableFuture<Subscriber.Element<String>>[] aFutures = new CompletableFuture[nCount];

            for (int i=0; i<aFutures.length; i++)
                {
                aFutures[i] = subscriber.receive();
                }

            CompletableFuture.allOf(aFutures).join();

            for (int i=0; i<aFutures.length; i++)
                {
                Subscriber.Element<String> element = aFutures[i].get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is(sPrefix + i));
                }
            }
        }

    @Test
    public void shouldConsumeInParallel() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        int                nCount    = 1000;
        String             sPrefix   = "Element-";
        TopicPublisher     publisher = new TopicPublisher(topic, sPrefix, nCount, true);

        try (Subscriber<String> subscriber1 = topic.createSubscriber();
             Subscriber<String> subscriber2 = topic.createSubscriber())
            {
            TopicSubscriber    topicSubscriber1 = new TopicSubscriber(subscriber1, sPrefix, nCount, 3, TimeUnit.MINUTES, true);
            TopicSubscriber    topicSubscriber2 = new TopicSubscriber(subscriber2, sPrefix, nCount, 3, TimeUnit.MINUTES, true);

            publisher.run();

            assertThat(publisher.getPublished(), is(nCount));
            TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

            Future<?> futureSubscriber1 = m_executorService.submit(topicSubscriber1);
            Future<?> futureSubscriber2 = m_executorService.submit(topicSubscriber2);

            futureSubscriber1.get(2, TimeUnit.MINUTES);
            futureSubscriber2.get(2, TimeUnit.MINUTES);

            assertThat(topicSubscriber1.getConsumedCount(), is(nCount));
            assertThat(topicSubscriber2.getConsumedCount(), is(nCount));
            }
        }

    @Test
    public void validateDestroyOfConfiguredSubscriberGroup()
        {
        String sTopicName = m_sSerializer + "-subscriber-group-1";
        Assume.assumeThat("Test only applies if topic-mapping has a subscriber-group configured",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic(sTopicName);
        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"durable-subscriber"}).getSet()));

        // Publish
        int            nCount    = 1000;
        String         sPrefix   = "Element-";
        TopicPublisher publisher = new TopicPublisher(topic, sPrefix, nCount, true);

        publisher.run();
        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        // Subscribe: validate topic-mapping configured subscriber-group "durable-subscriber"
        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("durable-subscriber")))
            {
            @SuppressWarnings("unused")
            TopicSubscriber topicSubscriber = new TopicSubscriber(subscriber, sPrefix, nCount, 3, TimeUnit.MINUTES, true);

            topic.destroySubscriberGroup("durable-subscriber");

            CacheService service          = (CacheService) topic.getService();
            NamedCache   cacheSubscribers = service.ensureCache(PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName), null);

            Eventually.assertThat(cacheSubscribers.getCacheName() + " should be empty",
                                  invoking(cacheSubscribers).isEmpty(), is(true));

            assertThat(topic.getSubscriberGroups().size(), is(0));
            }

        // explicitly destroy topic with statically configured subscriber group that was destroyed.
        topic.destroy();
        m_topic = null;

        // validate that statically configured subscriber group is configured when topic is recreated.
        topic = ensureTopic(sTopicName);
        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"durable-subscriber"}).getSet()));
        }

    @Test
    public void shouldShareAndDuplicateConsumption() throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 1000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("Foo"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("Bar")))
            {
            TopicSubscriber topicSubscriber1 = new TopicSubscriber(subscriber1, sPrefix, nCount, 3, TimeUnit.MINUTES, true);
            TopicSubscriber topicSubscriber2 = new TopicSubscriber(subscriber2, sPrefix, nCount, 3, TimeUnit.MINUTES, true);

            publisher.run();

            assertThat(publisher.getPublished(), is(nCount));
            TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

            Future<?> futureSubscriber1 = m_executorService.submit(topicSubscriber1);
            Future<?> futureSubscriber2 = m_executorService.submit(topicSubscriber2);

            futureSubscriber1.get(1, TimeUnit.MINUTES);
            futureSubscriber2.get(1, TimeUnit.MINUTES);

            assertThat(topicSubscriber1.getConsumedCount(), is(nCount));
            assertThat(topicSubscriber2.getConsumedCount(), is(nCount));
            }
        }

    @Test
    public void shouldRetainElements() throws Exception
        {
        NamedTopic<String> topic   = ensureTopic(m_sSerializer + "-rewindable");
        String             sPrefix = "Element-";
        int                nCount  = 123;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i).get(1, TimeUnit.MINUTES);
                }
            }

        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        try (Subscriber<String> subscriber1 = topic.createSubscriber())
            {
            for (int i=0; i<nCount; i++)
                {
                assertThat(subscriber1.receive().get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + i));
                }
            }

        try (Subscriber<String> subscriber2 = topic.createSubscriber())
            {
            for (int i=0; i<nCount; i++)
                {
                assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + i));
                }
            }
        }

    @Test
    public void shouldRetainElementsForSubscriberGroup() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        String sPrefix = "Element-";
        int    nCount  = 123;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i).join();
                }
            }

        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("one")))
            {
            for (int i = 0; i<nCount; i++)
                {
                assertThat(subscriber.receive().get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + i));
                }
            }
        }

    @Test
    public void shouldStartAtBeginningForSecondSubscriberInGroupWithZeroCommits() throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        String             sPrefix     = "Element-";
        int                nCount      = 123;

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("one"));
             Publisher<String> publisher = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i).join();
                }

            // read everything with subscriber one but do not commit anything
            for (int i = 0; i<nCount; i++)
                {
                Element<String> element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + i));
                }

            // create subscriber two
            try (Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("one")))
                {
                // close subscriber one so that two gets everything
                subscriber1.close();

                // should be able to re-read everything with subscriber two as one did zero commits
                for (int i = 0; i<nCount; i++)
                    {
                    assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + i));
                    }
                }
            }
        }

    @Test
    public void shouldPublishOrderableValues() throws Exception
        {
        NamedTopic<OrderableMessage<String>>       topic          = ensureRawTopic();
        int                                        cChannel       = topic.getChannelCount();
        List<Subscriber<OrderableMessage<String>>> listSubscriber = new ArrayList<>(cChannel);

        try
            {
            // creating the same number of subscribers as channels guarantees that there will be one channel per subscriber
            for (int i = 0; i < cChannel; i ++)
                {
                listSubscriber.add(topic.createSubscriber(inGroup("test"), Subscriber.CompleteOnEmpty.enabled()));
                }

            // publish one message per channel
            try (Publisher<OrderableMessage<String>> publisher = topic.createPublisher())
                {
                for (int i = 0; i < cChannel; i ++)
                    {
                    publisher.publish(new OrderableMessage<>(i, "message-" + i));
                    }
                }

            for (Subscriber<OrderableMessage<String>> subscriber : listSubscriber)
                {
                int[] aChannel = subscriber.getChannels();
                assertThat(aChannel.length, is(1));
                Element<OrderableMessage<String>> element = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getChannel(), is(aChannel[0]));
                OrderableMessage<String> value = element.getValue();
                assertThat(value, is(notNullValue()));
                assertThat(value.getOrderId(), is(aChannel[0]));
                assertThat(value.getValue(), is("message-" + aChannel[0]));
                }
            }
        finally
            {
            listSubscriber.forEach(Subscriber::close);
            }
        }

    @Test
    public void shouldReceiveNonCommittedElementsAsSubscribersComeAndGo() throws Exception
        {
        ListLogger listLog = new ListLogger();

        NamedTopic<String> topic        = ensureTopic();
        String             sPrefix      = "Element-";
        int                nCount       = 300;
        Element<String>    element;
        int                cChannel;

        try (Subscriber<String> subscriber1  = topic.createSubscriber(inGroup("one"));
             Subscriber<String> subscriber2  = topic.createSubscriber(inGroup("one"));
             Subscriber<String> subscriber3  = topic.createSubscriber(inGroup("one")))
            {
            Map<Integer, List<String>> mapReceived = new ConcurrentHashMap<>();
            Map<Integer, List<String>> mapSent     = new ConcurrentHashMap<>();
            AtomicInteger              nOrder      = new AtomicInteger();
            int                        cMessages   = 0;

            // publish to all of the channels to ensure all subscribers have messages
            try (Publisher<String> publisher = topic.createPublisher(OrderBy.value(v -> nOrder.get())))
                {
                cChannel = publisher.getChannelCount();
                for (int c = 0; c < cChannel; c++)
                    {
                    List<String> list = new ArrayList<>();

                    // set the channel this batch will be published to
                    nOrder.set(c);

                    for (int i = 0; i < nCount; i++)
                        {
                        String sMsg = sPrefix + c + "-" + i;
                        list.add(sMsg);
                        Status metadata = publisher.publish(sMsg).get(2, TimeUnit.MINUTES);
                        listLog.add("Sent: " + sMsg + " to " + metadata.getPosition());
                        cMessages++;
                        }

                    mapSent.put(c, list);
                    mapReceived.put(c, new ArrayList<>());
                    }
                }

            Set<ChannelPosition>   setPosn   = new HashSet<>();
            Map<Integer, Position> commits1  = new HashMap<>();
            Map<Integer, Position> commits2  = new HashMap<>();
            Map<Integer, Position> commits3  = new HashMap<>();
            int                    cRecieved = 0;

            System.err.println("Subscriber 1 has channels " + Arrays.toString(subscriber1.getChannels()));
            System.err.println("Subscriber 2 has channels " + Arrays.toString(subscriber2.getChannels()));
            System.err.println("Subscriber 3 has channels " + Arrays.toString(subscriber3.getChannels()));

            // read some messages with all subscribers, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (1): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits1.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (2): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits2.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits3.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                cRecieved += 3;
                }

            // commit all commit maps
            subscriber1.commitAsync(commits1).get(1, TimeUnit.MINUTES);
            commits1.clear();
            subscriber2.commitAsync(commits2).get(1, TimeUnit.MINUTES);
            commits1.clear();
            subscriber3.commitAsync(commits3).get(1, TimeUnit.MINUTES);
            commits3.clear();

            // read some more messages with subscriber 1, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (1): (No Commit) " + element.getValue() + " from " + element.getPosition());
                }

            // close subscriber 1 - it's channels will be reallocated
            subscriber1.close();

            // Wait for the channels to all be reallocated
            // In real life we don't do this as we do not guarantee once only but we need to do
            // this for the test to be stable
            Eventually.assertDeferred(() -> subscriber2.getChannels().length, is(not(0)));
            Eventually.assertDeferred(() -> subscriber3.getChannels().length, is(is(not(0))));
            Eventually.assertDeferred(() -> subscriber2.getChannels().length + subscriber3.getChannels().length, is(cChannel));

            System.err.println("Subscriber 2 has channels " + Arrays.toString(subscriber2.getChannels()));
            System.err.println("Subscriber 3 has channels " + Arrays.toString(subscriber3.getChannels()));

            // read some messages with remaining subscribers, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (2): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits2.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits3.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                cRecieved += 2;
                }

            // commit remaining subscribers
            subscriber2.commitAsync(commits2).get(1, TimeUnit.MINUTES);
            commits2.clear();
            subscriber3.commitAsync(commits3).get(1, TimeUnit.MINUTES);
            commits3.clear();

            // read some more messages with subscriber 2, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (2): (No Commit) " + element.getValue() + " from " + element.getPosition());
                }

            // close subscriber 2 - it's channels will be reallocated
            subscriber2.close();

            // Wait for the channels to all be reallocated
            // In real life we don't do this as we do not guarantee once only but we need to do
            // this for the test to be stable
            Eventually.assertDeferred(() -> subscriber3.getChannels().length, is(cChannel));

            System.err.println("Subscriber 3 has channels " + Arrays.toString(subscriber3.getChannels()));

            // read all messages left using subscriber 3
            while (cRecieved < cMessages)
                {
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                assertThat("Duplicate " + element.getValue(), setPosn.add(new ChannelPosition(element)), is(true));
                cRecieved++;
                }

            for (int i = 0; i < mapSent.size(); i++)
                {
                if (!Objects.equals(mapReceived.get(i), mapSent.get(i)))
                    {
                    List<String> listRec  = mapReceived.get(i);
                    List<String> listSent = mapSent.get(i);
                    for (int r=0; r<listRec.size(); r++)
                        {
                        if (!Objects.equals(listRec.get(r), listSent.get(r)))
                            {
                            listLog.add("Mismatch: " + listRec.get(r) + " " + listSent.get(r));
                            break;
                            }
                        }
                    if (listSent.size() > listRec.size())
                        {
                        for (int r = listRec.size(); r < listSent.size(); r++)
                            {
                            listLog.add("Missing: " + listSent.get(r));
                            }
                        }
                    listLog.getLog().forEach(System.err::println);
                    }
                assertThat(mapReceived.get(i), is(mapSent.get(i)));
                }
            System.err.println("***** Done, closing...");
            }
        }

    @Test
    public void shouldWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Publisher<String> publisher = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            Future<Subscriber.Element<String>> future = subscriber.receive();
            assertThat(future.isDone(), is(false));

            publisher.publish("blah");

            assertThat(future.get(1, TimeUnit.MINUTES).getValue(), is("blah"));
            }
        }

    @Test
    public void shouldShareWaitNotificationOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try(Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("subscriber"));
            Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("subscriber")))
            {
            Future<Subscriber.Element<String>> future1 = subscriber1.receive();
            Future<Subscriber.Element<String>> future2 = subscriber2.receive();

            // should eventually stop polling when all owned channels have been determined to be empty
            long start     = System.currentTimeMillis();
            long polls     = ((PagedTopicSubscriber<String>) subscriber1).getPolls();
            long pollsPrev = -1;
            while (polls != pollsPrev)
                {
                long now = System.currentTimeMillis();
                assertThat("Timed out waiting for the subscriber to stop polling",
                           start - now, is(lessThan(TimeUnit.MINUTES.toMillis(2))));
                Thread.sleep(10);
                pollsPrev = polls;
                polls = ((PagedTopicSubscriber<String>) subscriber1).getPolls();
                }

            // publish to all channels so that both subscriber get something
            try (Publisher<String> publisher = topic.createPublisher(OrderBy.roundRobin()))
                {
                for (int i = 0; i < topic.getChannelCount(); i++)
                    {
                    publisher.publish("element-" + i).get(1, TimeUnit.MINUTES);
                    }
                }

            // Both subscribers should have received notifications and re-poll for messages
            future1.get(2, TimeUnit.MINUTES);
            future2.get(2, TimeUnit.MINUTES);
            }
        }

    @Test
    public void shouldDrainAndWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Publisher<String> publisher = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            publisher.publish("blah");
            assertThat(subscriber.receive().get(1, TimeUnit.MINUTES).getValue(), is("blah"));

            Future<Subscriber.Element<String>> future = subscriber.receive();
            assertThat(future.isDone(), is(false));

            publisher.publish("blah blah");

            assertThat(future.get(1, TimeUnit.MINUTES).getValue(), is("blah blah"));
            }
        }

    @Test
    @SuppressWarnings("unused")
    public void shouldListSubscriberGroups()
        {
        NamedTopic<String> topic = ensureTopic();

        assertThat(topic.getSubscriberGroups().isEmpty(), is(true));

        try (Subscriber<String> subAnon = topic.createSubscriber())
            {
            assertThat(topic.getSubscriberGroups().isEmpty(), is(true));

            try (Subscriber<String> subFoo = topic.createSubscriber(Subscriber.Name.inGroup("foo"));
                 Subscriber<String> subBar = topic.createSubscriber(Subscriber.Name.inGroup("bar")))
                {
                assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"foo", "bar"}).getSet()));

                topic.destroySubscriberGroup("foo");

                assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"bar"}).getSet()));
                }
            }
        }

    @Test
    public void shouldCancelWaitOnClose() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            Future<Subscriber.Element<String>> future = subscriber.receive();
            Thread.sleep(100); // allow subscriber to enter wait state
            subscriber.close();
            assertThat(future.isCancelled(), is(true));
            }
        }

    @Test
    public void shouldNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber"), Subscriber.CompleteOnEmpty.enabled()))
            {
            Future<Subscriber.Element<String>> future = subscriber.receive();

            assertThat(future.get(1, TimeUnit.MINUTES), is(nullValue()));
            }
        }

    @Test
    public void shouldDrainAndNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber"), Subscriber.CompleteOnEmpty.enabled()))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("blah").join();
                }

            assertThat(subscriber.receive().get(1, TimeUnit.MINUTES).getValue(), is("blah"));

            Future<Subscriber.Element<String>> future = subscriber.receive();

            assertThat(future.get(1, TimeUnit.MINUTES), is(nullValue()));
            }
        }

    @Test
    public void shouldThrottleSubscribers() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        AtomicLong         cPending = new AtomicLong();
        AtomicLong         cReceive = new AtomicLong();
        long               nHigh    = CacheFactory.getCluster().getDependencies().getPublisherCloggedCount();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            DebouncedFlowControl flowControl = (DebouncedFlowControl) subscriber.getFlowControl();
            long                 nMaxBacklog = flowControl.getExcessiveLimit();

            long cMessage = nHigh * 2;
            Thread thread = new Thread(() ->
                {
                while (cReceive.get() < cMessage) // over aggressively schedule requests until we've received everything, i.e. depend on flow-control
                   {
                   cPending.incrementAndGet();
                   subscriber.receive().handle((Element<String> element, Throwable t) ->
                       {
                       cPending.decrementAndGet();
                       if (t == null)
                           {
                           cReceive.incrementAndGet();
                           }
                       return element;
                       });
                   }
                });

            thread.start();

            while (cPending.get() < nHigh) // wait for subscriber to build up a backlog
                {
                //noinspection BusyWait
                Thread.sleep(1);
                }

            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i = 0; i < cMessage; ++i)
                    {
                    publisher.publish("Element-" + i).get(2, TimeUnit.MINUTES); // .get() makes publisher slower then subscriber
                    assertThat(flowControl.getBacklog(), is(lessThanOrEqualTo(nMaxBacklog)));
                    }
                }

            long start = System.currentTimeMillis();
            while (cReceive.get() < cMessage)
                {
                //noinspection BusyWait
                Thread.sleep(1);
                long now = System.currentTimeMillis();
                assertThat("Timed out - received " + cReceive.get() + " out of " + cMessage,
                           now - start, is(lessThan(10000L)));
                }

            thread.interrupt(); // the thread may be blocked on flow control
            thread.join();

            assertThat(flowControl.getBacklog(), is(lessThanOrEqualTo(nMaxBacklog)));
            assertThat(cReceive.get(), is(cMessage));
            }
        }

    @Test
    public void shouldNotThrottleNonBlockingSubscriber()
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            int  cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
            long nHigh   = (getDependencies(topic).getMaxBatchSizeBytes() * 3) / cbValue;

            try (@SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                {
                for (int i = 0; i < nHigh * 100; ++i)
                    {
                    subscriber.receive();
                    }
                }
            }
        }

    @Test
    public void shouldThrottlePublisher() throws Exception
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String>           topic        = ensureTopic();
        PagedTopic.Dependencies      dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator   = dependencies.getElementCalculator();
        Serializer                   serializer   = topic.getService().getSerializer();
        int                          cbValue      = calculator.calculateUnits(ExternalizableHelper.toBinary("Element-" + 999, serializer));

        try (Publisher<String>  publisher = topic.createPublisher())
            {
            DebouncedFlowControl flowControl = (DebouncedFlowControl) publisher.getFlowControl();
            long                 nExcessive  = flowControl.getExcessiveLimit();
            int                  cMessage    = (int) nExcessive / cbValue;
            long                 nMaxBacklog = nExcessive + cbValue; // the max backlog is the excessive limit plus one message

            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                {
                for (int i = 0; i < cMessage * 100; ++i)
                    {
                    assertThat(flowControl.getBacklog(), is(lessThan(nMaxBacklog)));
                    publisher.publish("Element-" + i);
                    }
                });

            // will throw if the backlog is too big or it takes more than five minutes - which is a crazy amount of time
            future.get(5, TimeUnit.MINUTES);
            }
        }

    @Test
    public void shouldThrottlePublisherWhenFull() throws Exception
        {
        final long SERVER_CAPACITY = 10L * 1024L;

        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
                          getDependencies(topic).getServerCapacity(), is(SERVER_CAPACITY));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber())
            {
            String        sRand     = Base.getRandomString(64, 64, true);
            int           cbValue   = ExternalizableHelper.toBinary(sRand, topic.getService().getSerializer()).length();
            long          nHigh     = (topic.getService().getInfo().getServiceMembers().size() + 1) * SERVER_CAPACITY /*in config*/ / cbValue;
            int           cBacklogs = 0;
            AtomicInteger cValues   = new AtomicInteger();

            try (@SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                {
                for (int i = 0; i < nHigh * 2; ++i)
                    {
                    long                    ldtStart = System.currentTimeMillis();
                    CompletableFuture<Void> future   = publisher.publish(sRand)
                            .handle((status, err) ->
                                {
                                //System.err.println("Published message " + status.getChannel() + " " + status.getPosition());
                                return null;
                                });

                    cValues.incrementAndGet();

                    try
                        {
                        publisher.getFlowControl().flush();
                        future.get(100, TimeUnit.MILLISECONDS);
                        }
                    catch (TimeoutException t)
                        {
                        // we appear to be at capacity
                        ++cBacklogs;
                        CompletableFuture futureDrain = null;
                        for (int c = cValues.get(); c > 0; --c)
                            {
                            futureDrain = subscriber.receive()
                                    .whenComplete((r, e) ->
                                        {
                                        cValues.decrementAndGet();
                                        r.commit();
                                        });
                            }

                        if (futureDrain != null)
                            {
                            subscriber.getFlowControl().flush();
                            futureDrain.get(2, TimeUnit.MINUTES);
                            }
                        future.get(2, TimeUnit.MINUTES);
                        long cMillis = System.currentTimeMillis() - ldtStart;

                        // assert that we've not relied on notification expiry which isn't meant to be used
                        // here, that is basically just to cover partition movement giving us more space.
                        assertThat("timeout after PUBLISHER_NOTIFICATION_EXPIRY_MILLIS " + cMillis,
                                   cMillis, is(lessThan(PagedTopicPartition.PUBLISHER_NOTIFICATION_EXPIRY_MILLIS)));
                        break;
                        }
                    }
                }

            assertThat(cBacklogs, greaterThan(0)); // ensure we hit a backlog at least once
            }
        }

    @Test
    public void shouldThrowWhenFull()
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
                          getDependencies(topic).getServerCapacity(), is(SERVER_CAPACITY));

        try (@SuppressWarnings("unused") Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            int  cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
            long nHigh   = SERVER_CAPACITY / cbValue; // from config

            try(Publisher<String>  publisher = topic.createPublisher(Publisher.FailOnFull.enabled()))
                {
                for (int i = 0; i < nHigh * 2; ++i)
                    {
                    publisher.publish("Element-" + i).join();
                    }
                fail(); // we're not supposed to finish
                }
            catch (CompletionException e)
                {
                // expected
                assertThat(e.getCause(), is(instanceOf(TopicPublisherException.class)));
                Throwable cause = e.getCause().getCause();
                assertThat(cause, is(instanceOf(IllegalStateException.class)));
                assertThat(cause.getMessage().contains("topic is at capacity"), is(true));
                }
            }
        }

    @Test
    public void shouldCloseWhenFull()
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
                          getDependencies(topic).getServerCapacity(), is(SERVER_CAPACITY));

        try (@SuppressWarnings("unused") Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            int  cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
            long nHigh   = SERVER_CAPACITY / cbValue; // from config

            try(Publisher<String> publisher = topic.createPublisher(Publisher.FailOnFull.enabled()))
                {
                for (int i = 0; i < nHigh * 2; ++i)
                    {
                    publisher.publish("Element-" + i).join();
                    }
                fail(); // we're not supposed to finish
                }
            catch (CompletionException e)
                {
                // expected
                assertThat(e.getCause(), is(instanceOf(TopicPublisherException.class)));
                Throwable cause = e.getCause().getCause();
                assertThat(cause, is(instanceOf(IllegalStateException.class)));
                assertThat(cause.getMessage().contains("topic is at capacity"), is(true));
                }

            try (Publisher<String> publisherA = topic.createPublisher())
                {
                publisherA.publish("Element-after-full").exceptionally(t ->
                    {
                    System.out.println("send Element-after-full. Completed with exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    return null;
                    });
                }
            }
        }

    @Test
    public void shouldNotThrottleNonBlockingPublisher()
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic   = ensureTopic();
        AtomicLong         cReq    = new AtomicLong();
        long               cMax    = 0;
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh   = (getDependencies(topic).getMaxBatchSizeBytes() * 3) / cbValue;

        try (@SuppressWarnings("unused") Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber")))
            {
            try (Publisher<String> publisher = topic.createPublisher();
                 @SuppressWarnings("unused") NonBlocking nb = new NonBlocking())
                {
                for (int i = 0; i < nHigh * 100; ++i)
                    {
                    cMax = Math.max(cMax, cReq.incrementAndGet());
                    publisher.publish("Element0" + i).thenRun(cReq::decrementAndGet);
                    }
                }
            assertThat(cMax, greaterThan(nHigh)); // verify that publisher got ahead of flow-control
            }
        }

    @Test
    public void shouldNotPollExpiredElements() throws Exception
        {
        NamedTopic<String> topic      = ensureTopic(m_sSerializer + "-expiring");

        Assume.assumeThat("Test only applies when paged-topic-scheme has non-zero expiry configured",
                          getDependencies(topic).getElementExpiryMillis() != 0, is(true));

        String sPrefix = "Element-";
        int    nCount  = 20;

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("subscriber"), Subscriber.CompleteOnEmpty.enabled()))
            {
            try (Publisher<String> publisher  = topic.createPublisher())
                {
                for (int i=0; i<nCount; i++)
                    {
                    publisher.publish(sPrefix + i);
                    }

                Thread.sleep(3000);

                assertThat(subscriber.receive().get(1, TimeUnit.MINUTES), is(nullValue()));

                publisher.publish("Element-Last").get(1, TimeUnit.MINUTES);
                }

            String sValue = subscriber.receive().get(10, TimeUnit.SECONDS).getValue();
            assertThat(sValue, is("Element-Last"));
            }
        }

    @Test
    public void shouldStartSecondSubscriberFromCorrectPosition() throws Exception
        {
        NamedTopic<String> topic   = ensureTopic();
        String             sPrefix = "Element-";
        int                nCount  = 100;

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup("Foo")))
            {

            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i=0; i<nCount; i++)
                    {
                    publisher.publish(sPrefix + i).join();
                    }
                }

            int i = 0;
            for ( ; i<25; i++)
                {
                Element<String> element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + i));
                assertThat(element.commit().isSuccess(), is(true));
                }

            subscriber1.close();

            try (Subscriber<String> subscriber2 = topic.createSubscriber(inGroup("Foo")))
                {
                for ( ; i<50; i++)
                    {
                    assertThat(subscriber2.receive().get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + i));
                    }
                }
            }
        }

    @Test
    public void shouldConsumeFromTail() throws Exception
        {
        NamedTopic<String> topic   = ensureTopic();
        String             sPrefix = "Element-";

        try (Publisher<String> publisher = topic.createPublisher();
            @SuppressWarnings("unused")
            Subscriber<String> subscriberPin = topic.createSubscriber()) // ensures data inserted before test subscriber is created remains in the topic
            {
            publisher.publish(sPrefix + 1).join();

            Subscriber<String> subscriber = topic.createSubscriber();

            Future<Subscriber.Element<String>> future = subscriber.receive();

            publisher.publish(sPrefix + 2);

            assertThat(future.get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + 2));
            }
        }

    @Test
    public void shouldGroupConsumeFromLastCommittedIfReceiveAfterSubscriberClose() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        String             sPrefix   = "Element-";

        try (Publisher<String> publisher = topic.createPublisher())
            {
            publisher.publish(sPrefix + 1).join();

            try (Subscriber<String> subscriber1 = topic.createSubscriber(Subscriber.Name.inGroup("foo")))
                {
                Future<Subscriber.Element<String>> future = subscriber1.receive();

                publisher.publish(sPrefix + 2);
                publisher.publish(sPrefix + 3);

                Element<String> element = future.get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + 2));

                // commit element-2
                element.commit();
                // receieve element-3
                element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + 3));

                // ensure that a new member to the group doesn't reset the groups position in the topic, i.e. this subscriber
                // instance can see items created before it joined the group, as position is defined by the group not the
                // instance

                try (Subscriber<String> subscriber2 = topic.createSubscriber(Subscriber.Name.inGroup("foo")))
                    {
                    // close subscriber-1, which should reposition back to the last committed element-2, so the next read is element-3
                    subscriber1.close();

                    Future<Subscriber.Element<String>> future2 = subscriber2.receive();

                    assertThat(future2.get(10, TimeUnit.SECONDS).getValue(), is(sPrefix + 3));
                    }
                }
            }
        }

    @Test
    public void shouldOfferSingleElementLargerThanMaxPageSizeToEmptyPage() throws Exception
        {
        NamedTopic<String> topic      = ensureTopic(m_sSerializer + "-one-kilobyte-test");
        int                cbPageSize = getDependencies(topic).getPageCapacity();

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(2024));

        try (Subscriber<String> subscriber = topic.createSubscriber())
            {
            char[] aChars = new char[2024];
            Arrays.fill(aChars, 'a');

            String element = new String(aChars);

            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish(element).get(1, TimeUnit.MINUTES);
                }

            String sValue = subscriber.receive().get(1, TimeUnit.MINUTES).getValue();

            assertThat(sValue, is(element));
            }
        }

    @Test
    public void shouldOfferSingleElementLargerThanMaxPageSizeToPopulatedPage() throws Exception
        {
        NamedTopic<String> topic      = ensureTopic(m_sSerializer + "-one-kilobyte-test");
        int                cbPageSize = getDependencies(topic).getPageCapacity();

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(2024));

        try (Subscriber<String> subscriber = topic.createSubscriber())
            {
            char[] aChars = new char[2024];
            Arrays.fill(aChars, 'a');

            String element = new String(aChars);

            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("element-1").get(1, TimeUnit.MINUTES);
                publisher.publish(element).get(1, TimeUnit.MINUTES);
                }

            String sValue1 = subscriber.receive().get(1, TimeUnit.MINUTES).getValue();
            String sValue2 = subscriber.receive().get(1, TimeUnit.MINUTES).getValue();

            assertThat(sValue1, is("element-1"));

            assertThat(sValue2, is(element));
            }
        }

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     *
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     */
    @Test
    public void validateDefaultedStorageTopicMBeans() throws Exception
        {
        validateTopicMBeans(m_sSerializer + "-default-test");
        }

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     *
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     */
    @Test
    public void validateTopicMBeans() throws Exception
        {
        validateTopicMBeans(m_sSerializer + "-binary-test");
        }

    // regression test for when ValueTypeAssertion.withXXX returned values that were not considered equal
    @Test
    public void shouldBeSameSessionNamedTopic()
        {
        ValueTypeAssertion[] assertion1 =
                {
                ValueTypeAssertion.withType(String.class),
                ValueTypeAssertion.withRawTypes(),
                ValueTypeAssertion.withoutTypeChecking()
                };

        ValueTypeAssertion[] assertion2 =
                {
                ValueTypeAssertion.withType(String.class),
                ValueTypeAssertion.withRawTypes(),
                ValueTypeAssertion.withoutTypeChecking()
                };

        NamedTopic<Customer> topic1 = null;
        NamedTopic<Customer> topic2;

        Session session = getSession();
        for (int i = 0; i < assertion1.length; i++)
            {
            assertEquals(assertion1[i], assertion2[i]);
            topic1 = session.getTopic(m_sSerializer + "-XXX", assertion1[i]);
            topic2 = session.getTopic(m_sSerializer + "-XXX", assertion2[i]);
            assertThat("testing " + assertion1[i], topic1 == topic2);
            }

        final NamedTopic topic = topic1;

        topic.destroy();
        Eventually.assertDeferred(topic::isDestroyed, is(true));
        }

    @Test
    public void shouldSeekEmptyTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            Random random = new Random(System.currentTimeMillis());
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                Position position = subscriberOne.seek(nChannel, new PagedPosition(random.nextInt(10000), random.nextInt(100)));
                assertThat(position, is(new PagedPosition(0, 0)));
                }

            assertSubscribersAtTail(subscriberOne, subscriberTwo, caches, 0, null);
            }
        }

    @Test
    public void shouldSeekGroupSubscriberForwards() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-3");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 5000; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekGroupSubscriberForwardsUsingTimestamp() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-3");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 500; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                futurePublish.get(2, TimeUnit.MINUTES);
                Thread.sleep(20);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 250; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();
            int             nOffset       = pagedPosition.getOffset();

            // ensure the position is not a head or tail of a page
            Page page = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
            while (nOffset == 0 || nOffset == page.getTail())
                {
                // we're at a head or tail so read another
                future        = subscriberTwo.receive();
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                nOffset       = pagedPosition.getOffset();
                page          = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
                }

            // we know we're now not at a head or tail of a page
            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), element.getTimestamp());

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));

            // now move subscriber two some way ahead
            for (int i = 0; i < 100; i++)
                {
                subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                }

            element       = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            pagedPosition = (PagedPosition) element.getPosition();
            nOffset       = pagedPosition.getOffset();
            page          = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));

            // keep reading until subscriber two has read the tail of the page
            while (nOffset != page.getTail())
                {
                // we're not at the tail so read another
                future        = subscriberTwo.receive();
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                nOffset       = pagedPosition.getOffset();
                }

            // we're now at the tail of a page
            // Seek subscriber one to the last timestamp read by subscription two
            result = subscriberOne.seek(element.getChannel(), element.getTimestamp());

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));

            // now move subscriber two some way ahead
            for (int i = 0; i < 100; i++)
                {
                subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                }

            element       = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            pagedPosition = (PagedPosition) element.getPosition();
            nOffset       = pagedPosition.getOffset();

            // keep reading until subscriber two has read the head of the page
            while (nOffset != 0)
                {
                // we're not at the head so read another
                future        = subscriberTwo.receive();
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                nOffset       = pagedPosition.getOffset();
                }

            // we're now at the head of a page
            // Seek subscriber one to the last timestamp read by subscription two
            result = subscriberOne.seek(element.getChannel(), element.getTimestamp());

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekGroupSubscriberForwardsAfterReadingSome() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-3");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a some messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 20; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 5; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Read a couple of messages from subscriber one before seeking, as we buffer
            // we should have fetched the page back and need to clear the buffer on seeking
            subscriberOne.receive().get(2, TimeUnit.MINUTES);
            subscriberOne.receive().get(2, TimeUnit.MINUTES);

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsToEndOfPage() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 5019; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Get the Page and see it we have read the last element from that page
            Page page  = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
            int  nTail = page.getTail();

            // keep polling until we get the tail of the page
            while (pagedPosition.getOffset() != nTail)
                {
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                }

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seeking to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);

            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsToEndOfTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-5");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());
        int                cMsg   = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < cMsg; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < cMsg; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two (this should be the last element in the topic)
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seek to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            assertSubscribersAtTail(subscriberOne, subscriberTwo, caches, nChannel, pagedPosition);
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsToEndOfTopicWhereTopicEndsOnPageEnd() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-6");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());
        int                cMsg   = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       i;
        Status                    status;
        PagedPosition             positionLast;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (i = 0; i < cMsg; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            status = futurePublish.get(2, TimeUnit.MINUTES);
            // Get the position of the last message
            positionLast = (PagedPosition) status.getPosition();
            // Get the channel messages were published to
            nChannel = status.getChannel();
            }

        // get the last Page of the topic
        Page page  = caches.Pages.get(new Page.Key(nChannel, positionLast.getPage()));

        // publish more messages until the Page gets sealed (i.e. it is full)
        if (!page.isSealed())
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                while (!page.isSealed())
                    {
                    status = publisher.publish("element-" + i++).get(2, TimeUnit.MINUTES);
                    page   = caches.Pages.get(new Page.Key(nChannel, positionLast.getPage()));
                    }
                }
            }

        // Get the position of the last message (this is also now at the end of a page)
        positionLast = (PagedPosition) status.getPosition();

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            Element<String> element       = null;
            PagedPosition   pagedPosition = null;

            while (!Objects.equals(pagedPosition, positionLast))
                {
                element       = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                }

            // Subscriber two has now read the tail of the topic

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            assertSubscribersAtTail(subscriberOne, subscriberTwo, caches, nChannel, pagedPosition);
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsPastEndOfTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-5");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());
        int                cMsg   = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < cMsg; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber two on by receiving pages (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < cMsg; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two (this should be the last element in the topic)
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), new PagedPosition(pagedPosition.getPage() + 1, 20));

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            assertSubscribersAtTail(subscriberOne, subscriberTwo, caches, nChannel, pagedPosition);
            }
        }

    @Test
    public void shouldSeekGroupSubscriberBackwards() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-3");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            future.get(2, TimeUnit.MINUTES);

            // move subscriber two not as far than subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
            for (int i = 0; i < 5000; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seeking to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekGroupSubscriberBackAndResetCommitRewindableTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-3");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            Element<String> element = future.get(2, TimeUnit.MINUTES);
            // commit subscriber one
            Subscriber.CommitResult commitResult = element.commit();
            System.err.println("Committed One at: " + commitResult.getPosition());

            // move subscriber two not as far than subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
            for (int i = 0; i < 5000; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            element = future.get(2, TimeUnit.MINUTES);
            PagedPosition pagedPosition = (PagedPosition) element.getPosition();

            // commit subscriber two
            commitResult = element.commit();
            System.err.println("Committed Two at: " + commitResult.getPosition());

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seeking to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekGroupSubscriberBackAndResetCommit() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup("two")))
            {
            // publish a lot os messages so we have multiple pages spread over all of the partitions
            CompletableFuture<Status> futurePublish = null;
            int                       nChannel;

            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i = 0; i < 10001; i++)
                    {
                    futurePublish = publisher.publish("element-" + i);
                    }
                publisher.flush().get(2, TimeUnit.MINUTES);
                // Get the channel messages were published to
                nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
                }

            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            Element<String> element = future.get(2, TimeUnit.MINUTES);
            // commit subscriber one
            Subscriber.CommitResult commitResult = element.commit();
            System.err.println("Committed One at: " + commitResult.getPosition());

            // move subscriber two not as far than subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
            for (int i = 0; i < 5000; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            element = future.get(2, TimeUnit.MINUTES);
            PagedPosition pagedPosition = (PagedPosition) element.getPosition();

            // commit subscriber two
            commitResult = element.commit();
            System.err.println("Committed Two at: " + commitResult.getPosition());

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seeking to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);
            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);
            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberBackwardsToEndOfPage() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            future.get(2, TimeUnit.MINUTES);

            // move subscriber two not as far than subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
            for (int i = 0; i < 5019; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Get the Page and see it we have read the last element from that page
            Page page  = caches.Pages.get(new Page.Key(nChannel, pagedPosition.getPage()));
            int  nTail = page.getTail();

            // keep polling until we get the tail of the page
            while (pagedPosition.getOffset() != nTail)
                {
                element       = future.get(2, TimeUnit.MINUTES);
                pagedPosition = (PagedPosition) element.getPosition();
                }

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);

            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberBackToBeginningOfTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            future.get(2, TimeUnit.MINUTES);

            // Read first message with subscriber two (we'll then seek subscriber one back to the same place)
            future = subscriberTwo.receive();

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);

            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, pagedPosition);

            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberBackBeforeBeginningOfTopic() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (CacheService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages so we have multiple pages spread over all of the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber();
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber())
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            future.get(2, TimeUnit.MINUTES);

            // Read first message with subscriber two (we'll then seek subscriber one back to the same place)
            future = subscriberTwo.receive();

            // Obtain the first received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberTwo, nChannel, caches);

            // Seek subscriber one to the last position read by subscription two
            PagedPosition result = (PagedPosition) subscriberOne.seek(element.getChannel(), new PagedPosition(pagedPosition.getPage() - 1, 10));

            // should have seeked to the correct position
            assertThat(result.getPage(), is(pagedPosition.getPage() - 1));

            // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
            SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberOne, nChannel, caches);

            // the two lots of Subscriptions should match
            assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, new PagedPosition(pagedPosition.getPage(), -1));

            // Poll the next element for subscriber one, it should match that read by subscriber two
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getValue(), is(element.getValue()));
            assertThat(elementOne.getPosition(), is(element.getPosition()));
            }
        }

    @Test
    public void shouldSeekToTail() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-5");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            CompletableFuture<Status> futurePublish = null;
            for (int i = 0; i < 10000; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            int nChannel = futurePublish.get(2, TimeUnit.MINUTES).getChannel();

            try (Subscriber<String> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled()))
                {
                Map<Integer, Position> map = subscriber.seekToTail(nChannel);
                assertThat(map.get(nChannel), is(notNullValue()));

                CompletableFuture<Element<String>> future = subscriber.receive();

                publisher.publish("element-last").join();

                Element<String> elementTail = future.get(2, TimeUnit.MINUTES);
                assertThat(elementTail, is(notNullValue()));
                assertThat(elementTail.getValue(), is("element-last"));
                }
            }
        }

    @Test
    public void shouldSeekToHead() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-5");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i = 0; i < 10000; i++)
                {
                publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            try (Subscriber<String> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled()))
                {
                Element<String> elementHead = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementHead, is(notNullValue()));
                assertThat(elementHead.getValue(), is("element-0"));

                CompletableFuture<Element<String>> future = null;
                for (int i = 0; i < 5000; i ++)
                    {
                    future = subscriber.receive();
                    }
                Element<String> element  = future.get(2, TimeUnit.MINUTES);
                int             nChannel = element.getChannel();

                // seek to the head of the channel
                subscriber.seekToHead(nChannel);

                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is(elementHead.getValue()));
                assertThat(element.getPosition(), is(elementHead.getPosition()));
                }
            }
        }

    @Test
    public void shouldSeekToHeadRollingBackCommit() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-5");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            CompletableFuture<Status> futurePublish = null;
            for (int i = 0; i < 10000; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            int             nChannel    = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            Element<String> elementHead;

            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("test"), CompleteOnEmpty.enabled()))
                {
                elementHead = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementHead, is(notNullValue()));
                assertThat(elementHead.getValue(), is("element-0"));

                CompletableFuture<Element<String>> future = null;
                for (int i = 0; i < 5000; i++)
                    {
                    future = subscriber.receive();
                    }
                Element<String> element = future.get(2, TimeUnit.MINUTES);

                element.commit();

                // seek back to the head and close
                subscriber.seekToHead(nChannel);
                }

            // create a new subscriber in the same group, it should be at the head
            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup("test"), CompleteOnEmpty.enabled()))
                {
                Element<String> elementResult = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementResult, is(notNullValue()));
                assertThat(elementResult.getPosition(), is(elementHead.getPosition()));
                assertThat(elementResult.getValue(), is(elementHead.getValue()));
                }
            }
        }

    @Test
    public void shouldHaveDeterministicPositionUsingFixedCalculator() throws Exception
        {
        NamedTopic<String>      topic        = ensureTopic(m_sSerializer + "-fixed-test");
        PagedTopic.Dependencies dependencies = getDependencies(topic);

        Assume.assumeThat("Test only applies when paged-topic-scheme has FIXED element calculator configured",
                          dependencies.getElementCalculator(), is(instanceOf(FixedElementCalculator.class)));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            int    nPageSize   = dependencies.getPageCapacity();
            int    cPage       = 10;
            int    cMessage    = nPageSize * cPage; // publish multiple pages of messages
            Status statusFirst = null;

            for (int i = 0; i < cMessage; i++)
                {
                Status status = publisher.publish("element-" + i).get(2, TimeUnit.MINUTES);
                if (i == 0)
                    {
                    statusFirst = status;
                    }
                }

            assertThat(statusFirst, is(notNullValue()));

            PagedPosition positionFirst = (PagedPosition) statusFirst.getPosition();
            long          lPageFirst    = positionFirst.getPage();
            int           nChannel      = statusFirst.getChannel();

            // first message should be at offset zero of first page
            assertThat(positionFirst.getOffset(), is(0));

            try (Subscriber<String> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled()))
                {
                PagedPosition positionHead = (PagedPosition) subscriber.getHead(nChannel).orElse(null);
                assertThat(positionHead, is(notNullValue()));
                assertThat(positionHead.compareTo(positionFirst), is(lessThanOrEqualTo(0)));

                // first message should be element-0
                Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is("element-0"));

                PagedPosition position = (PagedPosition) element.getPosition();
                assertThat(position, is(positionFirst));

                // seek to the 5th message
                Position positionSeek = subscriber.seek(nChannel, new PagedPosition(lPageFirst, 5));
                assertThat(positionSeek, is(notNullValue()));
                assertThat(positionSeek, is(new PagedPosition(lPageFirst, 5)));

                // next element received should be element-6
                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is("element-6"));

                // seek to the 44th message
                int  cElement    = 44;
                long lPageSeek   = lPageFirst + (cElement / nPageSize);
                int  nOffsetSeek = cElement % nPageSize;

                positionSeek = subscriber.seek(nChannel, new PagedPosition(lPageSeek, nOffsetSeek));
                assertThat(positionSeek, is(notNullValue()));
                assertThat(positionSeek, is(new PagedPosition(lPageSeek, nOffsetSeek)));

                // next element received should be element-45
                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is("element-" + (cElement + 1)));
                }
            }

        }


    // ----- helper methods -------------------------------------------------

    /**
     * Assert that {@code subscriberTest} has been positioned at the tail of the topic.
     * <p>
     * Subscriber {@code subscriberExpected} has been positioned at the tail by calling {@link Subscriber#receive()}
     * whereas {@code subscriberTest} has been positioned by a call to {@link Subscriber#seek}.
     *
     * @param subscriberTest      the {@link Subscriber} to seek
     * @param subscriberExpected  the {@link Subscriber} already at the tail
     * @param caches              the {@link PagedTopicCaches} for the topic
     * @param nChannel            the channel being seeked
     * @param position            the tail position
     *
     * @throws Exception if an error occurs
     */
    protected void assertSubscribersAtTail(Subscriber<String> subscriberTest, Subscriber<String> subscriberExpected,
                                           PagedTopicCaches caches, int nChannel, PagedPosition position) throws Exception
        {
        // Collect all of the Subscriptions for the published channel for subscriber two (sorted by partition id)
        SortedMap<Subscription.Key, Subscription> mapSubscriptionTwo = getSubscriptions(subscriberExpected, nChannel, caches);

        // Collect all of the Subscriptions for the published channel for subscriber one (sorted by partition id)
        SortedMap<Subscription.Key, Subscription> mapSubscriptionOne = getSubscriptions(subscriberTest, nChannel, caches);

        // the two lots of Subscriptions should match
        assertSubscriptions(mapSubscriptionOne, mapSubscriptionTwo, position);

        // Poll the next element for each subscriber, they should block until we publish a new message
        CompletableFuture<Element<String>> futureOne = subscriberTest.receive();
        CompletableFuture<Element<String>> futureTwo = subscriberExpected.receive();

        // publish a new message
        NamedTopic<String>        topic         = subscriberTest.getNamedTopic();
        CompletableFuture<Status> futurePublish;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            futurePublish = publisher.publish("element-last");
            futurePublish.get(2, TimeUnit.MINUTES);
            }

        // both subscribers should receive the last message from the same position
        Element<String> elementOne = futureOne.get(2, TimeUnit.MINUTES);
        Element<String> elementTwo = futureTwo.get(2, TimeUnit.MINUTES);

        assertThat(elementTwo, is(notNullValue()));
        assertThat(elementTwo.getValue(), is("element-last"));

        assertThat(elementOne, is(notNullValue()));
        assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
        assertThat(elementOne.getValue(), is(elementTwo.getValue()));
        }

    SortedMap<Subscription.Key, Subscription> getSubscriptions(Subscriber<?> subscriber, int nChannel, PagedTopicCaches caches)
        {
        PagedTopicSubscriber<?>                             pagedSubscriber  = (PagedTopicSubscriber<?>) subscriber;
        ValueExtractor<Subscription.Key, Integer>           extractorChannel = new ReflectionExtractor<>("getChannelId", new Object[0], EntryExtractor.KEY);
        Filter<Subscription.Key>                            filterChannel    = Filters.equal(extractorChannel, nChannel);
        ValueExtractor<Subscription.Key, SubscriberGroupId> extractorGroup   = new ReflectionExtractor<>("getGroupId", new Object[0], EntryExtractor.KEY);
        Filter<Subscription.Key>                            filterGroup      = Filters.equal(extractorGroup, pagedSubscriber.getSubscriberGroupId());
        SortedMap<Subscription.Key, Subscription>           map              = new TreeMap<>(Comparator.comparingLong(Subscription.Key::getPartitionId));

        caches.Subscriptions.entrySet(Filters.all(filterChannel, filterGroup)).forEach(e -> map.put(e.getKey(), e.getValue()));

        return map;
        }

    void assertSubscriptions(SortedMap<Subscription.Key, Subscription> mapSubscription, SortedMap<Subscription.Key, Subscription> mapExpected, PagedPosition position)
        {
        Iterator<Map.Entry<Subscription.Key, Subscription>> itOne   = mapSubscription.entrySet().iterator();
        Iterator<Map.Entry<Subscription.Key, Subscription>> itTwo   = mapExpected.entrySet().iterator();
        long                                                lPage   = position == null ? Page.NULL_PAGE : position.getPage();
        int                                                 nOffset = position == null ? 0 : position.getOffset();

        while(itOne.hasNext() && itTwo.hasNext())
            {
            Map.Entry<Subscription.Key, Subscription> entryOne        = itOne.next();
            Map.Entry<Subscription.Key, Subscription> entryTwo        = itTwo.next();
            Subscription.Key                          keyOne          = entryOne.getKey();
            Subscription                              subscriptionOne = entryOne.getValue();
            Subscription.Key                          keyTwo          = entryTwo.getKey();
            Subscription                              subscriptionTwo = entryTwo.getValue();

            assertThat(keyOne.getPartitionId(), is(keyTwo.getPartitionId()));
            assertThat(keyOne.getChannelId(), is(keyTwo.getChannelId()));

            assertThat(subscriptionOne.getPage(), is(subscriptionTwo.getPage()));
//            assertThat(subscriptionOne.getSubscriptionHead(), is(subscriptionTwo.getSubscriptionHead()));
            assertThat(subscriptionOne.getCommittedPosition(), is(subscriptionTwo.getCommittedPosition()));
            assertThat(subscriptionOne.getRollbackPosition(), is(subscriptionTwo.getRollbackPosition()));

            if (subscriptionOne.getPage() == lPage && subscriptionOne.getPosition() != Integer.MAX_VALUE)
                {
                // for the requested page we should be at offset + 1
                assertThat(subscriptionOne.getPosition(), is(nOffset + 1));
                }
            else
                {
                assertThat(subscriptionOne.getPosition(), is(subscriptionTwo.getPosition()));
                }
            }
        }

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     *
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     */
    private void validateTopicMBeans(String sTopicName) throws Exception
        {
        Session            session    = getSession();
        NamedTopic<String> topic      = session.getTopic(sTopicName, ValueTypeAssertion.withType(String.class));

        final int          nMsgSizeBytes = 1024;
        final int          cMsg          = 500;

        try(@SuppressWarnings("unused") Subscriber<String> subscriber = topic.createSubscriber())
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                populate(publisher, nMsgSizeBytes, cMsg);
                }

            MBeanServer server = MBeanHelper.findMBeanServer();

            validateTopicMBean(server, "Cache", sTopicName, nMsgSizeBytes, cMsg);
            validateTopicMBean(server, "StorageManager", sTopicName, nMsgSizeBytes, cMsg);
            }

        topic.destroy();
        }

    /**
     * Validate that only {@link PagedTopicCaches.Names#CONTENT} MBean exists for topic sName.
     *
     * Validate across all Data CacheMBeans that there is enough binary memory to account
     * for size and number of messages sent to topic.
     *
     * @param server       MBean server
     * @param sTypeMBean   MBean type. expecting either Cache or StorageManager
     * @param sName        validate MBean for this topic name
     * @param nMessageSize message size published
     * @param cMessages   number of messages published to topic
     */
    @SuppressWarnings("SameParameterValue")
    private void validateTopicMBean(MBeanServer server, String sTypeMBean, String sName, int nMessageSize, int cMessages)
            throws Exception
        {
        String              elementsCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sName);
        boolean             fElementsDefined  = false;
        Set<ObjectInstance> setMBean          = server.queryMBeans(new ObjectName("Coherence:type=" + sTypeMBean +",*"), null);
        boolean             fCache            = false;
        int                 cUnits            = 0;
        int                 cSize             = 0;

        for (ObjectInstance inst : setMBean)
            {
            String sNameMBean = inst.getObjectName().toString();

            assertFalse("Topic MetaCache MBean containing prefix " +  PagedTopicCaches.Names.METACACHE_PREFIX + " must not exist: " + sNameMBean,
                        sNameMBean.contains(PagedTopicCaches.Names.METACACHE_PREFIX));
            if (sNameMBean.contains(elementsCacheName))
                {
                fElementsDefined = true;

                if (sNameMBean.contains("Cache"))
                    {
                    assertThat(sNameMBean + " MBean attribute constraint check: MemoryUnits", (boolean) server.getAttribute(inst.getObjectName(), "MemoryUnits"));

                    int  units      = (int) server.getAttribute(inst.getObjectName(),  "Units");
                    int  unitFactor = (int) server.getAttribute(inst.getObjectName(),  "UnitFactor");

                    fCache     = true;
                    cUnits     += (units * unitFactor);
                    cSize      += (int) server.getAttribute(inst.getObjectName(),  "Size");
                    }
                }
            }

            if (fCache)
                {
                // validate that number of messages and number of bytes sent is correct across all storage enabled cache servers.
                assertThat("MBean attribute constraint check: units * unitFactor", cUnits, greaterThan(nMessageSize * cMessages));
                assertThat("MBean attribute constraint check: size", cSize, is(cMessages));
                }

            assertThat("Missing " + sTypeMBean + " MBean for " + elementsCacheName, fElementsDefined);
        }

    @SuppressWarnings("SameParameterValue")
    protected void populate(Publisher<String> publisher, int nCount)
        {
        for (int i=0; i<nCount; i++)
            {
            try
                {
                publisher.publish("Element-" + i).get(1, TimeUnit.MINUTES);
                }
            catch (InterruptedException | ExecutionException | TimeoutException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    @SuppressWarnings("SameParameterValue")
    protected void populate(Publisher<String> publisher, int nMsgSize, int nCount)
        {
        byte[] bytes = new byte[nMsgSize];
        Arrays.fill(bytes, (byte)'A');
        for (int i=0; i<nCount; i++)
            {
            try
                {
                publisher.publish(new String(bytes)).get(1, TimeUnit.MINUTES);
                }
            catch (InterruptedException | ExecutionException | TimeoutException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        publisher.flush().join();
        }

    protected synchronized NamedTopic<String> ensureTopic()
        {
        if (m_topic == null)
            {
            String sName = ensureTopicName();
            m_topic = getSession().getTopic(sName, ValueTypeAssertion.withType(String.class));
            }

        return (NamedTopic<String>) m_topic;
        }

    protected synchronized String ensureTopicName()
        {
        return ensureTopicName(m_sSerializer);
        }

    protected synchronized String ensureTopicName(String sPrefix)
        {
        if (m_sTopicName == null)
            {
            m_sTopicName = sPrefix + "-" + m_nSuffix.getAndIncrement();
            }
        return m_sTopicName;
        }

    protected PagedTopic.Dependencies getDependencies(NamedTopic<?> topic)
        {
        return topic.getService().getResourceRegistry().getResource(PagedTopic.Dependencies.class, topic.getName());
        }
    
    protected synchronized <V> NamedTopic<V> ensureRawTopic()
        {
        if (m_topic == null)
            {
            String sName = ensureTopicName(m_sSerializer + "-raw");
            m_topic = getSession().getTopic(sName);
            }

        return (NamedTopic<V>) m_topic;
        }

    protected synchronized NamedTopic<String> ensureTopic(String sTopicName)
        {
        if (m_topic != null)
            {
            assertThat(m_topic.getName(), is(sTopicName + "-" + m_nSuffix.get()));
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
        return m_topicCustomer = getSession().getTopic(sTopicName, ValueTypeAssertion.withType(Customer.class));
        }

    protected boolean isCommitted(Subscriber<?> subscriber, int nChannel, Position position)
        {
        return ((PagedTopicSubscriber) subscriber).isCommitted(nChannel, position);
        }

    protected abstract Session getSession();

    @SuppressWarnings("unused")
    protected abstract void runInCluster(RemoteRunnable runnable);

    @SuppressWarnings("unused")
    protected abstract int getStorageMemberCount();

    protected abstract String getCoherenceCacheConfig();

    // ----- inner class: ChannelPosition -----------------------------------

    public static class ChannelPosition
        {
        public ChannelPosition(int nChannel, Position position)
            {
            m_nChannel = nChannel;
            m_position = position;
            }

        public ChannelPosition(Element<?> element)
            {
            this(element.getChannel(), element.getPosition());
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }
            ChannelPosition that = (ChannelPosition) o;
            return m_nChannel == that.m_nChannel && Objects.equals(m_position, that.m_position);
            }

        @Override
        public int hashCode()
            {
            return Objects.hash(m_nChannel, m_position);
            }

        @Override
        public String toString()
            {
            return "ChannelPosition(" +
                    "channel=" + m_nChannel +
                    ", position=" + m_position +
                    ')';
            }

        private final int m_nChannel;

        private final Position m_position;
        }

    // ----- inner class Watcher --------------------------------------------

    public static class Watcher
            extends TestWatcher
        {
        @Override
        protected void starting(Description d) {
            m_sName = d.getMethodName();
            System.err.println(">>>>> Starting test: " + m_sName + " in class " + d.getTestClass());
        }

        @Override
        protected void succeeded(Description description)
            {
            System.err.println(">>>>> Test Passed: " + m_sName);
            }

        @Override
        protected void failed(Throwable e, Description description)
            {
            System.err.println(">>>>> Test Failed: " + m_sName);
            e.printStackTrace();
            System.err.println("<<<<<");
            }

        @Override
        protected void skipped(AssumptionViolatedException e, Description description)
            {
            System.err.println(">>>>> Test Skipped: " + m_sName);
            }

        /**
         * @return the name of the currently-running test method
         */
        public String getMethodName() {
            return m_sName;
        }

        // ----- data members -----------------------------------------------

        private volatile String m_sName;
        }

    static class ListLogger
        {
        void add(String s)
            {
            m_listLog.add(s);
            if (m_fLog)
                {
                System.err.println(s);
                }
            }

        ArrayList<String> getLog()
            {
            return m_listLog;
            }

        private final ArrayList<String> m_listLog = new ArrayList<>(20000);

        @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
        private boolean m_fLog = false;
        }

    // ----- constants ------------------------------------------------------

    static public final String DEFAULT_COHERENCE_CACHE_CONFIG = "coherence-cache-config.xml";

    static public final String CUSTOMIZED_CACHE_CONFIG        = "topic-cache-config.xml";

    // ----- data members ---------------------------------------------------

    @Rule
    public Watcher m_testName = new Watcher();

    protected String               m_sSerializer;
    protected NamedTopic<?>        m_topic;
    protected String               m_sTopicName;
    protected AtomicInteger        m_nSuffix = new AtomicInteger();
    protected NamedTopic<Customer> m_topicCustomer;
    protected ExecutorService      m_executorService = Executors.newFixedThreadPool(4);
    }
