/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * A {@link PofConfigProvider} to automatically load the
 * POF configuration file for this module.
 */
public class AiPofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "coherence-ai-pof-config.xml";
        }
    }
