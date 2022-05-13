/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package graal;

import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.Processors;

import com.tangosol.util.stream.RemoteCollectors;

import graal.pojo.LorCharacter;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A collection of functional tests for testing script based
 * {@link EntryProcessor}s.
 *
 * @author mk 2019.08.07
 */
public class ScriptEntryProcessorTests
        extends AbstractGraalFunctionalTest
    {

    /**
     * Create a new ScriptEntryProcessorTests that will use the specified
     * serializer in test methods.
     *
     * @param sSerializer the serializer name
     */
    public ScriptEntryProcessorTests(String sSerializer)
        {
        super(sSerializer);
        }

    /**
     * Test if an ES6 JavaScript can import another ES6 script and use
     * methods on it. processors/flip_case_processor.mjs imports
     * './es6_case_converter.mjs' which defines two static functions.
     */
    @Test
    public void testUpperCaseProcessor()
        {
        String processorName = "UpperCaseProcessor";
        EntryProcessor<String, LorCharacter, String> proc;

        proc = Processors.script("js", processorName, "name");

        getNamedCache().invokeAll(proc);
        for (LorCharacter c : aLorChars)
            {
            LorCharacter uc = (LorCharacter) getNamedCache().get(c.getName());
            assertEquals(c.getName().toUpperCase(), uc.getName());
            }

        Filter<LorCharacter> myFltr = Filters.equal("gender", "male");

        NamedCache<String, LorCharacter> cache = getNamedCache();

        Object result = cache
                .stream(myFltr)
                .map(e -> e.getKey())
                .collect(RemoteCollectors.toSet());

        System.out.println("******************************************");
        System.out.println("** Result: " + result);
        System.out.println("******************************************");
        }

    /**
     * Test if an ES5 JavaScript can import another ES5 script using the
     * {@code require} function and use methods on it.
     * processors/flip_case_processor.js requires 'case_converter' which
     * is present in the node_modules dir.
     */
    @Test
    public void testLowerCaseProcessor()
        {
        String processorName = "LowerCaseProcessor";
        EntryProcessor<String, LorCharacter, String> proc;

        proc = Processors.script("js", processorName, "name");

        getNamedCache().invokeAll(proc);
        for (LorCharacter c : aLorChars)
            {
            LorCharacter uc = (LorCharacter) getNamedCache().get(c.getName());
            assertEquals(c.getName().toLowerCase(), uc.getName());
            }
        }
    }
