/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.Name;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.lang.reflect.Member;

import java.util.Map;

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
     * @param context  the Coherence {@link OperationalContext}
     */
    @Inject
    SerializerProducer(OperationalContext context, BeanManager beanManager)
        {
        f_mapSerializerFactory = context.getSerializerMap();
        f_beanManager          = beanManager;
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
     * Create an instance of a {@link SerializerProducer.Builder}.
     *
     * @return an instance of {@link SerializerProducer.Builder}
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
    public Serializer getNamedSerializer(InjectionPoint ip)
        {
        String name = ip.getQualifiers()
                .stream()
                .filter(q -> q instanceof Name)
                .map(q -> ((Name) q).value())
                .findFirst()
                .orElse("");

        Member      member = ip.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();

        return getNamedSerializer(name, loader);
        }

    /**
     * Produces instances of a named {@link Serializer}.
     * <p>
     * Named Serializers are first looked up in the Coherence operational
     * configuration and if not found there they will be looked up in as a
     * {@link javax.inject.Named} CDI bean of type {@link Serializer}.
     *
     * @param sName   the name of the serializer
     * @param loader  the {@link ClassLoader} to use to create a {@link Serializer}
     *
     * @return an instance of a named {@link Serializer}
     *
     * @throws java.lang.NullPointerException     if the name parameter is null
     * @throws java.lang.IllegalArgumentException if no serializer is discoverable
     *                                            with the specified name
     */
    public Serializer getNamedSerializer(String sName, ClassLoader loader)
        {
        if (sName.trim().isEmpty())
            {
            return ExternalizableHelper.ensureSerializer(loader);
            }

        // first, try the serializers configured in Coherence
        SerializerFactory factory = f_mapSerializerFactory.get(sName);
        if (factory != null)
            {
            return factory.createSerializer(loader);
            }

        if (f_beanManager == null)
            {
            throw new IllegalArgumentException("Cannot locate a serializer named '"
                                               + sName + "' in the Coherence operational configuration");
            }

        // not in Coherence configuration so try for named CDI bean serializers
        Instance<Serializer> instance = f_beanManager.createInstance().select(Serializer.class, NamedLiteral.of(sName));
        if (instance.isResolvable())
            {
            return instance.get();
            }

        throw new IllegalArgumentException("Cannot locate a serializer named '"
                                           + sName + "' neither as a @Named annotated bean nor in the Coherence operational configuration");
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
         * Set the Coherence {@link OperationalContext} that the {@link
         * SerializerProducer} will use to discover named {@link Serializer}s
         * configured in the Coherence operational configuration.
         *
         * @param context the Coherence {@link OperationalContext}
         *
         * @return this {@link Builder}
         */
        public Builder context(OperationalContext context)
            {
            m_operationalContext = context;
            return this;
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
            return new SerializerProducer(ensureOperationalContext(), m_beanManager);
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
         * The {@link javax.enterprise.inject.spi.BeanManager} that the {@link
         * SerializerProducer} will use to discover
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
    }
