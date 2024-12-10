/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.LiteralExpression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.SocketAddressProvider;

import java.util.Optional;

/**
 * Dependencies for a gRPC channel.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public interface GrpcChannelDependencies
    {
    /**
     * Return whether each remote AddressProvider address points to a
     * NameService which can be used to look up the remote address of
     * the gRPC proxy.
     *
     * @return whether each remote AddressProvider address points to
     *         a NameService which can be used to look up the remote
     *         address of the gRPC proxy
     */
    boolean isNameServiceAddressProvider();

    /**
     * Return the AddressProvider builder used by the TcpInitiator to obtain the address or
     * addresses of the remote TcpAcceptor(s) that it will connect to.
     *
     * @return the remote AddressProvider
     */
    ParameterizedBuilder<SocketAddressProvider> getRemoteAddressProviderBuilder();

    /**
     * Returns the optional value to be used when creating a
     * ManagedChannelBuilder for a target URI.
     *
     * @return  the value to be used when creating a ManagedChannelBuilder for
     *          a specific target URI
     */
    String getTarget();

    /**
     * Return the SocketProviderBuilder used by the TcpAcceptor to open ServerSocketChannels.
     *
     * @return the socket provider builder
     */
    SocketProviderBuilder getSocketProviderBuilder();

    /**
     * Returns the optional value to use to set the channel builder's overrideAuthority setting.
     *
     * @return the optional value to use to set the channel builder's overrideAuthority setting
     */
    Optional<String> getAuthorityOverride();

    /**
     * Returns the value for the default load balancer policy.
     * <p>
     * This must be a valid value for a gRPC ManagedChannelBuilder
     * load balancer policy.
     * <p>
     * The default value is {@link GrpcChannelDependencies#DEFAULT_LOAD_BALANCER_POLICY}
     *
     * @return the value for the default load balancer policy
     */
    String getDefaultLoadBalancingPolicy();

    /**
     * Return the load balancer timeout in seconds.
     *
     * @return the load balancer timeout in seconds
     */
    default long getLoadBalancerTimeout()
        {
        return getLoadBalancerTimeout(new NullParameterResolver());
        }

    /**
     * Return the load balancer timeout in seconds.
     *
     * @return the load balancer timeout in seconds
     */
    long getLoadBalancerTimeout(ParameterResolver resolver);

    /**
     * Return the configurer that will apply further configuration
     * to the managed channel builder.
     *
     * @return the configurer that will apply further configuration
     *         to the managed channel builder
     */
    Optional<?> getConfigurer();

    /**
     * Return an optional ChannelProvider to use.
     *
     * @return an optional ChannelProvider to use
     */
    <M> Optional<M> getChannelProvider();

    // ----- constants ------------------------------------------------------

    /**
     * The name of the default gRPC channel.
     */
    String DEFAULT_CHANNEL_NAME = "default";

    /**
     * The System channel name.
     */
    String SYSTEM_CHANNEL_NAME = "system";

    /**
     * The system property to use to override the default host name to use
     * for a named Channel if no channel is specified for the configuration.
     */
    String PROP_HOST = "coherence.grpc.channels.%s.host";

    /**
     * The system property to use to override the default host name to use
     * for a named Channel if no channel is specified for the configuration.
     */
    String PROP_DEFAULT_CHANNEL_HOST = String.format(PROP_HOST, DEFAULT_CHANNEL_NAME);

    /**
     * The system property to use to override the default port to use for a
     * named Channel if no channel is specified for the configuration.
     */
    String PROP_PORT = "coherence.grpc.channels.%s.port";

    /**
     * The system property to use to override the default port to use for the
     * default Channel if no channel is specified for the configuration.
     */
    String PROP_DEFAULT_CHANNEL_PORT = String.format(PROP_PORT, DEFAULT_CHANNEL_NAME);

    /**
     * The system property to use to override the default target to use for the
     * Channel if no channel is specified for the configuration.
     * <p>
     * If this property is specified the ManagedChannelBuilder#forTarget(String)
     * method will be used to create the channel builder.
     */
    String PROP_TARGET = "coherence.grpc.channels.%s.target";

    /**
     * The system property that sets the value to use to CA file.
     */
    String PROP_TLS_AUTHORITY = "coherence.grpc.channels.%s.tls.authority";

    /**
     * The system property that sets the value to use for the serializer format.
     */
    String PROP_SERIALIZER_FORMAT = "coherence.grpc.session.%s.serializer";

    /**
     * The default load balancer policy to pass to a managed channel builder.
     */
    String DEFAULT_LOAD_BALANCER_POLICY = "round_robin";

    /**
     * The default gRPC load balancer address resolution timeout.
     */
    Expression<Seconds> DEFAULT_LOAD_BALANCER_TIMEOUT = new LiteralExpression<>(new Seconds(10));
    }
