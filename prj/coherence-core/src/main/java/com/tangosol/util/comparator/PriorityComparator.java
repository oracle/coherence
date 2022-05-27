/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.comparator;


import javax.annotation.Priority;

import java.util.Comparator;
import java.util.ServiceLoader;

import java.util.function.Function;

/**
 * A {@link Comparator} that orders values that are annotated with
 * the {@link Priority} annotation.
 *
 * @param <T>  the type to be ordered
 *
 * @author Jonathan Knight  2022.05.25
 * @since 22.06
 */
public class PriorityComparator<T>
        implements Comparator<T>
    {
    public PriorityComparator(Function<T, Priority> function, int nDefault)
        {
        f_function = function;
        f_nDefault = nDefault;
        }

    @Override
    public int compare(T o1, T o2)
        {
        Priority priority1 = f_function.apply(o1);
        int      nValue1   = priority1 == null ? f_nDefault : priority1.value();
        Priority priority2 = f_function.apply(o2);
        int      nValue2   = priority2 == null ? f_nDefault : priority2.value();
        return Integer.compare(nValue1, nValue2);
        }

    /**
     * Returns a {@link Comparator} that can sort {@link ServiceLoader.Provider} instances
     * based on whether the class they provide is annotated with the {@link Priority}
     * annotation.
     *
     * @param <S> the type of service provided
     *
     * @return a {@link Comparator} to sort {@link Priority}
     *         annotated {@link ServiceLoader.Provider} instances
     */
    public static <S> Comparator<ServiceLoader.Provider<S>> forServiceLoader()
        {
        try
            {
            return new PriorityComparator<>(p -> p.type().getAnnotation(Priority.class), 0);
            }
        catch (NoClassDefFoundError | IllegalAccessError e)
            {
            // The Priority class is not accessible so we cannot sort using it
            return (o1, o2) -> 0;
            }
        }

    @Override
    public Comparator<T> reversed()
        {
        return Comparator.super.reversed();
        }

    private final Function<T, Priority> f_function;

    private final int f_nDefault;
    }
