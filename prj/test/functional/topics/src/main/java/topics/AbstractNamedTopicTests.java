/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.NonBlocking;

import com.oracle.coherence.testing.junit.ThreadDumpOnTimeoutRule;
import com.tangosol.internal.net.DebouncedFlowControl;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.io.Serializer;

import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PagedTopicService;
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
import com.tangosol.net.topic.Subscriber.CommitResult;
import com.tangosol.net.topic.Subscriber.CommitResultStatus;
import com.tangosol.net.topic.Subscriber.Element;
import com.tangosol.net.topic.TopicException;
import com.tangosol.net.topic.TopicPublisherException;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filters;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.PofExtractor;
import com.tangosol.util.extractor.UniversalExtractor;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.GreaterEqualsFilter;
import com.tangosol.util.filter.GreaterFilter;
import com.tangosol.util.filter.LessFilter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.SortedSet;
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
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import org.junit.runners.model.Statement;
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

import static com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber.withIdentifyingName;
import static com.tangosol.net.topic.Subscriber.completeOnEmpty;
import static com.tangosol.net.topic.Subscriber.inGroup;
import static com.tangosol.net.topic.Subscriber.withConverter;
import static com.tangosol.net.topic.Subscriber.withFilter;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.oneOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;


/**
 * @author jk 2015.05.28
 */
@SuppressWarnings({"unchecked", "rawtypes", "resource"})
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
    public void shouldFilter() throws Exception
        {
        Session session    = getSession();
        String  sTopicName = ensureTopicName();

        try (Subscriber<String> subscriberD   = session.createSubscriber(sTopicName, withFilter(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "d")));
             Subscriber<String> subscriberA   = session.createSubscriber(sTopicName, withFilter(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "a")));
             Subscriber<String> subscriberLen = session.createSubscriber(sTopicName, withFilter(new GreaterFilter<>(String::length, 1))))
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
        try(Subscriber<Integer> subscriber1 = topic.createSubscriber(withConverter(Integer::parseInt));
            Subscriber<Integer> subscriber2 = topic.createSubscriber(withConverter(String::length)))
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
        final NamedTopic<String> topic   = session.getTopic(sName);

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }

        assertThat(topic.isActive(), is(true));
        assertThat(topic.isDestroyed(), is(false));

        PagedTopicService service = (PagedTopicService) topic.getService();
        PagedTopicCaches  caches  = new PagedTopicCaches(sName, service);

        for (NamedCache<?, ?> cache : caches.getCaches())
            {
            assertThat(cache.isActive(), is(true));
            }

        topic.destroy();

        assertDeferred(topic::isActive, is(false));
        assertDeferred(topic::isDestroyed, is(true));
        assertThat(topic.isReleased(), is(true));

        for (NamedCache<?, ?> cache : caches.getCaches())
            {
            assertThat(cache.isActive(), is(false));
            }

        NamedTopic<String> topic2 = session.getTopic(sName);
        assertThat(topic2.isActive(), is(true));
        assertThat(topic2.isDestroyed(), is(false));

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
        final NamedTopic<String> topic   = session.getTopic(sName);

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }

        assertThat(topic.isActive(), is(true));
        assertThat(topic.isReleased(), is(false));

        PagedTopicService service = (PagedTopicService) topic.getService();
        PagedTopicCaches  caches  = new PagedTopicCaches(sName, service);

        for (NamedCache<?, ?> cache : caches.getCaches())
            {
            assertThat(cache.isActive(), is(true));
            }

        topic.release();

        assertDeferred(topic::isActive, is(false));
        assertThat(topic.isReleased(), is(true));
        assertThat(topic.isDestroyed(), is(false));

        for (NamedCache<?, ?> cache : caches.getCaches())
            {
            assertThat(cache.getCacheName(), cache.isReleased(), is(true));
            assertThat(cache.getCacheName(), cache.isDestroyed(), is(false));
            assertThat(cache.getCacheName(), cache.isActive(), is(false));
            }

        NamedTopic<String> topic2 = session.getTopic(sName);
        assertThat(topic2.isActive(), is(true));
        assertThat(topic2.isReleased(), is(false));

        try (Publisher<String> publisher = topic2.createPublisher())
            {
            populate(publisher, 20);
            }
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
    public void shouldNotBlockOnReceiveWithCompleteOnEmptySubscriber() throws Exception
        {
        int                nCount = 20;
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup), completeOnEmpty()))
            {
            for (int i = 0; i < nCount; i++)
                {
                publisher.publish("Element-" + i).get(5, TimeUnit.MINUTES);
                }

            for (int i = 0; i < nCount; i++)
                {
                String sExpected = "Element-" + i;
                String sElement  = subscriber.receive().get(1, TimeUnit.MINUTES).getValue();
                assertThat(sElement, is(sExpected));
                }

            // all messages processed from topic.
            // should be able to receive multiple nulls on complete on empty.
            // fails on second iteration.
            for (int i = 1; i <= 10; i++)
                {
                CompletableFuture<Element<String>> future = subscriber.receive();

                try
                    {
                    // the topic is empty so the future should complete with a null element
                    assertThat(future.get(1, TimeUnit.MINUTES), is(nullValue()));
                    }
                catch (Exception e)
                    {
                    fail("Failed on iteration " + i
                                 + " out of 10 to receive null element from empty topic for subscriber "
                                 + subscriber);
                    }
                }
            }
        }

    @Test
    public void shouldFillAndEmpty()
            throws Exception
        {
        int     nCount     = 1000;
        Session session    = getSession();
        String  sTopicName = ensureTopicName();
        String  sGroup     = ensureGroupName();
        
        try (Publisher<String>  publisher     = session.createPublisher(sTopicName);
             Subscriber<String> subscriberFoo = session.createSubscriber(sTopicName, inGroup(sGroup + "Foo"), completeOnEmpty());
             Subscriber<String> subscriberBar = session.createSubscriber(sTopicName, inGroup(sGroup + "Bar"), completeOnEmpty()))
            {
            for (int i = 0; i < nCount; i++)
                {
                String sElement = "Element-" + i;
                publisher.publish(sElement).get(5, TimeUnit.MINUTES);
                }

            for (int i = 0; i < nCount; i++)
                {
                String sExpected = "Element-" + i;
                String sElement  = subscriberFoo.receive().get(1, TimeUnit.MINUTES).getValue();
                assertThat(sElement, is(sExpected));
                }

            assertThat("Subscriber 'Foo' should see empty topic", subscriberFoo.receive().get(1, TimeUnit.MINUTES), is(nullValue()));

            for (int i = 0; i < nCount; i++)
                {
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
        String             sGroup = ensureGroupName();

        try (Publisher<String>  publisher     = topic.createPublisher();
             Subscriber<String> subscriberFoo = topic.createSubscriber(inGroup(sGroup + "Foo"));
             Subscriber<String> subscriberBar = topic.createSubscriber(inGroup(sGroup + "Bar")))
            {
            assertThat(publisher, is(notNullValue()));
            assertThat(subscriberFoo, is(notNullValue()));
            assertThat(subscriberBar, is(notNullValue()));

            for (int i = 0; i < nCount; i++)
                {
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

        PagedTopicCaches caches = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());
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
        String             sGroup  = ensureGroupName();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "Foo"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "Bar")))
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
        NamedTopic<String> topic     = ensureTopic();
        String             sGroupOne = ensureGroupName() + "-One";
        String             sGroupTwo = ensureGroupName() + "-Two";

        try (Subscriber<String> subscriberOne   = topic.createSubscriber(inGroup(sGroupOne));
             Subscriber<String> subscriberTwo   = topic.createSubscriber(inGroup(sGroupTwo));
             Subscriber<String> subscriberThree = topic.createSubscriber())
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("Element-0").get(2, TimeUnit.MINUTES);
                }

            assertThat(subscriberOne.receive().get(5, TimeUnit.MINUTES).getValue(), is("Element-0"));
            assertThat(subscriberTwo.receive().get(5, TimeUnit.MINUTES).getValue(), is("Element-0"));
            assertThat(subscriberThree.receive().get(5, TimeUnit.MINUTES).getValue(), is("Element-0"));
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
            CommitResult result = element.commit();
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
    public void shouldNotMissMessagesOnReconnectionAndChannelReallocation() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer);

        try (Publisher<String>            publisher     = topic.createPublisher(OrderBy.roundRobin());
             PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(withIdentifyingName("one"), inGroup("test"), completeOnEmpty());
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(withIdentifyingName("two"), inGroup("test"), completeOnEmpty()))
            {
            for (int i = 0; i < 170; i++)
                {
                publisher.publish(String.format("Message-%3d", i)).get(2, TimeUnit.MINUTES);
                }

            Element<String> elementOne = subscriberOne.receive().get(1, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));

            subscriberTwo.disconnect();
            subscriberOne.close();

            int cMessage = 0;
            Element<String> elementTwo = subscriberTwo.receive().get(1, TimeUnit.MINUTES);
            while (elementTwo != null)
                {
                cMessage++;
                elementTwo = subscriberTwo.receive().get(1, TimeUnit.MINUTES);
                }

            // Subscriber two should get the subscriber one's uncommitted message repeated
            assertThat(cMessage, is(170));
            }
        }

    @Test
    public void shouldPublishAndReceiveBatch() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Subscriber<String> subscriberOne   = topic.createSubscriber(inGroup(sGroup + "One"));
             Subscriber<String> subscriberTwo   = topic.createSubscriber(inGroup(sGroup + "Two"));
             Subscriber<String> subscriberThree = topic.createSubscriber(inGroup(sGroup + "Three")))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("Element-0").get(5, TimeUnit.MINUTES);
                publisher.publish("Element-1").get(5, TimeUnit.MINUTES);
                publisher.publish("Element-2").get(5, TimeUnit.MINUTES);
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
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Subscriber<String> subscriberOne   = topic.createSubscriber(inGroup(sGroup + "One"), completeOnEmpty());
             Subscriber<String> subscriberTwo   = topic.createSubscriber(inGroup(sGroup + "Two"), completeOnEmpty());
             Subscriber<String> subscriberThree = topic.createSubscriber(inGroup(sGroup + "Three"), completeOnEmpty()))
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
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "test"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "test")))
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

            CommitResult resultFoo = subscriber1.commit(element2.getChannel(), element2.getPosition());
            CommitResult resultBar = subscriber2.commit(element1.getChannel(), element1.getPosition());

            assertThat(resultFoo, is(notNullValue()));
            assertThat(resultBar.getStatus(), is(CommitResultStatus.Rejected));
            assertThat(resultFoo, is(notNullValue()));
            assertThat(resultBar.getStatus(), is(CommitResultStatus.Rejected));
            }
        }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void shouldGetCommits() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        int                cChannel = topic.getChannelCount();
        String             sGroup   = ensureGroupName();

        List<Subscriber<String>> listSubscriber = new ArrayList<>();
        try
            {
            for (int i = 0; i < cChannel; i++)
                {
                listSubscriber.add(topic.createSubscriber(inGroup(sGroup + "test-commits"), completeOnEmpty()));
                }

            long count = listSubscriber.stream()
                    .filter(s -> s.getChannels().length == 0)
                    .count();

            assertThat("not all subscribers have channels", count, is(not(0)));

            // no commits yet - should all return NULL_POSITION
            for (Subscriber<String> subscriber : listSubscriber)
                {
                Map<Integer, Position> mapCommit = subscriber.getLastCommitted();
                assertThat(mapCommit, is(notNullValue()));
                for (Position position : mapCommit.values())
                    {
                    assertThat(position, is(PagedPosition.NULL_POSITION));
                    }
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
                    CommitResult result = element.commit();
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
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Subscriber<String> subscriberGrouped = topic.createSubscriber(inGroup(sGroup + "test"));
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
            CommitResult result = element.commit();
            assertThat(result, is(notNullValue()));
            assertThat(result.getStatus(), is(CommitResultStatus.Committed));
            }
        }

    @Test
    public void shouldManuallyCommitGroupSubscriberByDefault() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup + "test"));
             Publisher<String>  publisher  = topic.createPublisher())
            {
            publisher.publish("value").get(2, TimeUnit.MINUTES);

            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(isCommitted(subscriber, element.getChannel(), element.getPosition()), is(false));
            CommitResult result = element.commit();
            assertThat(result, is(notNullValue()));
            assertThat(result.getStatus(), is(CommitResultStatus.Committed));
            }
        }

    @Test
    public void shouldNotStoreDataWhenNoSubscribersAndNotRewindable() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sName  = topic.getName();
        PagedTopicCaches   caches = new PagedTopicCaches(sName, (PagedTopicService) topic.getService());

        // must not be rewindable
        PagedTopicDependencies dependencies = getDependencies(topic);
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
        PagedTopicService       service      = (PagedTopicService) topic.getService();
        PagedTopicCaches        caches       = new PagedTopicCaches(sName, service);
        PagedTopicDependencies dependencies = getDependencies(topic);
        int                     cbPageSize   = dependencies.getPageCapacity();
        int                     cMessage     = cbPageSize * caches.getPartitionCount() * 3; // publish enough to have multiple pages per partition

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(100));

        // should have no pages or data
        assertThat(caches.Pages.size(), is(0));
        assertThat(caches.Data.size(), is(0));

        try (Subscriber<String> subscriberOne = topic.createSubscriber(completeOnEmpty());
             Subscriber<String> subscriberTwo = topic.createSubscriber(completeOnEmpty());
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

            // Commit the element, which is the tail of the first page, so we have fully read it
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

            // Commit the element, which is the tail of the first page, so we have fully read it
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
                // we have just read a tail element, so read another, so we're into the next page
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
        PagedTopicService       service      = (PagedTopicService) topic.getService();
        PagedTopicCaches        caches       = new PagedTopicCaches(sName, service);
        int                     cChannel     = caches.getChannelCount();
        PagedTopicDependencies dependencies = caches.getDependencies();
        int                     cbPageSize   = dependencies.getPageCapacity();
        int                     cMessage     = 500;

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(100));

        Assume.assumeThat("Test only applies if paged-topic-scheme is not set to retain pages",
            dependencies.isRetainConsumed(), is(false));

        // should not have any existing subscriptions
        assertThat(caches.Subscriptions.isEmpty(), is(true));

        try (Subscriber<String> subscriber = topic.createSubscriber(completeOnEmpty());
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
        NamedTopic<String> topic    = ensureTopic();
        int                cChannel = topic.getChannelCount();
        String             sGroup   = ensureGroupName();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup + "test-commits"), completeOnEmpty()))
            {
            int                    nChannel      = subscriber.getChannels()[0];
            Map<Integer, Position> mapHeadsStart = subscriber.getHeads();
            assertThat(mapHeadsStart, is(notNullValue()));
            assertThat(mapHeadsStart.size(), is(cChannel));

            // should have a head for each channel
            assertThat(mapHeadsStart.size(), is(cChannel));

            // no tails yet
            Map<Integer, Position> map = subscriber.getTails();
            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(cChannel));
            assertThat(map, is(mapHeadsStart));

            // add data
            int                      cMsgPerChannel = 100;
            Map<Integer, Position[]> mapHeadTail    = populateAllChannels(topic, cMsgPerChannel);
            Map<Integer, Position>   mapActualHeads = mapHeadTail.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
            Map<Integer, Position>   mapActualTails = mapHeadTail.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[1]));

            // should have no heads until first poll
            map = subscriber.getHeads();
            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(cChannel));
            assertThat(map, is(mapHeadsStart));

            // should have tails
            map = subscriber.getTails();
            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(cChannel));
            assertThat(map, is(mapActualTails));

            // read all messages
            Element<String> element = subscriber.receive().get(2, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            Element<String> elementCommit = element;
            while (element != null)
                {
                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                if (element != null)
                    {
                    elementCommit = element;
                    }
                }
            elementCommit.commit();

            // should have heads greater than initial heads
            map = subscriber.getHeads();
            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(cChannel));
            for (int c = 1; c < cChannel; c++)
                {
                assertThat(map.get(nChannel), is(greaterThan(mapActualHeads.get(c))));
                }
            }
        }

    /**
     * Returns {@code true} if all the subscribers in the specified list are allocated a distinct
     * set of channels.
     *
     * @param listSubscriber  the subscribers to verify
     * @param <V>             the type of value subscribed to
     *
     * @return {@code true} if all the subscribers in the specified list are allocated a distinct
     *         set of channels
     */
    protected <V> boolean subscribersHaveDistinctChannels(List<Subscriber<V>> listSubscriber)
        {
        Map<Integer, Integer> mapChannel = new HashMap<>();
        for (Subscriber subscriber : listSubscriber)
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
        NamedTopic<Customer> topic  = ensureCustomerTopic(m_sSerializer + "-customer-1");
        String               sGroup = ensureGroupName();

        try (Publisher<Customer>  publisher     = topic.createPublisher();
             Subscriber<Customer> subscriberFoo = topic.createSubscriber(inGroup(sGroup + "Foo"));
             Subscriber<Customer> subscriberBar = topic.createSubscriber(inGroup(sGroup + "Bar")))
            {
            Customer customer = new Customer("Mr Smith", 25, AddressExternalizableLite.getRandomAddress());
            try
                {
                publisher.publish(customer).get(5, TimeUnit.MINUTES);
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
        NamedTopic<Customer> topic  = ensureCustomerTopic(m_sSerializer + "-customer-2");
        String               sGroup = ensureGroupName();

        try (Publisher<Customer>  publisher     = topic.createPublisher();
             Subscriber<Customer> subscriberFoo = topic.createSubscriber(inGroup(sGroup + "Foo"));
             Subscriber<Customer> subscriberBar = topic.createSubscriber(inGroup(sGroup + "Bar")))
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
             Subscriber<Customer> subscriberD   = topic.createSubscriber(completeOnEmpty(),
                    withFilter(new GreaterEqualsFilter<>(new UniversalExtractor<>("id"), 12)));
             Subscriber<Customer> subscriberA   = topic.createSubscriber(completeOnEmpty(),
                    withFilter(new LessFilter<>(new UniversalExtractor<>("id"), 12))))
            {
            List<Customer> list = new ArrayList<>();

            for (int i = 0; i < 25; i++)
                {
                Customer customer = m_sSerializer.equals("pof") ?
                        new CustomerPof("Mr Smith " + i, i, AddressPof.getRandomAddress()) :
                        new CustomerExternalizableLite("Mr Smith " + i, i, AddressExternalizableLite.getRandomAddress());
                list.add(customer);
                }

            for (Customer customer : list)
                {
                assertThat(publisher.publish(customer).get(1, TimeUnit.MINUTES), is(notNullValue()));
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
        NamedTopic<Customer> topic  = ensureCustomerTopic(m_sSerializer + "-customer-4");
        String               sGroup = ensureGroupName();

        try (Subscriber<Customer> ignored = topic.createSubscriber(inGroup(sGroup + "durableSubscriber"), completeOnEmpty(),
                        withFilter(Filters.greaterEqual(Customer::getId, 12))))
            {
            assertThrows(TopicException.class, () ->
                    topic.createSubscriber(inGroup(sGroup + "durableSubscriber"), completeOnEmpty(),
                               withFilter(Filters.lessEqual(Customer::getId, 12))));
            }
        }

    @Test
    public void shouldConvertUsingAppropriateSerializer() throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-5");

        try (Publisher<Customer>  publisher           = topic.createPublisher();
             Subscriber<Integer>  subscriberOfId      = topic.createSubscriber(withConverter(Customer::getId));
             Subscriber<String>   subscriberOfName    = topic.createSubscriber(withConverter(Customer::getName));
             Subscriber<Address>  subscriberOfAddress = topic.createSubscriber(withConverter(Customer::getAddress)))
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

            for (Customer customer : list)
                {
                publisher.publish(customer).get(2, TimeUnit.MINUTES);
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

            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), is(oneOf(Address.arrAddress)));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), is(oneOf(Address.arrAddress)));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), is(oneOf(Address.arrAddress)));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), is(oneOf(Address.arrAddress)));
            assertThat(subscriberOfAddress.receive().get(2, TimeUnit.MINUTES).getValue(), is(oneOf(Address.arrAddress)));
            }
        }

    @Test
    public void shouldNotAllowSubscribersToChangeGroupConverter()
        {
        NamedTopic<Customer> topic  = ensureCustomerTopic(m_sSerializer + "-customer-6");
        String               sGroup = ensureGroupName() + "-durable-subscriber-3";

        try (Subscriber<Serializable> ignored = topic.createSubscriber(inGroup(sGroup ),
                        withConverter(Customer::getAddress)))
            {
            assertThrows(TopicException.class, () ->
                    topic.createSubscriber(inGroup(sGroup), withConverter(Customer::getId)));
            }
        }

    @Test
    public void shouldFilterAndConvert()
        throws Exception
        {
        NamedTopic<Customer> topic = ensureCustomerTopic(m_sSerializer + "-customer-7");

        try (Publisher<Customer>  publisher          = topic.createPublisher();
             Subscriber<Customer> subscriberMA       = topic.createSubscriber(completeOnEmpty(),
                    withFilter(new EqualsFilter<>(new ChainedExtractor<>("getAddress.getState"), "MA")));
            Subscriber<Address> subscriberMAAddress = topic.createSubscriber(withConverter(Customer::getAddress),
                completeOnEmpty(),
                withFilter(new EqualsFilter(new ChainedExtractor("getAddress.getState"), "MA"))))
            {
            ValueExtractor extractor = m_sSerializer.equals("pof") ?
                    new PofExtractor(String.class, new SimplePofPath(new int[] {CustomerPof.ADDRESS, AddressPof.STATE})) :
                    new ChainedExtractor(new UniversalExtractor("address"), new UniversalExtractor("state"));

            Subscriber<Customer> subscriberCA = topic.createSubscriber(completeOnEmpty(),
                    withFilter(new EqualsFilter<>(extractor, "CA")));

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
                publisher.publish(customer).get(5, TimeUnit.MINUTES);

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
            PagedTopicService      service      = (PagedTopicService) topic.getService();
            PagedTopicDependencies dependencies = getDependencies(topic);
            int                    cbPageSize   = dependencies.getPageCapacity();

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
                publisher.flush().get(5, TimeUnit.MINUTES);
                }

            int cPages = caches.Pages.size();
            assertThat(cPages, greaterThan(cPart * 3)); // we have partitions which have chains of pages

            // There should be one page per partition with a reference count of two
            ValueExtractor<Page, Integer>  extractor = Page::getReferenceCount;
            Set<Map.Entry<Page.Key, Page>> setPage   = caches.Pages.entrySet(new EqualsFilter(extractor, 2));

            assertThat(setPage.size(), is(cPart));
            // none of the entries should have a previous page (i.e. they are the first page in each partition)
            assertThat(setPage.stream().allMatch(entry -> entry.getValue().getPreviousPartitionPage() == Page.NULL_PAGE), is(true));

            // Read all the elements with subscriber A
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

            // Read all the elements with subscriber B
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
        NamedTopic<String> topic     = ensureTopic();
        int                nCount    = 99;
        String             sPrefix   = "Element-";
        TopicPublisher     publisher = new TopicPublisher(topic, sPrefix, nCount, true);
        String             sGroup    = ensureGroupName();

        try (@SuppressWarnings("unused") Subscriber<String> subscriberPin = topic.createSubscriber();
             Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup + "Foo")))
            {
            publisher.run();

            assertThat(publisher.getPublished(), is(nCount));
            TopicAssertions.assertPublishedOrder(topic, nCount, sPrefix);


            CompletableFuture<Element<String>>[] aFutures = new CompletableFuture[nCount];

            for (int i=0; i<aFutures.length; i++)
                {
                aFutures[i] = subscriber.receive();
                }

            CompletableFuture.allOf(aFutures).get(5, TimeUnit.MINUTES);

            for (int i=0; i<aFutures.length; i++)
                {
                Element<String> element = aFutures[i].get(1, TimeUnit.MINUTES);
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
        NamedTopic<String> topic     = ensureTopic();
        int                nCount    = 1000;
        String             sPrefix   = "Element-";
        TopicPublisher     publisher = new TopicPublisher(topic, sPrefix, nCount, true);
        String             sGroup    = ensureGroupName();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "Foo"));
             Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "Bar")))
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
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable");
        String             sGroup = ensureGroupName();

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        String sPrefix = "Element-";
        int    nCount  = 123;

        try (Publisher<String> publisher = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i).get(5, TimeUnit.MINUTES);
                }
            }

        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup + "one")))
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
        NamedTopic<String> topic   = ensureTopic();
        String             sPrefix = "Element-";
        int                nCount  = 123;
        String             sGroup  = ensureGroupName();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "one"));
             Publisher<String> publisher = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i).get(5, TimeUnit.MINUTES);
                }

            // read everything with subscriber one but do not commit anything
            for (int i = 0; i<nCount; i++)
                {
                Element<String> element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + i));
                }

            // create subscriber two
            try (Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "one")))
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
        String                                     sGroup         = ensureGroupName();

        try
            {
            // creating the same number of subscribers as channels guarantees that there will be one channel per subscriber
            for (int i = 0; i < cChannel; i ++)
                {
                listSubscriber.add(topic.createSubscriber(inGroup(sGroup), completeOnEmpty()));
                }

            // Channel allocation changes are notified async, so we need to wait until all subscribers have one channel
            // and all channels are allocated to a subscriber
            try
                {
                Eventually.assertDeferred(() -> subscribersHaveDistinctChannels(listSubscriber), is(true));
                }
            catch (AssertionError assertionError)
                {
                System.err.println("Not all subscribers have one channel");
                for (Subscriber<OrderableMessage<String>> subscriber : listSubscriber)
                    {
                    System.err.println(subscriber);
                    }
                throw assertionError;
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
        String             sGroup       = ensureGroupName();
        int                nCount       = 300;
        Element<String>    element;
        int                cChannel;

        System.err.println("Creating Subscribers for topic " + topic.getName() + " in group " + sGroup);
        try (Subscriber<String> subscriber1  = topic.createSubscriber(inGroup(sGroup));
             Subscriber<String> subscriber2  = topic.createSubscriber(inGroup(sGroup));
             Subscriber<String> subscriber3  = topic.createSubscriber(inGroup(sGroup)))
            {
            Map<Integer, List<String>> mapReceived = new ConcurrentHashMap<>();
            Map<Integer, List<String>> mapSent     = new ConcurrentHashMap<>();
            AtomicInteger              nOrder      = new AtomicInteger();
            int                        cMessages   = 0;

            // publish to all the channels to ensure all subscribers have messages
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

            Set<ChannelPosition>   setPosition = new HashSet<>();
            Map<Integer, Position> commits1    = new HashMap<>();
            Map<Integer, Position> commits2    = new HashMap<>();
            Map<Integer, Position> commits3    = new HashMap<>();
            int                    cReceived   = 0;

            System.err.println("Subscriber 1 " + subscriber1);
            System.err.println("Subscriber 2 " + subscriber2);
            System.err.println("Subscriber 3 " + subscriber3);

            // read some messages with all subscribers, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (1): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits1.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (2): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits2.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits3.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                cReceived += 3;
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

            System.err.println("Closing Subscriber 1 " + subscriber1);
            // close subscriber 1 - it's channels will be reallocated
            subscriber1.close();

            // Wait for the channels to all be reallocated
            // In real life we don't do this as we do not guarantee once only, but we need to do
            // this for the test to be stable
            Eventually.assertDeferred(() -> subscriber2.getChannels().length, is(not(0)));
            Eventually.assertDeferred(() -> subscriber3.getChannels().length, is(is(not(0))));
            System.err.println("Waiting for Subscriber 2 and 3 to have " + cChannel + " channels");

            try
                {
                Eventually.assertDeferred(() -> subscriber2.getChannels().length + subscriber3.getChannels().length, is(cChannel));
                }
            catch (AssertionError assertionError)
                {
                System.err.println("FAILED: Subscriber 2 " + subscriber2);
                System.err.println("FAILED: Subscriber 3 " + subscriber3);
                throw assertionError;
                }

            System.err.println("Subscriber 2 has channels " + subscriber2);
            System.err.println("Subscriber 3 has channels " + subscriber3);

            // read some messages with remaining subscribers, but do not commit anything
            for (int i = 0; i<19; i++)
                {
                element = subscriber2.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (2): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits2.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                commits3.put(element.getChannel(), element.getPosition());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                cReceived += 2;
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
            System.err.println("Closing Subscriber 2 " + subscriber2);
            subscriber2.close();

            // Wait for the channels to all be reallocated
            // In real life we don't do this as we do not guarantee once only, but we need to do
            // this for the test to be stable
            System.err.println("Waiting for Subscriber 3 to have " + cChannel + " channels");
            Eventually.assertDeferred(() -> subscriber3.getChannels().length, is(cChannel));

            System.err.println("Subscriber 3 has channels " + subscriber3);

            // read all messages left using subscriber 3
            while (cReceived < cMessages)
                {
                element = subscriber3.receive().get(1, TimeUnit.MINUTES);
                assertThat(element, is(notNullValue()));
                listLog.add("Received (3): " + element.getValue() + " from " + element.getPosition());
                mapReceived.get(element.getChannel()).add(element.getValue());
                assertThat("Duplicate " + element.getValue(), setPosition.add(new ChannelPosition(element)), is(true));
                cReceived++;
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
             Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
            {
            Future<Element<String>> future = subscriber.receive();
            assertThat(future.isDone(), is(false));

            publisher.publish("blah");

            assertThat(future.get(1, TimeUnit.MINUTES).getValue(), is("blah"));
            }
        }

    @Test
    public void shouldShareWaitNotificationOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        String             sGroup   = ensureGroupName() + "subscriber";
        int                cChannel = topic.getChannelCount();

        try(PagedTopicSubscriber<String> subscriber1 = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroup));
            PagedTopicSubscriber<String> subscriber2 = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroup)))
            {
            System.err.println("shouldShareWaitNotificationOnEmptyTopic: Created subscriber1 id=" + subscriber1.getId());
            System.err.println("shouldShareWaitNotificationOnEmptyTopic: Created subscriber2 id=" + subscriber2.getId());
            Future<Element<String>> future1 = subscriber1.receive();
            Future<Element<String>> future2 = subscriber2.receive();

            // should eventually stop polling when all owned channels have been determined to be empty
            long start      = Base.getSafeTimeMillis();
            long polls1     = subscriber1.getPolls();
            long polls2     = subscriber2.getPolls();
            long pollsPrev1 = -1;
            long pollsPrev2 = -1;

            while (polls1 != pollsPrev1 && polls2 != pollsPrev2)
                {
                long now = Base.getSafeTimeMillis();
                assertThat("Timed out waiting for the subscribers to stop polling",
                           now - start, is(lessThan(TimeUnit.MINUTES.toMillis(2))));
                Thread.sleep(100);
                pollsPrev1 = polls1;
                polls1 = subscriber1.getPolls();
                pollsPrev2 = polls2;
                polls2 = subscriber2.getPolls();
                }

            // publish to all channels so that both subscriber get something
            try (Publisher<String> publisher = topic.createPublisher(OrderBy.roundRobin()))
                {
                for (int i = 0; i < cChannel; i++)
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

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
            {
            publisher.publish("blah");
            assertThat(subscriber.receive().get(1, TimeUnit.MINUTES).getValue(), is("blah"));

            Future<Element<String>> future = subscriber.receive();
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

            String sGroup1 = ensureGroupName() + "-one";
            String sGroup2 = ensureGroupName() + "-two";

            try (Subscriber<String> subFoo = topic.createSubscriber(inGroup(sGroup1));
                 Subscriber<String> subBar = topic.createSubscriber(inGroup(sGroup2)))
                {
                assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{sGroup1, sGroup2}).getSet()));

                topic.destroySubscriberGroup(sGroup1);

                assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{sGroup2}).getSet()));
                }
            }
        }

    @Test
    public void shouldNotMissMessagesWhenCancellingFutures() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Publisher<String>            publisher  = topic.createPublisher();
             PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(ensureGroupName())))
            {
            long cWait = subscriber.getWaitCount();
            System.err.println(">>>> Calling receive on subscriber: " + subscriber);
            CompletableFuture<Element<String>> futureOne = subscriber.receive();
            CompletableFuture<Element<String>> futureTwo = subscriber.receive();

            // allow subscriber to enter wait state
            System.err.println(">>>> Waiting for subscriber wait count to be greater than " + cWait);
            Eventually.assertDeferred(subscriber::getWaitCount, is(greaterThan(cWait)));
            System.err.println(">>>> Subscriber wait count has increased " +subscriber);
            assertThat(futureOne.isDone(), is(false));
            assertThat(futureTwo.isDone(), is(false));

            System.err.println(">>>> Cancelling future " + futureOne);
            boolean fCancelled = futureOne.cancel(true);
            assertThat(fCancelled, is(true));
            assertThat(futureOne.isDone(), is(true));
            assertThat(futureOne.isCancelled(), is(true));

            publisher.publish("message-one").get(1, TimeUnit.MINUTES);
            publisher.publish("message-two").get(1, TimeUnit.MINUTES);
            publisher.publish("message-three").get(1, TimeUnit.MINUTES);

            Eventually.assertDeferred(futureTwo::isDone, is(true), Timeout.after(5, TimeUnit.MINUTES));
            assertThat(futureTwo.isCancelled(), is(false));

            Element<String> element = futureTwo.get();
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("message-one"));

            element = subscriber.receive().get(5, TimeUnit.MINUTES);
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is("message-two"));
            }
        }

    @Test
    public void shouldCancelWaitOnClose() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
            {
            Future<Element<String>> future = subscriber.receive();
            Thread.sleep(100); // allow subscriber to enter wait state
            subscriber.close();
            assertThat(future.isCancelled(), is(true));
            }
        }

    @Test
    public void shouldNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber"), completeOnEmpty()))
            {
            Future<Element<String>> future = subscriber.receive();

            assertThat(future.get(1, TimeUnit.MINUTES), is(nullValue()));
            }
        }

    @Test
    public void shouldDrainAndNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber"), completeOnEmpty()))
            {
            try (Publisher<String> publisher = topic.createPublisher())
                {
                publisher.publish("blah").get(5, TimeUnit.MINUTES);
                }

            assertThat(subscriber.receive().get(1, TimeUnit.MINUTES).getValue(), is("blah"));

            Future<Element<String>> future = subscriber.receive();

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

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
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

            try
                {
                Eventually.assertDeferred(cReceive::get, is(cMessage));
                }
            finally
                {
                thread.interrupt(); // the thread may be blocked on flow control
                }

            Eventually.assertDeferred(thread::isAlive, is(false));
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

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
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
        PagedTopicDependencies      dependencies = getDependencies(topic);
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

            // will throw if the backlog is too big, or it takes more than five minutes - which is a crazy amount of time
            future.get(5, TimeUnit.MINUTES);
            }
        }

    @Test
    @Ignore("This test is flawed as it needs a message in almost every partition to guarantee to pass but that can cause the test to take too long and fail anyway")
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
                                    .whenCompleteAsync((r, e) ->
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
    @Ignore("Skipped until we can figure out why the test hangs")
    public void shouldThrowWhenFull()
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
                          getDependencies(topic).getServerCapacity(), is(SERVER_CAPACITY));

        // create a subscriber group to ensure messages are retained and the topic wil fill up
        topic.ensureSubscriberGroup(ensureGroupName() + "-group");

        int  cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long nHigh   = SERVER_CAPACITY / cbValue; // from config

        try(Publisher<String>  publisher = topic.createPublisher(Publisher.FailOnFull.enabled()))
            {
            for (int i = 0; i < nHigh * 2; ++i)
                {
                try
                    {
                    publisher.publish("Element-" + i).get(5, TimeUnit.MINUTES);
                    }
                catch (InterruptedException | TimeoutException e)
                    {
                    fail("Failed to publish element " + i + ". Expected CompletionException but caught " + e);
                    }
                }
            fail("Publish loop should not have completed"); // we're not supposed to finish
            }
        catch (CompletionException | ExecutionException e)
            {
            // expected
            assertThat(e.getCause(), is(instanceOf(TopicPublisherException.class)));
            Throwable cause = e.getCause().getCause();
            assertThat(cause, is(instanceOf(IllegalStateException.class)));
            assertThat(cause.getMessage().contains("topic is at capacity"), is(true));
            }
        }

    @Test
    @Ignore("Skipped until we can figure out why the test hangs")
    public void shouldCloseWhenFull()
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
                          getDependencies(topic).getServerCapacity(), is(SERVER_CAPACITY));

        try (Subscriber<String> ignored = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
            {
            int  cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
            long nHigh   = SERVER_CAPACITY / cbValue; // from config

            try(Publisher<String> publisher = topic.createPublisher(Publisher.FailOnFull.enabled()))
                {
                for (int i = 0; i < nHigh * 2; ++i)
                    {
                    try
                        {
                        publisher.publish("Element-" + i).get(5, TimeUnit.MINUTES);
                        }
                    catch (InterruptedException | TimeoutException e)
                        {
                        fail("Failed to publish element " + i + ". Expected CompletionException but caught " + e);
                        }
                    }
                fail("Publish loop should not have completed"); // we're not supposed to finish
                }
            catch (CompletionException | ExecutionException e)
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

        try (@SuppressWarnings("unused") Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber")))
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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-expiring");

        Assume.assumeThat("Test only applies when paged-topic-scheme has non-zero expiry configured",
                          getDependencies(topic).getElementExpiryMillis() != 0, is(true));

        String sPrefix = "Element-";
        int    nCount  = 20;

        try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(ensureGroupName() + "subscriber"), completeOnEmpty());
             Publisher<String> publisher  = topic.createPublisher())
            {
            for (int i=0; i<nCount; i++)
                {
                publisher.publish(sPrefix + i);
                }

            // expiry is two seconds
            Thread.sleep(3000);

            assertThat(subscriber.receive().get(1, TimeUnit.MINUTES), is(nullValue()));
            }
        }

    @Test
    public void shouldStartSecondSubscriberFromCorrectPosition() throws Exception
        {
        NamedTopic<String> topic   = ensureTopic();
        String             sPrefix = "Element-";
        int                nCount  = 100;
        String             sGroup  = ensureGroupName();

        try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "Foo")))
            {

            try (Publisher<String> publisher = topic.createPublisher())
                {
                for (int i=0; i<nCount; i++)
                    {
                    publisher.publish(sPrefix + i).get(5, TimeUnit.MINUTES);
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

            try (Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "Foo")))
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
            publisher.publish(sPrefix + 1).get(5, TimeUnit.MINUTES);

            Subscriber<String> subscriber = topic.createSubscriber();

            Future<Element<String>> future = subscriber.receive();

            publisher.publish(sPrefix + 2);

            assertThat(future.get(1, TimeUnit.MINUTES).getValue(), is(sPrefix + 2));
            }
        }

    @Test
    public void shouldGroupConsumeFromLastCommittedIfReceiveAfterSubscriberClose() throws Exception
        {
        NamedTopic<String> topic   = ensureTopic();
        String             sPrefix = "Element-";
        String             sGroup  = ensureGroupName();

        try (Publisher<String> publisher = topic.createPublisher())
            {
            publisher.publish(sPrefix + 1).get(5, TimeUnit.MINUTES);

            try (Subscriber<String> subscriber1 = topic.createSubscriber(inGroup(sGroup + "foo")))
                {
                Future<Element<String>> future = subscriber1.receive();

                publisher.publish(sPrefix + 2);
                publisher.publish(sPrefix + 3);

                Element<String> element = future.get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + 2));

                // commit element-2
                element.commit();
                // receive element-3
                element = subscriber1.receive().get(1, TimeUnit.MINUTES);
                assertThat(element.getValue(), is(sPrefix + 3));

                // ensure that a new member to the group doesn't reset the groups position in the topic, i.e. this subscriber
                // instance can see items created before it joined the group, as position is defined by the group not the
                // instance

                try (Subscriber<String> subscriber2 = topic.createSubscriber(inGroup(sGroup + "foo")))
                    {
                    // close subscriber-1, which should reposition back to the last committed element-2, so the next read is element-3
                    subscriber1.close();

                    Future<Element<String>> future2 = subscriber2.receive();

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
     * <p/>
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
     * <p/>
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

        String               sTopicName = ensureTopicName();
        NamedTopic<Customer> topic1     = null;
        NamedTopic<Customer> topic2;

        Session session = getSession();
        for (int i = 0; i < assertion1.length; i++)
            {
            assertEquals(assertion1[i], assertion2[i]);
            String sName = m_sSerializer + "-" + sTopicName + "-" + i;
            topic1 = session.getTopic(sName, assertion1[i]);
            topic2 = session.getTopic(sName, assertion2[i]);
            assertThat("testing " + assertion1[i], topic1 == topic2);
            }

        final NamedTopic topic = topic1;

        topic.destroy();
        Eventually.assertDeferred(topic::isDestroyed, is(true));
        }

    @Test
    public void shouldSeekEmptyTopic() throws Exception
        {
        NamedTopic<String> topic        = ensureTopic();
        String             sGroupPrefix = ensureGroupName();

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "two")))
            {
            Random random = new Random(System.currentTimeMillis());
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                Position position = subscriberOne.seek(nChannel, new PagedPosition(random.nextInt(10000), random.nextInt(100)));
                assertThat(position, is(new PagedPosition(0, 0)));
                }

            assertSubscribersAtTail(subscriberOne, subscriberTwo);
            }
        }

    @Test
    public void shouldSeekGroupSubscriberForwards() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-3");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
            }

        String sGroupPrefix = ensureGroupName();

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-two")))
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
    public void shouldSeekGroupSubscriberForwardsAfterReadingSome() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-small-rewindable");

        Assume.assumeThat("Test only applies when the paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        Map<PagedPosition, String> mapPublished = new HashMap<>();
        String                     sSuffix      = "abcdefghijklmnopqrstuvwxyz";
        int                        nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 1000; i++)
                {
                String sMsg   = "element-" + i + sSuffix;
                CompletableFuture<Status> future = publisher.publish(sMsg);
                Status status = future.get(1, TimeUnit.MINUTES);
                int nChannelPublished = status.getChannel();
                assertThat(nChannelPublished, is(nChannel));
                mapPublished.put((PagedPosition) status.getPosition(), sMsg);
                }
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        String sGroupPrefix = ensureGroupName();
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-one"), Subscriber.CompleteOnEmpty.enabled());
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "-two")))
            {
            // move subscriber two on by receiving pages
            // (we'll then seek subscriber one to the same place)
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 200; i++)
                {
                future = subscriberTwo.receive();
                }

            // Obtain the last received element for subscriber two
            Element<String> element       = future.get(2, TimeUnit.MINUTES);
            PagedPosition   pagedPosition = (PagedPosition) element.getPosition();

            // Read some messages from subscriber one before seeking, as we buffer
            // we should have fetched the page back and need to clear the buffer on seeking
            for (int i = 0; i < 50; i++)
                {
                future = subscriberOne.receive();
                }
            future.get(2, TimeUnit.MINUTES);

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

            Map<PagedPosition, String> mapReceived = new HashMap<>();
            // Poll the next element for each subscriber, they should match
            Element<String> elementTwo = subscriberTwo.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementTwo, is(notNullValue()));
            Element<String> elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
            assertThat(elementOne, is(notNullValue()));
            assertThat(elementOne.getPosition(), is(elementTwo.getPosition()));
            assertThat(elementOne.getValue(), is(elementTwo.getValue()));

            while (elementOne != null)
                {
                mapReceived.put((PagedPosition) elementOne.getPosition(), elementOne.getValue());
                elementOne = subscriberOne.receive().get(2, TimeUnit.MINUTES);
                }

            mapPublished.keySet().removeIf(p -> p.compareTo(pagedPosition) <= 0);
            assertThat(mapReceived, is(mapPublished));
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsToEndOfPage() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-4");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            // Seek subscriber one to the last position read by subscription two
            System.err.println("Seeking to: " + pagedPosition);
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-5");
        int                cMsg  = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
                          getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < cMsg; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            assertSubscribersAtTail(subscriberOne, subscriberTwo);
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsToEndOfTopicWhereTopicEndsOnPageEnd() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic(m_sSerializer + "-rewindable-6");
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());
        int                cMsg   = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;
        int                       i;
        Status                    status;
        PagedPosition             positionLast;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
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
            int nChannelPublished = status.getChannel();
            assertThat(nChannelPublished, is(nChannel));
            }

        // get the last Page of the topic
        Page page  = caches.Pages.get(new Page.Key(nChannel, positionLast.getPage()));

        // publish more messages until the Page gets sealed (i.e. it is full)
        if (!page.isSealed())
            {
            try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
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

            assertSubscribersAtTail(subscriberOne, subscriberTwo);
            }
        }

    @Test
    public void shouldSeekAnonymousSubscriberForwardsPastEndOfTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-5");
        int                cMsg  = 10019;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < cMsg; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            assertSubscribersAtTail(subscriberOne, subscriberTwo);
            }
        }

    @Test
    public void shouldSeekGroupSubscriberBackwards() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-3");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        String sGroupPrefix = ensureGroupName();
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "two")))
            {
            // move subscriber one on by receiving pages
            CompletableFuture<Element<String>> future = null;
            for (int i = 0; i < 8000; i++)
                {
                future = subscriberOne.receive();
                }
            // wait for the last receive to complete
            future.get(2, TimeUnit.MINUTES);

            // move subscriber two not as far as subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-3");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10001; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
            }

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        String sGroupPrefix = ensureGroupName();
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "two")))
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
            CommitResult commitResult = element.commit();
            System.err.println("Committed One at: " + commitResult.getPosition());

            // move subscriber two not as far as subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-4");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // Create two subscribers in different groups.
        // We will receive messages from one and then seek the other to the same place
        String sGroupPrefix = ensureGroupName();
        try (PagedTopicSubscriber<String> subscriberOne = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "one"));
             PagedTopicSubscriber<String> subscriberTwo = (PagedTopicSubscriber<String>) topic.createSubscriber(inGroup(sGroupPrefix + "two")))
            {
            // publish a lot os messages, so we have multiple pages spread over all the partitions
            CompletableFuture<Status> futurePublish = null;
            int                       nChannel      = 1;

            try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
                {
                for (int i = 0; i < 10001; i++)
                    {
                    futurePublish = publisher.publish("element-" + i);
                    }
                publisher.flush().get(2, TimeUnit.MINUTES);
                // Get the channel messages were published to
                int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
                assertThat(nChannelPublished, is(nChannel));
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
            CommitResult commitResult = element.commit();
            System.err.println("Committed One at: " + commitResult.getPosition());

            // move subscriber two not as far as subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
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
        PagedTopicCaches   caches = new PagedTopicCaches(topic.getName(), (PagedTopicService) topic.getService());

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            // move subscriber two not as far as subscriber one by receiving fewer messages (we'll then seek subscriber one back to the same place)
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

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-4");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            // Seek subscriber one to the last position read by subscription two
            Position result = subscriberOne.seek(element.getChannel(), pagedPosition);

            // should have seeked to the correct position
            assertThat(result, is(pagedPosition));

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
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-rewindable-4");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        // publish a lot os messages, so we have multiple pages spread over all the partitions
        CompletableFuture<Status> futurePublish = null;
        int                       nChannel      = 1;

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10019; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            // Get the channel messages were published to
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));
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

            // Seek subscriber one to the last position read by subscription two
            PagedPosition result = (PagedPosition) subscriberOne.seek(element.getChannel(), new PagedPosition(pagedPosition.getPage() - 1, 10));

            // should have seeked to the correct position
            assertThat(result.getPage(), is(pagedPosition.getPage() - 1));

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
        NamedTopic<String> topic    = ensureTopic(m_sSerializer + "-rewindable-5");
        int                nChannel = 1;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));


        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            CompletableFuture<Status> futurePublish = null;
            for (int i = 0; i < 10000; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);
            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));

            try (PagedTopicSubscriber<String> subscriber = (PagedTopicSubscriber<String>) topic.createSubscriber(completeOnEmpty()))
                {
                Map<Integer, Position> map = subscriber.seekToTail(nChannel);
                assertThat(map.get(nChannel), is(notNullValue()));

                CompletableFuture<Element<String>> future      = subscriber.receive();
                Element<String>                    elementTail = future.get(2, TimeUnit.MINUTES);

                assertThat(elementTail, is(nullValue()));

                // the subscriber thinks the topic is empty and will be waiting on a notification
                // that the topic now has a message, this is async so if we immediately call
                // receive it may return null, so we wait for the notification count to go up

                // Get the current notification count
                long cNotification = subscriber.getNotify();

                // publish the message, which should trigger a notification
                publisher.publish("element-last").get(5, TimeUnit.MINUTES);

                // wait for the subscriber's notification count to go up
                Eventually.assertDeferred(subscriber::getNotify, is(greaterThan(cNotification)));

                future      = subscriber.receive();
                elementTail = future.get(2, TimeUnit.MINUTES);

                assertThat(elementTail, is(notNullValue()));
                assertThat(elementTail.getValue(), is("element-last"));
                }
            }
        }

    @Test
    public void shouldSeekToHead() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic(m_sSerializer + "-rewindable-5");
        int                nChannel = 1;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            for (int i = 0; i < 10000; i++)
                {
                int n = i;
                String sMsg = "element-" + i;
                publisher.publish(sMsg)
                        .thenAcceptAsync(status ->
                            {
                            m_testWatcher.println(">>>> Published message " + n + " '" + sMsg + "' status=" + status);
                            });
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            try (Subscriber<String> subscriber = topic.createSubscriber(completeOnEmpty()))
                {
                Element<String> elementHead = subscriber.receive().get(2, TimeUnit.MINUTES);
                assertThat(elementHead, is(notNullValue()));
                assertThat(elementHead.getValue(), is("element-0"));

System.setProperty("test.log.page", String.valueOf(((PagedPosition) elementHead.getPosition()).getPage()));

                CompletableFuture<Element<String>> future = null;
                for (int i = 0; i < 5000; i ++)
                    {
                    future = subscriber.receive();
                    }
                Element<String> element     = future.get(2, TimeUnit.MINUTES);
                int             nChannelSub = element.getChannel();

                assertThat(nChannelSub, is(nChannel));

                // seek to the head of the channel
                Map<Integer, Position> map = subscriber.getHeads();
                System.err.println(">>>> Current subscriber heads are " + map);
                System.err.println(">>>> Seeking subscriber to head for channel " + nChannelSub);
                map = subscriber.seekToHead(nChannelSub);
                System.err.println(">>>> Seeked to head: " + map);

                System.err.println(">>>> Calling receive on : " + subscriber);
                element = subscriber.receive().get(2, TimeUnit.MINUTES);
                System.err.println(">>>> Received element : " + element);
                assertThat(element, is(notNullValue()));
                assertThat(element.getValue(), is(elementHead.getValue()));
                assertThat(element.getPosition(), is(elementHead.getPosition()));
                }
            }
        }

    @Test
    public void shouldSeekToHeadRollingBackCommit() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic(m_sSerializer + "-rewindable-5");
        String             sGroup   = ensureGroupName();
        int                nChannel = 1;

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            getDependencies(topic).isRetainConsumed(), is(true));

        try (Publisher<String> publisher = topic.createPublisher(OrderBy.id(nChannel)))
            {
            CompletableFuture<Status> futurePublish = null;
            for (int i = 0; i < 10000; i++)
                {
                futurePublish = publisher.publish("element-" + i);
                }
            publisher.flush().get(2, TimeUnit.MINUTES);

            int nChannelPublished = futurePublish.get(2, TimeUnit.MINUTES).getChannel();
            assertThat(nChannelPublished, is(nChannel));

            Element<String> elementHead;

            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup), completeOnEmpty()))
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
            try (Subscriber<String> subscriber = topic.createSubscriber(inGroup(sGroup), completeOnEmpty()))
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
        PagedTopicDependencies dependencies = getDependencies(topic);

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

            try (Subscriber<String> subscriber = topic.createSubscriber(completeOnEmpty()))
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

    @Test
    public void shouldCountRemainingMessages() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();
        int                cTotal = 1000;

        try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup(sGroup));
             Subscriber<String> subscriberTwo = topic.createSubscriber(inGroup(sGroup));
             Publisher<String>  publisher     = topic.createPublisher(OrderBy.roundRobin()))
            {
            for (int i = 0; i < 1000; i++)
                {
                publisher.publish("message-" + i).get(1, TimeUnit.MINUTES);
                }

            assertThat(topic.getRemainingMessages(sGroup), is(cTotal));

            Map<Integer, Integer> mapCount = new HashMap<>();
            int                   cMessage = 0;
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                int c = topic.getRemainingMessages(sGroup, nChannel);
                cMessage += c;
                mapCount.put(nChannel, c);
                }

            assertThat(cMessage, is(cTotal));

            int cOne = subscriberOne.getRemainingMessages();
            int cTwo = subscriberTwo.getRemainingMessages();
            assertThat(cOne + cTwo, is(cTotal));

            for (int nChannel : subscriberOne.getChannels())
                {
                assertThat(subscriberOne.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }

            for (int nChannel : subscriberTwo.getChannels())
                {
                assertThat(subscriberTwo.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }
            }
        }

    @Test
    public void shouldCountRemainingMessagesAfterSeekToTailWithCommit() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();
        int                cTotal = 1000;

        try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup(sGroup));
             Subscriber<String> subscriberTwo = topic.createSubscriber(inGroup(sGroup));
             Publisher<String>  publisher     = topic.createPublisher(OrderBy.roundRobin()))
            {
            for (int i = 0; i < 1000; i++)
                {
                publisher.publish("Before-Messages-" + i).get(1, TimeUnit.MINUTES);
                }

            assertThat(topic.getRemainingMessages(sGroup), is(cTotal));

            Map<Integer, Integer> mapCount = new HashMap<>();
            int                   cMessage = 0;
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                int c = topic.getRemainingMessages(sGroup, nChannel);
                cMessage += c;
                mapCount.put(nChannel, c);
                }

            assertThat(cMessage, is(cTotal));

            int cOne = subscriberOne.getRemainingMessages();
            int cTwo = subscriberTwo.getRemainingMessages();
            assertThat(cOne + cTwo, is(cTotal));

            for (int nChannel : subscriberOne.getChannels())
                {
                assertThat(subscriberOne.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }

            for (int nChannel : subscriberTwo.getChannels())
                {
                assertThat(subscriberTwo.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }

            CompletableFuture<Element<String>> futureOneBefore = subscriberOne.receive();
            CompletableFuture<Element<String>> futureTwoBefore = subscriberTwo.receive();

            // work around for BUG 34164633 - possible race between an async receive and seek.
            futureOneBefore.get(5, TimeUnit.MINUTES);
            futureTwoBefore.get(5, TimeUnit.MINUTES);

            subscriberOne.seekToTailAndCommit(subscriberOne.getChannels());
            subscriberTwo.seekToTailAndCommit(subscriberTwo.getChannels());

            // We did seek and commit so the remaining count should be zero
            assertThat(subscriberOne.getRemainingMessages(), is(0));
            assertThat(subscriberTwo.getRemainingMessages(), is(0));

            CompletableFuture<Element<String>> futureOne = subscriberOne.receive();
            CompletableFuture<Element<String>> futureTwo = subscriberTwo.receive();

            for (int i = 2000; i < 2100; i++)
                {
                publisher.publish("After-Messages-" + i).get(1, TimeUnit.MINUTES);
                }

            Element<String> elementOneBefore = futureOneBefore.get(1, TimeUnit.MINUTES);
            Element<String> elementOneAfter  = futureOne.get(1, TimeUnit.MINUTES);
            Element<String> elementTwoBefore = futureTwoBefore.get(1, TimeUnit.MINUTES);
            Element<String> elementTwoAfter  = futureTwo.get(1, TimeUnit.MINUTES);

            assertThat(elementOneBefore.getValue(), startsWith("Before-"));
            assertThat(elementOneAfter.getValue(), startsWith("After-"));
            assertThat(elementTwoBefore.getValue(), startsWith("Before-"));
            assertThat(elementTwoAfter.getValue(), startsWith("After-"));
            }
        }

    @Test
    public void shouldCountRemainingMessagesAfterSeekToTailWithoutCommit() throws Exception
        {
        NamedTopic<String> topic  = ensureTopic();
        String             sGroup = ensureGroupName();
        int                cTotal = 1000;

        try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup(sGroup));
             Subscriber<String> subscriberTwo = topic.createSubscriber(inGroup(sGroup));
             Publisher<String>  publisher     = topic.createPublisher(OrderBy.roundRobin()))
            {
            System.err.println(">>>>> 1: Publishing " + cTotal + " messages");
            for (int i = 0; i < cTotal; i++)
                {
                publisher.publish("Before-Messages-" + i).get(1, TimeUnit.MINUTES);
                }

            assertThat(topic.getRemainingMessages(sGroup), is(cTotal));

            Map<Integer, Integer> mapCount = new HashMap<>();
            int                   cMessage = 0;
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                int c = topic.getRemainingMessages(sGroup, nChannel);
                cMessage += c;
                mapCount.put(nChannel, c);
                }

            assertThat(cMessage, is(cTotal));

            int cOne = subscriberOne.getRemainingMessages();
            int cTwo = subscriberTwo.getRemainingMessages();
            assertThat(cOne + cTwo, is(cTotal));

            for (int nChannel : subscriberOne.getChannels())
                {
                assertThat(subscriberOne.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }

            for (int nChannel : subscriberTwo.getChannels())
                {
                assertThat(subscriberTwo.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                }

            CompletableFuture<Element<String>> futureOneBefore = subscriberOne.receive();
            CompletableFuture<Element<String>> futureTwoBefore = subscriberTwo.receive();

            // work around for BUG 34164633 - possible race between an async receive and seek.
            futureOneBefore.get(5, TimeUnit.MINUTES);
            futureTwoBefore.get(5, TimeUnit.MINUTES);

            @SuppressWarnings("unused")
            Map<Integer, Position> mapTailOne = subscriberOne.seekToTail(subscriberOne.getChannels());
            @SuppressWarnings("unused")
            Map<Integer, Position> mapTailTwo = subscriberTwo.seekToTail(subscriberTwo.getChannels());

            // We did not seek and commit so the remaining count should not have changed,
            // If the subscribers were closed we would roll all the way back
            assertThat(subscriberOne.getRemainingMessages(), is(cOne));
            assertThat(subscriberTwo.getRemainingMessages(), is(cTwo));

            CompletableFuture<Element<String>> futureOne = subscriberOne.receive();
            CompletableFuture<Element<String>> futureTwo = subscriberTwo.receive();

            for (int i = 2000; i < 2100; i++)
                {
                publisher.publish("After-Messages-" + i).get(1, TimeUnit.MINUTES);
                }

            Element<String> elementOneBefore = futureOneBefore.get(1, TimeUnit.MINUTES);
            Element<String> elementOneAfter  = futureOne.get(1, TimeUnit.MINUTES);
            Element<String> elementTwoBefore = futureTwoBefore.get(1, TimeUnit.MINUTES);
            Element<String> elementTwoAfter  = futureTwo.get(1, TimeUnit.MINUTES);

            assertThat(elementOneBefore.getValue(), startsWith("Before-"));
            assertThat(elementOneAfter.getValue(), startsWith("After-"));
            assertThat(elementTwoBefore.getValue(), startsWith("Before-"));
            assertThat(elementTwoAfter.getValue(), startsWith("After-"));
            }
        }

    @Test
    public void shouldPurgeSubscriberGroup() throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        int                cChannel = topic.getChannelCount();
        String             sGroup   = ensureGroupName();
        int                cTotal   = 1000;

        topic.ensureSubscriberGroup(sGroup);

        try (Publisher<String>  publisher = topic.createPublisher(OrderBy.roundRobin()))
            {
            for (int i = 0; i < 1000; i++)
                {
                publisher.publish("Before-Messages-" + i).get(1, TimeUnit.MINUTES);
                }

            assertThat(topic.getRemainingMessages(sGroup), is(cTotal));

            Map<Integer, Integer> mapCount = new HashMap<>();
            int                   cMessage = 0;
            for (int nChannel = 0; nChannel < topic.getChannelCount(); nChannel++)
                {
                int c = topic.getRemainingMessages(sGroup, nChannel);
                cMessage += c;
                mapCount.put(nChannel, c);
                }

            assertThat(cMessage, is(cTotal));

            try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup(sGroup));
                 Subscriber<String> subscriberTwo = topic.createSubscriber(inGroup(sGroup)))
                {
                Eventually.assertDeferred(() -> subscriberOne.getChannels().length + subscriberTwo.getChannels().length, is(cChannel));

                int cOne = subscriberOne.getRemainingMessages();
                int cTwo = subscriberTwo.getRemainingMessages();
                assertThat(cOne + cTwo, is(cTotal));

                for (int nChannel : subscriberOne.getChannels())
                    {
                    assertThat(subscriberOne.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                    }

                for (int nChannel : subscriberTwo.getChannels())
                    {
                    assertThat(subscriberTwo.getRemainingMessages(nChannel), is(mapCount.get(nChannel)));
                    }
                }

            topic.destroySubscriberGroup(sGroup);
            assertThat(topic.getRemainingMessages(sGroup), is(0));

            try (Subscriber<String> subscriberOne = topic.createSubscriber(inGroup(sGroup));
                 Subscriber<String> subscriberTwo = topic.createSubscriber(inGroup(sGroup)))
                {
                assertThat(subscriberOne.getRemainingMessages(), is(0));
                assertThat(subscriberTwo.getRemainingMessages(), is(0));

                for (int nChannel : subscriberOne.getChannels())
                    {
                    assertThat(subscriberOne.getRemainingMessages(nChannel), is(0));
                    }

                for (int nChannel : subscriberTwo.getChannels())
                    {
                    assertThat(subscriberTwo.getRemainingMessages(nChannel), is(0));
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that {@code subscriberTest} has been positioned at the tail of the topic.
     * <p>
     * Subscriber {@code subscriberExpected} has been positioned at the tail by calling {@link Subscriber#receive()}
     * whereas {@code subscriberTest} has been positioned by a call to {@link Subscriber#seek(int, Position)}.
     *
     * @param subscriberTest      the {@link Subscriber} to seek
     * @param subscriberExpected  the {@link Subscriber} already at the tail
     * @throws Exception if an error occurs
     */
    protected void assertSubscribersAtTail(Subscriber<String> subscriberTest, Subscriber<String> subscriberExpected) throws Exception
        {
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

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     * <p/>
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     */
    private void validateTopicMBeans(String sSuffix) throws Exception
        {
        Session            session    = getSession();
        String             sTopicName = ensureTopicName(sSuffix);
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
     * <p/>
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

        for (int attempt = 0; attempt < 5; attempt++)
            {
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
                try
                    {
                    assertThat("MBean attribute constraint check: units * unitFactor", cUnits, greaterThan(nMessageSize * cMessages));
                    assertThat("MBean attribute constraint check: size", cSize, is(cMessages));
                    }
                catch (Throwable e)
                    {
                    System.err.println("Failed (attempt=" + attempt + ") " + e.getMessage());
                    Thread.sleep(1000);
                    if (attempt == 4)
                        {
                        throw Exceptions.ensureRuntimeException(e);
                        }
                    }
                }
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
    protected void populate(Publisher<String> publisher, int nMsgSize, int nCount) throws Exception
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
        publisher.flush().get(5, TimeUnit.MINUTES);
        System.err.println("**** Published " + nCount + " messages");
        }

    protected synchronized NamedTopic<String> ensureTopic()
        {
        if (m_topic == null)
            {
            String sName = ensureTopicName();
            System.err.println("Ensuring NamedTopic " + sName);
            m_topic = getSession().getTopic(sName);
            }

        return (NamedTopic<String>) m_topic;
        }

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
        System.err.println("Ensuring topic name (existing name " + m_sTopicName + ")");
        if (m_sTopicName == null)
            {
            m_sTopicName = sPrefix + "-" + m_nTopic.incrementAndGet();
            }
        return m_sTopicName;
        }

    protected PagedTopicDependencies getDependencies(NamedTopic<?> topic)
        {
        CacheService                service      = (CacheService) topic.getService();
        PagedTopicBackingMapManager mgr          = (PagedTopicBackingMapManager) service.getBackingMapManager();
        return mgr.getTopicDependencies(topic.getName());
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
        public Statement apply(Statement base, Description d)
            {
            try
                {
                Class<?> testClass = d.getTestClass();
                File folder = MavenProjectFileUtils.ensureTestOutputBaseFolder(testClass);
                m_out = new PrintWriter(new FileWriter(new File(folder, testClass.getSimpleName() + ".log"), true));
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            return super.apply(base, d);
            }

        @Override
        protected void starting(Description d)
            {
            m_sName = d.getMethodName();
            System.err.println(">>>>> Starting test: " + m_sName + " in class " + d.getTestClass());
            System.err.flush();
            m_out.println(">>>>> Starting test: " + m_sName + " in class " + d.getTestClass());
            m_out.flush();
            }

        @Override
        protected void succeeded(Description description)
            {
            System.err.println(">>>>> Test Passed: " + m_sName);
            System.err.flush();
            m_out.println(">>>>> Test Passed: " + m_sName);
            m_out.flush();
            }

        @Override
        protected void failed(Throwable e, Description description)
            {
            System.err.println(">>>>> Test Failed: " + m_sName);
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            System.err.println("<<<<<");
            System.err.flush();
            m_out.println(">>>>> Test Failed: " + m_sName);
            e.printStackTrace(m_out);
            m_out.println("<<<<<");
            m_out.flush();
            }

        @Override
        protected void skipped(AssumptionViolatedException e, Description description)
            {
            System.err.println(">>>>> Test Skipped: " + m_sName);
            System.err.flush();
            m_out.println(">>>>> Test Skipped: " + m_sName);
            m_out.flush();
            }

        @Override
        protected void finished(Description description)
            {
            m_out.flush();
            m_out.close();
            super.finished(description);
            }

        protected void println(String sMessage)
            {
            m_out.println(sMessage);
            m_out.flush();
            }

        /**
         * @return the name of the currently-running test method
         */
        public String getMethodName() {
            return m_sName;
        }

        // ----- data members -----------------------------------------------

        private volatile String m_sName;
        
        private PrintWriter m_out;
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

    @ClassRule
    public static final ThreadDumpOnTimeoutRule timeout = ThreadDumpOnTimeoutRule.after(30, TimeUnit.MINUTES);

    @Rule
    public Watcher m_testWatcher = new Watcher();

    // MUST BE STATIC because JUnit creates a new test class per test method
    protected static final AtomicInteger m_nTopic = new AtomicInteger(0);

    // MUST BE STATIC because JUnit creates a new test class per test method
    protected static final AtomicInteger m_nGroup = new AtomicInteger(0);

    protected String               m_sSerializer;
    protected NamedTopic<?>        m_topic;
    protected String               m_sTopicName;
    protected NamedTopic<Customer> m_topicCustomer;
    protected ExecutorService      m_executorService = Executors.newFixedThreadPool(4);
    }
