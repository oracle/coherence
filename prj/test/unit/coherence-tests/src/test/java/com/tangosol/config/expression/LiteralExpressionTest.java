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
 * Unit tests for the {@link LiteralExpression}.
 *
 * @author pfm 2013.09.24
 */
public class LiteralExpressionTest
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
        validate(TestSerializableHelper.<LiteralExpression<String>>convertPof(createAndPopulate()));
        }

    /**
     * Test ExternalizableLite serialization.
     */
    @Test
    public void testExternalizableLite()
        {
        validate(TestSerializableHelper.<LiteralExpression<String>>convertEL(createAndPopulate()));
        }

    // ----- helpers  -------------------------------------------------------

    /*
     * Create and populate the LiteralExpression.
     *
     * @return the populated LiteralExpression
     */
    protected LiteralExpression createAndPopulate()
        {
        return new LiteralExpression<String>(TEST_STR);
        }

    /*
     * Validate the LiteralExpression.
     *
     * @param  epr the populated LiteralExpression
     */
    protected void validate(LiteralExpression expr)
        {
        ParameterResolver resolver = new NullParameterResolver();
        assertEquals(expr.evaluate(resolver).toString(),TEST_STR);
        }

    // ----- data members ---------------------------------------------------

    private String TEST_STR = "hello";
    }
