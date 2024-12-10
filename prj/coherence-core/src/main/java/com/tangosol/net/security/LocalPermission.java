/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.security;

import java.security.BasicPermission;


/**
 * This class is for local (non-clustered) permissions. A LocalPermission contains
 * a name (also referred to as a "target name") but no actions list;
 * the caller either has the named permission or it doesn't.
 * <p>
 * The target name is the name of the local permission (see the list below). The
 * naming convention follows the hierarchical property naming convention defined
 * in {@link BasicPermission}.
 * <p>
 * The following table lists all the possible LocalPermission target names,
 * and for each provides a description of what the permission allows
 * and a discussion of the risks of granting code the permission.
 *
 * <table border=1 cellpadding=4 summary="permission target name,
 *  what the target allows,and associated risks">
 * <tr>
 * <th>Permission Target Name</th>
 * <th>What the Permission Allows</th>
 * <th>Risks of Allowing this Permission</th>
 * </tr>
 *
 * <tr>
 *   <td>CacheFactory.setCacheFactoryBuilder</td>
 *   <td>Setting the CacheFactoryBuilder</td>
 *   <td>This is an extremely dangerous permission to grant.
 *       Malicious applications that can set their own CacheFactoryBuilder could
 *       intercept any access or mutation requests to any caches and have access
 *       to any data that flows into and from those caches.
 *    </td>
 * </tr>
 *
 * <tr>
 *   <td>Cluster.shutdown</td>
 *   <td>Shutting down all clustered services</td>
 *   <td>This allows an attacker to mount a denial-of-service attack by forcing
 *       all clustered service to shutdown.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td>BackingMapManagerContext.getBackingMap</td>
 *   <td>Getting a reference to the underlying backing map for a cache</td>
 *   <td>This is a dangerous permission to grant.
 *       Malicious code that can get a reference to the backing map can access
 *       any stored data without any additional security checks.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td>BackingMapManagerContext.setClassLoader</td>
 *   <td>Setting a ClassLoader used by the CacheService associated with the context</td>
 *   <td>The class loader is used by the cache service to load application classes
 *       that might not exist in the system class loader. Granting this permission
 *       would allow code to change which class loader is used for a particular service.
 *   </td>
 * </tr>
 *
 * <tr>
 *   <td>Service.getInternalService</td>
 *   <td>Access to the internal Service, Cluster or Cache reference</td>
 *   <td>This allows an attacker to obtain direct access to the underlying Service,
 *       Cluster or cache Storage implementation.
 *   </td>
 *
 * <tr>
 *   <td>Service.registerResource</td>
 *   <td>Registering a resource associated with a clustered service</td>
 *   <td>This allows an attacker to re-register or unregister various resources
 *       associated with the service.
 *   </td>
 *
 * <tr>
 *   <td>Service.registerEventInterceptor</td>
 *   <td>Registering an event interceptor for a cache service</td>
 *   <td>This is a dangerous permission to grant. This allows an attacker to change
 *       or remove event interceptors associated with the cache service thus either
 *       getting access to underlying data or removing live events that are designed
 *       to protect the data integrity.
 *   </td>
 * </tr>
 * <tr>
 *     <td>{@link com.tangosol.net.management.MBeanServerProxy#execute(com.tangosol.util.function.Remote.Function) MBeanServerProxy.execute}</td>
 *     <td>Execute a {@link com.tangosol.util.function.Remote.Function function} on the management node and return a serializable result.</td>
 *     <td>This is a dangerous permission to grant. This allows an attacker to execute code in management node.</td>
 * </tr>
 * </table>
 *
 * @author gg 2014.08.05
 * @since Coherence 12.2.1
 */
public class LocalPermission
        extends BasicPermission
    {
    /**
     * Create a new LocalPermission with the specified target name.
     *
     * @param sName the name of the LocalPermission
     */
    public LocalPermission(String sName)
        {
        super(sName);
        }


    // ----- constants for frequently used permissions -----------------------

    /**
     * "Service.getInternalService" permission.
     */
    public final static LocalPermission INTERNAL_SERVICE =
        new LocalPermission("Service.getInternalService");

    /**
     * "BackingMapManagerContext.getBackingMap" permission.
     */
    public final static LocalPermission BACKING_MAP =
        new LocalPermission("BackingMapManagerContext.getBackingMap");
    }
