/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.proxy;

import com.oracle.coherence.grpc.proxy.common.DaemonPoolExecutor;
import com.oracle.coherence.grpc.proxy.common.GrpcProxyMetrics;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jonathan Knight  2020.10.15
 */
public class GrpcProxyMetricsTest
    {
    @Test
    public void shouldMarkSuccess()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.markSuccess();

        assertThat(metrics.getSuccessfulRequestCount(), is(1L));

        assertThat(metrics.getErrorRequestCount(), is(0L));
        assertThat(metrics.getResponsesSentCount(), is(0L));
        assertThat(metrics.getMessagesReceivedCount(), is(0L));
        }

    @Test
    public void shouldMarkError()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.markError();

        assertThat(metrics.getErrorRequestCount(), is(1L));

        assertThat(metrics.getSuccessfulRequestCount(), is(0L));
        assertThat(metrics.getResponsesSentCount(), is(0L));
        assertThat(metrics.getMessagesReceivedCount(), is(0L));
        }

    @Test
    public void shouldMarkSent()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.markSent();

        assertThat(metrics.getResponsesSentCount(), is(1L));

        assertThat(metrics.getErrorRequestCount(), is(0L));
        assertThat(metrics.getSuccessfulRequestCount(), is(0L));
        assertThat(metrics.getMessagesReceivedCount(), is(0L));
        }

    @Test
    public void shouldMarkReceived()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.markReceived();

        assertThat(metrics.getMessagesReceivedCount(), is(1L));

        assertThat(metrics.getErrorRequestCount(), is(0L));
        assertThat(metrics.getSuccessfulRequestCount(), is(0L));
        assertThat(metrics.getResponsesSentCount(), is(0L));
        }

    @Test
    public void shouldGetCorrectRequestDurationInMillis()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.addRequestDuration(TimeUnit.MILLISECONDS.toNanos(19L));

        assertThat(metrics.getRequestDurationMax(), is(19.0d));
        assertThat(metrics.getRequestDurationMin(), is(19.0d));
        assertThat(metrics.getRequestDurationMean(), is(19.0d));
        assertThat(metrics.getRequestDuration75thPercentile(), is(19.0d));
        assertThat(metrics.getRequestDuration95thPercentile(), is(19.0d));
        assertThat(metrics.getRequestDuration98thPercentile(), is(19.0d));
        assertThat(metrics.getRequestDuration99thPercentile(), is(19.0d));
        assertThat(metrics.getRequestDuration999thPercentile(), is(19.0d));
        }

    @Test
    public void shouldGetCorrectMessageDurationInMillis()
        {
        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", null);
        metrics.addMessageDuration(TimeUnit.MILLISECONDS.toNanos(19L));

        assertThat(metrics.getMessageDurationMax(), is(19.0d));
        assertThat(metrics.getMessageDurationMin(), is(19.0d));
        assertThat(metrics.getMessageDurationMean(), is(19.0d));
        assertThat(metrics.getMessageDuration75thPercentile(), is(19.0d));
        assertThat(metrics.getMessageDuration95thPercentile(), is(19.0d));
        assertThat(metrics.getMessageDuration98thPercentile(), is(19.0d));
        assertThat(metrics.getMessageDuration99thPercentile(), is(19.0d));
        assertThat(metrics.getMessageDuration999thPercentile(), is(19.0d));
        }

    @Test
    public void shouldGetThreadCounts()
        {
        DaemonPoolExecutor.DaemonPoolManagement pool = mock(DaemonPoolExecutor.DaemonPoolManagement.class);
        when(pool.getDaemonCountMax()).thenReturn(19);
        when(pool.getDaemonCountMin()).thenReturn(1);
        when(pool.getDaemonCount()).thenReturn(5);

        GrpcProxyMetrics metrics = new GrpcProxyMetrics("", pool);

        assertThat(metrics.getDaemonCountMax(), is(19));
        assertThat(metrics.getDaemonCountMin(), is(1));
        assertThat(metrics.getDaemonCount(), is(5));
        }
    }
