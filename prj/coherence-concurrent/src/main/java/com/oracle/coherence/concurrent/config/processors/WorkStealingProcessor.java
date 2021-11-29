/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.config.processors;

import com.oracle.coherence.concurrent.config.builders.WorkStealingBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.XmlSimpleName;

/**
 * An {@link ElementProcessor} for {@code work-stealing} elements.
 *
 * @author rl  11.26.21
 * @since 21.12
 */
@XmlSimpleName("work-stealing")
public class WorkStealingProcessor
        extends AbstractExecutorProcessor<WorkStealingBuilder>
    {
    // ----- AbstractExecutorProcessor methods ------------------------------

    protected WorkStealingBuilder builder()
        {
        return new WorkStealingBuilder();
        }
    }
