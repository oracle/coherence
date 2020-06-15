/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.tangosol.util.BinaryEntry;
import com.tangosol.util.SafeHashSet;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
* An abstract BinaryEntry-based bundler that serves as a base for
* BinaryEntryStore operation bundling.
*
* @author tb 2011.01.06
* @since Coherence 3.7
*/
public abstract class AbstractBinaryEntryBundler
        extends AbstractBundler
    {
    // ----- bundling support ------------------------------------------------

    /**
    * Process the specified binary entry in a most optimal way according to
    * the bundle settings.
    *
    * @param binEntry  the binary entry
    */
    protected void process(BinaryEntry binEntry)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                bundle(Collections.singleton(binEntry));
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
                        boolean fFirst = bundle.add(binEntry);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            bundle.process(fBurst, binEntry);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }

    /**
    * Process a collection of binary entries in a most optimal way according
    * to the bundle settings.
    *
    * @param set  the set of binary entries to process
    */
    protected void processAll(Set set)
        {
        AtomicInteger counter  = m_countThreads;
        int           cThreads = counter.incrementAndGet();
        try
            {
            if (cThreads < getThreadThreshold())
                {
                bundle(set);
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
                        boolean fFirst = bundle.addAll(set);

                        fBurst = bundle.waitForResults(fFirst);
                        break;
                        }
                    }
                }
            bundle.processAll(fBurst, set);
            }
        finally
            {
            counter.decrementAndGet();
            }
        }


    // ----- subclassing support --------------------------------------------

    /**
    * The bundle operation to be performed against a collected set of binary
    * entries by the concrete AbstractEntryBundler implementations. If an
    * exception occurs during bundle operation, it will be repeated using
    * singleton maps.
    *
    * @param setEntries  a set of binary entries to perform the bundled
    *                    operation for
    */
    abstract protected void bundle(Set setEntries);

    /**
    * Un-bundle bundled operation. This opeartion would be used if an
    * exception occurs during bundle operation or if the number of active
    * threads is below the {@link #getThreadThreshold() ThreadThreshold} value.
    *
    * @param binEntry  a binary entry to perform the un-bundled operation for
    */
    protected abstract void unbundle(BinaryEntry binEntry);

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

        // ----- bundling support ---------------------------------------

        /**
        * Add the specified binary entry to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param binEntry the binary entry
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean add(BinaryEntry binEntry)
            {
            Set     set    = m_setEntries;
            boolean fFirst = set.isEmpty();
            set.add(binEntry);
            return fFirst;
            }

        /**
        * Add the specified set of binary entries to the Bundle.
        * <p>
        * <b>Note:</b> a call to this method must be externally synchronized
        * for this Bundle object.
        *
        * @param setEntries  the set of binary entries
        *
        * @return true if this Bundle was empty prior to this call
        */
        protected boolean addAll(Set setEntries)
            {
            Set     set = m_setEntries;
            boolean fFirst     = set.isEmpty();
            set.addAll(setEntries);
            return fFirst;
            }

        /**
        * Process the specified binary entry according to this Bundle state.
        *
        * @param fBurst   true if this thread is supposed to perform an actual
        *                 bundled operation (burst); false otherwise
        * @param binEntry the binary entry
        */
        protected void process(boolean fBurst, BinaryEntry binEntry)
            {
            try
                {
                if (!ensureResults(fBurst))
                    {
                    bundle(Collections.singleton(binEntry));
                    }
                }
            finally
                {
                releaseThread();
                }
            }

        /**
        * Process the specified set of binary entries according to this Bundle
        * state.
        *
        * @param fBurst      true if this thread is supposed to perform an
        *                    actual bundled operation (burst);
        *                    false otherwise
        * @param setEntries  the set of entries
        */
        protected void processAll(boolean fBurst, Set setEntries)
            {
            try
                {
                if (!ensureResults(fBurst))
                    {
                    bundle(setEntries);
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
            return Math.max(super.getBundleSize(), m_setEntries.size());
            }

        /**
        * {@inheritDoc}
        */
        protected void ensureResults()
            {
            bundle(m_setEntries);
            }

        /**
        * {@inheritDoc}
        */
        protected synchronized boolean releaseThread()
            {
            boolean fRelease = super.releaseThread();
            if (fRelease)
                {
                m_setEntries.clear();
                }
            return fRelease;
            }

        // ----- data fileds --------------------------------------------

        /**
        * This bundle content.
        */
        private Set m_setEntries = new SafeHashSet();
        }
    }