/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.config.expression.ChainedParameterResolver;
import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.net.events.EventInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A class that can provide the configuration options for a {@link Session}.
 *
 * @author Jonathan Knight  2020.11.03
 * @since 20.12
 */
public interface SessionConfiguration
        extends Comparable<SessionConfiguration>
    {
    /**
     * Return the name of the {@link Session}.
     * <p>
     * If not specifically set the {@link Coherence#DEFAULT_NAME}
     * will be used.
     * <p>
     * A session name must be unique.
     *
     * @return the name of the {@link Session}
     */
    String getName();

    /**
     * Return the scope name of the {@link Session}.
     * <p>
     * If not specifically set, the name will default
     * to the {@link #getName()} value.
     * If the {@link #getName()} is {@link Coherence#DEFAULT_NAME}
     * no scope will be used.
     *
     * @return the scope name of the {@link Session}
     */
    String getScopeName();

    /**
     * Return the interceptors to add to the session.
     *
     * @return  the interceptors to add to the session
     */
    default Iterable<EventInterceptor<?>> getInterceptors()
        {
        return Collections.emptyList();
        }

    /**
     * Return {@code true} if this configuration is enabled and should be used
     * to configure a session.
     *
     * @return  {@code true} if this configuration is enabled
     */
    default boolean isEnabled()
        {
        return true;
        }

    /**
     * Return the optional configuration file URI for a session that
     * wraps a {@link ConfigurableCacheFactory}.
     *
     * @return the optional configuration file URI for a session that
     *         wraps a {@link ConfigurableCacheFactory}
     */
    default Optional<String> getConfigUri()
        {
        return Optional.empty();
        }

    /**
     * Return the optional {@link ClassLoader} to use for the session.
     *
     * @return the optional {@link ClassLoader} to use for the session
     */
    default Optional<ClassLoader> getClassLoader()
        {
        return Optional.empty();
        }

    /**
     * Returns the priority for this configuration.
     * <p>
     * Sessions will be created in priority order, the highest
     * priority first.
     * <p>
     * The default priority is zero (see {@link #DEFAULT_PRIORITY}).
     *
     * @return  the priority for this configuration
     */
    default int getPriority()
        {
        return DEFAULT_PRIORITY;
        }

    /**
     * Order SessionConfiguration by priority, the lowest priority
     * comes first.
     *
     * @param other  the configuration to compare to
     *
     * @return  compare this configuration with the specified
     *          configuration ordering by priority
     */
    @Override
    default int compareTo(SessionConfiguration other)
        {
        return Integer.compare(getPriority(), other.getPriority());
        }

    /**
     * Returns an optional {@link ParameterResolver} to use to resolve configuration
     * parameters when creating the session.
     *
     * @return an optional {@link ParameterResolver} to use to resolve configuration
     *         parameters
     */
    default Optional<ParameterResolver> getParameterResolver()
        {
        return Optional.empty();
        }

    /**
     * Return an optional {@link com.tangosol.net.Coherence.Mode} that the session
     * should use to set the {@code coherence.client} property.
     *
     * @return an optional {@link com.tangosol.net.Coherence.Mode} that the session
     *         should use to set the {@code coherence.client} property
     */
    default Optional<Coherence.Mode> getMode()
        {
        return Optional.empty();
        }

    /**
     * Return the {@link SessionProvider} to use to create a session.
     *
     * @return the {@link SessionProvider} to use to create a session
     */
    default Optional<SessionProvider> sessionProvider()
        {
        return Optional.empty();
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a {@link SessionConfiguration} builder.
     *
     * @return  a {@link SessionConfiguration} builder
     */
    static Builder builder()
        {
        return new Builder();
        }

    /**
     * Create a {@link SessionConfiguration} for the default {@link Session}.
     * <p>
     * The default session will wrap a {@link ConfigurableCacheFactory} that is
     * configured from the default cache configuration file.
     * <p>
     * The default session will have a name {@link Coherence#DEFAULT_NAME}
     * and the default scope name {@link Coherence#DEFAULT_SCOPE}.
     *
     * @return  a {@link SessionConfiguration} for the default {@link Session}
     */
    static SessionConfiguration defaultSession()
        {
        return create(Coherence.DEFAULT_NAME, CacheFactoryBuilder.URI_DEFAULT);
        }

    /**
     * Create a {@link SessionConfiguration} for the default {@link Session}
     * with the specified configuration file.
     * <p>
     * The default session will wrap a {@link ConfigurableCacheFactory} that is
     * configured from the default cache configuration file.
     * <p>
     * The default session will have a name {@link Coherence#DEFAULT_NAME}
     * and the default scope name {@link Coherence#DEFAULT_SCOPE}.
     *
     * @param sConfigURI  the location of the configuration file to use
     *
     * @return  a {@link SessionConfiguration} for the default {@link Session}
     */
    static SessionConfiguration create(String sConfigURI)
        {
        return create(Coherence.DEFAULT_NAME, sConfigURI);
        }

    /**
     * Create a {@link SessionConfiguration} for a {@link Session} with a
     * specific name and configuration file.
     * <p>
     * The session will have a scope name {@link Coherence#DEFAULT_SCOPE}.
     *
     * @param sName       the name of the session
     * @param sConfigURI  the location of the configuration file to use
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sConfigURI)
        {
        return builder().named(sName)
                .withConfigUri(sConfigURI)
                .build();
        }

    /**
     * Create a {@link SessionConfiguration} for a {@link Session} with a
     * specific name and configuration file.
     *
     * @param sName       the name of the session
     * @param sConfigURI  the location of the configuration file to use
     * @param sScopeName  the scope name to use for the session
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sConfigURI, String sScopeName)
        {
        return builder().named(sName)
                .withScopeName(sScopeName)
                .withConfigUri(sConfigURI)
                .build();
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to build a {@link ConfigurableCacheFactorySessionConfig}.
     */
    class Builder
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link SessionConfiguration} builder.
         */
        private Builder()
            {
            }

        // ----- Builder methods --------------------------------------------

        /**
         * Set the name for the session to be created.
         *
         * @param sName  the name for the session to be created
         *
         * @return  this {@link Builder}
         */
        public Builder named(String sName)
            {
            m_sName = sName;
            return this;
            }

        /**
         * Set the cache configuration URI for the session to be created.
         *
         * @param sURI  the cache configuration URI for the session to be created
         *
         * @return  this {@link Builder}
         */
        public Builder withConfigUri(String sURI)
            {
            m_sURI = sURI;
            return this;
            }

        /**
         * Set the {@link ClassLoader} for the session to be created.
         *
         * @param loader  the {@link ClassLoader} for the session to be created
         *
         * @return  this {@link Builder}
         */
        public Builder withClassLoader(ClassLoader loader)
            {
            m_loader = loader;
            return this;
            }

        /**
         * Set the scope name for the session to be created.
         *
         * @param sScope  the scope name for the session to be created
         *
         * @return  this {@link Builder}
         */
        public Builder withScopeName(String sScope)
            {
            m_sScope = sScope;
            return this;
            }

        /**
         * Add an {@link EventInterceptor} to add to the session
         * <p>
         * It is important that the interceptor is able to intercept
         * the event types from the session being configured. Ideally
         * use an implementation of {@link com.tangosol.net.events.EventDispatcherAwareInterceptor}
         * so that the interceptor can determine whether it should be
         * registered with a specific session event dispatcher.
         *
         * @param interceptor  an {@link EventInterceptor} to add to the session
         *
         * @return  this {@link Builder}
         */
        public Builder withInterceptor(EventInterceptor<?> interceptor)
            {
            if (interceptor != null)
                {
                f_listInterceptor.add(interceptor);
                }
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
         * Set the optional {@link ParameterResolver} to use to resolve
         * configuration parameters.
         * <p>
         * Any parameter added directly with one of the {@code addParameter}
         * methods will take precedence over the parameters in the specified
         * resolver.
         *
         * @param resolver  the optional {@link ParameterResolver} to use
         *                  to resolve configuration parameters
         *
         * @return  this {@link Builder}
         *
         * @see #withParameter(String, Object)
         * @see #withParameter(String, Expression)
         * @see #withParameter(Parameter)
         */
        public Builder withParameterResolver(ParameterResolver resolver)
            {
            m_parameterResolver = resolver;
            return this;
            }

        /**
         * Add a named parameter to use when resolving
         * configuration parameters.
         *
         * @param sName   the name of the parameter
         * @param oValue  the parameter value
         *
         * @return  this {@link Builder}
         */
        public Builder withParameter(String sName, Object oValue)
            {
            return withParameter(new Parameter(sName, oValue));
            }

        /**
         * Add a named parameter to use when resolving
         * configuration parameters.
         *
         * @param sName       the name of the parameter
         * @param expression  the parameter {@link Expression} to use to resolve the parameter value
         *
         * @return  this {@link Builder}
         */
        public Builder withParameter(String sName, Expression<?> expression)
            {
            return withParameter(new Parameter(sName, expression));
            }

        /**
         * Add a named parameter to use when resolving
         * configuration parameters.
         *
         * @param parameter   the named {@link Parameter}
         *
         * @return  this {@link Builder}
         */
        public Builder withParameter(Parameter parameter)
            {
            m_parameterList.add(parameter);
            return this;
            }

        /**
         * Set the session's {@code coherence.client} parameter to "remote".
         *
         * @return this {@link Builder}
         */
        public Builder clientRemote()
            {
            return withMode(Coherence.Mode.Client);
            }

        /**
         * Set the session's {@code coherence.client} parameter to "remote-fixed".
         *
         * @return this {@link Builder}
         */
        public Builder clientFixed()
            {
            return withMode(Coherence.Mode.ClientFixed);
            }

        /**
         * Set the session's {@code coherence.client} parameter to "direct".
         *
         * @return this {@link Builder}
         */
        public Builder clientDirect()
            {
            return withMode(Coherence.Mode.ClusterMember);
            }

        /**
         * Set the {@link Coherence.Mode} to use, which will override any mode used
         * by the {@link Coherence} instance.
         *
         * @param mode  the {@link Coherence.Mode} to use
         *
         * @return this {@link Builder}
         */
        public Builder withMode(Coherence.Mode mode)
            {
            m_mode = mode;
            return this;
            }

        /**
         * Set the {@link SessionProvider} to use.
         *
         * @param provider  the {@link SessionProvider} to use
         *
         * @return this {@link Builder}
         */
        public Builder withSessionProvider(SessionProvider provider)
            {
            m_sessionProvider = provider;
            return this;
            }

        /**
         * Build the {@link SessionConfiguration}.
         *
         * @return the {@link SessionConfiguration}
         */
        public SessionConfiguration build()
            {
            return new ConfigurableCacheFactorySessionConfig(this);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the {@link Session} to create from the configuration.
         */
        private String m_sName;

        /**
         * The URI of the configuration file to use to configure the underlying
         * {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}.
         */
        private String m_sURI;

        /**
         * The {@link ClassLoader} to use for the {@link Session} and the underlying
         * {@link com.tangosol.net.ConfigurableCacheFactory}.
         */
        private ClassLoader m_loader;

        /**
         * The scope name for the {@link Session}.
         */
        private String m_sScope;

        /**
         * The event interceptors to add for this session.
         */
        private final List<EventInterceptor<?>> f_listInterceptor = new ArrayList<>();

        /**
         * The priority order for this configuration.
         */
        private int m_nPriority;

        /**
         * An optional {@link ParameterResolver} to use to resolve configuration parameters.
         */
        private ParameterResolver m_parameterResolver;

        /**
         * An optional list of parameters.
         */
        private final ResolvableParameterList m_parameterList = new ResolvableParameterList();

        /**
         * The {@link Coherence.Mode} to use, which will override any mode used by the {@link Coherence} instance.
         */
        private Coherence.Mode m_mode;

        /**
         * The {@link SessionProvider} to use.
         */
        private SessionProvider m_sessionProvider;
        }

    // ----- inner class: ConfigurableCacheFactorySessionConfig -------------

    /**
     * An immutable {@link SessionConfiguration} to configure a {@link Session} that
     * wraps an underlying {@link com.tangosol.net.ConfigurableCacheFactory}.
     */
    class ConfigurableCacheFactorySessionConfig
            implements SessionConfiguration
        {
        /**
         * Create a {@link ConfigurableCacheFactorySessionConfig}.
         *
         * @param builder  the {@link Builder} to create this session configuration from
         */
        ConfigurableCacheFactorySessionConfig(Builder builder)
            {
            f_sName           = builder.m_sName == null || builder.m_sName.trim().isEmpty()
                                      ? Coherence.DEFAULT_NAME : builder.m_sName;
            f_sURI            = builder.m_sURI;
            f_loader          = builder.m_loader;
            f_listInterceptor = new ArrayList<>(builder.f_listInterceptor);
            f_nPriority       = builder.m_nPriority;
            f_sScope          = builder.m_sScope == null ? Coherence.DEFAULT_SCOPE : builder.m_sScope;
            f_mode            = builder.m_mode;
            f_sessionProvider = builder.m_sessionProvider;

            ParameterResolver resolver;
            if (!builder.m_parameterList.isEmpty())
                {
                ResolvableParameterList list = new ResolvableParameterList();
                for (Parameter parameter : builder.m_parameterList)
                    {
                    list.add(parameter);
                    }

                if (builder.m_parameterResolver == null)
                    {
                    resolver = list;
                    }
                else
                    {
                    resolver = new ChainedParameterResolver(list, builder.m_parameterResolver);
                    }
                }
            else
                {
                resolver = builder.m_parameterResolver;
                }

            f_parameterResolver = resolver;
            }

        // ----- SessionConfiguration methods -------------------------------

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public String getScopeName()
            {
            return f_sScope;
            }

        @Override
        public Iterable<EventInterceptor<?>> getInterceptors()
            {
            return Collections.unmodifiableList(f_listInterceptor);
            }

        @Override
        public int getPriority()
            {
            return f_nPriority;
            }

        @Override
        public Optional<ParameterResolver> getParameterResolver()
            {
            return Optional.ofNullable(f_parameterResolver);
            }

        @Override
        public Optional<String> getConfigUri()
            {
            return Optional.ofNullable(f_sURI);
            }

        @Override
        public Optional<ClassLoader> getClassLoader()
            {
            return Optional.ofNullable(f_loader);
            }

        @Override
        public Optional<Coherence.Mode> getMode()
            {
            Coherence.Mode mode = f_mode;
            if (mode == null && f_parameterResolver != null)
                {
                Parameter parameter = f_parameterResolver.resolve("coherence.client");
                if (parameter != null)
                    {
                    String sClient = (String) parameter.evaluate(f_parameterResolver).get();
                    if (sClient != null)
                        {
                        mode = Coherence.Mode.fromClientName(sClient);
                        }
                    }
                }
            return Optional.ofNullable(mode);
            }

        @Override
        public Optional<SessionProvider> sessionProvider()
            {
            return Optional.ofNullable(f_sessionProvider);
            }

        @Override
        public String toString()
            {
            return "ConfigurableCacheFactorySessionConfig(" +
                    "name='" + f_sName + '\'' +
                    ", uri='" + f_sURI + '\'' +
                    ", scope='" + f_sScope + '\'' +
                    ", mode=" + f_mode +
                    ')';
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the {@link Session} to create from the configuration.
         */
        private final String f_sName;

        /**
         * The URI of the configuration file to use to configure the underlying
         * {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}.
         */
        private final String f_sURI;

        /**
         * The {@link ClassLoader} to use for the {@link Session} and the underlying
         * {@link com.tangosol.net.ConfigurableCacheFactory}.
         */
        private final ClassLoader f_loader;

        /**
         * The scope name for the {@link Session}.
         */
        private final String f_sScope;

        /**
         * The event interceptors to add for this session.
         */
        private final List<EventInterceptor<?>> f_listInterceptor;

        /**
         * The priority order for this configuration.
         */
        private final int f_nPriority;

        /**
         * An optional {@link ParameterResolver} to use to resolve configuration parameters.
         */
        private final ParameterResolver f_parameterResolver;

        /**
         * The {@link Coherence.Mode} to use, which will override any mode used by the {@link Coherence} instance.
         */
        private final Coherence.Mode f_mode;

        /**
         * The {@link SessionProvider} to use.
         */
        private final SessionProvider f_sessionProvider;
        }

    // ----- constants --------------------------------------------------

    /**
     * The default priority for a configuration.
     * @see #getPriority()
     */
    int DEFAULT_PRIORITY = 0;
    }
