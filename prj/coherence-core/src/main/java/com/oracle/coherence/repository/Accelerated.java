/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import com.tangosol.util.extractor.DeserializationAccelerator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Repository implementations annotated with this class will keep deserialized
 * entities in memory, in addition to a serialized binary form, of all entities
 * in the repository.
 * <p/>
 * This can significantly improve performance of queries, aggregations, and
 * in-place reads and updates by avoiding deserialization of entities, at the
 * cost of (significantly) increased memory footprint.
 * <p/>
 * If all you are doing are key-based reads, writes and updates, or if the data
 * set is fairly small, acceleration is probably not worth the cost. Even with
 * the occasional query and/or aggregation, it may be more efficient (from a
 * memory consumption perspective) to create more specific {@link Indexed indices}
 * for your entity classes.
 * <p/>
 * However, if you are doing a lot of queries and aggregations across the large
 * data set, or need to perform updates as efficiently as possible and with
 * minimal deserialization and GC impact, it may be worth marking repository
 * implementation with this annotation.
 *
 * @author Aleks Seovic  2021.02.09
 * @since 21.06
 *
 * @see DeserializationAccelerator
 * @see Indexed
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Accelerated
    {
    }
