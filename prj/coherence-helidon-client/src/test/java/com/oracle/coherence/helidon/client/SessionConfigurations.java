/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.helidon.client;

import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.net.SessionConfiguration;
import io.grpc.Channel;
import io.helidon.microprofile.grpc.client.GrpcChannel;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * A set of {@link SessionInitializer} CDI beans
 * to configure the test sessions.
 *
 * @author Jonathan Knight  2020.11.06
 */
public class SessionConfigurations
    {
    public static final String SERVER_TEST = "Test";

    public static final String CLIENT_DEFAULT = "Client";

    public static final String CLIENT_TEST = "ClientTest";

    public static final String TEST_SCOPE = "test";

    @ApplicationScoped
    @Named(SessionConfigurations.SERVER_TEST)
    @Scope(SessionConfigurations.TEST_SCOPE)
    @ConfigUri("coherence-config-two.xml")
    public static class TestSession
            implements SessionInitializer
        {
        }

    @ApplicationScoped
    public static class ClientTestSession
            implements SessionConfiguration.Provider
        {
        @Inject
        @GrpcChannel(name = "test")
        public ClientTestSession(Channel channel)
            {
            f_channel = channel;
            }

        @Override
        public SessionConfiguration getConfiguration()
            {
            return GrpcSessionConfiguration.builder(f_channel)
                    .named(CLIENT_TEST)
                    .withScopeName(TEST_SCOPE)
                    .withPriority(10)
                    .build();
            }

        private final Channel f_channel;
        }

    @ApplicationScoped
    public static class ClientSession
            implements SessionConfiguration.Provider
        {
        @Inject
        @GrpcChannel(name = "helidon")
        public ClientSession(Channel channel)
            {
            f_channel = channel;
            }

        @Override
        public SessionConfiguration getConfiguration()
            {
            return GrpcSessionConfiguration.builder(f_channel)
                    .named(CLIENT_DEFAULT)
                    .withPriority(10)
                    .build();
            }

        private final Channel f_channel;
        }
    }
