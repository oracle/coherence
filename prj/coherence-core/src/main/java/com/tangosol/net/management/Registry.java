/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.management;


import com.tangosol.net.Member;


/**
* The Coherence cluster management gateway is an abstraction of the basic
* JMX registration APIs that is specific to managing Coherence clustered
* resources. Though this interface is closely related to the JMX
* infrastructure, it is independent from <tt>javax.management.*</tt> classes.
* This enables remote management support for cluster nodes that
* are not co-located with any JMX services.
* <p style="font-weight:bold">MBean Names</p>
* Each Coherence MBean has a unique {@link javax.management.ObjectName ObjectName}
* for registration in a MBeanServer that could either be co-located or remote
* in relation to the managed object.
* Each cluster node has a single instance of the following managed beans:
*
* <blockquote>
* <table border>
* <caption>Managed Bean ObjectName</caption>
* <tr>
* <th>Managed Bean</th>
* <th>ObjectName</th>
* </tr>
* <tr>
* <td> ManagementMBean </td>
* <td> {@link #MANAGEMENT_TYPE type=Management}</td>
* </tr>
* <tr>
* <td> ReporterMBean </td>
* <td> {@link #REPORTER_TYPE type=Reporter}</td>
* </tr>
* <tr>
* <td> ClusterMBean </td>
* <td> {@link #CLUSTER_TYPE type=Cluster}</td>
* </tr>
* <tr>
* <td> ClusterNodeMBean </td>
* <td> {@link #NODE_TYPE type=Node},
*       nodeId=<i>cluster&nbsp;node's id</i></td>
* </tr>
* <tr>
* <td> PointToPointMBean </td>
* <td> {@link #POINT_TO_POINT_TYPE type=PointToPoint},
*       nodeId=<i>cluster&nbsp;node's id</i></td>
* </tr>
* </table>
* </blockquote>
* <p>
* A cluster node may have zero or more instances of the following managed beans:
* <blockquote>
* <table border>
* <caption>Managed Bean ObjectName</caption>
* <tr>
* <th>Managed Bean</th>
* <th>ObjectName</th>
* </tr>
* <tr>
* <td> ServiceMBean </td>
* <td> {@link #SERVICE_TYPE type=Service},
*       name=<i>service&nbsp;name</i>,nodeId=<i>cluster&nbsp;node's&nbsp;id</i></td>
* </tr>
* <tr>
* <td> CacheMBean </td>
* <td> {@link #CACHE_TYPE type=Cache},
*       service=<i>service&nbsp;name</i>,name=<i>cache&nbsp;name</i>,
*       nodeId=<i>cluster&nbsp;node's&nbsp;id</i>[,tier=<i>tier&nbsp;tag</i>]</td>
* </tr>
* <tr>
* <td> StorageManagerMBean </td>
* <td> {@link #STORAGE_MANAGER_TYPE type=StorageManager},
*       service=<i>service&nbsp;name</i>,cache=<i>cache&nbsp;name</i>,
*       nodeId=<i>cluster&nbsp;node's&nbsp;id</i></td>
* </tr>
* <tr>
* <td> ConnectionManagerMBean </td>
* <td> {@link #CONNECTION_MANAGER_TYPE type=ConnectionManager},
*       name=<i>service&nbsp;name</i>,nodeId=<i>cluster&nbsp;node's&nbsp;id</i></td>
* </tr>
* <tr>
* <td> ConnectionMBean </td>
* <td> {@link #CONNECTION_TYPE type=Connection},
*       name=<i>service&nbsp;name</i>,nodeId=<i>cluster&nbsp;node's&nbsp;id</i>,
*       UUID=<i>connection's&nbsp;UUID&nbsp;</i></td>
* </tr>
* </table>
* </blockquote>
*
* The domain name for each managed bean will be assigned automatically (see
* {@link #getDomainName}.)
*
@MBEAN_JAVADOC@
*
* <p>
* <p style="font-weight:bold">Custom MBeans</p>
* In addition to the standard Coherence managed object types, any dynamic or
* standard MBean type may be registered locally or globally using the Registry.
* <p>
* For example, the following code registers a custom standard MBean object
* globally:
* <pre>
* Registry    registry = CacheFactory.ensureCluster().getManagement();
* CustomMBean bean     = new Custom();
* String      sName    = registry.ensureGlobalName("type=Custom");
*
* registry.register(sName, bean);
* </pre>
*
* @author gg 2004.11.01
* @author jh 2005.09.14
*
* @since Coherence 3.0
*/

/* The naming convention for corresponding MBeans is aimed to satisfy an
* important condition:
* if a life cycle of object C is fully enclosed in a lifecycle of object P
* and there are managed beans MBeanC and MBeanP that correspond to those
* objects, then the canonical name of MBeanC must be "calculated" based
* on the MBeanP name. Specifically, an "unregister" call for MBeanP must
* be able to automatically unregister all the contained beans MBeanC.
*/
public interface Registry
    {
    /**
    * Domain name for managed beans registered by Coherence clustered services.
    * For cluster nodes that are co-located with an MBeanServer instance,
    * the returned value is "Coherence" (or "Coherence@NNN" in a case of a name
    * conflict when multiple clusters run within the same JVM).
    * For the nodes that are managed remotely and do not run an MBeanServer,
    * the returned value is an empty string.
    *
    * @return the domain name
    */
    public String getDomainName();

    /**
    * Convert the passed MBean name to a global name if necessary.
    * <p>
    * A name is global if it satisfies either of the two conditions:
    * <ol>
    *   <li>it contains a <tt>nodeId</tt> key property that differentiates
    * an MBean name from an otherwise identical MBean name registered by a
    * different cluster node;</li>
    *   <li>it contains a <tt>responsibility</tt> key property and is guaranteed
    * to be registered by no more than one cluster node.</li>
    * </ol>
    *
    * @param sName any valid MBean name
    *
    * @return a fully qualified version of the given MBean name
    */
    public String ensureGlobalName(String sName);

    /**
    * Convert the passed MBean name to a global name for a given cluster Member
    * if necessary.
    * <p>
    * A name is global if it satisfies either of the two conditions:
    * <ol>
    *   <li>it contains a <tt>nodeId</tt> key property that differentiates
    * an MBean name from an otherwise identical MBean name registered by a
    * different cluster node;</li>
    *   <li>it contains a <tt>responsibility</tt> key property and is guaranteed
    * to be registered by no more than one cluster node.</li>
    * </ol>
    *
    * @param sName   any valid MBean name
    * @param member  a cluster member
    *
    * @return a fully qualified version of the given MBean name
    *
    * @since Coherence 12.2.1
    */
    public String ensureGlobalName(String sName, Member member);

    /**
    * Check whether or not the specified name is already registered.
    *
    * @param sName  the MBean's name
    *
    * @return true iff the specified name is already registered
    */
    public boolean isRegistered(String sName);

    /**
    * Register a manageable object. The object itself is not necessarily an
    * MBean.  If the object is not an MBean (standard or dynamic), the Registry
    * will determine what MBean corresponds to the object and will instantiate
    * and register the necessary MBean either locally or globally depending on
    * the type of the managed object.
    * <p>
    * If the given MBean name is a {@link #ensureGlobalName global} name, the
    * corresponding MBean will be registered with all available MBeanServers
    * (local or remote); otherwise, the MBean will only be registered with the
    * local MBeanServer.
    *
    * @param sName  the MBean's name
    * @param oBean  the managed object
    */
    public void register(String sName, Object oBean);

    /**
    * Unregister managed objects that match the specified object name or
    * name pattern (query name).
    *
    * @param sName  MBean's name or a name pattern
    */
    public void unregister(String sName);

    /**
    * Obtain a reference to the NotificationManager for this Registry.
    *
    * @return the NotificationManager object
    */
    public NotificationManager getNotificationManager();

    /**
     * Obtain a reference to the MBeanServerProxy for this Registry.
     *
     * @return the MBeanServerProxy
     */
    public MBeanServerProxy getMBeanServerProxy();


    // ----- constants ------------------------------------------------------

    /**
     * The name of the Invocation service used for remote management.
     */
    public static String SERVICE_NAME = "Management";

    /**
    * String representation of the <tt>ObjectName</tt> for the ClusterMBean.
    * There will be one and only one MBean by this name. It represents a local
    * cluster node and can only be viewed or managed locally.
    */
    public final static String CLUSTER_TYPE = "type=Cluster";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ClusterNodeMBean.
    */
    public final static String NODE_TYPE = "type=Node";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ServiceMBean.
    */
    public final static String SERVICE_TYPE = "type=Service";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * CacheMBean.
    */
    public final static String CACHE_TYPE = "type=Cache";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * StorageManagerMBean.
    */
    public final static String STORAGE_MANAGER_TYPE = "type=StorageManager";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * PointToPointMBean.
    */
    public final static String POINT_TO_POINT_TYPE = "type=PointToPoint";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ConnectionManagerMBean.
    */
    public final static String CONNECTION_MANAGER_TYPE = "type=ConnectionManager";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ConnectionMBean.
    */
    public final static String CONNECTION_TYPE = "type=Connection";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ManagementMBean.
    */
    public final static String MANAGEMENT_TYPE = "type=Management";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * ReporterMBean.
    */
    public final static String REPORTER_TYPE = "type=Reporter";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * TransactionManagerMBean.
    */
    public final static String TRANSACTION_MANAGER_TYPE = "type=TransactionManager";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * JournalMBean.
    */
    public final static String JOURNAL_TYPE = "type=Journal";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * FederationMBean.
    */
    public final static String FEDERATION_TYPE = "type=Federation";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * PersistenceManagerMBean.
    */
    public final static String PERSISTENCE_SNAPSHOT_TYPE = "type=Persistence";

    /**
    * String representing the "type" part of <tt>ObjectName</tt> for the
    * PartitionAssignmentStrategy (SimpleStrategyMBean).
    */
    public final static String PARTITION_ASSIGNMENT_TYPE = "type=PartitionAssignment";

    /**
    * String representing the "nodeId" key of an <tt>ObjectName</tt> for
    * a global MBean.
    */
    public final static String KEY_NODE_ID = "nodeId=";

    /**
    * String representing the "responsibility" key of an <tt>ObjectName</tt> for
    * a global MBean.
    */
    public final static String KEY_RESPONSIBILITY = "responsibility=";

    /**
    * String representing the "member" key of an <tt>ObjectName</tt> for
    * an extended global MBean.
    */
    public final static String KEY_MEMBER = "member=";

    /**
    * String representing the "cluster" key of an <tt>ObjectName</tt> for
    * an extended global MBean.
    */
    public final static String KEY_CLUSTER = "cluster=";

    /**
    * String representing the "service" key of an <tt>ObjectName</tt> for
    * an extended global MBean.
    */
    public final static String KEY_SERVICE = "service=";

    /**
     * String representing the "type" part of <tt>ObjectName</tt> for the
     * ExecutorMBean.
     *
     * @since 21.12
     */
    public final static String EXECUTOR_TYPE = "type=Executor";
    }

