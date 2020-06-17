/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.client;

import com.oracle.coherence.cdi.Scope;
import io.helidon.config.Config;
import io.helidon.config.MapConfigSource;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2020.06.16
 */
public class GrpcRemoteSessionTest
    {
    @Test
    public void shouldBuildWithDefaultScope() throws Exception
        {
        GrpcRemoteSession session = GrpcRemoteSession.builder().build();
        assertThat(session.getScope(), is(Scope.DEFAULT));
        }

    @Test
    public void shouldBuildWithSpecifiedScope() throws Exception
        {
        GrpcRemoteSession session = GrpcRemoteSession.builder().scope("foo").build();
        assertThat(session.getScope(), is("foo"));
        }

    @Test
    public void shouldBuildWithConfiguredScope() throws Exception
        {
        MapConfigSource   source  = MapConfigSource.create(Collections.singletonMap("coherence.sessions.test.scope", "foo"));
        Config            config  = Config.builder(source).build();
        GrpcRemoteSession session = GrpcRemoteSession.builder(config).name("test").build();
        assertThat(session.getScope(), is("foo"));
        }

    @Test
    public void shouldBuildWithSpecifiedScopeOverridingConfiguredScope() throws Exception
        {
        MapConfigSource   source  = MapConfigSource.create(Collections.singletonMap("coherence.sessions.test.scope", "foo"));
        Config            config  = Config.builder(source).build();
        GrpcRemoteSession session = GrpcRemoteSession.builder(config).name("test").scope("bar").build();
        assertThat(session.getScope(), is("bar"));
        }
    }
