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
 * Unit tests for the {@link ScopedParameterResolver}.
 *
 * @author pfm 2013.09.24
 */
public class ScopedParameterResolverTest
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
        validate(TestSerializableHelper.<ScopedParameterResolver>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<ScopedParameterResolver>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the ScopedParameterResolver.
     *
     * @return the populated ScopedParameterResolver
     */
    protected ScopedParameterResolver createAndPopulate()
        {
        ResolvableParameterList list1 = new ResolvableParameterList();
        list1.add(new Parameter("greeting", "Gudday Mate"));

        ScopedParameterResolver scoped = new ScopedParameterResolver(list1);
        scoped.add(new Parameter("farewell", "See Ya"));

        return scoped;
        }

    /*
    * Validate the ScopedParameterResolver.
    *
    * @param  resolver the populated ScopedParameterResolver
    */
    protected void validate(ScopedParameterResolver resolver)
        {
        assertEquals("Gudday Mate", resolver.resolve("greeting").evaluate(null).as(String.class));
        assertEquals("See Ya", resolver.resolve("farewell").evaluate(null).as(String.class));
        }
    }
