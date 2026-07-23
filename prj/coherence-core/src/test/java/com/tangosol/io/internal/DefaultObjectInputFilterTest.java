/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.internal;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link DefaultObjectInputFilter}.
 *
 * @author Aleks Seovic  2026.04.29
 * @since 26.04
 */
public class DefaultObjectInputFilterTest
    {
    @Test
    public void testUserFilterRejectionWins()
        {
        Object filter = DefaultObjectInputFilter.forTesting(userFilter(Date.class, "REJECTED"));

        assertEquals("REJECTED", check(filter, Date.class));
        }

    @Test
    public void testDefaultDenyListWins()
        {
        Object filter = DefaultObjectInputFilter.forTesting(userFilter(Runtime.class, "ALLOWED"));

        assertEquals("REJECTED", check(filter, Runtime.class));
        }

    @Test
    public void testDefaultAllowSurvivesUserUndecided()
        {
        Object filter = DefaultObjectInputFilter.forTesting(userFilter(Date.class, "UNDECIDED"));

        assertEquals("ALLOWED", check(filter, Date.class));
        }

    private static String check(Object filter, Class<?> clz)
        {
        try
            {
            Method method = filter.getClass().getMethod("checkInput", filterInfoClass());
            return ((Enum) method.invoke(filter, filterInfo(clz))).name();
            }
        catch (Exception e)
            {
            throw new AssertionError(e);
            }
        }

    private static Object userFilter(final Class<?> clzMatch, final String sMatchStatus)
        {
        return Proxy.newProxyInstance(filterClass().getClassLoader(), new Class<?>[] {filterClass()},
                (proxy, method, args) ->
            {
            if ("checkInput".equals(method.getName()))
                {
                Class<?> clz = serialClass(args[0]);
                return status(clz == clzMatch ? sMatchStatus : "UNDECIDED");
                }
            return null;
            });
        }

    private static Object filterInfo(final Class<?> clzSerial)
        {
        return Proxy.newProxyInstance(filterInfoClass().getClassLoader(), new Class<?>[] {filterInfoClass()},
                (proxy, method, args) ->
            {
            String sName = method.getName();
            if ("serialClass".equals(sName))
                {
                return clzSerial;
                }
            else if ("arrayLength".equals(sName))
                {
                return Long.valueOf(-1L);
                }
            else if ("depth".equals(sName))
                {
                return Long.valueOf(1L);
                }
            else if ("references".equals(sName) || "streamBytes".equals(sName))
                {
                return Long.valueOf(0L);
                }
            return null;
            });
        }

    private static Class<?> serialClass(Object filterInfo) throws Exception
        {
        return (Class<?>) filterInfo.getClass().getMethod("serialClass").invoke(filterInfo);
        }

    private static Enum status(String sStatus)
        {
        return Enum.valueOf((Class<Enum>) statusClass(), sStatus);
        }

    private static Class<?> filterClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter");
                }
            catch (ClassNotFoundException e2)
                {
                throw new AssertionError(e2);
                }
            }
        }

    private static Class<?> filterInfoClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$FilterInfo");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$FilterInfo");
                }
            catch (ClassNotFoundException e2)
                {
                throw new AssertionError(e2);
                }
            }
        }

    private static Class<?> statusClass()
        {
        try
            {
            return Class.forName("java.io.ObjectInputFilter$Status");
            }
        catch (ClassNotFoundException e)
            {
            try
                {
                return Class.forName("sun.misc.ObjectInputFilter$Status");
                }
            catch (ClassNotFoundException e2)
                {
                throw new AssertionError(e2);
                }
            }
        }
    }
