/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.internal.net.queue.BinaryNamedMapDeque;
import com.tangosol.internal.net.queue.ConverterNamedMapDeque;
import com.tangosol.net.NamedDeque;
import com.tangosol.net.Session;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalBinaryDequeTests<DequeType extends NamedDeque>
        extends LocalDequeTests<DequeType>
    {
    @Override
    public DequeType getNamedCollection(Session session, String sName)
        {
        return getCollection(session, sName);
        }

    @Override
    public DequeType getCollection(Session session, String sName)
        {
        return (DequeType) ConverterNamedMapDeque.createDeque(new BinaryNamedMapDeque(sName, session));
        }

    }
