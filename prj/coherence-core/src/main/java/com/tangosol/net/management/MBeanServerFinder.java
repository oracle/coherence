/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import javax.management.MBeanServer;

import javax.management.remote.JMXServiceURL;


/**
* MBeanServerFinder represents a facility that allows a pluggable run-time
* selection of an MBeanServer to be used by the Coherence JMX framework.
*
* @author gg 2009.06.22
* @since Coherence 3.6
*/
public interface MBeanServerFinder
    {
    /**
    * Find an MBeanServer that should be used by the Coherence JMX framework
    * to register new or locate existing MBeans.
    * <p>
    * Under some circumstances, there could exist more than one MBeanServer and
    * the default domain name parameter is used to identify which MBeanServer
    * should be returned by this method.
    * <p>
    * When standard Coherence MBeans are need to be registered with an
    * MBeanServer, this method is passed the value of the
    * "management-config/default-domain-name" element in the operational
    * configuration descriptor (empty string by default).
    * When a platform or custom MBeans are to be queried using the
    * "mbean/mbean-query" configuration element, the value of the
    * "mbean/mbean-server-domain" element is passed as this parameter.
    * <p>
    * <b>Note:</b> Returning null will result in Coherence using the standard
    * MBeanServer discovery algorithm
    * (see {@link MBeanHelper#findMBeanServer(String)}).
    *
    * @param sDefaultDomain  the default domain name; empty string indicates
    *                        that the caller does not have any preferences
    *                        and MBeanServerFinder could return any MBeanServer
    *                        it deems fit
    *
    * @return an existing or new MBeanServer with the specified default
    *          domain name (if specified) or null
    */
    public MBeanServer findMBeanServer(String sDefaultDomain);

    /**
     * Find the JMXServiceURL for the MBeanConnector used by the
     * Coherence JMX framework.
     * <p>
     * When "management-config/managed-nodes" value is set to "dynamic" and a
     * Coherence node is elected to be a management node, then it calls this
     * method to find out if there is an existing MBean Connector running that
     * can be used to access the MBeanServer on the node. It publishes this
     * JMXServiceURL thru the Coherence NamingService.
     * <p>
     * If there is no existing MBeanConnector running, then a new Connector
     * is started and its JMXServiceURL is published in the NamingService.
     *
     * @param sDefaultDomain  the default domain name; empty string indicates
     *                        that the caller does not have any preferences
     *                        and MBeanServerFinder could return any JMXServiceURL
     *                        it deems fit
     *
     * @return  JMXServiceURL for the MBeanConnector or null if no MBean
     *          Connector running.
     */
    public JMXServiceURL findJMXServiceUrl(String sDefaultDomain);
    }
