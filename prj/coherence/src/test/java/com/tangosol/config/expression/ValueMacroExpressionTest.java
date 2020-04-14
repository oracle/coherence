/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.config.expression;

import com.tangosol.config.ConfigurationException;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link ValueMacroExpression}.
 *
 * @author jf 2015.05.19
 *
 * @since Coherence 12.2.1
 */
public class ValueMacroExpressionTest
    {
    @Test
    public void testNoMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals("noMacroHere", sExpanded);
        }

    @Test
    public void testMalformedMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere${macro default";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new ParameterResolver()
        {
        public Parameter resolve(String parameter)
            {
            return new Parameter(parameter, "replacement");
            }
        });

        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testDefaultMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "near-${coherence.client direct}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-direct", sExpanded);
        }

    @Test
    public void testNoDefaultMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "near-${coherence.client}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-", sExpanded);
        }

    @Test
    public void testSimpleMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "near-${coherence.client direct}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                return new Parameter(parameter, "remote");
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-remote", sExpanded);
        }

    @Test
    public void testMultipleMacroProcessing()
            throws ConfigurationException
        {
        String               sValue = "prefix-${macro1 default1}-${macro2 default2}-postfix";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                return new Parameter(parameter, parameter + "_replacement");
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-macro1_replacement-macro2_replacement-postfix", sExpanded);
        }

    @Test
    public void testMultipleSameMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "prefix-${macro1 default1}-${macro1 default1}-postfix";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                return new Parameter(parameter, parameter + "_replacement");
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-macro1_replacement-macro1_replacement-postfix", sExpanded);
        }

    @Test
    public void testDefaultingMultipleSameMacroProcessing()
            throws ConfigurationException
        {
        String               sValue = "prefix-${macro1 default1}-${macro1 default2}-postfix";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);

        Assert.assertTrue(expr.containsMacro());

        String sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertEquals("prefix-default1-default2-postfix", sExpanded);
        }

    @Test
    public void testDefaultingMultipleMacroProcessing()
            throws ConfigurationException
        {
        String               sValue = "prefix-${macro1 default1}-${macro2 default2}-postfix";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);

        Assert.assertTrue(expr.containsMacro());

        String sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertEquals("prefix-default1-default2-postfix", sExpanded);
        }
    }
