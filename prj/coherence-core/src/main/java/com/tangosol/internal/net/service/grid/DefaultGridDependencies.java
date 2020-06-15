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
import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.cluster.DefaultServiceFailurePolicy;
import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.net.GuardSupport;
import com.tangosol.net.MemberListener;
import com.tangosol.util.Base;

import java.util.List;

/**
 * The base implementation of GridDependencies.
 *
 * @author bko 2013.05.15
 * @since Coherence 12.1.3
 */
public class DefaultGridDependencies
        extends DefaultServiceDependencies
        implements GridDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultGridDependencies object.
     */
    public DefaultGridDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultGridDependencies object, copying the values from the
     * specified GridDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultGridDependencies(GridDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_bldrActionPolicy         = deps.getActionPolicyBuilder();
            m_cGuardTimeoutMillis      = deps.getDefaultGuardTimeoutMillis();
            m_buildersMemberListener   = deps.getMemberListenerBuilders();
            m_bldrServiceFailurePolicy = deps.getServiceFailurePolicyBuilder();
            m_sTransport               = deps.getReliableTransport();
            }
        }

    // ----- GridDependencies interface ----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicyBuilder getActionPolicyBuilder()
        {
        return m_bldrActionPolicy;
        }

    /**
     * Set the ActionPolicyBuilder.
     * <p>
     * NOTE: This method is not directly @Injectable here as it is up to
     * sub-classes override define an appropriate property name for injection.
     *
     * @param builder  the ActionPolicyBuilder
     */
    public void setActionPolicyBuilder(ActionPolicyBuilder builder)
        {
        m_bldrActionPolicy = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDefaultGuardTimeoutMillis()
        {
        return m_cGuardTimeoutMillis;
        }

    /**
     * Set the default guardian timeout.
     *
     * @param cMillis  the guardian timeout
     */
    @Injectable("guardian-timeout")
    public void setDefaultGuardTimeoutMillis(long cMillis)
        {
        m_cGuardTimeoutMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ParameterizedBuilder<MemberListener>> getMemberListenerBuilders()
        {
        return m_buildersMemberListener;
        }

    /**
     * Set the MemberListener builder list.
     *
     * @param listBuilder  the MemberListener builder  list
     */
    @Injectable("member-listener")
    public void setMemberListenerBuilders(List<ParameterizedBuilder<MemberListener>> listBuilder)
        {
        m_buildersMemberListener = listBuilder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceFailurePolicyBuilder getServiceFailurePolicyBuilder()
        {
        return m_bldrServiceFailurePolicy;
        }

    /**
     * Set the ServiceFailurePolicy.
     *
     * @param builder  the ServiceFailurePolicyBuilder
     */
    @Injectable("service-failure-policy")
    public void setServiceFailurePolicyBuilder(ServiceFailurePolicyBuilder builder)
        {
        m_bldrServiceFailurePolicy = builder;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReliableTransport()
        {
        return m_sTransport;
        }

    /**
     * Set the name of the reliable transport to be used by this service.
     *
     * @param sTransport  the transport name
     */
    @Injectable("reliable-transport")
    public void setReliableTransport(String sTransport)
        {
        m_sTransport = sTransport;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{ActionPolicyBuilder=" + getActionPolicyBuilder() + ", GuardianTimeout="
               + getDefaultGuardTimeoutMillis() + ", MemberListenerBuilders=" + getMemberListenerBuilders()
               + ", ServiceFailurePolicyBuilder=" + getServiceFailurePolicyBuilder() + ", Transport=" + getReliableTransport() + "}";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultGridDependencies validate()
        {
        super.validate();

        Base.azzert(getActionPolicyBuilder() != null, "ActionPolicyBuilder can not be null");

        validateGuardTimeout();

        return this;
        }

    /**
     * Validate the guardian timeout.  If is not valid then use a default value and
     * change the service guardian policy to logging.
     */
    protected void validateGuardTimeout()
        {
        long cTimeoutMillis = getDefaultGuardTimeoutMillis();

        if (cTimeoutMillis > 0)
            {
            // default member-wide guardian timeout is overridden for this
            // service; reset the default guard timeout for this service and
            // its dependent threads (e.g. worker, write-behind)
            //
            // Note: service-startup is guarded by the cluster-service's
            // guardian SLA.  Only after the service is started, is the SLA
            // for the service thread updated.
            cTimeoutMillis = Math.max(cTimeoutMillis, GuardSupport.GUARDIAN_MAX_CHECK_INTERVAL);
            }
        else if (cTimeoutMillis == 0)
            {
            // COH-3090 fix:
            // the user explicitly set the timeout to 0, so set the policy to logging
            // and use the Cluster default timeout (see Grid.onDependencies)
            setServiceFailurePolicyBuilder(new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_LOGGING));
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ActionPolicyBuilder.
     */
    private ActionPolicyBuilder m_bldrActionPolicy = new ActionPolicyBuilder.NullImplementationBuilder();

    /**
     * The Guardian timeout. NOTE: Cluster.bindService() sets the default Guardian
     * timeout and policy for all services during startup using the Cluster settings.
     * Initialize the value to -1 so if there is no explicitly configured timeout in
     * the configuration, the cluster level setting will be kept (see validateGuardTimeout).
     */
    private long m_cGuardTimeoutMillis = -1;

    /**
     * The MemberListener Builders list.
     */
    private List<ParameterizedBuilder<MemberListener>> m_buildersMemberListener;

    /**
     * The ServiceFailurePolicyBuilder.
     */
    private ServiceFailurePolicyBuilder m_bldrServiceFailurePolicy;

    /**
     * The service's reliable transport.
     */
    private String m_sTransport;
    }
