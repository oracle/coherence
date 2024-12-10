/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.coherence.config.ResolvableParameterList;

import com.tangosol.config.TestSerializableHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link ChainedParameterResolver}.
 * 
 * @author bo 2012.12.04
 * 
 * @since Coherence 12.1.2
 */
public class ChainedParameterResolverTest
    {
    /**
     * Test basic get/set.
     */
    @Test
    public void testGetSet()
        {
        validate(createAndPopulate());
        }

    /**
     * Test POF serialization.
     */
    @Test
    public void testPof()
        {
        validate(TestSerializableHelper.<ChainedParameterResolver>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<ChainedParameterResolver>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
    * Create and populate the ChainedParameterResolver.
    *
    * @return the populated ChainedParameterResolver
    */
    protected ChainedParameterResolver createAndPopulate()
        {
        ResolvableParameterList list1 = new ResolvableParameterList();
        list1.add(new Parameter("greeting", "Gudday Mate"));

        ResolvableParameterList list2 = new ResolvableParameterList();
        list2.add(new Parameter("farewell", "See Ya"));

        return new ChainedParameterResolver(list1, list2);
        }

    /*
    * Validate the ChainedParameterResolver.
    *
    * @param  resolver the populated ChainedParameterResolver
    */
    protected void validate(ChainedParameterResolver resolver)
        {
        assertEquals("Gudday Mate", resolver.resolve("greeting").evaluate(null).as(String.class));
        assertEquals("See Ya", resolver.resolve("farewell").evaluate(null).as(String.class));
        }
    }
