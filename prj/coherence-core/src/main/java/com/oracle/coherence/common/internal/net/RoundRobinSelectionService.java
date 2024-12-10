/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.net.SelectionService;

import java.nio.channels.SelectableChannel;


/**
 * The RoundRobinSelectionService load-balances channel registrations over a number
 * of child SelectionServices in a round-robin fashion.
 *
 * @author mf  2013.08.15
 */
public class RoundRobinSelectionService
        extends AbstractStickySelectionService
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a RoundRobinSelectionService which delegates to the provided
     * SelectionServices.
     *
     * @param cServices  the maximum number of SelectionServices to use
     * @param factory    the factory for producing SelectionServices
     */
    public RoundRobinSelectionService(int cServices,
            Factory<? extends SelectionService> factory)
        {
        super(cServices, factory);
        }

    @Override
    protected SelectionService selectService(SelectableChannel chan)
        {
        return f_aServices[Math.abs(m_cReg++) % f_aServices.length];
        }


    // ----- data members ---------------------------------------------------

    /**
     * The registration counter.
     */
    protected int m_cReg;
    }
