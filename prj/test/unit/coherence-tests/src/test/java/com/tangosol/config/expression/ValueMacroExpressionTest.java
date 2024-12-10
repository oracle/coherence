/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
        String               sExpanded = expr.evaluate((p) -> new Parameter(p, "replacement"));

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
        String               sExpanded = expr.evaluate((s) -> new Parameter(s, "remote"));

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-remote", sExpanded);
        }

    @Test
    public void testMultipleMacroProcessing()
            throws ConfigurationException
        {
        String               sValue = "prefix-${macro1 default1}-${macro2 default2}-postfix";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> new Parameter(s, s + "_replacement"));

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-macro1_replacement-macro2_replacement-postfix", sExpanded);
        }

    @Test
    public void testMultipleSameMacroProcessing()
            throws ConfigurationException
        {
        String               sValue    = "prefix-${macro1 default1}-${macro1 default1}-postfix";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((s) -> new Parameter(s, s + "_replacement"));

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-macro1_replacement-macro1_replacement-postfix", sExpanded);
        }

    @Test(timeout = 10000)
    public void testCircularMacroExpansion()
            throws ConfigurationException
        {
        String               sValue = "${macro1}";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);
        String sExpanded;

        sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                if (parameter.equals("macro1"))
                    {
                    return new Parameter("name", "${macro2}");
                    }
                else if (parameter.equals("macro2"))
                    {
                    return new Parameter("name", "${macro1:-${macro3}}");
                    }

                return null;
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("", sExpanded);
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

    @Test
    public void testMacroForDefault()
        {
        String               sValue    = "near-${coherence.client ${default defaultValue}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((s) -> s.equals("default")
                                                              ? new Parameter(s,"remote")
                                                              : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-remote", sExpanded);
        }

    @Test
    public void testMacroForDefaultValue()
        {
        String               sValue    = "near-${coherence.client ${default defaultValue}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultValue", sExpanded);
        }

    @Test
    public void testMacroForDefaultWithBashDelimiter()
        {
        String               sValue    = "near-${coherence.client:-${default:-defaultValue}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((s) -> s.equals("default")
                                                              ? new Parameter(s,"remote")
                                                              : null);
        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-remote", sExpanded);
        }

    @Test
    public void testMacroForDefaultValueWithBashDelimiter()
        {
        String               sValue    = "near-${coherence.client:-${default:-defaultValue}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultValue", sExpanded);
        }

    @Test
    public void testMalformedMacroWithDefaultValue()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere${macro ${default defaultValue)";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((p) -> new Parameter(p, "replacement"));


        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testMalformedMacroWithDefaultValue2()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere${macro ${default defaultValue";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((p) -> new Parameter(p, "replacement"));


        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testMalformedMacroWithBashDelimiterDefaultValue()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere${macro:-${default:-defaultValue)";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((p) -> new Parameter(p, "replacement"));


        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testMalformedMacroWithBashDelimiterDefaultValue2()
            throws ConfigurationException
        {
        String               sValue    = "noMacroHere${macro:-${default:-defaultValue";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate((p) -> new Parameter(p, "replacement"));


        Assert.assertFalse(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }


    @Test
    public void testMultipleNestedMacros()
        {
        String               sValue    = "near-${coherence.client:-${default:-${defaultValue}}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);

        String sExpanded = expr.evaluate((s) -> s.equals("defaultValue") ? new Parameter(s, "defaultedDefaultedRemote") : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultedDefaultedRemote", sExpanded);

        expr      = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                if (parameter.equals("defaultValue"))
                    {
                    return new Parameter(parameter, "defaultedDefaultedRemote");
                    }
                else if (parameter.equals("default"))
                    {
                    return new Parameter(parameter, "defaultedRemote");
                    }
                return null;
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultedRemote", sExpanded);
        }

    @Test
    public void testMultipleMixedNestedMacros()
        {
        String               sValue    = "near-${coherence.client:-${default:-${defaultValue}}}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);

        String sExpanded = expr.evaluate((s) -> s.equals("defaultValue") ? new Parameter(s, "defaultedDefaultedRemote") : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultedDefaultedRemote", sExpanded);

        expr      = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate(new ParameterResolver()
            {
            public Parameter resolve(String parameter)
                {
                if (parameter.equals("defaultValue"))
                    {
                    return new Parameter(parameter, "defaulted${default2 never}");
                    }
                else if (parameter.equals("default"))
                    {
                    return new Parameter(parameter, "defaultedRemote");
                    }
                else if (parameter.equals("default2"))
                    {
                    return new Parameter(parameter, "remote");
                    }
                return null;
                }
            });

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("near-defaultedRemote", sExpanded);
        }

    @Test
    public void testSubstringExpansionOffsetOnly()
        {
        String sValue = "prefix-${parameter:7}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-7890abcdefgh", sExpanded);

        sValue = "prefix-${parameter: -7}";
        expr = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                         ? new Parameter(s,"01234567890abcdefgh")
                                         : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-bcdefgh", sExpanded);
        }

    @Test
    public void testSubstringExpansionOffsetLength()
        {
        String sValue = "prefix-${parameter:7:4}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-7890", sExpanded);

        sValue = "prefix-${parameter: -7:3}";
        expr = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter") ?
                                         new Parameter(s,"01234567890abcdefgh") : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-bcd", sExpanded);

        sValue = "prefix-${parameter: -7: -3}";
        expr = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                         ? new Parameter(s, "01234567890abcdefgh")
                                         : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-bcde", sExpanded);
        }

    @Test
    public void testSubstringExpansionOffsetOutOfBounds()
        {
        String sValue = "prefix-${parameter:99:4}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);
        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);

        sValue    = "prefix-${parameter: -99}";
        expr      = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);
        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-", sExpanded);
        }

    @Test
    public void testSubstringExpansionLengthOutOfBounds()
        {
        String sValue = "prefix-${parameter:7:99}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-7890abcdefgh", sExpanded);

        sValue = "prefix-${parameter:7: -99}";
        expr = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                         ? new Parameter(s,"01234567890abcdefgh")
                                         : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testDefaultWhenSet()
        {
        String               sValue = "prefix-${parameter:+Defaulted}";
        ValueMacroExpression expr   = new ValueMacroExpression(sValue);

        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s, "01234567890abcdefgh")
                                                : null);
        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-Defaulted", sExpanded);
        }

    @Test
    public void testSubexpressionExpansionOffsetSyntaxError()
        {
        String sValue = "prefix-${parameter:7r3:4}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testSubexpressionExpansionOffsetSyntaxError2()
        {
        String sValue = "prefix-${parameter:IncorrectLength:4}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    @Test
    public void testSubexpressionExpansionLengthSyntaxError()
        {
        String sValue = "prefix-${parameter:7:4r3}";
        ValueMacroExpression expr = new ValueMacroExpression(sValue);
        String sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                                ? new Parameter(s,"01234567890abcdefgh")
                                                : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals(sValue, sExpanded);
        }

    /**
     * No error, just not what user does not place space between ":" and "-" of negative offset.
     * This is exact behavior of bash shell for user error of missing required space.
     */
    @Test
    public void testSubstringExpansionMissingSpaceForNegativeLength()
        {
        String               sValue    = "prefix-${parameter:-7}";
        ValueMacroExpression expr      = new ValueMacroExpression(sValue);
        String               sExpanded = expr.evaluate(new NullParameterResolver());

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-7", sExpanded);

        sValue = "prefix-${parameter:-7}";
        expr = new ValueMacroExpression(sValue);
        sExpanded = expr.evaluate((s) -> s.equals("parameter")
                                         ?
                                         new Parameter(s,"01234567890abcdefgh")
                                         : null);

        Assert.assertTrue(expr.containsMacro());
        Assert.assertEquals("prefix-01234567890abcdefgh", sExpanded);
        }
    }
