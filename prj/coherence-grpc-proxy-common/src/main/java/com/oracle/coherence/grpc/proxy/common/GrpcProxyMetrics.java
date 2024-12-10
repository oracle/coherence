/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Exceptions;
import com.tangosol.internal.net.metrics.Histogram;
import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.internal.net.metrics.Snapshot;
import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.Registry;

import javax.management.NotCompliantMBeanException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * The gRPC Proxy metrics MBean implementation.
 *
 * @author Jonathan Knight  2020.10.14
 */
public class GrpcProxyMetrics
        implements GrpcProxyMetricsMBean, GrpcProxyServiceMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link GrpcProxyMetrics}.
     *
     * @param sName           the MBean ObjectName
     * @param poolManagement  the daemon pool management to get pool metrics
     *
     * @throws NullPointerException is the name parameter is {@code null}
     */
    public GrpcProxyMetrics(String sName, DaemonPoolExecutor.DaemonPoolManagement poolManagement)
        {
        Objects.requireNonNull(sName);

        f_sMBeanName       = sName;
        f_poolManagement   = poolManagement;
        f_requestHistogram = new Histogram();
        f_messageHistogram = new Histogram();
        f_meterSuccess     = new Meter();
        f_meterError       = new Meter();
        f_meterSent        = new Meter();
        f_meterReceived    = new Meter();
        }

    // ----- GrpcProxyMetrics methods ---------------------------------------

    /**
     * Add a timing sample to the metrics.
     *
     * @param nanos  the request time in nanos.
     */
    @Override
    public void addRequestDuration(long nanos)
        {
        // convert the time to millis
        f_requestHistogram.update(TimeUnit.NANOSECONDS.toMillis(nanos));
        }

    /**
     * Add a timing sample to the metrics.
     *
     * @param nanos  the request time in nanos.
     */
    @Override
    public void addMessageDuration(long nanos)
        {
        // convert the time to millis
        f_messageHistogram.update(TimeUnit.NANOSECONDS.toMillis(nanos));
        }

    /**
     * Update the successful request meter.
     */
    @Override
    public void markSuccess()
        {
        f_meterSuccess.mark();
        }

    /**
     * Update the failed request meter.
     */
    @Override
    public void markError()
        {
        f_meterError.mark();
        }

    /**
     * Update the messages sent meter.
     */
    @Override
    public void markSent()
        {
        f_meterSent.mark();
        }

    /**
     * Update the messages received meter.
     */
    @Override
    public void markReceived()
        {
        f_meterReceived.mark();
        }

    // ----- GrpcProxyMetricsMBean methods ----------------------------------

    @Override
    public long getSuccessfulRequestCount()
        {
        return f_meterSuccess.getCount();
        }

    @Override
    public double getSuccessfulRequestFifteenMinuteRate()
        {
        return f_meterSuccess.getFifteenMinuteRate();
        }

    @Override
    public double getSuccessfulRequestFiveMinuteRate()
        {
        return f_meterSuccess.getFiveMinuteRate();
        }

    @Override
    public double getSuccessfulRequestOneMinuteRate()
        {
        return f_meterSuccess.getOneMinuteRate();
        }

    @Override
    public double getSuccessfulRequestMeanRate()
        {
        return f_meterSuccess.getMeanRate();
        }

    @Override
    public long getErrorRequestCount()
        {
        return f_meterError.getCount();
        }

    @Override
    public double getErrorRequestFifteenMinuteRate()
        {
        return f_meterError.getFifteenMinuteRate();
        }

    @Override
    public double getErrorRequestFiveMinuteRate()
        {
        return f_meterError.getFiveMinuteRate();
        }

    @Override
    public double getErrorRequestOneMinuteRate()
        {
        return f_meterError.getOneMinuteRate();
        }

    @Override
    public double getErrorRequestMeanRate()
        {
        return f_meterError.getMeanRate();
        }

    @Override
    public double getRequestDuration75thPercentile()
        {
        return ensureRequestSnapshot().get75thPercentile();
        }

    @Override
    public double getRequestDuration95thPercentile()
        {
        return ensureRequestSnapshot().get95thPercentile();
        }

    @Override
    public double getRequestDuration98thPercentile()
        {
        return ensureRequestSnapshot().get98thPercentile();
        }

    @Override
    public double getRequestDuration99thPercentile()
        {
        return ensureRequestSnapshot().get99thPercentile();
        }

    @Override
    public double getRequestDuration999thPercentile()
        {
        return ensureRequestSnapshot().get999thPercentile();
        }

    @Override
    public double getRequestDurationMax()
        {
        return ensureRequestSnapshot().getMax();
        }

    @Override
    public double getRequestDurationMean()
        {
        return ensureRequestSnapshot().getMean();
        }

    @Override
    public double getRequestDurationMin()
        {
        return ensureRequestSnapshot().getMin();
        }

    @Override
    public double getRequestDurationStdDev()
        {
        return ensureRequestSnapshot().getStdDev();
        }

    @Override
    public double getMessageDuration75thPercentile()
        {
        return ensureMessageSnapshot().get75thPercentile();
        }

    @Override
    public double getMessageDuration95thPercentile()
        {
        return ensureMessageSnapshot().get95thPercentile();
        }

    @Override
    public double getMessageDuration98thPercentile()
        {
        return ensureMessageSnapshot().get98thPercentile();
        }

    @Override
    public double getMessageDuration99thPercentile()
        {
        return ensureMessageSnapshot().get99thPercentile();
        }

    @Override
    public double getMessageDuration999thPercentile()
        {
        return ensureMessageSnapshot().get999thPercentile();
        }

    @Override
    public double getMessageDurationMax()
        {
        return ensureMessageSnapshot().getMax();
        }

    @Override
    public double getMessageDurationMean()
        {
        return ensureMessageSnapshot().getMean();
        }

    @Override
    public double getMessageDurationMin()
        {
        return ensureMessageSnapshot().getMin();
        }

    @Override
    public double getMessageDurationStdDev()
        {
        return ensureMessageSnapshot().getStdDev();
        }

    @Override
    public long getMessagesReceivedCount()
        {
        return f_meterReceived.getCount();
        }

    @Override
    public double getMessagesReceivedFifteenMinuteRate()
        {
        return f_meterReceived.getFifteenMinuteRate();
        }

    @Override
    public double getMessagesReceivedFiveMinuteRate()
        {
        return f_meterReceived.getFiveMinuteRate();
        }

    @Override
    public double getMessagesReceivedOneMinuteRate()
        {
        return f_meterReceived.getOneMinuteRate();
        }

    @Override
    public double getMessagesReceivedMeanRate()
        {
        return f_meterReceived.getMeanRate();
        }

    @Override
    public long getResponsesSentCount()
        {
        return f_meterSent.getCount();
        }

    @Override
    public double getResponsesSentFifteenMinuteRate()
        {
        return f_meterSent.getFifteenMinuteRate();
        }

    @Override
    public double getResponsesSentFiveMinuteRate()
        {
        return f_meterSent.getFiveMinuteRate();
        }

    @Override
    public double getResponsesSentOneMinuteRate()
        {
        return f_meterSent.getOneMinuteRate();
        }

    @Override
    public double getResponsesSentMeanRate()
        {
        return f_meterSent.getMeanRate();
        }

    @Override
    public int getTaskBacklog()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getBacklog();
        }

    @Override
    public int getDaemonCountMax()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getDaemonCountMax();
        }

    @Override
    public void setDaemonCountMax(int count)
        {
        if (f_poolManagement != null)
            {
            f_poolManagement.setDaemonCountMax(count);
            }
        }

    @Override
    public int getDaemonCountMin()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getDaemonCountMin();
        }

    @Override
    public void setDaemonCountMin(int count)
        {
        if (f_poolManagement != null)
            {
            f_poolManagement.setDaemonCountMin(count);
            }
        }

    @Override
    public int getDaemonCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getDaemonCount();
        }

    @Override
    public int getAbandonedThreadCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getAbandonedCount();
        }

    @Override
    public long getTaskActiveMillis()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getActiveMillis();
        }

    @Override
    public int getHungTaskCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getHungCount();
        }

    @Override
    public long getHungTaskDuration()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getHungDuration();
        }

    @Override
    public long getLastResetMillis()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getLastResetMillis();
        }

    @Override
    public long getLastResizeMillis()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getLastResizeMillis();
        }

    @Override
    public long getTaskAddCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getTaskAddCount();
        }

    @Override
    public long getTaskCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getTaskCount();
        }

    @Override
    public int getMaxTaskBacklog()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getMaxBacklog();
        }

    @Override
    public int getTaskTimeoutCount()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getTimeoutCount();
        }

    @Override
    public long getTaskTimeout()
        {
        return f_poolManagement == null ? 0 : f_poolManagement.getTaskTimeout();
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Registers an MBean for this {@code GrpcProxyMetrics}.
     */
    public void registerMBean(Registry registry)
        {
        try
            {
            String           globalName = registry.ensureGlobalName(f_sMBeanName);
            registry.register(globalName, new AnnotatedStandardMBean(this, GrpcProxyMetricsMBean.class));
            }
        catch (NotCompliantMBeanException e)
            {
            throw Exceptions.ensureRuntimeException(e);
            }
        }

    /**
     * Ensure that there is a relatively recent snapshot from
     * the request duration histogram.
     *
     * @return  a relatively recent snapshot from the request
     *          duration histogram
     */
    Snapshot ensureRequestSnapshot()
        {
        long nLastSnapshot = m_nLastRequestSnapshot;
        long nNow          = System.currentTimeMillis();
        long nTime         = nNow - nLastSnapshot;

        // only refresh the snapshot at most every 250 millis
        if (m_requestSnapshot == null || nTime > MIN_SNAPSHOT_REFRESH)
            {
            m_requestSnapshot      = f_requestHistogram.getSnapshot();
            m_nLastRequestSnapshot = nNow;
            }
        return m_requestSnapshot;
        }

    /**
     * Ensure that there is a relatively recent snapshot from
     * the message duration histogram.
     *
     * @return  a relatively recent snapshot from the message
     *          duration histogram
     */
    Snapshot ensureMessageSnapshot()
        {
        long     nLastSnapshot = m_nLastMessageSnapshot;
        long     nNow          = System.currentTimeMillis();
        long     nTime         = nNow - nLastSnapshot;

        // only refresh the snapshot at most every 250 millis
        if (m_messageSnapshot == null || nTime > MIN_SNAPSHOT_REFRESH)
            {
            m_messageSnapshot      = f_messageHistogram.getSnapshot();
            m_nLastMessageSnapshot = nNow;
            }
        return m_messageSnapshot;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The minimum amount of time between request duration histogram snapshots.
     */
    public static final long MIN_SNAPSHOT_REFRESH = 250L;

    // ----- data members ---------------------------------------------------

    /**
     * The name of the MBean.
     */
    private final String f_sMBeanName;

    /**
     * The {@link DaemonPoolExecutor.DaemonPoolManagement} to get daemon pool metrics.
     */
    private final DaemonPoolExecutor.DaemonPoolManagement f_poolManagement;

    /**
     * The request duration histogram.
     */
    private final Histogram f_requestHistogram;

    /**
     * The message handling duration histogram.
     */
    private final Histogram f_messageHistogram;

    /**
     * The successful requests meter.
     */
    private final Meter f_meterSuccess;

    /**
     * The failed requests meter.
     */
    private final Meter f_meterError;

    /**
     * The failed requests meter.
     */
    private final Meter f_meterSent;

    /**
     * The failed requests meter.
     */
    private final Meter f_meterReceived;

    /**
     * The last snapshot from the request duration histogram.
     */
    private Snapshot m_requestSnapshot;

    /**
     * The time of the last snapshot from the request duration histogram.
     */
    private volatile long m_nLastRequestSnapshot;

    /**
     * The last snapshot from the message duration histogram.
     */
    private Snapshot m_messageSnapshot;

    /**
     * The time of the last snapshot from the message duration histogram.
     */
    private volatile long m_nLastMessageSnapshot;
    }
