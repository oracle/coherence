/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.tangosol.net.Member;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Base;
import com.tangosol.util.UID;

import java.util.Date;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

/**
 * UnitTests for GUIDHelper.
 *
 * @author rhl 2012.07.09
 */
public class GUIDHelperTest
    {
    @Test
    public void testGenerateGUID()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID1 = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        String sGUID = String.format("%d-%x-%x-%d", 42, 17742L, ldt,
                member1.getId());
        assertEquals(sGUID, sGUID1);

        Member member2 = getMockMember(2);

        String sGUID2 = GUIDHelper.generateGUID(0, 0L, ldt, member2);

        sGUID = String.format("%d-%x-%x-%d", 0, 0L, ldt,
                member2.getId());
        assertEquals(sGUID, sGUID2);
        }

    @Test
    public void testValidateGUID()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        assertTrue (GUIDHelper.validateGUID(sGUID));
        assertFalse(GUIDHelper.validateGUID(null));
        assertFalse(GUIDHelper.validateGUID("foobar"));
        assertTrue (GUIDHelper.validateGUID("1-1-1-1"));
        assertFalse(GUIDHelper.validateGUID("g-1-1-1"));
        assertFalse(GUIDHelper.validateGUID("1-g-1-1"));
        assertFalse(GUIDHelper.validateGUID("1-1-g-1"));
        assertFalse(GUIDHelper.validateGUID("1-1-1-g"));
        }

    @Test
    public void testGetPartition()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID1 = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        assertEquals(42, GUIDHelper.getPartition(sGUID1));

        Member member2 = getMockMember(2);

        String sGUID2 = GUIDHelper.generateGUID(0, 0L, ldt, member2);

        assertEquals(0, GUIDHelper.getPartition(sGUID2));
        }

    @Test
    public void testGetVersion()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID1 = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        assertEquals(17742L, GUIDHelper.getVersion(sGUID1));

        Member member2 = getMockMember(2);

        String sGUID2 = GUIDHelper.generateGUID(0, 0L, ldt, member2);

        assertEquals(0L, GUIDHelper.getVersion(sGUID2));
        }

    @Test
    public void testGetTimestamp()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID1 = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        assertEquals(1341890565000L, GUIDHelper.getServiceJoinTime(sGUID1));

        Member member2 = getMockMember(2);

        String sGUID2 = GUIDHelper.generateGUID(0, 0L, ldt, member2);

        assertEquals(1341890565000L, GUIDHelper.getServiceJoinTime(sGUID2));
        }

    @Test
    public void testGetMemberId()
        {
        Member member1 = getMockMember(1);

        long ldt = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime(); //1341890565000
        String sGUID1 = GUIDHelper.generateGUID(42, 17742L, ldt, member1);

        assertEquals(1, GUIDHelper.getMemberId(sGUID1));

        Member member2 = getMockMember(2);

        String sGUID2 = GUIDHelper.generateGUID(0, 0L, ldt, member2);

        assertEquals(2, GUIDHelper.getMemberId(sGUID2));
        }

    @Test
    public void testGUIDResolver()
        {
        GUIDHelper.GUIDResolver   resolver;
        Map<Member, PartitionSet> mapAvail;

        Member[] aMembers = new Member[10];
        for (int i = 0; i < 10; i++)
            {
            aMembers[i] = getMockMember(i);
            }

        // test empty resolver
        resolver = new GUIDHelper.GUIDResolver(7);
        resolver.registerGUIDs(aMembers[1], new String[] {});
        resolver.registerGUIDs(aMembers[2], new String[] {});
        resolver.registerGUIDs(aMembers[3], new String[] {});
        resolver.registerGUIDs(aMembers[4], new String[] {});
        resolver.registerGUIDs(aMembers[5], new String[] {});
        resolver.registerGUIDs(aMembers[6], new String[] {});

        mapAvail = resolver.resolve();
        assertEquals(6, mapAvail.size());
        assertEquals(0, mapAvail.get(aMembers[1]).cardinality());
        assertEquals(0, mapAvail.get(aMembers[2]).cardinality());
        assertEquals(0, mapAvail.get(aMembers[3]).cardinality());
        assertEquals(0, mapAvail.get(aMembers[4]).cardinality());
        assertEquals(0, mapAvail.get(aMembers[5]).cardinality());
        assertEquals(0, mapAvail.get(aMembers[6]).cardinality());

        // test a uniform view (everybody sees the newest)
        resolver = new GUIDHelper.GUIDResolver(7);
        resolver.registerGUIDs(aMembers[1], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[2], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[3], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[4], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[5], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[6], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});

        mapAvail = resolver.resolve();
        assertEquals(6, mapAvail.size());
        assertTrue(mapAvail.get(aMembers[1]).isFull());
        assertTrue(mapAvail.get(aMembers[2]).isFull());
        assertTrue(mapAvail.get(aMembers[3]).isFull());
        assertTrue(mapAvail.get(aMembers[4]).isFull());
        assertTrue(mapAvail.get(aMembers[5]).isFull());
        assertTrue(mapAvail.get(aMembers[6]).isFull());

        // test a mixed view (everybody sees some version)
        resolver = new GUIDHelper.GUIDResolver(7);
        resolver.registerGUIDs(aMembers[1], new String[]
                {GUIDHelper.generateGUID(0,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[2], new String[]
                {GUIDHelper.generateGUID(0,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[3], new String[]
                {GUIDHelper.generateGUID(0,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,5,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[4], new String[]
                {GUIDHelper.generateGUID(0,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,5,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,5,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[5], new String[]
                {GUIDHelper.generateGUID(0,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,2,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,2,1341890565000L,aMembers[1])});
        resolver.registerGUIDs(aMembers[6], new String[]
                {GUIDHelper.generateGUID(0,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(1,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(2,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(3,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(4,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(5,10,1341890565000L,aMembers[1]),
                 GUIDHelper.generateGUID(6,10,1341890565000L,aMembers[1])});

        mapAvail = resolver.resolve();
        assertEquals(6, mapAvail.size());
        assertTrue(mapAvail.get(aMembers[1]).contains(3));
        assertTrue(mapAvail.get(aMembers[1]).contains(4));
        assertTrue(mapAvail.get(aMembers[1]).contains(5));
        assertTrue(mapAvail.get(aMembers[1]).contains(6));
        assertTrue(mapAvail.get(aMembers[2]).contains(3));
        assertTrue(mapAvail.get(aMembers[2]).contains(4));
        assertTrue(mapAvail.get(aMembers[2]).contains(5));
        assertTrue(mapAvail.get(aMembers[2]).contains(6));
        assertTrue(mapAvail.get(aMembers[3]).contains(1));
        assertTrue(mapAvail.get(aMembers[3]).contains(2));
        assertTrue(mapAvail.get(aMembers[3]).contains(3));
        assertTrue(mapAvail.get(aMembers[4]).contains(1));
        assertTrue(mapAvail.get(aMembers[4]).contains(3));
        assertTrue(mapAvail.get(aMembers[4]).contains(5));
        assertTrue(mapAvail.get(aMembers[5]).isEmpty());
        assertTrue(mapAvail.get(aMembers[6]).isFull());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create and return a mock member.
     *
     * @param nMember  the member id
     *
     * @return the mock member
     */
    public static Member getMockMember(int nMember)
        {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(nMember);
        when(member.getUid()).thenReturn(
                new UID(2130706433 /*127.0.0.1*/, new Date().getTime(), nMember));

        return member;
        }
    }
