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
@Interceptor(identifier = "Reporter", entryEvents = {EntryEvent.Type.INSERTED, EntryEvent.Type.UPDATED, EntryEvent.Type.REMOVED})
public class RegistrationCounterInterceptor
        extends UnnamedInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable
    {
    }
