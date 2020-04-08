/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.SafeHashSet;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;


/**
* An abstract key-based bundler serves as a base for NamedCache get() and
* remove() operation bundling as well as the CacheStore load() and erase()
* operation bundling.
*
* @author gg 2007.01.28
* @since Coherence 3.3
*/
public abstract class AbstractKeyBundler
        extends AbstractBundler
    {
    // ----- bundling support ------------------------------------------------

    /**
    * Process the specified key in a most optimal way according to the
    * bundle settings.
    *
    * @param oKey  the key to process
    *
    * @return an execution result according to the caller's contract
    */
    protected Object process(Object oKey)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                return unbundle(oKey);
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
                        boolean fFirst = bundle.add(oKey);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            return bundle.process(fBurst, oKey);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }

    /**
    * Process a colKeys of specified items in a most optimal way according to
    * the bundle settings.
    *
    * @param colKeys  the collection of keys to process
    *
    * @return an execution result according to the caller's contract
    */
    protected Map processAll(Collection colKeys)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                return bundle(colKeys);
                }

            Bundle   bundle;
            boolean fBurst;
            while (true)
                {
                bundle = (Bundle) getOpenBundle();
                synchronized (bundle)
                    {
                    if (bundle.isOpen())
                        {
                        boolean fFirst = bundle.addAll(colKeys);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            return bundle.processAll(fBurst, colKeys);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }


    // ----- subclassing support --------------------------------------------

    /**
    * The bundle operation to be performed against a collected set of keys
    * by the concrete AbstractKeyBundler implementations. If an exception
    * occurs during bundle operation, it could be repeated using singleton sets.
    *
    * @param colKeys  a key collection to perform the bundled operation for
    *
    * @return the Map of operation results
    */
    protected abstract Map bundle(Collection colKeys);

    /**
    * Un-bundle bundled operation. This operation would be used if an exception
    * occurs during a bundled operation or if the number of active threads is
    * below the {@link #getThreadThreshold() ThreadThreshold} value.
    *
    * @param oKey  a key to perform the un-bundled operation for
    *
    * @return the operation result for the specified key, may be null
    */
    protected abstract Object unbundle(Object oKey);

    /**
    * {@inheritDoc}
    */
    protected AbstractBundler.Bundle instantiateBundle()
        {
        return new Bundle();
        }


    // ----- inner classes ---------------------------------------------------

    /**
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
        * Add the specified key to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param oKey  the key to add to this Bundle
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean add(Object oKey)
            {
            Set     setKeys = m_setKeys;
            boolean fFirst  = setKeys.isEmpty();
            setKeys.add(oKey);
            return fFirst;
            }

        /**
        * Add the specified collection of keys to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param colKeys  the collection of keys to add to this Bundle
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean addAll(Collection colKeys)
            {
            Set     setKeys = m_setKeys;
            boolean fFirst  = setKeys.isEmpty();
            setKeys.addAll(colKeys);
            return fFirst;
            }

        /**
        * Process the specified key according to this Bundle state.
        *
        * @param fBurst  true if this thread is supposed to perform an actual
        *                bundled operation (burst); false otherwise
        * @param oKey    the key to process
        *
        * @return an execution result according to the caller's contract
        */
        protected Object process(boolean fBurst, Object oKey)
            {
            try
                {
                return ensureResults(fBurst)
                    ? m_mapResults.get(oKey) : unbundle(oKey);
                }
            finally
                {
                releaseThread();
                }
            }

        /**
        * Process the specified key collection according to this Bundle state.
        *
        * @param fBurst  true if this thread is supposed to perform an actual
        *                bundled operation (burst); false otherwise
        * @param colKeys the collection of keys to process
        *
        * @return an execution result according to the caller's contract
        */
        protected Map processAll(boolean fBurst, Collection colKeys)
            {
            try
                {
                if (ensureResults(fBurst))
                    {
                    Map mapResults = m_mapResults;
                    Map map        = new HashMap(colKeys.size());
                    for (Iterator iter = colKeys.iterator(); iter.hasNext(); )
                        {
                        Object oKey = iter.next();
                        Object oVal = mapResults.get(oKey);
                        if (oVal != null)
                            {
                            map.put(oKey, oVal);
                            }
                        }
                    return map;
                    }
                else
                    {
                    return bundle(colKeys);
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
            return Math.max(super.getBundleSize(), m_setKeys.size());
            }

        /**
        * {@inheritDoc}
        */
        protected void ensureResults()
            {
            m_mapResults = bundle(m_setKeys);
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
                // array, which may cause unnecessary resizing later on...
                // ideally, we would want to preserve the bucket count, but
                // clear the content; consider adding SafeHashSet.clearContent()
                m_setKeys.clear();
                m_mapResults = null;
                }
            return fRelease;
            }

        // ----- data fields -----------------------------------------------

        /**
        * This bundle content.
        */
        private Set m_setKeys = new SafeHashSet();

        /**
        * A result of the bundled processing.
        */
        private Map m_mapResults;
        }
    }