/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package concurrent.queues;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.concurrent.Queues;
import com.tangosol.internal.net.queue.NamedMapBlockingDeque;
import com.tangosol.internal.net.queue.NamedMapDeque;
import com.tangosol.internal.net.queue.SimpleNamedMapDeque;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import queues.DequeTests;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This is a test interface. The tests are not run directly, they are run
 * by other classes that implement this interface. This allows a kind
 * of multiple inheritance way to include the same tests in different
 * test class hierarchies.
 *
 * @param <DequeType> the type of {@link NamedBlockingDeque} to test
 */
@SuppressWarnings({"rawtypes", "unchecked", "resource"})
public interface NamedBlockingDequeTests<DequeType extends NamedBlockingDeque>
        extends DequeTests<DequeType>
    {
    /**
     * {@link NamedMapBlockingDeque} that comes from the {@link Queues}
     * utility will have a different name to the cache.
     */
    @Test
    default void shouldHaveCorrectName()
        {
        String                     sName = getNewName();
        NamedBlockingDeque<String> deque = Queues.deque(sName);
        assertThat(deque.getName(), is(sName));
        }

    /*
     * Inserts the specified element at the front of this deque,
     * waiting if necessary for space to become available.
     *
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    // ----- putFirst() tests -----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutFirstToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        deque.putFirst(sValue);

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutFirstToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            deque.putFirst(sValue);
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(cMessage));

        TreeSet<QueueKey> setKey = (TreeSet<QueueKey>) new TreeSet<>(cache.keySet());
        assertThat(setKey.size(), is(cMessage));

        int i = cMessage - 1;
        for (QueueKey key : setKey)
            {
            String sExpected = sPrefix + i;
            assertThat(cache.get(key), is(sExpected));
            i--;
            }
        }

    // ----- putLast() tests ------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutLastToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        deque.putLast(sValue);

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutLastToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            deque.putLast(sValue);
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
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

    /*
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque), waiting if necessary for
     * space to become available.
     *
     * <p>This method is equivalent to {@link BlockingDeque#putLast(Object) putLast}.
     *
     * @param e the element to add
     * @throws InterruptedException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    // ----- put() tests ----------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        deque.put(sValue);

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldPutToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            deque.put(sValue);
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
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

    /*
     * Retrieves and removes the first element of this deque, waiting
     * if necessary until an element becomes available.
     *
     * @return the head of this deque
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- takeFirst() tests ----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromTakeFirstWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.takeFirst(), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockTakeFirstUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CountDownLatch    latch   = new CountDownLatch(1);
        CompletableFuture future1 = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    return deque.takeFirst();
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
                assertThat(deque.offer("message-1"), is(true));
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
    default void shouldBlockTakeFirstForever(String sSerializer)
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.takeFirst();
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        Assertions.assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.SECONDS));
        }

    // ----- takeLast() tests -----------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromTakeLastWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.takeLast(), is("message-3"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockTakeLastUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CountDownLatch    latch   = new CountDownLatch(1);
        CompletableFuture future1 = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    return deque.takeLast();
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
                assertThat(deque.offer("message-1"), is(true));
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
    default void shouldBlockTakeLastForever(String sSerializer)
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.takeLast();
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        Assertions.assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.SECONDS));
        }

    /*
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), waiting if
     * necessary until an element becomes available.
     *
     * <p>This method is equivalent to {@link BlockingDeque#takeFirst() takeFirst}.
     *
     * @return the head of this deque
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- take() tests ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromTakeWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.take(), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockTakeUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CountDownLatch    latch   = new CountDownLatch(1);
        CompletableFuture future1 = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    return deque.take();
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
                assertThat(deque.offer("message-1"), is(true));
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
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.take();
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        Assertions.assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.SECONDS));
        }

    /*
     * Retrieves and removes the first element of this deque, waiting
     * up to the specified wait time if necessary for an element to
     * become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return the head of this deque, or {@code null} if the specified
     *         waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- pollFirst(timeout) tests ---------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromPollFirstWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.pollFirst(1, TimeUnit.MINUTES), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockPollFirstUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        String            sMessage   = "message-foo";
        CountDownLatch    latch      = new CountDownLatch(1);
        CompletableFuture futurePoll = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    assertThat(deque.pollFirst(1, TimeUnit.MINUTES), is(sMessage));
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
                assertThat(deque.offer(sMessage), is(true));
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
    default void shouldBlockPollFirstUntilTimeout(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.pollFirst(5, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        assertThat(future.get(10, TimeUnit.SECONDS), is(nullValue()));
        }

    // ----- pollLast(timeout) tests ----------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromPollLastWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.pollLast(1, TimeUnit.MINUTES), is("message-3"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockPollLastUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        String            sMessage   = "message-foo";
        CountDownLatch    latch      = new CountDownLatch(1);
        CompletableFuture futurePoll = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    assertThat(deque.pollLast(1, TimeUnit.MINUTES), is(sMessage));
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
                assertThat(deque.offer(sMessage), is(true));
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
    default void shouldBlockPollLastUntilTimeout(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.pollLast(5, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        assertThat(future.get(10, TimeUnit.SECONDS), is(nullValue()));
        }

    /*
     * Retrieves and removes the head of the queue represented by this deque
     * (in other words, the first element of this deque), waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * <p>This method is equivalent to
     * {@link BlockingDeque#pollFirst(long,TimeUnit) pollFirst}.
     *
     * @return the head of this deque, or {@code null} if the
     *         specified waiting time elapses before an element is available
     * @throws InterruptedException if interrupted while waiting
     */
    // ----- poll(timeout) tests --------------------------------------------


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldReturnFromPollWithTimeoutWhenValueAlreadyPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.offer("message-1"), is(true));
        assertThat(deque.offer("message-2"), is(true));
        assertThat(deque.offer("message-3"), is(true));
        assertThat(deque.poll(1, TimeUnit.MINUTES), is("message-1"));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldBlockPollWithTimeoutUntilValuesPresent(String sSerializer) throws Exception
        {
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        String            sMessage   = "message-foo";
        CountDownLatch    latch      = new CountDownLatch(1);
        CompletableFuture futurePoll = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    latch.countDown();
                    assertThat(deque.poll(1, TimeUnit.MINUTES), is(sMessage));
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
                assertThat(deque.offer(sMessage), is(true));
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
        NamedBlockingDeque deque = getNewCollection(sSerializer);
        assertThat(deque.isEmpty(), is(true));

        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() ->
                {
                try
                    {
                    return deque.poll(5, TimeUnit.SECONDS);
                    }
                catch (InterruptedException e)
                    {
                    throw Exceptions.ensureRuntimeException(e);
                    }
                });

        assertThat(future.get(10, TimeUnit.SECONDS), is(nullValue()));
        }

    /*
     * Inserts the specified element at the front of this deque,
     * waiting up to the specified wait time if necessary for space to
     * become available.
     *
     * @param e the element to add
     * @param timeout how long to wait before giving up, in units of
     *        {@code unit}
     * @param unit a {@code TimeUnit} determining how to interpret the
     *        {@code timeout} parameter
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available
     * @throws InterruptedException if interrupted while waiting
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    // ----- offerFirst(timeout) tests --------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferFirstWithTimeoutToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        assertThat(deque.offerFirst(sValue, 10, TimeUnit.SECONDS), is(true));

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferFirstWithToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(deque.offerFirst(sValue, 10, TimeUnit.SECONDS), is(true));
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(cMessage));

        TreeSet<QueueKey> setKey = (TreeSet<QueueKey>) new TreeSet<>(cache.keySet());
        assertThat(setKey.size(), is(cMessage));

        int i = cMessage - 1;
        for (QueueKey key : setKey)
            {
            String sExpected = sPrefix + i;
            assertThat(cache.get(key), is(sExpected));
            i--;
            }
        }

    // ----- offerLast(timeout) tests --------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferLastWithTimeoutToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        assertThat(deque.offerLast(sValue, 10, TimeUnit.SECONDS), is(true));

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferLastWithToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(deque.offerLast(sValue, 10, TimeUnit.SECONDS), is(true));
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
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

    /*
     * Inserts the specified element into the queue represented by this deque
     * (in other words, at the tail of this deque), waiting up to the
     * specified wait time if necessary for space to become available.
     *
     * <p>This method is equivalent to
     * {@link #offerLast(Object,long,TimeUnit) offerLast}.
     *
     * @param e the element to add
     * @return {@code true} if the element was added to this deque, else
     *         {@code false}
     * @throws InterruptedException {@inheritDoc}
     * @throws ClassCastException if the class of the specified element
     *         prevents it from being added to this deque
     * @throws NullPointerException if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *         element prevents it from being added to this deque
     */
    // ----- offer(timeout) tests --------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferWithTimeoutToDeque(String sSerializer) throws Exception
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        assertThat(deque.offerLast(sValue, 10, TimeUnit.SECONDS), is(true));

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        assertThat(cache.size(), is(1));

        Object oKey = cache.keySet().iterator().next();
        assertThat(oKey, is(instanceOf(QueueKey.class)));

        QueueKey queueKey = (QueueKey) oKey;
        int      nHash    = deque.getQueueNameHash();
        assertThat(queueKey.getHash(), is(nHash));
        assertThat(queueKey.getAssociatedKey(), is(nHash));

        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldOfferWithToDequeInOrder(String sSerializer) throws Exception
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(deque.offerLast(sValue, 10, TimeUnit.SECONDS), is(true));
            }

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
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

    // ----- drainTo(col) tests ---------------------------------------------

    @Test
    @SuppressWarnings("CollectionAddedToSelf")
    default void shouldNotDrainToSelf()
        {
        DequeType deque = getNewCollection();
        Assertions.assertThrows(IllegalArgumentException.class, () -> deque.drainTo(deque));
        }

    @Test
    default void shouldNotDrainToSameCache()
        {
        DequeType     deque = getNewCollection();
        NamedMap      cache = getCollectionBinaryCache(deque);
        NamedMapDeque queue = new SimpleNamedMapDeque("foo", cache);
        Assertions.assertThrows(IllegalArgumentException.class, () -> deque.drainTo(queue));
        }

    @Test
    @SuppressWarnings("CollectionAddedToSelf")
    default void shouldNotDrainMaxToSelf()
        {
        DequeType deque = getNewCollection();
        Assertions.assertThrows(IllegalArgumentException.class, () -> deque.drainTo(deque, 100));
        }

    @Test
    default void shouldNotDrainMaxToSameCache()
        {
        DequeType     deque = getNewCollection();
        NamedMap      cache = getCollectionCache(deque.getName());
        NamedMapDeque queue = new SimpleNamedMapDeque("foo", cache);
        Assertions.assertThrows(IllegalArgumentException.class, () -> deque.drainTo(queue, 100));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainToCollection(String sSerializer)
        {
        DequeType    deque       = getNewCollection(sSerializer);
        String       sPrefix     = "message-";
        int          cMessage    = 987;
        List<String> listOffered = new ArrayList<>();

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            listOffered.add(sValue);
            assertThat(deque.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(deque.drainTo(listDrain), is(cMessage));
        assertThat(listDrain, is(listOffered));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainMaxNumberOfElementsToCollection(String sSerializer)
        {
        DequeType    deque       = getNewCollection(sSerializer);
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
            assertThat(deque.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(deque.drainTo(listDrain, cMax), is(cMax));
        assertThat(listDrain.size(), is(cMax));
        assertThat(listDrain, is(listOffered));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    default void shouldDrainMaxNumberOfElementsToCollectionWhereDequeIsSmaller(String sSerializer)
        {
        DequeType    deque       = getNewCollection(sSerializer);
        String       sPrefix     = "message-";
        int          cMessage    = 123;
        int          cMax        = 456;
        List<String> listOffered = new ArrayList<>();

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            listOffered.add(sValue);
            assertThat(deque.offer(sValue), is(true));
            }

        List<String> listDrain = new ArrayList<>();
        assertThat(deque.drainTo(listDrain, cMax), is(cMessage));
        assertThat(listDrain.size(), is(cMessage));
        assertThat(listDrain, is(listOffered));
        }

    }
