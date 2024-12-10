/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.io.DecorationOnlyDeltaCompressor;
import com.tangosol.io.DeltaCompressor;

import com.tangosol.util.Base;

import com.tangosol.config.annotation.Injectable;

/**
 * The DefaultPartitionedCacheDependencies is the default implementation of
 * PartitionedCacheDependencies.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.2
 */
public class DefaultPartitionedCacheDependencies
        extends DefaultPartitionedServiceDependencies
        implements PartitionedCacheDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultPartitionedCacheDependencies object.
     */
    public DefaultPartitionedCacheDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultPartitionedCacheDependencies object, copying the values from the
     * specified PartitionedCacheDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultPartitionedCacheDependencies(PartitionedCacheDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_cBackupAfterWriteBehind = deps.getBackupCountAfterWriteBehind();
            m_deltaCompressor         = deps.getDeltaCompressor();
            m_nLeaseGranularity       = deps.getLeaseGranularity();
            m_fStrictPartitioning     = deps.isStrictPartitioning();
            m_cStandardLeaseMillis    = deps.getStandardLeaseMillis();
            }
        }

    // ----- PartitionedCacheDependencies interface -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBackupCountAfterWriteBehind()
        {
        return m_cBackupAfterWriteBehind == -1 ? getPreferredBackupCount() : m_cBackupAfterWriteBehind;
        }

    /**
     * Set the backup after write-behind count.
     *
     * @param cBackups  the backup after write-behind value
     */
    @Injectable("backup-count-after-writebehind")
    public void setBackupCountAfterWriteBehind(int cBackups)
        {
        m_cBackupAfterWriteBehind = cBackups;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeltaCompressor getDeltaCompressor()
        {
        return m_deltaCompressor == null ? new DecorationOnlyDeltaCompressor() : m_deltaCompressor;
        }

    /**
     * Set the DeltaCompressor.
     *
     * @param compressor  the DeltaCompressor
     */
    @Injectable("compressor")
    public void setDeltaCompressor(DeltaCompressor compressor)
        {
        m_deltaCompressor = compressor;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isStrictPartitioning()
        {
        return m_fStrictPartitioning;
        }

    /**
     * Set the strict partitioning flag.
     *
     * @param fStrict  true if strict partitioning is enabled
     */
    public void setStrictPartitioning(boolean fStrict)
        {
        m_fStrictPartitioning = fStrict;
        }

    // ----- LeaseDependencies interface ------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLeaseGranularity()
        {
        return m_nLeaseGranularity;
        }

    /**
     * Set the lease granularity.
     *
     * @param nGranularity  the lease granularity
     */
    @Injectable("lease-granularity")
    public void setLeaseGranularity(int nGranularity)
        {
        m_nLeaseGranularity = nGranularity;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getStandardLeaseMillis()
        {
        return m_cStandardLeaseMillis;
        }

    /**
     * Set the standard lease milliseconds.
     *
     * @param cMillis  the standard lease milliseconds
     */
    @Injectable("standard-lease-milliseconds")
    public void setStandardLeaseMillis(long cMillis)
        {
        m_cStandardLeaseMillis = cMillis;
        }

    // ----- helpers --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultPartitionedServiceDependencies validate()
        {
        super.validate();

        Base.checkRange(getStandardLeaseMillis(), 0, Integer.MAX_VALUE, "StandardLeaseMillis");
        Base.checkRange(getLeaseGranularity(), 0, Integer.MAX_VALUE, "LeaseGranularity");

        validateBackupCountAfterWriteBehind();

        return this;
        }

    /**
     * Validate the backup count after write behind.
     */
    protected void validateBackupCountAfterWriteBehind()
        {
        int cBackups    = getPreferredBackupCount();
        int cBackupsOpt = getBackupCountAfterWriteBehind();

        if (cBackupsOpt != cBackups && cBackupsOpt > 0)
            {
            Logger.warn("Valid values for the <backup-count-after-writebehind> " + "element are 0 or " + cBackups
                        + " (the value from the " + "<backup-count> element); defaulting to " + cBackups
                        + " for the service.");
            setBackupCountAfterWriteBehind(cBackups);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The backup count after write behind.
     */
    private int m_cBackupAfterWriteBehind = -1;

    /**
     * The DeltaCompressor.
     */
    private DeltaCompressor m_deltaCompressor = null;

    /**
     * The lease granularity.
     */
    private int m_nLeaseGranularity = PartitionedCacheDependencies.LEASE_BY_THREAD;

    /**
     * The standard lease milliseconds.
     */
    private long m_cStandardLeaseMillis = 0;

    /**
     * The strict partitioning flag.
     */
    private boolean m_fStrictPartitioning = Config.getBoolean("coherence.distributed.strict", true);
    }
