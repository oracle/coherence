/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.extractor;

import data.extractor.TestInterface;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.List;

import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.DEFAULT_REFLECT_ALLOWED_BLACKLIST;
import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.REFLECT_FILTER_SEPARATOR;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class ReflectionAllowedFilterEnsureFilterTest
    {
    // ----- constructors ---------------------------------------------------

    public ReflectionAllowedFilterEnsureFilterTest(String sPatterns, boolean[] fAllow)
        {
        f_arr_fExpectedReflectionAllowed = fAllow;
        FILTER                           = ReflectionAllowedFilter.ensureFilter(sPatterns);
        }

    // ----- test lifecycle methods -----------------------------------------

    /**
     * Run all tests against these parameters and targets from {@link #f_arr_oTarget}.
     *
     * @return parameters to configure test constructor.
     */
    @Parameterized.Parameters(name ="EnsureFilterPattern={0} expectedReflectionAllowedResults={1}")
    public static List<Object[]> getParameters()
        {
        List<Object[]> list = new ArrayList<>();

        list.add(new Object[]
            {
                // allow reflection for all classes but blacklisted classes
                DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR + "*",

                // expected reflection allowed filter result for each target's class in f_arr_oTarget.
                new boolean[]{true, true, true, true, true, true}
            });
        list.add(new Object[]
            {
                // allow reflection for all classes but blacklisted classes using packages wildcard
                DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR + "data.extractor.**" + REFLECT_FILTER_SEPARATOR + "!*",

                // expected reflection allowed filter result for each target's class in f_arr_oTarget
                new boolean[]{true, true, true, true, true, true}
            });
        list.add(new Object[]
            {
                // reject all reflection attempts in extractor
                "!*",
                // expected reflection allowed filter result for each target's class in f_arr_oTarget
                new boolean[]{false, false, false, false, false, false}
            });
        list.add(
            new Object[]
                {
                    // reject any package with reject prefix and any class with Reject prefix; whitelist the rest
                    DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR +
                        "!data.extractor.Reject*" + REFLECT_FILTER_SEPARATOR +
                        "!data.extractor.rejectPackage.*" + REFLECT_FILTER_SEPARATOR +
                        "!data.extractor.allowPackage.RejectReflection" + REFLECT_FILTER_SEPARATOR + "*",

                    // expected reflection allowed filter result for each target's class in f_arr_oTarget
                    new boolean[]{true, false, true, false, false, false}
                });
        list.add(new Object[]
            {
                // reject any package with reject prefix and any class with Reject prefix; whitelist the rest;
                // whitelist all classes in package data
                DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR +
                    "!data.extractor.Reject*" + REFLECT_FILTER_SEPARATOR +
                    "!data.extractor.rejectPackage.*" + REFLECT_FILTER_SEPARATOR +
                    "!data.extractor.allowPackage.RejectReflection" + REFLECT_FILTER_SEPARATOR +
                    "data.extractor.**",

                // expected reflection allowed filter result for each target's class in f_arr_oTarget
                new boolean[]{true, false, true, false, false, false}
            });
        list.add(new Object[]
            {
                // reject all classes in package and subpackage(s)
                DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR + "!data.extractor.**" + REFLECT_FILTER_SEPARATOR + "*",
                new boolean[]{false, false, false, false, false, false}
            });
        list.add(new Object[]
            {
                // reject classes in package data and allow all classes in subpackages of package data.extractor.
                DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR + "!data.extractor.*" + REFLECT_FILTER_SEPARATOR +
                    "data.extractor.allowPackage.*" + REFLECT_FILTER_SEPARATOR + "data.extractor.rejectPackage.*",
                new boolean[]{false, false, true, true, true, true}
            });
        return list;
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testEnsureFilter()
        {
        for (int i = 0; i < f_arr_oTarget.length; i++)
            {
            TestInterface target             = f_arr_oTarget[i];
            boolean       fReflectionAllowed = f_arr_fExpectedReflectionAllowed[i];

            assertThat("target class=" + target.getClass().getName() + " pattern=" + FILTER, FILTER.evaluate(target.getClass()), is(fReflectionAllowed));
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * List of targets tested by each test.
     */
    private final TestInterface[] f_arr_oTarget =
        {
        new data.extractor.AllowReflection("AllowReflection"),
        new data.extractor.RejectReflection("RejectReflection"),
        new data.extractor.allowPackage.AllowReflection("allowPkg.AllowReflection"),
        new data.extractor.allowPackage.RejectReflection("allowPkg.RejectReflection"),
        new data.extractor.rejectPackage.AllowReflection("rejectPkg.AllowReflection"),
        new data.extractor.rejectPackage.RejectReflection("rejectPkg.RejectReflection")
        };

    /**
     * Filter being tested.
     */
    private final ReflectionAllowedFilter FILTER;

    /**
     * Expected results for each element of {@link #f_arr_oTarget} with current {@link #FILTER}.
     * True if reflection should be allowed by filter and false if reflection should be rejected for filter.
     */
    private final boolean[] f_arr_fExpectedReflectionAllowed;
    }
