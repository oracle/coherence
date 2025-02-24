/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.internal.extend.protocol;


import com.oracle.coherence.grpc.internal.extend.messageFactory.GrpcNamedTopicFactory;

import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;

/**
 * The gRPC {@link NamedTopicProtocol}.
 *
 * @author Jonathan Knight  2025.01.25
 */
public class GrpcNamedTopicExtendProtocol
        extends NamedTopicProtocol
    {
    public GrpcNamedTopicExtendProtocol()
        {
        setName(NamedTopicProtocol.class.getSimpleName());
        }

    @Override
    protected MessageFactory instantiateMessageFactory(int nVersion)
        {
        return new GrpcNamedTopicFactory();
        }

    public static GrpcNamedTopicExtendProtocol getInstance()
        {
        return INSTANCE;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The singleton instance of {@link GrpcNamedTopicExtendProtocol}.
     */
    private static final GrpcNamedTopicExtendProtocol INSTANCE = new GrpcNamedTopicExtendProtocol();
    }
