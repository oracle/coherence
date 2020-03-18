/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.cache;


import com.oracle.coherence.common.base.Associated;


/**
* A KeyAssociation represents a key object that has a natural association
* with another key object. The key object and the associated key may refer
* to entries in the same or different caches.
* <p>
* For example, the information provided by a key that implements
* KeyAssociation may be used to place the key into the same partition as
* its associated key.
* <p>
* See {@link com.tangosol.util.filter.KeyAssociatedFilter KeyAssociatedFilter}
* for an example of a distributed query that takes advantage of a custom
* KeyAssociation implementation to dramatically optimize its performance.
*
* @since Coherence 3.0
*/
public interface KeyAssociation<T>
        extends Associated<T>
    {
    }