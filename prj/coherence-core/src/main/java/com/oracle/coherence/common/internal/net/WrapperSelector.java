/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Blocking;

import com.tangosol.util.ConverterCollections;

import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

import java.io.IOException;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import java.lang.reflect.Method;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Set;


/**
* WrapperSelector is a Selector implementation which delegates all calls to a
* delegate Selector.
*
* @author mf  2010.04.27
* @since Coherence 3.6
*/
public class WrapperSelector
    extends AbstractSelector
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperSelector
    *
    * @param selector  the selector to wrap
    * @param provider  the WrapperSocketProvider
    *
    * @throws IOException  if an I/O error occurs
    */
    public WrapperSelector(Selector selector, SelectorProvider provider)
            throws IOException
        {
        super(provider);

        m_delegate        = selector;
        m_setKeys         = new KeySet(selector.keys());
        m_setSelectedKeys = new KeySet(selector.selectedKeys());
        }


    // ----- WrapperSelector methods ----------------------------------------

    /**
    * Return the Selector to which this selector delegates.
    *
    * @return the Selector to which this selector delegates
    */
    public Selector getDelegate()
        {
        return m_delegate;
        }


    // ----- Selector methods -----------------------------------------------

    /**
    * Unsupported.
    *
    * @return never
    *
    * @throws UnsupportedOperationException not supported
    */
    public static Selector open()
        {
        throw new UnsupportedOperationException();
        }

    /**
    * {@inheritDoc}
    */
    public Set keys()
        {
        return m_setKeys;
        }

    /**
    * {@inheritDoc}
    */
    public Set selectedKeys()
        {
        return m_setSelectedKeys;
        }

    /**
    * {@inheritDoc}
    */
    public int select()
            throws IOException
        {
        return select(0L);
        }

    /**
    * {@inheritDoc}
    */
    public int selectNow()
            throws IOException
        {
        return select(-1L);
        }

    /**
    * {@inheritDoc}
    */
    public int select(long timeout)
            throws IOException
        {
        // To fulfill the contract of thread-safety on the key sets we
        // synchronize on the Selector, Keys, and then SelectedKeys.  This is
        // is only necessary so that an application doing external
        // synchronization on any of these objects can block the selector from
        // proceeding as described in Selector JavaDoc.
        synchronized (this)
            {
            synchronized (m_setKeys)
                {
                synchronized (m_setSelectedKeys)
                    {
                    try
                        {
                        return timeout > 0 ? Blocking.select(m_delegate, timeout)
                            : timeout == 0 ? Blocking.select(m_delegate)
                                           : m_delegate.selectNow();
                        }
                    finally
                        {
                        cleanupCancelledKeys();
                        }
                    }
                }
            }
        }

    /**
    * {@inheritDoc}
    */
    public Selector wakeup()
        {
        m_delegate.wakeup();
        return this;
        }


    // ---- AbstractSelector methods ----------------------------------------

    /**
    * {@inheritDoc}
    */
    protected void implCloseSelector()
            throws IOException
        {
        // write lock to prevent concurrent register. This is a *work-around*
        // for the JDK bug described in Bug20721488.
        f_lockWrite.lock();
        try
            {
            m_delegate.close();
            }
        finally
            {
            f_lockWrite.unlock();
            }
        }

    /**
    * {@inheritDoc}
    */
    protected SelectionKey register(final AbstractSelectableChannel ch,
            int ops, Object att)
        {
        // read lock to prevent concurrent close. This is a *work-around*
        // for the JDK bug described in Bug20721488.
        f_lockRead.lock();
        try
            {
            return ((WrapperSelectableChannel) ch).registerInternal(
                    this, ops, att);
            }
        catch (IOException e)
            {
            // we must return a canceled invalid key
            return new WrapperSelectionKey(this, null, att)
                {
                public SelectableChannel channel()
                    {
                    throw new CancelledKeyException();
                    }

                public boolean isValid()
                    {
                    return false;
                    }

                public void cancel()
                    {
                    // already canceled
                    }

                public int interestOps()
                    {
                    throw new CancelledKeyException();
                    }

                public SelectionKey interestOps(int ops)
                    {
                    throw new CancelledKeyException();
                    }

                public int readyOps()
                    {
                    throw new CancelledKeyException();
                    }
                };
            }
        finally
            {
            f_lockRead.unlock();
            }
        }


    // ------ helper methods --------------------------------------------

    /**
     * Cleanup any previously cancelled keys
     *
     * @throws IOException  if an IO error occurs
     */
    protected void cleanupCancelledKeys()
            throws IOException
        {
        Set<SelectionKey> setCanceled = cancelledKeys();
        SelectionKey[]    aKey        = null;
        synchronized (setCanceled)
            {
            if (!setCanceled.isEmpty())
                {
                // first allow the delegates to finish cancellation
                m_delegate.selectNow();

                aKey = setCanceled.toArray(new SelectionKey[setCanceled.size()]);

                setCanceled.clear();
                }
            }

        // now finish cancellation of the wrappers

        // TBD: Commented out while considering Bug 35728460.
        /*
        if (aKey != null && METHOD_REMOVE_KEY != null)
            {
            for (SelectionKey key : aKey)
                {
                SelectableChannel chan = key.channel();
                if (chan instanceof AbstractSelectableChannel)
                    {
                    try
                        {
                        METHOD_REMOVE_KEY.invoke(chan, key);
                        }
                    catch (Throwable e) {}
                    }
                }
            }
         */
        }

    // ------ inner interface: WrapperSelectableChannel -----------------

    /**
    * An interface to be implemented by all channels which will be selectable
    * using this Selector.
    */
    public interface WrapperSelectableChannel
        {
        /**
        * Register with the specified selector.
        *
        * @param selector  the selector to register with
        * @param ops       the operations of interest
        * @param att       the attachment
        *
        * @return the wrapper selection key
        *
        * @throws IOException if an I/O error occurs
        */
        WrapperSelectionKey registerInternal(WrapperSelector selector, int ops,
                Object att)
                throws IOException;
        }


    // ------ inner class: WrapperSelectionKey --------------------------

    /**
    * WraperSelectionKey which delegates to a real SelectionKey.
    */
    public abstract static class WrapperSelectionKey
        extends SelectionKey
        {
        // ----- constructor --------------------------------------------

        public WrapperSelectionKey(WrapperSelector selector, SelectionKey key, Object att)
            {
            m_selector = selector;
            m_delegate = key;
            attach(att);

            if (key != null)
                {
                key.attach(this);
                }
            }


        // ----- SelectionKey methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public abstract SelectableChannel channel();

        /**
        * {@inheritDoc}
        */
        public Selector selector()
            {
            return m_selector;
            }

        /**
        * {@inheritDoc}
        */
        public boolean isValid()
            {
            return m_delegate.isValid();
            }

        /**
        * {@inheritDoc}
        */
        public void cancel()
            {
            Set<SelectionKey> setCancel = m_selector.cancelledKeys();
            synchronized (setCancel)
                {
                m_delegate.cancel();
                setCancel.add(this);
                }
            }

        /**
        * {@inheritDoc}
        */
        public int interestOps()
            {
            return m_delegate.interestOps();
            }

        /**
        * {@inheritDoc}
        */
        public SelectionKey interestOps(int ops)
            {
            m_delegate.interestOps(ops);
            return this;
            }

        /**
        * {@inheritDoc}
        */
        public int readyOps()
            {
            return m_delegate.readyOps();
            }

        /**
        * Return a description of the SelectionKey.
        *
        * @param key  the seleciton key
        * @return     the description
        */
        protected String getKeyString(SelectionKey key)
            {
            return key.isValid()
                   ? "interest=" + key.interestOps() + ", ready=" + key.readyOps()
                   : "cancelled";
            }

        // ----- Object methods -----------------------------------------

        /**
        * {@inheritDoc}
        */
        public String toString()
            {
            return getKeyString(this);
            }

        // ----- data members -------------------------------------------

        /**
        * The associated WrapperSelector.
        */
        protected WrapperSelector m_selector;

        /**
        * The delegate SelectionKey.
        */
        protected final SelectionKey m_delegate;
        }


    // ----- inner class: KeySet --------------------------------------------

    /**
    * A layered set implementation used for key sets.
    */
    public class KeySet
        extends ConverterCollections.ConverterSet
        {
        // ----- constructor --------------------------------------------

        protected KeySet(Set setBack)
            {
            super(setBack,
                    key -> ((SelectionKey) key).attachment(),
                    key -> ((WrapperSelectionKey) key).m_delegate);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Method which allows for cleanup of cached SelectionKeys from AbstractSelectableChannels.
     */
    private static final Method METHOD_REMOVE_KEY;

    static
        {
        // Note: the removeKey method on AbstractSelectableChannels is package private and is meant to be
        // accessed indirectly by having a Selector extend AbstractSelector and use
        // AbstractSelector.deregister(AbstractSelectionKey).  Unfortunately AbstractSelectionKey is largely
        // final and is not a sufficient base for what is needed to produce a usable WrapperSelectionKey.
        // Thus we are left relying on reflection.  Without invoking removeKey a cancelled SelectionKey will
        // remain cached within the SelectableChannel which ultimately prevents that channel from ever being
        // re-registered with the same Selector.
        // NOTE: Since this hack no longer works with Java9 we may eventually need to bite the bullet and work with
        // AbstractSelectionKey, the primary issue is that ASK.cancel() is final, but the effect of calling it ultimate
        // puts it into the AbstractSelector#cancelledKeys() which we do have access to, and thus could then call
        // our extension to ASK, adding a special cancelInternal() or something.  But until we have issues because of
        // lack of multi-reg we'll just hold off
        Method metRemove = null;

        // TBD: Commented out while considering Bug 35728460.
        //      In JDK 11 and greater, JPMS option --add-open java.base/java.nio=com.oracle.coherence
        //      is needed to call package private method AbstractSelectableChannel#removeKey(SelectionKey).
        //try
        //    {
        //    metRemove = AccessController.doPrivileged(
        //            (PrivilegedAction<Method>) () -> {
        //            try
        //                {
        //                Method met = AbstractSelectableChannel.class.getDeclaredMethod("removeKey", SelectionKey.class);
        //                met.setAccessible(true);
        //                return met;
        //                }
        //            catch (Throwable e)
        //                {
        //                return null;
        //                }
        //            });
        //
        //    }
        //catch (Exception e) {}

        METHOD_REMOVE_KEY = metRemove;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped Selector.
    */
    protected final Selector m_delegate;

    /**
    * The selector's keys
    */
    protected final Set m_setKeys;

    /**
    * The selector's selected keys
    */
    protected final Set m_setSelectedKeys;

    /**
     * Lock used to protect concurrent register/close
     */
    protected final ReadWriteLock f_lockSelector = new ReentrantReadWriteLock();
    protected final Lock          f_lockRead     = f_lockSelector.readLock();
    protected final Lock          f_lockWrite    = f_lockSelector.writeLock();
    }
