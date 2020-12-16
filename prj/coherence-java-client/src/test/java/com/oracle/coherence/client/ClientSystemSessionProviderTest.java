/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.SessionProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.12.16
 */
class ClientSystemSessionProviderTest
    {
    @Test
    void shouldCreateSessionOnClient()
        {
        SessionConfiguration config = SessionConfiguration.builder()
                .named(Coherence.SYSTEM_SESSION)
                .build();

        SessionProvider.Context initialContext   = mock(SessionProvider.Context.class);
        SessionProvider.Context completedContext = mock(SessionProvider.Context.class);

        when(initialContext.getMode()).thenReturn(Coherence.Mode.Client);
        when(initialContext.createSession(any(SessionConfiguration.class))).thenReturn(completedContext);

        ClientSystemSessionProvider provider = new ClientSystemSessionProvider();
        SessionProvider.Context result = provider.createSession(config, initialContext);

        assertThat(result, is(sameInstance(completedContext)));

        ArgumentCaptor<SessionConfiguration> captor = ArgumentCaptor.forClass(SessionConfiguration.class);
        verify(initialContext).createSession(captor.capture());

        SessionConfiguration actualConfig = captor.getValue();
        assertThat(actualConfig, is(instanceOf(GrpcSessionConfiguration.class)));

        GrpcSessionConfiguration grpcConfig = (GrpcSessionConfiguration) actualConfig;
        assertThat(grpcConfig.getName(), is(Coherence.SYSTEM_SESSION));
        assertThat(grpcConfig.getScopeName(), is(Coherence.SYSTEM_SCOPE));
        }

    @Test
    void shouldNotCreateSessionOnClusterMember()
        {
        SessionConfiguration config = SessionConfiguration.builder()
                .named(Coherence.SYSTEM_SESSION)
                .build();

        SessionProvider.Context context = mock(SessionProvider.Context.class);
        when(context.getMode()).thenReturn(Coherence.Mode.ClusterMember);

        ClientSystemSessionProvider provider = new ClientSystemSessionProvider();
        SessionProvider.Context result = provider.createSession(config, context);

        assertThat(result, is(notNullValue()));
        assertThat(result.isComplete(), is(false));
        }

    @Test
    void shouldNotCreateSessionOnClientForNoneSystemConfig()
        {
        SessionConfiguration config = SessionConfiguration.builder()
                .named("Foo")
                .build();

        SessionProvider.Context context = mock(SessionProvider.Context.class);
        when(context.getMode()).thenReturn(Coherence.Mode.Client);

        ClientSystemSessionProvider provider = new ClientSystemSessionProvider();
        SessionProvider.Context result = provider.createSession(config, context);

        assertThat(result, is(notNullValue()));
        assertThat(result.isComplete(), is(false));
        }
    }
