/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.comparator;


import java.util.Comparator;


/**
* EntryAwareComparator is an extension to the {@link Comparator Comparator}
* interface that allows the {@link EntryComparator} to know whether the
* underlying comparator expects to compare the corresponding Entries' keys or
* values.
*
* @author gg 2007.05.05
*/

public interface EntryAwareComparator<T>
    extends Comparator<T>
    {
    /**
    * Specifies whether this comparator expects to compare keys or values.
    *
    * @return true if Entry keys are expected; false otherwise
    */
    public boolean isKeyComparator();
    }