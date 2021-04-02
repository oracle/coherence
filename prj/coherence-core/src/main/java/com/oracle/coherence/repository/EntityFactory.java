/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.repository;

import java.io.Serializable;

/**
 * Provides a way to create entity instances with a known identity.
 *
 * @param <ID> the type of entity's identity
 * @param <T>  the type of entity
 *
 * @author Aleks Seovic  2021.02.01
 * @since 21.06
 */
// tag::doc[]
@FunctionalInterface
public interface EntityFactory<ID, T>
        extends Serializable
    {
    /**
     * Create an entity instance with the specified identity.
     *
     * @param id identifier to create entity instance with
     *
     * @return a created entity instance
     */
    T create(ID id);
    }
// end::doc[]
