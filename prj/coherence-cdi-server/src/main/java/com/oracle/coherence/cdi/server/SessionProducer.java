/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.Scope;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;

import com.tangosol.util.Base;

import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link Session} instances.
 *
 * @author Jonathan Knight  2019.11.06
 * @since 20.06
 */
@ApplicationScoped
public class SessionProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor used by CDI.
     */
    @Inject
    SessionProducer(CoherenceServerExtension extension, ConfigurableCacheFactoryProducer ccfProducer)
        {
        f_extension   = extension;
        f_ccfProducer = ccfProducer;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces the default {@link Session}.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the default {@link Session}
     */
    @Produces
    public Session getDefaultSession(InjectionPoint injectionPoint)
        {
        return getScopedSession(injectionPoint);
        }

    /**
     * Produces a {@link Session} for a given scope.
     * <p>
     * If the value of the scope qualifier is blank or empty, the default
     * {@link Session} will be returned.
     * <p>
     * The scope value will be resolved to a registered scope name, or, if
     * such scope name does not exist, to a cache configuration URI using
     * the {@link CacheFactoryUriResolver} bean.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link Session}
     */
    @Produces
    @Scope
    public Session getScopedSession(InjectionPoint injectionPoint)
        {
        String sScope = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Scope.class))
                .map(q -> ((Scope) q).value().trim())
                .findFirst()
                .orElse(null);

        Member      member = injectionPoint.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();

        ConfigurableCacheFactory ccf = sScope == null || sScope.isEmpty()
                ? f_extension.getDefaultCacheFactory()
                : f_ccfProducer.getConfigurableCacheFactory(sScope, injectionPoint);

        return new ConfigurableCacheFactorySession(ccf, loader);
        }

    // ---- data members ----------------------------------------------------

    /**
     * CDI bean manager.
     */
    private final ConfigurableCacheFactoryProducer f_ccfProducer;

    /**
     * Coherence CDI extension.
     */
    private final CoherenceServerExtension f_extension;
    }
