/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.Evolvable;


/**
* Extension of the {@link PortableObject} interface that supports forwards- and
* backwards-compatibility of POF data streams.
*
* @author cp/jh  2006.07.14
*
* @since Coherence 3.2
*/
public interface EvolvablePortableObject
        extends PortableObject, Evolvable
    {
    }
