/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

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
    implements CdiInterceptorSupport.EventObserver<E>
    {
    public CdiEventObserver(ProcessObserverMethod<E, ?> event)
        {
        this(event.getObserverMethod());
        }

    public CdiEventObserver(ObserverMethod<E> method)
        {
        m_method = method;
        }

    @Override
    public String getId()
        {
        return m_method.toString();
        }

    @Override
    public void notify(E event)
        {
        m_method.notify(event);
        }

    @Override
    public boolean isAsync()
        {
        return m_method.isAsync();
        }

    @Override
    public Set<Annotation> getObservedQualifiers()
        {
        return m_method.getObservedQualifiers();
        }

    private final ObserverMethod<E> m_method;
    }
