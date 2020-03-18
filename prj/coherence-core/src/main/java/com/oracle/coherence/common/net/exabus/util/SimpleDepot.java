/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus.util;


import com.oracle.coherence.common.net.SdpSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;
import com.oracle.coherence.common.net.exabus.Bus;
import com.oracle.coherence.common.net.exabus.Depot;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.MessageBus;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Factory;
import com.oracle.coherence.common.internal.Platform;
import com.oracle.coherence.common.net.exabus.spi.Driver;
import com.oracle.coherence.common.net.exabus.MemoryBus;
import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.internal.net.socketbus.SocketBusDriver;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;


/**
 * SimpleDepot is a Depot of well-known bus drivers.
 * <p>
 * While users may create their own depot instances, it is generally preferable to use the shared
 * {@link #getInstance() singleton} instances.
 * </p>
 *
 * @author mf 2010.12.03
 */
public class SimpleDepot
        implements Depot
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct the Depot with default dependencies.
     */
    public SimpleDepot()
        {
        this (null);
        }

    /**
     * Construct the Depot from the specified dependencies.
     *
     * @param deps  the depot's dependencies
     */
    public SimpleDepot(Dependencies deps)
        {
        m_dependencies = deps = copyDependencies(deps).validate();

        for (Driver driver : deps.getDrivers().values())
            {
            driver.setDepot(this);
            }
        }


    // ----- Singleton accessor ---------------------------------------------

    /**
     * Return a singleton instance of the SimpleDepot.
     *
     * @return the singleton SimpleDepot instance.
     */
    public static SimpleDepot getInstance()
        {
        return SingletonHolder.INSTANCE;
        }

    /**
     * Helper class to delay the initialization of the SimpleDepot singleton.
     */
    private static class SingletonHolder
        {
        public static final SimpleDepot INSTANCE = new SimpleDepot();
        }


    // ----- Depot interface ------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public EndPoint resolveEndPoint(String sName)
        {
        for (Driver driver : getDependencies().getDrivers().values())
            {
            EndPoint ep = driver.resolveEndPoint(sName);
            if (ep != null)
                {
                return ep;
                }
            }
        throw new IllegalArgumentException("unresolvable endpoint " + sName +
                "; no supporting driver registered");
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageBus createMessageBus(EndPoint pointLocal)
        {
        if (pointLocal == null)
            {
            pointLocal = getDefaultMessageBusEndPoint();
            }

        for (Driver driver : getDependencies().getDrivers().values())
            {
            if (driver.isSupported(pointLocal))
                {
                Bus bus = driver.createBus(pointLocal);
                if (bus instanceof MessageBus)
                    {
                    return (MessageBus) bus;
                    }
                new BusCloser(bus).close();
                throw new IllegalArgumentException(pointLocal +
                        " does not describe a MessageBus");
                }
            }

        throw new IllegalArgumentException(pointLocal +
                " is not creatable; no supporting driver registered");
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public MemoryBus createMemoryBus(EndPoint pointLocal)
        {
        if (pointLocal == null)
            {
            pointLocal = getDefaultMemoryBusEndPoint();
            }

        for (Driver driver : getDependencies().getDrivers().values())
            {
            if (driver.isSupported(pointLocal))
                {
                Bus bus = driver.createBus(pointLocal);
                if (bus instanceof MemoryBus)
                    {
                    return (MemoryBus) bus;
                    }
                new BusCloser(bus).close();
                throw new IllegalArgumentException(pointLocal +
                        " does not describe a MemoryBus");
                }
            }

        throw new IllegalArgumentException(pointLocal +
                " is not creatable; no supporting driver registered");
        }


    // ----- helpers --------------------------------------------------------

    /**
     * BusCloser is a collector which closes a bus as soon as it is opened.
     */
    protected class BusCloser
        implements Collector<Event>, Closeable
        {
        BusCloser(Bus bus)
            {
            m_bus = bus;
            bus.setEventCollector(this);
            bus.open();
            }

        @Override
        public void add(Event event)
            {
            switch (event.getType())
                {
                case OPEN:
                    m_bus.close();
                    break;

                case CLOSE:
                    synchronized (this)
                        {
                        m_bus = null;
                        notify();
                        }
                }
            }

        @Override
        public void flush() {}

        @Override
        public void close()
            {
            boolean fInterrupted = false;
            synchronized (this)
                {
                while (m_bus != null)
                    {
                    try
                        {
                        Blocking.wait(this);
                        }
                    catch (InterruptedException e)
                        {
                        fInterrupted = true;
                        }
                    }
                }

            if (fInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }

        private Bus m_bus;
        }

    /**
     * Return the default local MessageBus EndPoint.
     *
     * @return the default local MessageBus EndPoint
     */
    protected EndPoint getDefaultMessageBusEndPoint()
        {
        return resolveEndPoint(getDependencies().getDefaultMessageBusEndPoint());
        }

    /**
     * Return the default local MemoryBus EndPoint.
     *
     * @return the default local MemoryBus EndPoint
     */
    protected EndPoint getDefaultMemoryBusEndPoint()
        {
        return resolveEndPoint(getDependencies().getDefaultMemoryBusEndPoint());
        }

    /**
     * Register a Driver with the Depot.
     *
     * @param driver  the driver to register
     */
    protected void registerDriver(Driver driver)
        {
        if (driver == null)
            {
            throw new IllegalArgumentException("driver cannot be null");
            }

        driver.setDepot(this);
        }


    /**
     * Return the Depot's dependencies.
     *
     * @return the dependencies
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


    // ----- interface: Dependencies ----------------------------------------

    /**
     * Dependencies specifies all dependency requirements of the SimpleDepot.
     */
    public interface Dependencies
        {
        /**
         * Return the default MessageBus EndPoint name.
         *
         * @return the default MessageBus EndPoint name
         */
        public String getDefaultMessageBusEndPoint();

        /**
         * Specify the default MessageBus EndPoint name.
         *
         * @param sEp  the EndPoint name
         *
         * @return this object
         */
        public Dependencies setDefaultMessageBusEndPoint(String sEp);

        /**
         * Return the default MessageBus EndPoint name.
         *
         * @return the default MessageBus EndPoint name
         */
        public String getDefaultMemoryBusEndPoint();

        /**
         * Specify the default MemoryBus EndPoint name.
         *
         * @param sEp  the EndPoint name
         *
         * @return this object
         */
        public Dependencies setDefaultMemoryBusEndPoint(String sEp);

        /**
         * Return the Drivers to use in this Depot.
         *
         * @return the drivers to use
         */
        public Map<String, Driver> getDrivers();

        /**
         * Specify the set of Drivers to use in this Depot, keyed by a
         * descriptive name.
         *
         * @param mapDriver  the drivers to utilize
         *
         * @return this object
         */
        public Dependencies setDrivers(Map<String, Driver> mapDriver);

        /**
         * Return the Logger to use.
         *
         * @return the logger
         */
        public Logger getLogger();

        /**
         * Return the SSLSettings.
         *
         * @return the SSLSettings
         */
        public SSLSettings getSSLSettings();
        }


    // ----- inner class: DefaultDependencies -------------------------------

    /**
     * DefaultDepenencies provides a default implmentation of the Depot's
     * depencies.
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
         * specified dependencies object.
         *
         * @param deps  the dependencies to copy, or null
         */
        public DefaultDependencies(Dependencies deps)
            {
            if (deps != null)
                {
                m_sDefaultMsgBusEndPoint = deps.getDefaultMessageBusEndPoint();
                m_sDefaultMemBusEndPoint = deps.getDefaultMemoryBusEndPoint();
                m_mapDriver = deps.getDrivers();
                m_logger = deps.getLogger();
                }
            }


        // ----- Dependencies interface -------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDefaultMessageBusEndPoint()
            {
            String sEp = m_sDefaultMsgBusEndPoint;
            return sEp == null ? "tmb://0.0.0.0:-1" : sEp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public DefaultDependencies setDefaultMessageBusEndPoint(String sEp)
            {
            m_sDefaultMsgBusEndPoint = sEp;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getDefaultMemoryBusEndPoint()
            {
            String sEp = m_sDefaultMemBusEndPoint;
            return sEp == null ? "trb://0.0.0.0:-1" : sEp;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public DefaultDependencies setDefaultMemoryBusEndPoint(String sEp)
            {
            m_sDefaultMemBusEndPoint = sEp;
            return this;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Map<String, Driver> getDrivers()
            {
            Map<String, Driver> mapDriver = m_mapDriver;
            if (mapDriver == null)
                {
                mapDriver = new HashMap<String, Driver>();

                mapDriver.put(TCP_SOCKET_BUS, new SocketBusDriver(
                        new SocketBusDriver.DefaultDependencies()
                                .setMessageBusProtocol(TCP_MESSAGE_BUS_PROTOCOL)
                            .setMemoryBusProtocol(TCP_MEMORY_BUS_PROTOCOL)
                            .setSocketProvider(TcpSocketProvider.MULTIPLEXED)));
                mapDriver.put(SDP_SOCKET_BUS, new SocketBusDriver(
                        new SocketBusDriver.DefaultDependencies()
                            .setMessageBusProtocol(SDP_MESSAGE_BUS_PROTOCOL)
                            .setMemoryBusProtocol(SDP_MEMORY_BUS_PROTOCOL)
                            .setSocketProvider(SdpSocketProvider.MULTIPLEXED)));

                SSLSettings settingsSSL = m_settingsSSL;
                if (settingsSSL != null)
                    {
                    mapDriver.put(TCP_SECURE_SOCKET_BUS, new SocketBusDriver(
                            new SocketBusDriver.DefaultDependencies()
                                .setMessageBusProtocol(TCP_SECURE_MESSAGE_BUS_PROTOCOL)
                                .setMemoryBusProtocol(TCP_SECURE_MEMORY_BUS_PROTOCOL)
                                .setSocketProvider(new SSLSocketProvider(
                                        new SSLSocketProvider.DefaultDependencies()
                                            .applySSLSettings(settingsSSL)
                                            .setDelegate(TcpSocketProvider.MULTIPLEXED)))));

                    mapDriver.put(SDP_SECURE_SOCKET_BUS, new SocketBusDriver(
                            new SocketBusDriver.DefaultDependencies()
                                .setMessageBusProtocol(SDP_SECURE_MESSAGE_BUS_PROTOCOL)
                                .setMemoryBusProtocol(SDP_SECURE_MEMORY_BUS_PROTOCOL)
                                .setSocketProvider(new SSLSocketProvider(
                                        new SSLSocketProvider.DefaultDependencies()
                                            .applySSLSettings(settingsSSL)
                                            .setDelegate(SdpSocketProvider.MULTIPLEXED)))));
                    }

                m_mapDriver = mapDriver = Collections.unmodifiableMap(mapDriver);
                }
            return mapDriver;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public DefaultDependencies setDrivers(Map<String, Driver> mapDriver)
            {
            m_mapDriver = mapDriver;
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
        public SSLSettings getSSLSettings()
            {
            return m_settingsSSL;
            }

        /**
         * Set the SSLSettings.
         *
         * @param settingsSSL  the ssl properties
         *
         * @return this object
         */
        public DefaultDependencies setSSLSettings(SSLSettings settingsSSL)
            {
            m_settingsSSL = settingsSSL;
            return this;
            }


        // ----- helpers ------------------------------------------------

        /**
         * Validate the supplied dependencies.
         *
         * @throws IllegalArgumentException on an argument error
         *
         * @return this object
         */
        protected DefaultDependencies validate()
            {
            ensureArgument(getDefaultMemoryBusEndPoint(),  "DefaultMemoryBusEndPoint");
            ensureArgument(getDefaultMessageBusEndPoint(), "DefaultMessageBusEndPoint");
            ensureArgument(getDrivers(),                   "Drivers");

            for (Driver driver: getDrivers().values())
                {
                ensureArgument(driver, "driver");
                }

            return this;
            }

        /**
         * Ensure that the specified object is non-null.
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
         * The default MessageBus EndPoint name.
         */
        protected String m_sDefaultMsgBusEndPoint;

        /**
         * The default MemoryBus EndPoint name.
         */
        protected String m_sDefaultMemBusEndPoint;

        /**
         * The Drivers to use.
         */
        protected Map<String, Driver> m_mapDriver;

        /**
         * The Logger.
         */
        protected Logger m_logger;

        /**
         * The configured ssl properties.
         */
        protected SSLSettings m_settingsSSL;
        }


    // ----- inner class: DeferredDriver -------------------------------------

    /**
     * A DeferredDriver defers the instantiation of the specified driver until it is used.
     *
     * This is beneficial for drivers which are rarely used but which are expensive to instantiate
     */
    protected static class DeferredDriver
        implements Driver
        {
        public DeferredDriver(Factory<Driver> factory)
            {
            f_factory = factory;
            }

        @Override
        public void setDepot(Depot depot)
            {
            m_depot = depot;
            if (m_delegate != null)
                {
                m_delegate.setDepot(depot);
                }
            }

        @Override
        public Depot getDepot()
            {
            return m_depot;
            }

        @Override
        public EndPoint resolveEndPoint(String sName)
            {
            return delegate().resolveEndPoint(sName);
            }

        @Override
        public boolean isSupported(EndPoint point)
            {
            return delegate().isSupported(point);
            }

        @Override
        public Bus createBus(EndPoint pointLocal)
            {
            return delegate().createBus(pointLocal);
            }

        protected Driver delegate()
            {
            Driver driver = m_delegate;
            if (driver == null)
                {
                synchronized (this)
                    {
                    try
                        {
                        m_delegate = driver = f_factory.create();
                        }
                    catch (Exception e)
                        {
                        throw new UnsupportedOperationException(e);
                        }
                    }
                }

            return driver;
            }

        protected final Factory<Driver> f_factory;
        protected volatile Driver m_delegate;
        protected Depot m_depot;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Driver name for TCP Socket Bus.
     */
    private static final String TCP_SOCKET_BUS = "TcpSocketBus";

    /**
     * Protocol name for the TCP message bus.
     */
    public static final String TCP_MESSAGE_BUS_PROTOCOL = "tmb";

    /**
     * Protocol name for the TCP memory bus.
     */
    public static final String TCP_MEMORY_BUS_PROTOCOL = "trb";

    /**
     * Driver name for Secure TCP Socket Bus.
     */
    private static final String TCP_SECURE_SOCKET_BUS = "SecureTcpSocketBus";

    /**
     * Protocol name for the SSL protected TCP message bus.
     */
    public static final String TCP_SECURE_MESSAGE_BUS_PROTOCOL = "tmbs";

    /**
     * Protocol name for the SSL protected TCP memory bus.
     */
    public static final String TCP_SECURE_MEMORY_BUS_PROTOCOL  = "trbs";

    /**
     * Driver name for SDP Socket Bus.
     */
    private static final String SDP_SOCKET_BUS = "SdpSocketBus";

    /**
     * Protocol name for the SDP message bus.
     */
    public static final String SDP_MESSAGE_BUS_PROTOCOL = "sdmb";

    /**
     * Protocol name for the SDP memory bus.
     */
    public static final String SDP_MEMORY_BUS_PROTOCOL = "sdrb";

    /**
     * Driver name for Secure SDP Socket Bus.
     */
    private static final String SDP_SECURE_SOCKET_BUS = "SecureSdpSocketBus";

    /**
     * Protocol name for the SSL protected SDP message bus.
     */
    public static final String SDP_SECURE_MESSAGE_BUS_PROTOCOL = "sdmbs";

    /**
     * Protocol name for the SSL protected SDP memory bus.
     */
    public static final String SDP_SECURE_MEMORY_BUS_PROTOCOL = "sdrbs";

    /**
     * The default Logger for the depot.
     */
    private static Logger LOGGER = Logger.getLogger(SimpleDepot.class.getName());


    // ----- data members ---------------------------------------------------

    /**
     * The Depot's Dependencies.
     */
    protected Dependencies m_dependencies;
    }
