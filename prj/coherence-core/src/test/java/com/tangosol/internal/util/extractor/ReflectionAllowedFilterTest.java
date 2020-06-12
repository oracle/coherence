/*
 * Copyright (c) 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util.extractor;

import org.junit.Test;

import java.sql.Ref;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.DEFAULT_REFLECT_ALLOWED_BLACKLIST;
import static com.tangosol.internal.util.extractor.ReflectionAllowedFilter.REFLECT_FILTER_SEPARATOR;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


/**
 * Unit tests for {@link ReflectionAllowedFilter}.
 *
 * @author jf  2020.05.11
 */
public class ReflectionAllowedFilterTest
    {
    @Test
    public void testDefaultValueExtractorReflectionAllowedFiltering()
        {
        assertThat(ReflectionAllowedFilter.INSTANCE.evaluate(Runtime.getRuntime().getClass()), is(false));
        assertThat(ReflectionAllowedFilter.INSTANCE.evaluate(Runtime.getRuntime().getClass()), is(false));
        assertThat(ReflectionAllowedFilter.INSTANCE.evaluate(Class.class), is(false));
        assertThat(ReflectionAllowedFilter.INSTANCE.evaluate(String.class), is(true));
        }

    @Test
    public void testNonBuiltinClass()
        {
        assertThat(ReflectionAllowedFilter.INSTANCE.evaluate(Point.class), is(true));
        }

    @Test
    public void testWhiteListRejectRest()
        {
        String sPattern = Point.class.getName() + REFLECT_FILTER_SEPARATOR + "!*";
        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(false));
        assertThat(filter.evaluate(Runtime.getRuntime().getClass()), is(false));
        assertThat(filter.evaluate(Date.class), is(false));
        }

    @Test
    public void testWhiteListOnly()
        {
        String sPattern = Point.class.getName();
        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(true));
        assertThat(filter.evaluate(Runtime.getRuntime().getClass()), is(true));
        assertThat(filter.evaluate(Date.class), is(true));
        }

    @Test
    public void testPackageOnlyWhitelist()
        {
        String sPattern = Date.class.getPackage().getName() + ".*";
        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(true));
        assertThat(filter.evaluate(Date.class), is(true));
        assertThat(filter.evaluate(AtomicBoolean.class), is(true));
        }

    @Test
    public void testAllowSubPackageWhitelist()
        {
        String sPattern = Date.class.getPackage().getName() + ".**";
        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(true));
        assertThat(filter.evaluate(Date.class), is(true));
        assertThat(filter.evaluate(AtomicBoolean.class), is(true));
        }

    @Test
    public void testPackageOnlyBlacklist()
        {
        String sPattern = "!" + Date.class.getPackage().getName() + ".*";
        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(true));
        assertThat(filter.evaluate(Date.class), is(false));
        assertThat(filter.evaluate(AtomicBoolean.class), is(true));
        }

    @Test
    public void testAllowSubPackageBlacklist()
        {
        String sPattern = "!" + Date.class.getPackage().getName() + ".**";

        ReflectionAllowedFilter filter = ReflectionAllowedFilter.ensureFilter(sPattern);
        assertThat(filter.evaluate(Point.class), is(true));
        assertThat(filter.evaluate(String.class), is(true));
        assertThat(filter.evaluate(Date.class), is(false));
        assertThat(filter.evaluate(AtomicBoolean.class), is(false));
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidEmptyPattern()
        {
        ReflectionAllowedFilter.ensureFilter("");
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPackageMissingForAllClassesInPackage()
        {
        ReflectionAllowedFilter.ensureFilter(".*");
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPackageMissingForAllSubPackages()
        {
        ReflectionAllowedFilter.ensureFilter(".**");
        }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedModuleSyntax()
        {
        ReflectionAllowedFilter.ensureFilter("java.basic/java.lang.String");
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidSeparator()
        {
        ReflectionAllowedFilter.ensureFilter("java.lang.String,!java.lang.Runtime,*");
        }

    @Test
    public void testEnsureSafeFilterNullPattern()
        {
        assertThat(ReflectionAllowedFilter.ensureSafeFilter(null).toString(), is(ReflectionAllowedFilter.DEFAULT_FILTER_LIST));
        }

    @Test
    public void testEnsureSafeFilterEmptyPattern()
        {
        assertThat(ReflectionAllowedFilter.ensureSafeFilter("").toString(), is(ReflectionAllowedFilter.DEFAULT_FILTER_LIST));
        }

    @Test
    public void testInvalidPackageMissingForAllSubPackagesEnsureSafeFilter()
        {
        assertThat(ReflectionAllowedFilter.ensureSafeFilter(".**").toString(), is(ReflectionAllowedFilter.DEFAULT_FILTER_LIST));
        }

    @Test
    public void testInvalidSeparatorUsingEnsureSafeFilter()
        {
        ReflectionAllowedFilter.ensureSafeFilter(DEFAULT_REFLECT_ALLOWED_BLACKLIST + ",java.lang.String,*");
        }
    // ----- inner class: Point ---------------------------------------------
    
    public static class Point
        {
        int x;
        int y;
        }
    }
