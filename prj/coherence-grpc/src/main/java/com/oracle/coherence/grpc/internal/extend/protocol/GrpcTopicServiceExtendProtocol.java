/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.protocol;

import com.oracle.coherence.grpc.internal.extend.messageFactory.GrpcTopicServiceFactory;

import com.tangosol.coherence.component.net.extend.protocol.TopicServiceProtocol;

/**
 * The gRPC {@link TopicServiceProtocol}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcTopicServiceExtendProtocol
        extends TopicServiceProtocol
    {
    public GrpcTopicServiceExtendProtocol()
        {
        setName(TopicServiceProtocol.class.getSimpleName());
        }

    @Override
    protected MessageFactory instantiateMessageFactory(int nVersion)
        {
        return new GrpcTopicServiceFactory();
        }

    @Override
    public String getName()
        {
        return super.getName();
        }

    public static GrpcTopicServiceExtendProtocol getInstance()
        {
        return INSTANCE;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link GrpcTopicServiceExtendProtocol}.
     */
    private static final GrpcTopicServiceExtendProtocol INSTANCE = new GrpcTopicServiceExtendProtocol();
    }
