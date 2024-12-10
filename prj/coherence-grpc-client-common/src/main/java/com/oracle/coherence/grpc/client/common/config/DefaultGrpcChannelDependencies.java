/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.grpc.client.common.config;

import com.oracle.coherence.grpc.client.common.ChannelProvider;
import com.oracle.coherence.grpc.client.common.GrpcChannelConfigurer;
import com.tangosol.coherence.config.SimpleParameterList;
import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.config.builder.WrapperSocketAddressProviderBuilder;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.config.annotation.Injectable;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.net.AddressProvider;
import com.tangosol.net.SocketAddressProvider;
import com.tangosol.net.grpc.GrpcChannelDependencies;

import java.util.Optional;

/**
 * A default implementation of {@link GrpcChannelDependencies}.
 *
 * @author Jonathan Knight  2022.08.25
 * @since 22.06.2
 */
public class DefaultGrpcChannelDependencies
        implements GrpcChannelDependencies
    {
    /**
     * Returns the optional {@link ChannelProvider} to use to
     * create a {@link io.grpc.Channel}.
     *
     * @return the optional {@link ChannelProvider} to use to
     *         create a {@link io.grpc.Channel}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Optional<ChannelProvider> getChannelProvider()
        {
        if (m_bldrChannelProvider != null)
            {
            SystemPropertyParameterResolver resolver   = new SystemPropertyParameterResolver();
            ChannelProvider                 provider   = m_bldrChannelProvider.realize(resolver, null, null);
            return Optional.ofNullable(provider);
            }
        return Optional.empty();
        }

    /**
     * Set the {@link ParameterizedBuilder} that builds a {@link ChannelProvider}.
     *
     * @param bldr  the {@link ParameterizedBuilder} that builds a {@link ChannelProvider}
     */
    @Injectable("instance")
    public void setChannelProvider(ParameterizedBuilder<ChannelProvider> bldr)
        {
        m_bldrChannelProvider = bldr;
        }

    /**
     * Set the value to be used when creating a {@link io.grpc.ManagedChannelBuilder#forTarget(String)
     * ManagedChannelBuilder for a target URI}.
     *
     * @param sTarget  the value to be used when calling {@link io.grpc.ManagedChannelBuilder#forTarget(String)}
     */
    @Injectable
    public void setTarget(String sTarget)
        {
        m_sTarget = sTarget;
        }

    @Override
    public String getTarget()
        {
        return m_sTarget;
        }

    /**
     * Returns whether the configured {@link AddressProvider} is for a NameService lookup.
     *
     * @return  {@code true} if the configured {@link AddressProvider} is for a
     *          NameService lookup, or {@code false} if the addresses are for a
     *          gRPC proxy.
     */
    public boolean isNameServiceAddressProvider()
        {
        return m_fNameServiceAddressProvider || m_bldrAddressProviderRemote == null;
        }

    /**
     * Sets the remote AddressProvider is for connections to a NameService.
     * (set to <code>null</code> to disable)
     * <p>
     * After calling this method, {@link #isNameServiceAddressProvider()} will
     * return <code>true</code> (assuming a non-null value was provided).
     *
     * @param bldr  the {@link AddressProvider} builder for the NameService
     */
    @Injectable("name-service-addresses")
    public void setNameServiceAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        setRemoteAddressProviderBuilder(bldr);
        m_fNameServiceAddressProvider = bldr != null;
        }

    /**
     * Set the remote AddressProvider builder.
     * <p>
     * After calling this method, {@link #isNameServiceAddressProvider()} will return <code>false</code>.
     *
     * @param bldr  the remote AddressProvider builder
     */
    @Injectable("remote-addresses")
    public void setRemoteAddressProviderBuilder(AddressProviderBuilder bldr)
        {
        if (bldr == null)
            {
            m_bldrAddressProviderRemote = null;
            }
        else
            {
            m_bldrAddressProviderRemote = new WrapperSocketAddressProviderBuilder(bldr);
            }
        m_fNameServiceAddressProvider = false;
        }

    @Override
    public ParameterizedBuilder<SocketAddressProvider> getRemoteAddressProviderBuilder()
        {
        return m_bldrAddressProviderRemote;
        }

    /**
     * Set the {@link SocketProviderBuilder} to use to configure an SSL context
     * for the gRPC channel. If the socket provider is not an SSL provider it
     * will be ignored.
     *
     * @param builder  the {@link SocketProviderBuilder} to use to configure an SSL context
     *                 for the gRPC channel
     */
    @Injectable
    public void setSocketProvider(SocketProviderBuilder builder)
        {
        m_builderSocketProvider = builder;
        }

    @Override
    public SocketProviderBuilder getSocketProviderBuilder()
        {
        return m_builderSocketProvider;
        }

    /**
     * Set the value to use in {@link io.grpc.ManagedChannelBuilder#overrideAuthority(String)}.
     *
     * @param sAuthority  the value to use in {@link io.grpc.ManagedChannelBuilder#overrideAuthority(String)}
     */
    @Injectable("override-authority")
    public void setOverrideAuthority(String sAuthority)
        {
        m_sOverrideAuthority = sAuthority;
        }

    @Override
    public Optional<String> getAuthorityOverride()
        {
        return Optional.ofNullable(m_sOverrideAuthority);
        }

    /**
     * Set the value to use in {@link io.grpc.ManagedChannelBuilder#defaultLoadBalancingPolicy(String)}.
     * <p>
     * This value must be a valid load balancer registered with the gRPC framework.
     *
     * @param sPolicy  the value to use in {@link io.grpc.ManagedChannelBuilder#defaultLoadBalancingPolicy(String)}
     */
    @Injectable("load-balancer-policy")
    public void setLoadBalancerPolicy(String sPolicy)
        {
        m_sLoadBalancerPolicy = sPolicy;
        }

    @Override
    public String getDefaultLoadBalancingPolicy()
        {
        String sPolicy = m_sLoadBalancerPolicy;
        if (sPolicy == null || sPolicy.isBlank())
            {
            sPolicy = m_sLoadBalancerPolicy = GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_POLICY;
            }
        return sPolicy;
        }

    @Injectable("load-balancer-timeout")
    public void setLoadBalancerTimeout(Expression<Seconds> expr)
        {
        m_expLoadBalancerTimeout = expr == null
                ? GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_TIMEOUT
                : expr;
        }

    @Override
    public long getLoadBalancerTimeout(ParameterResolver resolver)
        {
        Seconds seconds = m_expLoadBalancerTimeout.evaluate(resolver);
        if (seconds == null)
            {
            seconds = GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_TIMEOUT.evaluate(resolver);
            }
        return seconds.get();
        }

    /**
     * Set the optional {@link ParameterizedBuilder} that will build a {@link GrpcChannelConfigurer}
     * that can apply further configuration to a {@link io.grpc.ManagedChannelBuilder}.
     *
     * @param bldr  the optional {@link ParameterizedBuilder} that will build a {@link GrpcChannelConfigurer}
     *              that can apply further configuration to a {@link io.grpc.ManagedChannelBuilder}
     */
    @Injectable("configurer")
    public void setConfigurer(ParameterizedBuilder<GrpcChannelConfigurer> bldr)
        {
        m_configurerBuilder = bldr;
        }

    @Override
    public Optional<GrpcChannelConfigurer> getConfigurer()
        {
        if (m_configurerBuilder != null)
            {
            SystemPropertyParameterResolver resolver   = new SystemPropertyParameterResolver();
            SimpleParameterList             parameters = new SimpleParameterList();
            GrpcChannelConfigurer           configurer = m_configurerBuilder.realize(resolver, null, parameters);
            return Optional.ofNullable(configurer);
            }
        return Optional.empty();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The optional target to use to construct a {@link io.grpc.ManagedChannelBuilder}
     * using a call to {@link io.grpc.ManagedChannelBuilder#forTarget(String)}.
     */
    private String m_sTarget;

    /**
     * An optional {@link SocketProviderBuilder} to create an SSL context for the channel.
     */
    private SocketProviderBuilder m_builderSocketProvider;

    /**
     * The remote SocketAddressProvider builder to build the list of server addresses.
     */
    private ParameterizedBuilder<SocketAddressProvider> m_bldrAddressProviderRemote;

    /**
     * Whether the remote AddressProvider is for connections to a NameService.
     */
    private boolean m_fNameServiceAddressProvider = false;

    /**
     * The value to pass to the {@link io.grpc.ManagedChannelBuilder#overrideAuthority(String)} method.
     */
    private String m_sOverrideAuthority;

    /**
     * The value to pass to the {@link io.grpc.ManagedChannelBuilder#defaultLoadBalancingPolicy(String)}} method.
     */
    private String m_sLoadBalancerPolicy;

    /**
     * The timeout to apply to load balancer address resolution.
     */
    private Expression<Seconds> m_expLoadBalancerTimeout = GrpcChannelDependencies.DEFAULT_LOAD_BALANCER_TIMEOUT;

    /**
     * A {@link ParameterizedBuilder} that can build a {@link GrpcChannelConfigurer}.
     */
    private ParameterizedBuilder<GrpcChannelConfigurer> m_configurerBuilder;

    /**
     * A {@link ParameterizedBuilder} that can build a {@link ChannelProvider}.
     */
    private ParameterizedBuilder<ChannelProvider> m_bldrChannelProvider;
    }
