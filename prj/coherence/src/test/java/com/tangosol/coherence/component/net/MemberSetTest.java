/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net;

import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet.TRANSPORT_COMPATIBILITY;
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

    // unit test for COH-28004
    @Test
    public void validateCompatForMemberJoiningVersion()
        {
        MasterMemberSet setMembers = instantiateMasterMemberSet(new int[] {1,2,3,4,5});

        // simulate various MEMBER states encountered during Coherence member joining cluster
        setMembers.setState(1, ServiceMemberSet.MEMBER_JOINED);
        setMembers.setState(2, ServiceMemberSet.MEMBER_JOINED);
        setMembers.setState(3, ServiceMemberSet.MEMBER_JOINING);
        setMembers.setState(4, ServiceMemberSet.MEMBER_NEW);
        setMembers.setState(5, ServiceMemberSet.MEMBER_LEAVING);


        // validate MasterMemberset.getDescription() from joined cluster log message
        int nCompatCount = 0;

        for (String sLine : setMembers.getDescription().split("\\R"))
            {
            if (sLine.contains("JOINING") || sLine.contains("NEW"))
                {
                assertThat("validating " + sLine + " contains \" + TRANSPORT_COMPATIBILITY\"",
                           sLine.contains(TRANSPORT_COMPATIBILITY), is(true));
                nCompatCount++;
                }
            else if (sLine.contains("JOINED") || sLine.contains("LEAVING"))
                {
                assertThat("validating " + sLine + " does not contain " + TRANSPORT_COMPATIBILITY,
                            sLine.contains(TRANSPORT_COMPATIBILITY), is(false));
                }
            }
        assertThat(nCompatCount, is(2));
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

    protected static MasterMemberSet instantiateMasterMemberSet(int[] anMember)
        {
        MasterMemberSet setMembers = new MasterMemberSet();
        for (int nMember : anMember)
            {
            setMembers.add(MemberTest.instantiateMember(nMember));
            }
        return setMembers;
        }
    }