/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.tangosol.coherence.config.Config;
import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.net.grpc.GrpcChannelDependencies;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

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
        applyNamedSecureTransport(dependencies, xml);
        return dependencies;
        }

    /**
     * Apply the named-channel secure-transport system property.
     *
     * @param dependencies  the channel dependencies
     * @param xml           the gRPC channel XML
     */
    private void applyNamedSecureTransport(DefaultGrpcChannelDependencies dependencies, XmlElement xml)
        {
        XmlValue xmlName = xml.getAttribute("id");
        String   sName   = xmlName == null || xmlName.getString().isBlank()
                ? GrpcChannelDependencies.DEFAULT_CHANNEL_NAME
                : xmlName.getString();
        String   sPolicy = Config.getProperty(String.format(GrpcChannelDependencies.PROP_SECURE_TRANSPORT, sName));
        if (sPolicy != null)
            {
            dependencies.setSecureTransport(sPolicy);
            }
        }
    }
