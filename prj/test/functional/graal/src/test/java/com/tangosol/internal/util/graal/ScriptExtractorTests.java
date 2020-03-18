/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util.graal;

import com.tangosol.internal.util.graal.pojo.LorCharacter;

import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ReflectionExtractor;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import java.util.stream.Collectors;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * A collection of functional tests for the {@link ValueExtractor}.
 *
 * @author mk 2019.08.07
 */
public class ScriptExtractorTests
        extends AbstractGraalFunctionalTest
    {

    /**
     * Create a new ScriptExtractorTests that will use the specified
     * serializer in test methods.
     *
     * @param sSerializer the serializer name
     */
    public ScriptExtractorTests(String sSerializer)
        {
        super(sSerializer);
        }

    /**
     * A simple test for the {@link ReflectionExtractor}.
     */
    @Test
    public void testNameExtractor()
        {
        ValueExtractor<LorCharacter, String> extractor =
                Extractors.script("js", "NameExtractor");

        assertNotNull(extractor);

        Set<Map.Entry<String, LorCharacter>> entries = getNamedCache()
                .entrySet(Filters.equal(extractor, "Bilbo"));

        assertTrue(entries.size() == 1);
        assertTrue(entries.iterator().next().getValue().getName().equals("Bilbo"));
        assertTrue(entries.iterator().next().getValue().getAge() == 111);
        }

    /**
     * A simple test for the {@link ReflectionExtractor}.
     */
    @Test
    public void testAgeMoreThan100Extractor()
        {
        ValueExtractor<LorCharacter, Integer> extractor =
                Extractors.script("js", "AgeExtractor");

        assertNotNull(extractor);

        Set<Map.Entry<String, LorCharacter>> entries = getNamedCache()
                .entrySet(Filters.greaterEqual(extractor, 100));

        assertTrue(entries.size() == 2);
        Set<String> names = entries.stream().map(e -> e.getKey()).collect(Collectors.toSet());
        assertEqualKeySet(names, Arrays.asList("Bilbo", "Galadriel").stream().collect(Collectors.toSet()));
        }
    }
