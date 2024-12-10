/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.BackingMapManager;

import java.util.Map;


/**
* In a partitioned configuration, backing maps that implement the
* PartitionAwareBackingMap interface are able to react to the partition
* life-cycle (a partition showing up on a node, or moving away from a node)
* and manage data more efficiently as a result.
*
* @since Coherence 3.5
* @author cp  2008-11-20
*/
public interface PartitionAwareBackingMap
        extends Map
    {
    // ----- methods for consumption by the partitioned service -------------

    /**
    * Obtain the BackingMapManager that this PartitionAwareBackingMap uses to
    * instantiate and release backing maps.
    *
    * @return the BackingMapManager
    */
    public BackingMapManager getBackingMapManager();

    /**
    * Determine the name of the cache for which this PartitionAwareBackingMap
    * exists.
    *
    * @return the cache name
    */
    public String getName();

    /**
    * Add a partition to the PartitionAwareBackingMap.
    *
    * @param nPid  the partition id that the PartitionAwareBackingMap will be
    *              responsible for, starting at this instant
    */
    public void createPartition(int nPid);

    /**
    * Remove a partition from the PartitionAwareBackingMap.
    *
    * @param nPid  the partition id that the PartitionAwareBackingMap will no
    *              longer be responsible for, starting at this instant
    */
    public void destroyPartition(int nPid);

    /**
    * Obtain a Map view for the data in a specific partition.
    *
    * @param nPid  the partition ID
    *
    * @return the backing map (or null if that partition is not owned)
    */
    public Map getPartitionMap(int nPid);

    /**
    * Obtain a Map view for the data in a specific set of partitions.
    *
    * @param partitions  the masking PartitionSet
    *
    * @return a read-only view into a subset of backing maps
    */
    public Map getPartitionMap(PartitionSet partitions);
    }
