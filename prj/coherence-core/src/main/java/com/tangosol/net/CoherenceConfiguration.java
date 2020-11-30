/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.net.events.EventInterceptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The immutable configuration for a {@link Coherence} instance.
 *
 * @author Jonathan Knight  2020.11.05
 * @since 20.12
 */
public interface CoherenceConfiguration
    {
    // ----- CoherenceConfiguration factory methods -------------------------

    /**
     * Returns a {@link Builder} that can configure and build instances
     * of {@link CoherenceConfiguration}.
     *
     * @return  a {@link Builder} that can configure and build instances
     *          of {@link CoherenceConfiguration}
     */
    static Builder builder()
        {
        return new Builder();
        }

    /**
     * Returns an immutable default {@link CoherenceConfiguration} instance.
     *
     * @return  an immutable default {@link CoherenceConfiguration} instance
     */
    static CoherenceConfiguration create()
        {
        return builder().build();
        }

    // ----- CoherenceConfiguration API methods -----------------------------

    /**
     * Return the name to use for the {@link Coherence} instance.
     *
     * @return  the name to use for the {@link Coherence} instance
     */
    String getName();

    /**
     * Return an immutable {@link Map} of named {@link SessionConfiguration}
     * instances that will be used by the {@link Coherence} instance
     * to create {@link Session Sessions}.
     *
     * @return  an immutable {@link Map} of named {@link SessionConfiguration}
     *          instances
     */
    Map<String, SessionConfiguration> getSessionConfigurations();

    /**
     * Return an immutable {@link Iterable} of {@link EventInterceptor interceptors}
     * that will be added to all sessions created by the {@link Coherence} instance.
     *
     * @return  an immutable {@link Iterable} of {@link EventInterceptor interceptors}
     */
    Iterable<EventInterceptor<?>> getInterceptors();

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder to build a {@link CoherenceConfiguration} instance.
     * <p>
     * This class is not thread-safe, application code that calls
     * methods on this builder from multiple threads must implement
     * its own thread-safety.
     */
    class Builder
        {
        /**
         * Set the name of the {@link Coherence} instance.
         * <p>
         * If the name is set ot {@code null} or empty/blank string
         * the {@link Coherence#DEFAULT_NAME} will be used.
         * <p>
         * The name must be unique across all {@link Coherence} instances.
         *
         * @param sName  the name of the {@link Coherence} instance
         *
         * @return  this {@link Builder}
         */
        public Builder named(String sName)
            {
            m_sName = sName;
            return this;
            }

        /**
         * Add a {@link SessionConfiguration} to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param config  the {@link SessionConfiguration} to configure a {@link Session}
         *
         * @return  this {@link Builder}
         */
        public Builder withSession(SessionConfiguration config)
            {
            if (config != null && config.isEnabled())
                {
                String sName = config.getName();
                if (sName == null || sName.trim().isEmpty())
                    {
                    throw new IllegalArgumentException("A session configuration must provide a non-null non-blank name");
                    }
                f_mapConfig.put(sName, config);
                }
            return this;
            }

        /**
         * Add the {@link SessionConfiguration} instances to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} instances on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param configs  the {@link SessionConfiguration} instances to configure
         *                 {@link Session} instances
         *
         * @return  this {@link Builder}
         */
        public Builder withSessions(Iterable<? extends SessionConfiguration> configs)
            {
            for (SessionConfiguration configuration : configs)
                {
                withSession(configuration);
                }
            return this;
            }

        /**
         * Add the {@link SessionConfiguration} instances to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} instances on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param configs  the {@link SessionConfiguration} instances to configure
         *                 {@link Session} instances
         *
         * @return  this {@link Builder}
         */
        public Builder withSessions(SessionConfiguration... configs)
            {
            for (SessionConfiguration configuration : configs)
                {
                withSession(configuration);
                }
            return this;
            }

        /**
         * Add the {@link SessionConfiguration} provider to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} instances on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param provider  the {@link SessionConfiguration} provider to configure
         *                  {@link Session} instances
         *
         * @return  this {@link Builder}
         */
        public Builder withSessionProvider(SessionConfiguration.Provider provider)
            {
            if (provider != null)
                {
                return withSession(provider.getConfiguration());
                }
            return this;
            }

        /**
         * Add the {@link SessionConfiguration} providers to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} instances on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param providers  the {@link SessionConfiguration} providers to configure
         *                   {@link Session} instances
         *
         * @return  this {@link Builder}
         */
        public Builder withSessionProviders(SessionConfiguration.Provider... providers)
            {
            if (providers == null)
                {
                return this;
                }
            return withSessionProviders(Arrays.asList(providers));
            }

        /**
         * Add the {@link SessionConfiguration} providers to the {@link Coherence}
         * instance that will be used to create the corresponding
         * {@link Session} instances on start-up.
         * <p>
         * The name of the {@link SessionConfiguration} returned by the
         * {@link SessionConfiguration#getName()} method must be unique
         * across all {@link Coherence} instances.
         * <p>
         * Adding a {@link SessionConfiguration} with the same name as
         * a {@link SessionConfiguration} already added to this builder
         * will overwrite the previously added configuration.
         *
         * @param providers  the {@link SessionConfiguration} providers to configure
         *                   {@link Session} instances
         *
         * @return  this {@link Builder}
         */
        public Builder withSessionProviders(Iterable<? extends SessionConfiguration.Provider> providers)
            {
            for (SessionConfiguration.Provider provider : providers)
                {
                withSession(provider.getConfiguration());
                }
            return this;
            }

        /**
         * Add an {@link EventInterceptor} that will be added to all {@link Session}
         * instances as they are created on start-up.
         *
         * @param interceptor  the {@link EventInterceptor} to add
         *
         * @return  this {@link Builder}
         */
        public Builder withEventInterceptor(EventInterceptor<?> interceptor)
            {
            f_listInterceptor.add(interceptor);
            return this;
            }

        /**
         * Add the {@link EventInterceptor} instances that will be added to all
         * {@link Session} instances as they are created on start-up.
         *
         * @param interceptors  the {@link EventInterceptor} instances to add
         *
         * @return  this {@link Builder}
         */
        public Builder withEventInterceptors(EventInterceptor<?>... interceptors)
            {
            Collections.addAll(f_listInterceptor, interceptors);
            return this;
            }

        /**
         * Add the {@link EventInterceptor} instances that will be added to all
         * {@link Session} instances as they are created on start-up.
         *
         * @param interceptors  the {@link EventInterceptor} instances to add
         *
         * @return  this {@link Builder}
         */
        public Builder withEventInterceptors(Iterable<? extends EventInterceptor<?>> interceptors)
            {
            for (EventInterceptor<?> interceptor : interceptors)
                {
                f_listInterceptor.add(interceptor);
                }
            return this;
            }

        /**
         * Build a {@link CoherenceConfiguration} from this {@link Builder}.
         *
         * @return  a {@link CoherenceConfiguration} created from this {@link Builder}
         */
        public CoherenceConfiguration build()
            {
            if (Coherence.getInstance(m_sName) != null)
                {
                throw new IllegalArgumentException("A Coherence instance already exists with the name " + m_sName);
                }

            Map<String, SessionConfiguration> mapConfig = new HashMap<>(f_mapConfig);

            // if no dependencies configured set the default
            if (mapConfig.isEmpty())
                {
                SessionConfiguration cfgDefault = SessionConfiguration.defaultSession();
                mapConfig.put(cfgDefault.getName(), cfgDefault);
                }

            return new SimpleConfig(m_sName, mapConfig, f_listInterceptor);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the {@link Coherence} instance.
         */
        private String m_sName;

        /**
         * A map of named {@link SessionConfiguration} instances.
         */
        private final Map<String, SessionConfiguration> f_mapConfig = new HashMap<>();

        /**
         * A list of {@link EventInterceptor} instances to add to all sessions
         * created by the {@link Coherence} instance.
         */
        private final List<EventInterceptor<?>> f_listInterceptor = new ArrayList<>();
        }

    // ----- inner class: SimpleConfig --------------------------------------

    /**
     * A simple immutable implementation of {@link CoherenceConfiguration}.
     */
    class SimpleConfig
            implements CoherenceConfiguration
        {
        // ----- constructors ---------------------------------------------------

        /**
         * Create an instance of a {@link CoherenceConfiguration} using the state from the
         * specified {@link Builder}.
         *
         * @param sName            the name for the {@link Coherence} instance created
         *                         from this {@link CoherenceConfiguration}
         * @param mapConfig        the {@link SessionConfiguration} to use to create
         *                         {@link com.tangosol.net.Session} instances
         *                         instances
         * @param listInterceptor  the interceptors to add to the
         *                         {@link com.tangosol.net.Session}
         *                         instances
         */
        private SimpleConfig(String                            sName,
                             Map<String, SessionConfiguration> mapConfig,
                             List<EventInterceptor<?>>         listInterceptor)
            {
            f_sName           = sName == null || sName.trim().isEmpty() ? Coherence.DEFAULT_NAME : sName.trim();
            f_mapConfig       = Collections.unmodifiableMap(new HashMap<>(mapConfig));
            f_listInterceptor = Collections.unmodifiableList(new ArrayList<>(listInterceptor));
            }

        // ----- CoherenceConfiguration API methods -------------------------

        @Override
        public String getName()
            {
            return f_sName;
            }

        @Override
        public Map<String, SessionConfiguration> getSessionConfigurations()
            {
            return f_mapConfig;
            }

        @Override
        public Iterable<EventInterceptor<?>> getInterceptors()
            {
            return f_listInterceptor;
            }

        // ----- data members ---------------------------------------------------

        /**
         * The name of the {@link Coherence} instance.
         */
        private final String f_sName;

        /**
         * The session configurations keyed by session name.
         */
        private final Map<String, SessionConfiguration> f_mapConfig;

        /**
         * The global interceptors to be added to all sessions.
         */
        private final List<EventInterceptor<?>> f_listInterceptor;
        }
    }
