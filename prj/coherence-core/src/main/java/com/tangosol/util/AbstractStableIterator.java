/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;

/**
* An abstract Iterator implementation that is stable between the
* {@link #hasNext} and {@link #next} methods, and between the {@link #next}
* and {@link #remove()} methods.
*
* @since Coherence 3.1
* @author cp  2003.05.24
* @deprecated As of Coherence 12.1.2, replaced by {@link com.oracle.coherence.common.collections.AbstractStableIterator}
*/
@Deprecated
public abstract class AbstractStableIterator
        extends com.oracle.coherence.common.collections.AbstractStableIterator<Object>
    {
    }
