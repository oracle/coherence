/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;

import com.oracle.coherence.common.base.Hasher;
import com.oracle.coherence.common.net.SelectionService;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SelectionServices;
import com.oracle.coherence.common.net.InetSocketAddressHasher;
import com.oracle.coherence.common.net.exabus.Bus;
import com.oracle.coherence.common.net.exabus.Depot;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.util.UrlEndPoint;
import com.oracle.coherence.common.net.exabus.spi.Driver;
import com.oracle.coherence.common.io.BufferManager;
import com.oracle.coherence.common.io.BufferManagers;
import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.MemorySize;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.ServerSocket;
import java.net.SocketOptions;
import java.net.SocketException;
import java.util.logging.Logger;


/**
 * SocketDriver is a base implementation for socket based busses.
 *
 * @author mf  2010.12.27
 */
public class SocketBusDriver
        implements Driver
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a SocketDriver.
     *
     * @param deps  the driver's dependencies
     */
    public SocketBusDriver(Dependencies deps)
        {
        m_dependencies = copyDependencies(deps).validate();
        }


    // ----- Driver interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public void setDepot(Depot depot)
        {
        m_depot = depot;
        }

    /**
     * {@inheritDoc}
     */
    public Depot getDepot()
        {
        return m_depot;
        }

    /**
     * {@inheritDoc}
     */
    public EndPoint resolveEndPoint(String sName)
        {
        if (sName == null)
            {
            return null;
            }

        Dependencies deps = getDependencies();
        String       sMsg = deps.getMessageBusProtocol();
        String       sMem = deps.getMemoryBusProtocol();
        if (sName.startsWith(sMsg) || sName.startsWith(sMem))
            {
            try
                {
                UrlEndPoint point     = resolveSocketEndPoint(sName);
                String      sProtocol = point.getProtocol();

                if (sProtocol.equals(sMsg) || sProtocol.equals(sMem))
                    {
                    return point;
                    }
                }
            catch (IllegalArgumentException e)
                {
                if (sName.startsWith(sMsg + UrlEndPoint.PROTOCOL_DELIMITER) ||
                    sName.startsWith(sMem + UrlEndPoint.PROTOCOL_DELIMITER))
                    {
                    throw e;
                    }
                }
            }
        return null;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isSupported(EndPoint point)
        {
        if (point == null)
            {
            return true;
            }
        else if (point instanceof UrlEndPoint)
            {
            Dependencies deps      = getDependencies();
            String       sProtocol = ((UrlEndPoint) point).getProtocol();
            return sProtocol.equals(deps.getMessageBusProtocol()) ||
                   sProtocol.equals(deps.getMemoryBusProtocol());
            }
        else
            {
            return false;
            }
        }

    /**
     * {@inheritDoc}
     */
    public Bus createBus(EndPoint pointLocal)
        {
        if (isSupported(pointLocal))
            {
            try
                {
                Dependencies deps        = getDependencies();
                UrlEndPoint  pointSocket = (UrlEndPoint) pointLocal;
                String       sProtocol   = pointSocket.getProtocol();

                if (sProtocol.equals(deps.getMessageBusProtocol()))
                    {
                    return new SocketMessageBus(this, pointSocket);
                    }
                // else; fall through
                }
            catch (IOException e)
                {
                throw new RuntimeException("Error creating SocketBus " +
                        "instance for " + pointLocal, e);
                }
            }

        throw new IllegalArgumentException("unsupported EndPoint " + pointLocal);
        }


    // ----- helpers -------------------------------------------------------

    /**
     * Resolve the supplied canonical name into a SocketEndPoint.
     *
     * @param sName  the endpoint name
     *
     * @return the resolved EndPoint
     *
     * @throws IllegalArgumentException if the name is not a valid SocketEndPoint
     */
    public UrlEndPoint resolveSocketEndPoint(String sName)
        {
        Dependencies deps = getDependencies();
        return new UrlEndPoint(sName, deps.getSocketProvider(),
                deps.getSocketAddressHasher());
        }

    /**
     * Resolve the EndPoint which the specified service socket is bound to.
     *
     * @param pointLocal  the requested bind point
     * @param socket     the bound socket
     *
     * @return  the EndPoint
     */
    public UrlEndPoint resolveBindPoint(UrlEndPoint pointLocal, ServerSocket socket)
        {
        Dependencies   deps     = getDependencies();
        SocketProvider provider = deps.getSocketProvider();
        String         sQuery   = pointLocal.getQueryString();
        return new UrlEndPoint(pointLocal.getProtocol() + UrlEndPoint.PROTOCOL_DELIMITER +
                provider.getAddressString(socket) + (sQuery == null ? "" : "?" + sQuery), provider,
                deps.getSocketAddressHasher());
        }


    /**
     * Return the driver's Dependencies.
     *
     * @return  the driver's Dependencies
     */
    public Dependencies getDependencies()
        {
        return m_dependencies;
        }

    /**
     * Produce a shallow copy of the supplied dependencies.
     *
     * @param deps  the dependencies to copy
     *
     * @return the dependencies
     */
    protected DefaultDependencies copyDependencies(Dependencies deps)
        {
        return new DefaultDependencies(deps);
        }


    // ----- inner interface: Dependencies ----------------------------------

    /**
     * Dependencies provides an interface by which the SocketBusDriver can
     * be provided with its external dependencies.
     */
    public interface Dependencies
        {
        /**
         * Return the MessageBus protocol prefix.
         *
         * @return the MessageBus protocol prefix
         */
        public String getMessageBusProtocol();

        /**
         * Return the MemoryBus protocol prefix.
         *
         * @return the MemoryBus protocol prefix
         */
        public String getMemoryBusProtocol();

        /**
         * Return the SelectionService used to run this driver.
         *
         * @return  the SelectionService.
         */
        public SelectionService getSelectionService();

        /**
         * Return the SocketProvider to use in producing sockets for this
         * driver.
         *
         * @return  the SocketProvider
         */
        public SocketProvider getSocketProvider();

        /**
         * Return the SocketAddress Hasher to use in comparing SocketAddresses.
         *
         * @return  the SocketAddress Hasher
         */
        public Hasher<? super SocketAddress> getSocketAddressHasher();

        /**
         * Return the SocketOptions to utilize in this driver.
         *
         * @return  the SocketOptions
         */
        public SocketOptions getSocketOptions();

        /**
         * Return the BufferManager to use in creating temporary buffers.
         *
         * @return the BufferManager
         */
        public BufferManager getBufferManager();

        /**
         * Return the Logger to use.
         *
         * @return the logger
         */
        public Logger getLogger();

        /**
         * Max time after which the receipt acks will be sent to the peer.
         *
         * @return receipt ack delay in millis
         */
        public long getMaximumReceiptDelayMillis();

        /**
         * Return the number of milliseconds after which a connection is considered to be bad due to a missing
         * acknowledgement and the connection should be reestablished.
         *
         * @return the timeout in milliseconds, or 0 for indefinite
         */
        public long getAckTimeoutMillis();

        /**
         * Return the default value of {@link #getAckTimeoutMillis()}.
         *
         * @return the default timeout in milliseconds
         */
        default public long getDefaultAckTimeoutMillis()
            {
            return getAckTimeoutMillis();
            };

        /**
         * Return the number of milliseconds after which a connection is considered to be unrecoverable due to a missing
         * acknowledgement and the connection an unsolicited DISCONNECT event should be emitted
         *
         * @return the timeout in milliseconds, or 0 for indefinite
         */
        public long getAckFatalTimeoutMillis();

        /**
         * Return the interval between reconnect attempts
         *
         * @return socket reconnect interval in millis
         */
        public long getSocketReconnectDelayMillis();

        /**
         * Return the maximum number sequential reconnect attempts to make
         *
         * @return the reconnect limit
         */
        public int getSocketReconnectLimit();

        /**
         * Return the maximum time a connection may be left idle before
         * the bus automatically injects traffic (hidden to the application)
         * in order to ensure that the underlying network infrastructure
         * does not disconnect the socket due to an idle timeout.
         *
         * Note: This is not SO_KEEPALIVE, this is a higher level construct
         * which ensures the connection is used often enough to not appear
         * idle.  While SO_KEEPALIVE operates similarly there are a few
         * key differences.  First the interval is not controllable by the
         * user.  Second when SO_KEEPALIVE actually encourages termination
         * of connections due to temporary network outages, that is it also
         * serves as a time based disconnect detection.  SO_KEEPALIVE is
         * most often used to try to keep an idel connection from being
         * terminated by the network infrastructure.  This is a misguided use
         * of the feature and often doesn't work as intended, hense this
         * higher level version.
         *
         * @return the idle timeout in milliseconds
         */
        public long getHeartbeatMillis();

        /**
         * Threshold at which a send/signal operation should perform
         * an auto-flush of the unflushed write batch.
         *
         * @return the number of queue'd bytes at which to auto-flush
         */
        public long getAutoFlushThreshold();

        /**
         * Threshold at which to request an immediate receipt from the peer.
         *
         * @return the number of bytes to send before requesting immediate receipts
         */
        public long getReceiptRequestThreshold();

        /**
         * Return the maximum number of threads which should concurrently attempt direct writes.
         * <p>
         * A direct write is a write performed on the calling thread rather then on a background thread. Direct writes
         * tend to reduce latency when contention is low. When contention is high though better throughput and latency
         * may be achieved by allowing writes to be offloaded to background threads.
         * </p>
         *
         * @return the thread count
         */
        public int getDirectWriteThreadThreshold();

        /**
         * For the purposes of testing, this method specifies a percentage of read operations which
         * should result in an underlying connection failure.  Specifically the socket's input and output
         * streams will be shutdown.
         *
         * @return a drop ratio
         */
        public int getDropRatio();

        /**
         * For the purposes of testing, this method specifies a percentage of read operations which
         * should result in a bit flip of the stream. This is to simulate data corruption from wire,  should
         * result in connection migration.
         *
         * @return a corruption ratio
         */
        public int getCorruptionRatio();

        /**
         * Return true if CRC validation is enabled.
         *
         * @return true if CRC validation is enabled
         */
        public boolean isCrcEnabled();
        }


    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * SimpleDependencies provides a basic Dependencies implementation as well
     * as default values where applicable.
     */
    public static class DefaultDependencies
            implements Dependencies
        {
        /**
         * Construct a DefaultDependencies object.
         */
        public DefaultDependencies()
            {
            }

        /**
         * Construct a DefaultDependencies object copying the values from the
         * specified dependencies object
         *
         * @param deps  the dependencies to copy, or null
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                m_provider                 = deps.getSocketProvider();
                m_hasher                   = deps.getSocketAddressHasher();
                m_service                  = deps.getSelectionService();
                m_sProtocolMessageBus      = deps.getMessageBusProtocol();
                m_sProtocolMemoryBus       = deps.getMemoryBusProtocol();
                m_options                  = deps.getSocketOptions();
                m_bufferManager            = deps.getBufferManager();
                m_logger                   = deps.getLogger();
                m_cMaxReceiptDelayMillis   = deps.getMaximumReceiptDelayMillis();
                m_cReconnectLimit          = deps.getSocketReconnectLimit();
                m_cReconnectDelayMillis    = deps.getSocketReconnectDelayMillis();
                m_cHeartbeatDelayMillis    = deps.getHeartbeatMillis();
                m_cAckTimeoutMillis        = deps.getAckTimeoutMillis();
                m_cDefaultAckTimeoutMillis = deps.getDefaultAckTimeoutMillis();
                m_cAckFatalTimeoutMillis   = deps.getAckFatalTimeoutMillis();
                m_cbAutoFlush              = deps.getAutoFlushThreshold();
                m_cbReceiptRequest         = deps.getReceiptRequestThreshold();
                m_cThreadsDirect           = deps.getDirectWriteThreadThreshold();
                m_nDropRatio               = deps.getDropRatio();
                m_nCorruptionRatio         = deps.getCorruptionRatio();
                m_fCrc                     = deps.isCrcEnabled();
                }
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMessageBusProtocol()
            {
            return m_sProtocolMessageBus;
            }

        /**
         * Specify the message bus protcol name
         *
         * @param sProtocol the message bus protocol name
         *
         * @return this object
         */
        public DefaultDependencies setMessageBusProtocol(String sProtocol)
            {
            m_sProtocolMessageBus = sProtocol;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getMemoryBusProtocol()
            {
            return m_sProtocolMemoryBus;
            }

        /**
         * Specify the memory bus protcol name
         *
         * @param sProtocol the memory bus protocol name
         *
         * @return this object
         */
        public DefaultDependencies setMemoryBusProtocol(String sProtocol)
            {
            m_sProtocolMemoryBus = sProtocol;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SelectionService getSelectionService()
            {
            SelectionService svc = m_service;
            return svc == null ? SelectionServices.getDefaultService() : svc;
            }


        /**
         * Specify the SelectionService to be used by this driver.
         *
         * @param service  the SelectionService
         *
         * @return this object
         */
        public DefaultDependencies setSelectionService(SelectionService service)
            {
            m_service = service;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketProvider getSocketProvider()
            {
            return m_provider;
            }

        /**
         * Specify the SocketProvider to use.
         *
         * @param provider  the SocketProvider to use.
         *
         * @return this object
         */
         public DefaultDependencies setSocketProvider(SocketProvider provider)
            {
            m_provider = provider;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Hasher<? super SocketAddress> getSocketAddressHasher()
            {
            Hasher<? super SocketAddress> hasher = m_hasher;
            if (hasher == null)
                {
                return InetSocketAddressHasher.INSTANCE;
                }
            return hasher;
            }

        /**
         * Specify the SocketAddress Hasher to be used in comparing addresses.
         *
         * @param hasher  the hasher
         *
         * @return this object
         */
        public DefaultDependencies setSocketAddressHahser(Hasher<? super SocketAddress> hasher)
            {
            m_hasher = hasher;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketOptions getSocketOptions()
            {
            SocketOptions options = m_options;
            return options == null ? DEFAULT_OPTIONS : options;
            }

        /**
         * Specify the SocketOptions to use.
         *
         * @param options  the options
         *
         * @return this object
         */
        public DefaultDependencies setSocketOptions(SocketOptions options)
            {
            m_options = options;
            return this;
            }


        /**
         * {@inheritDoc}
         */
        @Override
        public BufferManager getBufferManager()
            {
            BufferManager manager = m_bufferManager;
            return manager == null ? DEFAULT_BUFFER_MANAGER : manager;
            }

        /**
         * Specify the BufferManager to be used by this driver.
         *
         * @param manager  the buffer manager
         *
         * @return this object
         */
        public DefaultDependencies setBufferManager(BufferManager manager)
            {
            m_bufferManager = manager;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Logger getLogger()
            {
            Logger logger = m_logger;
            return logger == null ? LOGGER : logger;
            }

        /**
         * Specify the Logger to use.
         *
         * @param logger  the logger
         *
         * @return this object
         */
        public DefaultDependencies setLogger(Logger logger)
            {
            m_logger = logger;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getMaximumReceiptDelayMillis()
            {
            return m_cMaxReceiptDelayMillis;
            }

        /**
         * Set maximum receipt ack delay
         *
         * @param cDelayMillis Max receipt ack delay in millis
         *
         * @return this object
         */
        public DefaultDependencies setMaximumReceiptDelayMillis(long cDelayMillis)
            {
            m_cMaxReceiptDelayMillis = cDelayMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getAckTimeoutMillis()
            {
            return m_cAckTimeoutMillis;
            }

        /**
         * Set ack timeout
         *
         * @param cAckTimeoutMillis ack timeout, or 0 for indefinite
         *
         * @return this object
         */
        public DefaultDependencies setAckTimeoutMillis(long cAckTimeoutMillis)
            {
            m_cAckTimeoutMillis = cAckTimeoutMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getAckFatalTimeoutMillis()
            {
            return m_cAckFatalTimeoutMillis;
            }

        /**
         * Set ack timeout
         *
         * @param cAckFatalTimeoutMillis ack timeout, or 0 for indefinite
         *
         * @return this object
         */
        public DefaultDependencies setAckFatalTimeoutMillis(long cAckFatalTimeoutMillis)
            {
            m_cAckFatalTimeoutMillis = cAckFatalTimeoutMillis;
            return this;
            }

        /**
         * Return default value of ack timeout
         *
         * @return default value of ack timeout
         */
        public long getDefaultAckTimeoutMillis()
            {
            return m_cDefaultAckTimeoutMillis;
            }

        /**
         * Set default ack timeout
         *
         * @param cAckTimeoutMillis  default ack timeout
         *
         * @return this object
         */
        public DefaultDependencies setDefaultAckTimeoutMillis(long cAckTimeoutMillis)
            {
            m_cDefaultAckTimeoutMillis = cAckTimeoutMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getSocketReconnectDelayMillis()
            {
            return m_cReconnectDelayMillis;
            }

        /**
         * Set reconnect interval for the Connection
         *
         * @param cDelayMillis reconnect delay in millis
         *
         * @return this object
         */
        public DefaultDependencies setSocketReconnectDelayMillis(long cDelayMillis)
            {
            m_cReconnectDelayMillis = cDelayMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int getSocketReconnectLimit()
            {
            return m_cReconnectLimit;
            }

        /**
         * Set the reconnect limit
         *
         * @param cReconnectLimit the reconnect limit
         *
         * @return this object
         */
        public DefaultDependencies setSocketReconnectLimit(int cReconnectLimit)
            {
            m_cReconnectLimit = cReconnectLimit;
            return this;
            }

        @Override
        public long getHeartbeatMillis()
            {
            return m_cHeartbeatDelayMillis;
            }

        /**
         * Set the heartbeat interface for the connection.
         *
         * @param cMillis  the heartbeat interval
         *
         * @return this object
         */
        public DefaultDependencies setHeartbeatMillis(long cMillis)
            {
            m_cHeartbeatDelayMillis = cMillis;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getAutoFlushThreshold()
            {
            return m_cbAutoFlush;
            }

        /**
         * Set threshold for auto flush
         *
         * @param cbThreshold auto flush threshold
         *
         * @return this object
         */
        public DefaultDependencies setAutoFlushThreshold(long cbThreshold)
            {
            m_cbAutoFlush = cbThreshold;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getReceiptRequestThreshold()
            {
            return m_cbReceiptRequest;
            }

        /**
         * Set threshold for receipt requests
         *
         * @param cbRequest receipt request threshold
         *
         * @return this object
         */
        public DefaultDependencies setReceiptRequestThreshold(long cbRequest)
            {
            m_cbReceiptRequest = cbRequest;
            return this;
            }

        @Override
        public int getDirectWriteThreadThreshold()
            {
            return m_cThreadsDirect;
            }

        /**
         * Specify the direct write threshold.
         *
         * @param cThreads  the number of threads
         *
         * @return this object
         */
        public DefaultDependencies setDirectWriteThreadThreshold(int cThreads)
            {
            m_cThreadsDirect = cThreads;
            return this;
            }

        @Override
        public int getDropRatio()
            {
            return m_nDropRatio;
            }

        /**
         * Specify the drop rate.
         *
         * @param nDropRatio  the ration to drop, i.e. 1:nDropRatio
         *
         * @return this object
         */
        public DefaultDependencies setDropRatio(int nDropRatio)
            {
            m_nDropRatio = nDropRatio;
            return this;
            }

        @Override
        public int getCorruptionRatio()
            {
            return m_nCorruptionRatio;
            }

        /**
         * Specify the drop rate.
         *
         * @param nCorruptionRatio  the ration to drop, i.e. 1:nCorruptionRatio
         *
         * @return this object
         */
        public DefaultDependencies setCorruptionRatio(int nCorruptionRatio)
            {
            m_nCorruptionRatio = nCorruptionRatio;
            return this;
            }

        @Override
        public boolean isCrcEnabled()
            {
            return m_fCrc;
            }

        /**
         * Specify CRC validation should be enabled.
         *
         * @param fCrc  true if CRC is enabled
         *
         * @return this object
         */
        public DefaultDependencies isCrcEnabled(boolean fCrc)
            {
            m_fCrc = fCrc;
            return this;
            }


        // ----- helpers ------------------------------------------------

        /**
         * Validate the supplied dependencies.
         *
         * @throws IllegalArgumentException if the dependencies are not valid
         *
         * @return this object
         */
        protected DefaultDependencies validate()
            {
            ensureArgument(getMemoryBusProtocol(),   "MemoryBusProtocol");
            ensureArgument(getMessageBusProtocol(),  "MessageBusProtocol");
            ensureArgument(getSelectionService(),    "SelectionService");
            ensureArgument(getSocketAddressHasher(), "SocketAddressHasher");
            ensureArgument(getSocketProvider(),      "SocketProvider");

            if (getMemoryBusProtocol().equals(getMessageBusProtocol()))
                {
                throw new IllegalArgumentException(
                        "memory and mess bus protocols cannot use the sane names");
                }

            m_cAckTimeoutMillis = new Duration(System.getProperty( SocketBusDriver.class.getName()+".ackTimeoutMillis",
                    getDefaultAckTimeoutMillis() + "ms")).getNanos()/1000000;

            return this;
            }

        /**
         * Ensure that the specified object is non-null
         *
         * @param o      the object to ensure
         * @param sName  the name of the corresponding parameter
         *
         * @throws IllegalArgumentException if o is null
         */
        protected static void ensureArgument(Object o, String sName)
            {
            if (o == null)
                {
                throw new IllegalArgumentException(sName + " cannot be null");
                }
            }


        // ----- data members -------------------------------------------

        /**
         * The SocketProvider to use when producing sockets.
         */
        protected SocketProvider m_provider;

        /**
         * The SocketAddress hasher.
         */
        protected Hasher<? super SocketAddress> m_hasher;

        /**
         * The SelectionService the busses will use for IO processing.
         */
        protected SelectionService m_service;

        /**
         * The message bus protocol prefix.
         */
        protected String m_sProtocolMessageBus;

        /**
         * The message bus protocol prefix.
         */
        protected String m_sProtocolMemoryBus;

        /**
         * The SocketOptions.
         */
        protected SocketOptions m_options;

        /**
         * The BufferManager.
         */
        protected BufferManager m_bufferManager;

        /**
         * The Logger.
         */
        protected Logger m_logger;

        /**
         * Reconnect interval for the Connection in millis
         */
        protected long m_cReconnectDelayMillis = new Duration(System.getProperty(
                SocketBusDriver.class.getName()+".reconnectDelayMillis", "200ms")).getNanos()/1000000;

        /**
         * The maximum number of sequential reconnects to attempt.
         * <p>
         * A value of -1 indicates that no reconnects should be attempted.
         */
        protected int m_cReconnectLimit = Integer.parseInt(System.getProperty(
                SocketBusDriver.class.getName()+".reconnectLimit", "3"));

        /**
         * Maximum receipt ack delay in millis
         */
        protected long m_cMaxReceiptDelayMillis = new Duration(System.getProperty(
                SocketBusDriver.class.getName()+".maxReceiptDelayMillis", "500ms")).getNanos()/1000000;

        /**
         * Ack timeout in millis
         */
        protected long m_cAckTimeoutMillis;

        /**
         * Default ack timeout in millis
         */
        protected long m_cDefaultAckTimeoutMillis = 10_000L;


        /**
         * Fatal ack timeout in millis
         */
        protected long m_cAckFatalTimeoutMillis = new Duration(System.getProperty(
                SocketBusDriver.class.getName()+".fatalTimeoutMillis", "10m")).getNanos()/1000000;

        /**
         * Heartbeat interval in millis, disabled by default now that we support reconnects
         */
        protected long m_cHeartbeatDelayMillis = new Duration(System.getProperty(
                SocketBusDriver.class.getName()+".heartbeatInterval", "0s")).getNanos()/1000000;

        /**
         * Auto flush threshold
         */
        protected long m_cbAutoFlush = getSafeMemorySize(System.getProperty(
                SocketBusDriver.class.getName()+".autoFlushThreshold"));

        /**
         * Threshold after which to request receipts.
         */
        protected long m_cbReceiptRequest = getSafeMemorySize(System.getProperty(
                SocketBusDriver.class.getName()+".receiptRequestThreshold"));

        /**
         * The maximum number of concurrent writers on which to attempt direct writes.
         */
        protected int m_cThreadsDirect = Integer.parseInt(System.getProperty(
                SocketBusDriver.class.getName() + ".directWriteThreadThreshold", "4"));

        /**
         * The drop ratio.
         */
        protected int m_nDropRatio = Integer.parseInt(System.getProperty(
                        SocketBusDriver.class.getName() + ".dropRatio", "0"));

        /**
         * The force corruption ratio.
         */
        protected int m_nCorruptionRatio = Integer.parseInt(System.getProperty(
                SocketBusDriver.class.getName() + ".corruptionRatio", "0"));

        /**
         * True iff CRC validation is enabled
         */
        protected boolean m_fCrc = Boolean.parseBoolean(System.getProperty(
                SocketBusDriver.class.getName() + ".crc", "false"));


        private static long getSafeMemorySize(String sValue)
            {
            if (sValue == null)
                {
                return -1;
                }
            return new MemorySize(sValue).getByteCount();
            }

        // ----- constants ----------------------------------------------

        /**
         * Default BufferManager.
         */
        public static final BufferManager DEFAULT_BUFFER_MANAGER;

        static
            {
            String sManager = System.getProperty(SocketBusDriver.class.getName() + ".bufferManager", "network");
            switch (sManager)
                {
                case "heap":
                    DEFAULT_BUFFER_MANAGER = BufferManagers.getHeapManager();
                    break;

                case "direct":
                    DEFAULT_BUFFER_MANAGER = BufferManagers.getDirectManager();
                    break;

                case "network":
                    DEFAULT_BUFFER_MANAGER = BufferManagers.getNetworkDirectManager();
                    break;

                default:
                    throw new IllegalArgumentException("unknown BufferManager: " + sManager);
                }
            }

        /**
         * Default SocketOptions.
         */
        public static final SocketOptions DEFAULT_OPTIONS = new SocketOptions()
            {
            @Override
            public void setOption(int optID, Object value)
                    throws SocketException
                {
                throw new UnsupportedOperationException();
                }

            @Override
            public Object getOption(int optID)
                    throws SocketException
                {
                switch (optID)
                    {
                    case TCP_NODELAY:
                        return true;

                    case SO_LINGER:
                        return 0;

                    case SO_RCVBUF:
                        return RX_BUFFER_SIZE == -1 ? null : RX_BUFFER_SIZE;

                    case SO_SNDBUF:
                        return TX_BUFFER_SIZE == -1 ? null : TX_BUFFER_SIZE;

                    default:
                        return null;
                    }
                }

            final int RX_BUFFER_SIZE = (int) getSafeMemorySize(System.getProperty(
                                        SocketBusDriver.class.getName()+".socketRxBuffer"));
            final int TX_BUFFER_SIZE = (int) getSafeMemorySize(System.getProperty(
                                        SocketBusDriver.class.getName()+".socketTxBuffer"));
            };
        }


    // ----- constants ------------------------------------------------------

    /**
     * The default Logger for the driver.
     */
    private static Logger LOGGER = Logger.getLogger(SocketBusDriver.class.getName());


    // ----- data members ---------------------------------------------------

    /**
     * The Depot managing this driver.
     */
    protected Depot m_depot;

    /**
     * The driver's dependencies.
     */
    protected Dependencies m_dependencies;
    }
