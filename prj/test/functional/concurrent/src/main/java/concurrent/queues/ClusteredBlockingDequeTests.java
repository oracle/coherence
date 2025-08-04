/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package concurrent.queues;

import com.oracle.coherence.concurrent.Queues;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.util.Binary;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.TestAbortedException;
import queues.ClusteredDequeTests;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ClusteredBlockingDequeTests<QueueType extends NamedBlockingDeque>
        extends ClusteredDequeTests<QueueType>
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

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldFailToEnsureIncompatibleQueue(String sSerializer) throws Exception
        {
        throw new TestAbortedException("Test skipped for Concurrent");
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldEnsureCompatibleQueue(String sSerializer)
        {
        throw new TestAbortedException("Test skipped for Concurrent");
        }
    }
