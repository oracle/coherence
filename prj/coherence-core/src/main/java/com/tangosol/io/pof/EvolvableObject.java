/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.io.Evolvable;

/**
 * Defines an interface that should be implemented by the classes that want to
 * support evolution.
 *
 * @author as  2013.04.24
 * @since  12.2.1
 */
public interface EvolvableObject
    {
    /**
     * Return {@link Evolvable} object for the specified type id.
     * <p>
     * This method should only return Evolvable instance if the specified type
     * id matches its own type id. Otherwise, it should delegate to the parent:
     *
     * <pre>
     *     // assuming type ID == 1234 and impl. version == 3
     *     private Evolvable evolvable = new SimpleEvolvable(3);
     *     ...
     *     public Evolvable getEvolvable(int nTypeId)
     *         {
     *         if (1234 == nTypeId)
     *             {
     *             return this.evolvable;
     *             }
     *
     *         return super.getEvolvable(nTypeId);
     *         }
     * </pre>
     *
     * @param nTypeId  type id to get {@link Evolvable} instance for
     *
     * @return  Evolvable instance for the specified type id
     */
    Evolvable getEvolvable(int nTypeId);

    /**
     * Return {@link EvolvableHolder} that should be used to store information
     * about evolvable objects that are not known during deserialization.
     * <p>
     * For example, it is possible to evolve the class hierarchy by adding new
     * classes at any level in the hierarchy. Normally this would cause a problem
     * during deserialization on older clients that don't have new classes at all,
     * but EvolvableHolder allows us to work around that issue and simply store
     * type id to opaque binary value mapping within it.
     *
     * @return  EvolvableHolder instance
     */
    EvolvableHolder getEvolvableHolder();
    }
