/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package concurrent.queues;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.concurrent.Queues;
import com.tangosol.internal.net.queue.NamedMapBlockingQueue;
import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.SimpleNamedMapQueue;
import com.tangosol.net.NamedBlockingQueue;
import com.tangosol.net.NamedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import queues.QueueTests;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is a test interface. The tests are not run directly, they are run
 * by other classes that implement this interface. This allows a kind
 * of multiple inheritance way to include the same tests in different
 * test class hierarchies.
 *
 * @param <QueueType> the type of {@link NamedBlockingQueue} to test
 */
@SuppressWarnings({"rawtypes", "unchecked", "resource"})
public interface NamedBlockingQueueTests<QueueType extends NamedBlockingQueue>
        extends QueueTests<QueueType>
    {
    /**
     * {@link NamedMapBlockingQueue} that comes from the {@link Queues}
     * utility will have a different name to the cache.
     */
    @Test
    default void shouldHaveCorrectName()
        {
        String                     sName = getNewName();
        NamedBlockingQueue<String> queue = Queues.queue(sName);
        assertThat(queue.getName(), is(sName));
        }

    /*
     * Inserts the specified element into the queue represented by this queue
     * (in other words, at the tail of this queue), waiting if necessary for
     * space to become available.
     *
     * <p>This method is equivalent to {@link BlockingQueue#putLast(Object) putLast}.
     *
     * @param e the element to add
     * @throws InterruptedException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this queue
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this queue
     */
    // ----- put() tests ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutToQueue(String sSerializer) throws Exception
        {
        QueueType queue  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        queue.put(sValue);

        NamedMap<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(1));

        Object oKey   = cache.keySet().iterator().next();
        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutToQueueInOrder(String sSerializer) throws Exception
        {
        QueueType queue    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            queue.put(sValue);
            }

        NamedMap<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(cMessage));

        TreeSet<?> setKey = new TreeSet<>(cache.keySet());
        assertThat(setKey.size(), is(cMessage));

        int i = 0;
        for (Object key : setKey)
            {
            String sExpected = sPrefix + i;
            assertThat(cache.get(key), is(sExpected));
            i++;
            }
        }

    /*
     * Retrieves and removes the head of the queue represented by this queue
     * (in other words, the first element of this queue), waiting if
     * necessary until an element becomes available.
     *
     * <p>This method is equivalent to {@link BlockingQueue#takeFirst() takeFirst}.
     *
     * @return the head of this queue
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- take() tests ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromTakeWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.offer("message-1"), is(true));
        assertThat(queue.offer("message-2"), is(true));
        assertThat(queue.offer("message-3"), is(true));
        assertThat(queue.take(), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockTakeUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.isEmpty(), is(true));

        CountDownLatch    latch   = new CountDownLatch(1);
        CompletableFuture future1 = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    return queue.take();
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() ->
            {
            try
                {
                assertThat(latch.await(1, TimeUnit.MINUTES), is(true));
                Thread.sleep(5000);
                assertThat(queue.offer("message-1"), is(true));
                }
            catch (InterruptedException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            });

        future2.get(1, TimeUnit.MINUTES);

        Object oValue = future1.get(1, TimeUnit.MINUTES);
        assertThat(oValue, is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockTakeForever(String sSerializer)
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.isEmpty(), is(true));

        CompletableFuture future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return queue.take();
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        Assertions.assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.SECONDS));
        }

    /*
     * Retrieves and removes the head of the queue represented by this queue
     * (in other words, the first element of this queue), waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * <p>This method is equivalent to
     * {@link BlockingQueue#pollFirst(long,TimeUnit) pollFirst}.
     *
     * @return the head of this queue, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- poll(timeout) tests --------------------------------------------


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromPollWithTimeoutWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.offer("message-1"), is(true));
        assertThat(queue.offer("message-2"), is(true));
        assertThat(queue.offer("message-3"), is(true));
        assertThat(queue.poll(1, TimeUnit.MINUTES), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockPollWithTimeoutUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.isEmpty(), is(true));

        String            sMessage   = "message-foo";
        CountDownLatch    latch      = new CountDownLatch(1);
        CompletableFuture futurePoll = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    assertThat(queue.poll(1, TimeUnit.MINUTES), is(sMessage));
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                return null;
                });

        CompletableFuture<Void> futureOffer = CompletableFuture.runAsync(() ->
            {
            try
                {
                assertThat(latch.await(1, TimeUnit.MINUTES), is(true));
                Thread.sleep(5000);
                assertThat(queue.offer(sMessage), is(true));
                }
            catch (InterruptedException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            });

        futureOffer.get(1, TimeUnit.MINUTES);
        futurePoll.get(1, TimeUnit.MINUTES);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockPollWithTimeoutUntilTimeout(String sSerializer) throws Exception
        {
        NamedBlockingQueue queue = getNewCollection(sSerializer);
        assertThat(queue.isEmpty(), is(true));

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return queue.poll(5, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        assertThat(future.get(10, TimeUnit.SECONDS), is(nullValue()));
        }

    // ----- drainTo(col) tests ---------------------------------------------

    @Test
    @SuppressWarnings("CollectionAddedToSelf")
    default void shouldNotDrainToSelf()
        {
        QueueType queue = getNewCollection();
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.drainTo(queue));
        }

    @Test
    default void shouldNotDrainToSameCache()
        {
        QueueType     queue = getNewCollection();
        NamedMap      cache = getCollectionCache(queue.getName());
        NamedMapQueue ncq   = new SimpleNamedMapQueue("foo", cache);
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.drainTo(ncq));
        }

    @Test
    @SuppressWarnings("CollectionAddedToSelf")
    default void shouldNotDrainMaxToSelf()
        {
        QueueType queue = getNewCollection();
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.drainTo(queue, 100));
        }

    @Test
    default void shouldNotDrainMaxToSameCache()
        {
        QueueType     queue = getNewCollection();
        NamedMap      cache = getCollectionCache(queue.getName());
        NamedMapQueue ncq   = new SimpleNamedMapQueue("foo", cache);
        Assertions.assertThrows(IllegalArgumentException.class, () -> queue.drainTo(ncq, 100));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainToCollection(String sSerializer)
        {
        QueueType    queue       = getNewCollection(sSerializer);
        String       sPrefix     = "message-";
        int          cMessage    = 987;
        List<String> listOffered = new ArrayList<>();

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            listOffered.add(sValue);
            assertThat(queue.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(queue.drainTo(listDrain), is(cMessage));
        assertThat(listDrain, is(listOffered));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainMaxNumberOfElementsToCollection(String sSerializer)
        {
        QueueType    queue       = getNewCollection(sSerializer);
        String       sPrefix     = "message-";
        int          cMessage    = 1234;
        int          cMax        = 123;
        List<String> listOffered = new ArrayList<>();

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            if (i < cMax)
                {
                listOffered.add(sValue);
                }
            assertThat(queue.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(queue.drainTo(listDrain, cMax), is(cMax));
        assertThat(listDrain.size(), is(cMax));
        assertThat(listDrain, is(listOffered));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainMaxNumberOfElementsToCollectionWhereQueueIsSmaller(String sSerializer)
        {
        QueueType    queue       = getNewCollection(sSerializer);
        String       sPrefix     = "message-";
        int          cMessage    = 123;
        int          cMax        = 456;
        List<String> listOffered = new ArrayList<>();

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            listOffered.add(sValue);
            assertThat(queue.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(queue.drainTo(listDrain, cMax), is(cMessage));
        assertThat(listDrain.size(), is(cMessage));
        assertThat(listDrain, is(listOffered));
        }

    }
