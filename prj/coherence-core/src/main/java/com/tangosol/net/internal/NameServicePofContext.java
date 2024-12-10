/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofContext;

/**
 * The NameServicePofContext is a basic {@link PofContext} implementation which
 * supports the types used to manage Coherence*Extend connections.
 *
 * @author phf  2012.04.27
 *
 * @since Coherence 12.1.2
 */
public class NameServicePofContext
        extends ConfigurablePofContext
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a new NameServicePofContext.
     */
    public NameServicePofContext()
        {
        super("coherence-pof-config.xml");
        }

    // ----- constants ------------------------------------------------------

    /**
     * The NameServicePofContext singleton.
     */
    public static final NameServicePofContext INSTANCE = new NameServicePofContext();
    }
