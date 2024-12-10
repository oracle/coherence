/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * An implementation of a {@link PofConfigProvider} to make the
 * Concurrent POF configuration discoverable.
 *
 * @author Jonathan Knight  2022.06.03
 * @since 22.06
 */
public class ConcurrentPofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "coherence-concurrent-pof-config.xml";
        }
    }
