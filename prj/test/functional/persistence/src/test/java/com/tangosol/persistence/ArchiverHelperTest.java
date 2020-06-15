/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.persistence;

import com.tangosol.internal.util.Primes;

import com.tangosol.net.Member;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.net.proxy.RemoteMember;
import com.tangosol.util.Base;
import com.tangosol.util.UID;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.Map.Entry;

import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Various tests for the ArchiveHelper.
 *
 * @author  tam 2014.07.18
 */
@SuppressWarnings("unchecked")
@RunWith(Parameterized.class)
public class ArchiverHelperTest
    {
    // ----- test parameters ------------------------------------------------

    @Parameters(name="{index} = {0}")
    public static Iterable<Object[]> functions()
        {
        BiFunction<Map<Integer, Object[]>, Integer, Map<Integer, Object[]>> f1 = (mapStores, cParts) ->
            {
            ArchiverHelper.allocateSnapshotPartitions(mapStores, cParts);
            return mapStores;
            };

        BiFunction<Map<Integer, Object[]>, Integer, Map<Integer, ? extends Object[]>> f2 =
                GUIDHelper::assignStores;

    	return Arrays.asList(
                new Object[] {"GUIDHelper",    f2},
                new Object[] {"ArchiveHelper", f1});
        }

    // ----- constructors ---------------------------------------------------

    public ArchiverHelperTest(String sName, BiFunction<Map<Integer, Object[]>, Integer, Map<Integer, ? extends Object[]>> function)
        {
        f_function = function;
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test the case where all members see all partitions. E.g. they are using
     * the same shared directory structure.
     */
    @Test
    public void testAllMembersSeeAllPartitions()
        {
        System.out.println("Testing cases where all members see all partitions.");

        // test 1 member with 257 partitions
        Map<Integer, Object[]> mapStores = generatePartitionsForMembers(1, 257);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 257);
        validateExpectedResultAllMembers(mapStores, 1,257);

        // test 1 member with 127 partitions
        mapStores = generatePartitionsForMembers(1, 127);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 127);
        validateExpectedResultAllMembers(mapStores, 1, 127);

        // 2 members with 257 partitions
        mapStores = generatePartitionsForMembers(2, 257);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 257);
        validateExpectedResultAllMembers(mapStores, 2, 257);

        // 10 members with 257 partitions
        mapStores = generatePartitionsForMembers(10, 257);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 257);
        validateExpectedResultAllMembers(mapStores, 10, 257);

        // 3 members with 11 partitions
        mapStores = generatePartitionsForMembers(3, 11);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 11);
        validateExpectedResultAllMembers(mapStores, 3, 11);

        // 3 members with 12 partitions - not a good test case as not a prime number
        mapStores = generatePartitionsForMembers(3, 12);
        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, 12);
        validateExpectedResultAllMembers(mapStores, 3, 12);

        // test from 1 to 40 members with 257-1033 partitions
        for (int nMember = 2 ; nMember < 50 ; nMember++)
            {
            for (int nParts = 257; nParts < 1033 ; nParts+= 257)
                {
                mapStores = generatePartitionsForMembers(nMember, nParts);

                mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, nParts);

                validateExpectedResultAllMembers(mapStores, nMember, nParts);
                }
            }
        }

    /**
     * Test multiple members on same machine seeing partitions. E.g. members are
     * using same local disk.
     */
    @Test
    public void testMultipleMembersOnSameMachine()
        {
        System.out.println("Testing case where multiple members on same machine.");

        // 4 members, 2 on each machine using same location for persistence.
        // partition size of 7
        // machine 1 has partitions 0..3, and machine2 has partitions 4..6
        Map<Integer, Object[]> mapStores = new HashMap<>();

        mapStores.put(0, createStoreArray(7, 0, 3));
        mapStores.put(1, createStoreArray(7, 0, 3));
        mapStores.put(2, createStoreArray(7, 4, 6));
        mapStores.put(3, createStoreArray(7, 4, 6));

        Map<Integer, Object[]> mapAssigned = (Map<Integer, Object[]>) f_function.apply(mapStores, 7);

        validateNoDuplicates(7, mapAssigned);
        assertEquals(2, mapAssigned.get(0).length);
        assertEquals(2, mapAssigned.get(1).length);
        assertThat(mapAssigned.get(2).length, Matchers.anyOf(Matchers.is(1), Matchers.is(2)));
        assertThat(mapAssigned.get(3).length, Matchers.anyOf(Matchers.is(1), Matchers.is(2)));

        // 3 members, 2 using same location and one using different location
        // partition size if 11
        // member1 has 0..7, member2 has 0..7, member3 has 8..10
        mapStores = new HashMap<>();
        mapStores.put(0, createStoreArray(11, 0, 7));
        mapStores.put(1, createStoreArray(11, 0, 7));
        mapStores.put(2, createStoreArray(11, 8, 10));

        mapAssigned = (Map<Integer, Object[]>) f_function.apply(mapStores, 11);

        validateNoDuplicates(11, mapAssigned);
        assertEquals(4, mapAssigned.get(0).length);
        assertEquals(4, mapAssigned.get(1).length);
        assertEquals(3, mapAssigned.get(2).length);
        }

    /**
     * Test multiple members on same machine seeing partitions. E.g. members are
     * using same local disk.
     */

    @Test
    public void testSpecificScenarios()
        {
        Map<Integer, Object[]> mapStores = new HashMap<>();

        mapStores.put(0, new Object[] {getStore(0), getStore(1), getStore(2)});
        mapStores.put(1, new Object[] {getStore(0), getStore(2), getStore(3), getStore(4)});

        Map<Integer, Object[]> mapAssigned = (Map<Integer, Object[]>) f_function.apply(mapStores, 5);

        validateNoDuplicates(5, mapAssigned);
        assertThat(mapAssigned.get(0).length, Matchers.anyOf(Matchers.is(2), Matchers.is(3)));
        assertThat(mapAssigned.get(1).length, Matchers.anyOf(Matchers.is(2), Matchers.is(3)));
        }

    /**
     * Test randomly created {@link PartitionSet}s to ensure that no duplicates
     * exist.
     * <p>
     * This does nto test every possibility, but should be able to test enough
     * to get edge cases.   We start at ridiculous partition counts just to be sure.
     *
     */
    @Test
    public void testRandomPartitionSets()
        {
        final int START_MEMBER_COUNT    = 1;
        final int END_MEMBER_COUNT      = 40;
        final int PARTITION_SKIP        = 47;   // otherwise will take forever as n squared
        final Random random = new Random();

        System.out.println("Testing cases with randomly generated members and partitions.");
        System.out.println("Testing Member Count between " + START_MEMBER_COUNT + " and " + END_MEMBER_COUNT
                           + " for appropriate partition counts.");

        for (int iMember = START_MEMBER_COUNT; iMember < END_MEMBER_COUNT; iMember+= Math.max(1, random.nextInt(3)))
            {
            int nNextPrime = Primes.next(iMember * iMember);
            int nStartPart = Math.max(nNextPrime, 257);
            int nEndPart   = Math.max(nNextPrime, (int) (nStartPart * 1.5));

            System.out.println("Member count = " + iMember + ", start partition = " + nStartPart + ", end partition=" + nEndPart);

            for (int iParts = nStartPart; iParts <= nEndPart; iParts += random.nextInt(PARTITION_SKIP) + 17)
                {
                testAndValidateRandomPartitionSets(iParts, iMember, 1);
                testAndValidateRandomPartitionSets(iParts, iMember, 2);
                testAndValidateRandomPartitionSets(iParts, iMember, 3);
                }
            }
        }

    // ----- adhoc perf tests -----------------------------------------------

    /**
     * A test to reproduce and highlight a performance bottleneck within the
     * ArchiveHelper.allocateSnapshotPartitions algorithm and to compare/contrast
     * with the new algorith (GUIDHelper.assignStores).
     * <p>
     * Performance numbers
     */
    public static void testLargeClusterPerformance()
        {
        final int MEMBERS = 500;
        final int MACHINES = 10;
        final int PARTITION_COUNT = 4001;

        Map<Integer, Object[]> mapStores = generatePartitionsForMembers(PARTITION_COUNT, MEMBERS, MACHINES);

        long ldtNow = System.currentTimeMillis();
        Map<Integer, String[]> mapAssignedGUIDs = GUIDHelper.assignStores(mapStores, PARTITION_COUNT);
        System.out.println("GUIDHelper: took " + (System.currentTimeMillis() - ldtNow) + "ms to allocate parts");

        ldtNow = System.currentTimeMillis();
        ArchiverHelper.allocateSnapshotPartitions(mapStores, PARTITION_COUNT);
        System.out.println("ArchiveHelper: took " + (System.currentTimeMillis() - ldtNow) + "ms to allocate parts");

        PartitionSet parts = new PartitionSet(PARTITION_COUNT);
        mapAssignedGUIDs.forEach((NMemberId, aoGUIDs) ->
            {
            System.out.println("Member " + NMemberId + " is allocated " + aoGUIDs.length + " stores");
            for (Object oGUID : aoGUIDs)
                {
                parts.add(GUIDHelper.getPartition((String) oGUID));
                }
            });

        System.out.println("Missing: " + parts.cardinality());
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate results for random partitions.
     *
     * @param cPartitionCount partition count to generate for
     * @param nMembers        number of members to generate for
     * @param nMaxDuplicates  max number of duplicate stores
     */
    private void testAndValidateRandomPartitionSets(int cPartitionCount, int nMembers, int nMaxDuplicates)
        {
        Map<Integer, Object[]> mapStores = generateRandomStores(cPartitionCount, nMembers, nMaxDuplicates);
        Assert.assertTrue("Number of members should be " + nMembers + " but is " + mapStores.size() + ", dumpMap=" + dumpMap(mapStores),
                          mapStores.size() == nMembers);

        mapStores = (Map<Integer, Object[]>) f_function.apply(mapStores, cPartitionCount);

        validateNoDuplicates(cPartitionCount, mapStores);
        }

    /**
     * Generate random number of partition sets for different member and partition counts.
     *
     * @param cPartitionCount partition count to generate for
     * @param nMembers        number of members to generate for
     * @param nMaxDuplicates  max number of duplicate stores
     *
     * @return the {@link Map} of members and stores
     */
    private Map<Integer, Object[]> generateRandomStores(int cPartitionCount, int nMembers, int nMaxDuplicates)
        {
        Map<Integer, Object[]> mapStores = new HashMap<>();
        Random                 rand      = new Random();

        // populate the Map to ensure at least there is a PartitionSet for each
        // member even if it is empty

        for (int i = 0; i < nMembers; i++)
            {
            mapStores.put(i, new String[cPartitionCount]);
            }

        for (int iPart = 0; iPart < cPartitionCount; iPart++)
            {
            // assign the partition to "nMaxDuplicates" members

            for (int i = 0; i < nMaxDuplicates; i++)
                {
                int      nMemberId = rand.nextInt(nMembers);
                Object[] aoStores  = mapStores.get(nMemberId);

                appendToArray(aoStores, getStore(iPart));
                mapStores.put(nMemberId, aoStores);

                }
            }

        // trim all the String[] values()
        Iterator<Map.Entry<Integer, Object[]>> iter = mapStores.entrySet().iterator();

        while (iter.hasNext())
            {
            Map.Entry<Integer, Object[]> entry = iter.next();

            mapStores.put(entry.getKey(), trimArrayValues(entry.getValue()));
            }

        return mapStores;
        }

    /**
     * Append the value to the array of nulls.
     *
     * @param asArray the array to append to
     * @param oValue  the value to add
     */
    private void appendToArray(Object[] asArray, Object oValue)
        {
        for (int i = 0; i < asArray.length; i++)
            {
            if (asArray[i] == null)
                {
                asArray[i] = oValue;

                return;
                }
            else if (asArray[i].equals(oValue))
                {
                return;    // no duplciates - we should really use Set
                }
            }
        }

    /**
     * Trim the array and ignore any null values.
     *
     * @param aoArray the array to trim
     *
     * @return a trimmed array
     */
    private Object[] trimArrayValues(Object[] aoArray)
        {
        int      nSize      = aoArray.length;
        int      nLastEntry = 0;
        String[] asResult   = null;

        while (nLastEntry < nSize)
            {
            if (aoArray[nLastEntry] == null)
                {
                break;
                }

            nLastEntry++;
            }

        if (nLastEntry != 0)
            {
            asResult = new String[nLastEntry];
            System.arraycopy(aoArray, 0, asResult, 0, nLastEntry);
            }

        return asResult;
        }

    /**
     * Validate the result of a test when all members have all stores.
     *
     * @param mapStores  the {@link Map} of results from the allocateSnapshotPartitions call
     * @param cMembers   the expected number of members
     * @param cParts     the expected number of partitions
     */
    private void validateExpectedResultAllMembers(Map<Integer, Object[]> mapStores, int cMembers, int cParts)
        {
        assertNotNull(mapStores);

        int cFairShare   = cParts / cMembers + 1;
        int nMemberCount = mapStores.size();

        assertTrue("Member count is " +nMemberCount + " but should be " + cMembers,
                   nMemberCount == cMembers);

        // Unfortunately the old algorithm (ArchiveHelper) can result in uneven
        // distribution therefore relax the assertion
        int cMax = cFairShare + cParts - (cParts / cMembers) * cMembers;

        validateNoDuplicates(cParts, mapStores);

        mapStores.forEach((nMember, aoStores) ->
                assertThat(aoStores.length,
                Matchers.allOf(Matchers.lessThanOrEqualTo(cMax), Matchers.greaterThanOrEqualTo(cFairShare - 2))));
        }

    /**
     * Validate that there are no duplicate stores in the Map. e.g. only one member
     * owns each store and none are missing.
     *
     * @param cPartitions  the number of partitions
     * @param mapStores    resultant processed {@link Map}
     */
    private void validateNoDuplicates(int cPartitions, Map<Integer, Object[]> mapStores)
        {
        if (mapStores.size() == 1)
            {
            int nStores = mapStores.entrySet().iterator().next().getValue().length;
            Assert.assertTrue("Partition count of " + cPartitions + " does not match number of stores of " + nStores, nStores == cPartitions);
            }
        for (int iPart = 0; iPart < cPartitions; iPart++)
            {
            String       sStore     = getStore(iPart);
            Set<Integer> setMembers = com.tangosol.persistence.ArchiverHelper.getMembersOwningPartition(mapStores, sStore);

            Assert.assertTrue("Partition " + iPart + "(" + sStore + ") should be owned by one member"
                              + " but is owned by the following members " + setMembers + "\n" + dumpMap(mapStores),
                    setMembers.size() == 1);
            }
        }

    private String dumpMap(Map<Integer, Object[]> mapStores)
        {
        StringBuffer sb = new StringBuffer("Dump of stores: Member count = ")
                .append(mapStores.size())
                .append(" ");

        Iterator<Entry<Integer, Object[]>> iter = mapStores.entrySet().iterator();

        while (iter.hasNext())
            {
            Map.Entry<Integer, Object[]> entry = iter.next();

            int nLen = entry.getValue() == null ? 0 : entry.getValue().length;

            sb.append("Member=" + entry.getKey())
              .append(" array size=")
              .append(nLen)
              .append(", ")
              .append(Arrays.toString(entry.getValue()))
              .append('\n');
            }

        return sb.toString();
        }

    /**
     * Create a new {@link String array} with the given stores filled.
     *
     * @param nPartitionCount  the partition count
     * @param nStartPartition  start partition
     * @param nEndPartition    end partition
     *
     * @return new newly created {@link String} array
     */
    private static String[] createStoreArray(int nPartitionCount, int nStartPartition, int nEndPartition)
        {
        String[] asStores = new String[nEndPartition - nStartPartition + 1];

        fillStores(asStores, nStartPartition, nEndPartition);

        return asStores;
        }

    /**
     * Fill selected partitions for a given {@link String} array.
     *
     * @param asStores         the {@link String}[] to fill
     * @param nStartPartition  the start partition
     * @param nEndPartition    the end partition
     */
    private static void fillStores(String[] asStores, int nStartPartition, int nEndPartition)
        {
        int nPartition = nStartPartition;

        for (int i = 0; i < asStores.length; i++)
            {
            asStores[i] = getStore(nPartition++);
            }
        }

    /**
     * Create a {@link Map} of members and partitions where each member sees
     * each store.
     *
     * @param cMembers     the number of members to create
     * @param cPartitions  the number of partitions each member has
     *
     * @return the {@link Map} of {@link PartitionSet}s
     */
    private Map<Integer, Object[]> generatePartitionsForMembers(int cMembers, int cPartitions)
        {
        Map<Integer, Object[]> mapStores = new HashMap<>();

        for (int i = 0; i < cMembers; i++)
            {
            mapStores.put(new Integer(i), generateStores(cPartitions));
            }

        return mapStores;
        }

    /**
     * Return a Map of member id to an array of stores evenly distributing the
     * members across machines. All members on a machine will see the same stores,
     * furthermore a store will not span machines.
     *
     * @param cParts     the partition count
     * @param cMembers   the number of members
     * @param cMachines  the number of distinct machines
     *
     * @return a Map of member id to an array of stores
     */
    private static Map<Integer, Object[]> generatePartitionsForMembers(int cParts, int cMembers, int cMachines)
        {
        final int PARTS_PER_MACHINE   = cParts / cMachines;
        final int MEMBERS_PER_MACHINE = cMembers / cMachines;

        Map<Integer, Object[]> mapStores = new HashMap<>(cMembers);

        long   ldtNow = System.currentTimeMillis();
        Random rnd    = Base.getRandom();

        for (int iPart = 0, iMemberOff = 1; iPart < cParts; ++iPart)
            {
            int iStore = iPart % PARTS_PER_MACHINE;

            if (iPart == cParts - 1)
                {
                iStore = PARTS_PER_MACHINE;
                }
            else if (iPart > 0 && iPart % PARTS_PER_MACHINE == 0)
                {
                iMemberOff += MEMBERS_PER_MACHINE;
                }

            String sGuid = String.format("%d-%x-%x-%d", iPart, 1L, ldtNow, iMemberOff + rnd.nextInt(MEMBERS_PER_MACHINE));

            for (int nMemberId = iMemberOff, nMemberLast = nMemberId + MEMBERS_PER_MACHINE; nMemberId < nMemberLast; ++nMemberId)
                {
                Object[] aoStores = mapStores.get(nMemberId);
                if (aoStores == null || iStore >= aoStores.length)
                    {
                    if (aoStores == null)
                        {
                        aoStores = new Object[PARTS_PER_MACHINE];
                        }
                    else
                        {
                        Object[] aoStoresTmp = new Object[iStore + 1];
                        System.arraycopy(aoStores, 0, aoStoresTmp, 0, aoStores.length);
                        aoStores = aoStoresTmp;
                        }

                    mapStores.put(nMemberId, aoStores);
                    }

                aoStores[iStore] = sGuid;
                }

            }
        return mapStores;
        }

    /**
     * Generate an array of stores.
     *
     * @param cPartitions the number of stores to create
     *
     * @return the {@link Object} array of stores
     */
    private Object[] generateStores(int cPartitions)
        {
        String[] asStores = new String[cPartitions];

        for (int i = 0; i < cPartitions; i++)
            {
            asStores[i] = getStore(i);
            }

        return asStores;
        }

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
        when(member.getUid()).thenReturn(new UID(2130706433 /* 127.0.0.1 */, MEMBER_DATE, nMember));

        return member;
        }

    /**
     * Generate a store given a partition id.
     *
     * @param nPartition the partition this store belongs to
     *
     * @return the generated store name
     */
    private static String getStore(int nPartition)
        {
        return GUIDHelper.generateGUID(nPartition, 1, MEMBER_DATE, SINGLETON_MEMBER);
        }

    public static void main(String[] asArgs)
        {
        testLargeClusterPerformance();
        }

    // ----- static ---------------------------------------------------------

    public static final Member SINGLETON_MEMBER = new RemoteMember(null, 8088)
        {
        @Override
        public UID getUid()
            {
            return f_uid;
            }

        @Override
        public int getId()
            {
            return 1;
            }

        protected final UID f_uid = new UID(2130706433 /* 127.0.0.1 */, MEMBER_DATE, 1);
        };

    private static final long MEMBER_DATE = new Date("Mon Jul 09 20:22:45 PDT 2012").getTime();    ////1341890565000

    private final BiFunction<Map<Integer, Object[]>, Integer, Map<Integer, ? extends Object[]>> f_function;
    }
