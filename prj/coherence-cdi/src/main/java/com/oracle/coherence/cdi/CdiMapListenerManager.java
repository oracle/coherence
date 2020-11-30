/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.oracle.coherence.event.AnnotatedMapListenerManager;
import com.oracle.coherence.event.Created;

import com.tangosol.net.events.partition.cache.CacheLifecycleEvent;

import com.tangosol.util.MapListener;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.event.Observes;

import javax.inject.Inject;

/**
 * Registers discovered CDI observer-based {@link MapListener}s when the cache is
 * created, and unregisters them when it's destroyed.
 *
 * @author Aleks Seovic  2020.06.09
 * @since 20.06
 */
@ApplicationScoped
public class CdiMapListenerManager
        extends AnnotatedMapListenerManager
    {
    @Inject
    public CdiMapListenerManager(FilterProducer              filterProducer,
                                 MapEventTransformerProducer transformerProducer,
                                 CoherenceExtension          extension)
        {
        super(filterProducer, transformerProducer);
        extension.getMapListeners().forEach(this::addMapListener);
        }

    private void registerCacheListeners(@Observes @Created CacheLifecycleEvent event)
        {
        registerListeners(event.getCacheName(), event.getScopeName(), event.getSessionName(), event.getServiceName());
        }
    }
