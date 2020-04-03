/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test EventInterceptor for registration testing.
 *
 * @author pfm 2012.11.05
 * @since 12.1.2
 */
@EntryEvents({EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})
public class UnnamedInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable
    {
    // ----- constructors ---------------------------------------------------

    public UnnamedInterceptor()
        {
        }

    // ----- EventInterceptor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void onEvent(EntryEvent<?, ?> entryEvent)
        {
        CacheFactory.log("onEvent for: " + entryEvent);

        for (BinaryEntry entry : entryEvent.getEntrySet())
            {
            Integer NVal = (Integer) entry.getValue();

            switch (entryEvent.getType())
                {
                case INSERTED:
                case UPDATED:
                    {
                    NamedCache results = CacheFactory.getCache("results");
                    String sKey = this.getClass().getName() + m_nKeySuffix.incrementAndGet();
                    CacheFactory.log("Putting k=" + sKey + " v=" + entry.getValue() + "into results cache");
                    results.put(sKey, entry.getValue());
                    break;
                    }
                }
            }
        }

    /**
     * The non-remainder divisor used to determine whether an exception
     * should be thrown.
     */
    protected volatile int m_nThrowMod = 10;

    private static AtomicInteger m_nKeySuffix = new AtomicInteger();
    }
