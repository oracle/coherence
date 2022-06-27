/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.grpc.CredentialsHelper;
import com.tangosol.coherence.config.Config;

import com.tangosol.internal.tracing.TracingHelper;

import com.tangosol.io.Serializer;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;

import io.grpc.Channel;
import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannelBuilder;

import java.util.Optional;

/**
 * A {@link SessionConfiguration} that configures a gRPC client session.
 *
 * @author Jonathan Knight  2020.11.04
 * @since 20.12
 */
public interface GrpcSessionConfiguration
        extends SessionConfiguration
    {
    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link GrpcSessionConfiguration} builder.
     * <p>
     * Passing in a {@code null} channel will create a session using a default channel configuration
     * of {@code localhost:1408}. This can be overridden using the {@link #PROP_HOST}
     * and/or the {@link #PROP_PORT} system properties. which will create a channel using
     * the {@link ManagedChannelBuilder#forAddress(String, int)} method. Alternatively the
     * {@link #PROP_TARGET} system property can be set to create the channel using the
     * {@link ManagedChannelBuilder#forTarget(String)} method.
     *
     * @param channel  the {@link Channel} to use
     *
     * @return  a {@link GrpcSessionConfiguration} builder
     */
    static Builder builder(Channel channel)
        {
        return new Builder(channel, null, null);
        }

    /**
     * Create a {@link GrpcSessionConfiguration} builder for the default gRPC session.
     * <p>
     * If {@code coherence.grpc.channels.default.target} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forTarget(String)} creating
     * a plain text channel.
     * <p>
     * If {@code coherence.grpc.channels.default.host} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forAddress(String, int)} creating
     * a plain text channel.
     * The port will be taken from the {@code coherence.grpc.channels.default.port}
     * property or default to {@code 1408}.
     * <p>
     * The {@link Builder#build()} method will throw an {@link IllegalStateException} if no
     * channel properties exist for the channel name
     *
     * @return  a {@link GrpcSessionConfiguration} builder
     */
    static Builder builder()
        {
        return new Builder(null, ChannelProvider.DEFAULT_CHANNEL_NAME, null);
        }

    /**
     * Create a {@link GrpcSessionConfiguration} builder.
     * <p>
     * The {@code sChannelName} parameter will be used to lookup channel configuration
     * properties.
     * If {@code coherence.grpc.channels.[sChanneName].target} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forTarget(String)} creating
     * a plain text channel.
     * <p>
     * If {@code coherence.grpc.channels.[sChanneName].host} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forAddress(String, int)} creating
     * a plain text channel.
     * The port will be taken from the {@code coherence.grpc.channels.[sChanneName].port}
     * property or default to {@code 1408}.
     * <p>
     * The {@link Builder#build()} method will throw an {@link IllegalStateException} if no
     * channel properties exist for the channel name
     *
     * @param sChannelName  the name of the channel to use to lookup channel properties
     *
     * @return  a {@link GrpcSessionConfiguration} builder
     */
    static Builder builder(String sChannelName)
        {
        return new Builder(null, sChannelName, null);
        }

    /**
     * Create a {@link GrpcSessionConfiguration} builder.
     * <p>
     * The {@code channelName} parameter will be used to lookup channel configuration
     * properties.
     * If {@code coherence.grpc.channels.[sChannelName].target} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forTarget(String)} creating
     * a plain text channel.
     * <p>
     * If {@code coherence.grpc.channels.[sChanneName].host} is set this value will be used
     * to create a channel using {@link ManagedChannelBuilder#forAddress(String, int)} creating
     * a plain text channel.
     * The port will be taken from the {@code coherence.grpc.channels.[sChanneName].port}
     * property or default to {@code 1408}.
     * <p>
     * If no properties exist for the {@code channelName} the  {@code sFallbackName} parameter will be used
     * to lookup channel configuration properties.
     * <p>
     * The {@link Builder#build()} method will throw an {@link IllegalStateException} if no
     * channel properties exist for the either the channel name or fallback name.
     *
     * @param channelName    the name of the channel to use to lookup channel properties
     * @param sFallbackName  the name of the channel to use to lookup channel configuration if
     *                       no properties exist for the primary {@code channelName}
     *
     * @return  a {@link GrpcSessionConfiguration} builder
     */
    static Builder builder(String channelName, String sFallbackName)
        {
        return new Builder(null, channelName, sFallbackName);
        }

    // ----- GrpcSessionConfiguration methods -------------------------------

    @Override
    default String getScopeName()
        {
        return Coherence.DEFAULT_SCOPE;
        }

    /**
     * Returns the gRPC {@link Channel} to use.
     *
     * @return  the gRPC {@link Channel} to use
     */
    Channel getChannel();

    /**
     * Returns the {@link Serializer} to use to serialize gRPC
     * message payloads.
     * <p>
     * If an empty {@link Optional} is returned the serializer will
     * be the default Coherence serializer, either POF if it has
     * been enabled with the {@code coherence.pof.enabled} system
     * property, otherwise Java.
     *
     * @return the {@link Serializer} to use
     */
    default Optional<Serializer> getSerializer()
        {
        return Optional.empty();
        }

    /**
     * Returns the name of the serialization format to use to serialize gRPC
     * message payloads.
     * <p>
     * If an empty {@link Optional} is returned the serializer format will
     * be taken from the default Coherence serializer, either "pof" if it has
     * been enabled with the {@code coherence.pof.enabled} system
     * property, otherwise "java".
     *
     * @return the name of the serialization format
     */
    default Optional<String> getFormat()
        {
        return getSerializer().map(Serializer::getName);
        }

    /**
     * Returns {@code true} if client gRPC tracing should be enabled.
     *
     * @return {@code true} if client gRPC tracing should be enabled
     */
    default boolean enableTracing()
        {
        return false;
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds {@link GrpcSessionConfiguration} instances.
     */
    class Builder
        {
        /**
         * Create a builder.
         *
         * @param channel        the {@link Channel} to use.
         * @param sChannelName   the name used to lookup channel configuration if a specific channel is not set
         * @param sFallbackName  the name used to lookup channel configuration if the primary name is not configured
         */
        private Builder(Channel channel, String sChannelName, String sFallbackName)
            {
            f_channel              = channel;
            f_sChannelName         = sChannelName;
            f_sFallbackChannelName = sFallbackName;
            }

        /**
         * Set the session name.
         *
         * @param sName  the session name
         *
         * @return  this {@link Builder}
         */
        public Builder named(String sName)
            {
            m_sName = sName;
            return this;
            }

        /**
         * Set the session creation priority.
         *
         * @param nPriority  the session creation priority
         *
         * @return  this {@link Builder}
         * @see SessionConfiguration#getPriority()
         * @see #DEFAULT_PRIORITY
         */
        public Builder withPriority(int nPriority)
            {
            m_nPriority = nPriority;
            return this;
            }

        /**
         * Set the scope name.
         *
         * @param sScopeName  the scope name
         *
         * @return  this {@link Builder}
         */
        public Builder withScopeName(String sScopeName)
            {
            m_sScope = sScopeName;
            return this;
            }

        /**
         * Set the serializer.
         *
         * @param serializer the serializer
         *
         * @return  this {@link Builder}
         */
        public Builder withSerializer(Serializer serializer)
            {
            String sFormat = serializer == null ? null : serializer.getName();
            return withSerializer(serializer, sFormat);
            }

        /**
         * Set the serializer and format.
         *
         * @param serializer the serializer
         * @param sFormat    the serializer format
         *
         * @return  this {@link Builder}
         */
        public Builder withSerializer(Serializer serializer, String sFormat)
            {
            m_serializer = serializer;
            m_sFormat    = sFormat;
            return this;
            }

        /**
         * Set the serializer format.
         *
         * @param sFormat  the serializer format
         *
         * @return  this {@link Builder}
         */
        public Builder withSerializerFormat(String sFormat)
            {
            m_serializer = null;
            m_sFormat    = sFormat;
            return this;
            }

        /**
         * Enable or disable tracing.
         *
         * @param fEnabled  {@code true} to enable tracing
         *
         * @return  this {@link Builder}
         */
        public Builder withTracing(boolean fEnabled)
            {
            m_fTracing = fEnabled;
            return this;
            }

        /**
         * Build a {@link GrpcSessionConfiguration}.
         *
         * @return  a {@link GrpcSessionConfiguration}
         */
        public GrpcSessionConfiguration build()
            {
            String sName = m_sName == null || m_sName.trim().isEmpty()
                    ? Coherence.DEFAULT_NAME
                    : m_sName;

            Channel channel;
            if (f_channel == null)
                {
                if (f_sChannelName == null && f_sFallbackChannelName == null)
                    {
                    Logger.config("Session configuration for gRPC session " + sName
                        + " has no channel or channel name set, falling back to the default configuration.");
                    channel = createChannel(ChannelProvider.DEFAULT_CHANNEL_NAME, DEFAULT_HOST);
                    }
                else
                    {
                    channel = createChannel(f_sChannelName, null);
                    Logger.config("Session configuration for gRPC session " + sName
                        + " cannot find configuration properties for name " + f_sChannelName 
                        + ", falling back to channel name " + f_sFallbackChannelName);
                    if (channel == null && f_sFallbackChannelName != null)
                        {
                        channel = createChannel(f_sFallbackChannelName, null);
                        }
                    }
                }
            else
                {
                channel = f_channel;
                }

            if (channel == null)
                {
                throw new IllegalStateException("Could not configure a channel for this session configuration," +
                                    "channelName=" + f_sChannelName + " fallbackName=" + f_sFallbackChannelName);
                }

            String sScope = m_sScope == null || m_sScope.trim().isEmpty()
                    ? Coherence.DEFAULT_SCOPE
                    : m_sScope;

            String sFormat = m_sFormat;
            if (sFormat == null || sFormat.isBlank())
                {
                sFormat = getProperty(PROP_SERIALIZER_FORMAT, sName);
                }

            return new DefaultConfiguration(sName, sScope, m_nPriority, channel, m_serializer,
                    sFormat, m_fTracing, true);
            }

        /**
         * Return a {@link Channel} either from a {@link ChannelProvider} or attempt
         * to create a {@link Channel} using System properties for the channel's target,
         * host or port values.
         *
         * @param sName         the channel name
         * @param sDefaultHost  an optional value to use for host name
         *
         * @return  A plain text {@link Channel} created from property values
         */
        private Channel createChannel(String sName, String sDefaultHost)
            {
            Optional<Channel> optional = findChannel(sName);
            if (optional.isPresent())
                {
                return optional.get();
                }

            ChannelCredentials       credentials = CredentialsHelper.createChannelCredentials(sName);
            String                   target      = getProperty(PROP_TARGET, sName);
            ManagedChannelBuilder<?> builder;

            if (target == null || target.trim().isEmpty())
                {
                if (ChannelProvider.DEFAULT_CHANNEL_NAME.equals(sName) && sDefaultHost == null)
                    {
                    sDefaultHost = DEFAULT_HOST;
                    }
                String sHost = getProperty(PROP_HOST, sName, sDefaultHost);
                int    nPort = Config.getInteger(String.format(PROP_PORT, sName), 1408);

                if (sHost == null)
                    {
                    return null;
                    }

                builder = Grpc.newChannelBuilderForAddress(sHost, nPort, credentials);

                String sAuthority = getProperty(PROP_TLS_AUTHORITY, sName);
                if (sAuthority != null)
                    {
                    builder.overrideAuthority(sAuthority);
                    }
                }
            else
                {
                builder = Grpc.newChannelBuilder(target, credentials);
                }
            return builder.build();
            }

        /**
         * Find the first {@link ChannelProvider} matching one of
         * the specified names.
         *
         * @param asName  the names of the providers to find
         *
         * @return the {@link ChannelProvider} instance or an empty
         *         {@link Optional} if no provider matches any of the names
         */
        private Optional<Channel> findChannel(String... asName)
            {
            for (String sName : asName)
                {
                return ChannelProviders.INSTANCE.findChannel(sName);
                }
            return Optional.empty();
            }

        private String getProperty(String sProperty, String sName)
            {
            if (sName == null || sName.isBlank())
                {
                sName = ChannelProvider.DEFAULT_CHANNEL_NAME;
                }
            return Config.getProperty(String.format(sProperty, sName));
            }

        private String getProperty(String sProperty, String sName, String sDefault)
            {
            if (sName == null || sName.isBlank())
                {
                sName = ChannelProvider.DEFAULT_CHANNEL_NAME;
                }
            return Config.getProperty(String.format(sProperty, sName), sDefault);
            }

        /**
         * The {@link Channel} to use for the session.
         */
        private final Channel f_channel;

        /**
         * The name of the channel to use.
         */
        private final String f_sChannelName;

        /**
         * The name of the channel to use if there is no primary channel name configured.
         */
        private final String f_sFallbackChannelName;

        /**
         * The optional name of the session.
         */
        private String m_sName;

        /**
         * The session creation priority.
         */
        private int m_nPriority = GrpcSessionConfiguration.DEFAULT_PRIORITY;

        /**
         * The serializer for the session.
         */
        private Serializer m_serializer;

        /**
         * The serializer format for the session.
         */
        private String m_sFormat;

        /**
         * The scope for the session.
         */
        private String m_sScope;

        /**
         * Enable or disable tracing for the session.
         */
        private boolean m_fTracing = TracingHelper.isEnabled();
        }

    // ----- inner class: DefaultConfiguration ------------------------------

    class DefaultConfiguration
            implements GrpcSessionConfiguration
        {
        /**
         * Private constructor, use the builder to create instances.
         *
         * @param sName      the name of the session
         * @param sScope     the scope name of the session
         * @param nPriority  the session creation priority
         * @param channel    the {@link Channel} to use
         * @param serializer the serializer to use to serialize message payloads
         * @param sFormat    the name of the serializer format
         * @param fTracing   {@code true} to enable distributed tracing
         */
        private DefaultConfiguration(String     sName,
                                     String     sScope,
                                     int        nPriority,
                                     Channel    channel,
                                     Serializer serializer,
                                     String     sFormat,
                                     boolean    fTracing,
                                     boolean    fEnabled)
            {
            f_sName      = sName;
            f_sScopeName = sScope;
            f_nPriority  = nPriority;
            f_channel    = channel;
            f_serializer = serializer;
            f_sFormat    = sFormat;
            f_fTracing   = fTracing;
            f_fEnabled   = fEnabled;
            }

        // ----- GrpcSessionConfiguration methods -------------------------------

        @Override
        public boolean isEnabled()
            {
            return f_fEnabled;
            }

        /**
         * Returns the gRPC {@link Channel} to use.
         *
         * @return  the gRPC {@link Channel} to use
         */
        @Override
        public Channel getChannel()
            {
            return f_channel;
            }

        /**
         * Returns the {@link Serializer} to use to serialize gRPC
         * message payloads.
         *
         * @return the {@link Serializer} to use
         */
        @Override
        public Optional<Serializer> getSerializer()
            {
            return Optional.ofNullable(f_serializer);
            }

        /**
         * Returns the name of the serialization format to use to serialize gRPC
         * message payloads.
         *
         * @return the name of the serialization format
         */
        @Override
        public Optional<String> getFormat()
            {
            return Optional.ofNullable(f_sFormat);
            }

        /**
         * Returns {@code true} if client gRPC tracing should be enabled.
         *
         * @return {@code true} if client gRPC tracing should be enabled
         */
        @Override
        public boolean enableTracing()
            {
            return f_fTracing;
            }

        // ----- SessionConfiguration methods -----------------------------------

        @Override
        public String getName()
            {
            return f_sName;
            }

        /**
         * Return the scope name of the {@link Session}.
         * <p>
         * If not specifically set, the name will default to the
         * {@link com.tangosol.net.Coherence#DEFAULT_SCOPE} value.
         *
         * @return the scope name of the {@link Session}
         */
        @Override
        public String getScopeName()
            {
            return f_sScopeName;
            }

        @Override
        public int getPriority()
            {
            return f_nPriority;
            }

        // ----- data members -----------------------------------------------

        /**
         * A flag to enable or disable this configuration.
         */
        private final boolean f_fEnabled;

        /**
         * The name of the gRPC session.
         */
        private final String f_sName;

        /**
         * The scope name of the gRPC session.
         */
        private final String f_sScopeName;

        /**
         * The gRPC {@link Channel} to use.
         */
        private final Channel f_channel;

        /**
         * The session creation priority.
         */
        private final int f_nPriority;

        /**
         * The serializer for the session.
         */
        private final Serializer f_serializer;

        /**
         * The serializer format for the session.
         */
        private final String f_sFormat;

        /**
         * {@code true} to enable gRPC client tracing.
         */
        private final boolean f_fTracing;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The default priority for gRPC Sessions.
     * This is <b>lower</b> than the default so that by default gRPC client
     * sessions start after normal sessions as there is more likely to be a
     * dependency between gRPC and CCF sessions.
     */
    int DEFAULT_PRIORITY = SessionConfiguration.DEFAULT_PRIORITY - 1;

    /**
     * The default host name to use for the default channel.
     */
    String DEFAULT_HOST = "localhost";

    /**
     * The system property to use to override the default host name to use
     * for the {@link Channel} if no channel is specified for the configuration.
     */
    String PROP_HOST = "coherence.grpc.channels.%s.host";

    /**
     * The system property to use to override the default port to use for the
     * {@link Channel} if no channel is specified for the configuration.
     */
    String PROP_PORT = "coherence.grpc.channels.%s.port";

    /**
     * The system property that sets the value to use to CA file.
     */
    String PROP_TLS_AUTHORITY = "coherence.grpc.channels.%s.tls.authority";

    /**
     * The system property to use to override the default target to use for the
     * {@link Channel} if no channel is specified for the configuration.
     * <p>
     * If this property is specified the {@link ManagedChannelBuilder#forTarget(String)}
     * method will be used to create the channel builder.
     */
    String PROP_TARGET = "coherence.grpc.channels.%s.target";

    /**
     * The system property that sets the value to use for the serializer format.
     */
    String PROP_SERIALIZER_FORMAT = "coherence.grpc.session.%s.serializer";

    /**
     * The system property that sets whether a session is enabled.
     */
    String PROP_SESSION_ENABLED = "coherence.grpc.session.%s.enabled";

    /**
     * The system property that sets whether the default gRPC session is enabled.
     */
    String PROP_DEFAULT_SESSION_ENABLED = String.format(PROP_SESSION_ENABLED, "default");
    }
