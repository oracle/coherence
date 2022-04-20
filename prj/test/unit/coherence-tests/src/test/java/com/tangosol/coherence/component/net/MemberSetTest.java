/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import static org.junit.Assert.*;

/**
 * Regression tests for COH-5329, COH-11267.
 *
 * @author gg Jul 30, 2010
 */
public class MemberSetTest
    {
    @Test
    public void testEquals()
        {
        MemberSet setMembers = instantiateMemberSet(new int[] {1,2,3});
        assertEquals(setMembers, setMembers);

        Set setCopy = new HashSet(setMembers);

        assertTrue("Not equal 1", setCopy.equals(setMembers));
        assertTrue("Not equal 2", setMembers.equals(setCopy));
        }

    @Test
    public void testRandom()
        {
        MemberSet setMembers = instantiateMemberSet(new int[] {1,2,3});

        int nMember = setMembers.random();
        assertThat(nMember, is(not(0)));
        setMembers.remove(nMember);

        nMember = setMembers.random();
        assertThat(nMember, is(not(0)));
        setMembers.remove(nMember);

        nMember = setMembers.random();
        assertThat(nMember, is(not(0)));
        setMembers.remove(nMember);

        nMember = setMembers.random();
        assertThat(nMember, is(0));
        }

    protected static MemberSet instantiateMemberSet(int[] anMember)
        {
        MemberSet setMembers = new ActualMemberSet();
        for (int nMember : anMember)
            {
            setMembers.add(MemberTest.instantiateMember(nMember));
            }
        return setMembers;
        }
    }