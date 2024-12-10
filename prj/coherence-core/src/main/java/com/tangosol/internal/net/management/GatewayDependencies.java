/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.management.MBeanServerFinder;

import com.tangosol.util.Filter;

import javax.management.MBeanServer;

/**
 * The GatewayDependencies interface provides a Gateway object with external dependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public interface GatewayDependencies
    {
    /**
     * Return a flag that specifies whether or not this cluster node exposes its managed
     * objects to remote MBeanServer(s).
     *
     * @return boolean flag indicating whether exposes object to remote MBeanServer(s)
     */
    public boolean isAllowRemoteManagement();

    /**
     * Returns the configuration information for the connector.
     *
     * @return connector Dependencies
     */
    public ConnectorDependencies getConnectorDependencies();

    /**
     * Returns an iterable over pre-configured CustomMBeanDependencies.
     *
     * @return Iterable of custom MBean Dependencies
     */
    public Iterable<CustomMBeanDependencies> getCustomMBeanDependencies();

    /**
     * Return the MBeanServer default domain.
     *
     * @return the MBean server default domain
     */
    public String getDefaultDomain();

    /**
     * Return the MBean domain name suffix.
     *
     * @return the MBean domain name suffix to be appended to <code>Coherence@</code>
     *         or the default of empty string indicating no suffix.
     */
    public String getDomainNameSuffix();

    /**
     * Return the Filter used to evaluate whether or not to register a model with the
     * specified name.
     *
     * @return the filter
     */
    public Filter getFilter();

    /**
     * Return a string that specifies whether or not a cluster node's JVM has an [in-process]
     * MBeanServer and if so, whether or not this node allows management of other node's
     * managed objects. Legal values are: dynamic, none, local-only, remote-only or all.
     *
     * @return value of current managed node option
     */
    public String getManagedNodes();

    /**
     * Return a string that specifies whether or not a cluster node's JVM has an [in-process]
     * Management over REST server. Legal values are: all, inherit, or none.
     *
     * @return the value of current HTTP managed node option
     *
     * @since 12.2.1.4.0
     */
    public String getHttpManagedNodes();

    /**
     * Return the MBeanServerFinder as configured in the Coherence
     * operational configuration descriptor ("server-factory" element).
     *
     * @return the MBeanServerFinder (null if none was configured)
     */
     public MBeanServerFinder getMBeanServerFinder();

    /**
     * Return a flag which specifies whether or not only the viewing of attributes is allowed.
     *
     * @return boolean flag indicating if view of attributes is allowed
     */
    public boolean isReadOnly();

    /**
     * Return the ReporterDependencies.
     *
     * @return ReporterDependencies
     */
    public ReportBatch.Dependencies getReporterDependencies();

    /**
     * Return the MBeanServer.
     *
     * @return the MBean server
     */
    public MBeanServer getServer();

    /**
     * Return true iff extended global MBean names are to be used.
     *
     * @return true iff extended global MBean names are to be used
     */
    public boolean isExtendedMBeanName();
    }
