/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.VirtualPerTaskBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.XmlSimpleName;

/**
 * An {@link ElementProcessor} for {@code single} elements.
 *
 * @author rl  3.26.2024
 * @since 15.1.1.0
 */
@XmlSimpleName("virtual-per-task")
public class VirtualPerTaskProcessor
        extends AbstractExecutorProcessor<VirtualPerTaskBuilder>
    {
    // ----- AbstractExecutorProcessor methods ------------------------------

    @Override
    protected VirtualPerTaskBuilder builder()
        {
        return new VirtualPerTaskBuilder();
        }
    }
