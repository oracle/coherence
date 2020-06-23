/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import io.grpc.ManagedChannel;

import io.helidon.config.Config;

import io.helidon.grpc.client.GrpcChannelsProvider;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.spi.CDI;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional test for GrpcChannelBuilder.
 *
 * @author Jonathan Knight  2020.06.23
 * @since 20.06
 */
public class GrpcChannelBuilderIT
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.clustername", "ChannelBuilderIT");

        s_server = Server.create().start();
        }

    @AfterAll
    static void cleanup()
        {
        s_server.stop();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldBuildChannel() throws Exception
        {
        Config config = CDI.current().select(Config.class).get();
        GrpcChannelBuilder builder = GrpcChannelBuilder.builder(config).build();
        assertThat(builder, is(notNullValue()));

        ManagedChannel channelDefault = builder.channel(GrpcChannelsProvider.DEFAULT_CHANNEL_NAME);
        assertThat(channelDefault, is(notNullValue()));

        ManagedChannel channelTest = builder.channel("test");
        assertThat(channelTest, is(notNullValue()));
        }

    // ----- data members ---------------------------------------------------


    private static Server s_server;
    }
