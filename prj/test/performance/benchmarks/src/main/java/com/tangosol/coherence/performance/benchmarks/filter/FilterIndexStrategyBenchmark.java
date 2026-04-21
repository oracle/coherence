/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.performance.benchmarks.filter;

import com.tangosol.util.SubSet;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmark for filter index-application strategies used by ordered range filters.
 *
 * <p>This benchmark compares:
 * <ul>
 *   <li>inverse-index retain path (scan matching buckets, build retained set, retainAll)</li>
 *   <li>forward-index per-key evaluation (index.get(key) per remaining candidate key)</li>
 *   <li>adaptive path (threshold-based choice between inverse and forward)</li>
 * </ul>
 *
 * <p>The benchmark can use either a plain {@link HashSet} candidate set or a
 * {@link SubSet} candidate set to model query-engine behavior more closely.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(value = 1)
public class FilterIndexStrategyBenchmark
    {
    // ----- benchmark methods ---------------------------------------------

    @Benchmark
    public int inverseRetain(BenchmarkState state)
        {
        Set<Integer> setKeys = state.newWorkingCandidateSet();
        state.applyInverseRetain(setKeys);
        return setKeys.size();
        }

    @Benchmark
    public int forwardEvaluate(BenchmarkState state)
        {
        Set<Integer> setKeys = state.newWorkingCandidateSet();
        state.applyForwardEvaluate(setKeys);
        return setKeys.size();
        }

    @Benchmark
    public int adaptive(BenchmarkState state)
        {
        Set<Integer> setKeys = state.newWorkingCandidateSet();
        state.applyAdaptive(setKeys);
        return setKeys.size();
        }

    // ----- inner class: benchmark state ----------------------------------

    @State(Scope.Benchmark)
    public static class BenchmarkState
        {
        // ----- benchmark parameters ----------------------------------

        /**
         * Candidate key set implementation.
         */
        @Param({"HASH_SET", "SUB_SET"})
        public String candidateSetType;

        /**
         * Candidate key count.
         */
        @Param({"32", "128", "512", "2048", "8192"})
        public int candidateSize;

        /**
         * Number of indexed entries.
         */
        @Param({"10000", "100000"})
        public int indexSize;

        /**
         * Approximate matching range width as percent of bucket space.
         */
        @Param({"1", "10", "50", "90"})
        public int selectivityPercent;

        /**
         * Value distribution over buckets.
         */
        @Param({"UNIFORM", "SKEWED"})
        public String distribution;

        /**
         * Threshold factor for adaptive switching.
         */
        @Param({"1", "2", "4", "8", "16"})
        public int factor;

        // ----- setup -------------------------------------------------

        @Setup(Level.Trial)
        public void setup()
            {
            m_mapInverse = new TreeMap<>();
            m_mapForward = new HashMap<>(indexSize * 2);
            m_setAllKeys = new HashSet<>(indexSize * 2);

            for (int i = 0; i < indexSize; i++)
                {
                int nValue = nextValueFor(i);

                m_mapForward.put(i, nValue);
                m_setAllKeys.add(i);

                m_mapInverse.computeIfAbsent(nValue, key -> new HashSet<>()).add(i);
                }

            int cRange = Math.max(1, (int) ((long) BUCKET_COUNT * selectivityPercent / 100));
            m_nLowerBound = (BUCKET_COUNT - cRange) / 2;
            m_nUpperBound = m_nLowerBound + cRange - 1;

            List<Integer> listInside  = new ArrayList<>();
            List<Integer> listOutside = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : m_mapForward.entrySet())
                {
                if (isInRange(entry.getValue()))
                    {
                    listInside.add(entry.getKey());
                    }
                else
                    {
                    listOutside.add(entry.getKey());
                    }
                }

            Random       random   = new Random(12345L);
            int          cTarget  = Math.min(candidateSize, indexSize);
            int          cInside  = Math.min(cTarget / 2, listInside.size());
            int          cOutside = Math.min(cTarget - cInside, listOutside.size());
            Set<Integer> setSeed  = new HashSet<>(cTarget * 2);

            addRandomSample(setSeed, listInside, cInside, random);
            addRandomSample(setSeed, listOutside, cOutside, random);

            while (setSeed.size() < cTarget)
                {
                setSeed.add(random.nextInt(indexSize));
                }

            m_setCandidateSeed = setSeed;

            SubSet<Integer> subset = new SubSet<>(m_setAllKeys);
            subset.retainAll(m_setCandidateSeed);
            m_subSetTemplate = subset;

            m_colMatchingBuckets = m_mapInverse.subMap(m_nLowerBound, true, m_nUpperBound, true).values();
            }

        // ----- benchmark operations ---------------------------------

        Set<Integer> newWorkingCandidateSet()
            {
            if ("SUB_SET".equals(candidateSetType))
                {
                return (Set<Integer>) m_subSetTemplate.clone();
                }

            return new HashSet<>(m_setCandidateSeed);
            }

        void applyInverseRetain(Set<Integer> setKeys)
            {
            Set<Integer> setRetain = new HashSet<>();
            for (Set<Integer> set : m_colMatchingBuckets)
                {
                setRetain.addAll(set);
                }
            setKeys.retainAll(setRetain);
            }

        void applyForwardEvaluate(Set<Integer> setKeys)
            {
            for (Iterator<Integer> iterator = setKeys.iterator(); iterator.hasNext(); )
                {
                Integer nKey   = iterator.next();
                Integer nValue = m_mapForward.get(nKey);
                if (nValue == null || !isInRange(nValue))
                    {
                    iterator.remove();
                    }
                }
            }

        void applyAdaptive(Set<Integer> setKeys)
            {
            if (shouldEvaluateUsingForwardIndex(setKeys, m_colMatchingBuckets, factor))
                {
                applyForwardEvaluate(setKeys);
                }
            else
                {
                applyInverseRetain(setKeys);
                }
            }

        // ----- helpers ----------------------------------------------

        private int nextValueFor(int nKey)
            {
            if ("SKEWED".equals(distribution))
                {
                // deterministic skew toward low buckets
                long n = (long) nKey * nKey;
                return (int) (n % BUCKET_COUNT);
                }

            return nKey % BUCKET_COUNT;
            }

        private void addRandomSample(Set<Integer> setTarget, List<Integer> listSource, int cSample, Random random)
            {
            for (int i = 0; i < cSample; i++)
                {
                setTarget.add(listSource.get(random.nextInt(listSource.size())));
                }
            }

        private boolean shouldEvaluateUsingForwardIndex(Set<Integer> setKeys, Collection<Set<Integer>> colIndexSets, int nFactor)
            {
            if (setKeys.isEmpty())
                {
                return false;
                }

            long cThreshold = (long) setKeys.size() * nFactor;
            long cEntries   = 0;

            for (Set<Integer> set : colIndexSets)
                {
                cEntries += set.size();
                if (cEntries > cThreshold)
                    {
                    return true;
                    }
                }

            return false;
            }

        private boolean isInRange(int nValue)
            {
            return nValue >= m_nLowerBound && nValue <= m_nUpperBound;
            }

        // ----- data members -----------------------------------------

        private int m_nLowerBound;
        private int m_nUpperBound;

        private Set<Integer>                     m_setAllKeys;
        private Set<Integer>                     m_setCandidateSeed;
        private SubSet<Integer>                  m_subSetTemplate;
        private Map<Integer, Integer>            m_mapForward;
        private NavigableMap<Integer, Set<Integer>> m_mapInverse;
        private Collection<Set<Integer>>         m_colMatchingBuckets;

        // ----- constants --------------------------------------------

        private static final int BUCKET_COUNT = 1024;
        }
    }
