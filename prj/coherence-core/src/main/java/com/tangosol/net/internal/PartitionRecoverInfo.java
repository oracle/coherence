/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;


import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.net.partition.PartitionSet;

import java.util.List;
import java.util.Map;

/**
 * PartitionRecoverInfo is a data structure that holds partition recovery information.
 */
public class PartitionRecoverInfo
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct the PartitionRecoverInfo for the specified parameters.
     *
     * @param recoverRequest  the PartitionRecoverRequest (Component.Net.Message)
     * @param mapConfig       the partition config map
     * @param listRequests    the list of requests
     * @param mgrRecover      the persistence recovery manager
     * @param partsRecovered  the recovered {@link PartitionSet}
     */
    public PartitionRecoverInfo(Object recoverRequest, Map mapConfig, List listRequests,
                                PersistenceManager mgrRecover, PartitionSet partsRecovered)
        {
        this(recoverRequest, mapConfig, listRequests, mgrRecover, partsRecovered, null, null);
        }

    /**
     * Construct the PartitionRecoverInfo for the specified parameters.
     *
     * @param recoverRequest  the PartitionRecoverRequest (Component.Net.Message)
     * @param mapConfig       the partition config map
     * @param listRequests    the list of requests
     * @param mgrRecover      the persistence recovery manager
     * @param partsRecovered  the recovered {@link PartitionSet}
     * @param partsFail       the failed {@link PartitionSet}
     */
    public PartitionRecoverInfo(Object recoverRequest, Map mapConfig, List listRequests,
            PersistenceManager mgrRecover, PartitionSet partsRecovered,
            PartitionSet partsFail, PartitionSet partsFailEvents)
        {
        f_recoverRequest  = recoverRequest;
        f_mapConfig       = mapConfig;
        f_listRequests    = listRequests;
        f_manager         = mgrRecover;
        f_partsRecovered  = partsRecovered;
        f_partsFail       = partsFail;
        f_partsFailEvents = partsFailEvents;
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return the PartitionRecoverRequest.
     *
     * @return the target Member
     */
    public Object getRequest()
        {
        return f_recoverRequest;
        }

    /**
     * Return the partition config map.
     *
     * @return the config map
     */
    public Map getMapConfig()
        {
        return f_mapConfig;
        }

    /**
     * Return the list of requests.
     *
     * @return the list of requests
     */
    public List getListRequests()
        {
        return f_listRequests;
        }

    /**
     * Return persistence recovery manager.
     *
     * @return the recovery manager
     */
    public PersistenceManager getManager()
        {
        return f_manager;
        }

    /**
     * Return the recovered {@link PartitionSet}.
     *
     * @return the recovered PartitionSet
     */
    public PartitionSet getRecoveredPartitions()
        {
        return f_partsRecovered;
        }

    /**
     * Return the failed {@link PartitionSet}.
     */
    public PartitionSet getFailedPartitions()
        {
        return f_partsFail;
        }

    /**
     * Return the failed {@link PartitionSet}.
     */
    public PartitionSet getFailedEventPartitions()
        {
        return f_partsFailEvents;
        }

    // ----- data fields ----------------------------------------------------

    /**
     * The PartitionRecoverRequest.
     */
    private final Object f_recoverRequest;

    /**
     * The map of config.
     */
    private final Map f_mapConfig;

    /**
     * The list of requests that need to be posted when recover is done.
     */
    private final List f_listRequests;

    /**
     * The persistence recovery manager.
     */
    private final PersistenceManager f_manager;

    /**
     * The recovered PartitionSet.
     */
    private final PartitionSet f_partsRecovered;

    /**
     * The failed PartitionSet.
     */
    private final PartitionSet f_partsFail;

    /**
     * The failed PartitionSet.
     */
    private final PartitionSet f_partsFailEvents;
    }