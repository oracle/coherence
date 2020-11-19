/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.client;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionConfiguration;

import com.tangosol.io.Serializer;

import com.tangosol.net.Session;

import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

import io.grpc.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link SessionConfiguration} that configures a gRPC client session.
 *
 * @author Jonathan Knight  2020.11.04
 * @since 20.12
 */
public class GrpcSessionConfiguration
        implements SessionConfiguration
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor, use the builder to create instances.
     *
     * @param sName      the name of the session
     * @param sScope     the scope name of the session
     * @param nPriority  the session creation priority
     * @param listOpts   the options to use to create a session
     */
    private GrpcSessionConfiguration(String sName, String sScope, int nPriority, List<Session.Option> listOpts)
        {
        f_sName      = sName;
        f_sScopeName = sScope;
        f_nPriority  = nPriority;
        f_listOption = listOpts;
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link GrpcSessionConfiguration} builder.
     *
     * @param channel  the {@link Channel} to use
     *
     * @return  a {@link GrpcSessionConfiguration} builder
     */
    public static Builder builder(Channel channel)
        {
        Objects.requireNonNull(channel);
        return new Builder(channel);
        }

    /**
     * Create a {@link GrpcSessionConfiguration}.
     *
     * @param channel  the {@link Channel} to use
     *
     * @return  a {@link GrpcSessionConfiguration}
     */
    public static GrpcSessionConfiguration create(Channel channel)
        {
        return builder(channel).build();
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

    @Override
    public Session.Option[] getOptions()
        {
        return f_listOption.toArray(new Session.Option[0]);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds {@link GrpcSessionConfiguration} instances.
     */
    public static class Builder
        {
        /**
         * Create a builder.
         *
         * @param channel  the {@link Channel} to use.
         */
        Builder(Channel channel)
            {
            m_channel = channel;
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
            List<Session.Option> listOps = new ArrayList<>();

            listOps.add(GrpcSessions.channel(m_channel));

            String sName = m_sName == null || m_sName.trim().isEmpty()
                    ? Coherence.DEFAULT_NAME
                    : m_sName;

            listOps.add(WithName.of(sName));

            String sScope = m_sScope == null || m_sScope.trim().isEmpty()
                    ? Coherence.DEFAULT_SCOPE
                    : m_sScope;

            listOps.add(WithScopeName.of(sScope));

            if (m_serializer != null && m_sFormat != null)
                {
                listOps.add(GrpcSessions.serializer(m_serializer, m_sFormat));
                }
            else if (m_serializer != null)
                {
                listOps.add(GrpcSessions.serializer(m_serializer));
                }
            else if (m_sFormat != null)
                {
                listOps.add(GrpcSessions.serializerFormat(m_sFormat));
                }

            if (m_fTracing)
                {
                listOps.add(GrpcSessions.tracing(m_fTracing));
                }

            return new GrpcSessionConfiguration(sName, sScope, m_nPriority, listOps);
            }

        /**
         * The {@link Channel} to use for the session.
         */
        private final Channel m_channel;

        /**
         * The optional name of the session.
         */
        private String m_sName;

        /**
         * The session creation priority.
         */
        private int m_nPriority;

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
        private boolean m_fTracing;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The name of the gRPC session.
     */
    private final String f_sName;

    /**
     * The scope name of the gRPC session.
     */
    private final String f_sScopeName;

    /**
     * The session creation priority.
     */
    private final int f_nPriority;

    /**
     * The options used to create the session.
     */
    private final List<Session.Option> f_listOption;
    }
