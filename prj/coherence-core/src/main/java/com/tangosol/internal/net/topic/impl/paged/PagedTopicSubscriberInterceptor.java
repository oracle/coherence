/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.topic.impl.paged.model.PagedTopicSubscription;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.net.Member;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.partition.cache.EntryEvent;
import com.tangosol.net.events.partition.cache.PartitionedCacheDispatcher;
import com.tangosol.util.Base;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A server side interceptor used to detect removal of {@link SubscriberInfo} entries
 * from the subscriber {@link PagedTopicCaches#Subscribers} when a subscriber is closed
 * or is evicted due to timeout.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class PagedTopicSubscriberInterceptor
        implements EventDispatcherAwareInterceptor<EntryEvent<SubscriberInfo.Key, SubscriberInfo>>
    {
    /**
     * Create a {@link PagedTopicSubscriberInterceptor}.
     */
    public PagedTopicSubscriberInterceptor()
        {
        f_executor = Executors.newSingleThreadScheduledExecutor(runnable ->
            {
            String sName = "PagedTopic:SubscriberTimeoutInterceptor:" + f_instance.incrementAndGet();
            return Base.makeThread(null, runnable, sName);
            });
        }

    @Override
    public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
        {
        if (dispatcher instanceof PartitionedCacheDispatcher)
            {
            String sCacheName = ((PartitionedCacheDispatcher) dispatcher).getCacheName();
            if (PagedTopicCaches.Names.SUBSCRIBERS.equals(PagedTopicCaches.Names.fromCacheName(sCacheName)))
                {
                dispatcher.addEventInterceptor(sIdentifier, this, Collections.singleton(EntryEvent.Type.REMOVED), true);
                }
            }
        }

    @Override
    public void onEvent(EntryEvent<SubscriberInfo.Key, SubscriberInfo> event)
        {
        if (event.getType() == EntryEvent.Type.REMOVED)
            {
            SubscriberInfo.Key key     = event.getKey();
            SubscriberInfo     info    = event.getOriginalValue();
            long               nId     = key.getSubscriberId();
            SubscriberGroupId  groupId = key.getGroupId();

            Logger.finest(String.format(
                    "Cleaning up subscriber %d in group '%s' owned by member %d uuid=%s",
                    key.getSubscriberId(), groupId.getGroupName(), SubscriberId.memberIdFromId(nId),
                    info.getOwningUid()));

            // we MUST process the event on another thread so as not to block the event dispatcher thread.
            f_executor.execute(() -> processSubscriberRemoval(event));
            }
        }

    @SuppressWarnings({"unchecked"})
    private void processSubscriberRemoval(EntryEvent<SubscriberInfo.Key, SubscriberInfo> event)
        {
        SubscriberInfo.Key key            = event.getKey();
        SubscriberInfo     info           = event.getOriginalValue();
        long               nId            = key.getSubscriberId();
        SubscriberGroupId  groupId        = key.getGroupId();
        String             sTopicName     = PagedTopicCaches.Names.getTopicName(event.getCacheName());
        String             sSubscriptions = PagedTopicCaches.Names.SUBSCRIPTIONS.cacheNameForTopicName(sTopicName);
        PagedTopicService  topicService   = (PagedTopicService) event.getService();
        int                nMember        = SubscriberId.memberIdFromId(nId);
        long               lTimestamp     = info.getConnectionTimestamp();

        if (event.getEntry().isSynthetic())
            {
            Logger.finest(String.format(
                    "Subscriber expired after %d ms - groupId='%s', memberId=%d, notificationId=%d, last heartbeat at %s",
                    info.getTimeoutMillis(), groupId.getGroupName(), nMember, SubscriberId.notificationIdFromId(nId), info.getLastHeartbeat()));
            }
        else
            {
            boolean fManual = ((Set<Member>) topicService.getInfo().getServiceMembers()).stream().anyMatch(m -> m.getId() == nMember);
            String  sReason = fManual ? "manual removal of subscriber(s)" : String.format("departure of member %d uuid=%s", nMember, info.getOwningUid());
            Logger.finest(String.format("Subscriber %d in group '%s' removed due to %s", nId, groupId.getGroupName(), sReason));
            }

        SubscriberId           subscriberId    = new SubscriberId(nId, info.getOwningUid());
        long                   lSubscriptionId = topicService.getSubscriptionId(sTopicName, groupId);
        PagedTopicSubscription subscription    = topicService.getSubscription(lSubscriptionId);

        // This is an async event, the subscriber may have already reconnected with a newer timestamp
        if (subscription == null || subscription.getSubscriberTimestamp(subscriberId) <= lTimestamp)
            {
            PagedTopicSubscription.notifyClosed(topicService.ensureCache(sSubscriptions, null), groupId, lSubscriptionId, subscriberId);
            }
        }

    // ----- constants --------------------------------------------------

    private static final AtomicInteger f_instance = new AtomicInteger();

    // ----- data members -----------------------------------------------

    private final Executor f_executor;
    }
