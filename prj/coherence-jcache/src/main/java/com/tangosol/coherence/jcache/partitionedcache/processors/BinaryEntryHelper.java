/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.partitionedcache.processors;

import com.tangosol.coherence.jcache.CoherenceBasedCache;
import com.tangosol.coherence.jcache.common.JCacheContext;
import com.tangosol.coherence.jcache.common.JCacheEntryMetaInf;
import com.tangosol.coherence.jcache.common.JCacheIdentifier;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ResourceRegistry;

import java.util.concurrent.ConcurrentSkipListSet;

import javax.cache.configuration.CompleteConfiguration;

import javax.cache.expiry.ExpiryPolicy;

/**
 * A collection of helper methods that operate on {@link BinaryEntry}s.
 * <p>
 * Terminology:
 * <ul>
 *      <li>SYNTHETIC: an operation caused by some internal function, not requested by an application.
 *          eg: a synthetic update is an update that was caused internal operation, but not requested
 *              by an application</li>
 * </ul>
 *
 * @author jf  2013.07.02
 * @author bo  2014.01.14
 * @since Coherence 12.1.3
 */
public class BinaryEntryHelper
    {
    // ----- BinaryEntryHelper methods --------------------------------------

    /**
     * evaluate if binary entry with value binValue is considered expired at ldtNow.
     *
     * @param ctx native Coherence BackingMapManagerContext that binValue is being evaluated within
     * @param binValue binaryEntry's value
     * @param metaInf  JCache meta info extracted from binValue as DECO_JCACHE decoration
     * @param ldtNow  current time in milliseconds
     *
     * @return true iff binValue should be considered expired as computed via JCache expiry policy and
     * not accessible for JCache client.
     */
    public static boolean isExpired(BackingMapManagerContext ctx, Binary binValue, JCacheEntryMetaInf metaInf,
                                    long ldtNow)
        {
        if (binValue != null && metaInf != null && metaInf.isExpiredAt(ldtNow))
            {
            return true;
            }
        else
            {
            Byte bJCacheSynthetic = getJCacheSyntheticKind(binValue, ctx);

            return JCACHE_SYNTHETIC_CLEAR.equals(bJCacheSynthetic) || JCACHE_SYNTHETIC_EXPIRY.equals(bJCacheSynthetic);
            }
        }

    /**
     * compute from JCACHE decorations on binEntry.getValue() if entry is considered expired by Coherence
     * JCache Meta info.
     *
     * @param binEntry the binary entry being checked for expiration
     * @param ldtNow current time when current entry processor invocation started
     *
     * @return true iff <code>binEntry</code> is considered expired via JCache expiry policy semantics
     */
    public static boolean isExpired(BinaryEntry binEntry, long ldtNow)
        {
        return binEntry.isPresent()
               ? BinaryEntryHelper.isExpired(binEntry.getContext(), binEntry.getBinaryValue(),
                   BinaryEntryHelper.getValueMetaInf(binEntry), ldtNow) : false;
        }

    /**
     * Evaluate if <code>binEntry</code> is expired at <code>ldtNow</code> based on expiry in
     * JCache <code><metaInf</code>.
     *
     * @param binEntry a binary entry
     * @param metaInf  JCache meta info concerning binEntry
     * @param ldtNow  current time
     *
     * @return true iff binEntry should be considered expired by JCache specification and
     * must not be accessible to JCache client.
     */
    public static boolean isExpired(BinaryEntry binEntry, JCacheEntryMetaInf metaInf, long ldtNow)
        {
        return binEntry.isPresent()
               ? isExpired(binEntry.getContext(), binEntry.getBinaryValue(), metaInf, ldtNow) : false;
        }

    /**
     * decorate binValue with new JCache MetaInf
     *
     * @param binValue initial binValue
     * @param metaInf  updated JCache meta info.
     * @param ctx      native BackingMapManagerContext used for decorating internal form binValue.
     *
     * @return binValue decorated with new JCacheMetaInf
     */
    public static Binary decorateBinValueWithJCacheMetaInf(Binary binValue, JCacheEntryMetaInf metaInf,
        BackingMapManagerContext ctx)
        {
        if (ctx.isInternalValueDecorated(binValue, ExternalizableHelper.DECO_JCACHE))
            {
            binValue = (Binary) ctx.removeInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE);
            }

        binValue = (Binary) ctx.addInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE, metaInf);

        return binValue;
        }

    /**
     * Decorate a binValue with a DECO_JCACHE_SYNTHETIC set to true.
     * This decoration exists on a binary entry it indicates that a native coherence update
     * is considered synthetic JCache update.
     *
     * For example, updating the ACCESSED meta info for a binValue in GetProcessor.
     * This decoration is used in distinguishing when coherence cache entry update events should
     * be propagated to JCache CacheEntryListener(s).
     *
     * @param binValue to add the decoration to
     * @param ctx      context to use to decorate the binValue
     * @param bJCacheSyntheticUpdate  one of {@link BinaryEntryHelper#JCACHE_SYNTHETIC_CLEAR},
     *                           {@link BinaryEntryHelper#JCACHE_SYNTHETIC_EXPIRY},
     *                                {@link BinaryEntryHelper#JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES},
     *                               or {@link BinaryEntryHelper#JCACHE_SYNTHETIC_UPDATE}
     *
     * @return binValue with decoration DECO_JCACHE_SYNTHETIC set to SyntheticUpdateKind.
     */
    public static Binary decorateUpdateJCacheSynthetic(Binary binValue, BackingMapManagerContext ctx,
        Byte bJCacheSyntheticUpdate)
        {
        binValue = (Binary) ctx.removeInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC);
        binValue = (Binary) ctx.addInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC,
            bJCacheSyntheticUpdate);

        return binValue;
        }

    /**
     *  implements lazy expiration of entry when encountered during JCache entry processing operations.
     *
     *  Note: Originally implemented just relying on Coherence isSynthetic flag and that was discovered to
     *  not be propagated to client side cache entry listeners.  While that has been fixed in Coherence 12.1.3,
     *  there has not been time to see how much Coherence synthetic flag can be used. A complication is
     *  the JCache synthetic flag is been used to represent 5 distinct states needed to implement
     *  JCache CacheEntryListeners and to avoid writeThrough of entries that were just loaded. (The JCache
     *  synthethic updates to loaded entries cause it to want to write-through the just loaded entry without
     *  adding a JCache synthetic decoration representing that an entry was just loaded and should not
     *  be considered for write-through.
     *
     *  @param binEntryExpired expired binary entry.
     */
    public static void expireEntry(BinaryEntry binEntryExpired)
        {
        syntheticUpdateToEntry(binEntryExpired, JCACHE_SYNTHETIC_EXPIRY);
        }

    /**
     * Update native entry to enforce JCache semantics denoted by bJCacheSyntheticKind.
     * These updates do not generate JCache Modify.
     *
     * @param binEntry  native binEntry
     * @param bJCacheSyntheticKind  synthetic update to implement this JCache semantics.
     */
    public static void syntheticUpdateToEntry(BinaryEntry binEntry, byte bJCacheSyntheticKind)
        {
        // Current coherence does not have the ability to expire immediately.
        // Thus, without workaround, some coherence update events are generated by marking
        // an entry to expire in 1 millisecond.
        // If and when coherence implements immediate expire for 1L, the jcachesynthetic may be able to be
        // removed.
        // synthetic remove of an expiring entry.
        Binary binValue = decorateUpdateJCacheSynthetic(binEntry.getBinaryValue(), binEntry.getContext(),
                              bJCacheSyntheticKind);

        binEntry.updateBinaryValue(binValue);

        if (bJCacheSyntheticKind == JCACHE_SYNTHETIC_CLEAR || bJCacheSyntheticKind == JCACHE_SYNTHETIC_EXPIRY)
            {
            // there is no expire immediately.
            // additionally, there is no way to distinguish between the different kinds of JCache synthetic
            // updates using Coherence synthetic remove.  One can not decorate a null binValue indicating removal.
            binEntry.expire(1l);
            }
        }

    /**
     * Returns the jcache synthetic update kind decorated on binValue, if one exists
     *
     * @param binValue to test for decoration
     * @param ctx      context to use to check if decoration is in binValue.
     *
     * @return return JCache synthetic kind decorated on binValue, null if last update was not considered JCache
     * synthetic update.
     */
    public static Byte getJCacheSyntheticKind(Binary binValue, BackingMapManagerContext ctx)
        {
        return binValue == null
               ? null : (Byte) ctx.getInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC);
        }

    /**
     * Get the JCache Meta Information related to an entries value.
     *
     * @param binEntry get the JCache Meta Information that is a DECO_JCACHE decoration on the binEntry's value.
     * @return JCacheEntryMetaInf decoration from value, null if none yet.
     */
    public static JCacheEntryMetaInf getValueMetaInf(BinaryEntry binEntry)
        {
        // only load present binary entries, do not allow chance of cache loading here.
        Binary binValue = binEntry.isPresent() ? binEntry.getBinaryValue() : null;

        return getValueMetaInf(binValue, binEntry.getContext());
        }

    /**
     * return the JCache Meta information associated with binValue
     *
     * @param binValue  a binary value decorated with JCache meta info.
     * @param ctx       context
     *
     * @return  JCache meta info decorated on binValue (and the entry it came from or is going to be assigned to)
     */
    public static JCacheEntryMetaInf getValueMetaInf(Binary binValue, BackingMapManagerContext ctx)
        {
        return binValue == null
               ? null : (JCacheEntryMetaInf) ctx.getInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE);
        }

    /**
     * remove {@link ExternalizableHelper#DECO_JCACHE_SYNTHETIC} decoration from a bjnValue
     *
     * @param binValue to remove decoration from
     * @param ctx      to use to remove decoration
     *
     * @return binValue without this decoration on it.
     */
    public static Binary removeJCacheSyntheticDecoration(Binary binValue, BackingMapManagerContext ctx)
        {
        Binary binResult = binValue;

        while (ctx.isInternalValueDecorated(binResult, ExternalizableHelper.DECO_JCACHE_SYNTHETIC))
            {
            binResult = (Binary) ctx.removeInternalValueDecoration(binValue,
                ExternalizableHelper.DECO_JCACHE_SYNTHETIC);
            }

        assert(!ctx.isInternalValueDecorated(binResult, ExternalizableHelper.DECO_JCACHE_SYNTHETIC));

        return binResult;
        }

    /**
     * Decorate binValue with special decoration that indicates that EntryProcessor is reusing an entry.
     *
     * @param binValue the value of an entry that is being reused withing a single EntryProcessor invocation.
     * @param ctx  to use for decorating {code}binValue{/code}.
     *
     * @return the binValue decorated with {@link ExternalizableHelper#DECO_JCACHE_SYNTHETIC} with a value of
     * {@link BinaryEntryHelper#JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES}.
     */
    public static Binary jcacheSyntheticExpiryEventForReusedBinaryEntry(Binary binValue, BackingMapManagerContext ctx)
        {
        return BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, ctx,
            JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES);
        }

    /**
     * Return true if binEntry's last modification should be considered a synthetic update
     * (an update for bookkeeping of JCache meta info, not an update caused by a JCache client.)
     *
     * @param binEntry to evaluate if its value's update is considered synthetic
     *
     * @return true iff there is a {@link ExternalizableHelper#DECO_JCACHE_SYNTHETIC} decoration on binEntry value and
     * it is not {@link BinaryEntryHelper#JCACHE_SYNTHETIC_LOADED}.
     */
    public static boolean isJCacheSynthetic(BinaryEntry binEntry)
        {
        assert(binEntry != null);

        BackingMapManagerContext ctx      = binEntry.getContext();
        Binary                   binValue = binEntry.getBinaryValue();

        assert(ctx != null);

        if (binValue == null)
            {
            return false;
            }

        Byte result = (Byte) ctx.getInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

        return result == null || result.equals(JCACHE_SYNTHETIC_LOADED) ? false : true;
        }

    /**
     * Evaluate if binary entry has been updated due a JCache client operation or a JCache synthetic only update.
     *
     * @param binEntry a binary entry
     *
     * @return true iff the binEntry should not be writeThrough
     */
    public static boolean isJCacheSyntheticOrLoaded(BinaryEntry binEntry)
        {
        assert(binEntry != null);

        BackingMapManagerContext ctx      = binEntry.getContext();
        Binary                   binValue = binEntry.getBinaryValue();

        assert(ctx != null);

        if (binValue == null)
            {
            return false;
            }

        Byte result = (Byte) ctx.getInternalValueDecoration(binValue, ExternalizableHelper.DECO_JCACHE_SYNTHETIC);

        return result == null ? false : true;
        }

    /**
     * Given context provided by binEntry, get a resource registry.
     *
     * TODO: current implementation was not able to use resource registry scoped to CCF used to create
     * coherence cache associated with jcache.  Currently using global resource registry associated with
     * JCache meta cache.  Still pass in context in case able to switch back to original design in future.
     * The PartitionedCacheBinaryEntryStore constructor is unable to get the CCF context or cache manager URI needed
     * to look up resource registry associated with ccf used to create coherence named cache associated with
     * JCache cache.
     *
     * @param binEntry context
     *
     * @return resource registry
     */
    public static ResourceRegistry getResourceRegistry(BinaryEntry binEntry)
        {
        ConfigurableCacheFactory ccf = binEntry.getContext().getManager().getCacheFactory();

        return ccf.getResourceRegistry();
        }

    public static JCacheContext getContext(JCacheIdentifier cacheId, BinaryEntry binEntry)
        {
        ResourceRegistry reg = BinaryEntryHelper.getResourceRegistry(binEntry);
        JCacheContext    ctx = JCacheContext.getContext(reg, cacheId);

        if (ctx == null)
            {
            CompleteConfiguration config = getCacheToConfigurationMapping(binEntry, cacheId);

            ctx = JCacheContext.getContext(reg, cacheId, config);
            }

        return ctx;
        }

    /**
     * Obtains the {@link com.tangosol.net.NamedCache} to be used for storing
     * {@link com.tangosol.coherence.jcache.CoherenceBasedConfiguration}s when they
     * need to be shared across a cluster or made available to clients.
     *
     * @param binEntry  the {@link BinaryEntry} to use to locate the appropriate Configuration cache
     *
     * @return  the {@link com.tangosol.net.NamedCache} for {@link com.tangosol.coherence.jcache.CoherenceBasedConfiguration}s
     */
    static NamedCache getConfigurationCache(BinaryEntry binEntry)
        {
        // we don't specify a serializer to allow Coherence to use the service-level classloader
        ClassLoader loader = null;

        // determine the CCF
        ConfigurableCacheFactory ccf = binEntry.getContext().getManager().getCacheFactory();

        // acquire the NamedCache that holds the JCache Configurations (this will be on a separate service)
        return ccf.ensureCache(CoherenceBasedCache.JCACHE_CONFIG_CACHE_NAME, loader);
        }

    /**
     * Provide access to meta cache that maps JCache cache names to JCache configuration.
     * The mapping is a string to a JCache Configuration object.
     *
     * @param binEntry  the {@link BinaryEntry} to use to locate the appropriate Configuration cache
     * @param cacheId   JCache unique identifier
     *
     * @return a named cache that maps JCache cache names to JCache configurations.
     */
    static CompleteConfiguration getCacheToConfigurationMapping(BinaryEntry binEntry, JCacheIdentifier cacheId)
        {
        return (CompleteConfiguration) getConfigurationCache(binEntry).get(cacheId.getCanonicalCacheName());
        }

    /**
     * Mark binValue with JCache synthetic decoration that the entry has only been updated with JCache metaInfo
     *
     * @param binValue  binary value to be decorated
     * @param ctx       ctx to use when decorating binary value
     *
     * @return binary value decorated with JCache synthetic update
     */
    static Binary jcacheSyntheticUpdateEntry(Binary binValue, BackingMapManagerContext ctx)
        {
        return BinaryEntryHelper.decorateUpdateJCacheSynthetic(binValue, ctx,
            BinaryEntryHelper.JCACHE_SYNTHETIC_UPDATE);
        }

    // ----- constants ------------------------------------------------------

    private static int EXPIRY_POLICY_EXCEPTION_LOG_LEVEL = CacheFactory.LOG_WARN;

    /**
     * JCache should generate a Expiry MapEvent for this sythetic update kind.
     */
    public static final Byte JCACHE_SYNTHETIC_EXPIRY = 1;

    /**
     * JCache should treat Cache.clear() as a synthetic update that generates no JCache map event.
     */
    public static final Byte JCACHE_SYNTHETIC_CLEAR = 2;

    /**
     * Filter only JCache SYNTHETIC marker.
     * Entry was expired BUT entry was reused during processing. So the original values of this entry are what expired.
     * Since Entry was reused, this entry MUST not be considered expired as far as future use of jcache entry.
     * This type allows JCache to generate an EXPIRY event with the original values of this entry.
     */
    public static final Byte JCACHE_SYNTHETIC_EXPIRY_EVENT_FOR_ORIGINAL_VALUES = 3;

    /**
     *  A flag indicating that an update to an entry must be considered a synthetic.
     */
    public static final Byte JCACHE_SYNTHETIC_UPDATE = 4;

    /**
     * This entry was cache loaded from an external source so mark it so it will not be subject to
     * a non-optimal writeThrough back to the external source.  Updates to the cache load with
     * JCache expiry to JCache MetaInf makes it so native Coherence support for not writeThrough something
     * that was just loaded does not work.
     *
     * Create/Update events should still be generated for a readThrough entry.
     */
    public static final Byte JCACHE_SYNTHETIC_LOADED = 5;
    }
