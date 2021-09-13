/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Converter;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.collections.Arrays;

import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.topic.impl.paged.agent.EnsureSubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.PollProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.SeekProcessor;

import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.ContentKey;
import com.tangosol.internal.net.topic.impl.paged.model.PageElement;
import com.tangosol.internal.net.topic.impl.paged.model.PagedPosition;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberInfo;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;

import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CommitResultStatus;
import com.tangosol.net.topic.TopicException;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapNotFoundException;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ThreadLocalRandom;

import java.util.function.Function;

/**
 * This class encapsulates control of a single partition of a paged topic.
 *
 * Note many operations will interact with multiple caches.  Defining a consistent enlistment order proved to be
 * untenable so instead we rely on clients using unit-of-order to ensure all access for a given topic partition
 * is serial.
 *
 * @author jk/mf 2015.05.27
 * @since Coherence 14.1.1
 */
@SuppressWarnings("rawtypes")
public class PagedTopicPartition
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link PagedTopicPartition} using the specified {@link BackingMapManagerContext}
     * and topic name.
     *
     * @param ctxManager  the {@link BackingMapManagerContext} of the underlying caches
     * @param sName       the name of the topic
     * @param nPartition  the partition
     */
    public PagedTopicPartition(BackingMapManagerContext ctxManager, String sName, int nPartition)
        {
        f_ctxManager = ctxManager;
        f_sName      = sName;
        f_nPartition = nPartition;
        }

    // ----- PagedTopicPartitionControl methods ---------------------------------

    /**
     * Initialise a new topic if it has not already been initialised.
     * <p>
     * If the meta-data entry is present and has a value then the topic
     * has already been initialised and the existing meta-data is returned.
     * <p>
     * If the entry is not present or has a null value then the topic
     * is initialised with new meta-data.
     *
     * @param entry  the entry containing the {@link Usage
     *               for the topic.
     *
     * @return  the tail
     */
    public long initialiseTopic(BinaryEntry<Usage.Key, Usage> entry)
        {
        Usage usage = entry.getValue();

        if (usage == null)
            {
            usage = new Usage();
            }

        if (usage.getPublicationTail() == Page.NULL_PAGE)
            {
            // Set the initial page to a "random" value based on the partition count, the subscriber uses the same
            // start page (see TopicCaches.getBasePage().  This is done so that multiple topics hosted by the
            // same service can have their load spread over different cache servers
            long lPage = Math.abs(f_sName.hashCode() % getPartitionCount());

            usage.setPublicationTail(lPage);

            entry.setValue(usage);
            }

        return usage.getPublicationTail();
        }

    /**
     * A static helper method to create a {@link PagedTopicPartition}
     * from a {@link BinaryEntry}.
     *
     * @param entry  the {@link BinaryEntry} to use to create
     *               the {@link PagedTopicPartition}
     *
     * @return an instance of a {@link PagedTopicPartition}
     */
    public static PagedTopicPartition ensureTopic(BinaryEntry entry)
        {
        BackingMapContext ctx = entry.getBackingMapContext();
        return new PagedTopicPartition(ctx.getManagerContext(),
                PagedTopicCaches.Names.getTopicName(ctx.getCacheName()),
                ctx.getManagerContext().getKeyPartition(entry.getBinaryKey()));
        }

    /**
     * Special processing when the first element of a page is inserted.  This is pulled out to allow hotspot to
     * better handle the main path.
     *
     * @param nChannel         the channel
     * @param lPage            the page id
     * @param page             the Page object
     * @param nNotifyPostFull  the post-full notifier id
     * @param cElements        the number of elements being offered
     * @param cbCapServer      the server capacity
     * @param cbCapPage        the page capacity
     *
     * @return the offer result
     */
    protected OfferProcessor.Result onStartOfPage(int nChannel, long lPage, Page page, int nNotifyPostFull, int cElements,
            long cbCapServer, int cbCapPage)
        {
        // each time we start to fill a new page we check to see if any non-durable subscribers have leaked, we could
        // do this at any time but it only really has a negative impact if we allow that to cause us to
        // retain extra pages, thus we only bother to check at the start of a new page.
        cleanupNonDurableSubscribers(peekUsage(nChannel).getAnonymousSubscribers());

        if (!page.isSubscribed() && !getDependencies().isRetainConsumed())
            {
            // there are no subscribers we can simply accept but do not store the content
            // note, we don't worry about this condition when the page is not empty as such a condition
            // can't exist, as pages are deleted as part of unsubscribing
            // Note: we also seal and remove the page, this will cause publisher to advance the page
            // and touch other partitions, cleaning up any anonymous subscribers there as well

            page.setSealed(true);
            removePageIfNotRetainingElements(nChannel, lPage);
            return new OfferProcessor.Result(OfferProcessor.Result.Status.PageSealed, cElements, cbCapPage, 0);
            }
        else if (cbCapServer > 0 && getStorageBytes() >= cbCapServer)
            {
            if (nNotifyPostFull != 0)
                {
                requestRemovalNotification(nNotifyPostFull, nChannel);
                }

            // attempt to free up space by having subscribers remove non-full pages, remember we can't
            // add to them anyway as we're full. To trigger the removal we seal the tail page and then notify
            // the subscribers, thus causing them to revisit the page and detach from it which will eventually
            // result in the last subscriber removing the page, freeing up space.
            for (int i = 0, c = getChannelCount(); i < c; ++i)
                {
                Usage usage = peekUsage(i);
                long  lTail = usage.getPartitionTail();
                if (lTail != Page.NULL_PAGE && lTail == usage.getPartitionHead()) // just one page
                    {
                    Page pageSeal = enlistPage(i, usage.getPartitionHead());
                    pageSeal.setSealed(true);
                    notifyAll(pageSeal.resetInsertionNotifiers());
                    }
                // else; if there are other full pages and we aren't preventing their removal
                }

            return new OfferProcessor.Result(OfferProcessor.Result.Status.TopicFull, 0, cbCapPage, -1);
            }

        return null;
        }

    /**
     * Offer a {@link List} of elements to the tail of the {@link Page} held in
     * the specified entry, creating the {@link Page} if required.
     *
     * @param entry           the {@link BinaryEntry} holding the {@link Page}
     * @param listElements    the {@link List} of elements to offer
     * @param nNotifyPostFull the post full notifier, or zero for none
     * @param fSealPage       a flag indicating whether the page should be sealed
     *                        after this offer operation.
     *
     * @return the {@link OfferProcessor.Result} containing the results of this offer
     */
    public OfferProcessor.Result offerToPageTail(BinaryEntry<Page.Key, Page> entry,
            List<Binary> listElements, int nNotifyPostFull, boolean fSealPage)
        {
        Page.Key                keyPage       = entry.getKey();
        int                     nChannel      = keyPage.getChannelId();
        long                    lPage         = keyPage.getPageId();
        PagedTopic.Dependencies configuration = getDependencies();
        int                     cbCapPage     = configuration.getPageCapacity();
        long                    cbCapServer   = configuration.getServerCapacity();

        if (cbCapServer > 0 && nNotifyPostFull != 0)
            {
            // check if we own a large enough number of partitions that we would exceed the server capacity before
            // having placed a page in each partition. This is an issue if we use postFull notifications as we
            // can only notify once we delete a page from the partition which made us full,  Thankfully this
            // doesn't have to be channel based as we watch for page removals across all channels in a partition.
            int cbCapPageDyn = (int) Math.min(Integer.MAX_VALUE, (cbCapServer / 2) / getLocalPartitionCount());
            if (cbCapPage > cbCapPageDyn)
                {
                // ok, so we do risk the condition described above, dynamically scale back page size in an attempt
                // to avoid this issue.  Ultimately we'll allow ourselves to exceed the local capacity (see TopicFull above)
                // if need be but this logic ensures that we exceed it as minimally as is possible

                // TODO: base on the current storage rather than estimate, this is especially important if
                // we've gained partitions will full pages over time from dead members

                // TODO: the publisher could have similar logic to avoid sending overly large batches
                cbCapPage = Math.max(1, cbCapPageDyn);
                }
            }

        Page page = ensurePage(nChannel, entry);
        if (page == null || page.isSealed())
            {
            // The page has been removed or is full so the producer's idea of the tail
            // is out of date and it needs to re-offer to the correct tail page
            return new OfferProcessor.Result(OfferProcessor.Result.Status.PageSealed, 0, 0, -1);
            }
        else if (page.getTail() == Page.EMPTY)
            {
            OfferProcessor.Result result = onStartOfPage(nChannel, lPage, page, nNotifyPostFull, listElements.size(),
                    cbCapServer, cbCapPage);
            if (result != null)
                {
                return result;
                }
            }

        BackingMapContext            ctxContent    = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
        NamedTopic.ElementCalculator calculator    = configuration.getElementCalculator();
        long                         cMillisExpiry = configuration.getElementExpiryMillis();
        int                          cbPage        = page.getByteSize();
        int                          nTailStart    = page.getTail();
        int                          nTail         = nTailStart;
        OfferProcessor.Result.Status status        = OfferProcessor.Result.Status.Success;
        int                          cAccepted     = 0;
        long                         lTimestamp    = getClusterTime();

        // Iterate over all of the elements to be offered until they are
        // all offered or until the page has reached maximum capacity
        for (Iterator<Binary> iter = listElements.iterator(); iter.hasNext() && cbPage < cbCapPage; ++cAccepted)
            {
            Binary      binValue        = iter.next();
            Binary      binKey          = ContentKey.toBinary(f_nPartition, nChannel, lPage, ++nTail);
            BinaryEntry binElementEntry = (BinaryEntry) ctxContent.getBackingMapEntry(binKey);

            cbPage += calculator.calculateUnits(binValue);

            // decorate the value with the position
            // we do this here rather than on polling as there are likely to be more polls
            // that publishes so polling will be slightly faster
            Binary binElement = PageElement.toBinary(nChannel, lPage, nTail, lTimestamp, binValue);

            // Set the decorated binary element as the entry value
            binElementEntry.updateBinaryValue(binElement);

            // If the topic is configured with expiry then set this on the element entry
            if (cMillisExpiry > LocalCache.DEFAULT_EXPIRE)
                {
                binElementEntry.expire(cMillisExpiry);
                }
            }

        // Update the tail element pointer for the page
        page.setTail(nTail);
        // update the tail timestamp for the page
        page.setTimestampTail(lTimestamp);

        if (nTailStart == Page.EMPTY)
            {
            // we added the first element to the page so set the head timestamp
            page.setTimestampHead(lTimestamp);
            }

        int cbRemainingCapacity;

        if (cbPage >= cbCapPage || fSealPage)
            {
            page.setSealed(true);
            status = OfferProcessor.Result.Status.PageSealed;
            cbRemainingCapacity = cbCapPage; // amount of space in next page
            }
        else
            {
            cbRemainingCapacity = cbCapPage - cbPage; // amount of space in this page
            }

        // Update the page's byte size with the new size based on the offered elements
        page.setByteSize(cbPage);

        // notify any waiting subscribers
        notifyAll(page.resetInsertionNotifiers());

        // update the page entry with the modified page
        entry.setValue(page);

        return new OfferProcessor.Result(status, cAccepted, cbRemainingCapacity, nTailStart + 1);
        }

    /**
     * Return a mutable PagedUsagePartition for the specified partition.
     * <p>
     * Any changes made to the returned object will be committed as part of the transaction.
     * </p>
     *
     * @return the entry
     */
    protected Usage enlistUsage(int nChannel)
        {
        BinaryEntry<Usage.Key, Usage> entry = enlistBackingMapEntry(
                PagedTopicCaches.Names.USAGE, toBinaryKey(new Usage.Key(getPartition(), nChannel)));

        // entry can be null if topic is destroyed while a poll is in progress
        Usage usage = entry == null ? null : entry.getValue();

        if (usage == null)
            {
            // lazy instantiation
            usage = new Usage();
            }

        // by setting the value (even to the same value) the partition cache will persist any
        // subsequent changes made to the usage object when the processor completes.
        entry.setValue(usage);

        return usage;
        }

    /**
     * Return a Usage entry for the specified partition without enlisting it.
     *
     * @return the value
     */
    protected Usage peekUsage(int nChannel)
        {
        BinaryEntry<Usage.Key, Usage> entry = peekBackingMapEntry(
                PagedTopicCaches.Names.USAGE, toBinaryKey(new Usage.Key(getPartition(), nChannel)));

        return entry == null
                ? enlistUsage(nChannel) // on demand creation, usage must always exist
                : entry.getValue();
        }

    /**
     * Ensure that the specified entry contains a {@link Page}.
     * <p>
     * If the entry does not contain a {@link Page} then a new
     * {@link Page} will be created. A set of entries will also
     * be created in the subscriber page cache for this {@link Page}.
     *
     * @param entry  the entry to ensure contains a {@link Page}
     */
    protected Page ensurePage(int nChannel, BinaryEntry<Page.Key, Page> entry)
        {
        if (entry.isPresent())
            {
            return entry.getValue();
            }

        long  lPage = entry.getKey().getPageId();
        Usage usage = enlistUsage(nChannel);

        if (lPage <= usage.getPartitionMax())
            {
            // the page has already been created, it doesn't exist so it apparently was
            // also removed, it cannot be recreated
            return null;
            }

        // create new page page and update links between pages in this partition
        Page page      = new Page();
        long lTailPrev = usage.getPartitionTail();

        usage.setPartitionTail(lPage);
        usage.setPartitionMax(lPage); // unlike tail this is never reset to NULL_PAGE

        if (lTailPrev == Page.NULL_PAGE)
            {
            // partition was empty, our new tail is also our head
            usage.setPartitionHead(lPage);
            }
        else
            {
            // attach old tail to new tail
            page.adjustReferenceCount(1); // ref from old tail to new page

            Page pagePrev = enlistPage(nChannel, lTailPrev);
            if (pagePrev != null)
                {
                pagePrev.setNextPartitionPage(lPage);
                }
            }

        // attach on behalf of waiting subscribers, they will have to find this page on their own
        page.adjustReferenceCount(usage.resetWaitingSubscriberCount());

        long nTime = getClusterTime();
        page.setTimestampHead(nTime);
        page.setTimestampTail(nTime);

        // set the new Page's previous page
        page.setPreviousPartitionPage(lTailPrev);
        entry.setValue(page);

        return page;
        }

    /**
     * Cleanup potentially abandoned subscriber registrations
     */
    protected void cleanupSubscriberRegistrations()
        {
        // in a multi-channel topic there may be one or more unused channels.  The subscribers will naturally
        // register for notification if an insert ever happens into each of those empty channels, and as there
        // if there is never an insert these registrations would grow and grow as subscriber instances come
        // and go.  The intent of this method is to stem that growth by removing a random few subscriber registrations
        // from each channel.  Note that even in the single channel case we could have had the same leak if we don't
        // have insertions and we have many subscribers come and go

        for (int nChannel = 0, cChannel = getChannelCount(); nChannel < cChannel; ++nChannel)
            {
            Usage usage = enlistUsage(nChannel);
            long  lTail = usage.getPartitionTail();
            if (lTail != Page.NULL_PAGE)
                {
                Page  page        = enlistPage(nChannel, lTail);
                int[] anNotifiers = page.getInsertionNotifiers();
                if (anNotifiers != null && anNotifiers.length >= 2)
                    {
                    // remove two random notifiers (this method is called as part of a subscriber
                    // instance ensuring the subscription, by removing two we ensure we'll eventually
                    // clean out any garbage.  Note if we remove ones which are still in use those
                    // subscribers will receive the deletion event and reregister.  This is harmless
                    // as it just looks like a spurious notification.

                    int[] anNew = new int[anNotifiers.length - 2];
                    int   of    = ThreadLocalRandom.current().nextInt(anNotifiers.length - 1);

                    System.arraycopy(anNotifiers, 0, anNew, 0, of);
                    System.arraycopy(anNotifiers, of + 2, anNew, of, anNew.length - of);

                    notifyAll(new int[] {anNotifiers[of], anNotifiers[of + 1]});

                    page.setInsertionNotifies(anNew);
                    }
                }
            }
        }

    /**
     * Evaluate the supplied anonymous subscribers and destroy any ones which have died without closing themselves.
     *
     * @param colAnon the anonymous subscribers
     */
    @SuppressWarnings("unchecked")
    protected void cleanupNonDurableSubscribers(Collection<SubscriberGroupId> colAnon)
        {
        if (colAnon == null || colAnon.isEmpty())
            {
            return;
            }

        // group anon subscribers by their parent member
        Map<Long, List<SubscriberGroupId>> mapDead = new HashMap<>();
        for (SubscriberGroupId subscriberGroupId : colAnon)
            {
            Long                    ldtMember = subscriberGroupId.getMemberTimestamp();
            List<SubscriberGroupId> listPeer = mapDead.computeIfAbsent(ldtMember, k -> new ArrayList<>());
            listPeer.add(subscriberGroupId);
            }

        // remove all subscribers with live members from the map
        for (Member member : (Set<Member>) f_ctxManager.getCacheService().getInfo().getServiceMembers())
            {
            mapDead.remove(member.getTimestamp());
            }

        // cleanup the remaining and thus dead subscribers
        if (!mapDead.isEmpty())
            {
            for (List<SubscriberGroupId> list : mapDead.values())
                {
                for (SubscriberGroupId subscriberGroupId : list)
                    {
                    removeSubscription(subscriberGroupId);
                    }
                }
            }
        }

    /**
     * Return the associated partition.
     *
     * @return the partition
     */
    public int getPartition()
        {
        return f_nPartition;
        }

    /**
     * Return the partition count for the topic.
     *
     * @return the count
     */
    public int getPartitionCount()
        {
        return ((PartitionedService) f_ctxManager.getCacheService()).getPartitionCount();
        }

    /**
     * Return the channel count for this topic.
     *
     * @return the channel count
     */
    public int getChannelCount()
        {
        return getDependencies().getChannelCount(getPartitionCount());
        }

    /**
     * Return the number of locally owned partitions
     *
     * @return the owned partition count
     */
    public int getLocalPartitionCount()
        {
        PartitionedService service = ((PartitionedService) f_ctxManager.getCacheService());
        return service.getOwnedPartitions(service.getCluster().getLocalMember()).cardinality();
        }

    /**
     * Returns the cluster time (see {@link com.tangosol.net.Cluster#getTimeMillis()}.
     *
     * @return the cluster time.
     */
    public long getClusterTime()
        {
        return f_ctxManager.getCacheService().getCluster().getTimeMillis();
        }

    /**
     * Return the number the local server storage size for this topic.
     *
     * @return the number of bytes locally stored for this topic
     */
    public long getStorageBytes()
        {
        // Always ConfigurableCacheMap and always BINARY calculator
        Map mapBack = getBackingMapContext(PagedTopicCaches.Names.CONTENT).getBackingMap();
        if (mapBack instanceof ConfigurableCacheMap)
            {
            ConfigurableCacheMap cacheBack = (ConfigurableCacheMap) mapBack;
            return (long) cacheBack.getUnits() * cacheBack.getUnitFactor();
            }
        else // for instance live persistence
            {
            throw new UnsupportedOperationException();
            }
        }

    /**
     * Obtain a read-only copy of the specified page.
     *
     * @param lPageId  the id of the page
     *
     * @return  the page or null
     */
    protected Page peekPage(int nChannel, long lPageId)
        {
        BinaryEntry<Page.Key,Page> entry = peekPageEntry(nChannel, lPageId);
        return entry == null ? null : entry.getValue();
        }

    /**
     * Returns a read-only copy of the specified page entry or {code null} if the
     * requested page number is (@link {@link Page#NULL_PAGE}.
     *
     * @param lPageId  the id of the page
     *
     * @return  the page entry or or {code null} if the requested page number is (@link {@link Page#NULL_PAGE}
     */
    protected BinaryEntry<Page.Key,Page> peekPageEntry(int nChannel, long lPageId)
        {
        return lPageId == Page.NULL_PAGE
                ? null
                : peekBackingMapEntry(PagedTopicCaches.Names.PAGES, toBinaryKey(new Page.Key(nChannel, lPageId)));
        }

    /**
     * Obtain an enlisted page entry
     *
     * @param lPage  the id of the page
     *
     * @return  the enlisted page, note any updates to the value will be automatically committed
     */
    @SuppressWarnings("unchecked")
    protected BinaryEntry<Page.Key, Page> enlistPageEntry(int nChannel, long lPage)
        {
        BinaryEntry<Page.Key, Page> entry = (BinaryEntry) getBackingMapContext(PagedTopicCaches.Names.PAGES)
                .getBackingMapEntry(toBinaryKey(new Page.Key(nChannel, lPage)));
        Page page  = entry.getValue();

        if (page != null)
            {
            // ensure any subsequent updates to page will be committed along with the transaction
            entry.setValue(page);
            }

        return entry;
        }

    /**
     * Obtain an enlisted page
     *
     * @param lPage  the id of the page
     *
     * @return  the enlisted page, or null if it does not exist
     */
    protected Page enlistPage(int nChannel, long lPage)
        {
        return enlistPageEntry(nChannel, lPage).getValue();
        }

    /**
     * Remove the specified page and all of its elements, updating the Usage partition head/tail as appropriate.
     *
     * @param lPage  the page to remove
     *
     * @return true if the page was removed, false if the page entry was not present
     */
    public boolean removePageIfNotRetainingElements(int nChannel, long lPage)
        {
        PagedTopic.Dependencies dependencies = getDependencies();

        if (dependencies.isRetainConsumed())
            {
            return false;
            }

        return removePage(nChannel, lPage);
        }

    /**
     * Remove the specified page and all of its elements, updating the Usage partition head/tail as appropriate.
     *
     * @param lPage  the page to remove
     *
     * @return true if the page was removed, false if the page entry was not present
     */
    public boolean removePage(int nChannel, long lPage)
        {
        BinaryEntry<Page.Key, Page> entryPage = enlistPageEntry(nChannel, lPage);
        Page                        page      = entryPage.getValue();
        if (page == null)
            {
            return false;
            }

        BackingMapContext ctxElem = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
        for (int nPos = page.getTail(); nPos >= 0; --nPos)
            {
            ctxElem.getBackingMapEntry(ContentKey.toBinary(f_nPartition, nChannel, lPage, nPos)).remove(false);
            }

        Usage usage = enlistUsage(nChannel);
        if (usage.getPartitionTail() == lPage)
            {
            usage.setPartitionHead(Page.NULL_PAGE);
            usage.setPartitionTail(Page.NULL_PAGE);
            }
        else // must be the head
            {
            usage.setPartitionHead(page.getNextPartitionPage());
            }

        // if there is a next page remove this page's reference from it
        long lPageNext = page.getNextPartitionPage();
        if (lPageNext != Page.NULL_PAGE)
            {
            BinaryEntry<Page.Key, Page> entryNext = enlistPageEntry(nChannel, lPage);
            Page                        pageNext  = entryNext.getValue();
            if (pageNext != null)
                {
                pageNext.adjustReferenceCount(-1);
                entryNext.setValue(pageNext);
                }
            }

        entryPage.remove(false);

        // notify any publishers waiting for space to free up
        notifyAll(usage.resetRemovalNotifiers());

        return true;
        }

    /**
     * Issue notifications
     *
     * @param anNotify  the notifiers of interest
     */
    public void notifyAll(int[] anNotify)
        {
        if (anNotify != null)
            {
            BackingMapContext ctxNotify = getBackingMapContext(PagedTopicCaches.Names.NOTIFICATIONS);
            int               nPart     = getPartition();
            for (int nNotify : anNotify)
                {
                ctxNotify.getBackingMapEntry(toBinaryKey(new NotificationKey(nPart, nNotify))).remove(false);
                }
            }
        }

    /**
     * Remove the subscription from the partition
     *
     * @param subscriberGroupId  the subscriber
     */
    @SuppressWarnings("unchecked")
    public void removeSubscription(SubscriberGroupId subscriberGroupId)
        {
        BackingMapContext ctxSubscriptions = getBackingMapContext(PagedTopicCaches.Names.SUBSCRIPTIONS);

        for (int nChannel = 0, c = getChannelCount(); nChannel < c; ++nChannel)
            {
            BinaryEntry<Subscription.Key, Subscription> entrySub  = (BinaryEntry) ctxSubscriptions.getBackingMapEntry(
                  toBinaryKey(new Subscription.Key(getPartition(), nChannel, subscriberGroupId)));

            Subscription subscription = entrySub.getValue();

            if (subscription == null)
                {
                // no-op
                return;
                }

            Usage usage = enlistUsage(nChannel);
            entrySub.remove(false);

            if (subscriberGroupId.getMemberTimestamp() != 0)
                {
                usage.removeAnonymousSubscriber(subscriberGroupId);
                }

            // detach the subscriber from it's active page chain
            long lPage = subscription.getPage();
            Page page  = lPage == Page.NULL_PAGE ? null : enlistPage(nChannel, lPage);

            if (subscription.getPosition() == Integer.MAX_VALUE || // subscriber drained (and detached from) page N before subsequent page was inserted
                page == null)                                      // partition was empty when the subscriber pinned and it has never re-visited the partition
                {
                // the subscriber is one page behind the head, i.e. it drained this partition before the next page was added
                // the subscriber had registered interest via usage in the next page, and thus if it has since been
                // added we have attached to it and must therefore detach

                // find the next page
                if (page == null)
                    {
                    // the drained page has since been deleted, thus usage.head must be the next page
                    lPage = usage.getPartitionHead();
                    }
                else
                    {
                    // the drained page still exists and if another page has been added it will reference it
                    lPage = page.getNextPartitionPage();
                    }

                if (lPage == Page.NULL_PAGE)
                    {
                    // the next page does not exist, thus we have nothing to detach from other then removing
                    // our interest in auto-attaching to the next insert
                    usage.adjustWaitingSubscriberCount(-1);
                    page = null;
                    }
                else
                    {
                    page = enlistPage(nChannel, lPage);
                    }
                }

            while (page != null && page.adjustReferenceCount(-1) == 0)
                {
                removePageIfNotRetainingElements(nChannel, lPage);

                // evaluate the next page
                lPage = page.getNextPartitionPage();
                page  = lPage == Page.NULL_PAGE ? null : enlistPage(nChannel, lPage);
                }
            }
        }

    /**
     * Ensure the subscription within a partition
     *
     * @param key        the subscription key
     * @param processor  the {@link EnsureSubscriptionProcessor} containing the subscription attributes
     *
     * @return for INQUIRE we return the currently pinned page, or null if unpinned
     *         for PIN we return the pinned page, or tail if the partition is empty and the former tail is known,
     *              if the partition was never used we return null
     *         for ADVANCE we return the pinned page
     */
    @SuppressWarnings("unchecked")
    public long[] ensureSubscription(Subscription.Key key, EnsureSubscriptionProcessor processor)
        {
        int                     nPhase                   = processor.getPhase();
        long[]                  alSubscriptionHeadGlobal = processor.getPages();
        Filter                  filter                   = processor.getFilter();
        Function                fnConvert                = processor.getConverter();
        long                    nSubscriberId            = processor.getSubscriberId();
        boolean                 fReconnect               = processor.isReconnect();
        boolean                 fCreateGroupOnly         = processor.isCreateGroupOnly();
        BackingMapContext       ctxSubscriptions         = getBackingMapContext(PagedTopicCaches.Names.SUBSCRIPTIONS);
        PagedTopic.Dependencies dependencies             = getDependencies();
        SubscriberGroupId       subscriberGroupId        = key.getGroupId();
        boolean                 fAnonymous               = subscriberGroupId.getMemberTimestamp() != 0;
        long[]                  alResult                 = new long[getChannelCount()];
        int                     cParts                   = getPartitionCount();
        int                     nSyncPartition           = Subscription.getSyncPartition(subscriberGroupId, 0, cParts);
        boolean                 fSyncPartition           = key.getPartitionId() == nSyncPartition;

        if (fCreateGroupOnly)
            {
            if (fAnonymous)
                {
                throw new IllegalArgumentException("Cannot specify create group only action for an anonymous subscriber");
                }
            // cannot be a reconnect and group creation only
            fReconnect = false;
            }

        switch (nPhase)
            {
            case EnsureSubscriptionProcessor.PHASE_INQUIRE:
                // avoid leaking notification registrations in unused channels
                cleanupSubscriberRegistrations();
                break;

            case EnsureSubscriptionProcessor.PHASE_PIN:
                // avoid leaking anon-subscribers when there are no active publishers
                cleanupNonDurableSubscribers(enlistUsage(0).getAnonymousSubscribers());
                break;

            default:
                break;
            }

        Subscription subscriptionZero = null;

        for (int nChannel = 0; nChannel < alResult.length; ++nChannel)
            {
            BinaryEntry<Subscription.Key, Subscription> entrySub  = (BinaryEntry) ctxSubscriptions.getBackingMapEntry(
                    toBinaryKey(new Subscription.Key(getPartition(), nChannel, subscriberGroupId)));

            Subscription subscription = entrySub.getValue();
            Usage        usage        = enlistUsage(nChannel);

            if (nPhase == EnsureSubscriptionProcessor.PHASE_INQUIRE) // common case
                {
                if (subscription == null || subscription.getSubscriptionHead() == Page.NULL_PAGE)
                    {
                    // group isn't fully initialized yet, force client to lock and retry with a PIN request
                    return null;
                    }

                if (filter != null && !Base.equals(subscription.getFilter(), filter))
                    {
                    // do not allow new subscriber instances to update the filter
                    throw new TopicException("Cannot change the Filter in existing Subscriber group \""
                            + subscriberGroupId.getGroupName() + "\" current="
                            + subscription.getFilter() + " new=" + filter);
                    }

                if (fnConvert != null && !Base.equals(subscription.getConverter(), fnConvert))
                    {
                    // do not allow new subscriber instances to update the converter function
                    throw new TopicException("Cannot change the converter in existing Subscriber group \""
                            + subscriberGroupId.getGroupName() + "\" current="
                            + subscription.getFilter() + " new=" + filter);
                    }

                // else; common case (subscription to existing group)

                PagedPosition headPosition = subscription.getHeadPosition();
                // if the head page is Page.EMPTY then nothing has been polled yet, so use the subscription
                // head, else use the rollback head
                alResult[nChannel] = headPosition.getPage() == Page.EMPTY
                    ? subscription.getSubscriptionHead()
                    : subscription.getRollbackPosition().getPage();
                }
            else if (nPhase == EnsureSubscriptionProcessor.PHASE_PIN &&
                     subscription == null) // if non-null another member beat us here in which case the final else will simply return the pinned page
                {
                subscription = new Subscription();

                if (fAnonymous)
                    {
                    // add the anonymous subscriber to the Usage data
                    enlistUsage(nChannel);
                    usage.addAnonymousSubscriber(subscriberGroupId);
                    }

                long lPage = dependencies.isRetainConsumed() ? usage.getPartitionHead() : usage.getPartitionTail();
                
                if (lPage == Page.NULL_PAGE)
                    {
                    // the partition is empty; register interest to auto-pin the next added page
                    usage.adjustWaitingSubscriberCount(1);

                    lPage = usage.getPartitionMax();

                    subscription.setPage(lPage);

                    if (lPage != Page.NULL_PAGE)
                        {
                        subscription.setPosition(Integer.MAX_VALUE); // remember we are not attached to this page
                        }
                    }
                else
                    {
                    enlistPage(nChannel, lPage).adjustReferenceCount(1);
                    subscription.setPage(lPage);
                    }

                subscription.setFilter(filter);
                subscription.setConverter(fnConvert);

                entrySub.setValue(subscription);

                alResult[nChannel] = lPage;
                }
            else if (nPhase == EnsureSubscriptionProcessor.PHASE_ADVANCE && // final initialization request
                     subscription.getSubscriptionHead() == Page.NULL_PAGE)  // the subscription has yet to be fully initialized
                {
                // final initialization phase; advance to first page >= lSubscriberHeadGlobal
                PagedPosition position = subscription.getRollbackPosition();
                long          lPage    = position.getPage();
                Page          page     = lPage == Page.NULL_PAGE ? null : enlistPage(nChannel, lPage);
                long          lHead    = usage.getPartitionHead();

                if (page == null && lHead != Page.NULL_PAGE)
                    {
                    // when we pinned the partition was apparently empty but there has been a subsequent insertion
                    // start the evaluation at the inserted page

                    // note: we we're already automatically attached
                    lPage = lHead;
                    page  = enlistPage(nChannel, lPage);
                    subscription.setPage(lPage);
                    subscription.setPosition(0);
                    }

                while (lPage < alSubscriptionHeadGlobal[nChannel] && page != null)
                    {
                    long lPageNext = page.getNextPartitionPage();
                    if (page.adjustReferenceCount(-1) == 0)
                        {
                        removePageIfNotRetainingElements(nChannel, lPage);
                        }

                    // attach to the next page
                    if (lPageNext == Page.NULL_PAGE)
                        {
                        page = null;
                        usage.adjustWaitingSubscriberCount(1);

                        // we leave the subscription pointing at the prior non-existent page; same as in poll
                        // the page doesn't exist and thus poll will advance to the polled page
                        subscription.setPosition(Integer.MAX_VALUE);
                        }
                    else
                        {
                        lPage = lPageNext;
                        page  = enlistPage(nChannel, lPage);

                        subscription.setPage(lPage);
                        page.adjustReferenceCount(1);
                        }
                    }

                if (lPage == alSubscriptionHeadGlobal[nChannel] && page != null)
                    {
                    int nPos;

                    if (dependencies.isRetainConsumed())
                        {
                        nPos = 0;
                        }
                    else
                        {
                        nPos = page.getTail() + 1;
                        }

                    subscription.setPosition(nPos);
                    }

                subscription.setSubscriptionHead(alSubscriptionHeadGlobal[nChannel]); // mark the partition subscription as fully initialized
                entrySub.setValue(subscription);
                }
            else // phase already completed
                {
                PagedPosition position = subscription.getRollbackPosition();
                alResult[nChannel] = position.getOffset() == Integer.MAX_VALUE
                    ? Page.NULL_PAGE // the page has been removed
                    : position.getPage();
                }

            if (!fCreateGroupOnly)
                {
                if (nSubscriberId != 0)
                    {
                    // This is not an anonymous subscriber (i.e. it is part of a group)
                    // Ensure the subscriber is registered and allocated channels. We only do this in channel zero, so as
                    // not to bloat all of the other entries with the subscriber maps
                    if (nChannel == 0)
                        {
                        subscriptionZero = subscription;
                        if (!subscriptionZero.hasSubscriber(nSubscriberId))
                            {
                            // this is a new subscriber and is not an anonymous subscriber (nSubscriberId != 0)
                            subscriptionZero.addSubscriber(nSubscriberId, getChannelCount());
                            if (fSyncPartition)
                                {
                                // we only log the update for the sync partition
                                // (no need to repeat the same message for every partition)
                                Logger.fine(String.format("Added subscriber %d in group %s allocations %s",
                                        nSubscriberId, subscriberGroupId, subscriptionZero.getAllocations()));
                                }

                            fReconnect = false; // reset reconnect flag as this is effectively a new subscriber
                            }
                        }

                    // Update the subscription/channel owner as it may have chaned if this is a new subscriber
                    long nOwner = subscriptionZero.getChannelOwner(nChannel);
                    subscription.setOwningSubscriber(nOwner);

                    if (fReconnect && nOwner == nSubscriberId)
                        {
                        // the subscriber is the channel owner and is reconnecting, so rollback to the last committed position
                        subscription.rollback();
                        }
                    }
                else
                    {
                    // This is an anonymous subscriber.
                    // We do not need to do channel allocation as anonymous subscribers have all channels
                    if (fReconnect)
                        {
                        // this is a reconnect so rollback
                        subscription.rollback();
                        }
                    }
                }

            entrySub.setValue(subscription);
            }

        return alResult;
        }

    /**
     * Close a subscription for a specific subscriber in a subscriber group
     * <p>
     * This will trigger a reallocation of channels across any remaining subscribers in the same group.
     * @param key            the subscribtion key
     * @param nSubscriberId  the unique subscriber identifier
     *
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void closeSubscription(Subscription.Key key, long nSubscriberId)
        {
        BackingMapContext ctxSubscriptions  = getBackingMapContext(PagedTopicCaches.Names.SUBSCRIPTIONS);
        long[]            alResult          = new long[getChannelCount()];
        SubscriberGroupId subscriberGroupId = key.getGroupId();
        int               cParts            = getPartitionCount();
        int               nSyncPartition    = Subscription.getSyncPartition(subscriberGroupId, 0, cParts);
        boolean           fSyncPartition    = key.getPartitionId() == nSyncPartition;

        Subscription subscriptionZero = null;

        for (int nChannel = 0; nChannel < alResult.length; ++nChannel)
            {
            BinaryEntry<Subscription.Key, Subscription> entrySub = (BinaryEntry) ctxSubscriptions.getBackingMapEntry(
                    toBinaryKey(new Subscription.Key(getPartition(), nChannel, subscriberGroupId)));

            Subscription subscription = entrySub.getValue();
            if (subscription != null)
                {
                // ensure the subscriber is registered and allocated channels
                // we only do this in channel zero, so as not to bloat all of the other entries with the subscriber maps
                if (subscriptionZero == null)
                    {
                    int cChannel     = getChannelCount();
                    subscriptionZero = subscription;
                    boolean fRemoved = subscriptionZero.removeSubscriber(nSubscriberId, cChannel);
                    if (fSyncPartition && fRemoved)
                        {
                        // we only log the update for the sync partition
                        // (no need to repeat the same message for every partition)
                        Logger.fine(String.format("Removed subscriber %d, member=%d from group '%s', remaining allocations %s",
                                nSubscriberId, PagedTopicSubscriber.memberIdFromId(nSubscriberId),
                                subscriberGroupId.getGroupName(), subscriptionZero.getAllocations()));
                        }
                    entrySub.setValue(subscription);
                    }

                long nOwner = subscriptionZero.getChannelOwner(nChannel);
                subscription.setOwningSubscriber(nOwner);
                entrySub.setValue(subscription);
                }
            }
        }

    /**
     * Update the heartbeat for a subscriber.
     *
     * @param entry  the {@link SubscriberInfo} entry
     */
    @SuppressWarnings("rawtypes")
    public void heartbeat(InvocableMap.Entry<SubscriberInfo.Key, SubscriberInfo> entry)
        {
        PagedTopic.Dependencies dependencies = getDependencies();
        SubscriberInfo          info         = entry.isPresent() ? entry.getValue() : new SubscriberInfo();
        long                    cMillis      = dependencies.getSubscriberTimeoutMillis();

        info.setLastHeartbeat(LocalDateTime.now());
        info.setTimeoutMillis(cMillis);
        entry.setValue(info);
        ((BinaryEntry) entry).expire(cMillis);
        }

    /**
     * Poll the element from the head of a subscriber's page.
     *
     * @param entrySubscription  subscriber entry for this partition
     * @param lPage              the page to poll from
     * @param cReqValues         the number of elements the subscriber is requesting
     * @param nNotifierId        notification key to notify when transitioning from empty
     * @param nSubscriberId      the unique identifier of the subscriber
     *
     * @return  the {@link Binary} value polled from the head of the subscriber page
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public PollProcessor.Result pollFromPageHead(BinaryEntry<Subscription.Key, Subscription> entrySubscription,
                                                 long lPage, int cReqValues, int nNotifierId, long nSubscriberId)
        {
        Subscription.Key keySubscription = entrySubscription.getKey();
        Subscription     subscription    = entrySubscription.getValue();
        int              nChannel        = keySubscription.getChannelId();

        if (subscription == null)
            {
            // the subscriber is unknown but we're allowing that as the client should handle it
            return new PollProcessor.Result(PollProcessor.Result.UNKNOWN_SUBSCRIBER, 0, null);
            }

        long nOwner = subscription.getOwningSubscriber();
        if (nOwner != 0 && nOwner != nSubscriberId)
            {
            // the subscriber does not own this channel, it should not have got here but it probably had out of date state
            return new PollProcessor.Result(PollProcessor.Result.NOT_ALLOCATED_CHANNEL, Integer.MAX_VALUE, null);
            }

        Page page      = peekPage(nChannel, lPage); // we'll later enlist but only if we've exhausted the page
        long lPageThis = subscription.getPage();
        int  nPos;

        if (lPage == lPageThis) // current page
            {
            nPos = subscription.getPosition();
            if (lPage == Page.NULL_PAGE || nPos == Integer.MAX_VALUE)
                {
                // the client is making a blind request, or
                // we'd previously exhausted and detached from this page, we can't just fall through
                // as the page has already been detached we can't allow a double detach
                return new PollProcessor.Result(PollProcessor.Result.EXHAUSTED, Integer.MAX_VALUE, null);
                }
            }
        else if (lPage < lPageThis) // read from fully consumed page
            {
            return new PollProcessor.Result(PollProcessor.Result.EXHAUSTED, Integer.MAX_VALUE, null);
            }
        else // otherwise lPage > lPageThis; first poll from page, start at the beginning
            {
            if (page == null) // first poll from a yet to exist page
                {
                // create it so that we can subscribe for notification if necessary
                // also this keeps overall logic a bit simpler
                page = ensurePage(nChannel, enlistPageEntry(nChannel, lPage));
                if (page == null)
                    {
                    return new PollProcessor.Result(PollProcessor.Result.EXHAUSTED, Integer.MAX_VALUE, null);
                    }
                }

            // note we've already attached indirectly, to get here we'd previously exhausted all
            // pages in this partition and registered our interest in new pages under the Usage waiting
            // subscriber counter which was then applied to this page when it was created
            nPos = 0;
            subscription.setPage(lPage);
            subscription.setPosition(nPos);
            }

        entrySubscription.setValue(subscription); // ensure any subsequent changes will be retained

        // find the next entry >= the nPos (note some elements may have expired)
        int                nPosTail    = page.getTail();
        BackingMapContext  ctxElements = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
        LinkedList<Binary> listValues  = new LinkedList();
        Filter             filter      = subscription.getFilter();
        Function           fnConvert   = subscription.getConverter();
        int                cbResult    = 0;
        final long         cbLimit     = getDependencies().getMaxBatchSizeBytes();

        Converter<Binary, Object> converterFrom = getValueFromInternalConverter();
        Converter<Object, Binary> converterTo   = getValueToInternalConverter();

        for (; cReqValues > 0 && nPos <= nPosTail && cbResult < cbLimit; ++nPos)
            {
            Binary      binPosKey    = ContentKey.toBinary(f_nPartition, nChannel, lPage, nPos);
            BinaryEntry entryElement = (BinaryEntry) ctxElements.getReadOnlyEntry(binPosKey);
            Binary      binValue     = entryElement == null ? null : entryElement.getBinaryValue();

            if (binValue != null && (filter == null || InvocableMapHelper.evaluateEntry(filter, entryElement)))
                {
                if (fnConvert != null)
                    {
                    binValue = PageElement.fromBinary(binValue, converterFrom).convert(fnConvert, converterTo);
                    }

                if (binValue != null)
                    {
                    listValues.add(binValue);
                    cbResult += binValue.length();
                    --cReqValues;
                    }
                }
            }
        // nPos now refers to the next one to read

        // if nPos is the past last element in the page and the page is sealed
        // then the subscriber has exhausted this page
        if (nPos > nPosTail && page.isSealed())
            {
            page = enlistPage(nChannel, lPage);

            long lPageNext = page.getNextPartitionPage();

            if (lPageNext == Page.NULL_PAGE)
                {
                // next page for this partition doesn't exist yet, register this subscriber's interest so that
                // that next page will be initialized with a reference count which includes this subscriber
                enlistUsage(nChannel).adjustWaitingSubscriberCount(1);

                subscription.setPosition(Integer.MAX_VALUE); // indicator that we're waiting to learn the next page
                }
            else
                {
                subscription.setPage(lPageNext);
                subscription.setPosition(0);
                }
            return new PollProcessor.Result(PollProcessor.Result.EXHAUSTED, nPos, listValues);
            }
        else
            {
            // Update the subscription entry with the value of the next position to poll
            subscription.setPosition(nPos);

            if (nPos > nPosTail)
                {
                // that position is currently empty; register for notification when it is set
                requestInsertionNotification(enlistPage(nChannel, lPage), nNotifierId, nChannel);
                }
            return new PollProcessor.Result(nPosTail - nPos + 1, nPos, listValues);
            }
        }

    /**
     * Commit the specified {@link Position} in this subscription.
     * <p>
     * The {@link Position} does not have to represent an actual page and offset in this subscription.
     * The actual position committed will be the position in this subscription that is <= the requested
     * position.
     *
     * @param entrySubscription  the subscription to commit the position in
     * @param position           the {@link Position} to commit
     * @param nSubscriberId      the identifier of the {@link Subscriber} performing the commit request
     *
     * @return the result fo the commit request
     */
    public Subscriber.CommitResult commitPosition(BinaryEntry<Subscription.Key,
            Subscription> entrySubscription, Position position, long nSubscriberId)
        {
        Subscription.Key keySubscription = entrySubscription.getKey();
        int              nChannel        = keySubscription.getChannelId();

        if (!entrySubscription.isPresent())
            {
            // the subscriber group is unknown
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.Rejected,
                    new IllegalStateException("Unknown subscriber group " + keySubscription.getGroupId()));
            }

        if (!(position instanceof PagedPosition))
            {
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.Rejected,
                    new IllegalArgumentException("Invalid position type"));
            }

        Subscription  subscription  = entrySubscription.getValue();
        long          nOwner        = subscription.getOwningSubscriber();
        boolean       fOwned        = nOwner == nSubscriberId;

        if (!fOwned && getDependencies().isOnlyOwnedCommits())
            {
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.Rejected,
                    new IllegalStateException("Attempted to commit a position in an unowned channel"));
            }

        long          lPage         = subscription.getPage();
        PagedPosition pagedPosition = (PagedPosition) position;
        long          lPageCommit   = pagedPosition.getPage();
        int           nPosCommit    = pagedPosition.getOffset();
        int           nOffset       = subscription.getPosition();

        if (lPage == Page.NULL_PAGE || (lPage < lPageCommit && nOffset != Integer.MAX_VALUE) || (lPage == lPageCommit && nOffset < nPosCommit))
            {
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.Rejected,
                    new IllegalArgumentException("Attempted to commit an unread position"));
            }

        PagedPosition committedPosition = subscription.getCommittedPosition();
        if (position.compareTo(committedPosition) <= 0)
            {
            // the subscription's commit is already ahead of the requested commit
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.AlreadyCommitted);
            }

        // At this point the subscription's position is >= the requested commit
        // The Page to be committed should exist because we do not remove uncommitted pages
        // The actual Page/offset to commit is the Page and offset in this partition <= the requested commit position.

        // Find the actual position to commit, walk back from the current position to find the page <= commit point
        // We must find a position as we retain uncommitted pages
        Page page        = peekPage(nChannel, lPage);
        long lPageActual = lPage;
        // walk back down the Page chain...
        while (page != null && lPageActual > lPageCommit)
            {
            long lPrev = page.getPreviousPartitionPage();
            if (lPrev == Page.NULL_PAGE)
                {
                break;
                }
            lPageActual = lPrev;
            page        = peekPage(nChannel, lPageActual);
            }

        if (page == null)
            {
            // the page has been removed so was already committed
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.AlreadyCommitted);
            }

        // lPageActual is now the page <= the requested commit
        if (lPageActual == lPageCommit)
            {
            // we have the actual committed page
            if (page.getTail() < nPosCommit)
                {
                nPosCommit = page.getTail();
                }
            }
        else if (lPageActual < lPageCommit || page.isSealedAndEmpty())
            {
            // the actual page is lower than the requested commit page
            // or the page is an empty sealed page inserted when a topic became full
            // so we commit its tail position
            lPageCommit = lPageActual;
            nPosCommit  = page.getTail();
            }
        else // actual page is higher than the requested page
            {
            // nothing to commit
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.NothingToCommit);
            }

        if (committedPosition.getPage() != Page.NULL_PAGE && committedPosition.compareTo(new PagedPosition(lPageCommit, nPosCommit)) >= 0)
            {
            // the subscription's commit is already ahead of the requested commit
            return new Subscriber.CommitResult(nChannel, position, CommitResultStatus.AlreadyCommitted);
            }

        // At this point page is the Page to be committed, lPageCommit is the page number and nPosCommit
        // is the offset within the page to commit

        LinkedList<Long> listPagesDereference = new LinkedList<>();
        PagedPosition    prevRollbackPosition = subscription.getRollbackPosition();
        long             lPagePrevRollback    = prevRollbackPosition.getPage();
        PagedPosition    commitPosition       = new PagedPosition(lPageCommit, nPosCommit);
        PagedPosition    rollbackPosition;

        if (page.isSealed() && nPosCommit >= page.getTail())
            {
            // the committed page is full and sealed and we must have read it
            // add to the list of pages to be de-referenced
            listPagesDereference.add(lPageActual);

            long lPageNext = page.getNextPartitionPage();

            // Check whether the page above the current page is a sealed empty page.
            // These pages are added by publishers when blocked by a full topic.
            // We need to remove these pages as part of this commit.
            // There could in theory be multiple of them, though unlikely
            Page pageNext = lPageNext == Page.EMPTY ? null : peekPage(nChannel, lPageNext);
            while (pageNext != null && pageNext.isSealed() && pageNext.getTail() == Page.EMPTY)
                {
                lPageActual = lPageNext;
                listPagesDereference.add(lPageActual);
                lPageNext = pageNext.getNextPartitionPage();
                pageNext = lPageNext != Page.NULL_PAGE ? peekPage(nChannel, lPageNext) : null;
                }

            if (lPageNext == Page.NULL_PAGE)
                {
                rollbackPosition = new PagedPosition(lPageNext, Integer.MAX_VALUE); // indicator that we're waiting to learn the next page
                }
            else
                {
                rollbackPosition = new PagedPosition(lPageNext, 0);
                }
            }
        else
            {
            rollbackPosition = new PagedPosition(lPageCommit, nPosCommit + 1);
            }

        subscription.setSubscriptionHead(pagedPosition.getPage());

        subscription.setCommittedPosition(commitPosition, rollbackPosition);
        entrySubscription.setValue(subscription);

        // Ensure any pages still left in the partition are removed if no longer referenced
        // Due to the way page reference counting works we remove bottom up, so we need to work out the lowest page
        // At this point page is the Page being committed, lPageCommit is the page number and nPosCommit is the offset

        long                        lPagePrev = page.getPreviousPartitionPage();
        BinaryEntry<Page.Key, Page> entry     = peekPageEntry(nChannel, lPagePrev);
        // walk down the pages but only as far as the previous rollback page
        // or we get to a page that is already removed
        while (entry != null && entry.isPresent() && lPagePrev >= lPagePrevRollback)
            {
            listPagesDereference.add(entry.getKey().getPageId());
            lPagePrev = entry.getValue().getPreviousPartitionPage();
            entry     = lPagePrev > Page.NULL_PAGE ? peekPageEntry(nChannel, lPagePrev) : null;
            }

        // Walk back up to the committed page adjusting the reference count and possibly removing the page
        // We do things in this order to ensure that pages are always enlisted bottom up in the same order
        // so that we do not deadlock with another thread that is also committing and trying to enlist pages
        if (!listPagesDereference.isEmpty())
            {
            long                        lPageFirst       = listPagesDereference.peekFirst();
            long                        lPageDereference = listPagesDereference.removeLast();
            BinaryEntry<Page.Key, Page> entryDereference = enlistPageEntry(nChannel, lPageDereference);
            Page                        pageDereference  = entryDereference.getValue();

            // adjust the reference count of the first page to remove (the lowest page number)
            if (pageDereference.adjustReferenceCount(-1) == 0)
                {
                removePageIfNotRetainingElements(nChannel, entryDereference.getKey().getPageId());
                }

            if (!pageDereference.isSubscribed())
                {
                // we were the last reference to the first page so remove all the other page
                // that only have a single reference
                while (!listPagesDereference.isEmpty())
                    {
                    lPageDereference = listPagesDereference.removeLast();
                    entryDereference = enlistPageEntry(nChannel, lPageDereference);
                    pageDereference  = entryDereference.getValue();

                    if (pageDereference.getReferenceCount() == 1)
                        {
                        // there is only one reference to the page, which must be this subscription
                        removePageIfNotRetainingElements(nChannel, entryDereference.getKey().getPageId());
                        }
                    else
                        {
                        break;
                        }
                    }
                }

            BinaryEntry<Page.Key, Page> entryFirst = enlistPageEntry(nChannel, lPageFirst);
            Page                        pageFirst  = entryFirst.isPresent() ? entryFirst.getValue() : null;

            if (pageFirst != null && pageFirst.isSubscribed())
                {
                // we didn't remove the first page in the list
                // so we need to bump the reference count on the next page above this page
                long lPageNext = pageFirst.getNextPartitionPage();
                if (lPageNext != Page.NULL_PAGE)
                    {
                    enlistPage(nChannel, lPageNext).adjustReferenceCount(1);
                    }
                }
            }

        CommitResultStatus status = fOwned ? CommitResultStatus.Committed : CommitResultStatus.Unowned;
        return new Subscriber.CommitResult(nChannel, position, status);
        }

    /**
     * Move the subscriber to a specified position.
     *
     * @param entrySubscription  the subscription entry
     * @param position           the position to seek to
     * @param nSubscriberId      the identifier of the seeking subscriber
     *
     * @return the result of the seek request
     */
    public SeekProcessor.Result seekPosition(BinaryEntry<Subscription.Key, Subscription> entrySubscription,
                                             PagedPosition position, long nSubscriberId)
        {
        Subscription  subscription = entrySubscription.getValue();
        int           nChannel     = entrySubscription.getKey().getChannelId();
        long          lPage        = subscription.getPage();
        long          lPageSeek    = position.getPage();
        int           nOffsetSeek  = position.getOffset();
        PagedPosition positionHead;
        PagedPosition positionSeek;

        if (subscription.getOwningSubscriber() != nSubscriberId)
            {
            throw new IllegalStateException("Subscriber is not allocated channel " + nChannel);
            }

        Page page;
        long lPageRollback;

        if (lPage > lPageSeek)
            {
            // this subscription is ahead the requested page, walk back to find the page
            // in this partition that is >= the page being seeked to

            page = peekPage(nChannel, lPage);
            lPageRollback = lPage;

            while (page != null)
                {
                lPageRollback   = page.getPreviousPartitionPage();
                if (lPageRollback != Page.NULL_PAGE && lPageRollback >= lPageSeek)
                    {
                    lPage = lPageRollback;
                    page  = peekPage(nChannel, lPage);
                    }
                else
                    {
                    // no more pages
                    break;
                    }
                }

            // lPage is now first page in this partition >= requested page
            // lPageNext is the next page or Page.NULL_PAGE if there is no next page
            // We now need to set the position to the NEXT offset after the seek page/offset
            // which may mean it is on the next page.
            }
        else
            {
            // this subscription is behind the required page, walk forward to find the page
            // in this partition that is >= the page being seeked to

            if (lPage == Page.NULL_PAGE)
                {
                // There is no page, the topic is probably totally empty
                return new SeekProcessor.Result(null, new PagedPosition(0L, 0));
                }

            page = peekPage(nChannel, lPage);
            lPageRollback = lPage;

            while (lPageRollback < lPageSeek)
                {
                lPageRollback = page.getNextPartitionPage();
                if (lPageRollback != Page.NULL_PAGE)
                    {
                    lPage = lPageRollback;
                    page  = peekPage(nChannel, lPage);
                    }
                else
                    {
                    // no more pages
                    break;
                    }
                }

            // lPage is now first page in this partition >= requested page
            // lPageNext is the next page or Page.NULL_PAGE if there is no next page
            // We now need to set the position to the NEXT offset after the seek page/offset
            // which may mean it is on the next page.
            }

        if (page == null)
            {
            // the requested page was removed, so it must have been full and read from
            subscription.setPage(lPage);
            subscription.setPosition(Integer.MAX_VALUE);
            // the result position is the next page after the requested page at offset zero
            positionHead = new PagedPosition(lPageRollback, 0);
            positionSeek = new PagedPosition(lPage - 1, Integer.MAX_VALUE);
            }
        else if (lPage == lPageSeek)
            {
            // lPage is the requested page
            if (page.isSealed() && nOffsetSeek >= page.getTail())
                {
                // we're seeking past the tail of the page and the page is sealed so position on
                // the next page in this partition at offset zero
                lPageRollback = page.getNextPartitionPage();
                if (lPageRollback == Page.NULL_PAGE)
                    {
                    // we do not know the next page so position on lPage at offset Integer.MAX_VALUE
                    subscription.setPage(lPage);
                    subscription.setPosition(Integer.MAX_VALUE);
                    }
                else
                    {
                    // we have a next page so position on that at offset zero
                    subscription.setPage(lPageRollback);
                    subscription.setPosition(0);
                    }
                // the result position is the next page after the requested page at offset zero
                positionHead = new PagedPosition(lPageSeek + 1, 0);
                positionSeek = new PagedPosition(lPageSeek, page.getTail());
                }
            else
                {
                // we can position on the next offset on the requested page
                subscription.setPage(lPage);
                subscription.setPosition(nOffsetSeek + 1);
                // the result position is also the next offset on lPage
                positionHead = new PagedPosition(lPage, nOffsetSeek + 1);
                positionSeek = new PagedPosition(lPage, nOffsetSeek);
                }
            }
        else if (lPage < lPageSeek)
            {
            // lPage is less than the requested page we need to position on the next page
            // at offset zero
            if (page.isSealed())
                {
                // lPage is full and sealed, position on offset zero on the next page in this partition
                lPageRollback = page.getNextPartitionPage();
                if (lPageRollback == Page.NULL_PAGE)
                    {
                    // we do not know what the next page in the partition is,
                    // so position on lPage at offset Integer.MAX_VALUE
                    subscription.setPage(lPage);
                    subscription.setPosition(Integer.MAX_VALUE);
                    }
                else
                    {
                    // we have a real next page so position at offset zero
                    subscription.setPage(lPageRollback);
                    subscription.setPosition(0);
                    }
                // the result position is the next page after the requested page at offset zero
                positionHead = new PagedPosition(lPageSeek + 1, 0);
                positionSeek = new PagedPosition(lPageSeek, page.getTail());
                }
            else
                {
                // Page lPage is not sealed so is not full, the requested position is after the
                // end of the current tail in this partition
                // we can position on the next offset on the Page lPage
                subscription.setPage(lPage);
                int nTail     = page.getTail();
                int nTailNext = nTail + 1;
                subscription.setPosition(nTailNext);
                positionHead = new PagedPosition(lPage, nTailNext);
                positionSeek = new PagedPosition(lPage, nTail);
                }
            }
        else // lPage > lPageSeek
            {
            // Page lPage is after the requested position so position on that page
            // at offset zero
            subscription.setPage(lPage);
            subscription.setPosition(0);
            // the result position is the next page after the requested page at offset zero
            positionHead = new PagedPosition(lPage, 0);
            positionSeek = new PagedPosition(lPage - 1, Integer.MAX_VALUE);
            }

        // if the commit position is after the new position roll it back too
        PagedPosition committed = subscription.getCommittedPosition();
        long          lPageCommitted   = committed.getPage();
        int           nOffsetCommitted = committed.getOffset();
        long          lPageSub         = subscription.getPage();
        int           nOffsetSub       = subscription.getPosition();
        if (lPageCommitted > lPageSub || (lPageCommitted == lPageSub && nOffsetCommitted > nOffsetSub))
            {
            PagedPosition posRollback = new PagedPosition(lPageSub, nOffsetSub);
            PagedPosition posCommit;
            if (nOffsetSub == 0)
                {
                page = peekPage(nChannel, lPageSub);
                posCommit = new PagedPosition(page.getPreviousPartitionPage(), page.getTail());
                }
            else
                {
                posCommit = new PagedPosition(lPageSub, nOffsetSub - 1);
                }
            subscription.setCommittedPosition(posCommit, posRollback);
            }

        // update the cached subscription
        entrySubscription.setValue(subscription);

        return new SeekProcessor.Result(positionHead, positionSeek);
        }

    /**
     * Move the subscriber to a specified position based on a timestamp.
     * <p>
     * The position seeked to will be such that the next element polled from the channel
     * will be have a timestamp <i>greater than</i> the specified timestamp.
     *
     * @param entrySubscription  the subscription entry
     * @param lTimestamp         the timestamp to use to determine the position to seek to
     * @param nSubscriberId      the identifier of the seeking subscriber
     *
     * @return the result of the seek request
     */
    public SeekProcessor.Result seekTimestamp(BinaryEntry<Subscription.Key, Subscription> entrySubscription,
                                              long lTimestamp, long nSubscriberId)
        {
        Subscription  subscription = entrySubscription.getValue();
        int           nChannel     = entrySubscription.getKey().getChannelId();
        long          lPage        = subscription.getPage();
        PagedPosition positionHead;
        PagedPosition positionSeek;

        if (subscription.getOwningSubscriber() != nSubscriberId)
            {
            throw new IllegalStateException("Subscriber is not allocated channel " + nChannel);
            }

        if (lPage == Page.NULL_PAGE)
            {
            // There is no page for the subscription in this partition
            return new SeekProcessor.Result(null, new PagedPosition(0L, 0));
            }

        Page page          = peekPage(nChannel, lPage);
        int  nMatch        = page == null ? 0 : page.compareTimestamp(lTimestamp);
        long lPageRollback = lPage;

        if (nMatch > 0)
            {
            // this subscription is after the requested timestamp, walk back to find the page
            // in this partition that contains the timestamp being seeked to

            while (page != null)
                {
                lPageRollback = page.getPreviousPartitionPage();
                nMatch        = page.compareTimestamp(lTimestamp);
                if (lPageRollback != Page.NULL_PAGE && nMatch >= 0)
                    {
                    lPage = lPageRollback;
                    page  = peekPage(nChannel, lPage);
                    }
                else
                    {
                    // no more pages
                    break;
                    }
                }

            // lPage is now first page in this partition >= requested timestamp
            // lPageNext is the next page or Page.NULL_PAGE if there is no next page
            // We now need to set the position to the NEXT offset after the seek page/offset
            // which may mean it is on the next page.
            }
        else if (nMatch < 0)
            {
            // this subscription is before the requested timestamp, walk forward to find the page
            // in this partition that contains the timestamp being seeked to

            while (page != null)
                {
                lPageRollback = page.getNextPartitionPage();
                nMatch        = page.compareTimestamp(lTimestamp);
                if (lPageRollback != Page.NULL_PAGE && nMatch < 0)
                    {
                    lPage = lPageRollback;
                    page  = peekPage(nChannel, lPage);
                    }
                else
                    {
                    // no more pages
                    break;
                    }
                }

            // lPage is now first page in this partition >= requested timestamp
            // lPageNext is the next page or Page.NULL_PAGE if there is no next page
            // We now need to set the position to the NEXT offset after the seek page/offset
            // which may mean it is on the next page.
            }
        // else the page contains the timestamp

        if (page == null)
            {
            // the requested page was removed, so it must have been full and read from
            subscription.setPage(lPage);
            subscription.setPosition(Integer.MAX_VALUE);
            // the result position is the next page after the requested page at offset zero
            positionHead = new PagedPosition(lPageRollback, 0);
            positionSeek = new PagedPosition(lPage - 1, Integer.MAX_VALUE);
            }
        else if (nMatch == 0)
            {
            // lPage contains the requested timestamp, find the element > the timestamp

            BackingMapContext         ctxElements   = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
            Converter<Binary, Object> converterFrom = getValueFromInternalConverter();
            int                       nPos          = 0;
            long                      lElementTime  = 0;

            for (; nPos < page.getTail() && lElementTime < lTimestamp; nPos++)
                {
                Binary         binPosKey    = ContentKey.toBinary(f_nPartition, nChannel, lPage, nPos);
                BinaryEntry    entryElement = (BinaryEntry) ctxElements.getReadOnlyEntry(binPosKey);
                PageElement<?> element      = PageElement.fromBinary(entryElement.getBinaryValue(), converterFrom);
                lElementTime = element.getTimestampMillis();
                }

            if (nPos >= page.getTail() && page.isSealed())
                {
                // we're seeking past the tail of the page and the page is sealed so position on
                // the next page in this partition at offset zero
                lPageRollback = page.getNextPartitionPage();
                if (lPageRollback == Page.NULL_PAGE)
                    {
                    // we do not know the next page so position on lPage at offset Integer.MAX_VALUE
                    subscription.setPage(lPage);
                    subscription.setPosition(Integer.MAX_VALUE);
                    }
                else
                    {
                    // we have a next page so position on that at offset zero
                    subscription.setPage(lPageRollback);
                    subscription.setPosition(0);
                    }
                // the result position is the next page after the requested page at offset zero
                positionHead = new PagedPosition(lPage + 1, 0);
                positionSeek = new PagedPosition(lPage, page.getTail());
                }
            else
                {
                // we can position on the offset on the requested page
                subscription.setPage(lPage);
                subscription.setPosition(nPos);
                // the result position is also the next offset on lPage
                positionHead = new PagedPosition(lPage, nPos);
                if (nPos > 0)
                    {
                    positionSeek = new PagedPosition(lPage, nPos - 1);
                    }
                else
                    {
                    long lPagePrev = page.getPreviousPartitionPage();
                    Page pagePrev  = lPagePrev == Page.EMPTY ? null : peekPage(nChannel, lPagePrev);
                    int  nTail     = pagePrev == null ? Integer.MAX_VALUE : pagePrev.getTail();
                    positionSeek   = new PagedPosition(lPagePrev, nTail);
                    }
                }
            }
        else if (nMatch < 0)
            {
            // lPage is less than the requested timestamp we need to position on the next page
            // at offset zero
            if (page.isSealed())
                {
                // lPage is full and sealed, position on offset zero on the next page in this partition
                lPageRollback = page.getNextPartitionPage();
                if (lPageRollback == Page.NULL_PAGE)
                    {
                    // we do not know what the next page in the partition is,
                    // so position on lPage at offset Integer.MAX_VALUE
                    subscription.setPage(lPage);
                    subscription.setPosition(Integer.MAX_VALUE);
                    }
                else
                    {
                    // we have a real next page so position at offset zero
                    subscription.setPage(lPageRollback);
                    subscription.setPosition(0);
                    }
                // the result position is the next page after the requested page at offset zero
                positionHead = new PagedPosition(lPage + 1, 0);
                positionSeek = new PagedPosition(lPage, page.getTail());
                }
            else
                {
                // Page lPage is not sealed so is not full, the requested position is after the
                // end of the current tail in this partition
                // we can position on the next offset on the Page lPage
                subscription.setPage(lPage);
                int nTail     = page.getTail();
                int nTailNext = nTail + 1;
                subscription.setPosition(nTailNext);
                positionHead = new PagedPosition(lPage, nTailNext);
                positionSeek = new PagedPosition(lPage, nTail);
                }
            }
        else // nMatch > 0
            {
            // Page lPage is after the requested timestamp so position on that page at offset zero
            subscription.setPage(lPage);
            subscription.setPosition(0);
            // the result position is the next page after the requested page at offset zero
            positionHead = new PagedPosition(lPage + 1, 0);
            positionSeek = new PagedPosition(lPage, page.getTail());
            }

        // if the commit position is after the new position roll it back too
        PagedPosition committed        = subscription.getCommittedPosition();
        long          lPageCommitted   = committed.getPage();
        int           nOffsetCommitted = committed.getOffset();
        long          lPageSub         = subscription.getPage();
        int           nOffsetSub       = subscription.getPosition();
        if (lPageCommitted > lPageSub || (lPageCommitted == lPageSub && nOffsetCommitted > nOffsetSub))
            {
            PagedPosition posRollback = new PagedPosition(lPageSub, nOffsetSub);
            PagedPosition posCommit;
            if (nOffsetSub == 0)
                {
                page = peekPage(nChannel, lPageSub);
                posCommit = new PagedPosition(page.getPreviousPartitionPage(), page.getTail());
                }
            else
                {
                posCommit = new PagedPosition(lPageSub, nOffsetSub - 1);
                }
            subscription.setCommittedPosition(posCommit, posRollback);
            }

        // update the cached subscription
        entrySubscription.setValue(subscription);

        return new SeekProcessor.Result(positionHead, positionSeek);
        }

    /**
     * Mark this subscriber as requiring notification upon the next insert.
     *
     * @param page              the page to watch
     * @param nNotifyPostEmpty  the notifier id
     * @param nChannel          the channel id to include in the notification
     */
    protected void requestInsertionNotification(Page page, int nNotifyPostEmpty, int nChannel)
        {
        // record that this subscriber requires notification on the next insert to this page
        page.addInsertionNotifier(nNotifyPostEmpty);
        BinaryEntry<NotificationKey, int[]> entry = enlistBackingMapEntry(PagedTopicCaches.Names.NOTIFICATIONS,
                toBinaryKey(new NotificationKey(getPartition(), nNotifyPostEmpty)));

        if (entry != null)  // entry can be null if topic is destroyed while a poll is in progress
            {
            entry.setValue(Arrays.binaryInsert(entry.getValue(), nChannel));
            // we expire in half the subscriber timeout time to ensure we see receive requests from waiting subscribers
            // so that they do not timeout
            entry.expire(getDependencies().getNotificationTimeout());
            }
        }

    /**
     * Mark this publisher as requiring notification upon the next removal.
     *
     * @param nNotifyPostFull  the notifier id
     * @param nChannel         the channel id to include in the notification
     */
    protected void requestRemovalNotification(int nNotifyPostFull, int nChannel)
        {
        // Register for removal interest in all non-empty channels; registering in all channels is a bit wasteful and
        // we could just choose to store all our removal registrations in channel 0.  The problem with that is that
        // page removal is frequent while filling a topic is not.  By placing all registrations in channel 0 we
        // would then require that every page remove operation for every channel to enlist the Usage entry for channel 0,
        // even if there are no registrations.  Instead we choose to replicate the registration in all channels which
        // could see a removal.
        for (int i = 0, c = getChannelCount(); i < c; ++i)
            {
            Usage usage = enlistUsage(i);
            if (usage.getPartitionHead() != Page.NULL_PAGE)
                {
                usage.addRemovalNotifier(nNotifyPostFull);

                BinaryEntry<NotificationKey, int[]> entry = enlistBackingMapEntry(PagedTopicCaches.Names.NOTIFICATIONS,
                        toBinaryKey(new NotificationKey(getPartition(), nNotifyPostFull)));

                // entry can be null if topic is destroyed while a poll is in progress
                if (entry != null)
                    {
                    entry.setValue(Arrays.binaryInsert(entry.getValue(), nChannel));

                    // page removal from the same partition will remove this entry and thus notify the client via a remove event
                    // but we could also gain local space by having a new cache server join the cluster and take some of our
                    // partitions.  I don't yet see a way to ensure we could detect this and do the remove, so instead we just
                    // auto-remove every so often.  The chances of us relying on this is fairly low as we'd have to be out of space
                    // have idle subscribers, and then add a cache server.
                    entry.expire(PUBLISHER_NOTIFICATION_EXPIRY_MILLIS);
                    }
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Obtain the {@link PagedTopic.Dependencies} for this topic.
     *
     * @return the {@link PagedTopic.Dependencies} for this topic
     */
    public PagedTopic.Dependencies getDependencies()
        {
        CacheService service = f_ctxManager.getCacheService();

        return service.getResourceRegistry().getResource(PagedTopic.Dependencies.class, f_sName);
        }

    /**
     * Obtain the backing map entry from the specified cache backing map
     * for the specified key.
     *
     * @param cacheName  the name of the backing map to obtain the entry for
     * @param binaryKey  the key of the entry to obtain
     * @param <K>        the type of the entry key
     * @param <V>        the type of the entry value
     *
     * @return the backing map entry from the specified cache backing map
     *         for the specified key
     */
    @SuppressWarnings("unchecked")
    protected <K, V> BinaryEntry<K, V> enlistBackingMapEntry(PagedTopicCaches.Names<K, V> cacheName, Binary binaryKey)
        {
        BackingMapContext context = getBackingMapContext(cacheName);

        if (context == null)
            {
            // this can happen if the topic, and hence its caches are destroyed whilst a poll is in progress
            return null;
            }

        return (BinaryEntry) context.getBackingMapEntry(binaryKey);
        }

    /**
     * Obtain the read only backing map entry from the specified cache backing map
     * for the specified key.
     *
     * @param cacheName  the name of the backing map to obtain the entry for
     * @param binaryKey  the key of the entry to obtain
     * @param <K>        the type of the entry key
     * @param <V>        the type of the entry value
     *
     * @return the read only backing map entry from the specified cache backing map
     *         for the specified key
     */
    @SuppressWarnings("unchecked")
    protected <K, V> BinaryEntry<K, V> peekBackingMapEntry(PagedTopicCaches.Names<K, V> cacheName,
            Binary binaryKey)
        {
        return (BinaryEntry) getBackingMapContext(cacheName).getReadOnlyEntry(binaryKey);
        }

    /**
     * Obtain the {@link BackingMapContext} for the specified cache.
     *
     * @param cacheName  the name of the cache
     *
     * @return the {@link BackingMapContext} for the specified cache
     */
    protected BackingMapContext getBackingMapContext(PagedTopicCaches.Names cacheName)
        {
        String            sCacheName = cacheName.cacheNameForTopicName(f_sName);
        BackingMapContext ctx        = f_ctxManager.getBackingMapContext(sCacheName);

        if (ctx == null)
            {
            throw new MapNotFoundException(sCacheName);
            }

        return ctx;
        }

    /**
     * Obtain the key to internal converter to use to convert keys
     * to {@link Binary} values.
     *
     * @param <F>  the type of the key
     *
     * @return the key to internal converter to use to convert keys
     *         to {@link Binary} values
     */
    @SuppressWarnings("unchecked")
    protected <F> Converter<F, Binary> getKeyToInternalConverter()
        {
        return f_ctxManager.getKeyToInternalConverter();
        }

    /**
     * Serialize the specified key into its Binary form.
     *
     * @param o  the key
     *
     * @return the binary
     */
    @SuppressWarnings("unchecked")
    public Binary toBinaryKey(Object o)
        {
        return (Binary) f_ctxManager.getKeyToInternalConverter().convert(o);
        }

    /**
     * Obtain the value from internal converter to use to deserialize
     * {@link Binary} values.
     *
     * @param <F>  the type of the value
     *
     * @return the value from internal converter to use to deserialize
     *         {@link Binary} values
     */
    @SuppressWarnings("unchecked")
    protected <F> Converter<Binary, F> getValueFromInternalConverter()
        {
        return f_ctxManager.getValueFromInternalConverter();
        }

    /**
     * Obtain the value to internal converter to use to serialize
     * values to {@link Binary}.
     *
     * @param <F>  the type of the value
     *
     * @return the value from internal converter to use to serialize
     *         values to {@link Binary}
     */
    @SuppressWarnings("unchecked")
    protected <F> Converter<F, Binary> getValueToInternalConverter()
        {
        return f_ctxManager.getValueToInternalConverter();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The interval at which publisher notifications will expire.
     */
    public static final long PUBLISHER_NOTIFICATION_EXPIRY_MILLIS = new Duration(
            Config.getProperty("coherence.pagedTopic.publisherNotificationExpiry", "10s"))
                .as(Duration.Magnitude.MILLI);

    // ----- data members ---------------------------------------------------

    /**
     * The {@link BackingMapManagerContext} for the caches underlying this topic.
     */
    protected final BackingMapManagerContext f_ctxManager;

    /**
     * The name of this topic.
     */
    protected final String f_sName;

    /**
     * The partition.
     */
    protected final int f_nPartition;
    }
