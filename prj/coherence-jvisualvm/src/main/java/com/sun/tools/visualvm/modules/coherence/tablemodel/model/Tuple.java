/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.sun.tools.visualvm.modules.coherence.tablemodel.model;

import java.io.Serializable;

/**
 * An immutable sequence of values that are Serializable.
 *
 * @author Brian Oliver
 * @since  12.1.3
 */

public interface Tuple
        extends Serializable
    {

    // ----- Tuple methods --------------------------------------------------

    /**
     * Return the number of values in the {@link Tuple}.
     *
     * @return the number of values in the {@link Tuple}
     */
    public int size();

    /**
     * Return the value at the specified index.  The first value is at index 0.
     *
     * @param index index to get
     * @throws IndexOutOfBoundsException When 0 &lt; index &le; size()
     *
     * @return the value at the specified index
     */
    public Object get(int index)
            throws IndexOutOfBoundsException;
    }
