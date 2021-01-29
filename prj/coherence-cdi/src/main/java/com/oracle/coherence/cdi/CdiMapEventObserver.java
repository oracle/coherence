/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.AnnotatedMapListener;
import com.tangosol.util.MapEvent;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;
import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * An implementation of a {@link AnnotatedMapListener.MapEventObserver}
 * that wraps a CDI {@link ObserverMethod} that observes {@link MapEvent}.
 *
 * @author Jonathan Knight  2020.11.20
 */
public class CdiMapEventObserver<K, V>
        implements AnnotatedMapListener.MapEventObserver<K, V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link CdiMapEventObserver} from an observed method event.
     *
     * @param event  the observed method event
     */
    public CdiMapEventObserver(ProcessObserverMethod<MapEvent<K, V>, ?> event)
        {
        this(event.getObserverMethod());
        }

    /**
     * Create a {@link CdiMapEventObserver} from an {@link ObserverMethod}
     *
     * @param method  the {@link ObserverMethod}
     */
    public CdiMapEventObserver(ObserverMethod<MapEvent<K, V>> method)
        {
        f_method = method;
        }

    // ----- ObserverMapListener.MapEventObserver methods -------------------

    @Override
    public void notify(MapEvent<K, V> event)
        {
        f_method.notify(event);
        }

    @Override
    public boolean isAsync()
        {
        return f_method.isAsync();
        }

    @Override
    public Set<Annotation> getObservedQualifiers()
        {
        return f_method.getObservedQualifiers();
        }

    // ----- object methods -------------------------------------------------

    @Override
    public String toString()
        {
        return "CdiMapEventObserver{" +
                "method=" + f_method +
                '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped CDI {@link ObserverMethod}.
     */
    private final ObserverMethod<MapEvent<K, V>> f_method;
    }
