/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.cdi.events.EventObserverSupport;
import com.tangosol.net.events.Event;

import javax.enterprise.inject.spi.ObserverMethod;
import javax.enterprise.inject.spi.ProcessObserverMethod;

import java.lang.annotation.Annotation;

import java.util.Set;

/**
 * An observer of events that wraps a CDI {@link ProcessObserverMethod}.
 *
 * @author Jonathan Knight  2020.11.07
 * @since 20.12
 */
@SuppressWarnings("rawtypes")
public class CdiEventObserver<E extends Event>
        implements EventObserverSupport.EventObserver<E>
    {
    /**
     * Create a {@link CdiEventObserver} from an observed method event.
     *
     * @param event  the observed method event
     */
    public CdiEventObserver(ProcessObserverMethod<E, ?> event)
        {
        this(event.getObserverMethod());
        }

    /**
     * Create a {@link CdiEventObserver} from an {@link ObserverMethod}
     *
     * @param method  the {@link ObserverMethod}
     */
    public CdiEventObserver(ObserverMethod<E> method)
        {
        f_method = method;
        }

    // ----- EventObserverSupport.EventObserver methods ---------------------
    @Override
    public String getId()
        {
        return f_method.toString();
        }

    @Override
    public void notify(E event)
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

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped CDI {@link ObserverMethod}.
     */
    private final ObserverMethod<E> f_method;
    }
