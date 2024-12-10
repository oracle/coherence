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
 * The {@link RamJournalScheme} is used to create an instance of a Ram Journal map.
 *
 * @author pfm  2011.11.30
 * @since Coherence 12.1.2
 */
public class RamJournalScheme
        extends AbstractJournalScheme
    {
    // ----- MapBuilder interface  ------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Map realizeMap(ParameterResolver resolver, Dependencies dependencies)
        {
        throw new UnsupportedOperationException("Elastic Data features are not supported in Coherence CE");        
        }
    }
