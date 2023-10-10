/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.base.Assertions;
import com.tangosol.coherence.config.builder.DaemonPoolBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.internal.util.DaemonPool;
import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} to process a daemon pool configuration.
 */
public class DaemonPoolProcessor
        implements ElementProcessor<ParameterizedBuilder<DaemonPool>>
    {
    @Override
    @SuppressWarnings("DataFlowIssue")
    public ParameterizedBuilder<DaemonPool> process(ProcessingContext context, XmlElement xml) throws ConfigurationException
        {
        ParameterizedBuilderRegistry registry = context.getCookie(ParameterizedBuilderRegistry.class);
        Assertions.azzert(registry != null);

        DaemonPoolBuilder builder = new DaemonPoolBuilder();
        context.inject(builder, xml);
        registry.registerBuilder(DaemonPool.class, DaemonPool.COMMON_POOL_BUILDER_NAME, builder);

        return null;
        }
    }
