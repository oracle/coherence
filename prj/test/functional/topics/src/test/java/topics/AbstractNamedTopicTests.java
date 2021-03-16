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

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.internal.net.topic.impl.paged.Configuration;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicPartition;

import com.tangosol.io.pof.reflect.SimplePofPath;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.FlowControl;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Session;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Publisher.OrderBy;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;
import com.tangosol.net.topic.Subscriber.Element;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;
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

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;

import topics.data.Address;
import topics.data.AddressExternalizableLite;
import topics.data.AddressPof;
import topics.data.Customer;
import topics.data.CustomerExternalizableLite;
import topics.data.CustomerPof;

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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.tangosol.net.topic.Subscriber.Name.of;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author jk 2015.05.28
 */
public abstract class AbstractNamedTopicTests
    {
    // ----- constructors ---------------------------------------------------

    protected AbstractNamedTopicTests(String sSerializer)
        {
        m_sSerializer = sSerializer;
        }

    // ----- test lifecycle methods -----------------------------------------

    @After
    public void cleanup()
            throws Exception
        {
        if (m_topic != null)
            {
            m_topic.destroy();
            m_topic = null;
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldFilter() throws Exception
        {
        NamedTopic<String> topic         = ensureTopic();
        Publisher<String>  publisher     = topic.createPublisher();
        Subscriber<String> subscriberD   = topic.createSubscriber(Subscriber.Filtered.by(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "d")));
        Subscriber<String> subscriberA   = topic.createSubscriber(Subscriber.Filtered.by(new GreaterFilter<>(IdentityExtractor.INSTANCE(), "a")));
        Subscriber<String> subscriberLen = topic.createSubscriber(Subscriber.Filtered.by(new GreaterFilter<>(String::length, 1)));

        publisher.send("a");
        publisher.send("zoo");
        publisher.send("b");
        publisher.send("c");
        publisher.send("d");
        publisher.send("e");
        publisher.send("f");

        assertThat(subscriberD.receive().get().getValue(), is("zoo"));
        assertThat(subscriberD.receive().get().getValue(), is("e"));
        assertThat(subscriberD.receive().get().getValue(), is("f"));

        assertThat(subscriberA.receive().get().getValue(), is("zoo"));
        assertThat(subscriberA.receive().get().getValue(), is("b"));
        assertThat(subscriberA.receive().get().getValue(), is("c"));
        assertThat(subscriberA.receive().get().getValue(), is("d"));
        assertThat(subscriberA.receive().get().getValue(), is("e"));
        assertThat(subscriberA.receive().get().getValue(), is("f"));

        assertThat(subscriberLen.receive().get().getValue(), is("zoo"));
        }

    @Test
    public void shouldConvert() throws Exception
        {
        NamedTopic<String>  topic       = ensureTopic();
        Publisher<String>   publisher   = topic.createPublisher();
        Subscriber<Integer> subscriber1 = topic.createSubscriber(Subscriber.Convert.using(Integer::parseInt));
        Subscriber<Integer> subscriber2 = topic.createSubscriber(Subscriber.Convert.using(String::length));

        publisher.send("1");
        publisher.send("22");
        publisher.send("333");
        publisher.send("4444");
        publisher.send("55555");

        assertThat(subscriber1.receive().get().getValue(), is(1));
        assertThat(subscriber1.receive().get().getValue(), is(22));
        assertThat(subscriber1.receive().get().getValue(), is(333));
        assertThat(subscriber1.receive().get().getValue(), is(4444));
        assertThat(subscriber1.receive().get().getValue(), is(55555));

        assertThat(subscriber2.receive().get().getValue(), is(1));
        assertThat(subscriber2.receive().get().getValue(), is(2));
        assertThat(subscriber2.receive().get().getValue(), is(3));
        assertThat(subscriber2.receive().get().getValue(), is(4));
        assertThat(subscriber2.receive().get().getValue(), is(5));
        }

    // See COH-15185 why test is ignored.
    //@Test
    public void shouldCleanupAnonymousSubscriber() throws Exception
        {
        NamedTopic<String>      topic            = ensureTopic();
        DistributedCacheService service          = (DistributedCacheService) topic.getService();
        NamedCache              cacheSubscribers = service.ensureCache(PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(m_sSerializer), null);

        Assume.assumeThat("Test only applies if client is storage disabled", service.getOwnershipEnabledMembers().contains(service.getCluster().getLocalMember()), is(false));

        assertThat(cacheSubscribers.isEmpty(), is(true));

        Subscriber<String> subscriber = topic.createSubscriber();
        Publisher<String>  publisher  = topic.createPublisher();

        assertThat(cacheSubscribers.isEmpty(), is(false));

        // subscribers are cleaned up after member death *and* a new page is created
        ((SafeService) service).getService().stop();

        service.ensureCache(PagedTopicCaches.Names.PAGES.cacheNameForTopicName(m_sSerializer), null).size();

        while (!cacheSubscribers.isEmpty()) // eventually we'll touch all partitions and have removed all dead subscribers
            {
            publisher.send("foo");
            }

        assertThat(cacheSubscribers.isEmpty(), is(true));
        }

    // See COH-15158
    // @Test
    public void shouldRestartTopicClients() throws Exception
        {
        NamedTopic<String> topic      = ensureTopic();
        PartitionedService service    = (PartitionedService) topic.getService();

        Assume.assumeThat("Test only applies if client is storage disabled", service.getOwnershipEnabledMembers().contains(service.getCluster().getLocalMember()), is(false));

        Publisher<String>  publisher  = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber();

        ((SafeService) service).getService().stop();

        publisher.send("test");
        subscriber.receive().get();

        publisher.close();
        subscriber.close();

        publisher = topic.createPublisher();
        subscriber = topic.createSubscriber();

        publisher.send("test");
        subscriber.receive().get();
        }

    /**
     * Test defaulting for {@link NamedTopic.Option} {@link ValueTypeAssertion}.
     *
     * @throws Exception
     */
    @Test
    public void shouldCreateTopicNoValueTypeAssertionOption() throws Exception
        {
        Session            session = getSession();
        String             sName   = m_sSerializer + "-raw-test";
        NamedTopic<String> topic   = session.getTopic(sName);

        populate(topic.createPublisher(), 20);

        topic.destroy();
        }

    @Test
    public void shouldCreateAndDestroyTopic() throws Exception
        {
        Session                  session = getSession();
        String                   sName   = m_sSerializer + "-test";
        final NamedTopic<String> topic   = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }
        assertTrue("assert topic is active", topic.isActive());
        assertFalse("assert topic is not destroyed", topic.isDestroyed());

        topic.destroy();
        Eventually.assertDeferred("Assert topic " + sName + " is not active",
                                  () -> topic.isActive(), Matchers.is(false),
                                  DeferredHelper.within(30, TimeUnit.SECONDS));
        Eventually.assertDeferred("Assert topic " + sName + " is destroyed",
                                  () -> topic.isDestroyed(), Matchers.is(true),
                                  DeferredHelper.within(30, TimeUnit.SECONDS));
        assertTrue(topic.isReleased());


        NamedTopic<String> topic2 = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        populate(topic2.createPublisher(), 20);
        }

    @Test
    public void shouldCreateAndReleaseTopic() throws Exception
        {
        Session                  session = getSession();
        String                   sName   = m_sSerializer + "-test";
        final NamedTopic<String> topic   = session.getTopic(sName, ValueTypeAssertion.withType(String.class));

        try (Publisher<String> publisher = topic.createPublisher())
            {
            populate(publisher, 20);
            }
        assertTrue("assert topic is active", topic.isActive());
        assertFalse("assert topic is not released", topic.isReleased());
        topic.release();
        Eventually.assertDeferred("Assert topic " + sName + " is released",
                                  () -> topic.isReleased(), Matchers.is(true),
                                  DeferredHelper.within(15, TimeUnit.SECONDS));
        assertFalse(topic.isActive());
        }

    @Test
    public void shouldRunOnPublisherClose()
        {
        final AtomicBoolean fRan = new AtomicBoolean(false);

        NamedTopic<String> topic     = ensureTopic();
        Publisher          publisher = topic.createPublisher();

        publisher.onClose(() -> fRan.set(true));
        publisher.close();
        assertTrue(fRan.get());
        }

    @Test
    public void shouldRunOnSubscriberClose()
        {
        final AtomicBoolean fRan = new AtomicBoolean(false);

        NamedTopic<String> topic      = ensureTopic();
        Subscriber         subscriber = topic.createSubscriber();

        subscriber.onClose(() -> fRan.set(true));
        subscriber.close();
        assertTrue(fRan.get());
        }

    @Test
    public void shouldFillAndEmpty()
            throws Exception
        {
        int                nCount    = 1000;
        NamedTopic<String> topic     = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();

        try (Subscriber<String> subscriberFoo = topic.createSubscriber(of("Foo"), Subscriber.CompleteOnEmpty.enabled());
             Subscriber<String> subscriberBar = topic.createSubscriber(of("Bar"), Subscriber.CompleteOnEmpty.enabled()))
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

                publisher.send(sElement).join();
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
                String sElement  = subscriberFoo.receive().get().getValue();

                assertThat(sElement, is(sExpected));
                }

            System.out.println();

            assertThat("Subscriber 'Foo' should see empty topic", subscriberFoo.receive().get(), is(nullValue()));

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
                String sElement  = subscriberBar.receive().get().getValue();

                assertThat(sElement, is(sExpected));
                }

            System.out.println();

            assertThat("Subscriber 'Bar' should see empty topic", subscriberBar.receive().get(), is(nullValue()));
            }
        }

    @Test
    public void shouldFillAndEmptyOneByOne()
            throws Exception
        {
        int                nCount        = 1000;
        NamedTopic<String> topic         = ensureTopic();
        Publisher<String>  publisher     = topic.createPublisher();
        Subscriber<String> subscriberFoo = topic.createSubscriber(of("Foo"), Subscriber.CompleteOnEmpty.enabled());
        Subscriber<String> subscriberBar = topic.createSubscriber(of("Bar"), Subscriber.CompleteOnEmpty.enabled());

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

            publisher.send(sElement).get();

            assertThat(subscriberFoo.receive().get().getValue(), is(sElement));
            assertThat(subscriberBar.receive().get().getValue(), is(sElement));

            assertThat(subscriberFoo.receive().get(), is(nullValue()));
            assertThat(subscriberBar.receive().get(), is(nullValue()));
            }

        assertThat("Subscriber 'Bar' should see empty topic", subscriberBar.receive().get(), is(nullValue()));
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

        assertPopulateAndConsumeInParallel(publisher);
        }

    @Test
    public void shouldPopulateWithOrderByValueAndConsume() throws Exception
        {
        ToIntFunction<String> getValue = s ->
            {
            return 5;
            };

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
        boolean fVerifyOrder = option == null || ! (option instanceof Publisher.OrderByNone);

        assertPopulateAndConsumeInParallel(new TopicPublisher(topic, "", 10, false, option), fVerifyOrder);
        }

    public void assertPopulateAndConsumeInParallel(TopicPublisher publisher, boolean fVerifyOrder) throws Exception
        {
        NamedTopic<String> topic       = publisher.getTopic();
        int                nCount      = publisher.getCount();
        String             sPrefix     = publisher.getPrefix();
        TopicSubscriber    subscriber1 = new TopicSubscriber(topic.createSubscriber(of("Foo")), sPrefix, nCount, 2, TimeUnit.MINUTES, false, fVerifyOrder);
        TopicSubscriber    subscriber2 = new TopicSubscriber(topic.createSubscriber(of("Bar")), sPrefix, nCount, 2, TimeUnit.MINUTES, false, fVerifyOrder);

        Future<?> futurePublisher = m_executorService.submit(publisher);
        Future<?> futureSubscriber1 = m_executorService.submit(subscriber1);
        Future<?> futureSubscriber2 = m_executorService.submit(subscriber2);

        futurePublisher.get();
        futureSubscriber1.get();
        futureSubscriber2.get();

        assertThat(publisher.getPublished(), is(nCount));
        assertThat(subscriber1.getConsumedCount(), is(nCount));
        assertThat(subscriber2.getConsumedCount(), is(nCount));
        }

    public void assertPopulateAndConsumeInParallel(TopicPublisher publisher) throws Exception
        {
        assertPopulateAndConsumeInParallel(publisher, /*fVerifyOrder*/ true);
        }

    @Test
    public void shouldOfferAndPoll()
            throws Exception
        {
        NamedTopic<String> topic         = ensureTopic();
        Publisher<String>  publisher     = topic.createPublisher();
        Subscriber<String> subscriberFoo = topic.createSubscriber(of("Foo"));
        Subscriber<String> subscriberBar = topic.createSubscriber(of("Bar"));

        assertThat(publisher.send("Element-0").get(), is(nullValue()));
        assertThat(subscriberFoo.receive().get().getValue(), is("Element-0"));
        assertThat(subscriberBar.receive().get().getValue(), is("Element-0"));
        }

    @Test
    public void shouldOfferAndPollUsingJavaSerializationOnly()
        throws Exception
        {
        NamedTopic<Customer> topic         = ensureCustomerTopic(m_sSerializer + "-customer-1");
        Publisher<Customer>  publisher     = topic.createPublisher();
        Subscriber<Customer> subscriberFoo = topic.createSubscriber(of("Foo"));
        Subscriber<Customer> subscriberBar = topic.createSubscriber(of("Bar"));

        Customer customer = new Customer("Mr Smith", 25, AddressExternalizableLite.getRandomAddress());
        try
            {
            assertThat(publisher.send(customer).get(), is(nullValue()));
            assertThat(subscriberFoo.receive().get().getValue().equals(customer), is(true));
            assertThat(subscriberBar.receive().get().getValue().equals(customer), is(true));

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
            assertTrue(IOException.class.isAssignableFrom(e.getCause().getClass()));
            // ignore IOException for pof serialization, it is expected due to payload is only Serializable.
            }
        }

    @Test
    public void shouldOfferAndPollUsingAppropriateSerializer()
        throws Exception
        {
        NamedTopic<Customer> topic         = ensureCustomerTopic(m_sSerializer + "-customer-1");
        Publisher<Customer>  publisher     = topic.createPublisher();
        Subscriber<Customer> subscriberFoo = topic.createSubscriber(of("Foo"));
        Subscriber<Customer> subscriberBar = topic.createSubscriber(of("Bar"));

        Customer customer = m_sSerializer.equals("pof") ?
            new CustomerPof("Mr Smith", 25, AddressPof.getRandomAddress()) :
            new CustomerExternalizableLite("Mr Smith", 25, AddressExternalizableLite.getRandomAddress());
        assertThat(publisher.send(customer).get(), is(nullValue()));
        assertThat(subscriberFoo.receive().get().getValue().equals(customer), is(true));
        assertThat(subscriberBar.receive().get().getValue().equals(customer), is(true));
        }

    @Test
    public void shouldFilterUsingAppropriateSerializer()
        throws Exception
        {
        NamedTopic<Customer> topic         = ensureCustomerTopic(m_sSerializer + "-customer-1");
        Publisher<Customer>  publisher     = topic.createPublisher();
        Subscriber<Customer> subscriberD   = topic.createSubscriber(CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new GreaterEqualsFilter<>(new UniversalExtractor<>("id"), 12)));
        Subscriber<Customer> subscriberA   = topic.createSubscriber(CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new LessFilter<>(new UniversalExtractor<>("id"), 12)));

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
            assertThat(publisher.send(cust).get(), is(nullValue()));
            }

        int cSubscriberDReceive = 0;
        for (CompletableFuture<Element<Customer>> future = subscriberD.receive(); future.get() != null ; future = subscriberD.receive(), cSubscriberDReceive++)
            {
            Customer customer = future.get().getValue();
            assertThat(customer.getId(), greaterThanOrEqualTo(12));
            }
        assertThat(cSubscriberDReceive, is(13));

        int cSubscriberAReceive = 0;
        for (CompletableFuture<Element<Customer>> future = subscriberA.receive(); future.get() != null ; future = subscriberA.receive(), cSubscriberAReceive++)
            {
            Customer customer = future.get().getValue();
            assertThat(customer.getId(), lessThan(12));
            }
        assertThat(cSubscriberAReceive, is(12));

        publisher.close();
        subscriberA.close();
        subscriberD.close();
        }

    @Test
    public void shouldUseLastFilterForGroupSubscribers()
        throws Exception
        {
        NamedTopic<Customer> topic         = ensureCustomerTopic(m_sSerializer + "-customer-1");
        Publisher<Customer>  publisher     = topic.createPublisher();
        Subscriber<Customer> subscriberD   = topic.createSubscriber(of("durableSubscriber"), CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new GreaterEqualsFilter<>(new UniversalExtractor<>("id"), 12)));

        // effective Filter for group subscriber of "durableSubscriber" is lessFilter("id", 12) since it is last provided.
        // Only one Filter over all group subscribers for same group.
        Subscriber<Customer> subscriberA   = topic.createSubscriber(of("durableSubscriber"), CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new LessFilter<>(new UniversalExtractor<>("id"), 12)));

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
            assertThat(publisher.send(cust).get(), is(nullValue()));
            }

        int cSubscriberAReceive = 0;
        int cSubscriberDReceive = 0;

        CompletableFuture<Element<Customer>> future = subscriberD.receive();

        while (future.get() != null)
            {
            Customer customer = future.get().getValue();
            assertThat(customer.getId(), lessThan(12));
            cSubscriberDReceive++;

            future = subscriberA.receive();
            if (future.get() != null)
                {
                customer = future.get().getValue();
                assertThat(customer.getId(), lessThan(12));
                cSubscriberAReceive++;

                future = subscriberD.receive();
                }
            }
        assertThat(cSubscriberDReceive + cSubscriberAReceive, is(12));

        publisher.close();
        subscriberA.close();
        subscriberD.close();
        }

    @Test
    public void shouldConvertUsingAppropriateSerializer() throws Exception
        {
        NamedTopic<Customer> topic               = ensureCustomerTopic(m_sSerializer + "-customer-2");
        Publisher<Customer>  publisher           = topic.createPublisher();
        Subscriber<Integer>  subscriberOfId      = topic.createSubscriber(Subscriber.Convert.using(Customer::getId));
        Subscriber<String>   subscriberOfName    = topic.createSubscriber(Subscriber.Convert.using(Customer::getName));
        Subscriber<Address>  subscriberOfAddress = topic.createSubscriber(Subscriber.Convert.using(Customer::getAddress));

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
            assertThat(publisher.send(cust).get(), is(nullValue()));
            }

        assertThat(subscriberOfId.receive().get().getValue(), is(1));
        assertThat(subscriberOfId.receive().get().getValue(), is(2));
        assertThat(subscriberOfId.receive().get().getValue(), is(3));
        assertThat(subscriberOfId.receive().get().getValue(), is(4));
        assertThat(subscriberOfId.receive().get().getValue(), is(5));

        assertThat(subscriberOfName.receive().get().getValue(), is("Mr Smith 1"));
        assertThat(subscriberOfName.receive().get().getValue(), is("Mr Smith 2"));
        assertThat(subscriberOfName.receive().get().getValue(), is("Mr Smith 3"));
        assertThat(subscriberOfName.receive().get().getValue(), is("Mr Smith 4"));
        assertThat(subscriberOfName.receive().get().getValue(), is("Mr Smith 5"));

        assertThat(subscriberOfAddress.receive().get().getValue(), isOneOf(Address.arrAddress));
        assertThat(subscriberOfAddress.receive().get().getValue(), isOneOf(Address.arrAddress));
        assertThat(subscriberOfAddress.receive().get().getValue(), isOneOf(Address.arrAddress));
        assertThat(subscriberOfAddress.receive().get().getValue(), isOneOf(Address.arrAddress));
        assertThat(subscriberOfAddress.receive().get().getValue(), isOneOf(Address.arrAddress));
        }

    @Test
    public void shouldUseLastConverterForGroupSubscribers() throws Exception
        {
        NamedTopic<Customer> topic               = ensureCustomerTopic(m_sSerializer + "-customer-2");
        Publisher<Customer>  publisher           = topic.createPublisher();
        Subscriber           subscriberOfAddress = topic.createSubscriber(of("durable-subscriber-3"), Subscriber.Convert.using(Customer::getAddress));
        Subscriber           subscriberOfId      = topic.createSubscriber(of("durable-subscriber-3"), Subscriber.Convert.using(Customer::getId));

        // effective Converter for group subscribers of "durable-subscriber-3" is Customer::getName since it is last provided.
        // only one Converter over all group subscribers of subscriber group "durable-subsscriber-3".
        Subscriber           subscriberOfName    = topic.createSubscriber(of("durable-subscriber-3"), Subscriber.Convert.using(Customer::getName));

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
            assertThat(publisher.send(cust).get(), is(nullValue()));
            }

        CompletableFuture<Element<String>> future = subscriberOfId.receive();

        assertThat(future.get().getValue(), is("Mr Smith 1"));

        future = subscriberOfAddress.receive();
        assertThat(future.get().getValue(), is("Mr Smith 2"));

        future = subscriberOfName.receive();
        assertThat(future.get().getValue(), is("Mr Smith 3"));

        future = subscriberOfId.receive();
        assertThat(future.get().getValue(), is("Mr Smith 4"));

        future = subscriberOfAddress.receive();
        assertThat(future.get().getValue(), is("Mr Smith 5"));
        }

    @Test
    public void shouldFilterAndConvert()
        throws Exception
        {
        NamedTopic<Customer> topic              = ensureCustomerTopic(m_sSerializer + "-customer-1");
        Publisher<Customer>  publisher          = topic.createPublisher();
        Subscriber<Customer> subscriberMA       = topic.createSubscriber(CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new EqualsFilter<>(new ChainedExtractor<>("getAddress.getState"), "MA")));
        Subscriber<Address> subscriberMAAddress = topic.createSubscriber(Subscriber.Convert.using(Customer::getAddress),
            CompleteOnEmpty.enabled(),
            Subscriber.Filtered.by(new EqualsFilter(new ChainedExtractor("getAddress.getState"), "MA"))
            );

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
            assertThat(publisher.send(customer).get(), is(nullValue()));

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
        for (CompletableFuture<Element<Customer>> future = subscriberMA.receive(); future.get() != null ; future = subscriberMA.receive(), cSubscriberMAReceive++)
            {
            Customer customer = future.get().getValue();
            assertThat(customer.getAddress().getState(), is("MA"));
            }
        assertThat("number of customers in MA", cSubscriberMAReceive, is(cMA));

        int cSubscriberCAReceive = 0;
        for (CompletableFuture<Element<Customer>> future = subscriberCA.receive(); future.get() != null ; future = subscriberCA.receive(), cSubscriberCAReceive++)
            {
            Customer customer = future.get().getValue();
            assertThat(customer.getAddress().getState(), is("CA"));
            }
        assertThat("number of customers in CA", cSubscriberCAReceive, is(cCA));

        int cSubscriberMAAddress = 0;
        for (CompletableFuture<Element<Address>> future = subscriberMAAddress.receive(); future.get() != null ; future = subscriberMAAddress.receive(), cSubscriberMAAddress++)
            {
            Address address = future.get().getValue();
            assertThat(address.getState(), is("MA"));
            }
        assertThat("number of customer addresses in MA", cSubscriberMAAddress, is(cMA));

        publisher.close();
        subscriberMA.close();
        subscriberCA.close();
        subscriberMAAddress.close();
        }

    @Test
    public void shouldOfferAndPollAcrossPages()
          throws Exception
        {
        NamedTopic<String> topic      = ensureTopic();
        Publisher<String>  publisher  = topic.createPublisher();
        Subscriber<String> subscriberA = topic.createSubscriber();
        Subscriber<String> subscriberB = topic.createSubscriber();

        int  cbPageSize = topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getPageCapacity();

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(100));

        int  cPart      = ((PartitionedService) topic.getService()).getPartitionCount();
        int  cChan      = PagedTopicCaches.getChannelCount(cPart);
        int  cRecords   = cbPageSize * cPart; // ensure we use every partition a few times

        NamedCache cachePages = ((CacheService) topic.getService()).ensureCache(PagedTopicCaches.Names.PAGES.cacheNameForTopicName(m_sSerializer), null);
        assertThat(cachePages.size(), is(0)); // subs waiting on Usage rather then page

        for (int i = 0; i < cRecords; ++i)
            {
            publisher.send(Integer.toString(i));
            }

        publisher.flush().join();

        int cPages = cachePages.size();
        assertThat(cPages, greaterThan(cPart * 3)); // we have partitions which have chains of pages

        for (int i = 0; i < cRecords; ++i)
            {
            assertThat(subscriberA.receive().get().getValue(), is(Integer.toString(i)));
            }

        assertThat(cachePages.size(), is(cPages + cChan - 1)); // cPages populated pages, subA waits on last, cChan empty pages in each chan also waited by subA

        for (int i = 0; i < cRecords; ++i)
            {
            assertThat(subscriberB.receive().get().getValue(), is(Integer.toString(i)));
            }

        assertThat(cachePages.size(), is(cChan));  // all empty pages, waited on by subA and subB // TODO: can we put them back on Usage and avoid the page cost?
        }

    @Test
    public void shouldOfferAsyncAndMaintainOrder()
            throws Exception
        {
        NamedTopic<String>  topic     = ensureTopic();
        Publisher<String>   publisher = topic.createPublisher();
        Subscriber<String>  subscriber = topic.createSubscriber(); // ensure published data is retained
        String              sPrefix   = "Element-";
        int                 cOffers   = 100;
        CompletableFuture[] aFutures  = new CompletableFuture[cOffers];

        for (int i=0; i<cOffers; i++)
            {
            String sElement = sPrefix + i;
            aFutures[i] = publisher.send(sElement);
            }

        CompletableFuture.allOf(aFutures).get();

        TopicAssertions.assertPublishedOrder(topic, cOffers, sPrefix);

        subscriber.close();
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
        Subscriber<String> subscriberPin = topic.createSubscriber();

        Future<?> futurePub1 = m_executorService.submit(publisher1);
        Future<?> futurePub2 = m_executorService.submit(publisher2);

        futurePub1.get();
        futurePub2.get();

        assertThat(publisher1.getPublished(), is(nCount));
        assertThat(publisher2.getPublished(), is(nCount));

        TopicAssertions.assertPublishedOrder(topic, nCount, "1-Element-", "2-Element-");

        subscriberPin.close();
        }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldConsumeAsynchronously() throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 99;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);
        Subscriber<String> subscriberPin = topic.createSubscriber();
        Subscriber<String> subscriber = topic.createSubscriber(Subscriber.Name.of("Foo"));

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
            Subscriber.Element<String> element = aFutures[i].get();
            assertThat(element, is(notNullValue()));
            assertThat(element.getValue(), is(sPrefix + i));
            }

        subscriberPin.close();
        }

    @Test
    public void shouldConsumeInParallel()
            throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 1000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);
        TopicSubscriber    subscriber1 = new TopicSubscriber(topic.createSubscriber(), sPrefix, nCount, 3, TimeUnit.MINUTES, true);
        TopicSubscriber    subscriber2 = new TopicSubscriber(topic.createSubscriber(), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        publisher.run();

        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        Future<?> futureSubscriber1 = m_executorService.submit(subscriber1);
        Future<?> futureSubscriber2 = m_executorService.submit(subscriber2);

        futureSubscriber1.get();
        futureSubscriber2.get();

        assertThat(subscriber1.getConsumedCount(), is(nCount));
        assertThat(subscriber2.getConsumedCount(), is(nCount));
        }

    static class MultiplexedSubscriber<V>
        implements Subscriber<V>
        {
        public MultiplexedSubscriber(NamedTopic<V> topic, String sSubscriber, int cSubscribers)
            {
            f_aSubscriber = new Subscriber[cSubscribers];

            for (int i = 0; i < cSubscribers; ++i)
                {
                f_aSubscriber[i] = topic.createSubscriber(of(sSubscriber));
                }
            }

        @Override
        public CompletableFuture<Element<V>> receive()
            {
            return f_aSubscriber[m_c++ % f_aSubscriber.length].receive();
            }

        @Override
        public FlowControl getFlowControl()
            {
            return null;
            }

        @Override
        public void onClose(Runnable action)
            {
            m_listOnCloseActions.add(action);
            }

        @Override
        public boolean isActive()
            {
            return Arrays.stream(f_aSubscriber)
                        .allMatch(Subscriber::isActive);
            }

        @Override
        public void close()
            {
            for (Subscriber<V> sub : f_aSubscriber)
                {
                sub.close();
                }
            }

        protected final Subscriber<V>[] f_aSubscriber;

        int m_c;

        protected List<Runnable> m_listOnCloseActions = new ArrayList<>();
        }

    @Test
    public void shouldShareConsumption()
            throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 1000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);

        TopicSubscriber subscriber = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "Foo", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        publisher.run();

        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        subscriber.run();

        assertThat(subscriber.getConsumedCount(), is(nCount));
        }

    @Test
    public void validateConfiguredSubscriberGroup()
            throws Exception
        {
        Assume.assumeThat("Test only applies if topic-mapping has a subscriber-group configured",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-subscriber-group-1");
        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"durable-subscriber"}).getSet()));

        // Publish
        int            nCount    = 1000;
        String         sPrefix   = "Element-";
        TopicPublisher publisher = new TopicPublisher(topic, sPrefix, nCount, true);

        publisher.run();
        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        // Subscribe: validate topic-mapping configured subscriber-group "durable-subscriber"
        TopicSubscriber subscriber = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "durable-subscriber", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        subscriber.run();

        assertThat(subscriber.getConsumedCount(), is(nCount));
        }

    @Test
    public void validateDestroyOfConfiguredSubscriberGroup()
        throws Exception
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
        TopicSubscriber subscriber = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "durable-subscriber", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        topic.destroySubscriberGroup("durable-subscriber");
        CacheService service = (CacheService) topic.getService();
        NamedCache cacheSubscribers = service.ensureCache(PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName), null);
        Eventually.assertThat(cacheSubscribers.getCacheName() + " should be empty",
            invoking(cacheSubscribers).isEmpty(), is(true));
        assertThat(topic.getSubscriberGroups().size(), is(0));

        // explicitly destroy topic with statically configured subscriber group that was destroyed.
        topic.destroy();
        m_topic = null;

        // validate that statically configured subscriber group is configured when topic is recreated.
        topic = ensureTopic(sTopicName);
        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"durable-subscriber"}).getSet()));
        }

    @Test
    public void validateConfiguredSubscriberGroupWithAnonymousSubscriber()
            throws Exception
        {
        Assume.assumeThat("Test only applies when topping-mapping has a subscriber-group",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-subscriber-group-1");
        int nCount = 1000;
        String sPrefix = "Element-";
        TopicPublisher publisher = new TopicPublisher(topic, sPrefix, nCount, true);

        publisher.run();

        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        // can create subscriber after publishing to topic.  the topic-mapping configured durable-subscriber already existed
        TopicSubscriber subscriber = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "durable-subscriber", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        Subscriber<String> anonSubscriber = topic.createSubscriber(CompleteOnEmpty.enabled());
        assertThat(anonSubscriber.receive().get(), nullValue());

        anonSubscriber.close();

        subscriber.run();

        assertThat(subscriber.getConsumedCount(), is(nCount));
        }

    @Test
    public void shouldShareAndDuplicateConsumption()
            throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        int                nCount      = 1000;
        String             sPrefix     = "Element-";
        TopicPublisher     publisher   = new TopicPublisher(topic, sPrefix, nCount, true);

        TopicSubscriber subscriber1 = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "Foo", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);
        TopicSubscriber subscriber2 = new TopicSubscriber(new MultiplexedSubscriber<>(topic, "Bar", 2), sPrefix, nCount, 3, TimeUnit.MINUTES, true);

        publisher.run();

        assertThat(publisher.getPublished(), is(nCount));
        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        Future<?> futureSubscriber1 = m_executorService.submit(subscriber1);
        Future<?> futureSubscriber2 = m_executorService.submit(subscriber2);

        futureSubscriber1.get();
        futureSubscriber2.get();

        assertThat(subscriber1.getConsumedCount(), is(nCount));
        assertThat(subscriber2.getConsumedCount(), is(nCount));
        }

    @Test
    public void shouldRetainElements() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic(m_sSerializer + "-rewindable");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).isRetainConsumed(), is(true));

        Publisher<String>  publisher = topic.createPublisher();
        String             sPrefix   = "Element-";
        int                nCount    = 123;

        for (int i=0; i<nCount; i++)
            {
            publisher.send(sPrefix + i).get();
            }

        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        Subscriber<String> subscriber1 = topic.createSubscriber();

        for (int i=0; i<nCount; i++)
            {
            assertThat(subscriber1.receive().get().getValue(), is(sPrefix + i));
            }

        Subscriber<String> subscriber2 = topic.createSubscriber();

        for (int i=0; i<nCount; i++)
            {
            assertThat(subscriber2.receive().get().getValue(), is(sPrefix + i));
            }
        }

    @Test
    public void shouldRetainElementsForSubscriberGroup() throws Exception
        {
        NamedTopic<String> topic     = ensureTopic(m_sSerializer + "-rewindable");

        Assume.assumeThat("Test only applies when paged-topic-scheme has retain-consumed configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).isRetainConsumed(), is(true));

        Publisher<String>  publisher = topic.createPublisher();
        String             sPrefix   = "Element-";
        int                nCount    = 123;

        for (int i=0; i<nCount; i++)
            {
            publisher.send(sPrefix + i).get();
            }

        TopicAssertions.assertPublishedOrder(topic, nCount, "Element-");

        Subscriber<String> subscriber1 = topic.createSubscriber(of("one"));
        int                i           = 0;
        int                nFirstHalf  = nCount / 2;

        for (; i<nFirstHalf; i++)
            {
            assertThat(subscriber1.receive().get().getValue(), is(sPrefix + i));
            }

        Subscriber<String> subscriber2 = topic.createSubscriber(of("one"));

        for (; i<nCount; i++)
            {
            assertThat(subscriber2.receive().get().getValue(), is(sPrefix + i));
            }
        }

    @Test
    public void shouldWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Publisher<String> publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));

        Future<Subscriber.Element<String>> future = subscriber.receive();
        assertThat(future.isDone(), is(false));

        publisher.send("blah");

        assertThat(future.get().getValue(), is("blah"));

        subscriber.close();
        publisher.close();
        }

    @Test
    public void shouldShareWaitNotificationOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Publisher<String> publisher = topic.createPublisher();
        Subscriber<String> subscriber1 = topic.createSubscriber(of("subscriber"));
        Subscriber<String> subscriber2 = topic.createSubscriber(of("subscriber"));

        Future<Subscriber.Element<String>> future1 = subscriber1.receive();
        Future<Subscriber.Element<String>> future2 = subscriber2.receive();

        publisher.send("blah");
        publisher.send("blah");

        assertThat(future1.get().getValue(), is("blah"));
        assertThat(future2.get().getValue(), is("blah"));

        subscriber1.close();
        subscriber2.close();
        publisher.close();
        }

    @Test
    public void shouldDrainAndWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Publisher<String> publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));

        publisher.send("blah");
        assertThat(subscriber.receive().get().getValue(), is("blah"));

        Future<Subscriber.Element<String>> future = subscriber.receive();
        assertThat(future.isDone(), is(false));

        publisher.send("blah blah");

        assertThat(future.get().getValue(), is("blah blah"));

        subscriber.close();
        publisher.close();
        }

    @Test
    public void shouldListSubscriberGroups()
            throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        assertThat(topic.getSubscriberGroups().isEmpty(), is(true));

        Subscriber<String> subAnon = topic.createSubscriber();

        assertThat(topic.getSubscriberGroups().isEmpty(), is(true));

        Subscriber<String> subFoo = topic.createSubscriber(Subscriber.Name.of("foo"));
        Subscriber<String> subBar = topic.createSubscriber(Subscriber.Name.of("bar"));

        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"foo", "bar"}).getSet()));

        topic.destroySubscriberGroup("foo");

        assertThat(topic.getSubscriberGroups(), is(new ImmutableArrayList(new String[]{"bar"}).getSet()));
        }

    @Test
    public void shouldCancelWaitOnClose() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));

        Future<Subscriber.Element<String>> future = subscriber.receive();
        Thread.sleep(100); // allow subscriber to enter wait state
        subscriber.close();
        assertThat(future.isCancelled(), is(true));
        }

    @Test
    public void shouldNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"), Subscriber.CompleteOnEmpty.enabled());

        Future<Subscriber.Element<String>> future = subscriber.receive();

        assertThat(future.get(), is(nullValue()));

        subscriber.close();
        }

    @Test
    public void shouldDrainAndNotWaitOnEmptyTopic() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();

        Publisher<String> publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"), Subscriber.CompleteOnEmpty.enabled());

        publisher.send("blah").join();
        assertThat(subscriber.receive().get().getValue(), is("blah"));

        Future<Subscriber.Element<String>> future = subscriber.receive();

        assertThat(future.get(), is(nullValue()));

        subscriber.close();
        }

    @Test
    public void shouldThrottleSubscribers() throws Exception
        {
        NamedTopic<String> topic = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        AtomicLong         cPending = new AtomicLong();
        AtomicLong         cReceive = new AtomicLong();
        long               nHigh    = CacheFactory.getCluster().getDependencies().getPublisherCloggedCount();

        Thread thread = new Thread()
            {
            @Override
            public void run()
                {
                while (cReceive.get() < nHigh * 2) // over aggressively schedule requests until we've received everything, i.e. depend on flow-control
                    {
                    cPending.incrementAndGet();
                    subscriber.receive().handle((Subscriber.Element<String> v, Throwable t) ->
                        {
                        cPending.decrementAndGet();
                        if (t == null)
                            {
                            cReceive.incrementAndGet();
                            }
                        return v;
                        });
                    }
                }
            };

        thread.start();

        while (cPending.get() < nHigh) // wait for subscriber to build up a backlog
            {
            Thread.sleep(1);
            }

        for (int i = 0; i < nHigh * 2; ++i)
            {
            publisher.send("Element-" + i).get(); // .get() makes publisher slower then subscriber
            assertThat(cPending.get(), lessThanOrEqualTo(nHigh + 1));
            }

        while (cReceive.get() < nHigh * 2)
            {
            Thread.sleep(1);
            }

        thread.interrupt(); // the thread may be blocked on flow control
        thread.join();

        assertThat(cReceive.get(), is(nHigh * 2));
        assertThat(cPending.get(), lessThanOrEqualTo(nHigh + 1));

        subscriber.close();
        }

    @Test
    public void shouldNotThrottleNonBlockingSubscriber() throws Exception
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh = (topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getMaxBatchSizeBytes() * 3) / cbValue;

        try (NonBlocking nb = new NonBlocking())
            {
            for (int i = 0; i < nHigh * 100; ++i)
                {
                subscriber.receive();
                }
            }

        subscriber.close();
        }

    @Test
    public void shouldThrottlePublisher() throws Exception
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        AtomicLong         cReq = new AtomicLong();
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh = (topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getMaxBatchSizeBytes() * 3) / cbValue;


        Thread thread = new Thread()
            {
            @Override
            public void run()
                {
                for (int i = 0; i < nHigh * 100; ++i)
                    {
                    cReq.incrementAndGet();
                    publisher.send("Element-" + i).handle((Void v, Throwable t) ->
                        {
                        cReq.decrementAndGet();
                        return v;
                        });
                    }
                }
            };

        thread.start();

        for (int i = 0; i < nHigh * 100; ++i)
            {
            subscriber.receive().get(); // .get() makes subscriber slower then publisher
            assertThat(cReq.get(), lessThanOrEqualTo(nHigh + cbValue));
            }
        }

    @Test
    public void shouldThrottlePublisherWhenFull() throws Exception
        {
        final long SERVER_CAPACITY = 10L * 1024L;

        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getServerCapacity(), is(SERVER_CAPACITY));

        Publisher<String>  publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        AtomicLong         cReq = new AtomicLong();
        String             sRand = Base.getRandomString(64, 64, true);
        int                cbValue = ExternalizableHelper.toBinary(sRand, topic.getService().getSerializer()).length();
        long               nHigh = (topic.getService().getInfo().getServiceMembers().size() + 1) * SERVER_CAPACITY /*in config*/ / cbValue;
        int                cBacklogs = 0;
        FlowControl        fc = publisher.getFlowControl();

        AtomicInteger cValues = new AtomicInteger();
        try (NonBlocking nb = new NonBlocking())
            {
            for (int i = 0; i < nHigh * 2; ++i)
                {
                cValues.incrementAndGet();
                cReq.incrementAndGet();

                long ldtStart = System.currentTimeMillis();
                CompletableFuture<Void> future = publisher.send(sRand).handle((Void v, Throwable t) ->
                    {
                    cReq.decrementAndGet();
                    return v;
                    });

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
                        futureDrain = subscriber.receive().whenComplete((r, e) -> {
                        cValues.decrementAndGet();
                        });
                        }
                    if (futureDrain != null)
                        {
                        subscriber.getFlowControl().flush();
                        futureDrain.get();
                        }
                    future.get();
                    long cMillis = System.currentTimeMillis() - ldtStart;
                    if (cMillis > PagedTopicPartition.PUBLISHER_NOTIFICATION_EXPIRY_MILLIS)
                        {
                        // apparently we've relied on notification expiry which isn't meant to be used
                        // here, that is basically just to cover partition movement giving us more space.
                        throw new TimeoutException("timeout after " + cMillis);
                        }
                    }
                }
            }

        assertThat(cBacklogs, greaterThan(0)); // ensure we hit a backlog at least once
        }

    @Test
    public void shouldThrowWhenFull() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getServerCapacity(), is(SERVER_CAPACITY));

        Publisher<String>  publisher = topic.createPublisher(Publisher.FailOnFull.enabled());
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh = SERVER_CAPACITY / cbValue; // from config

        try
            {
            for (int i = 0; i < nHigh * 2; ++i)
                {
                publisher.send("Element-" + i).join();
                }
            assertFalse(true); // we're not supposed to finish
            }
        catch (CompletionException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertTrue(e.getCause().getMessage().contains("topic is at capacity"));
            }
        }

    @Test
    public void shouldCloseWhenFull() throws Exception
        {
        NamedTopic<String> topic = ensureTopic(m_sSerializer + "-limited");

        final long SERVER_CAPACITY = 10L * 1024L;

        Assume.assumeThat("Test only applies when paged-topic-scheme has per server capacity of " + SERVER_CAPACITY + " configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getServerCapacity(), is(SERVER_CAPACITY));

        Publisher<String>  publisher = topic.createPublisher(Publisher.FailOnFull.enabled());
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh = SERVER_CAPACITY / cbValue; // from config

        try
            {
            for (int i = 0; i < nHigh * 2; ++i)
                {
                publisher.send("Element-" + i).join();
                }
            assertFalse(true); // we're not supposed to finish
            }
        catch (CompletionException e)
            {
            // expected
            assertTrue(e.getCause() instanceof IllegalStateException);
            assertTrue(e.getCause().getMessage().contains("topic is at capacity"));
            }

        try (Publisher<String> publisherA = topic.createPublisher())
            {
            publisherA.send("Element-after-full").exceptionally(t ->
                {
                    System.out.println("send Element-after-full. Completed with exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
                    return null;
                });
            }
        }

    @Test
    public void shouldNotThrottleNonBlockingPublisher() throws Exception
        {
        Assume.assumeThat("Skip throttle test for default cache config",
            getCoherenceCacheConfig().compareTo(CUSTOMIZED_CACHE_CONFIG), is(0));

        NamedTopic<String> topic = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"));
        AtomicLong         cReq = new AtomicLong();
        long               cMax = 0;
        int                cbValue = ExternalizableHelper.toBinary( "Element-" + 0, topic.getService().getSerializer()).length();
        long               nHigh = (topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getMaxBatchSizeBytes() * 3) / cbValue;

        try (NonBlocking nb = new NonBlocking())
            {
            for (int i = 0; i < nHigh * 100; ++i)
                {
                cMax = Math.max(cMax, cReq.incrementAndGet());
                publisher.send("Element0" + i).thenRun(() -> cReq.decrementAndGet());
                }
            }

        assertThat(cMax, greaterThan(nHigh)); // verify that publisher got ahead of flow-control

        subscriber.close();
        }


    @Test
    public void shouldNotPollExpiredElements() throws Exception
        {
        NamedTopic<String> topic      = ensureTopic(m_sSerializer + "-expiring");

        Assume.assumeThat("Test only applies when paged-topic-scheme has non-zero expiry configured",
            topic.getService().getResourceRegistry().getResource(Configuration.class, topic.getName()).getElementExpiryMillis() != 0, is(true));

        Publisher<String>  publisher  = topic.createPublisher();
        Subscriber<String> subscriber = topic.createSubscriber(of("subscriber"), Subscriber.CompleteOnEmpty.enabled());
        String             sPrefix    = "Element-";
        int                nCount     = 20;

        for (int i=0; i<nCount; i++)
            {
            publisher.send(sPrefix + i);
            }

        Thread.sleep(3000);

        assertThat(subscriber.receive().get(), is(nullValue()));

        publisher.send("Element-Last").get();
        String sValue = subscriber.receive().get().getValue();
        assertThat(sValue, is("Element-Last"));
        }

    @Test
    public void shouldStartSecondSubscriberFromCorrectPosition()
            throws Exception
        {
        NamedTopic<String> topic       = ensureTopic();
        Publisher<String>  publisher   = topic.createPublisher();
        Subscriber<String> subscriber1 = topic.createSubscriber(of("Foo"));
        String             sPrefix     = "Element-";
        int                nCount      = 100;

        for (int i=0; i<nCount; i++)
            {
            publisher.send(sPrefix + i).get();
            }

        int i = 0;
        for ( ; i<25; i++)
            {
            assertThat(subscriber1.receive().get().getValue(), is(sPrefix + i));
            }

        Subscriber<String> subscriber2 = topic.createSubscriber(of("Foo"));

        for ( ; i<50; i++)
            {
            assertThat(subscriber2.receive().get().getValue(), is(sPrefix + i));
            }
        }

    @Test
    public void shouldConsumeFromTail()
            throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();
        String             sPrefix   = "Element-";

        Subscriber<String> subscriberPin = topic.createSubscriber(); // ensures data inserted before test subscriber is created remains in the topic

        publisher.send(sPrefix + 1).join();

        Subscriber<String> subscriber = topic.createSubscriber();

        Future<Subscriber.Element<String>> future = subscriber.receive();

        publisher.send(sPrefix + 2);

        assertThat(future.get().getValue(), is(sPrefix + 2));

        subscriber.close();
        }

    @Test
    public void shouldGroupConsumeFromTail()
            throws Exception
        {
        NamedTopic<String> topic     = ensureTopic();
        Publisher<String>  publisher = topic.createPublisher();
        String             sPrefix   = "Element-";

        publisher.send(sPrefix + 1).join();

        Subscriber<String> subscriber = topic.createSubscriber(Subscriber.Name.of("foo"));

        Future<Subscriber.Element<String>> future = subscriber.receive();

        publisher.send(sPrefix + 2);
        publisher.send(sPrefix + 3);

        assertThat(future.get().getValue(), is(sPrefix + 2));

        // ensure that a new member to the group doesn't reset the groups position in the topic, i.e. this subscriber
        // instance can see items created before it joined the group, as position is defined by the group not the
        // instance
        Subscriber<String> subscriber2 = topic.createSubscriber(Subscriber.Name.of("foo"));

        Future<Subscriber.Element<String>> future2 = subscriber2.receive();

        assertThat(future2.get().getValue(), is(sPrefix + 3));

        subscriber.close();
        subscriber2.close();
        }


    @Test
    public void shouldCloseOneSubscriberInGroupWithoutAffectingTheOtherSubscriber()
            throws Exception
        {
        NamedTopic<String> topic    = ensureTopic();
        Publisher<String> publisher = topic.createPublisher();
        Subscriber<String> subscriber1 = topic.createSubscriber(of("Foo"));
        Subscriber<String> subscriber2 = topic.createSubscriber(of("Foo"));
        String                            sPrefix  = "Element-";
        int                               nCount   = 100;

        for (int i=0; i<nCount; i++)
            {
            publisher.send(sPrefix + i).get();
            }


        int i = 0;
        for ( ; i<25; i++)
            {
            assertThat(subscriber1.receive().get().getValue(), is(sPrefix + i));
            }

        subscriber1.close();

        for ( ; i<50; i++)
            {
            assertThat(subscriber2.receive().get().getValue(), is(sPrefix + i));
            }
        }

    @Test
    public void shouldOfferSingleElementLargerThanMaxPageSizeToEmptyPage() throws Exception
        {
        m_topic = ensureTopic(m_sSerializer + "-binary-test");

        int  cbPageSize = m_topic.getService().getResourceRegistry().getResource(Configuration.class, m_topic.getName()).getPageCapacity();

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(2024));

        char[]                                  aChars   = new char[2024];
        Publisher<String> publisher = m_topic.createPublisher();
        Subscriber<String> subscriber = m_topic.createSubscriber();

        Arrays.fill(aChars, 'a');

        String element = new String(aChars);

        publisher.send(element).get();

        String sValue = subscriber.receive().get().getValue();

        assertThat(sValue, is(element));
        }

    @Test
    public void shouldOfferSingleElementLargerThanMaxPageSizeToPopulatedPage() throws Exception
        {
        m_topic = ensureTopic(m_sSerializer + "-binary-test");

        int  cbPageSize = m_topic.getService().getResourceRegistry().getResource(Configuration.class, m_topic.getName()).getPageCapacity();

        Assume.assumeThat("Test only applies if paged-topic-scheme sets page-size to a much lower value than default page-size",
            cbPageSize, lessThanOrEqualTo(2024));

        char[]                            aChars      = new char[2024];
        Publisher<String> publisher = m_topic.createPublisher();
        Subscriber<String> subscriber = m_topic.createSubscriber();

        Arrays.fill(aChars, 'a');

        String element = new String(aChars);

        publisher.send("element-1").get();
        publisher.send(element).get();

        String sValue1 = subscriber.receive().get().getValue();
        String sValue2 = subscriber.receive().get().getValue();

        assertThat(sValue1, is("element-1"));

        assertThat(sValue2, is(element));
        }

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     *
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     *
     * @throws Exception
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
     *
     * @throws Exception
     */
    @Test
    public void validateTopicMBeans() throws Exception
        {
        validateTopicMBeans(m_sSerializer + "-binary-test");
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Validate that default tangosol-coherence.xml mbean-filter removes Cache and StorageManger MBean
     * containing {@link PagedTopicCaches.Names#METACACHE_PREFIX}.
     *
     * Validate that Cache and Storage MBean for {@link PagedTopicCaches.Names#CONTENT} exist.
     *
     * @throws Exception
     */
    private void validateTopicMBeans(String sTopicName) throws Exception
        {
        Session            session    = getSession();
        NamedTopic<String> topic      = session.getTopic(sTopicName, ValueTypeAssertion.withType(String.class));

        final int          nMsgSizeBytes = 1024;
        final int          cMsg          = 500;
        Subscriber<String> subscriber = topic.createSubscriber();
        Publisher<String>  publisher  = topic.createPublisher();

        populate(publisher, nMsgSizeBytes, cMsg);

        MBeanServer        server     = MBeanHelper.findMBeanServer();

        validateTopicMBean(server, "Cache", sTopicName, nMsgSizeBytes, cMsg);
        validateTopicMBean(server, "StorageManager", sTopicName, nMsgSizeBytes, cMsg);

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
     *
     * @throws MalformedObjectNameException
     */
    private void validateTopicMBean(MBeanServer server, String sTypeMBean, String sName, int nMessageSize, int cMessages)
            throws MalformedObjectNameException, AttributeNotFoundException, MBeanException, ReflectionException, InstanceNotFoundException, InterruptedException
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
                    assertTrue(sNameMBean + " MBean attribute constraint check: MemoryUnits", (boolean) server.getAttribute(inst.getObjectName(), "MemoryUnits"));

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

            assertTrue("Missing " + sTypeMBean + " MBean for " + elementsCacheName, fElementsDefined);
        }

    protected void populate(Publisher<String> publisher, int nCount)
        {
        for (int i=0; i<nCount; i++)
            {
            try
                {
                publisher.send("Element-" + i).get();
                }
            catch (InterruptedException | ExecutionException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    protected void populate(Publisher<String> publisher, int nMsgSize, int nCount)
        {
        byte[] bytes = new byte[nMsgSize];
        Arrays.fill(bytes, (byte)'A');
        for (int i=0; i<nCount; i++)
            {
            try
                {
                publisher.send(new String(bytes)).get();
                }
            catch (InterruptedException | ExecutionException e)
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
            m_topic = getSession().getTopic(m_sSerializer, ValueTypeAssertion.withType(String.class));
            }

        return m_topic;
        }

    protected synchronized NamedTopic<String> ensureTopic(String sTopicName)
        {
        if (m_topic != null)
            {
            m_topic.destroy();
            }
        return m_topic = getSession().getTopic(sTopicName, ValueTypeAssertion.withType(String.class));
        }

    protected synchronized NamedTopic<Customer> ensureCustomerTopic(String sTopicName)
        {
        if (m_topicCustomer != null)
            {
            m_topicCustomer.destroy();
            }
        return m_topicCustomer = getSession().getTopic(sTopicName, ValueTypeAssertion.withType(Customer.class));
        }

    protected abstract Session getSession();

    protected abstract void runInCluster(RemoteRunnable runnable);

    protected abstract int getStorageMemberCount();

    protected abstract String getCoherenceCacheConfig();

    // ----- constants ------------------------------------------------------

    static public final String DEFAULT_COHERENCE_CACHE_CONFIG = "coherence-cache-config.xml";

    static public final String CUSTOMIZED_CACHE_CONFIG        = "topic-cache-config.xml";

    // ----- data members ---------------------------------------------------

    protected String               m_sSerializer;
    protected NamedTopic<String>   m_topic;
    protected NamedTopic<Customer> m_topicCustomer;
    protected ExecutorService      m_executorService = Executors.newFixedThreadPool(4);
    }
