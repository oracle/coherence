/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.util.Base;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import java.util.Collections;
import java.util.List;

/**
 * Helper methods for deriving a safe maximum size for platform-thread daemon
 * pools when the configured maximum is left unbounded.
 *
 * @author Aleks Seovic  2026.04.23
 * @since 26.04
 */
public final class DaemonPoolSizing
    {
    /**
     * Derive an effective thread count maximum for an unbounded daemon pool.
     *
     * @param cThreadsMax  the configured maximum
     * @param cThreadsMin  the configured minimum
     *
     * @return the sizing result
     */
    public static Result resolveThreadCountMax(int cThreadsMax, int cThreadsMin)
        {
        return resolveThreadCountMax(cThreadsMax, cThreadsMin, Runtime.getRuntime().maxMemory(),
                determineThreadStackSize(), DEFAULT_DERIVED_THREAD_MAX);
        }

    /**
     * Resolve an effective thread count maximum from deterministic sizing
     * inputs. This overload exists primarily to support focused unit tests.
     *
     * @param cThreadsMax      the configured maximum
     * @param cThreadsMin      the configured minimum
     * @param cbMaxMemory      the maximum JVM heap size
     * @param cbThreadStack    the platform thread stack size
     * @param cThreadsHardMax  the built-in hard ceiling
     *
     * @return the sizing result
     */
    static Result resolveThreadCountMax(int cThreadsMax, int cThreadsMin, long cbMaxMemory,
            long cbThreadStack, int cThreadsHardMax)
        {
        if (cThreadsMax != Integer.MAX_VALUE)
            {
            return new Result(cThreadsMax, false, cbMaxMemory, cbThreadStack, cThreadsHardMax);
            }

        cbThreadStack = cbThreadStack > 0L ? cbThreadStack : DEFAULT_THREAD_STACK_SIZE;
        cThreadsHardMax = Math.max(1, cThreadsHardMax);

        long cDerivedByMemory = cbMaxMemory <= 0L
                ? cThreadsHardMax
                : Math.max(1L, cbMaxMemory / cbThreadStack);
        int  cDerived         = (int) Math.min((long) cThreadsHardMax, cDerivedByMemory);
        int  cFloor           = Math.max(1, cThreadsMin);

        return new Result(Math.max(cFloor, cDerived), true, cbMaxMemory, cbThreadStack, cThreadsHardMax);
        }

    /**
     * Determine the configured platform thread stack size from the current JVM
     * arguments.
     *
     * @return the configured stack size, or the conservative default if none
     *         can be determined
     */
    static long determineThreadStackSize()
        {
        long cbStack = s_cbThreadStackSize;
        if (cbStack <= 0L)
            {
            cbStack = determineThreadStackSize(getInputArguments());
            s_cbThreadStackSize = cbStack;
            }

        return cbStack;
        }

    /**
     * Determine the configured platform thread stack size from the supplied JVM
     * arguments.
     *
     * @param listArgs  the JVM input arguments
     *
     * @return the configured stack size, or the conservative default if none
     *         can be determined
     */
    static long determineThreadStackSize(List<String> listArgs)
        {
        if (listArgs != null)
            {
            for (String sArg : listArgs)
                {
                long cbStack = parseThreadStackSize(sArg);
                if (cbStack > 0L)
                    {
                    return cbStack;
                    }
                }
            }

        return DEFAULT_THREAD_STACK_SIZE;
        }

    /**
     * Parse a JVM argument that may specify the platform thread stack size.
     *
     * @param sArg  the JVM argument to parse
     *
     * @return the parsed stack size in bytes, or {@code -1} if the argument
     *         does not specify a stack size
     */
    static long parseThreadStackSize(String sArg)
        {
        if (sArg == null || sArg.isEmpty())
            {
            return -1L;
            }

        if (sArg.startsWith("-Xss"))
            {
            String sValue = sArg.substring(4);
            if (sValue.isEmpty())
                {
                return -1L;
                }

            try
                {
                return Base.parseMemorySize(sValue);
                }
            catch (RuntimeException e)
                {
                return -1L;
                }
            }

        if (sArg.startsWith("-XX:ThreadStackSize="))
            {
            String sValue = sArg.substring("-XX:ThreadStackSize=".length());
            if (sValue.isEmpty())
                {
                return -1L;
                }

            try
                {
                return Long.parseLong(sValue) * 1024L;
                }
            catch (RuntimeException e)
                {
                return -1L;
                }
            }

        return -1L;
        }

    /**
     * Return the current JVM input arguments.
     *
     * @return the current JVM input arguments
     */
    static List<String> getInputArguments()
        {
        try
            {
            RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
            return bean == null ? Collections.emptyList() : bean.getInputArguments();
            }
        catch (Throwable ignored)
            {
            return Collections.emptyList();
            }
        }

    /**
     * Return the built-in hard ceiling used for derived daemon-pool maxima.
     *
     * @return the built-in hard ceiling
     */
    public static int getDefaultDerivedThreadMax()
        {
        return DEFAULT_DERIVED_THREAD_MAX;
        }

    // ----- data members --------------------------------------------------

    /**
     * The detected platform thread stack size for this JVM.
     */
    private static volatile long s_cbThreadStackSize;

    // ----- inner class: Result -------------------------------------------

    /**
     * The result of resolving an effective daemon-pool maximum.
     */
    public static final class Result
        {
        /**
         * Create a new sizing result.
         *
         * @param cEffectiveMax    the effective maximum
         * @param fDerived         {@code true} if the effective maximum was
         *                         derived from the unbounded sentinel
         * @param cbMaxMemory      the maximum JVM heap size
         * @param cbThreadStack    the detected thread stack size
         * @param cThreadsHardMax  the built-in hard ceiling
         */
        Result(int cEffectiveMax, boolean fDerived, long cbMaxMemory, long cbThreadStack, int cThreadsHardMax)
            {
            f_cEffectiveMax   = cEffectiveMax;
            f_fDerived        = fDerived;
            f_cbMaxMemory     = cbMaxMemory;
            f_cbThreadStack   = cbThreadStack;
            f_cThreadsHardMax = cThreadsHardMax;
            }

        /**
         * Return the effective maximum thread count.
         *
         * @return the effective maximum thread count
         */
        public int getEffectiveMax()
            {
            return f_cEffectiveMax;
            }

        /**
         * Return {@code true} if the maximum was derived from an unbounded
         * configured value.
         *
         * @return {@code true} if the maximum was derived
         */
        public boolean isDerived()
            {
            return f_fDerived;
            }

        /**
         * Return the maximum JVM heap size used by the derivation.
         *
         * @return the maximum JVM heap size
         */
        public long getMaxMemory()
            {
            return f_cbMaxMemory;
            }

        /**
         * Return the detected thread stack size used by the derivation.
         *
         * @return the detected thread stack size
         */
        public long getThreadStackSize()
            {
            return f_cbThreadStack;
            }

        /**
         * Return the built-in hard ceiling used by the derivation.
         *
         * @return the built-in hard ceiling
         */
        public int getHardMax()
            {
            return f_cThreadsHardMax;
            }

        // ----- data members ----------------------------------------------

        /**
         * The effective maximum thread count.
         */
        private final int f_cEffectiveMax;

        /**
         * {@code true} if the effective maximum was derived.
         */
        private final boolean f_fDerived;

        /**
         * The maximum JVM heap size used by the derivation.
         */
        private final long f_cbMaxMemory;

        /**
         * The detected thread stack size used by the derivation.
         */
        private final long f_cbThreadStack;

        /**
         * The built-in hard ceiling used by the derivation.
         */
        private final int f_cThreadsHardMax;
        }

    // ----- constants -----------------------------------------------------

    /**
     * The conservative default platform thread stack size.
     */
    static final long DEFAULT_THREAD_STACK_SIZE = 1024L * 1024L;

    /**
     * The built-in hard ceiling for automatically derived daemon-pool maxima.
     */
    static final int DEFAULT_DERIVED_THREAD_MAX = 2048;
    }
