/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import io.helidon.microprofile.grpc.client.GrpcChannel;

import javax.enterprise.util.AnnotationLiteral;

/**
 * A {@link GrpcChannel} annotation literal.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 14.1.2
 */
class GrpcChannelLiteral
        extends AnnotationLiteral<GrpcChannel>
        implements GrpcChannel
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code GrpcChannelLiteral}.
     *
     * @param sName  the channel literal name
     */
    protected GrpcChannelLiteral(String sName)
        {
        this.f_sName = sName;
        }

    // ----- GrpcChannel interface ------------------------------------------

    @Override
    public String name()
        {
        return f_sName;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Create a {@link GrpcChannel} annotation literal.
     *
     * @param sName  the channel name
     *
     * @return {@link GrpcChannel} annotation literal
     */
    public static GrpcChannelLiteral of(String sName)
        {
        return new GrpcChannelLiteral(sName);
        }

    // ----- constants ------------------------------------------------------

    private static final long serialVersionUID = 1L;

    // ----- data members ---------------------------------------------------

    private final String f_sName;
    }
