/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor.util;

import com.tangosol.util.function.Remote.Predicate;

import java.util.Iterator;

/**
 * An {@link Iterable} that filters elements produced by another {@link Iterable} based on a specified {@link
 * Predicate}.
 *
 * @param <T>  the type being iterated
 *
 * @author bo
 * @since 21.12
 */
public class FilteringIterable<T>
        implements Iterable<T>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link FilteringIterable}.
     *
     * @param iterable  the {@link Iterable} producing elements to filter
     * @param predicate the {@link Predicate} that must be satisfied
     */
    public FilteringIterable(Iterable<T> iterable,
                             Predicate<? super T> predicate)
        {
        f_iterable  = iterable;
        f_predicate = predicate;
        }

    // ----- Iterable interface ---------------------------------------------

    @Override
    public Iterator<T> iterator()
        {
        return new FilteringIterator<>(f_iterable.iterator(), f_predicate);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying {@link Iterable}, producing elements to filter.
     */
    private final Iterable<T> f_iterable;

    /**
     * The {@link Predicate} that must be satisfied for an element to be returned by {@link Iterator}s produced by this
     * {@link Iterable}.
     */
    private final Predicate<? super T> f_predicate;
    }
