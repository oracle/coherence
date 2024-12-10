/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.run.xml.XmlElement;

/**
 * An {@link ElementProcessor} to process a gRPC channel.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class GrpcChannelProcessor
        implements ElementProcessor<DefaultGrpcChannelDependencies>
    {
    @Override
    public DefaultGrpcChannelDependencies process(ProcessingContext ctx, XmlElement xml) throws ConfigurationException
        {
        DefaultGrpcChannelDependencies dependencies = new DefaultGrpcChannelDependencies();
        ctx.inject(dependencies, xml);
        return dependencies;
        }
    }
