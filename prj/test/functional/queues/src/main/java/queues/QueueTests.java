/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;

@SuppressWarnings("rawtypes")
public interface QueueTests<QueueType extends NamedQueue>
        extends CollectionTests<QueueType, QueueType>
    {
    @Override
    QueueType getNamedCollection(Session session, String sName);

    @Override
    QueueType getCollection(Session session, String sName);
    }
