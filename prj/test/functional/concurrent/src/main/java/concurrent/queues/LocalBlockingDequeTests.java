/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package concurrent.queues;

import com.oracle.coherence.concurrent.Queues;

import com.tangosol.internal.net.queue.WrapperNamedMapDeque;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Binary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import queues.LocalDequeTests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalBlockingDequeTests<QueueType extends NamedBlockingDeque>
        extends LocalDequeTests<QueueType>
        implements NamedBlockingDequeTests<QueueType>
    {
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldHaveCorrectSerializer(String sSerializer)
        {
        // overridden because the test is not really able to switch serializers for
        // the concurrent session. It does not matter for this as everything to do
        // with serialization is already tested by other tests.
        }

    @SuppressWarnings("resource")
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldGetSameQueueAndDequeForSameName(String sSerializer)
        {
        String               sName = getNewName(sSerializer);
        WrapperNamedMapDeque queue = (WrapperNamedMapDeque) Queues.queue(sName);
        WrapperNamedMapDeque deque = (WrapperNamedMapDeque) Queues.deque(sName);

        assertThat(isSameNamedMap(queue.getNamedMap(), deque.getNamedMap()), is(true));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldDifferentQueuesForSameNameDifferentSession(String sSerializer)
        {
        Session              sessionOne = Coherence.getInstance().getSession(Queues.SESSION_NAME);
        Session              sessionTwo = Coherence.getInstance().getSession();
        String               sName      = getNewName(sSerializer);
        WrapperNamedMapDeque queue      = (WrapperNamedMapDeque) Queues.queue(sName, sessionOne);
        WrapperNamedMapDeque deque      = (WrapperNamedMapDeque) Queues.deque(sName, sessionTwo);

        assertThat(isSameNamedMap(queue.getNamedMap(), deque.getNamedMap()), is(false));
        }


    @Override
    public Session getSession()
        {
        return Coherence.getInstance().getSession(Queues.SESSION_NAME);
        }

    @Override
    public QueueType getNamedCollection(Session session, String sName)
        {
        return (QueueType) Queues.deque(sName, session);
        }

    @Override
    public NamedMap getCollectionCache(String sName)
        {
        return super.getCollectionCache(Queues.QUEUE_CACHE_PREFIX + sName);
        }

    @Override
    public NamedMap<Binary, Binary> getCollectionBinaryCache(String sName)
        {
        return super.getCollectionBinaryCache(Queues.QUEUE_CACHE_PREFIX + sName);
        }
    }
