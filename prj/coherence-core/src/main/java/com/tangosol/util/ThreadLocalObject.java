/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.lang.reflect.Method;


/**
* This class provides an implementation of thread local storage.
* <p>
* As of Coherence 3.3 this class is a wrapper around {@link ThreadLocal}.
* <p>
* Note:  Where practical, use <code>java.lang.ThreadLocal</code> directly.
*
* @author cp 1997.05.03; mf 2007.04.27
*/
public class ThreadLocalObject
        extends Base
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Create a ThreadLocalObject.
    */
    public ThreadLocalObject()
        {
        this(null);
        }

    /**
    * Create a ThreadLocalObject with an initial value.
    *
    * @param object  the ThreadLocalObject value for the current thread
    */
    public ThreadLocalObject(Object object)
        {
        m_oInitial = object;
        }


    // ----- public methods -------------------------------------------------

    /**
    * Get the value of the ThreadLocalObject for the current thread.
    *
    * @return the value for the current thread
    *
    * @see #set(Object)
    */
    public Object get()
        {
        return m_tls.get();
        }

    /**
    * Set the value of the ThreadLocalObject for the current thread.
    *
    * @param object  the value for the current thread
    *
    * @see #get
    */
    public void set(Object object)
        {
        m_tls.set(object);
        }

    /**
    * Remove the ThreadLocalObject for the current thread.
    * <p>
    * Note: On 1.4.x JVMs this method will not free any storage but will
    *       reset the value to the initial value.
    */
    public void remove()
        {
        Method methodRemove = m_methodRemove;
        if (methodRemove == null)
            {
            m_tls.set(m_oInitial);
            }
        else
            {
            try
                {
                methodRemove.invoke(m_tls);
                }
            catch (Exception e)
                {
                m_tls.set(m_oInitial);
                }
            }
        }

    /**
    * Get the value of the ThreadLocalObject for the current thread.
    *
    * @return the value for the current thread
    *
    * @see #setObject(Object)
    */
    public Object getObject()
        {
        return m_tls.get();
        }

    /**
    * Set the value of the ThreadLocalObject for the current thread.
    *
    * @param object  the value for the current thread
    *
    * @see #getObject
    */
    public void setObject(Object object)
        {
        m_tls.set(object);
        }


    // ----- ThreadLocalObject maintenance ----------------------------------

    /**
    * Check if pruning is necessary.
    *
    * @deprecated as of Coherence 3.3 this method is a no-op
    */
    public void check()
        {
        }

    /**
    * Prune the list of dead threads.
    *
    * @deprecated as of Coherence 3.3 this method is a no-op
    */
    public void prune()
        {
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Get the number of sets/removes necessary before a rollover occurs.
    *
    * @return the rollover value
    *
    * @deprecated as of Coherence 3.3 this method always returns zero.
    */
    public int getRollover()
        {
        return 0;
        }

    /**
    * Set the number of sets/removes necessary before a rollover occurs.
    *
    * @param cRollover  the rollover value
    *
    * @deprecated as of Coherence 3.3 this method is a no-op
    */
    public void setRollover(int cRollover)
        {
        }

    /**
    * Get the minimum number of seconds between prunings.
    *
    * @return the minimum number of milliseconds to delay
    *
    * @deprecated as of Coherence 3.3 this method always returns zero.
    */
    public int getPruneDelay()
        {
        return 0;
        }

    /**
    * Set the minimum number of seconds between prunings.
    *
    * @param cMillisDelay  the minimum number of milliseconds to delay
    *
    * @deprecated as of Coherence 3.3 this method is a no-op
    */
    public void setPruneDelay(int cMillisDelay)
        {
        }

    /**
    * Get the value stored by this instance of ThreadLocalObject.
    *
    * @return the value stored by this instance of ThreadLocalObject
    */
    protected Object getValue()
        {
        return m_tls.get();
        }

    /**
    * Set the value stored by this instance of ThreadLocalObject.
    *
    * @param object the new value
    */
    protected void setValue(Object object)
        {
        m_tls.set(object);
        }


    // ---- data members ----------------------------------------------------

    /**
    * ThreadLocal backing the ThreadLocalObject.
    */
    private final ThreadLocal m_tls = new ThreadLocal()
        {
        protected Object initialValue()
            {
            return m_oInitial;
            }
        };

    /**
    * The initial value for the ThreadLocalObject.
    */
    private Object m_oInitial;


    // ----- constants ------------------------------------------------------

    /**
    * Reference to the 1.5 and later ThreadLocal.remove() method.
    */
    private static final Method m_methodRemove;
    static
        {
        Method methodTmp;
        try
            {
            methodTmp = Class.forName("java.lang.ThreadLocal").getMethod("remove");
            }
        catch (Exception e)
            {
            methodTmp = null;
            }
        m_methodRemove = methodTmp;
        }
    }
