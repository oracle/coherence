/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.event;

import com.oracle.coherence.inject.ExtractorBinding;
import com.oracle.coherence.inject.FilterBinding;
import com.oracle.coherence.inject.MapEventTransformerBinding;
import com.oracle.coherence.inject.SessionName;

import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapListener;

import java.lang.annotation.Annotation;

import java.util.EnumSet;
import java.util.Set;

import java.util.concurrent.CompletableFuture;

import java.util.stream.Collectors;

/**
 * {@link MapListener} implementation that dispatches {@code MapEvent}s
 * to a CDI observer.
 *
 * @author Aleks Seovic  2020.04.14
 * @since 20.06
 */
public class AnnotatedMapListener<K, V>
        implements MapListener<K, V>
    {
    public AnnotatedMapListener(MapEventObserver<K, V> observer, Set<Annotation> annotations)
        {
        m_observer = observer;

        String sCache   = WILD_CARD;
        String sService = WILD_CARD;
        String sScope   = null;

        for (Annotation a : observer.getObservedQualifiers())
            {
            if (a instanceof CacheName)
                {
                sCache = ((CacheName) a).value();
                }
            else if (a instanceof MapName)
                {
                sCache = ((MapName) a).value();
                }
            else if (a instanceof ServiceName)
                {
                sService = ((ServiceName) a).value();
                }
            else if (a instanceof ScopeName)
                {
                sScope = ((ScopeName) a).value();
                }
            else if (a instanceof Inserted)
                {
                addType(Type.INSERTED);
                }
            else if (a instanceof Updated)
                {
                addType(Type.UPDATED);
                }
            else if (a instanceof Deleted)
                {
                addType(Type.DELETED);
                }
            else if (a instanceof SessionName)
                {
                m_sSession = ((SessionName) a).value();
                }
            }

        if (annotations.contains(Lite.Literal.INSTANCE))
            {
            m_fLite = true;
            }
        if (annotations.contains(Synchronous.Literal.INSTANCE))
            {
            m_fSync = true;
            }

        m_setAnnFilter = annotations.stream()
                .filter(a -> a.annotationType().isAnnotationPresent(FilterBinding.class))
                .collect(Collectors.toSet());

        m_setAnnExtractor = annotations.stream()
                .filter(a -> a.annotationType().isAnnotationPresent(ExtractorBinding.class))
                .collect(Collectors.toSet());

        m_setAnnTransformer = annotations.stream()
                .filter(a -> a.annotationType().isAnnotationPresent(MapEventTransformerBinding.class))
                .collect(Collectors.toSet());

        m_sCacheName   = sCache;
        m_sServiceName = sService;
        m_sScopeName   = sScope;
        }

    // ---- MapListener interface -------------------------------------------

    @Override
    public void entryInserted(MapEvent<K, V> event)
        {
        handle(Type.INSERTED, event);
        }

    @Override
    public void entryUpdated(MapEvent<K, V> event)
        {
        handle(Type.UPDATED, event);
        }

    @Override
    public void entryDeleted(MapEvent<K, V> event)
        {
        handle(Type.DELETED, event);
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return the name of the session that this listener is for
     *
     * @return  the name of the session this listener is for
     */
    public String getSessionName()
        {
        return m_sSession;
        }

    /**
     * Returns {@code true} if this listener has a filter annotation to resolve.
     *
     * @return  {@code true} if this listener has a filter annotation to resolve
     */
    public boolean hasFilterAnnotation()
        {
        return m_setAnnFilter != null && !m_setAnnFilter.isEmpty();
        }

    /**
     * Resolve this listener's filter annotation into a {@link Filter} instance.
     * <p>
     * If this listener's filter has already been resolved this operation is a no-op.
     *
     * @param producer  the {@link FilterProducer} to use to resolve the {@link Filter}
     */
    public void resolveFilter(FilterProducer producer)
        {
        if (m_filter == null && m_setAnnFilter != null && !m_setAnnFilter.isEmpty())
            {
            m_filter = producer.resolve(m_setAnnFilter);
            }
        }

    /**
     * Returns {@code true} if this listener has a transformer annotation to resolve.
     *
     * @return  {@code true} if this listener has a transformer annotation to resolve
     */
    public boolean hasTransformerAnnotation()
        {
        return !m_setAnnTransformer.isEmpty() || !m_setAnnExtractor.isEmpty();
        }

    /**
     * Resolve this listener's transformer annotation into a {@link MapEventTransformer} instance.
     * <p>
     * If this listener's transformer has already been resolved this method is a no-op
     *
     * @param producer  the {@link MapEventTransformerProducer} to use to resolve
     *                  the {@link MapEventTransformer}
     */
    public void resolveTransformer(MapEventTransformerProducer producer)
        {
        if (m_transformer != null)
            {
            return;
            }

        if (!m_setAnnTransformer.isEmpty())
            {
            m_transformer = producer.resolve(m_setAnnTransformer);
            }
        else if (!m_setAnnExtractor.isEmpty())
            {
            m_transformer = producer.resolve(m_setAnnExtractor);
            }
        }

    /**
     * Obtain the {@link Filter} that should be used when registering this listener.
     *
     * @return the {@link Filter} that should be used when registering this listener
     */
    public Filter<?> getFilter()
        {
        return m_filter;
        }

    /**
     * Obtain the {@link MapEventTransformer} that should be used when registering this listener.
     *
     * @return the {@link MapEventTransformer} that should be used when registering this listener
     */
    @SuppressWarnings("rawtypes")
    public MapEventTransformer getTransformer()
        {
        return m_transformer;
        }

    /**
     * Return the name of the cache this listener is for, or {@code '*'} if
     * it should be registered regardless of the cache name.
     *
     * @return the name of the cache this listener is for
     */
    public String getCacheName()
        {
        return m_sCacheName;
        }

    /**
     * Return {@code true} if this listener is for a wild-card cache name.
     *
     * @return  {@code true} if this listener is for a wild-card cache name
     */
    public boolean isWildCardCacheName()
        {
        return WILD_CARD.equals(m_sCacheName);
        }

    /**
     * Return the name of the service this listener is for, or {@code '*'} if
     * it should be registered regardless of the service name.
     *
     * @return the name of the cache this listener is for
     */
    public String getServiceName()
        {
        return m_sServiceName;
        }

    /**
     * Return {@code true} if this listener is for a wild-card cache name.
     *
     * @return  {@code true} if this listener is for a wild-card cache name
     */
    public boolean isWildCardServiceName()
        {
        return WILD_CARD.equals(m_sServiceName);
        }

    /**
     * Return the name of the scope this listener is for, or {@code null} if
     * it should be registered regardless of the scope name.
     *
     * @return the name of the cache this listener is for
     */
    public String getScopeName()
        {
        return m_sScopeName;
        }

    /**
     * Return {@code true} if this is lite event listener.
     *
     * @return {@code true} if this is lite event listener
     */
    public boolean isLite()
        {
        return m_fLite;
        }

    /**
     * Return {@code true} if this is synchronous event listener.
     *
     * @return {@code true} if this is synchronous event listener
     */
    public boolean isSynchronous()
        {
        return m_fSync;
        }

    /**
     * Add specified event type to a set of types this interceptor should handle.
     *
     * @param type  the event type to add
     */
    private void addType(Type type)
        {
        m_setTypes.add(type);
        }

    /**
     * Return {@code true} if this listener should handle events of the specified
     * type.
     *
     * @param type  the type to check
     *
     * @return {@code true} if this listener should handle events of the specified
     *         type
     */
    private boolean isSupported(Type type)
        {
        return m_setTypes.isEmpty() || m_setTypes.contains(type);
        }

    /**
     * Notify the observer that the specified event occurred, if the event type
     * is supported.
     *
     * @param type   the event type
     * @param event  the event
     */
    private void handle(Type type, MapEvent<K, V> event)
        {
        if (isSupported(type))
            {
            if (m_observer.isAsync())
                {
                CompletableFuture.supplyAsync(() ->
                                              {
                                              m_observer.notify(event);
                                              return event;
                                              });
                }
            else
                {
                m_observer.notify(event);
                }
            }
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "CdiMapListener{" +
                "cacheName='" + m_sCacheName + '\'' +
                ", serviceName='" + m_sServiceName + '\'' +
                ", scopeName='" + m_sScopeName + '\'' +
                ", session='" + m_sSession + '\'' +
                '}';
        }

    // ---- inner enum: Type ------------------------------------------------

    /**
     * Event type enumeration.
     */
    enum Type
        {
        INSERTED,
        UPDATED,
        DELETED
        }

    // ----- inner interface MapEventObserver -------------------------------

    public interface MapEventObserver<K, V>
        {
        /**
         * Process an event.
         *
         * @param event  the event
         */
        void notify(MapEvent<K, V> event);

        /**
         * Return {@code true} if this observer should be async.
         *
         * @return  {@code true} if this observer should be async
         */
        boolean isAsync();

        /**
         * Return the qualifiers for the observer that wil be
         * used to further qualify which events are received.
         *
         * @return  the qualifiers for the observer
         */
        Set<Annotation> getObservedQualifiers();
        }

    // ----- inner interface FilterProducer ---------------------------------

    /**
     * A producer of {@link Filter} instances.
     */
    @FunctionalInterface
    public interface FilterProducer
        {
        /**
         * Produce a {@link Filter} instance from a set of annotations.
         *
         * @param annotations  the annotations to use to produce the {@link Filter}
         *
         * @return an instance of a {@link Filter}
         */
        <T> Filter<T> resolve(Set<Annotation> annotations);
        }

    // ----- inner interface MapEventTransformerProducer --------------------

    /**
     * A producer of {@link MapEventTransformer} instances.
     */
    @FunctionalInterface
    public interface MapEventTransformerProducer
        {
        /**
         * Produce a {@link MapEventTransformer} instance from a set of annotations.
         *
         * @param annotations  the annotations to use to produce the {@link MapEventTransformer}
         *
         * @return an instance of a {@link MapEventTransformer}
         */
        <K, V, U> MapEventTransformer<K, V, U> resolve(Set<Annotation> annotations);
        }

    // ---- constants -------------------------------------------------------

    /**
     * The wild-card value for cache and service names;
     */
    public static final String WILD_CARD = "*";

    // ---- data members ----------------------------------------------------

    /**
     * The event observer for this listener.
     */
    private final MapEventObserver<K, V> m_observer;

    /**
     * The name of the cache to observe map events for.
     */
    private final String m_sCacheName;

    /**
     * The name of the cache service owing the cache to observe map events for.
     */
    private final String m_sServiceName;

    /**
     * The scope name of the cache factory owning the cache to observer map events for.
     */
    private final String m_sScopeName;

    /**
     * The types of map event to observe.
     */
    private final EnumSet<Type> m_setTypes = EnumSet.noneOf(Type.class);

    /**
     * The optional annotation specifying the filter to use to filter events.
     */
    private final Set<Annotation> m_setAnnFilter;

    /**
     * The optional annotations specifying the map event transformers to use to
     * transform observed map events.
     */
    private final Set<Annotation> m_setAnnTransformer;

    /**
     * The optional annotations specifying the value extractors to use to
     * transform observed map events.
     */
    private final Set<Annotation> m_setAnnExtractor;

    /**
     * The name of the session if this listener is for a resource
     * managed by a specific session or {@code null} if this listener
     * is for a resource in any session.
     */
    private String m_sSession;

    /**
     * A flag indicating whether to subscribe to lite-events.
     */
    private boolean m_fLite;

    /**
     * A flag indicating whether the observer is synchronous.
     */
    private boolean m_fSync;

    /**
     * An optional {@link Filter} to use to filter observed map events.
     */
    private Filter<?> m_filter;

    /**
     * An optional {@link MapEventTransformer} to use to transform observed map events.
     */
    private MapEventTransformer<K, V, ?> m_transformer;
    }
