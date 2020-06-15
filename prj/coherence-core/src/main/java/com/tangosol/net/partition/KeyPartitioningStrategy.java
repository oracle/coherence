/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.PartitionedService;


/**
* A KeyPartitioningStrategy is a pluggable strategy for assigning keys
* to specific partitions.
* <p>
* <b>Note:</b> as of Coherence 3.6 the contract of the {@link
* #getKeyPartition(Object) getKeyPartition} method has changed and should take
* into consideration the service's {@link
* PartitionedService#getKeyAssociator() key associator}.
*
* @since Coherence 3.0
*/
public interface KeyPartitioningStrategy
    {
    /**
    * Initialize the KeyPartitioningStrategy and bind it to the specified
    * PartitionedService.
    *
    * @param service  the PartitionedService that this strategy is being
    *                   bound to
    */
    public void init(PartitionedService service);

    /**
    * Determine the partition to which a particular key should be assigned.
    * <p>
    * In general, implementations are expected to respect the associations
    * provided by the service's KeyAssociator in such a way that keys that
    * return the same {@link KeyAssociator#getAssociatedKey(Object) associated
    * key} would be assigned to the same partition.  Furthermore,
    * implementations are also expected to respect explicit
    * partition-assignments dictated by the {@link PartitionAwareKey} interface.
    * Naturally, those two interfaces should not be combined for a given key.
    * <p>
    * The resulting partition must be in the range <tt>[0..N-1]</tt>, where
    * <tt>N</tt> is the value returned from
    * {@link PartitionedService#getPartitionCount()}.
    *
    * @param oKey  a key in its Object form
    *
    * @return the partition ID that the specified key is assigned to
    *
    * @see PartitionedService#getPartitionOwner(int)
    */
    public int getKeyPartition(Object oKey);

    /**
    * Determine the set of partitions that all keys associated with the
    * specified key are assigned to. Most commonly, this method returns a
    * partition set containing a single partition returned by the {@link
    * #getKeyPartition(Object) getKeyPartition} method.
    *
    * @param oKey  a key in its Object form
    *
    * @return the PartitionSet associated with the specified key
    *
    * @since Coherence 3.6
    */
    public PartitionSet getAssociatedPartitions(Object oKey);


    // ----- inner interface: PartitionAwareKey ------------------------------

    /**
    * PartitionAwareKey is a well-known interface that should be respected by
    * {@link KeyPartitioningStrategy} implementations.
    * <p>
    * Naturally, since a PartitionAwareKey implementation explicitly dictates
    * its partition, it should not define any key-association.
    *
    * @since Coherence 3.7
    */
    public interface PartitionAwareKey
        {
        /**
        * Return the partition id that this key should be associated with.
        *
        * @return the partition id that this key should be associated with
        */
        public int getPartitionId();
        }
    }