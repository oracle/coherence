/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.reflect.internal;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

/**
 * A test suite for {@link NameManglers}.
 *
 * @author hr
 * @since 3.7.1
 */
public class ManglersTest
    {
    /**
     * Ensure we can normalize a field name based on known conventions.
     */
    @Test
    public void testFieldMangler()
        {
        NameMangler mangler = NameManglers.FIELD_MANGLER;
        String sEclipseMangled = mangler.mangle("fFoo");
        String sCppMangled     = mangler.mangle("m_Foo");

        assertThat(sEclipseMangled, is("foo"));
        assertThat(sCppMangled    , is("foo"));
        }

    /**
     * Ensure we can normalize method names.
     */
    @Test
    public void testMethodMangler()
        {
        NameMangler mangler = NameManglers.METHOD_MANGLER;
        String sEclipseMangled = mangler.mangle("getFoo");
        String sCppMangled     = mangler.mangle("setFoo");

        assertThat(sEclipseMangled, is("foo"));
        assertThat(sCppMangled    , is("foo"));        
        }
    }
