/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import io.helidon.microprofile.grpc.client.GrpcChannel;

import java.lang.annotation.Annotation;

/**
 * A {@link GrpcChannel} annotation literal.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
class GrpcChannelLiteral
    {
    /**
     * Create a {@link GrpcChannel} annotation literal.
     *
     * @param sName  the channel name
     *
     * @return {@link GrpcChannel} annotation literal
     *
     * @throws ClassNotFoundException if Helidon gRPC ius not on the classpath
     */
    public static Annotation of(String sName) throws ClassNotFoundException
        {
        Class<GrpcChannel> type = GrpcChannel.class;
        return new GrpcChannel()
            {
            @Override
            public Class<GrpcChannel> annotationType()
                {
                return type;
                }

            @Override
            public String name()
                {
                return sName;
                }
            };
        }
    }
