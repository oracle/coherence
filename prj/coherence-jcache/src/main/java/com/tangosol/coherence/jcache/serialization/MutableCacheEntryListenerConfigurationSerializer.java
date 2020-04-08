/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.serialization;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import java.io.IOException;

/**
 * The {@link PofSerializer} for {@link javax.cache.configuration.MutableCacheEntryListenerConfiguration}s.
 *
 * @author jf  2014.1.27
 * @since Coherence 12.1.3
 *
 */
public class MutableCacheEntryListenerConfigurationSerializer
        implements PofSerializer
    {
    // ----- PofSerializer interface ----------------------------------------

    @Override
    public void serialize(PofWriter out, Object o)
            throws IOException
        {
        MutableCacheEntryListenerConfiguration config = (MutableCacheEntryListenerConfiguration)o;
        out.writeObject(0, config.getCacheEntryListenerFactory());
        out.writeObject(1, config.getCacheEntryEventFilterFactory());
        out.writeBoolean(2, config.isSynchronous());
        out.writeBoolean(3, config.isOldValueRequired());
        out.writeRemainder(null);
        }

    @Override
    public Object deserialize(PofReader in)
            throws IOException
        {
        Factory<CacheEntryListener> factoryListener = (Factory<CacheEntryListener>) in.readObject(0);
        Factory<CacheEntryEventFilter> factoryFilter = (Factory<CacheEntryEventFilter>) in.readObject(1);
        boolean f_isSynchronous = in.readBoolean(2);
        boolean f_isOldValueRequired = in.readBoolean(3);
        in.readRemainder();
        return new MutableCacheEntryListenerConfiguration(factoryListener, factoryFilter, f_isOldValueRequired, f_isSynchronous);
        }
    }
