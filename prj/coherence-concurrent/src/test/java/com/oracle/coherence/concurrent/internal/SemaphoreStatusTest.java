/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.internal;

import com.oracle.coherence.concurrent.PermitAcquirer;

import com.tangosol.net.Member;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static com.oracle.coherence.concurrent.TestUtils.createRemoteMember;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link SemaphoreStatus}.
 *
 * @author Vaso Putica  2021.12.01
 */
public class SemaphoreStatusTest
    {
    public static final Member SINGLETON_MEMBER_1 = createRemoteMember(null, 8088);
    public static final Member SINGLETON_MEMBER_2 = createRemoteMember(null, 8088);
    public static final Member SINGLETON_MEMBER_3 = createRemoteMember(null, 8088);

    @Test
    void shouldAcquireSingle()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(2);
        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(1));
        assertThat(status.getMember(), equalTo(acquirer.getMemberId()));
        assertThat(status.m_permitsMap.get(acquirer), is(1));

        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(2));

        assertThat(status.acquire(acquirer, 1), is(false));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(2));
        }

    @Test
    void shouldAcquireMulti()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(5);
        assertThat(status.acquire(acquirer, 3), is(true));
        assertThat(status.getPermits(), is(2));
        assertThat(status.getMember(), equalTo(acquirer.getMemberId()));
        assertThat(status.m_permitsMap.get(acquirer), is(3));

        assertThat(status.acquire(acquirer, 3), is(false));
        assertThat(status.getPermits(), is(2));
        assertThat(status.m_permitsMap.get(acquirer), is(3));

        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(1));
        assertThat(status.m_permitsMap.get(acquirer), is(4));
        }

    @Test
    void shouldReleaseSingle()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(2);
        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(1));
        assertThat(status.m_permitsMap.get(acquirer), is(1));

        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(2));

        assertThat(status.acquire(acquirer, 1), is(false));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(2));

        assertThat(status.release(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(1));
        assertThat(status.m_permitsMap.get(acquirer), is(1));

        assertThat(status.acquire(acquirer, 1), is(true));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(2));
        }

    @Test
    void shouldReleaseMulti()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(5);
        assertThat(status.acquire(acquirer, 4), is(true));
        assertThat(status.getPermits(), is(1));
        assertThat(status.m_permitsMap.get(acquirer), is(4));

        assertThat(status.release(acquirer, 2), is(true));
        assertThat(status.getPermits(), is(3));
        assertThat(status.m_permitsMap.get(acquirer), is(2));
        }

    @Test
    void shouldReleaseBeforeAcquire()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(3);
        assertThat(status.release(acquirer, 5), is(true));
        assertThat(status.getPermits(), is(8));
        assertThat(status.m_permitsMap.containsKey(acquirer), is(false));
        }

    @Test
    void testReleaseMoreThanAcquired()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(3);
        assertThat(status.acquire(acquirer, 2), is(true));
        assertThat(status.m_permitsMap.get(acquirer), is(2));
        assertThat(status.release(acquirer, 5), is(true));
        assertThat(status.getPermits(), is(6));
        assertThat(status.m_permitsMap.containsKey(acquirer), is(false));
        }

    @Test
    void testMultipleAcquirers()
        {
        PermitAcquirer acquirer1 = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        PermitAcquirer acquirer2 = new PermitAcquirer(SINGLETON_MEMBER_2, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(10);

        assertThat(status.acquire(acquirer1, 3), is(true));
        assertThat(status.getPermits(), is(7));
        assertThat(status.acquire(acquirer2, 5), is(true));
        assertThat(status.getPermits(), is(2));
        assertThat(status.m_permitsMap.get(acquirer1), is(3));
        assertThat(status.m_permitsMap.get(acquirer2), is(5));

        assertThat(status.release(acquirer1, 5), is(true));
        assertThat(status.getPermits(), is(7));
        assertThat(status.m_permitsMap.containsKey(acquirer1), is(false));
        assertThat(status.m_permitsMap.get(acquirer2), is(5));
        }

    @Test
    void testReducePermits()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(10);

        assertThat(status.reducePermits(acquirer, 7), is(3));
        assertThat(status.getPermits(), is(3));
        assertThat(status.m_permitsMap.get(acquirer), nullValue());

        status = new SemaphoreStatus(10);
        assertThat(status.reducePermits(acquirer, 15), is(-5));
        assertThat(status.getPermits(), is(-5));
        assertThat(status.m_permitsMap.get(acquirer), nullValue());

        status = new SemaphoreStatus(10);
        assertThat(status.acquire(acquirer, 3), is(true));
        assertThat(status.m_permitsMap.get(acquirer), is(3));
        assertThat(status.reducePermits(acquirer, 15), is(-8));
        assertThat(status.getPermits(), is(-8));
        assertThat(status.m_permitsMap.get(acquirer), is(3));
        }

    @Test
    void testReducePermitsUnderflow()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(-2);

        Error e = assertThrows(Error.class, () -> status.reducePermits(acquirer, Integer.MAX_VALUE));
        assertThat(e.getMessage(), is("Permit count underflow"));
        }

    @Test
    void testDrainPermits()
        {
        PermitAcquirer acquirer = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(10);
        assertThat(status.drainPermits(acquirer), is(10));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(10));

        acquirer = new PermitAcquirer(SINGLETON_MEMBER_2, Thread.currentThread().getId());
        status = new SemaphoreStatus(10);
        assertThat(status.acquire(acquirer, 2), is(true));
        assertThat(status.drainPermits(acquirer), is(8));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), is(10));

        acquirer = new PermitAcquirer(SINGLETON_MEMBER_3, Thread.currentThread().getId());
        status = new SemaphoreStatus(0);
        assertThat(status.drainPermits(acquirer), is(0));
        assertThat(status.getPermits(), is(0));
        assertThat(status.m_permitsMap.get(acquirer), nullValue());
        }

    @Test
    void testRetainPermits()
        {
        PermitAcquirer acquirer1 = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        PermitAcquirer acquirer2 = new PermitAcquirer(SINGLETON_MEMBER_2, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(10);

        assertThat(status.acquire(acquirer1, 3),is(true));
        assertThat(status.acquire(acquirer2, 2),is(true));
        assertThat(status.getPermits(), is(5));
        assertThat(status.m_permitsMap.get(acquirer1), is(3));
        assertThat(status.m_permitsMap.get(acquirer2), is(2));

        assertThat(status.retainPermitsFor(Collections.singleton(acquirer1.getMemberId())), is(true));
        assertThat(status.m_permitsMap.get(acquirer1), is(3));
        assertThat(status.m_permitsMap.get(acquirer2), nullValue());
        assertThat(status.getPermits(), is(7));
        }

    @Test
    void testRemovePermits()
        {
        PermitAcquirer acquirer1 = new PermitAcquirer(SINGLETON_MEMBER_1, Thread.currentThread().getId());
        PermitAcquirer acquirer2 = new PermitAcquirer(SINGLETON_MEMBER_2, Thread.currentThread().getId());
        SemaphoreStatus status = new SemaphoreStatus(10);

        assertThat(status.acquire(acquirer1, 3),is(true));
        assertThat(status.acquire(acquirer2, 2),is(true));
        assertThat(status.getPermits(), is(5));
        assertThat(status.m_permitsMap.get(acquirer1), is(3));
        assertThat(status.m_permitsMap.get(acquirer2), is(2));

        assertThat(status.removePermitsFor(acquirer1.getMemberId()), is(true));
        assertThat(status.m_permitsMap.get(acquirer1), nullValue());
        assertThat(status.m_permitsMap.get(acquirer2), is(2));
        assertThat(status.getPermits(), is(8));
        }
    }
