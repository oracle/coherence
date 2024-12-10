/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.SafeHashMap;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


/**
* An abstract entry-based bundler serves as a base for NamedCache.put() and
* CacheStore.store() operation bundling.
*
* @author gg 2007.01.28
* @since Coherence 3.3
*/
public abstract class AbstractEntryBundler
        extends AbstractBundler
    {
    // ----- bundling support ------------------------------------------------

    /**
    * Process the specified entry in a most optimal way according to the
    * bundle settings.
    *
    * @param oKey   the entry key
    * @param oValue the entry value
    */
    protected void process(Object oKey, Object oValue)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                bundle(Collections.singletonMap(oKey, oValue));
                return;
                }

            Bundle  bundle;
            boolean fBurst;
            while (true)
                {
                bundle = (Bundle) getOpenBundle();
                synchronized (bundle)
                    {
                    if (bundle.isOpen())
                        {
                        boolean fFirst = bundle.add(oKey, oValue);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            bundle.process(fBurst, oKey, oValue);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }

    /**
    * Process a colllection of entries in a most optimal way according to the
    * bundle settings.
    *
    * @param map  the collection of entries to process
    */
    protected void processAll(Map map)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                bundle(map);
                return;
                }

            Bundle  bundle;
            boolean fBurst;
            while (true)
                {
                bundle = (Bundle) getOpenBundle();
                synchronized (bundle)
                    {
                    if (bundle.isOpen())
                        {
                        boolean fFirst = bundle.addAll(map);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            bundle.processAll(fBurst, map);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }


    // ----- subclassing support --------------------------------------------

    /**
    * The bundle operation to be performed against a collected map of entries
    * by the concrete AbstractEntryBundler implementations. If an exception
    * occurs during bundle operation, it will be repeated using singleton maps.
    *
    * @param mapEntries  a map to perform the bundled operation for
    */
    abstract protected void bundle(Map mapEntries);

    /**
    * {@inheritDoc}
    */
    protected AbstractBundler.Bundle instantiateBundle()
        {
        return new Bundle();
        }


    // ----- inner classes ---------------------------------------------------

    /*
    * Bundle represents a unit of optimized execution.
    */
    protected class Bundle
            extends AbstractBundler.Bundle
        {
        /**
        * Default constructor.
        */
        protected Bundle()
            {
            super();
            }

        // ----- bundling support ------------------------------------------

        /**
        * Add the specified entry to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param oKey   the entry key
        * @param oValue the entry value
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean add(Object oKey, Object oValue)
            {
            Map     map    = m_mapEntries;
            boolean fFirst = map.isEmpty();
            map.put(oKey, oValue);
            return fFirst;
            }

        /**
        * Add the specified collection of entries to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param map  the collection of entries
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean addAll(Map map)
            {
            Map     mapEntries = m_mapEntries;
            boolean fFirst     = mapEntries.isEmpty();
            mapEntries.putAll(map);
            return fFirst;
            }

        /**
        * Process the specified entry according to this Bundle state.
        *
        * @param fBurst  true if this thread is supposed to perform an actual
        *                bundled operation (burst); false otherwise
        * @param oKey    the entry key
        * @param oValue  the entry value
        */
        protected void process(boolean fBurst, Object oKey, Object oValue)
            {
            try
                {
                if (!ensureResults(fBurst))
                    {
                    bundle(Collections.singletonMap(oKey, oValue));
                    }
                }
            finally
                {
                releaseThread();
                }
            }

        /**
        * Process the specified collection of entries according to this Bundle
        * state.
        *
        * @param fBurst  true if this thread is supposed to perform an actual
        *                bundled operation (burst); false otherwise
        * @param map  the collection of entries
        */
        protected void processAll(boolean fBurst, Map map)
            {
            try
                {
                if (!ensureResults(fBurst))
                    {
                    bundle(map);
                    }
                }
            finally
                {
                releaseThread();
                }
            }

        /**
        * {@inheritDoc}
        */
        protected int getBundleSize()
            {
            return Math.max(super.getBundleSize(), m_mapEntries.size());
            }

        /**
        * {@inheritDoc}
        */
        protected void ensureResults()
            {
            bundle(m_mapEntries);
            }

        /**
        * {@inheritDoc}
        */
        protected synchronized boolean releaseThread()
            {
            boolean fRelease = super.releaseThread();
            if (fRelease)
                {
                // TODO: unfortunately, clear() will drop the underlying bucket
                // array, which may cause unnecesary resizing later on...
                // ideally, we would want to preserve the bucket count, but
                // clear the content; consider adding SafeHashMap.clearContent()
                m_mapEntries.clear();
                }
            return fRelease;
            }

        // ----- data fileds -----------------------------------------------

        /**
        * This bundle content.
        */
        private Map m_mapEntries = new SafeHashMap();
        }
    }