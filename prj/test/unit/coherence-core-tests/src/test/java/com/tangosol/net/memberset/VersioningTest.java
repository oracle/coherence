/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.memberset;

import com.tangosol.coherence.component.net.Member;

import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;

import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeAll;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Test to validate version encoding/decoding/toString.
 */
public class VersioningTest
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeAll
    public static void beforeClass() throws Exception
        {
        Method m = ServiceMemberSet.class.getDeclaredMethod("toVersionString",
                                                            int.class,
                                                            boolean.class);
        m.setAccessible(true);

        s_methodToVersionString = m;
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testCOH26149()
            throws Exception
        {
        int nCommercial  = ServiceMemberSet.encodeVersion(14,1,1,0,0);
        int nCommunity   = ServiceMemberSet.encodeVersion(22, 9, 22, 9, 0);
        int nFeaturePack = ServiceMemberSet.encodeVersion(22, 6, 0);

        // ensure we have the expected encodings
        assertThat(nCommercial, is(235147264));
        assertThat(nCommunity, is(371548736));
        assertThat(nFeaturePack, is(235148672));

        // validate the String result
        assertThat(toVersionString(nCommercial), is("14.1.1.0.0"));
        assertThat(toVersionString(nCommunity), is("22.9.22.9.0"));
        assertThat(toVersionString(nFeaturePack), is("14.1.1.2206.0"));

        Member mockMember1 = mock(Member.class);
        when(mockMember1.getId()).thenReturn(1);

        Member mockMember2 = mock(Member.class);
        when(mockMember2.getId()).thenReturn(2);

        Member mockMember3 = mock(Member.class);
        when(mockMember3.getId()).thenReturn(3);

        MasterMemberSet masterMemberSet = new MasterMemberSet();
        masterMemberSet.setServiceVersion(1,"14.1.1.0.0");
        masterMemberSet.setServiceVersion(2,"22.9.22.9.0");
        masterMemberSet.setServiceVersion(3,"14.1.1.2206.0");

        masterMemberSet.add(mockMember1);
        masterMemberSet.add(mockMember2);
        masterMemberSet.add(mockMember3);

        assertThat(masterMemberSet.getServiceVersionExternal(1), is("14.1.1.0.0"));
        assertThat(masterMemberSet.getServiceVersionExternal(2), is("22.09.0"));
        assertThat(masterMemberSet.getServiceVersionExternal(3), is("14.1.1.2206.0"));

        // now test handling of the month encoding for the feature pack
        // if year is 22 and encoded month is 0 (indicating month 6 previously, now month 3),
        nFeaturePack = ServiceMemberSet.encodeVersion(22, 3, 0);
        assertThat(toVersionString(nFeaturePack), is("14.1.1.2206.0"));

        nFeaturePack = ServiceMemberSet.encodeVersion(22, 9, 0);
        assertThat(toVersionString(nFeaturePack), is("14.1.1.2209.0"));

        // if year is 22 and encoded month is 1 (indicating month 12 previously, now month 9),
        nFeaturePack = ServiceMemberSet.encodeVersion(22, 12, 0);
        assertThat(toVersionString(nFeaturePack), is("14.1.1.2209.0"));

        // if year is 23 or greater, then 03 will be encoded
        nFeaturePack = ServiceMemberSet.encodeVersion(23, 3, 0);
        assertThat(toVersionString(nFeaturePack), is("14.1.1.2303.0"));
        }

    // ----- helper methods -------------------------------------------------

    private String toVersionString(int nEncodedVersion)
            throws Exception
        {
        return s_methodToVersionString.invoke(ServiceMemberSet.class,
                                              nEncodedVersion,
                                              true).toString();
        }

    // ----- data members ---------------------------------------------------

    private static Method s_methodToVersionString;
    }
