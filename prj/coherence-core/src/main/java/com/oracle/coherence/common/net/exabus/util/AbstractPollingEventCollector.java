/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;

/**
 * Abstract implementation of a PollingEventCollector
 *
 * @author mf  2013.02.19
 */
public abstract class AbstractPollingEventCollector
    implements PollingEventCollector
    {
    // ----- PollingEventCollector methods ----------------------------------

    /**
     * Bind the collector to the bus instance it will poll.
     *
     * This method is not for application use, but is to be called by busses which support binding themselves
     * to this type of collector.
     *
     * @param bus  the bus to bind to
     *
     * @throws IllegalStateException if already bound
     */
    public void bind(BusProcessor bus)
        {
        if (m_busProcessor == null)
            {
            m_busProcessor = bus;
            }
        else
            {
            throw new IllegalStateException("collector is already bound to a bus");
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The bus (if any) to which this collector is bound.
     */
    protected BusProcessor m_busProcessor;
    }
