/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.AnnotatedMapListener;
import com.oracle.coherence.cdi.events.EventObserverSupport;

import com.tangosol.net.Coherence;
import com.tangosol.net.SessionProvider;

import com.tangosol.net.events.CoherenceLifecycleEvent;
import com.tangosol.net.events.NamedEventInterceptor;
import com.tangosol.net.events.SessionLifecycleEvent;
import com.tangosol.net.events.application.LifecycleEvent;
import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.MapEvent;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Priority;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;

import javax.enterprise.event.Observes;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.BeforeShutdown;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.enterprise.inject.spi.configurator.AnnotatedMethodConfigurator;
import javax.enterprise.inject.spi.configurator.AnnotatedTypeConfigurator;

/**
 * A Coherence CDI {@link Extension} that is used on both cluster members and
 * the clients.
 *
 * @author Jonathan Knight  2019.10.24
 * @author Aleks Seovic  2020.03.25
 *
 * @since 20.06
 */
public class CoherenceExtension
        implements Extension
    {
    // ---- client-side event support ---------------------------------------

    /**
     * Process observer methods for {@link CoherenceLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCoherenceLifecycleEventObservers(
            @Observes ProcessObserverMethod<CoherenceLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.CoherenceLifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link SessionLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processSessionLifecycleEventObservers(
            @Observes ProcessObserverMethod<SessionLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.SessionLifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link LifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processLifecycleEventObservers(
            @Observes ProcessObserverMethod<LifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.LifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link CacheLifecycleEvent}s.
     *
     * @param event  the event to process
     */
    private void processCacheLifecycleEventObservers(
            @Observes ProcessObserverMethod<CacheLifecycleEvent, ?> event)
        {
        m_listInterceptors.add(new EventObserverSupport.CacheLifecycleEventHandler(new CdiEventObserver<>(event)));
        }

    /**
     * Process observer methods for {@link MapEvent}s.
     *
     * @param event  the event to process
     * @param <K>    the type of {@code EntryEvent} keys
     * @param <V>    the type of {@code EntryEvent} values
     */
    private <K, V> void processMapEventObservers(@Observes ProcessObserverMethod<MapEvent<K, V>, ?> event)
        {
        m_listListener.add(new AnnotatedMapListener<>(new CdiMapEventObserver<>(event),
                                                      event.getAnnotatedMethod().getAnnotations()));
        }

    // ---- Filter and Extractor injection support --------------------------

    /**
     * Process {@link FilterFactory} beans annotated with {@link FilterBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends FilterFactory<?, ?>> void processFilterInjectionPoint(
            @Observes @WithAnnotations(FilterBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(FilterBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapFilterSupplier.put(a, type.getJavaClass()));
        }

    /**
     * Process {@link ExtractorFactory} beans annotated with
     * {@link ExtractorBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends ExtractorFactory<?, ?, ?>> void processValueExtractorInjectionPoint(
            @Observes @WithAnnotations(ExtractorBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(ExtractorBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapExtractorSupplier.put(a, type.getJavaClass()));
        }

    /**
     * Process {@link MapEventTransformerFactory} beans annotated with {@link MapEventTransformerBinding}.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    private <T extends MapEventTransformerFactory<?, ?, ?, ?>> void processMapEventTransformerInjectionPoint(
            @Observes @WithAnnotations(MapEventTransformerBinding.class) ProcessAnnotatedType<T> event)
        {
        AnnotatedType<T> type = event.getAnnotatedType();
        type.getAnnotations()
                .stream()
                .filter(a -> a.annotationType().isAnnotationPresent(MapEventTransformerBinding.class))
                .map(AnnotationInstance::create)
                .forEach(a -> m_mapMapEventTransformerSupplier.put(a, type.getJavaClass()));
        }

    /**
     * Process beans annotated with caching annotations.
     *
     * @param event  the event to process
     * @param <T>    the declared type of the injection point
     */
    <T> void processCacheAnnotatedTypes(@Observes @WithAnnotations({CacheGet.class, CacheAdd.class, CachePut.class, CacheRemove.class})
                                        ProcessAnnotatedType<T> event)
        {
        AnnotatedTypeConfigurator<T>                annotatedTypeConfigurator = event.configureAnnotatedType();
        AnnotatedType<T>                            annotatedType             = annotatedTypeConfigurator.getAnnotated();
        Set<AnnotatedMethodConfigurator<? super T>> methods                   = annotatedTypeConfigurator.methods();

        for (AnnotatedMethodConfigurator<? super T> methodConfigurator : methods)
            {
            AnnotatedMethod<? super T> method = methodConfigurator.getAnnotated();
            if (method.getAnnotation(CacheGet.class) != null)
                {
                String interceptorKey = makeInterceptorKey(method);
                m_mapInterceptorCache.put(interceptorKey, createInterceptorInfo(annotatedType, method));
                }
            if (method.getAnnotation(CacheAdd.class) != null)
                {
                String interceptorKey = makeInterceptorKey(method);
                m_mapInterceptorCache.put(interceptorKey, createInterceptorInfo(annotatedType, method));
                }
            if (method.getAnnotation(CachePut.class) != null)
                {
                String interceptorKey      = makeInterceptorKey(method);
                MethodInterceptorInfo info = createInterceptorInfo(annotatedType, method);
                if (info.getValueParameterIndex() == null)
                    {
                    throw new IllegalStateException("@CacheValue annotation is missing on a parameter on @CachePut method " + interceptorKey);
                    }
                m_mapInterceptorCache.put(interceptorKey, info);
                }
            if (method.getAnnotation(CacheRemove.class) != null)
                {
                String interceptorKey = makeInterceptorKey(method);
                m_mapInterceptorCache.put(interceptorKey, createInterceptorInfo(annotatedType, method));
                }
            }
        }

    /**
     * Create unique key based on the target method.
     *
     * @param method  target method
     *
     * @return  unique key
     *
     * @throws IllegalStateException if the same key already exists in interceptor cache
     */
    private String makeInterceptorKey(AnnotatedMethod<?> method)
        {
        String interceptorKey = methodCacheKey(method.getJavaMember());
        if (m_mapInterceptorCache.containsKey(interceptorKey))
            {
            throw new IllegalStateException("Multiple cache annotation are not allowed on a method " + interceptorKey);
            }
        return interceptorKey;
        }

    /**
     * Capture all cache related metadata from the target type and method.
     *
     * @param annotatedType  target type
     * @param method         target method
     *
     * @return the captured metadata
     */
    private MethodInterceptorInfo createInterceptorInfo(Annotated annotatedType, AnnotatedMethod<?> method)
        {
        String                     cacheNameDef        = CdiHelpers.cacheName(annotatedType, method);
        String                     sessionNameDef      = CdiHelpers.sessionName(annotatedType, method);
        Function<Object[], Object> cacheKeyFunction    = CdiHelpers.cacheKeyFunction(method);
        Integer                    valueParameterIndex = CdiHelpers.annotatedParameterIndex(method.getParameters(), CacheValue.class);
        return new MethodInterceptorInfo(cacheNameDef, sessionNameDef, cacheKeyFunction, valueParameterIndex);
        }

    /**
     * A class that stores necessary method related data for caching interceptors.
     */
    static class MethodInterceptorInfo
        {

        /**
         * Constructs {@link MethodInterceptorInfo}.
         *
         * @param cacheName            the cache name
         * @param sessionName          the session name
         * @param cacheKeyFunction     the cache key function
         * @param valueParameterIndex  the parameter index of the cache value
         */
        MethodInterceptorInfo(String cacheName, String sessionName, Function<Object[], Object> cacheKeyFunction, Integer valueParameterIndex)
            {
            m_cacheName           = cacheName;
            m_cacheKeyFunction    = cacheKeyFunction;
            m_sessionName         = sessionName;
            m_valueParameterIndex = valueParameterIndex;
            }

        /**
         * Returns the cache name.
         *
         * @return  the cache name
         */
        String cacheName()
            {
            return m_cacheName;
            }

        /**
         * Return the session name.
         *
         * @return  session name
         */
        String sessionName()
            {
            return m_sessionName;
            }

        /**
         * Return the cache key function.
         *
         * @return  cache key function
         */
        Function<Object[], Object> cacheKeyFunction()
            {
            return m_cacheKeyFunction;
            }

        /**
         * Return the parameter index of the cache value.
         *
         * @return  the parameter index of the cache value
         */
        public Integer getValueParameterIndex()
            {
            return m_valueParameterIndex;
            }

        /**
         * The name of the cache.
         */
        private final String m_cacheName;

        /**
         * The name of the session.
         */
        private final String m_sessionName;

        /**
         * The cache key function.
         */
        private final Function<Object[], Object> m_cacheKeyFunction;

        /**
         * The parameter index of the cache value.
         */
        private final Integer m_valueParameterIndex;
        }

    /**
     * Return {@link MethodInterceptorInfo} for specified method.
     *
     * @param method  the target method
     *
     * @return interceptor info
     */
    MethodInterceptorInfo interceptorInfo(Method method)
        {
        return m_mapInterceptorCache.get(methodCacheKey(method));
        }

    private String methodCacheKey(Method method)
        {
        return method.getDeclaringClass().getName()
               + "." + method.getName()
               + "(" + Arrays.toString(method.getParameterTypes()) + ")";
        }

    /**
     * Register caching annotations as interceptor binding types.
     *
     * @param event  the event to use for annotation registration
     */
    void registerInterceptorBindings(@Observes BeforeBeanDiscovery event)
        {
        event.addInterceptorBinding(CacheGet.class);
        event.addAnnotatedType(CacheGetInterceptor.class, CacheGetInterceptor.class.getName())
                .add(CacheGet.Literal.INSTANCE)
                .add(Dependent.Literal.INSTANCE);
        event.addInterceptorBinding(CacheAdd.class);
        event.addAnnotatedType(CacheAddInterceptor.class, CacheAddInterceptor.class.getName())
                .add(CacheAdd.Literal.INSTANCE)
                .add(Dependent.Literal.INSTANCE);
        event.addInterceptorBinding(CachePut.class);
        event.addAnnotatedType(CachePutInterceptor.class, CachePutInterceptor.class.getName())
                .add(CachePut.Literal.INSTANCE)
                .add(Dependent.Literal.INSTANCE);
        event.addInterceptorBinding(CacheRemove.class);
        event.addAnnotatedType(CacheRemoveInterceptor.class, CacheRemoveInterceptor.class.getName())
                .add(CacheRemove.Literal.INSTANCE)
                .add(Dependent.Literal.INSTANCE);
        }

    /**
     * Register dynamic beans for this extension.
     *
     * @param event  the event to use for dynamic bean registration
     */
    private void addBeans(@Observes final AfterBeanDiscovery event)
        {
        // Register the filter producer bean that knows about all of the FilterFactory beans
        FilterProducer.FilterFactoryResolver filterResolver = new FilterProducer.FilterFactoryResolver(m_mapFilterSupplier);
        event.addBean()
                .produceWith(i -> filterResolver)
                .types(FilterProducer.FilterFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(FilterProducer.FilterFactoryResolver.class);

        // Register the value extractor producer bean that knows about all of the ValueExtractorFactory beans
        ExtractorProducer.ValueExtractorFactoryResolver extractorResolver
                = new ExtractorProducer.ValueExtractorFactoryResolver(m_mapExtractorSupplier);
        event.addBean()
                .produceWith(i -> extractorResolver)
                .types(ExtractorProducer.ValueExtractorFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(ExtractorProducer.ValueExtractorFactoryResolver.class);

        // Register the MapEventTransformer producer bean that knows about all of the MapEventTransformerFactory beans
        MapEventTransformerProducer.MapEventTransformerFactoryResolver mapEventTransformerResolver
                = new MapEventTransformerProducer.MapEventTransformerFactoryResolver(m_mapMapEventTransformerSupplier);
        event.addBean()
                .produceWith(i -> mapEventTransformerResolver)
                .types(MapEventTransformerProducer.MapEventTransformerFactoryResolver.class)
                .qualifiers(Default.Literal.INSTANCE)
                .scope(ApplicationScoped.class)
                .beanClass(MapEventTransformerProducer.MapEventTransformerFactoryResolver.class);
        }

    /**
     * Returns the discovered map listeners.
     *
     * @return the discovered map listeners
     */
    public List<AnnotatedMapListener<?, ?>> getMapListeners()
        {
        return m_listListener;
        }

    // ---- lifecycle support -----------------------------------------------

    /**
     * Start {@link Coherence} instance and wait for all services to start.
     *
     * @param event the event fired once the CDI container is initialized
     */
    @SuppressWarnings("unused")
    synchronized void startServer(@Observes @Priority(1) @Initialized(ApplicationScoped.class)
                                  Object event, BeanManager beanManager)
        {
        m_coherence = ensureCoherence(beanManager);
        }

    /**
     * Return the list of discovered event observers.
     *
     * @return the list of discovered event observers
     */
    List<EventObserverSupport.EventHandler<?, ?>> getInterceptors()
        {
        return m_listInterceptors;
        }

    /**
     * Ensure that a {@link Coherence} bean is resolvable and started.
     *
     * @param beanManager  the bean manager to use to resolve the {@link Coherence} bean
     *
     * @return  the {@link Coherence} bean
     * @throws IllegalStateException if no {@link Coherence} bean is resolvable
     */
    public static Coherence ensureCoherence(BeanManager beanManager)
        {
        Instance<Coherence> instance = beanManager.createInstance()
                .select(Coherence.class, Name.Literal.of(Coherence.DEFAULT_NAME));

        if (instance.isResolvable())
            {
            return instance.get();
            }

        throw new IllegalStateException("Cannot resolve default Coherence instance");
        }

    /**
     * Stop the {@link Coherence} instance.
     *
     * @param event the event fired before the CDI container is shut down
     */
    @SuppressWarnings("unused")
    synchronized void stopServer(@Observes BeforeShutdown event)
        {
        SessionProvider.get().close();
        if (m_coherence != null)
            {
            m_coherence.close();
            }
        Coherence.closeAll();
        }

    /**
     * Returns the {@link Coherence} instance started by the extension.
     *
     * @return  the {@link Coherence} instance started by the extension
     */
    public Coherence getCoherence()
        {
        return m_coherence;
        }

    // ----- inner interface: InterceptorProvider ---------------------------

    /**
     * A provider of {@link NamedEventInterceptor} instances.
     */
    public interface InterceptorProvider
        {
        /**
         * Returns the {@link NamedEventInterceptor} instances.
         *
         * @return  the {@link NamedEventInterceptor} instances
         */
        Iterable<NamedEventInterceptor<?>> getInterceptors();
        }

    // ---- data members ----------------------------------------------------

    /**
     * A map of {@link FilterBinding} annotation to {@link FilterFactory} bean
     * class.
     */
    private final Map<AnnotationInstance, Class<? extends FilterFactory<?, ?>>> m_mapFilterSupplier = new HashMap<>();

    /**
     * A map of {@link ExtractorBinding} annotation to {@link
     * ExtractorFactory} bean class.
     */
    private final Map<AnnotationInstance, Class<? extends ExtractorFactory<?, ?, ?>>>
            m_mapExtractorSupplier = new HashMap<>();

    /**
     * A map of {@link MapEventTransformerBinding} annotation to {@link MapEventTransformerFactory} bean class.
     */
    private final Map<AnnotationInstance, Class<? extends MapEventTransformerFactory<?, ?, ?, ?>>>
            m_mapMapEventTransformerSupplier = new HashMap<>();

    /**
     * The {@link Coherence} instance.
     */
    private Coherence m_coherence;

    /**
     * A list of event interceptors for all discovered observer methods.
     */
    private final List<EventObserverSupport.EventHandler<?, ?>> m_listInterceptors = new ArrayList<>();

    /**
     * A list of discovered map listeners.
     */
    private final List<AnnotatedMapListener<?, ?>> m_listListener = new ArrayList<>();

    /**
     * A map of {@link MethodInterceptorInfo} defined for each cache annotated
     * method.
     */
    private final Map<String, MethodInterceptorInfo> m_mapInterceptorCache = new HashMap<>();
    }
