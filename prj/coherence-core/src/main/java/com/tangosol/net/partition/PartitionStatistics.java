/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import java.util.concurrent.atomic.AtomicLong;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
 * PartitionStatistics encapsulates the statistics gathered for a given partition
 * in a PartitionedService.  Partition statistics are collected and managed by a
 * {@link DistributionManager} and can be used by a {@link PartitionAssignmentStrategy}
 * to drive decisions about partition distribution.
 *
 * @author rhl 2011.08.14
 * @since  Coherence 12.2.1
 */
public class PartitionStatistics
        implements ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PartitionStatistics()
        {
        }

    /**
     * Constructor for the specified partition.
     *
     * @param nPartition  the partition id
     * @param ldtArrived  the time at which this partition was transferred in
     */
    public PartitionStatistics(int nPartition, long ldtArrived)
        {
        m_nPartition      = nPartition;
        m_ldtLastTransfer = ldtArrived;

        reset();
        }

    /**
     * Copy the statistics from the specified PartitionStatistics reference.
     *
     * @param stats  the statistics to copy from
     *
     * @return the copied statistics
     */
    public PartitionStatistics copyFrom(PartitionStatistics stats)
        {
        m_nPartition        = stats.getPartition();
        m_ldtLastTransfer   = stats.getLastTransferTime();
        m_cExecutionTime    = stats.getTaskExecutionTime();
        m_cWaitTime         = stats.getTaskWaitTime();
        m_cRequests         = stats.getRequestCount();
        m_ldtSampleStart    = stats.getSampleStartTime();
        m_cSampleMillis     = stats.getSampleDuration();
        m_cbStorageDirect   = stats.getDirectStorageSize();
        m_cbStorageIndirect.set(stats.getIndirectStorageSize());

        return this;
        }

    /**
     * Update the partition statistics to record the specified request.
     *
     * @param cWaitMillis  the time (in ms) that the task/request spent waiting
     * @param cExecMillis  the time (in ms) that the task/request spent executing
     */
    public void recordRequest(long cWaitMillis, long cExecMillis)
        {
        m_cExecutionTime += cExecMillis;
        m_cWaitTime      += cWaitMillis;
        m_cRequests++;
        }

    /**
     * Adjust the non-PartitionAwareBackingMap storage size of the partition
     * by the specified amount.
     *
     * @param cbDelta  the amount to adjust the size (could be negative)
     */
    public void adjustIndirectStorageSize(long cbDelta)
        {
        m_cbStorageIndirect.addAndGet(cbDelta);
        }

    /**
     * Set the PartitionAwareBackingMaps storage size of the partition.
     * <p>
     * Note that this method is called only on the service thread at
     * the end of sampling period.
     *
     * @param cb  total storage size of the PartitionAwareBackingMaps in the partition
     *
     * @return the PartitionStatistics reference
     */
    public PartitionStatistics setDirectStorageSize(long cb)
        {
        m_cbStorageDirect = cb;
        m_cSampleMillis   = Base.getSafeTimeMillis() - m_ldtSampleStart;

        return this;
        }


    // ----- internal methods -----------------------------------------------

    /**
     * Reset the partition statistics state.
     */
    public void reset()
        {
        m_ldtSampleStart = Base.getLastSafeTimeMillis();
        m_cSampleMillis  = 0;
        m_cRequests      = 0;
        m_cExecutionTime = 0;
        m_cWaitTime      = 0;
        }


    // ----- accessors ------------------------------------------------------

    /**
     * Return the id of the partition for which these statistics apply.
     *
     * @return the partition id
     */
    public int getPartition()
        {
        return m_nPartition;
        }

    /**
     * Return the time (on the primary owner) at which the partition was last
     * transferred or restored.
     *
     * @return the time at which the specified storage was last transferred
     */
    public long getLastTransferTime()
        {
        return m_ldtLastTransfer;
        }

    /**
     * Return the total amount of time (in ms) that tasks/requests for this
     * partition spent executing.
     *
     * @return the total amount of time that tasks spent waiting
     */
    public long getTaskExecutionTime()
        {
        return m_cExecutionTime;
        }

    /**
     * Return the total amount of time (in ms) that tasks/requests for this
     * partition spent waiting before execution.
     *
     * @return the total amount of time that tasks spent waiting
     */
    public long getTaskWaitTime()
        {
        return m_cWaitTime;
        }

    /**
     * Return the number of requests/second issued for the partition.
     *
     * @return the request count/second
     */
    public long getRequestCount()
        {
        return m_cRequests;
        }

    /**
     * Return the partition size from partitioned maps.
     *
     * @return the partition size from partitioned maps
     */
    protected long getDirectStorageSize()
        {
        return m_cbStorageDirect;
        }

    /**
     * Return the partition size from non-PartitionAwareBackingMaps.
     *
     * @return the partition size from non-PartitionAwareBackingMaps
     */
    protected long getIndirectStorageSize()
        {
        return m_cbStorageIndirect.get();
        }

    /**
     * Return the total size (in bytes) of the storage associated with the partition.
     *
     * @return the storage size
     */
    public long getStorageSize()
        {
        return m_cbStorageDirect + m_cbStorageIndirect.get();
        }

    /**
     * Return the time (on the sampling member) at which this data collection
     * for this sample started.
     *
     * @return the time that this sample started
     */
    public long getSampleStartTime()
        {
        return m_ldtSampleStart;
        }

    /**
     * Return the duration (ms) over which data was collected for this sample.
     *
     * @return the sample duration
     */
    public long getSampleDuration()
        {
        return m_cSampleMillis;
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
        {
        m_nPartition        = ExternalizableHelper.readInt(in);
        m_ldtLastTransfer   = ExternalizableHelper.readLong(in);
        m_ldtSampleStart    = ExternalizableHelper.readLong(in);
        m_cSampleMillis     = ExternalizableHelper.readLong(in);
        m_cRequests         = ExternalizableHelper.readLong(in);
        m_cExecutionTime    = ExternalizableHelper.readLong(in);
        m_cWaitTime         = ExternalizableHelper.readLong(in);
        m_cbStorageDirect   = ExternalizableHelper.readLong(in);
        m_cbStorageIndirect.set(ExternalizableHelper.readLong(in));
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt (out, m_nPartition);
        ExternalizableHelper.writeLong(out, m_ldtLastTransfer);
        ExternalizableHelper.writeLong(out, m_ldtSampleStart);
        ExternalizableHelper.writeLong(out, m_cSampleMillis);
        ExternalizableHelper.writeLong(out, m_cRequests);
        ExternalizableHelper.writeLong(out, m_cExecutionTime);
        ExternalizableHelper.writeLong(out, m_cWaitTime);
        ExternalizableHelper.writeLong(out, m_cbStorageDirect);
        ExternalizableHelper.writeLong(out, m_cbStorageIndirect.get());
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return "PartitionStatistics{" + getDescription() + '}';
        }


    // ----- internal methods -----------------------------------------------

    /**
     * Return a human-readable description of this statistics object.
     *
     * @return a human-readable description of this statistics object
     */
    public String getDescription()
        {
        StringBuilder sb = new StringBuilder();

        sb.append("PartitionId=").append(getPartition()).append(", ")
          .append("LastTransferTime=\"").append(Base.formatDateTime(getLastTransferTime())).append("\", ")
          .append("SampleStartTime=\"").append(Base.formatDateTime(getSampleStartTime())).append("\", ")
          .append("SampleDuration=").append(getSampleDuration()).append(", ")
          .append("StorageSize=").append(getStorageSize()).append(", ")
          .append("RequestCount=").append(getRequestCount()).append(", ")
          .append("TaskExecutionTime=").append(getTaskExecutionTime()).append(", ")
          .append("TaskWaitTime=").append(getTaskWaitTime());

        return sb.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
     * The partition ID.
     */
    protected int m_nPartition;

    /**
     * The time at this this partition was last transferred.
     */
    protected long m_ldtLastTransfer;

    /**
     * The sample start time.
     */
    protected long m_ldtSampleStart;

    /**
     * The sample duration (in ms).
     */
    protected long m_cSampleMillis;

    /**
     * The portion of the partition size that is obtained directly from
     * {@link PartitionAwareBackingMap partitioned} maps (in bytes).
     * This value is only updated on the service thread and doesn't have to be
     * atomic.
     */
    protected long m_cbStorageDirect;

    /**
     * The portion of the partition size that cannot be obtained directly and
     * has to be adjusted upon each cache entry update (in bytes).
     */
    protected AtomicLong m_cbStorageIndirect = new AtomicLong();

    /**
     * The number of requests on this partition during the sampling period.
     */
    protected long m_cRequests;

    /**
     * The total amount of time (in ms) that tasks/requests were executing.
     */
    protected long m_cExecutionTime;

    /**
     * The total amount of time (in ms) that tasks/requests spent waiting.
     */
    protected long m_cWaitTime;
    }
