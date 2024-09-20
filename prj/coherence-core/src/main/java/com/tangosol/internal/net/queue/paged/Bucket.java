/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.paged;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.BackingMapManagerContext;

import com.tangosol.util.Binary;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.MapIndex;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class represents information about a bucket in a queue.
 * <p/>
 * In the implementation of a distributed queue elements in the queue are
 * stored in buckets to store the queue elements in groups for more
 * efficient access but still spread them around the cluster in a reasonably
 * even manner.
 * <p/>
 * Each bucket has a capacity that is used to determine when a bucket is full.
 * New queue elements are only added to the last bucket (as denoted by the last
 * bucket flag). Once the last bucket is filled its last bucket flag is set to
 * false and a new last bucket is created. Once a bucket becomes full then no
 * other elements will be added to that bucket, even if the number of elements
 * reduces due to a poll.
 * <p/>
 * This combined with the bucket capacity gives the total number of elements that
 * could possibly be added to the queue. This is not the total size, but the total
 * number of elements that may be added - these numbers are different. The size of
 * a queue may be very low due to elements being removed from the queue almost as
 * fast as they are added but the bucket count will still increase as buckets are
 * filled.
 */
@SuppressWarnings({"PatternVariableCanBeUsed", "rawtypes", "unchecked"})
public class Bucket
        extends AbstractEvolvable
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor used for POF.
     */
    public Bucket()
        {
        }

    /**
     * Create a {@link Bucket} with the given capacity.
     *
     * @param bucketId the id of this bucket
     * @param capacity the maximum number of elements that this
     *                 bucket can contain.
     */
    public Bucket(int bucketId, int capacity)
        {
        this(bucketId, new QueueVersionInfo(), capacity);
        }

    /**
     * Create a {@link Bucket} with the given version number and capacity.
     *
     * @param bucketId  the id of this
     * @param version   the version information for this bucket
     * @param capacity  the maximum number of elements that this
     *                  bucket can contain.
     */
    public Bucket(int bucketId, QueueVersionInfo version, long capacity)
        {
        m_id              = bucketId;
        m_version         = version;
        m_capacity        = capacity;
        m_acceptingOffers = true;
        }

    // ----- accessor methods -----------------------------------------------

    /**
     * Returns the id of this {@link Bucket}.
     *
     * @return the id of this {@link Bucket}.
     */
    public int getId()
        {
        return m_id;
        }

    /**
     * Obtain the {@link QueueVersionInfo} for this Bucket.
     *
     * @return the {@link QueueVersionInfo} for this Bucket
     */
    public QueueVersionInfo getVersion()
        {
        return m_version;
        }

    /**
     * Returns the maximum number of elements that this
     * {@link Bucket} should contain.
     *
     * @return the maximum number of elements that this
     *         {@link Bucket} should contain.
     */
    public long getMaxCapacity()
        {
        return m_capacity;
        }

    /**
     * @return the id of the current head element of this {@link Bucket}
     */
    public int getHead()
        {
        return m_head;
        }

    /**
     * Set the id of the current head element of this {@link Bucket}
     */
    public void setHead(int head)
        {
        m_head = head;

        if (m_tail == EMPTY)
            {
            m_tail = m_head;
            }
        }

    /**
     * @return the id of the current tail element of this {@link Bucket}
     */
    public int getTail()
        {
        return m_tail;
        }

    /**
     * Set the id of the current tail element of this {@link Bucket}
     */
    public void setTail(int tail)
        {
        m_tail = tail;

        if (m_head == EMPTY)
            {
            m_head = tail;
            }
        }

    /**
     * Marks this {@link Bucket} as being empty
     */
    public void markEmpty()
        {
        m_head      = EMPTY;
        m_tail      = EMPTY;
        m_bytesUsed = 0;
        }

    /**
     * Returns true if this bucket is empty, otherwise returns false.
     *
     * @return true if this bucket is empty, otherwise returns false
     */
    public boolean isEmpty()
        {
        return m_head == EMPTY && m_tail == EMPTY;
        }

    /**
     * Returns true if this bucket can accept offers otherwise returns false.
     *
     * @return true if this bucket can accept offers otherwise returns false
     */
    public boolean isAcceptingOffers()
        {
        return m_acceptingOffers;
        }

    /**
     * Set the flag indicating whether this bucket can accept offers.
     *
     * @param acceptingOffers  true if this bucket can accept offers, otherwise false
     */
    public void setAcceptingOffers(boolean acceptingOffers)
        {
        m_acceptingOffers = acceptingOffers;
        }

    /**
     * Return the number of bytes consumed by this bucket.
     *
     * @return the number of bytes consumed by this bucket
     */
    public long getBytesUsed()
        {
        return m_bytesUsed;
        }

    /**
     * Increase the number of bytes consumed by this bucket only if the
     * bucket would not go above its maximum capacity.
     *
     * @param n the number of bytes to add
     *
     * @return {@code true} if the size was incremented, or {@code false}
     *         if incrementing the size would have taken the bucket above
     *         its maximum capacity
     */
    public boolean increaseBytesUsed(long n)
        {
        if (m_bytesUsed >= m_capacity)
            {
            return false;
            }
        m_bytesUsed = m_bytesUsed + n;
        return true;
        }

    /**
     * Decrease the number of bytes consumed by this bucket.
     *
     * @param n the number of bytes to remove
     */
    public void decreaseBytesUsed(long n)
        {
        long nSize = m_bytesUsed - n;
        m_bytesUsed = Math.max(0L, nSize);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Provide a human-readable representation of this object.
     *
     * @return a String whose contents represent the value of this object
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + "id=" + m_id + ", capacity=" + m_capacity
                + ", bytesUsed=" + m_bytesUsed + ", head=" + m_head + ", tail=" + m_tail
                + ", acceptingOffers=" + m_acceptingOffers + ", version=" + m_version + ')';
        }

    /**
     * Returns a hash code value for this object.
     *
     * @return  a hash code value for this object
     */
    public int hashCode()
        {
        return m_id;
        }

    /**
     * Compares this object with another object for equality.
     *
     * @param  o  an object reference or null
     *
     * @return  true if the passed object reference is of the same class and
     *          has the same state as this object
     */
    public boolean equals(Object o)
        {
        if (o instanceof Bucket)
            {
            Bucket that = (Bucket) o;

            return this == that || this.getClass() == that.getClass() && this.m_id == that.m_id;
            }

        return false;
        }


    // ----- static helper methods ------------------------------------------

    /**
     * Returns the {@link SortedSet} of {@link PagedQueueKey} keys that have the
     * specified bucket in the given backing map.
     *
     * If the index map contains an index for bucket id then the index will be used
     * for the search.
     *
     * @param bucketId   the id of the bucket to find {@link PagedQueueKey} instances for.
     * @param backingMap the {@link Map} containing the queue elements.
     * @param indexes    a {@link Map} of indexes on the element {@link Map}.
     * @param context    The {@link BackingMapManagerContext} for the backing map.
     *
     * @return the {@link SortedSet} of {@link PagedQueueKey} instances for the given
     *         bucket id contained in the backing map.
     */
    @SuppressWarnings("unchecked")
    public static SortedSet<PagedQueueKey> findKeysForBucketId(int bucketId, Map backingMap, Map indexes,
                                                               BackingMapManagerContext context)
        {
        if (backingMap.isEmpty())
            {
            return new TreeSet<>();
            }

        Set<Binary>   results = findBinaryKeysForBucket(bucketId, backingMap, indexes, context);

        Set<PagedQueueKey> keySet  = new ConverterCollections.ConverterSet(results, context.getKeyFromInternalConverter(),
                                    context.getKeyToInternalConverter());

        return new TreeSet(keySet);
        }

    /**
     * Returns the {@link List} of elements from the given backing map that are contained
     * in the specified bucket
     * <p/>
     * If the index map contains an index for bucket id then the index will be used
     * for the search.
     *
     * @param bucketId        the id of the bucket to find {@link PagedQueueKey} instances for.
     * @param backingMap      the {@link java.util.Map} containing the queue elements.
     * @param indexes         a {@link java.util.Map} of indexes on the element {@link java.util.Map}.
     * @param context         the {@link com.tangosol.net.BackingMapManagerContext} for the backing map.
     * @param fHeadFirstOrder true if the resulting list should order the elements head first or false if the
     *                        resulting list should order the elements tail first.
     *
     * @return the {@link SortedSet} of {@link PagedQueueKey} instances for the given
     *         bucket id contained in the backing map.
     */
    public static List<Binary> findElementsForBucketId(int bucketId, Map<Binary, Binary> backingMap, Map indexes,
            BackingMapManagerContext context, boolean fHeadFirstOrder)
        {
        if (backingMap.isEmpty())
            {
            return new ArrayList<>();
            }

        Set<Binary>                 results      = findBinaryKeysForBucket(bucketId, backingMap, indexes, context);
        Converter                   keyConverter = context.getKeyFromInternalConverter();
        Comparator<PagedQueueKey>        comparator   = fHeadFirstOrder
                ? null
                : Collections.<PagedQueueKey>reverseOrder();
        SortedMap<PagedQueueKey, Binary> binaryKeyMap = new TreeMap<>(comparator);

        for (Binary binary : results)
            {
            PagedQueueKey queueKey = (PagedQueueKey) keyConverter.convert(binary);

            binaryKeyMap.put(queueKey, binary);
            }

        List<Binary> elementList = new ArrayList<>();

        for (PagedQueueKey key : binaryKeyMap.keySet())
            {
            Binary element = backingMap.get(binaryKeyMap.get(key));

            if (element != null)
                {
                elementList.add(element);
                }
            }

        return elementList;
        }

    /**
     * A helper method that will return the {@link Set} of {@link Binary} keys
     * for the elements that are contained in the specified bucket from the
     * given backing.
     *
     * @param bucketId    the id of the bucket to find element {@link Binary} keys for
     * @param backingMap  the backing map to search
     * @param indexes     a map of indexes that may be used in the search
     * @param context     the {@link BackingMapManagerContext} for the backing map
     *
     * @return return the {@link Set} of {@link Binary} keys
     *         for the elements that are contained in the specified bucket from the
     *         given backing.
     */
    @SuppressWarnings("unchecked")
    private static Set<Binary> findBinaryKeysForBucket(int bucketId, Map backingMap, Map indexes,
            BackingMapManagerContext context)
        {
        HashSet<Binary> results = new HashSet<>();
        MapIndex        index   = (indexes != null)
                                  ? (MapIndex) indexes.get(PagedQueueKey.BUCKET_ID_EXTRACTOR)
                                  : null;

        if (index != null)
            {
            Collection<Binary> keys = (Set<Binary>) index.getIndexContents().get(bucketId);

            if (keys != null)
                {
                results.addAll(keys);
                }
            }
        else
            {
            Converter  converter = context.getValueFromInternalConverter();

            for (Binary binaryKey : (Set<Binary>) backingMap.keySet())
                {
                PagedQueueKey queueKey    = (PagedQueueKey) converter.convert(binaryKey);
                int           keyBucketId = queueKey.getBucketId();

                if (keyBucketId == bucketId)
                    {
                    results.add(binaryKey);
                    }
                }
            }

        return results;
        }

    /**
     * A helper method that will return the {@link Set} of {@link Binary} keys
     * for the elements that are contained in the specified bucket from the
     * given backing.
     *
     * @param bucketId    the id of the bucket to find element {@link Binary} keys for
     * @param fromId      the first elementId in the range of keys to get
     * @param toId        the last elementId in the range of get keys to get
     * @param backingMap  the backing map to search
     * @param indexes     a map of indexes that may be used in the search
     * @param context     the {@link BackingMapManagerContext} for the backing map
     *
     * @return return the {@link Set} of {@link Binary} keys
     *         for the elements that are contained in the specified bucket from the
     *         given backing.
     */
    @SuppressWarnings("unchecked")
    private static Set<Binary> findBinaryKeysForBucket(int bucketId, int fromId, int toId, Map backingMap, Map indexes,
            BackingMapManagerContext context)
        {
        HashSet<Binary>    results = new HashSet<>();
        MapIndex           index   = (indexes != null)
                                     ? (MapIndex) indexes.get(PagedQueueKey.BUCKET_ID_EXTRACTOR)
                                     : null;

        Collection<Binary> keys;

        if (index != null)
            {
            keys = (Set<Binary>) index.getIndexContents().get(bucketId);
            }
        else
            {
            keys = (Collection<Binary>) backingMap.keySet();
            }

        if (keys == null)
            {
            return results;
            }

        Serializer serializer = context.getCacheService().getSerializer();

        for (Binary binaryKey : keys)
            {
            PagedQueueKey queueKey    = (PagedQueueKey) ExternalizableHelper.fromBinary(binaryKey, serializer);
            int           keyBucketId = queueKey.getBucketId();
            int      keyElementId = queueKey.getElementId();

            if (keyBucketId == bucketId && keyElementId >= fromId && keyElementId <= toId)
                {
                results.add(binaryKey);
                }
            }

        return results;
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_id              = in.readInt(0);
        m_version         = in.readObject(1);
        m_capacity        = in.readLong(2);
        m_head            = in.readInt(3);
        m_tail            = in.readInt(4);
        m_acceptingOffers = in.readObject(5);
        m_bytesUsed       = in.readLong(6);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_id);
        out.writeObject(1, m_version);
        out.writeLong(2, m_capacity);
        out.writeInt(3, m_head);
        out.writeInt(4, m_tail);
        out.writeObject(5, m_acceptingOffers);
        out.writeLong(6, m_bytesUsed);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_id              = ExternalizableHelper.readInt(in);
        m_version         = ExternalizableHelper.readObject(in);
        m_capacity        = ExternalizableHelper.readLong(in);
        m_head            = ExternalizableHelper.readInt(in);
        m_tail            = ExternalizableHelper.readInt(in);
        m_acceptingOffers = ExternalizableHelper.readObject(in);
        m_bytesUsed       = ExternalizableHelper.readLong(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_id);
        ExternalizableHelper.writeObject(out, m_version);
        ExternalizableHelper.writeLong(out, m_capacity);
        ExternalizableHelper.writeInt(out, m_head);
        ExternalizableHelper.writeInt(out, m_tail);
        ExternalizableHelper.writeObject(out, m_acceptingOffers);
        ExternalizableHelper.writeLong(out, m_bytesUsed);
        }

    // ----- constants ------------------------------------------------------

    /**
     * The POF evolvable version.
     */
    public static final int POF_IMPL_VERSION = 0;

    /**
     * Value for head and tail id to indicate an empty bucket
     */
    public static final int EMPTY = -1;

    // ----- data members ---------------------------------------------------

    /** The id of this bucket */
    protected int m_id;

    /** The version information for this bucket */
    protected QueueVersionInfo m_version;

    /**
     * The maximum number of bytes that this bucket should contain
     */
    protected long m_capacity = 0;

    /**
     * The id of the current head element of this {@link Bucket}
     */
    protected int m_head = EMPTY;

    /**
     * The id of the current tail element of this {@link Bucket}
     */
    protected int m_tail = EMPTY;

    /**
     * A flag indicating whether this bucket can accept offers
     */
    protected boolean m_acceptingOffers = true;

    /**
     * The current size of the bucket.
     */
    protected long m_bytesUsed;
    }
