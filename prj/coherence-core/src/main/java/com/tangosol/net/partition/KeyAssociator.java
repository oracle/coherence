/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.PartitionedService;


/**
* A KeyAssociator provides key associations on behalf of a set of keys.
* The information provided by a KeyAssociator will be used to place
* all associated keys into the same partition. Custom KeyAssociator
* implementations should not allow circular associations.
* <p>
* See {@link com.tangosol.util.filter.KeyAssociatedFilter KeyAssociatedFilter}
* for an example of a distributed query that takes advantage of a custom
* KeyAssociator implementation to dramatically optimize its performance.
*
* @since Coherence 3.0
*/
public interface KeyAssociator
    {
    /**
    * Initialize the KeyAssociator and bind it to a PartitionedService.
    *
    * @param service  the PartitionedService that this associator is being
    *                 bound to
    */
    public void init(PartitionedService service);

    /**
    * Determine the key object to which the specified key object is
    * associated.
    * <p>
    * <i>Note: Circular associations are not permitted.</i>
    *
    * @param oKey  the key to get an association for
    *
    * @return the key object that is associated with the specified key,
    *         or null if the specified key has no association
    */
    public Object getAssociatedKey(Object oKey);
    }
