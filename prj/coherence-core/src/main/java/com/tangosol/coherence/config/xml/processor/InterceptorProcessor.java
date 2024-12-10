/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.builder.NamedEventInterceptorBuilder;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.XmlSimpleName;

/**
 * A {@link ElementProcessor} for the &lt;interceptor&gt; element.
 *
 * @author pfm 2012.11.05
 * @since Coherence 12.1.2
 */
@XmlSimpleName("interceptor")
public class InterceptorProcessor
        extends CustomizableBuilderProcessor<NamedEventInterceptorBuilder>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an InterceptorProcessor.
     */
    public InterceptorProcessor()
        {
        super (NamedEventInterceptorBuilder.class);
        }
    }
