/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.FixedBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.XmlSimpleName;

/**
 * An {@link ElementProcessor} for {@code fixed} elements.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
@XmlSimpleName("fixed")
public class FixedProcessor
        extends AbstractExecutorProcessor<FixedBuilder>
    {
    // ----- AbstractExecutorProcessor methods ------------------------------

    protected FixedBuilder builder()
        {
        return new FixedBuilder();
        }
    }