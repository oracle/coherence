/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.net.Cluster;
import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;

import java.util.Set;


/**
 * A PartitionAssignmentStrategy used by a service to attempt to co-locate the
 * primary ownership of partitions on the same members as another service.  This
 * strategy does not guarantee that partitions will be co-located, but will make
 * a best-effort attempt.
 *
 * @author rhl 2011.06.29
 * @since  Coherence 3.7.1
 */
public class MirroringAssignmentStrategy
        extends SimpleAssignmentStrategy
    {
    // ----- constructors -------------------------------------------------
    /**
     * Construct a MirroringAssignmentStrategy to be associated the specified
     * service.
     *
     * @param sService  the name of the associated service
     */
    public MirroringAssignmentStrategy(String sService)
        {
        m_sService = sService;
        }


    // ----- accessors ----------------------------------------------------

    /**
     * Return the partitioned service for which this assignment strategy
     * controls the distribution.
     *
     * @return the partitioned service controlled by this assignment strategy
     */
    public PartitionedService getService()
        {
        return getManager().getService();
        }

    /**
     * Set the associated service.
     *
     * @param service  the associated service
     */
    protected void setAssociatedService(PartitionedService service)
        {
        m_service = service;
        }

    /**
     * Return the associated service.
     *
     * @return the associated service
     */
    public PartitionedService getAssociatedService()
        {
        return m_service;
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Validate that the distribution for the specified service is compatible
     * with this service.
     *
     * @param service  the service to validate
     *
     * @return true iff the specified service is compatible
     */
    protected boolean validateAssociatedService(PartitionedService service)
        {
        PartitionedService serviceThis = getService();

        return serviceThis.getPartitionCount() == service.getPartitionCount() &&
            serviceThis.getBackupCount() == service.getBackupCount();
        }

    /**
     * Update the analysis context to reflect the partition assignments of the
     * specified associated service.
     *
     * @param ctx          the analysis context
     * @param serviceThat  the associated service whose partition assignments to sync
     */
    protected void syncAssignments(AnalysisContext ctx, PartitionedService serviceThat)
        {
        Set setOwnersThis = getService().getOwnershipEnabledMembers();
        int cPartitions   = serviceThat.getPartitionCount();
        int cBackups      = serviceThat.getBackupCount();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                Member ownerThat = iStore == 0
                    ? serviceThat.getPartitionOwner(iPart)
                    : serviceThat.getBackupOwner(iPart, iStore);

                if (ownerThat != null && !setOwnersThis.contains(ownerThat))
                    {
                    if (iStore == 0)
                        {
                        // only sync the assignments if the primary owner is a
                        // member of both services.
                        break;
                        }

                    ownerThat = null;
                    }

                Ownership ownersThis = ctx.getPartitionOwnership(iPart);
                Member    ownerCurr  = getMember(ownersThis.getOwner(iStore));
                if (ownerThat != null && ownerCurr != ownerThat)
                    {
                    ctx.transitionPartition(iPart, iStore, ownerCurr, ownerThat);
                    }
                }
            }
        }

    /**
     * Bind this assignment strategy to the specified partitioned service, and
     * return the bound service.
     *
     * @param sService  the name of the service to bind
     *
     * @return the partitioned service
     */
    protected PartitionedService bindService(String sService)
        {
        Cluster            cluster     = getService().getCluster();
        PartitionedService serviceThat = (PartitionedService) cluster.getService(sService);

        serviceThat = serviceThat == null || !validateAssociatedService(serviceThat)
            ? null : serviceThat;

        setAssociatedService(serviceThat);
        return serviceThat;
        }

    /**
     * Return true iff the partition assignments sync'd from the specified
     * associated service should be further refined/balanced by the local
     * assignment strategy.
     *
     * @param ctx          the analysis context
     * @param serviceThis  the local service
     * @param serviceThat  the associated service (may be null if unbound)
     *
     * @return true iff the partition assignments should be further refined
     */
    protected boolean isRefinementNeeded(
            AnalysisContext ctx, PartitionedService serviceThis, PartitionedService serviceThat)
        {
        // if the ownership member sets for the 2 services match and there was
        // not any concurrent membership change to the associated service, then
        // use the associated service's assignments as-is

        return serviceThat == null ||
            !getManager().getOwnershipLeavingMembers().isEmpty() ||
            !serviceThis.getOwnershipEnabledMembers().equals(
                    serviceThat.getOwnershipEnabledMembers());
        }


    // ----- SimpleAssignmentStrategy methods -----------------------------

    /**
     * {@inheritDoc}
     */
    public void init(DistributionManager manager)
        {
        super.init(manager);

        bindService(m_sService);
        }

    /**
     * {@inheritDoc}
     */
    public String getDescription()
        {
        String sDesc = super.getDescription();
        if (sDesc.length() > 0)
            {
            sDesc += ", ";
            }

        PartitionedService serviceThat = getAssociatedService();
        sDesc += "AssociatedService=" + m_sService +
            (serviceThat == null
                 ? "(unbound)"
                 : serviceThat.isRunning()
                     ? "(RUNNING)" : "(STOPPED)");

        return sDesc;
        }

    /**
     * {@inheritDoc}
     */
    protected long analyzeDistribution(AnalysisContext ctx)
        {
        PartitionedService serviceThis = getService();
        PartitionedService serviceThat = getAssociatedService();

        if (serviceThat == null || !serviceThat.isRunning())
            {
            serviceThat = bindService(m_sService);
            }

        if (serviceThat != null)
            {
            syncAssignments(ctx, serviceThat);
            }

        if (isRefinementNeeded(ctx, serviceThis, serviceThat))
            {
            // finalize the distribution by balancing
            return super.analyzeDistribution(ctx);
            }
        else
            {
            // no balancing required; submit the distribution suggestions
            ctx.suggestDistribution();
            return 2000L;
            }
        }


    // ----- data members -------------------------------------------------

    /**
     * The associated service that this strategy attempts to co-locate
     * partitions with.  May be null.
     */
    protected PartitionedService m_service;

    /**
     * The name of the associated service.
     */
    protected String m_sService;
    }