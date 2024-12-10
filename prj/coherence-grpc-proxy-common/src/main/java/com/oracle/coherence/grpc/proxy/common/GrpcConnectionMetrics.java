/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.grpc.proxy.common;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.coherence.config.Config;
import com.tangosol.internal.net.metrics.Meter;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.management.AnnotatedStandardMBean;
import com.tangosol.net.management.Registry;
import com.tangosol.net.management.annotation.Description;
import com.tangosol.net.management.annotation.MetricsLabels;
import com.tangosol.net.management.annotation.MetricsScope;
import com.tangosol.net.management.annotation.MetricsTag;
import com.tangosol.net.management.annotation.MetricsValue;
import com.tangosol.net.metrics.MBeanMetric;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.UID;
import io.grpc.Grpc;
import io.grpc.ServerCall;

import javax.management.NotCompliantMBeanException;
import java.net.SocketAddress;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A holder for gRPC connection metrics.
 * <p>
 * This is a singleton so that one instance can track all connections in a process.
 * <p>
 * Because there is no way to actually track gRPC connections this holder tracks
 * them using the remote address in the request header, so each unique address is
 * a connection.
 * <p>
 * As there is no tracking of connection closed the connection metrics will expire
 * and be removed after a period of inactivity. The default is five minutes, but this
 * may be changed using the {@link #PROP_CONNECTION_TTL} system property.
 *
 * @author Jonathan Knight  2020.10.16
 */
public class GrpcConnectionMetrics
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Private constructor because we want this class to be a singleton.
     */
    @SuppressWarnings("unchecked")
    private GrpcConnectionMetrics()
        {
        Duration   ttl   = Config.getDuration(PROP_CONNECTION_TTL, DEFAULT_CONNECTION_TTL);
        LocalCache cache = new LocalCache(LocalCache.DEFAULT_UNITS, (int) ttl.as(Duration.Magnitude.MILLI));

        cache.addMapListener(new Listener());

        f_cacheConnections = cache;
        m_registry         = () -> CacheFactory.getCluster().getManagement();
        }

    // ----- GrpcConnectionMetrics methods ----------------------------------

    /**
     * Returns the singleton instance of {@link GrpcConnectionMetrics}.
     *
     * @return  the singleton instance of {@link GrpcConnectionMetrics}
     */
    public static GrpcConnectionMetrics getInstance()
        {
        return SingletonHolder.INSTANCE;
        }

    /**
     * Register a {@link ServerCall} with the connection metrics, creating a
     * new connection metric if this call is from a new remote address.
     *
     * @param call  the {@link ServerCall} to register
     */
    public void register(ServerCall<?, ?> call)
        {
        SocketAddress address = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (address != null)
            {
            ConnectionMetrics metrics = f_cacheConnections.computeIfAbsent(address, k -> new ConnectionMetrics(address));
            metrics.mark();
            }
        }

    /**
     * Set the {@link Registry} to use to register connection metric MBeans.
     *
     * @param registry  the {@link Registry} to use to register connection metric MBeans
     */
    void setRegistry(Supplier<Registry> registry)
        {
        if (registry != null)
            {
            m_registry = registry;
            }
        }

    /**
     * Returns the number of connections currently being tracked.
     *
     * @return  the number of connections currently being tracked
     */
    int getConnectionCount()
        {
        return f_cacheConnections.size();
        }

    // ----- inner interface: Listener --------------------------------------

    /**
     * A {@link MapListener} that registers and removes connection MBeans
     * as they are added to or evicted from the connection cache.
     */
    private class Listener
            implements MapListener<SocketAddress, ConnectionMetrics>
        {
        // ----- MapListener methods --------------------------------------------

        @Override
        public void entryInserted(MapEvent<SocketAddress, ConnectionMetrics> evt)
            {
            try
                {
                ConnectionMetrics mBean    = evt.getNewValue();
                Registry          registry = m_registry.get();
                String            sName    = registry.ensureGlobalName(MBEAN_PREFIX + mBean.getUID());
                if (registry.isRegistered(sName))
                    {
                    registry.unregister(sName);
                    }
                registry.register(sName, new AnnotatedStandardMBean(mBean, ConnectionMetricsMBean.class));
                }
            catch (NotCompliantMBeanException e)
                {
                Logger.err(e);
                }
            }

        @Override
        public void entryUpdated(MapEvent<SocketAddress, ConnectionMetrics> evt)
            {
            }

        @Override
        public void entryDeleted(MapEvent<SocketAddress, ConnectionMetrics> evt)
            {
            ConnectionMetrics mBean    = evt.getOldValue();
            Registry          registry = m_registry.get();
            String            sName    = registry.ensureGlobalName("type=GrpcConnection,uid=" + mBean.getUID());
            if (registry.isRegistered(sName))
                {
                registry.unregister(sName);
                }
            }
        }

    // ----- inner interface: ConnectionMetricsMBean ------------------------

    /**
     * A MBean to track gRPC connections.
     */
    @MetricsScope(MBeanMetric.Scope.VENDOR)
    public interface ConnectionMetricsMBean
        {
        /**
         * Returns the UID for the connection.
         *
         * @return  the UID for the connection
         */
        @Description("The UID of the connection")
        String getUID();

        /**
         * Returns the remote address of the connection.
         *
         * @return  the remote address of the connection
         */
        @MetricsTag("RemoteAddress")
        @Description("The remote address of the connection")
        String getAddress();

        /**
         * Returns the time that the connection was opened.
         *
         * @return  the time that the connection was opened
         */
        @Description("The time that the connection was opened")
        Date getTimestamp();

        /**
         * Returns the number of requests made by this connection.
         *
         * @return  the number of requests made by this connection
         */
        @MetricsValue("RequestCount")
        @Description("The number of requests made by this connection")
        long getRequestCount();

        /**
         * Returns the mean rate of requests made by this connection.
         *
         * @return  the mean rate of requests made by this connection
         */
        @MetricsValue("RequestRate")
        @MetricsLabels({"quantile", "mean"})
        @Description("The mean rate of requests made by this connection")
        double getRequestCountMeanRate();

        /**
         * Returns the one minute rate of requests made by this connection.
         *
         * @return  the one minute rate of requests made by this connection
         */
        @MetricsValue("RequestRate")
        @MetricsLabels({"quantile", "1min"})
        @Description("The 1 minute rate of requests made by this connection")
        double getRequestCountOneMinuteRate();

        /**
         * Returns the five minute rate of requests made by this connection.
         *
         * @return  the five minute rate of requests made by this connection
         */
        @MetricsValue("RequestRate")
        @MetricsLabels({"quantile", "5min"})
        @Description("The 5 minute rate of requests made by this connection")
        double getRequestCountFiveMinuteRate();

        /**
         * Returns the fifteen minute rate of requests made by this connection.
         *
         * @return  the fifteen minute rate of requests made by this connection
         */
        @MetricsValue("RequestRate")
        @MetricsLabels({"quantile", "15min"})
        @Description("The 15 minute rate of requests made by this connection")
        double getRequestCountFifteenMinuteRate();
        }

    // ----- inner class: ConnectionMetrics ---------------------------------

    /**
     * A MBean implementation to track gRPC connections.
     */
    static class ConnectionMetrics
            implements ConnectionMetricsMBean
        {
        // ----- constructors -----------------------------------------------

        ConnectionMetrics(SocketAddress address)
            {
            f_address   = address;
            f_sUid      = new UID().toString();
            f_timestamp = new Date();
            f_meter     = new Meter();
            }

        // ----- ConnectionMetricsMBean methods -----------------------------

        @Override
        public String getAddress()
            {
            return f_address.toString();
            }

        @Override
        public String getUID()
            {
            return f_sUid;
            }

        @Override
        public Date getTimestamp()
            {
            return f_timestamp;
            }

        @Override
        public long getRequestCount()
            {
            return f_meter.getCount();
            }

        @Override
        public double getRequestCountMeanRate()
            {
            return f_meter.getOneMinuteRate();
            }

        @Override
        public double getRequestCountOneMinuteRate()
            {
            return f_meter.getOneMinuteRate();
            }

        @Override
        public double getRequestCountFiveMinuteRate()
            {
            return f_meter.getFiveMinuteRate();
            }

        @Override
        public double getRequestCountFifteenMinuteRate()
            {
            return f_meter.getFifteenMinuteRate();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Increment the request count.
         */
        void mark()
            {
            f_meter.mark();
            }

        // ----- data members -----------------------------------------------

        /**
         * The UID for the connection.
         */
        private final String f_sUid;

        /**
         * The remote address for the connection.
         */
        private final SocketAddress f_address;

        /**
         * The time that the connection was opened.
         */
        private final Date f_timestamp;

        /**
         * The {@link Meter} to track requests made by this connection.
         */
        private final Meter f_meter;
        }

    // ----- inner class SingletonHolder ------------------------------------

    /**
     * A holder for the singleton {@link GrpcConnectionMetrics} instance.
     */
    private static class SingletonHolder
        {
        /**
         * The singleton {@link GrpcConnectionMetrics} instance.
         */
        private static final GrpcConnectionMetrics INSTANCE = new GrpcConnectionMetrics();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The system property used to set the expiry time used to evict connection MBeans
     * when connections have had no activity for a period of time.
     */
    public static final String PROP_CONNECTION_TTL = "coherence.grpc.metric.connection.ttl";

    /**
     * The default connection MBean expiry time.
     */
    private static final Duration DEFAULT_CONNECTION_TTL = new Duration(5, Duration.Magnitude.MINUTE);

    /**
     * The connection MBean type.
     */
    public static final String MBEAN_TYPE = "type=GrpcConnection";

    /**
     * The connection MBean ObjectName prefix.
     */
    public static final String MBEAN_PREFIX = MBEAN_TYPE + ",uid=";

    // ----- data members ---------------------------------------------------

    /**
     * The map of connections.
     * <p>
     * Connections will expire from the map if there has been no activity for
     * a while. We cannot actually track connection close.
     */
    private final Map<SocketAddress, ConnectionMetrics> f_cacheConnections;

    /**
     * The {@link Supplier} that supplies a {@link Registry} to use to register MBeans.
     */
    private Supplier<Registry> m_registry;
    }
