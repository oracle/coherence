/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.internal.net.queue.BinaryNamedMapDeque;
import com.tangosol.internal.net.queue.BinaryNamedMapQueue;
import com.tangosol.internal.net.queue.ConverterNamedMapDeque;
import com.tangosol.internal.net.queue.ConverterNamedMapQueue;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalBinaryQueueTests<QueueType extends NamedQueue>
        extends LocalQueueTests<QueueType>
    {
    @Override
    public QueueType getNamedCollection(Session session, String sName)
        {
        return getCollection(session, sName);
        }

    @Override
    public QueueType getCollection(Session session, String sName)
        {
        return (QueueType) ConverterNamedMapQueue.createQueue(new BinaryNamedMapQueue(sName, session));
        }

    }
