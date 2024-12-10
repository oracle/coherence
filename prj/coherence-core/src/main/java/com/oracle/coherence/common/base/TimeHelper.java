/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.base;

import com.oracle.coherence.common.util.SafeClock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Map;
import java.util.TimeZone;

import java.util.concurrent.ConcurrentHashMap;

import static com.oracle.coherence.common.base.Exceptions.ensureRuntimeException;

/**
 * Class for providing time functionality.
 *
 * @author cp  2000.08.02
 * @since 20.06
 */
public abstract class TimeHelper
    {
    // ----- time routines ----------------------------------------------

    /**
     * Return the number of milliseconds which have elapsed since the JVM was
     * started.
     *
     * @return the number of milliseconds which have elapsed since the JVM was
     * started
     */
    public static long getUpTimeMillis()
        {
        Method methodUptime = s_methodUptime;
        if (methodUptime == null)
            {
            return System.currentTimeMillis() - s_ldtStartTime;
            }

        try
            {
            return (Long) methodUptime.invoke(s_oRuntimeMXBean);
            }
        catch (Throwable e)
            {
            if (e instanceof InvocationTargetException)
                {
                e = ((InvocationTargetException) e).getTargetException();
                }
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Returns a "safe" current time in milliseconds.
     *
     * @return the difference, measured in milliseconds, between
     * the corrected current time and midnight, January 1, 1970 UTC.
     * @see SafeClock
     */
    public static long getSafeTimeMillis()
        {
        return s_safeClock.getSafeTimeMillis(System.currentTimeMillis());
        }

    /**
     * Returns the last "safe" time as computed by a previous call to
     * the {@link #getSafeTimeMillis} method.
     *
     * @return the last "safe" time in milliseconds
     * @see SafeClock
     */
    public static long getLastSafeTimeMillis()
        {
        return s_safeClock.getLastSafeTimeMillis();
        }

    /**
     * compute the number of milliseconds until the specified time.
     * <p>
     * Note: this method will only return zero if ldtTimeout == Long.MAX_VALUE.
     *
     * @param ldtTimeout the timeout as computed by {@link #getSafeTimeMillis}
     * @return the number of milliseconds to wait, or negative if the timeout
     * has expired
     */
    public static long computeSafeWaitTime(long ldtTimeout)
        {
        if (ldtTimeout == Long.MAX_VALUE)
            {
            return 0;
            }

        long ldtNow = getSafeTimeMillis();
        return ldtTimeout == ldtNow ? -1 : ldtTimeout - ldtNow;
        }

    /**
     * Gets the {@link TimeZone} for the given ID.
     * <p>
     * This method will cache returned TimeZones to avoid the contention
     * caused by the {@link TimeZone#getTimeZone(String)} implementation.
     *
     * @param sId the ID for a {@link TimeZone}
     * @return the specified {@link TimeZone}, or the GMT zone if the
     * given ID cannot be understood.
     * @see TimeZone#getTimeZone(String)
     * @since Coherence 12.1.3
     */
    public static TimeZone getTimeZone(String sId)
        {
        TimeZone timeZone = s_mapTimeZones.get(sId);
        if (timeZone == null)
            {
            timeZone = TimeZone.getTimeZone(sId);
            s_mapTimeZones.put(sId, timeZone);
            }
        return timeZone;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The map of cached {@link TimeZone}s keyed by ID.
     */
    private static final Map<String, TimeZone> s_mapTimeZones = new ConcurrentHashMap<>();

    /**
     * The estimated JVM start time.
     */
    private static final long s_ldtStartTime = System.currentTimeMillis();

    /**
     * The shared SafeClock.
     */
    private static final SafeClock s_safeClock = new SafeClock(s_ldtStartTime);

    /**
     * The java.lang.RuntimeMXBean or null if not available.
     */
    private static final Object s_oRuntimeMXBean;

    /**
     * RuntimeMXBean.getUpTime() Method or null if not available.
     */
    private static final Method s_methodUptime;

    static
        {
        Object oRuntimeMXBean = null;
        Method methodUptime = null;

        try
            {
            oRuntimeMXBean = Class.forName("java.lang.management.ManagementFactory")
                    .getMethod("getRuntimeMXBean").invoke(null);
            if (oRuntimeMXBean != null)
                {
                methodUptime = Class.forName("java.lang.management.RuntimeMXBean")
                        .getMethod("getUptime");
                }
            }
        catch (Throwable ignore)
            {
            }

        s_oRuntimeMXBean = oRuntimeMXBean;
        s_methodUptime = methodUptime;
        }
    }
