/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.util.MapEventTransformer;

import java.lang.annotation.Annotation;

/**
 * A factory that produces instances of {@link MapEventTransformer}
 * for a given {@link Annotation}.
 * <p>
 * A {@link MapEventTransformerFactory} is normally a CDI
 * bean that is also annotated with a {@link MapEventTransformerBinding}
 * annotation. Whenever an injection point annotated with the corresponding
 * {@link MapEventTransformerBinding} annotation is encountered the
 * {@link MapEventTransformerFactory} bean's
 * {@link MapEventTransformerFactory#create(Annotation)}
 * method is called to create an instance of a {@link MapEventTransformer}.
 *
 * @param <A> the annotation type that the factory supports
 * @param <K> the type of the event's key
 * @param <V> the type of event's value
 * @param <U> the type of resulting transformed value
 *
 * @author Jonathan Knight  2020.06.16
 * @since 20.06
 */
public interface MapEventTransformerFactory<A extends Annotation, K, V, U>
    {
    /**
     * Create a {@link MapEventTransformer} instance.
     *
     * @param annotation the {@link Annotation} that
     *                   defines the MapEventTransformer
     *
     * @return a {@link MapEventTransformer} instance
     */
    MapEventTransformer<K, V, U> create(A annotation);
    }
