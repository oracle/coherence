/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.util.Filters;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.util.Comparator;

/**
 * An annotation that should be applied to accessor methods for the
 * properties that need to be indexed.
 * <p/>
 * Indexing significantly improves query and aggregation performance by both
 * avoiding deserialization of stored entities and optimizing query execution,
 * so in general, any property that is used for querying should be indexed.
 *
 * @author Aleks Seovic  2021.02.12
 * @since 21.06
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Indexed
    {
    /**
     * Determines whether the index should be ordered or not (default).
     * <p/>
     * Ordered indices are more effective with range queries, such as
     * {@link Filters#greater greater than}, {@link Filters#less less than} and
     * {@link Filters#between between}, and should be used for attributes that
     * are primarily queried using those types of filters.
     * <p/>
     * When that's not the case, the unordered filters (default) are less
     * expensive to create and maintain, and should be preferred.
     *
     * @return whether the index should be ordered
     */
    boolean ordered() default false;

    /**
     * An optional {@link Comparator} class to use for ordering. Only applicable
     * when {@link #ordered()} is set to {@code true}. Must have a default
     * constructor.
     * <p/>
     * If not specified, the natural ordering will be used, assuming that the
     * property marked with this annotation returns a type that implements
     * {@link Comparable} interface.
     *
     * @return the class of the {@link Comparator} to use for ordering
     */
    Class<? extends Comparator> comparator() default Comparator.class;
    }
