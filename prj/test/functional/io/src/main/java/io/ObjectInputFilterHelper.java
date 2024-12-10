/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import com.oracle.coherence.common.base.Logger;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.ObjectInputStream;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.function.BinaryOperator;

import com.oracle.coherence.testing.CheckJDK;


/**
 * Helper to invoke ObjectInputFilter methods via reflection.
 *
 * @author jf  2021.09.29
 */
public class ObjectInputFilterHelper
        extends ExternalizableHelper
    {
    /**
     * Create an ObjectInputFilter from filter.
     *
     * @return return an ObjectInputFilter as an Object to enable working with
     * Java version 8
     */
    public static Object createObjectInputFilter(String sFilter)
        {
        Class<?> clzConfig = ExternalizableHelper.getClass("java.io.ObjectInputFilter$Config");

        clzConfig = clzConfig == null
                    ? ExternalizableHelper.getClass("sun.misc.ObjectInputFilter$Config")
                    : clzConfig;

        if (clzConfig != null)
            {
            try
                {
                Method methodConfigCreateFilter = clzConfig.getDeclaredMethod("createFilter", String.class);
                methodConfigCreateFilter.setAccessible(true);

                return methodConfigCreateFilter.invoke(null, sFilter);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                Base.err("Unable to invoke createFilter on ObjectInputFilter$Config");
                Base.err(e);
                }
            catch (Throwable t)
                {
                Base.err(t);
                }
            }
        return null;
        }

    /**
     * Get the process wide ObjectInputFilter.
     *
     * @return return process wide ObjectInputFilter
     */
    public static Object getConfigSerialFilter()
        {
        Class<?> clzConfig = ExternalizableHelper.getClass("java.io.ObjectInputFilter$Config");

        clzConfig = clzConfig == null
                    ? ExternalizableHelper.getClass("sun.misc.ObjectInputFilter$Config")
                    : clzConfig;

        if (clzConfig != null)
            {
            try
                {
                Method methodGetSerialFilter = clzConfig.getDeclaredMethod("getSerialFilter");
                methodGetSerialFilter.setAccessible(true);

                return methodGetSerialFilter.invoke(null);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                Base.err("Unable to invoke getSerialFilter on ObjectInputFilter$Config");
                Base.err(e);
                }
            catch (Throwable t)
                {
                Base.err(t);
                }
            }
        return null;
        }

    /**
     * Get the process wide serial filter factory
     *
     * @return return process wide ObjectInputFilter
     */
    public static BinaryOperator getConfigSerialFilterFactory()
        {
        Class<?> clzConfig = ExternalizableHelper.getClass("java.io.ObjectInputFilter$Config");

        clzConfig = clzConfig == null
                    ? ExternalizableHelper.getClass("sun.misc.ObjectInputFilter$Config")
                    : clzConfig;

        if (clzConfig != null)
            {
            try
                {
                Method methodGetSerialFilter = clzConfig.getDeclaredMethod("getSerialFilterFactory");
                methodGetSerialFilter.setAccessible(true);

                return (BinaryOperator) methodGetSerialFilter.invoke(null);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                Base.err("Unable to invoke getSerialFilterFactory on ObjectInputFilter$Config due to " +
                         e.getClass().getName() + ":" + e.getMessage() +
                         " using java version:" + System.getProperty("java.version"));
                }
            catch (Throwable t) {}
            }
        return null;
        }

    /**
     * Set filter for ObjectInputStream.
     *
     * @param sFilter filter pattern as used by JEP-290
     * @return ture if ObjectInputFilter is supported
     */
    public static boolean setObjectInputStreamFilter(ObjectInputStream ois, String sFilter)
            throws Throwable
        {
        try
            {
            Class<?> clzFilter      = null;
            String sSetFilterMethod = null;
            Method methodSetFilter  = null;

            if ((clzFilter = ExternalizableHelper.getClass("java.io.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setObjectInputFilter";
                }
            else if ((clzFilter = ExternalizableHelper.getClass("sun.misc.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setInternalObjectInputFilter";
                }

            if (sSetFilterMethod != null)
                {
                Object filter = sFilter != null
                                ? createObjectInputFilter(sFilter)
                                : null;

                if (filter != null)
                    {
                    return setObjectInputStreamFilter(ois, filter);
                    }

                return true;
                }
            }
        catch (IllegalStateException e)
            {
            throw e.getCause();
            }
        catch (Exception e)
            {
            }

        return false;
        }


    /**
     * Set filter for ObjectInputStream.
     *
     * @param filter ObjectInputFilter
     * @return ture if ObjectInputFilter is supported
     */
    public static boolean setObjectInputStreamFilter(ObjectInputStream ois, Object filter)
            throws Throwable
        {
        try
            {
            Class<?> clzFilter      = null;
            String sSetFilterMethod = null;
            Method methodSetFilter  = null;

            if ((clzFilter = ExternalizableHelper.getClass("java.io.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setObjectInputFilter";
                }
            else if ((clzFilter = ExternalizableHelper.getClass("sun.misc.ObjectInputFilter")) != null)
                {
                sSetFilterMethod = "setInternalObjectInputFilter";
                }

            if (sSetFilterMethod != null)
                {
                Class clzObjectInputStream = ObjectInputStream.class;

                methodSetFilter = clzObjectInputStream.getDeclaredMethod(sSetFilterMethod, clzFilter);
                methodSetFilter.setAccessible(true);
                methodSetFilter.invoke(ois, filter);

                return true;
                }
            }
        catch (InvocationTargetException e)
            {
            throw e.getCause();
            }
        catch (Exception e)
            {
            e.printStackTrace();
            }

        return false;
        }

    /**
     * Set filter for ObjectInputStream.
     *
     * @param sFilter  filter pattern as used by JEP-290
     *
     * @return true if ObjectInputFilter is supported
     */
    static public boolean setConfigObjectInputStreamFilter(String sFilter)
            throws Throwable
        {
        try
            {
            Class<?> clzFilter          = null;
            Class<?> clzFilterConfig    = null;
            Method   methodSetFilter    = null;

            if ((clzFilter = getClass("java.io.ObjectInputFilter")) != null)
                {
                clzFilterConfig = Class.forName("java.io.ObjectInputFilter$Config");
                }
            else if ((clzFilter = getClass("sun.misc.ObjectInputFilter")) != null)
                {
                clzFilterConfig = Class.forName("sun.misc.ObjectInputFilter$Config");
                }

            if (clzFilterConfig != null)
                {
                Object filter = sFilter != null  ? createObjectInputFilter(sFilter) : null;

                methodSetFilter = clzFilterConfig.getDeclaredMethod("setSerialFilter", clzFilter);
                methodSetFilter.setAccessible(true);
                methodSetFilter.invoke(null, filter);

                System.out.println("registered process-wide filter: " + getConfigSerialFilter());

                return true;
                }
            }
        catch (InvocationTargetException e)
            {
            throw e.getCause();
            }
        catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e)
            {
            out("setConfigObjectInputStreamFilter failed due to exception " + e.getCause().getClass().getName() + e.getCause().getMessage());
            }
        catch (Throwable t)
            {
            out(t);
            throw t;
            }

        return false;
        }

    /**
     * Set process-wide filter factory for ObjectInputFilter$Config in JDK 17
     * and higher.
     *
     * @param factory  serial filter factory
     *
     * @return true if able to set serial filter factory
     *
     * @throws IllegalStateException if called more than once in a process
     */
    static public boolean setConfigSerialFilterFactory(BinaryOperator factory)
        {
        try
            {
            if (factory != null)
                {
                Class<?> clzFilterConfig = ExternalizableHelper.getClass("java.io.ObjectInputFilter$Config");

                Method methodSetSerialFilterFactory = clzFilterConfig.getDeclaredMethod("setSerialFilterFactory", BinaryOperator.class);

                methodSetSerialFilterFactory.setAccessible(true);
                methodSetSerialFilterFactory.invoke(null, factory);
                System.out.println("registered process wide serial filter factory: " + factory);

                return true;
                }
            }
        catch (Exception e)
            {
            CheckJDK.assumeJDKVersionEqualOrGreater(17);
            Logger.fine(e);
            }
        return false;
        }

    /**
     * Return a merged ObjectInputFilter of param {@code filter1} and param {@code filter2}.
     *
     * @return return an ObjectInputFilter as an Object to enable working with
     * Java version 8
     */
    public static Object merge(Object filter1, Object filter2)
        {
        Class<?> clzFilter = ExternalizableHelper.getClass("java.io.ObjectInputFilter");

        if (clzFilter != null)
            {
            try
                {

                Method methodMerge = clzFilter.getDeclaredMethod("merge", clzFilter, clzFilter);
                methodMerge.setAccessible(true);
                return methodMerge.invoke(null, filter1, filter2);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                Base.err("Unable to invoke createFilter on ObjectInputFilter$Config");
                Base.err(e);
                }
            catch (Throwable t)
                {
                CheckJDK.assumeJDKVersionEqualOrGreater(17);

                Base.err(t);
                }
            }
        return null;
        }

    /**
     * Return a wrapped ObjectInputFilter that rejects an undecided class.
     *
     * @return return an ObjectInputFilter as an Object to enable working with
     * Java version 8
     */
    public static Object rejectUndecidedClass(Object filter)
        {
        Class<?> clzFilter = ExternalizableHelper.getClass("java.io.ObjectInputFilter");

        if (clzFilter != null)
            {
            try
                {
                Method methodRejectUndecidedClass = clzFilter.getDeclaredMethod("rejectUndecidedClass", clzFilter);

                methodRejectUndecidedClass.setAccessible(true);
                return methodRejectUndecidedClass.invoke(null, filter);
                }
            catch (IllegalAccessException | InvocationTargetException e)
                {
                Base.err("Unable to invoke rejectUndecidedClass on ObjectInputFilter");
                Base.err(e);
                }
            catch (Throwable t)
                {
                CheckJDK.assumeJDKVersionEqualOrGreater(17);

                Base.err(t);
                }
            }
        return null;
        }
    }