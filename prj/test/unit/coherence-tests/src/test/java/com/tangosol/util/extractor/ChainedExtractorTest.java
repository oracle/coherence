/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.extractor;

import com.tangosol.io.pof.reflect.PofNavigator;

import com.tangosol.util.Extractors;
import com.tangosol.util.ValueExtractor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.Mockito.mock;

/**
 * Validate equivalence between KeyExtractor, ChainedExtractor and PofExtractor with provided canonical name.
 *
 * @author jf  2017.10.19
 */
public class ChainedExtractorTest
    {
    @Test
    public void testAgainstMultiExtractor()
        {
        ValueExtractor[] ve = new ValueExtractor[] {new ReflectionExtractor("bar"), new ReflectionExtractor("blah"), new ReflectionExtractor("foo")};

        ChainedExtractor    chainExtractor  = new ChainedExtractor(ve);
        MultiExtractor      multiExtractor  = new MultiExtractor(ve);

        assertNotEquals(chainExtractor, multiExtractor);
        assertNotEquals(multiExtractor, chainExtractor);
        assertNull(multiExtractor.getCanonicalName());
        assertNotNull(chainExtractor.getCanonicalName());
        }

    @Test
    public void testAgainstExtractorsMulti()
        {
        ValueExtractor chainExtractor  = Extractors.chained("bar", "blab", "foo");
        ValueExtractor multiExtractor  = Extractors.multi("bar", "blah", "foo");

        assertNotEquals(chainExtractor, multiExtractor);
        assertNotEquals(multiExtractor, chainExtractor);
        assertNull(multiExtractor.getCanonicalName());
        assertNotNull(chainExtractor.getCanonicalName());
        }

    /**
     * Validates that composite key extractors instances from {@link KeyExtractor}, {@link ChainedExtractor} and {@link PofExtractor}
     * are equivalent when appropriate.  Validate {@link AbstractExtractor#getTarget()} is part of equality computation.
     */
    @SuppressWarnings("AssertEqualsBetweenInconvertibleTypes")
    @Test
    public void testKeyCompositeExtractor()
        {
        PofNavigator     navigator           = mock(PofNavigator.class);
        KeyExtractor     keyExtractor        = new KeyExtractor("getBar.getBlah.getFoo");
        ChainedExtractor chainedKeyExtractor = new ChainedExtractor(new ValueExtractor[] {new ReflectionExtractor("getBar", null, AbstractExtractor.KEY), new ReflectionExtractor("getBlah"), new ReflectionExtractor("getFoo")});
        ChainedExtractor chainedExtractor    = new ChainedExtractor(new ValueExtractor[] {new ReflectionExtractor("getBar"), new ReflectionExtractor("getBlah"), new ReflectionExtractor("getFoo")});
        PofExtractor pofKeyExtractor         = new PofExtractor<>(String.class, navigator, AbstractExtractor.KEY, "bar.blah.foo");
        PofExtractor pofExtractor            = new PofExtractor<>(String.class, navigator, "bar.blah.foo");
        PofExtractor pofExtractor2           = new PofExtractor<>(String.class, navigator);

        assertEquals(keyExtractor, chainedKeyExtractor);
        assertEquals(keyExtractor.hashCode(), chainedKeyExtractor.hashCode());

        assertNotEquals(keyExtractor, chainedExtractor);
        assertNotEquals(chainedKeyExtractor, chainedExtractor);

        assertEquals(keyExtractor, pofKeyExtractor);
        assertEquals(keyExtractor.hashCode(), pofKeyExtractor.hashCode());

        assertEquals(chainedKeyExtractor, keyExtractor);
        assertEquals(chainedKeyExtractor.hashCode(), keyExtractor.hashCode());

        assertEquals(chainedKeyExtractor, pofKeyExtractor);
        assertEquals(chainedKeyExtractor.hashCode(), pofKeyExtractor.hashCode());

        assertEquals(chainedExtractor, pofExtractor);
        assertEquals(pofExtractor, chainedExtractor);
        assertEquals(chainedExtractor.hashCode(), pofExtractor.hashCode());

        assertEquals(keyExtractor, pofKeyExtractor);
        assertEquals(keyExtractor.hashCode(), pofKeyExtractor.hashCode());

        assertNotEquals(chainedKeyExtractor, pofExtractor);
        assertNotEquals(keyExtractor, pofExtractor);

        assertNotEquals(keyExtractor, pofExtractor);
        assertNotEquals(pofKeyExtractor, pofExtractor);
        assertNotEquals(pofExtractor, pofExtractor2);
        assertNotEquals(pofKeyExtractor, pofExtractor2);

        assertNotEquals(pofKeyExtractor.hashCode(), pofExtractor2.hashCode());
        }

    @Test
    public void testNonCanonicalChainedExtractorEquivalence()
        {
        ValueExtractor[] ve1 = new ValueExtractor[]
                {
                        new ReflectionExtractor("getBar"),
                        new ReflectionExtractor("getBlah"),
                        new ReflectionExtractor("getFoo")
                };

        ValueExtractor[] ve2 = new ValueExtractor[]
                {
                        new PofExtractor<>(Object.class, 4, "bar"),
                        new PofExtractor<>(Object.class, 5, "blah"),
                        new PofExtractor<>(Object.class, 6, "foo")
                };

        // nonsensical but still worth testing these ChainedExtractors with
        // missing CanonicalName
        ValueExtractor[] ve3 = new ValueExtractor[]
                {
                        new PofExtractor<>(Object.class, 4, "bar"),
                        new PofExtractor<>(Object.class, 5, "blah"),
                        new PofExtractor<>(Object.class, 6)
                };

        ValueExtractor[] ve4 = new ValueExtractor[]
                {
                        new PofExtractor<>(Object.class, 4, "bar"),
                        new PofExtractor<>(Object.class, 5),
                        new PofExtractor<>(Object.class, 6, "foo")
                };

        ValueExtractor[] ve5 = new ValueExtractor[]
                {
                        new PofExtractor<>(Object.class, 4, "bar"),
                        new PofExtractor<>(Object.class, 5),
                        new PofExtractor<>(Object.class, 6, "foo")
                };

        ChainedExtractor ce1 = new ChainedExtractor(ve1);
        ChainedExtractor ce2 = new ChainedExtractor(ve2);
        ChainedExtractor ce3 = new ChainedExtractor(ve3);
        ChainedExtractor ce4 = new ChainedExtractor(ve4);
        ChainedExtractor ce5 = new ChainedExtractor(ve5);
        ChainedExtractor ce6 = new ChainedExtractor("getBar.getBlah.getFoo");
        ValueExtractor   ce7 = Extractors.chained("bar", "blah", "foo");


        // Equivalent cases
        assertNotNull(ce1.getCanonicalName());
        assertNotNull(ce2.getCanonicalName());
        assertNotNull(ce6.getCanonicalName());
        assertNotNull(ce7.getCanonicalName());

        assertEquals(ce1, ce2);
        assertEquals(ce1.hashCode(), ce2.hashCode());

        assertEquals(ce4, ce5);
        assertEquals(ce4.hashCode(), ce5.hashCode());

        assertEquals(ce6, ce1);
        assertEquals(ce6.hashCode(), ce1.hashCode());

        assertEquals(ce7, ce1);
        assertEquals(ce7.hashCode(), ce1.hashCode());

        assertEquals(ce6, ce2);
        assertEquals(ce6.hashCode(), ce2.hashCode());

        // non Equivalent cases
        assertNull(ce3.getCanonicalName());
        assertNull(ce4.getCanonicalName());
        assertNull(ce5.getCanonicalName());

        assertNotEquals(ce2, ce3);
        assertNotEquals(ce2, ce4);
        assertNotEquals(ce2, ce5);

        assertNotEquals(ce6, ce3);
        assertNotEquals(ce6, ce4);
        assertNotEquals(ce6, ce5);

        assertNotEquals(ce3, ce4);
        }
    }
