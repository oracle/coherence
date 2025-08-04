/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.internal.util;


import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit Test for {@link CanonicalNames}
 *
 * @author cp   05/15/2025
 * @since  25.03.2
 */
public class CanonicalNamesTests
    {

    /**
     * Test for COH-32395
     */
    @Test
    public void testComputeValueExtractorCanonicalName()
        {
        String canonicalName = CanonicalNames.computeValueExtractorCanonicalName("isbnNo");
        assertThat(canonicalName, is("isbnNo"));

        canonicalName = CanonicalNames.computeValueExtractorCanonicalName("getIsbnNo()");
        assertThat(canonicalName, is("isbnNo"));

        canonicalName = CanonicalNames.computeValueExtractorCanonicalName("isValidValue()");
        assertThat(canonicalName, is("validValue"));
        }
    }
