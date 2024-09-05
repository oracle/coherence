/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;
import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor.QueueIndex;

import com.tangosol.internal.net.queue.model.QueueKey;

import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;

import com.tangosol.net.BackingMapContext;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.net.cache.BinaryMemoryCalculator;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.Converter;
import com.tangosol.util.MapIndex;

import com.tangosol.util.processor.AbstractProcessor;

/**
 * A base class for queue entry processors.
 *
 * @param <K>  the type of the cache key
 * @param <V>  the type of the cache value
 * @param <R>  the type of the returned result
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class AbstractQueueProcessor<K, V, R>
        extends AbstractProcessor<K, V, R>
        implements EvolvablePortableObject, ExternalizableLite
    {
    /**
     * Poll the entry from the head of the queue.
     *
     * @param entry  the entry the processor is being invoked against
     *
     * @return the {@link QueuePollResult result} of polling the head entry
     */
    protected QueuePollResult pollFromHead(BinaryEntry<QueueKey, V> entry)
        {
        BinaryEntry<QueueKey, V> entryHead = enlistHeadEntry(entry, false);
        if (entryHead != null)
            {
            Binary  binResult = entryHead.getBinaryValue();
            entryHead.remove(false);
            return new QueuePollResult(entryHead.getKey().getId(), binResult);
            }
        return QueuePollResult.empty();
        }

    /**
     * Poll the entry from the tail of the queue.
     *
     * @param entry  the entry the processor is being invoked against
     *
     * @return the {@link QueuePollResult result} of polling the tail entry
     */
    protected QueuePollResult pollFromTail(BinaryEntry<QueueKey, V> entry)
        {
        BinaryEntry<QueueKey, V> entryTail = enlistTailEntry(entry, false);
        if (entryTail != null)
            {
            Binary  binResult = entryTail.getBinaryValue();
            entryTail.remove(false);
            return new QueuePollResult(entryTail.getKey().getId(), binResult);
            }
        return QueuePollResult.empty();
        }

    /**
     * Peek at the entry from the head of the queue.
     *
     * @param entry  the entry the processor is being invoked against
     *
     * @return the {@link QueuePollResult result} of peeking at the head entry
     */
    protected QueuePollResult peekAtHead(BinaryEntry<QueueKey, V> entry)
        {
        // ToDo How safe would it be to peek directly into the backing map for the head entry?
        BinaryEntry<QueueKey, V> entryHead = enlistHeadEntry(entry, true);
        if (entryHead != null && entryHead.isPresent())
            {
            Binary binResult = entryHead.getBinaryValue();
            return new QueuePollResult(entryHead.getKey().getId(), binResult);
            }
        return QueuePollResult.empty();
        }

    /**
     * Peek at the entry from the tail of the queue.
     *
     * @param entry  the entry the processor is being invoked against
     *
     * @return the {@link QueuePollResult result} of peeking at the tail entry
     */
    protected QueuePollResult peekAtTail(BinaryEntry<QueueKey, V> entry)
        {
        // ToDo How safe would it be to peek directly into the backing map for the tail entry?
        BinaryEntry<QueueKey, V> entryTail = enlistTailEntry(entry, true);
        if (entryTail != null && entryTail.isPresent())
            {
            Binary binResult = entryTail.getBinaryValue();
            return new QueuePollResult(entryTail.getKey().getId(), binResult);
            }
        return QueuePollResult.empty();
        }

    /**
     * Offer a value to the tail of the queue.
     *
     * @param entry   the entry the processor is being invoked against
     * @param binary  the {@link Binary} value to offer
     * @param oValue  the Object value to offer
     *
     * @return the {@link QueueOfferResult result} of the operation
     */
    public QueueOfferResult offerToTail(BinaryEntry<QueueKey, ?> entry, Binary binary, Object oValue)
        {
        BackingMapContext context     = entry.getBackingMapContext();
        Binary            binaryValue = ensureBinaryValue(context, binary, oValue);

        if (exceedsSizeLimit(entry, entry.getBinaryKey(), binaryValue))
            {
            return new QueueOfferResult(entry.getKey().getId(), QueueOfferResult.RESULT_FAILED_CAPACITY);
            }

        QueueIndex                index      = assertQueueIndex(entry);
        long                      nTail      = index.nextTailOffer();
        QueueKey                  keyNext    = new QueueKey(entry.getKey().getHash(), nTail);
        BackingMapManagerContext  mgrContext = context.getManagerContext();
        Converter                 converter  = mgrContext.getKeyToInternalConverter();
        Binary                    binKeyNext = (Binary) converter.convert(keyNext);
        BinaryEntry<Long, Object> entryTail  = (BinaryEntry<Long, Object>) context.getBackingMapEntry(binKeyNext);

        while (entryTail.isPresent())
            {
            // we should not get here, the index may be out of date!!!
            // we need to now walk up entries until we find one that is not there.
            keyNext    = keyNext.next();
            binKeyNext = (Binary) converter.convert(keyNext);
            entryTail  = (BinaryEntry<Long, Object>) context.getBackingMapEntry(binKeyNext);
            }

        entryTail.updateBinaryValue(binaryValue);
        return new QueueOfferResult(keyNext.getId(), QueueOfferResult.RESULT_SUCCESS);
        }

    /**
     * Offer a value to the tail of the queue.
     *
     * @param entry   the entry the processor is being invoked against
     * @param binary  the {@link Binary} value to offer
     * @param oValue  the Object value to offer
     *
     * @return the {@link QueueOfferResult result} of the operation
     */
    public QueueOfferResult offerToHead(BinaryEntry<QueueKey, ?> entry, Binary binary, Object oValue)
        {
        BackingMapContext context     = entry.getBackingMapContext();
        Binary            binaryValue = ensureBinaryValue(context, binary, oValue);

        if (exceedsSizeLimit(entry, entry.getBinaryKey(), binaryValue))
            {
            return new QueueOfferResult(entry.getKey().getId(), QueueOfferResult.RESULT_FAILED_CAPACITY);
            }

        QueueIndex                index      = assertQueueIndex(entry);
        long                      nHead      = index.nextHeadOffer();
        QueueKey                  keyPrev    = new QueueKey(entry.getKey().getHash(), nHead);
        BackingMapManagerContext  mgrContext = context.getManagerContext();
        Converter                 converter  = mgrContext.getKeyToInternalConverter();
        Binary                    binKeyPrev = (Binary) converter.convert(keyPrev);
        BinaryEntry<Long, Object> entryHead  = (BinaryEntry<Long, Object>) context.getBackingMapEntry(binKeyPrev);

        while (entryHead.isPresent())
            {
            // we should not get here, the index may be out of date!!!
            // we need to now walk down entries until we find one that is not there.
            keyPrev    = keyPrev.prev();
            binKeyPrev = (Binary) converter.convert(keyPrev);
            entryHead  = (BinaryEntry<Long, Object>) context.getBackingMapEntry(binKeyPrev);
            }

        entryHead.updateBinaryValue(binaryValue);
        return new QueueOfferResult(keyPrev.getId(), QueueOfferResult.RESULT_SUCCESS);
        }

    public Binary ensureBinaryValue(BackingMapContext context, Binary binary, Object oValue)
        {
        if (binary == null)
            {
            return (Binary) context.getManagerContext().getValueToInternalConverter().convert(oValue);
            }
        return binary;
        }

    public boolean exceedsSizeLimit(BinaryEntry<QueueKey, ?> entry, Binary binKey, Binary binValue)
        {
        QueueIndex index         = assertQueueIndex(entry);
        long       cMaxBytes     = index.getMaxQueueSize();
        long       cCurrentBytes = index.getQueueSize();
        return exceedsSizeLimit(binKey, binValue, cMaxBytes, cCurrentBytes);
        }

    public boolean exceedsSizeLimit(BinaryEntry<? extends QueueKey, ?> entry, Binary binKey, Binary binValue, long cMaxBytes)
        {
        QueueIndex index         = assertQueueIndex(entry);
        long       cCurrentBytes = index.getQueueSize();
        return exceedsSizeLimit(binKey, binValue, cMaxBytes, cCurrentBytes);
        }

    protected long entrySize(Binary binKey, Binary binValue)
        {
        return (binKey.length() + binValue.length()
                + ((long) BinaryMemoryCalculator.SIZE_BINARY * 2)
                + (long) BinaryMemoryCalculator.SIZE_ENTRY);
        }

    protected boolean exceedsSizeLimit(Binary binKey, Binary binValue, long cMaxBytes, long cCurrentBytes)
        {
        long cBytes = cCurrentBytes + entrySize(binKey, binValue);
        return cBytes > cMaxBytes;
        }


    /**
     * Enlist the head entry of the queue.
     *
     * @param entry      the entry the processor is being invoked against
     * @param fReadOnly  {@code true} to enlist the head as read-only
     * @param <E>        the type of elements in the queue
     *
     * @return the enlisted head element
     */
    protected static <E> BinaryEntry<QueueKey, E> enlistHeadEntry(BinaryEntry<QueueKey, E> entry, boolean fReadOnly)
        {
        BackingMapContext context  = entry.getBackingMapContext();
        QueueIndex        index    = assertQueueIndex(entry);
        Object            oHeadKey = index.getHeadBinaryKey(QueueKey.EMPTY_ID);

        if (oHeadKey != null)
            {
            try
                {
                return fReadOnly ? (BinaryEntry<QueueKey, E>) context.getReadOnlyEntry(oHeadKey)
                        : (BinaryEntry<QueueKey, E>) context.getBackingMapEntry(oHeadKey);
                }
            catch (Exception e)
                {
                Object o = oHeadKey;
                if (oHeadKey instanceof Binary)
                    {
                    o = context.getManagerContext().getValueFromInternalConverter().convert(o);
                    }
                Logger.err("Error enlisting head entry entryKey=" + entry.getKey()
                        + " key=" + o);
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        return null;
        }

    /**
     * Enlist the tail entry of the queue.
     *
     * @param entry      the entry the processor is being invoked against
     * @param fReadOnly  {@code true} to enlist the head as read-only
     * @param <E>        the type of elements in the queue
     *
     * @return the enlisted head element
     */
    protected static <E> BinaryEntry<QueueKey, E> enlistTailEntry(BinaryEntry<QueueKey, E> entry, boolean fReadOnly)
        {
        BackingMapContext context  = entry.getBackingMapContext();
        QueueIndex        index    = assertQueueIndex(entry);
        Object            oTailKey = index.getTailBinaryKey(QueueKey.EMPTY_ID);

        if (oTailKey != null)
            {
            try
                {
                return fReadOnly ? (BinaryEntry<QueueKey, E>) context.getReadOnlyEntry(oTailKey)
                        : (BinaryEntry<QueueKey, E>) context.getBackingMapEntry(oTailKey);
                }
            catch (Exception e)
                {
                Object o = oTailKey;
                if (o instanceof Binary)
                    {
                    o = context.getManagerContext().getValueFromInternalConverter().convert(o);
                    }
                Logger.err("Error enlisting tail entry entryKey=" + entry.getKey()
                        + " key=" + o);
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        return null;
        }

    /**
     * Obtains the index from the backing map for the {@link QueueKeyExtractor} and
     * asserts that it is an instance of a {@link QueueIndex}.
     *
     * @param entry  the {@link BinaryEntry} to use to obtain the index
     *
     * @return the {@link QueueIndex} for the entry
     */
    protected static QueueIndex assertQueueIndex(BinaryEntry<?, ?> entry)
        {
        MapIndex index = entry.getIndexMap().get(QueueKeyExtractor.INSTANCE);
        if (index instanceof QueueIndex)
            {
            return (QueueIndex) index;
            }
        String sCache = entry.getBackingMapContext().getCacheName();
        throw new IllegalStateException("The index on cache " + sCache + " foe extractor "
                + QueueKeyExtractor.INSTANCE + " must be an instance of " + QueueIndex.class
                + " but is a " + index.getClass());
        }

    // ----- EvolvablePortableObject methods --------------------------------

    @Override
    public int getDataVersion()
        {
        return m_nDataVersion;
        }

    @Override
    public void setDataVersion(int nVersion)
        {
        m_nDataVersion = nVersion;
        }

    @Override
    public Binary getFutureData()
        {
        return m_binFuture;
        }

    @Override
    public void setFutureData(Binary binFuture)
        {
        m_binFuture = binFuture;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link EvolvablePortableObject} data version.
     */
    protected transient int m_nDataVersion;

    /**
     * The {@link EvolvablePortableObject} remainder.
     */
    protected transient Binary m_binFuture;
    }
