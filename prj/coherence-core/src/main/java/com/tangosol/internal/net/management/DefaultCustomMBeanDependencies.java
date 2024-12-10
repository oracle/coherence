/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.util.ClassHelper;

/**
 * DefaultCustomMBeanDependencies is a base implementation for CustomMBeanDependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public class DefaultCustomMBeanDependencies
        implements CustomMBeanDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultCustomMBeanDependencies object. Uses default value
     * for each dependency.
     */
    public DefaultCustomMBeanDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultCustomMBeanDependencies object copying the values
     * from the specified CustomeMBeanDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultCustomMBeanDependencies(CustomMBeanDependencies deps)
        {
        if (deps != null)
            {
            m_sMBeanAccessor     = deps.getMBeanAccessor();
            m_sMBeanClass        = deps.getMBeanClass();
            m_sMBeanFactory      = deps.getMBeanFactory();
            m_sMBeanName         = deps.getMBeanName();
            m_sMBeanQuery        = deps.getMBeanQuery();
            m_sMBeanServerDomain = deps.getMBeanServerDomain();
            m_fEnabled           = deps.isEnabled();
            m_fExtendLifecycle   = deps.isExtendLifecycle();
            m_fLocalOnly         = deps.isLocalOnly();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled()
        {
        return m_fEnabled;
        }

    /**
     * Set whether the MBean should be instantiated and registered on this instance.
     *
     * @param fEnabled  flag indicating whether MBean is instantiated and registered
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setEnabled(boolean fEnabled)
        {
        m_fEnabled = fEnabled;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isExtendLifecycle()
        {
        return m_fExtendLifecycle;
        }

    /**
     * Sets a flag controlling the MBean life cycle. If set to true, the MBean life cycle extends to the
     * life cycle of the JVM; Otherwise, it coincides with the cluster node life cycle.
     *
     * @param fExtendLifecycle  MBean life cycle flag
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setExtendLifecycle(boolean fExtendLifecycle)
        {
        m_fExtendLifecycle = fExtendLifecycle;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocalOnly()
        {
        return m_fLocalOnly;
        }

    /**
     * The local-only element controls the MBean visibility across the cluster. If set to true,
     * the MBean is registered only with a local MBeanServer and is not accessible by other cluster nodes;
     * otherwise the "nodeId=..." key attribute is added to its name and the MBean will be visible from
     * any of the "managing" nodes (the ones that have the "managed-nodes" element set to values of "all"
     * or "remote-only").
     *
     * @param fLocalOnly  flag indicating whether MBean is visible locally
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setLocalOnly(boolean fLocalOnly)
        {
        m_fLocalOnly = fLocalOnly;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanAccessor()
        {
        return m_sMBeanAccessor;
        }

    /**
     * Set the method name on the factory class used to obtain an MBean.
     *
     * @param sMBAccessor  MBean factory class method
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanAccessor(String sMBAccessor)
        {
        m_sMBeanAccessor = sMBAccessor;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanClass()
        {
        return m_sMBeanClass;
        }

    /**
     * Set the name of an MBean class to instantiate and register with the Coherence
     * Management Framework.
     *
     * @param sMBClass  MBean class
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanClass(String sMBClass)
        {
        m_sMBeanClass = sMBClass;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanServerDomain()
        {
        return m_sMBeanServerDomain;
        }

    /**
     * Set the name of a default domain for the source MBeanServer. This is used to locate the
     * MBeanServer where the MBean query should be executed.
     *
     * @param sMBServerDomain  MBean domain
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanServerDomain(String sMBServerDomain)
        {
        m_sMBeanServerDomain = sMBServerDomain;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanFactory()
        {
        return m_sMBeanFactory;
        }

    /**
     * Set the name of an MBean class to factory to obtain MBeans to register with the Coherence
     * Management Framework.
     *
     * @param sMBFactory  MBean class factory
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanFactory(String sMBFactory)
        {
        m_sMBeanFactory = sMBFactory;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanName()
        {
        return m_sMBeanName;
        }

    /**
     * Set name of the MBean as it will be registered with the Coherence Management Framework.
     *
     * @param sMBName  MBean registered name
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanName(String sMBName)
        {
        m_sMBeanName = sMBName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMBeanQuery()
        {
        return m_sMBeanQuery;
        }

    /**
     * Set the MBean query string used to obtain MBeans from a local MBean server to register
     * with the Coherence Management Framework.
     *
     * @param sMBQuery  MBean query string
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies setMBeanQuery(String sMBQuery)
        {
        m_sMBeanQuery = sMBQuery;
        return this;
        }

    // ----- DefaultCustomMBeanDependencies methods -------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the configuration file are not valid
     *
     * @return this object
     */
    public DefaultCustomMBeanDependencies validate()
        {
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
                + "{Enabled="            + isEnabled()
                + ", ExtendLifecycle="   + isExtendLifecycle()
                + ", LocalOnly="         + isLocalOnly()
                + ", MBeanAccessor="     + getMBeanAccessor()
                + ", MBeanClass="        + getMBeanClass()
                + ", MBeanFactory="      + getMBeanFactory()
                + ", MBeanName="         + getMBeanName()
                + ", MBeanQuery="        + getMBeanQuery()
                + ", MBeanServerDomain=" + getMBeanServerDomain()
                + "}";
        }

    // ----- data members and constants -------------------------------------

    /**
     * Specifies whether the MBean should be instantiated and registered on this instance.
     */
    private boolean m_fEnabled;

    /**
     * A flag controlling the MBean life cycle. If set to true, the MBean life cycle extends to the
     * life cycle of the JVM; Otherwise, it coincides with the cluster node life cycle.
     */
    private boolean m_fExtendLifecycle;

    /**
     * The flag that controls the MBean visibility across the cluster.
     */
    private boolean m_fLocalOnly;

    /**
     * The method name on the factory class used to obtain an MBean.
     */
    private String m_sMBeanAccessor = "";

    /**
     * The name of an MBean class to instantiate and register with the Coherence
     * Management Framework.
     */
    private String m_sMBeanClass = "";

    /**
     * The name of an MBean class factory to obtain MBeans to register with the Coherence
     * Management Framework.
     */
    private String m_sMBeanFactory = "";

    /**
     * The name of the MBean as it will be registered with the Coherence Management Framework.
     */
    private String m_sMBeanName = "";

    /**
     * The MBean query string used to obtain MBeans from a local MBean server to register
     * with the Coherence Management Framework.
     */
    private String m_sMBeanQuery = "";

    /**
     * The name of a default domain for the source MBeanServer. This is used to locate the
     * MBeanServer where the MBean query should be executed.
     */
    private String m_sMBeanServerDomain = "";
    }
