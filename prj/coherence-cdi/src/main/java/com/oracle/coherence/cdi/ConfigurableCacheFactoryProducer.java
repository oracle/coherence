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
import javax.enterprise.inject.spi.BeanManager;
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
     * @param beanManager  the CDI {@code BeanManager}
     * @param extension    Coherence CDI extension instance
     * @param uriResolver  the URI resolver to use
     */
    @Inject
    ConfigurableCacheFactoryProducer(BeanManager beanManager,
                                     CoherenceExtension extension,
                                     CacheFactoryUriResolver uriResolver)
        {
        f_beanManager = beanManager;
        f_extension   = extension;
        f_uriResolver = uriResolver;
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
        return f_extension.getDefaultCacheFactory();
        }

    /**
     * Produces a named {@link ConfigurableCacheFactory}.
     * <p>
     * If the value of the scope qualifier is blank or empty, the default
     * {@link com.tangosol.net.ConfigurableCacheFactory} will be returned.
     * <p>
     * The scope value will be resolved to a registered scope name, or, if
     * such scope name does not exist, to a cache configuration URI using
     * the {@link CacheFactoryUriResolver} bean.
     *
     * @param injectionPoint  the {@link InjectionPoint} that the cache factory
     *                        it to be injected into
     *
     * @return the named {@link ConfigurableCacheFactory}
     */
    @Produces
    @Scope()
    public ConfigurableCacheFactory getNamedConfigurableCacheFactory(InjectionPoint injectionPoint)
        {
        String sScope = injectionPoint.getQualifiers()
                .stream()
                .filter(q -> q.annotationType().isAssignableFrom(Scope.class))
                .map(q -> ((Scope) q).value())
                .findFirst()
                .orElse(null);

        return getConfigurableCacheFactory(sScope, injectionPoint);
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
    void disposeQualifiedConfigurableCacheFactory(@Disposes @Scope() ConfigurableCacheFactory ccf)
        {
        ccf.dispose();
        }

    /**
     * Produces the {@link CacheFactoryBuilder}.
     *
     * @return the {@link CacheFactoryBuilder}
     */
    @Produces
    public CacheFactoryBuilder getCacheFactoryBuilder()
        {
        return f_extension.getCacheFactoryBuilder();
        }

    // ---- helpers ---------------------------------------------------------

    ConfigurableCacheFactory getConfigurableCacheFactory(String sScope, InjectionPoint injectionPoint)
        {
        if (sScope == null || sScope.trim().isEmpty())
            {
            return getDefaultConfigurableCacheFactory();
            }

        // try to find the factory using scope name
        ConfigurableCacheFactory ccf = f_extension.getCacheFactoryBuilder().getConfigurableCacheFactory(sScope);
        if (ccf != null)
            {
            return ccf;
            }

        // treat scope name as config URI and try to load it if necessary
        String sUri = f_uriResolver.resolve(sScope);
        if (sUri == null || sUri.trim().isEmpty())
            {
            sUri = WithConfiguration.autoDetect().getLocation();
            }

        Member member = injectionPoint.getMember();
        ClassLoader loader = member == null
                             ? Base.getContextClassLoader()
                             : member.getDeclaringClass().getClassLoader();

        ccf = getCacheFactoryBuilder().getConfigurableCacheFactory(sUri, loader);
        if (ccf.getResourceRegistry().getResource(BeanManager.class, "beanManager") != null)
            {
            return ccf;
            }
        
        ccf.getResourceRegistry().registerResource(BeanManager.class, "beanManager", f_beanManager);
        return f_extension.registerInterceptors(ccf);
        }

    // ---- data members ----------------------------------------------------

    /**
     * CDI bean manager.
     */
    private final BeanManager f_beanManager;

    /**
     * Coherence CDI extension.
     */
    private final CoherenceExtension f_extension;

    /**
     * Cache factory URI resolver.
     */
    private final CacheFactoryUriResolver f_uriResolver;
    }
