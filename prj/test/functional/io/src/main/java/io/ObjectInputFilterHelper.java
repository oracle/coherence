/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package io;


import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

import java.util.function.BinaryOperator;

/**
 * Helper to invoke ObjectInputFilter methods.
 *
 * @author jf  2021.09.29
 */
public class ObjectInputFilterHelper
    {
    /**
     * Create an ObjectInputFilter from filter.
     *
     * @return an ObjectInputFilter
     */
    public static Object createObjectInputFilter(String sFilter)
        {
        return ObjectInputFilter.Config.createFilter(sFilter);
        }

    /**
     * Get the process wide ObjectInputFilter.
     *
     * @return return process wide ObjectInputFilter
     */
    public static Object getConfigSerialFilter()
        {
        return ObjectInputFilter.Config.getSerialFilter();
        }

    /**
     * Get the process wide serial filter factory
     *
     * @return return process wide ObjectInputFilter
     */
    public static BinaryOperator getConfigSerialFilterFactory()
        {
        return ObjectInputFilter.Config.getSerialFilterFactory();
        }

    /**
     * Set filter for ObjectInputStream.
     *
     * @param sFilter filter pattern as used by JEP-290
     */
    public static void setObjectInputStreamFilter(ObjectInputStream ois, String sFilter)
        {
        Object filter = sFilter == null
                        ? null
                        : createObjectInputFilter(sFilter);

        if (filter != null)
            {
            setObjectInputStreamFilter(ois, filter);
            }
        }


    /**
     * Set filter for ObjectInputStream.
     *
     * @param filter ObjectInputFilter
     */
    public static void setObjectInputStreamFilter(ObjectInputStream ois, Object filter)
        {
        ois.setObjectInputFilter((ObjectInputFilter) filter);
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
        ObjectInputFilter filter = sFilter == null
                                   ? null
                                   : ObjectInputFilter.Config.createFilter(sFilter);

        ObjectInputFilter.Config.setSerialFilter(filter);
        System.out.println("registered process-wide filter: " + getConfigSerialFilter());
        return true;
        }

    /**
     * Set the process-wide filter factory for ObjectInputFilter.Config.
     *
     * @param factory  serial filter factory
     *
     * @return true if able to set serial filter factory
     *
     * @throws IllegalStateException if called more than once in a process
     */
    static public boolean setConfigSerialFilterFactory(BinaryOperator factory)
        {
        if (factory != null)
            {
            ObjectInputFilter.Config.setSerialFilterFactory(factory);
            System.out.println("registered process wide serial filter factory: " + factory);
            return true;
            }
        return false;
        }

    /**
     * Return a merged ObjectInputFilter of param {@code filter1} and param {@code filter2}.
     *
     * @return the merged ObjectInputFilter
     */
    public static Object merge(Object filter1, Object filter2)
        {
        return ObjectInputFilter.merge((ObjectInputFilter) filter1, (ObjectInputFilter) filter2);
        }

    /**
     * Return a wrapped ObjectInputFilter that rejects an undecided class.
     *
     * @return an ObjectInputFilter that rejects undecided classes
     */
    public static Object rejectUndecidedClass(Object filter)
        {
        return ObjectInputFilter.rejectUndecidedClass((ObjectInputFilter) filter);
        }
    }
