/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.net.cache.SimpleMemoryCalculator;


/**
* The Sizable interface is implemented by types that are capable of calculating
* the memory footprint of an instance.
*
* @author coh 2010.10.01
* @since Coherence 3.7
*/
public interface Sizable
    {
    /**
    * Calculate the memory footprint for this instance. The calculation is
    * discretionary to the implementation.
    * <p>
    * Note: If utilizing {@link SimpleMemoryCalculator#sizeOf(Object)}, do not
    *       pass the instance itself as it will result in endless recursion.
    *
    * @return the memory footprint (in bytes) of this instance
    */
    public int calculateSize();
    }