/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.CacheName;
import com.oracle.coherence.cdi.events.Deleted;
import com.oracle.coherence.cdi.events.Inserted;
import com.oracle.coherence.cdi.events.Lite;
import com.oracle.coherence.cdi.events.MapName;
import com.oracle.coherence.cdi.events.ScopeName;
import com.oracle.coherence.cdi.events.ServiceName;
import com.oracle.coherence.cdi.events.Synchronous;
import com.oracle.coherence.cdi.events.Updated;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapEventTransformer;
import com.tangosol.util.MapListener;

import java.lang.annotation.Annotation;
import java.util.EnumSet;

import java.util.Set;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.ObserverMethod;

/**
 * {@link MapListener} implementation that dispatches {@code MapEvent}s
 * to a CDI observer.
 *
 * @author Aleks Seovic  2020.04.14
 * @since 20.06
 */
public class CdiMapListener<K, V>
        implements MapListener<K, V>
    {
    CdiMapListener(ObserverMethod<MapEvent<K, V>> observer, Set<Annotation> annotations)
        {
        m_observer = observer;

        String sCache   = "*";
        String sService = "*";
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
     *
     * @param producer  the {@link FilterProducer} to use to resolve the {@link Filter}
     */
    public void resolveFilter(FilterProducer producer)
        {
        if (m_setAnnFilter != null && !m_setAnnFilter.isEmpty())
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
     *
     * @param producer  the {@link MapEventTransformerProducer} to use to resolve
     *                  the {@link MapEventTransformer}
     */
    public void resolveTransformer(MapEventTransformerProducer producer)
        {
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
            m_observer.notify(event);
            }
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

    // ---- data members ----------------------------------------------------

    /**
     * CDI event observer for this listener.
     */
    private final ObserverMethod<MapEvent<K, V>> m_observer;

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
