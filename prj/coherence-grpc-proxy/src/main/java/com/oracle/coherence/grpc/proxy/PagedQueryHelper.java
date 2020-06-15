/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy;


import com.google.protobuf.ByteString;
import com.google.protobuf.BytesValue;
import com.google.protobuf.UnsafeByteOperations;

import com.oracle.coherence.grpc.BinaryHelper;
import com.oracle.coherence.grpc.EntryResult;
import com.oracle.coherence.grpc.PageRequest;

import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

import com.tangosol.net.CacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.ImmutableArrayList;

import com.tangosol.util.filter.PartitionedFilter;

import io.grpc.Status;

import java.io.IOException;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import java.util.stream.Stream;

/**
 * A helper class for cache key set and entry set paged queries.
 *
 * @author Jonathan Knight  2019.11.28
 * @since 20.06
 */
final class PagedQueryHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor for helper class.
     */
    private PagedQueryHelper()
        {
        }

    /**
     * Perform a key set paged query.
     *
     * @param holder              the {@link CacheRequestHolder} containing the {@link PageRequest}
     * @param cTransferThreshold  the transfer threshold
     *
     * @return a {@link java.util.stream.Stream} of serialized cache keys
     */
    static Stream<BytesValue> keysPagedQuery(CacheRequestHolder<PageRequest, ?> holder, long cTransferThreshold)
        {
        try
            {
            return pagedQuery(holder, true, cTransferThreshold);
            }
        catch (Throwable t)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(t);
            }
        }

    /**
     * Perform a entry set paged query.
     *
     * @param holder              the {@link CacheRequestHolder} containing the {@link PageRequest}
     * @param cTransferThreshold  the transfer threshold
     *
     * @return a {@link Stream} of {@link EntryResult} instances containing serialized cache entries
     */
    static Stream<EntryResult> entryPagedQuery(CacheRequestHolder<PageRequest, ?> holder, long cTransferThreshold)
        {
        try
            {
            return pagedQuery(holder, false, cTransferThreshold);
            }
        catch (Throwable t)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(t);
            }
        }

    /**
     * Perform a paged query for either cache keys or entries.
     *
     * @param holder              the {@link CacheRequestHolder} containing the {@link PageRequest}
     * @param fKeysOnly           flag indicating only keys should be queried
     * @param cTransferThreshold  the transfer threshold
     * @param <T>                 the type of the streamed keys or entries
     *
     * @return a {@link Stream} of either cache keys or entries
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Stream<T> pagedQuery(CacheRequestHolder<PageRequest, ?> holder,
                                            boolean fKeysOnly, long cTransferThreshold)
        {
        NamedCache<Binary, Binary> cache = holder.getCache();

        ByteString   cookie       = holder.getRequest().getCookie();
        Service      service      = cache.getCacheService();
        Object[]     oaCookieData = decodeCookie(service, cookie);
        PartitionSet parts        = (PartitionSet) oaCookieData[0];
        int          cBatch       = (int) oaCookieData[1];
        int          cPart        = parts.getPartitionCount();
        Filter       filter       = Filters.always();

        Set set;
        if (cBatch == 0)
            {
            // query an initial partition
            set = query(cache, filter, fKeysOnly, parts, 1, null);

            // calculate the size of the first partition's worth of results
            int cb = calculateBinarySize(set, fKeysOnly);

            // calculate the batch size
            cBatch = calculateBatchSize(cPart, cb, cTransferThreshold);

            // query the remainder of the batch
            if (cBatch > 1 && cPart > 1)
                {
                set = query(cache, filter, fKeysOnly, parts, cBatch - 1, set);
                }
            }
        else
            {
            set = query(cache, filter, fKeysOnly, parts, cBatch, null);
            }

        ByteString newCookie = encodeCookie(parts, cBatch);
        if (fKeysOnly)
            {
            return (Stream<T>) Stream.concat(Stream.of(BytesValue.of(newCookie)),
                                             ((Set<Binary>) set).stream().map(bin -> BinaryHelper
                                                     .toBytesValue(holder.convertUp(bin))));
            }
        else
            {
            EntryResult first = EntryResult.newBuilder().setCookie(newCookie).build();
            return (Stream<T>) Stream.concat(Stream.of(first),
                                             ((Set<Map.Entry<Binary, Binary>>) set)
                                                     .stream()
                                                     .map(holder::toEntryResult));
            }
        }

    /**
     * Calculate the size of the {@link Binary} instances contained within the provided {@link Collection}.
     *
     * @param col        a {@link Collection} of {@link Binary} instances
     * @param fKeysOnly  flag indicating the collection contains keys only
     *
     * @return the binary size of the {@link Collection}
     */
    @SuppressWarnings("rawtypes")
    private static int calculateBinarySize(Collection col, boolean fKeysOnly)
        {
        int size = 0;
        if (col != null)
            {
            for (Iterator iter = col.iterator(); iter.hasNext(); )
                {
                if (fKeysOnly)
                    {
                    size += ((Binary) iter.next()).length();
                    }
                else
                    {
                    Map.Entry entry = (Map.Entry) iter.next();
                    size += ((Binary) entry.getKey()).length();
                    size += ((Binary) entry.getValue()).length();
                    }
                }
            }

        return size;
        }

    /**
     * Calculate the batch size based on in the partition count, a binary size based on the entries, and the
     * transfer threshold.
     *
     * @param cPart              partition count
     * @param cb                 size of keys/values in the partition
     * @param transferThreshold  transfer threshold before re-balancing occurs
     *
     * @return the batch size that may be used
     */
    private static int calculateBatchSize(int cPart, int cb, long transferThreshold)
        {
        // COH-2139: It is assumed that the size of each partition will be roughly equal.
        // Thus if the sampled partition yielded no results, then it is likely that the
        // overall result set will be small, and thus all partitions should be queried
        // in a single batch.
        int cBatch = cb == 0
                     ? cPart
                     : (int) (transferThreshold / cb);

        cBatch = Math.max(cBatch, 1);
        cBatch = Math.min(cBatch, cPart);

        return cBatch;
        }

    /**
     * Query a partition set for data.
     *
     * @param cache        the {@link NamedCache}
     * @param filter       the {@link Filter}
     * @param keysOnly     if only keys are to be considered
     * @param partsRemain  number of partitions remaining to query
     * @param cBatch       the batch size
     * @param setResult    the Set to store the results of the query
     *
     * @return the results of the query
     */
    @SuppressWarnings("rawtypes")
    private static Set query(NamedCache<Binary, Binary> cache, Filter<Binary> filter, boolean keysOnly,
                             PartitionSet partsRemain, int cBatch, Set setResult)
        {

        // calculate the next batch of partitions
        PartitionSet partsBatch = removePartitionBatch(cache.getCacheService(), partsRemain, cBatch);

        // limit the query to the next batch of partitions
        filter = new PartitionedFilter<>(filter, partsBatch);

        // perform the query
        Set set = keysOnly ? cache.keySet(filter) : cache.entrySet(filter);
        if (setResult == null || setResult.isEmpty())
            {
            setResult = set;
            }
        else if (!set.isEmpty())
            {
            Object[] aoOld = setResult.toArray();
            Object[] aoNew = set.toArray();
            int      cOld  = aoOld.length;
            int      cNew  = aoNew.length;
            int      cAll  = cOld + cNew;
            Object[] aoAll = new Object[cAll];

            System.arraycopy(aoOld, 0, aoAll, 0, cOld);
            System.arraycopy(aoNew, 0, aoAll, cOld, cNew);

            setResult = new ImmutableArrayList(aoAll);
            }

        return setResult;
        }

    /**
     * Removes partitions in a batch.
     *
     * @param service      the target {@link CacheService}
     * @param partsRemain  the remaining partitions
     * @param cBatch       the batch size
     *
     * @return the removed partitions
     */
    private static PartitionSet removePartitionBatch(CacheService service, PartitionSet partsRemain, int cBatch)
        {
        PartitionSet partsBatch;

        int cPartsAll    = partsRemain.getPartitionCount();
        int cPartsRemain = partsRemain.cardinality();

        if (cPartsRemain <= cBatch)
            {
            partsBatch = new PartitionSet(partsRemain); // copy
            partsRemain.clear();
            }
        else
            {
            partsBatch = new PartitionSet(cPartsAll);

            if (service instanceof PartitionedService)
                {
                PartitionedService svcPartitioned = (PartitionedService) service;
                int                cBatchLeft     = cBatch;
                while (!partsRemain.isEmpty() && cBatchLeft > 0)
                    {
                    // choose the first partition randomly
                    int nPart = partsRemain.rnd();

                    // the loop below should normally execute just once;
                    // during distribution we limit it to cPartsRemain attempts
                    Member member = null;
                    for (int i = 0; i < cPartsRemain; i++)
                        {
                        member = svcPartitioned.getPartitionOwner(nPart);
                        if (member != null)
                            {
                            break;
                            }
                        nPart = partsRemain.next(nPart);
                        }
                    if (member == null)
                        {
                        // every partition is in re-distribution; fall back on the default algorithm
                        break;
                        }

                    // add more partitions for the same member
                    PartitionSet parts = svcPartitioned.getOwnedPartitions(member);
                    parts.retain(partsRemain);

                    int c = parts.cardinality();
                    while (c > cBatchLeft)
                        {
                        parts.removeNext(0);
                        c--;
                        }
                    partsBatch.add(parts);
                    partsRemain.remove(parts);
                    cBatchLeft -= c;
                    }
                }

            if (partsBatch.isEmpty())
                {
                // service is not partitioned or the optimized algorithm failed
                // calculate the next batch of partitions randomly
                for (int nPart = partsRemain.rnd(); --cBatch >= 0; )
                    {
                    nPart = partsRemain.removeNext(nPart);
                    if (nPart < 0)
                        {
                        break;
                        }
                    partsBatch.add(nPart);
                    }
                }
            }
        return partsBatch;
        }

    /**
     * Decode the binary cookie used in a paged request.
     *
     * @param service  the cache {@link Service} to use to deserialize the cookie
     * @param cookie   the cookie
     *
     * @return the decoded cookie
     */
    static Object[] decodeCookie(Service service, ByteString cookie)
        {
        PartitionSet parts;
        int nPage;

        if (cookie == null || cookie.isEmpty())
            {
            int cParts;

            if (service instanceof PartitionedService)
                {
                cParts = ((PartitionedService) service).getPartitionCount();
                }
            else
                {
                throw Status.FAILED_PRECONDITION.withDescription("Service is not a PartitionedService")
                        .asRuntimeException();
                }

            parts = new PartitionSet(cParts);

            parts.fill();

            return new Object[] {parts, 0};
            }
        else
            {
            try
                {
                ReadBuffer.BufferInput in = BinaryHelper.toReadBuffer(cookie).getBufferInput();
                parts = new PartitionSet();
                parts.readExternal(in);
                nPage = in.readPackedInt();
                }
            catch (IOException e)
                {
                throw ErrorsHelper.ensureStatusRuntimeException(e, "error decoding cookie");
                }
            }

        return new Object[] {parts, nPage};
        }

    /**
     * Encode the opaque cookie used in a paged request.
     *
     * @param parts  the partition set to encode
     * @param page   the current page
     *
     * @return an encoded page request cookie
     */
    static ByteString encodeCookie(PartitionSet parts, int page)
        {
        if (parts.isEmpty())
            {
            return BinaryHelper.EMPTY_BYTE_STRING;
            }

        WriteBuffer.BufferOutput out = new BinaryWriteBuffer(64).getBufferOutput();

        try
            {
            parts.writeExternal(out);
            out.writePackedInt(page);
            }
        catch (IOException e)
            {
            throw ErrorsHelper.ensureStatusRuntimeException(e, "error encoding cookie");
            }

        return UnsafeByteOperations.unsafeWrap(out.getBuffer().getReadBuffer().toByteBuffer());
        }
    }
