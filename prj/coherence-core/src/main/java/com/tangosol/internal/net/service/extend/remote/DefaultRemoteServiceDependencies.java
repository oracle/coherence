/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.internal.net.service.peer.initiator.InitiatorDependencies;

import com.tangosol.io.SerializerFactory;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;

/**
 * The DefaultRemoteServiceDependencies class provides a default implementation of
 * RemoteServiceDependencies.
 *
 * @author pfm 2011.09.05
 * @since Coherence 12.1.2
 */
public class DefaultRemoteServiceDependencies
        extends DefaultServiceDependencies
        implements RemoteServiceDependencies
    {
    /**
     * Construct a DefaultRemoteServiceDependencies object.
     */
    public DefaultRemoteServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultRemoteServiceDependencies object, copying the values from the
     * specified RemoteServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultRemoteServiceDependencies(RemoteServiceDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_sNameClusterRemote = deps.getRemoteClusterName();
            m_sNameServiceRemote = deps.getRemoteServiceName();
            m_initiatorDependencies = deps.getInitiatorDependencies();
            }
        }

    // ----- DefaultRemoteServiceDependencies methods -----------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteClusterName()
        {
        return m_sNameClusterRemote;
        }

    /**
     * Set the cluster name.
     *
     * @param sName  the name of the remote cluster
     */
    @Injectable("cluster-name")
    public void setRemoteClusterName(String sName)
        {
        m_sNameClusterRemote = sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRemoteServiceName()
        {
        return m_sNameServiceRemote;
        }

    /**
     * Set the remote service name.
     *
     * @param sName  the name of the remote service
     */
    @Injectable("proxy-service-name")
    public void setRemoteServiceName(String sName)
        {
        m_sNameServiceRemote = sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InitiatorDependencies getInitiatorDependencies()
        {
        return m_initiatorDependencies;
        }

    /**
     * Set the InitiatorDependencies.
     *
     * @param deps  the InitiatorDependencies
     */
    @Injectable("initiator-config")
    public void setInitiatorDependencies(InitiatorDependencies deps)
        {
        m_initiatorDependencies = deps;
        }

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultRemoteServiceDependencies validate()
        {
        Base.checkNotNull(getInitiatorDependencies(), "InitiatorDependencies");

        return this;
        }

    @Override
    public long getRequestTimeoutMillis()
        {
        InitiatorDependencies depsInitiator = getInitiatorDependencies();

        return depsInitiator == null
                ? super.getRequestTimeoutMillis()
                : depsInitiator.getRequestSendTimeoutMillis();
        }

    @Override
    public SerializerFactory getSerializerFactory()
        {
        return getInitiatorDependencies().getSerializerFactory();
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + "{RemoteCluster=" + getRemoteClusterName() + ", RemoteService=" + getRemoteServiceName()
               + ", InitiatorDependencies=" + getInitiatorDependencies() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The Cluster name.
     */
    private String m_sNameClusterRemote;

    /**
     * The remote service name.
     */
    private String m_sNameServiceRemote;

    /**
     * The InitiatorDependencies.
     */
    private InitiatorDependencies m_initiatorDependencies;
    }
