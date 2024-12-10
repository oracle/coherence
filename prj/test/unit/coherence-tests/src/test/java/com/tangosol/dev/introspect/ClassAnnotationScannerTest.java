/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.dev.introspect;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * ClassAnnotationScannerTest is tests the functionality within
 * ClassAnnotationScanner.
 *
 * @author hr  2012.07.30
 *
 * @since Coherence 12.1.2
 */
public class ClassAnnotationScannerTest
    {

    public ClassAnnotationScannerTest()
        {
        m_scanner = new ClassAnnotationScanner();
        }

    @Test
    public void testPlacement()
        {
        UrlScanner<String> scanner    = m_scanner;
        String             sClassName = scanner.scan(m_loader.getResource("com/tangosol/dev/introspect/TestPlacement.class"));

        assertThat(sClassName, is(TestPlacement.class.getName()));
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link UrlScanner} to scan resources with.
     */
    protected UrlScanner<String> m_scanner;

    /**
     * ClassLoader used to lookup resources.
     */
    protected ClassLoader m_loader = Base.getContextClassLoader();
    }
