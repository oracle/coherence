/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.lucene.pof;

import com.tangosol.io.pof.PofConfigProvider;

/**
 * An implementation of a {@link PofConfigProvider} to make the
 * Coherence Lucene integration's POF configuration discoverable.
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
public class LucenePofConfigProvider
        implements PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "coherence-lucene-pof-config.xml";
        }
    }
