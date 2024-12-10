/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.tangosol.application.Context;
import com.tangosol.net.events.EventInterceptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

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
     * <p>
     * This configuration will include the default system session, and any
     * session configurations discovered by the {@link ServiceLoader}.
     *
     * @return  an immutable default {@link CoherenceConfiguration} instance
     * @see SessionConfiguration#defaultSession()
     * @see Builder#discoverSessions()
     */
    static CoherenceConfiguration create()
        {
        return builder()
                .withSession(SessionConfiguration.defaultSession())
                .discoverSessions()
                .build();
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

    /**
     * Return the optional application {@link Context} associated to this {@link Coherence} instance.
     *
     * @return the optional application {@link Context} associated to this {@link Coherence} instance
     */
    default Optional<Context> getApplicationContext()
        {
        return Optional.empty();
        }

    /**
     * Return the name of the default session.
     *
     * @return the name of the default session
     */
    default String getDefaultSessionName()
        {
        return Coherence.DEFAULT_NAME;
        }

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
         * Set the name of the {@link Session} to return is a {@link Session}
         * is requested with the {@link Coherence#DEFAULT_NAME default name}.
         *
         * @param sName  the name of the default {@link Session}
         *
         * @return  this {@link Builder}
         *
         * @throws IllegalArgumentException if this configuration does not contain
         *         a session with the specified name
         */
        public Builder withDefaultSession(String sName)
            {
            if (sName != null)
                {
                if (!sName.equals(Coherence.DEFAULT_NAME) && !f_mapConfig.containsKey(sName))
                    {
                    throw new IllegalArgumentException("A Session with the name " + sName
                            + " has not been added to this configuration");
                    }
                m_sDefaultSession = sName;
                }
            return this;
            }

        /**
         * Add all of the {@link SessionConfiguration} instances discovered
         * using the {@link ServiceLoader}.
         *
         * @return  this {@link Builder}
         */
        public Builder discoverSessions()
            {
            withSessions(ServiceLoader.load(SessionConfiguration.class));
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
                if (sName == null)
                    {
                    throw new IllegalArgumentException("A session configuration must provide a non-null name");
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
         * Set the {@link Context application context} to associate to the {@link Coherence} instance.
         *
         * @param context  the {@link Context application context} to associate to the {@link Coherence} instance
         *
         * @return  this {@link Builder}
         */
        public Builder withApplicationContext(Context context)
            {
            m_context = context;
            return this;
            }

        /**
         * Build a {@link CoherenceConfiguration} from this {@link Builder}.
         *
         * @return  a {@link CoherenceConfiguration} created from this {@link Builder}
         */
        public CoherenceConfiguration build()
            {
            Map<String, SessionConfiguration> mapConfig = new HashMap<>(f_mapConfig);

            if (mapConfig.isEmpty())
                {
                // if no dependencies configured add the default session
                SessionConfiguration cfgDefault = SessionConfiguration.defaultSession();
                mapConfig.put(cfgDefault.getName(), cfgDefault);
                }

            return new SimpleConfig(this, mapConfig);
            }

        // ----- data members -----------------------------------------------

        /**
         * The name of the {@link Coherence} instance.
         */
        private String m_sName;

        /**
         * The name of the default {@link Session}
         */
        private String m_sDefaultSession = Coherence.DEFAULT_NAME;

        /**
         * A map of named {@link SessionConfiguration} instances.
         */
        private final Map<String, SessionConfiguration> f_mapConfig = new HashMap<>();

        /**
         * A list of {@link EventInterceptor} instances to add to all sessions
         * created by the {@link Coherence} instance.
         */
        private final List<EventInterceptor<?>> f_listInterceptor = new ArrayList<>();

        /**
         * The Context application context to associate to the {@link Coherence} instance
         */
        private Context m_context;
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
         * @param builder          the configuration {@link Builder}
         * @param mapConfig        the {@link SessionConfiguration} to use to create
         *                         {@link com.tangosol.net.Session} instances
         *                         instances
         */
        private SimpleConfig(Builder                           builder,
                             Map<String, SessionConfiguration> mapConfig)
            {
            f_sName           = builder.m_sName == null || builder.m_sName.trim().isEmpty()
                                        ? Coherence.DEFAULT_NAME : builder.m_sName.trim();
            f_mapConfig       = Collections.unmodifiableMap(new HashMap<>(mapConfig));
            f_listInterceptor = Collections.unmodifiableList(new ArrayList<>(builder.f_listInterceptor));
            f_context         = builder.m_context;
            f_sDefaultSession = builder.m_sDefaultSession == null ? Coherence.DEFAULT_NAME : builder.m_sDefaultSession;
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

        @Override
        public Optional<Context> getApplicationContext()
            {
            return Optional.ofNullable(f_context);
            }

        @Override
        public String getDefaultSessionName()
            {
            return f_sDefaultSession;
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

        /**
         * An optional application {@link Context}.
         */
        private final Context f_context;

        /**
         * The name of the default session.
         */
        private final String f_sDefaultSession;
        }
    }
