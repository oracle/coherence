/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.topic.impl.paged.agent.CleanupSubscribers;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;

import com.tangosol.net.partition.PartitionEvent;
import com.tangosol.net.partition.PartitionListener;

import java.util.concurrent.CompletableFuture;

/**
 * A combination of {@link MemberListener} and {@link PartitionListener} that triggers
 * clean-up of orphaned subscribers left behind after member departure.
 * <p>
 * Clean-up is triggered by member left events, but this was found to occasionally be unreliable if
 * the departed member had been storage enabled  as the {@link CleanupSubscribers} entry processor
 * executed before the partitions had been fully redistributed and could miss some orphans. As a
 * belt-and-braces approach clean-up is also triggered on partitions being received after a
 * re-distribution, which may have occurred due to member departure.
 *
 * @author Jonathan Knight  2021.09.09
 * @since 21.06.2
 */
public class SubscriberCleanupListener
        implements MemberListener, PartitionListener
    {
    @Override
    public void onPartitionEvent(PartitionEvent evt)
        {
        int id = evt.getId();
        if (id == PartitionEvent.PARTITION_RECEIVE_COMMIT)
            {
            CompletableFuture.runAsync(() ->
                {
                DistributedCacheService service = (DistributedCacheService) evt.getService();
                CleanupSubscribers processor = new CleanupSubscribers();
                processor.execute(service, evt.getPartitionSet());
                }).handle((ignored, err) ->
                    {
                    // don't bother logging the error if the service shutdown
                    if (err != null && evt.getService().isRunning())
                        {
                        Logger.err("Error invoking subscriber clean-up", err);
                        }
                    return null;
                    });
            }
        }

    @Override
    public void memberLeft(MemberEvent evt)
        {
        DistributedCacheService service = (DistributedCacheService) evt.getService();

        if (!evt.isLocal())
            {
            // only do clean-up if it was not the local member that left
            CleanupSubscribers processor = new CleanupSubscribers();
            processor.execute(service);
            }
        }

    @Override
    public void memberJoined(MemberEvent evt)
        {
        }

    @Override
    public void memberLeaving(MemberEvent evt)
        {
        }
    }
