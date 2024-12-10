/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.coherence.common.util.Sentry;

/**
 * A Gate implementation that allows for 2^31 reentrant enter calls by a single
 * thread.
 *
 * @since Coherence 3.7
 * @author coh 2010.10.14
 */
public class WrapperReentrantGate
        implements Gate<Void>
    {
    // ----- constructors -----------------------------------------------------

    /**
    * Default constructor.
    */
    public WrapperReentrantGate()
        {
        this(new ThreadGateLite());
        }

    /**
    * Construct a WrapperReentrantGate around the specified underlying gate.
    *
    * @param gate  a {@link Gate} to be wrapped
    */
    public WrapperReentrantGate(Gate<Void> gate)
        {
        m_gate = gate;
        }


    // ----- Gate interface ---------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean close(long cMillis)
        {
        return m_gate.close(cMillis);
        }

    /**
    * {@inheritDoc}
    */
    public void open()
        {
        m_gate.open();
        }

    /**
    * {@inheritDoc}
    */
    public boolean enter(long cMillis)
        {
        if (m_gate.isEnteredByCurrentThread() || m_gate.enter(cMillis))
            {
            m_tloEnters.get()[0]++;
            return true;
            }
        return false;
        }

    @Override
    public Sentry close()
        {
        return m_gate.close();
        }

    @Override
    public Sentry enter()
        {
        if (m_gate.isEnteredByCurrentThread() || m_gate.enter(-1))
            {
            m_tloEnters.get()[0]++;
            }
        return f_exitSentry;
        }

    /**
    * {@inheritDoc}
    */
    public void exit()
        {
        if (m_gate.isEnteredByCurrentThread() && --m_tloEnters.get()[0] > 0)
            {
            // gate was entered by this thread multiple times
            }
        else
            {
            m_gate.exit();
            }
        }

    /**
    * {@inheritDoc}
    */
    public boolean barEntry(long cMillis)
        {
        return m_gate.barEntry(cMillis);
        }

    /**
    * {@inheritDoc}
    */
    public boolean isClosedByCurrentThread()
        {
        return m_gate.isClosedByCurrentThread();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isEnteredByCurrentThread()
        {
        return m_gate.isEnteredByCurrentThread();
        }

    /**
    * {@inheritDoc}
    */
    public boolean isClosed()
        {
        return m_gate.isClosed();
        }


    // ----- data members -----------------------------------------------------

    /**
    * The wrapped gate.
    */
    private final Gate m_gate;

    /**
    * Counter for the current number of enters.
    * <p>
    * The ThreadLocal uses an int[] so that we can update it directly by
    * using the array reference.
    * <p>
    * If an Integer (int) were used as value type, there would be a need
    * to store the value back using {@link ThreadLocal#set(Object)}.
    */
    private final ThreadLocal<int[]> m_tloEnters = new ThreadLocal<int[]>()
                           {
                           protected int[] initialValue()
                               {
                               return new int[] { 0 };
                               }
                           };

    /**
      * Sentry to return from {@link #enter} that will {@link #exit} when the sentry is closed.
      */
     protected final Sentry f_exitSentry = new Sentry()
         {
         @Override
         public Object getResource()
             {
             return null;
             }

         @Override
         public void close()
             {
             exit();
             }
         };
    }
