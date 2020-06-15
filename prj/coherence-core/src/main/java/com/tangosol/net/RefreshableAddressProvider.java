/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.coherence.config.Config;

import com.tangosol.util.Base;
import com.tangosol.util.Daemon;

import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
* A RefreshableAddressProvider is an AddressProvider implementation 
* that wraps another AddressProvider and refresh the address list of 
* the provider asynchronously. This ensures that the behaviour of this 
* AddressProvider will be performant (specifically non-blocking).
* 
* @author rhl 2008-12-14
* @since Coherence 3.5
*/
public class RefreshableAddressProvider
        extends Base
        implements DescribableAddressProvider
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a RefreshableAddressProvider with the given AddressProvider
    * using default refresh interval
    */
    public RefreshableAddressProvider(AddressProvider ap)
        {
        this(ap, REFRESH_DEFAULT);
        }

    /**
    * Construct a RefreshableAddressProvider with the specified AddressProvider
    * and refresh interval
    *
    * @param lRefresh  the refresh interval
    */
    public RefreshableAddressProvider(AddressProvider ap, long lRefresh)
        {
        m_apRefresh = ap;
        f_lRefresh  = lRefresh;
        // populate the initial address list cache
        refreshAddressList();
        // initialize the refresh thread
        f_daemonRefresh = new RefreshThread(lRefresh);
        }

    /**
    * Obtain the next available address to use.
    *
    * @return the next available address or null if the list of available
    *         addresses was exhausted
    */
    protected InetSocketAddress getNextAddressInternal()
        {
        return m_apRefresh == null ? null : m_apRefresh.getNextAddress();
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        return Base.equals(m_apRefresh,
                o instanceof RefreshableAddressProvider
                ? ((RefreshableAddressProvider) o).m_apRefresh : o);
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return m_apRefresh == null ? 0 : m_apRefresh.hashCode();
        }


    // ----- AddressProvider interface --------------------------------------

    /**
    * {@inheritDoc}
    *
    * If the internal iterator used to return addresses is exhausted
    * then a new iterator is initialised and null is returned.
    *
    * @return the next available address or null if the list of available
    *         addresses was exhausted
    */
    public synchronized final InetSocketAddress getNextAddress()
        {
        return f_iterator.hasNext()
                ? (InetSocketAddress) f_iterator.next() : null;
        }

    /**
    * {@inheritDoc}
    */
    public synchronized final void accept()
        {
        // one of the addresses provided was accepted; switch the iterator into
        // circular mode to offer remaining addresses in the iterator and other
        // addresses, after an iterator refresh
        f_iterator.setCircular(true);
        }

    /**
    * {@inheritDoc}
    */
    public synchronized final void reject(Throwable eCause)
        {
        try
            {
            // remove the address from the cache.
            f_iterator.remove();
            }
        catch (UnsupportedOperationException | IllegalStateException e)
            {
            // could happen from concurrent use, even though our getNextAddress call is sync'd some other
            // thread could call it between our call and the call to reject, leaving the iterator in a
            // state where remove can't function
            }

        // TODO: should we keep a list of rejected Addresses to reject from
        // the underlying provider at the next refresh?
        }

    /**
     * {@inheritDoc}
     */
    public String[] getAddressDescriptions()
        {
        return m_apRefresh instanceof DescribableAddressProvider ?
                   ((DescribableAddressProvider) m_apRefresh).getAddressDescriptions() :
                   InetAddressHelper.getAddressDescriptions(m_listCache);
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Refresh the cached address list using the underlying provider.
    */
    protected void refreshAddressList()
        {
        List list = new ArrayList();

        InetSocketAddress address = getNextAddressInternal();
        while (address != null)
            {
            list.add(address);
            address = getNextAddressInternal();
            }

        m_listCache      = list;
        m_ldtLastRefresh = Base.getSafeTimeMillis();
        }

    /**
    * Start the refresh thread if not already running.
    */
    protected void ensureRefreshThread()
        {
        if (!f_daemonRefresh.isRunning())
            {
            f_daemonRefresh.start();
            }
        }


    // ----- inner class: RefreshThread -------------------------------------

    protected class RefreshThread
            extends Daemon
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a new RefreshThread with the specified refresh interval.
        *
        * @param lRefresh  the refresh interval
        */
        protected RefreshThread(long lRefresh)
            {
            super(RefreshableAddressProvider.this.getClass().getName() +
                  ": RefreshThread");
            f_lRefresh = lRefresh;
            }

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            long lRefresh = f_lRefresh;
            while (!isStopping())
                {
                try
                    {
                    refreshAddressList();
                    stop();
                    }
                catch (Throwable t)
                    {
                    err("An exception occurred while refreshing an address list: " +
                        "\n" + getStackTrace(t) +
                        "\nReducing the refresh rate.");
                    Base.sleep(lRefresh);
                    lRefresh = 2 * lRefresh;
                    }
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * The interval with which to attempt to refresh the address list.
        */
        protected final long f_lRefresh;
        }


    // ----- inner class: CircularIterator ----------------------------------

    /**
    * An Iterator implementation that can be converted into {@link #setCircular(boolean)
    * circular mode} to allow the remaining addresses and previously skipped
    * addresses to be used; typically set once an address has been {@link #accept()
    * accepted}.
    */
    protected class CircularIterator
            implements Iterator
        {
        @Override
        public boolean hasNext()
            {
            while (true)
                {
                boolean fNext = ensureIterator().hasNext();
                if (!fNext)
                    {
                    // the address list iterator was exhausted and should be
                    // refreshed for subsequent usage
                    refreshIterator();
                    if (m_fCircular)
                        {
                        m_fCircular = false;
                        continue;
                        }
                    }
                return fNext;
                }
            }

        @Override
        public Object next()
            {
            return ensureIterator().next();
            }

        @Override
        public void remove()
            {
            ensureIterator().remove();
            }

        // ----- circular iterator support ----------------------------------

        /**
        * Set whether the Iterator should operate in a circular mode.
        *
        * @param fCircular  whether the Iterator should operate in a circular
        *                   mode
        */
        public void setCircular(boolean fCircular)
            {
            m_fCircular = fCircular;
            }

        // ----- helpers ----------------------------------------------------

        /**
        * Return the cache iterator.
        *
        * @return the cache iterator
        */
        protected Iterator ensureIterator()
            {
            if (m_iterator == null)
                {
                refreshIterator();
                }

            return m_iterator;
            }

        /**
        * Set the value of {@code m_iterator} to a new iterator from the address
        * list, and ensure the address list is refreshed.
        */
        protected void refreshIterator()
            {
            // constructor guarantees m_listCache to be non-null
            m_iterator = m_listCache.iterator();

            // refresh the address list if m_listCache is older than the specified refresh interval
            if ((Base.getSafeTimeMillis() - m_ldtLastRefresh) > f_lRefresh)
                {
                ensureRefreshThread();
                }
            }

        // ----- data members -----------------------------------------------

        /**
        * Whether this Iterator is currently in a circular mode.
        */
        protected boolean m_fCircular;

        /**
        * The iterator used to implement the AddressProvider interface.
        */
        protected Iterator m_iterator;
        }


    // ----- constants and data members -------------------------------------

    /**
    * Default refresh time of 10 seconds.
    */
    public static final long REFRESH_DEFAULT =
            Config.getLong("coherence.wka.refresh.interval", 10000);

    /**
    * The interval with which to attempt to refresh the address list.
    */
    protected final long f_lRefresh;

    /**
    * The refresh daemon.
    */
    protected final Daemon f_daemonRefresh;

    /**
    * An Iterator over the cached set of addresses.
    */
    protected final CircularIterator f_iterator = new CircularIterator();

    /**
    * The cached addresses.
    */
    protected volatile List m_listCache;

    /**
    * The last timestamp when the address list was refreshed.
    */
    protected long m_ldtLastRefresh;

    /**
    * The address provider to be refreshed. 
    */
    protected AddressProvider m_apRefresh;
    }