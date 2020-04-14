/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package rest;

import com.tangosol.coherence.rest.util.processor.ProcessorFactory;

import com.tangosol.util.InvocableMap;

/**
 * ProcessorFactory that creates instances of CustomNumberDoubler.
 *
 * @author jh  2012.02.27
 */
public class CustomNumberDoublerFactory
        implements ProcessorFactory
    {

    // ----- ProcessorFactory interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public InvocableMap.EntryProcessor getProcessor(String... asArgs)
        {
        return new CustomNumberDoubler(asArgs[0]);
        }
    }
