/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi;

import com.tangosol.util.Filter;

import java.lang.annotation.Annotation;

/**
 * A factory that produces instances of {@link com.tangosol.util.Filter} for a
 * given {@link java.lang.annotation.Annotation}.
 * <p>
 * A {@link FilterFactory} is normally a CDI bean that is also annotated with a
 * {@link FilterBinding} annotation. Whenever an injection point annotated with
 * the corresponding {@link FilterBinding} annotation is encountered the {@link
 * FilterFactory} bean's {@link FilterFactory#create(java.lang.annotation.Annotation)}
 * method is called to create an instance of a {@link com.tangosol.util.Filter}.
 *
 * @param <A> the annotation type that the factory supports
 * @param <T> the type of value being filtered
 *
 * @author Jonathan Knight  2019.10.24
 * @since 20.06
 */
public interface FilterFactory<A extends Annotation, T>
    {
    /**
     * Create a {@link Filter} instance.
     *
     * @param annotation the {@link Annotation} that defines the filter
     *
     * @return a {@link Filter} instance
     */
    public Filter<T> create(A annotation);
    }
