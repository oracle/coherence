/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events.common;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.annotation.EntryEvents;
import com.tangosol.net.events.annotation.Interceptor;

import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.util.BinaryEntry;

import java.io.Serializable;

/**
 * Test EventInterceptor for mutable events
 * @author nsa 2011.08.13
 * @since 3.7.1
 */
@Interceptor(identifier = "Mutator")
@EntryEvents({EntryEvent.Type.INSERTING, EntryEvent.Type.UPDATING, EntryEvent.Type.REMOVING})
public class MutatingInterceptor
        implements EventInterceptor<EntryEvent<?, ?>>, Serializable
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Construct a MutatingInterceptor that will register for all mutable events.
     */
    public MutatingInterceptor()
        {
        super();
        }

    // ----- EventInterceptor methods ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public void onEvent(EntryEvent<?, ?> entryEvent)
        {
        Logger.log("Mutating onEvent for: " + entryEvent, Logger.ALWAYS);

        for (BinaryEntry entry : entryEvent.getEntrySet())
            {
            Integer NVal      = (Integer) entry.getValue();
            int     nThrowMod = m_nThrowMod;
            if (nThrowMod < 0 || (NVal != null && NVal % nThrowMod == 0))
                {
                throw new RuntimeException("We don't allow mutations around here!");
                }
            switch (entryEvent.getType())
                {
                case INSERTING:
                case UPDATING:
                    {
                    entry.setValue(NVal + 1);
                    break;
                    }
                case REMOVING:
                    {
                    NamedCache results = CacheFactory.getCache("results");
                        {
                        results.put(entry.getKey(), "Removed");
                        }
                    }
                }
            }
        }

    // ----- inner class: SetThrowModInvocable ------------------------------

    public static class SetThrowModInvocable
            extends AbstractInvocable
            implements Serializable
        {

        // ----- constructors -----------------------------------------------

        public SetThrowModInvocable()
            {
            }

        public SetThrowModInvocable(int nThrowMod)
            {
            m_nThrowMod = nThrowMod;
            }

        // ----- AbstractInvocable methods ----------------------------------

        @Override
        public void run()
            {
            InterceptorRegistry registry    = CacheFactory.getConfigurableCacheFactory().getInterceptorRegistry();
            MutatingInterceptor interceptor = (MutatingInterceptor) registry.getEventInterceptor("Mutator");

            interceptor.m_nThrowMod = m_nThrowMod;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value to set on the interceptor.
         */
        protected int m_nThrowMod;
        }

    /**
     * The non-remainder divisor used to determine whether an exception
     * should be thrown.
     */
    protected volatile int m_nThrowMod = 10;
    }
