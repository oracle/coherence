/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package helidon.client;

import com.oracle.coherence.cdi.ConfigUri;
import com.oracle.coherence.cdi.Scope;
import com.oracle.coherence.cdi.SessionInitializer;
import com.oracle.coherence.client.GrpcSessionConfiguration;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.WrapperSessionConfiguration;
import io.grpc.Channel;
import io.helidon.microprofile.grpc.client.GrpcChannel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.util.Optional;

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
            extends WrapperSessionConfiguration
        {
        public ClientTestSession()
            {
            super(SessionConfiguration.builder()
                          .named(CLIENT_TEST)
                          .withScopeName(CLIENT_TEST)
                          .withPriority(100)
                          .withMode(Coherence.Mode.GrpcFixed)
                          .withParameter("coherence.profile", "thin")
                          .withParameter("coherence.grpc.remote.scope", TEST_SCOPE)
                          .withParameter("coherence.grpc.port", "1408")
                          .build());
            }
        }

    @ApplicationScoped
    public static class ClientSession
            extends WrapperSessionConfiguration
        {
        public ClientSession()
            {
            super(SessionConfiguration.builder()
                          .named(CLIENT_DEFAULT)
                          .withScopeName(CLIENT_DEFAULT)
                          .withPriority(100)
                          .withMode(Coherence.Mode.GrpcFixed)
                          .withParameter("coherence.profile", "thin")
                          .withParameter("coherence.grpc.port", "1408")
                          .build());
            }
        }
    }
