/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.rag.internal.pof;

/**
 * An implementation of a {@link com.tangosol.io.pof.PofConfigProvider} to make the
 * Coherence RAG's POF configuration discoverable.
 *
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public class PofConfigProvider
        implements com.tangosol.io.pof.PofConfigProvider
    {
    @Override
    public String getConfigURI()
        {
        return "coherence-rag-pof-config.xml";
        }
    }
