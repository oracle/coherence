/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.io.json;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * A {@link PofConfigProvider} implementation that will allow
 * the Coherence JSON module's POF configuration to be
 * automatically discovered and loaded.
 */
public class JsonPofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "json-pof-config.xml";
        }
    }
