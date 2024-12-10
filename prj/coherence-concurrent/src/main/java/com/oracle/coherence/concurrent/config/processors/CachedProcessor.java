/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.CachedBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.XmlSimpleName;

/**
 * An {@link ElementProcessor} for {@code cached} elements.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
@XmlSimpleName("cached")
public class CachedProcessor
        extends AbstractExecutorProcessor<CachedBuilder>
    {
    // ----- AbstractExecutorProcessor methods ------------------------------

    protected CachedBuilder builder()
        {
        return new CachedBuilder();
        }
    }
