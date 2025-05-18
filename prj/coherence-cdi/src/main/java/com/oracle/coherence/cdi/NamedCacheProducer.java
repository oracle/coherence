/*
 * Copyright (c) 2020, 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.Coherence;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;

import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;

import java.lang.annotation.Annotation;

import java.lang.reflect.Member;

import jakarta.enterprise.context.ApplicationScoped;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;

import jakarta.enterprise.inject.spi.Annotated;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.inject.spi.InjectionPoint;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A CDI producer for {@link NamedCache}, {@link AsyncNamedCache} and {@link
 * ContinuousQueryCache} instances.
 *
 * @author Jonathan Knight  2019.10.22
 * @since 20.06
 */
@ApplicationScoped
@SuppressWarnings("unchecked")
public class NamedCacheProducer
    {
    // ---- constructors ----------------------------------------------------
    
    /**
     * Construct a {@link NamedCacheProducer} instance.
     *
     * @param beanManager            the CDI bean manager
     * @param filterProducer         the producer to use to obtain {@link
     *                               com.tangosol.util.Filter} instances
     * @param extractorProducer      the producer to use to obtain {@link
     *                               com.tangosol.util.ValueExtractor} instances
     * @param cdiMapListenerManager  the CDI map listener manager
     */
    @Inject
    NamedCacheProducer(BeanManager           beanManager,
                       FilterProducer        filterProducer,
                       ExtractorProducer     extractorProducer,
                       CdiMapListenerManager cdiMapListenerManager)
        {
        f_beanManager           = beanManager;
        f_filterProducer        = filterProducer;
        f_extractorProducer     = extractorProducer;
        f_cdiMapListenerManager = cdiMapListenerManager;
        }

    // ---- producer methods ------------------------------------------------
    
    /**
     * Produce an {@link AsyncNamedCache} using the injection point member name
     * for the cache name and the default {@link ConfigurableCacheFactory} as
     * the source.
     *
     * @param injectionPoint the injection point to inject the {@link AsyncNamedCache} into
     * @param <K>            the type of the cache keys
     * @param <V>            the type of the cache values
     *
     * @return an {@link AsyncNamedCache} using the injection point member name
     *         for the cache name and the default {@link ConfigurableCacheFactory}
     *         as the source
     */
    @Produces
    <K, V> AsyncNamedCache<K, V> getNonQualifiedAsyncNamedCache(InjectionPoint injectionPoint)
        {
        return getAsyncNamedCache(injectionPoint);
        }

    /**
     * Produce an {@link AsyncNamedCache} using the name from the {@link Name}
     * qualifier as the cache name and the name from the optional {@link
     * SessionName} qualifier to identify the source {@link
     * ConfigurableCacheFactory}.
     * <p>
     * If no {@link SessionName} qualifier is present the default {@link
     * ConfigurableCacheFactory} will be used as the source.
     *
     * @param injectionPoint the injection point to inject the {@link AsyncNamedCache} into
     * @param <K>            the type of the cache keys
     * @param <V>            the type of the cache values
     *
     * @return an {@link AsyncNamedCache} using the name from the {@link Name}
     *         qualifier as the cache name and the name from the optional
     *         {@link SessionName} qualifier
     */
    @Produces
    @Name("")
    @SessionName("")
    <K, V> AsyncNamedCache<K, V> getAsyncNamedCache(InjectionPoint injectionPoint)
        {
        NamedCache<K, V> cache = getCache(injectionPoint);
        return cache.async();
        }

    /**
     * Produce an {@link NamedCache} using the injection point member name for
     * the cache name and the default {@link ConfigurableCacheFactory} as the
     * source.
     *
     * @param injectionPoint  the injection point to inject the {@link NamedCache} into
     * @param <K>             the type of the cache keys
     * @param <V>             the type of the cache values
     *
     * @return a {@link NamedCache} using the injection point member name for
     *         the cache name and the default {@link ConfigurableCacheFactory}
     *         as the source
     */
    @Produces
    <K, V> NamedCache<K, V> getNonQualifiedNamedCache(InjectionPoint injectionPoint)
        {
        return getCache(injectionPoint);
        }

    /**
     * Produce an {@link NamedCache} using the name from the {@link Name}
     * qualifier as the cache name and the name from the optional {@link
     * SessionName} qualifier to identify the source {@link
     * ConfigurableCacheFactory}.
     * <p>
     * If no {@link SessionName} qualifier is present the default {@link
     * Session} will be used as the source.
     *
     * @param injectionPoint  the injection point to inject the {@link NamedCache} into
     * @param <K>             the type of the cache keys
     * @param <V>             the type of the cache values
     *
     * @return an {@link NamedCache} using the name from the {@link Name}
     *         qualifier as the cache name and the name from the optional
     *         {@link SessionName} qualifier
     */
    @Produces
    @Name("")
    @View
    @SessionName("")
    <K, V> NamedCache<K, V> getCache(InjectionPoint injectionPoint)
        {
        return getCacheInternal(injectionPoint, false);
        }

    /**
     * Produce an {@link com.tangosol.net.cache.ContinuousQueryCache} using the
     * injection point member name for the underlying cache name and the default
     * {@link com.tangosol.net.ConfigurableCacheFactory} as the source for the
     * underlying cache.
     * <p>
     * This producer method is annotated with {@link Typed} to restrict the
     * types produced to only the class {@link com.tangosol.net.cache.ContinuousQueryCache}.
     * This is so that injection points that are of a super class of CQC, for
     * example {@link com.tangosol.net.NamedCache} but are not annotated with
     * any other qualifier do not see an ambiguous producer.
     *
     * @param injectionPoint  the injection point to inject the {@link
     *                        com.tangosol.net.cache.ContinuousQueryCache} into
     * @param <K>             the type of the cache entry keys
     * @param <V_BACK>        the type of the entry values in the back cache that
     *                        is used as the source for this {@link
     *                        com.tangosol.net.cache.ContinuousQueryCache}
     * @param <V_FRONT>       the type of the entry values in this {@link
     *                        com.tangosol.net.cache.ContinuousQueryCache}, which
     *                        will be the same as {@code V_BACK}, unless a {@code
     *                        transformer} is specified when creating this {@link
     *                        com.tangosol.net.cache.ContinuousQueryCache}
     *
     * @return a {@link com.tangosol.net.cache.ContinuousQueryCache} using the
     *         injection point member name for the underlying cache name and
     *         the default {@link com.tangosol.net.ConfigurableCacheFactory} as
     *         the source for the underlying cache
     */
    @Produces
    @Typed(ContinuousQueryCache.class)
    <K, V_BACK, V_FRONT> ContinuousQueryCache<K, V_BACK, V_FRONT> getNonQualifiedCQC(InjectionPoint injectionPoint)
        {
        return getCQC(injectionPoint);
        }

    /**
     * Produce an {@link com.tangosol.net.cache.ContinuousQueryCache} using the
     * name from the {@link Name} qualifier as the
     * underlying cache name and the name from the optional {@link
     * SessionName} qualifier to identify the source
     * {@link com.tangosol.net.ConfigurableCacheFactory}.
     * <p>
     * If no {@link SessionName} qualifier is present
     * the default {@link com.tangosol.net.ConfigurableCacheFactory} will be
     * used as the source.
     *
     * @param injectionPoint  the injection point to inject the {@link
     *                        com.tangosol.net.cache.ContinuousQueryCache} into
     * @param <K>             the type of the cache entry keys
     * @param <V_BACK>        the type of the entry values in the back cache that
     *                        is used as the source for this {@link ContinuousQueryCache}
     * @param <V_FRONT>       the type of the entry values in this {@link
     *                        ContinuousQueryCache}, which will be the same as
     *                        {@code V_BACK}, unless a {@code transformer} is
     *                        specified when creating this {@link ContinuousQueryCache}
     *
     * @return an {@link com.tangosol.net.cache.ContinuousQueryCache} using the
     *         name from the {@link Name} qualifier
     *         as the name of the underlying cache and the name from the optional
     *         {@link SessionName} qualifier to identify
     *         the source {@link com.tangosol.net.ConfigurableCacheFactory}
     */
    @Produces
    @Name("")
    @View
    @SessionName("")
    @Typed(ContinuousQueryCache.class)
    <K, V_BACK, V_FRONT> ContinuousQueryCache<K, V_BACK, V_FRONT> getCQC(InjectionPoint injectionPoint)
        {
        return (ContinuousQueryCache<K, V_BACK, V_FRONT>) getCacheInternal(injectionPoint, true);
        }

    /**
     * Dispose of a {@link ContinuousQueryCache} bean.
     * <p>
     * Disposing of a {@link ContinuousQueryCache} will call {@link
     * ContinuousQueryCache#destroy()} which will destroy the {@link
     * ContinuousQueryCache} but not the underlying {@link NamedCache}.
     *
     * @param cqc        the {@link ContinuousQueryCache} to dispose
     * @param <K>        the type of the cache entry keys
     * @param <V_BACK>   the type of the entry values in the back cache that is
     *                   used as the source for this {@link ContinuousQueryCache}
     * @param <V_FRONT>  the type of the entry values in this {@link ContinuousQueryCache},
     *                   which will be the same as {@code V_BACK}, unless a
     *                   {@code transformer} is specified when creating this
     *                   {@link ContinuousQueryCache}
     */
    <K, V_BACK, V_FRONT> void destroyCQC(@Disposes ContinuousQueryCache<K, V_BACK, V_FRONT> cqc)
        {
        destroyQualifiedCQC(cqc);
        }

    /**
     * Dispose of a {@link ContinuousQueryCache} bean.
     * <p>
     * Disposing of a {@link ContinuousQueryCache} will call {@link
     * ContinuousQueryCache#destroy()} which will destroy the {@link
     * ContinuousQueryCache} but not the underlying {@link NamedCache}.
     *
     * @param cqc       the {@link ContinuousQueryCache} to dispose
     * @param <K>       the type of the cache entry keys
     * @param <V_BACK>  the type of the entry values in the back cache that is
     *                  used as the source for this {@link ContinuousQueryCache}
     * @param <V_FRONT> the type of the entry values in this {@link
     *                  ContinuousQueryCache}, which will be the same as {@code
     *                  V_BACK}, unless a {@code transformer} is specified when
     *                  creating this {@link ContinuousQueryCache}
     */
    <K, V_BACK, V_FRONT> void destroyQualifiedCQC(
            @Disposes @Name("") @View @SessionName("") ContinuousQueryCache<K, V_BACK, V_FRONT> cqc)
        {
        cqc.destroy();
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Create a {@link NamedCache} instance.
     * <p>
     * If the injection point has a type of {@link ContinuousQueryCache} or is
     * qualified with the {@link View} annotation then a {@link
     * ContinuousQueryCache} instance will be returned otherwise a {@link
     * NamedCache} will be returned.
     *
     * @param injectionPoint the {@link InjectionPoint} that the {@link NamedCache} will be injected into
     * @param fView          a flag specifying whether to return a {@link ContinuousQueryCache}
     * @param <K>            the type of the cache keys
     * @param <V>            the type of the cache values
     * @param <C>            the type of the {@link NamedCache} to return
     *
     * @return a {@link NamedCache} instance to inject into the injection point
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <K, V, C extends NamedCache<K, V>> C getCacheInternal(InjectionPoint injectionPoint, boolean fView)
        {
        String  sName        = null;
        String  sSession     = Coherence.DEFAULT_NAME;
        boolean fCacheValues = true;

        for (Annotation annotation : injectionPoint.getQualifiers())
            {
            if (Name.class.equals(annotation.annotationType()))
                {
                sName = ((Name) annotation).value();
                }
            else if (SessionName.class.equals(annotation.annotationType()))
                {
                sSession = ((SessionName) annotation).value();
                }
            else if (View.class.equals(annotation.annotationType()))
                {
                fView = true;
                fCacheValues = ((View) annotation).cacheValues();
                }
            }

        Member member = injectionPoint.getMember();
        if (sName == null || sName.trim().isEmpty())
            {
            if (member == null)
                {
                throw new DefinitionException(
                        "Cannot determine cache name. No @Cache qualifier and injection point member is null");
                }
            sName = member.getName();
            }

        String  sSessionName = sSession;
        Instance<Session> instance = f_beanManager.createInstance()
                .select(Session.class, Name.Literal.of(sSessionName));
        Session         session    = instance.get();
        Set<Annotation> qualifiers = extractQualifiers(injectionPoint);
        CacheId         cacheId    = new CacheId(sName, sSessionName);
        final C         cache      = (C) f_cacheInstances.compute(cacheId, (id, current) ->
            {
            if (current != null && current.isActive())
                {
                return current;
                }
            C c = (C) session.getCache(id.f_cacheName);
            f_cdiMapListenerManager.registerCacheListeners(id.f_cacheName, session.getScopeName(), id.f_sessionName, c.getCacheService().getInfo().getServiceName());
            return c;
            });

        if (fView)
            {
            final boolean fCacheVals = fCacheValues;
            CacheId       cqcId      = new CacheId(sName, sSessionName, qualifiers);
            return (C) f_cqcInstances.compute(cqcId, (id, current) ->
                {
                if (current != null && current.isActive())
                    {
                    return current;
                    }
                Filter         filter    = f_filterProducer.getFilter(injectionPoint);
                ValueExtractor extractor = f_extractorProducer.getValueExtractor(injectionPoint);
                return new ContinuousQueryCache<>(cache, filter, fCacheVals, null, extractor);
                });
            }
        return cache;
        }

    private Set<Annotation> extractQualifiers(InjectionPoint injectionPoint)
        {
        Annotated annotated = injectionPoint.getAnnotated();
        if (annotated == null)
            {
            return Collections.emptySet();
            }
        return annotated.getAnnotations().stream()
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(FilterBinding.class) 
                                      || annotation.annotationType().isAnnotationPresent(ExtractorBinding.class)
                                      || annotation.annotationType().isAnnotationPresent(View.class))
                .collect(Collectors.toSet());
        }

    /**
     * A cache identifier used as a key for Coherence caches.
     */
    static class CacheId 
        {
        // ---- constructors ----------------------------------------------------

        /**
         * Construct a CacheId.
         * 
         * @param cacheName    cache name
         * @param sessionName  session name
         */
        CacheId(String cacheName, String sessionName)
            {
            this(cacheName, sessionName, null);
            }

        /**
         * Construct a CacheId.
         *
         * @param cacheName    cache name
         * @param sessionName  session name
         * @param qualifiers   qualifiers
         */
        CacheId(String cacheName, String sessionName, Set<Annotation> qualifiers)
            {
            this.f_cacheName   = cacheName;
            this.f_sessionName = sessionName;
            this.f_qualifiers  = qualifiers;
            }

        /**
         * Return cache name.
         *
         * @return cache name
         */
        public String getCacheName()
            {
            return f_cacheName;
            }

        /**
         * Return the session name.
         *
         * @return the session name
         */
        public String getSessionName()
            {
            return f_sessionName;
            }

        /**
         * Return qualifiers.
         * 
         * @return the qualifiers
         */
        public Set<Annotation> getQualifiers()
            {
            return f_qualifiers;
            }

        // ----- Object methods -------------------------------------------------

        @Override
        public boolean equals(Object o)
            {
            if (!(o instanceof CacheId cacheId))
                {
                return false;
                }

            return Objects.equals(f_cacheName, cacheId.f_cacheName) 
                   && Objects.equals(f_sessionName, cacheId.f_sessionName) 
                   && Objects.equals(f_qualifiers, cacheId.f_qualifiers);
            }

        @Override
        public int hashCode()
            {
            int result = Objects.hashCode(f_cacheName);
            result = 31 * result + Objects.hashCode(f_sessionName);
            result = 31 * result + Objects.hashCode(f_qualifiers);
            return result;
            }

        // ---- data members ----------------------------------------------------

        /**
         * Cache name.
         */
        private final String f_cacheName;

        /**
         * Session name.
         */
        private final String f_sessionName;

        /**
         * Qualifiers.
         */
        private final Set<Annotation> f_qualifiers;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The cached instances of NamedCache.
     */
    private final Map<CacheId, NamedCache> f_cacheInstances = new ConcurrentHashMap<>();

    /**
     * The cached instances of ContinuousQueryCache.
     */
    private final Map<CacheId, ContinuousQueryCache> f_cqcInstances = new ConcurrentHashMap<>();

    /**
     * The CDI bean manager.
     */
    private final BeanManager f_beanManager;

    /**
     * The producer of {@link com.tangosol.util.Filter} instances.
     */
    private final FilterProducer f_filterProducer;

    /**
     * The producer of {@link com.tangosol.util.ValueExtractor} instances.
     */
    private final ExtractorProducer f_extractorProducer;

    /**
     * The CDI map listener manager.
     */
    private final CdiMapListenerManager f_cdiMapListenerManager;
    }
