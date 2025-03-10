/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.util.ValueExtractor;

/**
 * A utility class for working with Filters.
 */
public final class FilterHelper
    {

    // ---- constructors ----------------------------------------------------

    /**
     * Not able to be constructed.
     */
    private FilterHelper()
       {
       }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return a String representation of a {@link ValueExtractor}.
     *
     * @param extractor {@link ValueExtractor}
     *
     * @return a string representation
     */
    public static <T, E> String getExtractorName(ValueExtractor<T, E> extractor)
        {
        String sName          = Lambdas.isLambda(extractor) ? extractor.getCanonicalName() : null;
        String sCanonicalName = extractor.getCanonicalName();

        return sName != null ? sName :
                sCanonicalName != null ? sCanonicalName : extractor.toString();
        }
    }
