/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;


import org.junit.Test;
import static org.junit.Assert.*;


/**
 * @author as  2015.06.02
 */
public class PredicateFilterTest
    {
    @Test
    public void testPredicateFilterWithExtractor()
        {
        PredicateFilter<String, Integer> filter = new PredicateFilter<>(String::length, n -> n > 3);

        assertTrue (filter.evaluate("Aleks"));
        assertFalse(filter.evaluate("Ana"));
        }

    @Test
    public void testPredicateFilterWithoutExtractor()
        {
        PredicateFilter<String, String> filter = new PredicateFilter<>(s -> s.length() > 3);

        assertTrue (filter.evaluate("Aleks"));
        assertFalse(filter.evaluate("Ana"));
        }
    }
