/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.locks.internal;

import com.oracle.coherence.concurrent.locks.LockOwner;

import com.tangosol.util.UID;

import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for ExclusiveLockHolder.
 *
 * @author Aleks Seovic  2021.11.19
 */
public class ExclusiveLockHolderTest
    {
    @Test
    public void shoudLockAndUnlock()
        {
        LockOwner owner = new LockOwner(new UID(), 1);
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
        LockOwner owner = new LockOwner(new UID(), 1);
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
        LockOwner o1 = new LockOwner(new UID(), 1);
        LockOwner o2 = new LockOwner(new UID(), 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.isLockedByMember(o1.getMemberId()), is(true));
        assertThat(lock.isPending(o2), is(true));
        assertThat(lock.isPendingForMember(o2.getMemberId()), is(true));
        assertThat(lock.unlock(o2), is(false));
        assertThat(lock.unlock(o1), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudRemoveLocksForMember()
        {
        LockOwner o1 = new LockOwner(new UID(), 1);
        LockOwner o2 = new LockOwner(new UID(), 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.isPending(o2), is(true));
        assertThat(lock.removeLocksFor(o2.getMemberId()), is(true));
        assertThat(lock.removeLocksFor(o2.getMemberId()), is(false));
        assertThat(lock.isPending(o2), is(false));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(true));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(false));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudRetainLocksForMember()
        {
        LockOwner o1 = new LockOwner(new UID(), 1);
        LockOwner o2 = new LockOwner(new UID(), 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.lock(o2), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.isPending(o2), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(false));
        assertThat(lock.isPending(o2), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lock(o1), is(true));
        assertThat(lock.retainLocksFor(Set.of(o1.getMemberId())), is(true));
        assertThat(lock.retainLocksFor(Set.of(o1.getMemberId())), is(false));
        assertThat(lock.isPending(o2), is(false));
        assertThat(lock.isLocked(), is(true));
        }

    @Test
    public void shoudReturnDetailsFromToString()
        {
        LockOwner o1 = new LockOwner(new UID(), 1);
        LockOwner o2 = new LockOwner(new UID(), 2);
        ExclusiveLockHolder lock = new ExclusiveLockHolder();

        assertThat(lock.toString(), is("ExclusiveLockHolder{locked=false, owner=null, pendingLocks=[]}"));
        lock.lock(o1);
        assertThat(lock.toString(), is(String.format("ExclusiveLockHolder{locked=true, owner=LockOwner{memberId=%s, threadId=1}, pendingLocks=[]}", o1.getMemberId())));
        lock.lock(o2);
        assertThat(lock.toString(), is(String.format("ExclusiveLockHolder{locked=true, owner=LockOwner{memberId=%s, threadId=1}, pendingLocks=[LockOwner{memberId=%s, threadId=2}]}", o1.getMemberId(), o2.getMemberId())));
        }
    }
