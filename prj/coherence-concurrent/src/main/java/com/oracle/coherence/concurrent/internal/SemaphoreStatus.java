/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal;

import com.oracle.coherence.concurrent.PermitAcquirer;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Member;
import com.tangosol.net.ServiceInfo;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.UUID;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A data structure that encapsulates server-side semaphore logic.
 *
 * @since 21.12
 * @author Vaso Putica 2021.11.30
 */
public class SemaphoreStatus
        implements ExternalizableLite, PortableObject
    {
    /**
     * Default constructor (necessary for serialization).
     */
    @SuppressWarnings("unused")
    public SemaphoreStatus()
        {
        }

    /**
     * Create an instance of {@code SemaphoreStatus}.
     *
     * @param initialState  the initial number of permits available
     */
    public SemaphoreStatus(int initialState)
        {
        m_initialPermits = initialState;
        m_permits = initialState;
        }

    /**
     * Acquires the given number of permits for specified acquirer.
     *
     * @param acquirer  identity of the acquirer
     * @param permits   the number of permits to acquire
     *
     * @return true if permits were acquired; otherwise returns false
     */
    public boolean acquire(PermitAcquirer acquirer, int permits)
        {
        if (m_permits < permits)
            {
            return false;
            }
        m_permits -= permits;
        m_memberId = acquirer.getMemberId();
        m_permitsMap.compute(acquirer, (o, acquirersPermits) ->
                acquirersPermits == null
                ? permits
                : acquirersPermits + permits);
        return true;
        }

    /**
     * Releases the given number of permits, returning them to the semaphore.
     *
     * @param acquirer  identity of the acquirer
     * @param releases  the number of permits to release
     *
     * @return true if permits were released; otherwise returns false
     */
    public boolean release(PermitAcquirer acquirer, int releases)
        {
        int current = m_permits;
        int next = current + releases;
        if (next < current)
            {
            throw new Error("Maximum permit count exceeded");
            }
        m_permits = next;
        m_memberId = acquirer.getMemberId();
        m_permitsMap.compute(acquirer, (acq, acquirerPermits) ->
            {
            if (acquirerPermits == null || acquirerPermits - releases <= 0)
                {
                return null;
                }
            return acquirerPermits - releases;
            });
        return true;
        }

    /**
     * Drain permits from the given {@link PermitAcquirer}.
     *
     * @param acquirer  the {@link PermitAcquirer}
     *
     * @return the number of permits drained
     */
    public int drainPermits(PermitAcquirer acquirer)
        {
        int current = m_permits;
        m_permits = 0;
        m_memberId = acquirer.getMemberId();
        m_permitsMap.compute(acquirer, (a, acquirersPermits) ->
            {
            int permits = acquirersPermits == null
                          ? current
                          : acquirersPermits + current;
            return permits == 0
                   ? null
                   : permits;
            });
        return current;
        }

    /**
     * Reduces the number of permits for the given {@link PermitAcquirer}.
     *
     * @param acquirer    the {@link PermitAcquirer}
     * @param reductions  the number of reductions
     *
     * @return  the number of permits
     */
    public int reducePermits(PermitAcquirer acquirer, int reductions)
        {
        int current = m_permits;
        int next = current - reductions;
        if (next > current)
            {
            throw new Error("Permit count underflow");
            }
        m_permits = next;
        m_memberId = acquirer.getMemberId();
        return m_permits;
        }

    /**
     * Returns the current number of permits available.
     *
     * @return the number of permits available
     */
    public int getPermits()
        {
        return m_permits;
        }

    /**
     * Sets the number of permits available.
     *
     * @param permits the number of permits available
     */
    public void setPermits(int permits)
        {
        m_permits = permits;
        }

    /**
     * Returns the identity of member who most recently updated the semaphore.
     *
     * @return the identity of member who most recently updated the semaphore.
     */
    public UUID getMember()
        {
        return m_memberId;
        }

    /**
     * Sets the identity of member who most recently updated the semaphore.
     *
     * @param member the identity of member who most recently updated the
     *               semaphore
     */
    public void setMember(UUID member)
        {
        m_memberId = member;
        }

    /**
     * Return {@code true} if this semaphore is currently acquired by the specified
     * {@link PermitAcquirer}.
     *
     * @param acquirer  the {@link PermitAcquirer}
     *
     * @return {@code true} if this semaphore is currently acquired by the specified
     *                      {@link PermitAcquirer}
     */
    public boolean isAcquiredBy(PermitAcquirer acquirer)
        {
        return m_permitsMap.containsKey(acquirer);
        }

    /**
     * Remove all the permits that are not acquired by one of the specified
     * members.
     *
     * @param setMemberIds the UUIDs of the member to retain the permits for
     *
     * @return {@code true} if this semaphore was modified
     */
    protected boolean retainPermitsFor(Set<UUID> setMemberIds)
        {
        boolean fModified = false;
        Iterator<Map.Entry<PermitAcquirer, Integer>> it = m_permitsMap.entrySet().iterator();
        while (it.hasNext())
            {
            Map.Entry<PermitAcquirer, Integer> entry = it.next();
            PermitAcquirer acquirer = entry.getKey();
            if (!acquirer.isClient() && !setMemberIds.contains(acquirer.getMemberId()))
                {
                it.remove();
                fModified = true;
                m_permits += entry.getValue();
                }
            }
        return fModified;
        }

    /**
     * Remove all the permits that are acquired by a specified member.
     *
     * @param memberId the UUID of a member to remove the permits for
     *
     * @return {@code true} if this holder was modified
     */
    protected boolean removePermitsFor(UUID memberId)
        {
        boolean fModified = false;
        Iterator<Map.Entry<PermitAcquirer, Integer>> it = m_permitsMap.entrySet().iterator();
        while (it.hasNext())
            {
            Map.Entry<PermitAcquirer, Integer> entry = it.next();
            if (entry.getKey().getMemberId().equals(memberId))
                {
                it.remove();
                fModified = true;
                m_permits += entry.getValue();
                }
            }
        return fModified;
        }

    /**
     * Returns number of permits that were used to initialise this semaphore.
     *
     * @return initial number of permits that were used to initialise this semaphore.
     */
    public int getInitialPermits()
        {
        return m_initialPermits;
        }

    /**
     * Returns an immutable map of acquirers and permits that they acquired.
     *
     * @return an immutable map of acquirers and permits that they acquired
     */
    public Map<PermitAcquirer, Integer> getPermitsMap()
        {
        return Collections.unmodifiableMap(m_permitsMap);
        }

    @Override
    public String toString()
        {
        return "SemaphoreStatus{" +
               "m_permits=" + m_permits +
               ", m_memberId=" + m_memberId +
               ", m_initialPermits=" + m_initialPermits +
               ", m_permitsMap=" + m_permitsMap +
               '}';
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_permits        = ExternalizableHelper.readInt(in);
        m_memberId       = ExternalizableHelper.readObject(in);
        m_initialPermits = ExternalizableHelper.readInt(in);
        ExternalizableHelper.readMap(in, m_permitsMap = new HashMap<>(), null);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeInt(out, m_permits);
        ExternalizableHelper.writeObject(out, m_memberId);
        ExternalizableHelper.writeInt(out, m_initialPermits);
        ExternalizableHelper.writeMap(out, m_permitsMap);

        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_permits        = in.readInt(0);
        m_memberId       = in.readObject(1);
        m_initialPermits = in.readInt(2);
        m_permitsMap     = in.readMap(3, new HashMap<>());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0,        m_permits);
        out.writeObject(1,     m_memberId);
        out.writeInt(2,        m_initialPermits);
        out.writeMap(3,        m_permitsMap);
        }

    // ----- inner class RemovePermits --------------------------------------

    /**
     * An EntryProcessor that will remove acquired permits for the provided
     * member ID, or all the permits for the members that are not in the cluster
     * any longer.
     */
    public static class RemovePermits
            implements InvocableMap.EntryProcessor<String, SemaphoreStatus, Void>, ExternalizableLite, PortableObject
        {
        /**
         * Default constructor (necessary for the ExternalizableLite interface).
         */
        public RemovePermits()
            {
            }

        /**
         * Create an instance of {@code SemaphoreStatus}.
         *
         * @param memberId the UUID of the Coherence member
         */
        public RemovePermits(UUID memberId)
            {
            m_memberId = memberId;
            }

        @SuppressWarnings("unchecked")
        @Override
        public Void process(InvocableMap.Entry<String, SemaphoreStatus> entry)
            {
            SemaphoreStatus status = entry.getValue();
            if (m_memberId == null) // clean up for invalid member UUIDs
                {
                ServiceInfo info = ((BinaryEntry<String, SemaphoreStatus>)entry)
                        .getContext().getCacheService().getInfo();
                Set<Member> setServiceMembers = info.getServiceMembers();
                Set<UUID> setValidMemberIds = setServiceMembers.stream()
                        .map(Member::getUuid)
                        .collect(Collectors.toSet());
                if (status.retainPermitsFor(setValidMemberIds))
                    {
                    entry.setValue(status);
                    }
                }
            else
                {
                if (status.removePermitsFor(m_memberId))
                    {
                    entry.setValue(status);
                    }
                }
            return null;
            }

        // ----- ExternalizableLite interface -----------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_memberId = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_memberId);
            }

        // ----- PortableObject interface ---------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_memberId = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_memberId);
            }

        // ---- data members ----------------------------------------------------

        /**
         * The member UUID to remove all the permits for.
         */
        protected UUID m_memberId;
        }

    // ---- data members ----------------------------------------------------

    /**
     * The current number of permits available.
     */
    private int m_permits;

    /**
     * The identity of a member who most recently updated the semaphore.
     */
    private UUID m_memberId;

    /**
     * Number of permits used to initialise
     */
    private int m_initialPermits;

    /**
     * The map of acquirers and permits that they acquired.
     */
    Map<PermitAcquirer, Integer> m_permitsMap = new HashMap<>();
    }
