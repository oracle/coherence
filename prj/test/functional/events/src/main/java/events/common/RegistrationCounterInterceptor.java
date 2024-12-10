/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.cache.EntryEvent;

import java.io.Serializable;

/**
 * Test EventInterceptor for registration testing.
 *
 * @author pfm 2012.11.05
 * @since 12.1.2
 */
@Interceptor(identifier = "Reporter")
@EntryEvents({EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})
public class RegistrationCounterInterceptor
        extends UnnamedInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable
    {
    }
