/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.Processors;

import graal.pojo.LorCharacter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A collection of functional tests for testing script based {@code Filter}s.
 *
 * @author mk 2019.08.07
 */
public class ScriptFilterTests
        extends AbstractGraalFunctionalTest
    {

    /**
     * Create a new ScriptFilterTests that will use the specified
     * serializer in test methods.
     *
     * @param sSerializer the serializer name
     */
    public ScriptFilterTests(String sSerializer)
        {
        super(sSerializer);
        }

    @Test
    public void testEvenAgeFilter()
        {
        String filterName = "AgeFilter";
        String processorName = "UpperCaseProcessor";
        Filter<LorCharacter> filter;

        InvocableMap.EntryProcessor<String, LorCharacter, String> ep =
                Processors.script("js", processorName, "name");

        for (boolean checkEven : new boolean[] {true, false})
            {
            // Reset cache entries.
            populateCache();

            filter = Filters.script("js", filterName, checkEven);

            getNamedCache().invokeAll(filter, ep);

            for (LorCharacter c : aLorChars)
                {
                String expected = c.getName();
                if (c.getAge() % 2 == (checkEven ? 0 : 1))
                    {
                    expected = c.getName().toUpperCase();
                    }
                LorCharacter uc = (LorCharacter) getNamedCache().get(c.getName());
                assertEquals(expected, uc.getName());
                }
            }
        }
    }
