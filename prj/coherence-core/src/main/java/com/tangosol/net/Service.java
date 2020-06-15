/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.tangosol.io.Serializer;

import com.tangosol.util.ResourceRegistry;


/**
* This Service interface represents a controllable service that operates in a
* clustered network environment.
*
* @author gg  2002.02.08
*
* @since Coherence 1.1
*/
public interface Service
        extends com.tangosol.util.Service
    {
    /**
    * Return the Cluster object that this Service is a part of.
    *
    * @return the Cluster object
    */
    public Cluster getCluster();

    /**
    * Return the ServiceInfo object for this Service.
    *
    * @return the ServiceInfo object
    */
    public ServiceInfo getInfo();

    /**
    * Add a Member listener.
    * <p>
    * MemberListeners will be invoked in the order in which they are registered.
    *
    * @param listener  the {@link MemberListener} to add
    */
    public void addMemberListener(MemberListener listener);

    /**
    * Remove a Member listener.
    *
    * @param listener  the {@link MemberListener} to remove
    */
    public void removeMemberListener(MemberListener listener);

    /**
    * Return the user context object associated with this Service.
    * <p>
    * The data type and semantics of this context object are entirely
    * application specific and are opaque to the Service itself.
    *
    * @return an associated user context object or null if a context
    *          has not been set
    *
    * @since Coherence 3.0
    */
    public Object getUserContext();

    /**
    * Associate a user context object with this Service.
    *
    * @param oCtx  a user context object
    *
    * @since Coherence 3.0
    */
    public void setUserContext(Object oCtx);

    /**
    * Return a Serializer used by this Service.
    *
    * @return the Serializer object
    *
    * @since Coherence 3.4
    */
    public Serializer getSerializer();

    /**
    * Configure the Service.
    * <p>
    * This method can only be called before the Service is started.
    *
    * @param deps  the dependencies object carrying configuration information
    *            specific to this Service
    *
    * @throws IllegalStateException thrown if the Service is already running
    * @throws IllegalArgumentException thrown if the configuration information
    *         is invalid
    *
    * @since Coherence 12.1.3
    */
    public void setDependencies(ServiceDependencies deps);

    /**
     * Return the service's dependencies.
     *
     * @return the service's dependencies
     *
     * @since Coherence 12.2.1
     */
    public ServiceDependencies getDependencies();

     /**
    * Retrieves a Service scoped {@link ResourceRegistry}. The resource
    * registry is used to:
    * <ul>
    *   <li>Register resources with the Service and make them accessible
    *   to the application code bound to this Service.
    *   <li>Dispose of resources when the Service is shut down; see
    *   the {@link ResourceRegistry} API for details on how to enable
    *   cleanup of resources.
    * </ul>
    *
    * @return a Service scoped resource registry
    *
    * @since Coherence 12.2.1
    */
    public ResourceRegistry getResourceRegistry();

    // ----- inner interface: MemberJoinAction ----------------------------

    /**
    * MemberJoinAction is taken to allow a new member to join a clustered
    * Service.
    * <p>
    * A MemberJoinAction object is valid <i>only</i> for the duration of an
    * invocation to {@link ActionPolicy#isAllowed}.
    */
    public interface MemberJoinAction
            extends Action
        {
        /**
        * Return the Member that is attempting to join the service.
        *
        * @return the Member that is attempting to join the service
        */
        public Member getJoiningMember();
        }
    }
