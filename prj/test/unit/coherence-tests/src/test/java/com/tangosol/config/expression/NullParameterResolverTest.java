/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.config.TestSerializableHelper;

import org.junit.Test;

/**
 * Unit tests for the {@link NullParameterResolver}.
 *
 * @author pfm 2013.09.24
 */
public class NullParameterResolverTest
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
        validate(TestSerializableHelper.<NullParameterResolver>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<NullParameterResolver>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the NullParameterResolver.
     *
     * @return the populated NullParameterResolver
     */
    protected ParameterResolver createAndPopulate()
        {
        return new NullParameterResolver();
        }

    /*
     * Validate the NullParameterResolver.
     *
     * @param  resolver the populated NullParameterResolver
     */
    protected void validate(ParameterResolver resolver)
        {
        }
    }
