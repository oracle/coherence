/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.coherence.config.scheme.SimpleDequeScheme;
import com.tangosol.internal.net.queue.QueuePageIterator;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractDequeTests<DequeType extends NamedDeque>
        extends AbstractQueueTests<DequeType>
    {
    @Override
    @SuppressWarnings("unchecked")
    public DequeType getNamedCollection(Session session, String sName)
        {
        return (DequeType) SimpleDequeScheme.INSTANCE.realize(sName, session);
        }

    @Override
    public DequeType getCollection(Session session, String sName)
        {
        return (DequeType) SimpleDequeScheme.INSTANCE.realize(sName, session);
        }

    // ----- test prepend() method ------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPrependToQueue(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue1 = "message-1";
        String    sValue2 = "message-2";
        String    sValue3 = "message-3";

        long nId1 = deque.prepend(sValue1);
        long nId2 = deque.prepend(sValue2);
        long nId3 = deque.prepend(sValue3);
        assertThat(nId1, is(greaterThan(Long.MIN_VALUE)));
        assertThat(nId2, is(greaterThan(Long.MIN_VALUE)));
        assertThat(nId3, is(greaterThan(Long.MIN_VALUE)));

        NamedMap<?, ?> cache = getCollectionCache(deque.getName());
        int            nHash = deque.getQueueNameHash();

        QueueKey queueKey1 = new QueueKey(nHash, nId1);
        QueueKey queueKey2 = new QueueKey(nHash, nId2);
        QueueKey queueKey3 = new QueueKey(nHash, nId3);

        assertThat(cache.get(queueKey1), is(sValue1));
        assertThat(cache.get(queueKey2), is(sValue2));
        assertThat(cache.get(queueKey3), is(sValue3));
        }

    // ----- test addFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddFirstToDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        deque.addFirst(sValue);

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
    public void shouldAddFirstToDequeInOrder(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            deque.addFirst(sValue);
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

    // ----- test addLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAddLastToDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";

        deque.addLast(sValue);

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
    public void shouldAddLastToDequeInOrder(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            deque.addLast(sValue);
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

    // ----- test offerFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferFirstToDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";
        assertThat(deque.offerFirst(sValue), is(true));

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
    public void shouldOfferFirstToDequeInOrder(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(deque.offerFirst(sValue), is(true));
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

    // ----- test offerLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldOfferLastToDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        String    sValue = "message-1";
        assertThat(deque.offerLast(sValue), is(true));

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
    public void shouldOfferLastToDequeInOrder(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        String    sPrefix  = "message-";
        int       cMessage = 100;

        for (int i = 0; i < cMessage; i++)
            {
            String sValue = sPrefix + i;
            assertThat(deque.offerLast(sValue), is(true));
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

    // ----- test removeFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstFromEmptyDeque(String sSerializer)
        {
        DequeType deque = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, deque::removeFirst);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstFromDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.removeFirst();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        Assertions.assertThrows(NoSuchElementException.class, deque::removeFirst);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.removeFirst();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::removeFirst);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.removeFirst();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::removeFirst);
        }

    // ----- test removeLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastFromEmptyDeque(String sSerializer)
        {
        DequeType deque = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, deque::removeLast);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastFromDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.removeLast();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        Assertions.assertThrows(NoSuchElementException.class, deque::removeLast);
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = cMessage; i > 0; i--)
            {
            Object oValue = deque.removeLast();
            assertThat(oValue, is(sPrefix + (i - 1)));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::removeLast);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = cMessage; i > 0; i--)
            {
            Object oValue = deque.removeLast();
            assertThat(oValue, is(sPrefix + (i - 1)));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::removeLast);
        }

    // ----- test pollFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFirstFromEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Object    oValue = deque.pollFirst();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFirstFromDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.pollFirst();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        oValue = deque.pollFirst();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFirstFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.pollFirst();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.pollFirst(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollFirstFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.pollFirst();
            assertThat(oValue, is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.pollFirst(), is(nullValue()));
        }

    // ----- test pollLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollLastFromEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Object    oValue = deque.pollLast();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollLastFromDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.pollLast();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));

        oValue = deque.pollLast();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollLastFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = cMessage; i > 0; i--)
            {
            Object oValue = deque.pollLast();
            assertThat(oValue, is(sPrefix + (i - 1)));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.pollLast(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPollLastFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = cMessage; i > 0; i--)
            {
            Object oValue = deque.pollLast();
            assertThat(oValue, is(sPrefix + (i - 1)));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.pollLast(), is(nullValue()));
        }

    // ----- test getFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetFirstFromEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, deque::getFirst);
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetFirstFromDeque(String sSerializer)
        {
        DequeType  degue  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(degue.getName());
        QueueKey   key    = new QueueKey(degue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = degue.getFirst();
        assertThat(oValue, is(sValue));
        assertThat(degue.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetFirstFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.getFirst();
            assertThat(oValue, is(sPrefix + i));
            assertThat(deque.pollFirst(), is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::getFirst);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetFirstFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.getFirst();
            assertThat(oValue, is(sPrefix + i));
            assertThat(deque.pollFirst(), is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::getFirst);
        }

    // ----- test getLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetLastFromEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Assertions.assertThrows(NoSuchElementException.class, deque::getLast);
        }


    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetLastFromDeque(String sSerializer)
        {
        DequeType  degue  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(degue.getName());
        QueueKey   key    = new QueueKey(degue.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = degue.getLast();
        assertThat(oValue, is(sValue));
        assertThat(degue.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetLastFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = cMessage; i > 0; i--)
            {
            String sExpected = sPrefix + (i - 1);
            assertThat(deque.getLast(), is(sExpected));
            assertThat(deque.pollLast(), is(sExpected));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::getLast);
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetLastFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = cMessage; i > 0; i--)
            {
            String sExpected = sPrefix + (i - 1);
            assertThat(deque.getLast(), is(sExpected));
            assertThat(deque.pollLast(), is(sExpected));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        Assertions.assertThrows(NoSuchElementException.class, deque::getLast);
        }

    // ----- test peekFirst() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekFirstAtEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Object    oValue = deque.peekFirst();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekFirstAtDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.peekFirst();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));

        oValue = deque.peekFirst();
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekFirstFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.peekFirst();
            assertThat(oValue, is(sPrefix + i));
            assertThat(deque.pollFirst(), is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.peekFirst(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekFirstFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = 0; i < cMessage; i++)
            {
            Object oValue = deque.peekFirst();
            assertThat(oValue, is(sPrefix + i));
            assertThat(deque.pollFirst(), is(sPrefix + i));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.peekFirst(), is(nullValue()));
        }

    // ----- test peekLast() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekLastAtEmptyDeque(String sSerializer)
        {
        DequeType deque  = getNewCollection(sSerializer);
        Object    oValue = deque.peekLast();
        assertThat(oValue, is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekLastAtDeque(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "message-1";

        cache.put(key, sValue);

        Object oValue = deque.peekLast();
        assertThat(oValue, is(sValue));
        assertThat(deque.isEmpty(), is(false));
        assertThat(cache.isEmpty(), is(false));
        assertThat(cache.get(key), is(sValue));

        oValue = deque.peekLast();
        assertThat(oValue, is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekLastFromDequeInOrder(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        int        nHash    = deque.getQueueNameHash();
        String     sPrefix  = "message-";
        long       cMessage = 100L;

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, i);
            cache.put(key, sPrefix + i);
            }

        for (long i = cMessage; i > 0; i--)
            {
            String sExpected = sPrefix + (i - 1);
            assertThat(deque.peekLast(), is(sExpected));
            assertThat(deque.pollLast(), is(sExpected));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.peekLast(), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldPeekLastFromDequeInOrderWithGaps(String sSerializer)
        {
        DequeType  deque    = getNewCollection(sSerializer);
        NamedMap cache    = getCollectionCache(deque.getName());
        String     sPrefix  = "message-";
        long       cMessage = 100L;
        long       nId      = Math.abs(m_random.nextLong());
        int        nHash    = deque.getQueueNameHash();

        for (long i = 0; i < cMessage; i++)
            {
            QueueKey key = new QueueKey(nHash, nId);
            cache.put(key, sPrefix + i);
            nId += m_random.nextLong(10L) + 1L; // we add one to make sure we do not re-use the last key!
            }

        for (long i = cMessage; i > 0; i--)
            {
            String sExpected = sPrefix + (i - 1);
            assertThat(deque.peekLast(), is(sExpected));
            assertThat(deque.pollLast(), is(sExpected));
            }

        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(deque.peekLast(), is(nullValue()));
        }

    // ----- test removeFirstOccurrence() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstOccurrenceFromEmptyDeque(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        boolean   fRemoved = deque.removeFirstOccurrence("foo");
        assertThat(fRemoved, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstOccurrenceFromDequeWithOneValue(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "foo";

        cache.put(key, sValue);

        boolean fRemoved = deque.removeFirstOccurrence("foo");
        assertThat(fRemoved, is(true));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(cache.get(key), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveFirstOccurrenceFromDequeWithMultipleValue(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);

        cache.put(key = key.next(), "One");
        cache.put(key = key.next(), "Two");
        cache.put(key = key.next(), "foo");
        cache.put(key = key.next(), "Three");
        cache.put(key = key.next(), "foo");
        cache.put(key = key.next(), "Four");

        boolean fRemoved = deque.removeFirstOccurrence("foo");
        assertThat(fRemoved, is(true));

        assertThat(deque.poll(), is("One"));
        assertThat(deque.poll(), is("Two"));
        assertThat(deque.poll(), is("Three"));
        assertThat(deque.poll(), is("foo"));
        assertThat(deque.poll(), is("Four"));
        }

    // ----- test removeLastOccurrence() method ---------------------------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastOccurrenceFromEmptyDeque(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        boolean   fRemoved = deque.removeLastOccurrence("foo");
        assertThat(fRemoved, is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastOccurrenceFromDequeWithOneValue(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);
        String     sValue = "foo";

        cache.put(key, sValue);

        boolean fRemoved = deque.removeLastOccurrence("foo");
        assertThat(fRemoved, is(true));
        assertThat(deque.isEmpty(), is(true));
        assertThat(cache.isEmpty(), is(true));
        assertThat(cache.get(key), is(nullValue()));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldRemoveLastOccurrenceFromDequeWithMultipleValue(String sSerializer)
        {
        DequeType  deque  = getNewCollection(sSerializer);
        NamedMap cache  = getCollectionCache(deque.getName());
        QueueKey   key    = new QueueKey(deque.getQueueNameHash(), 0L);

        cache.put(key = key.next(), "One");
        cache.put(key = key.next(), "Two");
        cache.put(key = key.next(), "foo");
        cache.put(key = key.next(), "Three");
        cache.put(key = key.next(), "foo");
        cache.put(key = key.next(), "Four");

        boolean fRemoved = deque.removeLastOccurrence("foo");
        assertThat(fRemoved, is(true));

        assertThat(deque.poll(), is("One"));
        assertThat(deque.poll(), is("Two"));
        assertThat(deque.poll(), is("foo"));
        assertThat(deque.poll(), is("Three"));
        assertThat(deque.poll(), is("Four"));
        }

    // ----- test descendingIterator() method -------------------------------

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetDescendingIteratorFromEmptyDeque(String sSerializer)
        {
        DequeType deque    = getNewCollection(sSerializer);
        Iterator  iterator = deque.descendingIterator();
        assertThat(iterator.hasNext(), is(false));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetDescendingIteratorFromDequeInOrder(String sSerializer)
        {
        DequeType    deque    = getNewCollection(sSerializer);
        NamedMap   cache    = getCollectionCache(deque.getName());
        int          nHash    = deque.getQueueNameHash();
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

        Collections.reverse(expected);

        Iterator<String>  iterator = deque.descendingIterator();
        assertThat(iterator.hasNext(), is(true));

        List<String> actual = new ArrayList<>();
        while (iterator.hasNext())
            {
            actual.add(iterator.next());
            }

        assertThat(actual, is(expected));
        assertThat(deque.size(), is(cMessage));
        assertThat(cache.size(), is(cMessage));
        }
    }
