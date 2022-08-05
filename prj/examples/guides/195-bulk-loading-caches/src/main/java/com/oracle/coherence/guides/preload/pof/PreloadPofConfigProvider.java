/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.preload.pof;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * An implementation of a {@link PofConfigProvider} that will
 * ensure the preload POF configuration file is included in
 * the POF configuration when POF is used for serialization
 */
public class PreloadPofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "preload-pof-config.xml";
        }
    }
