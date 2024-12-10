/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config;

import com.tangosol.config.expression.Expression;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * {@link ParameterMacroExpressionTest} defines unit tests for {@link ParameterMacroExpression}s.
 *
 * @author bo  2012.02.06
 */
public class ParameterMacroExpressionTest
    {
    /**
     * Tests a boolean Macro without parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithoutParameter()
            throws ClassNotFoundException
        {
        Expression<Boolean> expr     = new ParameterMacroExpression<Boolean>("true", Boolean.class);
        ParameterResolver   resolver = new NullParameterResolver();

        assertTrue(expr.evaluate(resolver).booleanValue());
        }

    /**
     * Tests a boolean macro without parameters but with default value.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithParameterUsingDefaultValue()
            throws ClassNotFoundException
        {
        Expression<Boolean> expr     = new ParameterMacroExpression<Boolean>("{my-parameter true}", Boolean.class);
        ParameterResolver   resolver = new NullParameterResolver();

        assertTrue(expr.evaluate(resolver).booleanValue());
        }

    /**
     * Tests a boolean macro using a parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testBooleanMacroWithParameter()
            throws ClassNotFoundException
        {
        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("my-parameter", false));

        Expression<Boolean> expr = new ParameterMacroExpression<Boolean>("{my-parameter}", Boolean.class);

        assertTrue(!expr.evaluate(resolver).booleanValue());
        }

    /**
     * Tests a string macro without parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithoutParameter()
            throws ClassNotFoundException
        {
        Expression<String> expr     = new ParameterMacroExpression<String>("Hello World", String.class);
        ParameterResolver  resolver = new NullParameterResolver();

        assertEquals("Hello World", expr.evaluate(resolver));
        }

    /**
     * Tests a string macro with an undefined parameter but with a default.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameterUsingDefaultValue()
            throws ClassNotFoundException
        {
        Expression<String> expr     = new ParameterMacroExpression<String>("{my-parameter Gudday}", String.class);
        ParameterResolver  resolver = new NullParameterResolver();

        assertEquals("Gudday", expr.evaluate(resolver));
        }

    /**
     * Tests a macro with a single parameter.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameter()
            throws ClassNotFoundException
        {
        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("my-parameter", "Hello World"));

        Expression<String> expr = new ParameterMacroExpression<String>("{my-parameter}", String.class);

        assertEquals("Hello World", expr.evaluate(resolver));
        }

    /**
     * Tests a macro with multiple parameters.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParameters()
            throws ClassNotFoundException
        {
        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("my-parameter-1", "Hello"));
        resolver.add(new Parameter("my-parameter-2", "World"));

        Expression<String> expr = new ParameterMacroExpression<String>("({my-parameter-1}-{my-parameter-2})",
                                      String.class);

        assertEquals("(Hello-World)", expr.evaluate(resolver));
        }

    /**
     * Tests a macro with multiple parameters using defaults.
     *
     * @throws ClassNotFoundException if class not found
     */
    @Test
    public void testStringMacroWithParametersAndDefaults()
            throws ClassNotFoundException
        {
        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("my-parameter-1", "Hello"));
        resolver.add(new Parameter("my-parameter-2", "World"));

        Expression<String> expr =
            new ParameterMacroExpression<String>("Greeting is \\{{my-parameter-0 Gudday}-{my-parameter-2}\\}",
                                         String.class);

        assertEquals("Greeting is {Gudday-World}", expr.evaluate(resolver));
        }
    }
