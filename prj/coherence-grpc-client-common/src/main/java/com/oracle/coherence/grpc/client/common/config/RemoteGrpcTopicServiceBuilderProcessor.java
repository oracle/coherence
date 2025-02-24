/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.tangosol.coherence.config.xml.processor.ServiceBuilderProcessor;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * A {@link ServiceBuilderProcessor} that builds a {@link GrpcTopicScheme}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class RemoteGrpcTopicServiceBuilderProcessor
        extends ServiceBuilderProcessor<GrpcTopicScheme>
    {
    public RemoteGrpcTopicServiceBuilderProcessor()
        {
        super(GrpcTopicScheme.class);
        }

    @Override
    public GrpcTopicScheme process(ProcessingContext context, XmlElement element) throws ConfigurationException
        {
        GrpcTopicScheme scheme = super.process(context, element);
        context.inject(scheme.getServiceDependencies(), element);
        return scheme;
        }
    }
