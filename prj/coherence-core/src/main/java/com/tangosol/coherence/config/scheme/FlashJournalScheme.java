/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.scheme;

import com.tangosol.config.expression.ParameterResolver;

import java.util.Map;

/**
 * The {@link FlashJournalScheme} is used to create an instance of a
 * Flash Journal map.
 *
 * @author pfm  2011.10.30
 * @since Coherence 12.1.2
 */
public class FlashJournalScheme
        extends AbstractJournalScheme
    {
    // ----- MapBuilder interface -------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        throw new UnsupportedOperationException("Elastic Data features are not supported in Coherence CE");
        }
    }
