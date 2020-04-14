/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.management;

import com.tangosol.util.Filter;

import com.tangosol.util.function.Remote;

import java.util.Map;
import java.util.Set;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

/**
 * MBeanServerProxy allows any cluster node that runs the Management service
 * to obtain and set attributes or invoke methods on registered MBeans.
 * <p>
 * Application logic can use MBeanServerProxy as follows:
 * <pre>
 *   Cluster          cluster  = CacheFactory.ensureCluster();
 *   Registry         registry = cluster.getManagement();
 *   MBeanServerProxy proxy    = registry.getMBeanServerProxy();
 *   String           sName    = registry.ensureGlobalName(
 *      "type=PartitionAssignment,service=partitioned,responsibility=DistributionCoordinator");
 *
 *   String sStatusHA = (String) proxy.getAttribute(sName, "HAStatus");
 * </pre>
 *
 * @author bbc 2014.09.24
 * @since Coherence 12.2.1
 */
public interface MBeanServerProxy
    {
    /**
     * Return the MBeanInfo for a given MBean.
     *
     * @param sName  the MBean name
     *
     * @return  the MBeanInfo for the MBean or {@code null}
     *          if the MBean does not exist
     */
    public MBeanInfo getMBeanInfo(String sName);
    
    /**
     * A {@link Remote.Function function} executed on the management node.
     * The function is provided a local MBeanServer and can return any serializable
     * result.
     *
     * @param function  the function to execute
     *
     * @param <R>  the return type
     *
     * @return the result returned by the function
     *
     * @since 12.2.1.4.0
     */
    public <R> R execute(Remote.Function<MBeanServer, R> function);

    /**
     * Return a Map of attribute name to attribute value for a given MBean
     * name.
     * <p>
     * The attributes returned must evaluate successfully against the provided
     * {@link Filter} to be returned.
     *
     * @param sName   the MBean name
     * @param filter  filter to limit the returned attributes
     *
     * @return a Map of attribute name to attribute value
     *
     * @since 12.2.1.4.0
     */
    public Map<String, Object> getAttributes(String sName, Filter<String> filter);

    /**
     * Obtain the value of the specified MBean attribute.
     *
     * @param sName  the MBean name
     * @param sAttr  the attribute name
     *
     * @return the value of the retrieved attribute
     */
    public Object getAttribute(String sName, String sAttr);

    /**
     * Set the value of the specified MBean attribute.
     *
     * @param sName   the MBean name
     * @param sAttr   the attribute name
     * @param oValue  the attribute value
     */
    public void setAttribute(String sName, String sAttr, Object oValue);

    /**
     * Invoke an operation on the specified MBean.
     *
     * @param sName        the MBean name to invoke the method on
     * @param sOpName      the name of the method to be invoked
     * @param aoParams     an array containing the method parameters
     * @param asSignature  an optional array containing the method signatures,
     *                     this parameter is only necessary if there are multiple
     *                     methods with the same name
     *
     * @return the result of invocation
     */
    public Object invoke(String sName, String sOpName, Object[] aoParams, String[] asSignature);

    /**
     * Get the names of MBeans controlled by the MBean server that is collocated
     * with the {@link com.tangosol.net.management.Registry cluster registry}.
     *
     * @param sPattern  the MBean name pattern identifying MBean names to be
     *                  retrieved; this pattern is the same as in the {@link
     *                  javax.management.MBeanServerConnection#queryMBeans
     *                  MBeanServer.queryNames} method;
     *                  if null, the name of all registered MBeans will be retrieved
     * @param filter    (optional) the filter to be applied for selecting MBeans
     *
     * @return a set of MBean names matching the pattern and the filter
     */
    public Set<String> queryNames(String sPattern, Filter<ObjectName> filter);

    /**
     * Get the names of MBeans controlled by the MBean server that is collocated
     * with the {@link com.tangosol.net.management.Registry cluster registry}.
     *
     * @param pattern  the MBean name pattern identifying MBean names to be
     *                 retrieved; this pattern is the same as in the {@link
     *                 javax.management.MBeanServerConnection#queryMBeans
     *                 MBeanServer.queryNames} method;
     *                 if null, the name of all registered MBeans will be retrieved
     * @param filter   (optional) the filter to be applied for selecting MBeans
     *
     * @return a set of MBean names matching the pattern and the filter
     */
    public Set<String> queryNames(ObjectName pattern, Filter<ObjectName> filter);

    /**
     * Check whether or not an MBean with the specified name is already registered.
     *
     * @param sName  the MBean name
     *
     * @return true iff the specified name is already registered
     */
    public boolean isMBeanRegistered(String sName);

    /**
     * Return a local only {@link MBeanServerProxy}.
     * <p>
     * A local only proxy operates only on the MBeans that are local
     * to this member.
     *
     * @return a local only {@link MBeanServerProxy}
     */
    public MBeanServerProxy local();

    /**
     * Adds a listener to a registered MBean.
     * <p>
     * Notifications emitted by the MBean will be forwarded to the listener.
     *
     * @param sName     the name of the MBean on which the listener should be  added
     * @param listener  the listener object which will handle the notifications
     *                  emitted by the registered MBean
     * @param filter    the filter object. If filter is null, no filtering will
     *                  be performed before handling notifications
     * @param handback  the context to be sent to the listener when a notification is emitted
     *
     * @see #removeNotificationListener(String, NotificationListener)
     * @see #removeNotificationListener(String, NotificationListener, NotificationFilter, Object)
     */
    public void addNotificationListener(String sName, NotificationListener listener,
        NotificationFilter filter, Object handback);

    /**
     * Removes a listener from a registered MBean.
     * <p>
     * If the listener is registered more than once, perhaps with
     * different filters or callbacks, this method will remove all
     * those registrations.
     *
     * @param sName     the name of the MBean on which the listener should be removed
     * @param listener  the listener to be removed
     *
     * @see #addNotificationListener(String, NotificationListener, NotificationFilter, Object)
     */
    public void removeNotificationListener(String sName, NotificationListener listener);

    /**
     * Removes a listener from a registered MBean.
     * <p>
     * The MBean must have a listener that exactly matches the
     * given <code>listener</code>, <code>filter</code>, and
     * <code>handback</code> parameters. If there is more than one
     * such listener, only one is removed.
     * <p>
     * The <code>filter</code> and <code>handback</code> parameters
     * may be null if and only if they are null in a listener to be
     * removed.
     *
     * @param sName     the name of the MBean on which the listener should be removed
     * @param listener  the listener to be removed
     * @param filter    the filter that was specified when the listener was added
     * @param handback  the handback that was specified when the listener was added
     *
     * @see #addNotificationListener(String, NotificationListener, NotificationFilter, Object)
     *
     */
    public void removeNotificationListener(String sName, NotificationListener listener,
        NotificationFilter filter, Object handback);
    }
