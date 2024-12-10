/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

import java.util.EventListener;
import java.util.HashSet;


/**
* Provide a simple, efficient, and thread-safe implementation of a list
* of event listeners.
*
* The implementation is optimized based on the assumption that listeners are
* added and removed relatively rarely, and that the list of listeners is
* requested relatively often.
*
* Thread safety is implemented by synchronizing on all methods that modify
* any data member of the class.  Read-only methods are not synchronized.
*
* @version 1.00, 11/16/97
* @author  Cameron Purdy
*/
public class Listeners
        extends Base
        implements Serializable
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public Listeners()
        {
        }


    // ----- methods --------------------------------------------------------

    /**
    * Add a listener.
    *
    * @param listener the EventListener to add
    */
    public synchronized void add(EventListener listener)
        {
        // Swing (Kestrel) will add/remove null listeners
        if (listener == null)
            {
            return;
            }

        if (!contains(listener))
            {
            EventListener[] aOld = getListenerListFor(listener);
            int             c    = aOld.length;
            EventListener[] aNew = new EventListener[c + 1];

            System.arraycopy(aOld, 0, aNew, 0, c);
            aNew[c] = listener;
            setListenerListFor(listener, aNew);
            m_sListenerNames = null;
            }
        }

    /**
    * Remove a listener.
    *
    * @param listener the EventListener to remove
    */
    public synchronized void remove(EventListener listener)
        {
        // Swing (Kestrel) will add/remove null listeners
        if (listener == null)
            {
            return;
            }

        m_sListenerNames = null;

        EventListener[] aOld = getListenerListFor(listener);
        int             c    = aOld.length;

        // most common case - exactly one listener
        if (c == 1)
            {
            if (listener.equals(aOld[0]))
                {
                setListenerListFor(listener, BLANKLIST);
                }
            return;
            }

        if (c > 0)
            {
            // locate the listener in the list
            int i = indexOf(aOld, listener);
            if (i >= 0)
                {
                // remove the listener from the list
                EventListener[] aNew = new EventListener[c - 1];
                if (i > 0)
                    {
                    System.arraycopy(aOld, 0, aNew, 0, i);
                    }
                if (i + 1 < c)
                    {
                    System.arraycopy(aOld, i + 1, aNew, i, c - i - 1);
                    }
                setListenerListFor(listener, aNew);
                }
            }
        }

    /**
    * Add all listeners from another Listeners object.
    *
    * @param listeners  the Listeners to add
    */
    public synchronized void addAll(Listeners listeners)
        {
        if (listeners == null)
            {
            return;
            }

        m_aAsyncListeners = union(m_aAsyncListeners, listeners.m_aAsyncListeners);
        m_aSyncListeners  = union(m_aSyncListeners,  listeners.m_aSyncListeners);
        m_sListenerNames = null;
        }

    /**
    * Return an array containing the union of the specified lists.
    *
    * @param aListener1  array of listeners
    * @param aListener2  array of listeners
    *
    * @return the union of the two arrays of listeners
    */
    private static EventListener[] union(
            EventListener[] aListener1, EventListener[] aListener2)
        {
        int cListener1 = aListener1.length;
        int cListener2 = aListener2.length;

        // check the degenerate cases
        if (cListener1 == 0)
            {
            return aListener2;
            }
        if (cListener2 == 0)
            {
            return aListener1;
            }

        // remove duplicates
        HashSet setUnion = new HashSet();
        for (int i = 0; i < cListener1; i++)
            {
            setUnion.add(aListener1[i]);
            }
        for (int i = 0; i < cListener2; i++)
            {
            setUnion.add(aListener2[i]);
            }

        // check the cheap cases where one array is a subset of the other
        int cSize = setUnion.size();
        if (cSize == cListener1)
            {
            return aListener1;
            }
        if (cSize == cListener2)
            {
            return aListener2;
            }

        return (EventListener[]) setUnion.toArray(new EventListener[cSize]);
        }

    /**
    * Remove all listeners.
    */
    public synchronized void removeAll()
        {
        m_aAsyncListeners = BLANKLIST;
        m_aSyncListeners  = BLANKLIST;
        m_sListenerNames  = null;
        }

    /**
    * Check if there are no listeners.
    *
    * @return true if there are no listeners
    */
    public boolean isEmpty()
        {
        return m_aAsyncListeners.length == 0 && m_aSyncListeners.length == 0;
        }

    /**
    * Returns the number of listeners.
    *
    * @return the number of listeners
    */
    public int getListenerCount()
        {
        return m_aAsyncListeners.length + m_aSyncListeners.length;
        }

    /**
    * Check if a listener is in the list of listeners.
    *
    * @param listener the EventListener to search for
    *
    * @return true if the listener is in the list of listeners
    */
    public boolean contains(EventListener listener)
        {
        return indexOf(getListenerListFor(listener), listener) >= 0;
        }

    /**
    * Return the array of listeners (sync or async) that corresponds to
    * the specified listener.
    *
    * @param listener  the listener to find a array for
    *
    * @return the array of listeners corresponding to the specified listener
    */
    private EventListener[] getListenerListFor(EventListener listener)
        {
        return listener instanceof SynchronousListener ||
               (listener instanceof MapListener && ((MapListener)listener).isSynchronous())
               ? m_aSyncListeners : m_aAsyncListeners;
        }

    /**
    * Set the array of listeners (sync or async) that corresponds to the
    * specified listener.  For example, if the specified listener is a
    * SynchronousListener, set the synchronous listener array to the specified
    * listener list.
    *
    * @param listener   the listener to set the array for
    * @param aListener  the array of listeners
    */
    private void setListenerListFor(EventListener listener, EventListener[] aListener)
        {
        if (listener instanceof SynchronousListener ||
            listener instanceof MapListener && ((MapListener)listener).isSynchronous())
            {
            m_aSyncListeners = aListener;
            }
        else
            {
            m_aAsyncListeners = aListener;
            }
        m_sListenerNames = null;
        }

    /**
    * Locate a listener in the specified array of listeners.
    *
    * @param aListener  the array of listeners to search
    * @param listener   the EventListener to search for
    *
    * @return the index of the listener in the array of listeners
    */
    private int indexOf(EventListener[] aListener, EventListener listener)
        {
        int cListener = aListener.length;
        for (int i = 0; i < cListener; ++i)
            {
            if (listener.equals(aListener[i]))
                {
                return i;
                }
            }

        return -1;
        }

    /**
    * Get the array of event listeners.  It is illegal to modify the return value
    * from this method.
    *
    * @return the array of event listeners
    */
    public EventListener[] listeners()
        {
        EventListener[] aSync  = getSynchronousListeners();
        EventListener[] aAsync = getAsynchronousListeners();
        int             cSync  = aSync .length;
        int             cAsync = aAsync.length;

        // check common cases
        if (cSync == 0)
            {
            return aAsync;
            }
        if (cAsync == 0)
            {
            return aSync;
            }

        // concatenate lists
        EventListener[] aNew = new EventListener[cSync + cAsync];
        System.arraycopy(aSync,  0, aNew, 0, cSync);
        System.arraycopy(aAsync, 0, aNew, cSync, cAsync);
        return aNew;
        }

    /**
    * Return a comma separated list of the listener class names.
    *
    * @return a comma separated list of the listener class names
    */
    public String getListenerClassNames()
        {
        String sNames = m_sListenerNames;
        if (sNames == null)
            {
            synchronized (this)
                {
                StringBuilder sb     = new StringBuilder();
                boolean       fFirst = true;
                for (EventListener listener : listeners())
                    {
                    String sName = listener.getClass().getName();
                    if (sName != null)
                        {
                        if (fFirst)
                            {
                            sb.append(", ");
                            fFirst = false;
                            }
                        sb.append(sName);
                        }
                    }
                sNames = m_sListenerNames = sb.toString();
                }
            }

        return sNames;
        }

    /**
    * Get the array of asynchronous event listeners.  It is illegal to modify the
    * return value from this method.
    *
    * @return the array of asynchronous event listeners
    */
    public EventListener[] getAsynchronousListeners()
        {
        return m_aAsyncListeners;
        }

    /**
    * Get the array of synchronous event listeners.  It is illegal to modify the
    * return value from this method.
    *
    * @return the array of synchronous event listeners
    */
    public EventListener[] getSynchronousListeners()
        {
        return m_aSyncListeners;
        }

    /**
    * Set the array of filters associated with this Listeners object.
    *
    * @param aFilter  the array of associated filters
    */
    public void setFilters(Filter[] aFilter)
        {
        m_aFilters = aFilter;
        }

    /**
    * Return the array of filters associated with this Listeners object.
    *
    * @return the array of filters associated with this Listeners object
    */
    public Filter[] getFilters()
        {
        return m_aFilters;
        }

    /**
    * Return a string representation of the Listeners object.
    *
    * @return a string representation of the Listeners object
    */
    public String toString()
        {
        StringBuffer sb = new StringBuffer("Listeners{");

        for (int i = 0; i < 2; i++)
            {
            EventListener[] a = i == 0 ? m_aAsyncListeners : m_aSyncListeners;
            for (int j = 0, c = a.length; j < c; ++j)
                {
                if (j > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(a[j]);
                }
            }
        sb.append('}');
        return sb.toString();
        }


    // ----- constants ------------------------------------------------------

    /**
    * A blank array of listeners.
    */
    private static final EventListener[] BLANKLIST = new EventListener[0];


    // ----- data members ---------------------------------------------------

    /**
    * The registered asynchronous listeners.  The contents of this array are
    * immutable; to add or remove listeners, a copy of this array is made and
    * the new array replaces the old.
    */
    private EventListener[] m_aAsyncListeners = BLANKLIST;

    /**
    * The registered synchronous listeners.  The contents of this array are
    * immutable; to add or remove listeners, a copy of this array is made and
    * the new array replaces the old.
    */
    private EventListener[] m_aSyncListeners  = BLANKLIST;

    /**
    * A comma separated list of the listener canonical class names.
    *
    * This information is maintained for tracing purposes, and is cached here as computing the canonical class
    * name is expensive.
    */
    private String m_sListenerNames;

    /**
    * An optional array of filters associated with this Listeners object.
    */
    private Filter[] m_aFilters;
    }
