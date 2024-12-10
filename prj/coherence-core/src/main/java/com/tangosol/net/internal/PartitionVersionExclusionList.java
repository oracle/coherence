/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import java.util.Arrays;

import java.util.function.Consumer;

/**
 * PartitionVersionExclusionList is a data structure that holds a number of
 * partition-id and version pairs that have been excluded.
 * <p>
 * Typically it is expected that this data structure is:
 * <ol>
 *     <li>Informed of {@link #exclude(int, int) exclusions}</li>
 *     <li>Asked whether a partition-id and version {@link #isExcluded(int, int)
 *     has been excluded}, or the {@link #isAllowed(int, int) negation}</li>
 *     <li>Request for exclusions for a {@link #reset(int, int) partition-id
 *     and version} or for all versions of a {@link #reset(int) partition-id
 *     to be reset}</li>
 * </ol>
 * This data structure is not thread-safe.
 *
 * @author hr  2015.07.10
 * @since 12.2.1
 */
public class PartitionVersionExclusionList
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct an instance of PartitionVersionExclusionList.
     */
    public PartitionVersionExclusionList()
        {
        this(8);
        }

    /**
     * Construct an instance of PartitionVersionExclusionList with the given
     * minimum size.
     *
     * @param cMinSize  the minimum size this data structure should use
     */
    public PartitionVersionExclusionList(int cMinSize)
        {
        f_cMinSize = cMinSize;

        initializeSlots(cMinSize);
        }

    // ----- public methods -------------------------------------------------

    /**
     * Exclude the provided partition-id and version from future calls to either
     * {@link #isExcluded(int, int)} or {@link #isAllowed(int, int)}.
     *
     * @param iPartition  the partition id to exclude
     * @param nVersion    the version to exclude
     */
    public void exclude(int iPartition, int nVersion)
        {
        assert iPartition >= 0;
        assert nVersion   >= 0;

        int iSlot = findIndex(iPartition, nVersion);
        if (iSlot < 0)
            {
            insert(iSlot, iPartition, nVersion);
            }
        }

    /**
     * Reset the knowledge this data structure holds for the provided partition.
     *
     * @param iPartition  the partition to reset
     */
    public void reset(int iPartition)
        {
        int iSlot;
        while ((iSlot = findIndex(iPartition)) != -1)
            {
            removeInternal(iSlot);
            }
        }
    /**
     * Reset the knowledge this data structure holds for the provided partition
     * and version.
     *
     * @param iPartition  the partition to reset
     * @param nVersion    the partition version to reset
     */
    public void reset(int iPartition, int nVersion)
        {
        int iPartVersion = findIndex(iPartition, nVersion);
        if (iPartVersion >= 0)
            {
            removeInternal(iPartVersion);
            }
        }

    /**
     * Return true if the provided partition id and version has been excluded.
     *
     * @param iPartition  the partition to check
     * @param nVersion    the partition version to check
     *
     * @return whether the partition and version has been excluded
     */
    public boolean isExcluded(int iPartition, int nVersion)
        {
        return findIndex(iPartition, nVersion) >= 0;
        }

    /**
     * Return true if the provided partition id and version has not been
     * excluded.
     *
     * @param iPartition  the partition to check
     * @param nVersion    the partition version to check
     *
     * @return whether the partition and version has not been excluded
     */
    public boolean isAllowed(int iPartition, int nVersion)
        {
        return !isExcluded(iPartition, nVersion);
        }

    /**
     * Iterate this data structure passing each excluded partition id and version
     * to the provided {@link Consumer} as an {@link Entry}.
     *
     * @param consumer  the consumer for each exluced partition and version
     */
    public void forEach(Consumer<Entry> consumer)
        {
        int   cSlots = m_alPartVersions.length;
        Entry entry  = new Entry();
        for (int i = cSlots - m_cSize; i < cSlots; ++i)
            {
            entry.reset(i);
            consumer.accept(entry);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Remove the given slot from the internal array.
     *
     * @param iSlot  the slot that should be removed
     */
    protected void removeInternal(int iSlot)
        {
        // shift to the right

        for (int i = iSlot, iStart = m_alPartVersions.length - m_cSize - 1; i > iStart; --i)
            {
            m_alPartVersions[i] = i <= 0 ? 0 : m_alPartVersions[i - 1];
            }
        --m_cSize;

        shrink();
        }

    /**
     * Return the index of the first excluded version for the given partition,
     * or a negative value.
     *
     * @param iPart  the partition to search for
     *
     * @return the index of the first excluded version for the given partition
     *         or a negative value
     */
    protected int findIndex(int iPart)
        {
        long   lInternal      = toInternal(iPart, 0);
        long[] alPartVersions = m_alPartVersions;
        int    iPartVersion   = Arrays.binarySearch(alPartVersions, lInternal);

        if (iPartVersion < 0)
            {
            int iClosest = -(iPartVersion + 1);

            iPartVersion = iClosest < alPartVersions.length && getPartition(alPartVersions[iClosest]) == iPart
                    ? iClosest : -1;
            }
        return iPartVersion;
        }

    /**
     * Return the index for the given partition and version, or a negative value
     * if the partition and version is not present.
     *
     * @param iPart     the partition to search for
     * @param nVersion  the version to search for
     *
     * @return the index for the given partition and version, or a negative value
     *         if the partition and version is not present
     */
    protected int findIndex(int iPart, int nVersion)
        {
        return Arrays.binarySearch(m_alPartVersions, toInternal(iPart, nVersion));
        }

    /**
     * Insert the provided partition and version into the given insertion point.
     * The insertion point is expected to be the return of executing {@link
     * Arrays#binarySearch(long[], long)}.
     *
     * @param iInsert     the insertion point
     * @param iPartition  the partition to insert
     * @param nVersion    the version to insert
     */
    protected void insert(int iInsert, int iPartition, int nVersion)
        {
        assert iInsert < 0;
        
        int    cSizeDelta     = ensureCapacity(m_cSize + 1);
        long[] alPartVersions = m_alPartVersions;

        if (cSizeDelta > 0)
            {
            // adjust the insertion point
            iInsert -= cSizeDelta;
            }

        // invert the insertion point to a slot index and shift the slots
        // to the left by 1
        iInsert = -(iInsert + 2);

        for (int i = alPartVersions.length - m_cSize; i <= iInsert; ++i)
            {
            alPartVersions[i - 1] = alPartVersions[i];
            }
        alPartVersions[iInsert] = toInternal(iPartition, nVersion);
        ++m_cSize;
        }

    /**
     * Ensure sufficient capacity is available within this data structure.
     * 
     * @param cSize  the minimum capacity required
     *
     * @return number of slots added
     */
    protected int ensureCapacity(int cSize)
        {
        int cSlots = m_alPartVersions.length;
        if (cSize >= cSlots)
            {
            // grow
            int    cNewSlots      = Math.min(cSize << 1, cSize + 32);
            int    cSizeDelta     = cNewSlots - cSize;
            long[] alPartVersions = m_alPartVersions;

            initializeSlots(cNewSlots);

            // ensure the head of the array are 0's
            System.arraycopy(alPartVersions, 0, m_alPartVersions, cSizeDelta, cSlots);

            return cSizeDelta;
            }
        return 0;
        }

    /**
     * Shrink this data structure if there is sufficient unused slots.
     */
    protected void shrink()
        {
        int cSlots = m_alPartVersions.length;
        int cSize  = m_cSize;
        int cFree  = cSlots - cSize;

        if (cSlots > f_cMinSize &&
            cFree > Math.max(cSlots >> 2, 16))
            {
            int    cNewSize       = Math.max(f_cMinSize, cSlots - (cFree >> 1));
            long[] alPartVersions = m_alPartVersions;

            initializeSlots(cNewSize);

            System.arraycopy(alPartVersions, cSlots - cSize, m_alPartVersions, cNewSize - cSize, cSize);
            }
        }

    /**
     * Return a long with the given partition and version encoded.
     *
     * @param iPart     the partition to encode
     * @param nVersion  the version to encode
     *
     * @return a long with the partition and version encoded
     */
    protected long toInternal(int iPart, int nVersion)
        {
        return (iPart & 0xFFFFFFFFL) << 32 | (nVersion & 0xFFFFFFFFL);
        }

    /**
     * Return the partition encoded in the provided long.
     *
     * @param lInternal  long with partition and version encoded
     *
     * @return the partition
     */
    protected int getPartition(long lInternal)
        {
        return (int) (lInternal >>> 32);
        }

    /**
     * Return the partition version encoded in the provided long.
     *
     * @param lInternal  long with partition and version encoded
     *
     * @return the partition version
     */
    protected int getVersion(long lInternal)
        {
        return  (int) (lInternal & 0xFFFFFFFFL);
        }

    /**
     * Initialize the slots to the provided size.
     *
     * @param cSlots  the number of slots
     */
    protected void initializeSlots(int cSlots)
        {
        m_alPartVersions = new long[cSlots];
        Arrays.fill(m_alPartVersions, 0, cSlots - m_cSize, NO_VALUE);
        }

    // ----- inner class: Entry ---------------------------------------------

    /**
     * An Entry exposing an excluded the partition and version.
     */
    public class Entry
        {
        // ----- constructors -----------------------------------------------

        /**
         * Construct an Entry.
         */
        protected Entry()
            {
            }

        /**
         * Construct an Entry with the given slot index.
         */
        protected Entry(int iSlot)
            {
            m_iSlot = iSlot;
            }

        /**
         * Return the partition.
         *
         * @return the partition
         */
        public int getPartition()
            {
            return PartitionVersionExclusionList.this.getPartition(m_alPartVersions[m_iSlot]);
            }

        /**
         * Return the partition version.
         *
         * @return the partition version
         */
        public int getVersion()
            {
            return PartitionVersionExclusionList.this.getVersion(m_alPartVersions[m_iSlot]);
            }

        /**
         * Reset the slot this Entry encapsulates.
         *
         * @param iSlot  the slot this Entry should expose
         */
        protected void reset(int iSlot)
            {
            m_iSlot = iSlot;
            }

        // ----- data members -----------------------------------------------

        /**
         * The slot index this Entry exposes.
         */
        protected int m_iSlot;
        }

    // ----- private helpers ------------------------------------------------

    /**
     * Represents a slot in m_alPartVersions with no value.
     */
    protected static final long NO_VALUE = -1L;

    // ----- data members ---------------------------------------------------

    /**
     * The minimum size this data structure should use.
     */
    protected final int f_cMinSize;

    /**
     * The storage for excluded partition and version information.
     */
    protected long[] m_alPartVersions;

    /**
     * The number of excluded partition and versions.
     */
    protected int m_cSize;
    }
