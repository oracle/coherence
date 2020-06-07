/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.net.options.WithConfiguration;

import com.tangosol.util.Base;

import java.lang.reflect.Member;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link com.tangosol.net.Session} instances.
 *
 * @author Jonathan Knight  2019.11.06
 */
@ApplicationScoped
public class SessionProducer
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor used by CDI.
     *
     * @param uriResolver  the URI resolver to use
     */
    @Inject
    SessionProducer(CacheFactoryUriResolver uriResolver)
        {
        this(uriResolver, com.tangosol.net.CacheFactory.getCacheFactoryBuilder());
        }

    /**
     * Create a ConfigurableCacheFactoryProducer.
     *
     * @param uriResolver          the URI resolver to use
     * @param cacheFactoryBuilder  the {@link CacheFactoryBuilder} to use to
     *                             obtain {@link com.tangosol.net.Session} instances
     */
    SessionProducer(CacheFactoryUriResolver uriResolver, CacheFactoryBuilder cacheFactoryBuilder)
        {
        f_uriResolver         = uriResolver;
        f_cacheFactoryBuilder = cacheFactoryBuilder;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces the default {@link com.tangosol.net.Session}.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the default {@link com.tangosol.net.Session}
     */
    @Produces
    public com.tangosol.net.Session getDefaultSession(InjectionPoint injectionPoint)
        {
        return getNamedSession(injectionPoint);
        }

    /**
     * Produces a named {@link com.tangosol.net.Session}.
     * <p>
     * If the value of the name qualifier is blank or empty String the default
     * {@link com.tangosol.net.Session} will be returned.
     * <p>
     * The name parameter will be resolved to a cache configuration URI using
     * the {@link CacheFactoryUriResolver} bean.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link com.tangosol.net.Session}
     */
    @Produces
    @Name("")
    public com.tangosol.net.Session getNamedSession(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Name.class))
                .map(q -> ((Name) q).value())
                .findFirst()
                .orElse(null);

        String sUri = f_uriResolver.resolve(sName);
        WithConfiguration cfg;

        if (sUri == null || sUri.trim().isEmpty())
            {
            cfg = WithConfiguration.autoDetect();
            }
        else
            {
            cfg = WithConfiguration.using(sUri);
            }

        Member      member = injectionPoint.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();
        Map<String, com.tangosol.net.Session> map = f_mapSession.computeIfAbsent(loader, l -> new HashMap<>());
        return map.computeIfAbsent(sUri, u -> f_cacheFactoryBuilder.createSession(cfg, WithClassLoader.using(loader)));
        }

    // ---- data members ----------------------------------------------------

    /**
     * Cache factory builder.
     */
    private final CacheFactoryBuilder f_cacheFactoryBuilder;

    /**
     * Cache factory URI resolver.
     */
    private final CacheFactoryUriResolver f_uriResolver;

    /**
     * Session map, keyed by class loader.
     */
    private final Map<ClassLoader, Map<String, com.tangosol.net.Session>> f_mapSession = new HashMap<>();
    }
