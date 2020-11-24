/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.Serializable;

/**
* ValueUpdater is used to update an object's state.
*
* @param <T>  the type of object
* @param <U>  the type of value used to update the object
*
* @author jh/gg 2005.10.25
* @since Coherence 3.1
*/
@FunctionalInterface
public interface ValueUpdater<T, U>
        extends Serializable
    {
    /**
    * Update the state of the passed target object using the passed value.
    * For intrinsic types, the specified value is expected to be a standard
    * wrapper type in the same manner that reflection works; for example, an
    * <tt>int</tt> value would be passed as a <tt>java.lang.Integer</tt>.
    *
    * @param target  the Object to update the state of
    * @param value   the new value to update the state with
    *
    * @throws ClassCastException if this ValueUpdater is incompatible with
    *         the passed target object or the value and the implementation
    *         <b>requires</b> the passed object or the value to be of a
    *         certain type
    * @throws WrapperException if this ValueUpdater encounters a checked
    *         exception in the course of updating the target object
    * @throws IllegalArgumentException if this ValueUpdater cannot handle
    *         the passed target object or value for any other reason;
    *         an implementor should include a descriptive message
    */
    public void update(T target, U value);
    }