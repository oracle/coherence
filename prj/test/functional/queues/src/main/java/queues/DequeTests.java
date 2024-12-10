/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;

@SuppressWarnings("rawtypes")
public interface DequeTests<QueueType extends NamedDeque>
        extends CollectionTests<QueueType, QueueType>
    {
    @Override
    QueueType getNamedCollection(Session session, String sName);

    @Override
    QueueType getCollection(Session session, String sName);
    }
