/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;

import java.io.Serializable;

/**
 * Test EventInterceptor for non mutable events.
 * @author nsa 2011.08.13
 * @since 3.7.1
 */
@Interceptor(identifier = "Reporter")
@EntryEvents({EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})
public class NonMutatingInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable
    {

    // ----- constructors ---------------------------------------------------

    public NonMutatingInterceptor()
        {
        }

    // ----- EventInterceptor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void onEvent(EntryEvent<?, ?> entryEvent)
        {
        Logger.log("onEvent for: " + entryEvent, Logger.ALWAYS);
        NamedCache results    = CacheFactory.getCache("results");

        for (BinaryEntry entry : entryEvent.getEntrySet())
            {
            Integer NVal = (Integer) entry.getValue();
            if (m_nThrowMod < 0 || (NVal != null && NVal % m_nThrowMod == 0))
                {
                throw new RuntimeException("We don't allow mutations around here!");
                }
            switch ((EntryEvent.Type) entryEvent.getType())
                {
                case INSERTED:
                case UPDATED:
                    {
                    Logger.log("Putting k=" + entry.getKey() + " v=" + entry.getValue() + "into results cache",
                               Logger.ALWAYS);
                    results.put(entry.getKey(), entry.getValue());
                    break;
                    }
                case REMOVED:
                    {
                    results = CacheFactory.getCache("results");
                        {
                        Logger.log("Putting k=" + entry.getKey() + " v=Removed into results cache", Logger.ALWAYS);
                        results.put(entry.getKey(), "Removed");
                        }
                    }
                }
            }
        }


    /**
     * The non-remainder divisor used to determine whether an exception
     * should be thrown.
     */
    protected volatile int m_nThrowMod = 10;
    }
