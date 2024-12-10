/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.partition;

import java.util.Iterator;

/**
 * VersionedPartitions implementations provide a mapping from partition to version.
 * This data structure is intended to be used to track versions for {@link VersionAwareMapListener}
 * allowing those implementations to return the last versions received when needed.
 *
 * @author hr  2021.02.17
 * @since 21.06
 */
public interface VersionedPartitions
        extends Iterable<Long>
    {
    /**
     * Return the partitions being tracked.
     *
     * @return the partitions being tracked
     */
    public Iterator<Integer> getPartitions();

    /**
     * Return the version last received for the given partition.
     *
     * @param iPartition  the partition in question
     *
     * @return the version last received for the given partition
     */
    public long getVersion(int iPartition);

    /**
     * Returns a {@link VersionedIterator} that allows retrieval of the next
     * element as a {@link VersionedIterator#nextVersion() primitive}. Additionally
     * the current partition can be returned via {@link VersionedIterator#getPartition()}.
     *
     * @return a versioned iterator to return the partition and version
     */
    @Override
    VersionedIterator iterator();

    // ----- inner interface: Iterator --------------------------------------

    /**
     * An Iterator for partitions being tracked by a {@link VersionedPartitions}
     * implementation that allows retrieval of the next partition as a {@link
     * Iterator#nextPartition() primitive}.
     *
     * @param <T>  the type for this Iterator
     */
    public interface Iterator<T>
            extends java.util.Iterator<T>
        {
        /**
         * Return the current partition.
         *
         * @return the current partition
         */
        public int getPartition();

        /**
         * Return and progress this Iterator to the next partition.
         *
         * @return the next partition
         */
        public int nextPartition();
        }

    // ----- inner interface: VersionedIterator -----------------------------

    /**
     * An Iterator for partitions being tracked by a {@link VersionedPartitions}
     * implementation that allows retrieval of partition and version.
     * <p>
     * This iterator allows for primitive types for partition and version to be
     * returned, as well as progression of this iterator via {@link VersionedIterator#nextVersion()}
     * or {@link VersionedIterator#nextPartition()}.
     */
    public interface VersionedIterator
            extends Iterator<Long>
        {
        /**
         * Return the current version.
         *
         * @return the current version
         */
        public long getVersion();

        /**
         * Return the next version by progressing this iterator to the next element
         * in this Iterator.
         *
         * @return the next version
         */
        public long nextVersion();
        }
    }
