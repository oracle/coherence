/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.PartitionedService;

import com.tangosol.util.ExternalizableHelper;


/**
* DefaultKeyPartitioningStrategy provides a simple strategy for assigning keys
* to partitions which is based on the hash code of keys in internal
* (serialized to Binary) form.
*
* @author gg 2010.05.19
* @since Coherence 3.6
*/
public class DefaultKeyPartitioningStrategy
        extends    ExternalizableHelper
        implements KeyPartitioningStrategy
    {
    /**
    * Default constructor.
    */
    public DefaultKeyPartitioningStrategy()
        {
        }

    /**
    * {@inheritDoc}
    */
    public void init(PartitionedService service)
        {
        m_service = service;
        }

    /**
    * {@inheritDoc}
    */
    public int getKeyPartition(Object oKey)
        {
        return calculateKeyPartition(m_service, oKey);
        }

    /**
    * {@inheritDoc}
    */
    public PartitionSet getAssociatedPartitions(Object oKey)
        {
        PartitionSet parts = new PartitionSet(m_service.getPartitionCount());
        parts.add(getKeyPartition(oKey));
        return parts;
        }

    /**
    * Determine the partition to which the given key should be assigned for
    * the specified PartitionService, taking into consideration the associations
    * provided by the service's KeyAssociator.
    * <p>
    * The resulting partition will be in the range <tt>[0..N)</tt>, where
    * <tt>N</tt> is the value returned from the
    * {@link com.tangosol.net.PartitionedService#getPartitionCount()} method.
    *
    * @param service  the PartitionedService
    * @param oKey     a key in its original (Object) format
    *
    * @return the partition that the corresponding key is assigned to
    */
    public static int calculateKeyPartition(PartitionedService service, Object oKey)
        {
        return calculatePartition(service, calculateKeyPartitionHash(service, oKey));
        }

    /**
    * Determine the partition-hash of the given key.  The partition-hash is a
    * property of the key identity itself, irrespective of the associated
    * PartitionService's configured partition-count.  The returned partition-hash
    * is calculated by taking into consideration the associations  provided by
    * the service's KeyAssociator.
    *
    * @param service  the PartitionedService
    * @param oKey     a key in its original (Object) format
    *
    * @return the partition-hash of the corresponding key
    */
    public static int calculateKeyPartitionHash(PartitionedService service, Object oKey)
        {
        Object oAssocKey = service.getKeyAssociator().getAssociatedKey(oKey);
        return calculateBasePartitionHash(service, oAssocKey == null ? oKey : oAssocKey);
        }

    /**
    * Determine the partition-hash of the given key.  The partition-hash is a
    * property of the key identity itself, irrespective of the associated
    * PartitionService's configured partition-count.
    *
    * @param service  the PartitionedService
    * @param oKey     a key in its original (Object) format
    *
    * @return the partition-hash of the corresponding key
    */
    public static int calculateBasePartitionHash(PartitionedService service, Object oKey)
        {
        return oKey instanceof PartitionAwareKey
            ? ((PartitionAwareKey) oKey).getPartitionId()
            : toBinary(oKey, service.getSerializer()).calculateNaturalPartition(0);
        }

    /**
    * Return the partition associated with the specified hash value in the
    * specified PartitionedService.
    *
    * @param service  the PartitionedService
    * @param nHash    the hash value
    *
    * @return the partition-id that the specified hash value belongs to
    */
    public static int calculatePartition(PartitionedService service, int nHash)
        {
        return (int) ((((long) nHash) & 0xFFFFFFFFL) % (long) service.getPartitionCount());
        }

    // ----- data fields ----------------------------------------------------

    /**
    * The PartitionedService that this strategy is bound to.
    */
    protected PartitionedService m_service;
    }