/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.io.pof.reflect.PofNavigator;

import com.tangosol.util.ValueExtractor;

import org.junit.Test;

import static com.tangosol.util.extractor.AbstractExtractor.KEY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import static org.mockito.Mockito.mock;

/**
 * Validate equivalence between KeyExtractor, ReflectionExtractor with KEY and PofExtractor with KEY and provided canonical name.
 *
 * @author jf  2017.10.19
 */
@SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
public class KeyExtractorTest
    {
    @Test
    public void testKeyExtractorEquivalence()
        {
        KeyExtractor                   keyExtractor    = new KeyExtractor("getName");
        ReflectionExtractor            refKeyExtractor = new ReflectionExtractor("getName", null, KEY);
        PofExtractor                   pofKeyExtractor = new PofExtractor<>(String.class,  mock(PofNavigator.class), KEY, "name");

        assertEquals(keyExtractor, refKeyExtractor);
        assertEquals(keyExtractor, pofKeyExtractor);
        assertEquals(refKeyExtractor, pofKeyExtractor);

        assertEquals(keyExtractor.hashCode(), refKeyExtractor.hashCode());
        assertEquals(pofKeyExtractor.hashCode(), refKeyExtractor.hashCode());
        assertEquals(keyExtractor.hashCode(), refKeyExtractor.hashCode());
        }

    @Test
    public void testEquivalentKeyExtractorsWithChainedExtractors()
        {
        PofNavigator     navigator           = mock(PofNavigator.class);

        ValueExtractor[] keyVe = new ValueExtractor[] {new ReflectionExtractor("getBlah", null, KEY)};
        ValueExtractor[] ve    = new ValueExtractor[] {new ReflectionExtractor("getBlah")};

        // Equivalent Extractors
        KeyExtractor        keyExtractor        = new KeyExtractor("getBlah");
        ReflectionExtractor refKeyExtractor     = new ReflectionExtractor("getBlah", null, KEY);
        ChainedExtractor    chainedKeyExtractor = new ChainedExtractor(keyVe);
        PofExtractor        pofKeyExtractor     = new PofExtractor<>(String.class, navigator, KEY, "blah");

        // Non Equivalent Extractor   (differ only by target)
        ReflectionExtractor refExtractor     = new ReflectionExtractor("getBlah", null);
        ChainedExtractor    chainedExtractor = new ChainedExtractor(ve);

        assertEquals(keyExtractor, refKeyExtractor);
        assertEquals(keyExtractor.hashCode(), refKeyExtractor.hashCode());
        assertEquals(keyExtractor, chainedKeyExtractor);
        assertEquals(keyExtractor.hashCode(), chainedKeyExtractor.hashCode());
        assertEquals(refKeyExtractor, chainedKeyExtractor);
        assertEquals(refKeyExtractor.hashCode(), chainedKeyExtractor.hashCode());
        assertEquals(keyExtractor, refKeyExtractor);
        assertEquals(keyExtractor.hashCode(), refKeyExtractor.hashCode());
        assertEquals(keyExtractor, pofKeyExtractor);
        assertEquals(keyExtractor.hashCode(), pofKeyExtractor.hashCode());
        assertEquals(pofKeyExtractor, refKeyExtractor);
        assertEquals(pofKeyExtractor.hashCode(), refKeyExtractor.hashCode());

        assertNotEquals(keyExtractor, refExtractor);
        assertNotEquals(refKeyExtractor, refExtractor);
        assertNotEquals(chainedKeyExtractor, refExtractor);
        assertNotEquals(chainedKeyExtractor, refExtractor);
        assertNotEquals(chainedKeyExtractor, chainedExtractor);
        assertNotEquals(pofKeyExtractor, refExtractor);
        assertNotEquals(pofKeyExtractor, chainedExtractor);
        }
    }
