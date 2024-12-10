/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.function.Remote.Predicate;

import java.util.ArrayList;
import java.util.List;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * An abstract {@link Task.Collector}.
 *
 * @param <T>  the type of input elements to the reduction operation
 * @param <R>  the result type of the reduction operation
 *
 * @author bo
 * @since 21.12
 */
public abstract class AbstractCollector<T, R>
        implements Task.Collector<T, List<T>, R>, PortableObject
    {
    // ----- Task.Collector interface ---------------------------------------

    @Override
    public BiConsumer<List<T>, T> accumulator()
        {
        return List::add;
        }

    @Override
    public Predicate<List<T>> finishable()
        {
        return Predicates.never();
        }

    @Override
    public Supplier<List<T>> supplier()
        {
        return ArrayList::new;
        }
    }
