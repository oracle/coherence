/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.net.Member;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.ServiceInfo;

import com.tangosol.net.management.AnnotatedStandardEmitterMBean;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;
import com.tangosol.util.SubSet;
import com.tangosol.util.SynchronousListener;

import com.tangosol.util.comparator.ChainedComparator;
import com.tangosol.util.comparator.InverseComparator;

import com.tangosol.util.filter.AllFilter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NotFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.NotCompliantMBeanException;
import javax.management.Notification;


/**
 * SimpleAssignmentStrategy is a PartitionAssignmentStrategy that attempts to
 * balance the partition distribution based on the number of primary and backup
 * partitions owned.  The SimpleAssignmentStrategy will attempt to ensure
 * machine-safety, but only if a balanced "safe" distribution is achievable.
 * <p>
 * The SimpleAssignmentStrategy is an extensible implementation of the internal
 * distribution algorithm that was available prior to Coherence 3.7.
 *
 * @author rhl 2010.11.08
 * @since  Coherence 3.7
 */
public class SimpleAssignmentStrategy
        implements PartitionAssignmentStrategy, SimpleStrategyMBean
    {
    // ----- constructors -------------------------------------------------

    /**
     * Default constructor.
     */
    public SimpleAssignmentStrategy()
        {
        }


    // ----- accessors ----------------------------------------------------

    /**
     * Return the DistributionManager.
     *
     * @return the DistributionManager
     */
    public DistributionManager getManager()
        {
        return m_manager;
        }

    /**
     * Return the last AnalysisContext.
     *
     * @return the last AnalysisContext
     */
    public AnalysisContext getLastAnalysisContext()
        {
        return m_ctxLast;
        }

    /**
     * Set the last AnalysisContext.
     *
     * @param ctx  the AnalysisContext
     */
    protected void setLastAnalysisContext(AnalysisContext ctx)
        {
        m_ctxLast = ctx;
        }

    /**
     * Return the set of ownership-enabled members present when the analysis was
     * last considered.
     *
     * @return the set of ownership-enabled members when analysis was last
     *         considered
     */
    public Set getLastOwnershipMembers()
        {
        return m_setOwnersLast;
        }

    /**
     * Record the set of ownership-enabled members present when the analysis was
     * last considered.
     *
     * @param setOwners  the set of ownership-enabled members
     */
    protected void setLastOwnershipMembers(Set setOwners)
        {
        m_setOwnersLast = setOwners;
        }

    /**
     * Return the amount of time in ms to delay the analysis after a member has
     * joined.  This delay could be used to "dampen" the reactivity of the
     * strategy to membership changes.
     *
     * @return the amount of time in ms to delay the analysis after a member joins
     */
    protected long getMemberJoinDelay()
        {
        return 1000L;
        }

    /**
     * Return the amount of time in ms to delay the analysis.
     *
     * @return the amount of time in ms to delay the analysis
     */
    protected long getSuggestionDelay()
        {
        return 60000L;
        }

    /**
     * Return the amount of time in ms to delay the analysis after a
     * distribution suggestion has been made and before it is carried out.  This
     * delay could be used to "dampen" the volatility of the strategy by
     * allowing sufficient time for in-flight transfers to complete prior to
     * considering further recommendations.
     *
     * @return the amount of time in ms to delay the analysis after a suggestion
     *         is made
     */
    protected long getSuggestionCompletionDelay()
        {
        return m_cPlanCompletionDelay;
        }


    // ----- PartitionAssignmentStrategy methods --------------------------

    /**
     * {@inheritDoc}
     */
    public void init(DistributionManager manager)
        {
        class ServiceStoppedListener
                implements ServiceListener, SynchronousListener
            {
            public void serviceStarting(ServiceEvent evt) {}

            public void serviceStarted(ServiceEvent evt) {}

            public void serviceStopping(ServiceEvent evt) {}

            public void serviceStopped(ServiceEvent evt)
                {
                unregisterMBean();
                }
            }

        m_manager              = manager;
        m_cPlanCompletionDelay = getPartitionCount() < 1 << 14 ? 60_000L : 300_000L;

        registerMBean();
        manager.getService().addServiceListener(new ServiceStoppedListener());
        }

    /**
     * {@inheritDoc}
     */
    public void analyzeOrphans(final Map<Member, PartitionSet> mapConstraints)
        {
        AnalysisContext ctx = instantiateAnalysisContext();

        int          cPartitions = getPartitionCount();
        PartitionSet partsLost   = new PartitionSet(cPartitions);
        Member[]     aMember     = ctx.getOwnershipMembersList();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            Ownership owners = ctx.getPartitionOwnership(iPart);
            int       nOwner = owners.getPrimaryOwner();
            if (nOwner == 0)
                {
                Comparator comparator =
                    chainComparators(ctx.instantiateStrengthComparator(owners),
                                     ctx.instantiateLoadComparator(true),
                                     ctx.instantiateDefaultComparator());

                // this partition is orphaned; assemble the set of members which
                // it could be assigned to ordered by strength and load
                int iPartition = iPart;
                int cMembers   = filterSort(aMember, comparator,
                        member ->
                           {
                           PartitionSet parts = mapConstraints.get(member);
                           return parts != null && parts.contains(iPartition);
                           });

                if (cMembers == 0)
                    {
                    // nothing to recover from; simply balance the assignments
                    partsLost.add(iPart);

                    cMembers = filterSort(aMember, comparator, AlwaysFilter.INSTANCE);
                    }

                if (cMembers > 0)
                    {
                    ctx.transitionPartition(iPart, 0, null, aMember[0]);
                    }
                }
            }

        ctx.suggestDistribution();

        if (!partsLost.isEmpty())
            {
            emitLossNotification(partsLost);

            // retain knowledge of partitions that were orphaned to prioritize
            // their distribution when aiming to achieve balance thus minimizing
            // transfer cost
            ctx.setOrphanedPartitions(partsLost);
            }

        // force an analysis of the ownership immediately following a partition
        // recovery or loss; we almost certainly need to move something around
        ctx.setAnalysisDelay(0L);
        setLastAnalysisContext(ctx);
        }

    /**
     * {@inheritDoc}
     */
    public long analyzeDistribution()
        {
        AnalysisContext ctx = instantiateAnalysisContext();

        ctx.copyTransients(getLastAnalysisContext());

        long cDelay = ctx.calculateAnalysisDelay();
        if (cDelay <= 0L)
            {
            cDelay = analyzeDistribution(ctx);

            ctx.resetTransients();
            }

        setLastAnalysisContext(ctx);

        // record the ownership member-set
        setLastOwnershipMembers(ctx.getOwnershipMembers());

        m_fRefresh = true;
        return cDelay;
        }

    /**
     * Analyze the distribution and suggest the resulting distribution
     * to distribution manager.
     *
     * @param ctx  the analysis context
     *
     * @return the time interval before the next desired analysis, or -1
     */
    protected long analyzeDistribution(AnalysisContext ctx)
        {
        // do a initial two server distribution to reduce the backup "fan out"
        primeDistribution(ctx);

        long cSuggestDelay = analyze(ctx);

        ctx.suggestDistribution();
        ctx.setCompletedTime(Base.getSafeTimeMillis());

        return cSuggestDelay;
        }

    /**
     * Optimize the distribution with minimum backup fan-out by distributing
     * the partitions among first two strong members.
     *
     * @param ctx  the analysis context
     */
    protected void primeDistribution(AnalysisContext ctx)
        {
        // If we delayed the initial (after the coordinator assigned all the
        // partitions to itself) distribution and have more than two new members
        // that have joined, the effect of an immediate call to "analyze" would
        // be a large "fan-out" of the backups. It is a result of the current
        // distribution algorithm and needs to be improved, but the scope ot
        // that project is rather large.
        //
        // In turn, the high backup fan-out results in a large number of backup
        // messages produced by any bulk operations that span multiple partitions
        // (see COH-14955).
        //
        // However, if we start with just two members that evenly divide the
        // partitions among themselves, the "fan-out" effect of the distribution
        // algorithm is dramatically lower. Hence our work around the fan-out
        // problem is to call the distribution analysis twice: first by pretending
        // that only two members exist and then proceed to the actual membership.

        Member memberCoordinator = getManager().getService().getCluster().getLocalMember();

        // first, quickly check that the current distribution is in the initial state -
        // the coordinator owns everything and no backups are owned

        if (!ctx.isInitialDistribution(memberCoordinator))
            {
            return;
            }

        if (ctx.getOwnershipMembers().size() <= 2 || ctx.getActualBackupCount() != 1)
            {
            return;
            }

        Member memberStrong = null;
        for (Member member : ctx.getOwnershipMembers())
            {
            if (ctx.isStrong(memberCoordinator, member))
                {
                memberStrong = member;
                break;
                }
            }

        // we must find a "strong" second, because the strength is based on
        // the context itself
        assert(memberStrong != null);

        // prime the distribution with a "two servers" scenario
        ctx.primeDistribution(memberCoordinator, memberStrong);

        analyze(ctx);

        // revert the context info to the original state
        ctx.initialize();
        }

    /**
     * Analyze the distribution using the specified analysis context.
     *
     * @param ctx  the analysis context
     *
     * @return the time interval before the next desired analysis, or -1
     */
    protected long analyze(AnalysisContext ctx)
        {
        long cSuggestDelay = getSuggestionDelay();

        checkLeaving(ctx);
        validateBackups(ctx);

        // as of Coherence 12.2.1.1, the two-server case with backup is treated
        // differently (SE One contract)
        if (m_fTrivialDistribution && checkSimpleDistribution(ctx))
            {
            return cSuggestDelay;
            }

        checkPrimaryBalance(ctx);

        BackupStrength strengthOrig  = null;

        if (ctx.getActualBackupCount() > 0)
            {
            checkEndangered(ctx);

            int cChanges;
            int cIters        = 0;
            int cIterMax      = 10;
            int cVariancePrev = getVariance(ctx, false);
            do
                {
                cChanges = 0;

                // redistribute until backup strength & balance are stable
                cChanges += checkBackupStrong (ctx);
                cChanges += checkBackupBalance(ctx);
                if (cChanges == 0)
                    {
                    // distribution may still not balanced; this should happen very rarely
                    cChanges = checkBackupOverloaded(ctx);
                    }

                if (cChanges == 0)
                    {
                    // reached fixed-point
                    break;
                    }

                // calculate the variance and make sure that we still making
                // progress toward the fixed-point
                int cVarianceCur = getVariance(ctx, false);
                if (cIters++ > cIterMax && cVarianceCur >= cVariancePrev)
                    {
                    if (strengthOrig == null)
                        {
                        // after cIterMax iterations, if we are not monotonically
                        // proceeding towards the fixed-point, slightly disturb the distribution
                        strengthOrig = ctx.getBackupStrength();
                        checkBackupOverloaded(ctx);
                        }
                    else
                        {
                        // after 10 iterations after the reshuffle, if we
                        // still couldn't reach the balance point, give up and decrease
                        // the strength as a fail-safe to prevent an infinite loop;
                        // reschedule the next analysis relatively soon and
                        // log a soft-assert, as this shouldn't really happen
                        Logger.err("Failed to find a partition assignment to satisfy "
                            + strengthOrig + " among the member-set "
                            + ctx.getOwnershipMembers()
                            + "; weakening the backup-strength");

                        ctx.setBackupStrength(strengthOrig.getWeaker());
                        strengthOrig  = null;
                        cSuggestDelay = 1000L;
                        }
                    cIters = 0;
                    }

                cVariancePrev = cVarianceCur;
                }
            while (true);
            }
        return cSuggestDelay;
        }

    /**
     * Analyze the distribution for the special two-server case using the
     * specified analysis context.
     *
     * @param ctx  the analysis context
     *
     * @return true if the simple distribution is in effect
     */
    protected boolean checkSimpleDistribution(AnalysisContext ctx)
        {
        DistributionManager manager = getManager();

        if (manager.getOwnershipMembers().size() > 2 || getBackupCount() == 0)
            {
            // more than two servers, or no backup is not SE One
            return m_fTrivialDistribution = false;
            }

        if (!ctx.getLeavingOwners().isEmpty() ||
                ctx.getOwnershipMembers().size() == 1)
            {
            // defer to the standard algorithm, but allow to come back
            return false;
            }

        Member member1 = manager.getService().getOwnershipSenior();
        Member member2 = null;

        for (Member member : ctx.getOwnershipMembers())
            {
            if (member != member1)
                {
                member2 = member;
                break;
                }
            }

        if (!ctx.getOwnedPartitions(member2, 0).isEmpty())
            {
            // we should never come here, but just in case...
            return m_fTrivialDistribution = false;
            }

        PartitionSet partsBackup2 = ctx.getOwnedPartitions(member2, 1);
        if (!partsBackup2.isFull())
            {
            // calculate the partitions that the backup member is not a backup owner yet
            // and suggest a backup transfer
            PartitionSet parts = new PartitionSet(partsBackup2);
            parts.invert();

            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                ctx.transitionPartition(iPart, 1, member1, member2);
                }
            }

        return true;
        }

    /**
     * {@inheritDoc}
     */
    public String getDescription()
        {
        AnalysisContext ctxLast = getLastAnalysisContext();
        StringBuilder   sb      = new StringBuilder();

        if (ctxLast != null)
            {
            sb.append("Fair-Share=").append(ctxLast.getFairShare(true))
              .append("(primary) ").append(ctxLast.getFairShare(false))
              .append("(backup)").append(", ")
              .append("Target Backup-Strength=")
              .append(ctxLast.getBackupStrength().getDescription());
            }

        return sb.toString();
        }


    // ----- Object methods -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '{' + getDescription() + '}';
        }


    // ----- internal -----------------------------------------------------

    /**
     * Check for any service members that are leaving, and adjust the
     * distribution plan accordingly.
     * <p>
     * Partitions owned by leaving members must be transferred to other members
     * before the departing members are able to shutdown.
     *
     * @param ctx  the AnalysisContext
     */
    protected void checkLeaving(AnalysisContext ctx)
        {
        Set      setLeaving     = ctx.getLeavingOwners();
        Member[] aOwners        = ctx.getOwnershipMembersList();
        int      cBackupsConfig = getBackupCount();

        if (setLeaving.isEmpty())
            {
            // no more departing (awaiting graceful exit pending transfer) members
            return;
            }

        // iterate the leaving members to transfer away the owned partitions
        for (Member memberLeaving : (Set<Member>) setLeaving)
            {
            // 1. transfer away the owned primary partitions
            PartitionSet partsPrime = ctx.getOwnedPartitions(memberLeaving, 0);
            partition_loop:
            for (int iPart = partsPrime.next(0); iPart >= 0;
                 iPart = partsPrime.next(iPart + 1))
                {
                Ownership owners = ctx.getPartitionOwnership(iPart);

                // There are 2 cases: either there is some other backup owner,
                // or the partition is endangered. If there is a backup owner,
                // promote it to primary.  Otherwise, pick the lightest member
                // to force the transfer to.
                for (int iStore = cBackupsConfig; iStore >= 1; iStore--)
                    {
                    int nOwner = owners.getOwner(iStore);
                    if (nOwner != 0)
                        {
                        Member memberTo = getMember(nOwner);

                        // make the previous backup member the new primary owner
                        ctx.transitionPartition(iPart, 0, memberLeaving, memberTo);

                        // endanger the storage-index previously owned
                        ctx.transitionPartition(iPart, iStore, memberTo, null);
                        continue partition_loop;
                        }
                    }

                // the partition had zero backups; force the transfer to the
                // most lightly loaded member
                Arrays.sort(aOwners, ctx.instantiateLoadComparator(true));
                ctx.transitionPartition(iPart, 0, memberLeaving, aOwners[0]);
                }

            // 2. set the backup partitions to "endangered" so that they will be
            //    handled by the next pass
            for (int iStore = 1; iStore <= cBackupsConfig; iStore++)
                {
                PartitionSet parts = ctx.getOwnedPartitions(memberLeaving, iStore);
                for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                    {
                    // set the previously owned storage-index to endangered
                    ctx.transitionPartition(iPart, iStore, memberLeaving, null);
                    }
                }
            }
        }

    /**
     * Check if there are enough ownership members to maintain the configured
     * backup count, reducing the number of backups otherwise.
     *
     * @param ctx  the AnalysisContext
     */
    protected void validateBackups(AnalysisContext ctx)
        {
        int cPartitions    = getPartitionCount();
        int cBackupsActual = ctx.getActualBackupCount();
        int cBackupsConfig = getBackupCount();

        // if departing (or departed) members leave us without enough members to
        // maintain full backups, prune the backup storage.
        //
        // Note: this logic emulates the automatic "pruning" performed by the
        //       PartitionedService.  See PartitionedService.validateBackupCount()
        if (cBackupsConfig != cBackupsActual)
            {
            for (int iPart = 0; iPart < cPartitions; iPart++)
                {
                Ownership owners = ctx.getPartitionOwnership(iPart);
                for (int iStore = 1, iStoreValid = 1; iStore <= cBackupsConfig; iStore++)
                    {
                    int nBackupOwner = owners.getOwner(iStore);
                    if (nBackupOwner != 0)
                        {
                        if (iStore > iStoreValid)
                            {
                            Member memberThis = getMember(nBackupOwner);

                            // move this backup index down from iStore to iStoreValid
                            ctx.transitionPartition(iPart, iStoreValid, null, memberThis);

                            // set the iStore storage index to endangered
                            ctx.transitionPartition(iPart, iStore, memberThis, null);
                            }

                        iStoreValid++;
                        }
                    }
                }
            }
        }

    /**
     * Check the distribution to ensure that primary the partition load is
     * balanced.
     *
     * @param ctx  the analysis context
     */
    protected void checkPrimaryBalance(AnalysisContext ctx)
        {
        Member[] aMembersOverload = ctx.getOwnershipMembersList();
        Member[] aMembersTarget   = aMembersOverload.clone();

        // repeat this until there are no more primary distributions to perform.
        // This will ensure a "refined" distribution.
        int cChanges;
        do
            {
            cChanges = 0;

            // sort the member list in decreasing order of primary partition load
            int cOverloaded = filterSort(
                    aMembersOverload,
                    new InverseComparator(ctx.instantiateLoadComparator(true)),
                    ctx.instantiateOverloadedFilter(true));
            for (int i = 0; i < cOverloaded; i++)
                {
                Member       memberFrom = aMembersOverload[i];
                PartitionSet partsAll   =
                    new PartitionSet(ctx.getOwnedPartitions(memberFrom, 0));

                // try orphaned partitions first
                PartitionSet partsOrphaned = ctx.collectOrphaned(partsAll);
                partsAll.remove(partsOrphaned);
                cChanges += doBalancePrimary(
                        ctx, memberFrom, partsOrphaned, aMembersTarget);

                // try endangered partitions first
                PartitionSet partsEndangered = ctx.collectEndangered(partsAll);
                partsAll.remove(partsEndangered);
                cChanges += doBalancePrimary(
                        ctx, memberFrom, partsEndangered, aMembersTarget);

                // next try vulnerable partitions
                PartitionSet partsWeak = ctx.collectWeak(partsAll);
                partsAll.remove(partsWeak);
                cChanges += doBalancePrimary(
                        ctx, memberFrom, partsWeak, aMembersTarget);

                // lastly, any partition
                cChanges += doBalancePrimary(
                        ctx, memberFrom, partsAll, aMembersTarget);
                }
            }
        while (cChanges > 0);
        }

    /**
     * Do balancing transfers for primary distribution.
     *
     * @param ctx             the analysis context
     * @param memberFrom      the member to transfer partitions from
     * @param parts           the set of partitions from which to transfer
     * @param aMembersTarget  the (unordered) array of members
     *
     * @return the number of changes (transfers) that were made
     */
    protected int doBalancePrimary(AnalysisContext ctx, Member memberFrom,
                                   PartitionSet parts, Member[] aMembersTarget)
        {
        int cFairShare      = ctx.getFairShare(true);
        int cLoadMemberFrom = ctx.getMemberLoad(memberFrom, true);
        int cChanges        = 0;

        for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
            {
            if (cLoadMemberFrom < cFairShare)
                {
                break;
                }

            // consider partitions to transfer
            Ownership owners = (Ownership) ctx.getPartitionOwnership(iPart).clone();
            int       cLoad  = ctx.getPartitionLoad(iPart, true);

            // clear the primary owner before evaluating potential replacements
            owners.setOwner(0, 0);

            // select the list of underloaded members, sorted by decreasing
            // strength (strongest-first)
            int cUnderloaded;
            try
                {
                cUnderloaded = filterSort(
                        aMembersTarget,
                        chainComparators(
                                ctx.instantiateStrengthComparator(owners),
                                ctx.instantiateLoadComparator(true),
                                ctx.instantiateDefaultComparator()),
                        ctx.instantiateUnderloadedFilter(true));
                }
            catch (Throwable t)
                {
                StringBuilder sb = new StringBuilder("Member array: [");
                for (Member member : aMembersTarget)
                    {
                    sb.append(member == null ? "null" : member.getId()).append(',');
                    }
                sb.replace(sb.length() - 1, sb.length(), "]")
                  .append("\nPartition Id: ").append(iPart);

                Logger.err(sb.toString());
                throw Base.ensureRuntimeException(t);
                }

            // find a new primary owner for this partition
            for (int i = 0; i < cUnderloaded; i++)
                {
                Member memberTo      = aMembersTarget[i];
                int    cLoadMemberTo = ctx.getMemberLoad(memberTo, true);

                // only if it balances load
                if (cLoadMemberTo + cLoad < cLoadMemberFrom)
                    {
                    ctx.transitionPartition(iPart, 0, memberFrom, memberTo);
                    cLoadMemberFrom -= cLoad;
                    ++cChanges;

                    break;
                    }
                }
            }

        return cChanges;
        }

    /**
     * Check the distribution to ensure that backups are created for any
     * "endangered" partitions.
     * <p>
     * A partition is "endangered" if it is incompletely backed up (e.g. some
     * backup copies do not exist).
     *
     * @param ctx  the analysis context
     */
    protected void checkEndangered(AnalysisContext ctx)
        {
        int      cBackups    = ctx.getActualBackupCount();
        int      cPartitions = getPartitionCount();
        Member[] aMember     = ctx.getOwnershipMembersList();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            Ownership owners        = ctx.getPartitionOwnership(iPart);
            Member    memberPrimary = getMember(owners.getPrimaryOwner());

            Base.azzert(memberPrimary != null); // there shouldn't be any orphans

            for (int iStore = 1; iStore <= cBackups; iStore ++)
                {
                if (owners.getOwner(iStore) != 0)
                    {
                    continue;
                    }

                // sort the member array and select the safe members, ordered by
                // strength and load
                int cSafe = filterSort(
                        aMember,
                        chainComparators(ctx.instantiateStrengthComparator(owners),
                                         ctx.instantiateLoadComparator(false),
                                         ctx.instantiateDefaultComparator()),
                        ctx.instantiateNotOwnedFilter(owners));

                Base.azzert(cSafe > 0,
                            "Failed to find a member to receive backup(" +
                            iStore + ") transfer of endangered partition " +
                            iPart + ", " + owners);

                ctx.transitionPartition(iPart, iStore, null, aMember[0]);
                }
            }
        }

    /**
     * Check that the backups are strong.
     *
     * @param ctx  the analysis context
     *
     * @return the number of changes (transfers) that were made
     */
    protected int checkBackupStrong(AnalysisContext ctx)
        {
        int      cPartitions    = getPartitionCount();
        int      cBackups       = ctx.getActualBackupCount();
        int      cChanges       = 0;
        Member[] aMembersTarget = ctx.getOwnershipMembersList();

        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            for (int iStore = 1; !ctx.isPartitionStrong(iPart) && iStore <= cBackups; iStore++)
                {
                // try to find a new strong backup owner for the specified partition
                Ownership owners     = (Ownership) ctx.getPartitionOwnership(iPart).clone();
                Member    memberFrom = getMember(owners.getOwner(iStore));

                // clear the backup owner before evaluating potential replacements
                owners.setOwner(iStore, 0);

                // pre-filter the member array for safety and determine if full
                // "safety" is achievable
                int cCandidate = filterArray(aMembersTarget, ctx.instantiateSafetyFilter(owners, iStore));
                if (cCandidate == 0)
                    {
                    // No member will produce a fully safe configuration after
                    // a single transfer.  This is possible if, for example, the
                    // topology is:
                    // Machine1:  1, 2, 3
                    // Machine2:  4, 5, 6
                    // Machine3:  7, 8, 9
                    //
                    // If a partition starts with ownership (4,5,6), no single
                    // transfer will render it "safe".
                    cCandidate = aMembersTarget.length;
                    }

                // first-pass: find the "strongest" safe, underloaded member
                int cUnderloaded = filterSort(
                        aMembersTarget,
                        cCandidate,
                        chainComparators(ctx.instantiateStrengthComparator(owners),
                                         ctx.instantiateLoadComparator(false),
                                         ctx.instantiateDefaultComparator()),
                        new AllFilter(new Filter[]
                                {
                                // Note: array is pre-filtered for safe members
                                ctx.instantiateUnderloadedFilter(false),
                                ctx.instantiateNotOwnedFilter(owners)
                                }));
                if (cUnderloaded > 0)
                    {
                    ctx.transitionPartition(iPart, iStore, memberFrom, aMembersTarget[0]);
                    ++cChanges;
                    continue;
                    }

                // second-pass: no strong underloaded members; find the least
                //              overloaded safe member
                int cOverloaded = filterSort(
                        aMembersTarget,
                        cCandidate,
                        chainComparators(ctx.instantiateLoadComparator(false),
                                         ctx.instantiateStrengthComparator(owners),
                                         ctx.instantiateDefaultComparator()),
                        new AllFilter(new Filter[]
                                {
                                // Note: array is pre-filtered for safe members
                                ctx.instantiateOverloadedFilter(false),
                                ctx.instantiateNotOwnedFilter(owners)
                                }));
                if (cOverloaded > 0)
                    {
                    ctx.transitionPartition(iPart, iStore, memberFrom, aMembersTarget[0]);
                    ++cChanges;
                    continue;
                    }
                }
            }

        return cChanges;
        }

    /**
     * Check that the distribution of backup partitions is balanced.
     *
     * @param ctx  the analysis context
     *
     * @return the number of changes (transfers) that were made
     */
    protected int checkBackupBalance(final AnalysisContext ctx)
        {
        int      cBackups         = ctx.getActualBackupCount();
        Member[] aMembersOverload = ctx.getOwnershipMembersList();
        Member[] aMembersTarget   = aMembersOverload.clone();
        int      cFairShare       = ctx.getFairShare(false);
        int      cChanges         = 0;

        // sort the overloaded members in decreasing backup load order
        int cOverloaded = filterSort(
                aMembersOverload,
                new InverseComparator(ctx.instantiateLoadComparator(false)),
                ctx.instantiateOverloadedFilter(false));

        member_loop: for (int i = 0; i < cOverloaded; i++)
            {
            Member memberFrom      = aMembersOverload[i];
            int    cLoadMemberFrom = ctx.getMemberLoad(memberFrom, false);

            for (int iStore = 1; iStore <= cBackups; iStore++)
                {
                PartitionSet parts = ctx.getOwnedPartitions(memberFrom, iStore);
                for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                    {
                    int       cLoad  = ctx.getPartitionLoad(iPart, false);
                    Ownership owners = (Ownership) ctx.getPartitionOwnership(iPart).clone();

                    // clear the backup owner before evaluating potential replacements
                    owners.setOwner(iStore, 0);

                    // select the underloaded and strong members, ordered by load
                    int cUnderloaded = filterSort(
                            aMembersTarget,
                            chainComparators(ctx.instantiateLoadComparator(false),
                                             ctx.instantiateStrengthComparator(owners),
                                             ctx.instantiateDefaultComparator()),
                            new AllFilter(new Filter[]
                                    {
                                    ctx.instantiateSafetyFilter(owners, iStore),
                                    ctx.instantiateUnderloadedFilter(false),
                                    ctx.instantiateNotOwnedFilter(owners)
                                    }));

                    for (int j = 0; j < cUnderloaded; j++)
                        {
                        Member memberTo      = aMembersTarget[j];
                        int    cLoadMemberTo = ctx.getMemberLoad(memberTo, false);

                        // only if it balances load
                        if (cLoadMemberTo + cLoad < cLoadMemberFrom)
                            {
                            ctx.transitionPartition(iPart, iStore, memberFrom, memberTo);
                            cLoadMemberFrom -= cLoad;
                            ++cChanges;

                            break;
                            }
                        }

                    if (cLoadMemberFrom < cFairShare)
                        {
                        continue member_loop;
                        }
                    }
                }
            }

        return cChanges;
        }

    /**
     * Check if the distribution of backup partitions is balanced.  If not,
     * disturb the distribution by moving a partition from the overloaded member
     * to another member that retains partition strength.
     *
     * @param ctx  the analysis context
     *
     * @return the number of unbalanced partitions that need to be transferred
     */
    protected int checkBackupOverloaded(final AnalysisContext ctx)
        {
        Member[] aMembers         = (Member[]) Base.randomize(ctx.getOwnershipMembersList());
        int      cFairShareBackup = ctx.getFairShare(false);
        int      cBackup          = getBackupCount();
        int      cOverload        = 0;
        Member   memberOverloaded = null;

        for (int i = 0; i < aMembers.length; i++)
            {
            Member memberFrom = aMembers[i];

            int cBackupLoad = ctx.getMemberLoad(memberFrom, false);
            int cLoad       = cBackupLoad - cFairShareBackup;

            if (cLoad > 0)
                {
                cOverload += cLoad;

                memberOverloaded = memberFrom;
                break;
                }
            }

        if (memberOverloaded != null)
            {
            PartitionSet partsBackup = new PartitionSet(getPartitionCount());
            for (int iStore = 1; iStore <= cBackup; iStore++)
                {
                partsBackup.add(ctx.getOwnedPartitions(memberOverloaded, iStore));
                }

            member_loop:  for (int i = 0; i < aMembers.length; i++)
                {
                Member memberTo = aMembers[i];

                if (memberOverloaded != memberTo)
                    {
                    PartitionSet partOwned = ctx.getOwnedPartitions(memberTo, 0);
                    for (int iStore = 1; iStore <= cBackup; iStore++)
                        {
                        if (!partOwned.intersects(partsBackup) && ctx.isStrong(memberOverloaded, memberTo))
                            {
                            ctx.transitionPartition(partsBackup.next(0), iStore, memberOverloaded, memberTo);

                            break member_loop;
                            }
                        }
                    }
                }
            }

        return cOverload;
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Return the PartitionedService Member with the specified mini-id.
     *
     * @param nMemberId  the mini-id
     *
     * @return the PartitionedService Member with the specified mini-id, or null
     */
    protected Member getMember(int nMemberId)
        {
        return getManager().getMember(nMemberId);
        }

    /**
     * Return the maximum load variance in the partition assignments represented
     * by the analysis context.  The maximum variance is the difference in load
     * between the 'lightest' and 'heaviest' members.
     *
     * @param ctx       the analysis context
     * @param fPrimary  true iff the "primary" load variance should be computed
     *
     * @return the maximum variance
     */
    protected int getVariance(AnalysisContext ctx, boolean fPrimary)
        {
        // select the list of underloaded members, sorted by decreasing
        // strength (strongest-first)
        Member[] aMembers = ctx.getOwnershipMembersList();
        int      cMembers = aMembers.length;
        Arrays.sort(aMembers, 0, cMembers,
                    chainComparators(
                            ctx.instantiateLoadComparator(fPrimary),
                            ctx.instantiateDefaultComparator()));

        return ctx.getMemberLoad(aMembers[cMembers - 1], fPrimary) -
               ctx.getMemberLoad(aMembers[0], fPrimary);
        }

    /**
     * Helper method to return a Comparator chaining the specified comparators.
     *
     * @param comp1  the first comparator
     * @param comp2  the second comparator
     *
     * @return a chained comparator
     */
    protected static Comparator chainComparators(Comparator comp1, Comparator comp2)
        {
        return new ChainedComparator(comp1, comp2);
        }

    /**
     * Helper method to return a Comparator chaining the specified comparators.
     *
     * @param comp1  the first comparator
     * @param comp2  the second comparator
     * @param comp3  the third comparator
     *
     * @return a chained comparator
     */
    protected static Comparator chainComparators(
            Comparator comp1, Comparator comp2, Comparator comp3)
        {
        return new ChainedComparator(comp1, comp2, comp3);
        }

    /**
     * Filter the elements in the specified array and sort any matching elements
     * using the specified comparator.  All matching results will be compacted to
     * the front of the array.  The order of results not matching the filter is
     * undefined.
     *
     * @param ao          the object array to sort and filter
     * @param comparator  the comparator to order the elements
     * @param filter      the filter to use to filter the results
     *
     * @return the number of elements matching the specified filter
     */
    protected static int filterSort(Object[] ao, Comparator comparator, Filter filter)
        {
        return filterSort(ao, ao.length, comparator, filter);
        }

    /**
     * Filter the specified array elements and sort any matching elements
     * using the specified comparator.  All matching results will be compacted to
     * the front of the array.  The order of results not matching the filter is
     * undefined.
     *
     * @param ao          the object array to sort and filter
     * @param cElems      the number of elements to filter and sort
     * @param comparator  the comparator to order the elements
     * @param filter      the filter to use to filter the results
     *
     * @return the number of elements matching the specified filter
     */
    protected static int filterSort(Object[] ao, int cElems, Comparator comparator, Filter filter)
        {
        cElems = filterArray(ao, cElems, filter);
        if (cElems > 1)
            {
            Arrays.sort(ao, 0, cElems, comparator);
            }

        return cElems;
        }

    /**
     * Apply the specified filter to the elements of the specified array.  All
     * matching results will be compacted to the front of the array in a "stable"
     * manner.  The order of results not matching the filter may not be
     * preserved.
     *
     * @param ao      the object array to apply the filter to
     * @param filter  the filter to apply
     *
     * @return the number of elements matching the specified filter
     */
    protected static int filterArray(Object[] ao, Filter filter)
        {
        return filterArray(ao, ao.length, filter);
        }

    /**
     * Apply the specified filter to the specified array elements.  All
     * matching results will be compacted to the front of the array in a "stable"
     * manner.  The order of results not matching the filter may not be
     * preserved.
     *
     * @param ao      the object array to apply the filter to
     * @param cElems  the number of elements to filter
     * @param filter  the filter to apply
     *
     * @return the number of elements matching the specified filter
     */
    protected static int filterArray(Object[] ao, int cElems, Filter filter)
        {
        int iShift = 0;
        for (int i = 0; i < cElems; i++)
            {
            if (!filter.evaluate(ao[i]))
                {
                ++iShift;
                }
            else if (iShift > 0)
                {
                Object oTemp = ao[i - iShift];
                ao[i - iShift] = ao[i];
                ao[i] = oTemp;
                }
            }

        return cElems - iShift;
        }


    // ----- SimpleStrategyMBean interface ----------------------------------

    /**
     * {@inheritDoc}
     */
    public int getPartitionCount()
        {
        return getManager().getService().getPartitionCount();
        }

    /**
     * {@inheritDoc}
     */
    public int getBackupCount()
        {
        return getManager().getService().getBackupCount();
        }

    /**
     * {@inheritDoc}
     */
    public int getServiceNodeCount()
        {
        return getManager().getService().getOwnershipEnabledMembers().size();
        }

    /**
     * {@inheritDoc}
     */
    public int getServiceMachineCount()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? 0 : ctx.getBackupStrength().getMachineCount();
        }

    /**
     * {@inheritDoc}
     */
    public int getServiceRackCount()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? 0 : ctx.getBackupStrength().getRackCount();
        }

    /**
     * {@inheritDoc}
     */
    public int getServiceSiteCount()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? 0 : ctx.getBackupStrength().getSiteCount();
        }

    /**
     * {@inheritDoc}
     */
    public String getHAStatus()
        {
        // TODO: move this functionality into a helper class
        try
            {
            return (String) ClassHelper.invoke(getManager().getService(),
                "getBackupStrengthName", ClassHelper.VOID_PARAMS);
            }
        catch (Exception e)
            {
            return "<unknown>";
            }
        }

    /**
     * {@inheritDoc}
     */
    public int getHAStatusCode()
        {
        try
            {
            return (int) ClassHelper.invoke(getManager().getService(),
                "getBackupStrength", ClassHelper.VOID_PARAMS);
            }
        catch (Exception e)
            {
            return -1;
            }
        }

    /**
     * {@inheritDoc}
     */
    public String getHATarget()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? MSG_NO_RESULT : ctx.getBackupStrength().getDescription();
        }

    /**
     * {@inheritDoc}
     */
    public int getFairShareBackup()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? 0 : ctx.getFairShare(false);
        }

    /**
     * {@inheritDoc}
     */
    public int getFairSharePrimary()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? 0 : ctx.getFairShare(true);
        }

    /**
     * {@inheritDoc}
     */
    public String getStrategyName()
        {
        return getClass().getSimpleName();
        }

    /**
     * {@inheritDoc}
     */
    public Date getLastAnalysisTime()
        {
        AnalysisContext ctx = getLastAnalysisContext();
        return ctx == null ? new Date(0) : new Date(ctx.getCompletedTime());
        }

    /**
     * {@inheritDoc}
     */
    public int getCoordinatorId()
        {
        try
            {
            return getManager().getService().getCluster().getLocalMember().getId();
            }
        catch (NullPointerException e)
            {
            return 0;
            }
        }

    /**
     * {@inheritDoc}
     */
    public int getRemainingDistributionCount()
        {
        int                         cScheduled   = 0;
        Map<Member, PartitionSet[]> mapScheduled = collectScheduledDistributions();

        for (Map.Entry<Member, PartitionSet[]> entry : mapScheduled.entrySet())
            {
            for (PartitionSet parts : entry.getValue())
                {
                cScheduled += parts == null ? 0 : parts.cardinality();
                }
            }

        return cScheduled;
        }

    /**
     * {@inheritDoc}
     */
    public long getAveragePartitionSizeKB()
        {
        return updateCompositeStats().getAveragePartitionSize();
        }

    /**
     * {@inheritDoc}
     */
    public long getMaxPartitionSizeKB()
        {
        return updateCompositeStats().getMaxPartitionSize();
        }

    /**
     * {@inheritDoc}
     */
    public long getMaxStorageSizeKB()
        {
        return updateCompositeStats().getMaxStorageSize();
        }

    /**
     * {@inheritDoc}
     */
    public long getAverageStorageSizeKB()
        {
        return updateCompositeStats().getAverageStorageSize();
        }

    /**
     * {@inheritDoc}
     */
    public int getMaxLoadNodeId()
        {
        return updateCompositeStats().getMaxLoadNodeId();
        }

    /**
     * Return a JMXPartitionStats that contains calculated MBean Attributes, updated
     * periodically.
     *
     * @return JMXPartitionStats with calculated MBean Attributes
     */
    protected JMXPartitionStats updateCompositeStats()
        {
        JMXPartitionStats stats = m_statsPartition;

        if (stats == null || m_fRefresh)
            {
            if (stats == null)
                {
                m_statsPartition = stats = new JMXPartitionStats();
                }

            stats.calculateJMXPartitionStats();

            m_fRefresh = false;
            }
        return stats;
        }

    /**
     * {@inheritDoc}
     */
    public String reportScheduledDistributions(boolean fVerbose)
        {
        Map<Member, PartitionSet[]> mapScheduled = collectScheduledDistributions();
        if (mapScheduled.isEmpty())
            {
            return getLastAnalysisContext() == null ? MSG_NO_RESULT : MSG_NO_PENDING;
            }

        StringBuilder sb = new StringBuilder();
        sb.append("Partition Distributions Scheduled for Service \"")
          .append(getManager().getService().getInfo().getServiceName())
          .append("\"\n");

        // 1. group members by machine
        Map<String, List<Member>> mapByMachine = new HashMap<>();
        for (Member member : mapScheduled.keySet())
            {
            String sMachine = member.getMachineName();
            sMachine = sMachine == null ? Integer.toString(member.getMachineId()) : sMachine;

            List<Member> listMembers = mapByMachine.get(sMachine);
            if (listMembers == null)
                {
                mapByMachine.put(sMachine, listMembers = new ArrayList<>());
                }

            listMembers.add(member);
            }

        // 2. for each machine, summarize the scheduled distributions per member
        for (Entry<String, List<Member>> entryMachine : mapByMachine.entrySet())
            {
            sb.append("\nMachine ").append(entryMachine.getKey());

            for (Member member : entryMachine.getValue())
                {
                sb.append("\n    Member ").append(member.getId()).append(":");

                for (int iStore = 0, cBackups = getBackupCount(); iStore <= cBackups; iStore++)
                    {
                    PartitionSet partsScheduled = mapScheduled.get(member)[iStore];
                    if (partsScheduled != null)
                        {
                        Map<Integer, PartitionSet> mapOwners = splitByOwner(partsScheduled);

                        int cScheduled = partsScheduled.cardinality();
                        if (cScheduled > 0)
                            {
                            String sStore = iStore == 0 ? " Primary" : " Backup"
                                    + (cBackups == 1 ? "" : ("[" + iStore + "]"));

                            sb.append("\n        - scheduled to receive ").append(cScheduled)
                                    .append(sStore).append(" partitions:");

                            for (Entry<Integer, PartitionSet> entrySource : mapOwners.entrySet())
                                {
                                Integer      IOwner = entrySource.getKey();
                                PartitionSet parts  = entrySource.getValue();

                                sb.append("\n           -- ").append(parts.cardinality())
                                  .append(" from member ").append(IOwner);

                                if (fVerbose)
                                    {
                                    sb.append(": ").append(parts);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        return sb.toString();
        }


    // ----- inner class: JMXPartitionStats ----------------------------------

    /**
     * A class that calculate MBean attribute values from last sampled PartitionStats
     */

    protected class JMXPartitionStats
        {
        /**
         * Return maximum node storage size.
         *
         * @return maximum node storage size in bytes
         */
        public long getMaxStorageSize()
            {
            return m_cbMaxStorage;
            }

        /**
         * Return average node storage size.
         *
         * @return average node storage size in bytes
         */
        public long getAverageStorageSize()
            {
            return m_cbAverageStorage;
            }

        /**
         * Return maximum partition storage size.
         *
         * @return Maximum partition storage size in bytes
         */
        public long getMaxPartitionSize()
            {
            return m_cbMaxPartition;
            }

        /**
         * Return average partition storage size.
         *
         * @return average partition storage size in bytes
         */
        public long getAveragePartitionSize()
            {
            return m_cbAveragePartition;
            }

        /**
         * Return node ID with maximum node storage size.
         *
         * @return node ID with maximum node storage size
         */
        public int getMaxLoadNodeId()
            {
            return m_nMaxLoadId;
            }

        /**
         * Calculate MBean attribute values for partition statistics.
         */
        public void calculateJMXPartitionStats()
            {
            DistributionManager manager = getManager();
            if (manager == null)
                {
                // not initialized yet
                return;
                }

            PartitionStatistics[] aStats    = manager.getPartitionStats();
            Set<Member>           setOwners = manager.getOwnershipMembers();

            if (setOwners == null)
                {
                return;
                }

            long cbTotalStorage = 0;
            long cbMaxPartition = 0;
            long cbMaxStorage   = 0;
            int  nMaxNodeId     = 0;

            for (Member member : setOwners)
                {
                PartitionSet parts = manager.getOwnedPartitions(member, 0);

                long cTotalStorageNode = 0;
                for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                    {
                    PartitionStatistics stat = aStats[iPart];

                    if (stat == null)
                        {
                        continue;
                        }

                    long cStoragePart = stat.getStorageSize();

                    cTotalStorageNode += cStoragePart;

                    if (cStoragePart > cbMaxPartition)
                        {
                        cbMaxPartition = cStoragePart;
                        }
                    }

                cbTotalStorage += cTotalStorageNode;
                if (cTotalStorageNode > cbMaxStorage)
                    {
                    cbMaxStorage = cTotalStorageNode;
                    nMaxNodeId   = member.getId();
                    }
                }

            m_cbAveragePartition = cbTotalStorage / (getPartitionCount() * 1024);
            m_cbAverageStorage = cbTotalStorage / (setOwners.size() * 1024);
            m_cbMaxPartition = cbMaxPartition / 1024;
            m_cbMaxStorage = cbMaxStorage / 1024;
            m_nMaxLoadId = nMaxNodeId;
            }


        // ----- data members -------------------------------

        /**
         * The maximum node storage size in bytes.
         */
        protected long m_cbMaxStorage;

        /**
         * The average node storage size in bytes.
         */
        protected long m_cbAverageStorage;

        /**
         * The maximum partition storage size in bytes.
         */
        protected long m_cbMaxPartition;

        /**
         * The average partition storage size in bytes.
         */
        protected long m_cbAveragePartition;

        /**
         * The node ID with maximum node storage size in bytes.
         */
        protected int m_nMaxLoadId;
        }


    // ----- MBean support ------------------------------------------------

    /**
     * Register an MBean representing this SimpleAssignmentStrategy.
     * <p>
     * This implementation creates and registers a {@link SimpleStrategyMBean}.
     */
    protected void registerMBean()
        {
        PartitionedService service  = getManager().getService();
        Registry           registry = service.getCluster().getManagement();

        if (registry != null)
            {
            try
                {
                registry.register(registry.ensureGlobalName(makeMBeanName(service)),
                        new AnnotatedStandardEmitterMBean(this, SimpleStrategyMBean.class));
                }
            catch (NotCompliantMBeanException e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }
        }

    /**
     * Create a name for the MBean representing this strategy.
     * <p>
     * The name must be globally unique, but not contain the <tt>nodeId</tt> property.
     * This allows us to re-bind the same JMX name to the MBean on a different node
     * when the distribution coordinator migrates.
     *
     * @param service  partitioned service that uses this strategy
     *
     * @return the name for the MBean
     */
    protected String makeMBeanName(PartitionedService service)
        {
        return Registry.PARTITION_ASSIGNMENT_TYPE + ",service=" + service.getInfo().getServiceName()
               + "," + Registry.KEY_RESPONSIBILITY + "DistributionCoordinator";
        }

    /**
     * Unregister the MBean representing this SimpleAssignmentStrategy.
     */
    protected void unregisterMBean()
        {
        PartitionedService service  = getManager().getService();
        Registry           registry = service.getCluster().getManagement();
        if (registry != null)
            {
            registry.unregister(registry.ensureGlobalName(makeMBeanName(service)));
            }
        }

    /**
     * Emit a partition loss notification for given partitions.
     *
     * @param partsLost  the partition id that has been lost
     */
    protected void emitLossNotification(PartitionSet partsLost)
        {
        PartitionedService service  = getManager().getService();
        Registry           registry = service.getCluster().getManagement();

        if (registry != null)
            {
            Member       memberThis = service.getCluster().getLocalMember();
            Notification note       = new Notification(NOTIFY_LOST, String.valueOf(memberThis),
                -1, partsLost.cardinality() + " partitions have been lost");
            note.setUserData(partsLost.toString());

            registry.getNotificationManager().trigger(
                registry.ensureGlobalName(makeMBeanName(service)), note);
            }
        }

    /**
     * Collect the scheduled partition distributions, grouped by the primary owner
     * and storage index.  A partition distribution (either primary or backup) is
     * considered to be scheduled if the distribution has been suggested but the
     * ownership change has not yet occurred.
     *
     * @return a Map of partition sets still awaiting distribution keyed by primary owner
     */
    protected Map<Member, PartitionSet[]> collectScheduledDistributions()
        {
        Map<Ownership, PartitionSet> mapSuggestion = m_mapSuggestLast;
        if (mapSuggestion == null || mapSuggestion.isEmpty())
            {
            return Collections.emptyMap();
            }

        int                         cBackups    = getBackupCount();
        int                         cPartitions = getPartitionCount();
        ServiceInfo                 serviceInfo = getManager().getService().getInfo();
        Map<Member, PartitionSet[]> mapScheduled  = new HashMap<>();

        for (Map.Entry<Ownership, PartitionSet> entry : mapSuggestion.entrySet())
            {
            Ownership    ownersSuggest = entry.getKey();
            PartitionSet partsSuggest  = entry.getValue();

            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                Member member = serviceInfo.getServiceMember(ownersSuggest.getOwner(iStore));
                if (member != null)
                    {
                    PartitionSet partsScheduled = getUnownedPartitions(partsSuggest, member.getId());
                    if (!partsScheduled.isEmpty())
                        {
                        PartitionSet[] aParts = mapScheduled.get(member);
                        if (aParts == null)
                            {
                            mapScheduled.put(member, aParts = new PartitionSet[cBackups + 1]);
                            }

                        PartitionSet partsMember = aParts[iStore];
                        if (partsMember == null)
                            {
                            aParts[iStore] = partsMember = new PartitionSet(cPartitions);
                            }

                        partsMember.add(partsScheduled);
                        }
                    }
                }
            }

        return mapScheduled;
        }

    /**
     * Calculate a subset of the PartitionSet consisting of partitions that are
     * not owned (primary or backup) by the specified member.
     *
     * @param parts    set of partitions to check the ownership of
     * @param nMember  the member-id
     *
     * @return the subset of partitions that are not owned by the specified member
     */
    protected PartitionSet getUnownedPartitions(PartitionSet parts, int nMember)
        {
        PartitionSet        partsUnowned = new PartitionSet(parts);
        int                 cBackups     = getBackupCount();
        DistributionManager mgr          = getManager();

        for (int i = parts.next(0); i >= 0; i = parts.next(i + 1))
            {
            Ownership owners = mgr.getPartitionOwnership(i);
            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                if (owners.getOwner(iStore) == nMember)
                    {
                    partsUnowned.remove(i);
                    break;
                    }
                }
            }

        return partsUnowned;
        }

    /**
     * Split the partition set scheduled for distribution by the current
     * primary owner (all transfers originate from the primary owner).
     *
     * @param parts  a set of partitions not yet transferred
     *
     * @return the partitions scheduled for distribution, associated with their
     *         primary owners
     */
    protected SortedMap<Integer, PartitionSet> splitByOwner(PartitionSet parts)
        {
        SortedMap<Integer, PartitionSet> mapByOwner  = new TreeMap<>();
        int                              cPartitions = getPartitionCount();
        DistributionManager              mgr         = getManager();

        for (int nPid = parts.next(0); nPid >= 0; nPid = parts.next(nPid + 1))
            {
            Ownership ownersCurrent = mgr.getPartitionOwnership(nPid);
            int       nOwnerCurrent = ownersCurrent.getPrimaryOwner();

            if (nOwnerCurrent == 0)
                {
                // orphaned partition; wait for the restore/recovery
                continue;
                }

            Integer      IOwnerCurrent = Integer.valueOf(nOwnerCurrent);
            PartitionSet partsOwned    = mapByOwner.get(IOwnerCurrent);
            if (partsOwned == null)
                {
                mapByOwner.put(IOwnerCurrent, partsOwned = new PartitionSet(cPartitions));
                }

            partsOwned.add(nPid);
            }

        return mapByOwner;
        }

    // ----- inner interface: LoadCalculator ------------------------------

    /**
     * LoadCalculator is used to calculate the scalar load (expressed as an
     * integer) of a partition (or set of partitions).
     */
    public static interface LoadCalculator
        {
        /**
         * Return the load for the specified partition.
         *
         * @param iPartition  the partition to determine the load for
         *
         * @return the load for the specified partition
         */
        public int getLoad(int iPartition);

        /**
         * Return the load for the specified set of partitions.
         *
         * @param parts  the partition set to determine the load for
         *
         * @return the load for the specified set of partitions
         */
        public int getLoad(PartitionSet parts);
        }


    // ----- inner class: SimpleLoadCalculator ----------------------------

    /**
     * Instantiate the load calculator.
     *
     * @param fPrimary  true iff the load calculator will be used for primary
     *                  partition load; backup otherwise
     *
     * @return a load calculator
     */
    public LoadCalculator instantiateLoadCalculator(boolean fPrimary)
        {
        return new SimpleLoadCalculator();
        }

    /**
     * SimpleLoadCalculator defines a "count-based" load (e.g. the load of each
     * partition is defined to be 1).
     */
    public static class SimpleLoadCalculator
            implements LoadCalculator
        {
        /**
         * {@inheritDoc}
         */
        public int getLoad(int nPartition)
            {
            return 1;
            }

        /**
         * {@inheritDoc}
         */
        public int getLoad(PartitionSet parts)
            {
            return parts.cardinality();
            }
        }


    // ----- inner class: AnalysisContext ---------------------------------

    /**
     * Factory method.
     *
     * @return a new AnalysisContext
     */
    public AnalysisContext instantiateAnalysisContext()
        {
        return new AnalysisContext();
        }

    /**
     * AnalysisContext holds the working view of the partition ownership that is
     * used throughout the analysis and is used to reflect changes made during
     * this analysis.
     */
    protected class AnalysisContext
        {
        // ----- constructors ---------------------------------------------

        /**
         * Default constructor.
         */
        public AnalysisContext()
            {
            initialize();
            }

        // ----- accessors ------------------------------------------------

        /**
         * Return the set of updated partitions; may be null.
         *
         * @return the set of updated partitions, or null
         */
        protected PartitionSet getUpdatedPartitions()
            {
            return m_partsUpdated;
            }

        /**
         * Return the BackupStrength for this analysis context.  The backup
         * strength determines the degree of resiliency that the resulting
         * distribution will ensure (e.g. machine-safe, rack-safe, site-safe).
         *
         * @return the backup strength
         */
        protected BackupStrength getBackupStrength()
            {
            return m_strength;
            }

        /**
         * Set the BackupStrength for this analysis context.
         *
         * @param strength  the backup strength
         */
        protected void setBackupStrength(BackupStrength strength)
            {
            m_strength = strength;
            }

        /**
         * Return the set of members across which to distribute the partition
         * ownership.
         * <p>
         * Note: The set of ownership members does not include any members that
         *       may be in the process of leaving
         *
         * @return the set of (non-leaving) ownership enabled members
         */
        protected Set<Member> getOwnershipMembers()
            {
            return m_setOwnershipMembers;
            }

        /**
         * Return the set of ownership members that are leaving.
         *
         * @return the set of leaving ownership enabled members
         */
        protected Set<Member> getLeavingOwners()
            {
            return m_setOwenersLeaving;
            }

        /**
         * Return an array containing the members across which to distribute the
         * partition ownership, arranged in arbitrary order.
         * <p>
         * Note: The array does not include any members that may be in the
         *       process of leaving
         *
         * @return an array containing the (non-leaving) ownership enabled members
         */
        protected Member[] getOwnershipMembersList()
            {
            return m_aOwnershipMembers;
            }

        /**
         * Return the LoadCalculator used to calculate the primary partition load.
         *
         * @return the primary partition load calculator
         */
        public LoadCalculator getPrimaryLoadCalculator()
            {
            return m_calculatorPrimary;
            }

        /**
         * Return the LoadCalculator used to calculate the backup partition load.
         *
         * @return the backup partition load calculator
         */
        public LoadCalculator getBackupLoadCalculator()
            {
            return m_calculatorBackup;
            }

        /**
         * Return the number of backups to maintain, given the actual set of
         * ownership-enabled and leaving members.
         *
         * @return the number of backups to maintain
         */
        protected int getActualBackupCount()
            {
            return m_cBackupActual;
            }

        /**
         * Return the time at which the analysis associated with this context
         * was completed, or 0 if it has not been completed.
         *
         * @return the time at which the analysis was completed, or 0
         */
        public long getCompletedTime()
            {
            return m_ldtCompleted;
            }

        /**
         * Set the timestamp at which the analysis associated with this context
         * completed.
         *
         * @param ldt  the completion timestamp, or 0
         */
        protected void setCompletedTime(long ldt)
            {
            m_ldtCompleted = ldt;
            }

        /**
         * Return the partitions deemed orphaned as a result of a previous
         * execution of {@link PartitionAssignmentStrategy#analyzeOrphans(Map)
         * analyzeOrphans}.
         *
         * @return the partitions deemed orphaned after executing analyzeOrphans
         */
        protected PartitionSet getOrphanedPartitions()
            {
            return m_partsOrphaned;
            }

        /**
         * Set the orphaned partitions that can be prioritized for transfer in
         * order to reduce the transfer cost.
         *
         * @param parts  the set of orphaned partitions
         */
        protected void setOrphanedPartitions(PartitionSet parts)
            {
            m_partsOrphaned = parts;
            }

        /**
         * Return the number of milliseconds the analysis should be delayed;
         * {@code 0L} suggests immediate analysis.
         *
         * @return the number of milliseconds the analysis should be delayed
         */
        protected long getAnalysisDelay()
            {
            return m_cDelay;
            }

        /**
         * Set the number of milliseconds the analysis should be delayed;
         * {@code 0L} suggests immediate analysis.
         *
         * @param cDelay  the number of milliseconds the analysis should be
         *                delayed
         */
        protected void setAnalysisDelay(long cDelay)
            {
            m_cDelay = cDelay;
            }

        /**
         * Reset those attributes that should be transient between {@link
         * #analyzeDistribution} requests.
         */
        protected void resetTransients()
            {
            setOrphanedPartitions(null);
            setAnalysisDelay(-1);
            }

        // ----- AnalysisContext methods ----------------------------------

        /**
         * Initialize the AnalysisContext.
         */
        protected void initialize()
            {
            DistributionManager manager    = getManager();
            Set                 setOwners  = manager.getOwnershipMembers();
            Set                 setLeaving = manager.getOwnershipLeavingMembers();

            if (!setLeaving.isEmpty())
                {
                setOwners = new SubSet(setOwners);
                setOwners.removeAll(setLeaving);
                }

            // cache the set of non-leaving ownership members
            m_setOwnershipMembers = setOwners;
            m_aOwnershipMembers   = (Member[]) setOwners.toArray(new Member[setOwners.size()]);
            m_setOwenersLeaving   = setLeaving;

            // the number of backups could be smaller than configured if we have a
            // small enough number of members
            m_cBackupActual = Math.min(getBackupCount(), setOwners.size() - 1);

            // instantiate the level of backup strength
            m_strength = instantiateBackupStrength(setOwners);

            // instantiate the load calculators
            m_calculatorPrimary = instantiateLoadCalculator(true);
            m_calculatorBackup  = instantiateLoadCalculator(false);

            // calculate and cache the fair-share load
            m_cFairSharePrimary = calculateFairShare(true);
            m_cFairShareBackup  = calculateFairShare(false);
            }

        /**
         * Copy transient values from another, generally the previous, AnalysisContext
         * to this AnalysisContext. This provides an opportunity for the other
         * context to impart knowledge to this context.
         *
         * @param ctxLast  the previous AnalysisContext
         */
        protected void copyTransients(AnalysisContext ctxLast)
            {
            if (ctxLast == null)
                {
                return;
                }

            PartitionSet partsOrphaned = ctxLast.getOrphanedPartitions();
            long         cDelay        = ctxLast.getAnalysisDelay();

            if (partsOrphaned != null)
                {
                setOrphanedPartitions(partsOrphaned);
                }
            if (cDelay >= 0)
                {
                setAnalysisDelay(cDelay);
                }
            }

        /**
         * Return the (primary or backup) fair-share partition load.
         *
         * @param fPrimary  true iff the primary fair-share should be returned
         *
         * @return the fair-share partition load
         */
        protected int getFairShare(boolean fPrimary)
            {
            return fPrimary ? m_cFairSharePrimary : m_cFairShareBackup;
            }

        /**
         * Return the "fair share" (F) load. It is a ceiling for the load on a
         * for fully balance distribution. The fairness goal is to achieve such a
         * state that the load for all members is between F-L and F, where L is
         * typically the minimum of partition load values.
         *
         * @param fPrimary    true for the primary fair-share, or false for backup
         *
         * @return the fair share
         */
        protected int calculateFairShare(boolean fPrimary)
            {
            Set            setOwners   = getOwnershipMembers();
            LoadCalculator calculator  = fPrimary
                ? getPrimaryLoadCalculator() : getBackupLoadCalculator();
            int            cMembers    = setOwners.size();
            PartitionSet   partsAll    = new PartitionSet(getPartitionCount());
            int            cLoadTotal;

            partsAll.fill();

            cLoadTotal = calculator.getLoad(partsAll);
            if (!fPrimary)
                {
                cLoadTotal *= getActualBackupCount();
                }

            return cMembers <= 1 ? cLoadTotal : cLoadTotal/cMembers + 1;
            }

        /**
         * Return true iff the specified member is in the process of leaving.
         *
         * @param member  the member
         *
         * @return true iff the specified member is in the process of leaving
         */
        protected boolean isMemberLeaving(Member member)
            {
            return getLeavingOwners().contains(member);
            }

        /**
         * Create a backup strength to be used for distribution among the
         * specified set of ownership members.
         *
         * @param setOwners  the ownership members
         *
         * @return the backup strength
         */
        protected BackupStrength instantiateBackupStrength(Set setOwners)
            {
            // split the members by machine, rack, site
            Map mapBySite    = new HashMap();
            Map mapByRack    = new HashMap();
            Map mapByMachine = new HashMap();

            for (Iterator iter = setOwners.iterator(); iter.hasNext(); )
                {
                Member member   = (Member) iter.next();
                String sSite    = member.getSiteName();
                String sRack    = member.getRackName();
                int    nMachine = member.getMachineId();

                Set setSite = (Set) mapBySite.get(sSite);
                if (setSite == null)
                    {
                    setSite = new HashSet();
                    mapBySite.put(sSite, setSite);
                    }
                setSite.add(member);

                Set setRack = (Set) mapByRack.get(sRack);
                if (setRack == null)
                    {
                    setRack = new HashSet();
                    mapByRack.put(sRack, setRack);
                    }
                setRack.add(member);

                Set setMachine = (Set) mapByMachine.get(nMachine);
                if (setMachine == null)
                    {
                    setMachine = new HashSet();
                    mapByMachine.put(nMachine, setMachine);
                    }
                setMachine.add(member);
                }

            // determine the highest level of safety that is achievable
            int nStrength = BackupStrength.NODE_SAFE;
            if (isStrongPossible(setOwners, mapBySite))
                {
                nStrength = BackupStrength.SITE_SAFE;
                }
            else if (isStrongPossible(setOwners, mapByRack))
                {
                nStrength = BackupStrength.RACK_SAFE;
                }
            else if (isStrongPossible(setOwners, mapByMachine))
                {
                nStrength = BackupStrength.MACHINE_SAFE;
                }

            return new BackupStrength(nStrength, new HashSet(mapBySite.keySet()),
                new HashSet(mapByRack.keySet()), new HashSet(mapByMachine.keySet()));
            }

        /**
         * Return true iff a "strong" balanced distribution is achievable for the
         * specified set of members, split among a set of categories
         * (e.g. machine, rack, site).
         *
         * @param setOwners  the set of ownership members
         * @param mapSplit   the map of members, associated by their category
         *
         * @return true iff a "strong" balanced distribution is achievable
         */
        protected boolean isStrongPossible(Set setOwners, Map mapSplit)
            {
            // Consider the number of members belonging to a specific group (e.g. a
            // machine, rack or site) to be given by m_i.  If there are K distinct
            // groups the counts of members-per-group can be expressed as
            // m_1, m_2, ... m_K where:
            //
            // 1) let CMembers = Sum(m_1, ..., m_K)
            // 2) m_1 <= m_2 <= ... <= m_(K-1) <= m_K
            //
            // With a backup-count = B, the "worst-case" number of members' worth of
            // primary partitions that could be lost and should be tolerated is:
            //
            // sum of the members in the largest B groups
            // 3) let MaxLost = m_(K-B-1), ... m_(K-1), m_K
            //
            // In order for the loss of that many members' worth of partitions to be
            // tolerated, the surviving members must have enough backup-storage (in
            // the trivially balanced case) to compensate:
            //
            // 4) B (CMembers - MaxLost) >= MaxLost
            //
            // therefore, it follows:
            //
            // B*CMembers - B*MaxLost >= MaxLost
            // B*CMembers >= MaxLost + B*MaxLost
            // B*CMembers >= (B + 1)*MaxLost
            //
            // If we take a conservative approximation that:
            //
            // m_(K-B-1) ~= m_(K-B-2) ~= ... ~= m_(K-1) ~= m_K
            //
            // then MaxLost is conservatively estimated as:
            //
            // MaxLost ~= B*m_K
            //
            // therefore:
            // B*CMembers >= (B + 1)*(B*m_K)
            // B*CMembers >= B*(B + 1)*(m_K)
            // CMembers >= (B + 1)*(m_K)

            int cMax = 0;
            for (Iterator iter = mapSplit.values().iterator(); iter.hasNext(); )
                {
                Set setGroup = (Set) iter.next();
                int cMembers = setGroup.size();
                if (cMembers > cMax)
                    {
                    cMax = cMembers;
                    }
                }

            return cMax * (getBackupCount() + 1) <= setOwners.size();
            }

        /**
         * Return true iff the ownership of the specified partition is "strong",
         * as defined by the current BackupStrength.
         *
         * @param iPartition  the partition
         *
         * @return true iff the specified partition is strong
         */
        protected boolean isPartitionStrong(int iPartition)
            {
            return isPartitionStrong(getPartitionOwnership(iPartition));
            }

        /**
         * Return true iff the specified ownership is "strong", as defined by the
         * current BackupStrength.
         *
         * @param owners  the ownership
         *
         * @return true iff the specified ownership is strong
         */
        protected boolean isPartitionStrong(Ownership owners)
            {
            BackupStrength strength = getBackupStrength();
            int            cBackups = getActualBackupCount();
            Member[]       aMembers = new Member[cBackups + 1];

            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                Member member = getMember(owners.getOwner(iStore));
                if (member == null)
                    {
                    // orphaned/endangered cannot be "strong"
                    return false;
                    }

                aMembers[iStore] = member;
                }

            // compare the pair-wise strength among all owners
            for (int iStoreThis = 0; iStoreThis <= cBackups; iStoreThis++)
                {
                for (int iStoreThat = iStoreThis + 1; iStoreThat <= cBackups; iStoreThat++)
                    {
                    if (!strength.isStrong(aMembers[iStoreThis], aMembers[iStoreThat]))
                        {
                        return false;
                        }
                    }
                }

            return true;
            }

        /**
         * Return true iff the specified partition transfer would result in a
         * "strong" ownership, as defined by the current BackupStrength.
         *
         * @param iPartition  the partition to transfer
         * @param iStore      the storage index to transfer
         * @param member      the member receiving the transfer
         *
         * @return true iff the specified partition transfer is strong
         */
        protected boolean isTransferStrong(int iPartition, int iStore, Member member)
            {
            Ownership owners = (Ownership) getPartitionOwnership(iPartition).clone();
            owners.setOwner(iStore, member.getId());

            return isPartitionStrong(owners);
            }

        /**
         * Return true iff the specified members are mutually "strong", as
         * defined by the backup strength.
         *
         * @param member1  the first member to compare
         * @param member2  the second member to compare
         *
         * @return true iff the specified members are mutually strong
         */
        protected boolean isStrong(Member member1, Member member2)
            {
            return getBackupStrength().isStrong(member1, member2);
            }

        /**
         * Return true iff the specified member is "strong" with respect to the
         * specified ownership, as defined by the backup strength.
         *
         * @param member  the member
         * @param owners  the ownership
         *
         * @return true iff the member is "strong" with respect to the ownership
         */
        protected boolean isStrong(Member member, Ownership owners)
            {
            int cBackups = getBackupCount();
            for (int iStore = 0; iStore <= cBackups; iStore++)
                {
                int nOwner = owners.getOwner(iStore);
                if (nOwner != 0 && isStrong(member, getMember(nOwner)))
                    {
                    return true;
                    }
                }

            return false;
            }

        /**
         * Return a partition set representing the subset of the specified
         * partitions that are "orphaned".
         *
         * @param parts  the partition set to collect
         *
         * @return a partition set containing the orphaned partitions
         */
        protected PartitionSet collectOrphaned(PartitionSet parts)
            {
            PartitionSet partsOrphaned = getOrphanedPartitions();
            PartitionSet partsOwnedOrphans;

            if (partsOrphaned != null && partsOrphaned.intersects(parts))
                {
                partsOwnedOrphans = new PartitionSet(partsOrphaned);
                partsOwnedOrphans.retain(parts);
                }
            else
                {
                partsOwnedOrphans = new PartitionSet(parts.getPartitionCount());
                }

            return partsOwnedOrphans;
            }

        /**
         * Return a partition set representing the subset of the specified
         * partitions that are "weak" or "vulnerable" (as defined by the backup
         * strength).
         *
         * @param parts  the partition set to collect
         *
         * @return a partition set containing the weak partitions
         */
        protected PartitionSet collectWeak(PartitionSet parts)
            {
            PartitionSet partsWeak = new PartitionSet(parts.getPartitionCount());
            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                if (!isPartitionStrong(iPart))
                    {
                    partsWeak.add(iPart);
                    }
                }

            return partsWeak;
            }

        /**
         * Return true iff the specified partition is "endangered".  A partition is
         * "endangered" if it is incompletely backed up (e.g. some backup copies
         * do not exist).
         *
         * @param iPartition  the partition to test the endangered status
         *
         * @return true iff the specified partition is endangered
         */
        protected boolean isPartitionEndangered(int iPartition)
            {
            return isPartitionEndangered(getPartitionOwnership(iPartition));
            }

        /**
         * Return true iff the specified ownership is "endangered".  A partition
         * is "endangered" if it is incompletely backed up (e.g. some backup
         * copies do not exist).
         *
         * @param owners  the ownership to test for endangered status
         *
         * @return true iff the specified partition is endangered
         */
        protected boolean isPartitionEndangered(Ownership owners)
            {
            int cBackups = getActualBackupCount();
            for (int iStore = 1; iStore <= cBackups; iStore++)
                {
                if (owners.getOwner(iStore) == 0)
                    {
                    return true;
                    }
                }

            return false;
            }

        /**
         * Return a partition set representing the subset of the specified
         * partitions that are "endangered".
         *
         * @param parts  the partition set to collect
         *
         * @return a partition set containing the endangered partitions
         */
        protected PartitionSet collectEndangered(PartitionSet parts)
            {
            PartitionSet partsEndangered = new PartitionSet(parts.getPartitionCount());
            for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                {
                if (isPartitionEndangered(iPart))
                    {
                    partsEndangered.add(iPart);
                    }
                }

            return partsEndangered;
            }

        /**
         * Ensure and return the set of updated partitions.
         *
         * @return the set of updated partitions
         */
        protected PartitionSet ensureUpdatedPartitions()
            {
            PartitionSet partsUpdated = getUpdatedPartitions();
            if (partsUpdated == null)
                {
                m_partsUpdated = partsUpdated = new PartitionSet(getPartitionCount());
                }

            return partsUpdated;
            }

        /**
         * Return the set of partitions for which the specified member owns (or
         * has been assigned by this analysis to own) the specified storage
         * index.
         *
         * @param member  the member
         * @param iStore  the storage index
         *
         * @return the set of partitions owned by the member at the specified index
         */
        public PartitionSet getOwnedPartitions(Member member, int iStore)
            {
            PartitionSet[] aParts = m_mapOwnedPartitions.get(member);
            if (aParts == null)
                {
                aParts = new PartitionSet[1 + getBackupCount()];
                m_mapOwnedPartitions.put(member, aParts);
                }

            PartitionSet parts = aParts[iStore];
            if (parts == null)
                {
                parts = getManager().getOwnedPartitions(member, iStore);
                aParts[iStore] = parts;
                }

            return parts;
            }

        /**
         * Check if the distribution is in the initial state, when the coordinator
         * owns all the partitions and there are no backups.
         *
         * @param memberCoordinator  the coordinator
         *
         * @return true if the coordinator owns all the partitions and there
         *         are no backups
         */
        public boolean isInitialDistribution(Member memberCoordinator)
            {
            PartitionedService service  = getManager().getService();
            int                cBackups = service.getBackupCount();

            for (int iPart = 0, c = service.getPartitionCount(); iPart < c; iPart++)
                {
                if (service.getPartitionOwner(iPart) != memberCoordinator)
                    {
                    return false;
                    }

                for (int iBackup = 1; iBackup <= cBackups; iBackup++)
                    {
                    if (service.getBackupOwner(iPart, iBackup) != null)
                        {
                        return false;
                        }
                    }
                }
            return true;
            }

        /**
         * Set the context to pretend to be the "two servers" membership.
         *
         * @param member1  the first member
         * @param member2  the second member
         */
        protected void primeDistribution(Member member1, Member member2)
            {
            int cFairInitial = getPartitionCount() / 2 + 1;
            Member[] aMembers = new Member[] {member1, member2};

            m_setOwnershipMembers = new ImmutableArrayList(aMembers);
            m_aOwnershipMembers   = aMembers;
            m_cFairSharePrimary   = cFairInitial;
            m_cFairShareBackup    = cFairInitial;
            }

        /**
         * Return the Ownership information (or the ownership assigned by this
         * analysis) for the specified partition.
         *
         * @param iPartition   the partition to return the ownership for
         *
         * @return the Ownership information
         */
        public Ownership getPartitionOwnership(int iPartition)
            {
            Ownership owners = m_aOwners[iPartition];
            if (owners == null)
                {
                owners = getManager().getPartitionOwnership(iPartition);
                m_aOwners[iPartition] = owners;
                }

            return owners;
            }

        /**
         * Return the load (as defined by the appropriate load calculator) for
         * the specified partition.
         *
         * @param iPartition  the partition to determine the load of
         * @param fPrimary    true iff the primary load should be returned, or
         *                    false for backup
         *
         * @return the load for the specified partition
         */
        protected int getPartitionLoad(int iPartition, boolean fPrimary)
            {
            return (fPrimary ? getPrimaryLoadCalculator() : getBackupLoadCalculator())
                .getLoad(iPartition);
            }

        /**
         * Return the (primary or backup) partition load of the specified member.
         *
         * @param member    the member to calculate the partition load for
         * @param fPrimary  true for primary partition load, else backup load
         *
         * @return the partition load for the specified member
         */
        protected int getMemberLoad(Member member, boolean fPrimary)
            {
            if (fPrimary)
                {
                return getPrimaryLoadCalculator().getLoad(
                        getOwnedPartitions(member, 0));
                }
            else
                {
                LoadCalculator calculator = getBackupLoadCalculator();
                int            cBackups   = getBackupCount();
                int            cLoad      = 0;
                for (int iStore = 1; iStore <= cBackups; iStore++)
                    {
                    cLoad += calculator.getLoad(
                            getOwnedPartitions(member, iStore));
                    }

                return cLoad;
                }
            }

        /**
         * Calculate whether the analysis should be delayed.
         *
         * @return the delay before the next analysis, or 0 if the analysis
         *         should commence immediately
         */
        protected long calculateAnalysisDelay()
            {
            AnalysisContext ctxLast = getLastAnalysisContext();
            if (ctxLast == null)
                {
                return 0;
                }

            // check to see what, if any, membership changes have occurred since
            // the last time analysis was considered
            Set  setOwnersPrev = getLastOwnershipMembers();
            Set  setOwnersCur  = getOwnershipMembers();
            long cDelay        = m_cDelay;

            if (cDelay >= 0)
                {
                // if the delay was explicitly specified we pass it on once
                m_cDelay = -1;
                return cDelay;
                }
            else if (setOwnersCur.equals(setOwnersPrev))
                {
                // no membership change has occurred since the last delay calculation;
                // check to see if the previously issued advice has been enacted
                PartitionSet parts = ctxLast.getUpdatedPartitions();
                if (parts == null || !ctxLast.getLeavingOwners().isEmpty())
                    {
                    // 1. no changes were suggested last time - no harm in proceeding
                    // 2. there were members leaving (graceful shutdown) therefore
                    //    re-execute a plan to ensure we get to balance asap and/or
                    //    re-issue any dropped advice due to restore during a plan update
                    return 0L;
                    }

                // account for all the rejected or dropped advice
                PartitionSet partsIgnored = getManager().getIgnoredAdvice();
                if (partsIgnored != null)
                    {
                    // there is no reason to remember about a given advice
                    // that was ignored
                    parts.remove(partsIgnored);
                    }

                for (int iPart = parts.next(0); iPart >= 0; iPart = parts.next(iPart + 1))
                    {
                    Ownership ownersSuggested = ctxLast.getPartitionOwnership(iPart);
                    Ownership ownersCurrent   = getPartitionOwnership(iPart);
                    if (!ownersCurrent.equals(ownersSuggested))
                        {
                        // there are still suggestions from the last analysis
                        // that are pending or in-flight and we should delay the
                        // analysis to give more time for those suggestions to
                        // take effect because the intermediate state
                        // (partially implemented suggestions) may yield different
                        // (conflicting) suggestions, leading to unnecessary
                        // transfers, or unbalanced distributions (see COH-14898)

                        cDelay = Math.max(0, ctxLast.getCompletedTime() +
                            getSuggestionCompletionDelay() - Base.getSafeTimeMillis());

                        if (setOwnersCur.equals(ctxLast.getOwnershipMembers()))
                            {
                            // no membership changes have occurred, so there is no
                            // real rush to re-analyze; proceed as scheduled; However
                            // we don't want service waiting too long to call back
                            return Math.min(cDelay, getSuggestionDelay());
                            }
                        else
                            {
                            // there was a membership change from the last analysis time;
                            // reschedule aggressively
                            return Math.min(cDelay, 1000);
                            }
                        }
                    }

                // all previous suggestions have been enacted, and there
                // is no reason to delay; proceed with the next analysis
                return 0L;
                }
            else if (setOwnersCur.containsAll(setOwnersPrev))
                {
                // some members have joined; delay to let the membership settle
                return getMemberJoinDelay();
                }
            else
                {
                // some members have left; we need to re-analyze immediately in
                // order to address the endangered partitions
                return 0L;
                }
            }

        /**
         * Update the analysis context to reflect the suggested transfer of the
         * specified number of primary partitions between the specified members.
         *
         * @param iPartition  the partition id to transfer
         * @param iStore      the storage index to transfer
         * @param memberFrom  the member to transfer partitions from, or null if
         *                    the partition storage-index was endangered
         * @param memberTo    the member to transfer partitions to, or null if
         *                    the partition storage index should be endangered
         */
        protected void transitionPartition(
                int iPartition, int iStore, Member memberFrom, Member memberTo)
            {
            int       nMemberTo = memberTo == null ? 0 : memberTo.getId();
            int       cBackups  = getBackupCount();
            Ownership owners    = getPartitionOwnership(iPartition);

            // update the ownership info
            if (memberFrom != null)
                {
                getOwnedPartitions(memberFrom, iStore).remove(iPartition);
                }
            if (memberTo != null)
                {
                getOwnedPartitions(memberTo, iStore).add(iPartition);
                }

            for (int i = 0; i <= cBackups; i++)
                {
                if (i == iStore) // the storage index we are transferring
                    {
                    owners.setOwner(iStore, nMemberTo);

                    // If we are doing a primary transfer, the old primary could
                    // either become a new backup, or it could release the
                    // storage.  Demote the old primary to be a backup iff it
                    // increases backup strength.
                    //
                    // See PartitionedService.assignPrimaryPartition()
                    if (iStore == 0 && memberTo != null && memberFrom != null &&
                        !isMemberLeaving(memberFrom))
                        {
                        for (int j = 1; j <= cBackups; j++)
                            {
                            Member memberBackup = getMember(owners.getOwner(j));

                            // Note: assignPrimaryPartition() is hard-coded to compare
                            //       for machine-safety
                            if (memberTo.getMachineId() != memberFrom.getMachineId() &&
                                (memberBackup == null ||
                                 memberTo.getMachineId() == memberBackup.getMachineId()))
                                {
                                getOwnedPartitions(memberFrom, j).add(iPartition);

                                if (memberBackup != null)
                                    {
                                    getOwnedPartitions(memberBackup, j).remove(iPartition);
                                    }
                                owners.setOwner(j, memberFrom.getId());
                                break;
                                }
                            }
                        }
                    }
                else if (nMemberTo != 0 && owners.getOwner(i) == nMemberTo)
                    {
                    // the new owner already owns a different storage-index of
                    // this partition; set it to be endangered
                    owners.setOwner(i, 0);
                    getOwnedPartitions(memberTo, i).remove(iPartition);
                    }
                }

            ensureUpdatedPartitions().add(iPartition);
            }

        /**
         * Suggest any distribution that may have been collected by this analysis
         * context to the DistributionManager.
         *
         * @return true iff a distribution was suggested
         */
        protected boolean suggestDistribution()
            {
            PartitionSet partsUpdated = getUpdatedPartitions();
            if (partsUpdated == null)
                {
                m_mapSuggestLast = Collections.emptyMap();
                return false;
                }

            int                 cPartitions = getPartitionCount();
            Map                 mapSuggest  = new HashMap();
            DistributionManager manager      = getManager();
            for (int iPart = partsUpdated.next(0); iPart >= 0;
                     iPart = partsUpdated.next(iPart + 1))
                {
                Ownership    owners = getPartitionOwnership(iPart);
                PartitionSet parts  = (PartitionSet) mapSuggest.get(owners);

                if (!owners.equals(manager.getPartitionOwnership(iPart)))
                    {
                    // there has actually been a change
                    if (parts == null)
                        {
                        parts = new PartitionSet(cPartitions);
                        mapSuggest.put(owners, parts);
                        }

                    parts.add(iPart);
                    }
                }

            for (Iterator iter = mapSuggest.entrySet().iterator(); iter.hasNext(); )
                {
                Entry        entry  = (Entry)        iter.next();
                Ownership    owners = (Ownership)    entry.getKey();
                PartitionSet parts  = (PartitionSet) entry.getValue();

                if (!parts.isEmpty())
                    {
                    manager.suggest(parts, owners);
                    }
                }

            m_mapSuggestLast = mapSuggest;
            return true;
            }

        // ----- inner class: NotOwnedFilter ------------------------------

        /**
         * Instantiate and return a NotOwnedFilter with the specified ownership.
         *
         * @param owners  the ownership
         *
         * @return a NotOwnedFilter
         */
        public Filter instantiateNotOwnedFilter(Ownership owners)
            {
            return new NotOwnedFilter(owners);
            }

        /**
         * NotOwnedFilter is a Filter implementation used to evaluate Member
         * objects, and selects members who are not represented in the reference
         * ownership object.
         */
        public class NotOwnedFilter
                implements Filter
            {
            /**
             * Construct a NotOwnedFilter with the specified ownership.
             *
             * @param owners  the ownership
             */
            public NotOwnedFilter(Ownership owners)
                {
                m_owners = owners;
                }

            // ----- accessors --------------------------------------------

            /**
             * Return the ownership used to evaluate member safety.
             *
             * @return the ownership
             */
            public Ownership getOwnership()
                {
                return m_owners;
                }

            // ----- Filter methods ---------------------------------------

            /**
             * {@inheritDoc}
             */
            public boolean evaluate(Object o)
                {
                Ownership owners   = getOwnership();
                int       nMember  = ((Member) o).getId();
                int       cBackups = getActualBackupCount();
                for (int iStore = 0; iStore <= cBackups; iStore++)
                    {
                    if (owners.getOwner(iStore) == nMember)
                        {
                        return false;
                        }
                    }

                return true;
                }

            // ----- data members -----------------------------------------

            /**
             * The ownership
             */
            protected Ownership m_owners;
            }

        // ----- inner class: SafetyFilter --------------------------------

        /**
         * Instantiate and return a SafetyFilter with the specified ownership.
         *
         * @param owners  the ownership
         * @param iStore  the storage index at which to evaluate members for safety
         *
         * @return a SafetyFilter
         */
        public Filter instantiateSafetyFilter(Ownership owners, int iStore)
            {
            return new SafetyFilter(owners, iStore);
            }

        /**
         * SafetyFilter is a Filter implementation used to evaluate Member
         * objects, and selects members that are "strong" with respect to the
         * reference ownership, as defined by the backup-strength.
         */
        public class SafetyFilter
                implements Filter
            {
            /**
             * Construct a SafetyFilter with the specified ownership.
             *
             * @param owners  the ownership
             * @param iStore  the storage index at which to evaluate members for safety
             */
            public SafetyFilter(Ownership owners, int iStore)
                {
                m_owners = (Ownership) owners.clone();
                m_iStore = iStore;
                }

            // ----- accessors --------------------------------------------

            /**
             * Return the ownership used to evaluate member safety.
             *
             * @return the ownership
             */
            public Ownership getOwnership()
                {
                return m_owners;
                }

            /**
             * Return the storage index at which to evaluate members for safety.
             *
             * @return the storage index at which to evaluate members for safety
             */
            public int getStorageIndex()
                {
                return m_iStore;
                }

            // ----- Filter methods ---------------------------------------

            /**
             * {@inheritDoc}
             */
            public boolean evaluate(Object o)
                {
                Ownership owners = getOwnership();
                owners.setOwner(getStorageIndex(), ((Member) o).getId());

                return isPartitionStrong(owners);
                }

            // ----- data members -----------------------------------------

            /**
             * The ownership.
             */
            protected Ownership m_owners;

            /**
             * The storage index at which to evaluate members for safety.
             */
            protected int m_iStore;
            }

        // ----- inner class: UnderloadedFilter ---------------------------

        /**
         * Instantiate a filter that matches members with an over-load.
         *
         * @param fPrimary  true for primary partition load
         *
         * @return a filter that matches members with an over-load
         */
        public Filter instantiateOverloadedFilter(boolean fPrimary)
            {
            return new NotFilter(new UnderloadedFilter(fPrimary));
            }

        /**
         * Instantiate a filter that matches members with an under-load.
         *
         * @param fPrimary  true for primary partition load
         *
         * @return a filter that matches members with an under-load
         */
        public Filter instantiateUnderloadedFilter(boolean fPrimary)
            {
            return new UnderloadedFilter(fPrimary);
            }

        /**
         * UnderloadedFilter is a Filter implementation that is used to evaluate
         * Member objects, and selects those whose partition load is
         * "underloaded" in comparison to the fair-share load.
         */
        public class UnderloadedFilter
                implements Filter
            {
            /**
             * Construct an UnderloadedFilter.
             *
             * @param fPrimary  true iff the filter should compare primary
             *                  partition load
             */
            protected UnderloadedFilter(boolean fPrimary)
                {
                m_fPrimary   = fPrimary;
                m_cFairShare = AnalysisContext.this.getFairShare(fPrimary);
                }

            // ----- accessors --------------------------------------------

            /**
             * Return true iff this Filter compares the primary partition load.
             *
             * @return true if comparing the primary partition load
             */
            public boolean isPrimary()
                {
                return m_fPrimary;
                }

            /**
             * Return the fair-share partition load.
             *
             * @return the fair-share partition load
             */
            public int getFairShare()
                {
                return m_cFairShare;
                }

            // ----- Filter methods ---------------------------------------

            /**
             * {@inheritDoc}
             */
            public boolean evaluate(Object o)
                {
                return getMemberLoad((Member) o, isPrimary()) < getFairShare();
                }

            // ----- data members -----------------------------------------

            /**
             * The cached fair-share load.
             */
            protected int m_cFairShare;

            /**
             * True iff this filter represents primary partition load.
             */
            protected boolean m_fPrimary;
            }

        // ----- inner class: LoadComparator ------------------------------

        /**
         * Return a comparator for primary or backup partition load.
         *
         * @param fPrimary  true for primary, or false for backup
         *
         * @return a comparator for primary or backup partition load
         */
        public LoadComparator instantiateLoadComparator(boolean fPrimary)
            {
            return new LoadComparator(fPrimary);
            }

        /**
         * LoadComparator is a Comparator that can be used to compare two Member
         * objects based on their partition load (as defined by the
         * LoadCalculator).
         * <p>
         * A member is ordered "lower" than another member if it has a lower
         * member load (as determined by the LoadCalculator).  Members with
         * equivalent loads are ordered equivalently.
         * <p>
         * Note: This comparator does not define an ordering that is "consistent
         *       with equals".  If used in a context requiring such a natural
         *       ordering, it should be chained with comparator that does
         *       provide a natural ordering.
         */
        public class LoadComparator
                implements Comparator
            {
            /**
             * Construct a LoadComparator.
             *
             * @param fPrimary  true if the comparator should use the primary load,
             *                  or false for backup load
             */
            public LoadComparator(boolean fPrimary)
                {
                m_fPrimary = fPrimary;
                }

            // ----- accessors --------------------------------------------

            /**
             * Return true iff the comparator should use the primary load.
             *
             * @return true iff the comparator should use the primary load
             */
            public boolean isPrimary()
                {
                return m_fPrimary;
                }

            // ----- Comparator methods -----------------------------------

            /**
             * {@inheritDoc}
             */
            public int compare(Object o1, Object o2)
                {
                Member  member1  = (Member) o1;
                Member  member2  = (Member) o2;
                boolean fPrimary = isPrimary();
                int     cLoad1   = getMemberLoad(member1, fPrimary);
                int     cLoad2   = getMemberLoad(member2, fPrimary);

                return cLoad1 - cLoad2;
                }

            // ----- data members -----------------------------------------

            /**
             * Flag for primary or backup load comparison.
             */
            protected boolean m_fPrimary;
            }

        // ----- inner class: StrengthComparator --------------------------

        /**
         * Instantiate a StrengthComparator for the specified reference ownership.
         *
         * @param owners  the ownership, from which to determine member strength
         *
         * @return a StrengthComparator
         */
        public StrengthComparator instantiateStrengthComparator(Ownership owners)
            {
            return new StrengthComparator(owners);
            }

        /**
         * StrengthComparator is an Comparator that can be used to compare two
         * Member objects based on their "distance" from a given set of members
         * (as represented by an Ownership object).  Member distances are
         * expressed with the granularity of "machine", "rack", and "site".
         * <p>
         * A member is ordered "lower" than another member if it has a larger
         * combined distance from the reference members (ownership) (i.e. it is
         * "stronger").  Members with equivalent distances are ordered
         * arbitrarily.
         * <p>
         * Note: This comparator does not define an ordering that is "consistent
         *       with equals".  If used in a context requiring such a natural
         *       ordering, it should be chained with comparator that does
         *       provide a natural ordering.
         */
        public class StrengthComparator
                implements Comparator
            {
            /**
             * Construct a StrengthComparator for the specified reference ownership.
             *
             * @param owners  the ownership, from which to determine member
             *                strength
             */
            public StrengthComparator(Ownership owners)
                {
                m_owners = owners;
                }

            // ----- accessors --------------------------------------------

            /**
             * Return the ownership to use in comparing member strength.
             *
             * @return the ownership
             */
            public Ownership getOwnership()
                {
                return m_owners;
                }

            // ----- Comparator methods -----------------------------------

            /**
             * {@inheritDoc}
             */
            public int compare(Object o1, Object o2)
                {
                // larger distances appear "first"
                return getDistance((Member) o2) - getDistance((Member) o1);
                }

            // ----- helpers ----------------------------------------------

            /**
             * Return the "distance" of the specified member from the reference
             * ownership.  The distance reflects granularities of "machine",
             * "rack" and "site", and in the case of multiple-backups, reflects
             * combined distances to each backup.
             *
             * @param member  the member to return the distance for
             *
             * @return the "distance"
             */
            protected int getDistance(Member member)
                {
                Ownership owners    = getOwnership();
                int       cBackups  = owners.getBackupCount();
                int       nDistance = 0;

                // Calculate a distance metric to be used in comparing
                // various members' strength with respect to the
                // single reference ownership.  While the resulting
                // metric needs to provide a consistent comparison
                // across multiple members, it does not hold any
                // meaning outside of the given reference ownership.
                //
                // For example, the reference ownership could take one
                // of 3 "shapes":
                //
                // (?, 0, 0) -  no owners at all
                // (?, a, 0) -  some storage indices are unowned
                // (?, a, b) -  all storage indices are owned
                //
                // The "distance" metric will return:
                // (?, 0, 0) - 0 (all members are equally "distant")
                // (?, a, 0) - dist(m, a)^2
                // (?, a, b) - dist(m, a)^2 + dist(m, b)^2
                for (int iStore = 0; iStore <= cBackups; iStore++)
                    {
                    int nOwner = owners.getOwner(iStore);
                    if (nOwner != 0)
                        {
                        Member memberBackup = getMember(nOwner);
                        if (member == null || memberBackup == null)
                            {
                            Logger.err(String.format("%s is null for %s",
                                    member == null ? "Slot in member target array" : "Backup owner", owners));
                            }

                        int n = getDistance(member, getMember(nOwner));
                        nDistance += n * n;
                        }
                    }

                return nDistance;
                }

            /**
             * Return the "distance" between the specified members.
             *
             * @param member1  the first member
             * @param member2  the second member
             *
             * @return the "distance" between the specified members
             */
            protected int getDistance(Member member1, Member member2)
                {
                if (!Base.equals(member1.getSiteName(), member2.getSiteName()))
                    {
                    return BackupStrength.SITE_SAFE;
                    }

                if (!Base.equals(member1.getRackName(), member2.getRackName()))
                    {
                    return BackupStrength.RACK_SAFE;
                    }

                if (member1.getMachineId() != member2.getMachineId())
                    {
                    return BackupStrength.MACHINE_SAFE;
                    }

                if (member1.getId() != member2.getId())
                    {
                    return BackupStrength.NODE_SAFE;
                    }

                Base.azzert(member1 == member2);
                return 0;
                }

            // ----- data members -----------------------------------------

            /**
             * The ownership.
             */
            protected Ownership m_owners;
            }

        // ----- default comparator ---------------------------------------

        /**
         * Instantiate a default member Comparator.  <em>The returned comparator
         * must define a strict total ordering over the set of Members.</em>  In
         * other words, no two distinct members may be {@link Comparator#compare
         * compared} to be equivalent.
         *
         * @return a Comparator that defines a strict total ordering of Members
         */
        public Comparator instantiateDefaultComparator()
            {
            // MEMBERID_COMPARATOR is immutable
            return MEMBERID_COMPARATOR;
            }

        // ----- data members ---------------------------------------------

        /**
         * The primary LoadCalculator.
         */
        protected LoadCalculator m_calculatorPrimary;

        /**
         * The backup LoadCalculator.
         */
        protected LoadCalculator m_calculatorBackup;

        /**
         * The map of member ownership information for this analysis context.
         */
        protected Map<Member, PartitionSet[]> m_mapOwnedPartitions = new HashMap<>();

        /**
         * The ownership array for this analysis context.
         */
        protected Ownership[] m_aOwners = new Ownership[getPartitionCount()];

        /**
         * The set of partitions that have been updated in this analysis context;
         * may be null.
         */
        protected PartitionSet m_partsUpdated;

        /**
         * The set of partitions that were determined to be orphaned in the call
         * to {@link PartitionAssignmentStrategy#analyzeOrphans(Map)}.
         */
        protected PartitionSet m_partsOrphaned;

        /**
         * The backup strength for the resiliency of the resulting distribution.
         */
        protected BackupStrength m_strength;

        /**
         * The number of backup storages to maintain.
         * <p>
         * Note: this may differ from the configured backup count if there is an
         *       inadequate number of ownership members to sustain the configured
         *       backup count.
         */
        protected int m_cBackupActual;

        /**
         * The fair-share primary partition load.
         */
        protected int m_cFairSharePrimary;

        /**
         * The fair-share backup partition load.
         */
        protected int m_cFairShareBackup;

        /**
         * The set of ownership members to include in the distribution.
         * <p>
         * Note: this set does not include members that are leaving
         */
        protected Set m_setOwnershipMembers;

        /**
         * The set of ownership members that are leaving.
         */
        protected Set m_setOwenersLeaving;

        /**
         * An array of the ownership members to include in the distribution,
         * arranged in arbitrary order.  This array could be used for algorithms
         * performing in-place sorting of the members.
         * <p>
         * Note: this list does not include members that are leaving
         */
        protected Member[] m_aOwnershipMembers;

        /**
         * The timestamp of when the analysis represented by this context was
         * completed, or 0 if it is not yet complete.
         */
        protected long m_ldtCompleted;

        /**
         * An explicit delay to be used in favor of a determined delay in
         * {@link #calculateAnalysisDelay()}.
         */
        protected long m_cDelay = -1L;
        }

    // ----- inner class: BackupStrength ----------------------------------

    /**
     * BackupStrength represents a level of "strength" or "resiliency" between
     * the primary and backup owners of a partition.  The BackupStrength is used
     * to determine which backup owners could serve as a "strong" backup for a
     * primary owner.
     */
    protected static class BackupStrength
        {
        /**
         * Construct a BackupStrength of the specified strength.
         *
         * @param nStrength    one of the BackupStrength.*_SAFE constants
         * @param setSites     the site names
         * @param setRacks     the rack names
         * @param setMachines  the machine names
         */
        protected BackupStrength(int nStrength, Set setSites, Set setRacks, Set setMachines)
            {
            m_nStrength   = nStrength;
            m_setSites    = setSites;
            m_setRacks    = setRacks;
            m_setMachines = setMachines;
            }

        // ----- BackupStrength methods -------------------------------

        /**
         * Return the next weakest BackupStrength.
         *
         * @return a BackupStrength that is immediately weaker than this
         */
        protected BackupStrength getWeaker()
            {
            int nStrength = m_nStrength;
            if (nStrength == NODE_SAFE)
                {
                // NODE_SAFE is the weakest possible strength
                throw new IllegalStateException(
                        "NODE_SAFE is the weakest BackupStrength");
                }

            return new BackupStrength(nStrength - 1,
                m_setSites, m_setRacks, m_setMachines);
            }

        /**
         * Return true iff the specified members are mutually "strong".
         *
         * @param member1  the first member to compare
         * @param member2  the second member to compare
         *
         * @return true iff the specified members are mutually "strong"
         */
        protected boolean isStrong(Member member1, Member member2)
            {
            switch (m_nStrength)
                {
                default:
                case NODE_SAFE:
                    return member1.getId() != member2.getId();

                case MACHINE_SAFE:
                    return member1.getMachineId() != member2.getMachineId();

                case RACK_SAFE:
                    return !Base.equals(member1.getRackName(), member2.getRackName());

                case SITE_SAFE:
                    return !Base.equals(member1.getSiteName(), member2.getSiteName());
                }
            }

        /**
         * Return the site count.
         *
         * @return the site count
         */
        public int getSiteCount()
            {
            return m_setSites.size();
            }

        /**
         * Return the rack count.
         *
         * @return the rack count
         */
        public int getRackCount()
            {
            return m_setRacks.size();
            }

        /**
         * Return the site count.
         *
         * @return the site count
         */
        public int getMachineCount()
            {
            return m_setMachines.size();
            }

        /**
         * Return a human-readable description string of this backup-strength.
         *
         * @return a human-readable description string of this backup-strength
         */
        public String getDescription()
            {
            switch (m_nStrength)
                {
                default:
                    return "ENDANGERED";

                case NODE_SAFE:
                    return "NODE-SAFE";

                case MACHINE_SAFE:
                    return "MACHINE-SAFE";

                case RACK_SAFE:
                    return "RACK-SAFE";

                case SITE_SAFE:
                    return "SITE-SAFE";
                }
            }

        // ----- Object methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return "BackupStrength{" + getDescription() + "}";
            }

        // ----- constants and data members -------------------------------

        /**
         * Node-safety (members are different).
         */
        protected static final int NODE_SAFE    = 1;

        /**
         * Machine-safety (members are on different machines).
         */
        protected static final int MACHINE_SAFE = 2;

        /**
         * Rack-safety (members are on different racks).
         */
        protected static final int RACK_SAFE    = 3;

        /**
         * Site-safety (members are on different sites).
         */
        protected static final int SITE_SAFE    = 4;

        /**
         * The strength (one of the *_SAFE constants).
         */
        protected int m_nStrength;

        /**
         * The set of site names.
         */
        protected Set m_setSites;

        /**
         * The set of rack names.
         */
        protected Set m_setRacks;

        /**
         * The set of machine names.
         */
        protected Set m_setMachines;
        }

    // ----- constants and data members -----------------------------------

    /**
     * Comparator used to provide arbitrary (equals-compatible) comparisons
     * between members.
     */
    protected static final Comparator MEMBERID_COMPARATOR =
        (o1, o2) -> ((Member) o1).getId() - ((Member) o2).getId();

    /**
     * The message returned by SimpleStrategyMBean when the distribution coordinator has not done its
     * first analysis yet.
     */
    protected static final String MSG_NO_RESULT = "There are no distribution analysis results.";

    /**
     * The message returned by SimpleStrategyMBean when all suggested distributions have completed
     * and none are in-progress or scheduled.
     */
    protected static final String MSG_NO_PENDING = "No distributions are currently scheduled for this service.";

    /**
     * The DistributionManager.
     */
    protected DistributionManager m_manager;

    /**
     * The last analysis context.
     */
    protected AnalysisContext m_ctxLast;

    /**
     * The Set of ownership-enabled members at the time of the last analysis.
     */
    protected Set m_setOwnersLast;

    /**
     * The Map containing the last distribution suggested by this strategy.
     */
    protected Map<Ownership, PartitionSet> m_mapSuggestLast;

    /**
     * The JMXPartitionStats that hold the last updated jmx attributes.
     */
    protected JMXPartitionStats m_statsPartition;

    /**
     * True if JMXPartitionStats should be updated.
     */
    protected boolean m_fRefresh;

    /**
     * Trivial distribution is a concept introduced in SE One, where a trivial
     * two servers topology behaves in a special way - holding all the primary
     * partitions on the ownership senior and all the backups on the other
     * server. As soon as a third server is added, this functionality should
     * be turned off for good and a standard algorithm take place.
     */
    private boolean m_fTrivialDistribution =
            Config.getBoolean("coherence.distribution.2server", false);

    /**
     * The amount of time in ms to delay the analysis after a
     * distribution suggestion has been made and before it is carried out.
     */
    protected long m_cPlanCompletionDelay;
    }
