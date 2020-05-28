/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package extractor;


import com.tangosol.internal.util.extractor.ReflectionAllowedFilter;

import com.tangosol.util.WrapperException;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.extractor.ReflectionUpdater;
import com.tangosol.util.extractor.UniversalExtractor;
import com.tangosol.util.extractor.UniversalUpdater;

import data.TestInterface;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.DEFAULT_REFLECT_ALLOWED_BLACKLIST;
import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.REFLECT_FILTER_SEPARATOR;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Validate all reflection enabled extractors against a ReflectionAllowedFilter.INSTANCE configured by
 * system property {@link ReflectionAllowedFilter#REFLECT_FILTER_PROPERTY}.
 *
 * @author jf  2020.5.18
 */
public class CoherenceReflectFilterTests
    {
    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void startup()
        {
        System.setProperty(ReflectionAllowedFilter.REFLECT_FILTER_PROPERTY, REFLECT_FILTER_VALUE);
        assertThat(ReflectionAllowedFilter.INSTANCE.toString(), is(REFLECT_FILTER_VALUE));
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void testUniversalExtractor()
        {
        for (int i = 0; i < f_arr_oTarget.length; i++)
            {
            TestInterface target             = f_arr_oTarget[i];
            boolean       fReflectionAllowed = f_arr_fExpectedReflectionAllowed[i];

            UniversalExtractor<TestInterface, String> extractor = new UniversalExtractor<>("getProperty()");
            try
                {
                extractor.extract(target);
                assertTrue("testUniversalExtractor: targetClass=" + target.getClass().getName() +
                    " expected reflection not allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            catch (WrapperException e)
                {
                assertFalse("testUniversalExtractor: targetClass=" + target.getClass().getName() +
                    " expected reflection allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            }
        }

    @Test
    public void testUniversalUpdater()
        {
        for (int i = 0; i < f_arr_oTarget.length; i++)
            {
            TestInterface target             = f_arr_oTarget[i];
            boolean       fReflectionAllowed = f_arr_fExpectedReflectionAllowed[i];

            UniversalUpdater extractor = new UniversalUpdater("setProperty()");
            try
                {
                extractor.update(target, target.getClass().getName() + " updatedValue");
                assertTrue("testUniversalUpdater: targetClass=" + target.getClass().getName() +
                    " expected reflection not allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            catch (WrapperException e)
                {
                assertFalse("testUniversalUpdater: targetClass=" + target.getClass().getName() +
                    " expected reflection allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            }
        }

    @Test
    public void testReflectionExtractor()
        {
        for (int i = 0; i < f_arr_oTarget.length; i++)
            {
            TestInterface target             = f_arr_oTarget[i];
            boolean       fReflectionAllowed = f_arr_fExpectedReflectionAllowed[i];

            ReflectionExtractor<TestInterface, String> extractor = new ReflectionExtractor<>("getProperty");
            try
                {
                extractor.extract(target);
                assertTrue("testReflectionExtractor: targetClass=" + target.getClass().getName() +
                    " expected reflection not allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            catch (WrapperException e)
                {
                assertFalse("testReflectionExtractor: targetClass=" + target.getClass().getName() +
                    " expected reflection allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            }
        }

    @Test
    public void testReflectionUpdater()
        {
        for (int i = 0; i < f_arr_oTarget.length; i++)
            {
            TestInterface target             = f_arr_oTarget[i];
            boolean       fReflectionAllowed = f_arr_fExpectedReflectionAllowed[i];

            ReflectionUpdater extractor = new ReflectionUpdater("setProperty");
            try
                {
                extractor.update(target, target.getClass().getName() + " updatedValue");
                assertTrue("testReflectionUpdater: targetClass=" + target.getClass().getName() +
                    " expected reflection not allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            catch (WrapperException e)
                {
                assertFalse("testReflectionUpdater: targetClass=" + target.getClass().getName() +
                    " expected reflection allowed for coherence.reflect.filter=" + REFLECT_FILTER_VALUE,
                    fReflectionAllowed);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Set system property {@link ReflectionAllowedFilter#REFLECT_FILTER_PROPERTY} to this value for these tests.
     */
    private static String REFLECT_FILTER_VALUE = DEFAULT_REFLECT_ALLOWED_BLACKLIST + REFLECT_FILTER_SEPARATOR +
        "data.AllowReflection" + REFLECT_FILTER_SEPARATOR +
        "!data.RejectReflection" + REFLECT_FILTER_SEPARATOR +
        "data.allowPackage.*" + REFLECT_FILTER_SEPARATOR +
        "!data.rejectPackage.*" + REFLECT_FILTER_SEPARATOR +
        "*";

    /**
     * List of targets tested by each test.
     */
    private final TestInterface[] f_arr_oTarget =
        {
        new data.AllowReflection("AllowReflection"),
        new data.RejectReflection("RejectReflection"),
        new data.allowPackage.AllowReflection("allowPkg.AllowReflection"),
        new data.allowPackage.RejectReflection("allowPkg.RejectReflection"),
        new data.rejectPackage.AllowReflection("rejectPkg.AllowReflection"),
        new data.rejectPackage.RejectReflection("rejectPkg.RejectReflection")
        };

    /**
     * Expected results for each element of {@link #f_arr_oTarget}.
     * True if reflection should be allowed for extractor and false if reflection should be rejected for extractor.
     */
    private boolean[] f_arr_fExpectedReflectionAllowed =
        {
        true,
        false,
        true,
        true,
        false,
        false
        };
    }
