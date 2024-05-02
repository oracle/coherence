/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.json.bind.annotation.JsonbProperty;

import static com.oracle.coherence.common.base.Assertions.azzert;
import static com.oracle.coherence.common.base.Randoms.getRandom;

/**
* PartitionSet is a light-weight data structure that represents a set of
* partitions that are used in parallel processing. This set quite often
* accompanies a result of partial parallel execution and is used to determine
* whether or not the entire set of partitions was successfully processed.
* <p>
* Note that all PartitionSet operations that take another set as an argument
* assume that both sets have the same partition count.
* <p>
* This implementation is not thread-safe.
*
* @author gg 2005.12.20
* @since Coherence 3.1
*/
/*
* Internal note: while this functionality is a small part of what the
* java.util.BitSet offers, prior to JDK 1.4.2 there was no way to extract the
* internal array that represents the BitSet state for a purpose of a
* lite serialization (bug_id=5037068). By the time it was fixed, the existing
* implementation proved to be sufficient. We may reevaluate this decision
* again in the future.
*/
public class PartitionSet
        implements ExternalizableLite, PortableObject, Iterable<Integer>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PartitionSet()
        {
        }

    /**
    * Construct an empty partition set with a given count.
    *
    * @param cPartitions  the partition count
    */
    public PartitionSet(int cPartitions)
        {
        azzert(cPartitions > 0);

        m_cPartitions = cPartitions;
        m_alBits      = new long[(cPartitions + 63) >>> 6];
        m_lTailMask   = -1L >>> (64 - (cPartitions & 63));
        m_cMarked     = 0;
        }

    /**
     * Construct a partition set with a given partition count and a single
     * marked partition.
     *
     * @param cPartitions  the partition count
     * @param nPartition   the partition to set
     */
    public PartitionSet(int cPartitions, int nPartition)
        {
        this(cPartitions);
        add(nPartition);
        }

    /**
     * Construct a partition set with a given partition count and the specified
     * partitions set.
     *
     * @param cPartitions    the partition count
     * @param colPartitions  the partitions to set
     */
    public PartitionSet(int cPartitions, Collection<? extends Integer> colPartitions)
        {
        this(cPartitions);
        colPartitions.forEach(this::add);
        }

    /**
     * Construct a partition set with a given partition count and the specified
     * partitions set.
     *
     * @param cPartitions   the partition count
     * @param aiPartitions  the partitions to set
     */
    public PartitionSet(int cPartitions, int... aiPartitions)
        {
        this(cPartitions);

        if (aiPartitions != null)
            {
            for (int nPart : aiPartitions)
                {
                add(nPart);
                }
            }
        }

    /**
    * Copy constructor: construct a new PartitionSet object equivalent to the
    * specified one.
    *
    * @param partitions  the partition set to copy
    */
    public PartitionSet(PartitionSet partitions)
        {
        m_cPartitions = partitions.m_cPartitions;
        m_alBits      = partitions.m_alBits.clone();
        m_lTailMask   = partitions.m_lTailMask;
        m_cMarked     = partitions.m_cMarked;
        }

    // ----- pseudo Set operations ------------------------------------------

    /**
    * Add the specified partition to the set.
    *
    * @param nPartition  the partition to add
    *
    * @return true if the specified partition was actually added as a result
    *         of this call; false otherwise
    */
    public boolean add(int nPartition)
        {
        if (nPartition < 0 || nPartition >= m_cPartitions)
            {
            throw new IndexOutOfBoundsException(
                    nPartition + " not in [0, " + m_cPartitions + ')');
            }

        long[] alBits = m_alBits;
        int    iLong  = nPartition >>> 6;
        long   lBits  = alBits[iLong];
        long   lMask  = 1L << (nPartition & 63);

        if ((lBits & lMask) == 0L)
            {
            alBits[iLong] = lBits | lMask;

            int cMarked = m_cMarked;
            if (cMarked >= 0)
                {
                m_cMarked = cMarked + 1;
                }

            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * Add the specified PartitionSet to this set.
    *
    * @param partitions  the PartitionSet to add
    *
    * @return true if all of the partitions were actually added as a result
    *         of this call; false otherwise
    */
    public boolean add(PartitionSet partitions)
        {
        int cPartitions = m_cPartitions;
        azzert(cPartitions == partitions.m_cPartitions);

        long[]  alBitsThis = this      .m_alBits;
        long[]  alBitsThat = partitions.m_alBits;
        boolean fResult    = true;

        for (int i = 0, c = alBitsThis.length; i < c; ++i)
            {
            long lBitsThis = alBitsThis[i];
            long lBitsThat = alBitsThat[i];

            fResult &= (lBitsThis & lBitsThat) == 0L;
            alBitsThis[i] = lBitsThis | lBitsThat;
            }

        m_cMarked = -1;
        return fResult;
        }

    /**
    * Remove the specified partition from the set.
    *
    * @param nPartition  the partition to remove
    *
    * @return true if the specified partition was actually removed as a
    *         result of this call; false otherwise
    */
    public boolean remove(int nPartition)
        {
        if (nPartition < 0 || nPartition >= m_cPartitions)
            {
            throw new IndexOutOfBoundsException(
                    nPartition + " not in [0, " + m_cPartitions + ')');
            }

        long[] alBits = m_alBits;
        int    iLong  = nPartition >>> 6;
        long   lBits  = alBits[iLong];
        long   lMask  = 1L << (nPartition & 63);

        if ((lBits & lMask) != 0L)
            {
            alBits[iLong] = lBits & ~lMask;

            int cMarked = m_cMarked;
            if (cMarked >= 0)
                {
                m_cMarked = cMarked - 1;
                }

            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * Remove the first marked partition starting at the specified partition.
    * If there are no marked partitions greater or equal to the specified
    * partition, the first marked partition greater or equal to 0 but less
    * than the specified partition is removed. If this PartitionSet is empty,
    * -1 is returned.
    *
    * @param  nPartition  the partition to start checking from (inclusive)
    *
    * @return the marked partition that was removed or -1 if this
    *         PartitionSet is empty
    *
    * @throws IndexOutOfBoundsException if the specified partition is invalid
    */
    public int removeNext(int nPartition)
        {
        int nNext = next(nPartition);
        if (nNext == -1 && nPartition > 0)
            {
            nNext = next(0);
            }

        if (nNext >= 0)
            {
            remove(nNext);
            }
        return nNext;
        }

    /**
    * Remove the specified PartitionSet from this set.
    *
    * @param partitions  the PartitionSet to remove
    *
    * @return true if all of the specified partitions were actually removed;
    *         false otherwise
    */
    public boolean remove(PartitionSet partitions)
        {
        int cPartitions = m_cPartitions;
        azzert(cPartitions == partitions.m_cPartitions);

        long[]  alBitsThis = this      .m_alBits;
        long[]  alBitsThat = partitions.m_alBits;
        boolean fResult    = true;

        for (int i = 0, c = alBitsThis.length; i < c; ++i)
            {
            long lBitsThis = alBitsThis[i];
            long lBitsThat = alBitsThat[i];

            fResult &= (lBitsThis & lBitsThat) == lBitsThat;
            alBitsThis[i] = lBitsThis & ~lBitsThat;
            }

        m_cMarked = -1;
        return fResult;
        }

    /**
    * Retain only partitions in this set that are contained in the specified
    * PartitionSet.
    *
    * @param partitions  the PartitionSet to retain
    *
    * @return true if this PartitionSet changes as a result of this call;
    *          false otherwise
    */
    public boolean retain(PartitionSet partitions)
        {
        int cPartitions = m_cPartitions;
        azzert(cPartitions == partitions.m_cPartitions);

        long[]  alBitsThis = this      .m_alBits;
        long[]  alBitsThat = partitions.m_alBits;
        boolean fResult    = false;

        for (int i = 0, c = alBitsThis.length; i < c; ++i)
            {
            long lBitsThis = alBitsThis[i];
            long lBitsThat = alBitsThat[i];
            long lIntrsctn = lBitsThis & lBitsThat;

            if (lIntrsctn != lBitsThis)
                {
                alBitsThis[i] = lIntrsctn;
                fResult = true;
                }
            }

        if (fResult)
            {
            m_cMarked = -1;
            }
        return fResult;
        }

    /**
    * Check whether or not the specified partition belongs to the set.
    *
    * @param nPartition  the partition to check
    *
    * @return true if the specified partition is in the set;
    *         false otherwise
    */
    public boolean contains(int nPartition)
        {
        if (nPartition < 0 || nPartition >= m_cPartitions)
            {
            throw new IndexOutOfBoundsException(
                    nPartition + " not in [0, " + m_cPartitions + ')');
            }

        int  iLong = nPartition >>> 6;
        long lBits = m_alBits[iLong];
        long lMask = 1L << (nPartition & 63);

        return (lBits & lMask) != 0L;
        }

    /**
    * Check whether or not the specified partition set belongs to this set.
    *
    * @param partitions  the partition set to check
    *
    * @return true if all the partitions from the specified set are in this
    *               set; false otherwise
    */
    public boolean contains(PartitionSet partitions)
        {
        int cPartitions = m_cPartitions;
        azzert(cPartitions == partitions.m_cPartitions);

        long[] alBitsThis = this      .m_alBits;
        long[] alBitsThat = partitions.m_alBits;

        for (int i = 0, c = alBitsThis.length; i < c; ++i)
            {
            long lBitsThis = alBitsThis[i];
            long lBitsThat = alBitsThat[i];
            long lIntrsctn = lBitsThis & lBitsThat;

            if (lIntrsctn != lBitsThat)
                {
                return false;
                }
            }

        return true;
        }

    /**
    * Check whether or not the specified partition set intersects with this
    * set.
    *
    * @param partitions  the partition set to check
    *
    * @return true if the specified set contains at least one partition that
    *        is also present in this partition set; false otherwise
    */
    public boolean intersects(PartitionSet partitions)
        {
        int cPartitions = m_cPartitions;
        azzert(cPartitions == partitions.m_cPartitions);

        long[] alBitsThis = this      .m_alBits;
        long[] alBitsThat = partitions.m_alBits;

        for (int i = 0, c = alBitsThis.length; i < c; ++i)
            {
            long lBitsThis = alBitsThis[i];
            long lBitsThat = alBitsThat[i];
            long lIntrsctn = lBitsThis & lBitsThat;

            if (lIntrsctn != 0L)
                {
                return true;
                }
            }

        return false;
        }

    /**
    * Check whether or not the partition set is empty.
    *
    * @return true if none of the partitions are marked; false otherwise
    */
    public boolean isEmpty()
        {
        int cMarked = m_cMarked;
        if (cMarked >= 0)
            {
            return cMarked == 0;
            }

        long[] alBits = m_alBits;
        for (int i = 0, c = alBits.length; i < c; ++i)
            {
            if (alBits[i] != 0L)
                {
                return false;
                }
            }

        m_cMarked = 0;
        return true;
        }

    /**
    * Check whether or not the partition set is full.
    *
    * @return true if all the partitions are marked; false otherwise
    */
    public boolean isFull()
        {
        return cardinality() == getPartitionCount();
        }

    /**
    * Clear the set.
    *
    * @return  this PartitionSet
    */
    public PartitionSet clear()
        {
        long[] alBits = m_alBits;
        for (int i = 0, c = alBits.length; i < c; ++i)
            {
            alBits[i] = 0L;
            }
        m_cMarked = 0;

        return this;
        }

    /**
    * Fill the set to contain all the partitions.
    *
    * @return  this PartitionSet
    */
    public PartitionSet fill()
        {
        long[] alBits = m_alBits;
        int    iLast   = alBits.length - 1;

        for (int i = 0; i < iLast; ++i)
            {
            alBits[i] = -1L;
            }
        alBits[iLast] = m_lTailMask;

        m_cMarked = m_cPartitions;

        return this;
        }

    /**
    * Invert all the partitions. As a result of this operation, all marked
    * partitions will be cleared and all cleared partitions will become
    * marked.
    *
    * @return  this PartitionSet
    */
    public PartitionSet invert()
        {
        long[] alBits = m_alBits;
        int    iLast   = alBits.length - 1;

        for (int i = 0; i <= iLast; ++i)
            {
            alBits[i] = ~alBits[i];
            }
        alBits[iLast] &= m_lTailMask;

        int cMarked = m_cMarked;
        if (cMarked >= 0)
            {
            m_cMarked = m_cPartitions - cMarked;
            }

        return this;
        }

    /**
     * Return an index of the first marked partition. If no marked partitions
     * exists then -1 is returned.
     *
     * @return  the first marked partition, or -1 if no marked partitions
     *          exists in the set
     */
    public int first()
        {
        return next(0);
        }

    /**
    * Return an index of the first marked partition that is greater than or
    * equal to the specified partition. If no such partition exists then -1 is
    * returned.
    * <p>
    * This method could be used to iterate over all marked partitions:
    * <pre>{@code
    * for (int i = ps.next(0); i >= 0; i = ps.next(i+1))
    *     {
    *     // process partition
    *     }
    * }</pre>
    *
    * @param   nPartition  the partition to start checking from (inclusive)
    *
    * @return  the next marked partition, or -1 if no next marked partition
    *          exists in the set
    *
    * @throws  IndexOutOfBoundsException if the specified partition is
    *          invalid
    */
    public int next(int nPartition)
        {
        int cPartitions = m_cPartitions;
        if (nPartition < 0 || nPartition > cPartitions)
            {
            throw new IndexOutOfBoundsException(
                    nPartition + " not in [0, " + cPartitions + ')');
            }

        if (nPartition == cPartitions || m_cMarked == 0)
            {
            return -1;
            }

        long[] alBits = m_alBits;
        int    iLong  = nPartition >>> 6;
        int    ofBit  = nPartition & 63;
        long   lBits  = alBits[iLong] >>> ofBit;

        if (lBits == 0L)
            {
            ofBit = 0;

            // skip empty parts
            for (int iLast = alBits.length - 1; lBits == 0L && iLong < iLast; )
                {
                lBits = alBits[++iLong];
                }

            if (lBits == 0L)
                {
                return -1;
                }
            }

        return (iLong << 6) + ofBit + Long.numberOfTrailingZeros(lBits);
        }

    /**
    * Returns the number of marked partitions.
    *
    * @return  the number of marked partitions
    */
    public int cardinality()
        {
        int cMarked = m_cMarked;
        if (cMarked < 0)
            {
            cMarked = 0;
            long[] alBits = m_alBits;
            for (int i = 0, c = alBits.length; i < c; ++i)
                {
                cMarked += Long.bitCount(alBits[i]);
                }
            m_cMarked = cMarked;
            }

        return cMarked;
        }

    /**
    * Convert the partition set to an array of partition identifiers.
    *
    * @return an array of integer partition identifiers
    */
    public int[] toArray()
        {
        int   cPids = cardinality();
        int[] anPid = new int[cPids];
        for (int i = next(0), c = 0; i >= 0; i = next(i+1))
            {
            anPid[c++] = i;
            }
        return anPid;
        }

    /**
    * Obtain a random partition from the partition set.
    *
    * @return a randomly selected marked partition, or -1 if no partitions
    *         are marked
    */
    public int rnd()
        {
        int cPids = cardinality();
        if (cPids == 0)
            {
            return -1;
            }

        int nPid  = next(0);
        int cSkip = getRandom().nextInt(cPids);
        while (cSkip-- > 0)
            {
            nPid = next(nPid + 1);
            }
        return nPid;
        }

    /**
     * Split this partition set into two partition sets that are mutually
     * exclusive.
     *
     * @return a new PartitionSet containing approximately half of the marked
     *         partitions from this set, or null if this PartitionSet cannot
     *         be split
     */
    public PartitionSet split()
        {
        int cPids = cardinality() / 2;
        if (cPids == 0)
            {
            return null;
            }

        PartitionSet parts = new PartitionSet(m_cPartitions);
        for (int i = next(0), c = 0; c < cPids; i = next(i+1), c++)
            {
            parts.add(i);
            remove(i);
            }

        return parts;
        }

    /**
     * Return a union of the two provided PartitionSets.
     * <p>
     * This method will return a reference to one of the provided sets (or null),
     * and may mutate the returned set.
     *
     * @param partsA  set A
     * @param partsB  set B
     *
     * @return union of {@code A & B}
     */
    public static PartitionSet union(PartitionSet partsA, PartitionSet partsB)
        {
        if (partsA == null)
            {
            return partsB;
            }
        if (partsB == null)
            {
            return partsA;
            }
        partsA.add(partsB);

        return partsA;
        }

    // ----- Iterable interface ---------------------------------------------

    @Override
    public Iterator<Integer> iterator()
        {
        return new Iterator()
            {
            @Override
            public boolean hasNext()
                {
                return PartitionSet.this.next(m_current + 1) != -1;
                }

            @Override
            public Integer next()
                {
                return m_current = m_current == -1
                                   ? first()
                                   : PartitionSet.this.next(m_current + 1);
                }

            private volatile int m_current = -1;
            };
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int    cPartitions = in.readUnsignedShort();
        int    cLongs      = (cPartitions + 63) >>> 6;
        long[] alBits      = new long[cLongs];

        m_cPartitions = cPartitions;
        m_alBits      = alBits;
        m_lTailMask   = -1L >>> (64 - (cPartitions & 63));
        m_cMarked     = 0;

        int nFormat = in.readUnsignedByte();
        switch (nFormat)
            {
            case MARKED_NONE:
                break;

            case MARKED_FEW:
                {
                for (int iLast = 0, cSkip; (cSkip = ExternalizableHelper.readInt(in)) >= 0; )
                    {
                    iLast += cSkip;
                    add(iLast);
                    }
                }
                break;

            case MARKED_MANY:
                {
                int cMarked = 0;
                for (int i = 0; i < cLongs; ++i)
                    {
                    long lBits = in.readLong();
                    alBits[i]  = lBits;
                    cMarked   += Long.bitCount(lBits);
                    }
                m_cMarked = cMarked;
                }
                break;

            case MARKED_ALL:
                fill();
                break;

            default:
                throw new IOException("stream corrupted; format=" + nFormat);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        int cPartitions = m_cPartitions;
        out.writeShort(cPartitions);

        int cMarked = cardinality();
        if (cMarked == 0)
            {
            out.writeByte(MARKED_NONE);
            }
        else if (cMarked == cPartitions)
            {
            out.writeByte(MARKED_ALL);
            }
        else if (cMarked < (cPartitions >>> 5))
            {
            // likely to be optimal with the "few" format
            out.writeByte(MARKED_FEW);
            for (int iLast = 0, iCurr = next(0); iCurr >= 0; iCurr = next(iCurr +1))
                {
                ExternalizableHelper.writeInt(out, iCurr-iLast);
                iLast = iCurr;
                }
            ExternalizableHelper.writeInt(out, -1);
            }
        else
            {
            out.writeByte(MARKED_MANY);
            long[] alBits = m_alBits;
            for (int i = 0, c = alBits.length; i < c; ++i)
                {
                out.writeLong(alBits[i]);
                }
            }
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        // 0: partition-count
        // 1: format-indicator
        // 2: int array of gaps (for MARKED_FEW format)
        // 3: long array of bit masks (for MARKED_MANY format)
        // 4: reserved

        int    cPartitions = in.readInt(0);
        int    nFormat     = in.readInt(1);
        int    cLongs      = (cPartitions + 63) >>> 6;
        long[] alBits      = nFormat == MARKED_MANY
                           ? in.readLongArray(3)
                           : new long[cLongs];

        m_cPartitions = cPartitions;
        m_alBits      = alBits;
        m_lTailMask   = -1L >>> (64 - (cPartitions & 63));
        m_cMarked     = -1;

        switch (nFormat)
            {
            case MARKED_NONE:
                m_cMarked = 0;
                break;

            case MARKED_FEW:
                {
                int[] acSkip = in.readIntArray(2);
                int   cSkips = acSkip.length;
                for (int i = 0, iLast = 0; i < cSkips; ++i)
                    {
                    iLast += acSkip[i];
                    add(iLast);
                    }
                m_cMarked = cSkips;
                }
                break;

            case MARKED_MANY:
                // handled above
                break;

            case MARKED_ALL:
                fill();
                break;

            default:
                throw new IOException("stream corrupted; format=" + nFormat);
            }

        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        int cPartitions = m_cPartitions;
        out.writeInt(0, cPartitions);

        int cMarked = cardinality();
        if (cMarked == 0)
            {
            out.writeInt(1, MARKED_NONE);
            }
        else if (cMarked == cPartitions)
            {
            out.writeInt(1, MARKED_ALL);
            }
        else if (cMarked < (cPartitions >>> 5))
            {
            out.writeInt(1, MARKED_FEW);
            int[] an = toArray();
            for (int i = an.length - 1; i > 0; --i)
                {
                an[i] -= an[i-1];
                }
            out.writeIntArray(2, an);
            }
        else
            {
            out.writeInt(1, MARKED_MANY);
            out.writeLongArray(3, m_alBits);
            }
        }

    // ----- Object methods -------------------------------------------------

    /**
    * Indicates whether some other object is "equal to" this one.
    *
    * @param o  the object to test for equality
    *
    * @return <code>true</code> if this object is the same as the given one;
    *         <code>false</code> otherwise.
    */
    public boolean equals(Object o)
        {
        if (o instanceof PartitionSet)
            {
            if (o == this)
                {
                return true;
                }

            PartitionSet that = (PartitionSet) o;
            if (this.m_cPartitions == that.m_cPartitions)
                {
                // short-cut: compare the number marked
                int cMarkedThis = this.m_cMarked;
                int cMarkedThat = that.m_cMarked;
                if (cMarkedThis != cMarkedThat && cMarkedThis >= 0 && cMarkedThat >= 0)
                    {
                    return false;
                    }

                // full compare: compare all bits
                long[] alBitsThis = this.m_alBits;
                long[] alBitsThat = that.m_alBits;
                for (int i = 0, c = alBitsThis.length; i < c; ++i)
                    {
                    if (alBitsThis[i] != alBitsThat[i])
                        {
                        return false;
                        }
                    }

                return true;
                }
            }

        return false;
        }

    /**
    * Returns a hash code value for this PartitionSet.
    *
    * @return the hash code value for this PartitionSet
    */
    public int hashCode()
        {
        return 7 + Arrays.hashCode(m_alBits)
                 + m_cPartitions + m_cMarked;
        }

    /**
    * Returns a string representation of this PartitionSet.
    *
    * @return a string representation of this PartitionSet
    */
    public String toString()
        {
        return toString(true);
        }

    /**
    * Returns a string representation of this PartitionSet.
    *
    *
    * @param fVerbose  true for full information, false for terse
    *
    * @return a string representation of this PartitionSet
    */
    public String toString(boolean fVerbose)
        {
        StringBuilder sb      = new StringBuilder();
        boolean       fAppend = false;
        int           cRange  = 0;
        int           iPrev   = -1;

        if (fVerbose)
            {
            sb.append("PartitionSet{");
            }

        for (int iPid = next(0); iPid >= 0; iPid = next(iPid + 1))
            {
            if (iPid == (iPrev + 1) && iPrev >= 0)
                {
                // range continuation
                cRange++;
                }
            else
                {
                if (cRange > 0)
                    {
                    // range completion
                    sb.append(cRange > 1 ? ".." : ", ").append(iPrev);
                    cRange = 0;
                    }

                if (fAppend)
                    {
                    sb.append(", ");
                    }
                else
                    {
                    fAppend = true;
                    }

                sb.append(iPid);
                }

            iPrev = iPid;
            }

        if (cRange > 0)
            {
            sb.append(cRange > 1 ? ".." : ", ").append(iPrev);
            }

        if (fVerbose)
            {
            sb.append('}');
            }

        return sb.toString();
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Return the number of partitions represented by this PartitionSet.
    *
    * @return the total partition count
    */
    public int getPartitionCount()
        {
        return m_cPartitions;
        }

    // ----- constants ------------------------------------------------------

    /**
    * Serialization format indicator: Indicates that no partitions are
    * marked; MARKED_NONE requires no additional data.
    */
    protected static final int MARKED_NONE = 0;

    /**
    * Serialization format indicator: Indicates that a small number of
    * partitions are marked; followed by stream of packed integers indicating
    * gaps between each marked partition, terminated with a -1.
    */
    protected static final int MARKED_FEW  = 1;

    /**
    * Serialization format indicator: Indicates that a large number of
    * partitions are marked; followed by a sequence of 64-bit values
    * sufficient to represent the cardinality of the PartitionSet.
    */
    protected static final int MARKED_MANY = 2;

    /**
    * Serialization format indicator: Indicates that all partitions are
    * marked; MARKED_ALL requires no additional data.
    */
    protected static final int MARKED_ALL  = 3;


    // ----- data members ---------------------------------------------------

    /**
    * Total partition count.
    */
    @JsonbProperty("partitionCount")
    private int m_cPartitions;

    /**
    * A bit array representing the partitions, stored as an array of longs.
    */
    @JsonbProperty("bits")
    private long[] m_alBits;

    /**
    * A mask for the last long that indicates what bits get used.
    */
    @JsonbProperty("tailMask")
    private long m_lTailMask;

    /**
    * A cached count of marked partitions; -1 indicates that the value must
    * be recalculated.
    */
    @JsonbProperty("markedCount")
    private int m_cMarked;
    }
