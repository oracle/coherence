/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.topic.impl.paged;

import com.oracle.coherence.common.base.Converter;
import com.oracle.coherence.common.collections.Arrays;
import com.oracle.coherence.common.util.Duration;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.topic.impl.paged.agent.EnsureSubscriptionProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.OfferProcessor;
import com.tangosol.internal.net.topic.impl.paged.agent.PollProcessor;
import com.tangosol.internal.net.topic.impl.paged.model.NotificationKey;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.internal.net.topic.impl.paged.model.Position;
import com.tangosol.internal.net.topic.impl.paged.model.SubscriberGroupId;
import com.tangosol.internal.net.topic.impl.paged.model.Subscription;
import com.tangosol.internal.net.topic.impl.paged.model.Usage;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.cache.ConfigurableCacheMap;
import com.tangosol.net.cache.LocalCache;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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

        if (!page.isSubscribed() && !getTopicConfiguration().isRetainConsumed())
            {
            // there are no subscribers we can simply accept but do not store the content
            // note, we don't worry about this condition when the page is not empty as such a condition
            // can't exist, as pages are deleted as part of unsubscribing
            // Note: we also seal and remove the page, this will cause publisher to advance the page
            // and touch other partitions, cleaning up any anonymous subscribers there as well

            page.setSealed(true);
            removePageIfNotRetainingElements(nChannel, lPage);
            return new OfferProcessor.Result(OfferProcessor.Result.Status.PageSealed, cElements, cbCapPage);
            }
        else if (cbCapServer > 0 && getStorageBytes() >= cbCapServer)
            {
            if (nNotifyPostFull != 0)
                {
                requestRemovalNotification(nNotifyPostFull, nChannel);
                }

            // attempt to free up space by having subscribers remove non-full pages, remember we can't
            // add to them anyway.  To trigger the removal we seal the tail page and then notify the
            // subscribers, thus causing them to revisit the page and detach from it which will eventually
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
                // else; if there are other full pages and we aren't preventing there removal
                }

            return new OfferProcessor.Result(OfferProcessor.Result.Status.TopicFull, 0, cbCapPage);
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
        Configuration           configuration = getTopicConfiguration();
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

                // TODO: base on the current storage rather then estimate, this is especially important if
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
            return new OfferProcessor.Result(OfferProcessor.Result.Status.PageSealed, 0, 0);
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
        long                         cMillisExpiry = configuration.getElementExpiryMillis();
        int                          cbPage        = page.getByteSize();
        int                          nTail         = page.getTail();
        OfferProcessor.Result.Status status        = OfferProcessor.Result.Status.Success;
        int                          cAccepted     = 0;

        // Iterate over all of the elements to be offered until they are
        // all offered or until the page has reached maximum capacity
        for (Iterator<Binary> iter = listElements.iterator(); iter.hasNext() && cbPage < cbCapPage; ++cAccepted)
            {
            Binary      binElement      = iter.next();
            Binary      binKey          = Position.toBinary(f_nPartition, nChannel, lPage, ++nTail);
            BinaryEntry binElementEntry = (BinaryEntry) ctxContent.getBackingMapEntry(binKey);

            cbPage += binElement.length();

            // Set the binary element as the entry value
            binElementEntry.updateBinaryValue(binElement);

            // If the topic is configured with expiry then set this on the element entry
            if (cMillisExpiry > LocalCache.DEFAULT_EXPIRE)
                {
                binElementEntry.expire(cMillisExpiry);
                }
            }

        // Update the tail element pointer for the page
        page.setTail(nTail);

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

        return new OfferProcessor.Result(status, cAccepted, cbRemainingCapacity);
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

            enlistPage(nChannel, lTailPrev).setNextPartitionPage(lPage);
            }

        // attach on behalf of waiting subscribers, they will have to find this page on their own
        page.adjustReferenceCount(usage.resetWaitingSubscriberCount());

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
            List<SubscriberGroupId> listPeer  = mapDead.get(ldtMember);
            if (listPeer == null)
                {
                listPeer = new ArrayList<>();
                mapDead.put(ldtMember, listPeer);
                }
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
        return PagedTopicCaches.getChannelCount(getPartitionCount());
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
    @SuppressWarnings("unchecked")
    protected Page peekPage(int nChannel, long lPageId)
        {
        BinaryEntry<Page.Key,Page> entry =  peekBackingMapEntry(PagedTopicCaches.Names.PAGES, toBinaryKey(new Page.Key(nChannel, lPageId)));
        return entry == null ? null : entry.getValue();
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
    @SuppressWarnings("unchecked")
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
        Configuration configuration = getTopicConfiguration();

        if (configuration.isRetainConsumed())
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
        Page page      = entryPage.getValue();
        if (page == null)
            {
            return false;
            }

        BackingMapContext ctxElem = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
        for (int nPos = page.getTail(); nPos >= 0; --nPos)
            {
            ctxElem.getBackingMapEntry(Position.toBinary(f_nPartition, nChannel, lPage, nPos)).remove(false);
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
     * @param subscriberGroupId         the subscriber id
     * @param nPhase                    EnsureSubscriptionProcessor.PHASE_INQUIRE/PIN/ADVANCE
     * @param alSubscriptionHeadGlobal  the page to advance to
     * @param filter                    the subscriber's filter or null
     * @param fnConvert                 the optional subscriber's converter function
     *
     * @return for INQUIRE we return the currently pinned page, or null if unpinned
     *         for PIN we return the pinned page, or tail if the partition is empty and the former tail is known,
     *              if the partition was never used we return null
     *         for ADVANCE we return the pinned page
     */
    public long[] ensureSubscription(SubscriberGroupId subscriberGroupId,
                                     int               nPhase,
                                     long[]            alSubscriptionHeadGlobal,
                                     Filter            filter,
                                     Function          fnConvert)
        {
        BackingMapContext ctxSubscriptions = getBackingMapContext(PagedTopicCaches.Names.SUBSCRIPTIONS);
        Configuration     configuration    = getTopicConfiguration();
        long[]            alResult         = new long[getChannelCount()];

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

                if (!Base.equals(subscription.getFilter(), filter))
                    {
                    // allow new subscriber instances to update the filter
                    subscription.setFilter(filter);
                    entrySub.setValue(subscription);
                    }

                if (!Base.equals(subscription.getConverter(), fnConvert))
                    {
                    // allow new subscriber instances to update the converter function
                    subscription.setConverter(fnConvert);
                    entrySub.setValue(subscription);
                    }

                // else; common case (subscription to existing group)

                alResult[nChannel] = subscription.getPosition() == Integer.MAX_VALUE
                    ? Page.NULL_PAGE // the page has been removed
                    : subscription.getPage();
                }
            else if (nPhase == EnsureSubscriptionProcessor.PHASE_PIN &&
                     subscription == null) // if non-null another member beat us here in which case the final else will simply return the pinned page
                {
                subscription = new Subscription();

                if (subscriberGroupId.getMemberTimestamp() != 0)
                    {
                    // add the anonymous subscriber to the Usage data
                    enlistUsage(nChannel);
                    usage.addAnonymousSubscriber(subscriberGroupId);
                    }

                long lPage = configuration.isRetainConsumed() ? usage.getPartitionHead() : usage.getPartitionTail();
                
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

                long lPage = subscription.getPage();
                Page page  = lPage == Page.NULL_PAGE ? null : enlistPage(nChannel, lPage);
                long lHead = usage.getPartitionHead();

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

                    if (configuration.isRetainConsumed())
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
                alResult[nChannel] = subscription.getPosition() == Integer.MAX_VALUE
                    ? Page.NULL_PAGE // the page has been removed
                    : subscription.getPage();
                }
            }

        return alResult;
        }

    /**
     * Poll the element from the head of a subscriber's page.
     *
     * @param entrySubscription  subscriber entry for this partition
     * @param lPage              the page to poll from
     * @param cReqValues         the number of elements the subscriber is requesting
     * @param nNotifierId        notification key to notify when transitioning from empty
     *
     * @return  the {@link Binary} value polled from the head of the subscriber page
     */
    @SuppressWarnings("unchecked")
    public PollProcessor.Result pollFromPageHead(BinaryEntry<Subscription.Key, Subscription> entrySubscription, long lPage, int cReqValues, int nNotifierId)
        {
        Subscription.Key keySubscription = entrySubscription.getKey();
        Subscription     subscription    = entrySubscription.getValue();
        int              nChannel        = keySubscription.getChannelId();

        if (subscription == null)
            {
            // the subscriber is unknown but we're allowing that as the client should handle it
            return new PollProcessor.Result(PollProcessor.Result.UNKNOWN_SUBSCRIBER, 0, null);
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
        int               nPosTail    = page.getTail();
        BackingMapContext ctxElements = getBackingMapContext(PagedTopicCaches.Names.CONTENT);
        ArrayList<Binary> listValues  = new ArrayList<>(Math.min(cReqValues, (nPosTail - nPos) + 1));
        Filter            filter      = subscription.getFilter();
        Function          fnConvert   = subscription.getConverter();
        int               cbResult    = 0;
        final long        cbLimit     = getTopicConfiguration().getMaxBatchSizeBytes();

        for (; cReqValues > 0 && nPos <= nPosTail && cbResult < cbLimit; ++nPos)
            {
            Binary      binPosKey    = Position.toBinary(f_nPartition, nChannel, lPage, nPos);
            BinaryEntry entryElement = (BinaryEntry) ctxElements.getReadOnlyEntry(binPosKey);

            Binary binValue = entryElement == null ? null : entryElement.getBinaryValue();
            if (binValue != null && (filter == null || InvocableMapHelper.evaluateEntry(filter, entryElement)))
                {
                if (fnConvert != null)
                    {
                    Object oValue     = getValueFromInternalConverter().convert(binValue);
                    Object oConverted = fnConvert.apply(oValue);

                    if (oConverted == null)
                        {
                        binValue = null;
                        }
                    else
                        {
                        binValue = getValueToInternalConverter().convert(oConverted);
                        }
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

            // detach from this page and move to next
            if (page.adjustReferenceCount(-1) == 0)
                {
                removePageIfNotRetainingElements(nChannel, lPage);
                }

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

                // We've detached from our old page, and now need to attach to the next page.  If we've removed
                // the old page then we also need to decrement the attach count on the next page.  Thus in the
                // case of a removal of the old page we have nothing to do as we'd just increment and decrement
                // the attach count on the next page.

                if (page.isSubscribed()) // check if the old page still has subscribers
                    {
                    // we didn't remove the old page, thus we can't take ownership of its ref to the next page
                    enlistPage(nChannel, lPageNext).adjustReferenceCount(1);
                    }
                // else; optimization avoid decrement+increment on next page
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
     * Obtain the {@link Configuration} for this topic.
     *
     * @return the {@link Configuration} for this topic
     */
    public Configuration getTopicConfiguration()
        {
        CacheService service = f_ctxManager.getCacheService();

        return service.getResourceRegistry().getResource(Configuration.class, f_sName);
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
