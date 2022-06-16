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
 * Unit tests for ReadWriteLockHolder.
 *
 * @author Aleks Seovic  2021.11.19
 */
public class ReadWriteLockHolderTest
    {
    public static final Member SINGLETON_MEMBER_1 = createRemoteMember(null, 8088);
    public static final Member SINGLETON_MEMBER_2 = createRemoteMember(null, 8088);
    public static final Member SINGLETON_MEMBER_3 = createRemoteMember(null, 8088);

    @Test
    public void shoudWriteLockAndUnlock()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(owner), is(true));
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedBy(owner), is(true));
        assertThat(lock.isWriteLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlockWrite(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isLockedBy(owner), is(false));
        assertThat(lock.isLockedByMember(owner.getMemberId()), is(false));
        }

    @Test
    public void shoudReadLockAndUnlock()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockRead(owner), is(true));
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.isReadLockedBy(owner), is(true));
        assertThat(lock.isReadLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlockRead(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.isLockedBy(owner), is(false));
        assertThat(lock.isLockedByMember(owner.getMemberId()), is(false));
        }

    @Test
    public void shoudWriteLockReentrantly()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(owner), is(true));
        assertThat(lock.lockWrite(owner), is(true));
        assertThat(lock.isWriteLocked(), is(true));
        assertThat(lock.isWriteLockedBy(owner), is(true));
        assertThat(lock.isWriteLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlockWrite(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudReadLockReentrantly()
        {
        LockOwner owner = new LockOwner(SINGLETON_MEMBER_1, 1);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockRead(owner), is(true));
        assertThat(lock.lockRead(owner), is(true));
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.isReadLockedBy(owner), is(true));
        assertThat(lock.isReadLockedByMember(owner.getMemberId()), is(true));
        assertThat(lock.unlockRead(owner), is(true));
        assertThat(lock.isLocked(), is(false));
        }

    @Test
    public void shoudAddWriteLockToPendingSet()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(o1), is(true));
        assertThat(lock.lockWrite(o2), is(false));
        assertThat(lock.isWriteLockedBy(o1), is(true));
        assertThat(lock.unlockWrite(o2), is(false));
        assertThat(lock.unlockWrite(o1), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lockWrite(o2), is(true));
        assertThat(lock.isWriteLockedBy(o2), is(true));
        }

    @Test
    public void shoudAddReadLockToPendingSet()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(o1), is(true));
        assertThat(lock.lockRead(o2), is(false));
        assertThat(lock.isWriteLockedBy(o1), is(true));
        assertThat(lock.unlockRead(o2), is(false));
        assertThat(lock.unlockWrite(o1), is(true));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lockRead(o2), is(true));
        assertThat(lock.isReadLockedBy(o2), is(true));
        }

    @Test
    public void shoudRemoveLocksForMember()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        LockOwner o3 = new LockOwner(SINGLETON_MEMBER_3, 3);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(o1), is(true));
        assertThat(lock.lockWrite(o2), is(false));
        assertThat(lock.lockRead(o3), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.removeLocksFor(o2.getMemberId()), is(false));
        assertThat(lock.removeLocksFor(o3.getMemberId()), is(false));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(true));
        assertThat(lock.removeLocksFor(o1.getMemberId()), is(false));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lockRead(o3), is(true));
        assertThat(lock.isReadLocked(), is(true));
        assertThat(lock.removeLocksFor(o3.getMemberId()), is(true));
        assertThat(lock.isReadLocked(), is(false));
        }

    @Test
    public void shoudRetainLocksForMember()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        LockOwner o2 = new LockOwner(SINGLETON_MEMBER_2, 2);
        LockOwner o3 = new LockOwner(SINGLETON_MEMBER_3, 3);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();
        assertThat(lock.lockWrite(o1), is(true));
        assertThat(lock.lockWrite(o2), is(false));
        assertThat(lock.lockRead(o3), is(false));
        assertThat(lock.isLockedBy(o1), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId(), o3.getMemberId())), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId(), o3.getMemberId())), is(false));
        assertThat(lock.isLocked(), is(false));
        assertThat(lock.lockWrite(o1), is(true));
        assertThat(lock.retainLocksFor(Set.of(o1.getMemberId(), o3.getMemberId())), is(false));
        assertThat(lock.isLocked(), is(true));
        assertThat(lock.unlockWrite(o1), is(true));
        assertThat(lock.lockRead(o1), is(true));
        assertThat(lock.lockRead(o2), is(true));
        assertThat(lock.retainLocksFor(Set.of(o2.getMemberId())), is(true));
        assertThat(lock.isReadLockedBy(o1), is(false));
        assertThat(lock.isReadLockedBy(o2), is(true));
        }

    @Test
    public void shoudReturnDetailsFromToString()
        {
        LockOwner o1 = new LockOwner(SINGLETON_MEMBER_1, 1);
        ReadWriteLockHolder lock = new ReadWriteLockHolder();

        assertThat(lock.toString(), is("ReadWriteLockHolder{writeLocked=false, readLocked=false, writeLockOwner=null, readLocks=[]}"));
        lock.lockWrite(o1);
        assertThat(lock.toString(), is(String.format("ReadWriteLockHolder{writeLocked=true, readLocked=false, writeLockOwner=LockOwner{memberId=%s, threadId=1, client=false}, readLocks=[]}", o1.getMemberId())));
        lock.retainLocksFor(Set.of());

        lock.lockRead(o1);
        assertThat(lock.toString(), is(String.format("ReadWriteLockHolder{writeLocked=false, readLocked=true, writeLockOwner=null, readLocks=[LockOwner{memberId=%s, threadId=1, client=false}]}", o1.getMemberId())));
        }
    }
