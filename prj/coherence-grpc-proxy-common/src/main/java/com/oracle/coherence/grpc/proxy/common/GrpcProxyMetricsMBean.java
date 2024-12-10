/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsLabels;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsValue;
import com.tangosol.net.metrics.MBeanMetric;

/**
 * The gRPC Proxy metrics MBean.
 *
 * @author Jonathan Knight  2020.10.14
 */
@MetricsScope(MBeanMetric.Scope.VENDOR)
public interface GrpcProxyMetricsMBean
    {
    /**
     * Return the count of requests that responded with success.
     *
     * @return  the count of requests that responded with success
     */
    @MetricsValue("RequestCount")
    @Description("The count of requests that responded with success")
    long getSuccessfulRequestCount();

    /**
     * Return the fifteen minute rate of successful requests.
     *
     * @return  the fifteen minute rate of successful requests
     */
    @MetricsValue("RequestRate")
    @MetricsLabels({"rate", "15min"})
    @Description("The fifteen minute rate of successful requests")
    double getSuccessfulRequestFifteenMinuteRate();

    /**
     * Return the five minute rate of successful requests.
     *
     * @return  the five minute rate of successful requests
     */
    @MetricsValue("RequestRate")
    @MetricsLabels({"rate", "5min"})
    @Description("The five minute rate of successful requests")
    double getSuccessfulRequestFiveMinuteRate();

    /**
     * Return the one minute rate of successful requests.
     *
     * @return  the one minute rate of successful requests
     */
    @MetricsValue("RequestRate")
    @MetricsLabels({"rate", "1min"})
    @Description("The one minute rate of successful requests")
    double getSuccessfulRequestOneMinuteRate();

    /**
     * Return the one minute rate of successful requests.
     *
     * @return  the one minute rate of successful requests.
     */
    @MetricsValue("RequestRate")
    @MetricsLabels({"rate", "mean"})
    @Description("The mean rate of successful requests")
    double getSuccessfulRequestMeanRate();

    /**
     * Return the count of requests that responded with an error.
     *
     * @return  the count of requests that responded with an error
     */
    @MetricsValue("ErrorRequestCount")
    @Description("The count of requests that responded with an error")
    long getErrorRequestCount();

    /**
     * Return the fifteen minute rate of requests that responded with an error.
     *
     * @return  the fifteen minute rate of requests that responded with an error
     */
    @MetricsValue("ErrorRequestRate")
    @MetricsLabels({"rate", "15min"})
    @Description("The fifteen minute rate of requests that responded with an error")
    double getErrorRequestFifteenMinuteRate();

    /**
     * Return the five minute rate of requests that responded with an error.
     *
     * @return  the five minute rate of requests that responded with an error
     */
    @MetricsValue("ErrorRequestRate")
    @MetricsLabels({"rate", "5min"})
    @Description("The five minute rate of requests that responded with an error")
    double getErrorRequestFiveMinuteRate();

    /**
     * Return the one minute rate of requests that responded with an error.
     *
     * @return  the one minute rate of requests that responded with an error
     */
    @MetricsValue("ErrorRequestRate")
    @MetricsLabels({"rate", "1min"})
    @Description("The one minute rate of requests that responded with an error")
    double getErrorRequestOneMinuteRate();

    /**
     * Return the mean rate of requests that responded with an error.
     *
     * @return  the mean rate of requests that responded with an error
     */
    @MetricsValue("ErrorRequestRate")
    @MetricsLabels({"rate", "mean"})
    @Description("The mean rate of requests that responded with an error")
    double getErrorRequestMeanRate();

    /**
     * Return the count of messages received.
     *
     * @return  the count of messages received
     */
    @MetricsValue("MessagesReceivedCount")
    @Description("The count of messages received")
    long getMessagesReceivedCount();

    /**
     * Return the fifteen minute rate of messages received.
     *
     * @return  the fifteen minute rate of messages received
     */
    @MetricsValue("MessagesReceivedRate")
    @MetricsLabels({"rate", "15min"})
    @Description("The fifteen minute rate of messages received")
    double getMessagesReceivedFifteenMinuteRate();

    /**
     * Return the five minute rate of messages received.
     *
     * @return  the five minute rate of messages received
     */
    @MetricsValue("MessagesReceivedRate")
    @MetricsLabels({"rate", "5min"})
    @Description("The five minute rate of messages received")
    double getMessagesReceivedFiveMinuteRate();

    /**
     * Return the one minute rate of messages received.
     *
     * @return  the one minute rate of messages received
     */
    @MetricsValue("MessagesReceivedRate")
    @MetricsLabels({"rate", "1min"})
    @Description("The one minute rate of messages received")
    double getMessagesReceivedOneMinuteRate();

    /**
     * Return the mean rate of messages received.
     *
     * @return  the mean rate of messages received
     */
    @MetricsValue("MessagesReceivedRate")
    @MetricsLabels({"rate", "mean"})
    @Description("The mean rate of messages received")
    double getMessagesReceivedMeanRate();

    /**
     * Return the count of responses sent.
     *
     * @return  the count of responses sent
     */
    @MetricsValue("ResponsesSentCount")
    @Description("The count of responses sent")
    long getResponsesSentCount();

    /**
     * Return the fifteen minute rate of responses sent.
     *
     * @return  the fifteen minute rate of responses sent
     */
    @MetricsValue("ResponsesSentRate")
    @MetricsLabels({"rate", "15min"})
    @Description("The fifteen minute rate of responses sent")
    double getResponsesSentFifteenMinuteRate();

    /**
     * Return the five minute rate of responses sent.
     *
     * @return  the five minute rate of responses sent
     */
    @MetricsValue("ResponsesSentRate")
    @MetricsLabels({"rate", "5min"})
    @Description("The five minute rate of responses sent")
    double getResponsesSentFiveMinuteRate();

    /**
     * Return the one minute rate of responses sent.
     *
     * @return  the one minute rate of responses sent
     */
    @MetricsValue("ResponsesSentRate")
    @MetricsLabels({"rate", "1min"})
    @Description("The one minute rate of responses sent")
    double getResponsesSentOneMinuteRate();

    /**
     * Return the mean rate of responses sent.
     *
     * @return  the mean rate of responses sent
     */
    @MetricsValue("ResponsesSentRate")
    @MetricsLabels({"rate", "mean"})
    @Description("The mean rate of responses sent")
    double getResponsesSentMeanRate();

    /**
     * Return the 75th percentile value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the 75th percentile value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "75th"})
    @Description("The 75th percentile value from the latest request duration metric distribution")
    double getRequestDuration75thPercentile();

    /**
     * Return the 95th percentile value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the 95th percentile value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "95th"})
    @Description("The 95th percentile value from the latest request duration metric distribution")
    double getRequestDuration95thPercentile();

    /**
     * Return the 98th percentile value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the 98th percentile value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "98th"})
    @Description("The 98th percentile value from the latest request duration metric distribution")
    double getRequestDuration98thPercentile();

    /**
     * Return the 99th percentile value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the 99th percentile value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "99th"})
    @Description("The 99th percentile value from the latest request duration metric distribution")
    double getRequestDuration99thPercentile();

    /**
     * Return the 99.9th percentile value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the 99.9th percentile value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "99.9th"})
    @Description("The 99.9th percentile value from the latest request duration metric distribution")
    double getRequestDuration999thPercentile();

    /**
     * Return the maximum value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the maximum value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "max"})
    @Description("The maximum value from the latest request duration metric distribution")
    double getRequestDurationMax();

    /**
     * Return the mean value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the mean value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "mean"})
    @Description("The mean value from the latest request duration metric distribution")
    double getRequestDurationMean();

    /**
     * Return the minimum value from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the minimum value from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "min"})
    @Description("The minimum value from the latest request duration metric distribution")
    double getRequestDurationMin();

    /**
     * Return the standard deviation from the distribution of samples in the latest snapshot
     * of the request duration metric.
     *
     * @return  the standard deviation from the distribution of samples in the latest snapshot
     *          of the request duration metric.
     */
    @MetricsValue("RequestDuration")
    @MetricsLabels({"quantile", "stddev"})
    @Description("The standard deviation from the latest request duration metric distribution")
    double getRequestDurationStdDev();

    /**
     * Return the 75th percentile value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the 75th percentile value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "75th"})
    @Description("The 75th percentile value from the latest message duration metric distribution")
    double getMessageDuration75thPercentile();

    /**
     * Return the 95th percentile value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the 95th percentile value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "95th"})
    @Description("The 95th percentile value from the latest message duration metric distribution")
    double getMessageDuration95thPercentile();

    /**
     * Return the 98th percentile value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the 98th percentile value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "98th"})
    @Description("The 98th percentile value from the latest message duration metric distribution")
    double getMessageDuration98thPercentile();

    /**
     * Return the 99th percentile value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the 99th percentile value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "99th"})
    @Description("The 99th percentile value from the latest message duration metric distribution")
    double getMessageDuration99thPercentile();

    /**
     * Return the 99.9th percentile value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the 99.9th percentile value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "99.9th"})
    @Description("The 99.9th percentile value from the latest message duration metric distribution")
    double getMessageDuration999thPercentile();

    /**
     * Return the maximum value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the maximum value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "max"})
    @Description("The maximum value from the latest message duration metric distribution")
    double getMessageDurationMax();

    /**
     * Return the mean value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the mean value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "mean"})
    @Description("The mean value from the latest message duration metric distribution")
    double getMessageDurationMean();

    /**
     * Return the minimum value from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the minimum value from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "min"})
    @Description("The minimum value from the latest message duration metric distribution")
    double getMessageDurationMin();

    /**
     * Return the standard deviation from the distribution of samples in the latest snapshot
     * of the message duration metric.
     *
     * @return  the standard deviation from the distribution of samples in the latest snapshot
     *          of the message duration metric.
     */
    @MetricsValue("MessageDuration")
    @MetricsLabels({"quantile", "stddev"})
    @Description("The standard deviation from the latest message duration metric distribution")
    double getMessageDurationStdDev();

    /**
     * Return the number of tasks that have been added to the pool, but not yet scheduled for execution.
     *
     * @return the number of tasks that have been added to the pool, but not yet scheduled for execution
     */
    @MetricsValue("TaskBacklog")
    @Description("The number of tasks that have been added to the pool, but not yet scheduled for execution")
    int getTaskBacklog();

    /**
     * Returns the maximum number of daemon threads that the pool can create.
     *
     * @return the maximum number of daemon threads that the pool can create
     */
    @MetricsValue("MaxDaemonThreadCount")
    @Description("The maximum number of Daemon threads that could exist")
    int getDaemonCountMax();

    /**
     * Set the maximum daemon pool thread count.
     *
     * @param count the maximum daemon pool thread count
     */
    void setDaemonCountMax(int count);

    /**
     * Returns the minimum number of daemon threads that the pool should have.
     *
     * @return the minimum number of daemon threads that the pool should have
     */
    @MetricsValue("MinDaemonThreadCount")
    @Description("The minimum number of Daemon threads that should exist")
    int getDaemonCountMin();

    /**
     * Set the minimum daemon pool thread count.
     *
     * @param count the minimum daemon pool thread count
     */
    void setDaemonCountMin(int count);

    /**
     * Return the number of Daemon threads that exist, if the pool has been started,
     * or the number of Daemon threads that will be created, if the pool has not yet been started.
     *
     * @return the number of Daemon threads that exist
     */
    @MetricsValue("DaemonThreadCount")
    @Description("The number of Daemon threads that exist")
    int getDaemonCount();

    /**
     * Return the total number of abandoned Daemon threads.
     * <p>
     * Note: this property is purposely not reset when stats are reset.
     *
     * @return the total number of abandoned Daemon threads
     */
    @MetricsValue("AbandonedThreadCount")
    @Description("The total number of abandoned Daemon threads")
    int getAbandonedThreadCount();

    /**
     * Return the total number of milliseconds spent by all Daemon threads while executing
     * tasks since the last time the statistics were reset.
     * <p>
     * Note: this value could be greater then the time elapsed since each daemon adds its
     * own processing time when working in parallel.
     *
     * @return the total number of milliseconds spent by all Daemon threads while executing
     *         tasks since the last time the statistics were reset
     */
    @MetricsValue("TaskActiveMillis")
    @Description("The total number of milliseconds spent by all Daemon threads while executing tasks")
    long getTaskActiveMillis();

    /**
     * Return the total number of currently executing hung tasks.
     * <p>
     * Note: this property is purposely not reset when stats are reset.
     *
     * @return the total number of currently executing hung tasks
     */
    @MetricsValue("TaskHungCount")
    @Description("The total number of currently executing hung tasks")
    int getHungTaskCount();

    /**
     * Return the longest currently executing hung task duration (in milliseconds).
     * <p>
     * Note: this property is purposely not reset when stats are reset.
     *
     * @return the longest currently executing hung task duration (in milliseconds)
     */
    @MetricsValue("TaskHungDuration")
    @Description("The longest currently executing hung task duration (in milliseconds)")
    long getHungTaskDuration();

    /**
     * Return the last time stats were reset.
     *
     * @return the last time stats were reset
     */
    @Description("The last time MBean statistics were reset")
    long getLastResetMillis();

    /**
     * Return the last time the daemon pool was resized.
     *
     * @return the last time the daemon pool was resized
     */
    @Description("The last time the daemon pool was resized")
    long getLastResizeMillis();

    /**
     * Return the total number of tasks added to the pool since the last time the
     * statistics were reset.
     *
     * @return the total number of tasks added to the pool since the last time
     *         the statistics were reset
     */
    @MetricsValue("TaskAddedCount")
    @Description("The total number of tasks added to the pool since the last time the statistics were reset")
    long getTaskAddCount();

    /**
     * Return the total number of tasks executed by Daemon threads since the last
     * time the statistics were reset.
     *
     * @return the total number of tasks executed by Daemon threads since the last
     *         time the statistics were reset
     */
    @MetricsValue("TaskCount")
    @Description("The total number of tasks executed by Daemon threads since"
                 + " the last time the statistics were reset")
    long getTaskCount();

    /**
     * Return the maximum backlog value since the last time the statistics were reset.
     *
     * @return the maximum backlog value since the last time the statistics were reset
     */
    @MetricsValue("TaskMaxBacklogCount")
    @Description("The maximum backlog value since the last time the statistics were reset")
    int getMaxTaskBacklog();

    /**
     * Return the total number of timed-out tasks since the last time the statistics were reset.
     *
     * @return the total number of timed-out tasks since the last time the statistics were reset
     */
    @MetricsValue("TaskTimeoutCount")
    @Description("The total number of timed-out tasks since the last time the statistics were reset")
    int getTaskTimeoutCount();

    /**
     * Return the default timeout value for PriorityTasks that don't explicitly specify the
     * execution timeout value.
     *
     * @return the default timeout value for PriorityTasks that don't explicitly specify the
     *         execution timeout value
     */
    @Description("The default timeout value for PriorityTasks that don't explicitly specify the execution timeout value")
    long getTaskTimeout();
    }
