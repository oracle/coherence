/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.base.Disposable;

import com.tangosol.net.management.Registry;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.Controllable;
import com.tangosol.util.ResourceRegistry;

import java.util.Enumeration;
import java.util.Set;

/**
* The Cluster interface represents a collection of
* services that operate in a clustered network environment.
*
* @author gg  2002.02.08
*
* @since Coherence 1.1
*/
public interface Cluster
        extends Controllable
    {
    /**
    * Returns an Enumeration of String objects, one for each service that
    * has been previously registered in the cluster.
    * <p>
    * For each name, a call to {@link #getServiceInfo(String)} will return
    * a ServiceInfo describing a service. However, the call to
    * {@link #getService(String)} may return null if that service is not
    * available locally.
    *
    * @return Enumeration of service names
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    */
    public Enumeration<String> getServiceNames();

    /**
    * Returns a ServiceInfo object for the specified service name.
    *
    * @param sName  the service name, within the cluster, that uniquely
    *               identifies a registered service
    *
    * @return a ServiceInfo for the specified service; null if that
    *         service name has not been registered
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    *
    * @see #getService(String)
    */
    public ServiceInfo getServiceInfo(String sName);

    /**
    * Returns a Service object for the specified service name.
    *
    * @param sName  the service name, within the cluster, that uniquely
    *               identifies a service
    *
    * @return a Service for the specified name; null if the
    *         specified service is not available locally
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    *
    * @see #getServiceInfo(String)
    * @see #ensureService(String, String)
    */
    public Service getService(String sName);

    /**
    * Obtains a Service object for the specified service name and type.
    * <p>
    * If the service with the specified name already exists, a reference
    * to the same service will be returned. Otherwise a new service object
    * will be instantiated and returned. The service's context ClassLoader
    * will be initialized with the Cluster's context ClassLoader.
    * <p>
    * It is essential to understand that until the service is started using
    * {@link Controllable#start()}, the cluster doesn't keep a reference to
    * that service instance. Therefore, the external synchronization on the
    * cluster object is necessary to prevent creation of a duplicate service.
    *
    * @param sName  the service name, within the cluster, that uniquely
    *               identifies a service
    * @param sType  the service type, that serves as a key to the cluster
    *               configuration info, allowing the cluster instantiate
    *               the corresponding service implementations if the
    *               specified service is not available locally
    *
    * @return a Service object for the specified service name an type
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    * @exception IllegalArgumentException thrown if the type is illegal or
    *            unknown
    */
    public Service ensureService(String sName, String sType);

    /**
    * Suspend all members of the service identified by the specified name.  A
    * suspended Service has been placed in a "quiesced" or "deactivated" state
    * in preparation to be shutdown.  Once suspended, a service may be "resumed"
    * or "reactivated" with the {@link #resumeService(String) resumeService}
    * method.
    * <p>
    * If "Cluster" is passed for the service name, all services (including
    * the ClusterService) will be suspended.
    *
    * @param sName  the service name
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    * @exception IllegalArgumentException thrown if the name is illegal or
    *            unknown
    */
    public void suspendService(String sName);

    /**
    * Resume all suspended members of the service identified by the specified
    * name.
    * <p>
    * If "Cluster" is passed for the service name, all services (including
    * the ClusterService) will be resumed.
    *
    * @param sName  the service name
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    * @exception IllegalArgumentException thrown if the name is illegal or
    *            unknown
    */
    public void resumeService(String sName);

    /**
    * Returns a Set of Member objects, one for each Member of
    * the cluster.
    *
    * @return Set of cluster members
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    */
    public Set<Member> getMemberSet();

    /**
    * Returns a Member object representing the local (i.e. this JVM)
    * member of the cluster.
    *
    * @return the local cluster member
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    */
    public Member getLocalMember();

    /**
    * Returns the current "cluster time", which is analogous to the
    * {@link System#currentTimeMillis()} except that the cluster
    * time is the roughly the same for all Members in the cluster.
    *
    * @return the cluster time in milliseconds
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    */
    public long getTimeMillis();

    /**
    * Returns a Member object representing the senior cluster member.
    *
    * @return the senior cluster member
    *
    * @exception IllegalStateException thrown if the cluster service
    *            is not running or has stopped
    */
    public Member getOldestMember();

    /**
    * Returns the current management registry.
    *
    * @return the current management registry or null
    *         if the management is disabled on this node
    *
    * @since Coherence 3.0
    */
    public Registry getManagement();

    /**
    * Sets the current management registry.
    *
    * @param registry  the management registry to use on this node
    *
    * @since Coherence 3.0
    */
    public void setManagement(Registry registry);

    /**
    * Determine the configured name for the Cluster. This name is defined by
    * the application, and can be used for any application-specific purpose.
    * Furthermore, as a safety feature, when joining into a Cluster, a Member
    * will only join it if the Cluster has the same name as the Member was
    * configured to join.
    *
    * @return the configured name for the Cluster or null if none is
    *         configured
    *
    * @since Coherence 3.2
    */
    public String getClusterName();

    /**
    * Retrieves a {@link Disposable} resource that was previously registered
    * using {@link Cluster#registerResource(String, Disposable)}.
    *
    * @param sName  the name of the resource to retrieve
    *
    * @return the {@link Disposable} resource, or null if no such resource is
    *         currently registered
    *
    * @since Coherence 3.7
    *
    * @deprecated Use {@link #getResourceRegistry()} to manage resources.
    */
    @Deprecated
    public Disposable getResource(String sName);

    /**
    * Registers the passed {@link Disposable} resource with the Cluster, using
    * the specified name. There are two reasons to register a resource:
    * <ul><li>Subsequent calls to {@link #getResource(String)} using the same
    * name will return the registered resource, and</li>
    * <li>The Cluster will invoke the {@link Disposable#dispose()} method of
    * the resource when the Cluster is shut down; specifically, when the
    * Cluster is shutting down, after it has shut down all of the
    * running services, the Cluster calls {@link Disposable#dispose()} on each
    * registered resource.</li></ul>
    * <p>
    * If a resource is already registered under the specified name, and the
    * new resource to register is not the same resource (the same reference)
    * as the previously registered resource, then the Cluster will throw an
    * {@link IllegalStateException}.
    * <p>
    * To unregister a previously registered resource, call the
    * {@link #unregisterResource(String)} method with the name of the
    * resource to unregister.
    * <p>
    * Note: It is the responsibility of the caller to manage concurrent access
    * to the {@link #getResource}, {@link #unregisterResource(String)} and
    * <tt>registerResource</tt> methods of the Cluster. Specifically, while
    * the Cluster manages the registry of resources in a thread-safe manner,
    * it is possible for a thread to call <tt>getResource</tt>, get a null
    * return value because a resource had not yet been registered under the
    * specified name, but by the time the thread instantiates a resource and
    * attempts to register it by calling <tt>registerResource</tt>, another
    * thread may have already done the same thing.
    *
    * @param sName     the name of the resource
    * @param resource  the {@link Disposable} resource to register
    *
    * @exception IllegalStateException if the resource is null
    *
    * @since Coherence 3.7
    *
    * @deprecated Use {@link #getResourceRegistry()} to manage resources.
    */
    @Deprecated
    public void registerResource(String sName, Disposable resource);

    /**
    * Unregisters a resource with the specified name.
    * <p>
    * Note: It is the responsibility of the caller to manage concurrent access
    * to the {@link #getResource}, <tt>unregisterResource(String)</tt> and
    * {@link #registerResource} methods of the Cluster. Specifically, while
    * the Cluster manages the registry of resources in a thread-safe manner,
    * it is possible for a thread to call <tt>getResource</tt>, get a null
    * return value because a resource had not yet been registered under the
    * specified name, but by the time the thread instantiates a resource and
    * attempts to register it by calling <tt>registerResource</tt>, another
    * thread may have already done the same thing.
    *
    * @param sName the name of the resource
    *
    * @return the registered {@link Disposable} resource or null if none
    *         was registered under that name
    *
    * @since Coherence 3.7
    *
    * @deprecated Use {@link #getResourceRegistry()} to manage resources.
    */
    @Deprecated
    public Disposable unregisterResource(String sName);

    /**
    * Retrieves a Cluster scoped {@link ResourceRegistry}. The resource
    * registry is used to:
    * <ul>
    *   <li>Register resources with the Cluster and make them accessible
    *   to the cluster.
    *   <li>Dispose of resources when the Cluster is shut down; see
    *   the {@link ResourceRegistry} API for details on how to enable
    *   cleanup of resources.
    * </ul>
    *
    * @return a Cluster scoped resource registry
    *
    * @since Coherence 12.1.2
    */
    public ResourceRegistry getResourceRegistry();

    /**
    * Retrieve the Cluster configuration.
    *
    * @return the Cluster configuration
    *
    * @since Coherence 12.2.1
    */
    public ClusterDependencies getDependencies();

    /**
    * Configure the Cluster.
    * <p>
    * This method can only be called before the Cluster is started.
    *
    * @param deps  the dependencies object carrying the Cluster configuration
    *              information
    *
    * @throws IllegalStateException thrown if the Cluster is already running
    * @throws IllegalArgumentException thrown if the configuration information
    *         is invalid
    *
    * @since Coherence 12.1.3
    */
    public void setDependencies(ClusterDependencies deps);

    /**
    * Return a description of the running services in this Cluster.
    *
    * @return string containing a description of the running services
    */
    default String getServiceBanner()
        {
        // Extract the Cluster object out of the Safe layer to
        // de-clutter the logging
        Cluster cluster;
        try
            {
            cluster = (Cluster) ClassHelper.
                    invoke(this, "getCluster", null);
            }
        catch (Exception e)
            {
            // If this fails for some reason, revert to SafeCluster
            cluster = this;
            }

        StringBuilder sb = new StringBuilder();

        sb.append("\nServices\n  (\n  ");

        if (cluster != null)
            {
            for (Enumeration<String> e = cluster.getServiceNames(); e.hasMoreElements();)
                {
                Service service = cluster.getService(e.nextElement());
                if (service != null)
                    {
                    sb.append(service)
                            .append("\n  ");
                    }
                }
            }
        sb.append(")\n");

        return sb.toString();
        }

    // ----- inner interface: MemberTimeoutAction -------------------------

    /**
    * MemberTimeoutAction is taken by the cluster service to remove members from
    * the cluster who have exceeded a network timeout (e.g. packet-timeout).
    * <p>
    * A MemberTimeoutAction object is valid <i>only</i> for the duration of an
    * invocation to {@link ActionPolicy#isAllowed}.
    * <p>
    * Note: ActionPolicy implementations should assume that the effects of a
    *       given physical outage may not be detected in an atomic fashion.
    */
    public interface MemberTimeoutAction
            extends Action
        {
        /**
        * Return the set of Members that have exceeded their timeout.
        *
        * @return the set of Members that have exceeded their timeout
        */
        public Set<Member> getTimedOutMemberSet();

        /**
        * Return the set of Members that have recently responded to this member.
        * <p>
        * Note: inclusion in the set of responsive members is meaningful, but
        *       exclusion is not.  Implementations should take care not to infer
        *       meaning in members not appearing in the responsive set.
        *
        * @return the set of Members that are known to be healthy
        */
        public Set<Member> getResponsiveMemberSet();

        /**
        * Return the set of Members who are "announcing".  Announcing members
        * are potential new cluster members who are broadcasting their presence
        * and attempting to join the cluster.
        *
        * @return the set of Members who are announcing
        */
        public Set<Member> getAnnouncingMemberSet();

        /**
        * Return the time at which the current outage "incident" was first
        * detected.  An "incident" is considered to start when the first member
        * timeout is reported, and lasts until no timed-out members remain in
        * the cluster.
        * <p>
        * Note: ActionPolicy implementations should not make assumptions about
        *       the time period within which physical outages will be detected,
        *       nor should they assume that outages will be reported
        *       simultaneously on different members.
        *
        * @return the time in ms at which the current incident started
        */
        public long getIncidentStartTime();
        }
    }
