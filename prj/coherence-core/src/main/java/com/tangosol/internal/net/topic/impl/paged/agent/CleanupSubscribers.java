/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged.agent;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicSubscriber;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import com.tangosol.util.processor.AbstractEvolvableProcessor;

import java.io.IOException;

import java.time.Instant;
import java.time.ZoneId;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;

/**
 * An entry processor that removes subscribers that have a member id for a member that is
 * no longer part of the topic service.
 * <p>
 * Clean-up is only invoked on the local partitions for topics.
 *
 * @author Jonathan Knight  2021.09.09
 * @since 21.06.2
 */
public class CleanupSubscribers
        extends AbstractEvolvableProcessor<SubscriberInfo.Key,SubscriberInfo, Boolean>
    {
    /**
     * Default constructor for serialization.
     */
    public CleanupSubscribers()
        {
        }

    // ----- CleanupSubscribers methods -------------------------------------

    /**
     * Execute clean-up against all topics on the specified cache service.
     * <p>
     * The cache service should be the underlying cache service from a paged topic.
     *
     * @param service  the cache service to clean-up
     */
    public void execute(DistributedCacheService service)
        {
        execute(service, null);
        }

    /**
     * Execute clean-up against all topics on the specified cache service.
     * <p>
     * The cache service should be the underlying cache service from a paged topic.
     *
     * @param service  the cache service to clean-up
     * @param parts    the optional specific partitions to clean-up
     */
    @SuppressWarnings("unchecked")
    public void execute(DistributedCacheService service, PartitionSet parts)
        {
        if (!service.isRunning())
            {
            // Nothing to do if the service is stopped.
            return;
            }

        ClassLoader         loader      = service.getContextClassLoader();
        Enumeration<String> enumeration = service.getCacheNames();

        // Get the topic names we know about
        Set<String> setTopic = new HashSet<>();
        while (enumeration.hasMoreElements())
            {
            String sCacheName = enumeration.nextElement();
            String sTopicName = PagedTopicCaches.Names.getTopicName(sCacheName);
            setTopic.add(sTopicName);
            }

        // For each topic call the CleanupSubscribers only for the local partitions.
        // We do this because we cannot guarantee other members are not on a version of
        // Coherence that does not have this clean-up code and EntryProcessor.
        for (String sTopic : setTopic)
            {
            try
                {
                if (!service.isRunning())
                    {
                    // service has been stopped, nothing more to do
                    break;
                    }

                PartitionSet partsCleanup;
                if (parts == null)
                    {
                    // No partitions specified so get the local partitions - we do this in the loop in case it changes
                    // while looping. There is still a window where this may happen and the partition we're targeting
                    // moves to a member possibly using an older version of Coherence that does not have the entry
                    // processor. This will then obviously fail.
                    // In that case we'll just have to rely on any timeout to clean up subscribers in the moved partition.
                    partsCleanup = service.getOwnedPartitions(service.getCluster().getLocalMember());
                    }
                else
                    {
                    // use the specified partitions.
                    partsCleanup = parts;
                    }
                PartitionedFilter<?> filter = new PartitionedFilter<>(AlwaysFilter.INSTANCE(), partsCleanup);

                NamedCache<SubscriberInfo.Key, SubscriberInfo> cache =
                        service.ensureCache(PagedTopicCaches.Names.SUBSCRIBERS.cacheNameForTopicName(sTopic), loader);

                if (!cache.isActive())
                    {
                    // nothing to do if the cache is inactive
                    continue;
                    }

                cache.async().invokeAll(filter, this).handle((mapResult, err) ->
                    {
                    if (err == null)
                        {
                        if (!mapResult.isEmpty())
                            {
                            Map<Integer, Map<SubscriberGroupId, List<Long>>> mapRemoved = new HashMap<>();

                            for (SubscriberInfo.Key key : mapResult.keySet())
                                {
                                int nMember = PagedTopicSubscriber.memberIdFromId(key.getSubscriberId());

                                mapRemoved.computeIfAbsent(nMember, k-> new HashMap<>())
                                          .computeIfAbsent(key.getGroupId(), k -> new ArrayList<>())
                                          .add(key.getSubscriberId());
                                }

                            for (Map.Entry<Integer, Map<SubscriberGroupId, List<Long>>> entry : mapRemoved.entrySet())
                                {
                                int nMember = entry.getKey();
                                String sMsg = entry.getValue()
                                            .entrySet().stream()
                                            .map(e -> "[Group='" + e.getKey().getGroupName()
                                                    + "' Subscribers="
                                                    + PagedTopicSubscriber.idToString(e.getValue())
                                                    + "]")
                                            .collect(Collectors.joining(", "));

                                Logger.info("Removed the following subscribers from topic " + sTopic
                                            + " due to departure of member " + nMember + " " + sMsg);
                                }
                            }
                        }
                    else
                        {
                        if (cache.isActive())
                            {
                            Logger.err("Caught exception cleaning up subscribers in topic " + sTopic, err);
                            }
                        // else the cache was deactivated, probably causing the exception
                        }
                    return null;
                    });
                }
            catch (Throwable t)
                {
                Logger.err("Caught exception cleaning up subscribers in topic " + sTopic, t);
                }
            }
        }

    // ----- AbstractEvolvableProcessor methods -----------------------------

    @Override
    @SuppressWarnings("unchecked")
    public Map<SubscriberInfo.Key, Boolean> processAll(Set<? extends InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo>> setEntries)
        {
        Map<SubscriberInfo.Key, Boolean> mapResult = new HashMap<>();
        BinaryEntry<SubscriberInfo.Key, SubscriberInfo> binaryEntry = setEntries.stream()
                .findFirst()
                .map(InvocableMap.Entry::asBinaryEntry)
                .orElse(null);

        if (binaryEntry != null)
            {
            DistributedCacheService service   = (DistributedCacheService) binaryEntry.getContext().getCacheService();
            Map<Integer, Member>    mapMember = ((Set<Member>) service.getInfo()
                                                       .getServiceMembers())
                                                       .stream()
                                                       .collect(Collectors.toMap(Member::getId, m -> m));

            for (InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry : setEntries)
                {
                if (process(entry, mapMember))
                    {
                    mapResult.put(entry.getKey(), true);
                    }
                }
            }
        return mapResult;
        }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean process(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry)
        {
        if (entry.isPresent())
            {
            DistributedCacheService service   = (DistributedCacheService) entry.asBinaryEntry().getContext().getCacheService();
            Map<Integer, Member>    mapMember = ((Set<Member>) service.getInfo()
                                                       .getServiceMembers())
                                                       .stream()
                                                       .collect(Collectors.toMap(Member::getId, m -> m));

            return process(entry, mapMember);
            }
        return false;
        }

    @Override
    public int getImplVersion()
        {
        return 0;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Process the specified subscriber entry to see whether it should be removed.
     * <p>
     * If the entry is present and the subscriber's member id is not in the
     * member set then it will be removed.
     *
     * @param entry      the subscriber entry to potentially remove
     * @param mapMember  the current map of members, keyed by member Id
     *
     * @return {@code true} if the subscriber was removed
     */
    Boolean process(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry, Map<Integer, Member> mapMember)
        {
        if (entry.isPresent())
            {
            long           nId     = entry.getKey().getSubscriberId();
            SubscriberInfo info    = entry.getValue();
            UUID           uuid    = info.getOwningUid();
            int            nMember = PagedTopicSubscriber.memberIdFromId(nId);
            Member         member  = mapMember.get(nMember);
            boolean        fRemove = false;

            if (member == null)
                {
                // owing member no longer exists
                fRemove = true;
                }
            else if (uuid != null && !member.getUuid().equals(uuid))
                {
                // owing member exists, but with a different UUID
                fRemove = true;
                }
            else if (Instant.ofEpochMilli(member.getTimestamp()).atZone(ZoneId.systemDefault()).toLocalDateTime().isAfter(info.getLastHeartbeat()))
                {
                // owing member exists, but has a timestamp after the last heartbeat time
                fRemove = true;
                }

            if (fRemove)
                {
                entry.remove(false);
                return true;
                }
            }
        return false;
        }
    }
