/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Randoms;
import com.tangosol.internal.net.queue.QueuePageIterator;
import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ObservableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.lessThanOrEqualTo;

@SuppressWarnings({"rawtypes", "unchecked", "MismatchedQueryAndUpdateOfCollection"})
public abstract class AbstractQueueTests<QueueType extends NamedQueue>
        extends AbstractCollectionTests<QueueType, QueueType>
        implements QueueTests<QueueType>
    {
    @Override
    @SuppressWarnings("unchecked")
    public QueueType getNamedCollection(Session session, String sName)
        {
        return (QueueType) session.getQueue(sName);
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getCollection(Session session, String sName)
        {
        return (QueueType) session.getQueue(sName);
        }


    // ----- test add() method ----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddToQueue(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        String    sValue = "message-1";
        assertThat(queue.add(sValue), is(true));

        NamedCache<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = queue.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddToQueueInOrder(String sSerializer)
        {
        QueueType queue    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(queue.add(sValue), is(true));
            }

        NamedCache<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(cMessage));

        TreeSet<QueueKey> setKey = (TreeSet<QueueKey>) new TreeSet<>(cache.keySet());
        assertThat(setKey.size(), is(cMessage));

        int i = 0;
        for (QueueKey key : setKey)
            {
            String sExpected = sPrefix + i;
            assertThat(cache.get(key), is(sExpected));
            i++;
            }
        }

    // ----- test offer() method --------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferToQueue(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        String    sValue = "message-1";
        assertThat(queue.offer(sValue), is(true));

        NamedCache<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = queue.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferToQueueInOrder(String sSerializer)
        {
        QueueType queue    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(queue.offer(sValue), is(true));
            }

        NamedCache<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(cMessage));

        TreeSet<QueueKey> setKey = (TreeSet<QueueKey>) new TreeSet<>(cache.keySet());
        assertThat(setKey.size(), is(cMessage));

        int i = 0;
        for (QueueKey key : setKey)
            {
            String sExpected = sPrefix + i;
            assertThat(cache.get(key), is(sExpected));
            i++;
            }
        }

    // ----- test remove() method ------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFromEmptyQueue(String sSerializer)
        {
        QueueType queue = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, queue::remove);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFromQueue(String sSerializer)
        {
        QueueType  queue  = getNewCollection(sSerializer);
        NamedCache cache  = getCollectionCache(queue.getName());
        QueueKey   key    = new QueueKey(queue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = queue.remove();
        assertThat(oValue, is(sValue));
        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        Assertions.assertThrows(NoSuchElementException.class, queue::remove);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFromQueueInOrder(String sSerializer)
        {
        QueueType  queue    = getNewCollection(sSerializer);
        NamedCache cache    = getCollectionCache(queue.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        int        nHash    = queue.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = queue.remove();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, queue::remove);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFromQueueInOrderWithGaps(String sSerializer)
        {
        QueueType  queue    = getNewCollection(sSerializer);
        NamedCache cache    = getCollectionCache(queue.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = queue.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = queue.remove();
            assertThat(oValue, is(sPrefix + i));
            }
        
        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, queue::remove);
        }

    // ----- test poll() method ---------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFromEmptyQueue(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        Object    oValue = queue.poll();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFromQueue(String sSerializer)
        {
        QueueType  queue  = getNewCollection(sSerializer);
        NamedCache cache  = getCollectionCache(queue.getName());
        QueueKey   key    = new QueueKey(queue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = queue.poll();
        assertThat(oValue, is(sValue));
        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        oValue = queue.poll();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFromQueueInOrder(String sSerializer)
        {
        QueueType  queue    = getNewCollection(sSerializer);
        NamedCache cache    = getCollectionCache(queue.getName());
        int        nHash    = queue.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = queue.poll();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(queue.poll(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFromQueueInOrderWithGaps(String sSerializer)
        {
        QueueType  queue    = getNewCollection(sSerializer);
        NamedCache cache    = getCollectionCache(queue.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = queue.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = queue.poll();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(queue.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(queue.poll(), is(nullValue()));
        }

    // ----- test element() method ------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetElementFromEmptyQueue(String sSerializer)
        {
        QueueType queue = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, queue::element);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetElementFromQueue(String sSerializer)
        {
        QueueType  queue  = getNewCollection(sSerializer);
        NamedCache cache  = getCollectionCache(queue.getName());
        QueueKey   key    = new QueueKey(queue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = queue.element();
        assertThat(oValue, is(sValue));
        assertThat(queue.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));

        oValue = queue.element();
        assertThat(oValue, is(sValue));
        }

    // ----- test peek() method ---------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekAtEmptyQueue(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        Object    oValue = queue.peek();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekAtQueue(String sSerializer)
        {
        QueueType  queue  = getNewCollection(sSerializer);
        NamedCache cache  = getCollectionCache(queue.getName());
        QueueKey   key    = new QueueKey(queue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = queue.peek();
        assertThat(oValue, is(sValue));
        assertThat(queue.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));

        oValue = queue.peek();
        assertThat(oValue, is(sValue));
        }

    // ----- size limited tests ---------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferAndPollSizeLimitedQueue(String sSerializer)
        {
        QueueType                    queue  = getNewCollection(sSerializer);
        NamedCache<QueueKey, String> cache  = getCollectionCache(queue.getName());
        QueueKey                     key    = QueueKey.head(queue.getName());
        long                         cMax   = 100000;
        int                          cBytes = 100;
        long                         cEntry = cMax / cBytes;

        assertThat(cache.invoke(key, entry -> AbstractQueueTests.setMaxQueueSize(entry, cMax)), is(true));

        String sPad     = Randoms.getRandomString(cBytes, cBytes, true);
        int    cElement = 0;
        for (int i = 0; i < (cEntry * 2); i++)
            {
            String sElement = sPad + "-" + i;
            if (!queue.offer(sElement))
                {
                break;
                }
            cElement++;
            }

        // should be <= max queue size
        Long cUnits = cache.invoke(key, AbstractQueueTests::getCacheUnits);
        assertThat(cUnits, is(notNullValue()));
        assertThat(cUnits, is(lessThanOrEqualTo(cMax)));

        // the queue should be full so an offer should fail
        assertThat(queue.offer(sPad + "-X"), is(false));
        // poll one then the next offer should succeed
        assertThat(queue.poll(), is(notNullValue()));
        // the queue should NOT be full so an offer should succeed
        assertThat(queue.offer(sPad + "-X"), is(true));
        }

    public static Boolean setMaxQueueSize(InvocableMap.Entry<?, ?> entry, long cMaxSize)
        {
        MapIndex index = entry.asBinaryEntry().getIndexMap().get(QueueKeyExtractor.INSTANCE);
        assertThat(index, is(instanceOf(QueueKeyExtractor.QueueIndex.class)));
        QueueKeyExtractor.QueueIndex queueIndex = (QueueKeyExtractor.QueueIndex) index;
        queueIndex.setMaxQueueSize(cMaxSize);
        return true;
        }

    @SuppressWarnings({"deprecation", "PatternVariableCanBeUsed"})
    public static Long getCacheUnits(InvocableMap.Entry<?, ?> entry)
        {
        ObservableMap<?, ?> backingMap = entry.asBinaryEntry().getBackingMapContext().getBackingMap();
        if (backingMap instanceof ConfigurableCacheMap)
            {
            ConfigurableCacheMap map = (ConfigurableCacheMap) backingMap;
            return (long) map.getUnits() * (long) map.getUnitFactor();
            }
        return -1L;
        }

    // ----- other tests ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferAndPoll(String sSerializer)
        {
        QueueType queue = getNewCollection(sSerializer);
        for (int i = 0; i < 100; i++)
            {
            String sElement = "message-" + i;
            assertThat(queue.offer(sElement), is(true));
            assertThat(queue.poll(), is(sElement));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferPeekAndPoll(String sSerializer)
        {
        QueueType queue = getNewCollection(sSerializer);
        for (int i = 0; i < 100; i++)
            {
            String sElement = "message-" + i;
            assertThat(queue.offer(sElement), is(true));
            assertThat(queue.peek(), is(sElement));
            assertThat(queue.poll(), is(sElement));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferAndPollRandomly(String sSerializer)
        {
        QueueType queue = getNewCollection(sSerializer);

        List   listOffer  = new ArrayList<>();
        List   listPolled = new ArrayList<>();
        String sPrefix    = "message-";
        int    cOffer     = 0;

        for (int i = 0; i < 10; i++)
            {
            int c = m_random.nextInt(10) + 1;
            for (int j = 0; j < c; j++)
                {
                String sValue = sPrefix + cOffer;
                assertThat(queue.offer(sValue), is(true));
                listOffer.add(sValue);
                cOffer++;
                }

            // poll up to the queue size, so we should not poll a null
            c = m_random.nextInt(queue.size());
            for (int j = 0; j < c; j++)
                {
                listPolled.add(queue.poll());
                }
            }

        // poll whatever is left
        Object oValue = queue.poll();
        while (oValue != null)
            {
            listPolled.add(oValue);
            oValue = queue.poll();
            }

        assertThat(listPolled, is(listOffer));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferAndPollFromMultipleThreads(String sSerializer) throws Exception
        {
        int            cMessage = 100000;
        CountDownLatch latch    = new CountDownLatch(1);
        QueueType      queue    = getNewCollection(sSerializer);
        OfferRunnable  offer    = new OfferRunnable(cMessage, latch, queue, NamedQueue::offer);
        PollRunnable   poll     = new PollRunnable(cMessage, latch, queue, NamedQueue::poll);

        CompletableFuture<Void> futureOffer = CompletableFuture.runAsync(offer);
        CompletableFuture<Void> futurePoll  = CompletableFuture.runAsync(poll);

        latch.countDown();
        futureOffer.get(5, TimeUnit.MINUTES);
        futurePoll.get(5, TimeUnit.MINUTES);

        List<Object> listOffered = offer.getOffered();
        List<Object> listPolled  = poll.getPolled();

        assertThat(listPolled, is(listOffered));
        }

    // ----- test iterator() method -----------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldIterateEmptyQueue(String sSerializer)
        {
        QueueType queue    = getNewCollection(sSerializer);
        Iterator  iterator = queue.iterator();
        assertThat(iterator.hasNext(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldIterateQueueInOrder(String sSerializer)
        {
        QueueType    queue    = getNewCollection(sSerializer);
        NamedCache   cache    = getCollectionCache(queue.getName());
        int          nHash    = queue.getQueueNameHash();
        String       sPrefix  = "message-";
        int          cMessage = (QueuePageIterator.DEFAULT_PAGE_SIZE * 2) + 5;
        List<String> expected = new ArrayList<>();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            String   sValue = sPrefix + i;
            cache.put(key, sValue);
            expected.add(sValue);
            }

        Iterator<String>  iterator = queue.iterator();
        assertThat(iterator.hasNext(), is(true));

        List<String> actual = new ArrayList<>();
        while (iterator.hasNext())
            {
            actual.add(iterator.next());
            }

        assertThat(actual, is(expected));
        assertThat(queue.size(), is(cMessage));
        assertThat(cache.size(), is(cMessage));
        }

    // ----- test iterator() method -----------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetArrayFromEmptyQueue(String sSerializer)
        {
        NamedQueue<String> queue = getNewCollection(sSerializer);
        assertThat(queue.toArray(new String[0]).length, is(0));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetArrayFromQueueInOrder(String sSerializer)
        {
        NamedQueue<String> queue    = getNewCollection(sSerializer);
        NamedCache         cache    = getCollectionCache(queue.getName());
        int                nHash    = queue.getQueueNameHash();
        String             sPrefix  = "message-";
        int                cMessage = (QueuePageIterator.DEFAULT_PAGE_SIZE * 2) + 5;
        String[]           expected = new String[cMessage];

        for (int i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            String   sValue = sPrefix + i;
            cache.put(key, sValue);
            expected[i] = sValue;
            }

        String[] array = queue.toArray(new String[0]);
        assertThat(array, is(expected));
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetObjectArrayFromQueueInOrder(String sSerializer)
        {
        NamedQueue<String> queue    = getNewCollection(sSerializer);
        NamedCache         cache    = getCollectionCache(queue.getName());
        int                nHash    = queue.getQueueNameHash();
        String             sPrefix  = "message-";
        int                cMessage = (QueuePageIterator.DEFAULT_PAGE_SIZE * 2) + 5;
        Object[]           expected = new String[cMessage];

        for (int i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            String   sValue = sPrefix + i;
            cache.put(key, sValue);
            expected[i] = sValue;
            }

        Object[] array = queue.toArray();
        assertThat(array, is(expected));
        }

    // ----- inner class: OfferRunnable -------------------------------------

    /**
     * A {@link Runnable} that offers to a queue.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected class OfferRunnable
            implements Runnable
        {
        /**
         * Create an {@link OfferRunnable}.
         *
         * @param cElement       the number of elements to offer
         * @param latch          a {@link CountDownLatch} to use to trigger offering
         * @param queue          the queue to offer to
         * @param offerFunction  the function that offers the specified value to the specified queue
         */
        public OfferRunnable(int cElement,
                             CountDownLatch latch,
                             QueueType queue,
                             BiFunction<QueueType, Object, Boolean> offerFunction)
            {
            m_cElement      = cElement;
            m_queue         = queue;
            m_offerFunction = offerFunction;
            m_latch         = latch;
            }

        @Override
        public void run()
            {
            // wait for the latch to trigger start
            try
                {
                m_latch.await(1, TimeUnit.MINUTES);
                }
            catch (InterruptedException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }

            for (int i = 0; i < m_cElement; i++)
                {
                String sValue = "message-" + i;
                Boolean fResult = m_offerFunction.apply(m_queue, sValue);
                assertThat(fResult, is(true));
                m_listOffered.add(sValue);
                }
            }

        public List<Object> getOffered()
            {
            return m_listOffered;
            }

        // ----- data members -----------------------------------------------

        /**
         * The number of elements to offer.
         */
        private final int m_cElement;

        private final QueueType m_queue;

        private final CountDownLatch m_latch;

        private final BiFunction<QueueType, Object, Boolean> m_offerFunction;

        private final List<Object> m_listOffered = new ArrayList<>();
        }

    // ----- inner class: PollRunnable --------------------------------------

    /**
     * A {@link Runnable} that polls from a queue.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected class PollRunnable
            implements Runnable
        {
        /**
         * Create an {@link PollRunnable}.
         *
         * @param cElement      the number of elements to poll
         * @param latch          a {@link CountDownLatch} to use to trigger polling
         * @param queue         the queue to poll to
         * @param pollFunction  the function that polls the specified value to the specified queue
         */
        public PollRunnable(int cElement,
                            CountDownLatch latch,
                            QueueType queue,
                            Function<QueueType, Object> pollFunction)
            {
            m_cElement     = cElement;
            m_latch        = latch;
            m_queue        = queue;
            m_pollFunction = pollFunction;
            }

        @Override
        public void run()
            {
            // wait for the latch to trigger start
            try
                {
                m_latch.await(1, TimeUnit.MINUTES);
                }
            catch (InterruptedException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }

            while (m_listPolled.size() < m_cElement)
                {
                Object oValue = m_pollFunction.apply(m_queue);
                if (oValue != null)
                    {
                    m_listPolled.add(oValue);
                    }
                }
            }

        public List<Object> getPolled()
            {
            return m_listPolled;
            }

        // ----- data members -----------------------------------------------

        /**
         * The number of elements to poll.
         */
        private final int m_cElement;

        private final QueueType m_queue;

        private final CountDownLatch m_latch;

        private final Function<QueueType, Object> m_pollFunction;

        private final List<Object> m_listPolled = new ArrayList<>();
        }
    }
