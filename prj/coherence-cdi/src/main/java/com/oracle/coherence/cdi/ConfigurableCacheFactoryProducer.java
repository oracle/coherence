/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.options.WithConfiguration;

import com.tangosol.util.Base;

import java.lang.reflect.Member;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

import javax.inject.Inject;

/**
 * A CDI producer for {@link com.tangosol.net.ConfigurableCacheFactory} and
 * {@link com.tangosol.net.CacheFactoryBuilder} instances.
 *
 * @author Jonathan Knight  2019.10.19
 */
@ApplicationScoped
class ConfigurableCacheFactoryProducer
    {
    // ---- constructors ----------------------------------------------------
    
    /**
     * Default constructor used by CDI.
     * 
     * @param uriResolver  the URI resolver to use
     */
    @Inject
    ConfigurableCacheFactoryProducer(CacheFactoryUriResolver uriResolver)
        {
        this(uriResolver, com.tangosol.net.CacheFactory.getCacheFactoryBuilder());
        }

    /**
     * Create a ConfigurableCacheFactoryProducer.
     *
     * @param uriResolver          the URI resolver to use
     * @param cacheFactoryBuilder  the {@link CacheFactoryBuilder} to use to
     *                             obtain {@link ConfigurableCacheFactory} instances
     */
    ConfigurableCacheFactoryProducer(CacheFactoryUriResolver uriResolver, CacheFactoryBuilder cacheFactoryBuilder)
        {
        f_cacheFactoryBuilder = cacheFactoryBuilder;
        m_uriResolver         = uriResolver;
        }

    // ---- producer methods ------------------------------------------------

    /**
     * Produces the default {@link ConfigurableCacheFactory}.
     *
     * @return the default {@link ConfigurableCacheFactory}
     */
    @Produces
    public ConfigurableCacheFactory getDefaultConfigurableCacheFactory()
        {
        return f_cacheFactoryBuilder.getConfigurableCacheFactory(Base.getContextClassLoader());
        }

    /**
     * Produces a named {@link ConfigurableCacheFactory}.
     * <p>
     * If the value of the name qualifier is blank or empty String the default
     * {@link com.tangosol.net.ConfigurableCacheFactory} will be returned.
     * <p>
     * The name parameter will be resolved to a cache configuration URI using
     * the {@link CacheFactoryUriResolver} bean.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link ConfigurableCacheFactory}
     */
    @Produces
    @Name("")
    public ConfigurableCacheFactory getNamedConfigurableCacheFactory(InjectionPoint injectionPoint)
        {
        String sName = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Name.class))
                .map(q -> ((Name) q).value())
                .findFirst()
                .orElse(null);

        return getConfigurableCacheFactory(sName, injectionPoint);
        }

    /**
     * Dispose of a {@link ConfigurableCacheFactory} bean.
     * <p>
     * Disposing of a {@link ConfigurableCacheFactory} will call {@link
     * ConfigurableCacheFactory#dispose()}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} to dispose
     */
    void disposeConfigurableCacheFactory(@Disposes ConfigurableCacheFactory ccf)
        {
        ccf.dispose();
        }

    /**
     * Dispose of a {@link ConfigurableCacheFactory} bean.
     * <p>
     * Disposing of a {@link ConfigurableCacheFactory} will call {@link
     * ConfigurableCacheFactory#dispose()}.
     *
     * @param ccf  the {@link ConfigurableCacheFactory} to dispose
     */
    void disposeQualifiedConfigurableCacheFactory(@Disposes @Name("") ConfigurableCacheFactory ccf)
        {
        ccf.dispose();
        }

    /**
     * Produces the default {@link ConfigurableCacheFactory}.
     *
     * @return the default {@link ConfigurableCacheFactory}
     */
    @Produces
    public CacheFactoryBuilder getCacheFactoryBuilder()
        {
        return f_cacheFactoryBuilder;
        }

    // ---- helpers ---------------------------------------------------------

    ConfigurableCacheFactory getConfigurableCacheFactory(String sName, InjectionPoint injectionPoint)
        {
        String sUri = m_uriResolver.resolve(sName);
        if (sUri == null || sUri.trim().isEmpty())
            {
            sUri = WithConfiguration.autoDetect().getLocation();
            }

        Member member = injectionPoint.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();

        return f_cacheFactoryBuilder.getConfigurableCacheFactory(sUri, loader);
        }

    // ---- data members ----------------------------------------------------

    /**
     * Cache factory builder.
     */
    private final CacheFactoryBuilder f_cacheFactoryBuilder;

    /**
     * Cache factory URI resolver.
     */
    private CacheFactoryUriResolver m_uriResolver;
    }
