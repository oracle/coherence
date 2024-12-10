/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.config.TestSerializableHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link Parameter}.
 *
 * @author pfm 2013.09.24
 */
public class ParameterTest
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
        validate(TestSerializableHelper.<Parameter>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<Parameter>convertEL(createAndPopulate()));
        }

    /**
     * Test non-null class using POF.
     */
    @Test
    public void testClassPof()
        {
        Parameter paramPof = TestSerializableHelper.convertPof(new Parameter(TEST_NAME, String.class, TEST_EXPR));
        validate(paramPof);
        assertEquals(String.class, paramPof.getExplicitType());
        }

    /**
     * Test non-null class using ExternalizableLite.
     */
    @Test
    public void testClassEl()
        {
        Parameter paramEl = TestSerializableHelper.convertEL(new Parameter(TEST_NAME, String.class, TEST_EXPR));
        validate(paramEl);
        assertEquals(String.class, paramEl.getExplicitType());
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the Parameter.
     *
     * @return the populated Parameter
     */
    protected Parameter createAndPopulate()
        {
        return new Parameter(TEST_NAME, null, TEST_EXPR);
        }

    /*
     * Validate the Parameter.
     *
     * @param  param the populated Parameter
     */
    protected void validate(Parameter param)
        {
        assertEquals(TEST_NAME, param.getName());
        assertEquals(TEST_EXPR.evaluate(new NullParameterResolver()),param.getExpression().evaluate(
                             new NullParameterResolver()));
        }

    // ----- data members ---------------------------------------------------

    private String                    TEST_NAME = "my-param";
    private LiteralExpression<String> TEST_EXPR = new LiteralExpression<String>("my-expr");
    }
