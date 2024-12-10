/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.internal.net.service.ServiceDependencies;

import com.tangosol.net.MemberListener;
import com.tangosol.net.ServiceFailurePolicy;

import java.util.List;

/**
 * The GridDependencies interface provides a Grid component with its external dependencies.
 *
 * @author pfm  2011.05.12
 * @since Coherence 12.1.2
 */
public interface GridDependencies
        extends ServiceDependencies
    {
    /**
     * Return the ActionPolicyBuilder which is a pluggable policy builder that defines certain aspects
     * of a service's behavior at runtime.
     *
     * @return the ActionPolicyBuilder for the service
     */
    public ActionPolicyBuilder getActionPolicyBuilder();

    /**
     * Return the default guardian timeout for this service.  This value is used to guard
     * against deadlocked or unresponsive worker threads used by this service and the
     * service itself.
     *
     * @return the guardian timeout
     */
    public long getDefaultGuardTimeoutMillis();

    /**
     * Return the MemberListeners which listen for MemberEvents.
     *
     * @return the MemberListener list
     */
    public List<ParameterizedBuilder<MemberListener>> getMemberListenerBuilders();

    /**
     * Return the ServiceFailurePolicyBuilder.  If the ServiceFailurePolicyBuilder is
     * null then the cluster policy will be used.
     *
     * @return the ServiceFailurePolicyBuilder
     */
    public ServiceFailurePolicyBuilder getServiceFailurePolicyBuilder();

    /**
     * Return the name of the reliable transport used by this service.
     *
     * @return the reliable transport name, or null to use the node's default
     */
    public String getReliableTransport();
    }
