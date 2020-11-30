/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.events.EventInterceptor;

import com.tangosol.net.options.WithClassLoader;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.options.WithName;
import com.tangosol.net.options.WithScopeName;

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
     * Return the {@link Session.Option}s to be used to configure
     * a {@link Session}.
     *
     * @return  the {@link Session.Option}s to be used to configure
     *          a {@link Session}
     */
    Session.Option[] getOptions();

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
     * Return an optional {@link SessionProvider} that should be used to create the
     * {@link Session} from this configuration.
     *
     * @return  the {@link SessionProvider} that should be used to create the
     *          {@link Session} from this configuration.
     */
    default Optional<SessionProvider> getSessionProvider()
        {
        return Optional.empty();
        }

    /**
     * Returns the priority for this configurations.
     * <p>
     * Sessions will be created in priority order, lowest
     * priority first.
     * <p>
     * The default priority is zero (see {@link #DEFAULT_PRIORITY}.
     *
     * @return  the priority for this configurations
     */
    default int getPriority()
        {
        return DEFAULT_PRIORITY;
        }

    /**
     * Order SessionConfiguration by priority, lowest priority
     * comes first.
     *
     * @param other  the configuration to compare to
     *
     * @return  compare this configuration with the specified
     *          configuration ordering by by priority
     */
    @Override
    default int compareTo(SessionConfiguration other)
        {
        return Integer.compare(getPriority(), other.getPriority());
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
     *
     * @return  a {@link SessionConfiguration} for the default {@link Session}
     */
    static SessionConfiguration defaultSession()
        {
        return create(Coherence.DEFAULT_NAME, CacheFactoryBuilder.URI_DEFAULT, null, null);
        }

    /**
     * Create a default named {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration file will be used.
     *
     * @param sURI   the URI of the configuration file to use to configure the underlying
     *               {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     *
     * @return  a default named {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sURI)
        {
        return create(null, sURI, null, null);
        }

    /**
     * Create a {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration
     * file will be used.
     * <p>
     * If the name parameter is {@code null} or blank the {@link Coherence#DEFAULT_NAME}
     * will be used for the name.
     *
     * @param sName  the name of the {@link Session} to create from the configuration
     * @param sURI   the URI of the configuration file to use to configure the underlying
     *               {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sURI)
        {
        return create(sName, sURI, null, null);
        }

    /**
     * Create a default named {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration file will be used.
     *
     * @param sURI    the URI of the configuration file to use to configure the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     * @param loader  the {@link ClassLoader} to use for the {@link Session} and the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory}
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sURI, ClassLoader loader)
        {
        return create(null, sURI, loader, null);
        }

    /**
     * Create a {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration file will be used.
     * <p>
     * If the name parameter is {@code null} or blank the {@link Coherence#DEFAULT_NAME}
     * will be used for the name.
     *
     * @param sName   the name of the {@link Session} to create from the configuration
     * @param sURI    the URI of the configuration file to use to configure the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     * @param loader  the {@link ClassLoader} to use for the {@link Session} and the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory}
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sURI, ClassLoader loader)
        {
        return create(sName, sURI, loader, null);
        }

    /**
     * Create a {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration file will be used.
     * <p>
     * If the name parameter is {@code null} or blank a unique UID wil be used for the name.
     *
     * @param sName   the name of the {@link Session} to create from the configuration
     * @param sURI    the URI of the configuration file to use to configure the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     * @param sScope  the scope name for the {@link Session}
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sURI, String sScope)
        {
        return create(sName, sURI, null, sScope);
        }

    /**
     * Create a {@link SessionConfiguration}.
     * <p>
     * If the uri parameter is {@code null} or blank the default configuration file will be used.
     * <p>
     * If the name parameter is {@code null} or blank a unique UID wil be used for the name.
     *
     * @param sName   the name of the {@link Session} to create from the configuration
     * @param sURI    the URI of the configuration file to use to configure the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
     * @param loader  the {@link ClassLoader} to use for the {@link Session} and the underlying
     *                {@link com.tangosol.net.ConfigurableCacheFactory}
     * @param sScope  the scope name for the {@link Session}
     *
     * @return  a {@link SessionConfiguration}
     */
    static SessionConfiguration create(String sName, String sURI, ClassLoader loader, String sScope)
        {
        return builder()
                .named(sName)
                .withConfigUri(sURI)
                .withClassLoader(loader)
                .withScopeName(sScope)
                .build();
        }

    // ----- inner class: Provider ------------------------------------------

    /**
     * A provider of a {@link SessionConfiguration}.
     */
    @FunctionalInterface
    interface Provider
        {
        /**
         * Returns the {@link SessionConfiguration}.
         *
         * @return  the {@link SessionConfiguration}
         */
        SessionConfiguration getConfiguration();
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to build a {@link ConfigurableCacheFactorySessionConfig}.
     */
    class Builder
        {
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
         * Build the {@link SessionConfiguration}.
         *
         * @return the {@link SessionConfiguration}
         */
        public SessionConfiguration build()
            {
            return new ConfigurableCacheFactorySessionConfig(m_sName, m_sURI, m_loader, m_sScope,
                    f_listInterceptor, m_nPriority);
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
         * @param sName            the name of the {@link Session} to create from the configuration
         * @param sURI             the URI of the configuration file to use to configure the underlying
         *                         {@link com.tangosol.net.ConfigurableCacheFactory} for the {@link Session}
         * @param loader           the {@link ClassLoader} to use for the {@link Session} and the underlying
         *                         {@link com.tangosol.net.ConfigurableCacheFactory}
         * @param sScope           the scope name for the {@link Session}
         * @param listInterceptor  the event interceptors to add to the session
         */
        ConfigurableCacheFactorySessionConfig(String                    sName,
                                              String                    sURI,
                                              ClassLoader               loader,
                                              String                    sScope,
                                              List<EventInterceptor<?>> listInterceptor,
                                              int                       nPriority)
            {
            f_sName           = sName == null || sName.trim().isEmpty() ? Coherence.DEFAULT_NAME : sName;
            f_sURI            = sURI;
            f_loader          = loader;
            f_listInterceptor = new ArrayList<>(listInterceptor);
            f_nPriority       = nPriority;

            if (sScope == null)
                {
                f_sScope = Coherence.DEFAULT_NAME.equals(f_sName) ? Coherence.DEFAULT_SCOPE : f_sName;
                }
            else
                {
                f_sScope = sScope;
                }
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
        public Session.Option[] getOptions()
            {
            List<Session.Option> listOps = new ArrayList<>();

            listOps.add(WithName.of(f_sName));

            if (f_sURI != null && !f_sURI.trim().isEmpty())
                {
                listOps.add(WithConfiguration.using(f_sURI));
                }

            if (f_sScope != null)
                {
                listOps.add(WithScopeName.of(f_sScope));
                }

            if (f_loader != null)
                {
                listOps.add(WithClassLoader.using(f_loader));
                }

            return listOps.toArray(new Session.Option[0]);
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
        }

    // ----- constants --------------------------------------------------

    /**
     * The default priority for a configuration.
     * @see #getPriority()
     */
    int DEFAULT_PRIORITY = 0;
    }
