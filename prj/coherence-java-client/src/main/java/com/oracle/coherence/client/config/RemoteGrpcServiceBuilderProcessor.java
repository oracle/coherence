/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client.config;

import com.tangosol.coherence.config.xml.processor.ServiceBuilderProcessor;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ServiceBuilderProcessor} that builds a {@link GrpcCacheScheme}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class RemoteGrpcServiceBuilderProcessor
        extends ServiceBuilderProcessor<GrpcCacheScheme>
    {
    public RemoteGrpcServiceBuilderProcessor()
        {
        super(GrpcCacheScheme.class);
        }

    @Override
    public GrpcCacheScheme process(ProcessingContext context, XmlElement element) throws ConfigurationException
        {
        GrpcCacheScheme scheme = super.process(context, element);
        context.inject(scheme.getServiceDependencies(), element);
        return scheme;
        }
    }
