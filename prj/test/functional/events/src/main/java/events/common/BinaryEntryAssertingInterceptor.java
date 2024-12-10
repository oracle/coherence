/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.util.BinaryEntry;

/**
 * BinaryEntryAssertingInterceptor is an EventInterceptor that calls
 * {@link BinaryEntry#getOriginalBinaryValue()} for INSERTED events.
 *
 * @author hr  2012.09.07
 * @since Coherence 12.1.2
 */
@Interceptor(identifier = BinaryEntryAssertingInterceptor.IDENTIFIER)
@EntryEvents(EntryEvent.Type.INSERTED)
public class BinaryEntryAssertingInterceptor
        extends AbstractTestInterceptor<EntryEvent<?, ?>>
    {

    @Override
    public void processEvent(EntryEvent<?, ?> event)
        {
        Enum eventType = event.getType();

        for (BinaryEntry<?, ?> binEntry : event.getEntrySet())
            {
            if (EntryEvent.Type.INSERTED == eventType) // better to be safe
                {
                if (binEntry.getOriginalBinaryValue() != null)
                    {
                    err("INSERTED original binary value should be null for key: " + binEntry.getKey());
                    }
                }
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String IDENTIFIER = "BinaryEntryAsserter";
    }
