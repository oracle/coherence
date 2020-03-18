/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net;


import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.net.SelectionService;

import java.nio.channels.SelectableChannel;


/**
 * The HashSelectionService partitions channel registrations over a number
 * of child SelectionServices for the purposes of load balancing.
 *
 * @author mf  2010.11.23
 */
public class HashSelectionService
        extends AbstractStickySelectionService
    {
    // ----- constructors --------------------------------------------------

    /**
     * Construct a HashSelectionService which delegates to the provided
     * SelectionServices.
     *
     * @param cServices  the maximum number of SelectionServices to use
     * @param factory    the factory for producing SelectionServices
     */
    public HashSelectionService(int cServices, Factory<? extends SelectionService> factory)
        {
        super(cServices, factory);
        }

    @Override
    protected SelectionService selectService(SelectableChannel chan)
        {
        return f_aServices[Hasher.mod(chan.hashCode(), f_aServices.length)];
        }
    }
