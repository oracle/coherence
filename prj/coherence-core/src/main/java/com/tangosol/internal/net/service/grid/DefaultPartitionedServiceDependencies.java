/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.PartitionAssignmentStrategyBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.net.partition.DefaultKeyAssociator;
import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.KeyPartitioningStrategy;
import com.tangosol.net.partition.PartitionListener;

import com.tangosol.util.Base;

import java.util.LinkedList;
import java.util.List;

/**
 * The DefaultPartitionedServiceDependencies is the default implementation of
 * PartitionedServiceDependencies.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.2
 */
public class DefaultPartitionedServiceDependencies
        extends DefaultGridDependencies
        implements PartitionedServiceDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultPartitionedServiceDependencies object.
     */
    public DefaultPartitionedServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultPartitionedServiceDependencies object, copying the values from the
     * specified PartitionedServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultPartitionedServiceDependencies(PartitionedServiceDependencies deps)
        {
        super(deps);

        if (deps == null)
            {
            setWorkerThreadCountMin(1); // enable auto-sizing pool by default
            }
        else
            {
            m_cBackups                        = deps.getPreferredBackupCount();
            m_nDistributionAggressiveness     = deps.getDistributionAggressiveness();
            m_fDistributionSynchronized       = deps.isDistributionSynchronized();
            m_keyAssociator                   = deps.getKeyAssociator();
            m_keyPartitioningStrategy         = deps.getKeyPartitioningStrategy();
            m_fOwnershipCapable               = deps.isOwnershipCapable();
            m_cPartitions                     = deps.getPreferredPartitionCount();
            m_bldrsPartitionListener          = deps.getPartitionListenerBuilders();
            m_bldrAssignmentStrategy          = deps.getPartitionAssignmentStrategyBuilder();
            m_cbTransferThreshold             = deps.getTransferThreshold();
            m_fAsyncBackup                    = deps.isAsyncBackupEnabled();

            // copy PersistenceDependencies independent of OwnershipCapable.
            m_depsPersistence                 = deps.getPersistenceDependencies();
            }
        }

    // ----- GridDependencies interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Injectable("partitioned-quorum-policy-scheme")
    public void setActionPolicyBuilder(ActionPolicyBuilder builder)
        {
        super.setActionPolicyBuilder(builder);
        }

    // ----- DefaultPartitionedServiceDependencies methods ------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPreferredBackupCount()
        {
        return m_cBackups;
        }

    /**
     * Set the backup count.
     *
     * @param cBackups  the backup count
     */
    @Injectable("backup-count")
    public void setPreferredBackupCount(int cBackups)
        {
        m_cBackups = cBackups;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDistributionAggressiveness()
        {
        return m_nDistributionAggressiveness;
        }

    /**
     * Set the distribution aggressiveness.
     *
     * @param nAggressiveness  the distribution aggressiveness
     */
    public void setDistributionAggressiveness(int nAggressiveness)
        {
        m_nDistributionAggressiveness = nAggressiveness;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDistributionSynchronized()
        {
        return m_fDistributionSynchronized;
        }

    /**
     * Set the distribution synchronized flag.
     *
     * @param fSync  true if distribution is synchronized
     */
    public void setDistributionSynchronized(boolean fSync)
        {
        m_fDistributionSynchronized = fSync;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyAssociator getKeyAssociator()
        {
        KeyAssociator keyAssociator = m_keyAssociator;

        if (keyAssociator == null)
            {
            m_keyAssociator = keyAssociator = new DefaultKeyAssociator();
            }

        return keyAssociator;
        }

    /**
     * Set the KeyAssociator.
     *
     * @param associator  the KeyAssociator
     */
    @Injectable("key-associator")
    public void setKeyAssociator(KeyAssociator associator)
        {
        m_keyAssociator = associator;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public KeyPartitioningStrategy getKeyPartitioningStrategy()
        {
        return m_keyPartitioningStrategy;
        }

    /**
     * Set the KeyPartitioningStrategy.
     *
     * @param strategy  the KeyPartitioningStrategy
     */
    @Injectable("key-partitioning")
    public void setKeyPartitioningStrategy(KeyPartitioningStrategy strategy)
        {
        m_keyPartitioningStrategy = strategy;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOwnershipCapable()
        {
        return m_fOwnershipCapable;
        }

    /**
     * Set the ownership enabled flag.
     *
     * @param fEnabled  true if ownership is enabled
     */
    @Injectable("local-storage")
    public void setOwnershipCapable(boolean fEnabled)
        {
        m_fOwnershipCapable = fEnabled;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAsyncBackupEnabled()
        {
        return m_fAsyncBackup;
        }

    /**
     * Set the async-backup enabled flag.
     *
     * @param fEnabled  true if async-backup is enabled
     */
    @Injectable("async-backup")
    public void setAsyncBackupEnabled(boolean fEnabled)
        {
        m_fAsyncBackup = fEnabled;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public PartitionAssignmentStrategyBuilder getPartitionAssignmentStrategyBuilder()
        {
        PartitionAssignmentStrategyBuilder bldr = m_bldrAssignmentStrategy;

        if (bldr == null)
            {
            m_bldrAssignmentStrategy = bldr = new PartitionAssignmentStrategyBuilder("simple", null);
            }

        return bldr;
        }

    /**
     * Set the PartitionAssignmentStrategyBuilder.
     *
     * @param builder  the PartitionAssignmentStrategyBuilder
     */
    @Injectable("partition-assignment-strategy")
    public void setPartitionAssignmentStrategyBuilder(PartitionAssignmentStrategyBuilder builder)
        {
        m_bldrAssignmentStrategy = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPreferredPartitionCount()
        {
        return m_cPartitions;
        }

    /**
     * Set the partition count.
     *
     * @param cPartitions  the partition count
     */
    @Injectable("partition-count")
    public void setPreferredPartitionCount(int cPartitions)
        {
        m_cPartitions = cPartitions;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterizedBuilder<PartitionListener>> getPartitionListenerBuilders()
        {
        List<ParameterizedBuilder<PartitionListener>> listeners = m_bldrsPartitionListener;

        if (listeners == null)
            {
            m_bldrsPartitionListener = listeners = new LinkedList<ParameterizedBuilder<PartitionListener>>();
            }

        return listeners;
        }

    /**
     * Set the PartitionListener list.
     *
     * @param bldrsListener  the PartitionListener Builders list
     */
    @Injectable("partition-listener")
    public void setPartitionListenerBuilders(List<ParameterizedBuilder<PartitionListener>> bldrsListener)
        {
        m_bldrsPartitionListener = bldrsListener;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransferThreshold()
        {
        return m_cbTransferThreshold;
        }

    /**
     * Set the transfer threshold (in KB).
     *
     * @param cbThreshold  the transfer threshold (in KB)
     */
    @Injectable("transfer-threshold")
    public void setTransferThreshold(int cbThreshold)
        {
        final int KB = 1024;

        m_cbTransferThreshold = cbThreshold * KB;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public PersistenceDependencies getPersistenceDependencies()
        {
        return m_depsPersistence;
        }

    /**
     * Set the PersistenceDependencies.
     *
     * @param deps  the PersistenceDependencies
     */
    @Injectable("persistence")
    public void setPersistenceDependencies(PersistenceDependencies deps)
        {
        m_depsPersistence = deps;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultPartitionedServiceDependencies validate()
        {
        super.validate();

        Base.checkRange(getPreferredBackupCount(), 0, 256, "BackupCount");
        Base.checkRange(getPreferredPartitionCount(), 1, Short.MAX_VALUE, "PartitionCount");
        Base.checkRange(getTransferThreshold(), 1024, Integer.MAX_VALUE, "TransferThreshold");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
                + "{PreferredBackupCount="                + getPreferredBackupCount()
                + ", DistributionAggressiveness="         + getDistributionAggressiveness()
                + ", DistributionSynchronized="           + isDistributionSynchronized()
                + ", KeyAssociator="                      + getKeyAssociator()
                + "  KeyPartitioningStrategy="            + getKeyPartitioningStrategy()
                + ", OwnershipCapable="                   + isOwnershipCapable()
                + ", PreferredPartitionCount="            + getPreferredPartitionCount()
                + ", PartitionListenerBuilders="          + getPartitionListenerBuilders()
                + ", PartitionAssignmentStrategyBuilder=" + getPartitionAssignmentStrategyBuilder()
                + ", TransferThreshold="                  + getTransferThreshold()
                + ", AsyncBackupEnabled="                 + isAsyncBackupEnabled()
                + ", PersistenceDependencies="            + getPersistenceDependencies()
                + "}";
        }

    // ----- data members ---------------------------------------------------

    /**
     * The backup count.
     */
    private int m_cBackups = 1;

    /**
     * The distribution aggressiveness.
     */
    private int m_nDistributionAggressiveness = Config.getInteger("coherence.distributed.aggressive", 20);

    /**
     * The distribution synchronized flag.
     */
    private boolean m_fDistributionSynchronized = Config.getBoolean("coherence.distributed.synchronize", true);

    /**
     * The key associator.
     */
    private KeyAssociator m_keyAssociator;

    /**
     * The key partitioning strategy.
     */
    private KeyPartitioningStrategy m_keyPartitioningStrategy;

    /**
     * The ownership capable flag.
     */
    private boolean m_fOwnershipCapable = true;

    /**
     * The async backup flag.
     */
    private boolean m_fAsyncBackup = false;

    /**
     * The partition count.
     */
    private int m_cPartitions = 257;

    /**
     * The PartitionListener Builders list.
     */
    private List<ParameterizedBuilder<PartitionListener>> m_bldrsPartitionListener;

    /**
     * The partition assignment strategy builder.
     */
    private PartitionAssignmentStrategyBuilder m_bldrAssignmentStrategy;

    /**
     * The transfer threshold.
     */
    private int m_cbTransferThreshold = 524288;

    /**
     * The PersistenceDependencies.
     */
    private PersistenceDependencies m_depsPersistence;
    }
