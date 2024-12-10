/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;


import com.tangosol.internal.net.queue.BinaryNamedMapDeque;
import com.tangosol.internal.net.queue.ConverterNamedMapDeque;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import java.util.Queue;

public class BinaryQueueCertTests
        extends QueueCertTests
    {
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected Queue<String> createQueue(String sName, Session session)
        {
        NamedMap<Binary, Binary> cache       = session.getCache(sName, WithClassLoader.nullImplementation());
        BinaryNamedMapDeque      binaryDeque = new BinaryNamedMapDeque(sName, cache);
        BackingMapManagerContext context     = cache.getService().getBackingMapManager().getContext();

        return new ConverterNamedMapDeque<>(binaryDeque, context.getKeyFromInternalConverter(),
                context.getValueFromInternalConverter(),
                context.getKeyToInternalConverter(),
                context.getValueToInternalConverter());
        }
    }
