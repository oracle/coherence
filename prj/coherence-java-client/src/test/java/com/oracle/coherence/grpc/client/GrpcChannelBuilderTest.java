/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import io.grpc.ManagedChannel;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.06.23
 * @since 20.06
 */
public class GrpcChannelBuilderTest
    {
    @Test
    public void shouldLoadChannelsFromEmptyConfig() throws Exception
        {
        Config               cfg     = Config.empty();
        GrpcChannelBuilder   builder = GrpcChannelBuilder.create(cfg);

        ManagedChannel       channelDefault = builder.channel("default");
        assertThat(channelDefault, is(notNullValue()));
        assertThat(channelDefault.authority(), is("localhost:1408"));

        ManagedChannel       channelTest = builder.channel("test");
        assertThat(channelTest, is(notNullValue()));
        assertThat(channelTest.authority(), is("test:1408"));
        }

    @Test
    public void shouldLoadChannelsFromList() throws Exception
        {
        Config               cfg     = Config.create(ConfigSources.classpath("channels-as-list.yaml"));
        GrpcChannelBuilder   builder = GrpcChannelBuilder.create(cfg);

        ManagedChannel       channelDefault = builder.channel("default");
        assertThat(channelDefault, is(notNullValue()));
        assertThat(channelDefault.authority(), is("foo.com:1234"));

        ManagedChannel       channelTest = builder.channel("test");
        assertThat(channelTest, is(notNullValue()));
        assertThat(channelTest.authority(), is("bar.com:5678"));
        }

    @Test
    public void shouldLoadChannelsFromMap() throws Exception
        {
        Config               cfg     = Config.create(ConfigSources.classpath("channels-as-map.yaml"));
        GrpcChannelBuilder   builder = GrpcChannelBuilder.create(cfg);

        ManagedChannel       channelDefault = builder.channel("default");
        assertThat(channelDefault, is(notNullValue()));
        assertThat(channelDefault.authority(), is("foo.com:1234"));

        ManagedChannel       channelTest = builder.channel("test");
        assertThat(channelTest, is(notNullValue()));
        assertThat(channelTest.authority(), is("bar.com:5678"));
        }

    }
