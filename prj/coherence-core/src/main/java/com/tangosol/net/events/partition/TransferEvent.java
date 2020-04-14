/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.events.partition;

import com.tangosol.net.Member;
import com.tangosol.util.BinaryEntry;

import java.util.Map;
import java.util.Set;

/**
 * A TransferEvent captures information concerning the transfer
 * of a partition for a storage enabled member. Transfer events are
 * raised against the set of {@link BinaryEntry entries} that are being
 * transferred.
 * <p>
 * Note: TransferEvents are dispatched to interceptors while
 * holding a lock on the partition for being transferred, blocking any
 * operations for the partition.
 *
 * @author rhl/hr/gg  2012.09.21
 * @since Coherence 12.1.2
 */
public interface TransferEvent
        extends Event<TransferEvent.Type>
    {
    /**
     * Return the ID of the partition being transferred.
     *
     * @return the ID of the partition being transferred
     */
    public int getPartitionId();

    /**
     * Return the local {@link Member} associated with this transfer
     * operation. For the {@link Type#DEPARTING DEPARTING} event
     * this is the member the entries are being transferred from. For the
     * {@link Type#ARRIVED ARRIVED} event, this is the member
     * that is receiving the entries.
     *
     * @return the local Member associated with this event
     */
    public Member getLocalMember();

    /**
     * Return the remote {@link Member} associated with this transfer
     * operation. For the {@link Type#DEPARTING DEPARTING} event
     * this is the member the entries are being transferred to. For the
     * {@link Type#ARRIVED ARRIVED} event, this is the member that the
     * entries are being transferred from.
     * <p>
     * In the case the {@link Type#ARRIVED ARRIVED} event, the
     * returned member could be null, indicating a "partition restore"
     * operation.
     *
     * @return the remote Member associated with this event
     */
    public Member getRemoteMember();

    /**
     * Return a map of cache names and associated set of read-only {@link
     * BinaryEntry entries} encapsulated in this {@link TransferEvent}. The
     * returned map and contained sets are immutable.
     *
     * @return a map of cache names and associated set of entries
     */
    public Map<String, Set<BinaryEntry>> getEntries();

    // ----- inner interface: RecoveryTransferEvent -------------------------

    /**
     * A RecoveryTransferEvent is raised due to the recovery of a partition
     * from a persistent store.
     * <p>
     * The cause of this event can be due to either active persistence recovery
     * or recovery from a snapshot. This can be distinguished by the snapshot
     * name being null or non-null for active persistence recovery or snapshot
     * recovery respectively.
     */
    public interface RecoveryTransferEvent
            extends TransferEvent
        {
        /**
         * Return the name of the snapshot if the partition was recovered from
         * a snapshot and not active persistence.
         *
         * @return the name of the snapshot the partition was recovered from
         *         or null in the case of active persistence
         */
        public String getSnapshotName();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link TransferEvent} types.
     */
    public static enum Type
        {
        /**
         * This {@link TransferEvent} is dispatched when a set of {@link
         * BinaryEntry entries} have been transferred to the {@link
         * TransferEvent#getLocalMember local member} or restored from backup.
         * <p>
         * The reason for the event (primary transfer from another member or
         * restore from backup) can be derived as follows:
         * <pre>{@code
         *     TransferEvent event;
         *     boolean       fRestored = event.getRemoteMember() == event.getLocalMember();
         * }</pre>
         */
        ARRIVED,

        /**
         * This {@link TransferEvent} is dispatched when a partition has been
         * assigned to the {@link TransferEvent#getLocalMember local member}.
         * This event may only be emitted at the ownership senior during the
         * initial partition assignment.
         */
        ASSIGNED,

        /**
         * This {@link TransferEvent} is dispatched when a set of {@link
         * BinaryEntry entries} are being transferred from the {@link
         * TransferEvent#getLocalMember local member}. This event is followed
         * by either a DEPARTED or ROLLBACK event to indicate the success
         * or failure of the transfer.
         */
        DEPARTING,

        /**
         * This {@link TransferEvent} is dispatched when a partition has
         * been successfully transferred from the {@link
         * TransferEvent#getLocalMember local member}. To derive the {@link
         * BinaryEntry entries} associated with the transfer, consumers should
         * subscribe to the DEPARTING event that would precede this event.
         */
        DEPARTED,

        /**
         * This {@link TransferEvent} is dispatched when a partition has been
         * orphaned (data loss may have occurred), and the ownership is
         * assumed by the {@link TransferEvent#getLocalMember local member}.
         * This event may only be emitted at the ownership senior.
         */
        LOST,

        /**
         * This {@link TransferEvent} is dispatched when a set of {@link
         * BinaryEntry entries} have been recovered from a persistent storage
         * by the {@link TransferEvent#getLocalMember local member}.
         */
        RECOVERED,

        /**
         * This {@link TransferEvent} is dispatched when partition transfer
         * has failed and was therefore rolled back. To derive the {@link
         * BinaryEntry entries} associated with the failed transfer, consumers
         * should subscribe to the DEPARTING event that would precede this event.
         */
        ROLLBACK
        }
    }
