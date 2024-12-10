/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * SubSetTest tests various methods on {@link SubSet}.
 *
 * @author hr  2014.05.15
 */
public class SubSetTest
    {
    /**
     * Test the functionality of retainAll.
     */
    @Test
    public void testRetainAll()
        {
        Set<String> setOrig = new HashSet<>(asList("i", "ii", "iii", "iv", "v"));

        // test retaining 3 elements
        Set<String> set = new SubSet<>(setOrig);
        assertTrue(set.retainAll(new HashSet<>(asList("ii", "iii", "iv"))));
        assertTrue(set.size() == 3 && set.contains("ii") && set.contains("iii") && set.contains("iv"));

        // test subsequent retainAll from 3 elements to 2
        assertTrue(set.retainAll(new HashSet<>(asList("ii", "iv"))));
        assertTrue(set.size() == 2 && set.contains("ii") && set.contains("iv"));

        // test where passed set is significantly smaller than retained set
        set = new SubSet<>(setOrig);
        assertTrue(set.retainAll(new HashSet<>(asList("i", "ii", "iii", "iv"))));
        assertTrue(set.size() == 4);

        assertTrue(set.retainAll(new HashSet<>(asList("ii"))));
        assertTrue(set.size() == 1 && set.contains("ii"));

        // test when no action is taken
        assertFalse(new SubSet<>(setOrig).retainAll(setOrig));

        // test when items have also been removed
        set = new SubSet<>(setOrig);

        assertTrue(set.remove("ii"));
        assertTrue(set.remove("iv"));

        assertFalse(set.retainAll(new HashSet<>(asList("i", "iii", "v"))));
        assertTrue(set.size() == 3 && set.contains("i") && set.contains("iii") && set.contains("v"));
        assertTrue(set.retainAll(new HashSet<>(asList("i", "v"))));
        assertTrue(set.size() == 2 && set.contains("i") && set.contains("v"));
        }

    // ----- performance test -----------------------------------------------

    /**
     * Test used to measure the performance of the three retainAll impls;
     * current SubSet, old SubSet, and HashSet.
     */
    // @Test
    public void testRetainAllPerf()
        {
        String[] asImpl  = new String[] {"SubSet", "OldSubSet", "HashSet"};
        long[]   alTimes = new long[9];

        doRetainAllTest(alTimes, asImpl, 10000, 100);
        System.out.println();
        doRetainAllTest(alTimes, asImpl, 10000, 50000);
        System.out.println();
        doRetainAllTest(alTimes, asImpl, 100000, 500000, 60000);
        }

    /**
     * Run many iterations of a test given the original size and retained size.
     */
    protected void doRetainAllTest(long[] alTimes, String[] asImpl, int cOrig, int cRetain)
        {
        doRetainAllTest(alTimes, asImpl, cOrig, cRetain, Math.max(cOrig - cRetain - 1, 0));
        }

    /**
     * Run many iterations of a test given the original size and retained size.
     */
    protected void doRetainAllTest(long[] alTimes, String[] asImpl, int cOrig, int cRetain, int ofRetain)
        {
        final int ITERS = 100;
        for (int i = 0; i < ITERS; ++i)
            {
            Set<Integer> setOrig   = instantiateSet(cOrig, 0);
            Set<Integer> setRetain = instantiateSet(cRetain, ofRetain);

            long[] al = i > 9 ? alTimes : null;
            doSingleRetainAllTest(new SubSet<>(setOrig), setRetain, al, 0);
            doSingleRetainAllTest(new OldSubSet<>(setOrig), setRetain, al, 3);
            doSingleRetainAllTest(new HashSet<>(setOrig), setRetain, al, 6);
            }
        outputResults(alTimes, (int) (ITERS * 0.9), cOrig, cRetain, asImpl);
        }

    /**
     * Run a single test and time.
     */
    protected void doSingleRetainAllTest(Set<Integer> set, Set<Integer> setRetain, long[] alTimes, int iTime)
        {
        long lStart = System.nanoTime();
        set.retainAll(setRetain);

        if (alTimes == null)
            {
            return;
            }

        // elapsed
        alTimes[iTime] += System.nanoTime() - lStart;
        // min
        if (alTimes[iTime] < alTimes[iTime + 1] || alTimes[iTime + 1] == 0)
            {
            alTimes[iTime + 1] = alTimes[iTime];
            }
        // max
        if (alTimes[iTime] > alTimes[iTime + 2])
            {
            alTimes[iTime + 2] = alTimes[iTime];
            }
        }

    /**
     * Output the results observed.
     */
    protected void outputResults(long[] alTimes, int cIters, int cOrig, int cRetain, String[] asImpl)
        {
        for (int i = 0; i < 9; i += 3)
            {
            System.out.printf("took %s {mean:%dµs, min:%dµs, max:%dµs} to retain %d/%d\n",
                    asImpl[i / 3],
                    alTimes[i] / (cIters * 1000), alTimes[i + 1] / 1000, alTimes[i + 2] / 1000,
                    cRetain, cOrig);
            }
        }

    /**
     * Create a set with {@code cSize} Integers starting at {@code of}.
     *
     * @param cSize  number of elements to add
     * @param of     offset to start from
     *
     * @return a set with the specified size
     */
    protected Set<Integer> instantiateSet(int cSize, int of)
        {
        Set<Integer> set = new HashSet<>(10000);
        for (int c = of + cSize; of < c; ++of)
            {
            set.add(Integer.valueOf(of));
            }
        return set;
        }


    // ----- inner class: OldSubSet -----------------------------------------

    /**
     * A previous implementation of SubSet to gauge performance differences
     * against (see P4 CL #52042).
     */
    protected class OldSubSet<E>
            extends SubSet<E>
        {
        /**
         * Construct this set based on an existing set.
         *
         * @param set the set to base this subset on
         */
        public OldSubSet(Set<? extends E> set)
            {
            super(set);
            }

        @Override
        public boolean retainAll(Collection<?> col)
            {
            if (m_fRetained)
                {
                return ensureRetained().retainAll(col);
                }
            else
                {
                // switch to retain regardless
                Set setOrig    = m_setOrig;
                Set setRemoved = m_setMod;
                Set setMod     = instantiateModificationSet(0);

                m_setMod    = setMod;
                m_fRetained = true;

                if (setRemoved == null || setRemoved.isEmpty())
                    {
                    // optimization: the SubSet is still the whole set,
                    // so evaluate the retain operation against the whole set
                    for (E e : (Iterable<E>) col)
                        {
                        if (setOrig.contains(e))
                            {
                            setMod.add(e);
                            }
                        }
                    return setMod.size() != setOrig.size();
                    }
                else
                    {
                    // some items have already been removed from the set,
                    // so avoid retaining those
                    boolean fMod = false;
                    for (E e : (Iterable<E>) col)
                        {
                        if (!setRemoved.contains(e) && setOrig.contains(e))
                            {
                            setMod.add(e);
                            fMod = true;
                            }
                        }
                    return fMod;
                    }
                }
            }
        }
    }
