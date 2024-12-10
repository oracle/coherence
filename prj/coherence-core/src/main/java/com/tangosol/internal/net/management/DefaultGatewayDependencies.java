/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.coherence.reporter.ReportBatch;

import com.tangosol.net.management.MBeanServerFinder;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.NullImplementation;

import javax.management.MBeanServer;

/**
 * DefaultGatewayDependencies is a default implementation for GatewayDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultGatewayDependencies
        implements GatewayDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultGatewayDependencies object.
     */
    public DefaultGatewayDependencies()
        {
        this(null);
        }

    /**
     * Construct a {@link DefaultGatewayDependencies} object, copying the values
     * from the specified DefaultGatewayDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultGatewayDependencies(GatewayDependencies deps)
        {
        if (deps != null)
            {
            m_fAllowRemoteManagement = deps.isAllowRemoteManagement();
            m_connectorDependencies  = deps.getConnectorDependencies();
            m_itrbCustomMBeanDeps    = deps.getCustomMBeanDependencies();
            m_filter                 = deps.getFilter();
            m_sManagedNodes          = deps.getManagedNodes();
            m_sHttpManagedNodes      = deps.getHttpManagedNodes();
            m_fReadOnly              = deps.isReadOnly();
            m_reporterDependencies   = deps.getReporterDependencies();
            m_server                 = deps.getServer();
            m_fExtendedMBeanName     = deps.isExtendedMBeanName();
            m_mbeanServerFinder      = deps.getMBeanServerFinder();
            m_sDomainNameSuffix      = deps.getDomainNameSuffix();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAllowRemoteManagement()
        {
        return m_fAllowRemoteManagement;
        }

    /**
     * Set flag indicating whether or not this cluster node exposes its managed objects to
     * remote MBeanServer(s).
     *
     * @param fAllow  flag indicating whether objects are exposed to remote MBeanServer(s)
     *
     * @return this object
     */
    public DefaultGatewayDependencies setAllowRemoteManagement(boolean fAllow)
        {
        m_fAllowRemoteManagement = fAllow;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConnectorDependencies getConnectorDependencies()
        {
        return m_connectorDependencies;
        }

    /**
     * Set the configuration information (dependencies) for the connector.
     *
     * @param connDep  Connector dependencies
     *
     * @return this object
     */
    public DefaultGatewayDependencies setConnectorDependencies(ConnectorDependencies connDep)
        {
        m_connectorDependencies = connDep;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<CustomMBeanDependencies> getCustomMBeanDependencies()
        {
        return m_itrbCustomMBeanDeps;
        }

    /**
     * Set an iterable of custom MBean configuration dependencies.
     *
     * @param itrbCustomMBeanDeps  an iterable over custom MBean dependencies
     *
     * @return this object
     */
    public DefaultGatewayDependencies setCustomMBeanDependencies(Iterable<CustomMBeanDependencies> itrbCustomMBeanDeps)
        {
        m_itrbCustomMBeanDeps = itrbCustomMBeanDeps;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultDomain()
        {
        return m_sDefaultDomain;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDomainNameSuffix()
        {
        return m_sDomainNameSuffix;
        }

    /**
     * Set the MBeanServer default domain.
     *
     * @param sDefaultDomain  the MBean Server default domain
     *
     * @return this object
     */
    protected DefaultGatewayDependencies setDefaultDomain(String sDefaultDomain)
        {
        m_sDefaultDomain = sDefaultDomain;
        return this;
        }

    /**
     * Set a MBean domain name suffix.
     *
     * @param sDomainNameSuffix  the MBean domain name suffix
     *
     * @return this object
     */
    protected DefaultGatewayDependencies setDomainNameSuffix(String sDomainNameSuffix)
        {
        m_sDomainNameSuffix = sDomainNameSuffix;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getFilter()
        {
        return m_filter;
        }

    /**
     * Set the Filter used to evaluate whether or not to register a model with the
     * specified name.
     *
     * @param filter  used for registration
     *
     * @return this object
     */
    public DefaultGatewayDependencies setFilter(Filter filter)
        {
        m_filter = filter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getManagedNodes()
        {
        return m_sManagedNodes;
        }

    /**
     * Set managedNodes which specifies whether or not a cluster node's JVM has an [in-process]
     * MBeanServer and if so, whether or not this node allows management of other nodes'
     * managed objects. See {@link GatewayDependencies#getHttpManagedNodes()} for legal values.
     *
     * @param sManagedNodes  managed nodes
     *
     * @return this object
     */
    public DefaultGatewayDependencies setManagedNodes(String sManagedNodes)
        {
        m_sManagedNodes = sManagedNodes;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHttpManagedNodes()
        {
        return m_sHttpManagedNodes;
        }

    /**
     * Set whether or not a cluster node's JVM has an [in-process] Management over REST
     * service. See {@link GatewayDependencies#getHttpManagedNodes()} for legal values.
     *
     * @param sHttpManagedNodes  managed nodes
     *
     * @return this object
     *
     * @since 12.2.1.4.0
     */
    public DefaultGatewayDependencies setHttpManagedNodes(String sHttpManagedNodes)
        {
        m_sHttpManagedNodes = sHttpManagedNodes;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanServerFinder getMBeanServerFinder()
        {
        return m_mbeanServerFinder;
        }

    /**
     * Set the MBeanServerFinder as configured in the Coherence
     * operational configuration descriptor ("server-factory" element).
     *
     * @param finder  MBeanServerFinder
     *
     * @return this object
     */
    public DefaultGatewayDependencies setMBeanServerFinder(MBeanServerFinder finder)
        {
        m_mbeanServerFinder = finder;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly()
        {
        return m_fReadOnly;
        }

    /**
     * Set flag indicating whether or not only the viewing of attributes is allowed.
     *
     * @param fReadOnly  flag indicating whether or not only the viewing of
     *        attribute is allowed
     *
     * @return this object
     */
    public DefaultGatewayDependencies setReadOnly(boolean fReadOnly)
        {
        m_fReadOnly = fReadOnly;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReportBatch.Dependencies getReporterDependencies()
        {
        return m_reporterDependencies;
        }

    /**
     * Set the configuration information (dependencies) for the Reporter.
     *
     * @param reporterDep  Reporter dependencies
     *
     * @return this object
     */
    public DefaultGatewayDependencies setReporterDependencies(ReportBatch.Dependencies reporterDep)
        {
        m_reporterDependencies = reporterDep;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public MBeanServer getServer()
        {
        return m_server;
        }

    /**
     * The MBeanServer.
     *
     * @param server  the MBean Server
     *
     * @return this object
     */
    protected DefaultGatewayDependencies setServer(MBeanServer server)
        {
        m_server = server;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isExtendedMBeanName()
        {
        return m_fExtendedMBeanName;
        }

    /**
     * Set the flag indicating whether or not extended global MBean names are
     * to be used.
     *
     * @param fExtended  the flag indicating whether or not extended global
     *                   MBean names are to be used
     *
     * @return this object
     */
    public DefaultGatewayDependencies setExtendedMBeanName(boolean fExtended)
        {
        m_fExtendedMBeanName = fExtended;
        return this;
        }

    // ----- DefaultGatewayDependencies methods -----------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultGatewayDependencies validate()
        {
        Base.checkNotNull(getManagedNodes(), "ManagedNodes");
        if (getConnectorDependencies() != null)
            {
            ((DefaultConnectorDependencies) getConnectorDependencies()).validate();
            }
        if (getReporterDependencies() != null)
            {
            ((ReportBatch.DefaultDependencies) getReporterDependencies()).validate();
            }
        customMBeanDependenciesValidate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass())
            + "{AllowRemoteManagement="    + isAllowRemoteManagement()
            + ", ConnectorDependencies="   + getConnectorDependencies()
            + ", CustomMBeanDependencies=" + customMBeanDependenciesToString()
            + ", DefaultDomain="           + getDefaultDomain()
            + ", Filter="                  + getFilter()
            + ", ManagedNodes="            + getManagedNodes()
            + ", HttpManagedNodes="        + getHttpManagedNodes()
            + ", ReadOnly="                + isReadOnly()
            + ", ReporterDependencies="    + getReporterDependencies()
            + ", Server="                  + getServer()
            + "}\n";
        }

    // ----- internal methods  ----------------------------------------------

    /**
     * Return the CustomMBeanDependencies map in string format.
     *
     * @return CustomMBeanDependencies map in string format
     */
    private String customMBeanDependenciesToString()
        {
        StringBuilder sb = new StringBuilder();

        for (CustomMBeanDependencies deps : getCustomMBeanDependencies())
            {
            sb.append(deps.toString());
            }

        return sb.toString();
        }

    /**
     * Validate the CustomMBeanDependencies map in string format.
     */
    private void customMBeanDependenciesValidate()
        {
        for (CustomMBeanDependencies deps : getCustomMBeanDependencies())
            {
            ((DefaultCustomMBeanDependencies) deps).validate();
            }
        }

    // ----- data members and constants -------------------------------------

    /**
     * Specifies whether or not this cluster node exposes its managed objects to
     * remote MBeanServer(s).
     */
    private boolean m_fAllowRemoteManagement = true;

    /**
     * Contains the configuration information for the connector.
     */
    private ConnectorDependencies m_connectorDependencies;

    /**
     * A map that contains pre-configured custom MBean dependencies.
     */
    @SuppressWarnings("unchecked")
    private Iterable<CustomMBeanDependencies> m_itrbCustomMBeanDeps = NullImplementation.getIterable();

    /**
     * The MBeanServer Default Domain.
     */
    private String m_sDefaultDomain = "";

    /**
     * The MBean domain name suffix.
     */
    private String m_sDomainNameSuffix = "";

    /**
     * The Filter used to evaluate whether or not to register a model with the
     * specified name.
     */
    private Filter m_filter;

    /**
     * Specifies whether or not a cluster node's JVM has an [in-process] MBeanServer
     * and if so, whether or not this node allows management of other nodes'
     * managed objects. Legal values are: none, local-only, remote-only or all.
     */
    private String m_sManagedNodes = "none";

    /**
     * Specifies whether or not a cluster node's JVM has an [in-process]
     * Management over REST server. See {@link #getHttpManagedNodes()} for legal values.
     *
     * @since 12.2.1.4.0
     */
    private String m_sHttpManagedNodes = "inherit";

    /**
     * The MBeanServerFinder as configured in the Coherence
     * operational configuration descriptor ("server-factory" element).
     */
    private MBeanServerFinder m_mbeanServerFinder;

    /**
     * Specifies whether or not only the viewing of attributes is allowed.
     */
    private boolean m_fReadOnly = false;

    /**
     * Contains the configuration information for the reporter.
     */
    private ReportBatch.Dependencies m_reporterDependencies;

    /**
     * The MBeanServer.
     */
    private MBeanServer m_server;
    /**
     * The flag specifying whether or not extended global MBean names should be used.
     */
    private boolean m_fExtendedMBeanName = false;
    }
