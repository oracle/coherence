/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.coherence.config.Config;
import com.tangosol.io.DefaultSerializer;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.lang.reflect.Member;

import java.util.Collections;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;


/**
 * A CDI producer of {@link Serializer} instances.
 *
 * @author Jonathan Knight  2019.11.20
 * @since 20.06
 */
@ApplicationScoped
public class SerializerProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Create a {@link SerializerProducer}.
     *
     * @param beanManager the CDI {@link BeanManager}
     */
    @Inject
    SerializerProducer(BeanManager beanManager)
        {
        f_beanManager = beanManager;

        Instance<OperationalContext> instance = beanManager == null
                ? null
                : beanManager.createInstance().select(OperationalContext.class);

        if (instance != null && instance.isResolvable())
            {
            f_mapSerializerFactory = instance.get().getSerializerMap();
            }
        else
            {
            f_mapSerializerFactory = Collections.emptyMap();
            }
        }

    /**
     * Create a {@link SerializerProducer}.
     *
     * @param beanManager the CDI {@link BeanManager}
     * @param context     the Coherence operational context to use to obtain serializers
     */
    SerializerProducer(BeanManager beanManager, OperationalContext context)
        {
        f_beanManager          = beanManager;
        f_mapSerializerFactory = context == null ? Collections.emptyMap() : context.getSerializerMap();
        }

    /**
     * Create an instance of {@link SerializerProducer}.
     *
     * @return an instance of {@link SerializerProducer}
     */
    public static SerializerProducer create()
        {
        return builder().build();
        }

    /**
     * Create an instance of a {@link Builder}.
     *
     * @return an instance of {@link Builder}
     */
    public static Builder builder()
        {
        return new Builder();
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces an instance of the default {@link Serializer}.
     *
     * @param ip  the {@link InjectionPoint} that the {@link Serializer} will be
     *            injected into
     *
     * @return an instance of the default {@link Serializer}
     */
    @Produces
    public Serializer getDefaultSerializer(InjectionPoint ip)
        {
        Member      member = ip.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();
        return getNamedSerializer("", loader);
        }

    /**
     * Produces instances of a named {@link Serializer}.
     * <p>
     * Named Serializers are first looked up in the Coherence operational
     * configuration and if not found there they will be looked up in as a
     * {@link javax.inject.Named} CDI bean of type {@link Serializer}.
     *
     * @param ip the {@link InjectionPoint} that the {@link Serializer} will be
     *           injected into
     *
     * @return an instance of a named {@link Serializer}
     */
    @Produces
    @Name("")
    @ConfigUri("")
    public Serializer getNamedSerializer(InjectionPoint ip)
        {
        String sName = ip.getQualifiers()
                         .stream()
                         .filter(q -> q instanceof Name)
                         .map(q -> ((Name) q).value())
                         .findFirst()
                         .orElse("");

        String sURI = ip.getQualifiers()
                        .stream()
                        .filter(q -> q instanceof ConfigUri)
                        .map(q -> ((ConfigUri) q).value())
                        .findFirst()
                        .orElse("");

        Member      member = ip.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();

        return getNamedSerializer(sName, sURI, loader);
        }

    /**
     * Produces instances of a named {@link Serializer}.
     *
     * @param sName   the name of the serializer
     * @param loader  the {@link ClassLoader} to use to create a {@link Serializer}
     *
     * @return an instance of a named {@link Serializer}
     *
     * @throws NullPointerException     if the name parameter is null
     * @throws IllegalArgumentException if no serializer is discoverable
     *                                            with the specified name
     */
    public Serializer getNamedSerializer(String sName, ClassLoader loader)
        {
        return getNamedSerializer(sName, null, loader);
        }

    /**
     * Produces instances of a named {@link Serializer}.
     *
     * @param sName       the name of the serializer
     * @param sConfigUri  the URI of the configuration for the serializer
     * @param loader      the {@link ClassLoader} to use to create a {@link Serializer}
     *
     * @return an instance of a named {@link Serializer}
     *
     * @throws NullPointerException     if the name parameter is null
     * @throws IllegalArgumentException if no serializer is discoverable
     *                                            with the specified name
     */
    Serializer getNamedSerializer(String sName, String sConfigUri, ClassLoader loader)
        {
        if (sName.trim().isEmpty())
            {
            // no name specified so return the default serializer.
            return ExternalizableHelper.ensureSerializer(loader);
            }

        // first, try the serializers configured in Coherence
        SerializerFactory factory = f_mapSerializerFactory.get(sName);
        if (factory != null)
            {
            return factory.createSerializer(loader);
            }

        if ("java".equalsIgnoreCase(sName))
            {
            return f_defaultSerializer;
            }
        else if ("pof".equalsIgnoreCase(sName))
            {
            String sURI = sConfigUri == null ? f_sDefaultPofConfig : sConfigUri;
            return f_mapPofSerializer.computeIfAbsent(sURI, this::ensurePofSerializer);
            }

        if (f_beanManager == null)
            {
            throw new IllegalArgumentException("Cannot locate a serializer named '"
                                               + sName + "' no BeanManager present");
            }

        // not in Coherence configuration so try for named CDI bean serializers
        Instance<Serializer> instance = f_beanManager.createInstance().select(Serializer.class, NamedLiteral.of(sName));
        if (instance.isResolvable())
            {
            return instance.get();
            }

        throw new IllegalArgumentException("Cannot locate a serializer named '" + sName + "'");
        }

    private ConfigurablePofContext ensurePofSerializer(String sConfig)
        {
        return new ConfigurablePofContext(sConfig);
        }

    // ----- inner class: Builder -------------------------------------------

    /**
     * A builder that builds {@link SerializerProducer}
     * instances.
     */
    public static class Builder
        {
        /**
         * Private constructor.
         */
        private Builder()
            {
            }

        /**
         * Set the Coherence {@link BeanManager} that the {@link
         * SerializerProducer} will use to discover named {@link Serializer}s
         * beans.
         *
         * @param beanManager the {@link BeanManager} to use to discover {@link
         *                    Serializer} beans
         *
         * @return this {@link Builder}
         */
        public Builder beanManager(BeanManager beanManager)
            {
            m_beanManager = beanManager;
            return this;
            }

        /**
         * Build a new instance of a {@link SerializerProducer}.
         *
         * @return a new instance of a {@link SerializerProducer}
         */
        public SerializerProducer build()
            {
            return new SerializerProducer(m_beanManager, ensureOperationalContext());
            }

        private OperationalContext ensureOperationalContext()
            {
            if (m_operationalContext == null)
                {
                return (OperationalContext) CacheFactory.getCluster();
                }
            return m_operationalContext;
            }

        // ---- data members ------------------------------------------------

        /**
         * The Coherence {@link com.tangosol.net.OperationalContext}.
         */
        private OperationalContext m_operationalContext;

        /**
         * The {@link BeanManager} that the {@link SerializerProducer} will use to discover
         * named {@link Serializer} beans.
         */
        private BeanManager m_beanManager;
        }

    // ---- data members ----------------------------------------------------
    
    /**
     * The map of named {@link com.tangosol.io.SerializerFactory} instances
     * configured in the Coherence operational configuration.
     */
    private final Map<String, SerializerFactory> f_mapSerializerFactory;

    /**
     * The CDI {@link BeanManager}.
     */
    private final BeanManager f_beanManager;

    /**
     * The default Java serializer.
     */
    private final DefaultSerializer f_defaultSerializer = new DefaultSerializer();

    /**
     * A map of POF serializers keyed by configuration URI.
     */
    private final Map<String, ConfigurablePofContext> f_mapPofSerializer = new ConcurrentHashMap<>();

    /**
     * The default POF configuration URI.
     */
    private final String f_sDefaultPofConfig = Config.getProperty("coherence.pof.config", "pof-config.xml");
    }
