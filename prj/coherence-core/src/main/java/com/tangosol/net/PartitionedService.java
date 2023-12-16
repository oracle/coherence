/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.net.internal.QuorumInfo;

import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionAssignmentStrategy;
import com.tangosol.net.partition.PartitionListener;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.persistence.GUIDHelper.GUIDResolver;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;

import java.util.Set;


/**
* A PartitionedService is aware of a mapping of keys to partitions and of
* partitions to cluster members.
*
* @since Coherence 3.0
*/
public interface PartitionedService
        extends Service
    {
    /**
    * Determine the number of partitions that the service has been configured
    * to "break up" the conceptual "key set" into.
    * <p>
    * The value of this property is in the range <tt>[1..n]</tt> where
    * <tt>n</tt> is an arbitrarily large integer value that does not
    * exceed <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of separate partitions into which this service
    *         partitions the set of potential keys
    */
    public int getPartitionCount();

    /**
    * Determine the configured redundancy count that this service has been
    * configured to maintain. A redundancy count of zero implies that the
    * service will not maintain backup copies of information for failover
    * purposes, while a redundancy count greater than zero indicates that
    * the service must attempt to synchronously maintain that number of
    * backup copies of the data under the management of the service in order
    * to provide seamless (and lossless) failover of data and processing.
    * <p>
    * The value of this property is in the range <tt>[0..n]</tt> where
    * <tt>n</tt> is an arbitrarily large integer value that does not
    * exceed <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the configured redundancy count for this service
    */
    public int getBackupCount();

    /**
    * Determine the key associator that this service has been
    * configured to use. Information provided by this KeyAssociator will be used
    * to place all associated keys into the same partition.
    *
    * @return the KeyAssociator for this service
    */
    public KeyAssociator getKeyAssociator();

    /**
    * Determine the strategy for key partitioning that this service has been
    * configured to use.
    *
    * @return the KeyPartitioningStrategy for this service
    */
    public KeyPartitioningStrategy getKeyPartitioningStrategy();

    /**
    * Determine the strategy for assigning partitions to cluster members that
    * this service has been configured to use.
    *
    * @return the PartitionAssignmentStrategy for this service
    */
    public PartitionAssignmentStrategy getPartitionAssignmentStrategy();

    /**
    * Determine the primary owner of the specified key, as determined by the
    * combination of the KeyPartitioningStrategy and the
    * PartitionAssignmentStrategy. It's possible that during partition
    * re-distribution (e.g. as a result of a failover) this method will return
    * null, indicating that the partition ownership is currently undetermined.
    *
    * @param oKey  a key in its Object form
    *
    * @return the cluster Member that is currently the owner for the specified
    *          key or null if the ownership is currently undetermined
    */
    public Member getKeyOwner(Object oKey);

    /**
     * Instantiate a {@link Converter} that can convert a key to a {@link Binary} key.
     *
     * @param loader        the {@link ClassLoader} to use
     * @param fPassThrough  {@link true} if the converter should be a pass-thru binary converter
     * @param <V>           the type of key to convert
     *
     * @return a {@link Converter} that can convert a key to a {@link Binary} key
     */
    public <V> Converter<V, Binary> instantiateKeyToBinaryConverter(ClassLoader loader, boolean fPassThrough);

    /**
    * Determine the primary owner of the specified partition.  It is possible
    * that during partition re-distribution (e.g. as a result of a failover)
    * that this method will return null, indicating that the partition ownership
    * is currently undetermined.
    *
    * @param nPartition  a partition ID
    *
    * @return the cluster Member that is currently the owner for the specified
    *         partition or null if the distribution is currently undetermined
    *
    * @throws IllegalArgumentException if the partition number is negative or
    *         greater than the {@link PartitionedService#getPartitionCount()
    *         partition count} for this partitioned service
    */
    public Member getPartitionOwner(int nPartition);

    /**
    * Determine the primary owner's version of the specified partition.  It is possible
    * that during partition re-distribution (e.g. as a result of a failover)
    * that this method will return -1, indicating that the partition ownership
    * is currently undetermined.
    *
    * @param nPartition  a partition ID
    *
    * @return the partition ownership version or -1
    *
    * @throws IllegalArgumentException if the partition number is negative or
    *         greater than the {@link PartitionedService#getPartitionCount()
    *         partition count} for this partitioned service
    *
    * @since Coherence 12.2.1.1
    */
    public int getOwnershipVersion(int nPartition);

    /**
    * Determine the backup owner of the specified partition.  It is possible
    * that during partition re-distribution (e.g. as a result of a failover)
    * that this method will return null, indicating that the partition ownership
    * is currently undetermined.
    *
    * @param nPartition  a partition ID
    * @param nBackup     the backup number (one-based)
    *
    * @return the cluster Member that is currently the owner for the specified
    *         backup copy for the given partition, or null if the distribution
    *         is currently undetermined
    *
    * @throws IllegalArgumentException if the partition number is negative or
    *         greater than the {@link PartitionedService#getPartitionCount()
    *         partition count} or if the backup number is non-positive or
    *         greater than the {@link PartitionedService#getBackupCount()
    *         backup count} for this partitioned service
    */
    public Member getBackupOwner(int nPartition, int nBackup);

    /**
    * Determine the PartitionSet that is currently owned by a cluster Member.
    * If the specified member does not run this clustered service, null is
    * returned.
    * <p>
    * <b>Note:</b> the returned PartitionSet represents a "snapshot" of the
    * ownership information at a time of the call and may change at any moment.
    *
    * @param member  the cluster Member
    *
    * @return the PartitionSet that the cluster Member currently owns
    *
    * @since Coherence 3.4
    */
    public PartitionSet getOwnedPartitions(Member member);

    /**
    * Return a Set of Member objects, one for each Member that has registered
    * this PartitionedService and is partition ownership-enabled.
    *
    * @return a set of Member objects that provide partition ownership
    *        for this partitioned service
    *
    * @since Coherence 3.6
    */
    public Set<Member> getOwnershipEnabledMembers();

    /**
     * Return the senior ownership-enabled member in the service.
     *
     * @return the senior ownership-enabled member in the service
     *
     * @since Coherence 12.2.1
     */
    public Member getOwnershipSenior();

    /**
    * Add a PartitionListener to this service.
    *
    * @param listener  the listener to add
    *
    * @since Coherence 3.7
    */
    public void addPartitionListener(PartitionListener listener);

    /**
    * Remove a PartitionListener from this service.
    *
    * @param listener  the listener to remove
    *
    * @since Coherence 3.7
    */
    public void removePartitionListener(PartitionListener listener);

    /**
     * Return the current backup strength of the partitioned service.
     *
     * @return the current backup strength of the partitioned service
     */
    public int getBackupStrength();

    /**
     * Return the string representing current backup strength.
     *
     * @return the string representing current backup strength
     */
    public String getBackupStrengthName();

    /**
     * Return the persistence mode, or {@code null} if persistence
     * is not configured.
     *
     * @return the persistence mode, or {@code null} if persistence
     *         is not configured
     */
    public String getPersistenceMode();

    // ----- inner interface: PartitionedAction ---------------------------

    /**
    * PartitionedAction represents a type of action taken by a
    * PartitionedService.
    */
    public interface PartitionedAction
            extends Action
        {
        /**
        * Singleton action for partition distribution/backup.
        */
        public static final Action DISTRIBUTE = new PartitionedAction() {};

        /**
        * Singleton action for partition restore.
        */
        public static final Action RESTORE    = new PartitionedAction() {};
        }


    // ----- inner class: PartitionRecoveryAction -------------------------

    /**
    * A PartitionedAction representing the recovery of orphaned partitions from
    * the persistent storage, or the assignment of empty partitions if the
    * persistent storage is unavailable or lost.
    */
    public static class PartitionRecoveryAction
            implements PartitionedAction
        {
        /**
        * Construct a PartitionRecoveryAction for the specified partitions,
        * GUID resolver and the "last good" membership info.
        *
        * @param partsOrphan  the set of orphaned partitions
        * @param resolver     the GUID resolver
        * @param infoQuorum   the "last good" membership info
        */
        public PartitionRecoveryAction(PartitionSet partsOrphan,
                GUIDResolver resolver, QuorumInfo infoQuorum)
            {
            m_partsOrphan = partsOrphan;
            m_resolver    = resolver;
            m_infoQuorum  = infoQuorum;
            }

        // ----- accessors ------------------------------------------------

        /**
        * Return the GUID resolver.
        *
        * @return the GUID resolver
        */
        public GUIDResolver getResolver()
            {
            return m_resolver;
            }

        /**
        * Return the set of orphaned partitions.
        *
        * @return the set of orphaned partitions
        */
        public PartitionSet getOrphanedPartitions()
            {
            return m_partsOrphan;
            }

        /**
        * Return the "last good" service membership info.
        *
        * @return the "last good" service membership
        */
        public QuorumInfo getQuorumInfo()
            {
            return m_infoQuorum;
            }

        // ----- data members ---------------------------------------------

        /**
        * The GUID resolver.
        */
        protected GUIDResolver m_resolver;

        /**
        * The set of orphaned partitions.
        */
        protected PartitionSet m_partsOrphan;

        /**
        * The "last good" service membership.
        */
        protected QuorumInfo m_infoQuorum;
        }
    }
