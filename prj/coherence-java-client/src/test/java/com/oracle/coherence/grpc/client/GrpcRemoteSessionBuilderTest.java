/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

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
public class GrpcRemoteSessionBuilderTest
    {
    @Test
    public void shouldBuildSessionFromEmptyConfig() throws Exception
        {
        Config            cfg     = Config.empty();
        GrpcRemoteSession session = GrpcRemoteSession.builder(cfg).name("test").build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("test"));
        assertThat(session.getSerializerFormat(), is("java"));
        assertThat(session.getChannel(), is(notNullValue()));
        assertThat(session.getChannel().authority(), is("localhost:1408"));
        session.getChannel().shutdownNow();
        }

    @Test
    public void shouldBuildSessionFromList() throws Exception
        {
        Config            cfg     = Config.create(ConfigSources.classpath("sessions-as-list.yaml"));
        GrpcRemoteSession session = GrpcRemoteSession.builder(cfg).name("test-pof").build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("test-pof"));
        assertThat(session.getSerializerFormat(), is("pof"));
        assertThat(session.getChannel(), is(notNullValue()));
        assertThat(session.getChannel().authority(), is("foo.com:1234"));
        session.getChannel().shutdownNow();
        }


    @Test
    public void shouldBuildSessionFromMap() throws Exception
        {
        Config            cfg     = Config.create(ConfigSources.classpath("sessions-as-map.yaml"));
        GrpcRemoteSession session = GrpcRemoteSession.builder(cfg).name("test-pof").build();

        assertThat(session, is(notNullValue()));
        assertThat(session.getName(), is("test-pof"));
        assertThat(session.getSerializerFormat(), is("pof"));
        assertThat(session.getChannel(), is(notNullValue()));
        assertThat(session.getChannel().authority(), is("foo.com:1234"));
        session.getChannel().shutdownNow();
        }
    }
