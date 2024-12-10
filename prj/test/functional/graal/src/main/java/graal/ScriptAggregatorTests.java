/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import com.tangosol.util.Aggregators;

import com.tangosol.util.InvocableMap.StreamingAggregator;

import com.tangosol.util.aggregator.ScriptAggregator;

import java.util.Map;

import java.util.stream.Stream;

import graal.pojo.LorCharacter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A collection of functional tests for the {@link StreamingAggregator}.
 *
 * @author mk 2019.09.24
 */
public class ScriptAggregatorTests
        extends AbstractGraalFunctionalTest
    {

    /**
     * Create a new ScriptAggregatorTests that will use the specified
     * serializer in test methods.
     *
     * @param sSerializer the serializer name
     */
    public ScriptAggregatorTests(String sSerializer)
        {
        super(sSerializer);
        }

    /**
     * A simple test for the {@link ScriptAggregator}.
     */
    @Test
    public void testAgeAdder()
        {
        StreamingAggregator aggregator = Aggregators.script("js", "AgeAdder");

        int result   = (int) getNamedCache().aggregate(aggregator);
        int expected = Stream.of(aLorChars).mapToInt(LorCharacter::getAge).sum();

        assertEquals(expected, result);
        }

    /**
     * A simple test for the {@link ScriptAggregator}.
     */
    @Test
    public void testMinMaxAgeFinder()
        {
        StreamingAggregator aggregator = Aggregators.script("js", "MinMaxAgeFinder");

        assertNotNull(aggregator);

        Map<String, Integer> result = (Map<String, Integer>) getNamedCache().aggregate(aggregator);

        int expectedMin = 33;
        int expectedMax = 8372;

        assertEquals(expectedMin, result.get("min").intValue());
        assertEquals(expectedMax, result.get("max").intValue());
        }
    }
