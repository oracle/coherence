/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.LockOwner;

import com.tangosol.net.Member;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static com.oracle.coherence.concurrent.TestUtils.createRemoteMember;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for ExclusiveLockHolder.
 *
 * @author Aleks Seovic  2021.11.19
 */
public class ExclusiveLockHolderTest
    {
    public static final Member SINGLETON_MEMBER_1 = createRemoteMember(null, 8088);
    public static final Member SINGLETON_MEMBER_2 = createRemoteMember(null, 8088);

    @Test
    public void shoudLockAndUnlock()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(owner), is(true));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isLockedBy(owner), is(true));
        assertThat(lock.isLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlock(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isLockedBy(owner), is(false));
        assertThat(lock.isLockedByMember(owner.getMemberId()), is(false));
        }

    @Test
    public void shoudLockReentrantly()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(owner), is(true));
        assertThat(lock.lock(owner), is(true));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isLockedBy(owner), is(true));
        assertThat(lock.isLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlock(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudAddLockToPendingSet()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.isLockedByMember(o1.getMemberId()), is(true));
        assertThat(lock.unlock(o2), is(false));
        assertThat(lock.unlock(o1), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudRemoveLocksForMember()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.removeLocksFor(o2.getMemberId()), is(false));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(true));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(false));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudRetainLocksForMember()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(false));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lock(o2), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(false));
        assertThat(lock.retainLocksFor(Set.of(o1.getMemberId())), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudReturnDetailsFromToString()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();

        assertThat(lock.toString(), is("ExclusiveLockHolder{locked=false, owner=null}"));
        lock.lock(o1);
        assertThat(lock.toString(), is(String.format("ExclusiveLockHolder{locked=true, owner=LockOwner{memberId=%s, threadId=1, client=false}}", o1.getMemberId())));
        }
    }
