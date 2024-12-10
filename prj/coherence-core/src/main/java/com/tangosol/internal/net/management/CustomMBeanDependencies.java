/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

/**
 * CustomMBeanDependencies is the interface used to provide CustomMBean external dependencies.
 *
 * @author der  2011.08.10
 * @since Coherence 12.1.2
 */
public interface CustomMBeanDependencies
    {
    /**
     * Return the method name on the factory class used to obtain an MBean.
     *
     * @return method name of factory class
     */
    public String getMBeanAccessor();

    /**
     * Return the name of an MBean class used to instantiate and register with the Coherence
     * Management Framework.
     *
     * @return MBean class
     */
    public String getMBeanClass();

    /**
     * Return the name of an MBean class factory used to obtain MBeans to register with the Coherence
     * Management Framework.
     *
     * @return MBean class factory
     */
    public String getMBeanFactory();

    /**
     * Return the name of the MBean as it will be registered with the Coherence Management Framework.
     *
     * @return registered MBean name
     */
    public String getMBeanName();

    /**
     * Return the MBean query string used to obtain MBeans from a local MBean server to register
     * with the Coherence Management Framework.
     *
     * @return query string
     */
    public String getMBeanQuery();

    /**
     * Return the name of a default domain for the source MBeanServer. This is used to locate the
     * MBeanServer where the MBean query should be executed.
     *
     * @return MBean server default domain
     */
    public String getMBeanServerDomain();

    /**
     * Return a flag indicating whether the MBean should be instantiated and registered on this instance.
     *
     * @return flag indicating whether MBean is instantiated and registered
     */
    public boolean isEnabled();

    /**
     * Return a flag controlling the MBean life cycle. If set to true, the MBean life cycle extends to the
     * life cycle of the JVM; Otherwise, it coincides with the cluster node life cycle.
     *
     * @return MBean life cycle flag
     */
    public boolean isExtendLifecycle();

    /**
     * Return a flag indicating the MBean visibility across the cluster. If set to true,
     * the MBean is registered only with a local MBeanServer and is not accessible by other cluster nodes;
     * otherwise the "nodeId=..." key attribute is added to its name and the MBean will be visible from
     * any of the "managing" nodes (the ones that have the "managed-nodes" element set to values of "all"
     * or "remote-only").
     *
     * @return flag indicating of MBean accessible by other cluster nodes
     */
    public boolean isLocalOnly();
    }
