/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicBackingMapManager;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;

import com.tangosol.net.topic.BinaryElementCalculator;
import com.tangosol.net.topic.FixedElementCalculator;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("unchecked")
public class TopicsCalculatorTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.cacheconfig", "topics-calculator-config.xml");
        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();
        s_session = coherence.getSession();
        }

    @AfterClass
    public static void cleanup()
        {
        Coherence.getInstance().close();
        }

    @Test
    public void shouldUseBinaryCalculatorByDefault() throws Exception
        {
        NamedTopic<String>           topic        = s_session.getTopic("default-test");
        PagedTopicDependencies       dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator   = dependencies.getElementCalculator();

        assertThat(calculator, is(instanceOf(BinaryElementCalculator.class)));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            int    cbSize = (dependencies.getPageCapacity() / 2) + 1; // just over half the page size
            String sValue = Base.getRandomString(cbSize, cbSize, true);

            for (int i = 0; i < 10; i ++)
                {
                publisher.publish(sValue).get(1, TimeUnit.MINUTES);
                }

            List<Subscriber.Element<String>> list = new ArrayList<>();
            for (int i = 0; i < 10; i ++)
                {
                list.add(subscriber.receive().get(1, TimeUnit.MINUTES));
                }

            Subscriber.Element<String> elementFirst = list.get(0);
            assertThat(elementFirst, is(notNullValue()));

            PagedPosition position = (PagedPosition) elementFirst.getPosition();
            long          lPage    = position.getPage();
            assertThat(position.getOffset(), is(0));

            // element two should be the same page
            Subscriber.Element<String> element2 = list.get(1);
            position = (PagedPosition) element2.getPosition();
            assertThat(position.getPage(), is(lPage));
            assertThat(position.getOffset(), is(1));

            // element three should be the next page
            Subscriber.Element<String> element3 = list.get(2);
            position = (PagedPosition) element3.getPosition();
            assertThat(position.getPage(), is(lPage + 1));
            assertThat(position.getOffset(), is(0));
            }
        }

    @Test
    public void shouldUseBinaryCalculator() throws Exception
        {
        NamedTopic<String>           topic        = s_session.getTopic("binary-test");
        PagedTopicDependencies       dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator   = dependencies.getElementCalculator();

        assertThat(calculator, is(instanceOf(BinaryElementCalculator.class)));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            int    cbSize = (dependencies.getPageCapacity() / 2) + 1; // just over half the page size
            String sValue = Base.getRandomString(cbSize, cbSize, true);

            for (int i = 0; i < 10; i ++)
                {
                publisher.publish(sValue).get(1, TimeUnit.MINUTES);
                }

            List<Subscriber.Element<String>> list = new ArrayList<>();
            for (int i = 0; i < 10; i ++)
                {
                list.add(subscriber.receive().get(1, TimeUnit.MINUTES));
                }

            Subscriber.Element<String> elementFirst = list.get(0);
            assertThat(elementFirst, is(notNullValue()));

            PagedPosition position = (PagedPosition) elementFirst.getPosition();
            long          lPage    = position.getPage();
            assertThat(position.getOffset(), is(0));

            // element two should be the same page
            Subscriber.Element<String> element2 = list.get(1);
            position = (PagedPosition) element2.getPosition();
            assertThat(position.getPage(), is(lPage));
            assertThat(position.getOffset(), is(1));

            // element three should be the next page
            Subscriber.Element<String> element3 = list.get(2);
            position = (PagedPosition) element3.getPosition();
            assertThat(position.getPage(), is(lPage + 1));
            assertThat(position.getOffset(), is(0));
            }
        }

    @Test
    public void shouldUseFixedCalculatorDueToNonMemoryUnitPageSize() throws Exception
        {
        NamedTopic<String>           topic        = s_session.getTopic("units-test");
        PagedTopicDependencies       dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator   = dependencies.getElementCalculator();
        int                          nPageSize    = dependencies.getPageCapacity();

        assertThat(nPageSize, is(50));
        assertThat(calculator, is(instanceOf(FixedElementCalculator.class)));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            String sValue = Base.getRandomString(10, 10, true);

            // fill three pages
            for (int i = 0; i < nPageSize * 3; i ++)
                {
                publisher.publish(sValue).get(1, TimeUnit.MINUTES);
                }

            // receive a page size of elements
            List<Subscriber.Element<String>> list = new ArrayList<>();
            for (int i = 0; i < nPageSize; i ++)
                {
                list.add(subscriber.receive().get(1, TimeUnit.MINUTES));
                }

            // get the distinct page numbers from the received elements
            Long[] alPage = list.stream().map(e -> ((PagedPosition) e.getPosition()).getPage()).distinct().toArray(Long[]::new);
            // the elements should all have been from one page
            assertThat(alPage.length, is(1));

            // receive the next element, it should be from the next page at offset zero
            Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            assertThat(((PagedPosition) element.getPosition()).getPage(), is(alPage[0] + 1));
            assertThat(((PagedPosition) element.getPosition()).getOffset(), is(0));
            }
        }

    @Test
    public void shouldUseSpecifiedFixedCalculatorAndPageSize() throws Exception
        {
        NamedTopic<String>           topic        = s_session.getTopic("fixed-test");
        PagedTopicDependencies       dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator   = dependencies.getElementCalculator();
        int                          nPageSize    = dependencies.getPageCapacity();

        assertThat(nPageSize, is(100));
        assertThat(calculator, is(instanceOf(FixedElementCalculator.class)));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            String sValue = Base.getRandomString(10, 10, true);

            // fill three pages
            for (int i = 0; i < nPageSize * 3; i ++)
                {
                publisher.publish(sValue).get(1, TimeUnit.MINUTES);
                }

            // receive a page size of elements
            List<Subscriber.Element<String>> list = new ArrayList<>();
            for (int i = 0; i < nPageSize; i ++)
                {
                list.add(subscriber.receive().get(1, TimeUnit.MINUTES));
                }

            // get the distinct page numbers from the received elements
            Long[] alPage = list.stream().map(e -> ((PagedPosition) e.getPosition()).getPage()).distinct().toArray(Long[]::new);
            // the elements should all have been from one page
            assertThat(alPage.length, is(1));

            // receive the next element, it should be from the next page at offset zero
            Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            assertThat(((PagedPosition) element.getPosition()).getPage(), is(alPage[0] + 1));
            assertThat(((PagedPosition) element.getPosition()).getOffset(), is(0));
            }
        }

    @Test
    public void shouldUseCustomCalculator() throws Exception
        {
        NamedTopic<String>           topic         = s_session.getTopic("custom-test");
        PagedTopicDependencies       dependencies = getDependencies(topic);
        NamedTopic.ElementCalculator calculator    = dependencies.getElementCalculator();

        assertThat(calculator, is(instanceOf(CustomCalculator.class)));

        try (Publisher<String>  publisher  = topic.createPublisher();
             Subscriber<String> subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            String sValue           = Base.getRandomString(10, 10, true);
            int    cElementsPerPage = CUSTOM_PAGE_SIZE / CUSTOM_ELEMENT_SIZE;

            // fill three pages
            for (int i = 0; i < cElementsPerPage * 3; i ++)
                {
                publisher.publish(sValue).get(1, TimeUnit.MINUTES);
                }

            // receive a page size of elements
            List<Subscriber.Element<String>> list = new ArrayList<>();
            for (int i = 0; i < cElementsPerPage; i ++)
                {
                list.add(subscriber.receive().get(1, TimeUnit.MINUTES));
                }

            // get the distinct page numbers from the received elements
            Long[] alPage = list.stream().map(e -> ((PagedPosition) e.getPosition()).getPage()).distinct().toArray(Long[]::new);
            // the elements should all have been from one page
            assertThat(alPage.length, is(1));

            // receive the next element, it should be from the next page at offset zero
            Subscriber.Element<String> element = subscriber.receive().get(1, TimeUnit.MINUTES);
            assertThat(((PagedPosition) element.getPosition()).getPage(), is(alPage[0] + 1));
            assertThat(((PagedPosition) element.getPosition()).getOffset(), is(0));
            }
        }

    // ----- helper methods -------------------------------------------------

    private PagedTopicDependencies getDependencies(NamedTopic<?> topic)
        {
        PagedTopicService service = (PagedTopicService) topic.getService();
        PagedTopicBackingMapManager manager = service.getTopicBackingMapManager();
        return manager.getTopicDependencies(topic.getName());
        }

    // ----- inner class: CustomCalculator ----------------------------------

    /**
     * A custom element size calculator
     */
    public static class CustomCalculator
            implements NamedTopic.ElementCalculator
        {
        @Override
        public int calculateUnits(Binary binElement)
            {
            return CUSTOM_ELEMENT_SIZE;
            }
        }

    // ----- constants ------------------------------------------------------

    public static final int CUSTOM_ELEMENT_SIZE = 2;

    public static final int CUSTOM_PAGE_SIZE = 10;

    // ----- data members ---------------------------------------------------

    private static Session s_session;
    }
