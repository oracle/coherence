/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.helidon.client;

import com.oracle.coherence.helidon.client.GrpcChannelLiteral;
import io.helidon.microprofile.grpc.client.GrpcChannel;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.09.29
 */
public class GrpcChannelLiteralTest
    {
    @Test
    public void shouldHaveName() throws Exception
        {
        Annotation literal = GrpcChannelLiteral.of("foo");
        assertThat(literal, is(instanceOf(GrpcChannel.class)));
        assertThat(((GrpcChannel) literal).name(), is("foo"));
        }

    @Test
    public void shouldHaveType() throws Exception
        {
        Annotation literal = GrpcChannelLiteral.of("foo");
        assertThat(literal.annotationType(), is(equalTo(GrpcChannel.class)));
        }

    }
